package org.javafxports.retrobuffer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Retrobuffer {

    public static void run(SystemPropertiesConfig config) throws IOException {
        Path inputDir = config.getInputDir();
        Path outputDir = config.getOutputDir();
        List<Path> classpath = config.getClasspath();

        Thread.currentThread().setContextClassLoader(new NonDelegatingClassLoader(asUrls(classpath)));

        ClassAnalyzer analyzer = new ClassAnalyzer();
        Transformer transformer = new Transformer();

        OutputDirectory outputDirectory = new OutputDirectory(outputDir);
        Files.walkFileTree(inputDir, new ClasspathVisitor() {
            @Override
            protected void visitClass(byte[] bytecode) {
                analyzer.analyze(bytecode);
            }

            @Override
            protected void visitResource(Path relativePath, byte[] content) throws IOException {
                outputDirectory.writeFile(relativePath, content);
            }
        });

        List<ClassInfo> interfaces = analyzer.getInterfaces();
        List<ClassInfo> classes = analyzer.getClasses();

        List<byte[]> transformed = new ArrayList<>();
        for (ClassInfo c : interfaces) {
            transformed.add(transformer.backport(c.getReader()));
        }
        for (ClassInfo c : classes) {
            transformed.add(transformer.backport(c.getReader()));
        }

        for (byte[] bytecode : transformed) {
            outputDirectory.writeClass(bytecode);
        }
    }

    private static URL[] asUrls(List<Path> classpath) {
        return classpath.stream()
                .map(Path::toUri)
                .map(Retrobuffer::uriToUrl)
                .toArray(URL[]::new);
    }

    private static URL uriToUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
