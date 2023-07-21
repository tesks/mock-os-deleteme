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
 * AlgorithmM1015 is a M-channel algorithmic derivation from mon158.ccl.
 *
 */
public class AlgorithmM1015 extends AbstractAlgorithmMDateTime
{
    /**
     * Perform derivations.
     *
     * @param parentChannelValues The input channels
     *
     * @return The output channels
     *
     * @throws DerivationException Problem deriving
     */
    @Override
    public Map<String,IChannelValue> deriveChannels(final Map<String,IChannelValue> parentChannelValues)
        throws DerivationException
    {
        final IChannelValue parentM1014 = parentChannelValues.get("M-1014");
        final IChannelValue parentM1015 = parentChannelValues.get("M-1015");

        if (parentM1014 == null)
        {
            throw new DerivationException("Missing parent M-1014");
        }

        if (parentM1015 == null)
        {
            throw new DerivationException("Missing parent M-1015");
        }
        
        return deriveMDateTimeChannels(parentM1014.longValue(), parentM1015.longValue(), "M-3502", "M-3503");
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
