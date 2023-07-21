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

/**
 * Interface for the SSL Properties configuration
 * 
 *
 */
public interface ISslConfiguration {

    /** Javax ssl system property for keystore path */
    public static final String JAVAX_SSL_KEYSTORE_PATH         = "javax.net.ssl.keyStore";
    /** Javax ssl system property for keystore type */
    public static final String JAVAX_SSL_KEYSTORE_TYPE         = "javax.net.ssl.keyStoreType";
    /** Javax ssl system property for keystore file password */
    public static final String JAVAX_SSL_KEYSTORE_PW           = "javax.net.ssl.keyStorePassword";

    /** Javax ssl system property for truststore path */
    public static final String JAVAX_SSL_TRUSTSTORE_PATH       = "javax.net.ssl.trustStore";
    /** Javax ssl system property for truststore type */
    public static final String JAVAX_SSL_TRUSTSTORE_TYPE       = "javax.net.ssl.trustStoreType";
    /** Javax ssl system property for truststore file password */
    public static final String JAVAX_SSL_TRUSTSTORE_PW         = "javax.net.ssl.trustStorePassword";

    /** System property for defining what https protocols to use */
    public static final String HTTPS_PROTOCOLS                 = "https.protocols";
    /** The default protocol to use */
    public final String        DEFAULT_AMMOS_PROTOCOL          = "TLSv1.2";

    /** System property for defining what ciphers to use */
    public static final String HTTPS_CIPHERS                   = "https.cipherSuites";

    /** The default fully qualified path for SSL/TLS keystore */
    public final String        DEFAULT_AMMOS_KEYSTORE_PATH     = "/ammos/etc/pki/tls/certs/ammos-keystore.p12";

    /** The default type for SSL/TLS keystore */
    public final String        DEFAULT_AMMOS_KEYSTORE_TYPE     = "PKCS12";

    /** The default password for SSL/TLS keystore */
    public final String        DEFAULT_AMMOS_KEYSTORE_PW       = "changeit";

    /** The default fully qualified path for SSL/TLS truststore */
    public final String        DEFAULT_AMMOS_TRUSTSTORE_PATH   = "/ammos/etc/pki/tls/certs/ammos-truststore.jks";

    /** The default type for SSL/TLS truststore */
    public final String        DEFAULT_AMMOS_TRUSTSTORE_TYPE   = "JKS";

    /** The default password for SSL/TLS truststore */
    public final String        DEFAULT_AMMOS_TRUSTSTORE_PW     = "changeit";

    /** "server" */
    public static final String SPRING_SERVER_PROPERTY          = "server";

    /** "server." */
    public static final String SERVER_BLOCK                    = SPRING_SERVER_PROPERTY + ".";

    /** "server.ssl" */
    public static final String SPRING_SSL_PROPERTY             = SERVER_BLOCK + "ssl";

    /** "server.ssl." */
    public static final String SSL_BLOCK                       = SPRING_SSL_PROPERTY + ".";

    /** "server.ssl.key-strore" */
    public static final String SPRING_KEYSTORE_PATH_PROPERTY   = SSL_BLOCK + "key-store";

    /** "server.ssl.key-strore-type" */
    public static final String SPRING_KEYSTORE_TYPE_PROPERTY   = SPRING_KEYSTORE_PATH_PROPERTY + "-type";

    /** "server.ssl.key-strore-password" */
    public static final String SPRING_KEYSTORE_PW_PROPERTY     = SPRING_KEYSTORE_PATH_PROPERTY + "-password";

    /** "server.ssl.trust-store" */
    public static final String SPRING_TRUSTSTORE_PATH_PROPERTY = SSL_BLOCK + "trust-store";

    /** "server.ssl.trust-store-type" */
    public static final String SPRING_TRUSTSTORE_TYPE_PROPERTY = SPRING_TRUSTSTORE_PATH_PROPERTY + "-type";

    /** "server.ssl.trust-store-password" */
    public static final String SPRING_TRUSTSTORE_PW_PROPERTY   = SPRING_TRUSTSTORE_PATH_PROPERTY + "-password";

    /** "server.ssl.ciphers" */
    public static final String SPRING_CIPHERS_PROPERTY         = SSL_BLOCK + "ciphers";

    /** "server.ssl.protocol" */
    public static final String SPRING_PROTOCOLS_PROPERTY       = SSL_BLOCK + "protocol";

    /**
     * @return the fully qualified path to the SSL/TLS keystore
     */
    public String getKeystorePath();

    /**
     * @return the password for the SSL/TLS keystore
     */
    public String getKeystorePassword();

    /**
     * @return the type of keystore
     */
    public String getKeystoreType();

    /**
     * @return the fully qualified path to the SSL/TLS truststore
     */
    public String getTruststorePath();

    /**
     * @return the password for the SSL/TLS truststore
     */
    public String getTruststorePassword();

    /**
     * @return true if the HTTPS protocol has been specified, false if not
     */
    public boolean hasHttpsProtocols();

    /**
     * @return the type of truststore
     */
    public String getTruststoreType();

    /**
     * @return the SSL/TLS protocol being used
     */
    public String getHttpsProtocol();

    /**
     * @return true if ciphers have been specified, false if not
     */
    public boolean hasCiphers();

    /**
     * @return the ciphers supported by the server
     */
    public String getCiphers();

    /**
     * @return the property name prefix for server properties
     */
    public String getPropertyPrefix();

    /**
     * @return true if configuring for secure transport (HTTPS), false if configuring for plain-text transport (HTTP)
     */
    boolean isSecure();

    /**
     * @param secure
     *            true if configuring for secure transport (HTTPS), false if configuring for plain-text transport (HTTP)
     */
    void setSecure(boolean secure);
}
