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

package org.wso2.migration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.migration.exception.DataMigrationException;
import org.wso2.migration.service.Migrator;

import java.util.List;

/**
 * Implementation of Data migration client
 **/
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
