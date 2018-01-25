package org.javafxports.retrobuffer;

import java.net.URL;
import java.net.URLClassLoader;

public class NonDelegatingClassLoader extends URLClassLoader {

    public NonDelegatingClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("java.")) { // the java.* classes can only be loaded by the bootstrap class loader
            return super.loadClass(name);
        }
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name);
        }
    }
}
