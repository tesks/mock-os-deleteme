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
package jpl.gds.globallad.data;

public class GlobalLadDataException extends Exception {

	private static final long serialVersionUID = 6041120698701624070L;

	/**
	 * 
	 */
	public GlobalLadDataException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public GlobalLadDataException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public GlobalLadDataException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public GlobalLadDataException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public GlobalLadDataException(Throwable cause) {
		super(cause);
	}
}
