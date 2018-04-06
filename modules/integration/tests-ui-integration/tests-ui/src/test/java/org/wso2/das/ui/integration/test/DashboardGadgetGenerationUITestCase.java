/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.das.integration.common.utils.DASIntegrationUITest;

public class DashboardGadgetGenerationUITestCase extends DASIntegrationUITest {
    private WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        String session = getSessionCookie();
        driver = BrowserManager.getWebDriver();
    }

    @Test(groups = "wso2.das", description = "Verifying The first step of the gadget generation wizard is working")
    public void testGadgetGenWizardFirstStep() throws Exception {
        boolean nextStepAvailable = false;
        driver.get("https://localhost:10143/portal");
        driver.findElement(By.name("username")).clear();
        driver.findElement(By.name("username")).sendKeys(dasServer.getContextTenant().getContextUser().getUserName());
        driver.findElement(By.name("password")).clear();
        driver.findElement(By.name("password")).sendKeys(dasServer.getContextTenant().getContextUser().getPassword());
        driver.findElement(By.className("ues-signin")).click();
        driver.findElement(By.xpath("//*[@data-target='#navbar2']")).click();
        driver.findElement(By.xpath("//*[@href='../gadget/']")).click();
        driver.findElement(By.xpath("//*[@href='../create-gadget']")).click();
        Select dropdown = new Select(driver.findElement(By.id("providers")));
        dropdown.selectByValue("rest");
        driver.findElement(By.xpath("//*[@class='wiz-control btn-next']")).click();
        if (driver.findElements( By.name("authorizationMethod")).size() != 0) {
            nextStepAvailable = true;
        }
        Assert.assertTrue(nextStepAvailable);
        driver.close();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
    }
}
