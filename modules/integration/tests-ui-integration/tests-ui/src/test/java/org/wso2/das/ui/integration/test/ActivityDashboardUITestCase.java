/*
 * Copyright (c)  2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.das.ui.integration.test;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.analytics.api.AnalyticsDataAPI;
import org.wso2.carbon.analytics.api.CarbonAnalyticsAPI;
import org.wso2.carbon.analytics.stream.persistence.stub.dto.AnalyticsTable;
import org.wso2.carbon.analytics.stream.persistence.stub.dto.AnalyticsTableRecord;
import org.wso2.carbon.analytics.webservice.stub.beans.StreamDefAttributeBean;
import org.wso2.carbon.analytics.webservice.stub.beans.StreamDefinitionBean;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.das.integration.common.clients.AnalyticsWebServiceClient;
import org.wso2.das.integration.common.clients.DataPublisherClient;
import org.wso2.das.integration.common.clients.EventReceiverClient;
import org.wso2.das.integration.common.clients.EventStreamPersistenceClient;
import org.wso2.das.integration.common.utils.DASIntegrationUITest;
import org.wso2.das.integration.common.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ActivityDashboardUITestCase extends DASIntegrationUITest {
    private static final String STREAM_NAME = "integration.ui.test.activity.stream";
    private static final String TABLE_NAME = "integration_ui_test_activity_stream";
    private static final String STREAM_VERSION = "1.0.0";
    AnalyticsDataAPI analyticsDataAPI;
    private EventStreamPersistenceClient persistenceClient;
    private DataPublisherClient dataPublisherClient;
    private AnalyticsWebServiceClient webServiceClient;
    private EventReceiverClient eventReceiverClient;
    private WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        String session = getSessionCookie();
        driver = BrowserManager.getWebDriver();
        // initialize clients
        this.persistenceClient = new EventStreamPersistenceClient(this.backendURL, session);
        this.webServiceClient = new AnalyticsWebServiceClient(this.backendURL, session);
        this.dataPublisherClient = new DataPublisherClient();
        this.eventReceiverClient = new EventReceiverClient(this.backendURL, session);
        String apiConf = new File(this.getClass().getClassLoader().getResource("dasconfig" + File.separator +
                "api" + File.separator + "analytics-data-config.xml").toURI()).getAbsolutePath();
        // remove existing persisted streams
        analyticsDataAPI = new CarbonAnalyticsAPI(apiConf);
        analyticsDataAPI.deleteTable(MultitenantConstants.SUPER_TENANT_ID, TABLE_NAME);
    }

    @Test(groups = "wso2.das", description = "Verifying XSS Vulnerability in Activity Dashboard")
    public void testXSSVulnerability() throws Exception {
        boolean isVulnerable = false;
        // Add stream with persistence
        StreamDefinitionBean streamDefinition = getStreamDefinition();
        AnalyticsTable analyticsTableDefinition = getAnalyticsTableDefinition();
        Utils.addStreamAndPersistence(
                this.webServiceClient, this.persistenceClient, streamDefinition, analyticsTableDefinition
        );

        // Add event receiver
        boolean status = this.eventReceiverClient.addOrUpdateEventReceiver("activity_receiver",
                getResourceContent(ActivityDashboardUITestCase.class,
                        "eventreceivers" + File.separator + "activity_receiver.xml"
                )
        );
        Thread.sleep(10000);

        // Send an Event (to generate an activity)
        Event event = new Event(
                null, System.currentTimeMillis(), new Object[0], new Object[]{"activity_1"},
                new Object[]{"<script>document.getElementById('workArea').id='vulnerable';</script>"}
        );
        this.dataPublisherClient.publish(STREAM_NAME, STREAM_VERSION, event);
        Utils.checkAndWaitForTableSize(webServiceClient, TABLE_NAME, 1);

        // Login to Management Console
        driver.get(getLoginURL());
        driver.findElement(By.id("txtUserName")).clear();
        driver.findElement(By.id("txtUserName")).sendKeys(dasServer.getContextTenant().getContextUser().getUserName());
        driver.findElement(By.id("txtPassword")).clear();
        driver.findElement(By.id("txtPassword")).sendKeys(dasServer.getContextTenant().getContextUser().getPassword());
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.id("menu-panel-button4")).click();

        // Goto Activity Dashboard page
        String pageUrl = backendURL.split("/services/")[0] + "/carbon/activitydashboard/index.jsp?";
        List<NameValuePair> pageParams = new ArrayList<>();
        pageParams.add(new BasicNameValuePair("region", "region1"));
        pageParams.add(new BasicNameValuePair("item", "activity_dashboard"));
        pageUrl += URLEncodedUtils.format(pageParams, "UTF-8");
        driver.get(pageUrl);
        WebDriverWait webDriverWait = new WebDriverWait(driver, 5);
        try {
            driver.findElement(By.cssSelector("#workArea > div.sectionSub > div.buttonRow > input:nth-child(1)")).click();
            Thread.sleep(1000 * 3);

            driver.findElement(By.cssSelector("#workArea > div.sectionSeperator > a")).click();
            Thread.sleep(1000 * 3);

            driver.findElement(By.cssSelector("#records_activity_1 > table > tbody > tr > td > i > a")).click();
            Thread.sleep(1000 * 5);

            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.id("vulnerable")));
            // If not vulnerable, element with #vulnerable would not get injected
            // Therefore it'll throw an exception mentioning that.
            isVulnerable = true;
        } catch (Exception ignored) {
        }
        Assert.assertFalse(isVulnerable);
        driver.close();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
    }

    private StreamDefinitionBean getStreamDefinition() {
        StreamDefinitionBean streamDefinition = new StreamDefinitionBean();
        streamDefinition.setName(STREAM_NAME);
        streamDefinition.setVersion(STREAM_VERSION);
        // Set Correlation Attributes
        StreamDefAttributeBean[] correlationAttributes = new StreamDefAttributeBean[1];
        StreamDefAttributeBean activityId = new StreamDefAttributeBean();
        activityId.setName("activity_id");
        activityId.setType("STRING");
        correlationAttributes[0] = activityId;
        streamDefinition.setCorrelationData(correlationAttributes);
        // Set PayLoad Attributes
        StreamDefAttributeBean[] payloadAttributes = new StreamDefAttributeBean[1];
        StreamDefAttributeBean id = new StreamDefAttributeBean();
        id.setName("data");
        id.setType("STRING");
        payloadAttributes[0] = id;
        streamDefinition.setPayloadData(payloadAttributes);
        return streamDefinition;
    }

    private AnalyticsTable getAnalyticsTableDefinition() {
        AnalyticsTable table = new AnalyticsTable();
        table.setPersist(true);
        table.setMergeSchema(false);
        table.setTableName(STREAM_NAME);
        table.setStreamVersion(STREAM_VERSION);
        AnalyticsTableRecord[] records = new AnalyticsTableRecord[2];
        // Persist Column "correlation_activity_id"
        AnalyticsTableRecord activity_id = new AnalyticsTableRecord();
        activity_id.setColumnName("correlation_activity_id");
        activity_id.setColumnType("STRING");
        activity_id.setPersist(true);
        activity_id.setIndexed(true);
        activity_id.setFacet(true);
        activity_id.setPrimaryKey(false);
        activity_id.setScoreParam(false);
        records[0] = activity_id;
        // Persist Column "id"
        AnalyticsTableRecord id = new AnalyticsTableRecord();
        id.setColumnName("data");
        id.setColumnType("STRING");
        id.setPersist(true);
        id.setIndexed(true);
        id.setFacet(false);
        id.setPrimaryKey(false);
        id.setScoreParam(false);
        records[1] = id;
        table.setAnalyticsTableRecords(records);
        return table;
    }

}
