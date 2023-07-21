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
package jpl.gds.tc.impl.frame;

import java.util.Arrays;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.frame.ITcTransferFrameBuilder;

/**
 *
 * This class is the code representation of a Telecommand Transfer Frame as defined in
 * the CCSDS Blue Book on TC Space Data Link Protocol.
 *
 *
 * MPCS-10745 - 07/23/19 - reconciled and consolidated "legacy" and "mps"
 *          into one object. Removed all functions that serialize data
 */
public class TcTransferFrame implements Cloneable, ITcTransferFrame {

    /**
     * The version number of this frame header
     */
    private byte versionNumber;

    /**
     * The bypass flag value for this frame header
     */
    private byte bypassFlag = (byte)0x01;

    /**
     * The control command flag value for this frame header
     */
    private byte controlCommandFlag = (byte)0x00;

    /**
     * The spare value for this frame header
     */
    private byte spare = (byte)0x00;

    /**
     * The spacecraft ID for this frame header
     */
    private int spacecraftId;

    /**
     * The execution string type for this frame header
     * (first part of the VCID)
     */
    private byte executionString;

    /**
     * The virtual channel number for this frame header
     * (second part of the VCID)
     */
    private byte virtualChannelNumber;

    /**
     * The frame length for this frame header
     * (it IS the length - 1)
     */
    private int length;

    /**
     * The sequence number for this frame header
     */
    private int sequenceNumber = 0x00000000;

    /** The frame data section */
    private byte[] data = new byte[0];

    /** The Frame Error Control Field (FECF).  It's like a checksum. */
    private byte[] fecf = new byte[0];

    /** True if this type of frame has an FECF, false otherwise. */
    private boolean hasFecf = false;

    /** Utility field used by the Fault Injector's Frame Editor to keep frames
     * in the user specified order.
     */
    private Integer orderId;

    private final CommandFrameProperties frameConfig;

    public TcTransferFrame(final CommandFrameProperties frameConfig) {
        this.frameConfig = frameConfig;
    }

    public TcTransferFrame() {
        this.frameConfig = null;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public void setData(final byte[] data)
    {
        if(data == null)
        {
            throw new IllegalArgumentException("Input data was null.");
        }

        this.data = data;
    }

    @Override
    public byte[] getFecf() {
        if (!this.hasFecf || (this.hasFecf() && this.fecf.length == 0)) {
            throw new IllegalStateException("FECF has not been set.");
        }
        return fecf;
    }

    @Override
    public void setFecf(final byte[] fecf) {
        if(fecf == null) {
            throw new IllegalArgumentException("Input FECF value was null");
        }
        this.fecf = fecf;
        this.setHasFecf(fecf.length > 0);
    }

    @Override
    public boolean hasFecf() {
        return (this.hasFecf);
    }

    @Override
    public void setHasFecf(final boolean hasFecf) {
        this.hasFecf = hasFecf;
    }

    @Override
    public byte getVirtualChannelId() {
        byte vcid = 0x00;

        vcid += ((this.executionString & BITMASK_EXECUTION_STRING) << EXECUTION_STRING_BIT_OFFSET);

        vcid += (this.virtualChannelNumber & BITMASK_VC_NUMBER);

        return(vcid);
    }

    @Override
    public byte getBypassFlag()
    {
        return this.bypassFlag;
    }

    @Override
    public void setBypassFlag(final byte bypassFlag) {
        this.bypassFlag = bypassFlag;
    }

    @Override
    public byte getControlCommandFlag() {
        return controlCommandFlag;
    }

    @Override
    public void setControlCommandFlag(final byte controlCommandFlag) {
        this.controlCommandFlag = controlCommandFlag;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(final int length) {
        this.length = length;
    }

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public void setSequenceNumber(final int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int getSpacecraftId() {
        return spacecraftId;
    }

    @Override
    public void setSpacecraftId(final int spacecraftId) {
        this.spacecraftId = spacecraftId;
    }

    @Override
    public byte getSpare() {
        return spare;
    }

    @Override
    public void setSpare(final byte spare) {
        this.spare = spare;
    }

    @Override
    public byte getVersionNumber() {
        return versionNumber;
    }

    @Override
    public void setVersionNumber(final byte versionNumber) {
        this.versionNumber = versionNumber;
    }

    @Override
    public byte getExecutionString() {
        return this.executionString;
    }

    @Override
    public void setExecutionString(final byte executionString) {
        this.executionString = executionString;
    }

    @Override
    public byte getVirtualChannelNumber() {
        return this.virtualChannelNumber;
    }

    @Override
    public void setVirtualChannelNumber(final byte virtualChannelNumber) {
        this.virtualChannelNumber = virtualChannelNumber;
    }

    @Override
    public Integer getOrderId() {
        return orderId;
    }

    @Override
    public void setOrderId(final Integer orderId) {
        if(orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return getHeaderString();
    }

    @Override
    public String getHeaderString() {
        final StringBuilder buf = new StringBuilder(1024);
        ExecutionStringType est =  null;
        VirtualChannelType vct = null;

        if(frameConfig != null) {
            est = ExecutionStringType.getTypeFromVcidValue(frameConfig, this.executionString);
            vct = VirtualChannelType.getTypeFromNumber(frameConfig, this.virtualChannelNumber);
        }

        buf.append("Frame Version = ").append(getVersionNumber() + 1).append("\n");
        buf.append("Bypass Flag = ").append(getBypassFlag()).append("\n");
        buf.append("Control Command Flag = ").append(getControlCommandFlag()).append("\n");
        buf.append("Spare Bits = ").append(getSpare()).append("\n");
        buf.append("Spacecraft ID = ").append(getSpacecraftId()).append("\n");
        buf.append("RCE String = ").append(getExecutionString());
        if(est != null) {
            buf.append(" (").append(est.toString()).append(")");
        }
        buf.append("\n");
        buf.append("Virtual Channel Number = ").append(getVirtualChannelNumber());
        if(vct != null) {
            buf.append(" (").append(vct.toString()).append(")");
        }
        buf.append("\n");
        buf.append("Sequence Number = ").append(getSequenceNumber()).append("\n");
        buf.append("Length = ").append(getLength() + 1).append("\n");

        return (buf.toString());
    }

    @Override
    protected Object clone() {
        return copy();
    }

    @Override
    public ITcTransferFrame copy() {
        ITcTransferFrameBuilder builder = new TcTransferFrameBuilder()
                .setData(Arrays.copyOf(getData(), getData().length))
                .setFrameConfig(frameConfig)
                .setHasFecf(hasFecf)
                .setSequenceNumber(getSequenceNumber())
                .setSpacecraftId(getSpacecraftId())
                .setVirtualChannelNumber(getVirtualChannelNumber())
                .setExecutionString(getExecutionString())
                .setCtrlCmdFlag(getControlCommandFlag())
                .setBypassFlag(getBypassFlag())
                .setVersion(getVersionNumber())
                .setFrameLength(getLength())
                .setSpare(getSpare());

        if(this.hasFecf) {
            builder.setFecf(Arrays.copyOf(getFecf(), getFecf().length));
        }

        return builder.build();
    }







}
