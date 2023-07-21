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

import java.io.File;


/**
 * Holds a parameter value. The value may be null. This class assumes you
 * know the value type. It is not generic because the type of data may change
 * over time. For example, options are first parsed as String and then changed
 * to their desired type later. That would also cause a lot of casting
 * downstream.
 *
 */
public final class Value extends AbstractParameter
{
    private static final String ME = "Value: ";

    private Object  _value     = null;
    private boolean _defaulted = false;


    /**
     * Constructor.
     */
    public Value()
    {
        super();
    }


    /**
     * Setter of any value type.
     *
     * @param value Value
     */
    public void setValue(final Object value)
    {
        _value = value;
    }


    /**
     * Detect null state.
     *
     * @return True if a value is present
     */
    public boolean hasValue()
    {
        return (_value != null);
    }


    /**
     * Getter of any value type. The cast accepts a null.
     *
     * @param <T> Desired type
     * @param clss Class object for T
     *
     * @return Value
     *
     * @throws ParameterException If value cannot be cast to the desired type
     *
     */
    public <T> T getValue(final Class<T> clss) throws ParameterException
    {
        checkForNull(ME, clss, "Class");

        if ((_value != null) && ! clss.isInstance(_value))
        {
            throw new ParameterException(ME                          +
                                         "Unable to cast "           +
                                         _value                      +
                                         " of "                      +
                                         _value.getClass().getName() +
                                         " to "                      +
                                         clss.getName());
        }

        return clss.cast(_value);
    }


    /**
     * Getter for string.
     *
     * @param uppercase Uppercase the value?
     *
     * @return String value
     *
     * @throws ParameterException If cannot get value as String
     */
    public String getValueAsString(final UppercaseBool uppercase)
        throws ParameterException
    {
        final String value = getValue(String.class);

        return (((value != null) && uppercase.get())
                    ? value.toUpperCase()
                    : value);
    }


    /**
     * Set defaulted.
     */
    public void setDefaulted()
    {
        _defaulted = true;
    }


    /**
     * Get defaulted state.
     *
     * @return True if value was defaulted.
     */
    public boolean getDefaulted()
    {
        return _defaulted;
    }


    /**
     * Return defaulted status suitable for display.
     *
     * @return String
     */
    public String defaultedStatus()
    {
        return ((hasValue() && getDefaulted()) ? getDefaultedString() : "");
    }


    /**
     * Return defaulted status suitable for display.
     *
     * @return String
     */
    public static String getDefaultedString()
    {
        return " (defaulted)";
    }


    /**
     * Summarize as a string.
     *
     * @return String
     */
    @Override
    public String toString()
    {
        if (! hasValue())
        {
            return "Unassigned";
        }

        final StringBuilder sb = new StringBuilder();

        if (! getDefaulted())
        {
            sb.append("Set to: ");
        }
        else
        {
            sb.append("Defaulted to: ");
        }

        if (File.class.isInstance(_value))
        {
            sb.append(getAbsolutePath(File.class.cast(_value)));
        }
        else
        {
            sb.append(_value);
        }

        return sb.toString();
    }
}
