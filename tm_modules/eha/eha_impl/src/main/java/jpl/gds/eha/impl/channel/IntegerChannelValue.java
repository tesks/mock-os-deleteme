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
/*
 * Title: IntegerChannelValue.java
 * 
 * Author: dan
 * Created: Nov 9, 2005
 * 
 */
package jpl.gds.eha.impl.channel;

import jpl.gds.eha.channel.api.InvalidChannelValueException;

/**
 * IntegerChannelValue represents a channel value instance for a channel that
 * has a signed integral data number.
 */
public class IntegerChannelValue extends ServiceChannelValue
{
    /**
     * Creates an instance of IntegerChannelValue with a value of 0.
     */
    public IntegerChannelValue() {
    	super();
    }
    
    /**
     * Creates an instance of IntegerChannelValue using the given byte value
     * as the data number.
     * @param v the data number
     */
    public IntegerChannelValue(final byte v)
    {
        this();
        dn = Byte.valueOf(v);
    }

    /**
     * Creates an instance of IntegerChannelValue using the given short value
     * as the data number.
     * @param v the data number
     */
    public IntegerChannelValue(final short v)
    {
        this();
        dn = Short.valueOf(v);
    }

    /**
     * Creates an instance of IntegerChannelValue using the given int value
     * as the data number.
     * @param v the data number
     */
    public IntegerChannelValue(final int v)
    {       
        this();
        dn = Integer.valueOf(v);
    }

    /**
     * Creates an instance of IntegerChannelValue using the given long value
     * as the data number.
     * @param v the data number
     */
    public IntegerChannelValue(final long v)
    {
        this();
        dn = Long.valueOf(v);
    }

    /**
     * Creates an instance of IntegerChannelValue using the given Number value
     * as the data number.
     * @param n the data number
     * 
     * @throws InvalidChannelValueException if the input value cannot be cast to an integer type.
     */
    public IntegerChannelValue(final Number n) {
        if (!(n instanceof Integer || n instanceof Byte || n instanceof Short || n instanceof Long)) {
             throw new InvalidChannelValueException("cannot create channel value of type " +
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
            setDn(Long.valueOf(0));
        } else {
            setDn(Long.parseLong(value));
        }
    }
}
