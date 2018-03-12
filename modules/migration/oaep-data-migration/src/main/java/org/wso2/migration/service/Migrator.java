package org.wso2.migration.service;

import org.wso2.migration.exception.DataMigrationException;

public abstract class Migrator {
    /**
     * Migrator specific implementation.
     *
     */
    public abstract void migrate() throws DataMigrationException;
}
