package org.javafxports.retrobuffer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class ClassAnalyzer {

    private Map<Type, ClassInfo> classes = new HashMap<>();

    public void analyze(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        ClassInfo c = new ClassInfo(cr);

        classes.put(c.getType(), c);
    }

    public List<ClassInfo> getInterfaces() {
        return classes.values()
                .stream()
                .filter(ClassInfo::isInterface)
                .collect(toList());
    }

    public List<ClassInfo> getClasses() {
        return classes.values()
                .stream()
                .filter(ClassInfo::isClass)
                .collect(toList());
    }
}
