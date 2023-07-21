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
 * AlgorithmM0646 is a M-channel algorithmic derivation from mon158.ccl.
 *
 */
public class AlgorithmM0646 extends AbstractAlgorithmMDateTime
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
        final IChannelValue parentM0645 = parentChannelValues.get("M-0645");
        final IChannelValue parentM0646 = parentChannelValues.get("M-0646");

        if (parentM0645 == null)
        {
            throw new DerivationException("Missing parent M-0645");
        }

        if (parentM0646 == null)
        {
            throw new DerivationException("Missing parent M-0646");
        }
        
        return deriveMDateTimeChannels(parentM0645.longValue(), parentM0646.longValue(), "M-3500", "M-3501");
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
