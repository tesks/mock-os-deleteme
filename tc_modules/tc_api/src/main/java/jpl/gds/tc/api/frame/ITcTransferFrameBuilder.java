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
package jpl.gds.tc.api.frame;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.config.CommandFrameProperties;

/**
 * Interface for TcTransferFrame builders
 *
 */
public interface ITcTransferFrameBuilder {

    /**
     * Build the MPS TC frame
     *
     * @return an MPS TC frame
     */
    ITcTransferFrame build();

    /**
     * Set a frame configuration for creation of a new empty frame
     *
     * @param frameConfig the desired frame configuraiton
     * @return the builder
     */
    ITcTransferFrameBuilder setFrameConfig(final CommandFrameProperties frameConfig);

    /**
     * Set the sequence number
     *
     * @param sequenceNumber the sequence number
     * @return the builder
     */
    ITcTransferFrameBuilder setSequenceNumber(final int sequenceNumber);

    /**
     * Set the version
     *
     * @param version the version
     * @return the builder
     */
    ITcTransferFrameBuilder setVersion(final int version);

    /**
     * Set the bypass flag
     *
     * @param bypassFlag the bypass flag
     * @return the builder
     */
    ITcTransferFrameBuilder setBypassFlag(final int bypassFlag);

    /**
     * Set the control command flag
     *
     * @param ctrlCmdFlag the control command flag
     * @return the builder
     */
    ITcTransferFrameBuilder setCtrlCmdFlag(final int ctrlCmdFlag);

    /**
     * Set the spare
     *
     * @param spare the spare
     * @return the builder
     */
    ITcTransferFrameBuilder setSpare(final byte spare);

    /**
     * Set the spacecraft ID
     *
     * @param spacecraftId the spacecraft ID
     * @return the builder
     */
    ITcTransferFrameBuilder setSpacecraftId(final int spacecraftId);

    /**
     * Set the virtual channel ID. This will auto populate the execution string and virtual channel number.
     *
     * @param vcid the virtual channel ID
     * @return the builder
     */
    ITcTransferFrameBuilder setVcid(final byte vcid);

    /**
     * Set the execution string value.
     *
     * @param executionString the execution string value
     * @return the builder
     */
    ITcTransferFrameBuilder setExecutionString(final byte executionString);

    /**
     * Set the virtual channel number
     *
     * @param vcNumber the virtual channel number
     * @return the builder
     */
    ITcTransferFrameBuilder setVirtualChannelNumber(final byte vcNumber);

    /**
     * Set the frame length
     *
     * @param frameLength the frame length
     * @return the builder
     */
    ITcTransferFrameBuilder setFrameLength(final int frameLength);

    /**
     * Set the order ID
     *
     * @param orderId the order ID
     * @return the builder
     */
    ITcTransferFrameBuilder setOrderId(final int orderId);

    /**
     * Set if the frame has FECF
     *
     * @param hasFecf TRUE if this TC transfer frame has an FECF value
     * @return the builder
     */
    ITcTransferFrameBuilder setHasFecf(final boolean hasFecf);

    /**
     * Set the FECF. This will set the hasFecf boolean to true
     *
     * @param fecf FECF bytes
     * @return the builder
     */
    ITcTransferFrameBuilder setFecf(final byte[] fecf);

    /**
     * Set the frame data
     *
     * @param data frame data
     * @return the builder
     */
    ITcTransferFrameBuilder setData(final byte[] data);
}
