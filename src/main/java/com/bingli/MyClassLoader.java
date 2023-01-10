package com.bingli;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MyClassLoader extends URLClassLoader {
    private static volatile MyClassLoader instance;
    private final List<ClassLoader> classLoaderChain = new LinkedList();
    private final Map<String, ClassLoader> moduleClassLoaders = new HashMap();

    private MyClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public static MyClassLoader getInstance(ClassLoader parent) {
        if (instance == null) {
            synchronized(MyClassLoader.class) {
                if (instance == null) {
                    instance = new MyClassLoader(parent);
                }
            }
        }

        return instance;
    }

    public synchronized void addModuleClassLoader(ClassLoader loader, String moduleName) {
        this.classLoaderChain.add(loader);
        this.moduleClassLoaders.put(moduleName, loader);
    }

    public synchronized void removeModuleClassLoader(String moduleName) {
        ClassLoader classLoaderToRemove = this.moduleClassLoaders.get(moduleName);
        if (classLoaderToRemove != null) {
            this.classLoaderChain.remove(classLoaderToRemove);
            this.moduleClassLoaders.remove(moduleName);
        }

    }

    public void clear() {
        this.classLoaderChain.clear();
        this.moduleClassLoaders.clear();
    }

    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

        Class clz = null;
        if (getParent() != null) {
            try {

                clz = getParent().loadClass(name);
                if (clz != null) {
                    if (resolve) {
                        this.resolveClass(clz);
                    }
                    return clz;
                }
            } catch (ClassNotFoundException ex) {
            }
        }

        List<ClassLoader> tmpChain = new LinkedList();
        if (this.classLoaderChain.size() > 0) {
            synchronized(this) {
                tmpChain.addAll(this.classLoaderChain);
            }
        }

        Iterator classLoaderIterator = tmpChain.iterator();

        while(classLoaderIterator.hasNext()) {
            ClassLoader ccl = (ClassLoader)classLoaderIterator.next();
            try {
                clz = ccl.loadClass(name);
                if (clz != null) {
                    if (resolve) {
                        this.resolveClass(clz);
                    }

                    return clz;
                }
            } catch (ClassNotFoundException ex) {
            }
        }
        throw new ClassNotFoundException(name);
    }

}
