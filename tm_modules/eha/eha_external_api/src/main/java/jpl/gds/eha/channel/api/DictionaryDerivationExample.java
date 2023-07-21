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
package jpl.gds.eha.channel.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Example of a simple derivation class that uses the dictionary API.
 * 
 *
 */
public class DictionaryDerivationExample extends DerivationAlgorithmBase
{

    @Override
    public Map<String,IChannelValue> deriveChannels(final Map<String,IChannelValue>
    parentChannelValues)
            throws DerivationException
    {
        try {
            final Map<String, IChannelValue> result = new HashMap<>();

            final IChannelValue parent = parentChannelValues.get("A-0037");

            final IChannelValue child = createChannelValue("A-5555", parent.getDnAlarmState());

            result.put("A-5555", child);

            return result;
        } catch (final Exception e) {
            throw new DerivationException("Cannot derive channel A-5555", e);
        }

    }
}

