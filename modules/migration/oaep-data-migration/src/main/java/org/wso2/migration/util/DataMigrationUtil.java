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

package org.wso2.migration.util;

import org.wso2.carbon.core.internal.CarbonCoreDataHolder;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.migration.exception.DataMigrationException;

/**
 * Migration util methods
 **/
public class DataMigrationUtil {

    private DataMigrationUtil() {

    }

    public static boolean isNewlyEncrypted(String encryptedValue) throws DataMigrationException, CryptoException {
        CryptoUtil cryptoUtil;
        try {
            cryptoUtil = CryptoUtil.getDefaultCryptoUtil(CarbonCoreDataHolder.getInstance().
                            getServerConfigurationService(),
                    CarbonCoreDataHolder.getInstance().getRegistryService());
        } catch (Exception e) {
            throw new DataMigrationException("Error while initializing cryptoUtil", e);
        }
        return cryptoUtil.base64DecodeAndIsSelfContainedCipherText(encryptedValue);
    }

    public static String reEncryptByNewAlgorithm(String value) throws CryptoException {
        byte[] decryptedValue = CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(
                value, "RSA");
        return CryptoUtil.getDefaultCryptoUtil()
                .encryptAndBase64Encode(decryptedValue);
    }
}
