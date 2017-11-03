/*
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.detect.bomtool.pip

import java.nio.file.Path

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.graph.MutableMapDependencyGraph
import com.blackducksoftware.integration.hub.bdio.model.Forge
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import com.blackducksoftware.integration.hub.detect.model.BomToolType
import com.blackducksoftware.integration.hub.detect.model.DetectCodeLocation

import groovy.transform.TypeChecked

@Component
@TypeChecked
class PipInspectorTreeParser {
    final Logger logger = LoggerFactory.getLogger(PipInspectorTreeParser.class)

    public static final String SEPARATOR = '=='
    public static final String UNKNOWN_PROJECT_NAME = 'n?'
    public static final String UNKNOWN_PROJECT_VERSION = 'v?'
    public static final String UNKNOWN_REQUIREMENTS_PREFIX = 'r?'
    public static final String UNPARSEABLE_REQUIREMENTS_PREFIX = 'p?'
    public static final String UNKNOWN_PACKAGE_PREFIX = '--'
    public static final String INDENTATION = ' '.multiply(4)

    @Autowired
    ExternalIdFactory externalIdFactory

    DetectCodeLocation parse(String treeText, Path sourcePath) {
        def lines = treeText.trim().split(System.lineSeparator()).toList()

        MutableMapDependencyGraph dependencyGraph = null
        Stack<Dependency> tree = new Stack<>()

        Dependency project = null

        int indentation = 0
        for (String line: lines) {
            if (!line.trim()) {
                continue
            }

            if (line.trim().startsWith(UNKNOWN_REQUIREMENTS_PREFIX)) {
                String path = line.replace(UNKNOWN_REQUIREMENTS_PREFIX, '').trim()
                logger.info("Pip inspector could not find requirements file @ ${path}")
                continue
            }

            if (line.trim().startsWith(UNPARSEABLE_REQUIREMENTS_PREFIX)) {
                String path = line.replace(UNPARSEABLE_REQUIREMENTS_PREFIX, '').trim()
                logger.info("Pip inspector could not parse requirements file @ ${path}")
                continue
            }

            if (line.trim().startsWith(UNKNOWN_PACKAGE_PREFIX)) {
                String packageName = line.replace(UNKNOWN_PACKAGE_PREFIX, '').trim()
                logger.info("Pip inspector could not resolve the package: ${packageName}")
                continue
            }

            if (line.contains(SEPARATOR) && !dependencyGraph) {
                dependencyGraph = new MutableMapDependencyGraph()
                project = projectLineToDependency(line, sourcePath)
                continue
            }

            if (!dependencyGraph) {
                continue
            }

            int currentIndentation = getCurrentIndentation(line)
            Dependency next = lineToDependency(line)
            if (currentIndentation == indentation) {
                tree.pop()
            } else {
                for (;indentation >= currentIndentation; indentation--) {
                    tree.pop()
                }
            }

            if (tree.size() > 0){
                dependencyGraph.addChildWithParents(next, [tree.peek()])
            } else {
                dependencyGraph.addChildrenToRoot(next)
            }

            indentation = currentIndentation
            tree.push(next)
        }

        if (project && !(project.name.equals('') && project.version.equals('') && dependencyGraph && dependencyGraph.getRootDependencyExternalIds().empty)) {
            new DetectCodeLocation(BomToolType.PIP, sourcePath, project.name, project.version, project.externalId, dependencyGraph)
        } else {
            null
        }
    }

    Dependency projectLineToDependency(String line, Path sourcePath) {
        if (!line.contains(SEPARATOR)) {
            return null
        }
        def segments = line.split(SEPARATOR)
        String name = segments[0].trim()
        String version = segments[1].trim()

        def externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, name, version)
        if (name.equals(UNKNOWN_PROJECT_NAME) || version.equals(UNKNOWN_PROJECT_VERSION) ){
            externalId = externalIdFactory.createPathExternalId(Forge.PYPI, sourcePath.toRealPath().toString())
        }

        name = name.equals(UNKNOWN_PROJECT_NAME) ? '' : name
        version = version.equals(UNKNOWN_PROJECT_VERSION) ? '' : version

        def node = new Dependency(name, version, externalId)

        node
    }

    Dependency lineToDependency(String line) {
        if (!line.contains(SEPARATOR)) {
            return null
        }
        def segments = line.split(SEPARATOR)
        String name = segments[0].trim()
        String version = segments[1].trim()

        def externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, name, version)
        def node = new Dependency(name, version, externalId)

        node
    }

    int getCurrentIndentation(String line) {
        int currentIndentation = 0
        while (line.startsWith(INDENTATION)) {
            currentIndentation++
            line = line.replaceFirst(INDENTATION, '')
        }

        currentIndentation
    }
}
