package org.javafxports.retrobuffer;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OutputDirectory {

    private final Path outputDir;

    public OutputDirectory(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void writeClass(byte[] bytecode) throws IOException {
        if (bytecode == null) {
            return;
        }
        ClassReader cr = new ClassReader(bytecode);
        Path relativePath = outputDir.getFileSystem().getPath(cr.getClassName() + ".class");
        writeFile(relativePath, bytecode);
    }

    public void writeFile(Path relativePath, byte[] content) throws IOException {
        Path outputFile = outputDir.resolve(relativePath);
        Files.createDirectories(outputFile.getParent());
        Files.write(outputFile, content);
    }
}
