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
package jpl.gds.message.api.config;

import java.util.Map;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.shared.config.JndiProperties;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.util.HostPortUtility;

/**
 * This class is used to hold the current general message service configuration.
 * These are properties for the general message service, not specific to any
 * specific implementation of the message portal or service. If supplied a
 * JndiProperties object, this class will maintain consistency in the provider
 * host and port information in that object versus this one.
 */
public class MessageServiceConfiguration implements Templatable {
    
    private boolean useMessaging;
    private String host = HostPortUtility.LOCALHOST;
    private int port = HostPortUtility.UNDEFINED_PORT;
    private final JndiProperties jndiProperties;
    
   
    /**
     * Constructor.
     * 
     * @param genProps GeneralProperties object to read defaults from
     * @param jndiProps JNDI properties object, if one is used by the current message
     *                  service implementation; may be null
     */
    public MessageServiceConfiguration(final GeneralProperties genProps, final JndiProperties jndiProps) {
      
        useMessaging = genProps.getUseRealtimePublicationDefault();
        this.jndiProperties = jndiProps;
        if (jndiProperties != null) {
            this.host = jndiProperties.getProviderHost();
            this.port = jndiProperties.getProviderPort();
        }
    }
    
    /**
     * Constructor.
     * 
     * @param genProps GeneralProperties object to read defaults from
     */
    public MessageServiceConfiguration(final GeneralProperties genProps) {
      
        this(genProps, null);
    }
    
    /**
     * Sets the use of realtime message publication.
     * 
     * @param use
     *            Whether to use messaging or not.
     */
    public void setUseMessaging(final boolean use) {
        useMessaging = use;
    }

    /**
     * Returns whether not realtime message publication is being used.
     * 
     * @return true to use messaging
     */
    public boolean getUseMessaging() {
        return useMessaging;
    }
    
    /**
     * Sets the message server host name.
     * 
     * @param host host name to set
     */
    public void setMessageServerHost(final String host) {
        this.host = host;
        if (this.jndiProperties != null) {
            this.jndiProperties.setProviderHost(host);
        }
    }

    /**
     * Sets the message server host port.
     * 
     * @param port port number to set
     */
    public void setMessageServerPort(final int port) {
        this.port = port;
        if (this.jndiProperties != null) {
            this.jndiProperties.setProviderPort(port);
        }
    }
    
    /**
     * Gets the message server host.
     * 
     * @return host name
     */
    public String getMessageServerHost() {
        return this.host;
    }
    
    /**
     * Gets the JMS server port.
     * 
     * @return port number
     */
    public int getMessageServerPort() {
        return this.port;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        map.put("jmsHost", host);
        map.put("messageServiceHost", host);
        map.put("jmsPort", port);  
        map.put("messageServicePort", port); 
    }
}