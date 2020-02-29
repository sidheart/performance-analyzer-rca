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

import com.amazon.opendistro.elasticsearch.performanceanalyzer.grpc.FlowUnitMessage;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.Rca;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.temperatureProfile.NodeSummaryForAResourceType;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.HeatPointSystem;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.PyrometerAggrMetrics;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.byShard.AvgCpuUtilByShards;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.byShard.CpuUtilByShard;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.capacity.NodeLevelUsageForCpu;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.shardIndependent.CpuUtilShardIndependent;
import java.util.ArrayList;
import java.util.List;

public class CpuUtilHeatRca extends Rca<ResourceFlowUnit> {
    private final NodeLevelUsageForCpu CPU_UTIL_PEAK_USAGE;
    private final CpuUtilByShard CPU_UTIL_BY_SHARD;
    private final AvgCpuUtilByShards AVG_CPU_UTIL_BY_SHARD;
    private final CpuUtilShardIndependent CPU_UTIL_SHARD_INDEPENDENT;

    private final HeatPointSystem THRESHOLD_PERCENT_FOR_HEAT_ZONE_ASSIGNMENT =
            new HeatPointSystem((short) 2);

    public CpuUtilHeatRca(CpuUtilByShard cpuUtilByShard, AvgCpuUtilByShards avgCpuUtilByShards,
                          CpuUtilShardIndependent cpuUtilShardIndependent, NodeLevelUsageForCpu cpuUtilPeakUsage) {
        super(5);
        this.CPU_UTIL_PEAK_USAGE = cpuUtilPeakUsage;
        this.CPU_UTIL_BY_SHARD = cpuUtilByShard;
        this.CPU_UTIL_SHARD_INDEPENDENT = cpuUtilShardIndependent;
        this.AVG_CPU_UTIL_BY_SHARD = avgCpuUtilByShards;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        List<ResourceFlowUnit> flowUnitList = new ArrayList<>();
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }

    @Override
    public ResourceFlowUnit operate() {
        NodeSummaryForAResourceType nodeSummaryForAResourceType =
                ResourceHeatCalculator.getResourceHeat(PyrometerAggrMetrics.PyrometerMetricType.CpuUtil,
                        CPU_UTIL_BY_SHARD,
                        AVG_CPU_UTIL_BY_SHARD, CPU_UTIL_SHARD_INDEPENDENT, CPU_UTIL_PEAK_USAGE,
                        THRESHOLD_PERCENT_FOR_HEAT_ZONE_ASSIGNMENT);
        System.out.println(nodeSummaryForAResourceType);
        return ResourceFlowUnit.generic();
    }
}
