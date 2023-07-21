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

package jpl.gds.tc.impl.icmd;


/**
 * This interface represents the CPD parameters poller. It provides all of the
 * methods required to initialize and operate the poller.
 * 
 * @since AMPCS R3
 */
public interface ICpdParametersPoller {

	/**
	 * Initializes and starts the CPD parameters poller.
	 * 
	 * @throws RuntimeException
	 *             Exception is thrown if an unrecoverable error is encountered
	 *             during initialization and startup.
	 */
	public void start() throws RuntimeException;

	/**
	 * Stops the poller.
	 * 
	 * @throws RuntimeException
	 *             Exception is thrown if an unrecoverable error is encountered
	 *             while trying to stop the poller.
	 */
	public void stop() throws RuntimeException;
	
	/**
	 * 
	 * Sets the polling interval.
	 * 
	 * @param milliseconds
	 *            Number of milliseconds between each poll to set.
	 * @throws IllegalArgumentException
	 *             Exception is thrown if the interval provided is illegal.
	 */
	public void setInterval(int milliseconds) throws IllegalArgumentException;

	/**
	 * Forces an immediate poll of the CPD parameters.
	 * 
	 * @throws RuntimeException
	 *             Exception is thrown if an unrecoverable error is encountered
	 *             while trying to force the poll.
	 */
	public void forcePoll() throws RuntimeException;
}
