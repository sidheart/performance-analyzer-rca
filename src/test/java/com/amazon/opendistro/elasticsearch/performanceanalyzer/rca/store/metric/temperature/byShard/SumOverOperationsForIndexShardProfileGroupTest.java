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

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.temperature.byShard;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.Queryable;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.temperature.capacity.NodeLevelUsageForCpu;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.CpuUtilShardIndependent;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.util.SQLiteReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class SumOverOperationsForIndexShardProfileGroupTest {

    @Test
    public void readCPU() throws SQLException {
        String cwd = System.getProperty("user.dir");
        Path sqliteFile = Paths.get(cwd, "src", "test", "resources", "metricsdbs",
                "metricsdb_1582661700000");
        Queryable reader = new SQLiteReader(sqliteFile.toString());
        CpuUtilByShard cpuUtilByShard = new CpuUtilByShard();
        MetricFlowUnit mfu = cpuUtilByShard.gather(reader);

        List<String> expected = new ArrayList<String>() {{
            add("accounts");
            add("0");
            add("0.0064432211");
        }};

        List<String> actual = mfu.getData().get(1);
        for (int i = 0; i < actual.size(); i++) {
            if (i == 2) {
                // The actiual value because it is a double we only compare the first 10 digits.
                Assert.assertEquals(expected.get(i), actual.get(i).substring(0, 12));
            } else {
                Assert.assertEquals(expected.get(i), actual.get(i));
            }
        }

        AvgCpuUtilByShards avgCpuUtilByShards = new AvgCpuUtilByShards();
        System.out.println(avgCpuUtilByShards.gather(reader).getData());

        Map<String, String> map = new HashMap<String, String>() {{
            put("sum", "0.1266879414");
        }};

        CpuUtilShardIndependent shardIndependent = new CpuUtilShardIndependent();
        mfu = shardIndependent.gather(reader);

        // We expect the mfu.getdata() to be: [[sum], [0.126687941459211]]
        Assert.assertEquals(2, mfu.getData().size());
        Assert.assertEquals("sum", mfu.getData().get(0).get(0));
        Assert.assertEquals("0.1266879414", mfu.getData().get(1).get(0).substring(0, 12));


        NodeLevelUsageForCpu cpuUtilPeakUsage = new NodeLevelUsageForCpu();
        mfu = cpuUtilPeakUsage.gather(reader);

        Assert.assertEquals(0.133131162644534, Double.parseDouble(mfu.getData().get(1).get(0)),
                0.0001);
    }
}