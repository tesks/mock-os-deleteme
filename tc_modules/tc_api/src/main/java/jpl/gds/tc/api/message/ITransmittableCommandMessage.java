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

public interface ITransmittableCommandMessage extends ICommandMessage {

    /**
     * Gets the id that links this cmd message with a transmit event
     * 
     * @return hashcode object associated with TransmitEvent object
     * 
     */
    public int getTransmitEventId();

    /**
     * Sets the id that links this cmd message with a transmit event
     * 
     * @param transmitEventId hashcode associated with TransmitEvent object for
     *            this msg
     *            
     */
    public void setTransmitEventId(int transmitEventId);

    /**
     * Get whether or not uplink was successful
     * @return a flag indicating whether or not uplink was successful
     * 
     */
    public boolean isSuccessful();

    /**
     * Set whether or not uplink was successful
     * @param isSuccessful whether or not uplink was successful
     * 
     */
    public void setSuccessful(boolean isSuccessful);

}