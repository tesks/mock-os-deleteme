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
package jpl.gds.tc.api.cltu;

import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.IPlopSerializable;
import jpl.gds.tc.api.ITcTransferFrame;

import java.util.List;

/**
 * An interface to be implemented by CLTU (Command Link Transmission Unit) classes.
 *
 * @since R8
 */
public interface ICltu extends IPlopSerializable {

    /**
     * Get the binary representation of this CLTU.
     *
     * @return A byte array representing this CLTU
     */
    byte[] getBytes();

    /**
     * Accessor for the CLTU frame list.
     *
     * @return The list of telecommand frame objects that are encoded into this CLTU
     */
    // TODO - refactor to remove
    List<ITcTransferFrame> getFrames();

    /**
     * Make a deep copy of this CLTU
     *
     * @return A deep copy of this CLTU
     */
    ICltu copy();

    /**
     * Returns the order ID for this CLTU.
     *
     * @return the orderId.
     */
    Integer getOrderId();

    /**
     * Sets the order ID for this CLTU.
     *
     * @param orderId The orderId to set.
     */
    void setOrderId(Integer orderId);

    /**
     * Returns the acquisition byte sequence for this CLTU.
     *
     * @return the acquisitionSequence.
     */
    byte[] getAcquisitionSequence();

    /**
     * Sets the acquisition byte sequence for this CLTU.
     *
     * @param acquisitionSequence The acquisitionSequence to set.
     */
    void setAcquisitionSequence(byte[] acquisitionSequence);

    /**
     * Returns the start byte sequence for this CLTU.
     *
     * @return the startSequence.
     */
    byte[] getStartSequence();

    /**
     * Sets the start byte sequence for this CLTU.
     *
     * @param startSequence The startSequence to set.
     */
    void setStartSequence(byte[] startSequence);

    /**
     * Returns the CLTU data.
     *
     * @return the data.
     */
    byte[] getData();

    /**
     * Sets the CLTU data.
     *
     * @param data The data to set.
     */
    void setData(byte[] data);

    /**
     * Returns the tail byte sequence for this CLTU.
     *
     * @return the tailSequence.
     */
    byte[] getTailSequence();

    /**
     * Sets the tail byte sequence for this CLTU.
     *
     * @param tailSequence The tailSequence to set.
     */
    void setTailSequence(byte[] tailSequence);

    /**
     * Returns the idle byte sequence for this CLTU.
     *
     * @return the idleSequence.
     */
    byte[] getIdleSequence();

    /**
     * Sets the idle byte sequence for the CLTU.
     *
     * @param idleSequence The idleSequence to set.
     */
    void setIdleSequence(byte[] idleSequence);

    /**
     * Returns the BCH code block objects for this CLTU.
     *
     * @return the codeblockObjects.
     */
    List<IBchCodeblock> getCodeblocks();

    /**
     * Sets the BCH code block objects for this CLTU
     *
     * @param codeblockObjects The codeblockObjects to set.
     */
    void setCodeblocks(List<IBchCodeblock> codeblockObjects);

    /**
     * Sets the list of frames in this CLTU.
     *
     * @param frames The frames to set.
     */
    void setFrames(List<ITcTransferFrame> frames);

    /**
     * Display this CLTU as a hex string.
     *
     * @return this CLTU as a hex string.
     */
    String getHexDisplayString();

}