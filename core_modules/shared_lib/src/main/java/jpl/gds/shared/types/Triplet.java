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



/**
 * Class Triplet.
 *
 * Holds immutable triplets of objects of arbitrary type.
 *
 * @param <T> First element type
 * @param <U> Second element type
 * @param <V> Third element type
 *
 */
public class Triplet<T, U, V> extends Pair<T, U>
{
    private static final long serialVersionUID = 0L;

    private final V _three;


    /**
     * Constructor.
     *
     * @param one   First of triplet
     * @param two   Second of triplet
     * @param three Third of triplet
     */
    public Triplet(final T one,
                   final U two,
                   final V three)
    {
        super(one, two);

        _three = three;
    }


    /**
     * Constructor.
     *
     * @param other The triplet to be copied
     */
    public Triplet(final Triplet<? extends T, ? extends U, ? extends V> other)
    {
        super(other);

        _three = other._three;
    }


    /**
     * Constructor.
     */
    public Triplet()
    {
        this(null, null, null);
    }


    /**
     * Get third element.
     *
     * @return The third value of the triplet
     */
    public V getThree()
    {
        return _three;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Triplet<T, U, V> clone()
    {
        // Cast is required, so warning is ignored

        return (Triplet<T, U, V>) super.clone();
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

        final Triplet<?, ?, ?> triplet = (Triplet<?, ?, ?>) other;

        if (_three == null)
        {
            if (triplet._three != null)
            {
                return false;
            }
        }
        else if (! _three.equals(triplet._three))
        {
            return false;
        }

        return super.equals(triplet);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode() + ((_three != null) ? _three.hashCode() : 0);
    }
}
