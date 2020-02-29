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

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.temperatureProfile;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.grpc.FlowUnitMessage;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.GenericSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.HeatPointSystem;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.PyrometerAggrMetrics;
import com.google.protobuf.GeneratedMessageV3;
import java.util.List;
import org.jooq.Field;

public class NodeSummaryForAResourceType extends GenericSummary {
    private final HotZoneSummary hotZoneSummary;
    private final WarmZoneSummary warmZoneSummary;
    private final LukeWarmZoneSummary lukeWarmZoneSummary;
    private final ColdZoneSummary coldZoneSummary;

    private final HeatPointSystem meanTemperature;
    private final HeatPointSystem thresholdTemperature;
    private final double totalUsage;

    private final PyrometerAggrMetrics.PyrometerMetricType metricType;

    public NodeSummaryForAResourceType(HeatPointSystem meanTemperature,
                                       HeatPointSystem thresholdTemperature,
                                       double totalUsage,
                                       PyrometerAggrMetrics.PyrometerMetricType metricType) {
        this.meanTemperature = meanTemperature;
        this.thresholdTemperature = thresholdTemperature;
        this.totalUsage = totalUsage;
        this.metricType = metricType;
        hotZoneSummary = new HotZoneSummary();
        warmZoneSummary = new WarmZoneSummary();
        lukeWarmZoneSummary = new LukeWarmZoneSummary();
        coldZoneSummary = new ColdZoneSummary();
    }

    public HotZoneSummary getHotZoneSummary() {
        return hotZoneSummary;
    }

    public WarmZoneSummary getWarmZoneSummary() {
        return warmZoneSummary;
    }

    public LukeWarmZoneSummary getLukeWarmZoneSummary() {
        return lukeWarmZoneSummary;
    }

    public ColdZoneSummary getColdZoneSummary() {
        return coldZoneSummary;
    }

    public PyrometerAggrMetrics.PyrometerMetricType getMetricType() {
        return metricType;
    }

    @Override
    public <T extends GeneratedMessageV3> T buildSummaryMessage() {
        return null;
    }

    @Override
    public void buildSummaryMessageAndAddToFlowUnit(FlowUnitMessage.Builder messageBuilder) {

    }

    @Override
    public List<Field<?>> getSqlSchema() {
        return null;
    }

    @Override
    public List<Object> getSqlValue() {
        return null;
    }

    @Override
    public String toString() {
        return "NodeSummaryForAResourceType{"
                + "meanTemperature=" + meanTemperature
                + ", thresholdTemperature=" + thresholdTemperature
                + ", totalUsage=" + totalUsage
                + ", metricType=" + metricType
                + "\n  hotZoneSummary=" + hotZoneSummary
                + "\n  warmZoneSummary=" + warmZoneSummary
                + "\n  lukeWarmZoneSummary=" + lukeWarmZoneSummary
                + "\n  coldZoneSummary=" + coldZoneSummary
                + '}';
    }
}
