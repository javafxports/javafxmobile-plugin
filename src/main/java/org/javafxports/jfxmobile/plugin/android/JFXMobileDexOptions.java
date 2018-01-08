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

import com.android.build.gradle.internal.dsl.DexOptions;
import com.android.builder.core.ErrorReporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JFXMobileDexOptions extends DexOptions {

    private List<String> additionalParameters = new ArrayList<>();

    private boolean keepRuntimeAnnotatedClasses = true;

    public JFXMobileDexOptions(ErrorReporter errorReporter) {
        super(errorReporter);
    }

    public void additionalParameters(String... parameters) {
        additionalParameters = Arrays.asList(parameters);
    }

    @Override
    public List<String> getAdditionalParameters() {
        return additionalParameters;
    }

    @Override
    public void setAdditionalParameters(List<String> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public void keepRuntimeAnnotatedClasses(boolean keep) {
        this.keepRuntimeAnnotatedClasses = keep;
    }

    public boolean isKeepRuntimeAnnotatedClasses() {
        return keepRuntimeAnnotatedClasses;
    }

    @Override
    public void setKeepRuntimeAnnotatedClasses(boolean keepRuntimeAnnotatedClasses) {
        this.keepRuntimeAnnotatedClasses = keepRuntimeAnnotatedClasses;
    }
}
