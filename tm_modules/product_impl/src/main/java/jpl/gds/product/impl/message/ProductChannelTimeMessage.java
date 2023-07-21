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

package jpl.gds.product.impl.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.product.api.decom.ProductDecomTimestampType;
import jpl.gds.product.api.message.InternalProductMessageType;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeUnit;

/**
 * ProductChannelTimeMessage carries a time value to be applied to channel
 * values produced during product channelization.
 * 
 *
 */
public class ProductChannelTimeMessage extends Message {

    private ISclk                     sclkTime;
    private IAccurateDateTime otherTime;
    private boolean isASclk;
    private int bitSize;
    private ProductDecomTimestampType timeType;
    private TimeUnit unit;

    /**
     * Creates an instance of ProductChannelTimeMessage with a current event
     * time.
     * 
     */
    public ProductChannelTimeMessage() {
        super(InternalProductMessageType.ProductChannelTime);
    }

    /**
     * Returns the time unit of the message.
     * 
     * @return time unit
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Sets the time unit of the message.
     * 
     * @param unit
     *            time unit
     */
    public void setUnit(final TimeUnit unit) {
        this.unit = unit;
    }

    /**
     * Gets the type of this timestamp (absolute, base, delta)
     * 
     * @return TimestampType
     */
    public ProductDecomTimestampType getTimeType() {
        return timeType;
    }

    /**
     * Sets the type of this timestamp (absolute, base, delta)
     * 
     * @param timeType
     *            the TimestampType to set
     */
    public void setTimeType(final ProductDecomTimestampType timeType) {
        this.timeType = timeType;
    }

    /**
     * Creates an instance of ProductChannelTimeMessage with the given event
     * time.
     * 
     * @param time
     *            the time the event occurred.
     */
    public ProductChannelTimeMessage(final IAccurateDateTime time) {
        super(InternalProductMessageType.ProductChannelTime, time.getTime());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        if (sclkTime == null && otherTime == null) {
            return "Empty Product Channel Time";
        }
        return "Product Channel Time: "
                + (isASclk ? sclkTime.toString() : otherTime
                        .getFormattedErt(true));
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }

    /**
     * Retrieves the SCLK. This object carries EITHER a SCLK or an ISO time. Use
     * isSclk() to determine which to retrieve.
     * 
     * @return the sclk
     */
    public ISclk getSclkTime() {
        return sclkTime;
    }

    /**
     * Sets the SCLK. This object carries EITHER a SLCK or an ISO time. Use
     * isSclk() to determine which to retrieve.
     * 
     * @param sclkTime
     *            the sclk to set
     */
    public void setSclkTime(final ISclk sclkTime) {
        this.sclkTime = sclkTime;
    }

    /**
     * Retrieves the non-SCLK (extended ISO) time. This object carries EITHER a
     * SLCK or an ISO time. Use isSclk() to determine which to retrieve.
     * 
     * @return IAccurateDateTime
     */
    public IAccurateDateTime getIsoTime() {
        return otherTime;
    }

    /**
     * Sets the non-SCLK (extended ISO) time. This object carries EITHER a SLCK
     * or an ISO time. Use isSclk() to determine which to retrieve.
     * 
     * @param isoTime
     *            the IAccurateDateTime to set
     */
    public void setIsoTime(final IAccurateDateTime isoTime) {
        otherTime = isoTime;
    }

    /**
     * Retrieves the flag indicating whether this object carries a SCLK or an
     * extended ISO time.
     * 
     * @return true if this object carries a SCLK, false otherwise
     */
    public boolean isSclk() {
        return isASclk;
    }

    /**
     * Sets the flag indicating whether this object carries a SCLK or an
     * extended ISO time.
     * 
     * @param isSclk
     *            true if this object carries a SCLK time, false otherwise
     */
    public void setIsSclk(final boolean isSclk) {
        this.isASclk = isSclk;
    }

    /**
     * Retrieves the bit length of the time field.
     * 
     * @return the bitSize
     */
    public int getBitSize() {
        return bitSize;
    }

    /**
     * Sets the bit length of the time field.
     * 
     * @param bitSize
     *            the bitSize to set
     */
    public void setBitSize(final int bitSize) {
        this.bitSize = bitSize;
    }
}
