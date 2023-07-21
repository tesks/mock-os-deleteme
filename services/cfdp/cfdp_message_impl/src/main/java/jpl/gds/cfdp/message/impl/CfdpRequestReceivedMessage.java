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
import jpl.gds.cfdp.message.api.ICfdpRequestReceivedMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpRequestReceivedMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;

/**
 * Class CfdpRequestReceivedMessage
 * 
 * @since R8
 * 
 */
public class CfdpRequestReceivedMessage extends ACfdpMessage implements ICfdpRequestReceivedMessage {

	private String requestId;
	private String requesterId;
	private String httpUser;
	private String httpHost;
	private String requestContent;

	public CfdpRequestReceivedMessage(final ApplicationContext appContext) {
		super(CfdpMessageType.CfdpRequestReceived, appContext);
	}

	public CfdpRequestReceivedMessage(final ApplicationContext appContext, final Proto3CfdpRequestReceivedMessage msg)
			throws InvalidProtocolBufferException {
		super(CfdpMessageType.CfdpRequestReceived, appContext, msg.getSuper(), msg.getHeader());
		requestId = msg.getRequestId();
		requesterId = msg.getRequesterId();
		httpUser = msg.getHttpUser();
		httpHost = msg.getHttpHost();
		requestContent = msg.getRequestContent();
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	@Override
	public String getOneLineSummary() {
		return "RequestReceived: " + super.getOneLineSummary() + ", RequestId=" + requestId + ", RequesterId="
				+ requesterId + ", HttpUser=" + httpUser + ", HttpHost=" + httpHost + ", RequestContent=\""
				+ requestContent + "\"";
	}

	@Override
	public Proto3CfdpRequestReceivedMessage build() {
		final Proto3CfdpRequestReceivedMessage.Builder builder = Proto3CfdpRequestReceivedMessage.newBuilder();
		builder.setSuper((Proto3AbstractMessage) super.build());
		return builder.setHeader(super.getBuilder()).setRequestId(requestId).setRequesterId(requesterId)
				.setHttpUser(httpUser).setHttpHost(httpHost).setRequestContent(requestContent).build();
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
				final Proto3CfdpRequestReceivedMessage protoCfdpRequestReceivedMessage = Proto3CfdpRequestReceivedMessage
						.parseFrom(messageBytes);
				final ICfdpRequestReceivedMessage cfdpRequestReceivedMessage = appContext
						.getBean(ICfdpRequestReceivedMessage.class, protoCfdpRequestReceivedMessage);
				addMessage(cfdpRequestReceivedMessage);
			}

			return getMessages();
		}

	}

	@Override
	public String getRequestId() {
		return requestId;
	}

	@Override
	public ICfdpRequestReceivedMessage setRequestId(final String requestId) {
		this.requestId = requestId;
		return this;
	}

	@Override
	public String getRequesterId() {
		return requesterId;
	}

	@Override
	public ICfdpRequestReceivedMessage setRequesterId(final String requesterId) {
		this.requesterId = requesterId;
		return this;
	}

	@Override
	public String getHttpUser() {
		return httpUser;
	}

	@Override
	public ICfdpRequestReceivedMessage setHttpUser(final String httpUser) {
		this.httpUser = httpUser;
		return this;
	}

	@Override
	public String getHttpHost() {
		return httpHost;
	}

	@Override
	public ICfdpRequestReceivedMessage setHttpHost(final String httpHost) {
		this.httpHost = httpHost;
		return this;
	}

	@Override
	public String getRequestContent() {
		return requestContent;
	}

	@Override
	public ICfdpRequestReceivedMessage setRequestContent(final String requestContent) {
		this.requestContent = requestContent;
		return this;
	}

}
