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
 * Class Quintuplet.
 *
 * Holds immutable quintuplets of objects.
 *
 * @param <T> First element type
 * @param <U> Second element type
 * @param <V> Third element type
 * @param <W> Fourth element type
 * @param <X> Fifth element type
 *
 */
public class Quintuplet<T, U, V, W, X> extends Quadruplet<T, U, V, W>
{
    private static final long serialVersionUID = 0L;

    private final X _five;


    /**
     * Constructor.
     *
     * @param one   First of quintuplet
     * @param two   Second of quintuplet
     * @param three Third of quintuplet
     * @param four  Fourth of quintuplet
     * @param five  Fifth of quintuplet
     */
    public Quintuplet(final T one,
                      final U two,
                      final V three,
                      final W four,
                      final X five)
    {
        super(one, two, three, four);

        _five = five;
    }


    /**
     * Constructor.
     *
     * @param other The quintuplet to be copied
     */
    public Quintuplet(final Quintuplet<? extends T, ? extends U,
                                       ? extends V, ? extends W,
                                       ? extends X> other)
    {
        super(other);

        _five = other._five;
    }


    /**
     * Constructor.
     */
    public Quintuplet()
    {
        this(null, null, null, null, null);
    }


    /**
     * Get fifth value.
     *
     * @return The fifth value of the quintuplet
     */
    public X getFive()
    {
        return _five;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Quintuplet<T, U, V, W, X> clone()
    {
        // Cast is required, so warning is ignored

        return (Quintuplet<T, U, V, W, X>) super.clone();
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

        final Quintuplet<?, ?, ?, ?, ?> quintuplet =
            (Quintuplet<?, ?, ?, ?, ?>) other;

        if (_five == null)
        {
            if (quintuplet._five != null)
            {
                return false;
            }
        }
        else if (! _five.equals(quintuplet._five))
        {
            return false;
        }

        return super.equals(quintuplet);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode() + ((_five != null) ? _five.hashCode() : 0);
    }
}
