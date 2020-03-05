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

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.rca.temperature;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.temperature.DetailedNodeTemperatureFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.temperature.HeatZoneAssigner;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.temperature.NormalizedConsumption;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.temperature.ShardStore;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.temperature.TemperatureVector;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.temperature.DetailedNodeTemperatureSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.temperature.profile.level.ShardProfile;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.temperature.byShard.AvgResourceUsageAcrossAllIndexShardGroups;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.temperature.byShard.SumOverOperationsForIndexShardGroup;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.temperature.capacity.NodeLevelUsageForResourceType;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.PyrometerAggrMetricsShardIndependent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourceHeatCalculator {
    private static final Logger LOG = LogManager.getLogger(ResourceHeatCalculator.class);

    enum ColumnTypes {
        IndexName,
        ShardID,
        sum;
    }

    ;

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
    public static DetailedNodeTemperatureFlowUnit getResourceHeat(
            ShardStore shardStore, TemperatureVector.Dimension metricType,
            SumOverOperationsForIndexShardGroup resourceByShardId,
            AvgResourceUsageAcrossAllIndexShardGroups avgResUsageByAllShards,
            PyrometerAggrMetricsShardIndependent resourceShardIndependent,
            NodeLevelUsageForResourceType resourcePeakUsage,
            TemperatureVector.NormalizedValue threshold) {
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
        TemperatureVector.NormalizedValue avgUsageAcrossShards =
                NormalizedConsumption.calculate(avgValOverShards, totalConsumedInNode);

        List<List<String>> rowsPerShard = shardIdBasedFlowUnits.get(0).getData();

        DetailedNodeTemperatureSummary nodeDimensionProfile =
                new DetailedNodeTemperatureSummary(metricType, avgUsageAcrossShards, totalConsumedInNode);

        // The shardIdBasedFlowUnits is supposed to contain one row per shard.
        nodeDimensionProfile.setNumberOfShards(rowsPerShard.size());

        Map<ColumnTypes, Integer> columnTypesToColIndexMap = new HashMap<>();

        for (int i = 0; i < rowsPerShard.size(); i++) {
            if (i == 0) {
                List<String> colNames = rowsPerShard.get(0);
                int length = colNames.size();
                for (int colIdx = 0; colIdx < length; colIdx++) {
                    // This can throw IllegalArgument execption but we don't want to catch it
                    // here. That means the name of the column has changed in which case the enum
                    // must be changed accordingly.
                    columnTypesToColIndexMap.put(ColumnTypes.valueOf(colNames.get(colIdx)), colIdx);
                }
                // Row 0 is the column names. So, we skip it. Actual data starts from row 1.
                continue;
            }

            List<String> currRow = rowsPerShard.get(i);
            // Each row has columns like:
            // IndexName, ShardID, sum
            String indexName = currRow.get(columnTypesToColIndexMap.get(ColumnTypes.IndexName));
            int shardId;
            try {
                int shardIdIndex = columnTypesToColIndexMap.get(ColumnTypes.ShardID);
                String shardIdString = currRow.get(shardIdIndex);
                shardId = Integer.parseInt(shardIdString);
            } catch (NumberFormatException ex) {
                LOG.error("Could not parse the shardId into int for row: {}. Skipping..", currRow);
                continue;
            }

            try {
                int sumIndex = columnTypesToColIndexMap.get(ColumnTypes.sum);
                String sumString = currRow.get(sumIndex);
                String err = String.format("index:%s, shard:%s, value:%s", indexName, shardId, sumString);
                double usage = parseDoubleValue(sumString, err);

                TemperatureVector.NormalizedValue normalizedConsumptionByShard =
                        NormalizedConsumption.calculate(usage, totalConsumedInNode);
                HeatZoneAssigner.Zone heatZoneForShard = HeatZoneAssigner.assign(normalizedConsumptionByShard, avgUsageAcrossShards,
                        threshold);

                ShardProfile shardProfile = shardStore.getOrCreateIfAbsent(indexName, shardId);
                shardProfile.addTemperatureForDimension(metricType, normalizedConsumptionByShard);
                nodeDimensionProfile.addShardToZone(shardProfile, heatZoneForShard);
            } catch (NumberFormatException ex) {
                continue;
            }
        }
        return new DetailedNodeTemperatureFlowUnit(System.currentTimeMillis(), nodeDimensionProfile);
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
