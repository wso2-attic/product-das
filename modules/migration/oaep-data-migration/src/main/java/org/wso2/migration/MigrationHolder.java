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

import org.wso2.migration.service.InputOutputDataMigration;
import org.wso2.migration.service.KeyStoreAndTrustStoreMigration;
import org.wso2.migration.service.Migrator;
import org.wso2.migration.service.ProfileDataMigration;
import org.wso2.migration.service.UserStorePasswordMigration;

import java.util.ArrayList;
import java.util.List;

/**
 * Migration data holder class
 * **/
public class MigrationHolder {
    private static MigrationHolder migrationHolder = new MigrationHolder();
    private List<Migrator> migrators = new ArrayList<>();

    private MigrationHolder() {
        migrators.add(new InputOutputDataMigration());
        migrators.add(new ProfileDataMigration());
        migrators.add(new KeyStoreAndTrustStoreMigration());
        migrators.add(new UserStorePasswordMigration());
    }

    public static MigrationHolder getInstance() {
        return MigrationHolder.migrationHolder;
    }

    public List<Migrator> getMigrators() {
        return migrators;
    }

}
