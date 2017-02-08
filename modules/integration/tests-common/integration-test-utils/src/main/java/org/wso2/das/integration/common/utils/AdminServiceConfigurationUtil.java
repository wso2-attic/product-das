/*
 * Copyright (c)  2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.das.integration.common.utils;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.wso2.das.integration.common.clients.EventReceiverClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AdminServiceConfigurationUtil {

    private static AdminServiceConfigurationUtil configurationUtil;
    private EventReceiverClient eventReceiverAdminServiceClient;

    private AdminServiceConfigurationUtil() {
    }

    public static AdminServiceConfigurationUtil getConfigurationUtil() {
        if (configurationUtil == null) {
            configurationUtil = new AdminServiceConfigurationUtil();
        }
        return configurationUtil;
    }

    public EventReceiverClient getEventReceiverAdminServiceClient(
            String backendURL,
            String loggedInSessionCookie) throws AxisFault {
        initEventReceiverAdminServiceClient(backendURL, loggedInSessionCookie);
        return eventReceiverAdminServiceClient;
    }

    private void initEventReceiverAdminServiceClient(
            String backendURL,
            String loggedInSessionCookie)
            throws AxisFault {
        eventReceiverAdminServiceClient = new EventReceiverClient(backendURL, loggedInSessionCookie);
        ServiceClient client = eventReceiverAdminServiceClient._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, loggedInSessionCookie);
    }
}
