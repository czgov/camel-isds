package com.github.czgov.isds.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;

/**
 * Created by jludvice on 17.2.17.
 */
public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    /**
     * Create new instance of given class using reflection.
     * @param clazz create new instance of this class
     * @param constructorArgs constructor arguments
     * @param <T> type of clazz
     * @return new instance of clazz
     * @throws Exception if required constructor isn't found
     */
    public static <T> T createInstance(Class<T> clazz, Object... constructorArgs) throws Exception {
        log.trace("Instantiating {} with params {}", clazz, Arrays.toString(constructorArgs));
        // get args type classes
        Class<?>[] types = new Class[constructorArgs.length];
        for (int i = 0; i < constructorArgs.length; i++) {
            types[i] = constructorArgs[i].getClass();
        }
        Optional<Constructor<?>> constructor = Arrays.asList(clazz.getConstructors()).stream()
                // only constructors with required arguments count
                .filter(c -> c.getParameterCount() == constructorArgs.length)
                .filter(c -> {
                    // filter constructors which have matching types for given constructor arguments
                    for (int i = 0; i < c.getParameterCount(); i++) {
                        if (!c.getParameterTypes()[i].isAssignableFrom(types[i])) {
                            return false;
                        }
                    }
                    return true;
                }).findFirst();
        if (constructor.isPresent()) {
            return (T) constructor.get().newInstance(constructorArgs);
        }
        throw new RuntimeException("Class " + clazz + " doesn't have constructor for " + Arrays.toString(constructorArgs));
    }
}
