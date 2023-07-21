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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * This class manages client WebSocket connections.
 * Also starts the Message Distribution Timer Task.
 * 
 */
public enum ClientConnectionManager {
	INSTANCE;
	
	/**
	 * Map used to store client connections
	 */
	private final ConcurrentHashMap<String, IWebsocketClient> streamMap;
	
	/**
	 * Message Distribution Timer
	 */
	private Timer messageDistibutionTimer;
	
	/**
	 * Interval used to run the TimerTask which pushes log event messages
	 * to the GUI clients. Specified in seconds.
	 */
	private static final int DISTRIBUTION_TIMER_INTERVAL = 2;
	
	/**
	 * Each client connection is identified by a unique id. This property
	 * is used to specify the ID length in characters.
	 */
	private static final int ID_LENGTH = 30;
	
	private ClientConnectionManager() {
		streamMap = new ConcurrentHashMap<String, IWebsocketClient>();
	}
	
	/**
	 * Add new client to the internal map.
	 * 
	 * @param client
	 * 			Websocket endpoint client which implements the IWebsocketClient interface
	 */
	public synchronized void addNewClient(final IWebsocketClient client) {
		
		if (messageDistibutionTimer == null) {
			messageDistibutionTimer = new Timer("Message Distribution Timer Process");
			messageDistibutionTimer.schedule(new MessageDistributionTimerTask(), DISTRIBUTION_TIMER_INTERVAL * 1000, DISTRIBUTION_TIMER_INTERVAL * 1000);			
		}
		
		String newStreamId = generateUniqueId();

		while (streamMap.containsKey(newStreamId)) {
			newStreamId = generateUniqueId();
		}
		
		streamMap.put(newStreamId, client);
	}
	
	/**
	 * Remove client from the internal map.
	 * 
	 * @param client
	 * 			Websocket endpoint client which implements the IWebsocketClient interface
	 */
	public synchronized void removeClient(final IWebsocketClient client) {
		for (final Entry<String, IWebsocketClient> entry : streamMap.entrySet()) {
			if (entry.getValue().equals(client)) {
				streamMap.remove(entry.getKey());
			}
		}
	}
	
	private synchronized String generateUniqueId() {
		return RandomStringUtils.randomAlphanumeric(ID_LENGTH);
	}
	
	/**
	 * Get all connected clients
	 * 
	 * @return 
	 * 		A collection of all connected clients
	 */
	public Collection<IWebsocketClient> getClientList() {
		return streamMap.values();
	}
}
