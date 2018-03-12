package org.wso2.migration.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.wso2.carbon.core.internal.CarbonCoreDataHolder;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.migration.exception.DataMigrationException;
import org.wso2.migration.utill.DataMigrationConstants;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.Objects;

public class InputOutputDataMigration extends Migrator {
    private static final Log LOG = LogFactory.getLog(InputOutputDataMigration.class);

    private static InputOutputDataMigration instance = new InputOutputDataMigration();

    public static InputOutputDataMigration getInstance() {
        return instance;
    }


    @Override
    public void migrate() throws DataMigrationException {
        String carbonPath = System.getProperty(DataMigrationConstants.CARBON_HOME);
        try {
            migratePublishers(carbonPath);
            migrateReceivers(carbonPath);

        } catch (Exception e) {
            throw new DataMigrationException("Error while migrating data : ", e);
        }
    }

    private static File readFiles(String path) {
        return new File(path);
    }

    private static void migratePublishers(String carbonHome) throws DataMigrationException {
        File publisherPath = readFiles(carbonHome + DataMigrationConstants.EVENT_PUBLISHER_PATH);
        try {
            migrateData(publisherPath);
            LOG.info("Migrating publishers was successful");
        } catch (DataMigrationException e) {
            throw new DataMigrationException("Error while migrating publishers", e);
        }
    }

    private static void migrateReceivers(String carbonHome) throws DataMigrationException {
        File recieverPath = readFiles(carbonHome + DataMigrationConstants.EVENT_RECIEVER_PATH);
        try {
            migrateData(recieverPath);
            LOG.info("Migrating receivers was successful");
        } catch (DataMigrationException e) {
            throw new DataMigrationException("Error while migrating receivers : ", e);
        }
    }

    private static void migrateData(File folder) throws DataMigrationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder builder;
        Document doc;
        try {
            for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
                builder = documentBuilderFactory.newDocumentBuilder();
                doc = builder.parse(fileEntry);
                XPathFactory xpathFactory = XPathFactory.newInstance();
                XPath xpath = xpathFactory.newXPath();
                NodeList data = getEncryptedPayload(doc, xpath);
                if (data.getLength() > 0) {
                    for (int i = 0; i < data.getLength(); i++) {
                        if (isOldDecryptedValue(data.item(i).getNodeValue())) {
                            byte[] decryptedPassword = CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(
                                    data.item(i).getNodeValue(), "RSA");
                            String newEncryptedPassword = CryptoUtil.getDefaultCryptoUtil()
                                    .encryptAndBase64Encode(decryptedPassword);
                            data.item(i).setNodeValue(newEncryptedPassword);
                        }
                    }

                    Transformer xformer = TransformerFactory.newInstance().newTransformer();
                    xformer.transform(new DOMSource(doc), new StreamResult(new File(
                            fileEntry.getAbsolutePath()).getPath()));
                }
            }

        } catch (Exception e) {
            throw new DataMigrationException("Error occurred while migrating data in folder : " +
                    folder.getAbsolutePath() + " . ", e);
        }

    }

    private static boolean isOldDecryptedValue(String encryptedValue) throws DataMigrationException, CryptoException {
        CryptoUtil cryptoUtil = null;
        try {
            cryptoUtil = CryptoUtil.getDefaultCryptoUtil(CarbonCoreDataHolder.getInstance().
                            getServerConfigurationService(),
                    CarbonCoreDataHolder.getInstance().getRegistryService());
        } catch (Exception e) {
            throw new DataMigrationException("Error while initializing cryptoUtil", e);
        }
        return cryptoUtil.base64DecodeAndIsSelfContainedCipherText(encryptedValue);
    }

    private static NodeList getEncryptedPayload(Document doc, XPath xpath) throws Exception {

        try {
            XPathExpression expr = xpath.compile(
                    "//*[local-name()='property'][@*[local-name()='encrypted'='true']]/text()");
            return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DataMigrationException("Error has occurred while retriving the payload from file : " +
                    doc.getDocumentURI(), e);
        }

    }
}
