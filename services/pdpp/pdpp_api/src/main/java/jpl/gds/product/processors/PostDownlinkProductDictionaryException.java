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


/**
 * This exception class is used when there is an issue with a dictionary while
 * performing post downlink product processing.
 * 
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public class PostDownlinkProductDictionaryException extends PostDownlinkProductProcessingException {
	private static final long	serialVersionUID	= -2582492592399301065L;

	public PostDownlinkProductDictionaryException() {
		super();
	}

	public PostDownlinkProductDictionaryException(String message, Throwable rootCause) {
		super(message, rootCause);
	}

	public PostDownlinkProductDictionaryException(String message) {
		super(message);
	}
}
