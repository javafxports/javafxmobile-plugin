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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ModuleDependency

import java.nio.file.Paths

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

    /**
     * <p>Downloads the artifact of a snapshot dependency that contains a classifier and stores it locally in the
     * gradle caches directory. Use as follows:</p>
     *
     * <pre>
     *     dependencies {
     *         compile snapshotLocal('org.nd4j:nd4j-native:0.9.2-SNAPSHOT:android-arm')
     *     }
     * </pre>
     *
     * <p>If a non-snapshot dependency is provided, or the artifact is omitted, the dependency will be returned as if
     * used without wrapping it in <code>snapshotLocal</code>.</p>
     *
     * <p>This is a little trick to circumvent the gradle issue where there are snapshot dependencies with classifiers
     * and the maven repository uses different version names for each dependency artifact. Consider the following use
     * case.</p>
     *
     * <pre>
     *     dependencies {
     *         compile 'org.nd4j:nd4j-native:0.9.2-SNAPSHOT'
     *         androidRuntime 'org.nd4j:nd4j-native:0.9.2-SNAPSHOT:android-arm'
     *         androidRuntime 'org.nd4j:nd4j-native:0.9.2-SNAPSHOT:android-x86'
     *     }
     * </pre>
     *
     * <p>Gradle will fail to resolve the above dependencies, because 0.9.2-SNAPSHOT of the non-classifier snapshot
     * will resolve to a full snapshot version like: <code>0.9.2-20180209.143336-1690</code>. However, the android-arm
     * and android-x86 artifacts are published with a different version number. I.e android-arm might have version
     * <code>0.9.2-20180209.095130-1677</code>. Since gradle internally doesn't handle classifiers, the versions for
     * all dependencies to <code>org.nd4j:nd4j-native</code> will be translated to use the highest available version,
     * the one of the non-classifier dependency. Hence it tries to download the classifier artifacts with version
     * <code>0.9.2-20180209.095130-1677</code> as well, which of course doesn't exist.</p>
     *
     * @param dependencyNotation the dependency to download
     * @return
     */
    def snapshotLocal(Object dependencyNotation) {
        Dependency dependency = project.dependencies.create(dependencyNotation)
        if (dependency.version != null &&
                dependency.version.endsWith("-SNAPSHOT") &&
                dependency instanceof ModuleDependency) {
            ModuleDependency moduleDependency = (ModuleDependency) dependency
            if (!moduleDependency.artifacts.isEmpty()) {
                DependencyArtifact dependencyArtifact = moduleDependency.artifacts[0]
                def localFile = downloadLatestSnapshotVersions(dependencyArtifact, moduleDependency,
                        'https://oss.sonatype.org/content/repositories/snapshots/')
                return project.dependencies.create(project.files(localFile))
            }
        }
        return dependency
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

    // downloads the latest snapshot versions to use for a snapshot dependency
    private def downloadLatestSnapshotVersions(DependencyArtifact artifact, ModuleDependency dependency, String repository) {
        def localDependenciesDir = Paths.get(project.gradle.gradleUserHomeDir.absolutePath,
                "caches", "jfxmobile-local-snapshots")
        def localDependencyDir = Paths.get(localDependenciesDir.toFile().absolutePath, dependency.group, artifact.name, dependency.version)

        def metadataFile = new File(localDependencyDir.toFile().absolutePath, 'maven-metadata.xml')
        if (!metadataFile.exists() || metadataFile.lastModified() < (System.currentTimeMillis() - 3600 * 1000 * 24)) {
            metadataFile.parentFile.mkdirs()
            def conn = (repository + "/" + dependency.group.replace(".", "/") + "/" + dependency.name
                + "/" + dependency.version + "/maven-metadata.xml").toURL().openConnection()
            conn.addRequestProperty('User-Agent', 'curl/7.29.0')
            conn.addRequestProperty('Accept', '*/*')
            def xml = conn.content.getText('utf-8')
            metadataFile.write(xml, 'utf-8')
        }

        def artifactName = artifact.name
        if (artifact.classifier != null) {
            artifactName += '-' + artifact.classifier
        }
        artifactName += '.' + artifact.extension
        def artifactFile = new File(localDependencyDir.toFile().absolutePath, artifactName)
        if (!artifactFile.exists() || artifactFile.lastModified() < (System.currentTimeMillis() - 3600 * 1000 * 24)) {
            def metadata = new XmlSlurper().parseText(metadataFile.getText('utf-8'))
            def version = metadata.versioning.snapshotVersions.snapshotVersion.findAll { snapshot ->
                snapshot.classifier == artifact.classifier
            }*.value[0]

            def conn = (repository + "/" + dependency.group.replace(".", "/") + "/" + dependency.name
                    + "/" + dependency.version + "/" + artifact.name + "-" + version + (artifact.classifier == null ? '' : ('-' + artifact.classifier)) + '.' + artifact.extension).toURL().openConnection()
            conn.addRequestProperty('User-Agent', 'curl/7.29.0')
            conn.addRequestProperty('Accept', '*/*')
            artifactFile.setBytes(conn.content.bytes)

            println "Downloaded version $version of artifact '$dependency.group:$dependency.name:$dependency.version${artifact.classifier == null ? '' : (':' + artifact.classifier)}' to $artifactFile.absolutePath"
        }

        return artifactFile
    }
}
