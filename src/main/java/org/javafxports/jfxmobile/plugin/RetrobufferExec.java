package org.javafxports.jfxmobile.plugin;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class RetrobufferExec {

    private static final int COMMANDLINE_LENGTH_LIMIT = 3496;

    private FileCollection retrobufferClasspath;
    private File inputDir;
    private File outputDir;
    private List<String> jvmArgs;

    private final Project project;

    public RetrobufferExec(Project project) {
        this.project = project;
    }

    public FileCollection getRetrobufferClasspath() {
        return retrobufferClasspath;
    }

    public void setRetrobufferClasspath(FileCollection retrobufferClasspath) {
        this.retrobufferClasspath = retrobufferClasspath;
    }

    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public Project getProject() {
        return project;
    }

    public void exec() {
        project.javaexec(exec -> {
            exec.setClasspath(project.getBuildscript().getConfigurations().getByName("classpath"));

            String path = retrobufferClasspath.getAsPath();

            exec.setMain("org.javafxports.retrobuffer.Main");
            exec.setJvmArgs(Arrays.asList(
                    "-Dretrobuffer.inputDir=" + inputDir,
                    "-Dretrobuffer.outputDir=" + outputDir,
                    "-Dretrobuffer.classpath=" + path
            ));

            if (classpathLengthGreaterThanLimit(path)) {
                try {
                    File classpathFile = File.createTempFile("inc-", ".path");
                    try (BufferedWriter writer = Files.newBufferedWriter(classpathFile.toPath(), StandardCharsets.UTF_8)) {
                        for (File item : this.retrobufferClasspath) {
                            writer.write(item.toString() + "\n");
                        }
                    }
                    classpathFile.deleteOnExit();
                    exec.getJvmArgs().add("-Dretrobuffer.classpathFile=" + classpathFile.getAbsolutePath());
                } catch (IOException e) {
                }
            } else {
                exec.getJvmArgs().add("-Dretrobuffer.classpath=" + path);
            }

            for (String arg : jvmArgs) {
                exec.getJvmArgs().add(arg);
            }
        });
    }

    private static boolean classpathLengthGreaterThanLimit(String path) {
        return path.length() > COMMANDLINE_LENGTH_LIMIT;
    }
}
