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

package org.wso2.migration.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.core.internal.CarbonCoreDataHolder;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.migration.DataMigrationClient;
import org.wso2.migration.DataMigrationClientImpl;

/**
 * @scr.component name="org.wso2.carbon.migration.internal" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="serverconfiguration.service"
 * interface="org.wso2.carbon.base.api.ServerConfigurationService" cardinality="1..1"
 * policy="dynamic" bind="setServerConfigurationService" unbind="unsetServerConfigurationService"
 */
public class MigrationServiceComponent {
    private static final Log LOG = LogFactory.getLog(MigrationServiceComponent.class);

    /**
     * Method to activate bundle.
     *
     * @param context OSGi component context.
     */
    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(DataMigrationClient.class, new DataMigrationClientImpl(), null);

            // if -Dmigrate option is used.
            String migrate = System.getProperty("migrate");
            if (Boolean.parseBoolean(migrate)) {
                LOG.info("Executing Migration client : " + DataMigrationClient.class.getName());
                DataMigrationClient migrationClientImpl = new DataMigrationClientImpl();
                migrationClientImpl.execute();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("WSO2 DAS migration bundle is activated");
            }
        } catch (Exception e) {
            LOG.error("Error while initiating Config component", e);
        }

    }

    /**
     * Method to deactivate bundle.
     *
     * @param context OSGi component context.
     */
    protected void deactivate(ComponentContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("WSO2 DAS migration bundle is deactivated");
        }
    }

    /**
     * Method to set realm service.
     *
     * @param realmService service to get tenant data.
     */
    protected void setRealmService(RealmService realmService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting RealmService to WSO2 DAS Config component");
        }
        MigrationServiceDataHolder.setRealmService(realmService);
    }

    /**
     * Method to unset realm service.
     *
     * @param realmService service to get tenant data.
     */
    protected void unsetRealmService(RealmService realmService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsetting RealmService from WSO2 DAS Config component");
        }
        MigrationServiceDataHolder.setRealmService(null);
    }

    /**
     * Method to set registry service.
     *
     * @param registryService service to get tenant data.
     */
    protected void setRegistryService(RegistryService registryService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting RegistryService to WSO2 DAS Config component");
        }
        MigrationServiceDataHolder.setRegistryService(registryService);
        CarbonCoreDataHolder.getInstance().setRegistryService(registryService);
    }

    /**
     * Method to unset registry service.
     *
     * @param registryService service to get tenant data.
     */
    protected void unsetRegistryService(RegistryService registryService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsetting RegistryService from WSO2 DAS Config component");
        }
        MigrationServiceDataHolder.setRegistryService(null);
    }

    /**
     * Method to set server configuration service.
     *
     * @param serverConfigurationService service to get tenant data.
     */
    protected void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting ServerConfigurationService to WSO2 DAS Config component");
        }
        MigrationServiceDataHolder.setServerConfigurationService(serverConfigurationService);
        CarbonCoreDataHolder.getInstance().setServerConfigurationService(serverConfigurationService);
    }

    /**
     * Method to unset server configuration service.
     *
     * @param serverConfigurationService service to get tenant data.
     */
    protected void unsetServerConfigurationService(ServerConfigurationService serverConfigurationService) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsetting ServerConfigurationService from WSO2 DAS Config component");
        }
        MigrationServiceDataHolder.setServerConfigurationService(null);
    }
}


