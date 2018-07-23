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

import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.build.gradle.internal.scope.AndroidTask
import com.android.builder.core.BuilderConstants
import com.android.builder.signing.DefaultSigningConfig
import com.android.ide.common.res2.AssetSet
import com.android.ide.common.signing.KeystoreHelper
import com.android.prefs.AndroidLocation
import com.android.repository.Revision
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.tooling.BuildException
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.javafxports.jfxmobile.plugin.android.task.Apk
import org.javafxports.jfxmobile.plugin.android.task.CreateMainDexList
import org.javafxports.jfxmobile.plugin.android.task.CreateManifestKeepList
import org.javafxports.jfxmobile.plugin.android.task.Dex
import org.javafxports.jfxmobile.plugin.android.task.Install
import org.javafxports.jfxmobile.plugin.android.task.MergeAssets
import org.javafxports.jfxmobile.plugin.android.task.MergeResources
import org.javafxports.jfxmobile.plugin.android.task.ProcessResources
import org.javafxports.jfxmobile.plugin.android.task.Retrobuffer
import org.javafxports.jfxmobile.plugin.android.task.Retrolambda
import org.javafxports.jfxmobile.plugin.android.task.ValidateManifest
import org.javafxports.jfxmobile.plugin.android.task.ValidateSigning
import org.javafxports.jfxmobile.plugin.android.task.WriteDexInputListFile
import org.javafxports.jfxmobile.plugin.android.task.ZipAlign
import org.javafxports.jfxmobile.plugin.embedded.RemotePlatformConfiguration
import org.javafxports.jfxmobile.plugin.embedded.task.CopyRemoteDir
import org.javafxports.jfxmobile.plugin.embedded.task.RunEmbedded
import org.javafxports.jfxmobile.plugin.ios.task.CreateIpa
import org.javafxports.jfxmobile.plugin.ios.task.IosDevice
import org.javafxports.jfxmobile.plugin.ios.task.IosInstall
import org.javafxports.jfxmobile.plugin.ios.task.IosSimulator
import proguard.gradle.ProGuardTask

import javax.inject.Inject

/**
 *
 * @author joeri
 */
class JFXMobilePlugin implements Plugin<Project> {

    private static final RETROLAMBDA_COMPILE = 'net.orfjackal.retrolambda:retrolambda'
    private static final EMBEDDED_TASKS_GROUP = 'Gluon Mobile for Embedded'
    private static final ANDROID_TASKS_GROUP = 'Gluon Mobile for Android'
    private static final IOS_TASKS_GROUP = 'Gluon Mobile for iOS'

    private ObjectFactory objectFactory
    private ToolingModelBuilderRegistry registry
    private Project project

    private int gradleMajor
    private int gradleMinor

    List<Task> androidTasks = []
    List<Task> iosTasks = []
    List<Task> embeddedTasks = []

    private boolean hasAndroidMavenRepository = false

    @Inject
    JFXMobilePlugin(ObjectFactory objectFactory, ToolingModelBuilderRegistry registry) {
        this.objectFactory = objectFactory
        this.registry = registry
    }

    void apply(Project project) {
        this.project = project

        def gradleVersion = project.gradle.gradleVersion.split("[\\.]")
        gradleMajor = Integer.parseInt(gradleVersion[0])
        gradleMinor = Integer.parseInt(gradleVersion[1])
        if (gradleMajor < 4 || gradleMajor == 4 && gradleMinor < 2) {
            throw new GradleException("You are using Gradle ${project.gradle.gradleVersion} but we require version 4.2 or higher")
        }

        project.plugins.apply JavaPlugin
        project.plugins.apply ApplicationPlugin
        project.sourceSets {
            desktop {
                java {
                    compileClasspath += main.output
                    runtimeClasspath += main.output
                }
            }
            android {
                java {
                    compileClasspath += main.output
                    runtimeClasspath += main.output
                }
            }
            ios {
                java {
                    compileClasspath += main.output
                    runtimeClasspath += main.output
                }
            }
            embedded {
                java {
                    compileClasspath += main.output
                    runtimeClasspath += main.output
                }
            }
        }

        JFXMobileExtension jfxMobileExtension = project.extensions.create("jfxmobile", JFXMobileExtension, project,
                objectFactory, registry)

        JFXMobileConvention pluginConvention = new JFXMobileConvention(project)
        project.convention.plugins.'org.javafxports.jfxmobile' = pluginConvention

        project.configurations {
            retrolambdaConfig

            compileNoRetrolambda
            runtimeNoRetrolambda {
                extendsFrom compileNoRetrolambda
            }
            compile {
                extendsFrom compileNoRetrolambda
            }
            runtime {
                extendsFrom runtimeNoRetrolambda
            }

            androidCompileNoRetrolambda {
                extendsFrom compileNoRetrolambda
            }
            androidRuntimeNoRetrolambda {
                extendsFrom androidCompileNoRetrolambda, runtimeNoRetrolambda
            }

            androidBootclasspath

            androidSdk
            dalvikSdk
            iosSdk
            sshAntTask
        }

        project.configurations.desktopCompile.extendsFrom project.configurations.compile
        project.configurations.desktopRuntime.extendsFrom project.configurations.desktopCompile, project.configurations.runtime
        project.configurations.androidCompile.extendsFrom project.configurations.compile, project.configurations.androidCompileNoRetrolambda, project.configurations.androidBootclasspath, project.configurations.androidSdk
        project.configurations.androidRuntime.extendsFrom project.configurations.androidCompile, project.configurations.runtime, project.configurations.androidRuntimeNoRetrolambda
        project.configurations.iosCompile.extendsFrom project.configurations.compile
        project.configurations.iosRuntime.extendsFrom project.configurations.iosCompile, project.configurations.runtime
        project.configurations.embeddedCompile.extendsFrom project.configurations.compile
        project.configurations.embeddedRuntime.extendsFrom project.configurations.embeddedCompile, project.configurations.runtime

        createAndroidTasks()
        createIosTasks()
        createEmbeddedTasks()

        // include the maven repositories from the android sdk if they were downloaded
        String androidSdkLocation = locateAndroidSdk()
        if (androidSdkLocation != null) {
            File mavenRepository = project.file("$androidSdkLocation/extras/m2repository")
            if (mavenRepository.exists()) {
                project.logger.info("Adding $mavenRepository.absolutePath to project repositories.")
                project.repositories {
                    maven {
                        url mavenRepository.toURI().toString()
                    }
                }
            }

            File androidMavenRepository = project.file("$androidSdkLocation/extras/android/m2repository")
            if (androidMavenRepository.exists()) {
                project.logger.info("Adding $androidMavenRepository.absolutePath to project repositories.")
                project.repositories {
                    maven {
                        url androidMavenRepository.toURI().toString()
                    }
                }
                hasAndroidMavenRepository = true
            } 

            File googleMavenRepository = project.file("$androidSdkLocation/extras/google/m2repository")
            if (googleMavenRepository.exists()) {
                project.logger.info("Adding $googleMavenRepository.absolutePath to project repositories.")
                project.repositories {
                    maven {
                        url googleMavenRepository.toURI().toString()
                    }
                }
            }
        }

        project.afterEvaluate {
            // apply downConfig to project configurations
            project.jfxmobile.downConfig.applyConfiguration(project.configurations.compile)
            project.jfxmobile.downConfig.applyConfiguration(project.configurations.desktopRuntime)
            project.jfxmobile.downConfig.applyConfiguration(project.configurations.iosRuntime)
            project.jfxmobile.downConfig.applyConfiguration(project.configurations.embeddedRuntime)

            // try to set android.jar dependency as early as possible, but only
            // when the jar can be found
            File platformAndroidJar = getPlatformAndroidJar()
            if (platformAndroidJar != null && platformAndroidJar.exists()) {
                project.logger.info("Adding $platformAndroidJar.absolutePath to androidSdk dependency configuration.")
                project.dependencies {
                    androidSdk project.files(platformAndroidJar.absolutePath)
                    androidRuntimeNoRetrolambda 'com.android.support:multidex:1.0.1'
                }
            }

            // configure android and ios dependencies
            project.dependencies {
                retrolambdaConfig "${RETROLAMBDA_COMPILE}:${project.jfxmobile.android.retrolambdaVersion}"
                androidCompile("org.javafxports:jfxdvk:${project.jfxmobile.javafxportsVersion}") {
                    force = true
                }
                dalvikSdk "org.javafxports:dalvik-sdk:${project.jfxmobile.javafxportsVersion}@zip"
                sshAntTask 'org.apache.ant:ant-jsch:1.9.6'
            }

            // set the encoding option for the javac compile tasks
            project.tasks.withType(JavaCompile) {
                options.encoding = project.jfxmobile.javacEncoding
            }

            // add our own debug task, calling replace because the NetBeans gradle plugin already creates one
            project.tasks.replace('debug', JavaExec)
            project.tasks.debug {
                description = 'Runs this program as a JVM application for debugging'
                group = 'application'
                main = project.mainClassName
                classpath = project.sourceSets.desktop.runtimeClasspath
                debug = true
            }
            project.tasks.debug.dependsOn project.tasks.classes

            // include desktop when creating jar and running application
            project.tasks.run {
                classpath = project.sourceSets.desktop.runtimeClasspath
                if (project.preloaderClassName != null && !project.preloaderClassName.empty) {
                    systemProperties('javafx.preloader' : project.preloaderClassName)
                }
                if (JavaVersion.current().isJava9Compatible()) {
                    jvmArgs += "--add-opens=javafx.controls/javafx.scene.control.skin=ALL-UNNAMED"
                }
            }
            project.tasks.jar {
                from project.sourceSets.desktop.output
                manifest {
                    if (project.preloaderClassName != null && !project.preloaderClassName.empty) {
                        attributes(
                            'Main-Class' : project.mainClassName,
                            'JavaFX-Preloader-Class' : project.preloaderClassName
                        )
                    } else {
                        attributes(
                            'Main-Class' : project.mainClassName
                        )
                    }
                }
            }
        }

        project.gradle.taskGraph.whenReady {
            project.logger.info("Using javafxports version ${project.jfxmobile.javafxportsVersion}")
            configure()

            // only configure android when one of the android tasks will be run
            if (androidTasks.find { project.gradle.taskGraph.hasTask(it) } != null) {
                if (!hasAndroidMavenRepository)  {
                    throw new GradleException("You must install the Android Support Repository. Open the Android SDK Manager and choose the Android Support Repository from the Extras category at the bottom of the list of packages.")
                }
                configureAndroid()

                project.jfxmobile.downConfig.applyConfiguration(project.configurations.androidRuntime)

                // explode aar dependencies, adding a dependency on the inner classes.jar
                pluginConvention.explodeAarDependencies(project.configurations.androidCompile,
                        project.configurations.androidRuntime, project.configurations.androidCompileNoRetrolambda,
                        project.configurations.androidRuntimeNoRetrolambda)

                project.dependencies {
                    androidRuntime project.fileTree("${project.jfxmobile.android.dalvikSdkLib}/ext") {
                        include 'compat-1.0.0.jar'
                    }
                    androidRuntimeNoRetrolambda project.fileTree("${project.jfxmobile.android.dalvikSdkLib}/ext") {
                        include 'jfxrt.jar'
                    }
                }

                // configure android boot classpath
                def androidBootclasspath = project.configurations.androidBootclasspath
                if (!androidBootclasspath.empty) {
                    project.tasks.compileAndroidJava {
                        options.bootstrapClasspath = androidBootclasspath
                    }
                }

                // NOTE: from is set after all configuration for androidRuntime has completed
                project.tasks.copyClassesForDesugar.from {
                    (project.configurations.androidRuntime - project.configurations.androidRuntimeNoRetrolambda - project.configurations.androidSdk).filter {
                        !it.isDirectory()
                    }.collect {
                        project.logger.info("Apply Desugar to $it")
                        project.zipTree(it)
                    }
                }

                // NOTE: from is set after all configuration for androidRuntimeNoRetrolambda has completed
                project.tasks.mergeClassesIntoJar.from {
                    project.configurations.androidRuntimeNoRetrolambda.collect { project.zipTree(it) }
                }
            } else {
                // ignore tasks for android sourceSet
                project.tasks.androidClasses.enabled = false
                project.tasks.compileAndroidJava.enabled = false
                project.tasks.processAndroidResources.enabled = false
            }

            // only configure ios when one of the ios tasks will be run
            if (iosTasks.find { project.gradle.taskGraph.hasTask(it) } != null) {
                if (!JavaVersion.current().isJava9Compatible()) {
                    throw new GradleException("Gluon VM with iOS requires Java " + JavaVersion.VERSION_1_9.getMajorVersion() + " or higher, but Java " + JavaVersion.current().getMajorVersion() + " was detected.");
                }

                // NOTE: from is set after all configuration for iosRuntime has completed
//                project.tasks.iosExtractNativeLibs.from {
//                    project.configurations.iosRuntime.collect { project.zipTree(it).matching { include 'native/*.a' }}
//                }
            } else {
                // ignore tasks for ios sourceSet
                project.tasks.iosClasses.enabled = false
                project.tasks.compileIosJava.enabled = false
                project.tasks.processIosResources.enabled = false
            }
            // only configure embedded when one of the embedded tasks will be run
            if (embeddedTasks.find { project.gradle.taskGraph.hasTask(it) } != null) {
                configureEmbedded()
            }
        }
    }

    private void configure() {
        if (project.mainClassName == null || project.mainClassName.empty) {
            throw new GradleException("Missing or empty mainClassName property.")
        }
    }

    private void createAndroidTasks() {
        Delete cleanAndroidTask = project.tasks.create("cleanAndroid", Delete) {
            delete project.jfxmobile.android.temporaryDirectory, project.jfxmobile.android.installDirectory
        }

        ValidateManifest validateManifestTask = project.tasks.create("validateManifest", ValidateManifest)
        validateManifestTask.conventionMapping.map("output") { project.file("${project.jfxmobile.android.temporaryDirectory}/AndroidManifest.xml") }
        androidTasks.add(validateManifestTask)

        CreateManifestKeepList manifestKeepListTask = project.tasks.create("collectMultiDexComponents", CreateManifestKeepList)
        manifestKeepListTask.conventionMapping.map("outputFile") { project.file("${project.jfxmobile.android.multidexOutputDirectory}/manifest_keep.txt") }
        manifestKeepListTask.conventionMapping.map("manifest") { validateManifestTask.output }
        manifestKeepListTask.conventionMapping.map("dexOptions") { project.jfxmobile.android.dexOptions }
        manifestKeepListTask.conventionMapping.map("proguardFile") { project.jfxmobile.android.proguardFile != null && !project.jfxmobile.android.proguardFile.trim().empty ? project.file(project.jfxmobile.android.proguardFile) : null }
        manifestKeepListTask.dependsOn validateManifestTask
        androidTasks.add(manifestKeepListTask)

        Copy copyClassesForDesugar = project.tasks.create("copyClassesForDesugar", Copy)
        copyClassesForDesugar.from project.sourceSets.main.output.classesDirs
        copyClassesForDesugar.from project.sourceSets.android.output.classesDirs
        copyClassesForDesugar.include '**/*.class'
        copyClassesForDesugar.includeEmptyDirs = false
        copyClassesForDesugar.exclude 'META-INF/versions/**/*.class'
        copyClassesForDesugar.exclude 'module-info.class'
        copyClassesForDesugar.destinationDir = project.file("${project.jfxmobile.android.temporaryDirectory}/desugar/input")
        copyClassesForDesugar.dependsOn project.tasks.compileJava, project.tasks.compileAndroidJava
        androidTasks.add(copyClassesForDesugar)

        Retrobuffer retrobufferTask = project.tasks.create("applyRetrobuffer", Retrobuffer)
        retrobufferTask.conventionMapping.map("classpath") { project.files(project.configurations.androidCompile, project.configurations.androidSdk) }
        retrobufferTask.retrobufferOutput = project.file("${project.jfxmobile.android.temporaryDirectory}/retrobuffer/output")

        Retrolambda retrolambdaTask = project.tasks.create("applyRetrolambda", Retrolambda)
        retrolambdaTask.conventionMapping.map("classpath") { project.files(project.configurations.androidCompileNoRetrolambda, project.configurations.androidSdk) }
        retrolambdaTask.retrolambdaOutput = project.file("${project.jfxmobile.android.temporaryDirectory}/retrolambda/output")
        androidTasks.add(retrolambdaTask)

        if (JavaVersion.current().isJava9Compatible()) {
            retrobufferTask.retrobufferInput = copyClassesForDesugar.destinationDir
            retrobufferTask.dependsOn copyClassesForDesugar

            retrolambdaTask.retrolambdaInput = retrobufferTask.retrobufferOutput
            retrolambdaTask.dependsOn retrobufferTask

            androidTasks.add(retrobufferTask)
        } else {
            retrobufferTask.enabled = false

            retrolambdaTask.retrolambdaInput = copyClassesForDesugar.destinationDir
            retrolambdaTask.dependsOn copyClassesForDesugar
        }

/*
        AndroidTask<DesugarTask> desugarTask = project.jfxmobile.android.androidTaskRegistry.create(
                project.jfxmobile.android.taskFactory,
                new DesugarTask.ConfigAction(project.jfxmobile.android,
                        "desugar",
                        copyClassesForDesugar.destinationDir,
                        project.file("${project.jfxmobile.android.temporaryDirectory}/desugar/output")))
        desugarTask.get(project.jfxmobile.android.taskFactory).dependsOn copyClassesForDesugar
        androidTasks.add(desugarTask.get(project.jfxmobile.android.taskFactory))
*/

        Jar mergeClassesIntoJarTask = project.tasks.create("mergeClassesIntoJar", Jar)
        mergeClassesIntoJarTask.destinationDir = project.file("${project.jfxmobile.android.multidexOutputDirectory}")
        mergeClassesIntoJarTask.archiveName = 'allclasses.jar'
        mergeClassesIntoJarTask.from retrolambdaTask.retrolambdaOutput
//        mergeClassesIntoJarTask.from desugarTask.get(project.jfxmobile.android.taskFactory).outputDir
        mergeClassesIntoJarTask.include '**/*.class'
        mergeClassesIntoJarTask.dependsOn retrolambdaTask
//        mergeClassesIntoJarTask.dependsOn desugarTask.get(project.jfxmobile.android.taskFactory)
        androidTasks.add(mergeClassesIntoJarTask)

        ProGuardTask proguardComponentsTask = project.tasks.create("shrinkMultiDexComponents", ProGuardTask)
        proguardComponentsTask.dontobfuscate()
        proguardComponentsTask.dontoptimize()
        proguardComponentsTask.dontpreverify()
        proguardComponentsTask.dontnote()
        proguardComponentsTask.dontwarn()
        if (project.logger.debugEnabled) {
            proguardComponentsTask.verbose()
        }
        proguardComponentsTask.forceprocessing()
        proguardComponentsTask.configuration(manifestKeepListTask.outputFile)
        proguardComponentsTask.libraryjars({
            return project.file("${project.jfxmobile.android.buildToolsLib}/shrinkedAndroid.jar")
        })
        proguardComponentsTask.injars(mergeClassesIntoJarTask.archivePath)
        File componentsJarFile = project.file("${project.jfxmobile.android.multidexOutputDirectory}/componentClasses.jar")
        proguardComponentsTask.outjars(componentsJarFile)
        proguardComponentsTask.printconfiguration("${project.jfxmobile.android.multidexOutputDirectory}/components.flags")
        proguardComponentsTask.dependsOn cleanAndroidTask, manifestKeepListTask, mergeClassesIntoJarTask
        androidTasks.add(proguardComponentsTask)

        CreateMainDexList createMainDexListTask = project.tasks.create("createMainDexList", CreateMainDexList)
        createMainDexListTask.allClassesJarFile = mergeClassesIntoJarTask.archivePath
        createMainDexListTask.conventionMapping.map("componentsClassesJarFile") { componentsJarFile }
        createMainDexListTask.conventionMapping.map("dexOptions") { project.jfxmobile.android.dexOptions }
        createMainDexListTask.outputFile = project.file("${project.jfxmobile.android.multidexOutputDirectory}/maindexlist.txt")
        createMainDexListTask.dependsOn proguardComponentsTask
        androidTasks.add(createMainDexListTask)

        WriteDexInputListFile writeInputListFileTask = project.tasks.create("writeInputListFile", WriteDexInputListFile)
        writeInputListFileTask.inputListFile = project.file("${project.jfxmobile.android.dexOutputDirectory}/inputList.txt")
        writeInputListFileTask.jar = mergeClassesIntoJarTask.archivePath
        writeInputListFileTask.dependsOn mergeClassesIntoJarTask
        androidTasks.add(writeInputListFileTask)

        Dex dexTask = project.tasks.create("dex", Dex)
        dexTask.conventionMapping.map("mainDexListFile") { createMainDexListTask.outputFile }
        dexTask.conventionMapping.map("inputListFile") { writeInputListFileTask.inputListFile }
        dexTask.conventionMapping.map("dexOptions") { project.jfxmobile.android.dexOptions }
        dexTask.outputDirectory = project.file("${project.jfxmobile.android.dexOutputDirectory}")
        dexTask.dependsOn createMainDexListTask, writeInputListFileTask
        androidTasks.add(dexTask)

        AndroidTask<MergeResources> mergeResourcesTask = project.jfxmobile.android.androidTaskRegistry.create(
                project.jfxmobile.android.taskFactory,
                new MergeResources.ConfigAction(project.jfxmobile.android,
                        "merge",
                        project.file("${project.jfxmobile.android.resourcesDirectory}/res"),
                        true,
                        true))
        androidTasks.add(mergeResourcesTask.get(project.jfxmobile.android.taskFactory))

        MergeAssets mergeAssetsTask = project.tasks.create("mergeAndroidAssets", MergeAssets)
        mergeAssetsTask.conventionMapping.map("inputAssetSets") {
            AssetSet mainAssetSet = new AssetSet(BuilderConstants.MAIN)
            mainAssetSet.addSource(project.file(project.jfxmobile.android.assetsDirectory))
            return [ mainAssetSet ]
        }
        mergeAssetsTask.conventionMapping.map("outputDir") { project.file("${project.jfxmobile.android.resourcesDirectory}/assets") }
        androidTasks.add(mergeAssetsTask)

        SigningConfig releaseSigningConfig = project.jfxmobile.android.signingConfig
        ZipAlign apkReleaseTask = createApkTasks('Release', releaseSigningConfig)

        DefaultTask androidReleaseTask = project.tasks.create("androidRelease", DefaultTask)
        androidReleaseTask.description("Generates a release Android apk containing the JavaFX application.")
        androidReleaseTask.dependsOn apkReleaseTask
        androidTasks.add(androidReleaseTask)

        SigningConfig debugSigningConfig = new SigningConfig('debug')
        try {
            debugSigningConfig.initWith(
                    DefaultSigningConfig.debugSigningConfig(
                            new File(KeystoreHelper.defaultDebugKeystoreLocation())));
        } catch (AndroidLocation.AndroidLocationException e) {
            throw new BuildException("Failed to get default debug keystore location.", e);
        }

        ZipAlign apkDebugTask = createApkTasks('Debug', debugSigningConfig)

        Install installDebugTask = project.tasks.create("androidInstall", Install)
        installDebugTask.description("Launch the application on a connected android device.")
        installDebugTask.conventionMapping.map("adbExe") { project.file("${project.jfxmobile.android.androidSdk}/platform-tools/adb${platformExtension()}") }
        installDebugTask.conventionMapping.map("apk") { apkDebugTask.outputFile }
        installDebugTask.dependsOn apkDebugTask
        androidTasks.add(installDebugTask)

        DefaultTask androidDebugTask = project.tasks.create("android", DefaultTask)
        androidDebugTask.description("Generates a debug Android apk containing the JavaFX application.")
        androidDebugTask.dependsOn apkDebugTask
        androidTasks.add(androidDebugTask)
        
        androidTasks.each {
            task -> task.group = ANDROID_TASKS_GROUP
        }
    }

    private ZipAlign createApkTasks(String variant, SigningConfig signingConfig) {
        ProcessResources processResourcesTask = project.tasks.create("processAndroidResources${variant}", ProcessResources)
        if ("Debug" == variant) {
            processResourcesTask.setDebuggable(true)
        }
        processResourcesTask.conventionMapping.map("manifest") { project.tasks.collectMultiDexComponents.manifest }
        processResourcesTask.conventionMapping.map("resDir") { project.tasks.mergeAndroidResources.outputDir }
        processResourcesTask.conventionMapping.map("assetsDir") { project.tasks.mergeAndroidAssets.outputDir }
        processResourcesTask.conventionMapping.map("packageOutputFile") { project.file("${project.jfxmobile.android.resourcesDirectory}/resources.ap_") }
        processResourcesTask.conventionMapping.map("aaptExe") { project.file("${project.jfxmobile.android.buildToolsDir}/aapt${platformExtension()}") }
        processResourcesTask.dependsOn project.tasks.processAndroidResources, project.tasks.mergeAndroidResources, project.tasks.mergeAndroidAssets
        androidTasks.add(processResourcesTask)

        Apk apkTask = project.tasks.create("apk${variant}", Apk)
        apkTask.conventionMapping.map("resourceFile") { processResourcesTask.packageOutputFile }
        apkTask.conventionMapping.map("dexDirectory") { project.file("${project.tasks.dex.outputDirectory}") }
        apkTask.conventionMapping.map("dexedLibraries") { Collections.<File> emptyList() }
        apkTask.conventionMapping.map("jniFolders") {
            project.files(
                "${project.jfxmobile.android.dalvikSdkLib}",
                "${project.jfxmobile.android.nativeDirectory}"
            ).files
        }
        apkTask.conventionMapping.map("outputFile") { project.file("${project.jfxmobile.android.installDirectory}/${project.name}-unaligned.apk") }
        apkTask.conventionMapping.map("mainResourcesDirectory") {
            def mainResourcesOutputDir = project.tasks.processResources.destinationDir
            mainResourcesOutputDir != null && mainResourcesOutputDir.isDirectory() ? mainResourcesOutputDir : null
        }
        apkTask.conventionMapping.map("androidResourcesDirectory") {
            def androidResourcesOutputDir = project.tasks.processAndroidResources.destinationDir
            androidResourcesOutputDir != null && androidResourcesOutputDir.isDirectory() ? androidResourcesOutputDir : null
        }
        apkTask.conventionMapping.map("signingConfig") {
            if (signingConfig.getStoreFile() != null) {
                signingConfig
            }
        }
        apkTask.conventionMapping.map("packagingOptions") { project.jfxmobile.android.packagingOptions }
        apkTask.dependsOn processResourcesTask, project.tasks.dex
        androidTasks.add(apkTask)

        if (signingConfig != null) {
            ValidateSigning validateSigningTask = project.tasks.create("validateSigning${variant}", ValidateSigning)
            validateSigningTask.signingConfig = signingConfig
            androidTasks.add(validateSigningTask)
            apkTask.dependsOn validateSigningTask
        }

        ZipAlign zipAlignTask = project.tasks.create("zipalign${variant}", ZipAlign)
        zipAlignTask.conventionMapping.map("inputFile") { apkTask.outputFile }
        zipAlignTask.conventionMapping.map("outputFile") { project.file("${project.jfxmobile.android.installDirectory}/${project.name}.apk") }
        zipAlignTask.conventionMapping.map("zipAlignExe") { project.file("${project.jfxmobile.android.buildToolsDir}/zipalign${platformExtension()}") }
        zipAlignTask.dependsOn apkTask
        androidTasks.add(zipAlignTask)

        return zipAlignTask
    }

    private void createIosTasks() {
        // NOTE: the from input is taken from the iosRuntime configuration, but can only be applied
        // when that configuration is completely configured. the from is applied above at a later
        // time after the project's taskGraph is ready
//        Sync extractNativeLibsTask = project.tasks.create("iosExtractNativeLibs", Sync) {
//            into project.file("${project.jfxmobile.ios.temporaryDirectory}")
//            include 'native/*.a'
//        }

        IosInstall iosInstallTask = project.tasks.create("iosInstall", IosInstall)
        iosInstallTask.description = "Install the application on a connected ios device."
        iosInstallTask.dependsOn([project.tasks.iosClasses])
        iosTasks.add(iosInstallTask)
        
        IosDevice iosDeviceTask = project.tasks.create("launchIOSDevice", IosDevice)
        iosDeviceTask.description = "Launch the application on a connected ios device."
        iosDeviceTask.dependsOn([project.tasks.iosClasses])
        iosTasks.add(iosDeviceTask)

        IosSimulator ipadSimulatorTask = project.tasks.create("launchIPadSimulator", IosSimulator)
        ipadSimulatorTask.description = "Launch the application on an iPad simulator."
        ipadSimulatorTask.dependsOn([project.tasks.iosClasses])
        iosTasks.add(ipadSimulatorTask)

        IosSimulator iphoneSimulatorTask = project.tasks.create("launchIPhoneSimulator", IosSimulator)
        iphoneSimulatorTask.description = "Launch the application on an iPhone simulator."
        iphoneSimulatorTask.dependsOn([project.tasks.iosClasses])
        iosTasks.add(iphoneSimulatorTask)

        CreateIpa createIpaTask = project.tasks.create("createIosApp", CreateIpa)
        createIpaTask.description = "Generates an iOS ipa containing the JavaFX application."
        createIpaTask.dependsOn([project.tasks.iosClasses])
        iosTasks.add(createIpaTask)

        iosTasks.each {
            task -> task.group = IOS_TASKS_GROUP
        }
    }

    private void createEmbeddedTasks() {
        project.task('copyEmbeddedDependencies', type: Copy) {
            into project.file("$project.buildDir/javafxports/embedded/libs")
            from project.configurations.embeddedRuntime
        }
        embeddedTasks.add(project.tasks.copyEmbeddedDependencies)

        project.task('embeddedJar', type: Jar) {
            from project.sourceSets.embedded.output
            from project.sourceSets.main.output
            destinationDir project.tasks.copyEmbeddedDependencies.destinationDir
        }
        project.tasks.embeddedJar.conventionMapping.map('manifest') {
            project.manifest {
                if (project.preloaderClassName != null && !project.preloaderClassName.empty) {
                    attributes(
                            'Main-Class': project.mainClassName,
                            'JavaFX-Preloader-Class': project.preloaderClassName
                    )
                } else {
                    attributes(
                            'Main-Class': project.mainClassName
                    )
                }
            }
        }
        embeddedTasks.add(project.tasks.embeddedJar)

        CopyRemoteDir copyJarTask = project.tasks.create("copyJarToEmbeddedPlatform", CopyRemoteDir)
        copyJarTask.conventionMapping.map('from') { project.tasks.copyEmbeddedDependencies.destinationDir }
        copyJarTask.conventionMapping.map('remotePlatform') { getRemotePlatformConfiguration() }
        copyJarTask.dependsOn project.tasks.embeddedJar, project.tasks.copyEmbeddedDependencies
        embeddedTasks.add(copyJarTask)

        RunEmbedded runEmbeddedTask = project.tasks.create("runEmbedded", RunEmbedded)
        runEmbeddedTask.description('Launch the application on a remote embedded platform.')
        runEmbeddedTask.conventionMapping.map('remotePlatform') { getRemotePlatformConfiguration() }
        runEmbeddedTask.dependsOn copyJarTask
        embeddedTasks.add(runEmbeddedTask)

        embeddedTasks.each {
            task -> task.group = EMBEDDED_TASKS_GROUP
        }
    }

    /**
     * Locates the android sdk and returns the android.jar for the configured
     * android platform. The android platform can be configured by setting the
     * compileSdkVersion. If no android sdk could be located, this method will
     * return <code>null</code>.
     * Please note that the returned file does not necessarily need to exist.
     */
    private File getPlatformAndroidJar() {
        String androidSdkLocation = locateAndroidSdk()
        if (androidSdkLocation != null) {
            return project.file("${androidSdkLocation}/platforms/android-${project.jfxmobile.android.compileSdkVersion}/android.jar")
        }
        return null
    }

    private void configureAndroid() {
        project.logger.info("Configuring Build for Android")

        if (project.jfxmobile.android.dalvikSdk == null) {
            project.jfxmobile.android.dalvikSdk = resolveSdk(project.configurations.dalvikSdk, "dalvik-sdk")
        }
        project.jfxmobile.android.dalvikSdkLib = project.file("${project.jfxmobile.android.dalvikSdk}/rt/lib")
        if (!project.jfxmobile.android.dalvikSdkLib.exists()) {
            project.jfxmobile.android.dalvikSdkLib = project.file("${project.jfxmobile.android.dalvikSdk}/modules_libs");
            if (!project.jfxmobile.android.dalvikSdkLib.exists()) {
                throw new GradleException("Configured dalvikSdk is invalid: ${project.jfxmobile.android.dalvikSdk}")
            }
        }
        project.logger.info("Using javafxports dalvik sdk from location ${project.jfxmobile.android.dalvikSdk}")

        // try and set the androidSdk extension value if it is not set
        project.jfxmobile.android.androidSdk = locateAndroidSdk()
        if (project.jfxmobile.android.androidSdk == null) {
            throw new GradleException("ANDROID_HOME not specified. Either set it as a gradle property, a system environment variable or directly in your build.gradle by setting the extension jfxmobile.android.androidSdk.")
        }
        project.logger.info("Using androidSdk from location: ${project.jfxmobile.android.androidSdk}")

        // check if android sdk points to correct directory by checking if the
        // configured androidSdk directory contains a build-tools subdirectory
        def buildToolsDir = project.file("${project.jfxmobile.android.androidSdk}/build-tools")
        if (!buildToolsDir.exists()) {
            throw new GradleException("Configured androidSdk is invalid: ${project.jfxmobile.android.androidSdk}")
        }

        // automatically figure out most recent build tools version if it is not
        // specified on extension
        if (project.jfxmobile.android.buildToolsVersion == null) {
            project.logger.info("There was no buildToolsVersion specified, looking for most recent installed version automatically")
            def maxRevisionDir = null
            def maxRevision = null
            buildToolsDir.eachDir { dir ->
                try {
                    def revision = Revision.parseRevision(dir.name)
                    if (revision.preview) {
                        project.logger.info("Ignoring directory ${dir.absolutePath} as it denotes a preview build tools version")
                    } else if (maxRevision == null || maxRevision.compareTo(revision) < 0) {
                        maxRevision = revision
                        maxRevisionDir = dir
                    }
                } catch (NumberFormatException ex) {
                    project.logger.info("Ignoring directory ${dir.absolutePath} as it does not denote a valid android build tools revision number")
                }
            }

            if (maxRevision == null) {
                throw new GradleException("No valid build tools version could be detected in ${project.jfxmobile.android.androidSdk}. Please check your androidSdk installation.")
            } else {
                project.jfxmobile.android.buildToolsVersion = maxRevisionDir.name
                project.logger.info("Using the following automatically detected buildToolsVersion: ${maxRevisionDir.name}")
            }
        }

        project.jfxmobile.android.buildToolsDir = project.file("${project.jfxmobile.android.androidSdk}/build-tools/${project.jfxmobile.android.buildToolsVersion}")
        project.jfxmobile.android.buildToolsLib = project.file("${project.jfxmobile.android.buildToolsDir}/lib")

        project.jfxmobile.android.validate()

        if (project.jfxmobile.android.applicationPackage == null) {
            def dotIndex = project.mainClassName.lastIndexOf('.')
            if (dotIndex != -1) {
                project.jfxmobile.android.applicationPackage = project.mainClassName[0..<dotIndex]
            } else {
                project.jfxmobile.android.applicationPackage = project.mainClassName
            }
        }
    }

    private String locateAndroidSdk() {
        if (project.jfxmobile.android.androidSdk == null) {
            // first see if there is a gradle property ANDROID_HOME
            if (project.hasProperty('ANDROID_HOME')) {
                return project.property('ANDROID_HOME')
            } else {
                // next see if there is a system environment variable ANDROID_HOME
                def envAndroidHome = System.env['ANDROID_HOME']
                if (envAndroidHome != null) {
                    return envAndroidHome
                }
            }
        }

        return project.jfxmobile.android.androidSdk
    }

    private void configureIos() {
        project.logger.info("Configuring build for iOS")

        if (project.jfxmobile.ios.iosSdk == null) {
            project.jfxmobile.ios.iosSdk = resolveSdk(project.configurations.iosSdk, "ios-sdk")
        }
        project.jfxmobile.ios.iosSdkLib = project.file("${project.jfxmobile.ios.iosSdk}/rt/lib")
        if (!project.jfxmobile.ios.iosSdkLib.exists()) {
            throw new GradleException("Configured iosSdk is invalid: ${project.jfxmobile.ios.iosSdk}")
        }
        project.logger.info("Using javafxports ios sdk from location ${project.jfxmobile.ios.iosSdk}")
    }

    private void configureEmbedded() {
        project.ant.taskdef(name: 'sshexec', classname: 'org.apache.tools.ant.taskdefs.optional.ssh.SSHExec',
                classpath: project.configurations.sshAntTask.asPath)
        project.ant.taskdef(name: 'scp', classname: 'org.apache.tools.ant.taskdefs.optional.ssh.Scp',
                classpath: project.configurations.sshAntTask.asPath)
    }

    private RemotePlatformConfiguration getRemotePlatformConfiguration() {
        if (project.hasProperty('remotePlatform')) {
            return project.jfxmobile.embedded.remotePlatforms.getByName(project.getProperty('remotePlatform'))
        } else {
            if (project.jfxmobile.embedded.remotePlatforms.size() == 0) {
                throw new GradleException("Can not execute embedded task because no remote platform was configured in jfxmobile.embedded.remotePlatforms")
            } else if (project.jfxmobile.embedded.remotePlatforms.size() == 1) {
                return project.jfxmobile.embedded.remotePlatforms.iterator().next()
            } else {
                RemotePlatformConfiguration first = project.jfxmobile.embedded.remotePlatforms.iterator().next()
                throw new GradleException("Multiple remote platforms are configured. Please specify which remote platform configuration to use by providing a gradle property with name 'remotePlatform', e.g.: gradle -PremotePlatform=${first.name} runEmbedded")
            }
        }
    }

    private String platformExtension() {
        def os = System.properties['os.name']
        if (os.startsWith('Windows')) {
            return '.exe'
        } else {
            return ''
        }
    }

    private String resolveSdk(Configuration configuration, String unpackedSubDirectory) {
        Set<File> files = configuration.resolve()
        if (!files.isEmpty()) {
            return unpackSdk(configuration.getAllDependencies().getAt(0), files.iterator().next(), unpackedSubDirectory).absolutePath
        }
        return null
    }

    private File unpackSdk(Dependency dependency, File archive, String unpackedSubDirectory) {
        final File unpackedDirectory = new File(archive.parent, "unpacked")
        final File unpackedDistDirectory = new File(unpackedDirectory, unpackedSubDirectory)

        if (unpackedDirectory.exists() && dependency.version.endsWith('-SNAPSHOT')) {
            unpackedDirectory.deleteDir()
        }

        if (!unpackedDirectory.exists()) {
            if (!unpackedDirectory.mkdirs()) {
                throw new GradleException("Unable to create base directory to unpack into: " + unpackedDirectory)
            }

            if (archive.name.endsWith(".zip")) {
                project.ant.unzip(src: archive, dest: unpackedDirectory)
            } else if (archive.name.endsWith(".tar.gz")) {
                project.ant.untar(src: archive, dest: unpackedDirectory, compression: 'gzip')
            }
            if (!unpackedDistDirectory.exists()) {
                throw new GradleException("Unable to unpack archive.")
            }

            File binDirectory = new File(unpackedDistDirectory, 'bin')
            if (binDirectory.exists()) {
                project.ant.chmod(dir: binDirectory, perm: '+x', includes: '*')
            }
        }

        return unpackedDistDirectory
    }

}
