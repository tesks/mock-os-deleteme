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
package jpl.gds.shared.types;

import java.io.Serializable;


/**
 * Class Pair.
 *
 * Holds mutable pairs of objects of any type.
 *
 * @param <T> First element type
 * @param <U> Second element type
 *
 */
public class Pair<T, U> extends Object implements Cloneable, Serializable
{
    private static final long serialVersionUID = 0L;

    private T _one;
    private U _two;


    /**
     * Constructor.
     *
     * @param one First of pair
     * @param two Second of pair
     */
    public Pair(final T one,
                final U two)
    {
        super();

        _one = one;
        _two = two;
    }


    /**
     * Constructor.
     *
     * @param other Pair to copy
     */
    public Pair(final Pair<? extends T, ? extends U> other)
    {
        this(other._one, other._two);
    }


    /**
     * Constructor.
     */
    public Pair()
    {
        this(null, null);
    }


    /**
     * Get first value.
     *
     * @return The value of the first of the pair of values
     */
    public T getOne()
    {
        return _one;
    }


    /**
     * Get second value.
     *
     * @return The value of the second of the pair of values
     */
    public U getTwo()
    {
        return _two;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Pair<T, U> clone()
    {
        try
        {
            // Cast is required, so warning is ignored

            return (Pair<T, U>) super.clone();
        }
        catch (CloneNotSupportedException cnse)
        {
            throw new RuntimeException("Unexpected clone failure", cnse);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other)
        {
            return true;
        }

        if ((other == null) || (getClass() != other.getClass()))
        {
            return false;
        }

        final Pair<?, ?> pair = (Pair<?, ?>) other;

        if (_one == null)
        {
            if (pair._one != null)
            {
                return false;
            }
        }
        else if (! _one.equals(pair._one))
        {
            return false;
        }

        if (_two == null)
        {
            if (pair._two != null)
            {
                return false;
            }
        }
        else if (! _two.equals(pair._two))
        {
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return ((_one != null) ? _one.hashCode() : 0) +
               ((_two != null) ? _two.hashCode() : 0);
    }


    /**
     * Set first value.
     *
     * @param data Value
     */
    public void setOne(final T data)
    {	
    	_one = data;
    }


    /**
     * Set second value.
     *
     * @param data Value
     */
    public void setTwo(final U data)
    {	
    	_two = data;
    }
}
