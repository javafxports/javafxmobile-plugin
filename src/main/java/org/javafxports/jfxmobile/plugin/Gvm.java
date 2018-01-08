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
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.javafxports.jfxmobile.plugin.ios.IosExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Gvm {

/*

    public static void build(Project project, String gvm, String target, String[] forcelinkClasses ) {
        boolean isLaunchOnDevice = "device".equals(target);
        Map<String, ?> properties = project.getProperties();

//        System.out.println("IOS Extension: ForceLink classes: ");
//        System.out.println(forcelinkClasses.getClass().getName());
//        for( String cls: forcelinkClasses) {
//            System.out.println(cls);
//        }

        ConfigurationContainer configurations = project.getConfigurations();
        Set<File> files = configurations.getByName("iosRuntime").resolve();
        // TODO: only include jars that are relevant to gvm: the built jar file + its dependencies
//        GvmBuilder gvmBuilder = GvmBuilder.create().buildDirName(project.getBuildDir().getAbsolutePath())
//                .rootdDirName(project.getRootDir().getAbsolutePath())
//                .jarDependencies(files.stream().map(f -> f.getAbsolutePath()).collect(Collectors.toList()))
//                .mainClassName((String) properties.get("mainClassName"))
//                .variant(gvm)
//                .target(target)
//                .appName(project.getName());
//            gvmBuilder.build();

        try {
            String rootDirName = project.getRootDir().getAbsolutePath();
            String appName = project.getName();
            String mainClassName = (String) properties.get("mainClassName");

//            String forcelinkClasses = configurations.
            String vm = "boson";
            BosonAppBuilder appBuilder = new BosonAppBuilder();
            appBuilder.rootDir(rootDirName).appId(mainClassName).appName(appName);
            appBuilder.jarDependencies(files.stream().map(f -> f.getAbsolutePath()).collect(Collectors.toList()));
            if (isLaunchOnDevice) {
                appBuilder.arch("arm64");
            }
            if (vm!= null) {
                appBuilder.vm(vm);
            }
            appBuilder.build();
            String launchDir = rootDirName + "/build/gvm/" + appName + ".app";
            if (isLaunchOnDevice) {
                appBuilder.launchOnDevice(launchDir);
            } else {
                appBuilder.launchOnSimulator(launchDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

*/

    public static void build(String target, Project project ) {
        System.out.println("PLUGIN: GVM BUILD CALLED, target = "+target);
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

        try {
            String vm = "boson";
                 //   System.out.println("GVM BUILDING!!!!!!!! classesfiles = "+classes.getFiles());

            BosonAppBuilder appBuilder = new BosonAppBuilder();
            appBuilder.vm(vm)
                    .rootDir(config.getRootDirName())
                    .classesDirs(classes)
                    .resourcesDirs(resources)
                    .appId(config.getMainClassName())
                    .appName(config.getAppName())
                    .forcelinkClasses(Arrays.asList(config.getForcelinkClasses()))
                    .jarDependencies( config.getJarDependecies());
            System.out.println("p = "+project);
            System.out.println("pex = "+project.getExtensions());
            System.out.println("pexios = "+project.getExtensions().findByType(JFXMobileExtension.class));
            System.out.println("pexiost = "+project.getExtensions().findByType(JFXMobileExtension.class).getIosExtension().getTemporaryDirectory());
            String tempDir =  project.getExtensions().findByType(JFXMobileExtension.class).getIosExtension().getTemporaryDirectory().getAbsolutePath();
            String nativeLibDir =  project.getExtensions().findByType(JFXMobileExtension.class).getIosExtension().getNativeDirectory();

            List<String> nativeLibs = new ArrayList<>();

            File nativeDir = new File(nativeLibDir);
            if (nativeDir.exists() && nativeDir.isDirectory()) {
                for (File nativeLib:   nativeDir.listFiles()) {
                    nativeLibs.add(nativeLib.getAbsolutePath());
                }
            }
            File nativeTmpDir = new File(tempDir, "native");
            if (nativeTmpDir.exists()) {
                for (File nativeLib:   nativeTmpDir.listFiles()) {
                    nativeLibs.add(nativeLib.getAbsolutePath());
                }
            }
            System.out.println("NATIVE LIBS = "+nativeLibs);
            appBuilder.nativeLibs(nativeLibs);

            if (isLaunchOnDevice) {
                appBuilder.arch("arm64");
            }
            System.out.println("Plugin will now build bosonappbuilder");
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
