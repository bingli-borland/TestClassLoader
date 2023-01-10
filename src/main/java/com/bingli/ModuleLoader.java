package com.bingli;

import java.net.URL;
import java.net.URLClassLoader;

public class ModuleLoader extends URLClassLoader {
    public ModuleLoader(URL[] urls) {
        super(urls);
    }

}