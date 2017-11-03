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
package com.blackducksoftware.integration.hub.detect.model

import java.nio.file.Path

import com.blackducksoftware.integration.hub.bdio.graph.DependencyGraph
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId

class DetectCodeLocation {
    private final BomToolType bomToolType
    private final Path sourcePath
    private final String bomToolProjectName
    private final String bomToolProjectVersionName
    private final ExternalId bomToolProjectExternalId
    private final DependencyGraph dependencyGraph

    DetectCodeLocation(BomToolType bomToolType, Path sourcePath, ExternalId bomToolProjectExternalId, DependencyGraph dependencyGraph) {
        this.bomToolType = bomToolType
        this.sourcePath = sourcePath
        this.bomToolProjectExternalId = bomToolProjectExternalId
        this.dependencyGraph = dependencyGraph
    }

    DetectCodeLocation(BomToolType bomToolType, Path sourcePath, String bomToolProjectName, String bomToolProjectVersionName, ExternalId bomToolProjectExternalId, DependencyGraph dependencyGraph) {
        this.bomToolType = bomToolType
        this.sourcePath = sourcePath
        this.bomToolProjectName = bomToolProjectName
        this.bomToolProjectVersionName = bomToolProjectVersionName
        this.bomToolProjectExternalId = bomToolProjectExternalId
        this.dependencyGraph = dependencyGraph
    }

    BomToolType getBomToolType() {
        bomToolType
    }

    Path getSourcePath() {
        sourcePath
    }

    String getBomToolProjectName() {
        bomToolProjectName
    }

    String getBomToolProjectVersionName() {
        bomToolProjectVersionName
    }

    ExternalId getBomToolProjectExternalId() {
        bomToolProjectExternalId
    }

    DependencyGraph getDependencyGraph() {
        dependencyGraph
    }
}
