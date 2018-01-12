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
package org.javafxports.jfxmobile.plugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileCollection;
import org.gradle.util.VersionNumber;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RetrolambdaExec {

    private static final int COMMANDLINE_LENGTH_LIMIT = 3496;

    private FileCollection retrolambdaClasspath;
    private File inputDir;
    private File outputDir;
    private FileCollection includedFiles;
    private List<String> jvmArgs;
    private int bytecodeVersion;
    private boolean defaultMethods;

    private final Project project;

    public RetrolambdaExec(Project project) {
        this.project = project;
    }

    public FileCollection getRetrolambdaClasspath() {
        return retrolambdaClasspath;
    }

    public void setRetrolambdaClasspath(FileCollection retrolambdaClasspath) {
        this.retrolambdaClasspath = retrolambdaClasspath;
    }

    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public FileCollection getIncludedFiles() {
        return includedFiles;
    }

    public void setIncludedFiles(FileCollection includedFiles) {
        this.includedFiles = includedFiles;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public int getBytecodeVersion() {
        return bytecodeVersion;
    }

    public void setBytecodeVersion(int bytecodeVersion) {
        this.bytecodeVersion = bytecodeVersion;
    }

    public boolean isDefaultMethods() {
        return defaultMethods;
    }

    public void setDefaultMethods(boolean defaultMethods) {
        this.defaultMethods = defaultMethods;
    }

    public Project getProject() {
        return project;
    }

    public void exec() {
        project.javaexec(exec -> {
            Configuration retrolambdaConfig = project.getConfigurations().getByName("retrolambdaConfig");

            String path = retrolambdaClasspath.getAsPath();

            exec.setClasspath(project.files(retrolambdaConfig));
            exec.setMain("net.orfjackal.retrolambda.Main");
            exec.setJvmArgs(Arrays.asList(
                    "-Dretrolambda.inputDir=" + inputDir,
                    "-Dretrolambda.outputDir=" + outputDir,
                    "-Dretrolambda.classpath=" + path,
                    "-Dretrolambda.bytecodeVersion=" + bytecodeVersion
            ));

            VersionNumber retrolambdaVersion = retrolambdaVersion(retrolambdaConfig);
            boolean requiresJavaAgent = !requireVersion(retrolambdaVersion, "1.6.0", false);
            if (requiresJavaAgent) {
                exec.getJvmArgs().add("-javaagent:" + exec.getClasspath().getAsPath());
            }

            boolean supportIncludeFiles = requireVersion(retrolambdaVersion, "2.1.0", false);
            if (supportIncludeFiles && classpathLengthGreaterThanLimit(path)) {
                try {
                    File classpathFile = File.createTempFile("inc-", ".path");
                    try (BufferedWriter writer = Files.newBufferedWriter(classpathFile.toPath(), StandardCharsets.UTF_8)) {
                        for (File item : this.retrolambdaClasspath) {
                            writer.write(item.toString() + "\n");
                        }
                    }
                    classpathFile.deleteOnExit();
                    exec.getJvmArgs().add("-Dretrolambda.classpathFile=" + classpathFile.getAbsolutePath());
                } catch (IOException e) {

                }
            } else {
                exec.getJvmArgs().add("-Dretrolambda.classpath=" + path);
            }

            if (includedFiles != null) {
                if (supportIncludeFiles && changeFileLengthGreaterThanLimit(includedFiles)) {
                    try {
                        File includedFile = File.createTempFile("inc-", ".list");
                        try (BufferedWriter writer = Files.newBufferedWriter(includedFile.toPath(), StandardCharsets.UTF_8)) {
                            for (File file : includedFiles) {
                                writer.write(file.toString() + "\n");
                            }
                        }
                        includedFile.deleteOnExit();
                        exec.getJvmArgs().add("-Dretrolambda.includedFilesFile=" + includedFile.getAbsolutePath());
                    } catch (IOException e) {

                    }
                } else {
                    String includedArg = includedFiles.getFiles().stream().map(f -> f.toString())
                            .collect(Collectors.joining(File.pathSeparator));
                    exec.getJvmArgs().add(includedArg);
                    project.getLogger().quiet(includedArg);
                }
            }

            if (defaultMethods) {
                exec.getJvmArgs().add("-Dretrolambda.defaultMethods=true");
            }

            for (String arg : jvmArgs) {
                exec.getJvmArgs().add(arg);
            }
        });
    }

    private static boolean classpathLengthGreaterThanLimit(String path) {
        return path.length() > COMMANDLINE_LENGTH_LIMIT;
    }

    private static boolean changeFileLengthGreaterThanLimit(FileCollection includedFiles) {
        int total = 0;
        for (File file : includedFiles) {
            total += file.toString().length();
            if (total > COMMANDLINE_LENGTH_LIMIT) {
                return true;
            }
        }
        return false;
    }

    private static VersionNumber retrolambdaVersion(Configuration retrolambdaConfig) {
        retrolambdaConfig.resolve();
        Dependency retrolambdaDep = retrolambdaConfig.getDependencies().iterator().next();
        if (retrolambdaDep.getVersion() == null) {
            // Don't know version
            return null;
        }
        return VersionNumber.parse(retrolambdaDep.getVersion());
    }

    private static boolean requireVersion(VersionNumber retrolambdaVersion, String version, boolean fallback) {
        if (retrolambdaVersion == null) {
            // Don't know version, assume fallback
            return fallback;
        }
        VersionNumber targetVersionNumber = VersionNumber.parse(version);
        return retrolambdaVersion.compareTo(targetVersionNumber) >= 0;
    }
}
