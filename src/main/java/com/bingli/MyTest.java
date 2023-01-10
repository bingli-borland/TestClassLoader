package com.bingli;

import java.io.File;
import java.net.URL;


public class MyTest {
    public static void main(String args[]) {
        test(args.length != 0);
    }

    public static boolean test(boolean gc) {

        // init env
        MyClassLoader.getInstance(ClassLoader.getSystemClassLoader()).clear();
        System.gc();

        try {
            System.out.println("*********************************************");
            //创建一个类加载器b1Loader，从b.jar里加载B类
            URL[] urlsB = new URL[1];
            urlsB[0] = new File("lib/b.jar").toURI().toURL();
            ModuleLoader b1Loader = new ModuleLoader(urlsB);
            System.out.println("b1 classloader: " + b1Loader.hashCode());
            MyClassLoader.getInstance(ClassLoader.getSystemClassLoader()).addModuleClassLoader(b1Loader, "ModuleB");
            //创建一个新的类加载器，从a.jar里加载A类
            URL[] urls = new URL[1];
            urls[0] = new File("lib/a.jar").toURI().toURL();
            new TestLoader(urls, MyClassLoader.getInstance(ClassLoader.getSystemClassLoader())).loadClass("A").newInstance();

            //移除b1Loader
            MyClassLoader.getInstance(ClassLoader.getSystemClassLoader()).removeModuleClassLoader("ModuleB");
            b1Loader = null;

            //触发GC
            if (gc) {
                System.gc();
                Thread.sleep(500);
            }

            //再次创建一个类加载器b2Loader，从b.jar里加载B类
            ModuleLoader b2Loader = new ModuleLoader(urlsB);
            System.out.println("b2 classloader: " + b2Loader.hashCode());
            MyClassLoader.getInstance(ClassLoader.getSystemClassLoader()).addModuleClassLoader(b2Loader, "B");

            ClassLoader delegate = MyClassLoader.getInstance(ClassLoader.getSystemClassLoader());
            System.out.println("Class.forName load B class, the classloader is : " + Class.forName("B", false, delegate).getClassLoader().hashCode());
            System.out.println("ClassLoader.loadClass load B class, the classloader is : " + delegate.loadClass("B").getClassLoader().hashCode());
            System.out.println("*********************************************");
            return Class.forName("B", false, delegate).getClassLoader().hashCode() == delegate.loadClass("B").getClassLoader().hashCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}