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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.transforms.DesugarTransform;
import com.android.builder.Version;
import com.android.builder.core.DesugarProcessBuilder;
import com.android.builder.utils.FileCache;
import com.android.ide.common.internal.WaitableExecutor;
import com.android.ide.common.process.JavaProcessExecutor;
import com.android.ide.common.process.LoggedProcessOutputHandler;
import com.android.ide.common.process.ProcessException;
import com.android.utils.PathUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.javafxports.jfxmobile.plugin.JFXMobileExtension;
import org.javafxports.jfxmobile.plugin.android.AndroidExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DesugarTask extends DefaultTask {

    private enum FileCacheInputParams {

        /** The input file. */
        FILE,

        /** Version of the plugin containing Desugar used to generate the output. */
        PLUGIN_VERSION,

        /** Minimum sdk version passed to Desugar, affects output. */
        MIN_SDK_VERSION,
    }

    private static class InputEntry {
        @Nullable private final FileCache cache;
        @Nullable private final FileCache.Inputs inputs;
        @NonNull private final Path inputPath;
        @NonNull private final Path outputPath;

        public InputEntry(
                @Nullable FileCache cache,
                @Nullable FileCache.Inputs inputs,
                @NonNull Path inputPath,
                @NonNull Path outputPath) {
            this.cache = cache;
            this.inputs = inputs;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
        }

        @Nullable
        public FileCache getCache() {
            return cache;
        }

        @Nullable
        public FileCache.Inputs getInputs() {
            return inputs;
        }

        @NonNull
        public Path getInputPath() {
            return inputPath;
        }

        @NonNull
        public Path getOutputPath() {
            return outputPath;
        }
    }

    // we initialize this field only once, so having unsynchronized reads is fine
    private static final AtomicReference<Path> desugarJar = new AtomicReference<Path>(null);

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(DesugarTransform.class);

    private static final String DESUGAR_JAR = "desugar_deploy.jar";

    private File inputDir;
    private File tmpDir;
    private File outputDir;
    private Configuration androidRuntime;

    @NonNull private Supplier<Set<File>> androidJarClasspath;
    @NonNull private List<Path> compilationBootclasspath;
    private FileCache userCache;
    private int minSdk;
    @NonNull private JavaProcessExecutor executor;
    @NonNull private final WaitableExecutor waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool();
    private boolean verbose;

    @NonNull private Set<InputEntry> cacheMisses = Sets.newConcurrentHashSet();

    /**
     * Gradle's entry-point into this task. Determines whether or not it's possible to do this task
     * incrementally and calls either doIncrementalTaskAction() if an incremental build is possible,
     * and doFullTaskAction() if not.
     */
    @TaskAction
    void taskAction(IncrementalTaskInputs inputs) throws Exception {
        initDesugarJar(getProject().getExtensions().findByType(JFXMobileExtension.class).getAndroidExtension().getBuildCache());

        if (Files.notExists(inputDir.toPath())) {
            PathUtils.deleteIfExists(outputDir.toPath());
        } else {
            processSingle(inputDir.toPath(), outputDir.toPath(), Collections.emptySet());

        }
        waitableExecutor.waitForTasksWithQuickFail(true);

        processNonCachedOnes(getClasspath());
        waitableExecutor.waitForTasksWithQuickFail(true);
    }

    @InputDirectory
    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    @Input
    public int getMinSdk() {
        return minSdk;
    }

    public void setMinSdk(int minSdk) {
        this.minSdk = minSdk;
    }

    @InputDirectory
    public File getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    private void processNonCachedOnes(List<Path> classpath) throws IOException, ProcessException {
        int parallelExecutions = waitableExecutor.getParallelism();

        int index = 0;
        Multimap<Integer, InputEntry> procBuckets = ArrayListMultimap.create();
        for (InputEntry pathPathEntry : cacheMisses) {
            int bucketId = index % parallelExecutions;
            procBuckets.put(bucketId, pathPathEntry);
            index++;
        }

        List<Path> desugarBootclasspath = getBootclasspath();
        for (Integer bucketId : procBuckets.keySet()) {
            Callable<Void> callable =
                    () -> {
                        Map<Path, Path> inToOut = Maps.newHashMap();
                        for (InputEntry e : procBuckets.get(bucketId)) {
                            inToOut.put(e.getInputPath(), e.getOutputPath());
                        }

                        DesugarProcessBuilder processBuilder =
                                new DesugarProcessBuilder(
                                        desugarJar.get(),
                                        verbose,
                                        inToOut,
                                        classpath,
                                        desugarBootclasspath,
                                        minSdk,
                                        tmpDir.toPath());
                        boolean isWindows =
                                SdkConstants.currentPlatform() == SdkConstants.PLATFORM_WINDOWS;
                        executor.execute(
                                processBuilder.build(isWindows),
                                new LoggedProcessOutputHandler(logger))
                                .rethrowFailure()
                                .assertNormalExitValue();

                        // now copy to the cache because now we have the file
                        for (InputEntry e : procBuckets.get(bucketId)) {
                            if (e.getCache() != null && e.getInputs() != null) {
                                e.getCache()
                                        .createFileInCacheIfAbsent(
                                                e.getInputs(),
                                                in -> Files.copy(e.getOutputPath(), in.toPath()));
                            }
                        }

                        return null;
                    };
            waitableExecutor.execute(callable);
        }
    }

    @NonNull
    private List<Path> getClasspath() {
        ImmutableList.Builder<Path> classpathEntries = ImmutableList.builder();

        classpathEntries.addAll(androidRuntime.resolve().stream()
                .map(File::toPath)
                .iterator());

        List<Path> l = classpathEntries.build();
        l.forEach(System.out::println);
        return l;
    }

    @NonNull
    private List<Path> getBootclasspath() throws IOException {
        List<Path> desugarBootclasspath =
                androidJarClasspath.get().stream().map(File::toPath).collect(Collectors.toList());
        desugarBootclasspath.addAll(compilationBootclasspath);

        return desugarBootclasspath;
    }

    private void processSingle(
            @NonNull Path input, @NonNull Path output, @NonNull Set<? super QualifiedContent.Scope> scopes)
            throws Exception {
        waitableExecutor.execute(
                () -> {
                    if (output.toString().endsWith(SdkConstants.DOT_JAR)) {
                        Files.createDirectories(output.getParent());
                    } else {
                        Files.createDirectories(output);
                    }

                    FileCache cacheToUse;
                    if (Files.isRegularFile(input)
                            && Objects.equals(
                            scopes, Collections.singleton(QualifiedContent.Scope.EXTERNAL_LIBRARIES))) {
                        cacheToUse = userCache;
                    } else {
                        cacheToUse = null;
                    }

                    processUsingCache(input, output, cacheToUse);
                    return null;
                });
    }

    private void processUsingCache(
            @NonNull Path input,
            @NonNull Path output,
            @Nullable FileCache cache)
            throws Exception {
        if (cache != null) {
            try {
                FileCache.Inputs cacheKey = getBuildCacheInputs(input, minSdk);
                if (cache.cacheEntryExists(cacheKey)) {
                    FileCache.QueryResult result =
                            cache.createFile(
                                    output.toFile(),
                                    cacheKey,
                                    () -> {
                                        throw new AssertionError("Entry should exist.");
                                    });

                    if (result.getQueryEvent().equals(FileCache.QueryEvent.CORRUPTED)) {
                        Objects.requireNonNull(result.getCauseOfCorruption());
                        logger.verbose(
                                "The build cache at '%1$s' contained an invalid cache entry.\n"
                                        + "Cause: %2$s\n"
                                        + "We have recreated the cache entry.\n",
                                cache.getCacheDirectory().getAbsolutePath(),
                                Throwables.getStackTraceAsString(result.getCauseOfCorruption()));
                    }

                    if (Files.notExists(output)) {
                        throw new RuntimeException(
                                String.format(
                                        "Entry for %s is invalid. Please clean your build cache "
                                                + "under %s.",
                                        output.toString(),
                                        cache.getCacheDirectory().getAbsolutePath()));
                    }
                } else {
                    cacheMissAction(cache, cacheKey, input, output);
                }
            } catch (Exception exception) {
                logger.error(
                        null,
                        String.format(
                                "Unable to Desugar '%1$s' to '%2$s' using the build cache at"
                                        + " '%3$s'.\n",
                                input.toString(),
                                output.toString(),
                                cache.getCacheDirectory().getAbsolutePath()));
                throw new RuntimeException(exception);
            }
        } else {
            cacheMissAction(null, null, input, output);
        }
    }

    private void cacheMissAction(
            @Nullable FileCache cache,
            @Nullable FileCache.Inputs inputs,
            @NonNull Path input,
            @NonNull Path output)
            throws IOException, ProcessException {
        // add it to the list of cache misses, that will be processed
        cacheMisses.add(new InputEntry(cache, inputs, input, output));
    }

    public static class ConfigAction implements TaskConfigAction<DesugarTask> {

        private final AndroidExtension androidExtension;
        private final String taskNamePrefix;
        private final File inputLocation;
        private final File outputLocation;

        public ConfigAction(
                AndroidExtension androidExtension,
                String taskNamePrefix,
                File inputLocation,
                File outputLocation) {
            this.androidExtension = androidExtension;
            this.taskNamePrefix = taskNamePrefix;
            this.inputLocation = inputLocation;
            this.outputLocation = outputLocation;
        }

        @Override
        public String getName() {
            return "desugar";
        }

        @Override
        public Class<DesugarTask> getType() {
            return DesugarTask.class;
        }

        @Override
        public void execute(DesugarTask desugarTask) {
            String bcp = System.getProperty("sun.boot.class.path");
            desugarTask.androidJarClasspath = () -> androidExtension.getProject().getConfigurations().getByName("androidBootclasspath").resolve();
            if (bcp != null) {
                desugarTask.compilationBootclasspath = PathUtils.getClassPathItems(System.getProperty("sun.boot.class.path"));
            } else {
                desugarTask.compilationBootclasspath = Collections.emptyList();
            }
            desugarTask.userCache = androidExtension.getBuildCache();
            desugarTask.setMinSdk(Integer.parseInt(androidExtension.getMinSdkVersion()));

            desugarTask.androidRuntime = androidExtension.getProject().getConfigurations().getByName("androidRuntime");

            desugarTask.setInputDir(inputLocation);
            desugarTask.setTmpDir(androidExtension.getTemporaryDirectory());
            desugarTask.setOutputDir(outputLocation);

            desugarTask.verbose = androidExtension.getProject().getLogger().isInfoEnabled();

            desugarTask.executor = androidExtension.getAndroidBuilder().getJavaProcessExecutor();
        }
    }

    @NonNull
    private static FileCache.Inputs getBuildCacheInputs(@NonNull Path input, int minSdkVersion)
            throws IOException {
        FileCache.Inputs.Builder buildCacheInputs =
                new FileCache.Inputs.Builder(FileCache.Command.DESUGAR_LIBRARY);

        buildCacheInputs
                .putFile(
                        FileCacheInputParams.FILE.name(),
                        input.toFile(),
                        FileCache.FileProperties.PATH_HASH)
                .putString(
                        FileCacheInputParams.PLUGIN_VERSION.name(),
                        Version.ANDROID_GRADLE_PLUGIN_VERSION)
                .putLong(FileCacheInputParams.MIN_SDK_VERSION.name(), minSdkVersion);

        return buildCacheInputs.build();
    }

    /** Set this location of extracted desugar jar that is used for processing. */
    private static void initDesugarJar(@Nullable FileCache cache) throws IOException {
        if (isDesugarJarInitialized()) {
            return;
        }

        URL url = DesugarProcessBuilder.class.getClassLoader().getResource(DESUGAR_JAR);
        Preconditions.checkNotNull(url);

        Path extractedDesugar = null;
        if (cache != null) {
            try {
                String fileHash;
                try (HashingInputStream stream =
                             new HashingInputStream(Hashing.sha256(), url.openStream())) {
                    fileHash = stream.hash().toString();
                }
                FileCache.Inputs inputs =
                        new FileCache.Inputs.Builder(FileCache.Command.EXTRACT_DESUGAR_JAR)
                                .putString("pluginVersion", Version.ANDROID_GRADLE_PLUGIN_VERSION)
                                .putString("jarUrl", url.toString())
                                .putString("fileHash", fileHash)
                                .build();

                File cachedFile =
                        cache.createFileInCacheIfAbsent(
                                inputs, file -> copyDesugarJar(url, file.toPath()))
                                .getCachedFile();
                Preconditions.checkNotNull(cachedFile);
                extractedDesugar = cachedFile.toPath();
            } catch (IOException | ExecutionException e) {
                logger.error(e, "Unable to cache Desugar jar. Extracting to temp dir.");
            }
        }

        synchronized (desugarJar) {
            if (isDesugarJarInitialized()) {
                return;
            }

            if (extractedDesugar == null) {
                extractedDesugar = PathUtils.createTmpToRemoveOnShutdown(DESUGAR_JAR);
                copyDesugarJar(url, extractedDesugar);
            }
            desugarJar.set(extractedDesugar);
        }
    }

    private static void copyDesugarJar(@NonNull URL inputUrl, @NonNull Path targetPath)
            throws IOException {
        try (InputStream inputStream = inputUrl.openConnection().getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isDesugarJarInitialized() {
        return desugarJar.get() != null && Files.isRegularFile(desugarJar.get());
    }
}
