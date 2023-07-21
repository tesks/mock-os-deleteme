/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.tc.api.frame;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;

import java.util.List;

/**
 * {@code ITcTransferFramesBuilder} is a {@code ITcTransferFrame} list builder.
 * <p>
 * To use this builder, first get an instance of this class by grabbing the bean via Spring. Then, set the data for
 * the frame(s), VCID, and any other parameters to set. (If those parameters are not manually set, default values
 * will be used.) After setting all the parameters including data, call {@link #build()} to retrieve the list of {@code
 * ITcTransferFrame} objects built. If the data size is less than the maximum allowed for a transfer frame, the list
 * will be size 1.
 *
 * @since 8.2.0
 */
public interface ITcTransferFramesBuilder {

    /**
     * Set the transfer frame version number.
     *
     * @param transferFrameVersionNumber transfer frame version number to set, integer range 0 through 3 inclusive
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setTransferFrameVersionNumber(int transferFrameVersionNumber);

    /**
     * Set the bypass flag to 1.
     *
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setBypassFlagOn();

    /**
     * Set the bypass flag to 0.
     *
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setBypassFlagOff();

    /**
     * Set the control command flag to 1.
     *
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setControlCommandFlagOn();

    /**
     * Set the control command flag to 0.
     *
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setControlCommandFlagOff();

    /**
     * Set the reserved spare.
     *
     * @param reservedSpare reserved spare to set, integer range 0 through 3 inclusive
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setReservedSpare(int reservedSpare);

    /**
     * Set the spacecraft ID.
     *
     * @param scid spacecraft ID to set, integer range 0 through 1023 inclusive
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setScid(int scid);

    /**
     * Set the virtual channel ID.
     * <p>
     * Note: This is the exact value that fills the transfer frame's virtual channel identifier field. For those
     * missions that use part of this VCID field to specify the execution string ID, the caller is responsible for
     * supplying the effective VCID value.
     *
     * @param vcid virtual channel ID to set, integer range 0 through 63 inclusive
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setVcid(int vcid);

    /**
     * Set the frame length.
     *
     * @param frameLength frame length to set, integer range 0 through 1023 inclusive
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setFrameLength(int frameLength);

    /**
     * Set the frame sequence number.
     *
     * @param frameSequenceNumber frame sequence number to set, integer range 0 through 255 inclusive
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setFrameSequenceNumber(int frameSequenceNumber);

    /**
     * Start frame sequence number from 0 and increment, rolling over at its maximum.
     *
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder rollFrameSequenceNumber();

    /**
     * Set the data. If the data is larger than that can be set within a single transfer frame, it will be split into
     * multiple transfer frames.
     *
     * @param data data to set
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setData(byte[] data);

    /**
     * Add the frame error control field using the specified algorithm.
     *
     * @param fecfAlgorithm algorithm to use for the frame error control field
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setFrameErrorControlFieldAlgorithm(FrameErrorControlFieldAlgorithm fecfAlgorithm);

    /**
     * Remove the frame error control field.
     *
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder noFrameErrorControlField();

    /**
     * Manually set the frame error control field value.
     *
     * @param fecfValue frame error control field value, integer range 0 through 65535 inclusive
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setFrameErrorControlFieldValue(byte[] fecfValue);

    /**
     * Manually set the frame error control field length.
     *
     * @param length the length in bytes. Default is 2. Must be a power of 2, and less than or equal to 8 bytes.
     * @return this object (builder pattern)
     */
    ITcTransferFramesBuilder setFrameErrorControlFieldLength(int length);

    /**
     * Build the transfer frame(s) using the parameters set in this builder (or defaults for those parameters not set)
     * and return the built {@code ITcTransferFrame} object(s) as a list.
     *
     * @return list of built telecommand transfer frame object(s)
     * @throws FrameWrapUnwrapException thrown when MPSA UplinkUtils library reports an error
     */
    List<ITcTransferFrame> build() throws FrameWrapUnwrapException;

}