/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.grpc.FlowUnitMessage;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.grpc.NodeTemperatureSummaryMessage;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.grpc.ResourceTemperatureMessage;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.GenericSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.TemperatureVector;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.profile.level.FullNodeProfile;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.profile.level.NodeDimensionProfile;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class NodeTemperatureSummary extends GenericSummary {
    public static final String NODE_TEMPERATURE_SUMMARY_TABLE =
            NodeTemperatureSummary.class.getSimpleName();
    // private final FullNodeProfile nodeProfile;
    private TemperatureVector temperatureVector;
    private final String nodeId;
    private final String hostAddress;
    private double totalConsumedByDimension[];
    private int numOfShards[];

    public NodeTemperatureSummary(String nodeId, String hostAddress) {
        super();
        this.nodeId = nodeId;
        this.hostAddress = hostAddress;
        this.temperatureVector = new TemperatureVector();
    }

    public void fillFromNodeProfile(FullNodeProfile nodeProfile) {
        this.temperatureVector = nodeProfile.getTemperatureVector();
        this.totalConsumedByDimension = new double[TemperatureVector.Dimension.values().length];
        this.numOfShards = new int[TemperatureVector.Dimension.values().length];
        for (NodeDimensionProfile nodeDimensionProfile : nodeProfile.getNodeDimensionProfiles()) {
            if (nodeDimensionProfile != null) {
                int index = nodeDimensionProfile.getProfileForDimension().ordinal();
                totalConsumedByDimension[index] = nodeDimensionProfile.getTotalUsage();
                numOfShards[index] = nodeDimensionProfile.getNumberOfShards();
            }
        }
    }

    public double getTotalConsumedByDimension(TemperatureVector.Dimension dimension) {
        return totalConsumedByDimension[dimension.ordinal()];
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setTotalConsumedByDimension(TemperatureVector.Dimension dimension,
                                            double totalConsumedByDimension) {
        this.totalConsumedByDimension[dimension.ordinal()] = totalConsumedByDimension;
    }

    public void setNumOfShards(TemperatureVector.Dimension dimension, int numOfShards) {
        this.numOfShards[dimension.ordinal()] = numOfShards;
    }

    public int  getNumberOfShardsByDimension(TemperatureVector.Dimension dimension) {
        return numOfShards[dimension.ordinal()];
    }

    public void setTemperatureForDimension(TemperatureVector.Dimension dimension,
                                           TemperatureVector.NormalizedValue value) {
        temperatureVector.updateTemperatureForDimension(dimension, value);
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public NodeTemperatureSummaryMessage buildSummaryMessage() {
        if (totalConsumedByDimension == null) {
            throw new IllegalArgumentException("totalConsumedByDimension is not initialized");
        }

        final NodeTemperatureSummaryMessage.Builder summaryBuilder =
                NodeTemperatureSummaryMessage.newBuilder();
        summaryBuilder.setNodeID(nodeId);
        summaryBuilder.setHostAddress(hostAddress);


        for (TemperatureVector.Dimension dimension : TemperatureVector.Dimension.values()) {
            int index = dimension.ordinal();
            ResourceTemperatureMessage.Builder builder = ResourceTemperatureMessage.newBuilder();
            builder.setResourceName(dimension.NAME);
            builder.setMeanUsage(temperatureVector.getTemperatureFor(dimension).getPOINTS());
            builder.setNumberOfShards(numOfShards[index]);
            builder.setTotalUsage(totalConsumedByDimension[index]);
            summaryBuilder.setCpuTemperature(index, builder);
        }
        return summaryBuilder.build();
    }

    @Override
    public void buildSummaryMessageAndAddToFlowUnit(FlowUnitMessage.Builder messageBuilder) {
        messageBuilder.setNodeTemperatureSummary(buildSummaryMessage());
    }

    public static NodeTemperatureSummary buildNodeTemperatureProfileFromMessage(NodeTemperatureSummaryMessage message) {
        NodeTemperatureSummary nodeTemperatureSummary =
                new NodeTemperatureSummary(message.getNodeID(), message.getHostAddress());

        nodeTemperatureSummary.totalConsumedByDimension = new double[TemperatureVector.Dimension.values().length];
        nodeTemperatureSummary.numOfShards = new int[TemperatureVector.Dimension.values().length];
        for (ResourceTemperatureMessage resourceMessage : message.getCpuTemperatureList()) {
            TemperatureVector.Dimension dimension =
                    TemperatureVector.Dimension.valueOf(resourceMessage.getResourceName());
            nodeTemperatureSummary.temperatureVector.updateTemperatureForDimension(dimension,
                    new TemperatureVector.NormalizedValue((short) resourceMessage.getMeanUsage()));
            nodeTemperatureSummary.totalConsumedByDimension[dimension.ordinal()] =
                    resourceMessage.getTotalUsage();
            nodeTemperatureSummary.numOfShards[dimension.ordinal()] =
                    resourceMessage.getNumberOfShards();
        }
        return nodeTemperatureSummary;
    }

    /**
     * @return Returns a list of columns that this table would contain.
     */
    @Override
    public List<Field<?>> getSqlSchema() {
        List<Field<?>> schema = new ArrayList<>();
        schema.add(DSL.field(DSL.name(HotNodeSummary.SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME), String.class));
        schema.add(DSL.field(DSL.name(HotNodeSummary.SQL_SCHEMA_CONSTANTS.HOST_IP_ADDRESS_COL_NAME), String.class));
        for (TemperatureVector.Dimension dimension : TemperatureVector.Dimension.values()) {
            schema.add(DSL.field(DSL.name(dimension.NAME + "_mean_usage"), String.class));
            schema.add(DSL.field(DSL.name(dimension.NAME + "_total_usage"), String.class));
            schema.add(DSL.field(DSL.name(dimension.NAME + "_num_shards"), String.class));
        }
        return schema;
    }

    @Override
    public List<Object> getSqlValue() {
        List<Object> value = new ArrayList<>();
        value.add(nodeId);
        value.add(hostAddress);

        for (TemperatureVector.Dimension dimension : TemperatureVector.Dimension.values()) {
            value.add(temperatureVector.getTemperatureFor(dimension));
            value.add(totalConsumedByDimension[dimension.ordinal()]);
            value.add(numOfShards[dimension.ordinal()]);
        }
        return value;
    }
}
