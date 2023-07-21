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
package ammos.datagen.generators.seeds;

/**
 * This exception can be throw by generators if the seed data supplied cannot be
 * used to successfully initialize the generator.
 * 
 */
@SuppressWarnings("serial")
public class InvalidSeedDataException extends RuntimeException {

	/**
	 * Basic constructor.
	 */
	public InvalidSeedDataException() {

		// do nothing
	}

	/**
	 * Constructor with a message.
	 * 
	 * @param message
	 *            a detail message for the exception
	 */
	public InvalidSeedDataException(final String message) {

		super(message);
	}

	/**
	 * Constructor with a root cause.
	 * 
	 * @param cause
	 *            the root Throwable that caused this exception
	 */
	public InvalidSeedDataException(final Throwable cause) {

		super(cause);
	}

	/**
	 * Constructor with a message and a root cause.
	 * 
	 * @param message
	 *            a detail message for the exception
	 * @param cause
	 *            the root Throwable that caused this exception
	 */
	public InvalidSeedDataException(final String message, final Throwable cause) {

		super(message, cause);
	}
}
