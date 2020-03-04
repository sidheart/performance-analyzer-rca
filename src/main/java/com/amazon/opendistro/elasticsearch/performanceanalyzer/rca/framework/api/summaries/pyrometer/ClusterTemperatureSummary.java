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
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.GenericSummary;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.TemperatureVector;
import com.google.protobuf.GeneratedMessageV3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jooq.Field;

public class ClusterTemperatureSummary extends GenericSummary {
    private List<NodeTemperatureSummary> nodesSummary;
    private TemperatureVector temperatureVector;

    public ClusterTemperatureSummary() {
        nodesSummary = new ArrayList<>();
        temperatureVector = new TemperatureVector();
    }


    public TemperatureVector getTemperatureVector() {
        return temperatureVector;
    }

    public void setTemperatureByDimension(TemperatureVector.Dimension dimension,
                                          TemperatureVector.NormalizedValue value) {
        temperatureVector.updateTemperatureForDimension(dimension, value);
    }

    public void addNodesSummaries(Collection<NodeTemperatureSummary> nodeTemperatureSummaries) {
        nodesSummary.addAll(nodeTemperatureSummaries);
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
}
