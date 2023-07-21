/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.common.websocket;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.SystemUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * This class implements the WebSocket connection handlers and
 * uses an instance of IWebSocketConnectionManager for connection
 * management.
 *
 */
@Component
public class WebSocketConnectionHandler implements WebSocketHandler {

    /** The Web Socket Connection Manager */
    private IWebSocketConnectionManager connectionManager;

    /** The Tracer object*/
    private Tracer trace;

    public WebSocketConnectionHandler(final Tracer trace) {
        this.trace = trace;
    }

    /**
     * Gets called after the websocket connection has gone through the handshake process
     * and the connection is established.
     *
     * @param session The WebSocketSession object
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        trace.debug("Established new websocket connection: ", session.toString());
        // Add this session to the connected clients list
        connectionManager.addSession(session);
    }

    /**
     * Gets called when the connected client sends a message.
     *
     * @param session The WebSocketSession object
     * @param message The WebSocketMessage object
     * @throws Exception
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        SystemUtilities.doNothing();
    }

    /**
     * Gets called when a transport error occurs on the websocket connection
     *
     * @param session The WebSocketSession object
     * @param exception The Throwable exception
     * @throws Exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        trace.error("Encountered a transport error for session: ",
                session.toString(), ExceptionTools.rollUpMessages(exception));
    }

    /**
     * Gets called after the connection has been closed.
     *
     * @param session The WebSocketSession object
     * @param closeStatus
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        trace.debug("Closed websocket connection: ", session.toString(), " ", closeStatus.toString());
        // Remove this session from the connected clients list
        connectionManager.removeSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    @Autowired
    public void setSocketConnectionManager(IWebSocketConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
}
