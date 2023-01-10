package com.bingli;

import java.net.URL;
import java.net.URLClassLoader;

public class TestLoader extends URLClassLoader {

    public TestLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @SuppressWarnings("resource")
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.equals("B")) {
            try {
                return Class.forName("B", false, getParent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return super.loadClass(name);
        }
        return null;
    }


}