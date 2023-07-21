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
 * UnsignedChannelValue represents a channel value instance for a channel that
 * takes an unsigned integral value for data number.
 * 
 */
public class UnsignedChannelValue extends ServiceChannelValue
{

    /**
     * Creates an instance of UnsignedChannelValue with a value of 0.
     */
    public UnsignedChannelValue() {
    	super();
    }
    
    /**
     * Creates an instance of UnsignedChannelValue with the given byte as the
     * data number.
     * @param v the data number
     */
    public UnsignedChannelValue(final byte v)
    {
        this();
        if (v < 0) {
            throw new InvalidChannelValueException(v + " is not a valid value for an Unsigned Channel");
        }
        dn = Byte.valueOf(v);
    }
    
    /**
     * Creates an instance of UnsignedChannelValue with the given short as the
     * data number.
     * @param v the data number
     */
    public UnsignedChannelValue(final short v)
    {
        this();
        if (v < 0) {
            throw new InvalidChannelValueException(v + " is not a valid value for an Unsigned Channel");
        }
        dn = Short.valueOf(v);
    }

    /**
     * Creates an instance of UnsignedChannelValue with the given integer as the
     * data number.
     * @param v the data number
     */
    public UnsignedChannelValue(final int v)
    {
        this();
        if (v < 0) {
            throw new InvalidChannelValueException(v + " is not a valid value for an Unsigned Channel");
        }
        dn = Integer.valueOf(v);
    }

   
    /**
     * Creates an instance of UnsignedChannelValue with the given long as the
     * data number.
     * @param l the data number
     * @throws InvalidChannelValueException if an unisgned channel value cannot be created for the
     * input value.
     */
    public UnsignedChannelValue(final long l) throws InvalidChannelValueException
    {
        this();
        if (l < 0) {
            throw new InvalidChannelValueException(l + " is not a valid value for an Unsigned Channel");
        }
        if (l > 0x7fffffffffffffffL) {
            throw new InvalidChannelValueException(l + " is too large to represent as an unsigned value");
        }
        dn = Long.valueOf(l);
    }
   
    /**
     * Creates an instance of UnsignedChannelValue with the given Number as the
     * data number.
     * @param n the data number
     * @throws InvalidChannelValueException if an unsigned channel value cannot be created for the
     * input value.
     */
    public UnsignedChannelValue(final Number n) throws InvalidChannelValueException
    {
        if (n instanceof Integer) {
            if (n.intValue() < 0) {
                throw new InvalidChannelValueException(n.intValue() + " is not a valid value for an Unsigned Channel");
            }
        } else if (n instanceof Byte) {
            if (n.byteValue() < 0) {
                throw new InvalidChannelValueException(n.byteValue() + " is not a valid value for an Unsigned Channel");
            }
        } else if (n instanceof Short) {
            if (n.shortValue() < 0) {
                throw new InvalidChannelValueException(n.shortValue() + " is not a valid value for an Unsigned Channel");
            }
        } else if (n instanceof Long) {
            if (n.longValue() < 0) {
                throw new InvalidChannelValueException(n.longValue() + " is not a valid value for an Unsigned Channel");
            }
            if (n.longValue() > 0x7fffffffffffffffL) {
                throw new InvalidChannelValueException(n.longValue() + " is too large to represent as an unsigned value");
            }
        } else {
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
