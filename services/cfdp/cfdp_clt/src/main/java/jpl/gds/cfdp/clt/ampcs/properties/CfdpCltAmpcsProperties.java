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

package jpl.gds.cfdp.clt.ampcs.properties;

import jpl.gds.shared.config.GdsHierarchicalProperties;

public class CfdpCltAmpcsProperties extends GdsHierarchicalProperties {

	private static final String PROPERTY_FILE = "cfdp_clt.properties";

	private static final String PROPERTY_PREFIX = "cfdpClt.";

	private static final String CFDP_PROCESSOR_HOST_DEFAULT_PROPERTY = PROPERTY_PREFIX + "cfdpProcessor.host.default";
	private static final String DEFAULT_CFDP_PROCESSOR_HOST_DEFAULT = "localhost";

	private static final String CFDP_PROCESSOR_PORT_DEFAULT_PROPERTY = PROPERTY_PREFIX + "cfdpProcessor.port.default";
	private static final int DEFAULT_CFDP_PROCESSOR_PORT_DEFAULT = 8080;

	private static final String CFDP_PROCESSOR_URI_ROOT_PROPERTY = PROPERTY_PREFIX + "cfdpProcessor.uri.root";
	private static final String DEFAULT_CFDP_PROCESSOR_URI_ROOT = "/cfdp";

	private static final String CFDP_PROCESSOR_HTTP_TYPE_PROPERTY = PROPERTY_PREFIX + "cfdpProcessor.http.type";
	private static final String DEFAULT_CFDP_PROCESSOR_HTTP_TYPE = "http";

	/**
	 * Constructor that loads the default property file, which will be found using a
	 * standard configuration search.
	 */
	public CfdpCltAmpcsProperties() {
		super(PROPERTY_FILE, true);
	}

	public String getCfdpProcessorHostDefault() {
		return getProperty(CFDP_PROCESSOR_HOST_DEFAULT_PROPERTY, DEFAULT_CFDP_PROCESSOR_HOST_DEFAULT);
	}

	public int getCfdpProcessorPortDefault() {
		return getIntProperty(CFDP_PROCESSOR_PORT_DEFAULT_PROPERTY, DEFAULT_CFDP_PROCESSOR_PORT_DEFAULT);
	}

	public String getCfdpProcessorUriRoot() {
		return getProperty(CFDP_PROCESSOR_URI_ROOT_PROPERTY, DEFAULT_CFDP_PROCESSOR_URI_ROOT);
	}

	public String getCfdpProcessorHttpType() {
		return getProperty(CFDP_PROCESSOR_HTTP_TYPE_PROPERTY, DEFAULT_CFDP_PROCESSOR_HTTP_TYPE);
	}

	@Override
	public String getPropertyPrefix() {
		return PROPERTY_PREFIX;
	}

}