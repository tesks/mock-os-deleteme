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
 * Class Quadruplet.
 *
 * Holds immutable quadruplets of objects of arbitrary type.
 *
 * @param <T> First element type
 * @param <U> Second element type
 * @param <V> Third element type
 * @param <W> Fourth element type
 *
 */
public class Quadruplet<T, U, V, W> extends Triplet<T, U, V>
{
    private static final long serialVersionUID = 0L;

    private final W _four;


    /**
     * Constructor.
     *
     * @param one   First of quadruplet
     * @param two   Second of quadruplet
     * @param three Third of quadruplet
     * @param four  Fourth of quadruplet
     */
    public Quadruplet(final T one,
                      final U two,
                      final V three,
                      final W four)
    {
        super(one, two, three);

        _four = four;
    }


    /**
     * Constructor.
     *
     * @param other The quadruplet to be copied
     */
    public Quadruplet(final Quadruplet<? extends T, ? extends U,
                                       ? extends V, ? extends W> other)
    {
        super(other);

        _four = other._four;
    }


    /**
     * Constructor.
     */
    public Quadruplet()
    {
        this(null, null, null, null);
    }


    /**
     * Get fourth value.
     *
     * @return The fourth value of the quadruplet
     */
    public W getFour()
    {
        return _four;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Quadruplet<T, U, V, W> clone()
    {
        // Cast is required, so warning is ignored

        return (Quadruplet<T, U, V, W>) super.clone();
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

        final Quadruplet<?, ?, ?, ?> quadruplet =
            (Quadruplet<?, ?, ?, ?>) other;

        if (_four == null)
        {
            if (quadruplet._four != null)
            {
                return false;
            }
        }
        else if (! _four.equals(quadruplet._four))
        {
            return false;
        }

        return super.equals(quadruplet);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode() + ((_four != null) ? _four.hashCode() : 0);
    }
}
