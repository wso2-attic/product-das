/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.bam.ui.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.bam.integration.common.utils.BAMIntegrationUITest;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

public class AddEventStreamTestCase extends BAMIntegrationUITest {
    private WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
    }

    @Test(groups = "wso2.bam", description = "verify adding an event stream via management-console UI")
    public void testAddEventStream() throws Exception {
        driver.get(getLoginURL());
        driver.findElement(By.id("txtUserName")).clear();
        driver.findElement(By.id("txtUserName")).sendKeys(userInfo.getUserName());
        driver.findElement(By.id("txtPassword")).clear();
        driver.findElement(By.id("txtPassword")).sendKeys(userInfo.getPassword());
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.linkText("Event Streams")).click();
        driver.findElement(By.linkText("Add Event Stream")).click();
        driver.findElement(By.id("eventStreamNameId")).clear();
        driver.findElement(By.id("eventStreamNameId")).sendKeys("testStream");
        driver.findElement(By.id("eventStreamVersionId")).clear();
        driver.findElement(By.id("eventStreamVersionId")).sendKeys("1.0.0");
        driver.findElement(By.id("outputPayloadDataPropName")).clear();
        driver.findElement(By.id("outputPayloadDataPropName")).sendKeys("x");
        driver.findElement(By.xpath("(//input[@value='Add'])[3]")).click();
        driver.findElement(By.cssSelector("td.buttonRow > input[type=\"button\"]")).click();
        driver.findElement(By.cssSelector("button[type=\"button\"]")).click();
        driver.findElement(By.xpath("(//button[@type='button'])[2]")).click();
        driver.close();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if(driver != null){
            driver.quit();
        }
    }
}
