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
package jpl.gds.shared.channel;

/**
 * ChannelIdException is throws when an invalid channel ID is detected in the
 * EHA logic.
 *
 */
@SuppressWarnings("serial")
public class ChannelIdException extends RuntimeException {

	/**
	 * Default constructor.
	 */
	public ChannelIdException() {
		super();
	}

	/**
	 * Constructs a ChannelIdException with the given message.
	 * @param detail a detailed message string
	 */
	public ChannelIdException(final String detail) {
		super(detail);
	}

	/**
     * Constructs a ChannelIdException with the given message and root cause.
     * @param detail a detailed message string
     * @param cause the Throwable that triggered this exception
     */
	public ChannelIdException(final String detail, final Throwable cause) {
		super(detail, cause);
	}

	/**
     * Constructs a ChannelIdException with the given root cause.
     * @param cause the Throwable that triggered this exception
     */
	public ChannelIdException(final Throwable cause) {
		super(cause);
	}
}
