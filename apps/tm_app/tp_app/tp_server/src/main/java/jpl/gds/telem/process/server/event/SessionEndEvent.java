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
import jpl.gds.shared.sys.SystemUtilities;
import org.springframework.context.ApplicationEvent;

/**
 * A session end event for use in the telemetry processor server.
 */
public class SessionEndEvent extends ApplicationEvent implements ISessionEvent {

    /**
     * Constructor
     * @param source session identification
     */
    public SessionEndEvent(SessionIdentification source) {
        super(source);
    }

    /**
     * Not implemented in session end events.
     * @return null
     */
    @Override
    public SessionConfiguration getSession() {
        SystemUtilities.doNothing();
        return null;
    }

    /**
     * Retrieve the event source's session identification object
     * @return session identification
     */
    @Override
    public SessionIdentification getSource() {
        return (SessionIdentification) source;
    }

    @Override
    public SessionIdentification getSessionIdentification() {
        return (SessionIdentification) source;
    }

    @Override
    public String toString() {
        return getSource().toString();
    }

}
