package org.wso2.migration.exception;

public class DataMigrationException extends Exception {
    public DataMigrationException(String msg) {
        super(msg);
    }

    public DataMigrationException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
