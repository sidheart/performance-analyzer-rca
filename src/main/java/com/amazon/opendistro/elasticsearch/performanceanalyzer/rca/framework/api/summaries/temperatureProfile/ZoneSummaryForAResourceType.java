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
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.HeatZoneAssigner;
import com.google.protobuf.GeneratedMessageV3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jooq.Field;

public class ZoneSummaryForAResourceType extends GenericSummary {
    private final HeatZoneAssigner.Zone zone;
    private Shard minValueShard;
    private Shard maxValueShard;
    private List<Shard> shardListByTemperatureDesc;

    public ZoneSummaryForAResourceType(HeatZoneAssigner.Zone zone) {
        this.zone = zone;
        this.shardListByTemperatureDesc = new ArrayList<>();
        this.minValueShard = null;
        this.maxValueShard = null;
    }

    public void addShard(final Shard shard) {
        if (minValueShard == null || minValueShard.heatPoint.isGreaterThan(shard.heatPoint)) {
            minValueShard = shard;
        }
        if (maxValueShard == null || shard.heatPoint.isGreaterThan(maxValueShard.heatPoint)) {
            maxValueShard = shard;
        }

        shardListByTemperatureDesc.add(shard);
    }

    List<Shard> getShardListByTemperatureDesc() {
        Comparator<Shard> compareById = Comparator.comparingInt(s -> s.heatPoint.POINTS);
        Collections.sort(shardListByTemperatureDesc, compareById);
        return shardListByTemperatureDesc;
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
        return "ZoneSummaryForAResourceType{"
                + "zone=" + zone
                + ", minValueShard=" + minValueShard
                + ", maxValueShard=" + maxValueShard
                + "\n    allShardsInDesc=" + getShardListByTemperatureDesc()
                + '}';
    }
}
