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
package jpl.gds.tc.api;

/**
 * The interface class for Telecommand (uplink) transfer frames
 *
 * MPCS-10928  - remove FECF_BYTE_LENGTH
 */
public interface ITcTransferFrame {

    /** The bitmask used to get the execution string bits.
     * <br> If used on a VCID this only applies AFTER the appropriate bit shift has been performed  */
    int                    BITMASK_EXECUTION_STRING       = 0b00000111;
    /** The bitmask used to get the virtual channel number bits */
    int                    BITMASK_VC_NUMBER              = 0b00000111;
    /** The offset in a VCID where the execution string bits can be found.  */
    int                    EXECUTION_STRING_BIT_OFFSET = 3;

    /**
     * Accessor for the data portion of the frame
     *
     * @return Returns the data.
     */
    byte[] getData();

    /**
     * Mutator for the data portion of the frame
     *
     * @param data The data to set.
     */
    void setData(byte[] data);

    /**
     * Accessor for the FECF portion of the frame
     *
     * @return Returns the fecf.
     */
    byte[] getFecf();

    /**
     * Mutator for the FECF portion of the frame
     *
     * @param fecf The fecf to set.
     */
    void setFecf(byte[] fecf);

    /**
     * Make a deep copy of this frame object
     *
     * @return A deep copy of this telecommand frame object
     */
    ITcTransferFrame copy();

    /**
     * See if this frame is supposed to have an FECF
     *
     * @return True if the frame has an FECF, false otherwise.
     */
    boolean hasFecf();

    /**
     * Set whether or not this frame has an FECF
     *
     * @param hasFecf True if the frame has an FECF, false otherwise
     */
    void setHasFecf(boolean hasFecf);

    /**
     * Construct the binary virtual channel ID for this frame header.  NOTE: The execution string and
     * virtual channel type must be set for this operation to succeed.
     *
     * @return The binary virtual channel ID for this frame header
     */
    byte getVirtualChannelId();

    /**
     * Accessor for the bypass flag
     *
     * @return Returns the bypassFlag.
     */
    byte getBypassFlag();

    /**
     * Mutator for the bypass flag
     *
     * @param bypassFlag The bypassFlag to set.
     */
    void setBypassFlag(byte bypassFlag);

    /**
     * Accessor for the CC flag
     *
     * @return Returns the controlCommandFlag.
     */
    byte getControlCommandFlag();

    /**
     * Mutator for the CC flag
     *
     * @param controlCommandFlag The controlCommandFlag to set.
     */
    void setControlCommandFlag(byte controlCommandFlag);

    /**
     * Accessor for the frame length
     *
     * @return Returns the length.
     */
    int getLength();

    /**
     * Mutator for the frame length
     *
     * @param length The length to set.
     */
    void setLength(int length);

    /**
     * Accessor for the frame sequence number
     *
     * @return Returns the sequenceNumber.
     */
    int getSequenceNumber();

    /**
     * Mutator for the frame sequence number
     *
     * @param sequenceNumber The sequenceNumber to set.
     */
    void setSequenceNumber(int sequenceNumber);

    /**
     * Accessor for the spacecraft ID
     *
     * @return Returns the spacecraftId.
     */
    int getSpacecraftId();

    /**
     * Mutator for the spacecraft ID
     *
     * @param spacecraftId The spacecraftId to set.
     */
    void setSpacecraftId(int spacecraftId);

    /**
     * Accessor for the spare field
     *
     * @return Returns the spare.
     */
    byte getSpare();

    /**
     * Mutator for the spare field
     *
     * @param spare The spare to set.
     */
    void setSpare(byte spare);

    /**
     * Accessor for the frame version number
     *
     * @return Returns the versionNumber.
     */
    byte getVersionNumber();

    /**
     * Mutator for the frame version number
     *
     * @param versionNumber The versionNumber to set.
     */
    void setVersionNumber(byte versionNumber);

    /**
     * Accessor for the execution string
     *
     * @return Returns the executionString.
     */
    byte getExecutionString();

    /**
     * Mutator for the execution string
     *
     * @param executionString The executionString to set.
     */
    void setExecutionString(byte executionString);

    /**
     * Accessor for the virtual channel number.
     *
     * @return Returns the virtualChannel.
     */
    byte getVirtualChannelNumber();

    /**
     * Mutator for the virtual channel number
     *
     * @param virtualChannelNumber The virtualChannel to set.
     */
    void setVirtualChannelNumber(byte virtualChannelNumber);

    /**
     * Get a string representation of the frame header
     *
     * @return A string containing the frame header details in a human-readable form
     */
    String getHeaderString();

    /**
     * Accessor for the Order ID
     *
     * @return Returns the orderId.
     */
    Integer getOrderId();

    /**
     * Mutator for the Order ID
     *
     * @param orderId The orderId to set.
     */
    void setOrderId(Integer orderId);

}