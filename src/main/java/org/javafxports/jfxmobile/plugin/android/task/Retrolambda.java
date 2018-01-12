package org.javafxports.jfxmobile.plugin.android.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.javafxports.jfxmobile.plugin.RetrolambdaExec;

import java.io.File;
import java.util.Collections;

/**
 * Applies retrolambda on the provided input and outputs to the provided output
 */
public class Retrolambda extends DefaultTask {

    @InputFiles
    @Optional
    private FileCollection classpath;

    @InputDirectory
    private File retrolambdaInput;

    @OutputDirectory
    private File retrolambdaOutput;

    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    public File getRetrolambdaInput() {
        return retrolambdaInput;
    }

    public void setRetrolambdaInput(File retrolambdaInput) {
        this.retrolambdaInput = retrolambdaInput;
    }

    public File getRetrolambdaOutput() {
        return retrolambdaOutput;
    }

    public void setRetrolambdaOutput(File retrolambdaOutput) {
        this.retrolambdaOutput = retrolambdaOutput;
    }

    @TaskAction
    public void action() {
        RetrolambdaExec exec = new RetrolambdaExec(getProject());
        exec.setInputDir(getRetrolambdaInput());
        exec.setOutputDir(getRetrolambdaOutput());

        exec.setBytecodeVersion(50);

        if (getClasspath() == null || getClasspath().isEmpty()) {
            exec.setRetrolambdaClasspath(getProject().files(getRetrolambdaInput()));
        } else {
            exec.setRetrolambdaClasspath(getProject().files(getRetrolambdaInput(), getClasspath()));
        }

        exec.setDefaultMethods(true);

        exec.setJvmArgs(Collections.emptyList());
        exec.exec();
    }
}
