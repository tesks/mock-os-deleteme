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
package jpl.gds.evr.api;

import jpl.gds.common.config.types.ConvertableEnum;

/**
 * Enumeration of keywords corresponding to enum column EvrMetadata.keyword.
 * 
 */
public enum EvrMetadataKeywordEnum
    implements ConvertableEnum
{
    /** UNKNOWN */
    UNKNOWN,
    /** TaskName */
    TASKNAME("TaskName"),
    /** SequenceId */
    SEQUENCEID("SequenceId"),
    /** CategorySequenceId */
    CATEGORYSEQUENCEID("CategorySequenceId"),
    /** AddressStack */
    ADDRESSSTACK("AddressStack"),
    /** Source */
    SOURCE("Source"),
    /** TaskId */
    TASKID("TaskId"),
    /** errno */
    ERRNO("errno");


    private final String _output;


    /**
     * Constructor with no special output value.
     */
    private EvrMetadataKeywordEnum()
    {
        _output = null;
    }


    /**
     * Constructor with special output value.
     *
     * @param output
     */
    private EvrMetadataKeywordEnum(final String output)
    {
        _output = output;
    }


    /**
     * Convert from external representation to enum constant, as necessary.
     *
     * May throw IllegalArgumentException.
     *
     * @param value Keyword to convert
     *
     * @return Converted value
     */
    public static EvrMetadataKeywordEnum convert(final String value)
    {
        if (value == null)
        {
            return UNKNOWN;
        }

        final String s = value.trim().toUpperCase();

        if (s.length() == 0)
        {
            // Covers bad values inserted in DB
            return UNKNOWN;
        }

        // Other bad values throw IllegalArgumentException

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
     * @param value Keyword to convert
     *
     * @return Converted value
     */
    public static String convert(final EvrMetadataKeywordEnum value)
    {
        return ((value != null) ? value : UNKNOWN).convert();
    }
}
