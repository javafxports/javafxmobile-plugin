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

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.ConfigureUtil

import org.javafxports.jfxmobile.plugin.android.AndroidExtension
import org.javafxports.jfxmobile.plugin.embedded.EmbeddedExtension
import org.javafxports.jfxmobile.plugin.ios.IosExtension

/**
 *
 * @author joeri
 */
class JFXMobileExtension {

    String javacEncoding = 'utf-8'
    String javafxportsVersion = "8.60.10"

    DownConfiguration downConfig

    AndroidExtension androidExtension
    IosExtension iosExtension

    JFXMobileExtension(Project project, ObjectFactory objectFactory, ToolingModelBuilderRegistry registry) {
        androidExtension = extensions.create("android", AndroidExtension, project, registry)
        iosExtension = extensions.create("ios", IosExtension, project)
        extensions.create("embedded", EmbeddedExtension, project)

        downConfig = objectFactory.newInstance(DownConfiguration, project)
    }

    void downConfig(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, downConfig)
    }
}
