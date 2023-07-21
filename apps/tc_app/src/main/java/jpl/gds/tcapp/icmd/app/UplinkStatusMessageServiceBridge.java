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
package jpl.gds.tcapp.icmd.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.message.api.BaseMessageHeader;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.ITopicPublisher;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.IUplinkMetadata;
import jpl.gds.tc.api.icmd.config.IntegratedCommandProperties;
import jpl.gds.tc.api.message.ICpdUplinkStatusMessage;

/**
 * This class is responsible for publishing uplink status messages to the message bus.
 * 
 * It listens on the internal message context for CpdUplinkStatus messages (expecting individual messages).
 * Each message is translated into xml form and wrapped in a DSMS message structure and then published
 * to the topic associated with the message's request id. Session information including topic publishers are
 * cached locally to avoid having to do a lookup for each message.
 * 
 */
public class UplinkStatusMessageServiceBridge extends BaseMessageHandler
{
    /** Retry for closed sessions; at least 2 */
    private static final int SEND_RETRY = 2;

    private static final int LAST_RETRY = SEND_RETRY - 1;

    /** For testing ONLY */
    private boolean fakeFail  = false;
    private int     fakeDelay = 100;

	/** Logging interface */
	private final Tracer trace;
	
	/**
	 * Application's session config
	 */
	private final IContextConfiguration sessionConfig;
	
	/**
	 * The Executor service responsible for running the background thread to
	 * clean up the publisher cache
	 */
	private static final ScheduledExecutorService timedExec = Executors
			.newScheduledThreadPool(1);
	
	private final long ageOut;
	
	private final ApplicationContext appContext;
	

	public class ContextNotFoundException extends Exception {
		private static final long serialVersionUID = 1L;

        /**
         * @param key
         *            the ID for which no context can be found
         */
		public ContextNotFoundException(final String key) {
			super("Context not found for id "+key);
		}
		
	}
	
	/**
     * Probably need some config here to locate database
     * 
     * @param appContext
     *            the Spring Application Context
     * @param sessionConfig2
     *            calling application's session config
     * @param tracer
     *            the Tracer to use for logging
     */
	public UplinkStatusMessageServiceBridge(final ApplicationContext appContext, final IContextConfiguration sessionConfig2, final Tracer tracer) {
		this.appContext = appContext;
		sessionConfig=sessionConfig2;
		this.trace=tracer;
		this.ageOut = appContext.getBean(IntegratedCommandProperties.class)
				                .getMessagePublisherAgeout();
		timedExec.scheduleWithFixedDelay(new PublisherCacheCleanerThread(), ageOut,
				                         ageOut, TimeUnit.MILLISECONDS);
	}
	
	// Cache for topic publishers keyed to topic name
	private final Map<String, TopicPublisher> publisherCache = new HashMap<String,TopicPublisher>();
	
	/**
	 * 
	 * @param topic
	 * @return publisher for given topic
	 * @throws MessageServiceException 
	 */
	private ITopicPublisher getPublisher(final String topic) throws MessageServiceException {
		final TopicPublisher topicPublisher=publisherCache.get(topic);
		ITopicPublisher publisher = null;
		if (topicPublisher==null) {
			publisher= appContext.getBean(IMessageClientFactory.class).getAsyncTopicPublisher(
					TopicNameToken.APPLICATION_COMMAND.getApplicationDataTopic(topic), 
					false, sessionConfig.getGeneralInfo().getOutputDir());
			publisherCache.put(topic, new TopicPublisher(publisher));
		} else {
			publisher = topicPublisher.getPublisher();
			topicPublisher.updateLastUsedTimestamp();
		}
		
		return publisher;
	}
	
	
	/**
	 * Cleanly close all JMS connections
	 */
	public void shutdown() {
		for (final TopicPublisher pub : publisherCache.values()) {
			if(pub.getPublisher() != null) {
				pub.getPublisher().close();
			}
		}
		publisherCache.clear();
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	public void handleMessage(final IMessage m) {
		String topic = null;
		
		try {
			final ICpdUplinkStatusMessage inputMessage = (ICpdUplinkStatusMessage)m;
			final ICpdUplinkStatus status=inputMessage.getStatus(); 
			final IUplinkMetadata metadata=status.getUplinkMetadata();
			topic = metadata.getMessageServiceTopicName();
			
			final int scid = metadata.getScid();
			
            final IAccurateDateTime eventTime = inputMessage.getEventTime();
			final String body = inputMessage.toXml();
			
			// NOTE: It would seem that this call will configure the DSMS header using information from
			// the fudged sessionConfig which is likely to conflict with session configs that might
			// show up in the reflected status messages.
			final BaseMessageHeader header = new BaseMessageHeader(appContext.getBean(MissionProperties.class), 
					inputMessage.getType(), sessionConfig.getMetadataHeader(),
                    eventTime);
			// OVERRIDE SPACECRAFT ID ATTRIBUTE -- PROBABLY NEED TO OVERRIDE OTHERS
			header.setHeaderProperty(MetadataKey.SPACECRAFT_ID, Integer.valueOf(scid).toString());
			
			final String wrapTemplate = header.getWrapTemplate(inputMessage.getType());
			final String outputMessage = String.format(wrapTemplate, body);
			// Construct complete DSMS message
            
            trace.debug("handle status message topic="+topic+" message=\n"+outputMessage);

            for (int i = 0; i < SEND_RETRY; ++i)
            {
                final int attempt = i + 1;

                try
                {
                    final ITopicPublisher sender = getPublisher(topic);

                    if (fakeFail)
                    {
                        if (fakeDelay > 0)
                        {
                            --fakeDelay;
                        }
                        else
                        {
                            fakeFail = false;

                            sender.close();
                        }
                    }

                    // Choosing simplest possible JMS publish API until someone tells me otherwise.
                    sender.publishTextMessage(outputMessage, header.getPropertiesWithStringKeys());

                    // Since we are OK, exit the loop
                    break;
                }
                catch (final MessageServiceException ise)
                {
                    
                    if (ise.isDisconnect()) {
                        // Publisher has been closed, force recreate
    
                        final String message = ise.getCause().getMessage();
    
                        if (i >= LAST_RETRY)
                        {
                            // Give up
    
                            trace.error("Topic "                            +
                                        topic                               +
                                        " could not be recreated, attempt " +
                                        attempt                             +
                                        ": "                                +
                                        message                             );
    
                            throw ise;
                        }
    
                        publisherCache.remove(topic);
    
                        trace.warn("Topic "               +
                                   topic                  +
                                   " recreated, attempt " +
                                   attempt                +
                                   ": "                   +
                                   message);
                    
                    } else {
                        throw ise;
                    }
                }
            }

			/*
			// keep cache from growing infinitely. Note that you can have many concurrent requests
			// all sharing the same topic. This caching scheme will work allow them all to share the
			// same publisher until the first one returns a final status, which will close the publisher.
			// The next non-final request will open publisher again until the next final status, etc.
			// There is no way to be more efficient without knowing something about how topics are being
			// used by the potential requests. At least this ensures that last one out closes the door.
			if (status.getStatus().isFinal()) {
				publisherCache.remove(topic);
				sender.close();
			}
			*/
		} catch (final MessageServiceException x) {

			// Get rid of ugly exception if JMS is down and print something nice.
			if ((x.getMessage() != null && x.getMessage().contains("Connection refused")) || 
					(x.getCause() != null && x.getCause() instanceof java.net.ConnectException)) {
				trace.error("Attempt to create JMS publisher on topic " + topic + " failed. The JMS server is probably down.");

			} else {
				trace.warn("Message dropped due to error: ",x);
			}

		} catch (final Exception x) {
			trace.warn("Message dropped due to error: ",x);
		}
	}

	/**
	 * A wrapper class to provide a last used timestamp for topic publishers
	 */
	private class TopicPublisher {
		/** The publisher */
		private final ITopicPublisher publisher;
		/** The last used timestamp */
		private long lastUsed;
		
		/**
		 * Constructor
		 * @param publisher the publisher to wrap
		 */
		public TopicPublisher(final ITopicPublisher publisher) {
			this.publisher = publisher;
		}
		
		/**
		 * Update the last used timestamp to current time
		 */
		public void updateLastUsedTimestamp() {
			this.lastUsed = System.currentTimeMillis();
		}
		
		/**
		 * Get the last used timestamp
		 * @return last used timestamp in milliseconds
		 */
		public long getLastUsedTimestamp() {
			return this.lastUsed;
		}
		
		/**
		 * Get the publisher wrapped by this class
		 * @return the publisher wrapped by this class
		 */
		public ITopicPublisher getPublisher() {
			return this.publisher;
		}
	}
	
	private class PublisherCacheCleanerThread implements Runnable{
		@Override
		public void run() {
			final List<String> topicsToClear = new ArrayList<String>(publisherCache.size());
			
			for (final String k : publisherCache.keySet()) {
				final TopicPublisher pub = publisherCache.get(k);
				final long age = System.currentTimeMillis() - pub.getLastUsedTimestamp();
				if(pub.getPublisher() != null && age > UplinkStatusMessageServiceBridge.this.ageOut) {
					pub.getPublisher().close();
					topicsToClear.add(k);
				}
			}
			
			for(final String t : topicsToClear) {
				publisherCache.remove(t);
			}
		}
	}
}
