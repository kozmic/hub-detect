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
package com.blackducksoftware.integration.hub.detect.util.executable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.detect.type.ExecutableType;
import com.blackducksoftware.integration.hub.detect.type.OperatingSystemType;
import com.blackducksoftware.integration.hub.detect.util.DetectFileManager;

@Component
public class ExecutableManager {
    private final Logger logger = LoggerFactory.getLogger(ExecutableManager.class);

    @Autowired
    private DetectFileManager detectFileManager;

    private OperatingSystemType currentOs;

    public void init() {
        if (SystemUtils.IS_OS_LINUX) {
            currentOs = OperatingSystemType.LINUX;
        } else if (SystemUtils.IS_OS_MAC) {
            currentOs = OperatingSystemType.MAC;
        } else if (SystemUtils.IS_OS_WINDOWS) {
            currentOs = OperatingSystemType.WINDOWS;
        }

        if (currentOs == null) {
            logger.warn("Your operating system is not supported. Linux will be assumed.");
            currentOs = OperatingSystemType.LINUX;
        } else {
            logger.info("You seem to be running in a " + currentOs + " operating system.");
        }
    }

    public String getExecutableName(final ExecutableType executableType) {
        return executableType.getExecutable();
    }

    public Path getExecutablePath(final ExecutableType executableType, final boolean searchSystemPath, final Path path) {
        final File executable = getExecutable(executableType, searchSystemPath, path);
        if (executable != null) {
            return executable.toPath();
        } else {
            return null;
        }
    }

    public File getExecutable(final ExecutableType executableType, final boolean searchSystemPath, final Path path) {
        final String executable = getExecutableName(executableType);
        File executableFile = findExecutableFileFromPath(path.toFile().getAbsolutePath(), executable);
        if (searchSystemPath && (executableFile == null || !executableFile.exists())) {
            executableFile = findExecutableFileFromSystemPath(executable);
        }

        return executableFile;
    }

    private File findExecutableFileFromSystemPath(final String executable) {
        final String systemPath = System.getenv("PATH");
        return findExecutableFileFromPath(systemPath, executable);
    }

    private File findExecutableFileFromPath(final String path, final String executableName) {
        final List<String> executables;
        if (currentOs == OperatingSystemType.WINDOWS) {
            executables = Arrays.asList(executableName + ".cmd", executableName + ".bat", executableName + ".exe");
        } else {
            executables = Arrays.asList(executableName);
        }

        for (final String pathPiece : path.split(File.pathSeparator)) {
            for (final String possibleExecutable : executables) {
                final File foundFile = detectFileManager.findFile(Paths.get(pathPiece), possibleExecutable);
                if (foundFile != null && foundFile.exists() && foundFile.canExecute()) {
                    return foundFile;
                }
            }
        }
        return null;
    }
}
