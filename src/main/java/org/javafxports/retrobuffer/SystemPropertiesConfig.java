package org.javafxports.retrobuffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SystemPropertiesConfig {

    private static final String PREFIX = "retrobuffer.";
    private static final String CLASSPATH = PREFIX + "classpath";
    private static final String CLASSPATH_FILE = CLASSPATH + "File";
    private static final String INPUT_DIR = PREFIX + "inputDir";
    private static final String OUTPUT_DIR = PREFIX + "outputDir";

    private final Properties p;

    public SystemPropertiesConfig(Properties properties) {
        this.p = properties;
    }

    public Path getInputDir() {
        String inputDir = p.getProperty(INPUT_DIR);
        if (inputDir != null) {
            return Paths.get(inputDir);
        }
        throw new IllegalArgumentException("Missing required property: " + INPUT_DIR);
    }

    public Path getOutputDir() {
        String outputDir = p.getProperty(OUTPUT_DIR);
        if (outputDir != null) {
            return Paths.get(outputDir);
        }
        return getInputDir();
    }

    public List<Path> getClasspath() {
        String classpath = p.getProperty(CLASSPATH);
        if (classpath != null) {
            return parsePathList(classpath);
        }
        String classpathFile = p.getProperty(CLASSPATH_FILE);
        if (classpathFile != null) {
            return readPathList(Paths.get(classpathFile));
        }
        throw new IllegalArgumentException("Missing required property: " + CLASSPATH);
    }

    private static List<Path> parsePathList(String paths) {
        return Stream.of(paths.split(File.pathSeparator))
                .filter(path -> !path.isEmpty())
                .map(Paths::get)
                .collect(Collectors.toList());
    }

    private static List<Path> readPathList(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .filter(line -> !line.isEmpty())
                    .map(Paths::get)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }
}
