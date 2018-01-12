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
package org.javafxports.jfxmobile.plugin.android.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.javafxports.jfxmobile.plugin.RetrolambdaExec;

import java.io.File;
import java.util.Collections;

/**
 * Applies retrolambda on the provided input and outputs to the provided output
 */
public class Retrolambda extends DefaultTask {

    @InputFiles
    @Optional
    private FileCollection classpath;

    @InputDirectory
    private File retrolambdaInput;

    @OutputDirectory
    private File retrolambdaOutput;

    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    public File getRetrolambdaInput() {
        return retrolambdaInput;
    }

    public void setRetrolambdaInput(File retrolambdaInput) {
        this.retrolambdaInput = retrolambdaInput;
    }

    public File getRetrolambdaOutput() {
        return retrolambdaOutput;
    }

    public void setRetrolambdaOutput(File retrolambdaOutput) {
        this.retrolambdaOutput = retrolambdaOutput;
    }

    @TaskAction
    public void action() {
        RetrolambdaExec exec = new RetrolambdaExec(getProject());
        exec.setInputDir(getRetrolambdaInput());
        exec.setOutputDir(getRetrolambdaOutput());

        exec.setBytecodeVersion(50);

        if (getClasspath() == null || getClasspath().isEmpty()) {
            exec.setRetrolambdaClasspath(getProject().files(getRetrolambdaInput()));
        } else {
            exec.setRetrolambdaClasspath(getProject().files(getRetrolambdaInput(), getClasspath()));
        }

        exec.setDefaultMethods(true);

        exec.setJvmArgs(Collections.emptyList());
        exec.exec();
    }
}
