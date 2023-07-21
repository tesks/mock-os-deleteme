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
package jpl.gds.shared.interfaces;

/**
 * IService is an interface to be implemented by classes that act as services
 * within the downlink processor infrastructure.  These services are associated with 
 * features. Features are in turn responsible for starting and stopping services via
 * this interface.
 * 
 *
 */
public interface IService {

	/**
	 * Starts the DownlinkService. This is likely to involve subscribing to the 
	 * internal message context, opening files, reading dictionaries, and general 
	 * initialization of the service.
	 * 
	 * @return true if the service is successfully started, false if not
	 */
	public boolean startService();

	/**
	 * Stops the DownlinkService. This is likely to involve un-subscribing from the 
	 * internal message context, closing files, flushing queues, creating summaries, and 
	 * cleaning up memory.
	 */
	public void stopService();
}
