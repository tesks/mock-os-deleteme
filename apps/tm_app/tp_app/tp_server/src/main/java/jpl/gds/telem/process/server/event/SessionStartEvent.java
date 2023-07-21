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
package jpl.gds.telem.process.server.event;

import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionIdentification;
import jpl.gds.shared.metadata.context.IContextKey;
import org.springframework.context.ApplicationEvent;

/**
 * A session start event for use in the telemetry processor server.
 */
public class SessionStartEvent extends ApplicationEvent implements ISessionEvent {

    private IContextKey contextKey;

    /**
     * Constructor
     *
     * @param source     session configuration
     * @param contextKey the originating session's context key
     */
    public SessionStartEvent(SessionConfiguration source, IContextKey contextKey) {
        super(source);
        this.contextKey = contextKey;
    }

    @Override
    public SessionConfiguration getSession() {
        return getSource();
    }

    /**
     * Retrieves the session configuration from the event source.
     *
     * @return
     */
    @Override
    public SessionConfiguration getSource() {
        return (SessionConfiguration) source;
    }

    @Override
    public SessionIdentification getSessionIdentification() {
        return (SessionIdentification) ((SessionConfiguration) source).getContextId();
    }

    @Override
    public String toString() {
        return getSource().getContextId().toString();
    }

    /**
     * Return the foreign context key that originates from the session start event
     * @return the foreign context key
     */
    public IContextKey getForeignContextKey() {
        return this.contextKey;
    }

}
