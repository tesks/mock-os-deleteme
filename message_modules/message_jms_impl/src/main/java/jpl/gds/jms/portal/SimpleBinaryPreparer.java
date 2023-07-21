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
package jpl.gds.jms.portal;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.google.protobuf.ByteString;

import jpl.gds.message.api.BaseMessageHeader;
import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.serialization.block.Proto3MessageBlock;
import jpl.gds.serialization.metadata.Proto3MetadataMap;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * A message preparer that pushes one internal message at a time to JMS
 * publishers as a binary JMS Bytes message. Handles multiple message types.
 */
public class SimpleBinaryPreparer extends AbstractMessagePreparer {

    private Proto3MetadataMap headerMsg = null;

    /**
     * Constructor.
     * 
     * @param appContext
     *            the current application context
     * @param type
     *            internal message type; not used by this class, so may be null,
     *            and the shared message type constant will be returned by
     *            getMessageType().
     * @param pubs
     *            list of publishers to send messages to
     */
    public SimpleBinaryPreparer(final ApplicationContext appContext,
    		final IMessageType type, 
            final List<IAsyncTopicPublisher> pubs) {
        super(appContext, type, pubs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized boolean pushToPublishers(final List<TranslatedMessage> messages,
            final IMessageType type) {

        try {
            jmsDebugLogger.trace("Preparer for type " , type , " is pushing " ,
                    messages.size() , " messages");

            /*
             * Use the time of the last internal message as the CREATE_TIME
             * property in the DSMS header.
             */
            final TranslatedMessage lastMessage = messages.get(messages.size() - 1);
            final IAccurateDateTime eventTime = lastMessage.getMessage().getEventTime();

			/*
             * The session header proto message will be cached the first time a
             * BaseMessageHeader instance is created for the current session.
             * This means that any change to session information does not get
             * reflected in the outgoing headers.
             */
            final BaseMessageHeader header = new BaseMessageHeader(missionProps, type, this.headerContext.getMetadataHeader(),
                    eventTime);
            final Map<String, Object> headerProps = header.getPropertiesWithStringKeys();

            if (headerMsg == null) {
                headerMsg = header.getContextHeader().build();
            }

            final Proto3MessageBlock.Builder block = Proto3MessageBlock.newBuilder();

            block.setHeader(headerMsg);

            for (final TranslatedMessage tmo : messages) {
                block.addMessageBytes(ByteString.copyFrom(tmo.getTranslationAsBytes()));
            }

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            block.build().writeDelimitedTo(baos);

            /*
             * Get the time to live and deliver mode from the configuration
             * for the current message type.
             */
            final long ttl = portalConfig.getMessageConfig(type).getTimeToLive();
            final ExternalDeliveryMode deliveryMode = portalConfig.getMessageConfig(type)
                    .getPersistence();

            /*
             * Push the message content to all the publishers.
             */
            for (final IAsyncTopicPublisher pub: publishers) {
                pub.queueMessageForPublication(type,
                                               baos.toByteArray(), headerProps, ttl, deliveryMode);
            }

            publishTotal++;            
            addToMessageCount(type, messages.size());
            
            return true;
        } catch (final Throwable t) {
        	jmsDebugLogger.error("Unable to publish " , type ,
                      " message(s) to JMS.", ExceptionTools.getMessage(t), t);
            t.printStackTrace();
        }
        return false;
    }
}
