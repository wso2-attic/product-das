package org.wso2.migration.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.common.jmx.agent.profiles.Profile;
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
import org.wso2.migration.utill.DataMigrationConstants;
import org.wso2.migration.utill.DataMigrationUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileDataMigration extends Migrator {
    private static final String PROFILE_SAVE_REG_LOCATION = "repository/components/org.wso2.carbon.publish.jmx.agent/";
    private static final Log LOG = LogFactory.getLog(ProfileDataMigration.class);

    private Registry registry;
    private RegistryService registryService = MigrationServiceDataHolder.getRegistryService();

    @Override
    public void migrate() throws DataMigrationException {
        migrateProfilePassword();
    }

    private void migrateProfilePassword() throws DataMigrationException {
        Tenant[] tenants;
        //for super tenant
        try {
            migrateProfilePasswordForTenant(DataMigrationConstants.SUPER_TENANT_ID);
        } catch (DataMigrationException e) {
            LOG.error("Error while migrating profiles. for tenant '".concat(
                    String.valueOf(DataMigrationConstants.SUPER_TENANT_ID)).concat("'. "), e);
        }
        try {
            tenants = MigrationServiceDataHolder.getRealmService().getTenantManager().getAllTenants();
        } catch (UserStoreException e) {
            LOG.error("Error while migrating profiles. Tenant retrieving failed. ", e);
            return;
        }
        for (Tenant tenant : tenants) {
            try {
                migrateProfilePasswordForTenant(tenant.getId());
            } catch (DataMigrationException e) {
                throw new DataMigrationException("Error while migrating profiles for tenant '".concat(
                        String.valueOf(tenant.getId())).concat("'. "), e);
            }
        }
    }

    private void migrateProfilePasswordForTenant(int tenantID) throws DataMigrationException {
        try {
            registry = registryService.getGovernanceSystemRegistry(tenantID);
            Collection profilesCollection = (Collection) registry.get(PROFILE_SAVE_REG_LOCATION);
            for (String profileName : profilesCollection.getChildren()) {
                Profile profile = getProfile(profileName);
                if (!DataMigrationUtil.isNewlyEncrypted(profile.getPass())) {
                    reEncryptProfileWithNewCipher(profile);
                }
            }
        } catch (RegistryException e) {
            LOG.error("error while obtaining the registry ", e);
        } catch (CryptoException e) {
            throw new DataMigrationException("error while encrypting the registry ", e);
        }
    }

    private void reEncryptProfileWithNewCipher(Profile profile) throws DataMigrationException, CryptoException {
        String reEncryptedValue = DataMigrationUtil.reEncryptByNewAlgorithm(profile.getPass());
        profile.setPass(reEncryptedValue);
        saveUpdatedProfile(profile);
    }


    private Profile getProfile(String profileName) throws DataMigrationException {
        ByteArrayInputStream byteArrayInputStream;
        try {
            //if the profile exists
            Resource res = registry.get(profileName);
            byteArrayInputStream = new ByteArrayInputStream((byte[]) res.getContent());
        } catch (RegistryException e) {
            LOG.error("Unable to get profile : " + profileName + ". ", e);
            throw new DataMigrationException("Unable to get profile : ".concat(profileName).concat(". "), e);
        }

        Profile profile;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Profile.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            profile = (Profile) jaxbUnmarshaller.unmarshal(byteArrayInputStream);
        } catch (JAXBException e) {
            LOG.error("JAXB unmarshalling exception :" + profileName + ". ", e);
            throw new DataMigrationException("JAXB unmarshalling exception has occurred while retrieving '".
                    concat(profileName).concat("' profile from registry"), e);
        }
        return profile;

    }

    private void saveUpdatedProfile(Profile profile) throws DataMigrationException {
        String path = PROFILE_SAVE_REG_LOCATION + profile.getName();

        JAXBContext jaxbContext;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            jaxbContext = JAXBContext.newInstance(Profile.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.marshal(profile, byteArrayOutputStream);
        } catch (JAXBException e) {
            throw new DataMigrationException("JAXB unmarshalling exception has occurred while saving '".
                    concat(profile.getName()).concat("'."), e);
        }

        //replace the profile if it exists
        try {
            Resource res = registry.newResource();
            res.setContent(byteArrayOutputStream.toString());
            //delete the existing profile
            registry.delete(path);
            //save the new profile
            registry.put(path, res);
        } catch (RegistryException e) {
            throw new DataMigrationException("Error has occurred while trying to save '".concat(profile.getName())
                    .concat("' profile on registry. "), e);
        }

        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            // Just log the exception. Do nothing.
            LOG.warn("Unable to close byte stream ...", e);

        }
    }

}
