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
package jpl.gds.tc.api.message;

import jpl.gds.tc.api.ICpdUplinkStatus;

public interface ICpdUplinkStatusMessage extends IUplinkMessage {

    /**
     * Returns the uplink request status in this message.
     * 
     * @return the uplink request status
     */
    public ICpdUplinkStatus getStatus();

    /**
     * Set do-not-insert.
     */
    public void setDoNotInsert();

    /**
     * Return state of do-not-insert.
     * 
     * @return True if not to insert
     */
    public boolean getDoNotInsert();

}