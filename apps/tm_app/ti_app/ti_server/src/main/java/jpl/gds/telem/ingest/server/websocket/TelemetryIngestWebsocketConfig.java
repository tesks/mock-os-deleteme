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

package jpl.gds.telem.ingest.server.websocket;

import jpl.gds.common.websocket.AbstractWebSocketConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * This class handles the configuration of websocket connection
 * for the TI Server. Because of the changes made to TI to host/serve
 * the MC GUI code, it needs to override the default URL.
 *
 */
@Configuration
@EnableWebSocket
@Controller
public class TelemetryIngestWebsocketConfig extends AbstractWebSocketConfig {

    /** TI server URL where Service will listen for Web Socket Connection requests */
    private static final String INGEST_WEB_SOCKET_URL = "/ingest/websocket";

    public TelemetryIngestWebsocketConfig() {
        super();
        this.webSocketUrl = INGEST_WEB_SOCKET_URL;
    }
}
