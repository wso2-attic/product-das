package org.wso2.migration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.migration.exception.DataMigrationException;
import org.wso2.migration.service.Migrator;

import java.util.List;

public class DataMigrationClientImpl implements DataMigrationClient {
    private static final Log LOG = LogFactory.getLog(DataMigrationClient.class);

    @Override
    public void execute() throws DataMigrationException {
        try {
            MigrationHolder migrationHolder = MigrationHolder.getInstance();
            List<Migrator> migrators = migrationHolder.getMigrators();
            for (Migrator migrator : migrators) {
                migrator.migrate();
            }
            LOG.info("Migration was successful.");
        } catch (Exception e) {
            throw new DataMigrationException("Error occurred while migrating. Migration stopped. ", e);
        }
    }
}
