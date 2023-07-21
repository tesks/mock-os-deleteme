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
package jpl.gds.db.api.sql.fetch;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;

public class FetchConfiguration implements IFetchConfiguration {
    /**
     * 
     */
    private final FetchIdentifier                     id;

    /**
     * 
     */
    private final Map<List<Class<?>>, Constructor<?>> constructorCache;

    /**
     * 
     */
    private final Class<? extends IDbSqlFetch>        klass;

    /**
     * 
     */
    private final Class<? extends IDbSqlFetch>        returnType;

    /**
     * 
     */
    private final List<List<Class<?>>>                argLists;

    /**
     * @param id
     *            the Fetch Identifier for which this configuration is being
     *            created
     */
    public FetchConfiguration(final FetchIdentifier id) {
        this.id = id;
        this.constructorCache = null;
        this.klass = null;
        this.returnType = null;
        this.argLists = null;
    }

    /**
     * @param id
     *            the Fetch Identifier for which this configuration is being
     *            created
     * @param klass
     *            the name of the class being instantiated
     * @param returnType
     *            the return type of the fetch being instantiated (interface)
     * @param argLists
     *            the list of arguements for the constructor of this fetch class
     */
    public FetchConfiguration(final FetchIdentifier id,
            final Class<? extends IDbSqlFetch> klass,
            final Class<? extends IDbSqlFetch> returnType, final List<List<Class<?>>> argLists) {
        this.id = id;
        this.constructorCache = new HashMap<List<Class<?>>, Constructor<?>>();
        this.klass = klass;
        this.returnType = returnType;
        this.argLists = argLists;
    }

    /**
     * @return FetchIdentifier
     */
    @Override
    public FetchIdentifier getFetchIdentifier() {
        return this.id;
    }

    /**
     * @return Class Name
     */
    @Override
    public Class<? extends IDbSqlFetch> getFetchClass() {
        return klass;
    }

    /**
     * @return the Interface that will be returned from the factory to represent
     *         the class
     */
    @Override
    public Class<? extends IDbSqlFetch> getReturnType() {
        return returnType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.fetch.IFetchConfiguration#getArgsLists()
     */
    @Override
    public List<List<Class<?>>> getArgsLists() {
        return argLists;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.fetch.IFetchConfiguration#getArgList(java.lang.Object[
     * ])
     */
    @Override
    public Class<?>[] getArgList(final Object... args) {
        for (final List<Class<?>> potentialArgList : argLists) {
            if (potentialArgList.size() != args.length) {
                continue; // argument lists are of different lengths.
            }
            for (int i = 0; i < args.length; i++) {
                if (null == args[i]) {
                    final Class<?> clazz = potentialArgList.get(i);
                    args[i] = clazz.cast(args[i]);
                }
            }
            if (ClassUtils.isAssignable(getArgTypes(args), potentialArgList.toArray(new Class<?>[0]), true)) {
                return potentialArgList.toArray(new Class<?>[0]);
            }
        }
        throw new IllegalArgumentException("No contructor could be found for \"" + id.name()
                + "\" with a matching argument list: " + Arrays.toString(getArgTypes(args)));
    }

    /**
     * Convert an array of Objects into a parallel array of those Objects' class
     * types.
     * 
     * @param args
     *            the Array of Objects to classify
     * @return an array of Classes corresponding to the class of each element in
     *         the input Object array
     */
    @Override
    public Class<?>[] getArgTypes(final Object... args) {
        final Class<?>[] classArray = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (null != args[i]) {
                final Class<?> clazz = args[i].getClass();
                final Class<?> primitive = ClassUtils.wrapperToPrimitive(clazz);
                classArray[i] = (null == primitive) ? clazz : primitive;
            }
            else {
                classArray[i] = null;
            }
        }
        return classArray;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.fetch.IFetchConfiguration#getConstructor(java.lang.
     * Object[])
     */
    @Override
    public Constructor<?> getConstructor(final Object[] args) {
        final List<Class<?>> argTypes = new ArrayList<Class<?>>(args.length);
        for (int i = 0; i < args.length; i++) {
            if (null != args[i]) {
                final Class<?> clazz = args[i].getClass();
                final Class<?> primitive = ClassUtils.wrapperToPrimitive(clazz);
                argTypes.add(i, (null == primitive) ? clazz : primitive);
            }
            else {
                argTypes.add(i, null);
            }
        }
        return constructorCache.get(argTypes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.fetch.IFetchConfiguration#setConstructor(java.lang.
     * Object[], java.lang.reflect.Constructor)
     */
    @Override
    public void setConstructor(final Object[] args, final Constructor<?> ctor) {
        final List<Class<?>> argTypes = new ArrayList<Class<?>>(args.length);
        for (int i = 0; i < args.length; i++) {
            final Class<?> clazz = (null != args[i]) ? args[i].getClass() : ctor.getParameterTypes()[i];
            final Class<?> primitive = ClassUtils.wrapperToPrimitive(clazz);
            argTypes.add(i, (null == primitive) ? clazz : primitive);
        }
        constructorCache.put(argTypes, ctor);
    }
}
