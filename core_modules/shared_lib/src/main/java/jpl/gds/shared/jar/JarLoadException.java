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
package jpl.gds.shared.jar;

/**
 * 
 * Thrown when a problem is encountered trying to add a JAR file
 * to the classpath once the JVM has already been started
 * (what could possibly go wrong there?).
 * 
 *
 */
@SuppressWarnings("serial")
public class JarLoadException extends RuntimeException
{
	/**
	 * Creates an instance of JarLoadException
	 */
	public JarLoadException()
	{
		super();
	}

	/**
	 * Creates an instance of JarLoadException
	 * 
	 * @param message The associated error message
	 * @param cause The exception that caused this one
	 */
	public JarLoadException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates an instance of JarLoadException
	 * 
	 * @param message The associated error message
	 */
	public JarLoadException(String message)
	{
		super(message);
	}

	/**
	 * Creates an instance of JarLoadException
	 * 
	 * @param cause The exception that caused this one
	 */
	public JarLoadException(Throwable cause)
	{
		super(cause);
	}
}
