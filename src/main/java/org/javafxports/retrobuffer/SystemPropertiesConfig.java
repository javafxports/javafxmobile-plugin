/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2018, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.javafxports.retrobuffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SystemPropertiesConfig {

    private static final String PREFIX = "retrobuffer.";
    private static final String CLASSPATH = PREFIX + "classpath";
    private static final String CLASSPATH_FILE = CLASSPATH + "File";
    private static final String INPUT_DIR = PREFIX + "inputDir";
    private static final String OUTPUT_DIR = PREFIX + "outputDir";

    private final Properties p;

    public SystemPropertiesConfig(Properties properties) {
        this.p = properties;
    }

    public Path getInputDir() {
        String inputDir = p.getProperty(INPUT_DIR);
        if (inputDir != null) {
            return Paths.get(inputDir);
        }
        throw new IllegalArgumentException("Missing required property: " + INPUT_DIR);
    }

    public Path getOutputDir() {
        String outputDir = p.getProperty(OUTPUT_DIR);
        if (outputDir != null) {
            return Paths.get(outputDir);
        }
        return getInputDir();
    }

    public List<Path> getClasspath() {
        String classpath = p.getProperty(CLASSPATH);
        if (classpath != null) {
            return parsePathList(classpath);
        }
        String classpathFile = p.getProperty(CLASSPATH_FILE);
        if (classpathFile != null) {
            return readPathList(Paths.get(classpathFile));
        }
        throw new IllegalArgumentException("Missing required property: " + CLASSPATH);
    }

    private static List<Path> parsePathList(String paths) {
        return Stream.of(paths.split(File.pathSeparator))
                .filter(path -> !path.isEmpty())
                .map(Paths::get)
                .collect(Collectors.toList());
    }

    private static List<Path> readPathList(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .filter(line -> !line.isEmpty())
                    .map(Paths::get)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }
}
