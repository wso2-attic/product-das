package org.wso2.migration.utill;

import org.wso2.carbon.core.internal.CarbonCoreDataHolder;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.migration.exception.DataMigrationException;

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
