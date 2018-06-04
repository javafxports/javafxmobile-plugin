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
            protected void visitClass(Path relativePath, byte[] bytecode) {
                try {
                    analyzer.analyze(bytecode);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Failed to analyze class: '" + relativePath + "'.\nClasses compiled with JDK 9 or later are currently not supported on Android. Please make sure that your project does not contain JDK 9+ dependencies.", e);
                }
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
