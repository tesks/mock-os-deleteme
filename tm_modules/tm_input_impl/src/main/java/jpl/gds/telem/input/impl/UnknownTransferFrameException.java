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
package jpl.gds.telem.input.impl;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.telem.input.api.RawInputException;

/**
 * This exception is thrown when a Transfer Frame cannot be found in the
 * transfer_frame.xml file that satisfies the size and type requirements for the
 * received telemetry *
 */
@SuppressWarnings("serial")
public class UnknownTransferFrameException extends RawInputException {
	/**
	 * Basic No-Arg Constructor
	 */
	public UnknownTransferFrameException() {
		super();
	}

	/**
	 * Constructs a UnknownTransferFrameException with the given message.
	 * 
	 * @param message
	 *            the detailed error message
	 */
	public UnknownTransferFrameException(String message) {
		super(message);
	}

	/**
	 * Constructs a UnknownTransferFrameException with the given message and
	 * LogMessageType.
	 * 
	 * @param message
	 *            the detailed error message
	 * @param logMsg
	 *            the log message type
	 */
	public UnknownTransferFrameException(String message, LogMessageType logMsg) {
		super(message, logMsg);
	}

	/**
	 * Constructs a UnknownTransferFrameException with the given cause.
	 * 
	 * @param cause
	 *            the Throwable that triggered this exception
	 */
	public UnknownTransferFrameException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a UnknownTransferFrameException with the given message and
	 * cause.
	 * 
	 * @param message
	 *            the detailed error message
	 * @param cause
	 *            the Throwable that triggered this exception
	 */
	public UnknownTransferFrameException(String message, Throwable cause) {
		super(message, cause);
	}
}
