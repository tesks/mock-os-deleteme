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
package jpl.gds.time.api.message;

import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;

/**
 * An interface to be implemented by flight software time correlation messages.
 * 
 *
 * @since R8
 */
public interface IFswTimeCorrelationMessage extends IMessage {

    /**
     * Gets the expected SCLK (from inside the TC packet).
     * 
     * @return SCLK object
     */
    public ISclk getExpectedSclk();

    /**
     * Gets the packet SCLK (from the TC packet header).
     * 
     * @return SCLK object
     */
    public ISclk getPacketSclk();

    /**
     * Gets the bit rate at which the TC reference frame was received.
     * 
     * @return the bit rate as a double.
     */
    public double getBitrate();

    /**
     * Gets the ERT of the TC reference frame.
     * 
     * @return ERT as an IAccurateDateTime
     */
    public IAccurateDateTime getFrameErt();

    /**
     * Gets the ERT of the TC packet.
     * 
     * @return ERT as an IAccurateDateTime
     */
    public IAccurateDateTime getPacketErt();

    /**
     * Gets the virtual channel sequence count of the TC reference frame.
     * 
     * @return the VCFC
     */
    public long getFrameVcfc();

    /**
     * Gets the virtual channel identifier of the TC reference frame.
     * 
     * @return the VCID
     */
    public int getFrameVcid();

    /**
     * Indicates whether the TC reference frame was found in recently received
     * frames. If it was not found, frame-related fields in this message will be
     * undefined.
     * 
     * @return true if the frame was found, false if not
     */
    public boolean isReferenceFrameFound();

    /**
     * Gets the length in bytes of the TC reference frame.
     * 
     * @return byte length
     */
    public double getFrameLength();

    /**
     * Gets the bit rate index from the TC packet.
     * The interpretation of this value is mission specific.
     * 
     * @return bit rate index value
     */
    public long getBitRateIndex();

    /**
     * Gets the frame encoding type from the TC packet.
     * 
     * @return EncodingType
     * 
     */
    public EncodingType getFrameEncoding();

}