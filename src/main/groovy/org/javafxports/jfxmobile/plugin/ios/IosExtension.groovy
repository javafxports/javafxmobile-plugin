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
package org.javafxports.jfxmobile.plugin.ios

import com.gluonhq.higgs.Architecture
import com.gluonhq.higgs.OS
import org.gradle.api.Project

/**
 *
 * @author joeri
 */
class IosExtension {

    Project project

    /**
     * The fully qualified name of the iOS Launcher class.
     */
    String launcherClassName = 'org.javafxports.jfxmobile.ios.BasicLauncher'

    String os = OS.ios.name()
    String arch = Architecture.thumbv7.name()
    List<String> ipaArchs

    String propertiesFile
    String configFile

    String stdoutFifo
    String stderrFifo

    List<String> forceLinkClasses = []

    File infoPList

    String iosSdk
    File iosSdkLib

    String assetsDirectory = 'src/ios/assets'
    String nativeDirectory = 'src/ios/jniLibs'

    File installDirectory
    File temporaryDirectory

    boolean iosSkipSigning = false
    String iosSignIdentity
    String iosProvisioningProfile

    String apsEnvironment // null | development | production

    IosExtension(Project project) {
        this.project = project

        installDirectory = new File(project.buildDir, "javafxports/ios")
        project.logger.info("iOS install directory: " + installDirectory)

        temporaryDirectory = new File(project.buildDir, "javafxports/tmp/ios")
        project.logger.info("iOS temporary output directory: " + temporaryDirectory)
    }

    boolean isIosSkipSigning() {
        return iosSkipSigning
    }

    String getIosSignIdentity() {
        return iosSignIdentity
    }

    String getIosProvisioningProfile() {
        return iosProvisioningProfile
    }

    String getOs() {
        return os
    }

    String getArch() {
        return arch
    }

    String getApsEnvironment() {
        return project.hasProperty("jfxmobile.ios.apsEnvironment") ?
                project.properties.get("jfxmobile.ios.apsEnvironment") :
                apsEnvironment
    }
}
