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
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.Resources;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer.DimensionHeatFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer.NodeTemperatureFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.summaries.pyrometer.NodeTemperatureSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.profile.level.FullNodeProfile;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.profile.level.NodeDimensionProfile;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NodeHeatRca extends Rca<NodeTemperatureFlowUnit> {
    private final CpuUtilHeatRca cpuUtilHeatRca;
    private static final Logger LOG = LogManager.getLogger(NodeHeatRca.class);
    //private final HeapAllocHeatRca heapAllocHeatRca;

    public NodeHeatRca(CpuUtilHeatRca cpuUtilHeatRca) {
        super(5);
        this.cpuUtilHeatRca = cpuUtilHeatRca;
        //this.heapAllocHeatRca = heapAllocHeatRca;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        List<NodeTemperatureFlowUnit> flowUnitList = new ArrayList<>();
        LOG.debug("rca: Executing fromWire: {}", this.getClass().getSimpleName());
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(NodeTemperatureFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }

    /**
     * The goal of the NodeHeatRca is to build a node level temperature profile.
     *
     * This is done by accumulating the {@code DimensionalFlowUnit} s it receives from the
     * individual ResourceHeatRcas. The heat profile build here is sent to the elected master
     * node where this is used to calculate the cluster temperature profile.
     * @return
     */
    @Override
    public NodeTemperatureFlowUnit operate() {
        List<DimensionHeatFlowUnit> cpuFlowUnits = cpuUtilHeatRca.getFlowUnits();
        // EachResourceLevelHeat RCA should generate a one @{code DimensionalFlowUnit}.
        if (cpuFlowUnits.size() != 1) {
            throw new IllegalArgumentException("One flow unit expected. Found: " + cpuFlowUnits);
        }

        List<NodeDimensionProfile> nodeDimensionProfiles = new ArrayList<>();
        nodeDimensionProfiles.add(cpuFlowUnits.get(0).getNodeDimensionProfile());
        FullNodeProfile nodeProfile = buildNodeProfile(nodeDimensionProfiles);

        System.out.println("Executing: " + name());


        ResourceContext resourceContext = new ResourceContext(Resources.State.UNKNOWN);
        NodeTemperatureSummary summary = new NodeTemperatureSummary(nodeProfile.getNodeId(),
                nodeProfile.getHostAddress());
        summary.fillFromNodeProfile(nodeProfile);

        return new NodeTemperatureFlowUnit(System.currentTimeMillis(), resourceContext, summary,
                true);
    }

    private FullNodeProfile buildNodeProfile(List<NodeDimensionProfile> dimensionProfiles) {
        ClusterDetailsEventProcessor.NodeDetails currentNodeDetails =
                ClusterDetailsEventProcessor.getCurrentNodeDetails();
        FullNodeProfile nodeProfile = new FullNodeProfile(currentNodeDetails.getId(),
                currentNodeDetails.getHostAddress());
        for (NodeDimensionProfile profile: dimensionProfiles) {
            nodeProfile.updateNodeDimensionProfile(profile);
        }
        return nodeProfile;
    }


}
