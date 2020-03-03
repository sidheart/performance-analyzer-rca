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

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.store;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.metrics.AllMetrics;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.RcaTestHelper;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.exceptions.MalformedConfig;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.ConnectedComponent;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.Queryable;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.core.RcaConf;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.util.RcaConsts;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.util.RcaUtil;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.persistence.Persistable;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.persistence.PersistenceFactory;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.scheduler.RCASchedulerTask;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.util.SQLiteReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.Test;

public class ResourceHeatMapGraphTest {

    class AnalysisGraphTest extends DummyGraph {
        @Override
        public void construct() {
            super.constructResourceHeatMapGraph();
        }
    }

    static class RcaSchedulerTaskT extends RCASchedulerTask {
        static final int THREADS = 3;
        static String cwd = System.getProperty("user.dir");
        static Path sqliteFile = Paths.get(cwd, "src", "test", "resources", "metricsdbs",
                "metricsdb_1582865425000");
        static Queryable reader;

        static RcaConf rcaConf =
                new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());
        static Persistable persistable;

        static {
            try {
                persistable = PersistenceFactory.create(rcaConf);
            } catch (MalformedConfig malformedConfig) {
                malformedConfig.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                reader = new SQLiteReader(sqliteFile.toString());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public RcaSchedulerTaskT(List<ConnectedComponent> connectedComponents) {
            super(
                    1000,
                    Executors.newFixedThreadPool(THREADS),
                    connectedComponents,
                    reader,
                    persistable,
                    new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString()),
                    null);
        }
    }

    @Test
    public void constructResourceHeatMapGraph() throws Exception {
        AnalysisGraph analysisGraph = new ResourceHeatMapGraphTest.AnalysisGraphTest();
        List<ConnectedComponent> connectedComponents =
                RcaUtil.getAnalysisGraphComponents(analysisGraph);
        RcaTestHelper.setEvaluationTimeForAllNodes(connectedComponents, 1);

        RCASchedulerTask rcaSchedulerTask = new ResourceHeatMapGraphTest.RcaSchedulerTaskT(connectedComponents);
        AllMetrics.NodeRole nodeRole = AllMetrics.NodeRole.DATA;
        RcaTestHelper.setMyIp("192.168.0.1", nodeRole);
        rcaSchedulerTask.run();
    }
}