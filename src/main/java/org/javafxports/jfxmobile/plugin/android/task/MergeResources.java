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
package org.javafxports.jfxmobile.plugin.android.task;

import com.android.build.gradle.internal.CombinedInput;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.aapt.AaptGeneration;
import com.android.build.gradle.internal.aapt.AaptGradleFactory;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.tasks.IncrementalTask;
import com.android.build.gradle.internal.tasks.TaskInputHelper;
import com.android.build.gradle.tasks.MergeManifests;
import com.android.build.gradle.tasks.ResourceException;
import com.android.build.gradle.tasks.WorkerExecutorAdapter;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.core.BuilderConstants;
import com.android.builder.internal.aapt.Aapt;
import com.android.builder.internal.aapt.v1.AaptV1;
import com.android.builder.png.VectorDrawableRenderer;
import com.android.builder.utils.FileCache;
import com.android.ide.common.blame.MergingLog;
import com.android.ide.common.blame.MergingLogRewriter;
import com.android.ide.common.blame.ParsingProcessOutputHandler;
import com.android.ide.common.blame.parser.ToolOutputParser;
import com.android.ide.common.blame.parser.aapt.Aapt2OutputParser;
import com.android.ide.common.blame.parser.aapt.AaptOutputParser;
import com.android.ide.common.process.LoggedProcessOutputHandler;
import com.android.ide.common.process.ProcessOutputHandler;
import com.android.ide.common.process.TeeProcessOutputHandler;
import com.android.ide.common.res2.FileStatus;
import com.android.ide.common.res2.FileValidity;
import com.android.ide.common.res2.GeneratedResourceSet;
import com.android.ide.common.res2.MergedResourceWriter;
import com.android.ide.common.res2.MergingException;
import com.android.ide.common.res2.QueueableResourceCompiler;
import com.android.ide.common.res2.ResourceMerger;
import com.android.ide.common.res2.ResourcePreprocessor;
import com.android.ide.common.res2.ResourceSet;
import com.android.ide.common.vectordrawable.ResourcesNotSupportedException;
import com.android.ide.common.workers.WorkerExecutorFacade;
import com.android.resources.Density;
import com.android.sdklib.BuildToolInfo;
import com.android.utils.FileUtils;
import com.android.utils.ILogger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.workers.WorkerExecutor;
import org.javafxports.jfxmobile.plugin.android.AndroidExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class MergeResources extends IncrementalTask {

    private File outputDir;

    private File generatedPngsOutputDir;

    /**
     * Optional file to write any publicly imported resource types and names to
     */
    private File publicFile;

    private boolean processResources;

    private boolean crunchPng;

    private boolean validateEnabled;

    private File blameLogFolder;

    private FileCache fileCache;

    private Supplier<Collection<File>> sourceFolderInputs;
    private Supplier<List<ResourceSet>> resSetSupplier;

    private List<ResourceSet> processedInputs;

    private ArtifactCollection libraries;

    private FileCollection renderscriptResOutputDir;
    private FileCollection generatedResOutputDir;

    private final FileValidity<ResourceSet> fileValidity = new FileValidity<>();

    private Supplier<BuildToolInfo> buildToolInfo;
    private int minSdk;

    private AaptGeneration aaptGeneration;

    private static Aapt makeAapt(
            BuildToolInfo buildToolInfo,
            AaptGeneration aaptGeneration,
            AndroidBuilder builder,
            boolean crunchPng,
            MergingLog blameLog) {
        ProcessOutputHandler teeOutputHandler =
                new TeeProcessOutputHandler(
                        blameLog != null
                                ? new ParsingProcessOutputHandler(
                                new ToolOutputParser(
                                        aaptGeneration == AaptGeneration.AAPT_V1
                                                ? new AaptOutputParser()
                                                : new Aapt2OutputParser(),
                                        builder.getLogger()),
                                new MergingLogRewriter(blameLog::find, builder.getErrorReporter()))
                                : new LoggedProcessOutputHandler(
                                new AaptGradleFactory.FilteringLogger(builder.getLogger())),
                        new LoggedProcessOutputHandler(new AaptGradleFactory.FilteringLogger(builder.getLogger())));

        return new AaptV1(
                builder.getProcessExecutor(),
                teeOutputHandler,
                buildToolInfo,
                new AaptGradleFactory.FilteringLogger(builder.getLogger()),
                crunchPng ? AaptV1.PngProcessMode.ALL : AaptV1.PngProcessMode.NO_CRUNCH,
                0);
    }

    @Override
    protected boolean isIncremental() {
        return true;
    }

    @Input
    public int getMinSdk() {
        return minSdk;
    }

    public void setMinSdk(int minSdk) {
        this.minSdk = minSdk;
    }

    /**
     * Release resource sets not needed any more, otherwise they will waste heap space for the
     * duration of the build.
     *
     * <p>This might be called twice when an incremental build falls back to a full one.
     */
    private void cleanup() {
        fileValidity.clear();
        processedInputs = null;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getRenderscriptResOutputDir() {
        return renderscriptResOutputDir;
    }

    @VisibleForTesting
    void setRenderscriptResOutputDir(FileCollection renderscriptResOutputDir) {
        this.renderscriptResOutputDir = renderscriptResOutputDir;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getGeneratedResOutputDir() {
        return generatedResOutputDir;
    }

    @VisibleForTesting
    void setGeneratedResOutputDir(FileCollection generatedResOutputDir) {
        this.generatedResOutputDir = generatedResOutputDir;
    }

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getLibraries() {
        if (libraries != null) {
            return libraries.getArtifactFiles();
        }

        return null;
    }

    void setLibraries(ArtifactCollection libraries) {
        this.libraries = libraries;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public Collection<File> getSourceFolderInputs() {
        return sourceFolderInputs.get();
    }

    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    @Input
    public boolean getCrunchPng() {
        return crunchPng;
    }

    @Optional
    @OutputFile
    public File getPublicFile() {
        return publicFile;
    }

    public void setPublicFile(File publicFile) {
        this.publicFile = publicFile;
    }

    // Synthetic input: the validation flag is set on the resource sets in ConfigAction.execute.
    @Input
    public boolean isValidateEnabled() {
        return validateEnabled;
    }

    public void setValidateEnabled(boolean validateEnabled) {
        this.validateEnabled = validateEnabled;
    }

    @OutputDirectory
    @Optional
    public File getBlameLogFolder() {
        return blameLogFolder;
    }

    public void setBlameLogFolder(File blameLogFolder) {
        this.blameLogFolder = blameLogFolder;
    }

    @OutputDirectory
    public File getGeneratedPngsOutputDir() {
        return generatedPngsOutputDir;
    }

    public void setGeneratedPngsOutputDir(File generatedPngsOutputDir) {
        this.generatedPngsOutputDir = generatedPngsOutputDir;
    }

    private final WorkerExecutorFacade<MergedResourceWriter.FileGenerationParameters>
            workerExecutorFacade;

    @Inject
    public MergeResources(WorkerExecutor workerExecutor) {
        this.workerExecutorFacade =
                new WorkerExecutorAdapter<>(workerExecutor, FileGenerationWorkAction.class);
    }

    @Override
    protected void doFullTaskAction() throws IOException {
        ResourcePreprocessor preprocessor = getPreprocessor();

        // this is full run, clean the previous output
        File destinationDir = getOutputDir();
        FileUtils.cleanOutputDir(destinationDir);

        List<ResourceSet> resourceSets = getConfiguredResourceSets(preprocessor);

        // create a new merger and populate it with the sets.
        ResourceMerger merger = new ResourceMerger(minSdk);
        MergingLog mergingLog =
                getBlameLogFolder() != null ? new MergingLog(getBlameLogFolder()) : null;

        try (QueueableResourceCompiler resourceCompiler =
                     processResources
                             ? makeAapt(
                             buildToolInfo.get(),
                             aaptGeneration,
                             getBuilder(),
                             crunchPng,
                             mergingLog)
                             : QueueableResourceCompiler.NONE) {

            for (ResourceSet resourceSet : resourceSets) {
                resourceSet.loadFromFiles(getILogger());
                merger.addDataSet(resourceSet);
            }

            MergedResourceWriter writer =
                    new MergedResourceWriter(
                            workerExecutorFacade,
                            destinationDir,
                            getPublicFile(),
                            mergingLog,
                            preprocessor,
                            resourceCompiler,
                            getIncrementalFolder(),
                            null,
                            null,
                            false,
                            getCrunchPng());

            merger.mergeData(writer, false /*doCleanUp*/);

            // No exception? Write the known state.
            merger.writeBlobTo(getIncrementalFolder(), writer, false);
        } catch (MergingException e) {
            System.out.println(e.getMessage());
            merger.cleanBlob(getIncrementalFolder());
            throw new ResourceException(e.getMessage(), e);
        } finally {
            cleanup();
        }
    }

    @Override
    protected void doIncrementalTaskAction(Map<File, FileStatus> changedInputs)
            throws IOException {
        ResourcePreprocessor preprocessor = getPreprocessor();

        // create a merger and load the known state.
        ResourceMerger merger = new ResourceMerger(minSdk);
        try {
            if (!merger.loadFromBlob(getIncrementalFolder(), true /*incrementalState*/)) {
                doFullTaskAction();
                return;
            }

            for (ResourceSet resourceSet : merger.getDataSets()) {
                resourceSet.setPreprocessor(preprocessor);
            }

            List<ResourceSet> resourceSets = getConfiguredResourceSets(preprocessor);

            // compare the known state to the current sets to detect incompatibility.
            // This is in case there's a change that's too hard to do incrementally. In this case
            // we'll simply revert to full build.
            if (!merger.checkValidUpdate(resourceSets)) {
                getLogger().info("Changed Resource sets: full task run!");
                doFullTaskAction();
                return;
            }

            // The incremental process is the following:
            // Loop on all the changed files, find which ResourceSet it belongs to, then ask
            // the resource set to update itself with the new file.
            for (Map.Entry<File, FileStatus> entry : changedInputs.entrySet()) {
                File changedFile = entry.getKey();

                merger.findDataSetContaining(changedFile, fileValidity);
                if (fileValidity.getStatus() == FileValidity.FileStatus.UNKNOWN_FILE) {
                    doFullTaskAction();
                    return;
                } else if (fileValidity.getStatus() == FileValidity.FileStatus.VALID_FILE) {
                    if (!fileValidity.getDataSet().updateWith(
                            fileValidity.getSourceFile(), changedFile, entry.getValue(),
                            getILogger())) {
                        getLogger().info(
                                String.format("Failed to process %s event! Full task run",
                                        entry.getValue()));
                        doFullTaskAction();
                        return;
                    }
                }
            }

            MergingLog mergingLog =
                    getBlameLogFolder() != null ? new MergingLog(getBlameLogFolder()) : null;

            try (QueueableResourceCompiler resourceCompiler =
                         processResources
                                 ? makeAapt(
                                 buildToolInfo.get(),
                                 aaptGeneration,
                                 getBuilder(),
                                 crunchPng,
                                 mergingLog)
                                 : QueueableResourceCompiler.NONE) {

                MergedResourceWriter writer =
                        new MergedResourceWriter(
                                workerExecutorFacade,
                                getOutputDir(),
                                getPublicFile(),
                                mergingLog,
                                preprocessor,
                                resourceCompiler,
                                getIncrementalFolder(),
                                null,
                                null,
                                false,
                                getCrunchPng());

                merger.mergeData(writer, false /*doCleanUp*/);

                // No exception? Write the known state.
                merger.writeBlobTo(getIncrementalFolder(), writer, false);
            }
        } catch (MergingException e) {
            merger.cleanBlob(getIncrementalFolder());
            throw new ResourceException(e.getMessage(), e);
        } finally {
            cleanup();
        }
    }

    public static class FileGenerationWorkAction implements Runnable {

        private final MergedResourceWriter.FileGenerationWorkAction workAction;

        @Inject
        public FileGenerationWorkAction(MergedResourceWriter.FileGenerationParameters workItem) {
            this.workAction = new MergedResourceWriter.FileGenerationWorkAction(workItem);
        }

        @Override
        public void run() {
            workAction.run();
        }
    }

    private static class MergeResourcesVectorDrawableRenderer extends VectorDrawableRenderer {

        public MergeResourcesVectorDrawableRenderer(
                int minSdk,
                File outputDir,
                Collection<Density> densities,
                Supplier<ILogger> loggerSupplier) {
            super(minSdk, outputDir, densities, loggerSupplier);
        }

        @Override
        public void generateFile(File toBeGenerated, File original) throws IOException {
            try {
                super.generateFile(toBeGenerated, original);
            } catch (ResourcesNotSupportedException e) {
                // Add gradle-specific error message.
                throw new GradleException(
                        String.format(
                                "Can't process attribute %1$s=\"%2$s\": "
                                        + "references to other resources are not supported by "
                                        + "build-time PNG generation. "
                                        + "See http://developer.android.com/tools/help/vector-asset-studio.html "
                                        + "for details.",
                                e.getName(), e.getValue()));
            }
        }
    }

    private ResourcePreprocessor getPreprocessor() {
        // Only one pre-processor for now. The code will need slight changes when we add more.

        return new MergeResourcesVectorDrawableRenderer(
                getMinSdk(),
                getGeneratedPngsOutputDir(),
                Collections.emptyList(),
                LoggerWrapper.supplierFor(MergeResources.class));
    }

    private List<ResourceSet> getConfiguredResourceSets(ResourcePreprocessor preprocessor) {
        // it is possible that this get called twice in case the incremental run fails and reverts
        // back to full task run. Because the cached ResourceList is modified we don't want
        // to recompute this twice (plus, why recompute it twice anyway?)
        if (processedInputs == null) {
            processedInputs = computeResourceSetList();
            List<ResourceSet> generatedSets = Lists.newArrayListWithCapacity(processedInputs.size());

            for (ResourceSet resourceSet : processedInputs) {
                resourceSet.setPreprocessor(preprocessor);
                ResourceSet generatedSet = new GeneratedResourceSet(resourceSet);
                resourceSet.setGeneratedSet(generatedSet);
                generatedSets.add(generatedSet);
            }

            // We want to keep the order of the inputs. Given inputs:
            // (A, B, C, D)
            // We want to get:
            // (A-generated, A, B-generated, B, C-generated, C, D-generated, D).
            // Therefore, when later in {@link DataMerger} we look for sources going through the
            // list backwards, B-generated will take priority over A (but not B).
            // A real life use-case would be if an app module generated resource overrode a library
            // module generated resource (existing not in generated but bundled dir at this stage):
            // (lib, app debug, app main)
            // We will get:
            // (lib generated, lib, app debug generated, app debug, app main generated, app main)
            for (int i = 0; i < generatedSets.size(); ++i) {
                processedInputs.add(2 * i, generatedSets.get(i));
            }
        }

        return processedInputs;
    }

    /**
     * Compute the list of resource set to be used during execution based all the inputs.
     */
    List<ResourceSet> computeResourceSetList() {
        List<ResourceSet> sourceFolderSets = resSetSupplier.get();
        int size = sourceFolderSets.size() + 4;
        if (libraries != null) {
            size += libraries.getArtifacts().size();
        }

        List<ResourceSet> resourceSetList = Lists.newArrayListWithExpectedSize(size);

        // add at the beginning since the libraries are less important than the folder based
        // resource sets.
        // get the dependencies first
        if (libraries != null) {
            Set<ResolvedArtifactResult> libArtifacts = libraries.getArtifacts();
            // the order of the artifact is descending order, so we need to reverse it.
            for (ResolvedArtifactResult artifact : libArtifacts) {
                ResourceSet resourceSet =
                        new ResourceSet(
                                MergeManifests.getArtifactName(artifact),
                                null,
                                null,
                                validateEnabled);
                resourceSet.setFromDependency(true);
                resourceSet.addSource(artifact.getFile());

                // add to 0 always, since we need to reverse the order.
                resourceSetList.add(0,resourceSet);
            }
        }

        // add the folder based next
        resourceSetList.addAll(sourceFolderSets);

        // We add the generated folders to the main set
        List<File> generatedResFolders = Lists.newArrayList();

        generatedResFolders.addAll(renderscriptResOutputDir.getFiles());
        generatedResFolders.addAll(generatedResOutputDir.getFiles());

        // add the generated files to the main set.
        final ResourceSet mainResourceSet = sourceFolderSets.get(0);
        assert mainResourceSet.getConfigName().equals(BuilderConstants.MAIN);
        mainResourceSet.addSources(generatedResFolders);

        return resourceSetList;
    }

    /**
     * Obtains the temporary directory for {@code aapt} to use.
     *
     * @return the temporary directory
     */
    private File getAaptTempDir() {
        return FileUtils.mkdirs(new File(getIncrementalFolder(), "aapt-temp"));
    }

    public static class ConfigAction implements TaskConfigAction<MergeResources> {

        private final AndroidExtension androidExtension;
        private final String taskNamePrefix;
        private final File outputLocation;
        private final boolean includeDependencies;
        private final boolean processResources;

        public ConfigAction(
                AndroidExtension androidExtension,
                String taskNamePrefix,
                File outputLocation,
                boolean includeDependencies,
                boolean processResources) {
            this.androidExtension = androidExtension;
            this.taskNamePrefix = taskNamePrefix;
            this.outputLocation = outputLocation;
            this.includeDependencies = includeDependencies;
            this.processResources = processResources;
        }

        @Override
        public String getName() {
            return "mergeAndroidResources";
        }

        @Override
        public Class<MergeResources> getType() {
            return MergeResources.class;
        }

        @Override
        public void execute(MergeResources mergeResourcesTask) {
            final Project project = androidExtension.getProject();

            mergeResourcesTask.setMinSdk(Integer.parseInt(androidExtension.getMinSdkVersion()));
            mergeResourcesTask.buildToolInfo = () -> BuildToolInfo.fromStandardDirectoryLayout(
                    androidExtension.getBuildToolsRevision(), androidExtension.getBuildToolsDir());

            mergeResourcesTask.aaptGeneration = AaptGeneration.AAPT_V1;
            mergeResourcesTask.setAndroidBuilder(androidExtension.getAndroidBuilder());
            mergeResourcesTask.fileCache = androidExtension.getBuildCache();
            mergeResourcesTask.setVariantName("jfx");
            mergeResourcesTask.setIncrementalFolder(androidExtension.getIncrementalDirectory(getName()));
            // Libraries use this task twice, once for compilation (with dependencies),
            // where blame is useful, and once for packaging where it is not.
            if (includeDependencies) {
                mergeResourcesTask.setBlameLogFolder(androidExtension.getResourceBlameLogDirectory());
            }
            mergeResourcesTask.processResources = processResources;
            mergeResourcesTask.crunchPng = true;

            final boolean validateEnabled = true;

            mergeResourcesTask.setValidateEnabled(validateEnabled);

//            if (includeDependencies) {
//                mergeResourcesTask.libraries = scope.getArtifactCollection(
//                        RUNTIME_CLASSPATH, ALL, ANDROID_RES);
//            }

            mergeResourcesTask.resSetSupplier =
                    () -> {
                        ResourceSet mainResourceSet = new ResourceSet(BuilderConstants.MAIN, null, null, validateEnabled);
                        mainResourceSet.addSource(project.file(androidExtension.getResDirectory()));
                        return Collections.singletonList(mainResourceSet);
                    };
            mergeResourcesTask.sourceFolderInputs =
                    TaskInputHelper.bypassFileSupplier(
                            () -> Collections.singletonList(project.file(androidExtension.getResDirectory())));
            mergeResourcesTask.renderscriptResOutputDir = project.files(androidExtension.getRenderscriptResOutputDirectory());
            mergeResourcesTask.generatedResOutputDir = project.files(androidExtension.getGeneratedResOutputDirectory());

            mergeResourcesTask.setOutputDir(outputLocation);
            mergeResourcesTask.setGeneratedPngsOutputDir(androidExtension.getGeneratedPngsOutputDirectory());
        }
    }

    // Workaround for https://issuetracker.google.com/67418335
    @Override
    @Input
    public String getCombinedInput() {
        return new CombinedInput(super.getCombinedInput())
                .add("dataBindingLayoutInfoOutFolder", null)
                .add("publicFile", getPublicFile())
                .add("blameLogFolder", getBlameLogFolder())
                .add(
                        "mergedNotCompiledResourcesOutputDirectory",
                        null)
                .toString();
    }
}
