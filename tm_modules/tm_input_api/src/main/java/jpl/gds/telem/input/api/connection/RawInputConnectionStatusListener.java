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
package jpl.gds.telem.input.api.connection;

/**
 * This interface defines the required methods for a class to receive updates on
 * Raw Input Connection status changes.
 * 
 *
 */
public interface RawInputConnectionStatusListener {
	/**
	 * Called when connection is lost
	 */
	public void onConnectionLost();

	/**
	 * Called when connection is gained
	 */
	public void onConnectionGained();
}
