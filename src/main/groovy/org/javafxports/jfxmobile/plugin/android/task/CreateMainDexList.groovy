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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.javafxports.jfxmobile.plugin.android.JFXMobileDexOptions

/**
 *
 * @author joeri
 */
class CreateMainDexList extends DefaultTask {

    @Nested
    JFXMobileDexOptions dexOptions

    @InputFile
    File componentsClassesJarFile

    @InputFile
    File allClassesJarFile

    @OutputFile
    File outputFile

    @TaskAction
    void createMainDexList() {
        project.ant.java(outputproperty: "createMainDexListCmdOut",
            errorProperty: "createMainDexListCmdErr",
            resultProperty: "createMainDexListResult",
            classname: "com.android.multidex.ClassReferenceListBuilder",
            fork: true,
            classpath: project.jfxmobile.android.buildToolsLib.absolutePath + "/dx.jar") {
            jvmarg(value: "-Xmx1024M")
            if (!getDexOptions().keepRuntimeAnnotatedClasses) {
                arg(value: "--disable-annotation-resolution-workaround")
            }
            arg(value: getComponentsClassesJarFile())
            arg(value: getAllClassesJarFile())
        }

        project.logger.debug("CreateMainDexList result value = ${project.ant.project.properties.createMainDexListResult}")
        if (project.ant.project.properties.createMainDexListResult != "0") {
            throw new GradleException(project.ant.project.properties.createMainDexListCmdErr)
        } else {
            getOutputFile().text = project.ant.project.properties.createMainDexListCmdOut
        }
    }

}

