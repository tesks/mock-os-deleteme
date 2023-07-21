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

import jpl.gds.shared.log.AmpcsLog4jMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * Interface implemented by the Web Socket Connection Manager.
 * <p>The Web Socket Connection Manager is expected to exist as
 * a Singleton within a Spring Application Context and function
 * at the service layer. The initial implementation was done
 * for Telemetry Ingestor, Telemetry Processor and CFDP Services however,
 * the design is generic enough where it can be easily added to other
 * Spring Boot based AMPCS Services.
 *
 *
 */
public interface IWebSocketConnectionManager {

    /**
     * Send the information contained by an AmpcsLog4jMessage object to connected
     * clients.
     *
     * @param msg
     */
    void handleLogMessage(final AmpcsLog4jMessage msg);

    /**
     * Add Web Socket Session associated with the client connection
     * to the managed client list. This method would normally be used
     * when a new client connection is established with the server.
     *
     * @param session The WebSocketSession object
     */
    void addSession(final WebSocketSession session);

    /**
     * Remove the Web Socket Session associated with a client connection
     * from the managed client list. This method would normally be used
     * when the client connecting is closed.
     *
     * @param session
     */
    void removeSession(final WebSocketSession session);


    /**
     * Get the list of connected Web Socket Sessions
     *
     * @return The list of WebSocketSession objects
     */
    List<WebSocketSession> getSessions();
}
