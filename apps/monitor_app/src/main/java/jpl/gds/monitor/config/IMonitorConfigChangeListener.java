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
package jpl.gds.monitor.config;


/**
 * An interface for notifying listeners of changes to the global
 * MonitorConfigValues.
 */
public interface IMonitorConfigChangeListener {
    /**
     * Notifies the listener of a new global configuration value.
     * 
     * @param param
     *            the identifier of the configuration value that changed
     * @param newValue
     *            the new value of the parameter
     */
    public void globalConfigurationChange(GlobalPerspectiveParameter param,
            Object newValue);

}
