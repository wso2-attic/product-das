/*
 *  Copyright (c), WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.bam.integration.tests.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.bam.integration.common.utils.BAMIntegrationTest;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class BAM1942LogSummarizerTestCase extends BAMIntegrationTest {
    protected ServerConfigurationManager serverManager;
    private LogViewerClient logViewerClient;
    private static final Log log = LogFactory.getLog(BAM1942LogSummarizerTestCase.class);
    private String carbonHome;
    private static final String ERROR_LINE = "Error while scheduling script : logSummarizer for tenant";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        String zipFileLocation = System.getProperty("carbon.zip");

        System.getProperty("base.dir");
        carbonHome = System.getProperty("carbon.home");
        init();

        try {
            serverManager = new ServerConfigurationManager(bamServer);
        } catch (MalformedURLException e) {
            throw new RemoteException("Malformed URL exception thrown when initializing BAM server", e);
        }

        String repositoryDir = System.getProperty("carbon.home") + File.separator + "repository" + File.separator;

        File log4jPropFile = new File(repositoryDir + "conf" + File.separator + "log4j.properties");
        File hiveSiteFile = new File(repositoryDir + "conf" + File.separator + "advanced" + File.separator + "hive-site.xml");
        File loggingConfigFile = new File(repositoryDir + "conf" + File.separator + "etc" + File.separator + "logging-config.xml");
        File summarizerConfigFile = new File(repositoryDir + "conf" + File.separator + "etc" + File.separator + "logging-summarizer-config.xml");

        serverManager.applyConfigurationWithoutRestart(new File(getSystemResourceLocation() + File.separator + "logging-summarizer" + File.separator + "hive-site.xml"), hiveSiteFile, true);
        serverManager.applyConfigurationWithoutRestart(new File(getSystemResourceLocation() + File.separator + "logging-summarizer" + File.separator + "log4j.properties"), log4jPropFile, true);
        serverManager.applyConfigurationWithoutRestart(new File(getSystemResourceLocation() + File.separator + "logging-summarizer" + File.separator + "logging-config.xml"), loggingConfigFile, true);
        serverManager.applyConfigurationWithoutRestart(new File(getSystemResourceLocation() + File.separator + "logging-summarizer" + File.separator + "logging-summarizer-config.xml"), summarizerConfigFile, true);
        serverManager.copyToComponentDropins(new File(getSystemResourceLocation() + File.separator + "logging-summarizer" + File.separator + "org.wso2.carbon.logging.logging-summarizer-4.2.0.jar"));
    }

    @Test(groups = "wso2.bam")
    public void testAnalysePatchLogs() throws Exception {
        serverManager.restartGracefully();
        String serverLog = readPatchLogs();
        Assert.assertTrue(!serverLog.contains(ERROR_LINE),
                "Error occurred while starting server with Log Summarizer");
    }

    private String readPatchLogs() throws Exception {
        File patchFile = new File(carbonHome + File.separator + "repository" + File.separator +
                "logs" + File.separator + "wso2carbon.log");
        return new Scanner(patchFile).useDelimiter("\\A").next();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverManager = null;
    }

    private static String getSystemResourceLocation() {
        String resourceLocation;
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            resourceLocation = System.getProperty("framework.resource.location").replace("/", "\\");
        } else {
            resourceLocation = System.getProperty("framework.resource.location").replace("/", "/");
        }
        return resourceLocation;
    }

}
