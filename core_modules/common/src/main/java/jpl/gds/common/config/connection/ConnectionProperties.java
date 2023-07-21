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
package jpl.gds.common.config.connection;

import jpl.gds.common.config.types.*;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.AsStringComparator;
import jpl.gds.shared.util.HostPortUtility;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A configuration properties object for default uplink and downlink connection
 * parameters. Should not be confused with @see IConnectionMap, which
 * carries current connection settings.
 * 
 * @since R8
 *
 */
public class ConnectionProperties extends GdsHierarchicalProperties {
	/**
	 * Name of the default properties file.
	 */
	public static final String PROPERTY_FILE = "connection.properties";

	private static final String PROPERTY_PREFIX = "connection.";

	private static final String DEFAULT_TOKEN = "DEFAULT";
	private static final String DEFAULT_HOST_SUFFIX = "defaultHost";
	private static final String DEFAULT_PORT_SUFFIX = "defaultPort";
	
	private static final String LIST_DELIM = ",";
	private static final String ALLOWED = "allowed";
	private static final String DEFAULT = "default";

	private static final TelemetryConnectionType DEFAULT_DL_CONNECTION_TYPE = TelemetryConnectionType.CLIENT_SOCKET;
	private static final TelemetryInputType DEFAULT_INPUT_TYPE = TelemetryInputType.RAW_TF;


	private static final String FSW_BLOCK = PROPERTY_PREFIX + "flight.";
	private static final String FSW_UPLINK_BLOCK = FSW_BLOCK + "uplink.";
	private static final String FSW_DOWNLINK_BLOCK = FSW_BLOCK + "downlink.";

	private static final String FSW_DOWNLINK_HOST = FSW_DOWNLINK_BLOCK
			+ DEFAULT_HOST_SUFFIX;
	private static final String FSW_DOWNLINK_PORT = FSW_DOWNLINK_BLOCK
			+ DEFAULT_PORT_SUFFIX;
	private static final String FSW_UPLINK_HOST = FSW_UPLINK_BLOCK
			+ DEFAULT_HOST_SUFFIX;
	private static final String FSW_UPLINK_PORT = FSW_UPLINK_BLOCK
			+ DEFAULT_PORT_SUFFIX;

	private static final String SSE_BLOCK = PROPERTY_PREFIX + "sse.";
	private static final String SSE_UPLINK_BLOCK = SSE_BLOCK + "uplink.";
	private static final String SSE_DOWNLINK_BLOCK = SSE_BLOCK + "downlink.";

	private static final String SSE_DOWNLINK_HOST = SSE_DOWNLINK_BLOCK
			+ DEFAULT_HOST_SUFFIX;
	private static final String SSE_DOWNLINK_PORT = SSE_DOWNLINK_BLOCK
			+ DEFAULT_PORT_SUFFIX;
	private static final String SSE_UPLINK_HOST = SSE_UPLINK_BLOCK
			+ DEFAULT_HOST_SUFFIX;
	private static final String SSE_UPLINK_PORT = SSE_UPLINK_BLOCK
			+ DEFAULT_PORT_SUFFIX;

	private static final String LOGICAL_HOST_PREFIX = PROPERTY_PREFIX
			+ "logicalHost.";

    private static final String FSW_DL_CONNECTION_BLOCK = FSW_DOWNLINK_BLOCK
            + "connectionType.";

    private static final String FSW_UL_CONNECTION_BLOCK = FSW_UPLINK_BLOCK
            + "connectionType.";

    private static final String FSW_ALLOWED_DL_CONNECTIONS_PROPERTY = FSW_DL_CONNECTION_BLOCK
            + ALLOWED;
    private static final String FSW_DEFAULT_DL_CONNECTION_PROPERTY = FSW_DL_CONNECTION_BLOCK
            + DEFAULT;

    private static final String FSW_ALLOWED_UL_CONNECTIONS_PROPERTY = FSW_UL_CONNECTION_BLOCK
            + ALLOWED;
    private static final String FSW_DEFAULT_UL_CONNECTION_PROPERTY = FSW_UL_CONNECTION_BLOCK
            + DEFAULT;

    private static final String FSW_SOURCE_FORMATS_BLOCK = FSW_DOWNLINK_BLOCK
            + "sourceFormat.";

    private static final String FSW_ALLOWED_SOURCE_FORMATS_PROPERTY = FSW_SOURCE_FORMATS_BLOCK
            + ALLOWED;
    private static final String FSW_DEFAULT_SOURCE_FORMAT_PROPERTY = FSW_SOURCE_FORMATS_BLOCK
            + DEFAULT;

    private static final String SSE_DL_CONNECTION_BLOCK = SSE_DOWNLINK_BLOCK
            + "connectionType.";

    private static final String SSE_UL_CONNECTION_BLOCK = SSE_UPLINK_BLOCK
            + "connectionType.";

    private static final String SSE_ALLOWED_DL_CONNECTIONS_PROPERTY = SSE_DL_CONNECTION_BLOCK
            + ALLOWED;
    private static final String SSE_DEFAULT_DL_CONNECTION_PROPERTY = SSE_DL_CONNECTION_BLOCK
            + DEFAULT;

    private static final String SSE_ALLOWED_UL_CONNECTIONS_PROPERTY = SSE_UL_CONNECTION_BLOCK
            + ALLOWED;
    private static final String SSE_DEFAULT_UL_CONNECTION_PROPERTY = SSE_UL_CONNECTION_BLOCK
            + DEFAULT;

    private static final String SSE_SOURCE_FORMATS_BLOCK = SSE_DOWNLINK_BLOCK
            + "sourceFormat.";

    private static final String SSE_ALLOWED_SOURCE_FORMATS_PROPERTY = SSE_SOURCE_FORMATS_BLOCK
            + ALLOWED;
    private static final String SSE_DEFAULT_SOURCE_FORMAT_PROPERTY = SSE_SOURCE_FORMATS_BLOCK
            + DEFAULT;

	/**
	 * Hard coded default fsw uplink port.
	 */
	private static final int DEFAULT_FSW_UPLINK_PORT = 12345;

	/**
	 * Hard coded default sse default uplink port.
	 */
	private static final int DEFAULT_SSE_UPLINK_PORT = 12346;

	/**
	 * Hard coded default FSW downlink port.
	 */
	private static final int DEFAULT_FSW_DOWNLINK_PORT = 12347;

	/**
	 * Hard coded default SSE downlink port.
	 */
	private static final int DEFAULT_SSE_DOWNLINK_PORT = 12348;

	/**
     * Test constructor
     *
     */
    public ConnectionProperties() {
        this(new SseContextFlag());
	}

    /**
     * Constructor for a unique instance. Loads the default properties file.
     *
     * @param sseFlag
     *            The SSE context flag
     *
     */
    public ConnectionProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }


    @Override
    protected void validate() {

        /* Default connection types must be in allowed connection types */
        if (!getAllowedDownlinkConnectionTypes(true).contains(
                getDefaultDownlinkConnectionType(true))) {
            logContainmentError(SSE_DEFAULT_DL_CONNECTION_PROPERTY,
                    SSE_ALLOWED_DL_CONNECTIONS_PROPERTY);
        }

        if (!getAllowedDownlinkConnectionTypes(false).contains(
                getDefaultDownlinkConnectionType(false))) {
            logContainmentError(FSW_DEFAULT_DL_CONNECTION_PROPERTY,
                    FSW_ALLOWED_DL_CONNECTIONS_PROPERTY);
        }

        if (getDefaultUplinkConnectionType(true) != null
                && !getAllowedUplinkConnectionTypes(true).isEmpty()
                && !getAllowedUplinkConnectionTypes(true).contains(
                        getDefaultUplinkConnectionType(true))) {
            logContainmentError(SSE_DEFAULT_UL_CONNECTION_PROPERTY,
                    SSE_ALLOWED_UL_CONNECTIONS_PROPERTY);
        }

        if (getDefaultUplinkConnectionType(false) != null
                && !getAllowedUplinkConnectionTypes(false).isEmpty()
                && !getAllowedUplinkConnectionTypes(false).contains(
                        getDefaultUplinkConnectionType(false))) {
            logContainmentError(FSW_DEFAULT_UL_CONNECTION_PROPERTY,
                    FSW_ALLOWED_UL_CONNECTIONS_PROPERTY);
        }

        /* Default source formats must be in the list of allowed formats */
        if (!getAllowedDownlinkSourceFormats(true).contains(
                getDefaultSourceFormat(true))) {
            logContainmentError(SSE_DEFAULT_SOURCE_FORMAT_PROPERTY,
                    SSE_ALLOWED_SOURCE_FORMATS_PROPERTY);
        }

        if (!getAllowedDownlinkSourceFormats(false).contains(
                getDefaultSourceFormat(false))) {
            logContainmentError(FSW_DEFAULT_SOURCE_FORMAT_PROPERTY,
                    FSW_ALLOWED_SOURCE_FORMATS_PROPERTY);
        }

        /* Now check venue specific values */
        for (final VenueType vt : VenueType.values()) {

            if (!getAllowedDownlinkConnectionTypes(vt, true).contains(
                    getDefaultDownlinkConnectionType(vt, true))) {
                logContainmentError(
                        SSE_DEFAULT_DL_CONNECTION_PROPERTY + "." + vt.toString(),
                        SSE_ALLOWED_DL_CONNECTIONS_PROPERTY + "." + vt.toString());
            }

            if (!getAllowedDownlinkConnectionTypes(vt, false).contains(
                    getDefaultDownlinkConnectionType(vt, false))) {
                logContainmentError(
                        FSW_DEFAULT_DL_CONNECTION_PROPERTY + "." + vt.toString(),
                        FSW_ALLOWED_DL_CONNECTIONS_PROPERTY + "." + vt.toString());
            }


            if (getDefaultUplinkConnectionType(vt, true) != null
                    && !getAllowedUplinkConnectionTypes(true).isEmpty()
                    && !getAllowedUplinkConnectionTypes(vt, true).contains(
                            getDefaultUplinkConnectionType(vt, true))) {
                logContainmentError(
                        SSE_DEFAULT_UL_CONNECTION_PROPERTY + "." + vt.toString(),
                        SSE_ALLOWED_UL_CONNECTIONS_PROPERTY + "." + vt.toString());

            }

            if (getDefaultUplinkConnectionType(vt, false) != null
                    && !getAllowedUplinkConnectionTypes(false).isEmpty()
                    && !getAllowedUplinkConnectionTypes(vt, false).contains(
                            getDefaultUplinkConnectionType(vt, false))) {
                logContainmentError(
                        FSW_DEFAULT_UL_CONNECTION_PROPERTY + "." + vt.toString(),
                        FSW_ALLOWED_UL_CONNECTIONS_PROPERTY + "." + vt.toString());

            }

            if (!getAllowedDownlinkSourceFormats(true).contains(
                    getDefaultSourceFormat(vt, true))) {
                logContainmentError(
                        SSE_DEFAULT_SOURCE_FORMAT_PROPERTY + "." + vt.toString(),
                        SSE_ALLOWED_SOURCE_FORMATS_PROPERTY);
            }

            if (!getAllowedDownlinkSourceFormats(false).contains(
                    getDefaultSourceFormat(vt, false))) {
                logContainmentError(
                        FSW_DEFAULT_SOURCE_FORMAT_PROPERTY + "." + vt.toString(),
                        FSW_ALLOWED_SOURCE_FORMATS_PROPERTY);
            }
        }

        if (errorLogged) {
            log.error("Something seems seriously wrong with " + getBaseFilename());
        }
    }

	/**
	 * Returns the host name or IP (as string) that the supplied logical host
	 * name maps to in the configuration file.
	 *
	 * @param hostToMap
	 *            logical host name
	 * @return host or IP string the logical host maps to, or null if no match
	 *         in the configuration
	 */
	public String mapLogicalHost(final String hostToMap) {
		String mappedVal = getProperty(LOGICAL_HOST_PREFIX + hostToMap, null);

		if (mappedVal == null) {
			mappedVal = getProperty(
					LOGICAL_HOST_PREFIX + hostToMap.toUpperCase(), null);

			if (mappedVal == null) {
				getProperty(LOGICAL_HOST_PREFIX + hostToMap.toLowerCase(), null);
			}
		}
		return (mappedVal == null ? hostToMap : mappedVal);
	}

	/**
	 * Gets the default downlink host name for flight or SSE.
	 *
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default host name; never null, will default to localhost
	 */
	public String getDefaultDownlinkHost(final boolean forSse) {
		return getHostProperty(
				(forSse ? SSE_DOWNLINK_HOST : FSW_DOWNLINK_HOST),
				HostPortUtility.LOCALHOST);
	}

	/**
	 * Gets the default downlink host name for flight or SSE for a specific
	 * venue type. If no default is defined for the given venue type or it is
	 * null, the general default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default host name; never null, will default to localhost
	 */
	public String getDefaultDownlinkHost(final VenueType venue, final boolean forSse) {
		if (venue == null) {
			return getDefaultDownlinkHost(forSse);
		}
		return getHostProperty((forSse ? SSE_DOWNLINK_HOST : FSW_DOWNLINK_HOST)
				+ "." + venue.name(), getDefaultDownlinkHost(forSse));
	}

	/**
	 * Gets the default downlink host name for flight or SSE for a specific
	 * venue type and testbed name. If no default is defined for the given
	 * testbed or the testbed name is null, the venue default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param testbedName
	 *            testbedName to get the default for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default host name; never null, will default to localhost
	 */
	public String getDefaultDownlinkHost(final VenueType venue, final String testbedName,
			final boolean forSse) {
		if (testbedName == null) {
			return getDefaultDownlinkHost(venue, forSse);
		}
		if (venue == null) {
			return getDefaultDownlinkHost(forSse);
		}
		return getHostProperty((forSse ? SSE_DOWNLINK_HOST : FSW_DOWNLINK_HOST)
				+ "." + venue.name() + "." + testbedName,
				getDefaultDownlinkHost(venue, forSse));
	}

	/**
	 * Gets the default flight downlink host name for a specific venue type,
	 * testbed name, and stream type. If no default is defined for the given
	 * stream type or the stream type name is null or N/A, the testbed default
	 * is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param testbedName
	 *            testbedName to get the default for; may be null
	 * @param stream
	 *            the downlink stream type; may be null
	 * @return default host name; never null, will default to localhost
	 */
	public String getDefaultDownlinkHost(final VenueType venue, final String testbedName,
			final DownlinkStreamType stream) {

		if (stream == null || stream == DownlinkStreamType.NOT_APPLICABLE) {
			return getDefaultDownlinkHost(venue, testbedName, false);
		}
		if (testbedName == null) {
			return getDefaultDownlinkHost(venue, false);
		}
		if (venue == null) {
			return getDefaultDownlinkHost(false);
		}
		String host = getHostProperty(FSW_DOWNLINK_HOST + "." + venue.name()
				+ "." + testbedName + "." + stream.name(), null);
		if (host == null) {
			host = getHostProperty(FSW_DOWNLINK_HOST + "." + venue.name() + "."
					+ DEFAULT_TOKEN + "." + stream.name(), null);
			if (host == null) {
				host = getHostProperty(FSW_DOWNLINK_HOST + "." + venue.name()
						+ "." + DEFAULT_TOKEN,
						getDefaultDownlinkHost(venue, testbedName, false));
			}
		}
		return host;
	}

	/**
	 * Gets the default downlink port for flight or SSE.
	 *
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default port
	 */
	public int getDefaultDownlinkPort(final boolean forSse) {
		return getIntProperty(
				(forSse ? SSE_DOWNLINK_PORT : FSW_DOWNLINK_PORT),
				(forSse ? DEFAULT_SSE_DOWNLINK_PORT : DEFAULT_FSW_DOWNLINK_PORT));
	}

	/**
	 * Gets the default downlink port for flight or SSE for a specific venue
	 * type. If no default is defined for the given venue type or the venue type
	 * is null, the general default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default port
	 */
	public int getDefaultDownlinkPort(final VenueType venue, final boolean forSse) {
		if (venue == null) {
			return getDefaultDownlinkPort(forSse);
		}
		return getIntProperty((forSse ? SSE_DOWNLINK_PORT : FSW_DOWNLINK_PORT)
				+ "." + venue.name(), getDefaultDownlinkPort(forSse));
	}

	/**
	 * Gets the default downlink port for flight or SSE for a specific venue
	 * type and testbed name. If no default is defined for the given testbed or
	 * the testbed name is null, the venue default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param testbedName
	 *            testbedName to get the default for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default port
	 */
	public int getDefaultDownlinkPort(final VenueType venue, final String testbedName,
			final boolean forSse) {
		if (testbedName == null) {
			return getDefaultDownlinkPort(venue, forSse);
		}
		if (venue == null) {
			return getDefaultDownlinkPort(forSse);
		}
		return getIntProperty((forSse ? SSE_DOWNLINK_PORT : FSW_DOWNLINK_PORT)
				+ "." + venue.name() + "." + testbedName,
				getDefaultDownlinkPort(venue, forSse));

	}

	/**
	 * Gets the default flight downlink port for a specific venue type, testbed
	 * name, and stream type. If no default is defined for the given stream type
	 * or the stream type name is null or N/A, the testbed default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param testbedName
	 *            testbedName to get the default for; may be null
	 * @param stream
	 *            the downlink stream type; may be null
	 * @return default port
	 */
	public int getDefaultDownlinkPort(final VenueType venue, final String testbedName,
			final DownlinkStreamType stream) {

		if (stream == null || stream == DownlinkStreamType.NOT_APPLICABLE) {
			return getDefaultDownlinkPort(venue, testbedName, false);
		}
		if (testbedName == null) {
			return getDefaultDownlinkPort(venue, false);
		}
		if (venue == null) {
			return getDefaultDownlinkPort(false);
		}
		int port = getIntProperty(FSW_DOWNLINK_PORT + "." + venue.name() + "."
				+ testbedName + "." + stream.name(),
				HostPortUtility.UNDEFINED_PORT);

		if (port == HostPortUtility.UNDEFINED_PORT) {
			port = getIntProperty(FSW_DOWNLINK_PORT + "." + venue.name() + "."
					+ DEFAULT_TOKEN + "." + stream.name(),
					HostPortUtility.UNDEFINED_PORT);
			if (port == HostPortUtility.UNDEFINED_PORT) {
				port = getIntProperty(FSW_DOWNLINK_PORT + "." + venue.name()
						+ "." + DEFAULT_TOKEN,
						getDefaultDownlinkPort(venue, testbedName, false));
			}
		}

		return port;
	}

	/**
	 * Gets the default uplink host name for flight or SSE.
	 *
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default host name; never null, will default to localhost
	 */
	public String getDefaultUplinkHost(final boolean forSse) {
		return getHostProperty((forSse ? SSE_UPLINK_HOST : FSW_UPLINK_HOST),
				HostPortUtility.LOCALHOST);
	}

	/**
	 * Gets the default uplink host name for flight or SSE for a specific venue
	 * type. If no default is defined for the given venue type or it is null,
	 * the general default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default host name; never null, will default to localhost
	 */
	public String getDefaultUplinkHost(final VenueType venue, final boolean forSse) {
		if (venue == null) {
			return getDefaultUplinkHost(forSse);
		}
		return getHostProperty((forSse ? SSE_UPLINK_HOST : FSW_UPLINK_HOST)
				+ "." + venue.name(), getDefaultUplinkHost(forSse));
	}

	/**
	 * Gets the default uplink host name for flight or SSE for a specific venue
	 * type and testbed name. If no default is defined for the given testbed or
	 * the testbed name is null, the venue default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param testbedName
	 *            testbedName to get the default for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default host name; never null, will default to localhost
	 */
	public String getDefaultUplinkHost(final VenueType venue, final String testbedName,
			final boolean forSse) {
		if (testbedName == null) {
			return getDefaultUplinkHost(venue, forSse);
		}
		if (venue == null) {
			return getDefaultUplinkHost(forSse);
		}
		return getHostProperty((forSse ? SSE_UPLINK_HOST : FSW_UPLINK_HOST)
				+ "." + venue.name() + "." + testbedName,
				getDefaultDownlinkHost(venue, forSse));
	}

	/**
	 * Gets the default uplink port for flight or SSE.
	 *
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default port
	 */
	public int getDefaultUplinkPort(final boolean forSse) {
		return getIntProperty((forSse ? SSE_UPLINK_PORT : FSW_UPLINK_PORT),
				(forSse ? DEFAULT_SSE_UPLINK_PORT : DEFAULT_FSW_UPLINK_PORT));
	}

	/**
	 * Gets the default uplink port for flight or SSE for a specific venue type.
	 * If no default is defined for the given venue type or the venue type is
	 * null, the general default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default port
	 */
	public int getDefaultUplinkPort(final VenueType venue, final boolean forSse) {
		if (venue == null) {
			return getDefaultUplinkPort(forSse);
		}
		return getIntProperty((forSse ? SSE_UPLINK_PORT : FSW_UPLINK_PORT)
				+ "." + venue.name(), getDefaultUplinkPort(forSse));
	}

	/**
	 * Gets the default uplink port for flight or SSE for a specific venue type
	 * and testbed name. If no default is defined for the given testbed or the
	 * testbed name is null, the venue default is returned.
	 *
	 * @param venue
	 *            the venue type to get the default host for; may be null
	 * @param testbedName
	 *            testbedName to get the default for; may be null
	 * @param forSse
	 *            true if for SSE, false if for flight
	 * @return default port
	 */
	public int getDefaultUplinkPort(final VenueType venue, final String testbedName,
			final boolean forSse) {
		if (testbedName == null) {
			return getDefaultUplinkPort(venue, forSse);
		}
		if (venue == null) {
			return getDefaultUplinkPort(forSse);
		}
		return getIntProperty((forSse ? SSE_UPLINK_PORT : FSW_UPLINK_PORT)
				+ "." + venue.name() + "." + testbedName,
				getDefaultUplinkPort(venue, forSse));
	}

	  /**
     * Gets a sorted set of all allowed downlink connection types in the current
     * configuration.
     *
	 * @param forSse true if for SSE, false if for flight
     *
     * @return Set of DownlinkConnectionType, never null or empty
     */
    public Set<TelemetryConnectionType> getAllowedDownlinkConnectionTypes(final boolean forSse) {
        final List<String> temp = getListProperty(forSse ? SSE_ALLOWED_DL_CONNECTIONS_PROPERTY : FSW_ALLOWED_DL_CONNECTIONS_PROPERTY,
                DEFAULT_DL_CONNECTION_TYPE.toString(), LIST_DELIM);
        return convertDownlinkConnectionList(temp,
        		forSse ? SSE_ALLOWED_DL_CONNECTIONS_PROPERTY : FSW_ALLOWED_DL_CONNECTIONS_PROPERTY);
    }

    /**
     * Gets a sorted set of all allowed downlink connection types in the current
     * configuration, as Strings.
     *
     * @param forSse true if for SSE, false if for flight
     *
     * @return Set of String, never null or empty
     */
    public Set<String> getAllowedDownlinkConnectionTypesAsStrings(final boolean forSse) {
        final Set<TelemetryConnectionType> temp = getAllowedDownlinkConnectionTypes(forSse);
        return toSortedStringSet(temp);
    }

    /**
     * Gets a sorted set of allowed downlink connection types in the current
     * configuration, qualified by a specific venue type. Defaults to the
     * general list of connection types if no allowed list is specifically
     * configured for the venue.
     *
     * @param vt
     *            VenueType to get property for
     * @param forSse true if for SSE, false if for flight
     *
     * @return Set of DownlinkConnectionType, never null or empty
     */
    public Set<TelemetryConnectionType> getAllowedDownlinkConnectionTypes(
            final VenueType vt, final boolean forSse) {

        if (vt == null) {
            return getAllowedDownlinkConnectionTypes(forSse);
        }

        final List<String> temp = getListProperty((forSse ? SSE_ALLOWED_DL_CONNECTIONS_PROPERTY : FSW_ALLOWED_DL_CONNECTIONS_PROPERTY)
                + "." + vt.toString(), null, LIST_DELIM);
        if (temp.isEmpty()) {
            return getAllowedDownlinkConnectionTypes(forSse);
        }
        return convertDownlinkConnectionList(temp,
        		(forSse ? SSE_ALLOWED_DL_CONNECTIONS_PROPERTY : FSW_ALLOWED_DL_CONNECTIONS_PROPERTY) + "." + vt.toString());
    }

	/**
	 * Gets a sorted set of strings for the allowed downlink connection types in
	 * the current configuration, qualified by a specific venue type. Defaults
	 * to the general list of connection types if no allowed list is
	 * specifically configured for the venue.
	 *
	 * @param vt
	 *            VenueType to get property for
	 * @param forSse true if for SSE, false if for flight
	 *
	 * @return Set of String DownlinkConnectionTypes, never null or empty
	 *
	 */
    public Set<String> getAllowedDownlinkConnectionTypesAsStrings(final VenueType vt, final boolean forSse) {
    	final Set<TelemetryConnectionType> temp = getAllowedDownlinkConnectionTypes(vt, forSse);
    	return toSortedStringSet(temp);
    }

    /**
     * Gets the general default for downlink connection type in the current
     * configuration.
     *
     * @param forSse true if for SSE, false if for flight
     *
     * @return DownlinkConnectionType, never null
     */
    public TelemetryConnectionType getDefaultDownlinkConnectionType(final boolean forSse) {
        final String temp = getProperty(forSse ? SSE_DEFAULT_DL_CONNECTION_PROPERTY : FSW_DEFAULT_DL_CONNECTION_PROPERTY,
                DEFAULT_DL_CONNECTION_TYPE.toString());
        try {
            return TelemetryConnectionType.valueOf(temp);
        } catch (final IllegalArgumentException e) {
            reportError(forSse ? SSE_DEFAULT_DL_CONNECTION_PROPERTY : FSW_DEFAULT_DL_CONNECTION_PROPERTY, temp,
                    DEFAULT_DL_CONNECTION_TYPE.toString());
            return DEFAULT_DL_CONNECTION_TYPE;
        }
    }

    /**
     * Gets the default for downlink connection type for the specified venue in
     * the current configuration. If none is defined for the given venue,
     * returns the general default connection type.
     *
     * @param vt
     *            VenueType to get property for
     * @param forSse true if for SSE, false if for flight
     *
     * @return DownlinkConnectionType, never null
     */
    public TelemetryConnectionType getDefaultDownlinkConnectionType(final VenueType vt, final boolean forSse) {
        if (vt == null) {
            return getDefaultDownlinkConnectionType(forSse);
        }
        final String temp = getProperty(
        		(forSse ? SSE_DEFAULT_DL_CONNECTION_PROPERTY : FSW_DEFAULT_DL_CONNECTION_PROPERTY) + "." + vt.toString(), null);
        if (temp == null) {
            return getDefaultDownlinkConnectionType(forSse);
        }
        try {
            return TelemetryConnectionType.valueOf(temp);
        } catch (final IllegalArgumentException e) {
            final TelemetryConnectionType dct = getDefaultDownlinkConnectionType(forSse);
            reportError((forSse ? SSE_DEFAULT_DL_CONNECTION_PROPERTY : FSW_DEFAULT_DL_CONNECTION_PROPERTY) + "." + vt.toString(),
                    temp, dct.toString());
            return dct;
        }
    }

    /**
     * Gets the general default for uplink connection type in the current
     * configuration.
     *
     * @param forSse true if for SSE, false if for flight
     *
     * @return UplinkConnectionType, may be null in mission configurations that
     *         do not support uplink
     */
    public UplinkConnectionType getDefaultUplinkConnectionType(final boolean forSse) {
        final String temp = getProperty(forSse ? SSE_DEFAULT_UL_CONNECTION_PROPERTY : FSW_DEFAULT_UL_CONNECTION_PROPERTY, null);
        if (temp == null) {
            return null;
        }
        try {
            return UplinkConnectionType.valueOf(temp);
        } catch (final IllegalArgumentException e) {
            reportError(forSse ? SSE_DEFAULT_UL_CONNECTION_PROPERTY : FSW_DEFAULT_UL_CONNECTION_PROPERTY, temp, null);
            return null;
        }
    }

    /**
     * Gets the default for uplink connection type for the specified venue in
     * the current configuration. If none is defined for the given venue,
     * returns the general default connection type.
     *
     * @param vt
     *            VenueType to get property for
     * @param forSse true if for SSE, false if for flight
     *
     * @return UplinkConnectionType, may be null in mission configurations that
     *         do not support uplink
     */
    public UplinkConnectionType getDefaultUplinkConnectionType(final VenueType vt, final boolean forSse) {
        if (vt == null) {
            return getDefaultUplinkConnectionType(forSse);
        }
        final String temp = getProperty(
        		(forSse ? SSE_DEFAULT_UL_CONNECTION_PROPERTY : FSW_DEFAULT_UL_CONNECTION_PROPERTY) + "." + vt.toString(), null);
        if (temp == null) {
            return getDefaultUplinkConnectionType(forSse);
        }
        try {
            return UplinkConnectionType.valueOf(temp);
        } catch (final IllegalArgumentException e) {
            final UplinkConnectionType utc = getDefaultUplinkConnectionType(forSse);
            reportError((forSse ? SSE_DEFAULT_UL_CONNECTION_PROPERTY : FSW_DEFAULT_UL_CONNECTION_PROPERTY) + "." + vt.toString(),
                    temp, utc.toString());
            return utc;
        }
    }

    /**
     * Gets a sorted set of all allowed uplink connection types in the current
     * configuration.
     *
     * @param forSse true if for SSE, false if for flight
     *
     * @return Set of UplinkConnectionType, never null but possibly empty
     */
    public Set<UplinkConnectionType> getAllowedUplinkConnectionTypes(final boolean forSse) {
        final List<String> temp = getListProperty(forSse ? SSE_ALLOWED_UL_CONNECTIONS_PROPERTY : FSW_ALLOWED_UL_CONNECTIONS_PROPERTY,
                null, LIST_DELIM);
        return convertUplinkConnectionList(temp,
        		forSse ? SSE_ALLOWED_UL_CONNECTIONS_PROPERTY : FSW_ALLOWED_UL_CONNECTIONS_PROPERTY);
    }

    /**
     * Gets a sorted set of all allowed uplink connection types in the current
     * configuration, as Strings.
     *
     * @param forSse true if for SSE, false if for flight
     *
     * @return Set of String, never null but possibly empty
     */
    public Set<String> getAllowedUplinkConnectionTypesAsStrings(final boolean forSse) {
        final Set<UplinkConnectionType> temp = getAllowedUplinkConnectionTypes(forSse);
        return toSortedStringSet(temp);
    }

    /**
     * Gets a sorted set of allowed uplink connection types in the current
     * configuration, qualified by a specific venue type. Defaults to the
     * general list of connection types if no allowed list is specifically
     * configured for the venue.
     *
     * @param vt
     *            VenueType to get property for
     * @param forSse true if for SSE, false if for flight
     *
     * @return Set of UplinkConnectionType, never null but possibly empty
     */
    public Set<UplinkConnectionType> getAllowedUplinkConnectionTypes(
            final VenueType vt, final boolean forSse) {

        if (vt == null) {
            return getAllowedUplinkConnectionTypes(forSse);
        }
        final List<String> temp = getListProperty((forSse ? SSE_ALLOWED_UL_CONNECTIONS_PROPERTY : FSW_ALLOWED_UL_CONNECTIONS_PROPERTY)
                + "." + vt.toString(), null, LIST_DELIM);
        if (temp.isEmpty()) {
            return getAllowedUplinkConnectionTypes(forSse);
        }
        return convertUplinkConnectionList(temp,
        		forSse ? SSE_ALLOWED_UL_CONNECTIONS_PROPERTY : FSW_ALLOWED_UL_CONNECTIONS_PROPERTY);
    }

    /**
     * Gets a sorted set of allowed raw input types (source formats) in the
     * current configuration.
     *
     * @param forSse true if for SSE, false if for flight
     *
     * @return Set of RawInputType, never empty or null
     */
    public Set<TelemetryInputType> getAllowedDownlinkSourceFormats(final boolean forSse) {
        final List<String> temp = getListProperty(forSse ? SSE_ALLOWED_SOURCE_FORMATS_PROPERTY : FSW_ALLOWED_SOURCE_FORMATS_PROPERTY,
                DEFAULT_INPUT_TYPE.toString(), LIST_DELIM);
        return convertSourceFormatList(temp, forSse ?  SSE_ALLOWED_SOURCE_FORMATS_PROPERTY : FSW_ALLOWED_SOURCE_FORMATS_PROPERTY);
    }

    /**
     * Gets a sorted set of allowed raw input types (source formats) in the
     * current configuration, as Strings.
     * 
     * @param forSse true if for SSE, false if for flight
     * 
     * @return Set of String, never empty or null
     */
    public Set<String> getAllowedDownlinkSourceFormatsAsStrings(final boolean forSse) {
        final Set<TelemetryInputType> temp = getAllowedDownlinkSourceFormats(forSse);
        return toSortedStringSet(temp);
    }

    /**
     * Gets a sorted set of allowed raw input types (source formats) for the
     * specified downlink connection type in the current configuration. If no
     * property specific to the supplied connection type is found, the general
     * set of allowed source formats is returned.
     * 
     * @param dct the TelemetryConnnectionType
     * @param forSse true if for SSE, false if for flight
     * 
     * @return Set of RawInputType, never empty or null
     */
    public Set<TelemetryInputType> getAllowedDownlinkSourceFormats(
            final TelemetryConnectionType dct, final boolean forSse) {
        if (dct == null) {
            return getAllowedDownlinkSourceFormats(forSse);
        }
        final List<String> temp = getListProperty((forSse ? SSE_ALLOWED_SOURCE_FORMATS_PROPERTY : FSW_ALLOWED_SOURCE_FORMATS_PROPERTY)
                + "." + dct.toString(), null, LIST_DELIM);
        if (temp.isEmpty()) {
            return getAllowedDownlinkSourceFormats(forSse);
        }
        return convertSourceFormatList(temp, (forSse ? SSE_ALLOWED_SOURCE_FORMATS_PROPERTY : FSW_ALLOWED_SOURCE_FORMATS_PROPERTY)
                + ".");
    }

    /**
     * Gets a sorted set of allowed raw input types (source formats) for the
     * specified downlink connection type in the current configuration, as
     * Strings. If no property specific to the supplied connection type is
     * found, the general set of allowed source formats is returned.
     * 
     * @param dct The TelemetryConnectionType
     * @param forSse true if for SSE, false if for flight
     *      
     * @return Set of String, never empty or null
     */
    public Set<String> getAllowedDownlinkSourceFormatsAsStrings(
            final TelemetryConnectionType dct, final boolean forSse) {
        final Set<TelemetryInputType> temp = getAllowedDownlinkSourceFormats(dct, forSse);
        return toSortedStringSet(temp);
    }

    /**
     * Gets the default raw input type (source format) for the current
     * configuration.
     * 
     * @param forSse true if for SSE, false if for flight
     *      
     * @return RawInputType, never null
     */
    public TelemetryInputType getDefaultSourceFormat(final boolean forSse) {
        final String temp = getProperty(forSse ? SSE_DEFAULT_SOURCE_FORMAT_PROPERTY : FSW_DEFAULT_SOURCE_FORMAT_PROPERTY,
                DEFAULT_INPUT_TYPE.toString());
        try {
            return TelemetryInputType.valueOf(temp.trim().toUpperCase());
        } catch (final IllegalArgumentException e) {
            reportError(forSse ? SSE_DEFAULT_SOURCE_FORMAT_PROPERTY : FSW_DEFAULT_SOURCE_FORMAT_PROPERTY, temp,
                    DEFAULT_INPUT_TYPE.toString());
            return DEFAULT_INPUT_TYPE;
        }
    }

    /**
     * Gets the default raw input type (source format) for specified venue in
     * the current configuration. If there is no such property, returns the
     * general default.
     * 
     * @param vt the VenueType     
     * @param forSse true if for SSE, false if for flight
     * 
     * @return RawInputType, never null
     */
    public TelemetryInputType getDefaultSourceFormat(final VenueType vt, final boolean forSse) {

        if (vt == null) {
            return getDefaultSourceFormat(forSse);
        }
        final String temp = getProperty(
        		(forSse ? SSE_DEFAULT_SOURCE_FORMAT_PROPERTY : FSW_DEFAULT_SOURCE_FORMAT_PROPERTY) + "." + vt.toString(), null);
        if (temp == null) {
            return getDefaultSourceFormat(forSse);
        }
        try {
            return TelemetryInputType.valueOf(temp.trim().toUpperCase());
        } catch (final IllegalArgumentException e) {
            final TelemetryInputType utc = getDefaultSourceFormat(forSse);
            reportError((forSse ? SSE_DEFAULT_SOURCE_FORMAT_PROPERTY : FSW_DEFAULT_SOURCE_FORMAT_PROPERTY) + "," + vt.toString(),
                    temp, utc.toString());
            return utc;
        }
    }

    /**
     * Converts list of strings to a sorted set of raw input types.
     * 
     * @param toConvert
     *            list of Strings to convert
     * @param propertyName
     *            property name the conversion is for
     * @return Set of RawInputType
     */
    private Set<TelemetryInputType> convertSourceFormatList(final List<String> toConvert,
            final String propertyName) {
        final Set<TelemetryInputType> result = new TreeSet<>(
                new AsStringComparator<>());
        for (final String vtStr : toConvert) {
            try {
            	//dynamic types that are not initialized yet
	            //note: they will be updated when the adaptation bootstrap will be loaded
            	if(!TelemetryInputType.valuesAsString().contains(vtStr)){
            		new TelemetryInputType(vtStr, TelemetryInputType.values().length,
		                                   false, false, false, false);
	            }
                final TelemetryInputType dct = TelemetryInputType.valueOf(vtStr.trim().toUpperCase());
                result.add(dct);
            } catch (final IllegalArgumentException e) {
                reportError(propertyName, vtStr, null);
                log.error("Value will be omitted from the configured list");
            }
        }
        return result;

    }
    
	/**
	 * Gets the value of a host name property. Will convert any localhost
	 * value to actual host name. Will map the host name to IP if it is
	 * a logical host name.
	 * 
	 * @param propertyName name of the host property to fetch
	 * @param defValue default value; may be null 
	 * @return host name
	 */
	private String getHostProperty(final String propertyName, final String defValue) {
		String val = getProperty(propertyName, defValue);

		if (val != null && HostPortUtility.LOCALHOST.equalsIgnoreCase(val)) {
			val = HostPortUtility.getLocalHostName();
		}

		if (val != null) {
			return mapLogicalHost(val);
		} else {
			return null;
		}
	}
	

    /**
     * Converts list of strings to a sorted set of downlink connection types.
     * 
     * @param toConvert
     *            list of Strings to convert
     * @param propertyName
     *            property name the conversion is for
     * @return Set of DownlinkConnectionType
     */
    private Set<TelemetryConnectionType> convertDownlinkConnectionList(
            final List<String> toConvert, final String propertyName) {
        final SortedSet<TelemetryConnectionType> result = new TreeSet<TelemetryConnectionType>(
                new AsStringComparator<TelemetryConnectionType>());
        for (final String vtStr : toConvert) {
            try {
                final TelemetryConnectionType dct = TelemetryConnectionType
                        .valueOf(vtStr.trim().toUpperCase());
                result.add(dct);
            } catch (final IllegalArgumentException e) {
                reportError(propertyName, vtStr, null);
                log.error("Value will be omitted from the configured list");
            }
        }
        return result;

    }

    /**
     * Converts list of strings to a sorted set of uplink connection types.
     * 
     * @param toConvert
     *            list of Strings to convert
     * @param propertyName
     *            property name the conversion is for
     * @return Set of UplinkConnectionType
     */
    private Set<UplinkConnectionType> convertUplinkConnectionList(
            final List<String> toConvert, final String propertyName) {
        final SortedSet<UplinkConnectionType> result = new TreeSet<UplinkConnectionType>(
                new AsStringComparator<UplinkConnectionType>());
        for (final String vtStr : toConvert) {
            try {
                final UplinkConnectionType dct = UplinkConnectionType.valueOf(vtStr.trim().toUpperCase());
                result.add(dct);
            } catch (final IllegalArgumentException e) {
                reportError(propertyName, vtStr, null);
                log.error("Value will be omitted from the configured list");
            }
        }
        return result;

    }

    public TelemetryInputType getSseOverrideTelemetryInputType(final VenueType venue,
                                                               final TelemetryConnectionType telemetryConnectionType,
                                                               final TelemetryInputType originalTelemetryInputType) throws org.apache.commons.cli.ParseException {

	    final Set<TelemetryInputType> allFormats = getAllowedDownlinkSourceFormats(true);

	    if (telemetryConnectionType != null &&
			    !allFormats.contains(originalTelemetryInputType)) {
		    return getDefaultSourceFormat(venue, true);
	    }
	    return originalTelemetryInputType;

    }

    public TelemetryConnectionType getSseOverrideTelemetryConnectionType(VenueType venue,
                                                                         TelemetryConnectionType originalTelemetryConnectionType)  throws
		    org.apache.commons.cli.ParseException {

	    final Set<TelemetryConnectionType> dctsForMission = getAllowedDownlinkConnectionTypes(true);
	    /*
	     * Get the current SSE connection type and the SSE default connection type. Determine
	     * if the current SSE connection type is one of the valid SSE connection types
	     * for the mission. If there is no default, throw out.
	     */

	    final TelemetryConnectionType      defdct         = getDefaultDownlinkConnectionType(venue, true);


	    /*
	     * Determine if the current SSE connection type setting is valid for SSE in an integrated
	     * configuration. In this configuration, we cannot support an SSE connection type that
	     * requires an input file or input database session key and host.  The mission space
	     * emulator and NEN/SN connections, should also never be supported by the SSE.
	     */
	    boolean dctOk = true;

	    if ((defdct == null) || ! dctsForMission.contains(defdct))
	    {
		    throw new org.apache.commons.cli.ParseException("Default SSE downlink connection type " + defdct
				                                                    + " is not configured for this mission");
	    }
	    if (originalTelemetryConnectionType != null)
	    {

		    switch (originalTelemetryConnectionType)
		    {
			    case FILE:
			    case DATABASE:
			    case TDS:
			    case NEN_SN_SERVER:
			    case NEN_SN_CLIENT:
				    // Always switch these

				    dctOk = false;
				    break;

			    case CLIENT_SOCKET:
			    case SERVER_SOCKET:
				    // switch these only if invalid
				    break;

			    case UNKNOWN:
			    default:
				    throw new org.apache.commons.cli.ParseException("SSE downlink connection type is UNKNOWN");
		    }

	    }
	    if (!dctOk) {
		    return defdct;
	    } else {
		    return originalTelemetryConnectionType;
	    }
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    } 
}
