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

package jpl.gds.telem.common.app.mc.rest.resources;

/**
 * Simple POJO used to construct JSON response for new session creation requests
 *
 */
public class SessionCreateResponse {

    private String message;
    private long key;
    private String host;

    /**
     * Constructor which takes the message, session key and session host
     * to initialize the response object
     *
     * @param message the message associated with session creation
     * @param key the session key
     * @param host the session host
     */
    public SessionCreateResponse(final String message, final long key, final String host) {
        this.message = message;
        this.key = key;
        this.host = host;
    }

    /**
     * Gets the message associated with session creation
     *
     * @return the string message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message associated with session creation
     *
     * @param message the string message
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Gets the session key
     *
     * @return the session key
     */
    public long getKey() {
        return key;
    }

    /**
     * Sets the session key
     *
     * @param key the session key
     */
    public void setKey(final long key) {
        this.key = key;
    }

    /**
     * Gets the session host
     *
     * @return the session host
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the session host
     *
     * @param host the string session host
     */
    public void setHost(final String host) {
        this.host = host;
    }
}
