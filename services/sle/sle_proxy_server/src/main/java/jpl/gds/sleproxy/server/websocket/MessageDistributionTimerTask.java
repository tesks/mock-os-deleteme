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
package jpl.gds.sleproxy.server.websocket;

import java.util.TimerTask;

/**
 * This TimerTask is used to flush event log messages to clients
 * based on an interval and using batches. If the web GUI client is 
 * blasted with a high rate of messages through the websocket connection, 
 * it can cause the User Interface to become unresponsive due to the single
 * threaded execution of Web Applications(Javascript) within the 
 * browser environment.
 * 
 */
public class MessageDistributionTimerTask extends TimerTask {

	private final MessageDistributor messageDistibutor;
	
	public MessageDistributionTimerTask() {
		messageDistibutor = MessageDistributor.INSTANCE;
	}

	@Override
	public void run() {
		
		try {
			messageDistibutor.notifyClients();
		} catch (final Exception e) {
			// Do nothing
		}
	}

}