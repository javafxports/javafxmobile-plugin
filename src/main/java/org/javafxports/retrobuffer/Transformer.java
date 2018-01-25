package org.javafxports.retrobuffer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.util.function.Consumer;

public class Transformer {

    public byte[] backport(ClassReader reader) {
        return transform(reader, (next) -> {
            next = new UpdateBufferMethods(next);
            return next;
        });
    }

    private byte[] transform(ClassReader reader, ClassVisitorChain chain) {
        return transform(reader.getClassName(), cv -> reader.accept(cv, 0), chain);
    }

    private byte[] transform(String className, Consumer<ClassVisitor> reader, ClassVisitorChain chain) {
        try {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor next = writer;

            next = chain.wrap(next);

            reader.accept(next);
            return writer.toByteArray();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to backport class: " + className, t);
        }
    }

    private interface ClassVisitorChain {
        ClassVisitor wrap(ClassVisitor next);
    }
}
