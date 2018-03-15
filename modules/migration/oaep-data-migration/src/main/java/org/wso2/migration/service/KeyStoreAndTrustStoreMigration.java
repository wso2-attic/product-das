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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.migration.exception.DataMigrationException;
import org.wso2.migration.internal.MigrationServiceDataHolder;
import org.wso2.migration.util.DataMigrationUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;

/**
 * Migrate encrypted data in key stores and trust stores.
 **/
public class KeyStoreAndTrustStoreMigration extends Migrator {
    private static final String KEYSTORE_RESOURCE_PATH = "/repository/security/key-stores/";
    private static final Log LOG = LogFactory.getLog(KeyStoreAndTrustStoreMigration.class);
    private static final String PASSWORD = "password";
    private static final String PRIVATE_KEY_PASS = "privatekeyPass";


    private RegistryService registryService = MigrationServiceDataHolder.getRegistryService();

    @Override
    public void migrate() throws DataMigrationException {
        try {
            migrateKeystorePassword();
        } catch (UserStoreException | CryptoException | RegistryException e) {
            throw new DataMigrationException("Error has occurred while migrating Key Stores and Trust Stores");
        }
    }

    private void startTenantFlow(Tenant tenant) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantId(tenant.getId());
        carbonContext.setTenantDomain(tenant.getDomain());
    }

    private void migrateKeystorePassword() throws DataMigrationException, RegistryException, CryptoException,
            UserStoreException {
        try {
            migrateKeyStorePasswordForTenant(SUPER_TENANT_ID);
            LOG.info("Keystore passwords migrated for tenant : " + SUPER_TENANT_DOMAIN_NAME);
        } catch (Exception e) {
            LOG.error("Error while migrating Keystore passwords for tenant : " + SUPER_TENANT_DOMAIN_NAME);
            throw e;
        }

        //migrating tenant configurations
        Tenant[] tenants = MigrationServiceDataHolder.getRealmService().getTenantManager().getAllTenants();
        for (Tenant tenant : tenants) {
            try {
                startTenantFlow(tenant);
                migrateKeyStorePasswordForTenant(tenant.getId());
                LOG.info("Keystore passwords migrated for tenant : " + tenant.getDomain());
            } catch (Exception e) {
                LOG.error("Error while migrating keystore passwords for tenant : " + tenant.getDomain());
                throw e;
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    private void migrateKeyStorePasswordForTenant(int tenantId) throws RegistryException, CryptoException,
            DataMigrationException {

        Registry registry = registryService.getGovernanceSystemRegistry(tenantId);
        if (registry.resourceExists(KEYSTORE_RESOURCE_PATH)) {
            Collection keyStoreCollection = (Collection) registry.get(KEYSTORE_RESOURCE_PATH);
            for (String keyStorePath : keyStoreCollection.getChildren()) {
                updateRegistryProperties(registry, keyStorePath,
                        new ArrayList<>(Arrays.asList(PASSWORD, PRIVATE_KEY_PASS)));
            }
        }
    }

    private void updateRegistryProperties(Registry registry, String resource, List<String> properties)
            throws RegistryException, CryptoException, DataMigrationException {
        String newValue;

        if (registry == null || StringUtils.isEmpty(resource) || CollectionUtils.isEmpty(properties)) {
            return;
        }

        if (registry.resourceExists(resource)) {
            try {
                registry.beginTransaction();
                Resource resourceObj = registry.get(resource);
                for (String encryptedPropertyName : properties) {
                    String oldValue = resourceObj.getProperty(encryptedPropertyName);
                    if (oldValue != null && !DataMigrationUtil.isNewlyEncrypted(oldValue)) {
                        newValue = DataMigrationUtil.reEncryptByNewAlgorithm(oldValue);
                        resourceObj.setProperty(encryptedPropertyName, newValue);
                    }
                }
                registry.put(resource, resourceObj);
                registry.commitTransaction();
            } catch (RegistryException e) {
                registry.rollbackTransaction();
                LOG.error("Unable to update the registry resource", e);
                throw e;
            } catch (DataMigrationException e) {
                throw new DataMigrationException("Error while migrating Key Store and Trust Store.", e);
            }
        }
    }
}
