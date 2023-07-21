/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.linksim.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;

public class CfdpLinkSimAmpcsProperties extends GdsHierarchicalProperties {

    private static final String PROPERTY_FILE = "cfdp_linksim.properties";

    private static final String PROPERTY_PREFIX = "cfdpLinkSim.";

    private static final String PORT_PROPERTY = PROPERTY_PREFIX + "port";
    private static final String REST_API_OUTPUT_URL_PROPERTY = PROPERTY_PREFIX + "restApiOutput.url";

    /**
     * Constructor that loads the default property file, which will be found using a
     * standard configuration search.
     */
    public CfdpLinkSimAmpcsProperties() {
        super(PROPERTY_FILE, true);
    }

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    public String getRestApiOutputUrl() {
        return getProperty(REST_API_OUTPUT_URL_PROPERTY, null);
    }

    public int getPort() {
        return getIntProperty(PORT_PROPERTY, 8099);
    }

}