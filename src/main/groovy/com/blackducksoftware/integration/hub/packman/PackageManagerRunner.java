/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.packman;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.bdio.simple.BdioWriter;
import com.blackducksoftware.integration.hub.bdio.simple.DependencyNodeTransformer;
import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode;
import com.blackducksoftware.integration.hub.bdio.simple.model.SimpleBdioDocument;
import com.blackducksoftware.integration.hub.packman.packagemanager.PackageManager;
import com.blackducksoftware.integration.hub.packman.type.PackageManagerType;
import com.blackducksoftware.integration.util.IntegrationEscapeUtil;
import com.google.gson.Gson;

@Component
public class PackageManagerRunner {
    private final Logger logger = LoggerFactory.getLogger(PackageManagerRunner.class);

    @Value("${packman.package.manager.type.override}")
    private String packageManagerTypeOverride;

    @Value("${packman.project.name}")
    private String projectName;

    @Value("${packman.project.version}")
    private String projectVersionName;

    @Autowired
    PackmanProperties packmanProperties;

    @Autowired
    private List<PackageManager> packageManagers;

    @Autowired
    private Gson gson;

    @Autowired
    private DependencyNodeTransformer dependencyNodeTransformer;

    public List<File> createBdioFiles() throws IOException {
        final String[] sourcePaths = packmanProperties.getSourcePaths();
        final List<File> createdBdioFiles = new ArrayList<>();
        boolean foundSomePackageManagers = false;
        final List<PackageManager> filteredPackageManagers = filterPackageManagers();
        for (final PackageManager packageManager : filteredPackageManagers) {
            for (final String sourcePath : sourcePaths) {
                final String packageManagerName = packageManager.getPackageManagerType().toString().toLowerCase();
                logger.info(String.format("Searching source path for %s: %s", packageManagerName, sourcePath));
                try {
                    if (packageManager.isPackageManagerApplicable(sourcePath)) {
                        logger.info(String.format("Found files for %s", packageManagerName));
                        final List<DependencyNode> projectNodes = packageManager.extractDependencyNodes(sourcePath);
                        if (projectNodes != null && projectNodes.size() > 0) {
                            foundSomePackageManagers = true;
                            createOutput(createdBdioFiles, packageManager.getPackageManagerType(), projectNodes);
                        }
                    }
                } catch (final Exception e) {
                    logger.error(String.format("Error running package manager %s for sourcePath %s: %s", packageManager.getPackageManagerType().toString(),
                            sourcePath, e.getMessage()));
                    e.printStackTrace();
                }
            }
        }
        if (!foundSomePackageManagers) {
            logger.info("Could not find any package managers");
        }
        return createdBdioFiles;
    }

    private List<PackageManager> filterPackageManagers() {
        final EnumSet<PackageManagerType> packageManagerTypes = determinePackageManagerTypes();
        if (packageManagerTypes.isEmpty()) {
            return packageManagers;
        }

        final List<PackageManager> filteredPackageManagers = new ArrayList<>();
        for (final PackageManager packageManager : packageManagers) {
            if (packageManagerTypes.contains(packageManager.getPackageManagerType())) {
                filteredPackageManagers.add(packageManager);
            }
        }
        return filteredPackageManagers;
    }

    private EnumSet<PackageManagerType> determinePackageManagerTypes() {
        final EnumSet<PackageManagerType> packageManagerTypes = EnumSet.noneOf(PackageManagerType.class);
        final String[] packageManagerTypePieces = StringUtils.trimToEmpty(packageManagerTypeOverride).split(",");
        for (final String packageManagerTypePiece : packageManagerTypePieces) {
            final String trimmedPiece = StringUtils.trimToEmpty(packageManagerTypePiece);
            try {
                final PackageManagerType packageManagerType = PackageManagerType.valueOf(trimmedPiece);
                packageManagerTypes.add(packageManagerType);
            } catch (final IllegalArgumentException e) {
                logger.warn("Invalid PackageManagerType used in override: " + trimmedPiece);
            }
        }

        return packageManagerTypes;
    }

    private void createOutput(final List<File> createdBdioFiles, final PackageManagerType packageManagerType,
            final List<DependencyNode> projectNodes) {
        final File outputDirectory = new File(packmanProperties.getOutputDirectoryPath());

        logger.info("Creating " + projectNodes.size() + " project nodes");
        for (final DependencyNode project : projectNodes) {
            final IntegrationEscapeUtil escapeUtil = new IntegrationEscapeUtil();
            final String safeProjectName = escapeUtil.escapeForUri(project.name);
            final String safeVersionName = escapeUtil.escapeForUri(project.version);
            final String safeName = String.format("%s_%s_%s_bdio", packageManagerType.toString(), safeProjectName, safeVersionName);
            final String filename = String.format("%s.jsonld", safeName);
            final File outputFile = new File(outputDirectory, filename);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            try (final BdioWriter bdioWriter = new BdioWriter(gson, new FileOutputStream(outputFile))) {
                if (StringUtils.isNotBlank(projectName)) {
                    project.name = projectName;
                }
                if (StringUtils.isNotBlank(projectVersionName)) {
                    project.version = projectVersionName;
                }
                final SimpleBdioDocument bdioDocument = dependencyNodeTransformer.transformDependencyNode(project);
                if (StringUtils.isNotBlank(projectName) && StringUtils.isNotBlank(projectVersionName)) {
                    bdioDocument.billOfMaterials.spdxName = String.format("%s/%s/%s Black Duck I/O Export", project.name, project.version,
                            packageManagerType.toString());
                }
                bdioWriter.writeSimpleBdioDocument(bdioDocument);
                createdBdioFiles.add(outputFile);
                logger.info("BDIO Generated: " + outputFile.getAbsolutePath());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
