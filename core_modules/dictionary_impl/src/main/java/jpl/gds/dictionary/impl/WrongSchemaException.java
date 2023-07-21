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
package jpl.gds.dictionary.impl;

import org.xml.sax.SAXException;

/**
 * This exception is to be thrown when any dictionary parser determines the XML
 * it is parsing may be using the wrong XML schema.
 * 
 */
public class WrongSchemaException extends SAXException {

	private static final long serialVersionUID = 1L;

	/**
	 * Basic Constructor.
	 * 
	 * @param message the detailed error message
	 */
	public WrongSchemaException(final String message) {
		super(message);
	}

	/**
	 * Constructor with exception as cause.
	 * 
	 * @param message the detailed error message
	 * @param cause the Exception that caused this Exception
	 */
	public WrongSchemaException(final String message, final Exception cause) {
		super(message, cause);
	}
}
