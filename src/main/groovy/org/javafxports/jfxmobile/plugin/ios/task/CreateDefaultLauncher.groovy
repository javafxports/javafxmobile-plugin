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
package org.javafxports.jfxmobile.plugin.ios.task

import java.nio.charset.StandardCharsets
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Copies the default launcher class from the plugin's classpath to a temporary
 * ios sources folder. It also replaces the strings "<mainClassName>" and
 * "<preloaderClassName>" in the file with the mainClassName and
 * preloaderClassName of the project respectively.
 *
 * @author joeri
 */
class CreateDefaultLauncher extends DefaultTask {

    @Input
    String mainClassName

    @Input
    @Optional
    String preloaderClassName

    @OutputFile
    File outputFile

    @TaskAction
    void createDefaultLauncher() {
        File basicLauncherSourceFile = getOutputFile()
        InputStream originalLauncherSourceFile = getClass().getClassLoader().getResourceAsStream('ios/sources/BasicLauncher.java')
        if (originalLauncherSourceFile != null) {
            // String basicLauncherSourceString = originalLauncherSourceFile.getText(StandardCharsets.UTF_8.name())
            String basicLauncherSourceString = originalLauncherSourceFile.getText("UTF-8");
            basicLauncherSourceString = basicLauncherSourceString.replaceAll('<mainClassName>', "${getMainClassName()}.class")
            if (getPreloaderClassName() != null && !getPreloaderClassName().empty) {
                basicLauncherSourceString = basicLauncherSourceString.replaceAll("<preloaderClassName>", "${getPreloaderClassName()}.class")
            } else {
                basicLauncherSourceString = basicLauncherSourceString.replaceAll("<preloaderClassName>", "null")
            }

            // basicLauncherSourceFile.write(basicLauncherSourceString, StandardCharsets.UTF_8.name())
            basicLauncherSourceFile.write(basicLauncherSourceString, "UTF-8")
        }
    }

}

