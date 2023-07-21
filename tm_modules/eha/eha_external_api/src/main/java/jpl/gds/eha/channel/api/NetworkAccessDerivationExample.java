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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Example of a simple derivation class that opens a network connection
 * This should not be allowed in a derivation
 * 
 */
public class NetworkAccessDerivationExample extends DerivationAlgorithmBase {

    @Override
    public Map<String, IChannelValue> deriveChannels(final Map<String, IChannelValue> parentChannelValues)
            throws DerivationException {
        final Map<String, IChannelValue> result = new HashMap<>();

        // should not be allowed
        accessNetwork();

        return result;
    }

    // access network
    private void accessNetwork() {
        URL url = null;
        try {
            url = new URL("https://www.jpl.nasa.gov/events/accommodations.php");

        }
        catch (final MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            url.openStream();
            System.out.println("Accessing URL");
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
