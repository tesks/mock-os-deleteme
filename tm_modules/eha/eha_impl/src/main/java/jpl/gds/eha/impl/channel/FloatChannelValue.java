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
package jpl.gds.eha.impl.channel;

import jpl.gds.eha.channel.api.InvalidChannelValueException;

/**
 * FloatChannelValue represents a channel value instance for a channel which has
 * a floating point data number.
 * 
 *
 */
public class FloatChannelValue extends ServiceChannelValue
{
    /**
     * Creates an instance of FloatChannelValue with a value of 0.0.
     */
    public FloatChannelValue() {
    	super();
    }
    
    /**
     * Creates an instance of FloatChannelValue with the given float value.
     * @param v the data number
     */
    public FloatChannelValue(final float v)
    {
        this();
        dn = Float.valueOf(v);
    }

    /**
     * Creates an instance of FloatChannelValue with the given double value.
     * @param v the data number
     */
    public FloatChannelValue(final double v)
    {
        this();        
        dn = Double.valueOf(v);
    }
    
    /**
     * Creates an instance of FloatChannelValue with the given Number object as 
     * value.
     *
     * @param n the data number
     * 
     * @throws InvalidChannelValueException if the input value cannot be cast to an integer type.
     */
    public FloatChannelValue(final Number n) throws InvalidChannelValueException {
        this();
        if (!(n instanceof Float || n instanceof Double)) {
            throw new InvalidChannelValueException("Cannot create floating point channel value from type " +
                    n.getClass().getName());
        }
        dn = n;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.eha.impl.channel.ServiceChannelValue#setDnFromString(java.lang.String, int)
     */
    @Override
    public void setDnFromString(final String value, final int componentSize) {
        if (value == null) {
            setDn(Double.valueOf(0));
        } else {
            setDn(Double.parseDouble(value));
        }
    }
}
