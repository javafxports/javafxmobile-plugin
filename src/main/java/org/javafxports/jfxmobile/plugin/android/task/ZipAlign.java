package org.javafxports.jfxmobile.plugin.android.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Arrays;

public class ZipAlign extends DefaultTask {

    @InputFile
    private File zipAlignExe;

    @InputFile
    private File inputFile;
    @OutputFile
    private File outputFile;

    public File getZipAlignExe() {
        return zipAlignExe;
    }

    public void setZipAlignExe(File zipAlignExe) {
        this.zipAlignExe = zipAlignExe;
    }

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    @TaskAction
    void action() {
        getProject().exec(execSpec -> {
            execSpec.setExecutable(getZipAlignExe());
            execSpec.setArgs(Arrays.asList(
                    "-f", "4", getInputFile().getAbsolutePath(), getOutputFile().getAbsolutePath()
            ));
        });
    }
}
