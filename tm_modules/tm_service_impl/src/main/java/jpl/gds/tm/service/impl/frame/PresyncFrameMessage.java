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
package jpl.gds.tm.service.impl.frame;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IPresyncFrameMessage;

/**
 * PresyncFrameMessage is used to send chunks of raw frame data read from the telemetry
 * input interface in the downlink processor to the frame synchronizer.
 *
 */
class PresyncFrameMessage extends Message implements IPresyncFrameMessage {


    /** DSN info object */
    private final IStationTelemInfo dsnInfo;

    /** Length in bits */
    private final int bitLen;

    /** Data bytes */
    private final byte data[];

    /** ERT */
    private final IAccurateDateTime ert;
       
    /**
     * Gets the raw data ERT.  Do not be tempted to remove this and use the one
     * in the DSN Info. You will break the ERTs in framesync.
     * @return Returns the raw data ERT.
     */
    @Override
    public IAccurateDateTime getErt() {
        return this.ert;
    }
  
    /**
     * Creates an instance of PresyncFrameMessage.
     *
     * Note that the data is padded at the end by an extra byte. The reason
     * is unknown, but do not change it.
     *
     * @param di the DSNInfo object describing the data
     * @param buff the data buffer containing the raw data
     * @param offset the byte offset of the data in the buffer
     * @param bitLen the length of the data in bits
     * @param ert the Earth Receive Time of the data
     */
    protected PresyncFrameMessage(final IStationTelemInfo di, final byte buff[], final int offset, final int bitLen, final IAccurateDateTime ert) {
        super(TmServiceMessageType.PresyncFrameData, System.currentTimeMillis());
        dsnInfo = di;
        this.bitLen = bitLen;
        data = new byte[bitLen / 8 + 1];
        java.lang.System.arraycopy(buff, offset, data, 0, (bitLen / 8));
        this.ert = ert;
    }

    /**
     * Gets the station information object.
     *
     * @return the station information object describing the raw data
     */
    @Override
    public IStationTelemInfo getStationInfo() {
        return dsnInfo;
    }

    /**
     * Gets number of bytes.
     *
     * @return the length of the raw data in bytes
     */
    @Override
    public int getNumBytes() {
        return bitLen / 8;
    }


    /**
     * Gets the data.
     *
     * @return the buffer containing the raw data
     */
    @Override
    public byte[] getData() {
        return data;
    }


    /**
     * Gets the byte value at the given offset in the raw data buffer
     * @param off the byte offset to fetch
     * @return the byte value
     * @throws ArrayIndexOutOfBoundsException On index out of bounds
     */
    @Override
    public int get(final int off) throws ArrayIndexOutOfBoundsException {
        if (off < 0 || off >= data.length) {
            throw new ArrayIndexOutOfBoundsException("offset out of range "
                    + off + " of len " + data.length);
        }
        return 0xff & data[off];
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "MSG:" + getType() + " " + getEventTimeString() + " stationInfo=" + dsnInfo
                + " numBytes=" + getNumBytes();
    }

    /**
     * Generate XML.
     *
     * @param writer XML writer
     *
     * @throws XMLStreamException On error
     */  
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    	writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return TmServiceMessageType.PresyncFrameData.toString() + ": Raw Data bytes=" + getNumBytes();
    }
}
