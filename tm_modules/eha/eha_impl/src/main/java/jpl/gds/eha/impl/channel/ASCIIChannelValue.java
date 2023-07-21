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

/**
 * ASCIIInternalChannelValue represents an instance of a channel value that has a String
 * type data number.
 * 
 *
 */
public class ASCIIChannelValue extends ServiceChannelValue
{
    /**
     * Creates an ASCII channel value from a String.
     * @param v the string value of the channel
     */
    public ASCIIChannelValue(final String v)
    {
        this();  
        setDn(v);
    }

    /**
     * Creates an instance of ASCIIInternalChannelValue with no value.
     */
    public ASCIIChannelValue() {
    	super();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.eha.impl.channel.ServiceChannelValue#setDnFromString(java.lang.String, int)
     */
    @Override
    public void setDnFromString(final String value, final int componentLength) {
        setDn(value);        
    }  
}
