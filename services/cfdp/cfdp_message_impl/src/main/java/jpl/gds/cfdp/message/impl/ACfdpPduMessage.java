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

package jpl.gds.cfdp.message.impl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;

import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpPduMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpMessageHeader;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.MessageRegistry;

/**
 * Class ACfdpPduMessage
 *
 * @since R8
 * 
 */
public abstract class ACfdpPduMessage extends ACfdpMessage implements ICfdpPduMessage {

	private String pduId;
	private List<String> metadata;

	protected ACfdpPduMessage(final CfdpMessageType cfdpMessageType, final ApplicationContext appContext) {
		super(cfdpMessageType, appContext);
		metadata = new ArrayList<>();
	}

	public ACfdpPduMessage(final CfdpMessageType cfdpMessageType, final ApplicationContext appContext,
			final Proto3AbstractMessage abstractMessage, final Proto3CfdpMessageHeader cfdpMessageHeader,
			final String pduId, final int metadataSize, final ProtocolStringList metadata)
			throws InvalidProtocolBufferException {
		super(cfdpMessageType, appContext, abstractMessage, cfdpMessageHeader);
		this.pduId = pduId;
		this.metadata = new ArrayList<>(metadataSize);
		this.metadata.addAll(metadata);
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	@Override
	public String getOneLineSummary() {
		String delim = "";
		StringBuilder sb = new StringBuilder();

		for (String s : metadata) {
			sb.append(delim);
			sb.append(s);
			delim = ",";
		}

		return super.getOneLineSummary() + ", PduId=" + pduId + " [" + sb.toString() + "]";
	}

	@Override
	public String getPduId() {
		return pduId;
	}

	@Override
	public ICfdpPduMessage setPduId(final String pduId) {
		this.pduId = pduId;
		return this;
	}

	@Override
	public List<String> getMetadata() {
		return metadata;
	}

	@Override
	public ICfdpPduMessage addAllMetadata(final List<String> metadata) {
		this.metadata.addAll(metadata);
		return this;
	}

}
