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
import jpl.gds.cfdp.message.api.ICfdpPduSentMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpPduSentMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;

/**
 * Class CfdpPduSentMessage
 *
 * @since R8
 * 
 */
public class CfdpPduSentMessage extends jpl.gds.cfdp.message.impl.ACfdpPduMessage implements ICfdpPduSentMessage {

	public CfdpPduSentMessage(final ApplicationContext appContext) {
		super(CfdpMessageType.CfdpPduSent, appContext);
	}

	public CfdpPduSentMessage(final ApplicationContext appContext, final Proto3CfdpPduSentMessage msg)
			throws InvalidProtocolBufferException {
		super(CfdpMessageType.CfdpPduSent, appContext, msg.getSuper(), msg.getHeader(), msg.getPduId(),
				msg.getMetadataCount(), msg.getMetadataList());
	}

	@Override
	public String getOneLineSummary() {
		return "PduSent: " + super.getOneLineSummary();
	}

	@Override
	public Proto3CfdpPduSentMessage build() {
		final Proto3CfdpPduSentMessage.Builder builder = Proto3CfdpPduSentMessage.newBuilder();
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
				final Proto3CfdpPduSentMessage protoCfdpPduSentMessage = Proto3CfdpPduSentMessage
						.parseFrom(messageBytes);
				final ICfdpPduSentMessage cfdpPduSentMessage = appContext.getBean(ICfdpPduSentMessage.class,
						protoCfdpPduSentMessage);
				addMessage(cfdpPduSentMessage);
			}

			return getMessages();
		}

	}

}
