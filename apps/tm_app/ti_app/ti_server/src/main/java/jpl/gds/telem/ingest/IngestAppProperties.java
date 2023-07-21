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
package jpl.gds.telem.ingest;

import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.config.AbstractDownlinkProperties;

/**
 * The IngestAppProperties class encapsulates a set of flags and values
 * indicating which features are enabled in a downlink session. It is
 * initialized from the configuration file and then modified based upon the
 * user's session configuration.
 *
 * This is not the place for general configuration items but only those
 * concerned with enabling features. Anything else should be moved out.
 *
 *
 */
public class IngestAppProperties extends AbstractDownlinkProperties implements IIngestProperties {

    /** File prefix for the writable config file */
    public static final String      WRITABLE_CONFIG_FILE_PREFIX            = "telem_ingest";

	private static final String PROPERTY_FILE = "ingest_app.properties";
	
	private static final String PROPERTY_PREFIX = "ingestApp.";
	private static final String SERVICES_BLOCK = PROPERTY_PREFIX + "services.";
	private static final String ENABLE = ".enable";

	/**
	 * Name of the configuration property which enables database ongoing mode feature.
	 */
	private static final String ENABLE_ONGOING_MODE_PROPERTY   			 = PROPERTY_PREFIX + "ongoingDbLoad" + ENABLE;

	private static final String REST_PORT_FSW = SERVICES_BLOCK + "port.fsw";
	private static final String REST_PORT_SSE = SERVICES_BLOCK + "port.sse";

    /**
     * Name of the configuration property which enables the frame synchronization feature.
     */
	private static final String ENABLE_FRAME_SYNC_PROPERTY     			 = SERVICES_BLOCK + "frameSync" + ENABLE;
	/**
     * Name of the configuration property which enables the packet extraction feature.
     */
	private static final String ENABLE_PACKET_EXTRACT_PROPERTY			 = SERVICES_BLOCK + "packetExtract" + ENABLE;

	/**
	 * New with AMPCS R3: Allow generic downlink features to be added simply by
	 * configuring them.
	 */
	private static final String MISC_DOWNLINK_SERVICES_PROPERTY_STEM = SERVICES_BLOCK + "miscellaneous";
	/**
	 * Name of the configuration property which lists the misc downlink
	 * features.
	 */
	public static final String MISC_DOWNLINK_SERVICES_LIST_PROPERTY = MISC_DOWNLINK_SERVICES_PROPERTY_STEM + ".managerClasses";
	/**
	 * Name of the configuration property which lists the enable/disable flags
	 * for the list of generic downlink features.
	 */
	public static final String ENABLE_MISC_DOWNLINK_SERVICES_PROPERTY = MISC_DOWNLINK_SERVICES_PROPERTY_STEM + ENABLE;
	/**
	 * Name of the configuration property which enables the time correlation feature.
	 */
	private static final String ENABLE_TIME_CORR_PROPERTY = SERVICES_BLOCK + "timeCorrelationPublishing" + ENABLE;


	private boolean enableFrameSync     	        = true;
	private boolean enablePacketExtract    		    = true;
	private boolean enableTimeCorr       		    = true;

	
	/**
     * Constructor that loads the default property file, which will be located using the
     * standard configuration search.
     *
     * @param sseFlag
     *            the SSE context flag
     */
	public IngestAppProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
	    load();
	}
	
	/**
	 * Loads the feature enable/disable flags from the properties.
	 *
	 */
	private void load() {

		enableOngoingMode = getBooleanProperty(ENABLE_ONGOING_MODE_PROPERTY, DEFAULT_REMOTE_MODE_PROPERTY);
		enableFrameSync = getBooleanProperty(ENABLE_FRAME_SYNC_PROPERTY, true);
		enablePacketExtract = getBooleanProperty(ENABLE_PACKET_EXTRACT_PROPERTY, true);
		enableTimeCorr = getBooleanProperty(ENABLE_TIME_CORR_PROPERTY, true);
        miscFeaturesClassNames = getListProperty(MISC_DOWNLINK_SERVICES_LIST_PROPERTY, null, ",");

		if (miscFeaturesClassNames != null && !miscFeaturesClassNames.isEmpty()) {
		    enableMiscFeatures = getBooleanProperty(ENABLE_MISC_DOWNLINK_SERVICES_PROPERTY, true);
		}

		restPortFsw = getIntProperty(REST_PORT_FSW, 8081);
		restPortSse= getIntProperty(REST_PORT_SSE, 8083);

	}

	@Override
	public boolean isEnableFrameSync() {
		return enableFrameSync;
	}

	@Override
	public boolean isEnablePacketExtract() {
		return enablePacketExtract;
	}
	
	@Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    @Override
    public String getConfigName() {
        return WRITABLE_CONFIG_FILE_PREFIX;
    }

	@Override
	public boolean isEnableTimeCorr() {
		return enableTimeCorr;
	}

}
