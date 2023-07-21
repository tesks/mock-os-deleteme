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
package jpl.gds.tc.api.icmd.config;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.message.MessageConstants;
import jpl.gds.shared.string.StringUtil;

/**
 * This class facilitates retrieval of configuration values for ICMD related
 * tasks.
 *
 */
public class IntegratedCommandProperties extends GdsHierarchicalProperties {
    /**
     * Name of the default properties file.
     */
    public static final String PROPERTY_FILE = "icmd.properties";

    private static final String PROPERTY_PREFIX = "icmd.";

    private static final String REST_SERVICE_PROPERTY_BLOCK =
            PROPERTY_PREFIX + "rest.";
    private static final String CPD_URL_PROTOCOL_PROPERTY =
            REST_SERVICE_PROPERTY_BLOCK + "protocol";
    private static final String CPD_HOST_DOMAIN_PROPERTY =
            REST_SERVICE_PROPERTY_BLOCK + "domain";
	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Differentiated timeout properties for
	 * REST connect, read from regular REST calls, and reading from long-poll
	 * style REST calls.
	 */
    private static final String REST_SERVICE_CONNECT_TIMEOUT_PROPERTY =
            REST_SERVICE_PROPERTY_BLOCK + "connectTimeout";
    private static final String REST_SERVICE_REGULAR_CALL_TIMEOUT_PROPERTY =
            REST_SERVICE_PROPERTY_BLOCK + "regularCallTimeout";
    private static final String REST_SERVICE_LONG_POLL_TIMEOUT_PROPERTY =
            REST_SERVICE_PROPERTY_BLOCK + "longPollTimeout";
    private static final String REST_SERVICE_URI_PARAMETER_PROPERTY_BLOCK =
            REST_SERVICE_PROPERTY_BLOCK + "parameters.";
    private static final String REST_SERVICE_NAME_PROPERTY_BLOCK =
            REST_SERVICE_PROPERTY_BLOCK + "serviceUris.";
    
    private static final String JAXB_PROPERTY_BLOCK = PROPERTY_PREFIX + "jaxb.";
    private static final String JAXB_PACKAGE_NAME_PROPERTY =
            JAXB_PROPERTY_BLOCK + "packageName";
    private static final String JAXB_PREFIX_MAPPER_PROPERTY =
            JAXB_PROPERTY_BLOCK + "prefixMapperProperty";
    private static final String XML_ENCODING_PROPERTY =
            JAXB_PROPERTY_BLOCK +  "xmlEncoding";
    private static final String ROOT_MESSAGE_CONFIGURATION_BLOCK =
            JAXB_PROPERTY_BLOCK + "rootMessage.";
    private static final String JAXB_SCHEMA_CONFIGURATION_BLOCK =
            JAXB_PROPERTY_BLOCK + "schema.";

    private static final String POLLERS_PROPERTY_BLOCK = PROPERTY_PREFIX + "pollers.";
	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Removed REQUEST_STATUS_POLL_PROPERTY,
	 * CPD_PARAMETERS_POLL_PROPERTY, and STALE_PARAMETERS_PERIOD_PROPERTY since
	 * we are now using CPD's long-polling.
	 */
    private final static String FAILED_POLLS_BEFORE_STALE_PROPERTY =
            POLLERS_PROPERTY_BLOCK + "failedPollsBeforeStale";
    /*
	 * MPCS-5934 - Josh Choi - 4/10/2015: Duration in milliseconds to wait
	 * between checks to see if command was radiated (finalized).
	 */
    private final static String RADIATION_CHECK_INTERVAL_PROPERTY =
            POLLERS_PROPERTY_BLOCK + "radiationCheckInterval";
    
    /*
     * MPCS-10132 (via MPCS-9315  - 2/7/18): Add configuration for the CPD DMS Broadcast
     * status message polling
	 */
    private final static String CPD_DMS_POLL_PROPERTY = POLLERS_PROPERTY_BLOCK + "dmsPollInterval";

    private static final String PUBLISHER_PROPERTY_BLOCK =
            PROPERTY_PREFIX + "publisher.";
    private static final String PUBLISHER_AGEOUT_PROPERTY =
            PUBLISHER_PROPERTY_BLOCK + "ageout";
    private static final String PUBLISHER_EXTREME_AGEOUT_PROPERTY =
            PUBLISHER_PROPERTY_BLOCK + "extremeAgeout";
    private static final String PUBLISHER_EPOCH_PROPERTY =
            PUBLISHER_PROPERTY_BLOCK + "epoch";

    private static final String JMS_PUBLISHER_AGEOUT_PROPERTY =
            PUBLISHER_PROPERTY_BLOCK + "jmsPublisherAgeout";

    private static final String DEFAULT_COMMAND_SERVICE_SCHEMA_NAMESPACE =
            "http://dsms.jpl.nasa.gov/cmd/schema";
    private static final String DEFAULT_COMMAND_SERVICE_NAMESPACE_PREFIX = "";

    private static final String DEFAULT_XML_ENCODING = "UTF-8";

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Poll interval values are no longer
	 * needed. Removed.
	 */

    private static final long DEFAULT_PUBLISHER_AGEOUT = 8L * 60L * 60L; // Eight
                                                                         // hours
                                                                         // in
                                                                         // seconds

    private static final long DEFAULT_PUBLISHER_EXTREME_AGEOUT =
            24L * 60L * 60L; // 24
                             // hours
                             // in
                             // seconds
    private static final long DEFAULT_PUBLISHER_EPOCH = 7L * 24L * 60L * 60L; // Seven
                                                                              // days
                                                                              // in
                                                                              // seconds

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Differentiated default timeout values
	 * for REST connect, reading from regular REST calls, and reading from
	 * long-poll style REST calls.
	 */
    private static final int DEFAULT_REST_CONNECT_TIMEOUT = 10000;

    private static final int DEFAULT_REGULAR_REST_CALL_TIMEOUT = 10000;

    private static final int DEFAULT_LONG_REST_POLL_TIMEOUT = 60000;

    /*
	 * MPCS-5934 - Josh Choi - 4/5/2015: DEFAULT_STALE_PARAMETERS_PERIOD removed
	 * because CPD long polling renders it obsolete.
	 */

    private final static int DEFAULT_FAILED_POLLS_BEFORE_STALE = 3;

    private final static int DEFAULT_RADIATION_CHECK_INTERVAL = 1000;
    
    private final static int DEFAULT_DMS_POLL_INTERVAL = 1000;

    private static final long DEFAULT_JMS_PUBLISHER_AGEOUT = 60000;

    /** The default JAXB namespace prefix mapper property */
    private static final String DEFAULT_JAXB_NAMESPACE_PREFIX_MAPPER_PROPERTY =
            "com.sun.xml.bind.namespacePrefixMapper";

    /* MPCS-5734 2/6/14: Config values for CPD response buffer */
    private static final String BUFFER_CPD_RESPONSE_PROPERTY =
            REST_SERVICE_PROPERTY_BLOCK + "bufferCpdResponse";
    private static final String CPD_RESPONSE_BUFFER_SIZE_PROPERTY =
            REST_SERVICE_PROPERTY_BLOCK + "cpdReponseBufferSize";
    /** CPD response buffer size, in number of characters */
    private static final int DEFAULT_CPD_RESPONSE_BUFFER_SIZE = 100000;

    private NamespacePrefixMapper nsPrefixMapper;

    /**
     * Constructor for IntegratedCommandConfiguration
     */
    public IntegratedCommandProperties() {
        super(PROPERTY_FILE, true);
    }

    /**
     * Get the ICMD service URI
     *
     * @return the ICMD service URI
     */
    public String getServiceUri(final CpdService service) {
        return getProperty(REST_SERVICE_NAME_PROPERTY_BLOCK
                + service.toString());
    }

    /**
     * Get the URI parameter place holder
     *
     * @param parameter the URI parameter place holder to get
     * @return the URI parameter place holder
     */
    public String getUriParameter(final CpdServiceUriParameter parameter) {
        return getProperty(REST_SERVICE_URI_PARAMETER_PROPERTY_BLOCK
                        + parameter);
    }

    /**
     * Get the JAXB package name. This is the package name where all the Java
     * classes representing the compiled XML schemas reside
     *
     * @return the JAXB package name.
     */
    public String getJaxbPackageName() {
        return getProperty(JAXB_PACKAGE_NAME_PROPERTY);
    }

    /**
     * Get time parameter.
     *
     * @param propertyName Property to read
     * @param defalt Default value
     * @param minimum Minimum value
     *
     * @return The configured parameter
     */
    private long getLongTimeParameter(final String propertyName,
            final long defalt, final long minimum) {
        final String property =
                StringUtil.emptyAsNull(getProperty(propertyName,
                        null));
        long result = 0L;

        if (property == null) {
            return defalt;
        }

        try {
            result = Long.parseLong(property);
        } catch (final NumberFormatException nfe) {
            throw new IllegalArgumentException(propertyName
                    + " is not integral: '" + property + "'");
        }

        return Math.max(result, minimum);
    }

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Removed poll interval parameter
	 * fetching methods since they're no longer needed, with CPD long-polling
	 * now available.
	 */

    /**
     * Get the uplink status publisher ageout parameter. This specifies the
     * length of time in seconds to keep finalized entries before discarding.
     *
     * @return The configured parameter
     */
    public long getPublisherAgeout() {
        return this.getLongTimeParameter(PUBLISHER_AGEOUT_PROPERTY,
                DEFAULT_PUBLISHER_AGEOUT, 0L);
    }

    /**
     * Get the uplink status publisher extreme ageout parameter. This specifies
     * the length of time in seconds to keep old entries before discarding. This
     * is used for items that get "stuck".
     *
     * @return The configured parameter
     */
    public long getPublisherExtremeAgeout() {
        return this.getLongTimeParameter(PUBLISHER_EXTREME_AGEOUT_PROPERTY,
                DEFAULT_PUBLISHER_EXTREME_AGEOUT, 0L);
    }

    /**
     * Get the uplink status publisher epoch parameter. This specifies the
     * length of time in seconds we allow for fetching values from the database.
     *
     * @return The configured parameter
     */
    public long getPublisherEpoch() {
        return this.getLongTimeParameter(PUBLISHER_EPOCH_PROPERTY,
                DEFAULT_PUBLISHER_EPOCH, 0L);
    }

    /**
     * Get the encoding that will be used for generating XML request messages
     *
     * @return the encoding that will be used for generating XML request
     *         messages
     */
    public String getXmlEncoding() {
        return getProperty(XML_ENCODING_PROPERTY,
                DEFAULT_XML_ENCODING);
    }

    /**
     * Get the root message type that is used for requests
     *
     * @return the root message type that is used for requests
     */
    public String getRootMessageType() {
        return getProperty(ROOT_MESSAGE_CONFIGURATION_BLOCK
                + "type");
    }

    /**
     * Get the root message schema that is used for requests
     *
     * @return the root message schema that is used for requests
     */
    public String getRootMessageSchema() {
        return getProperty(ROOT_MESSAGE_CONFIGURATION_BLOCK
                + "schemaName");
    }

    /**
     * Get the root message schema version that is used for requests
     *
     * @return the root message schema version that is used for requests
     */
    public String getRootMessageSchemaVersion() {
        return getProperty(ROOT_MESSAGE_CONFIGURATION_BLOCK
                + "schemaVersion");
    }

    /**
     * Get the command service schema namespace
     *
     * @return the command service schema namespace
     */
    public String getCommandServiceSchemaNamespace() {
        return getProperty(JAXB_SCHEMA_CONFIGURATION_BLOCK
                + "commandServiceNamespace",
                DEFAULT_COMMAND_SERVICE_SCHEMA_NAMESPACE);
    }

    /**
     * Get the command service schema namespace prefix
     *
     * @return the command service schema namespace prefix
     */
    public String getCommandServiceSchemaPrefix() {
        return getProperty(JAXB_SCHEMA_CONFIGURATION_BLOCK
                + "commandServiceNamespacePrefix",
                DEFAULT_COMMAND_SERVICE_NAMESPACE_PREFIX);
    }

    /*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Differentiated "get" methods for three
	 * types of timeout values: One for REST connect, one for reading from
	 * regular REST calls, and another for reading from long-poll style REST
	 * calls (which is new).
	 */
	/**
	 * Get the number of milliseconds before a connection attempt to a REST
	 * server is timed out
	 *
	 * @return the number of milliseconds before a REST connect should time out
	 */
    public int getRestConnectTimeout() {
        return getIntProperty(REST_SERVICE_CONNECT_TIMEOUT_PROPERTY,
                DEFAULT_REST_CONNECT_TIMEOUT);
    }

	/**
	 * Get the number of milliseconds before a regular rest call should time out
	 *
	 * @return the number of milliseconds before a regular rest call should time
	 *         out
	 */
	public int getRegularRestCallTimeout() {
		return getIntProperty(
				REST_SERVICE_REGULAR_CALL_TIMEOUT_PROPERTY,
				DEFAULT_REGULAR_REST_CALL_TIMEOUT);
	}

    /**
	 * Get the number of milliseconds before a regular rest call should time out
	 *
	 * @return the number of milliseconds before a regular rest call should time
	 *         out
	 */
    public int getLongRestPollTimeout() {
        return getIntProperty(REST_SERVICE_LONG_POLL_TIMEOUT_PROPERTY,
                DEFAULT_LONG_REST_POLL_TIMEOUT);
    }

	/*
	 * MPCS-5934 - Josh Choi - 4/2/2015: Removed getStaleParameterPeriod method,
	 * since CPD long polling ensures that we will never have stale data (unless
	 * the poll itself fails, which is handled by counting failures, not time)
	 */

	/**
	 * Get the number of failed polls before data is considered stale
	 *
	 * @return the number of failed polls before data is considered stale
	 */
	public int getNumFailedPollsBeforeStale() {
		return this.getIntProperty(FAILED_POLLS_BEFORE_STALE_PROPERTY, DEFAULT_FAILED_POLLS_BEFORE_STALE);
	}

	/*
	 * MPCS-5934 - Josh Choi - 4/10/2015
	 */
	/**
	 * Get the number of milliseconds between checks to see if command was
	 * radiated (internal map poll, not CPD poll).
	 *
	 * @return the interval, in milliseconds
	 */
	public int getRadiationCheckInterval() {
		return this.getIntProperty(RADIATION_CHECK_INTERVAL_PROPERTY, DEFAULT_RADIATION_CHECK_INTERVAL);
	}
	
	/**
	 * MPCS-10132 - dyates (via MPCS-9315 )
	 * Get the number of milliseconds between CPD broadcast status messages
	 * 
	 * @return polling interval in milliseconds
	 */
	public int getDmsPollingInterval() {
		return this.getIntProperty(CPD_DMS_POLL_PROPERTY, DEFAULT_DMS_POLL_INTERVAL);
 	}

    /**
     * Get the protocol used for communicating with CPD
     *
     * @return the protocol used for communicating with CPD
     */
    public String getCpdUrlProtocol() {
        return this.getProperty(
                CPD_URL_PROTOCOL_PROPERTY, "http");
    }

    /**
     * Get the domain the CPD host is on
     *
     * @return the domain the CPD host is on
     */
    public String getCpdHostDomain() {
        return this.getProperty(
                CPD_HOST_DOMAIN_PROPERTY, "");
    }

    /**
     * Get the message service publisher age out time
     *
     * @return the message service publisher age out time
     */
    public long getMessagePublisherAgeout() {
        return this.getLongProperty(
                JMS_PUBLISHER_AGEOUT_PROPERTY, DEFAULT_JMS_PUBLISHER_AGEOUT);
    }

    /**
     * Get the JAXB property to set to point to the namespace prefix mapper
     *
     * @return the JAXB property to set to point to the namespace prefix mapper
     */
    public String getJaxbPrefixMapperProperty() {
        return this.getProperty(
                JAXB_PREFIX_MAPPER_PROPERTY,
                DEFAULT_JAXB_NAMESPACE_PREFIX_MAPPER_PROPERTY);
    }

    /**
     * Get the JAXB namespace prefix mapper
     *
     * @return the JAXB namespace prefix mapper
     */
    public NamespacePrefixMapper getPrefixMapper() {
        if (this.nsPrefixMapper == null) {
            this.nsPrefixMapper = new CommandServicePrefixMapper();
        }

        return this.nsPrefixMapper;
    }

    /**
     * Namespace prefix mapper
     *
     * @since AMPCS R5
     */
    private class CommandServicePrefixMapper extends NamespacePrefixMapper {
        @Override
        public String getPreferredPrefix(final String namespaceUri,
                final String suggestion, final boolean requirePrefix) {

            if (namespaceUri
                    .equalsIgnoreCase(IntegratedCommandProperties.this
                            .getCommandServiceSchemaNamespace())) {
                return IntegratedCommandProperties.this
                        .getCommandServiceSchemaPrefix();
            } else if (namespaceUri
                    .equalsIgnoreCase(MessageConstants.DSMS_NAMESPACE)) {
                return MessageConstants.DSMS_PREFIX;
            } else {
                return suggestion;
            }
        }
    }

    /*
     * BEGIN MPCS-5734 2/6/14: Added configuration values for CPD
     * response buffering
     */

    /**
     * Whether or not to buffer CPD's response
     *
     * @return true if configured to buffer CPD's response, false otherwise
     */
    public boolean bufferCpdResponse() {
        return this.getBooleanProperty(
                BUFFER_CPD_RESPONSE_PROPERTY, false);
    }

    /**
     * Get the configured CPD response buffer size
     *
     * @return the configured CPD response buffer size
     */
    public int getCpdResponseBufferSize() {
        return this.getIntProperty(
                CPD_RESPONSE_BUFFER_SIZE_PROPERTY,
                DEFAULT_CPD_RESPONSE_BUFFER_SIZE);
    }

    /*
     * END MPCS-5734 2/6/14
     */
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
