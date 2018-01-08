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

import com.android.build.gradle.internal.LoggerWrapper
import com.android.builder.core.DefaultApiVersion
import com.android.builder.internal.InstallUtils
import com.android.builder.testing.ConnectedDeviceProvider
import com.android.builder.testing.api.DeviceConnector
import com.android.builder.testing.api.DeviceProvider
import com.android.ddmlib.IDevice
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author joeri
 */
class Install extends DefaultTask {

    @InputFile
    File adbExe

    @InputFile
    File apk

    int timeOut = 0
    
    @TaskAction
    void install() {
        boolean preview = project.jfxmobile.android.preview
        LoggerWrapper loggerWrapper = new LoggerWrapper(project.logger)

        DeviceProvider deviceProvider = new ConnectedDeviceProvider(getAdbExe())
        deviceProvider.init()

        boolean successFull = false
        for (DeviceConnector device : deviceProvider.getDevices()) {
            if (device.getState() != IDevice.DeviceState.UNAUTHORIZED) {
                if (preview || (device.getApiLevel()== 0) || InstallUtils.checkDeviceApiLevel(device, new DefaultApiVersion(1, null), loggerWrapper, project.name, 'debug')) {
                    if (getApk() != null) {
                        if (preview || (device.getApiLevel()== 0) || device.getApiLevel() >= 21) {
                            device.installPackages([ getApk() ], getTimeOut(), loggerWrapper)
                            successFull = true
                        } else {
                            device.installPackage(getApk(), getTimeOut(), loggerWrapper)
                            successFull = true
                        }
                    } else {
                        project.logger.lifecycle("Could not find apk file")
                    }
                }
            } else {
                project.logger.lifecycle("Skipping device '${device.getName()}' for '${project.name}:debug': Device not authorized, see http://developer.android.com/tools/help/adb.html#Enabling.")
            }
        }

        if (!successFull) {
            throw new GradleException("Failed to install on any devices.")
        } else {
            project.logger.quiet("Installed on device.")
        }

    }

}

