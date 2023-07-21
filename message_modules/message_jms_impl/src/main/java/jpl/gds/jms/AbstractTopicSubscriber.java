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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.jms.config.JmsProperties;
import jpl.gds.jms.message.JmsExternalMessage;
import jpl.gds.jms.util.JmsMessageServiceExceptionFactory;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.config.JndiProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * Convenience class for subscribing to JMS topics. Pass in a configuration,
 * then either poll for messages or extend the callback methods.
 */
public abstract class AbstractTopicSubscriber implements ITopicSubscriber {
    /**
     * Thread class used to shutdown the current process.
     */
    public class ShutdownThread implements Runnable {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
		public void run() {
            if (subscriber != null) {
                try {
                    subscriber.close();
                } catch (final JMSException e) {
                    // Ignore this - nothing we can do 
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (final JMSException e) {
                	 // ignore this - nothing we can do 
                }
            }
            if (connection != null) {
                try {
                    connection.stop();
                    connection.close();
                } catch (final JMSException e) {
                	// nothing we can do
                }
            }
            stopped = true;
            connection = null;
        }
    }

    /** Shared trace logger */
    protected final Tracer         log;
    
    private boolean stopped;
    
    /**
     * Connection to the JMS.
     */
    protected TopicConnection connection;

    /**
     * Session to the JMS.
     */
    protected TopicSession session;

    /**
     * Subscriber to the JMS topic.
     */
    protected TopicSubscriber subscriber;

    /**
     * Subscriber name.
     */
    protected String subscriberName;
    /**
     * Whether the connection is transacted.
     */
    protected boolean transacted;
    
    /**
     * The current JMS configuration object.
     */
    protected JmsProperties jmsConfig;

    /**
     * Gds property for JMS close timeout.
     */
    private final long jmsCloseTime;
    
    /**
     * The current JNDI properties object.
     */
    protected final JndiProperties jndiProperties;
    

    /**
     * Protected default constructor.
     * @param context the current application context
     */
    protected AbstractTopicSubscriber(final ApplicationContext context) {
    	jmsConfig = context.getBean(JmsProperties.class);
        jmsConfig.init(); // complete initialization (if required) before use.
    	jmsCloseTime = jmsConfig.getSubscriberCloseTimeout();
    	jndiProperties = context.getBean(JndiProperties.class);
        this.log = TraceManager.getTracer(context, Loggers.JMS);
    }

    /**
     * Constructor which initializes the JMS connection and session if one has
     * not already been established.
     * 
     * @param appContext the current application context
     * 
     * @param config
     *            JMS Topic configuration used to configure the session topic.
     * @param name
     *            Subscriber name.
     * @param filter
     *            Filter for subscriber.
     * @param isTransacted
     *            Whether the session is transacted.
     * @throws NamingException
     *             Thrown if unable to looked up the class.
     * @throws JMSException
     *             Throw if unable to establish a connection to the JMS.
     */
    public AbstractTopicSubscriber(
    		final ApplicationContext appContext,
            final JmsTopicConfiguration config, final String name,
            final String filter, final boolean isTransacted)
            throws NamingException, JMSException {
    	
    	this(appContext);
    	
        this.transacted = isTransacted;
            final Context context = createNamingContext(config);
            final TopicConnectionFactory factory = (TopicConnectionFactory) context
                    .lookup(jmsConfig.getTopicFactoryName());
            createConnection(factory);
            session = connection.createTopicSession(isTransacted,
                    isTransacted ? Session.SESSION_TRANSACTED
                            : Session.AUTO_ACKNOWLEDGE);
        

        final Topic topic = session.createTopic(config.getTopicName());

        if (name != null) {
            this.subscriberName = name;
            this.subscriber = session.createDurableSubscriber(topic, name,
                    filter, false);
        } else {
            this.subscriber = session.createSubscriber(topic, filter, false);
        }
    }

    /**
     * Creates a TopicConnection given a TopicConnectionFactory.
     * 
     * @param factory the TopicConnectionFactory.
     * @throws JMSException if there is a JMS error creating the connection
     */
    protected void createConnection(final TopicConnectionFactory factory) throws JMSException {
        final String userName = jndiProperties.getSecurityPrincipal();
        final String userCredential = jndiProperties.getSecurityCredentials();
        if ((userName == null) || (userCredential == null)) {
            connection = factory.createTopicConnection();
        } else {
            connection = factory.createTopicConnection(userName,
                    userCredential);
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#close()
     */
    @Override
    public void close() {

        // Since JMS close sometimes hangs, we close on another thread, so we
        // can kill it after the close timeout regardless
        final Thread stopThread = new Thread(new ShutdownThread(), "Subscriber Shutdown Thread");
        stopThread.start();
        long totalSleep = 0;
        while (!stopped && totalSleep < jmsCloseTime) {
            SleepUtilities.checkedSleep(500);
            totalSleep += 500;
        }
        stopThread.interrupt();
        connection = null;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#closeNoDisconnect()
     */
    @Override
    public void closeNoDisconnect() {
        session = null;
        this.subscriber = null;
        connection = null;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#commit()
     */
    @Override
    public void commit() throws MessageServiceException {
        try {
            if (this.transacted) {
                session.commit();
            }
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to commit message subscriber transaction", e);
        }
    }

    /**
     * Creates a context based off the configurations.
     * 
     * @param config
     *            Context configurations.
     * @return New context based off of the configuration.
     * @throws NamingException
     *             Thrown if unable to initialize the context.
	 */
    @SuppressWarnings("PMD.ReplaceHashtableWithMap")
    protected Context createNamingContext(final JmsTopicConfiguration config)
            throws NamingException {
        final Hashtable<String, Object> table = new Hashtable<>();
        final Properties p = jndiProperties.asProperties();
        for (final Iterator<Object> i = p.keySet().iterator(); i.hasNext();) {
            final String key = (String) i.next();
            table.put(key, p.getProperty(key));
        }
        return new InitialContext(table);
    }

    /**
     * Returns the topic connection.
     * 
     * @return JMS topic connection object
     */
    protected TopicConnection getConnection() {
        return connection;
    }

    /**
     * Returns the session.
     * 
     * @return JMS session object
     */
    protected TopicSession getSession() {
        return session;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#isTransacted()
     */
    @Override
    public boolean isTransacted() {
        return this.transacted;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#receive()
     */
    @Override
    public IExternalMessage receive() throws MessageServiceException {
        try {
            return new JmsExternalMessage(this.subscriber.receive());
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to receive message via subscriber", e);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#setMessageListener(jpl.gds.message.api.external.IMessageServiceListener)
     */
    @Override
    public void setMessageListener(final IMessageServiceListener listener)
            throws MessageServiceException {
        try {
            if (listener == null) {
                this.subscriber.setMessageListener(null);
            } else {
                this.subscriber.setMessageListener(new InnerListener(listener));
            }
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to set message listener in message subscriber", e);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#start()
     */
    @Override
    public void start() throws MessageServiceException {
        try {
            connection.start();
        } catch (final JMSException e) {
            JmsMessageServiceExceptionFactory.createException("Error starting message subscriber", e);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#unsubscribe()
     */
    @Override
    public void unsubscribe() throws MessageServiceException {
        if (this.subscriberName == null) {
            return;
        }
        if (session == null) {
            return;
        }
        try {
            session.unsubscribe(this.subscriberName);
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to unsubscribe durable message subscriber " + this.subscriberName, e);
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#getTopic()
     */
    @Override
    public String getTopic() {
        try {
            return this.subscriber == null ? null : this.subscriber.getTopic().getTopicName();
        } catch (final JMSException e) {
            log.error("Unexpected exception fetching message topic from session " + ExceptionTools.getMessage(e), e);
            return null;
        }
    }

    /**
     * Wraps a non-vendor-specific message listener inside a JMS listener.
     */
    private class InnerListener implements MessageListener {

        private final IMessageServiceListener wrappedListener;

        /**
         * Constructor
         * @param l the listener to wrap
         */
        public InnerListener(final IMessageServiceListener l) {
            this.wrappedListener = l;
        }
        
        /**
         * @{inheritDoc}
         * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
         */
        @Override
        public void onMessage(final Message m) {
            wrappedListener.onMessage(new JmsExternalMessage(m));          
        }
        
    }
    
}