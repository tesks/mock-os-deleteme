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
package jpl.gds.session.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/**
 * Enum for session parameter.
 *
 */
public enum SessionParameterEnum
{
    /** Venue */
    VENUE,

    /** Downlink connection type */
    DOWNLINK_CONNECTION_TYPE,

    /** Uplink connection type */
    UPLINK_CONNECTION_TYPE,

    /** Raw input type */
    RAW_INPUT_TYPE,

    /** Downlink stream id */
    DOWNLINK_STREAM_ID,

    /** Testbed name */
    TESTBED_NAME,

    /** Station */
    STATION,

    /** Session name */
    SESSION_NAME,

    /** Downlink input file */
    DOWNLINK_INPUT_FILE,

    /** Trailing downlink input file */
    TRAILING_DOWNLINK_INPUT_FILE,

    /** JMS subtopic */
    JMS_SUBTOPIC;


    /** Values as a sorted set */
    public static final Set<SessionParameterEnum> PARAMETERS =
        Collections.unmodifiableSet(
            EnumSet.<SessionParameterEnum>copyOf(Arrays.asList(values())));


    /**
     * Get number of elements.
     *
     * @return Size
     */
    public static int size()
    {
        return PARAMETERS.size();
    }
}
