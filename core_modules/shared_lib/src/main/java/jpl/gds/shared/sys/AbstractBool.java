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

import java.io.InvalidClassException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jpl.gds.shared.annotation.AlwaysThrows;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.string.StringUtil;


/**
 * Class AbstractBool. Provides independent classes similar to Boolean, but
 * mutually incompatible. We accept null in all of the valueOf methods as a
 * false. Immutable.
 *
 * Each subclass has its own versions of true and false. The names can be
 * YES and NO instead of TRUE and FALSE, for instance. The string forms are
 * also set by the subclasses.
 *
 * All methods are final.
 *
 * The constructors are purposely not made available, you must go through the
 * valueOf methods. That is because there are only two values for each
 * instance. You can use == to compare them. That is not safe with Boolean.
 *
 * The subclasses must pass up their true and false instances and their names.
 *
 * AbstractBool is serializable but when reawakened must call the apropriate
 * valueOf to replace with one of the cached values.
 *
 * PMD warning is bogus.
 *
 *
 * @param <T> One of our subclasses
 */
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
abstract public class AbstractBool<T extends AbstractBool<?>>
    extends Object implements Comparable<T>, Serializable
{
    private static final long serialVersionUID = 0L;

    private static final String DEFAULT_TRUE_NAME  = "true";
    private static final String DEFAULT_FALSE_NAME = "false";
    private static final String RESOLVE_METHOD     = "valueOf";

    private final boolean  _value;
    private final String   _name;
    private final Class<T> _tClass =
        SystemUtilities.<Class<T>>castNoWarning(getClass());


    /**
     * Protected constructor.
     *
     * @param value Value
     * @param name  Name for this value
     */
    protected AbstractBool(final boolean value,
                           final String  name)
    {
        super();

        _value = value;

        String temp = StringUtil.emptyAsNull(name);

        if (temp == null)
        {
            temp = (_value ? DEFAULT_TRUE_NAME : DEFAULT_FALSE_NAME);
        }

        _name = temp;
    }


    /**
     * Private nullary constructor for deserialization.
     */
    private AbstractBool()
    {
        this(false, DEFAULT_FALSE_NAME);
    }


    /**
     * Return value as a boolean.
     *
     * @return Value
     */
    public final boolean booleanValue()
    {
        return _value;
    }


    /**
     * Return value as a boolean.
     *
     * @return Value
     */
    public final boolean get()
    {
        return _value;
    }


    /**
     * Return value as a Boolean.
     *
     * @return Value
     */
    public final Boolean getAsBoolean()
    {
        return (_value ? Boolean.TRUE : Boolean.FALSE);
    }

    /** Do not need a boolean overload because it will box to Boolean
     *  and that is very fast because the values are cached.
     */

    /**
     * Compute value from a Boolean.
     *
     * @param value  Boolean value
     * @param aTrue  True object from the subclass
     * @param aFalse False object from the subclass
     *
     * @return Value
     *
     * @param <T> Subclass type
     */
    protected static final <T extends AbstractBool<?>> T valueOf(
        final Boolean value,
        final T       aTrue,
        final T       aFalse)
    {
        return (Boolean.TRUE.equals(value) ? aTrue : aFalse);
    }


    /**
     * Compute value from a string. Anything other than the trimmed
     * case-insensitive true value is false.
     *
     * @param value  String value
     * @param aTrue  True object from the subclass
     * @param aFalse False object from the subclass
     *
     * @return Value
     *
     * @param <T> Subclass type
     */
    protected static final <T extends AbstractBool<?>> T valueOf(
        final String value,
        final T      aTrue,
        final T      aFalse)
    {
        final boolean isTrue =
            aTrue.toString().equalsIgnoreCase(StringUtil.safeTrim(value));

        return (isTrue ? aTrue : aFalse);
    }


    /**
     * Compute value from another AbstractBool.
     *
     * @param value  Any kind of AbstractBool
     * @param aTrue  True object from the subclass
     * @param aFalse False object from the subclass
     *
     * @return Value
     *
     * @param <T> Subclass type returned
     * @param <U> Subclass type of parameter
     */
    protected static final <T extends AbstractBool<?>,
                            U extends AbstractBool<?>> T valueOf(
        final U value,
        final T aTrue,
        final T aFalse)
    {
        return (((value != null) && value.get()) ? aTrue : aFalse);
    }


    /**
     * Compute value from a Container. Supported only for Containers holding
     * Boolean, String, or AbstractBool. Note that we have to do it this way
     * because splitting them out gives us multiple methods with the same
     * erasure. Note also that a null Container will throw (as opposed to an
     * empty Container.)
     *
     * It's better to do a get() or getRaw() on the Container and call one of
     * the other valueOf's.
     *
     * @param value  Container value
     * @param aTrue  True object from the subclass
     * @param aFalse False object from the subclass
     *
     * @return Value
     *
     * @param <T> Subclass type
     * @param <U> The type in the Container
     */
    protected static final <T extends AbstractBool<?>, U> T valueOf(
        final Container<U> value,
        final T            aTrue,
        final T            aFalse)
    {
        final U inside = value.getRaw();

        if (inside == null)
        {
            return aFalse;
        }

        try
        {
            final Boolean b = Boolean.class.cast(inside);

            return (Boolean.TRUE.equals(b) ? aTrue : aFalse);
        }
        catch (ClassCastException cce)
        {
            SystemUtilities.doNothing();
        }

        try
        {
            final String s = String.class.cast(inside);

            final boolean isTrue =
                aTrue.toString().equalsIgnoreCase(StringUtil.safeTrim(s));

            return (isTrue ? aTrue : aFalse);
        }
        catch (ClassCastException cce)
        {
            SystemUtilities.doNothing();
        }

        try
        {
            final AbstractBool<?> a = AbstractBool.class.cast(inside);

            return (a.get() ? aTrue : aFalse);
        }
        catch (ClassCastException cce)
        {
            SystemUtilities.doNothing();
        }

        throw new IllegalArgumentException("AbstractBool.valueOf(Container)");
    }


    /**
     * Get hash code. Same values as Boolean.
     *
     * @return Hash code
     */
    @Override
    public final int hashCode()
    {
        return (_value ? 1231 : 1237);
    }


    /**
     * Test for equality.
     *
     * @param o Object to compare against
     *
     * @return True if equal
     */
    @Override
    public final boolean equals(final Object o)
    {
        if (o == this)
        {
            return true;
        }

        if ((o == null) || ! _tClass.equals(o.getClass()))
        {
            return false;
        }

        return (_tClass.cast(o).booleanValue() == _value);
    }


    /**
     * Compare to another. Subclasses are final, so o is of the correct class.
     *
     * @param o The value to compare against
     *
     * @return Result of comparison
     */
    @Override
    public final int compareTo(final T o)
    {
        if (o == this)
        {
            return 0;
        }

        if (o.booleanValue())
        {
            return (_value ? 0 : -1);
        }

        return (_value ? 1 : 0);
    }


    /**
     * Convert to a string.
     *
     * @return String value
     */
    @Override
    public final String toString()
    {
        return _name;
    }


    /**
     * This is here just in case a subclass tries to be cloneable.
     *
     * Made protected final so subclasses may not override.
     *
     * @return Object Cloned object
     *
     * @throws CloneNotSupportedException Always
     */
    @Override
    @AlwaysThrows
    protected final Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("AbstractBool.clone");
    }


    /**
     * Deserialization has created a new copy; but we must have one of the two
     * cached copies, so we call the appropriate method to get it.
     *
     * Made protected final so subclasses may not override and also so that
     * subclasses have it available.
     *
     * @return Equivalent object from bottom-level class
     *
     * @throws ObjectStreamException On any error
     */
    protected final Object readResolve() throws ObjectStreamException
    {
        Method method = null;
        Object result = null;

        // We will call the static valueOf for class T that takes a
        // Boolean.

        try
        {
            method = _tClass.getMethod(RESOLVE_METHOD, Boolean.class);
        }
        catch (NoSuchMethodException nsme)
        {
            ExceptionTools.addCauseAndThrow(
                new InvalidClassException(
                        "AbstractBool.readResolve could not find "+
                        RESOLVE_METHOD                            +
                        " method"),
                nsme);
        }

        if (method == null)
        {
            // Not necessary because addCauseAndThrow will always throw
            // but it prevents PMD from complaining about a dereference.

            return null;
        }

        try
        {
            result = method.invoke(null, Boolean.valueOf(_value));
        }
        catch (IllegalAccessException iae)
        {
            ExceptionTools.addCauseAndThrow(
                new InvalidClassException(
                        "AbstractBool.readResolve could not invoke " +
                        RESOLVE_METHOD                               +
                        " method, access denied"),
                iae);
        }
        catch (InvocationTargetException ite)
        {
            final Throwable real = ite.getCause();

            ExceptionTools.addCauseAndThrow(
                new InvalidClassException(
                        "AbstractBool.readResolve could not invoke " +
                        RESOLVE_METHOD                               +
                        " method: "                                  +
                        real.getLocalizedMessage()),
                real);
        }

        return result;
    }
}
