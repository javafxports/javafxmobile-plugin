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

import com.android.build.gradle.tasks.ResourceException
import com.android.build.gradle.tasks.WorkerExecutorAdapter
import com.android.ide.common.res2.AssetMerger
import com.android.ide.common.res2.AssetSet
import com.android.ide.common.res2.MergedAssetWriter
import com.android.ide.common.res2.MergingException
import com.android.ide.common.workers.WorkerExecutorFacade
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

/**
 *
 * @author joeri
 */
class MergeAssets extends DefaultTask {

    @OutputDirectory
    File outputDir

    List<AssetSet> inputAssetSets

    private final WorkerExecutorFacade<MergedAssetWriter.AssetWorkParameters> workerExecutor;

    @Inject
    MergeAssets(WorkerExecutor workerExecutor) {
        this.workerExecutor = new WorkerExecutorAdapter<>(workerExecutor, MergedAssetWriter.AssetWorkAction);
    }

    @TaskAction
    void mergeAssets() {
        List<AssetSet> assetSets = getInputAssetSets()
        AssetMerger merger = new AssetMerger()

        try {
            assetSets.each {
                it.loadFromFiles(null)
                merger.addDataSet(it)
            }

            MergedAssetWriter writer = new MergedAssetWriter(getOutputDir(), workerExecutor)
            merger.mergeData(writer, false)
        } catch (MergingException ex) {
            throw new ResourceException(ex.message, ex)
        }
    }

}

