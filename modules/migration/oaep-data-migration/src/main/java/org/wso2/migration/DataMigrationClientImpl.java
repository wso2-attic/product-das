package org.wso2.migration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.migration.exception.DataMigrationException;
import org.wso2.migration.service.Migrator;

public class DataMigrationClientImpl implements DataMigrationClient {
    private static final Log LOG = LogFactory.getLog(DataMigrationClient.class);

    @Override
    public void execute() throws DataMigrationException {
        try {
            MigrationHolder migrationHolder = MigrationHolder.getInstance();
            Migrator migrator = migrationHolder.getMigrator();

            migrator.migrate();
            LOG.info("Migration was successful.");
        } catch (DataMigrationException e) {
            LOG.error("Error occurred while migrating. Migration stopped. ", e);
        }
    }
}
