/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.migration.service;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.migration.exception.DataMigrationException;
import org.wso2.migration.internal.MigrationServiceDataHolder;
import org.wso2.migration.util.DataMigrationConstants;
import org.wso2.migration.util.DataMigrationUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Migrate user store passwords
 **/
public class UserStorePasswordMigration extends Migrator {

    private static final Log LOG = LogFactory.getLog(UserStorePasswordMigration.class);

    @Override
    public void migrate() throws DataMigrationException {
        LOG.info("Migration starting on Secondary User Stores");
        updateSuperTenantConfigs();
        updateTenantConfigs();
    }

    private void updateTenantConfigs() {
        Tenant[] tenants;
        try {
            tenants = MigrationServiceDataHolder.getRealmService().getTenantManager().getAllTenants();
            for (Tenant tenant : tenants) {
                File[] userstoreConfigs = getUserStoreConfigFiles(tenant.getId());
                for (File file : userstoreConfigs) {
                    if (file.isFile()) {
                        updatePassword(file.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error while updating secondary user store password for tenant", e);
        }
    }

    private void updateSuperTenantConfigs() {
        try {
            File[] userstoreConfigs = getUserStoreConfigFiles(DataMigrationConstants.SUPER_TENANT_ID);
            for (File file : userstoreConfigs) {
                if (file.isFile()) {
                    updatePassword(file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            LOG.error("Error while updating secondary user store password for super tenant", e);
        }
    }

    private File[] getUserStoreConfigFiles(int tenantId) throws FileNotFoundException {

        String carbonHome = System.getProperty(DataMigrationConstants.CARBON_HOME);
        String userStorePath;
        if (tenantId == DataMigrationConstants.SUPER_TENANT_ID) {
            userStorePath = Paths.get(carbonHome,
                    new String[]{"repository", "deployment", "server", "userstores"}).toString();
        } else {
            userStorePath = Paths.get(carbonHome,
                    new String[]{"repository", "tenants", String.valueOf(tenantId), "userstores"}).toString();
        }
        File[] files = new File(userStorePath).listFiles();
        return files != null ? files : new File[0];
    }

    private void updatePassword(String filePath) throws FileNotFoundException, CryptoException, DataMigrationException {

        XMLStreamReader parser = null;
        FileInputStream stream = null;
        try {
            LOG.info("Migrating password in: " + filePath);
            stream = new FileInputStream(filePath);
            parser = XMLInputFactory.newInstance().createXMLStreamReader(stream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement documentElement = builder.getDocumentElement();
            Iterator it = documentElement.getChildElements();
            String newEncryptedPassword = null;
            while (it.hasNext()) {
                OMElement element = (OMElement) it.next();
                if ("password".equals(element.getAttributeValue(new QName("name"))) ||
                        "ConnectionPassword".equals(element.getAttributeValue(new QName("name")))) {
                    String oldValue = element.getText();
                    if (oldValue != null && !DataMigrationUtil.isNewlyEncrypted(oldValue)) {
                        newEncryptedPassword = DataMigrationUtil.reEncryptByNewAlgorithm(oldValue);
                        element.setText(newEncryptedPassword);
                    }
                }
            }

            if (newEncryptedPassword != null) {
                OutputStream outputStream = new FileOutputStream(filePath);
                documentElement.serialize(outputStream);
            }
        } catch (XMLStreamException ex) {
            LOG.error("Error while updating password for: " + filePath);
        } finally {
            try {
                if (parser != null) {
                    parser.close();
                }
                if (stream != null) {
                    stream.close();
                }
            } catch (XMLStreamException e) {
                LOG.error("Error while closing XML stream", e);
            } catch (IOException e) {
                LOG.error("Error while closing input stream", e);
            }

        }
    }
}
