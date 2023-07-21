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
package jpl.gds.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;

/**
 * Message object for broadcasting global lad data objects.
 *
 * Deprecated since this is only used in tests.  Should be removed at some point.
 */
@Deprecated
public class GlobalLadDataMessage extends Message {

	private final IGlobalLADData data;

	public GlobalLadDataMessage(final IGlobalLADData data) {
		super(GlobalLadMessageType.GlobalLadData);
		this.data = data;
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));		
	}

	@Override
	public String getOneLineSummary() {
		return "Global Lad Data Message";
	}

	@Override
	public String toString() {
		return String.format("%s with data length %s", getOneLineSummary(), data);
	}
	
	public IGlobalLADData getData() {
		return data;
	}

}
