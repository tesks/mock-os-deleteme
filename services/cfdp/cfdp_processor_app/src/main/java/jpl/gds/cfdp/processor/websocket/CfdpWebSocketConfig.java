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

package jpl.gds.cfdp.processor.websocket;

import jpl.gds.common.websocket.AbstractWebSocketConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Most of the Web Socket Configuration code is common code and
 * there really is not much being done besides setting the URL
 * where the service will accept Web Socket connections and the
 * connection handler, at least for the initial implementation.
 * See the Abstract parent class for details.
 * <p>However, since the common package is shared by a lot of
 * our codebase and most of modules have nothing to do with Web
 * Sockets, we can't have the following Spring annotations as part
 * of the common package:
 * <p>@Configuration, @EnableWebSocket, @Controller
 *
 */
@Configuration
@EnableWebSocket
@Controller
public class CfdpWebSocketConfig extends AbstractWebSocketConfig {
    // MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
    // MPCS-11266  - 09/17/19 : Most CFDP endpoints have lost their "/cfdp/" prefixes.
    /** CFDP server URL where Service will listen for Web Socket Connection requests */
    private static final String CFDP_WEB_SOCKET_URL = "/cfdp/websocket";

    public CfdpWebSocketConfig() {
        super();
        this.webSocketUrl = CFDP_WEB_SOCKET_URL;
    }
}