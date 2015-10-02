/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.das.integration.tests.tools;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.analytics.api.AnalyticsDataAPI;
import org.wso2.carbon.analytics.api.CarbonAnalyticsAPI;
import org.wso2.carbon.analytics.stream.persistence.stub.dto.AnalyticsTable;
import org.wso2.carbon.analytics.stream.persistence.stub.dto.AnalyticsTableRecord;
import org.wso2.carbon.analytics.webservice.stub.beans.RecordBean;
import org.wso2.carbon.analytics.webservice.stub.beans.StreamDefAttributeBean;
import org.wso2.carbon.analytics.webservice.stub.beans.StreamDefinitionBean;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.das.integration.common.clients.AnalyticsWebServiceClient;
import org.wso2.das.integration.common.clients.DataPublisherClient;
import org.wso2.das.integration.common.clients.EventStreamPersistenceClient;
import org.wso2.das.integration.common.clients.MessageConsoleClient;
import org.wso2.das.integration.common.utils.DASIntegrationTest;

import java.io.File;
import java.io.IOException;

public class BackupToolTestCase extends DASIntegrationTest{
    private static final String BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME = "backup.tool.persist.table";
    private static final String BACKUP_TOOL_FILESYSTEM_TEST_STREAM_NAME = "backup.tool.persist.table";
    private static final String STREAM_VERSION = "1.0.0";
    private static final int RECORD_COUNT = 100;
    private static final int MAX_TRIES = 5;
    //todo: change this path to have it inside the DAS deployment
    private static final String BACKUP_DIR_PATH = "/home/sachith/temp/test";
    private DataPublisherClient dataPublisherClient;
    private AnalyticsWebServiceClient webServiceClient;
    private EventStreamPersistenceClient persistenceClient;
    private ServerConfigurationManager serverManager;
    private MessageConsoleClient messageConsoleClient;
    private AnalyticsDataAPI analyticsDataAPI;

    @BeforeClass(alwaysRun = true, dependsOnGroups = "wso2.das")
    protected void init() throws Exception {
        super.init();
        String session = getSessionCookie();
        messageConsoleClient = new MessageConsoleClient(backendURL, session);
        webServiceClient = new AnalyticsWebServiceClient(backendURL, session);
        persistenceClient = new EventStreamPersistenceClient(backendURL, session);
        String apiConf =
                new File(this.getClass().getClassLoader().
                        getResource("dasconfig" + File.separator + "api" + File.separator + "analytics-data-config.xml").toURI())
                        .getAbsolutePath();
        analyticsDataAPI = new CarbonAnalyticsAPI(apiConf);
        analyticsDataAPI.deleteTable(-1234, BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.', '_'));
    }
    @Test(groups = "wso2.das.backupTool", description = "Test backend availability of persistence service")
    public void testBackendAvailability() throws Exception {
        init();
        Assert.assertTrue(persistenceClient.isBackendServicePresent(), "The persistence service is unavailable");
    }

    @Test(groups = "wso2.das.backupTool", description = "Adding the new analytics table", dependsOnMethods = "testBackendAvailability")
    public void addAnalyticsTable() throws Exception {
        StreamDefinitionBean streamDefinition = getEventStreamBean();
        webServiceClient.addStreamDefinition(streamDefinition);
        AnalyticsTable analyticsTable = getAnalyticsTable();
        persistenceClient.addAnalyticsTable(analyticsTable);
        //todo: take out this thread.sleep if can
        Thread.sleep(15000);
        /*int tries = 0;
        while (persistenceClient.getAnalyticsTable(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME, STREAM_VERSION) == null) {
            Thread.sleep(3000);
            tries++;
            if(tries >= MAX_TRIES)
                Assert.fail("Could not persist the analytics table: " + BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.','_'));
        }*/
    }

    @Test(groups = "wso2.das.backupTool", description = "Test if the table is created", dependsOnMethods = "addAnalyticsTable")
    public void testPersistenceOfTable() throws Exception {
        AnalyticsTable analyticsTable = persistenceClient.getAnalyticsTable(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME, STREAM_VERSION);
        Assert.assertEquals(analyticsTable.getPersist(), true, "Table persistence state is not correct");
    }

    @Test(groups = "wso2.das.backupTool", description = "Check event stream persistence", dependsOnMethods = "testPersistenceOfTable")
    public void checkDataPersistence() throws Exception {
        deployEventReceivers();
        Thread.sleep(20000);
        publishEvents(1, "Test Event 1", RECORD_COUNT);
        RecordBean[] recods = webServiceClient.getByRange(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.', '_'), Long.MIN_VALUE + 1, Long.MAX_VALUE, 0, 1000);
        long count = recods.length;
        Assert.assertEquals(count, RECORD_COUNT, "Record count is invalid");
    }

    @Test(groups = "wso2.das.backupTool", description = "Check Record Store backing up", dependsOnMethods = "checkDataPersistence")
    public void testBackupRecordStore() throws Exception {
        backupRecordStore();
        String backedUpRecordStorePath = BACKUP_DIR_PATH + File.separator + BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.', '_');
        Assert.assertTrue(new File(backedUpRecordStorePath).exists(), "Backing up table has failed.");
    }

    @Test(groups = "wso2.das.backupTool", description = "Check Record Store restoring", dependsOnMethods = "testBackupFileSystem")
    public void testRestoreRecordStore() throws Exception {
        purgeData();
        restoreRecordStore();
        RecordBean[] recods = webServiceClient.getByRange(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.', '_'), Long.MIN_VALUE + 1, Long.MAX_VALUE, 0, 1000);
        long count = recods.length;
        Assert.assertEquals(count, RECORD_COUNT, "Restoring of records of " + BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME + " has failed.");
    }

    @Test(groups = "wso2.das.backupTool", description = "Test the filesystem backing up", dependsOnMethods = "checkDataPersistence")
    public void testBackupFileSystem() throws Exception {
        backupFileSystem();
        String backedUpFileSystemPath = BACKUP_DIR_PATH + File.separator + "_data";
        Assert.assertTrue(new File(backedUpFileSystemPath).exists(), "Backing up the filesystem has failed.");
    }

    @Test(groups = "wso2.das.backupTool", description = "Check Record Store restoring", dependsOnMethods = "testRestoreRecordStore")
    public void testRestoreFileSystem() throws Exception {
        restoreFileSystem();
    }

    private AnalyticsTable getAnalyticsTable() {
        AnalyticsTable table = new AnalyticsTable();
        table.setPersist(true);
        table.setTableName(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME);
        table.setStreamVersion(STREAM_VERSION);
        AnalyticsTableRecord[] records = new AnalyticsTableRecord[3];
        AnalyticsTableRecord uuid = new AnalyticsTableRecord();
        uuid.setPersist(true);
        uuid.setPrimaryKey(false);
        uuid.setIndexed(true);
        uuid.setColumnName("uuid");
        uuid.setColumnType("LONG");
        uuid.setScoreParam(false);
        records[0] = uuid;
        AnalyticsTableRecord name = new AnalyticsTableRecord();
        name.setPersist(true);
        name.setPrimaryKey(false);
        name.setIndexed(true);
        name.setColumnName("name");
        name.setColumnType("STRING");
        name.setScoreParam(false);
        records[1] = name;
        AnalyticsTableRecord age = new AnalyticsTableRecord();
        age.setPersist(true);
        age.setPrimaryKey(false);
        age.setIndexed(true);
        age.setColumnName("_age");
        age.setColumnType("INTEGER");
        age.setScoreParam(false);
        records[2] = age;
        table.setAnalyticsTableRecords(records);
        return table;
    }

    private StreamDefinitionBean getEventStreamBean() {
        StreamDefinitionBean definitionBean = new StreamDefinitionBean();
        definitionBean.setName(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME);
        definitionBean.setVersion(STREAM_VERSION);
        StreamDefAttributeBean[] attributeBeans = new StreamDefAttributeBean[2];
        StreamDefAttributeBean uuid = new StreamDefAttributeBean();
        uuid.setName("uuid");
        uuid.setType("LONG");
        attributeBeans[0] = uuid;
        StreamDefAttributeBean name = new StreamDefAttributeBean();
        name.setName("name");
        name.setType("STRING");
        attributeBeans[1] = name;
        definitionBean.setPayloadData(attributeBeans);
        return definitionBean;
    }

    private void deployEventReceivers() throws IOException {
        String streamResourceDir = FrameworkPathUtil.getSystemResourceLocation() + "tools" + File.separator;
        String streamsLocation = FrameworkPathUtil.getCarbonHome() + File.separator + "repository"
                + File.separator + "deployment" + File.separator + "server" + File.separator + "eventreceivers" + File.separator;
        FileManager.copyResourceToFileSystem(streamResourceDir + "backupToolTable.xml", streamsLocation, "backupToolTable.xml");
    }

    private void publishEvents(long id, String name, int record_count) throws Exception {
        Event event = null;
        dataPublisherClient = new DataPublisherClient();
        for (int i = 0; i < record_count; i++) {
            event = new Event(null, System.currentTimeMillis(), new Object[0], new Object[0], new Object[]{id, name});
            dataPublisherClient.publish(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME, STREAM_VERSION, event);
        }
        //fixme : is there anyway to reduce this?
        Thread.sleep(10000);
        dataPublisherClient.shutdown();
        analyticsDataAPI.waitForIndexing(MultitenantConstants.SUPER_TENANT_ID, BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.', '_').toUpperCase(), 10000L);
    }

    private void backupRecordStore() throws IOException, InterruptedException {
        String filePath = getExecutableScript();

        //todo: make super tenant ID get from the multitenant constants
        ProcessBuilder pb = new ProcessBuilder(filePath, "-backupRecordStore", "-tenantId", "-1234", "-tables", BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.', '_'),
                "-dir", BACKUP_DIR_PATH);
        Process p = pb.start();

        //wait for the backup to execute
        p.waitFor();
        p.destroy();
    }

    private void restoreRecordStore() throws IOException, InterruptedException {
        String filePath = getExecutableScript();

        ProcessBuilder pb = new ProcessBuilder(filePath, "-restoreRecordStore", "-tenantId", "-1234", "-disableStaging",
                "-dir", BACKUP_DIR_PATH + File.separator + BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.','_'));
        Process p = pb.start();

        //wait for the backup to execute
        p.waitFor();
        p.destroy();
    }

    private void backupFileSystem() throws IOException, InterruptedException {
        String filePath = getExecutableScript();

        ProcessBuilder pb = new ProcessBuilder(filePath, "-backupFileSystem", "-tenantId", "-1234", "-dir", BACKUP_DIR_PATH);
        Process p = pb.start();

        //wait for the backup to execute
        p.waitFor();
        p.destroy();
    }

    private void restoreFileSystem() throws IOException, InterruptedException {
        String filePath = getExecutableScript();

        ProcessBuilder pb = new ProcessBuilder(filePath, "-restoreFileSystem", "-tenantId", "-1234",
                "-dir", BACKUP_DIR_PATH);
        Process p = pb.start();

        //wait for the backup to execute
        p.waitFor();
        p.destroy();
    }
    private String getExecutableScript() {
        String filePath = getBackupScriptPath();
        //setting file permission to execute the script
        File backupToolScript = new File(filePath);
        if (!backupToolScript.canExecute())
            backupToolScript.setExecutable(true);
        return filePath;
    }

    private String getBackupScriptPath() {
        return FrameworkPathUtil.getCarbonHome() + File.separator + "bin" + File.separator + "analytics-backup.sh";
    }

    private void purgeData() throws Exception {
        messageConsoleClient.scheduleDataPurgingTask(BACKUP_TOOL_RECORDSTORE_TEST_STREAM_NAME.replace('.', '_'), "10 * * * * ?", -1);
        Thread.sleep(10000);
    }
}
