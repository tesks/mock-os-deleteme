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
package jpl.gds.tcapp.app.gui.fault;

/**
 * This exception is a wrapper exception through anywhere in the flow of the
 * Fault Injector Wizard where the user made an error such that the wizard
 * cannot advance to the previous or next pages or process the user's input.
 * 
 *
 */
public class FaultInjectorException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates an instance of FaultInjectorException.
	 */
	public FaultInjectorException() {
		super();
	}

	/**
	 * Creates an instance of FaultInjectorException.
	 * 
	 * @param message The description of this exception
	 * @param cause The item that caused this exception to occur
	 */
	public FaultInjectorException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates an instance of FaultInjectorException.
	 * 
	 * @param message The description of this exception
	 */
	public FaultInjectorException(final String message) {
		super(message);
	}

	/**
	 * Creates an instance of FaultInjectorException.
	 * 
	 * @param cause The item that caused this exception to occur
	 */
	public FaultInjectorException(final Throwable cause) {
		super(cause);
	}
}
