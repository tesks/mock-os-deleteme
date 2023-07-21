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
package jpl.gds.common.config;

/**
 * CsvQueryPropertiesException is thrown when there is an error while loading a
 * CSV property set (columns and headers) from a csv_query.properties file.
 * 
 *
 */
public class CsvQueryPropertiesException extends Exception{
	
	private static final long serialVersionUID = 0L;

	/**
	 * Default constructor.
	 */
	protected CsvQueryPropertiesException(){
		super();
	}
	
	/**
	 * Constructs a CsvQueryPropertiesException with the given message.
	 * 
	 * @param message
	 *            a detailed message string
	 */
	protected CsvQueryPropertiesException(final String message){
		super(message);
	}
	
	/**
	 * Constructs a CsvQueryPropertiesException with the given root cause.
	 * 
	 * @param rootCause
	 *            the Throwable that triggered this exception
	 */
	protected CsvQueryPropertiesException(final Throwable rootCause){
		super(rootCause);
	}
	
	/**
	 * Constructs a CsvQueryPropertiesException with the given message and root
	 * cause
	 * 
	 * @param message
	 *            a detailed message string
	 * @param rootCause
	 *            the Throwable that triggered this exception
	 */
	protected CsvQueryPropertiesException(final String message, final Throwable rootCause){
		super(message, rootCause);
	}
}