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


public interface IRawUplinkDataMessage extends ICpdUplinkMessage {

    /**
     * If the raw data contains deliberate faults (came from fault injector)
     * 
     * @return true if the raw data contains deliberate faults (came from fault
     *         injector), false otherwise
     */
    public boolean isFaultInjected();

    /**
     * Set if the raw data contains deliberate faults (came from fault injector)
     * 
     * @param true if the raw data contains deliberate faults (came from fault
     *        injector), false otherwise
     */
    public void setFaultInjected(boolean isFaultInjected);

}