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
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.profile.level.NodeDimensionProfile;

public class DimensionHeatFlowUnit extends ResourceFlowUnit {
    private final NodeDimensionProfile nodeDimensionProfile;

    public DimensionHeatFlowUnit(long timeStamp, NodeDimensionProfile nodeDimensionProfile) {
        super(timeStamp);
        this.nodeDimensionProfile = nodeDimensionProfile;
    }

    // A dimension flow unit never leaves a node. So, we don't need to generate protobuf messages.
    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, String esNode) {
        return null;
    }

    public NodeDimensionProfile getNodeDimensionProfile() {
        return nodeDimensionProfile;
    }
}
