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

package jpl.gds.cfdp.message.impl.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.cfdp.message.api.CfdpMessageApiBeans;
import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpFileGenerationMessage;
import jpl.gds.cfdp.message.api.ICfdpFileUplinkFinishedMessage;
import jpl.gds.cfdp.message.api.ICfdpIndicationMessage;
import jpl.gds.cfdp.message.api.ICfdpMessageHeader;
import jpl.gds.cfdp.message.api.ICfdpPduReceivedMessage;
import jpl.gds.cfdp.message.api.ICfdpPduSentMessage;
import jpl.gds.cfdp.message.api.ICfdpRequestReceivedMessage;
import jpl.gds.cfdp.message.api.ICfdpRequestResultMessage;
import jpl.gds.cfdp.message.impl.CfdpFileGenerationMessage;
import jpl.gds.cfdp.message.impl.CfdpFileUplinkFinishedMessage;
import jpl.gds.cfdp.message.impl.CfdpIndicationMessage;
import jpl.gds.cfdp.message.impl.CfdpMessageHeader;
import jpl.gds.cfdp.message.impl.CfdpPduReceivedMessage;
import jpl.gds.cfdp.message.impl.CfdpPduSentMessage;
import jpl.gds.cfdp.message.impl.CfdpRequestReceivedMessage;
import jpl.gds.cfdp.message.impl.CfdpRequestResultMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpFileGenerationMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpFileUplinkFinishedMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpIndicationMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpPduReceivedMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpPduSentMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpRequestReceivedMessage;
import jpl.gds.serialization.cfdp_message_impl.Proto3CfdpRequestResultMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;

/**
 * Spring bootstrap configuration class for CFDP message projects.
 * 
 * @since R8
 */
@Configuration
public class CfdpMessageSpringBootstrap {

	@Autowired
	private ApplicationContext appContext;

	/**
	 * Constructor.
	 */
	public CfdpMessageSpringBootstrap() {
		MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CfdpMessageType.CfdpIndication, null,
				CfdpIndicationMessage.BinaryParseHandler.class.getName()));
		MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CfdpMessageType.CfdpFileGeneration, null,
				CfdpFileGenerationMessage.BinaryParseHandler.class.getName()));
		MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CfdpMessageType.CfdpFileUplinkFinished,
				null, CfdpFileUplinkFinishedMessage.BinaryParseHandler.class.getName()));
		MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CfdpMessageType.CfdpRequestReceived,
				null, CfdpRequestReceivedMessage.BinaryParseHandler.class.getName()));
		MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CfdpMessageType.CfdpRequestResult, null,
				CfdpRequestResultMessage.BinaryParseHandler.class.getName()));
		MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CfdpMessageType.CfdpPduReceived, null,
				CfdpPduReceivedMessage.BinaryParseHandler.class.getName()));
		MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CfdpMessageType.CfdpPduSent, null,
				CfdpPduSentMessage.BinaryParseHandler.class.getName()));
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_INDICATION_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpIndicationMessage getCfdpIndicationMessage() {
		return new CfdpIndicationMessage(appContext);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_INDICATION_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpIndicationMessage getCfdpIndicationMessage(final Proto3CfdpIndicationMessage msg)
			throws InvalidProtocolBufferException {
		return new CfdpIndicationMessage(appContext, msg);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_FILE_GENERATION_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpFileGenerationMessage getCfdpFileGenerationMessage() {
		return new CfdpFileGenerationMessage(appContext);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_FILE_GENERATION_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpFileGenerationMessage getCfdpFileGenerationMessage(final Proto3CfdpFileGenerationMessage msg)
			throws InvalidProtocolBufferException {
		return new CfdpFileGenerationMessage(appContext, msg);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_FILE_UPLINK_FINISHED_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpFileUplinkFinishedMessage getCfdpUplinkFinishedMessage() {
		return new CfdpFileUplinkFinishedMessage(appContext);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_FILE_UPLINK_FINISHED_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpFileUplinkFinishedMessage getCfdpUplinkFinishedMessage(final Proto3CfdpFileUplinkFinishedMessage msg)
			throws InvalidProtocolBufferException {
		return new CfdpFileUplinkFinishedMessage(appContext, msg);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_REQUEST_RECEIVED_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpRequestReceivedMessage getCfdpRequestReceivedMessage() {
		return new CfdpRequestReceivedMessage(appContext);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_REQUEST_RECEIVED_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpRequestReceivedMessage getCfdpRequestReceivedMessage(final Proto3CfdpRequestReceivedMessage msg)
			throws InvalidProtocolBufferException {
		return new CfdpRequestReceivedMessage(appContext, msg);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_REQUEST_RESULT_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpRequestResultMessage getCfdpRequestResultMessage() {
		return new CfdpRequestResultMessage(appContext);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_REQUEST_RESULT_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpRequestResultMessage getCfdpRequestResultMessage(final Proto3CfdpRequestResultMessage msg)
			throws InvalidProtocolBufferException {
		return new CfdpRequestResultMessage(appContext, msg);
	}
	
	@Bean(name = CfdpMessageApiBeans.CFDP_PDU_RECEIVED_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpPduReceivedMessage getCfdpPduReceivedMessage() {
		return new CfdpPduReceivedMessage(appContext);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_PDU_RECEIVED_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpPduReceivedMessage getCfdpPduReceivedMessage(final Proto3CfdpPduReceivedMessage msg)
			throws InvalidProtocolBufferException {
		return new CfdpPduReceivedMessage(appContext, msg);
	}
	
	@Bean(name = CfdpMessageApiBeans.CFDP_PDU_SENT_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpPduSentMessage getCfdpPduSentMessage() {
		return new CfdpPduSentMessage(appContext);
	}

	@Bean(name = CfdpMessageApiBeans.CFDP_PDU_SENT_MESSAGE)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpPduSentMessage getCfdpPduSentMessage(final Proto3CfdpPduSentMessage msg)
			throws InvalidProtocolBufferException {
		return new CfdpPduSentMessage(appContext, msg);
	}
	
	@Bean(name = CfdpMessageApiBeans.CFDP_MESSAGE_HEADER)
	@Scope("prototype")
	@Lazy(value = true)
	public ICfdpMessageHeader getCfdpMessageHeader() {
		return new CfdpMessageHeader();
	}

}
