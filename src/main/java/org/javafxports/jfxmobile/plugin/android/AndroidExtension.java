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
package org.javafxports.jfxmobile.plugin.android;

import com.android.SdkConstants;
import com.android.build.gradle.internal.ExtraModelInfo;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.SdkHandler;
import com.android.build.gradle.internal.TaskContainerAdaptor;
import com.android.build.gradle.internal.TaskFactory;
import com.android.build.gradle.internal.dsl.PackagingOptions;
import com.android.build.gradle.internal.dsl.SigningConfig;
import com.android.build.gradle.internal.process.GradleJavaProcessExecutor;
import com.android.build.gradle.internal.process.GradleProcessExecutor;
import com.android.build.gradle.internal.scope.AndroidTaskRegistry;
import com.android.build.gradle.options.ProjectOptions;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.utils.FileCache;
import com.android.prefs.AndroidLocation;
import com.android.repository.Revision;
import com.android.utils.FileUtils;
import com.android.utils.StringHelper;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import java.io.File;
import java.util.Collections;

import static org.codehaus.groovy.runtime.ResourceGroovyMethods.deleteDir;

public class AndroidExtension {

    private final Project project;

    private String androidSdk;
    private String compileSdkVersion = "25";
    private String minSdkVersion = "4";
    private String targetSdkVersion;
    private boolean preview;
    private Revision minimalBuildToolsVersion = Revision.parseRevision(SdkConstants.CURRENT_BUILD_TOOLS_VERSION);
    private String buildToolsVersion;
    private File buildToolsDir;
    private File buildToolsLib;

    private String dalvikSdk;
    private File dalvikSdkLib;

    private String assetsDirectory = "src/android/assets";
    private String resDirectory = "src/android/res";
    private String nativeDirectory = "src/android/jniLibs";

    private File installDirectory;
    private File temporaryDirectory;
    private File resourcesDirectory;
    private File multidexOutputDirectory;
    private File dexOutputDirectory;

    private String applicationPackage;
    private String manifest;
    private String proguardFile;

    private final FileCache buildCache;

    private ExtraModelInfo extraModelInfo;

    private SigningConfig signingConfig;
    private PackagingOptions packagingOptions;
    private JFXMobileDexOptions dexOptions;

    private SdkHandler sdkHandler;
    private AndroidBuilder androidBuilder;
    private AndroidTaskRegistry androidTaskRegistry;
    private TaskFactory taskFactory;

    public AndroidExtension(Project project, ToolingModelBuilderRegistry registry) {
        this.project = project;

        this.extraModelInfo = new ExtraModelInfo(new ProjectOptions(project), project.getLogger());

        this.signingConfig = project.getExtensions().create("signingConfig", SigningConfig.class, "signing");
        this.packagingOptions = project.getExtensions().create("packagingOptions", PackagingOptions.class);
        this.dexOptions = project.getExtensions().create("dexOptions", JFXMobileDexOptions.class, extraModelInfo);

        try {
            this.buildCache = FileCache.getInstanceWithMultiProcessLocking(new File(AndroidLocation.getFolder(), "build-cache"));
        } catch (AndroidLocation.AndroidLocationException e) {
            throw new RuntimeException(e);
        }

        LoggerWrapper loggerWrapper = new LoggerWrapper(project.getLogger());

        this.sdkHandler = new SdkHandler(project, loggerWrapper);
        this.androidBuilder = new AndroidBuilder(
                project == project.getRootProject() ? project.getName() : project.getPath(),
                "JavaFX Mobile",
                new GradleProcessExecutor(project),
                new GradleJavaProcessExecutor(project),
                extraModelInfo,
                loggerWrapper,
                false);

        this.androidTaskRegistry = new AndroidTaskRegistry();
        this.taskFactory = new TaskContainerAdaptor(project.getTasks());

        installDirectory = new File(project.getBuildDir(), "javafxports/android");
        deleteDir(installDirectory);
        installDirectory.mkdirs();
        project.getLogger().info("Android install directory: " + installDirectory);

        temporaryDirectory = new File(project.getBuildDir(), "javafxports/tmp/android");
        deleteDir(temporaryDirectory);
        temporaryDirectory.mkdirs();
        project.getLogger().info("Android temporary output directory: " + temporaryDirectory);

        resourcesDirectory = new File(temporaryDirectory, "resources");
        resourcesDirectory.mkdirs();
        project.getLogger().info("Resources directory: " + resourcesDirectory);

        multidexOutputDirectory = new File(temporaryDirectory, "multi-dex");
        multidexOutputDirectory.mkdirs();
        project.getLogger().info("Multi-dex output directory: " + multidexOutputDirectory);

        dexOutputDirectory = new File(temporaryDirectory, "dex");
        dexOutputDirectory.mkdirs();
        project.getLogger().info("Dex output directory: " + dexOutputDirectory);
    }

    public Project getProject() {
        return project;
    }

    public String getAndroidSdk() {
        return androidSdk;
    }

    public void setAndroidSdk(String androidSdk) {
        this.androidSdk = androidSdk;
    }

    public String getCompileSdkVersion() {
        return compileSdkVersion;
    }

    public void setCompileSdkVersion(String compileSdkVersion) {
        this.compileSdkVersion = compileSdkVersion;
    }

    public String getMinSdkVersion() {
        if (minSdkVersion == null || minSdkVersion.isEmpty()) {
            return "4";
        }
        return minSdkVersion;
    }

    public void setMinSdkVersion(String minSdkVersion) {
        this.minSdkVersion = minSdkVersion;
    }

    public String getTargetSdkVersion() {
        if (targetSdkVersion == null || targetSdkVersion.isEmpty()) {
            return compileSdkVersion;
        }
        return targetSdkVersion;
    }

    public void setTargetSdkVersion(String targetSdkVersion) {
        this.targetSdkVersion = targetSdkVersion;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public Revision getMinimalBuildToolsVersion() {
        return minimalBuildToolsVersion;
    }

    public void setMinimalBuildToolsVersion(Revision minimalBuildToolsVersion) {
        this.minimalBuildToolsVersion = minimalBuildToolsVersion;
    }

    public String getBuildToolsVersion() {
        return buildToolsVersion;
    }

    public void setBuildToolsVersion(String buildToolsVersion) {
        this.buildToolsVersion = buildToolsVersion;
    }

    public Revision getBuildToolsRevision() {
        return Revision.parseRevision(buildToolsVersion);
    }

    public File getBuildToolsDir() {
        return buildToolsDir;
    }

    public void setBuildToolsDir(File buildToolsDir) {
        this.buildToolsDir = buildToolsDir;
    }

    public File getBuildToolsLib() {
        return buildToolsLib;
    }

    public void setBuildToolsLib(File buildToolsLib) {
        this.buildToolsLib = buildToolsLib;
    }

    public String getDalvikSdk() {
        return dalvikSdk;
    }

    public void setDalvikSdk(String dalvikSdk) {
        this.dalvikSdk = dalvikSdk;
    }

    public File getDalvikSdkLib() {
        return dalvikSdkLib;
    }

    public void setDalvikSdkLib(File dalvikSdkLib) {
        this.dalvikSdkLib = dalvikSdkLib;
    }

    public String getAssetsDirectory() {
        return assetsDirectory;
    }

    public void setAssetsDirectory(String assetsDirectory) {
        this.assetsDirectory = assetsDirectory;
    }

    public String getResDirectory() {
        return resDirectory;
    }

    public void setResDirectory(String resDirectory) {
        this.resDirectory = resDirectory;
    }

    public String getNativeDirectory() {
        return nativeDirectory;
    }

    public void setNativeDirectory(String nativeDirectory) {
        this.nativeDirectory = nativeDirectory;
    }

    public File getInstallDirectory() {
        return installDirectory;
    }

    public void setInstallDirectory(File installDirectory) {
        this.installDirectory = installDirectory;
    }

    public File getTemporaryDirectory() {
        return temporaryDirectory;
    }

    public void setTemporaryDirectory(File temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
    }

    public File getResourcesDirectory() {
        return resourcesDirectory;
    }

    public void setResourcesDirectory(File resourcesDirectory) {
        this.resourcesDirectory = resourcesDirectory;
    }

    public File getMultidexOutputDirectory() {
        return multidexOutputDirectory;
    }

    public void setMultidexOutputDirectory(File multidexOutputDirectory) {
        this.multidexOutputDirectory = multidexOutputDirectory;
    }

    public File getDexOutputDirectory() {
        return dexOutputDirectory;
    }

    public void setDexOutputDirectory(File dexOutputDirectory) {
        this.dexOutputDirectory = dexOutputDirectory;
    }

    public FileCache getBuildCache() {
        return buildCache;
    }

    private File getGeneratedDirectory() {
        return new File(project.getBuildDir(), "generated");
    }

    private File getIntermediatesDirectory() {
        return new File(project.getBuildDir(), "intermediates");
    }

    public File getIncrementalDirectory(String name) {
        return FileUtils.join(
                getIntermediatesDirectory(),
                "incremental",
                name);
    }

    public File getResourceBlameLogDirectory() {
        return FileUtils.join(
                getIntermediatesDirectory(),
                StringHelper.toStrings(
                        "blame", "res", Collections.singletonList("android")));
    }

    private File getGeneratedResourcesDirectory(String name) {
        return FileUtils.join(
                getGeneratedDirectory(),
                StringHelper.toStrings(
                        "res",
                        name,
                        Collections.singletonList("android")));
    }

    public File getRenderscriptResOutputDirectory() {
        return getGeneratedResourcesDirectory("rs");
    }

    public File getGeneratedResOutputDirectory() {
        return getGeneratedResourcesDirectory("resValues");
    }

    public File getGeneratedPngsOutputDirectory() {
        return getGeneratedResourcesDirectory("pngs");
    }

    public String getApplicationPackage() {
        return applicationPackage;
    }

    public void setApplicationPackage(String applicationPackage) {
        this.applicationPackage = applicationPackage;
    }

    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    public String getProguardFile() {
        return proguardFile;
    }

    public void setProguardFile(String proguardFile) {
        this.proguardFile = proguardFile;
    }

    /**
     * Checks whether the properties on the android extension are valid after
     * everything was configured.
     */
    public void validate() {
        // check if android build tool version is at least minimal required version
        Revision revBuildToolsVersion = Revision.parseRevision(buildToolsVersion);
        if (minimalBuildToolsVersion.compareTo(revBuildToolsVersion) > 0) {
            throw new GradleException("Android buildToolsVersion should be at least version " + minimalBuildToolsVersion + ": currently using " + buildToolsVersion + " from " + buildToolsDir + ". See https://developer.android.com/studio/intro/update.html on how to update the build tools in the Android SDK.");
        }

        // check if valid android build tools exists
        if (!project.file(buildToolsDir + "/aapt").exists() &&
                !project.file(buildToolsDir + "/aapt.exe").exists()) {
            throw new GradleException("Configured buildToolsVersion is invalid: " + buildToolsVersion + " (" + buildToolsDir + ")");
        }

        // check if android platform exists
        if (!project.file(androidSdk + "/platforms/android-" + compileSdkVersion).exists()) {
            throw new GradleException("Configured compileSdkVersion is invalid: " + compileSdkVersion + " (" + androidSdk + "/platforms/android-" + compileSdkVersion + ")");
        }

//        sdkHandler.initTarget(AndroidTargetHash.getPlatformHashString(new AndroidVersion(Integer.parseInt(compileSdkVersion))),
//                getBuildToolsRevision(), Collections.emptyList(), androidBuilder, false);

        sdkHandler.ensurePlatformToolsIsInstalled(extraModelInfo);
    }

    public ExtraModelInfo getExtraModelInfo() {
        return extraModelInfo;
    }

    public SigningConfig getSigningConfig() {
        return signingConfig;
    }

    public PackagingOptions getPackagingOptions() {
        return packagingOptions;
    }

    public JFXMobileDexOptions getDexOptions() {
        return dexOptions;
    }

    public AndroidBuilder getAndroidBuilder() {
        return androidBuilder;
    }

    public AndroidTaskRegistry getAndroidTaskRegistry() {
        return androidTaskRegistry;
    }

    public TaskFactory getTaskFactory() {
        return taskFactory;
    }
}