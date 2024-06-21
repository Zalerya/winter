package com.itu.prom16.others;

public class Tools {
    public static boolean isCustomClass (Class clazz) {
        // Le chargeur de classes pour les bibliothèques standards de Java est null (Bootstrap ClassLoader)
        ClassLoader classLoader = clazz.getClassLoader();
        return classLoader != null;
    }
}