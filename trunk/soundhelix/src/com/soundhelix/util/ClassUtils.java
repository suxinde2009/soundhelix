package com.soundhelix.util;

/**
 * Implements some static methods for classes.
 * 
 * @author Thomas Schürger (thomas@schuerger.com)
 */

public final class ClassUtils {
    
    private ClassUtils() {
    }
    
    /**
     * Creates an instance of the given class name using the nullary constructor. The class is expected
     * to be of type T.
     * 
     * @param clazz the class
     * @param className the class name
     * @param <T> the expected class type
     * @return the instance
     * 
     * @throws ClassNotFoundException 
     * @throws InstantiationException 
     * @throws IllegalAccessException 
     */
    
    public static <T> T newInstance(String className, Class<T> clazz)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return Class.forName(className).asSubclass(clazz).newInstance();
    }
}
