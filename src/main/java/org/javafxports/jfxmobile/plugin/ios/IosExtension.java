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
package org.javafxports.jfxmobile.plugin.ios;

import com.gluonhq.higgs.Architecture;
import com.gluonhq.higgs.OS;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IosExtension {


    private final Project project;

    private String os = OS.ios.name();
    private String arch = Architecture.thumbv7.name();
    private List<String> ipaArchs;

    private List<String> forceLinkClasses = new ArrayList<>();
    private List<String> ignoreNativeLibs = new ArrayList<>();
    private List<String> runtimeModules = new ArrayList<>();
    private boolean smallIio = false; // true if the libjavafx_iio should not include libjpeg

    private File infoPList;

    private String iosSdk;
    private File iosSdkLib;

    private String assetsDirectory = "src/ios/assets";
    private String nativeDirectory = "src/ios/jniLibs";
    private List<String> frameworks = new ArrayList<>();
    private List<String> frameworksPaths = new ArrayList<>();

    private File installDirectory;
    private File temporaryDirectory;

    private boolean iosSkipSigning = false;
    private String iosSignIdentity;
    private String iosProvisioningProfile;

    private String apsEnvironment; // null | development | production

    public IosExtension(Project project) {
        this.project = project;

        installDirectory = new File(project.getBuildDir(), "javafxports/ios");
        project.getLogger().info("iOS install directory: " + installDirectory);

        temporaryDirectory = new File(project.getBuildDir(), "javafxports/tmp/ios");
        project.getLogger().info("iOS temporary output directory: " + temporaryDirectory);
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public List<String> getIpaArchs() {
        return ipaArchs;
    }

    public void setIpaArchs(List<String> ipaArchs) {
        this.ipaArchs = ipaArchs;
    }

    public List<String> getForceLinkClasses() {
        return forceLinkClasses;
    }

    public void setForceLinkClasses(List<String> forceLinkClasses) {
        this.forceLinkClasses = forceLinkClasses;
    }

    public File getInfoPList() {
        return infoPList;
    }

    public void setInfoPList(File infoPList) {
        this.infoPList = infoPList;
    }

    public String getIosSdk() {
        return iosSdk;
    }

    public void setIosSdk(String iosSdk) {
        this.iosSdk = iosSdk;
    }

    public File getIosSdkLib() {
        return iosSdkLib;
    }

    public void setIosSdkLib(File iosSdkLib) {
        this.iosSdkLib = iosSdkLib;
    }

    public String getAssetsDirectory() {
        return assetsDirectory;
    }

    public void setAssetsDirectory(String assetsDirectory) {
        this.assetsDirectory = assetsDirectory;
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

    public boolean isIosSkipSigning() {
        return iosSkipSigning;
    }

    public void setIosSkipSigning(boolean iosSkipSigning) {
        this.iosSkipSigning = iosSkipSigning;
    }

    public String getIosSignIdentity() {
        return iosSignIdentity;
    }

    public void setIosSignIdentity(String iosSignIdentity) {
        this.iosSignIdentity = iosSignIdentity;
    }

    public String getIosProvisioningProfile() {
        return iosProvisioningProfile;
    }

    public void setIosProvisioningProfile(String iosProvisioningProfile) {
        this.iosProvisioningProfile = iosProvisioningProfile;
    }

    public String getApsEnvironment() {
        return project.hasProperty("jfxmobile.ios.apsEnvironment") ?
                (String) project.getProperties().get("jfxmobile.ios.apsEnvironment") :
                apsEnvironment;
    }

    public void setApsEnvironment(String apsEnvironment) {
        this.apsEnvironment = apsEnvironment;
    }
    
    /**
     * @return the smallIio
     */
    public boolean isSmallIio() {
        return smallIio;
    }

    /**
     * @param smallIio the smallIio to set
     */
    public void setSmallIio(boolean smallIio) {
        this.smallIio = smallIio;
    }

    /**
     * @return the ignoreNativeLibs
     */
    public List<String> getIgnoreNativeLibs() {
        return ignoreNativeLibs;
    }

    /**
     * @param ignoreNativeLibs the ignoreNativeLibs to set
     */
    public void setIgnoreNativeLibs(List<String> ignoreNativeLibs) {
        this.ignoreNativeLibs = ignoreNativeLibs;
    }

    /**
     * @return the runtimeModules
     */
    public List<String> getRuntimeModules() {
        return runtimeModules;
    }

    /**
     * @param runtimeModules the runtimeModules to set
     */
    public void setRuntimeModules(List<String> runtimeModules) {
        this.runtimeModules = runtimeModules;
    }

    /**
     * 
     * @return the frameworks
     */
    public List<String> getFrameworks() {
        return frameworks;
    }

    /**
     * @param frameworks the name of the frameworks to set
     */
    public void setFrameworks(List<String> frameworks) {
        this.frameworks = frameworks;
    }

    /**
     * 
     * @return gets the directories that contain frameworks
     */
    public List<String> getFrameworksPaths() {
        return frameworksPaths;
    }

    /**
     * 
     * @param frameworksPaths the name of the paths that contain one or more 
     * frameworks
     */
    public void setFrameworksPaths(List<String> frameworksPaths) {
        this.frameworksPaths = frameworksPaths;
    }
}
