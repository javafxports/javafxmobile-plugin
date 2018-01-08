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
package org.javafxports.jfxmobile.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 *
 * @author joeri
 */
class JFXMobileConvention {

    /**
     * The fully qualified name of the application's Preloader class.
     */
    String preloaderClassName

    final Project project

    JFXMobileConvention(Project project) {
        this.project = project
    }

    def explodeAarDependencies(Configuration...configurations) {
        configurations.each { configuration ->
            Set<File> files = configuration.copy().resolve()
            files.findAll {
                it.name.endsWith('.aar')
            }.each { aarFile ->
                def aarFileWithoutExtension = aarFile.name.take(aarFile.name.lastIndexOf('.'))
                final File explodedDirectory = new File(aarFile.parent, "exploded")
                if (!explodedDirectory.exists()) {
                    project.logger.info("Explode aar file: $aarFile into $explodedDirectory")

                    if (!explodedDirectory.mkdirs()) {
                        throw new GradleException("Unable to create base directory to explode aar into: " + explodedDirectory)
                    }

                    project.copy {
                        from project.zipTree(aarFile)
                        into project.file(explodedDirectory)
                        include 'classes.jar'
                        include 'libs/*.jar'
                        rename('classes.jar', "${aarFileWithoutExtension}.jar")
                    }
                }

                if (project.file("$explodedDirectory.absolutePath/${aarFileWithoutExtension}.jar").exists()) {
                    project.dependencies.add(configuration.name, project.files("$explodedDirectory.absolutePath/${aarFileWithoutExtension}.jar"))
                }
                if (project.file("$explodedDirectory.absolutePath/libs").exists()) {
                    project.files(project.file("$explodedDirectory.absolutePath/libs").listFiles()).findAll {
                        it.name.endsWith('.jar')
                    }.each {
                        project.dependencies.add(configuration.name, project.files(it))
                    }
                }
            }
        }
    }
}
