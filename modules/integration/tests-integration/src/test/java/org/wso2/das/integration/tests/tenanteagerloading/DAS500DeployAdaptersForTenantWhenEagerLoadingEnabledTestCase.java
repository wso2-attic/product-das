package org.wso2.das.integration.tests.tenanteagerloading;
/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.das.integration.common.clients.EventReceiverClient;
import org.wso2.das.integration.common.utils.AdminServiceConfigurationUtil;
import org.wso2.das.integration.common.utils.DASIntegrationTest;

import java.io.File;
import java.io.IOException;

public class DAS500DeployAdaptersForTenantWhenEagerLoadingEnabledTestCase extends DASIntegrationTest {
    private static final Log log = LogFactory.getLog(DAS500DeployAdaptersForTenantWhenEagerLoadingEnabledTestCase.class);
    protected final String cAppFileName = "test_cApp_1.0.0.car";
    private EventReceiverClient eventReceiverAdminServiceClient;
    private AdminServiceConfigurationUtil configurationUtil;
    private ServerConfigurationManager serverManager;
    private String loggedInSessionCookie = null;


    @BeforeClass(alwaysRun = true)
    protected void init() throws Exception {
        super.init();
        serverManager = new ServerConfigurationManager(dasServer);

        //add CAPP for tenant
        copyCarFile();

        //add eager loading config file and restart gracefully
        String carbonXMLLocation = FrameworkPathUtil.getSystemResourceLocation() + "tenanteagerloading" + File.separator + "carbon-01.xml";
        applyCarbonXMLConfigChange(carbonXMLLocation);
    }

    @Test(groups = "wso2.das", description = "Testing event receiver deployment when eager loading enabled for tenant")
    public void testEventReceiverDeploymentForTenant() throws Exception {
        configurationUtil = AdminServiceConfigurationUtil.getConfigurationUtil();
        loggedInSessionCookie = getSessionCookieForTenant("admin@wso2.com", "admin", "localhost");
        eventReceiverAdminServiceClient = configurationUtil.getEventReceiverAdminServiceClient(backendURL,
                loggedInSessionCookie);

        int eventReceiverCount = eventReceiverAdminServiceClient.getActiveEventReceiverCount();
        Assert.assertEquals(eventReceiverCount, 1);
    }

    private void applyCarbonXMLConfigChange(String carbonXMLLocation) throws Exception {
        File sourceFile = new File(carbonXMLLocation);
        File targetFile = new File(FrameworkPathUtil.getCarbonServerConfLocation() +
                File.separator + "carbon.xml");
        serverManager.applyConfigurationWithoutRestart(sourceFile, targetFile, true);
        log.info("carbon.xml replaced with :" + carbonXMLLocation);
        serverManager.restartGracefully();
        log.info("Server Restarted after applying carbon.xml and tenant information utility web application");
    }

    private void copyCarFile() throws IOException {
        String carFile = FrameworkPathUtil.getSystemResourceLocation() +
                "tenanteagerloading" + File.separator + cAppFileName;
        String carbonAppsDir = FrameworkPathUtil.getCarbonHome() + File.separator + "repository"
                + File.separator + "tenants" + File.separator + "1" + File.separator + "carbonapps" + File.separator;
        FileManager.copyResourceToFileSystem(carFile, carbonAppsDir, cAppFileName);
    }
}