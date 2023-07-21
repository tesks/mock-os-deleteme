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

import jpl.gds.db.api.types.IDbRawData;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.telem.input.api.InternalTmInputMessageType;
import jpl.gds.telem.input.api.message.IRawDbMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;


/**
 * This is an internal MPCS message that contains raw (unprocessed) data queried
 * from the database
 * 
 *
 */
public class RawDatabaseMessage extends Message implements
        IRawDbMessage {
	private final RawInputMetadata metadata;
	private final IDbRawData data;

	/**
	 * Constructor
	 * @param metadata the raw input metadata  
	 * @param data the actual raw data
	 * 
	 */
	public RawDatabaseMessage(final RawInputMetadata metadata, final IDbRawData data) {
		super(InternalTmInputMessageType.RawDatabase);
		this.metadata = metadata;
		this.data = data;
	}


	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
	        throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}


	@Override
	public String getOneLineSummary() {
		return "Database Raw Data";
	}


	@Override
	public String toString() {
		return "MSG:" + getType() + " " + getEventTimeString();
	}


	@Override
	public RawInputMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public byte[] getData() {
		return data.getRecordBytes();
	}


	@Override
	public IDbRawData getDbRawData() {
		return this.data;
	}


	@Override
	public HeaderHolder getRawHeader()
    {
		return data.getRawHeader();
	}


	@Override
	public TrailerHolder getRawTrailer()
    {
		return data.getRawTrailer();
	}
}
