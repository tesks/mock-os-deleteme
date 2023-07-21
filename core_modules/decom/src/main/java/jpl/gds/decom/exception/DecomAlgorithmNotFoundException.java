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
package jpl.gds.decom.exception;

import jpl.gds.decom.algorithm.IDecommutator;
import jpl.gds.decom.algorithm.ITransformer;
import jpl.gds.decom.algorithm.IValidator;
import jpl.gds.shared.time.ISclkExtractor;

/**
 * This exception represents the failure to find any custom algorithm supported by generic
 * decommutation, including {@link ITransformer}, {@link IDecommutator}, {@link IValidator},
 * and {@link ISclkExtractor}
 *
 */
public class DecomAlgorithmNotFoundException extends DecomException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create an exception instance.
	 * @param message the error message describing the nature of the exception.
	 * @param cause the nested cause of the exception
	 */
	public DecomAlgorithmNotFoundException(String message, Exception cause) {
		super(message, cause);
	}

	/**
	 * Create an exception instance.
	 * @param message the error message describing the nature of the exception.
	 */
	public DecomAlgorithmNotFoundException(String message) {
		super(message);
	}
}

