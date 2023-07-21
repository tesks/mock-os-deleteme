/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.shared.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.commons.lang3.ClassUtils;

/**
 * Tool kit for creating objects via reflection.
 *
 */
abstract public class ReflectionToolkit extends Object
{
    private static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];
    private static final Object[]   EMPTY_ARGS    = new Object[0];


    /**
     * Create an object via reflection through its default constructor. See
     * three-argument version for more comments.
     *
     * @param className Class name
     *
     * @return Object
     *
     * @throws ReflectionException Problem with reflection
     */
    public static Object createObject(final String className)
        throws ReflectionException
    {
        return createObject(className, EMPTY_CLASSES, EMPTY_ARGS);
    }
    
    /**
     * Create an object via reflection through its default constructor. See
     * three-argument version for more comments.
     *
     * @param klazz class of the object to create
     *
     * @return Object
     *
     * @throws ReflectionException Problem with reflection
     */
    public static Object createObject(final Class<?> klazz)
        throws ReflectionException
    {
        try {
            return klazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ReflectionException("Unable to create instance of class " + klazz.getName(), e);
        }
    }


    /**
     * Create an object via reflection through the appropriate constructor
     * defined by the class name and argument types (and argument values.)
     *
     * Because we use getDeclaredConstructor we can call protected and private
     * constructors as well as public ones.
     *
     * RuntimeException is caught separately so that FindBugs won't object
     * to the Exception catch.
     *
     * @param className Class name
     * @param argTypes  Array of class of arguments
     * @param args      Array of arguments
     *
     * @return Object
     *
     * @throws ReflectionException Problem with reflection
     */
    public static Object createObject(final String     className,
                                      final Class<?>[] argTypes,
                                      final Object[]   args)
        throws ReflectionException
    {
        try
        {
            final Class<?> classObj = Class.forName(className);
            
            return createObject(classObj, argTypes, args);
        }
        catch (final ClassNotFoundException e)
        {
            throw new ReflectionException("Class not found: '" +
                                              className        +
                                              "'",
                                          e);
        }
        catch (final RuntimeException e)
        {
            throw new ReflectionException("Can't create '" +
                                              className    +
                                              "' object: " +
                                              e,
                                          e);
        }
        catch (final Exception e)
        {
            throw new ReflectionException("Can't create '" +
                                              className    +
                                              "' object: " +
                                              e,
                                          e);
        }
    }
    
    /**
     * Create an object via reflection through the appropriate constructor
     * defined by the class object and argument types (and argument values.)
     *
     * Because we use getDeclaredConstructor we can call protected and private
     * constructors as well as public ones.
     *
     * RuntimeException is caught separately so that FindBugs won't object
     * to the Exception catch.
     *
     * @param classObj  Object class
     * @param argTypes  Array of class of arguments
     * @param args      Array of arguments
     *
     * @return Object
     *
     * @throws ReflectionException Problem with reflection
     * 
     */
    public static Object createObject(final Class<?>   classObj,
            final Class<?>[] argTypes,
            final Object[]   args)
                    throws ReflectionException
    {
        try
        {
            final Constructor<?> constructor = getConstructor(classObj, argTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        }
        catch (final InvocationTargetException e)
        {
            // Remove the useless wrapper to get the real exception
            final Throwable cause = e.getCause();
            throw new ReflectionException("Can't create '" +
                    classObj.getName()    +
                    "' object: " +
                    cause,
                    cause);
        }
        catch (final RuntimeException e)
        {
            throw new ReflectionException("Can't create '" +
                    classObj.getName()    +
                    "' object: " +
                    e,
                    e);
        }
        catch (final Exception e)
        {
            throw new ReflectionException("Can't create '" +
                    classObj.getName()    +
                    "' object: " +
                    e,
                    e);
        }
    }
    
    /**
     * Create an object via reflection using the given constructor and argument
     * values.
     *
     * RuntimeException is caught separately so that FindBugs won't object to
     * the Exception catch.
     * 
     * @param constructor the class Constructor method object
     * @param args
     *            Array of arguments
     *
     * @return Object
     *
     * @throws ReflectionException
     *             Problem with reflection
     * 
     */
    public static Object createObject(final Constructor<?> constructor, final Object[] args)
            throws ReflectionException {
        try {
            return constructor.newInstance(args);
        } catch (final InvocationTargetException e) {
            // Remove the useless wrapper to get the real exception

            final Throwable cause = e.getCause();

            throw new ReflectionException("Can't create '"
                    + constructor.getDeclaringClass().getName() + "' object: "
                    + cause, cause);
        } catch (final RuntimeException e) {
            throw new ReflectionException("Can't create '"
                    + constructor.getDeclaringClass().getName() + "' object: "
                    + e, e);
        } catch (final Exception e) {
            throw new ReflectionException("Can't create '"
                    + constructor.getDeclaringClass().getName() + "' object: "
                    + e, e);
        }
    }

    /**
     * Gets a Constructor object for the specified class object and argument
     * type list.
     * 
     * @param classObj
     *            class to get constructor for
     * @param argTypes
     *            types of the constructor arguments
     * @return Constructor
     * @throws ReflectionException
     *             if there is a problem with reflection
     * 
     */
    public static Constructor<?> getConstructor(final Class<?> classObj, final Class<?>[] argTypes) throws ReflectionException {
        Exception exception = null;
        try {
            final Constructor<?> constructor = classObj.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor;
        }
        catch (final Exception e) {
            exception = e;
            final Constructor<?>[] constructors = classObj.getConstructors();
            for (final Constructor<?> constructor : constructors) {
                if (ClassUtils.isAssignable(argTypes, constructor.getParameterTypes(), true)) {
                    constructor.setAccessible(true);
                    return constructor;
                }
            }
        }
        if (null != exception) {
            if (exception instanceof InvocationTargetException) {
                // Remove the useless wrapper to get the real exception
                final Throwable cause = exception.getCause();
                throw new ReflectionException("Can't find constructor: '" + classObj.getSimpleName() + "("
                        + Arrays.toString(argTypes) + "): " + cause, cause);
            }
            if (exception instanceof RuntimeException) {
                throw new ReflectionException("Can't find constructor: '" + classObj.getSimpleName() + "("
                        + Arrays.toString(argTypes) + "): " + exception, exception);
            }            
        }
        throw new ReflectionException(
                "Can't find constructor: '" + classObj.getSimpleName() + "(" + Arrays.toString(argTypes) + ")");
    }

    /**
     * Gets a Constructor object for the specified class name and argument type
     * list.
     * 
     * @param className
     *            name of the class to get constructor for
     * @param argTypes
     *            types of the constructor arguments
     * @return Constructor
     * @throws ReflectionException
     *             if there is a problem with reflection
     * 
     */
    public static Constructor<?> getConstructor(final String className,
            final Class<?>[] argTypes) throws ReflectionException {
        try {
            final Class<?> classObj = Class.forName(className);
            return getConstructor(classObj, argTypes);
        }
        catch (final Exception e) {
            throw new ReflectionException("Can't create '" + className
                    + "' constructor: " + e, e);
        }
    }
}
