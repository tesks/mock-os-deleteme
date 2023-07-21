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
package jpl.gds.telem.input.api.connection;

/**
 * An interface to be implemented by raw input connection classes that
 * support loading from a remote DB.
 * 
 *
 * @since R8
 */
public interface IRemoteConnectionSupport {
    /**
     * Sets the flag indicating the connector is loading from a remote DB.
     * 
     * @param enable true to enable remote DB, false to disable
     */
    public void setRemoteMode(boolean enable);
      
    /**
     * Gets the flag indicating the connector is loading from a remote DB.
     * 
     * @return true if remote DB loading enabled, false if not
     */
    public boolean getRemoteMode();
}
