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
package jpl.gds.message.api.external;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.ByteString;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.message.api.BaseMessageHeader;
import jpl.gds.serialization.block.Proto3MessageBlock;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.IXmlMessageParseHandler;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * Messaging utility class for serializing, de-serializing, and formatting
 * messages.
 */
public class BaseExternalMessageUtility {

    private final Tracer trace;

    /**
     * Constructor that takes in the tracer being used
     * 
     * @param trace
     *            the current application tracer
     */
    public BaseExternalMessageUtility(final Tracer trace) {
        this.trace = trace;
    }


	private final Map<String, Class<?>> parseClasses = new HashMap<>();

	/**
	 * Parses a series of IMessages out of an XML stream. The stream is assumed
	 * to contain the aggregate XML for a series of messages with the same
	 * internal type.
	 * 
	 * @param xml
	 *            the XML text to parse
	 * @param type
	 *            The IMessageConfiguration object for the message type being
	 *            parsed
	 * @return array of parsed IMessage objects
	 * 
	 * @throws ClassNotFoundException
	 *             if the message class or its parsing class cannot be found
	 * @throws NoSuchMethodException
	 *             if the constructor for the message parsing class cannot be
	 *             found
	 */
	public IMessage[] parseFromXml(final String xml,
			final IMessageType type) throws ClassNotFoundException,
			NoSuchMethodException {
		return parseFromXml(xml, type.getSubscriptionTag());

	}

	/**
	 * Parses a series of IMessages out of an XML stream. The stream is assumed
	 * to contain the aggregate XML for a series of messages with the same
	 * internal type.
	 * 
	 * @param xml
	 *            the XML text to parser
	 * @param subscriptionTagOrClass
	 *            the internal subscription tag/type for the message type being
	 *            parsed; can also just be the class name of the XML parser for
	 *            the message type in question
	 * @return array of parsed IMessage objects
	 * 
	 * @throws ClassNotFoundException
	 *             if the message class or its parsing class cannot be found
	 * @throws NoSuchMethodException
	 *             if the constructor for the message parsing class cannot be
	 *             found
	 */
	private IMessage[] parseFromXml(final String xml,
			final String subscriptionTagOrClass) throws ClassNotFoundException,
			NoSuchMethodException {
		final IMessageConfiguration config = MessageRegistry
				.getMessageConfig(subscriptionTagOrClass);
		final String className = config == null ? subscriptionTagOrClass
				: config.getXmlParserClassName();

		Class<?> parserClass = parseClasses.get(className + ".XmlParser");
		if (parserClass == null) {
			parserClass = Class.forName(className);
			parseClasses.put(className + ".XmlParser", parserClass);
		}

		IXmlMessageParseHandler handler;
		try {
			handler = (IXmlMessageParseHandler) ReflectionToolkit.createObject(
					parserClass, new Class[0], new Object[0]);
		} catch (final ReflectionException e1) {
			throw new UnsupportedOperationException(
					"Trying to parse XML message that provides no XML handler", e1);
		}
		try {
			return handler.parse(xml);

		} catch (final Exception e) {
            TraceManager.getDefaultTracer()
                        .error(
					"Error parsing message using XML parser class "
							+ subscriptionTagOrClass, e);

		}

		return null;

	}

	/**
	 * Parses a series of IMessages out of a byte array. The array is assumed to
	 * contain the aggregate binary for a series of messages with the same
	 * internal type.
	 * 
	 * @param content
	 *            the binary data to parse
	 * @param type
	 *            The IMessageConfiguration object for the message type being
	 *            parsed
	 * @return array of parsed IMessage objects
	 * 
	 * @throws ClassNotFoundException
	 *             if the message class or its parsing class cannot be found
	 * @throws NoSuchMethodException
	 *             if the constructor for the message parsing class cannot be
	 *             found
	 */
    public IMessage[] parseFromBinary(final List<byte[]> content,
			final IMessageType type) throws ClassNotFoundException,
			NoSuchMethodException {
        return parseFromBinary(content, type.getSubscriptionTag());
	}

	/**
	 * Parses a series of IMessages out of a byte array. The array is assumed to
	 * contain the aggregate binary for a series of messages with the same
	 * internal type.
	 * 
	 * @param content
	 *            the binary data to parse
	 * @param subscriptionTagOrClass
	 *            the internal subscription tag/type for the message type being
	 *            parsed; can also just be the class name of the XML parser for
	 *            the message type in question
	 * @return array of parsed IMessage objects
	 * 
	 * @throws ClassNotFoundException
	 *             if the message class or its parsing class cannot be found
	 */
    private IMessage[] parseFromBinary(final List<byte[]> content, final String subscriptionTagOrClass)
            throws ClassNotFoundException {

		final IMessageConfiguration config = MessageRegistry
				.getMessageConfig(subscriptionTagOrClass);
		final String className = config == null ? subscriptionTagOrClass
				: config.getBinaryParserClassName();

		Class<?> parserClass = parseClasses.get(className + ".BinaryParser");
		if (parserClass == null) {
			parserClass = Class.forName(className);
			parseClasses.put(className + ".BinaryParser", parserClass);
		}

		IBinaryMessageParseHandler handler;
		try {
			handler = (IBinaryMessageParseHandler) ReflectionToolkit
					.createObject(parserClass, new Class[0], new Object[0]);
		} catch (final ReflectionException e1) {
			throw new UnsupportedOperationException(
					"Trying to parse binary message that provides no binary handler");
		}
		try {
            return handler.parse(content);

		} catch (final Exception e) {
            trace.error(
					"Error parsing message using binary parser class "
							+ subscriptionTagOrClass, e);

		}

		return null;
	}

	
	/**
	 * Creates the aggregated XML, suitable for external publication, for a
	 * series of IMessages.
	 * 
	 * @param props
	 *            the current MissionProperties object
	 * @param msgs
	 *            the IMessage objects to serialize to XML
	 * @param context
	 *            the metadata context header to attach to the resulting XML
	 *            text
	 * @return the message contents as XML
	 */
	public String createExternalXml(final MissionProperties props,
			final IMessage[] msgs, final ISerializableMetadata context) {

        final IMessageType type = msgs[0].getType();
        final IAccurateDateTime eventTime = new AccurateDateTime();
		final BaseMessageHeader header = new BaseMessageHeader(props,
				msgs[0].getType(), context, eventTime);
		final StringBuilder body = new StringBuilder();
		for (int i = 0; i < msgs.length; i++) {
			body.append(msgs[i].toXml() + "\n");
		}
		final String wholeMsg = header
				.wrapContent(type, body.toString());
		return wholeMsg;
	}

	/**
	 * Creates the aggregated binary, suitable for external publication, for a
	 * series of IMessages.
	 * 
	 * @param props
	 *            the current MissionProperties object
	 * @param msgs
	 *            the IMessage objects to serialize to binary
	 * @param context
	 *            the metadata context header to attach to the resulting binary
	 * @return the message contents as binary
	 */
	public byte[] createExternalBinary(final MissionProperties props,
			final IMessage[] msgs, final ISerializableMetadata context) {

        final IAccurateDateTime eventTime = new AccurateDateTime();
		final BaseMessageHeader header = new BaseMessageHeader(props,
				msgs[0].getType(), context, eventTime);

        final Proto3MessageBlock.Builder block = Proto3MessageBlock.newBuilder();

        block.setHeader(header.getContextHeader().build());

		for (int i = 0; i < msgs.length; i++) {
            block.addMessageBytes(ByteString.copyFrom(msgs[i].toBinary()));
		}

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            block.build().writeDelimitedTo(baos);
        }
        catch (final IOException e) {
            // shouldn't throw here, as the message is already in a byte array
			// and the output stream isn't getting blocked
            trace.warn("Error in serializing message into message block. ", e);
        }

        return baos.toByteArray();
	}



	/**
	 * Instantiates (de-serializes) a series of IMessage objects from the given
	 * XML stream, which is assumed to contain messages of only one type.
	 * 
	 * @param xml
	 *            the XML to parse
	 * @param type
	 *            the IMessageConfiguration object for the message type in
	 *            question
	 * @param context
	 *            the current ApplicationContext
	 * @return array of parsed IMessages
	 * 
	 * @throws ClassNotFoundException
	 *             if the message class or message parser class cannot be found
	 * @throws NoSuchMethodException
	 *             if the constructor for the message parser cannot be created
	 */
	public IMessage[] instantiateExternalMessages(final String xml,
            final IMessageType type,
            final ApplicationContext context)
			throws ClassNotFoundException, NoSuchMethodException {

		return parseFromXml(xml, type.getSubscriptionTag(), context);
	}

	/**
     * Instantiates (de-serializes) a series of IMessage objects from the given
     * byte stream, which is assumed to contain messages of only one type.
     * 
     * @param blob
     *            the byte array to parse
     * @param type
     *            the IMessageConfiguration object for the message type in
     *            question
     * @param context
     *            the current ApplicationContext
     * @return array of parsed IMessages
     * 
     * @throws ClassNotFoundException
     *             if the message class or message parser class cannot be found
     * @throws NoSuchMethodException
     *             if the constructor for the message parser cannot be created
     */
	public IMessage[] instantiateExternalMessages(final byte[] blob,
            final IMessageType type,
            final ApplicationContext context)
            throws ClassNotFoundException, NoSuchMethodException {

		return instantiateExternalMessages(blob, type.getSubscriptionTag(),
				context);
	}

	/**
     * Instantiates (de-serializes) a series of IMessage objects from the given
     * byte stream, which is assumed to contain messages of only one type.
     * 
     * @param blob
     *            the byte array to parse
     * @param subscriptionTag
     *            the internal message type/subscription tag for the message
     *            type in question
     * @param context
     *            the current ApplicationContext
     * @return array of parsed IMessages
     * 
     * @throws ClassNotFoundException
     *             if the message class or message parser class cannot be found
     * @throws NoSuchMethodException
     *             if the constructor for the message parser cannot be created
     */
	private IMessage[] instantiateExternalMessages(final byte[] blob,
			final String subscriptionTag, final ApplicationContext context)
            throws ClassNotFoundException, NoSuchMethodException {

        final ByteArrayInputStream bais = new ByteArrayInputStream(blob);
        final List<byte[]> messages = new ArrayList<>();

		try{

            final Proto3MessageBlock block = Proto3MessageBlock.parseDelimitedFrom(bais);

            for (final ByteString msg : block.getMessageBytesList()) {
                messages.add(msg.toByteArray());
            }

		} catch (final IOException e) {
            TraceManager.getTracer(context, Loggers.BUS)
                        .warn("Error encountered while attempting to read the message block", e);
		}

        return parseFromBinary(messages, subscriptionTag, context);

	}

	private IMessage[] parseFromXml(final String xml,
			final String subscriptionTagOrClass, final ApplicationContext context)
			throws ClassNotFoundException, NoSuchMethodException {
		final IMessageConfiguration config = MessageRegistry
				.getMessageConfig(subscriptionTagOrClass);
		final String className = config == null ? subscriptionTagOrClass
				: config.getXmlParserClassName();

		Class<?> parserClass = parseClasses.get(className + ".XmlParser");
		if (parserClass == null) {
			parserClass = Class.forName(className);
			parseClasses.put(className + ".XmlParser", parserClass);
		}

		IXmlMessageParseHandler handler = null;
		try {
			handler = (IXmlMessageParseHandler) ReflectionToolkit.createObject(
					parserClass, new Class[] { ApplicationContext.class },
					new Object[] { context });

		} catch (final ReflectionException e1) {
			// do nothing. try next constructor
		}

		if (handler == null) {
			try {
				handler = (IXmlMessageParseHandler) ReflectionToolkit
						.createObject(parserClass, new Class[] {},
								new Object[] {});
			} catch (final ReflectionException e1) {
				e1.printStackTrace();
				throw new UnsupportedOperationException(
						"Trying to parse XML message that provides no XML handler");
			}
		}

		try {
			return handler.parse(xml);

		} catch (final Exception e) {
            trace.error("Error parsing message using XML parser class " + subscriptionTagOrClass, e);
		}

		return null;

	}

    private IMessage[] parseFromBinary(final List<byte[]> messages,
			final String subscriptionTagOrClass, final ApplicationContext context)
			throws ClassNotFoundException, NoSuchMethodException {

		final IMessageConfiguration config = MessageRegistry
				.getMessageConfig(subscriptionTagOrClass);
		final String className = config == null ? subscriptionTagOrClass
				: config.getBinaryParserClassName();

		Class<?> parserClass = parseClasses.get(className + ".BinaryParser");
		if (parserClass == null) {
			parserClass = Class.forName(className);
			parseClasses.put(className + ".BinaryParser", parserClass);
		}

		IBinaryMessageParseHandler handler = null;
		try {
			handler = (IBinaryMessageParseHandler) ReflectionToolkit
					.createObject(parserClass,
							new Class[] { ApplicationContext.class },
							new Object[] { context });
		} catch (final ReflectionException e1) {
            e1.printStackTrace();
		}

		if (handler == null) {
			try {
				handler = (IBinaryMessageParseHandler) ReflectionToolkit
						.createObject(parserClass, new Class[] {},
								new Object[] {});
			} catch (final ReflectionException e1) {
				throw new UnsupportedOperationException(
						"Trying to parse binary message that provides no binary handler", e1);
			}
		}
		try {
            return handler.parse(messages);

		} catch (final Exception e) {
			trace.error(
					"Error parsing message using binary parser class "
							+ subscriptionTagOrClass, e);
			e.printStackTrace();

		}

		return null;
	}
}
