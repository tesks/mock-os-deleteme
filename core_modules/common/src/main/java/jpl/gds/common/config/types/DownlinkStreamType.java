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



/**
 * Enumeration of downlink stream IDs allowed in a session
 * configuration. 
 *
 */
public enum DownlinkStreamType
    implements ConvertableEnum
{
    /** TZ */
    TZ(),
    /** LV */
    LV(),
    /** SA */
    SA(),
    /** Command Echo */
    COMMAND_ECHO("Command Echo"),
    /** Selected DL */
    SELECTED_DL("Selected DL"),
    /** Not applicable */
    NOT_APPLICABLE("Not applicable");


    private final String _output;


    /**
     * Constructor with no special output value.
     */
    private DownlinkStreamType()
    {
        _output = null;
    }


    /**
     * Constructor with special output value.
     *
     * @param output
     */
    private DownlinkStreamType(final String output)
    {
        _output = output;
    }


    /**
     * Convert from external representation to enum constant, as necessary.
     * Convert blanks to underscores.
     *
     * May throw IllegalArgumentException.
     * Throw on null or any bad value.
     *
     * @param value Value to convert
     *
     * @return Converted value
     */
    public static DownlinkStreamType convert(final String value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(
                          "DownlinkStreamType cannot be null");
        }

        final String s = value.trim().toUpperCase().replace(' ', '_');

        // All bad values throw IllegalArgumentException

        return valueOf(s);
    }


    /**
     * Convert from enum constant to external representation, as necessary.
     *
     * @return Converted value
     */
    public String convert()
    {
        return ((_output != null) ? _output : name());
    }


    /**
     * Convert from enum constant to external representation, as necessary.
     *
     * @param value Enum to convert
     *
     * @return Converted value
     */
    public static String convert(final DownlinkStreamType value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(
                          "DownlinkStreamType cannot be null");
        }

        return value.convert();
    }
}
