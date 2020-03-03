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

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.profile.level;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.HeatZoneAssigner;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.TemperatureVector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A node dimension profile is categorization of all shards in the node into different heatZones.
 */
public class NodeDimensionProfile {
    private final TemperatureVector.Dimension profileForDimension;
    private final TemperatureVector.NormalizedValue meanTemperature;
    private final double totalUsage;

    private final ZoneProfile[] zoneProfiles;
    private int numberOfShards;

    public NodeDimensionProfile(TemperatureVector.Dimension profileForDimension, TemperatureVector.NormalizedValue meanTemperature, double totalUsage) {
        this.profileForDimension = profileForDimension;
        this.meanTemperature = meanTemperature;
        this.totalUsage = totalUsage;
        this.zoneProfiles = new ZoneProfile[HeatZoneAssigner.Zone.values().length];
        for (int i = 0; i < this.zoneProfiles.length; i++) {
            this.zoneProfiles[i] = new ZoneProfile(HeatZoneAssigner.Zone.values()[i]);
        }
    }

    public int getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public void addShardToZone(ShardProfile shard, HeatZoneAssigner.Zone zone) {
        ZoneProfile profile = zoneProfiles[zone.ordinal()];
        profile.addShard(shard);
    }

    @Override
    public String toString() {
        return "NodeDimensionProfile{" +
                "profileForDimension=" + profileForDimension +
                ", zoneProfiles=" + Arrays.toString(zoneProfiles) +
                '}';
    }

    public TemperatureVector.NormalizedValue getMeanTemperature() {
        return meanTemperature;
    }

    public TemperatureVector.Dimension getProfileForDimension() {
        return profileForDimension;
    }

    public double getTotalUsage() {
        return totalUsage;
    }

    class ZoneProfile {
        List<ShardProfile> shardProfiles;
        ShardProfile minShard;
        ShardProfile maxShard;

        private final HeatZoneAssigner.Zone myZone;

        ZoneProfile(HeatZoneAssigner.Zone myZone) {
            this.myZone = myZone;
            shardProfiles = new ArrayList<>();
        }

        @Nonnull
        public List<ShardProfile> getOrderedShardProfiles() {
            return null;
        }

        void addShard(ShardProfile shard) {
            if (minShard == null) {
                minShard = shard;
            } else {
                if (getMinTemperature().isGreaterThan(shard.getHeatInDimension(profileForDimension))) {
                    minShard = shard;
                }
            }

            if (maxShard == null) {
                maxShard = shard;
            } else {
                if (shard.getHeatInDimension(profileForDimension).isGreaterThan(getMaxTemperature())) {
                    maxShard = shard;
                }
            }
        }

        @Nullable
        TemperatureVector.NormalizedValue getMinTemperature() {
            if (minShard != null) {
                return minShard.getHeatInDimension(profileForDimension);
            }
            return null;
        }

        @Nullable
        TemperatureVector.NormalizedValue getMaxTemperature() {
            if (maxShard != null) {
                return maxShard.getHeatInDimension(profileForDimension);
            }
            return null;
        }

        @Override
        public String toString() {
            return "{"
                    + "myZone=" + myZone
                    + ", shardProfiles=" + shardProfiles
                    + ", minShard=" + minShard
                    + ", maxShard=" + maxShard
                    + '}';
        }
    }
}
