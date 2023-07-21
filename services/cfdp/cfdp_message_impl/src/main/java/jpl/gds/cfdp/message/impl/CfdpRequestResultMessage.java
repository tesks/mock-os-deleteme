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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpRequestResultMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpRequestResultMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;

/**
 * Class CfdpRequestResultMessage
 *
 * @since R8
 * 
 */
public class CfdpRequestResultMessage extends ACfdpMessage implements ICfdpRequestResultMessage {

	private String requestId;
	private boolean rejected;
	private String resultContent;

	public CfdpRequestResultMessage(final ApplicationContext appContext) {
		super(CfdpMessageType.CfdpRequestResult, appContext);
	}

	public CfdpRequestResultMessage(final ApplicationContext appContext, final Proto3CfdpRequestResultMessage msg)
			throws InvalidProtocolBufferException {
		super(CfdpMessageType.CfdpRequestResult, appContext, msg.getSuper(), msg.getHeader());
		requestId = msg.getRequestId();
		rejected = msg.getRejected();
		resultContent = msg.getResultContent();
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	@Override
	public String getOneLineSummary() {
		return "RequestResult: " + super.getOneLineSummary() + ", RequestId=" + requestId + ", Rejected=" + rejected
				+ ", ResultContent=\"" + resultContent + "\"";
	}

	@Override
	public Proto3CfdpRequestResultMessage build() {
		final Proto3CfdpRequestResultMessage.Builder builder = Proto3CfdpRequestResultMessage.newBuilder();
		builder.setSuper((Proto3AbstractMessage) super.build());
		return builder.setHeader(super.getBuilder()).setRequestId(requestId).setRejected(rejected)
				.setResultContent(resultContent).build();
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
				final Proto3CfdpRequestResultMessage protoCfdpRequestResultMessage = Proto3CfdpRequestResultMessage
						.parseFrom(messageBytes);
				final ICfdpRequestResultMessage cfdpRequestResultMessage = appContext
						.getBean(ICfdpRequestResultMessage.class, protoCfdpRequestResultMessage);
				addMessage(cfdpRequestResultMessage);
			}

			return getMessages();
		}

	}

	@Override
	public String getRequestId() {
		return requestId;
	}

	@Override
	public boolean isRejected() {
		return rejected;
	}

	@Override
	public String getResultContent() {
		return resultContent;
	}

	@Override
	public ICfdpRequestResultMessage setRequestId(final String requestId) {
		this.requestId = requestId;
		return this;
	}
	
	@Override
	public ICfdpRequestResultMessage setRejected(final boolean rejected) {
		this.rejected = rejected;
		return this;
	}

	@Override
	public ICfdpRequestResultMessage setResultContent(final String resultContent) {
		this.resultContent = resultContent;
		return this;
	}

}
