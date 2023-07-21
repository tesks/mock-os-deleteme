/*
 * Copyright 2006-2019. California Institute of Technology.
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

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.IPlopSerializable;
import jpl.gds.tc.api.exception.CltuEndecException;

/**
 * IScmfCommandMessage is the interface to be utilized by an object
 * that itends to represent the messages that are contained within
 * an IScmf.
 *
 */
public interface IScmfCommandMessage extends IPlopSerializable {

    /**
     * Get the byte length of this command message
     *
     * @return The byte length of this command message
     */
    int getMessageByteLength();

    /**
     * Get the byte representation of this command message
     *
     *  @return The byte array representation of this object
     */
    byte[] getBytes();

    /**
     * Accessor for the close window
     *
     * @return Returns the closeWindow.
     */
    String getCloseWindow();

    /**
     * Sets the closeWindow
     *
     * @param closeWindow The closeWindow to set.
     */
    void setCloseWindow(String closeWindow);

    /**
     * Accessor for the message comment
     *
     * @return Returns the messageComment.
     */
    String getMessageComment();

    /**
     * Sets the messageComment
     *
     * @param messageComment The messageComment to set.
     */
    void setMessageComment(String messageComment);

    /**
     * Accessor for the message number
     *
     * @return Returns the messageNumber.
     */
    long getMessageNumber();

    /**
     * Sets the messageNumber
     *
     * @param messageNumber The messageNumber to set.
     */
    void setMessageNumber(long messageNumber);

    /**
     * Accessor for the open window
     *
     * @return Returns the openWindow.
     */
    String getOpenWindow();

    /**
     * Sets the openWindow
     *
     * @param openWindow The openWindow to set.
     */
    void setOpenWindow(String openWindow);

    /**
     * Accessor for the transmission time
     *
     * @return Returns the transmissionStartTime.
     */
    String getTransmissionStartTime();

    /**
     * Sets the transmissionStartTime
     *
     * @param transmissionStartTime The transmissionStartTime to set.
     */
    void setTransmissionStartTime(String transmissionStartTime);

    /**
     * @return Returns the messageChecksum.
     */
    int getMessageChecksum();

    /**
     * Sets the messageChecksum
     *
     * @param messageChecksum The messageChecksum to set.
     */
    void setMessageChecksum(int messageChecksum);

    /**
     * 
     * @return The data portion of the command message
     */
    byte[] getData();

    /**
     * @param data The data portion of the command message
     */
    void setData(byte[] data);

    /**
     * Set the CLTU object that was parsed from the data section of this command message
     *
     * @param cltu the CLTU object that was parsed from the data section of this command message
     */
    void setCltuFromData(ICltu cltu);

    /**
     * Get a CLTU object from the data section of the command message
     * 
     * @return The CLTU object corresponding to the data portion of this command message
     * 
     * @throws CltuEndecException If the data portion of the command message
     * can't be interpreted as a CLTU
     */
    ICltu getCltuFromData() throws CltuEndecException;

}