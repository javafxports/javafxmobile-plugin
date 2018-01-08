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
package org.javafxports.jfxmobile.plugin.android.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.javafxports.jfxmobile.plugin.android.JFXMobileDexOptions

/**
 *
 * @author joeri
 */
class Dex extends DefaultTask {

    @Nested
    JFXMobileDexOptions dexOptions

    @InputFile
    File mainDexListFile

    @InputFile
    File inputListFile

    @OutputDirectory
    File outputDirectory

    @TaskAction
    void dex() {
        project.ant.java(outputproperty: "dexCmdOut",
            errorProperty: "dexCmdErr",
            resultProperty : "dexResult",
            classname: "com.android.dx.command.Main",
            fork: true,
            classpath: project.jfxmobile.android.buildToolsLib.absolutePath + "/dx.jar") {
            if (getDexOptions().javaMaxHeapSize != null) {
                jvmarg(value: "-Xmx${getDexOptions().javaMaxHeapSize}")
            } else {
                jvmarg(value: "-Xmx2g")
            }
            arg(value: "--dex")
            if (project.logger.debugEnabled) {
                arg(value: "--debug")
            }
            arg(value: "--verbose")
            if (getDexOptions().threadCount == null) {
                arg(value: "--num-threads=4")
            } else {
                arg(value: "--num-threads=${getDexOptions().threadCount}")
            }
            if (getDexOptions().jumboMode) {
                args(value: "--force-jumbo")
            }
            getDexOptions().additionalParameters.each {
                arg(value: it)
            }
            arg(value: "--no-optimize")
            arg(value: "--multi-dex")
            arg(value: "--main-dex-list=" + getMainDexListFile().absolutePath)
            arg(value: "--core-library")
            arg(value: "--output=" + getOutputDirectory().absolutePath)
            arg(value: "--input-list=" + getInputListFile().absolutePath)
        }

        project.logger.debug("Dex result value = ${project.ant.project.properties.dexResult}")
        if (project.ant.project.properties.dexResult != "0") {
            throw new GradleException(project.ant.project.properties.dexCmdErr)
        }
    }

}

