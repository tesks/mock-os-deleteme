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
package jpl.gds.eha.contrib.dsnmonitor;

import java.util.Map;

import jpl.gds.eha.channel.api.DerivationException;
import jpl.gds.eha.channel.api.IChannelValue;


/**
 * AlgorithmM1504 is a M-channel algorithmic derivation from mon158.ccl.
 *
 */
public class AlgorithmM1504 extends AbstractAlgorithmMDateTime
{
    /**
     * Perform derivation.
     *
     * @param parentChannelValues The input channels
     *
     * @return The output channels
     *
     * @throws DerivationException Problem with derivation
     */
    @Override
    public Map<String,IChannelValue> deriveChannels(final Map<String,IChannelValue> parentChannelValues)
        throws DerivationException
    {
        final IChannelValue parentM1503 = parentChannelValues.get("M-1503");
        final IChannelValue parentM1504 = parentChannelValues.get("M-1504");

        if (parentM1503 == null)
        {
            throw new DerivationException("Missing parent M-1503");
        }

        if (parentM1504 == null)
        {
            throw new DerivationException("Missing parent M-1504");
        }
        
        return deriveMDateTimeChannels(parentM1503.longValue(), parentM1504.longValue(), "M-3528", "M-3529");
    }

    @Override
    public void init()
    {
    	//do nothing
    }
    
    @Override
    public void cleanup()
    {
    	//do nothing
    }
    
}
