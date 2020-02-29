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

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.temperatureProfile.NodeSummaryForAResourceType;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.temperatureProfile.Shard;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.HeatPointSystem;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.HeatZoneAssigner;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.NormalizedConsumption;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.PyrometerAggrMetrics;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.byShard.AvgResourceUsageAcrossAllIndexShardGroups;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.byShard.SumOverOperationsForIndexShardGroup;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.capacity.NodeLevelUsageForResourceType;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.shardIndependent.PyrometerAggrMetricsShardIndependent;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceHeatCalculator {
    private static final Logger LOG = LogManager.getLogger(ResourceHeatCalculator.class);

    /**
     * The categorization of shards as hot, warm, lukeWarm and cold based on the average resource
     * utilization of the resource across all shards. This value is not the actual number but a
     * normalized value that is between 0 and 10.
     *
     * <p>The shard independent usage of the resource as a normalized value between 0 and 10.
     *
     * <p>The Node temperature as the actual value. (If normalized to 10 this will always be 10
     * as this is the base for the normalization).
     *
     * @param resourceByShardId        This gives the resource utilization at a shard level
     * @param resourceShardIndependent This is the additional component that use resource but
     *                                 cannot be accounted for at a shard level. For
     *                                 example, HttpServer consuming CPU will form part of this.
     * @param resourcePeakUsage        This is the total usage of the resource at the node level. This
     *                                 should be the sum of the other two.
     * @return The return is a composition of three things:
     */
    public static NodeSummaryForAResourceType getResourceHeat(PyrometerAggrMetrics.PyrometerMetricType metricType,
                                                              SumOverOperationsForIndexShardGroup resourceByShardId,
                                                              AvgResourceUsageAcrossAllIndexShardGroups avgResUsageByAllShards,
                                                              PyrometerAggrMetricsShardIndependent resourceShardIndependent,
                                                              NodeLevelUsageForResourceType resourcePeakUsage,
                                                              HeatPointSystem threshold) {
        List<MetricFlowUnit> shardIdBasedFlowUnits = resourceByShardId.getFlowUnits();
        List<MetricFlowUnit> avgResUsageFlowUnits = avgResUsageByAllShards.getFlowUnits();
        List<MetricFlowUnit> shardIdIndependentFlowUnits = resourceShardIndependent.getFlowUnits();
        List<MetricFlowUnit> resourcePeakFlowUnits = resourcePeakUsage.getFlowUnits();

        // example:
        // [0: [[IndexName, ShardID, sum], [geonames, 0, 0.35558242693567], [geonames, 2, 0.0320651297686606]]]
        if (shardIdBasedFlowUnits.size() != 1 || shardIdBasedFlowUnits.get(0).getData().get(0).size() != 3) {
            // we expect it to have three columns but the number of rows is determined by the
            // number of indices and shards in the node.
            throw new IllegalArgumentException("Size more than expected: " + shardIdBasedFlowUnits);
        }
        if (avgResUsageFlowUnits.size() != 1 || avgResUsageFlowUnits.get(0).getData().size() != 2) {
            throw new IllegalArgumentException("Size more than expected: " + avgResUsageFlowUnits);
        }
        if (shardIdIndependentFlowUnits.size() != 1 || shardIdIndependentFlowUnits.get(0).getData().size() != 2) {
            throw new IllegalArgumentException("Size more than expected: " + shardIdIndependentFlowUnits);
        }
        if (resourcePeakFlowUnits.size() != 1 || resourcePeakFlowUnits.get(0).getData().size() != 2) {
            throw new IllegalArgumentException("Size more than expected: " + resourcePeakFlowUnits);
        }
        double avgValOverShards =
                parseDoubleValue(avgResUsageFlowUnits.get(0).getData().get(1).get(0),
                        "AverageResourceUsageAcrossShards");
        double totalConsumedInNode =
                parseDoubleValue(resourcePeakFlowUnits.get(0).getData().get(1).get(0),
                        "totalResourceConsumed");
        HeatPointSystem avgUsageAcrossShards =
                NormalizedConsumption.calculate(avgValOverShards, totalConsumedInNode);

        List<List<String>> rowsPerShard = shardIdBasedFlowUnits.get(0).getData();

        NodeSummaryForAResourceType nodeSummaryForAResourceType =
                new NodeSummaryForAResourceType(avgUsageAcrossShards, threshold, totalConsumedInNode, metricType);

        for (int i = 0; i < rowsPerShard.size(); i++) {
            if (i == 0) {
                continue;
            }
            // Each row has columns like:
            // IndexName, ShardID, sum
            String indexName = rowsPerShard.get(i).get(0);
            int shardId;
            try {
                shardId = Integer.parseInt(rowsPerShard.get(i).get(1));
            } catch (NumberFormatException ex) {
                continue;
            }

            String err = String.format("index:%s, shard:%s, value:%s", indexName, shardId,
                    rowsPerShard.get(i).get(2));

            try {
                double usage = parseDoubleValue(rowsPerShard.get(i).get(2), err);
                HeatPointSystem normalizedConsumptionByShard =
                        NormalizedConsumption.calculate(usage, totalConsumedInNode);
                HeatZoneAssigner.Zone heatZoneForShard = HeatZoneAssigner.assign(normalizedConsumptionByShard, avgUsageAcrossShards,
                        threshold);
                Shard shard = new Shard(indexName, shardId, normalizedConsumptionByShard);
                switch (heatZoneForShard) {
                    case HOT:
                        nodeSummaryForAResourceType.getHotZoneSummary().addShard(shard);
                        break;
                    case WARM:
                        nodeSummaryForAResourceType.getWarmZoneSummary().addShard(shard);
                        break;
                    case LUKE_WARM:
                        nodeSummaryForAResourceType.getLukeWarmZoneSummary().addShard(shard);
                        break;
                    case COLD:
                        nodeSummaryForAResourceType.getColdZoneSummary().addShard(shard);
                        break;
                }
            } catch (NumberFormatException ex) {
                continue;
            }
        }

        return nodeSummaryForAResourceType;
    }

    private static double parseDoubleValue(String val, String identifier) {
        try {
            double totalConsumedInNode =
                    Double.parseDouble(val);
            return totalConsumedInNode;
        } catch (NumberFormatException ne) {
            LOG.error("Error parsing double from in {}, found {}: " + identifier, val);
            throw ne;
        }
    }
}
