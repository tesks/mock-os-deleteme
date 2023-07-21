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
import java.util.List;


public interface IFetchConfiguration {
    /**
     * @return FetchIdentifier
     */
    public FetchIdentifier getFetchIdentifier();
    
	/**
	 * @return Class Name
	 */
	public Class<? extends IDbSqlFetch> getFetchClass();

	/**
	 * @return the Interface that will be returned from the factory to represent
	 *         the class
	 */
	public Class<? extends IDbSqlFetch> getReturnType();

    /**
     * @return get a list of argument lists for all implemented constructors of
     *         the fetch to be instantiated
     */
	public List<List<Class<?>>> getArgsLists();

	/**
     * Looks up and returns a constructor that takes the specified arguments.
     * Supports primitive/object auto-boxing
     * 
     * @param args
     *            the argument list to match
     * @return a constructor that takes the specified arguments. Supports
     *         primitive/object auto-boxing
     */
	public Class<?>[] getArgList(final Object... args);

	/**
	 * Convert an array of Objects into a parallel array of those Objects' class
	 * types.
	 * 
	 * @param args
	 *            the Array of Objects to classify
	 * @return an array of Classes corresponding to the class of each element in
	 *         the input Object array
	 */
	public Class<?>[] getArgTypes(Object... args);

    /**
     * Get the constructor for a particular type and constructor argument set
     * 
     * @param args
     *            the constructor arguments
     * @return a Constructor for the class
     */
	public Constructor<?> getConstructor(Object[] args);

    /**
     * Set the constructor for a particular type and constructor argument list
     * 
     * @param args
     *            the constructor arguments
     * @param ctor
     *            the actual Constructor that satisfies the argument list
     */
	public void setConstructor(Object[] args, Constructor<?> ctor);
}
