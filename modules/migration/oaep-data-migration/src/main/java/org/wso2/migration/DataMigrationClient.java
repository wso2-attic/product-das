package org.wso2.migration;

import org.wso2.migration.exception.DataMigrationException;

public interface DataMigrationClient {
    void execute() throws DataMigrationException;
}

