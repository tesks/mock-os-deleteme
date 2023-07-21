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
package jpl.gds.product.processors;

import jpl.gds.product.api.ProductException;


/**
 * This exception class is used when an error occurs during processing of
 * products during post downlink product processing
 * 
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public class PostDownlinkProductProcessingException extends ProductException {
	private static final long	serialVersionUID	= -6978331435730013027L;

	/**
	 * 
	 */
	public PostDownlinkProductProcessingException() {
		// empty
	}

	/**
	 * @param message
	 */
	public PostDownlinkProductProcessingException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param rootCause
	 */
	public PostDownlinkProductProcessingException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
