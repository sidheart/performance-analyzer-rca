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

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.TemperatureVector;

public class FullNodeProfile {
    /**
     * A node has a temperature profile of its own. The temperature profile of a node is the mean
     * temperature along each dimension.
     */
    private TemperatureVector temperatureVector;

    /**
     * A node also has the complete list of shards in each dimension, broken down by the
     * different temperature zones.
     */
    private NodeDimensionProfile[] nodeDimensionProfiles;

    private final String nodeId;
    private final String hostAddress;

    public FullNodeProfile(String nodeId, String hostAddress) {
        this.nodeId = nodeId;
        this.hostAddress = hostAddress;
        this.nodeDimensionProfiles =
                new NodeDimensionProfile[TemperatureVector.Dimension.values().length];
        this.temperatureVector = new TemperatureVector();
    }

    public TemperatureVector getTemperatureVector() {
        return temperatureVector;
    }

    public NodeDimensionProfile[] getNodeDimensionProfiles() {
        return nodeDimensionProfiles;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void updateNodeDimensionProfile(NodeDimensionProfile nodeDimensionProfile) {
        TemperatureVector.Dimension dimension = nodeDimensionProfile.getProfileForDimension();
        this.nodeDimensionProfiles[dimension.ordinal()] = nodeDimensionProfile;
        temperatureVector.updateTemperatureForDimension(dimension, nodeDimensionProfile.getMeanTemperature());
    }

    public NodeDimensionProfile getProfileByDimension(TemperatureVector.Dimension dimension) {
        return nodeDimensionProfiles[dimension.ordinal()];
    }
}
