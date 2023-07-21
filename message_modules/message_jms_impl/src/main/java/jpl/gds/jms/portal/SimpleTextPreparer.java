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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.BaseMessageHeader;
import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * A message preparer that pushes one internal message at a time to JMS
 * publishers as a JMS Text message. Handles multiple message types.
 */
public class SimpleTextPreparer extends AbstractMessagePreparer {
	
	/** Cached message header XML for each message type */
	private final Map<IMessageType, String> cachedWrappers = new HashMap<IMessageType, String>();

    /**
     * Text preparer of messages.
     * 
     * @param appContext the current application context
     * @param type
     *            internal message type; not used by this preparer, so may be
     *            null
     * @param pubs
     *            List of JMS publishers to send messages to
     */
    public SimpleTextPreparer(final ApplicationContext appContext,
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
            jmsDebugLogger.trace("Message prepare is pushing " , messages.size() , " messages");

            /*
             * Use the time of the last internal message as the CREATE_TIME
             * property in the DSMS header.
             */
            final TranslatedMessage lastMessage = messages.get(messages.size() - 1);
            final IAccurateDateTime eventTime = lastMessage.getMessage().getEventTime();

            /*
             * Create the DSMS header and get the XML wrap template,
             * which essentially includes the DSMS XML header (if enabled)
             * and the session XML header, and a placeholder for the XML 
             * message body.
             */
            final BaseMessageHeader header = new BaseMessageHeader(missionProps, type, this.headerContext.getMetadataHeader(),
                    eventTime);          
            final Map<String, Object> headerProps = header.getPropertiesWithStringKeys();
            
            /* The XML wrapper for each message type is cached for efficiency. */
            String wrapTemplate = this.cachedWrappers.get(type);
            if (wrapTemplate == null) {
            	wrapTemplate = header.getWrapTemplate(type);
            	this.cachedWrappers.put(type, wrapTemplate);
            }
            
            /*
             * Append all the message XML to one StringBuilder for
             * the message body.
             */
            final StringBuilder body = new StringBuilder(1024);

            for (final TranslatedMessage tmo: messages) {
                body.append(tmo.getTranslationAsString() + "\n");
            }

            /*
             * Insert the message body into the wrap template.
             */
            final String wholeMsg = String.format(wrapTemplate, body);

            /*
             * Time-to-live and delivery mode must be fetched per message type.
             */
            final long ttl = portalConfig.getMessageConfig(type).getTimeToLive();
            final ExternalDeliveryMode deliveryMode = portalConfig.getMessageConfig(type)
                    .getPersistence();

            /*
             * Push the message content to all publishers. This
             * may block if a publisher is blocked.
             */
            for (final IAsyncTopicPublisher pub: publishers) {
                pub.queueMessageForPublication(type, wholeMsg,
                        headerProps, ttl, deliveryMode);
            }

            /*
             * Update statistics.
             */
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
