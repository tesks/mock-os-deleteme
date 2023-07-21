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

import jpl.gds.shared.metadata.context.IContextKey;

/**
 * Simple POJO used as a worker key container within REST response objects
 * for JSON serialization
 */
public class WorkerId {

    private String id;
    private long key;
    private String host;
    private int fragment;

    /**
     * Constructor which takes the context key of a telemetry worker
     * as parameter
     *
     * @param contextKey The context key of worker
     */
    public WorkerId(final IContextKey contextKey) {
        this.id = contextKey.toString();
        this.key = contextKey.getNumber();
        this.host = contextKey.getHost();
        this.fragment = contextKey.getFragment();
    }

    /**
     * Get the worker id in string form: session/host/fragment
     * <br>Ex: 11/atb-ocio-7/1
     *
     * @return the worker id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the worker id
     *
     * @param id the worker id
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Get the session number from the worker id
     *
     * @return the session number
     */
    public long getKey() {
        return key;
    }

    /**
     * Set the session number of the worker id
     *
     * @param key the session number
     */
    public void setKey(final long key) {
        this.key = key;
    }

    /**
     * Get the session host
     *
     * @return the session host
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the session host
     *
     * @param host the session host
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Get the session fragment
     *
     * @return the session fragment
     */
    public int getFragment() {
        return fragment;
    }

    /**
     * Set the session fragment
     *
     * @param fragment the session fragment
     */
    public void setFragment(final int fragment) {
        this.fragment = fragment;
    }
}
