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

import jpl.gds.shared.string.StringUtil;


/**
 * UplinkConnectionType is an enumeration of the possible interfaces supported
 * for performing commanding.
 *
 *
 */
public enum UplinkConnectionType
{
    /**
     * Command output is via network socket.
     */
    SOCKET,



    /**
     * Command output is via command service.
     */
    COMMAND_SERVICE,

    /**
     * Command output is unidentified.
     */
    UNKNOWN;

    /**
     * Convert string to uplink connection type, return null if error.
     *
     * @param uct String value to convert
     *
     * @return Uplink connection type
     */
    public static UplinkConnectionType safeValueOf(final String uct)
    {
        final String type = StringUtil.safeTrim(uct);

        if (type.isEmpty())
        {
            return null;
        }

        UplinkConnectionType result = null;

        try
        {
            result = valueOf(type);
        }
        catch (IllegalArgumentException iae)
        {
            result = null;
        }

        return result;
    }

}
