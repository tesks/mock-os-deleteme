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

import jpl.gds.shared.gdr.GDR;

/**
 * BooleanChannelValue represents a channel value instance for a channel that
 * has an unsigned data number that is interpreted as true or false.
 * 
 *
 */
public class BooleanChannelValue extends ServiceChannelValue
{
    private static final Byte TRUE  = Byte.valueOf((byte) 1);
    private static final Byte FALSE = Byte.valueOf((byte) 0);


    /**
     * Creates an instance of BooleanChannelValue with a value of false.
     */
    public BooleanChannelValue()
    {
    	super();
    }
    

    /**
     * Creates an instance of BooleanChannelValue with the given boolean value.
     * @param v the data number
     */
    public BooleanChannelValue(final boolean v)
    {
        super();

        dn = (v ? TRUE : FALSE);
    }


    /**
     * Creates an instance of ChannelValue with the value of true if the longValue of the input is
     * non-zero, false if 0.
     *
     * @param v the data number
     */
    public BooleanChannelValue(final Number v)
    {
        this(v.longValue() != 0L);
    }
    

    /**
     * {@inheritDoc}
     * @see jpl.gds.eha.impl.channel.ServiceChannelValue#setDn(java.lang.Object)
     */
    @Override
    public void setDn(final Object n)
    {

        if (n instanceof Boolean) {
            final Boolean b = (Boolean)n;
            if (b.booleanValue()) {
                dn = TRUE;
            } else {
                dn = FALSE;
            }
        } else if (n instanceof String) {
            if (GDR.parse_boolean((String)n)) {
                dn = TRUE;
            } else {
                dn = FALSE;
            }
        } else if (n instanceof Number) {
            if (((Number)n).longValue() == 0L) {
                dn = FALSE;
            } else {
                dn = TRUE;
            }
        } else {
            throw new IllegalArgumentException("Cannot set boolean channel value from type " + n.getClass());
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.eha.impl.channel.ServiceChannelValue#setDnFromString(java.lang.String, int)
     */
    @Override
    public void setDnFromString(final String value, final int componentSize) {
        if (value == null) {
            setDn(false);
        } else {
            setDn(GDR.parse_boolean(value));
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.eha.impl.channel.ServiceChannelValue#stringValue()
     */
    @Override
     public String stringValue()
     {
         if (dn == null) {
             return "";
        } else if (this.getChannelDefinition() != null && this.getChannelDefinition().getLookupTable() != null
                && this.getChannelDefinition().getLookupTable().getValue(longValue()) != null) {
            return this.getChannelDefinition().getLookupTable().getValue(longValue());
        } else {
            if (longValue() == 0) {
                return "False";
            } else {
                return "True";
            }
         }
     }
}
