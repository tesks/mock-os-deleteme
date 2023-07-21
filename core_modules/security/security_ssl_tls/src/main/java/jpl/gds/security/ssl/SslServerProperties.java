/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.security.ssl;

import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.core.env.ConfigurableEnvironment;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * SSL Configuration manager to be extended by ssl-capable spring apps
 * Loads the SSL properties from a spring properties file
 * 
 */
public class SslServerProperties extends Properties implements ISslConfiguration {
    private static final long     serialVersionUID    = 7653535697830691220L;

    /**
     * Tracer for use during file loading
     * 
     */
    protected static final Tracer log                 = TraceManager.getTracer(Loggers.CONFIG);

    /** Prefix for ssl server configurations */
    public static final String    PROPERTY_PREFIX     = "server.";

    /** Path to the keystore file */
    protected String              keystorePath        = ISslConfiguration.DEFAULT_AMMOS_KEYSTORE_PATH;
    /** Keystore file type */
    protected String              keystoreType        = ISslConfiguration.DEFAULT_AMMOS_KEYSTORE_TYPE;
    /** Password to the keystore file */
    protected String              keystorePw          = ISslConfiguration.DEFAULT_AMMOS_KEYSTORE_PW;

    /** Path to the truststore file */
    protected String              truststorePath      = ISslConfiguration.DEFAULT_AMMOS_TRUSTSTORE_PATH;
    /** Truststore file type */
    protected String              truststoreType      = ISslConfiguration.DEFAULT_AMMOS_TRUSTSTORE_TYPE;
    /** Password to the truststore file */
    protected String              truststorePw        = ISslConfiguration.DEFAULT_AMMOS_TRUSTSTORE_PW;

    /** SSL protocol to use (if any) */
    protected String              protocols           = ISslConfiguration.DEFAULT_AMMOS_PROTOCOL;
    /** Cipher's to use (if any) */
    protected String              ciphers             = "";

    /** true if configuring for secure transport (HTTPS), false if configuring for plain-text transport (HTTP) */
    protected boolean             isSecure            = false;

    /**
     * Constructor for loading a specific SSL property file
     * 
     * @param props
     *            a Properties object
     */
    public SslServerProperties(final Properties props) {
        // First load system default from default_spring_server_ssl.properties
        setSslPropertiesFromProperties(props);

        validate(); // prints debug for now
    }

    /**
     * Constructor for loading a specific SSL property file
     * 
     * @param env
     *            Spring Environment
     */
    public SslServerProperties(final ConfigurableEnvironment env) {
        // First load system default from default_spring_server_ssl.properties
        setSslPropertiesFromProperties(env);

        validate(); // prints debug for now
    }

    private void setSslPropertiesFromProperties(final Properties props) {
        if (props.getProperty(ISslConfiguration.SPRING_PROTOCOLS_PROPERTY) != null) {
            this.protocols = props.getProperty(ISslConfiguration.SPRING_PROTOCOLS_PROPERTY);
        }
        if (props.getProperty(ISslConfiguration.SPRING_CIPHERS_PROPERTY) != null) {
            this.ciphers = props.getProperty(ISslConfiguration.SPRING_CIPHERS_PROPERTY);
        }
        if (props.getProperty(ISslConfiguration.SPRING_KEYSTORE_PATH_PROPERTY) != null) {
            this.keystorePath = props.getProperty(ISslConfiguration.SPRING_KEYSTORE_PATH_PROPERTY);
        }
        if (props.getProperty(ISslConfiguration.SPRING_KEYSTORE_TYPE_PROPERTY) != null) {
            this.keystoreType = props.getProperty(ISslConfiguration.SPRING_KEYSTORE_TYPE_PROPERTY);
        }
        if (props.getProperty(ISslConfiguration.SPRING_KEYSTORE_PW_PROPERTY) != null) {
            this.keystorePw = props.getProperty(ISslConfiguration.SPRING_KEYSTORE_PW_PROPERTY);
        }
        if (props.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PATH_PROPERTY) != null) {
            this.truststorePath = props.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PATH_PROPERTY);
        }
        if (props.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_TYPE_PROPERTY) != null) {
            this.truststoreType = props.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_TYPE_PROPERTY);
        }
        if (props.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PW_PROPERTY) != null) {
            this.truststorePw = props.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PW_PROPERTY);
        }
    }

    private void setSslPropertiesFromProperties(final ConfigurableEnvironment env) {
        if (env.getProperty(ISslConfiguration.SPRING_PROTOCOLS_PROPERTY) != null) {
            this.protocols = env.getProperty(ISslConfiguration.SPRING_PROTOCOLS_PROPERTY);
        }
        if (env.getProperty(ISslConfiguration.SPRING_CIPHERS_PROPERTY) != null) {
            this.ciphers = env.getProperty(ISslConfiguration.SPRING_CIPHERS_PROPERTY);
        }
        if (env.getProperty(ISslConfiguration.SPRING_KEYSTORE_PATH_PROPERTY) != null) {
            this.keystorePath = env.getProperty(ISslConfiguration.SPRING_KEYSTORE_PATH_PROPERTY);
        }
        if (env.getProperty(ISslConfiguration.SPRING_KEYSTORE_TYPE_PROPERTY) != null) {
            this.keystoreType = env.getProperty(ISslConfiguration.SPRING_KEYSTORE_TYPE_PROPERTY);
        }
        if (env.getProperty(ISslConfiguration.SPRING_KEYSTORE_PW_PROPERTY) != null) {
            this.keystorePw = env.getProperty(ISslConfiguration.SPRING_KEYSTORE_PW_PROPERTY);
        }
        if (env.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PATH_PROPERTY) != null) {
            this.truststorePath = env.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PATH_PROPERTY);
        }
        if (env.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_TYPE_PROPERTY) != null) {
            this.truststoreType = env.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_TYPE_PROPERTY);
        }
        if (env.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PW_PROPERTY) != null) {
            this.truststorePw = env.getProperty(ISslConfiguration.SPRING_TRUSTSTORE_PW_PROPERTY);
        }
    }

    private void setJavaxSystemProperties() {
        if (hasHttpsProtocols()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.HTTPS_PROTOCOLS, protocols);
            log.debug("Using protocols ", protocols);
        }
        if (hasCiphers()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.HTTPS_CIPHERS, ciphers);
            log.debug("Using ciphers ", ciphers);
        }
        if (!keystorePath.isEmpty()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.JAVAX_SSL_KEYSTORE_PATH, keystorePath);
            log.debug("Using keystore file=", keystorePath);
        }
        if (!keystoreType.isEmpty()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.JAVAX_SSL_KEYSTORE_TYPE, keystoreType);
            log.debug("Using keystoreType=", keystoreType);
        }
        if (!keystorePw.isEmpty()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.JAVAX_SSL_KEYSTORE_PW, keystorePw);
        }
        if (!truststorePath.isEmpty()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.JAVAX_SSL_TRUSTSTORE_PATH, truststorePath);
            log.debug("Using truststore file=", truststorePath);
        }
        if (!truststoreType.isEmpty()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.JAVAX_SSL_TRUSTSTORE_TYPE, truststoreType);
            log.debug("Using truststoreType=", truststoreType);
        }
        if (!truststorePw.isEmpty()) {
            GdsSystemProperties.setSystemProperty(ISslConfiguration.JAVAX_SSL_TRUSTSTORE_PW, truststorePw);
        }
    }

    /**
     * Validate properties.
     */
    protected void validate() {
        final StringBuilder sb = new StringBuilder();
        if (keystorePath != null && !Paths.get(keystorePath).toFile().exists()) {
            sb.append("Unable to load keystore file ");
            sb.append(keystorePath);
            sb.append("\n");
        }
        if (truststorePath != null && !Paths.get(truststorePath).toFile().exists()) {
            sb.append("Unable to load truststore file ");
            sb.append(truststorePath);
        }
        if (sb.length() > 0) {
            log.debug(sb.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasCiphers() {
        return !ciphers.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasHttpsProtocols() {
        return !protocols.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKeystorePath() {
        return this.keystorePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKeystorePassword() {
        return this.keystorePw;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKeystoreType() {
        return this.keystoreType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTruststorePath() {
        return this.truststorePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTruststorePassword() {
        return this.truststorePw;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTruststoreType() {
        return this.truststoreType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHttpsProtocol() {
        return this.protocols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCiphers() {
        return this.ciphers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setSecure(final boolean secure) {
        this.isSecure = secure;
//        if (this.isSecure) {
//            setJavaxSystemProperties();
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {
        return this.isSecure;
    }

    @Override
    public String toString() {
        // Don't log passwords!
        return "SslServerProperties ["
                + "\n  isSecure       = " + isSecure
                + "\n  keystorePath   = " + keystorePath
                + "\n  keystoreType   = " + keystoreType
                + "\n  truststorePath = " + truststorePath
                + "\n  truststoreType = " + truststoreType
                + "\n  protocols      = " + protocols
                + "\n  ciphers        = " + ciphers
                + "\n]"
                + "\nRelevant System Properties ["
                + "\n  protocols      = " + GdsSystemProperties.getSystemProperty(ISslConfiguration.HTTPS_PROTOCOLS)
                + "\n  ciphers        = " + GdsSystemProperties.getSystemProperty(ISslConfiguration.HTTPS_CIPHERS)
                + "\n  keystorePath   = " + GdsSystemProperties.getSystemProperty(ISslConfiguration.JAVAX_SSL_KEYSTORE_PATH)
                + "\n  keystoreType   = " + GdsSystemProperties.getSystemProperty(ISslConfiguration.JAVAX_SSL_KEYSTORE_TYPE)
                + "\n  truststorePath = " + GdsSystemProperties.getSystemProperty(ISslConfiguration.JAVAX_SSL_TRUSTSTORE_PATH)
                + "\n  truststoreType = " + GdsSystemProperties.getSystemProperty(ISslConfiguration.JAVAX_SSL_TRUSTSTORE_TYPE)
                + "\n]";
    }
}
