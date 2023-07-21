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
package jpl.gds.jms.spring.bootstrap;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.jms.JmsClientFactory;
import jpl.gds.jms.config.JmsProperties;
import jpl.gds.jms.message.JmsClientHeartbeatPublisher;
import jpl.gds.jms.portal.BatchingBinaryPreparer;
import jpl.gds.jms.portal.BatchingTextPreparer;
import jpl.gds.jms.portal.IMessagePreparer;
import jpl.gds.jms.portal.JmsMessagePortal;
import jpl.gds.jms.portal.SimpleBinaryPreparer;
import jpl.gds.jms.portal.SimpleTextPreparer;
import jpl.gds.jms.util.JmsMessageUtil;
import jpl.gds.message.api.MessageApiBeans;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.message.api.external.IClientHeartbeatPublisher;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.JndiProperties;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.util.HostPortUtility;

/**
 * Spring bootstrap configuration for JMS/ActiveMQ-related beans.
 */
@Configuration
public class JmsSpringBootstrap {
    
    @Autowired
    ApplicationContext appContext;

       
    /**
     * Gets the singleton JMS properties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return JmsProperties bean
     */
    @Bean(name=JmsApiBeans.JMS_PROPERTIES) 
    @Scope("singleton")
    @Lazy(value = true)
    public JmsProperties getJmsProperties(final SseContextFlag sseFlag) {
        return new JmsProperties(sseFlag);
    }
    
    /**
     * Gets the singleton IMessagePortal bean, in this case a JmsMessagePortal. 
     * 
     * @return IMessagePortal bean
     */
    @Bean(name=MessageApiBeans.MESSAGE_SERVICE_PORTAL) 
    @Scope("singleton")
    @Lazy(value = true)
    public IMessagePortal getMessagePortal() {
        return new JmsMessagePortal(appContext);
    }
    
    /**
     * Gets the singleton IMessageClientFactory bean, in this case a JmsClientFactory. 
     * 
     * @return IMessageClientFactory bean
     */
    @Bean(name=MessageApiBeans.MESSAGE_CLIENT_FACTORY) 
    @Scope("singleton")
    @Lazy(value = true)
    public IMessageClientFactory getMessageClientFactory() {
        return new JmsClientFactory(appContext);
    }
    
    /**
     * Gets the singleton JndiProperties bean.
     * 
     * @param jmsProps the JmsProperties bean, autowired
     * @return JndiProperties bean
     */
    @Bean(name=MessageApiBeans.JNDI_PROPERTIES) 
    @Scope("singleton")
    @Lazy(value = true)
    public JndiProperties getJndiProperties(final JmsProperties jmsProps) {
        return jmsProps.getJndiProperties();
    }
    
    
    /**
     * Gets the singleton IExternalMessageUtility bean.
     * 
     * @return IExternalMessageUtility bean.
     */
    @Bean(name=MessageApiBeans.EXTERNAL_MESSAGE_UTIL) 
    @Scope("singleton")
    @Lazy(value = true)
    public IExternalMessageUtility geExternalMessageUtility() {
        return new JmsMessageUtil(appContext);
    }
    
    /**
     * Gets the singleton IClientHeartbeatPublisher bean. Client ID will be
     * automatically assigned using the current application name and process
     * ID.
     * 
     * @return  IClientHeartbeatPublisher bean
     */
    @Bean(name=MessageApiBeans.CLIENT_HEARTBEAT_PUBLISHER) 
    @Scope("singleton")
    @Lazy(value = true)
    public IClientHeartbeatPublisher getClientHeartbeatPublisher() {
        
        final long pid  = GdsSystemProperties.getIntegerPid();
        final UUID uuid = new UUID(System.currentTimeMillis(),
                ((pid << Integer.SIZE)));
        final String myId = ApplicationConfiguration.getApplicationName() + "-" + 
                HostPortUtility.getLocalHostName() + "-" + uuid.toString();
        
        return new JmsClientHeartbeatPublisher(appContext, myId);
    }
    
    /**
     * Gets a prototype IMessagePreparer bean. A new instance is returned every time.
     * 
     * @param type the message preparer type
     * @param messageType the message type to be handled by the preparer
     * @param pubs the publishers to be used by the preparer
     * @return  IMessagePreparer bean
     */
    @Bean(name=JmsApiBeans.MESSAGE_PREPARER) 
    @Scope("prototype")
    @Lazy(value = true)
    public IMessagePreparer getMessagePreparer(final JmsProperties.PreparerClassType type, final IMessageType messageType,
            final List<IAsyncTopicPublisher> pubs) {
        switch(type) {
        case BATCHING_BINARY:
            return new BatchingBinaryPreparer(appContext, messageType, pubs);
        case BATCHING_TEXT:
            return new BatchingTextPreparer(appContext, messageType, pubs);
        case SIMPLE_BINARY:
            return new SimpleBinaryPreparer(appContext, messageType, pubs);
        case SIMPLE_TEXT:
            return new SimpleTextPreparer(appContext, messageType, pubs);
        case CUSTOM:
        default:
            throw new IllegalArgumentException("Unrecognized message preparer type: " + type);
        
        }
    }
}
  
    
