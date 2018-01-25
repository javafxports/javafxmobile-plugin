package org.javafxports.retrobuffer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

public class ClassInfo {

    private ClassReader reader;
    private final int access;
    private final Type type;

    public ClassInfo(ClassReader cr) {
        this.reader = cr;
        this.access = cr.getAccess();
        this.type = Type.getObjectType(cr.getClassName());
    }

    public ClassReader getReader() {
        return reader;
    }

    public Type getType() {
        return type;
    }

    public boolean isClass() {
        return !isInterface();
    }

    public boolean isInterface() {
        return Util.isInterface(access);
    }
}
