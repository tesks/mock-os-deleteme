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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * This Abstract class implements the WebSocketConfigurer interface,
 * defines the main URL path where the service will accept
 * websocket connections and sets the WebSocketHandler used
 * for handling the connection related callbacks.
 *
 */
public abstract class AbstractWebSocketConfig implements WebSocketConfigurer {

    /** Main URL where Service will listen for Web Socket Connection requests */
    private static final String DEFAULT_WEB_SOCKET_URL = "/websocket";

    /** The Web Socket Handler */
    protected WebSocketHandler handler;

    /**
     * Holds the Web Socket URL. Default is set to constant DEFAULT_WEB_SOCKET_URL
     * but can be overwritten by sub classes.
     **/
    protected String webSocketUrl;

    public AbstractWebSocketConfig() {
        this.webSocketUrl = DEFAULT_WEB_SOCKET_URL;
    }

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, webSocketUrl).setAllowedOrigins("*");
    }

    @Autowired
    public void setSocketHandler(final WebSocketHandler handler) {
        this.handler = handler;
    }
}