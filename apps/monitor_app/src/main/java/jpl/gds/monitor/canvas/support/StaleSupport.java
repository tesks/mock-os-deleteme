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
package jpl.gds.monitor.canvas.support;

/**
 * This class must be implemented by all CanvasElement classes that 
 * support a stale data indicator.
 *
 */
public interface StaleSupport extends AbstractSupport {
	/**
	 * Checks to see if this field is stale, and sets the stale flag 
	 * if it is.
	 * @param staleInterval interval in seconds after which the 
	 *                      value is considered stale.
	 * @return true if the field is stale, false if not
	 */
	public boolean checkStale(int staleInterval);

	/**
	 * Clears the stale indicator.
	 */
	public void clearStale();

	/**
	 * Returns the stale indicator.
	 * @return true if the field value si stale, false if not
	 */
	public boolean isStale();
}
