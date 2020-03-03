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

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.capacity;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.heat.TemperatureVector;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.pyrometer.PyrometerAggrMetrics;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;

/**
 * This class gets all rows for the given metric table where the shardID is not NULL. This is to
 * tract resource utilization by shard ID. Shard on a node can be uniquely identified by the
 * index name and shard ID. It might also help to also consider the Operation dimension ?
 */
public class NodeLevelUsageForResourceType extends PyrometerAggrMetrics {
    // For peak usage there is no group by clause used, therefore this is empty.
    private static final String[] dimensions = {};

    public NodeLevelUsageForResourceType(TemperatureVector.Dimension metricType) {
        super(metricType, dimensions);
    }

    @Override
    protected Result<Record> createDslAndFetch(final DSLContext context,
                                               final String tableName,
                                               final Field<?> aggDimension,
                                               final List<Field<?>> groupByFieldsList,
                                               final List<Field<?>> selectFieldsList) {

        Result<?> r1 = context
                .select(selectFieldsList)
                .from(tableName)
                .fetch();
        return (Result<Record>) r1;
    }

    @Override
    protected List<Field<?>> getSelectFieldsList(final List<Field<?>> groupByFields,
                                                 Field<?> aggrDimension) {
        return aggrColumnAsSelectField();
        // List<Field<?>> selectFieldsList = new ArrayList<>(aggrColumnAsSelectField());

        // final Field<Double> numDimension = DSL.field(
        //         DSL.name(PyrometerAggrMetrics.METRICS_DB_AGG_COLUMN_USED), Double.class);
        // // This aggregate function is applied after group by.

        // selectFieldsList.add(getAggDimension(numDimension, AggregateFunction.AVG));

        // return selectFieldsList;
    }
}
