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

/**
 * Interface for telemetry processor server session events
 */
public interface ISessionEvent {

    /**
     * Retrieves the session configuration from a session event
     * @return session identification
     */
    SessionConfiguration getSession();

    /**
     * Retrieves the session identification from a session event
     * @return session identification
     */
    SessionIdentification getSessionIdentification();

}
