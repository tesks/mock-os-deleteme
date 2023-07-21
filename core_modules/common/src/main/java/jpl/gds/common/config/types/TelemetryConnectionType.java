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
package jpl.gds.common.config.types;

import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.string.StringUtil;


/**
 * DownlinkConnectionType is an enumeration of the possible interfaces supported for
 * receiving telemetry.
 *
 */
public enum TelemetryConnectionType
{
	/**
	 * Telemetry source connection reads from a file.
	 */
	FILE,

	/**
	 * Telemetry source connection uses a network socket, in client mode.
	 */
	CLIENT_SOCKET,

	/**
	 * Telemetry source connection uses a network socket, in server mode.
	 */
	SERVER_SOCKET,

	/**
	 * Telemetry source connection uses the NEN/SN API as server.
	 */
	NEN_SN_SERVER,

	/**
	 * Telemetry source connection uses the NEN/SN API as client.
	 */
	NEN_SN_CLIENT,


	/**
	 * Telemetry source is the JPL Deep Space Network Telemetry Data System (TDS).
	 */
    TDS,

	/**
	 * Telemetry source connection reads from the AMPCS database.
	 */
    DATABASE,

	/**
	 * Telemetry source connection is unidentified.
	 */
    UNKNOWN;

    @ToDo("Remove this after upgrading past V4 database")
    private static final String OLD_NEN_SN_SERVER = "NEN_SN";

    /**
     * Convert string to downlink connection type, return null if error.
     *
     * @param dct String value to convert
     *
     * @return  connection type
     */
    @ToDo("Remove old server check after upgrading past V4 database")
    public static TelemetryConnectionType safeValueOf(final String dct)
    {
        final String type = StringUtil.safeTrim(dct);

        if (type.isEmpty())
        {
            return null;
        }

        if (type.equalsIgnoreCase(OLD_NEN_SN_SERVER))
        {
            return NEN_SN_SERVER;
        }

        TelemetryConnectionType result = null;

        try
        {
            result = valueOf(type);
        }
        catch (final IllegalArgumentException iae)
        {
            result = null;
        }

        return result;
    }
    
    /**
     * Indicates whether this connection type requires an input file.
     * @return true if type requires an input file, false if not
     */
    public boolean hasInputFile() {
    	if (this == TDS || this == FILE) {
    		return true;
    	}
    	return false;
    }
    

    /**
     * Indicates whether this connection type supports a downlink stream ID
     * in TESTBED or ATLO venues.
     *
     * @return true if stream ID can be used; false if not
     */
    public boolean usesStreamId()
    {
        switch (this)
        {
            case NEN_SN_SERVER:
            case NEN_SN_CLIENT:
            // the following  are for list of connection types that support Downlink Stream Ids
            case TDS:
            case CLIENT_SOCKET:
    	 	case SERVER_SOCKET:
                return true;

            default:
                break;
        }

        return false;
    }
    
    /**
     * CLIENT_SOCKET and SERVER_SOCKET do not require stream IDs
     * @return true if stream ID required; false if not
     */
    public boolean requiresStreamId() {

        switch (this)
        {
            case NEN_SN_SERVER:
            case NEN_SN_CLIENT:
            case TDS: // Added TDS to the list of connection types that support Downlink Stream Ids
                return true;

            default:
                break;
        }

        return false;
    }
    

    /**
     * Test for any NEN SN.
     *
     * @return true if either NEN SN
     */
    public boolean isNenSn()
    {
        return ((this == NEN_SN_SERVER) ||
                (this == NEN_SN_CLIENT));
    }

    /**
     * Indicates of the connection type uses the network.
     * 
     * @return true if needs network, false if not
     */
    public boolean isNetwork() {
        switch(this) {
        case CLIENT_SOCKET:
        case NEN_SN_CLIENT:
        case NEN_SN_SERVER:
        case SERVER_SOCKET:
        case TDS:
            return true;
        default:
            return false;
        
        }
    }
}
