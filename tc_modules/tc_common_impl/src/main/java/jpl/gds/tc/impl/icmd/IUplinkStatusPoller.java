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

import java.util.List;

import jpl.gds.tc.api.ICpdUplinkStatus;

/**
 * This interface represents the command status poller. It provides all of the
 * methods required to initialize and operate the poller.
 * 
 * @since AMPCS R3
 */
public interface IUplinkStatusPoller {

	/**
	 * Initializes and starts the command status poller.
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
	 * Returns the list of request statuses that were polled recently.
	 * 
	 * @return An empty list if no request statuses have been polled, or a list
	 *         of request statuses that were last polled.
	 */
	public List<CpdUplinkStatus> getLastPolledRequestStatuses();
	
	/**
	 * Returns the request status of the specified request ID.
	 * 
	 * @param requestId
	 *            ICMD request ID to fetch the status for.
	 * @return Most-recently polled request status of the specified request ID.
	 */
	public ICpdUplinkStatus getLastPolledRequestStatus(String requestId);
	

	/**
	 * Removes an entry from the request status table (useful to keep memory
	 * footprint down, if no longer needed).
	 * 
	 * @param requestId
	 *            request ID of the status to purge
	 */
	public void purgeStatus(final String requestId);
}
