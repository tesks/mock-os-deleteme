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
 * AlgorithmM1185 is a M-channel algorithmic derivation from mon158.ccl.
 *
 */
public class AlgorithmM1185 extends AbstractAlgorithmMHHMMSSFromSecs
{
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,IChannelValue> deriveChannels(final Map<String,IChannelValue> parentChannelValues)
        throws DerivationException
    {
        final IChannelValue parentM1185 = parentChannelValues.get("M-1185");

        if (parentM1185 == null)
        {
            throw new DerivationException("Missing parent M-1185");
        }
        
        return deriveMHHMMSSChannelFromSecs(parentM1185.intValue(), "M-3628");
    }
    
}
