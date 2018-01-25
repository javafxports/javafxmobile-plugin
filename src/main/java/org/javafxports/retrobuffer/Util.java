package org.javafxports.retrobuffer;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class Util {

    public static boolean hasFlag(int subject, int flag) {
        return (subject & flag) == flag;
    }

    public static boolean isInterface(int access) {
        return hasFlag(access, ACC_INTERFACE);
    }
}
