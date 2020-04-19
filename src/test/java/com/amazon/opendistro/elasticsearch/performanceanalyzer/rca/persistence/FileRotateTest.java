/*
 *  Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.persistence;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.RcaLog4j2ConfigurationFactory;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.RcaTestHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

public class FileRotateTest {
  static {
    ConfigurationFactory.setConfigurationFactory(new RcaLog4j2ConfigurationFactory());
  }

  private static final Logger LOG = LogManager.getLogger(FileRotateTest.class);

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
  private static Path testLocation = null;
  private Path fileToRotate = null;

  @BeforeClass
  public static void cleanupLogs() {
    List<String> allLines = RcaTestHelper.getAllLinesFromLog(RcaTestHelper.LogType.PerformanceAnalyzerLog);
    LOG.info("Pre truncation lines are: {}", allLines);
    RcaTestHelper.cleanUpLogs();
    allLines = RcaTestHelper.getAllLinesFromLog(RcaTestHelper.LogType.PerformanceAnalyzerLog);
    LOG.info("Post truncation lines are: {}", allLines);

  }

  @AfterClass
  public static void cleanup() throws IOException {
    cleanupLogs();
    FileUtils.cleanDirectory(testLocation.toFile());
  }

  @Before
  public void init() throws IOException {
    List<String> allLines = RcaTestHelper.getAllLinesFromLog(RcaTestHelper.LogType.PerformanceAnalyzerLog);
    LOG.info("Pre init lines are: {}", allLines);
    String cwd = System.getProperty("user.dir");
    testLocation = Paths.get(cwd, "src", "test", "resources", "tmp", "file_rotate");
    Files.createDirectories(testLocation);
    FileUtils.cleanDirectory(testLocation.toFile());
    fileToRotate = Paths.get(testLocation.toString(), "fileRotate.test");
    Files.deleteIfExists(fileToRotate);
    allLines = RcaTestHelper.getAllLinesFromLog(RcaTestHelper.LogType.PerformanceAnalyzerLog);
    LOG.info("Post init lines are: {}", allLines);
  }

  class TestFileRotate extends FileRotate {
    TestFileRotate(TimeUnit rotation_time_unit, long rotation_period) {
      super(fileToRotate, rotation_time_unit, rotation_period, DATE_FORMAT);
    }

    public void setLastRotated(long value) {
      lastRotatedMillis = value;
    }

    @Override
    public Path rotate(long millis) throws IOException {
      return super.rotate(millis);
    }

    @Override
    public boolean shouldRotate(long currentTimeMillis) {
      return super.shouldRotate(currentTimeMillis);
    }
  }

  @Test
  public void shouldRotate() throws InterruptedException, IOException {
    TestFileRotate fileRotate = new TestFileRotate(TimeUnit.MILLISECONDS, 100);
    Thread.sleep(100);
    Assert.assertTrue(fileRotate.shouldRotate(System.currentTimeMillis()));
    fileRotate.setLastRotated(System.currentTimeMillis());
    Assert.assertFalse(fileRotate.shouldRotate(System.currentTimeMillis()));
  }

  @Test
  public void rotate() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
    List<String> allLines = RcaTestHelper.getAllLinesFromLog(RcaTestHelper.LogType.PerformanceAnalyzerLog);
    LOG.info("Pre rotate line are: {}", allLines);

    TestFileRotate fileRotate = new TestFileRotate(TimeUnit.MILLISECONDS, 100);
    Assert.assertFalse(fileToRotate.toFile().exists());
    Assert.assertNull(fileRotate.rotate(System.currentTimeMillis()));

    // Let's create a file and try rotating it.
    long currentMillis = System.currentTimeMillis();

    Files.createFile(fileToRotate);
    Assert.assertTrue(fileToRotate.toFile().exists());
    fileRotate.rotate(currentMillis);
    String formatNow = DATE_FORMAT.format(currentMillis);
    for (String f : testLocation.toFile().list()) {
      String prefix = fileToRotate.getFileName() + "." + formatNow;
      Assert.assertTrue(
          String.format("expected prefix: '%s', found: '%s'", prefix, f), f.startsWith(prefix));
    }

    Assert.assertFalse(fileToRotate.toFile().exists());
    Files.createFile(fileToRotate);
    Assert.assertTrue(fileToRotate.toFile().exists());
    fileRotate.rotate(currentMillis);
    allLines = RcaTestHelper.getAllLinesFromLog(RcaTestHelper.LogType.PerformanceAnalyzerLog);
    LOG.info("It's over here: {}", RcaTestHelper.getLogFilePath(RcaTestHelper.LogType.PerformanceAnalyzerLog));
    Assert.assertTrue(Files.exists(Paths.get(RcaTestHelper.getLogFilePath(RcaTestHelper.LogType.PerformanceAnalyzerLog))));
    List<String> lines =
        RcaTestHelper.getAllLogLinesWithMatchingString(
            RcaTestHelper.LogType.PerformanceAnalyzerLog, "FileAlreadyExistsException");
    LOG.info("Post rotate lines are: {}", allLines);
    Assert.assertEquals(1, lines.size());
  }

  @Test
  public void tryRotate() throws IOException {
    TestFileRotate fileRotate = new TestFileRotate(TimeUnit.MILLISECONDS, 100);
    long currentMillis = System.currentTimeMillis();
    fileRotate.setLastRotated(currentMillis - 100);

    Files.createFile(fileToRotate);
    Assert.assertTrue(fileToRotate.toFile().exists());
    fileRotate.tryRotate(currentMillis);

    String formatNow = DATE_FORMAT.format(currentMillis);
    for (String f : testLocation.toFile().list()) {
      String prefix = fileToRotate.getFileName() + "." + formatNow;
      Assert.assertTrue(
              String.format("expected prefix: '%s', found: '%s'", prefix, f), f.startsWith(prefix));
    }
    currentMillis = System.currentTimeMillis();
    fileRotate.setLastRotated(currentMillis);
    Assert.assertEquals(null, fileRotate.tryRotate(currentMillis));

  }
}
