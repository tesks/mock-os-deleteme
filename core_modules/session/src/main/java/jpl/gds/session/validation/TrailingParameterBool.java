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

import jpl.gds.shared.sys.AbstractBool;
import jpl.gds.shared.sys.Container;


/**
 * This boolean class is used to specify whether a command-line option is to
 * be a regular parameter (with an option name) or a nameless value that is
 * supplied at the end of the command-line after the regular parameters.
 *
 */
public final class TrailingParameterBool
    extends AbstractBool<TrailingParameterBool>
    implements Comparable<TrailingParameterBool>
{
    private static final long serialVersionUID = 0L;

    /** True of this type */
    public static final TrailingParameterBool TRAILING =
        new TrailingParameterBool(true, "trailing");

    /** False of this type */
    public static final TrailingParameterBool OPTION =
        new TrailingParameterBool(false, "option");


    /**
     * Private constructor. Used ONLY to construct the two static values.
     *
     * @param value Value
     * @param name  Name of value
     */
    private TrailingParameterBool(final boolean value,
                                  final String  name)
    {
        super(value, name);
    }


    /**
     * Construct value from a Boolean. boolean will box and use this one.
     *
     * @param value Boolean value
     *
     * @return Value
     */
    public static TrailingParameterBool valueOf(final Boolean value)
    {
        return valueOf(value, TRAILING, OPTION);
    }


    /**
     * Construct value from a string.
     *
     * @param value String value
     *
     * @return Value
     */
    public static TrailingParameterBool valueOf(final String value)
    {
        return valueOf(value, TRAILING, OPTION);
    }


    /**
     * Construct value from another AbstractBool.
     *
     * @param value AbstractBool value
     *
     * @return Value
     *
     * @param <T> Subtype
     */
    public static <T extends AbstractBool<?>> TrailingParameterBool valueOf(
        final AbstractBool<T> value)
    {
        return valueOf(value, TRAILING, OPTION);
    }


    /**
     * Construct value from a Container.
     *
     * @param value Container value
     *
     * @return Value
     *
     * @param <T> Type of Container
     */
    public static <T> TrailingParameterBool valueOf(final Container<T> value)
    {
        return valueOf(value, TRAILING, OPTION);
    }
}
