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

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.rca.pyrometer;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.Rca;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.Resources;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer.ClusterTemperatureFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer.ClusterTemperatureSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer.NodeTemperatureFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer.NodeTemperatureSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.NormalizedConsumption;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.TemperatureVector;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClusterHeatRca extends Rca<ResourceFlowUnit> {
    private final NodeHeatRca nodeHeatRca;
    private static final Logger LOG = LogManager.getLogger(ClusterHeatRca.class);

    public ClusterHeatRca(NodeHeatRca nodeHeatRca) {
        super(5);
        this.nodeHeatRca = nodeHeatRca;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new IllegalArgumentException(name() + "'s generateFlowUnitListFromWire() should not " +
                "be required.");
    }

    /**
     * @return
     */
    @Override
    public ResourceFlowUnit operate() {
        List<NodeTemperatureFlowUnit> flowUnits = nodeHeatRca.getFlowUnits();
        Map<String, NodeTemperatureSummary> nodeTemperatureSummaryMap = new HashMap<>();

        ClusterTemperatureSummary clusterTemperatureSummary = new ClusterTemperatureSummary();
        for (TemperatureVector.Dimension dimension : TemperatureVector.Dimension.values()) {
            double totalForDimension = 0.0;
            for (NodeTemperatureFlowUnit nodeFlowUnit : flowUnits) {
                NodeTemperatureSummary summary = nodeFlowUnit.getNodeTemperatureSummary();
                totalForDimension += summary.getTotalConsumedByDimension(dimension);
            }
            double averageForDimension = totalForDimension / flowUnits.size();
            TemperatureVector.NormalizedValue value =
                    NormalizedConsumption.calculate(averageForDimension, totalForDimension);
            clusterTemperatureSummary.setTemperatureByDimension(dimension, value);

            for (NodeTemperatureFlowUnit nodeFlowUnit : flowUnits) {
                NodeTemperatureSummary obtainedNodeTempSummary =
                        nodeFlowUnit.getNodeTemperatureSummary();
                String key = obtainedNodeTempSummary.getNodeId();

                nodeTemperatureSummaryMap.putIfAbsent(key,
                        new NodeTemperatureSummary(obtainedNodeTempSummary.getNodeId(), obtainedNodeTempSummary.getHostAddress()));
                NodeTemperatureSummary constructedNodeTemperatureSummary = nodeTemperatureSummaryMap.get(key);

                double obtainedTotal = obtainedNodeTempSummary.getTotalConsumedByDimension(dimension);
                TemperatureVector.NormalizedValue newClusterBasedValue =
                        NormalizedConsumption.calculate(obtainedTotal, totalForDimension);

                constructedNodeTemperatureSummary.setTemperatureForDimension(dimension, newClusterBasedValue);
                constructedNodeTemperatureSummary.setNumOfShards(dimension,
                        obtainedNodeTempSummary.getNumberOfShardsByDimension(dimension));
                constructedNodeTemperatureSummary.setTotalConsumedByDimension(dimension, obtainedTotal);
            }
        }
        clusterTemperatureSummary.addNodesSummaries(nodeTemperatureSummaryMap.values());
        return new ClusterTemperatureFlowUnit(System.currentTimeMillis(),
                new ResourceContext(Resources.State.UNKNOWN), clusterTemperatureSummary);
    }
}
