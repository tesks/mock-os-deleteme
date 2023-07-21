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
package jpl.gds.shared.config;

import java.util.Properties;

import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * A general properties class for reading and accessing properties in a
 * Java Naming and Directory Interface (JNDI) properties file.
 * 
 */
public class JndiProperties extends GdsHierarchicalProperties {
    
    /** Default properties file name.*/
    public static final String DEFAULT_JNDI_PROPERTIES = "jndi.properties";
    
    private static final String PROPERTY_PREFIX = "java.naming.";
    
    private static final String CONTEXT_FACTORY_PROPERTY = PROPERTY_PREFIX + "factory.initial";
    private static final String PROVIDER_URL_PROPERTY = PROPERTY_PREFIX + "provider.url";
    private static final String JNDI_SECURITY_PRINCIPAL_PROPERTY = PROPERTY_PREFIX + "security.principal";
    private static final String JNDI_SECURITY_CREDENTIALS_PROPERTY = PROPERTY_PREFIX + "security.credentials";
    
    /* Used for overriding provider host and port */
    private String overrideUrl = null;
   
    /**
     * test constructor
     */
    public JndiProperties() {
        this(DEFAULT_JNDI_PROPERTIES, new SseContextFlag());
    }
    
    /**
     * Constructor that uses the specified properties file.
     * 
     * @param fileName
     *            the name of the properties file to load (less path)
     * @param sseFlag
     *            The current SSE context flag
     */
    public JndiProperties(final String fileName, final SseContextFlag sseFlag) {
        super(fileName, sseFlag);
    }
    
    /**
     * Gets the JNDI context factory name.
     * 
     * @return the JNDI context factory name, or null if none defined
     */
    public String getContextFactoryName() {
        return getProperty(CONTEXT_FACTORY_PROPERTY);
    }

    /**
     * Returns the JNDI Provider host name.
     * 
     * @return provider host name, or null if none defined
     */
    public String getProviderHost() {
        final String url = getProviderUrl();
        if (url == null) {
            return null;
        }
        // Parse host name out of URL: protocol://host:port
        final int slashIndex = url.indexOf("//");
        if (slashIndex == -1 || slashIndex + 3 > url.length()) {
            throw new RuntimeException(
                    "Cannot interpret JNDI provider url: format is unexpected");
        }
        final int colonIndex = url.indexOf(":", slashIndex);
        if (colonIndex == -1) {
            throw new RuntimeException(
                    "Cannot interpret JNDI provider url: format is unexpected");
        }
        return url.substring(slashIndex + 2, colonIndex);
    }

    /**
     * Returns the JNDI Provider port number.
     * 
     * @return provider port number, or 0 if none defined
     */
    public int getProviderPort() {
        final String url = getProviderUrl();
        if (url == null) {
            return 0;
        }
        // Parse host name out of URL: protocol://host:port
        final int slashIndex = url.indexOf("//");
        if (slashIndex == -1 || slashIndex + 3 > url.length()) {
            throw new RuntimeException(
                    "Cannot interpret JNDI provider url: format is unexpected");
        }
        final int colonIndex = url.indexOf(":", slashIndex);
        if (colonIndex == -1) {
            throw new RuntimeException(
                    "Cannot interpret JNDI provider url: format is unexpected");
        }
        int questionIndex = url.indexOf("?", colonIndex);
        if (questionIndex == -1) {
            questionIndex = url.length();
        }
        return Integer.parseInt(url.substring(colonIndex + 1, questionIndex));
    }

    /**
     * Returns the value of the JNDI Provider URL.
     * 
     * @return provider URL, or null if none defined
     */
    public String getProviderUrl() {
        return overrideUrl == null ? getProperty(PROVIDER_URL_PROPERTY) : overrideUrl;
    }

    /**
     * Sets the JNDI provider host name by modifying the provider URL.  The new URL
     * is cached and will from this point forward override the value in the properties
     * file.
     * 
     * @param host
     *            Host name to set
     */
    public void setProviderHost(final String host) {
        final String url = getProviderUrl();
        if (url == null) {
            throw new IllegalStateException(
                    "Cannot modify JNDI provider url because it is not yet defined");
        }
        // Parse host name out of URL: protocol://host:port
        final int slashIndex = url.indexOf("//");
        if (slashIndex == -1 || slashIndex + 3 > url.length()) {
            throw new RuntimeException(
                    "Cannot modify JNDI provider url: format is unexpected");
        }
        final int colonIndex = url.indexOf(":", slashIndex);
        if (colonIndex == -1) {
            throw new RuntimeException(
                    "Cannot modify JNDI provider url: format is unexpected");
        }
        final String newUrl = url.substring(0, slashIndex + 2) + host
                + url.substring(colonIndex);
        setProviderUrl(newUrl);
    }

    /**
     * Set the JNDI provider port number by modifying the provider URL.  The new URL
     * is cached and will from this point forward override the value in the properties
     * file.
     * 
     * @param port
     *            Port number to set
     */
    public void setProviderPort(final int port) {
        final String url = getProviderUrl();
        if (url == null) {
            throw new IllegalStateException(
                    "Cannot modify JNDI provider url because it is not yet defined");
        }
        // Parse host name out of URL: protocol://host:port
        final int slashIndex = url.indexOf("//");
        if (slashIndex == -1 || slashIndex + 3 > url.length()) {
            throw new RuntimeException(
                    "Cannot modify JNDI provider url: format is unexpected");
        }
        final int colonIndex = url.indexOf(":", slashIndex);
        if (colonIndex == -1) {
            throw new RuntimeException(
                    "Cannot modify JNDI provider url: format is unexpected");
        }
        int questionIndex = url.indexOf("?", colonIndex);
        if (questionIndex == -1) {
            questionIndex = url.length();
        }
        final String newUrl = url.substring(0, colonIndex + 1)
                + String.valueOf(port) + url.substring(questionIndex);
        setProviderUrl(newUrl);
    }

    /**
     * Sets the JNDI Provider URL. The new URL is cached and will from this
     * point forward override the value in the properties file.
     * 
     * @param value
     *            URL to set.
     */
    public void setProviderUrl(final String value) {
        overrideUrl = value;
    }
    
    /**
     * Gets the JNDI security principal property.
     * 
     * @return security principal name, or null if none defined
     */
    public String getSecurityPrincipal() {
        return getProperty(JNDI_SECURITY_PRINCIPAL_PROPERTY);
    }
    
    /**
     * Gets the JNDI security credentials property.
     * 
     * @return security credentials, or null if none defined
     */
    public String getSecurityCredentials() {
        return getProperty(JNDI_SECURITY_CREDENTIALS_PROPERTY);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.config.GdsHierarchicalProperties#asProperties()
     */
    @Override
    public Properties asProperties() {
        final Properties p = super.asProperties();
        if (overrideUrl != null) {
            p.setProperty(PROVIDER_URL_PROPERTY, overrideUrl);
        }
        return p;
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
