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
package jpl.gds.telem.input.impl.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.telem.input.api.InternalTmInputMessageType;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;


/**
 * This is an internal MPCS message that contains raw (unprocessed) data from a
 * packet
 * 
 */
public class RawPacketMessage extends Message implements
        IRawDataMessage {

    private ISpacePacketHeader packetHeader;
	private ISecondaryPacketHeader secondaryHeader;
	private final byte[] header;
	private final byte[] body;

    private final HeaderHolder  rawHeader;
    private final TrailerHolder rawTrailer;

	private final RawInputMetadata metadata;

	/**
	 * Constructor
	 * @param metadata the associated <code>RawInputMetadata</code>
	 * @param header the packet header
	 * @param body the packet body
     * @param rawHdr the raw header
     * @param rawTr  the raw trailer
	 */
	public RawPacketMessage(final RawInputMetadata metadata,
                            final byte[]           header,
	                        final byte[]           body,
                            final HeaderHolder     rawHdr,
                            final TrailerHolder    rawTr)
    {
		super(InternalTmInputMessageType.RawPacket);
		this.header = header;
		this.body = body;

        this.rawHeader  = HeaderHolder.getSafeHolder(rawHdr);
        this.rawTrailer = TrailerHolder.getSafeHolder(rawTr);

		this.metadata = metadata;
	}


	@Override
	public RawInputMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public byte[] getData() {
		return this.body;
	}

	/**
	 * Get the packet header
	 * @return the packet header
	 */
	public byte[] getHeader() {
		return this.header;
	}

    /**
     * Get raw header.
     *
     * @return Raw header holder
     */
    @Override
    public HeaderHolder getRawHeader()
    {
        return rawHeader;
    }


    /**
     * Get raw trailer.
     *
     * @return Raw trailer holder
     */
    @Override
    public TrailerHolder getRawTrailer()
    {
        return rawTrailer;
    }


	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
	        throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	@Override
	public String getOneLineSummary() {
		return "Raw Packet";
	}


	@Override
	public String toString() {
		return "MSG:" + getType() + " " + getEventTimeString();
	}

	/**
	 * Retrieve the <code>ISpacePacketHeader</code>
	 * @return the <code>ISpacePacketHeader</code>
	 */
	public ISpacePacketHeader getPacketHeader() {
		return packetHeader;
	}
	
	/**
	 * @return the secondary header associated with this object
	 */
	public ISecondaryPacketHeader getSecondaryHeader() {
		return secondaryHeader;
	}
	
	/**
	 * Set a secondary packet header to associate with this object
	 * @param secondaryHeader the secondary header object to use
	 */
	public void setSecondaryHeader(final ISecondaryPacketHeader secondaryHeader) {
		this.secondaryHeader = secondaryHeader;
	}

	/**
	 * Set the <code>ISpacePacketHeader</code>
	 * @param packetHeader the <code>ISpacePacketHeader</code> to set
	 */
	public void setPacketHeader(final ISpacePacketHeader packetHeader) {
		this.packetHeader = packetHeader;
	}

}
