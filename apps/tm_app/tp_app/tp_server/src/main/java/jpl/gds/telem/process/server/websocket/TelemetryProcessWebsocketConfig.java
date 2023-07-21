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

package jpl.gds.telem.process.server.websocket;

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
 *
 */
@Configuration
@EnableWebSocket
@Controller
public class TelemetryProcessWebsocketConfig extends AbstractWebSocketConfig {}