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

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.shared.types.AsStringComparator;


/**
 * Immutable class to hold a session parameter and its associated data.
 *
 *
 * @param <T> Class of session parameter
 * 
 */
public class SessionParameter<T> extends Object
{
    private final T       _value;
    private final boolean _readOnly;
    private final boolean _defaulted;
    private final Set<T>  _values;


    /**
     * Constructor.
     *
     * @param value     Value of session parameter
     * @param readOnly  True if read-only
     * @param defaulted True if defaulted
     * @param values    Set of parameter values for mission
     */
    public SessionParameter(final T                value,
                            final boolean          readOnly,
                            final boolean          defaulted,
                            final Set<? extends T> values)
    {
        super();

        _value     = value;
        _readOnly  = readOnly;
        _defaulted = defaulted;

        // Values are in alphabetical order

        final Set<T> set =
            new TreeSet<T>(new AsStringComparator<T>());

        set.addAll(values);

        _values = Collections.unmodifiableSet(set);
    }


    /**
     * Get value.
     *
     * @return Value
     */
    public T getValue()
    {
        return _value;
    }


    /**
     * Get read-only state.
     *
     * @return True if read-only
     */
    public boolean getReadOnly()
    {
        return _readOnly;
    }


    /**
     * Get defaulted state.
     *
     * @return True if defaulted
     */
    public boolean getDefaulted()
    {
        return _defaulted;
    }


    /**
     * Get configured mission values.
     *
     * @return Set of values
     */
    public Set<T> getValues()
    {
        return _values;
    }


    /**
     * Convert to string.
     *
     * @return Value as a string
     */
    @Override
    public String toString()
    {
        return getValue().toString();
    }
}
