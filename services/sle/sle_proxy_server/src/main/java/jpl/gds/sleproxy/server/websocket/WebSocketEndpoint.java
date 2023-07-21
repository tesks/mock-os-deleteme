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

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket Endpoint which serves as the entry point for WebSocket connections
 * 
 */
@ServerEndpoint(value = "/websocket")
public class WebSocketEndpoint implements IWebsocketClient {
	
	/**
	 * WebSocket session object
	 */
	private Session session;
	
	/**
	 * Called when the WebSocket connection is opened
	 * 
	 * @param session
	 * 			Session object associated with the WebSocket connection
	 */
	@OnOpen
	public void connectionOpened(final Session session) {
		this.session = session;
		ClientConnectionManager.INSTANCE.addNewClient(this);
	}
	
	/**
	 * Called when the WebSocket connection is closed
	 * 
	 * @param session
	 * 			Session object associated with the WebSocket connection
	 */
	@OnClose
	public void connectionClosed(final Session session) {
		ClientConnectionManager.INSTANCE.removeClient(this);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.sleproxy.server.websocket.IWebsocketClient#sendMessage(java.lang.String)
	 */
	@Override
	public void sendMessage(final String message) {
		try {
			if (session.isOpen()) {
				session.getBasicRemote().sendText(message);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}		
	}	
	
	
}
