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
package jpl.gds.db.mysql.impl.sql.store.ldi;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.Pair;


/**
 * Holds the value for a channel value as a type and an object. Object will
 * be Long, Double, or String. The value can be null.
 *
 * There is no special code necessary for unsigned, those values will just be
 * non-negative.
 *
 * For numeric types, him is equal to me when:
 *     Abs(me - him) <= Abs(me * factor) with factor in [0.0, 1.0]
 *
 * For any type, if the factor is negative, equality is forced false.
 *
 */
public class ChannelValue extends Pair<ChannelType, Object>
{
    /** FastTracer that also records to database */
    private static final Tracer _trace           = TraceManager.getTracer(Loggers.TLM_EHA);

	private static final long serialVersionUID = 1L;
	
	// Values in [0.0, 1.0] (or negative) that tell us when a change is "enough"
    private final double _double_factor;


    /**
     * Constructor.
     *
     * @param cte           Channel type
     * @param value         Value as Object
     * @param double_factor When a change is "enough" for floats
     */
    public ChannelValue(final ChannelType cte,
                        final Object      value,
                        final double      double_factor)
    {
        super(cte, value);
        _trace.setPrefix("LDI: ");

        if ((double_factor > 1.0) || Double.isNaN(double_factor))
        {
            throw new IllegalArgumentException(
                          "Double factor must be in [0.0,1.0] or negative");
        }

        _double_factor = double_factor;
    }


    /**
     * Get type.
     *
     * @return ChannelTypeEnum
     */
    public ChannelType getType()
    {
        return getOne();
    }


    /**
     * Get value.
     *
     * @return Object
     */
    public Object getValue()
    {
        return getTwo();
    }


    /**
     * Compare against another channel value.
     *
     * @param cv Channel value to compare against
     *
     * @return boolean
     */
    public boolean isEqual(final ChannelValue cv)
    {
        if (getType() != cv.getType())
        {
        	_trace.error("Inconsistent channel value types");

            return false;
        }

        final Object left  = getValue();
        final Object right = cv.getValue();

        if (left == null)
        {
            return (right == null);
        }

        if (right == null)
        {
            return false;
        }

        if (left.getClass() != right.getClass())
        {
        	_trace.error("Inconsistent channel value object types");

            return false;
        }

        if (left instanceof Double)
        {
            if (_double_factor < 0.0)
            {
                return false;
            }

            final double l = ((Double) left).doubleValue();
            final double r = ((Double) right).doubleValue();

            // Two NaNs are considered equal, otherwise a single NaN
            // is not equal to anything.

            if (Double.isNaN(l))
            {
                return Double.isNaN(r);
            }
            else if (Double.isNaN(r))
            {
                return false;
            }

            // Two infinities are considered equal if they are the same sign,
            // otherwise a single infinity is not equal to anything.

            if (Double.isInfinite(l))
            {
                if (! Double.isInfinite(r))
                {
                    return false;
                }

                // Both infinite

                if (l > 0.0D)
                {
                    return (r > 0.0D);
                }

                return (r < 0.0D);
            }
            else if (Double.isInfinite(r))
            {
                return false;
            }

            return (Math.abs(l - r) <= _double_factor);
        }

        // Not a type we handle specially, regular equals must handle

        return left.equals(right);
    }


    /**
     * Defined to explicitly show that we are not incorporating the new
     * attributes into the logical value.
     *
     * @param other Object to compare against
     *
     * @return boolean
     */
    @Override
	public boolean equals(final Object other)
    {
        return super.equals(other);
    }


    /**
     * Defined to explicitly show that we are not incorporating the new
     * attributes into the logical value.
     *
     * @return int
     */
    @Override
	public int hashCode()
    {
        return super.hashCode();
    }


    /**
     * Get string value. For boolean or status, may be set in addition to
     * other value.
     *
     * @param cte Type of channel
     * @param val Channel value
     *
     * @return String as value or status or null
     */
    public static String computeStringValue(final ChannelType   cte,
                                            final IClientChannelValue val)
    {
        if (cte.equals(ChannelType.ASCII))
        {
            return val.stringValue();
        }

        if (cte.hasEnumeration())
        {
            return val.getStatus();
        }

        return null;
    }
}
