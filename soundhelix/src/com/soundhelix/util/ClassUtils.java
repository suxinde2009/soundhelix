package com.soundhelix.util;

/**
 * Implements some static methods for classes.
 * 
 * @author Thomas Schuerger (thomas@schuerger.com)
 */

public final class ClassUtils {

    /**
     * Private constructor.
     */

    private ClassUtils() {
    }

    /**
     * Creates an instance of the given class name using the nullary constructor. The class is expected to be of type T.
     * 
     * @param clazz the class
     * @param className the class name
     * @param <T> the expected class type
     * @return the instance
     * 
     * @throws ClassNotFoundException if the class could not be found
     * @throws InstantiationException if the class could not be instantiated
     * @throws IllegalAccessException if the class access was illegal
     */

    public static <T> T newInstance(String className, Class<T> clazz) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return Class.forName(className).asSubclass(clazz).newInstance();
    }
}
