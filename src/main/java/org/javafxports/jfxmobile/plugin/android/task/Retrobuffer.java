package org.javafxports.jfxmobile.plugin.android.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.javafxports.jfxmobile.plugin.RetrobufferExec;

import java.io.File;
import java.util.Collections;

public class Retrobuffer extends DefaultTask {

    @InputFiles
    @Optional
    private FileCollection classpath;

    @InputDirectory
    private File retrobufferInput;

    @OutputDirectory
    private File retrobufferOutput;

    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    public File getRetrobufferInput() {
        return retrobufferInput;
    }

    public void setRetrobufferInput(File retrobufferInput) {
        this.retrobufferInput = retrobufferInput;
    }

    public File getRetrobufferOutput() {
        return retrobufferOutput;
    }

    public void setRetrobufferOutput(File retrobufferOutput) {
        this.retrobufferOutput = retrobufferOutput;
    }

    @TaskAction
    public void action() {
        RetrobufferExec exec = new RetrobufferExec(getProject());
        exec.setInputDir(getRetrobufferInput());
        exec.setOutputDir(getRetrobufferOutput());

        if (getClasspath() == null || getClasspath().isEmpty()) {
            exec.setRetrobufferClasspath(getProject().files(getRetrobufferInput()));
        } else {
            exec.setRetrobufferClasspath(getProject().files(getRetrobufferInput(), getClasspath()));
        }

        exec.setJvmArgs(Collections.emptyList());
        exec.exec();
    }

}
