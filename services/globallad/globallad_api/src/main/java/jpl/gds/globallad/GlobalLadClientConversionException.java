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
package jpl.gds.globallad;

public class GlobalLadClientConversionException extends GlobalLadException {
	/**
	 * This class can be used to indicate any error that occurs when attempting to parse
	 * results returned by a global LAD query.
	 */

	public GlobalLadClientConversionException() {
	}

	/**
	 * @param message
	 */
	public GlobalLadClientConversionException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public GlobalLadClientConversionException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public GlobalLadClientConversionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public GlobalLadClientConversionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
