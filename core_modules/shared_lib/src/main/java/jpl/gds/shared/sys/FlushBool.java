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
package jpl.gds.shared.sys;


/**
 * Class FlushBool. See AbstractBool. Immutable.
 * This class serves as a template for all subclasses of AbstractBool.
 *
 * It is serializable. Do NOT expose constructors.
 *
 * You can add some functionality here, but if it is not specific to your
 * subclass it should go in AbstractBool for everyone to use.
 *
 */
public final class FlushBool
    extends AbstractBool<FlushBool> implements Comparable<FlushBool>
{
    private static final long serialVersionUID = 0L;

    /** True of this type */
    public static final FlushBool YES = new FlushBool(true, "yes");

    /** False of this type */
    public static final FlushBool NO = new FlushBool(false, "no");


    /**
     * Private constructor. Used ONLY to construct the two static values.
     *
     * @param value Value
     * @param name  Name of value
     */
    private FlushBool(final boolean value,
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
    public static FlushBool valueOf(final Boolean value)
    {
        return valueOf(value, YES, NO);
    }


    /**
     * Construct value from a string.
     *
     * @param value String value
     *
     * @return Value
     */
    public static FlushBool valueOf(final String value)
    {
        return valueOf(value, YES, NO);
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
    public static <T extends AbstractBool<?>> FlushBool valueOf(
        final AbstractBool<T> value)
    {
        return valueOf(value, YES, NO);
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
    public static <T> FlushBool valueOf(final Container<T> value)
    {
        return valueOf(value, YES, NO);
    }
}
