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
package jpl.gds.compression.lzo;

/**
 * Class DecompressionException
 * 
 * MPCS-8180 - 07/20/2016 - Imported to AMPCS
 */
public class DecompressionException extends Exception {
	private static final long	serialVersionUID	= 5301399379952832093L;

	/**
	 * 
	 */
	public DecompressionException() {
		super();
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public DecompressionException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 */
	public DecompressionException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public DecompressionException(Throwable arg0) {
		super(arg0);
	}
}
