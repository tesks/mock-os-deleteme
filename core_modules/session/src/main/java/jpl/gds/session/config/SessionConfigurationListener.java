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
package jpl.gds.session.config;

/**
 * SessionConfigurationListener is implemented by any class that wants to be
 * notified when a session configuration changes. An object of that class can
 * then register with the SessionConfiguration.
 */
public interface SessionConfigurationListener {
    /**
     * Notifies the listener that a session configuration has changed.
     * @param config the SessionConfiguration that changed
     */
    void configurationChanged(SessionConfiguration config);
}