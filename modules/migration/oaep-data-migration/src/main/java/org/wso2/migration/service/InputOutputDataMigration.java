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

package org.wso2.migration.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.wso2.migration.exception.DataMigrationException;
import org.wso2.migration.util.DataMigrationConstants;
import org.wso2.migration.util.DataMigrationUtil;

import java.io.File;
import java.util.Objects;
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


/**
 * Migrate encrypted data in event publishers and event receivers
 **/
public class InputOutputDataMigration extends Migrator {
    private static final Log log = LogFactory.getLog(InputOutputDataMigration.class);
    private static InputOutputDataMigration instance = new InputOutputDataMigration();
    public static InputOutputDataMigration getInstance() {
        return instance;
    }

    @Override
    public void migrate() throws DataMigrationException {
        String carbonPath = System.getProperty(DataMigrationConstants.CARBON_HOME);
        migratePublishers(carbonPath);
        migrateReceivers(carbonPath);
    }

    private static File readFiles(String path) {
        return new File(path);
    }

    private static void migratePublishers(String carbonHome) throws DataMigrationException {
        File publisherPath = readFiles(carbonHome + DataMigrationConstants.EVENT_PUBLISHER_PATH);
        try {
            migrateData(publisherPath);
            log.info("Migrating publishers was successful");
        } catch (DataMigrationException e) {
            throw new DataMigrationException("Error while migrating publishers in path : ".
                    concat(publisherPath.getName()), e);
        }
    }

    private static void migrateReceivers(String carbonHome) throws DataMigrationException {
        File recieverPath = readFiles(carbonHome + DataMigrationConstants.EVENT_RECIEVER_PATH);
        try {
            migrateData(recieverPath);
            log.info("Migrating receivers was successful");
        } catch (DataMigrationException e) {
            throw new DataMigrationException("Error while migrating receivers in path : "
                    .concat(recieverPath.getName()), e);
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
                if (!fileEntry.getName().endsWith(".xml")) {
                    log.error("File type is not supported. file : '".
                            concat(fileEntry.getName()).concat("'. Hence ignored"));
                }
                doc = builder.parse(fileEntry);
                XPathFactory xpathFactory = XPathFactory.newInstance();
                XPath xpath = xpathFactory.newXPath();
                NodeList data = getEncryptedPayload(doc, xpath);
                if (data.getLength() > 0) {
                    for (int i = 0; i < data.getLength(); i++) {
                        if (!DataMigrationUtil.isNewlyEncrypted(data.item(i).getNodeValue())) {
                            String reEncryptedValue = DataMigrationUtil.reEncryptByNewAlgorithm(data.item(i).
                                    getNodeValue());
                            data.item(i).setNodeValue(reEncryptedValue);
                        }
                    }

                    Transformer xformer = TransformerFactory.newInstance().newTransformer();
                    xformer.transform(new DOMSource(doc), new StreamResult(new File(
                            fileEntry.getAbsolutePath()).getPath()));
                }
            }

        } catch (Exception e) {
            throw new DataMigrationException("Error occurred while migrating data in folder : ".concat(
                    folder.getAbsolutePath()).concat(" . "), e);
        }

    }

    private static NodeList getEncryptedPayload(Document doc, XPath xpath) throws DataMigrationException {
        try {
            XPathExpression expr = xpath.compile(
                    "//*[local-name()='property'][@*[local-name()='encrypted'='true']]/text()");
            return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new DataMigrationException("Error has occurred while retriving the payload from file : ".concat(
                    doc.getDocumentURI()), e);
        }

    }
}
