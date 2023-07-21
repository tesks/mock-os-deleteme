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

import org.springframework.context.ApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpPduReceivedMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpPduReceivedMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;

/**
 * Class CfdpPduReceivedMessage
 *
 * @since R8
 * 
 */
public class CfdpPduReceivedMessage extends ACfdpPduMessage implements ICfdpPduReceivedMessage {

	public CfdpPduReceivedMessage(final ApplicationContext appContext) {
		super(CfdpMessageType.CfdpPduReceived, appContext);
	}

	public CfdpPduReceivedMessage(final ApplicationContext appContext, final Proto3CfdpPduReceivedMessage msg)
			throws InvalidProtocolBufferException {
		super(CfdpMessageType.CfdpPduReceived, appContext, msg.getSuper(), msg.getHeader(), msg.getPduId(),
				msg.getMetadataCount(), msg.getMetadataList());
	}

	@Override
	public String getOneLineSummary() {
		return "PduReceived: " + super.getOneLineSummary();
	}

	@Override
	public Proto3CfdpPduReceivedMessage build() {
		final Proto3CfdpPduReceivedMessage.Builder builder = Proto3CfdpPduReceivedMessage.newBuilder();
		builder.setSuper((Proto3AbstractMessage) super.build());
		return builder.setHeader(super.getBuilder()).setPduId(getPduId()).addAllMetadata(getMetadata()).build();
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
				final Proto3CfdpPduReceivedMessage protoCfdpPduReceivedMessage = Proto3CfdpPduReceivedMessage
						.parseFrom(messageBytes);
				final ICfdpPduReceivedMessage cfdpPduReceivedMessage = appContext.getBean(ICfdpPduReceivedMessage.class,
						protoCfdpPduReceivedMessage);
				addMessage(cfdpPduReceivedMessage);
			}

			return getMessages();
		}

	}

}
