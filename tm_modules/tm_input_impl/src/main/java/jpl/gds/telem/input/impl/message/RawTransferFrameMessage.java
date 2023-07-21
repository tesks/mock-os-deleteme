/*
 * Copyright 2006-2021. California Institute of Technology.
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

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.telem.input.api.InternalTmInputMessageType;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**
 * This is an internal MPCS message that contains raw (unprocessed) data from
 * transfer frames
 *
 */
public class RawTransferFrameMessage extends Message implements
		IRawDataMessage
{
	private final byte[] data;
	private final RawInputMetadata metadata;

	private final HeaderHolder  header;
	private final TrailerHolder trailer;


	/**
	 * Constructor
	 *
	 * @param metadata the associated <code>RawInputMetadata</code>
	 * @param data     the payload data
	 * @param hdr      Header holder
	 * @param tr       Trailer holder
	 */
	public RawTransferFrameMessage(final RawInputMetadata metadata,
								   final byte[]           data,
								   final HeaderHolder     hdr,
								   final TrailerHolder    tr)
	{
		super(InternalTmInputMessageType.RawTransferFrame);
		this.data = data;
		this.metadata = metadata;

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
		return "Raw Transfer Frame";
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return "MSG:" + getType() + " " + getEventTimeString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.rawio.input.message.IRawDataMessage#getMetadata()
	 */
	@Override
	public RawInputMetadata getMetadata() {
		return this.metadata;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.rawio.input.message.IRawDataMessage#getData()
	 */
	@Override
	public byte[] getData() {
		return this.data;
	}


	/**
	 * Getter for header.
	 *
	 * @return Header byte holder
	 */
	@Override
	public HeaderHolder getRawHeader()
	{
		return header;
	}


	/**
	 * Getter for trailer.
	 *
	 * @return Trailer byte holder
	 */
	@Override
	public TrailerHolder getRawTrailer()
	{
		return trailer;
	}
}