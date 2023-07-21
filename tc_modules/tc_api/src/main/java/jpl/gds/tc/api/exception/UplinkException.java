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

package jpl.gds.tc.api.exception;

import jpl.gds.tc.api.IUplinkResponse;

/**
 * 
 * This exception is thrown when any type of transmission error (actual
 * network-related problems) occur when attempting to transmit (a.k.a.
 * "radiate") uplink.
 * 
 *
 */
public class UplinkException extends Exception {
	private static final long serialVersionUID = 1L;
	private IUplinkResponse uplinkResponse;

	/**
	 * Creates an instance of UplinkException.
	 */
	public UplinkException() {
		super();
	}

	/**
	 * Creates an instance of UplinkException
	 * 
	 * @param message An explanation of what happened.
	 */
	public UplinkException(final String message) {
		super(message);
	}

	/**
	 * Creates an instance of UplinkException
	 * 
	 * @param message An explanation of what happened.
	 * @param cause The exception that caused this one.
	 */
	public UplinkException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates an instance of UplinkException
	 * 
	 * @param cause The exception that caused this one.
	 */
	public UplinkException(final Throwable cause) {
		super(cause);
	}

	public UplinkException(final String message, final IUplinkResponse uplinkResponse) {
		super(message);
		this.uplinkResponse = uplinkResponse;
	}

	public UplinkException(final String message, final Throwable cause,
			final IUplinkResponse uplinkResponse) {
		super(message, cause);
		this.uplinkResponse = uplinkResponse;
	}

	public IUplinkResponse getUplinkResponse() {
		return this.uplinkResponse;
	}
}