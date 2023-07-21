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
package ammos.datagen.config;

/**
 * This exception is thrown if the current data generator configuration is found
 * to be invalid.
 * 
 */
@SuppressWarnings("serial")
public class InvalidConfigurationException extends Exception {

	/**
	 * Creates an instance of InvalidConfigurationException.
	 * 
	 * @param string
	 *            An explanation of what happened.
	 */
	public InvalidConfigurationException(final String string) {

		super(string);
	}

	/**
	 * Creates an instance of InvalidConfigurationException.
	 * 
	 * @param message
	 *            An explanation of what happened.
	 * @param cause
	 *            The exception that caused this one.
	 */
	public InvalidConfigurationException(final String message,
			final Throwable cause) {

		super(message, cause);
	}

	/**
	 * Creates an instance of InvalidConfigurationException.
	 * 
	 * @param cause
	 *            The exception that caused this one.
	 */
	public InvalidConfigurationException(final Throwable cause) {

		super(cause);
	}
}
