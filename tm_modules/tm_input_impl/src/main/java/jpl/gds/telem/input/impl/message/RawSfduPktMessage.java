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
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.telem.input.api.InternalTmInputMessageType;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;

/**
 * This class represents an MPCS internal message containing SFDU wrapped raw
 * (unprocessed) packet data.
 * 
 */
public class RawSfduPktMessage extends Message implements
        IRawDataMessage {
	private ISpacePacketHeader packetHeader;
	private RawInputMetadata metadata;
	private int apid;
	private byte[] data;
	private IAccurateDateTime scet;
	private ISclk sclk;
	private int scid;
	private int vcid;
	private int vfc;
	// MPCS-8918 - 06/16/2016: added secondary header len
	private int secondaryHeaderLen;


	private PacketIdHolder packetId = null;

    private final HeaderHolder  header;
    private final TrailerHolder trailer;
    private IChdoSfdu sfdu;


	/**
	 * Constructor
	 * @param sfdu the IChdoSdfu object for the SFDU
	 * @param metadata the associated <code>RawInputMetadata<code>
	 * @param data the payload data
     * @param hdr  the header
     * @param tr   the trailer
	 */
	public RawSfduPktMessage(final IChdoSfdu sfdu,
	                         final RawInputMetadata metadata,
                             final byte[]           data,
                             final HeaderHolder     hdr,
                             final TrailerHolder    tr)
    {
		super(InternalTmInputMessageType.RawSfduPkt);
		this.metadata = metadata;
		this.sfdu = sfdu;
		this.data = data;

        header  = HeaderHolder.getSafeHolder(hdr);
        trailer = TrailerHolder.getSafeHolder(tr);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
	        throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {
		return "SFDU Raw Data";
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return "MSG:" + getType() + " " + getEventTimeString();
	}

	/**
	 * Set the associated <code>RawInputMetadata<code>
	 * @param metadata the <code>RawInputMetadata<code> to set
	 */
	public void setMetadata(final RawInputMetadata metadata) {
		this.metadata = metadata;
	}

	/**
	 * Set the payload data
	 * @param data the payload data to set
	 */
	public void setData(final byte[] data) {
		this.data = data;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.telem.input.api.message.IRawDataMessage#getMetadata()
	 */
	@Override
	public RawInputMetadata getMetadata() {
		return metadata;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.telem.input.api.message.IRawDataMessage#getData()
	 */
	@Override
	public byte[] getData() {
		return this.data;
	}

	/**
	 * Retrieve the APID
     * @return the APID
     */
    public int getApid() {
    	return apid;
    }

	/**
	 * Set the APID
     * @param apid the APID to set
     */
    public void setApid(final int apid) {
    	this.apid = apid;
    }

	/**
	 * Retrieve the SCET for this chunk of telemetry
     * @return the SCET for this chunk of telemetry
     */
    public IAccurateDateTime getScet() {
    	return scet;
    }

	/**
	 * Set the SCET for this chunk of telemetry
     * @param scet the SCET to set for this chunk of telemetry
     */
    public void setScet(final IAccurateDateTime scet) {
    	this.scet = scet;
    }

	/**
	 * Retrieve the SCLK for this chunk of telemetry
     * @return the SCLK for this chunk of telemetry
     */
    public ISclk getSclk() {
    	return sclk;
    }

	/**
	 * Set the SCLK for this chunk of telemetry
     * @param sclk the SCLK to set this chunk of telemetry
     */
    public void setSclk(final ISclk sclk) {
    	this.sclk = sclk;
    }

	/**
	 * Get the spacecraft ID
     * @return the spacecraft ID
     */
    public int getScid() {
    	return scid;
    }

	/**
	 * Set the spacecraft ID
     * @param scid the spacecraft ID to set
     */
    public void setScid(final int scid) {
    	this.scid = scid;
    }

	/**
	 * Get the virtual channel ID
     * @return the virtual channel ID
     */
    public int getVcid() {
    	return vcid;
    }

	/**
	 * Set the virtual channel ID
     * @param vcid the virtual channel ID
     */
    public void setVcid(final int vcid) {
    	this.vcid = vcid;
    }

	/**
	 * Get the virtual frame count
     * @return the virtual frame count
     */
    public int getVfc() {
    	return vfc;
    }

	/**
	 * Set the virtual frame count
     * @param vfc the virtual frame count
     */
    public void setVfc(final int vfc) {
    	this.vfc = vfc;
    }

	/**
	 * Get the <code>ISpacePacketHeader</code>
     * @return the <code>ISpacePacketHeader</code>
     */
    public ISpacePacketHeader getPacketHeader() {
    	return packetHeader;
    }

	/**
	 * Set the <code>ISpacePacketHeader</code>
     * @param packetHeader the  <code>ISpacePacketHeader</code> to set
     */
    public void setPacketHeader(final ISpacePacketHeader packetHeader) {
    	this.packetHeader = packetHeader;
    }


    /**
     * Get packet id if set.
     *
     * @return Packet id
     */
    public PacketIdHolder getPacketId()
    {
        return packetId;
    }


    /**
     * Set packet id.
     *
     * @param pi Packet id
     */
    public void setPacketId(final PacketIdHolder pi)
    {
        packetId = pi;
    }


    /**
     * Getter for header.
     *
     * @return Header holder
     */
    @Override
    public HeaderHolder getRawHeader()
    {
        return header;
    }


    /**
     * Getter for trailer.
     *
     * @return Trailer holder
     */
    @Override
    public TrailerHolder getRawTrailer()
    {
        return trailer;
    }
    
    /**
     * Gets the secondary packet header length associated with
     * the packet
     * @return length of the secondary header in bytes
     */
    public int getSecondaryHeaderLen() {
		return secondaryHeaderLen;
	}

    /**
     * Sets the secondary header length associated with the packet
     * @param secondaryHeaderLen the length to set in bytes
     */
	public void setSecondaryHeaderLen(final int secondaryHeaderLen) {
		this.secondaryHeaderLen = secondaryHeaderLen;
	}

	/**
	 * Gets the IChdoSfdu object associated with this message.
	 * 
	 * @return IChdoSfdu
	 */
    public IChdoSfdu getSfdu() {
        return sfdu;
    }

}
