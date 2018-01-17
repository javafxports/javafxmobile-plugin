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

import com.android.build.gradle.internal.dsl.PackagingOptions
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.ide.common.signing.CertificateInfo
import com.android.ide.common.signing.KeystoreHelper
import com.android.sdklib.build.DuplicateFileException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author joeri
 */
class Apk extends DefaultTask {

    @InputFile
    File resourceFile

    @InputDirectory
    File dexDirectory

    @InputFiles
    Collection<File> dexedLibraries

    @InputFiles
    Collection<File> jniFolders

    @InputDirectory @Optional
    File mainResourcesDirectory
    @InputDirectory @Optional
    File androidResourcesDirectory

    @Nested @Optional
    SigningConfig signingConfig

    @Nested
    PackagingOptions packagingOptions

    @OutputFile
    File outputFile

    @TaskAction
    void apk() {
        SigningConfig signingConfig = getSigningConfig()

        CertificateInfo certificateInfo
        if (signingConfig != null && signingConfig.signingReady) {
                project.logger.info("apk: will be signing with keystore ${signingConfig.getStoreFile()}")
                certificateInfo = KeystoreHelper.getCertificateInfo(signingConfig.getStoreType(),
                    signingConfig.getStoreFile(), signingConfig.getStorePassword(),
                    signingConfig.getKeyPassword(), signingConfig.getKeyAlias())
                if (certificateInfo == null) {
                    throw new GradleException("Failed to read key from keystore")
                }
        } else {
            throw new GradleException("You need to configure a valid signingConfig when releasing an APK.")
        }

        try {
            ApkBuilder apkBuilder = new ApkBuilder(getOutputFile().absolutePath, getResourceFile().absolutePath,
                    null, certificateInfo.key, certificateInfo.certificate, getPackagingOptions(), null)

            getDexDirectory().listFiles().findAll {
                it.name.endsWith(".dex")
            }.each {
                apkBuilder.addFile(it, it.name)
            }

            if (getMainResourcesDirectory() != null) {
                apkBuilder.addSourceFolder(getMainResourcesDirectory())
            }
            if (getAndroidResourcesDirectory() != null) {
                apkBuilder.addSourceFolder(getAndroidResourcesDirectory())
            }

            // add resources for all jar dependencies, except for android platform's android.jar
            project.configurations.androidRuntime.filter {
                it.name.endsWith('.jar') && !it.name.endsWith('android.jar')
            }.each() {
                project.logger.info("apk: adding ${it} to packager.addResourcesFromJar()")
                apkBuilder.addResourcesFromJar(project.file(it))
            }

            if (getJniFolders() != null) {
                getJniFolders().each {
                    if (it.isDirectory()) {
                        apkBuilder.addNativeLibraries(it)
                    }
                }
            }

            apkBuilder.sealApk()
        } catch (DuplicateFileException e) {
            throw new GradleException(e.getMessage() + ": ${e.archivePath}\nFile 1: ${e.file1}\nFile 2: ${e.file2}")
        }
    }

}

