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
package jpl.gds.jms.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.external.BaseExternalMessageUtility;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.MessageHeaderMode;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.util.BinOctHexUtility;

/**
 * A utility class for creating and manipulating JMS messages. This content
 * was broken out from the R7 MessageUtil class so that JMS-specific capability
 * could be removed from the basic message utilities.
 */
public class JmsMessageUtil extends BaseExternalMessageUtility implements IExternalMessageUtility {

    private static SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final ApplicationContext appContext;
    
    /**
     * Constructor.
     * 
     * @param appContext the current application context
     */
    public JmsMessageUtil(final ApplicationContext appContext) {
        super(TraceManager.getTracer(appContext, Loggers.JMS));
        this.appContext = appContext;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IExternalMessageUtility#getInternalType(jpl.gds.message.api.external.IExternalMessage)
     */
    @Override
    public IMessageType getInternalType(final IExternalMessage msg)
            throws MessageServiceException {
        try {
            final Message jmsMessage = (Message)msg.getMessageObject();
            final String type =
                    jmsMessage.getStringProperty(MetadataKey.MESSAGE_TYPE.toString());
            final IMessageConfiguration mc = MessageRegistry.getMessageConfig(type);
            if (mc == null) {
                throw new MessageServiceException("Cannot find message registry entry for message type " + type);
            }
            return mc.getMessageType();
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to get internal message type from external message header", e);
        }
    }
      
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IExternalMessageUtility#instantiateMessages(jpl.gds.message.api.external.IExternalMessage)
     */
    @Override
    public jpl.gds.shared.message.IMessage[] instantiateMessages(
            final IExternalMessage msg) throws MessageServiceException {
          
        final IMessageType type = getInternalType(msg);
        try {

            if (msg.isBinary()) {
                return instantiateExternalMessages(msg.getBinaryContent(), type, appContext);

            } else {
                return instantiateExternalMessages(msg.getTextContent(), type, appContext);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new MessageServiceException("Unable to instantiate internal messages from external message for type " + type, e);
        } 

    }
    
 
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IExternalMessageUtility#getHeaderText(jpl.gds.message.api.external.IExternalMessage, jpl.gds.message.api.external.MessageHeaderMode)
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getHeaderText(final IExternalMessage message,
            final MessageHeaderMode headersMode) {
        StringBuilder headerText = null;
        try {
            final Message jmsMessage = (Message) message.getMessageObject();
            if (headersMode == MessageHeaderMode.HEADERS_OFF) {
                return "";
            }
            headerText = new StringBuilder(2048);
            headerText.append("\n<!--*** JMS Header Properties ***\n");
            final Date pub = new Date(jmsMessage.getJMSTimestamp());
            final Date exp = new Date(jmsMessage.getJMSExpiration());
            headerText.append("JMS Timestamp = " + dateFormat.format(pub)
                    + "\n");
            headerText.append("JMS Expiration = " + dateFormat.format(exp)
                    + "\n");
            for (final Enumeration<String> e = jmsMessage.getPropertyNames(); e
                .hasMoreElements();) {
                final String name = e.nextElement();
                if (!name.startsWith("JMSX")) {
                    headerText.append(name + "="
                            + jmsMessage.getStringProperty((name)) + "\n");
                }
            }
            headerText.append("\n*** End JMS Header Properties ***-->\n");
        } catch (final JMSException e) {
            e.printStackTrace();
        }

        return headerText.toString();
    }

    @Override
    public String getStringHeaderProperty(final IExternalMessage message, final String propName) throws MessageServiceException {
        try {
            return ((Message)message.getMessageObject()).getStringProperty(propName);
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to fetch message header property " + propName, e);
        }
    }
    
    @Override
    public Integer getIntHeaderProperty(final IExternalMessage message, final String propName) throws MessageServiceException {
        try {
            return ((Message)message.getMessageObject()).getIntProperty(propName);
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to fetch message header property " + propName, e);
        }
    }
    
    @Override
    public Long getLongHeaderProperty(final IExternalMessage message, final String propName) throws MessageServiceException {
        try {
            return ((Message)message.getMessageObject()).getLongProperty(propName);
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to fetch message header property " + propName, e);
        }
    }
    
    @Override
    public Float getFloatHeaderProperty(final IExternalMessage message, final String propName) throws MessageServiceException {
        try {
            return ((Message)message.getMessageObject()).getFloatProperty(propName);
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to fetch message header property " + propName, e);
        }
    }
    
    
    @Override
    public Double getDoubleHeaderProperty(final IExternalMessage message, final String propName) throws MessageServiceException {
        try {
            return ((Message)message.getMessageObject()).getDoubleProperty(propName);
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to fetch message header property " + propName, e);
        }
    }

    @Override
    public String getMessageId(final IExternalMessage message) throws MessageServiceException {
        try {
            return ((Message)message.getMessageObject()).getJMSMessageID();
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to fetch message ID", e);
        }
    }

    @Override
    public long getMessageTimestamp(final IExternalMessage message) throws MessageServiceException {
        try {
            return ((Message)message.getMessageObject()).getJMSTimestamp();
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to fetch message ID", e);
        }
    }

    @Override
    public String getContentDump(final IExternalMessage message)
            throws MessageServiceException {
        
        final Message m = (Message)message.getMessageObject();
        
        try {
            String text = null;
            if (m instanceof TextMessage) {
                text = ((TextMessage)m).getText();
            } else {
                final byte [] blob = new byte[(int)((BytesMessage)m).getBodyLength()];
                ((BytesMessage)m).readBytes(blob);   
                text = BinOctHexUtility.formatBytes(blob);
            }
            return text;
        } catch (final JMSException e) {
            throw JmsMessageServiceExceptionFactory.createException("Unable to get message content", e);
        }
    }
    
    @Override
    public String getTopic(final IExternalMessage m) {
        try {
            final Topic t = (Topic)((Message)m.getMessageObject()).getJMSDestination();
            if (t == null) {
                return "Unknown";
            }
            return t.getTopicName();
        } catch (final JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

}
