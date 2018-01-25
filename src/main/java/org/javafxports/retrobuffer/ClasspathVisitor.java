package org.javafxports.retrobuffer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class ClasspathVisitor extends SimpleFileVisitor<Path> {

    private Path baseDir;

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (baseDir == null) {
            baseDir = dir;
        }
        return super.preVisitDirectory(dir, attrs);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relativePath = baseDir.relativize(file);
        byte[] content = Files.readAllBytes(file);

        if (isJavaClass(relativePath)) {
            visitClass(content);
        } else {
            visitResource(relativePath, content);
        }

        return FileVisitResult.CONTINUE;
    }

    protected abstract void visitClass(byte[] bytecode) throws IOException;

    protected abstract void visitResource(Path relativePath, byte[] bytecode) throws IOException;

    private static boolean isJavaClass(Path file) {
        String fileName = file.getFileName().toString();
        return fileName.endsWith(".class") && !fileName.equals("module-info.class");
    }
}
