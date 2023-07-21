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
package jpl.gds.decom.algorithm;

/**
 * Definition class for static argument names that should be provided
 * to IDecommutator algorithms if possible. If any parameters pertaining
 * to the current telemetry context need to be passed to IDecommutators,
 * add an argument name here.
 *
 */
public class DecomArgs {

	/**
	 * Name for the SCLK argument an IDecommutator can use
	 * to tag telemetry values.
	 */
	public static final String SCLK = "SCLK";
}
