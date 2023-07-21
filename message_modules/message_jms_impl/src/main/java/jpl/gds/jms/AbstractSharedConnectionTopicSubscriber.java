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
import java.util.concurrent.atomic.AtomicInteger;

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
 * Convenience class for subscribing to JMS topics using a shared connection
 * and JMS session. Pass in a configuration,
 * then either poll for messages or extend the callback methods.
 */
public abstract class AbstractSharedConnectionTopicSubscriber implements ITopicSubscriber {
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
                 // ignore this - nothing we can do 
                }
            }
            decrementOpenSubscriber();
            if (session != null && openSubscribers.get() == 0) {
                try {
                    session.close();
                } catch (final JMSException e) {
                	 // ignore this - nothing we can do 
                }
            }
            if (connection != null && openSubscribers.get() == 0) {
                try {
                    connection.stop();
                    connection.close();
                } catch (final JMSException e) {
                	// nothing we can do
                }
            }
            stopped = true;
            if (openSubscribers.get() == 0) {
                closeSharedNoDisconnect();
            }
        }
    }
    
    /** Shared trace logger */
    protected final Tracer         log;

    /**
     * Shared connection to the JMS.
     */
    private static TopicConnection connection;

    /**
     * Shared session to the JMS.
     */
    private static TopicSession session;

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
     * Number of subscribers currently using the connection.
     */
    private static AtomicInteger openSubscribers = new AtomicInteger(0);
    
    private boolean stopped;
    
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
    protected AbstractSharedConnectionTopicSubscriber(final ApplicationContext context) {
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
    public AbstractSharedConnectionTopicSubscriber(
    		final ApplicationContext appContext,
            final JmsTopicConfiguration config, final String name,
            final String filter, final boolean isTransacted)
            throws NamingException, JMSException {
    	
    	this(appContext);
    	
        this.transacted = isTransacted;
        
        establishConnectionSession(jndiProperties, jmsConfig, config, isTransacted);

        final Topic topic = session.createTopic(config.getTopicName());

        if (name != null) {
            this.subscriberName = name;
            this.subscriber = session.createDurableSubscriber(topic, name,
                    filter, false);
        } else {
            this.subscriber = session.createSubscriber(topic, filter, false);
        }
        
        incrementOpenSubscriber();
    }

    private static synchronized void establishConnectionSession(final JndiProperties jndi, final JmsProperties jmsProps, 
            final JmsTopicConfiguration config, final boolean isTransacted)
            throws NamingException, JMSException {

        if (connection == null) {
            final Context context = createNamingContext(jndi, config);
            final TopicConnectionFactory factory = (TopicConnectionFactory) context
                    .lookup(jmsProps.getTopicFactoryName());
            createConnection(jndi, factory);
            session = connection.createTopicSession(isTransacted,
                    isTransacted ? Session.SESSION_TRANSACTED
                            : Session.AUTO_ACKNOWLEDGE);
        }
        
    }

    /**
     * Creates a TopicConnection given a TopicConnectionFactory.
     * 
     * @param jndi the current JNDI properties object
     * @param factory the TopicConnectionFactory.
     * @throws JMSException if there is a JMS error creating the connection
     */
    protected static synchronized void createConnection(final JndiProperties jndi, final TopicConnectionFactory factory) throws JMSException {
        final String userName = jndi.getSecurityPrincipal();
        final String userCredential = jndi.getSecurityCredentials();
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
     * 
     * Closes the current subscriber. If it is the last subscriber, closes
     * the shared session and connection. 
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
        synchronized(AbstractSharedConnectionTopicSubscriber.class) {
            if (openSubscribers.get() == 0) {
                closeSharedNoDisconnect();
            }
        }
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#closeNoDisconnect()
     */
    @Override
    public void closeNoDisconnect() {
        this.subscriber = null;
        closeSharedNoDisconnect();
    }
    
    /**
     * Resets the shared session and connection to null state
     * without actually closing them.
     */
    protected static synchronized void closeSharedNoDisconnect() {
        session = null;
        openSubscribers.set(0);
        connection = null;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicSubscriber#commit()
     * 
     * Note that this will commit all the subscribers sharing this connection.
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
     * @param jndi the current JNDI properties object
     * @param config
     *            Context configurations.
     * @return New context based off of the configuration.
     * @throws NamingException
     *             Thrown if unable to initialize the context.
     *             
     * Added suppress. Cannot change to HashMap because JNDI expects Hashtable
	 */
    @SuppressWarnings("PMD.ReplaceHashtableWithMap")
    protected static Context createNamingContext(final JndiProperties jndi, final JmsTopicConfiguration config)
            throws NamingException {
        final Hashtable<String, Object> table = new Hashtable<>();
        final Properties p = jndi.asProperties();
        for (final Iterator<Object> i = p.keySet().iterator(); i.hasNext();) {
            final String key = (String) i.next();
            table.put(key, p.getProperty(key));
        }
        return new InitialContext(table);
    }

    /**
     * Set the shared connection.
     * 
     * @param sharedConnection
     *            shared connection
     */
    protected static void setSharedConnection(final TopicConnection sharedConnection) {
        if (connection == null) {
            connection = sharedConnection;
        }
    }

    /**
     * Returns the shared connection.
     * 
     * @return shared connection
     */
    protected static TopicConnection getSharedConnection() {
        return connection;
    }

    /**
     * Sets the shared session if one is not already set.
     * 
     * @param sharedSession
     *            to set the session to
     */
    protected static synchronized void setSharedSession(final TopicSession sharedSession) {
        if (session == null) {
            session = sharedSession;
        }
    }

    /**
     * Returns the shared session.
     * 
     * @return shared session
     */
    protected static synchronized TopicSession getSharedSession() {
        return session;
    }

    /**
     * Increments how many open subscribers there are.
     */
    protected static void incrementOpenSubscriber() {
        openSubscribers.incrementAndGet();
    }
    
    /**
     * Decrements how many open subscribers there are.
     */
    protected static void decrementOpenSubscriber() {
        openSubscribers.decrementAndGet();
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
    public synchronized void start() throws MessageServiceException {
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
        if (getSharedSession() == null) {
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