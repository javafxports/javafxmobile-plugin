package org.javafxports.retrobuffer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UpdateBufferMethods extends ClassVisitor {

    private static final Logger LOG = Logger.getLogger(UpdateBufferMethods.class.getName());

    private String methodOwner;

    private static final List<String> owners = Arrays.asList("java/nio/ByteBuffer",
            "java/nio/CharBuffer", "java/nio/DoubleBuffer", "java/nio/FloatBuffer",
            "java/nio/IntBuffer", "java/nio/LongBuffer", "java/nio/ShortBuffer",
            "java/nio/MappedByteBuffer");

    private static final List<String> names = Arrays.asList("clear", "flip",
            "limit", "mark", "position", "reset", "rewind");

    private static final List<MethodInstruction> methodInstructions = owners.stream()
            .map(owner -> names.stream()
                    .map(name -> new MethodInstruction(owner, name))
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    public UpdateBufferMethods(ClassVisitor next) {
        super(Opcodes.ASM5, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        this.methodOwner = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor next = super.visitMethod(access, methodName, desc, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM5, next) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKEVIRTUAL) {
                    MethodInstruction lookup = new MethodInstruction(owner, name, desc);
                    if (methodInstructions.contains(lookup)) {
                        LOG.log(Level.INFO, "Transforming java.nio.Buffer invocation in " + methodOwner + "." + methodName + ": " + owner + "." + name + " " + desc);
                        super.visitMethodInsn(opcode, "java/nio/Buffer", name, desc.substring(0, desc.indexOf("L") + 1) + "java/nio/Buffer;", itf);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        };
    }

    private static final class MethodInstruction {

        private final String owner;
        private final String name;
        private final String desc;

        public MethodInstruction(String owner, String name) {
            this.owner = owner;
            this.name = name;

            switch (name) {
                case "limit":
                case "position":
                    this.desc = "(I)L" + owner + ";";
                    break;
                default:
                    this.desc = "()L" + owner + ";";
                    break;
            }
        }

        public MethodInstruction(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodInstruction that = (MethodInstruction) o;
            return Objects.equals(owner, that.owner) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(desc, that.desc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, name, desc);
        }
    }
}
