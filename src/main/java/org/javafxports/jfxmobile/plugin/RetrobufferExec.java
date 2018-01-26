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
import org.gradle.api.file.FileCollection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class RetrobufferExec {

    private static final int COMMANDLINE_LENGTH_LIMIT = 3496;

    private FileCollection retrobufferClasspath;
    private File inputDir;
    private File outputDir;
    private List<String> jvmArgs;

    private final Project project;

    public RetrobufferExec(Project project) {
        this.project = project;
    }

    public FileCollection getRetrobufferClasspath() {
        return retrobufferClasspath;
    }

    public void setRetrobufferClasspath(FileCollection retrobufferClasspath) {
        this.retrobufferClasspath = retrobufferClasspath;
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

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public Project getProject() {
        return project;
    }

    public void exec() {
        project.javaexec(exec -> {
            exec.setClasspath(project.getBuildscript().getConfigurations().getByName("classpath"));

            String path = retrobufferClasspath.getAsPath();

            exec.setMain("org.javafxports.retrobuffer.Main");
            exec.setJvmArgs(Arrays.asList(
                    "-Dretrobuffer.inputDir=" + inputDir,
                    "-Dretrobuffer.outputDir=" + outputDir,
                    "-Dretrobuffer.classpath=" + path
            ));

            if (classpathLengthGreaterThanLimit(path)) {
                try {
                    File classpathFile = File.createTempFile("inc-", ".path");
                    try (BufferedWriter writer = Files.newBufferedWriter(classpathFile.toPath(), StandardCharsets.UTF_8)) {
                        for (File item : this.retrobufferClasspath) {
                            writer.write(item.toString() + "\n");
                        }
                    }
                    classpathFile.deleteOnExit();
                    exec.getJvmArgs().add("-Dretrobuffer.classpathFile=" + classpathFile.getAbsolutePath());
                } catch (IOException e) {
                }
            } else {
                exec.getJvmArgs().add("-Dretrobuffer.classpath=" + path);
            }

            for (String arg : jvmArgs) {
                exec.getJvmArgs().add(arg);
            }
        });
    }

    private static boolean classpathLengthGreaterThanLimit(String path) {
        return path.length() > COMMANDLINE_LENGTH_LIMIT;
    }
}
