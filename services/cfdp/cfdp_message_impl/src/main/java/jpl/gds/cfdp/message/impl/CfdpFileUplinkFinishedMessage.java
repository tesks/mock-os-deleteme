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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpFileUplinkFinishedMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpFileUplinkFinishedMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.config.OrderedProperties;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;

/**
 * Class CfdpFileUplinkFinishedMessage
 *
 * @since R8
 * 
 */
public class CfdpFileUplinkFinishedMessage extends ACfdpMessage implements ICfdpFileUplinkFinishedMessage {

	private OrderedProperties uplinkFileMetadata;
	private String uplinkFileMetadataFileLocation;
	private String uplinkFileLocation;

	public CfdpFileUplinkFinishedMessage(final ApplicationContext appContext) {
		super(CfdpMessageType.CfdpFileUplinkFinished, appContext);
	}

	public CfdpFileUplinkFinishedMessage(final ApplicationContext appContext,
			final Proto3CfdpFileUplinkFinishedMessage msg) throws InvalidProtocolBufferException {
		super(CfdpMessageType.CfdpFileUplinkFinished, appContext, msg.getSuper(), msg.getHeader());

		if (msg.getUplinkFileMetadataMap() != null) {
			uplinkFileMetadata = new OrderedProperties();
			uplinkFileMetadata.putAll(msg.getUplinkFileMetadataMap());
		}

		uplinkFileMetadataFileLocation = msg.getUplinkFileMetadataFileLocation();
		uplinkFileLocation = msg.getUplinkFileLocation();
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	@Override
	public String getOneLineSummary() {
		return "FileUplinkFinished: " + super.getOneLineSummary() + ", UplinkFileLocation="
				+ uplinkFileLocation + ", UplinkFileMetadataFileLocation="
				+ uplinkFileMetadataFileLocation;
	}

	@Override
	public Proto3CfdpFileUplinkFinishedMessage build() {
		final Proto3CfdpFileUplinkFinishedMessage.Builder builder = Proto3CfdpFileUplinkFinishedMessage.newBuilder();
		builder.setSuper((Proto3AbstractMessage) super.build());
		builder.setHeader(super.getBuilder());

		if (uplinkFileMetadata != null) {
			builder.putAllUplinkFileMetadata((Map) uplinkFileMetadata);
		}

		builder.setUplinkFileMetadataFileLocation(uplinkFileMetadataFileLocation);
		builder.setUplinkFileLocation(uplinkFileLocation);
		return builder.build();
	}

	/**
	 * BinaryParseHandler is the message-specific protobuf parse handler for
	 * creating this Message from its binary representation.
	 */
	public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {

		private final ApplicationContext appContext;

		/**
		 * Constructor.
		 * 
		 * @param context
		 *            current application context
		 */
		public BinaryParseHandler(final ApplicationContext context) {
			this.appContext = context;
		}

		@Override
		public IMessage[] parse(final List<byte[]> content) throws IOException {

			for (final byte[] messageBytes : content) {
				final Proto3CfdpFileUplinkFinishedMessage protoCfdpUplinkFinishedMessage = Proto3CfdpFileUplinkFinishedMessage
						.parseFrom(messageBytes);
				final ICfdpFileUplinkFinishedMessage cfdpUplinkFinishedMessage = appContext
						.getBean(ICfdpFileUplinkFinishedMessage.class, protoCfdpUplinkFinishedMessage);
				addMessage(cfdpUplinkFinishedMessage);
			}

			return getMessages();
		}

	}

	@Override
	public jpl.gds.shared.config.OrderedProperties getUplinkFileMetadata() {
		return uplinkFileMetadata;
	}

	@Override
	public ICfdpFileUplinkFinishedMessage setUplinkFileMetadata(
			jpl.gds.shared.config.OrderedProperties uplinkFileMetadata) {
		this.uplinkFileMetadata = uplinkFileMetadata;
		return this;
	}

	@Override
	public String getUplinkFileMetadataFileLocation() {
		return uplinkFileMetadataFileLocation;
	}

	@Override
	public ICfdpFileUplinkFinishedMessage setUplinkFileMetadataFileLocation(String uplinkFileMetadataFileLocation) {
		this.uplinkFileMetadataFileLocation = uplinkFileMetadataFileLocation;
		return this;
	}

	@Override
	public String getUplinkFileLocation() {
		return uplinkFileLocation;
	}

	@Override
	public ICfdpFileUplinkFinishedMessage setUplinkFileLocation(String uplinkFileLocation) {
		this.uplinkFileLocation = uplinkFileLocation;
		return this;
	}

}
