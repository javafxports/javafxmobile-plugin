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

import com.gluonhq.gvmbuild.BosonAppBuilder;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Gvm {

    public static void build(String target, Project project ) {
        GvmConfig config = new GvmConfig(project);
        boolean isLaunchOnDevice = "device".equals(target);

        SourceSetContainer sourceSetContainer = (SourceSetContainer) project.getProperties().get("sourceSets");
        SourceSet mainSourceSet = sourceSetContainer.findByName("main");

        List<File> classes = new ArrayList<>();
        List<File> resources = new ArrayList<>();
        if (mainSourceSet != null) {
            classes.addAll(mainSourceSet.getOutput().getClassesDirs().getFiles());
            resources.add(mainSourceSet.getOutput().getResourcesDir());
        }
        SourceSet iosSourceSet = sourceSetContainer.findByName("ios");
        if (iosSourceSet != null) {
            classes.addAll(iosSourceSet.getOutput().getClassesDirs().getFiles());
            resources.add(iosSourceSet.getOutput().getResourcesDir());
        }

        String logLevelName = "info";
        if (project.hasProperty("gvmlog")) {
            String lll = (String)project.getProperties().get("gvmlog");
            logLevelName = lll;
        }
        try {
            String vm = "boson"; 
            BosonAppBuilder appBuilder = new BosonAppBuilder();
            appBuilder.vm(vm)
                    .rootDir(config.getRootDirName())
                    .classesDirs(classes)
                    .resourcesDirs(resources)
                    .appId(config.getMainClassName())
                    .appName(config.getAppName())
                    .logLevel(logLevelName)
                    .addRuntimeModules(Arrays.asList(config.getRuntimeModules()))
                    .forcelinkClasses(Arrays.asList(config.getForcelinkClasses()))
                    .jarDependencies(config.getJarDependecies());

            List<String> nativeLibs = new ArrayList<>();

            String nativeLibDir = config.getIos().getNativeDirectory();
            File nativeDir = project.file(nativeLibDir);
            project.getLogger().debug("Adding native libs from " + nativeDir.getAbsolutePath());
            if (nativeDir.exists() && nativeDir.isDirectory()) {
                File[] nativeFiles = nativeDir.listFiles();
                if (nativeFiles != null) {
                    for (File nativeFile : nativeFiles) {
                        nativeLibs.add(nativeFile.getAbsolutePath());
                    }
                }
            }
            File nativeTmpDir = new File(config.getIos().getTemporaryDirectory(), "native");
            project.getLogger().debug("Adding native libs from " + nativeTmpDir.getAbsolutePath());
            if (nativeTmpDir.exists()) {
                File[] nativeFiles = nativeTmpDir.listFiles();
                if (nativeFiles != null) {
                    for (File nativeFile : nativeFiles) {
                        nativeLibs.add(nativeFile.getAbsolutePath());
                    }
                }
            }
            appBuilder.nativeLibs(nativeLibs);

            if (isLaunchOnDevice) {
                appBuilder.arch("arm64");
            }
            appBuilder.build();
            if (isLaunchOnDevice) {
                appBuilder.launchOnDevice(config.getLaunchDir());
            } else {
                appBuilder.launchOnSimulator(config.getLaunchDir());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
