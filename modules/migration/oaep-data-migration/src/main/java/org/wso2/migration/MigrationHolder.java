package org.wso2.migration;

import org.wso2.migration.service.InputOutputDataMigration;
import org.wso2.migration.service.Migrator;

public class MigrationHolder {
    private static MigrationHolder migrationHolder = new MigrationHolder();
    private Migrator migrator;

    private MigrationHolder(){
        migrator = new InputOutputDataMigration();
    }
    public static MigrationHolder getInstance() {
        return MigrationHolder.migrationHolder;
    }
    public Migrator getMigrator(){
        return migrator;
    }
}
