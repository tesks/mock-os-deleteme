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
import java.util.Map;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.jms.config.JmsProperties;
import jpl.gds.jms.message.JmsExternalMessage;
import jpl.gds.jms.util.JmsMessageServiceExceptionFactory;
import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.ITopicPublisher;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.config.JndiProperties;

/**
 * Convenience class for publishing to JMS topics. Pass in a configuration, call
 * the publish method.
 * 
 * Abstract methods and constants rightly belonging only to asynchronous queuing
 * publishers have been moved into the AbstractAsynchronousTopicPublisher subclass.
 * We need a simple publisher, not a queuing publisher, for some applications. Class
 * is still abstract because one must create the publisher for the current JMS provider.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class AbstractTopicPublisher implements ITopicPublisher {
    /**
     * Jms topic name.
     */
    protected String name;
    /**
     * Creates a connection to the Jms server.
     */
    protected Connection connection;
    /**
     * Creates a session based on the topic name.
     */
    protected Session session;
    /**
     * Publisher based off the Jms session.
     */
    protected MessageProducer publisher;
    /**
     * Whether the publisher is transaction based.
     */
    protected boolean transacted;

    private boolean persistent;
    
    private final JmsProperties jmsConfig;
    private final JndiProperties jndiProperties;

    /**
     * Allow children classes to implement their own constructors.
     * 
     * @param appContext
     *            the current Application Context
     */
    protected AbstractTopicPublisher(final ApplicationContext appContext) {
        this.jmsConfig = appContext.getBean(JmsProperties.class);
        this.jmsConfig.init(); // complete initialization (if required) before use.
        this.jndiProperties = appContext.getBean(JndiProperties.class);
    }

    /**
     * Constructor which establishes the JMS connection and session.
     * @param appContext the current application context
     * @param config JMS configuration used to configure topic, connection, and
     *            session.
     * @param isTransacted Whether the publisher is transacted.
     * @throws MessageServiceException Thrown if a JMS exception is encountered.
     */
    protected AbstractTopicPublisher(final ApplicationContext appContext,
    		final JmsTopicConfiguration config,
            final boolean isTransacted) throws MessageServiceException {
    	
    	this(appContext);
        this.transacted = isTransacted;
        this.name = config.getTopicName();
        this.persistent = config.isPersistent();
        open();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#close()
     */
    @Override
	public void close() {
        if (this.session != null) {
            try {
                this.session.close();
            } catch (final JMSException e) {
            	 // ignore this - nothing we can do 
            }
        }
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (final JMSException e) {
            	 // ignore this - nothing we can do 
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#open()
     */
    @Override
    public void open() throws MessageServiceException {
        try {
            final Context context = createNamingContext();
            final ConnectionFactory connectionFactory =
                    (ConnectionFactory) context.lookup(jmsConfig.getTopicFactoryName());

            final String userName = jndiProperties.getSecurityPrincipal();
            final String userCredential =
                    jndiProperties.getSecurityCredentials();
            if ((userName == null) || (userCredential == null)) {
                this.connection = connectionFactory.createConnection();
            } else {
                this.connection =
                        connectionFactory.createConnection(userName, userCredential);
            }

            this.connection.start();
            this.session = this.connection.createSession(this.transacted, transacted
            		? Session.SESSION_TRANSACTED : Session.AUTO_ACKNOWLEDGE);
            final Topic topic = this.session.createTopic(this.name);

            this.publisher = this.session.createProducer(topic);
            this.publisher.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT 
            		: DeliveryMode.NON_PERSISTENT);
        } catch (Exception e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Unable to open message "
            		+ "publisher connection", e);
        } 
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#commit()
     */
    @Override
	public void commit() throws MessageServiceException {
        if (!this.transacted) {
            throw new IllegalStateException(
                "Attempted to commit non-transacted session");
        }
        try {
            this.session.commit();
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Unable to commit publisher transaction", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#getName()
     */
    @Override
	public String getName() {
        return this.name;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#publishMessage(jpl.gds.message.api.external.IExternalMessage)
     */
    @Override
    public void publishMessage(final IExternalMessage m)
            throws MessageServiceException {
        publishAnyMessage((Message)m.getMessageObject());
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#createBinaryMessage(byte[], java.util.Map)
     */
    @Override
    public IExternalMessage createBinaryMessage(final byte [] blob,
            final Map<String, Object> properties) throws MessageServiceException {
        BytesMessage message = null;
        try {
            message = this.session.createBytesMessage();
            message.writeBytes(blob);
            setMessageProperties(message, properties);
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Unable to create binary message", e);
        }
        return new JmsExternalMessage(message);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#publishBinaryMessage(byte[], java.util.Map, long, jpl.gds.message.api.external.ExternalDeliveryMode)
     */
    @Override
	public void publishBinaryMessage(final byte[] blob,
            final Map<String, Object> properties, final long timeToLive,
            final ExternalDeliveryMode deliveryMode) throws MessageServiceException {
        BytesMessage message = null;
        try {
            message = this.session.createBytesMessage();
            message.writeBytes(blob);
            setMessageProperties(message, properties);
            
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Unable to publish binary message", e);
        }

        publishAnyMessage(message, mapDeliveryMode(deliveryMode), Message.DEFAULT_PRIORITY,
            timeToLive);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#publishBinaryMessage(byte[])
     */
    @Override
    public void publishBinaryMessage(final byte[] blob)
            throws MessageServiceException {
        publishBinaryMessage(blob, null, 0, ExternalDeliveryMode.PERSISTENT);

    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#publishBinaryMessage(byte[], java.util.Map)
     */
    @Override
    public void publishBinaryMessage(final byte[] blob, final Map<String, Object> properties)
            throws MessageServiceException {
        publishBinaryMessage(blob, properties, 0, ExternalDeliveryMode.PERSISTENT);

    }
 

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#createTextMessage(java.lang.String, java.util.Map)
     */
    @Override
    public IExternalMessage createTextMessage(final String text,
            final Map<String, Object> properties) throws MessageServiceException {
        TextMessage message = null;
        try {
            message = this.session.createTextMessage(text);
            setMessageProperties(message, properties);
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Unable to create text message", e);
        }
        return new JmsExternalMessage(message);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#publishTextMessage(java.lang.String)
     */
    @Override
    public void publishTextMessage(final String text) throws MessageServiceException {
        try {
            publishAnyMessage(this.session.createTextMessage(text));
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Unable to publish text message", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#publishTextMessage(java.lang.String, java.util.Map)
     */
    @Override
    public void publishTextMessage(final String text,
            final Map<String, Object> properties) throws MessageServiceException {
        publishTextMessage(text, properties, 0, ExternalDeliveryMode.PERSISTENT);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#publishTextMessage(java.lang.String, java.util.Map, long, jpl.gds.message.api.external.ExternalDeliveryMode)
     */
    @Override
    public void publishTextMessage(final String text,
            final Map<String, Object> properties, final long timeToLive,
            final ExternalDeliveryMode deliveryMode) throws MessageServiceException {
        TextMessage message;
        try {
            message = this.session.createTextMessage(text);
            setMessageProperties(message, properties);
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Unable to publish text message", e);
        }

        publishAnyMessage(message, mapDeliveryMode(deliveryMode), Message.DEFAULT_PRIORITY,
                timeToLive);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#isTransacted()
     */
    @Override
    public boolean isTransacted() {
        return this.transacted;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#isPersistent()
     */
    @Override
    public boolean isPersistent() {
        return this.persistent;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.ITopicPublisher#getTopic()
     */
    @Override
    public String getTopic() {
        try {
            return this.publisher == null ? null 
            		: ((Topic)this.publisher.getDestination()).getTopicName();
        } catch (final JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a naming context based off of the current JmsConfiguration.
     * 
     * @return the naming context.
     * @throws NamingException
     *             Thrown if unable to create the context.
     */
    @SuppressWarnings("PMD.ReplaceHashtableWithMap")
    protected Context createNamingContext() throws NamingException {
        final Hashtable<String, Object> table = new Hashtable<String, Object>();
        final Properties p = jndiProperties.asProperties();
        for (final Iterator<Object> i = p.keySet().iterator(); i.hasNext();) {
            final String key = (String) i.next();
            table.put(key, p.getProperty(key));
        }
        return new InitialContext(table);
    }

    private int mapDeliveryMode(final ExternalDeliveryMode mode) {
        switch(mode) {
        case NON_PERSISTENT:
            return DeliveryMode.NON_PERSISTENT;
        case PERSISTENT:
            return DeliveryMode.PERSISTENT;
        default:
            return DeliveryMode.PERSISTENT;

        }
    }

    private void setMessageProperties(final Message m, final Map<String, Object> properties) throws JMSException {
        if (!(properties == null) && !properties.isEmpty()) {
            for (final Map.Entry<String, Object> entry
                    : properties.entrySet()) {
                if (entry.getKey() == null) {
                    System.out.println("Found null key in properties map"
                            + " for bytes message");
                    continue;
                }
                if (entry.getValue() == null) {
                    System.out.println("Found null value for key "
                            + entry.getKey());
                }
                m.setStringProperty(entry.getKey(), entry.getValue()
                        .toString());
            }
        }

    }

    /**
     * Encapsulates publishing details.
     * @param m Message to publish.
     * @throws MessageServiceException Thrown if unable to publish the Jms message.
     */
    private void publishAnyMessage(final Message m) throws MessageServiceException {
        try {
            publisher.send(m);
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Could not publish message", e);
        }
    }

    /**
     * Encapsulates publishing details.
     * @param m Message to publish.
     * @param mode Mode of publication.
     * @param priority Priority of message.
     * @param ttl Time to live for the message.
     * @throws MessageServiceException Thrown if unable to publish the Jms message.
     */
    private void publishAnyMessage(final Message m, final int mode,
            final int priority, final long ttl) throws MessageServiceException {
        try {
            publisher.send(m, mode, priority, ttl);
        } catch (final JMSException e) {
            e.printStackTrace();
            throw JmsMessageServiceExceptionFactory.createException("Could not publish message", e);
        }
    }

}
