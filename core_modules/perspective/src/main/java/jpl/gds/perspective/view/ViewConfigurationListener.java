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
package jpl.gds.perspective.view;

/**
 * ViewConfigurationListener is implemented by any class that wants to be
 * notified when a view configuration changes. An object of that class can then
 * register with the ViewConfiguration.
 * 
 *
 */
public interface ViewConfigurationListener {
    /**
     * Notifies the listener that a view configuration has changed.
     * 
     * @param config
     *            the ViewConfiguration that changed
     */
    public void configurationChanged(IViewConfiguration config);
}
