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
package jpl.gds.jms;

import javax.jms.JMSException;
import javax.naming.NamingException;

import org.springframework.context.ApplicationContext;

import jpl.gds.jms.activemq.ActivemqAsynchronousTopicPublisher;
import jpl.gds.jms.activemq.ActivemqSharedConnectionTopicSubscriber;
import jpl.gds.jms.activemq.ActivemqTopicPublisher;
import jpl.gds.jms.activemq.ActivemqTopicSubscriber;
import jpl.gds.jms.config.JmsMessageProvider;
import jpl.gds.jms.config.JmsProperties;
import jpl.gds.jms.util.JmsMessageServiceExceptionFactory;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.ITopicPublisher;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;


/**
 * Factory to create JMS topic publisher and subscriber objects.
 */
public final class JmsClientFactory implements IMessageClientFactory {
    
    private final ApplicationContext appContext;
    private final int transactionSize;
    
    /**
     * Constructor.
     * 
     * @param appContext the current application context
     */
    public JmsClientFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.transactionSize = appContext.getBean(JmsProperties.class).getPublishCommitSize();
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IMessageClientFactory#getTopicSubscriber(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public ITopicSubscriber getTopicSubscriber(
            final String topic,
            final String messageFilter,
            final boolean shared)
            throws MessageServiceException {
        
        try {
            if (isActivemq()) {
                if (shared) {
                return new ActivemqSharedConnectionTopicSubscriber(
                        appContext,
                        new JmsTopicConfiguration(topic), null, messageFilter, false);
                } else {
                    return new ActivemqTopicSubscriber(
                            appContext,
                            new JmsTopicConfiguration(topic), null, messageFilter, false);
                }
            }
     
            throw new MessageServiceException("The configured JMS provider is not recognized: " +  
                    appContext.getBean(JmsProperties.class).getRealtimePublicationProvider());
            
        } catch (NamingException | JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Failed to create message subscriber for topic " + topic, e);
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IMessageClientFactory#getDurableTopicSubscriber(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public ITopicSubscriber getDurableTopicSubscriber(
    		final String topic, final String name,
            final String messageFilter, 
            final boolean shared)
            throws MessageServiceException {
    	
        try {
            if (isActivemq()) {
                if (shared) {
                    return new ActivemqSharedConnectionTopicSubscriber(
                            appContext,
                            new JmsTopicConfiguration(topic), name, messageFilter, false);
                } else {
                    return new ActivemqTopicSubscriber(
                            appContext,
                            new JmsTopicConfiguration(topic), null, messageFilter, false);
                }
            }
     
            throw new MessageServiceException("The configured JMS provider is not recognized: " +  
                    appContext.getBean(JmsProperties.class).getRealtimePublicationProvider());
           
        } catch (NamingException | JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Failed to create message subscriber for topic " + topic, e);
        }
    }

	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IMessageClientFactory#getTopicPublisher(java.lang.String, boolean)
     */
    @Override
    public ITopicPublisher getTopicPublisher(
            final String topic, final boolean persistent)
            throws MessageServiceException {

        try {
            if (isActivemq()) {
                    return new ActivemqTopicPublisher(appContext, new JmsTopicConfiguration(topic, persistent),
                            1);
                
            }
            else {
            	/*
            	 * We were creating a non-provider-specific JMS class here, which we can no longer do
            	 * because the class has been made abstract. This was the wrong thing to do anyway. If
            	 * we do not know the JMS provider, what is it we think we are doing?
            	 */
               throw new MessageServiceException("The configured JMS provider is not recognized: " +  
            	 appContext.getBean(JmsProperties.class).getRealtimePublicationProvider());
            }
        } catch (final Exception e) {
            throw JmsMessageServiceExceptionFactory.createException("Failed to create message publisher for topic " + topic, e);
        }
    }

	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IMessageClientFactory#getTransactedAsyncTopicPublisher(java.lang.String, boolean, java.lang.String)
     */
   @Override
   public IAsyncTopicPublisher getTransactedAsyncTopicPublisher(
           final String topic, final boolean persistent, 
           final String spillDir)
           throws MessageServiceException {

	   IAsyncTopicPublisher publisher;

       if (isActivemq()) {
               try {
                publisher = new ActivemqAsynchronousTopicPublisher(
                           appContext, 
                           new JmsTopicConfiguration(topic, persistent), transactionSize, spillDir);
            } catch (final Exception e) {
                throw JmsMessageServiceExceptionFactory.createException("Could not create message publisher for topic " + topic, 
                        e);
            }
      
       } else {
           /*
            * We were creating a non-provider-specific JMS class here, which we can no longer do
            * because the class has been made abstract. This was the wrong thing to do anyway. If
            * we do not know the JMS provider, what is it we think we are doing?
            */
    	   throw new MessageServiceException("There is no JMS provider configured");
       }

       /* Used publisher start method instead of creating the thread here. */
       publisher.start();

       return publisher;
   }

	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IMessageClientFactory#getAsyncTopicPublisher(java.lang.String, boolean, java.lang.String)
     */
	@Override
    public IAsyncTopicPublisher getAsyncTopicPublisher(
			final String topic, final boolean persistent, final String spillDir)
			throws MessageServiceException {
	    IAsyncTopicPublisher publisher;

	       if (isActivemq()) {
	               try {
	                publisher = new ActivemqAsynchronousTopicPublisher(
	                           appContext, 
	                           new JmsTopicConfiguration(topic, persistent), 1, spillDir);
	            } catch (final Exception e) {
	                throw JmsMessageServiceExceptionFactory.createException("Could not create message publisher for topic " + topic, 
	                        e);
	            }
	      
	       } else {
               /*
                * We were creating a non-provider-specific JMS class here, which we can no longer do
                * because the class has been made abstract. This was the wrong thing to do anyway. If
                * we do not know the JMS provider, what is it we think we are doing?
                */
	           throw new MessageServiceException("There is no JMS provider configured");
	       }
	       publisher.start();

	       return publisher;
	}
   
	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IMessageClientFactory#getNonPersistentTopicPublisher(java.lang.String)
     */
	@Override
    public ITopicPublisher getNonPersistentTopicPublisher(
			final String topic)
			throws MessageServiceException {
		return (getTopicPublisher(topic, false));
	}

    /**
     * Returns whether ActiveMQ is the defined JMS service.
     * 
     * @return if the JMS is activemq
     */
    private boolean isActivemq() {
        return appContext.getBean(JmsProperties.class).getRealtimePublicationProvider() == JmsMessageProvider.ACTIVEMQ;
    }

}
