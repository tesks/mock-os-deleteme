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
package jpl.gds.monitor.perspective.view;

/**
 * This interface should be implemented by view configuration classes and
 * preferences shells that support a station filter.
 */
public interface StationSupport {

    /**
     * Gets the station ID a GUI view should display monitor channels for.
     * 
     * @return station ID, or SessionConfiguration.UNSPECIFIED_DSSID if none
     *         defined or station found in the view configuration is invalid.
     */
    public int getStationId();

    /**
     * Sets the station ID a GUI view should display monitor channels for.
     * 
     * @param station
     *            station ID to set
     */
    public void setStationId(final int station);
}
