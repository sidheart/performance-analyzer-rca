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
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.Resources;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;

public class NodeTemperatureFlowUnit extends ResourceFlowUnit {
    private final NodeTemperatureSummary nodeSummary;

    public NodeTemperatureFlowUnit(long timeStamp, ResourceContext context,
                                   NodeTemperatureSummary resourceSummary,
                                   boolean persistSummary) {
        super(timeStamp, context, resourceSummary, persistSummary);
        this.nodeSummary = resourceSummary;
    }

    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, String esNode) {
        FlowUnitMessage.Builder builder = FlowUnitMessage.newBuilder();
        builder.setGraphNode(graphNode);
        builder.setEsNode(esNode);
        builder.setTimeStamp(System.currentTimeMillis());
        nodeSummary.buildSummaryMessageAndAddToFlowUnit(builder);
        return builder.build();
    }

    public static NodeTemperatureFlowUnit buildFlowUnitFromWrapper(final FlowUnitMessage message) {
        NodeTemperatureSummary nodeTemperatureSummary =
                NodeTemperatureSummary.buildNodeTemperatureProfileFromMessage(message.getNodeTemperatureSummary());
        return new NodeTemperatureFlowUnit(message.getTimeStamp(), new ResourceContext(Resources.State.UNKNOWN),
                nodeTemperatureSummary, false);
    }
}
