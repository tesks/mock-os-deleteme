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
 * This boolean class is used to specify whether or not SSE downlink is allowed
 * in the present context.
 *
 */
public final class AllowSseDownlinkBool
    extends AbstractBool<AllowSseDownlinkBool>
    implements Comparable<AllowSseDownlinkBool>
{
    private static final long serialVersionUID = 0L;

    /** True of this type */
    public static final AllowSseDownlinkBool ALLOW_SSE =
        new AllowSseDownlinkBool(true, "allow");

    /** False of this type */
    public static final AllowSseDownlinkBool DISALLOW_SSE =
        new AllowSseDownlinkBool(false, "disallow");


    /**
     * Private constructor. Used ONLY to construct the two static values.
     *
     * @param value Value
     * @param name  Name of value
     */
    private AllowSseDownlinkBool(final boolean value,
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
    public static AllowSseDownlinkBool valueOf(final Boolean value)
    {
        return valueOf(value, ALLOW_SSE, DISALLOW_SSE);
    }


    /**
     * Construct value from a string.
     *
     * @param value String value
     *
     * @return Value
     */
    public static AllowSseDownlinkBool valueOf(final String value)
    {
        return valueOf(value, ALLOW_SSE, DISALLOW_SSE);
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
    public static <T extends AbstractBool<?>> AllowSseDownlinkBool valueOf(
        final AbstractBool<T> value)
    {
        return valueOf(value, ALLOW_SSE, DISALLOW_SSE);
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
    public static <T> AllowSseDownlinkBool valueOf(final Container<T> value)
    {
        return valueOf(value, ALLOW_SSE, DISALLOW_SSE);
    }
}
