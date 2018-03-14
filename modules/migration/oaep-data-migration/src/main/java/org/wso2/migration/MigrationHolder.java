package org.wso2.migration;

import org.wso2.migration.service.InputOutputDataMigration;
import org.wso2.migration.service.Migrator;
import org.wso2.migration.service.ProfileDataMigration;

import java.util.ArrayList;
import java.util.List;

public class MigrationHolder {
    private static MigrationHolder migrationHolder = new MigrationHolder();
    private List<Migrator> migrators = new ArrayList<>();

    private MigrationHolder() {
        migrators.add(new InputOutputDataMigration());
        migrators.add(new ProfileDataMigration());
    }

    public static MigrationHolder getInstance() {
        return MigrationHolder.migrationHolder;
    }

    public List<Migrator> getMigrators() {
        return migrators;
    }

}
