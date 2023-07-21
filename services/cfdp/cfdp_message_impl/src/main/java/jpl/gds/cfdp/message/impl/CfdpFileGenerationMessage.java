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
import jpl.gds.cfdp.message.api.ICfdpFileGenerationMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpFileGenerationMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.config.OrderedProperties;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;

/**
 * Class CfdpFileGenerationMessage
 *
 * @since R8
 * 
 */
public class CfdpFileGenerationMessage extends ACfdpMessage implements ICfdpFileGenerationMessage {

	private OrderedProperties downlinkFileMetadata;
	private String downlinkFileMetadataFileLocation;
	private String downlinkFileLocation;

	public CfdpFileGenerationMessage(final ApplicationContext appContext) {
		super(CfdpMessageType.CfdpFileGeneration, appContext);
	}

	public CfdpFileGenerationMessage(final ApplicationContext appContext, final Proto3CfdpFileGenerationMessage msg)
			throws InvalidProtocolBufferException {
		super(CfdpMessageType.CfdpFileGeneration, appContext, msg.getSuper(), msg.getHeader());

		if (msg.getDownlinkFileMetadataMap() != null) {
			downlinkFileMetadata = new OrderedProperties();
			downlinkFileMetadata.putAll(msg.getDownlinkFileMetadataMap());
		}

		downlinkFileMetadataFileLocation = msg.getDownlinkFileMetadataFileLocation();
		downlinkFileLocation = msg.getDownlinkFileLocation();
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	@Override
	public String getOneLineSummary() {
		return "FileGeneration: " + super.getOneLineSummary() + ", DownlinkFileLocation="
				+ downlinkFileLocation + ", DownlinkFileMetadataFileLocation="
				+ downlinkFileMetadataFileLocation;
	}

	@Override
	public Proto3CfdpFileGenerationMessage build() {
		final Proto3CfdpFileGenerationMessage.Builder builder = Proto3CfdpFileGenerationMessage.newBuilder();
		builder.setSuper((Proto3AbstractMessage) super.build());
		builder.setHeader(super.getBuilder());

		if (downlinkFileMetadata != null) {
			builder.putAllDownlinkFileMetadata((Map) downlinkFileMetadata);
		}

		builder.setDownlinkFileMetadataFileLocation(downlinkFileMetadataFileLocation);
		builder.setDownlinkFileLocation(downlinkFileLocation);
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
				final Proto3CfdpFileGenerationMessage protoCfdpFileGenerationMessage = Proto3CfdpFileGenerationMessage
						.parseFrom(messageBytes);
				final ICfdpFileGenerationMessage cfdpFileGenerationMessage = appContext
						.getBean(ICfdpFileGenerationMessage.class, protoCfdpFileGenerationMessage);
				addMessage(cfdpFileGenerationMessage);
			}

			return getMessages();
		}

	}

	@Override
	public jpl.gds.shared.config.OrderedProperties getDownlinkFileMetadata() {
		return downlinkFileMetadata;
	}

	@Override
	public ICfdpFileGenerationMessage setDownlinkFileMetadata(
			jpl.gds.shared.config.OrderedProperties downlinkFileMetadata) {
		this.downlinkFileMetadata = downlinkFileMetadata;
		return this;
	}

	@Override
	public String getDownlinkFileMetadataFileLocation() {
		return downlinkFileMetadataFileLocation;
	}

	@Override
	public String getDownlinkFileLocation() {
		return downlinkFileLocation;
	}

	@Override
	public ICfdpFileGenerationMessage setDownlinkFileMetadataFileLocation(String downlinkFileMetadataFileLocation) {
		this.downlinkFileMetadataFileLocation = downlinkFileMetadataFileLocation;
		return this;
	}

	@Override
	public ICfdpFileGenerationMessage setDownlinkFileLocation(String downlinkFileLocation) {
		this.downlinkFileLocation = downlinkFileLocation;
		return this;
	}

}
