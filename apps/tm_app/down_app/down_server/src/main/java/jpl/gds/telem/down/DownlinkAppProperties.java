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
package jpl.gds.telem.down;

import java.util.List;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * The DownlinkAppProperties class encapsulates a set of flags and values
 * indicating which features are enabled in a downlink session. It is
 * initialized from the configuration file and then modified based upon the
 * user's session configuration.
 *
 * This is not the place for general configuration items but only those
 * concerned with enabling features. Anything else should be moved out.
 *
 *
 */
public class DownlinkAppProperties extends GdsHierarchicalProperties {
	
	private static final String PROPERTY_FILE = "down_app.properties";
	
	private static final String PROPERTY_PREFIX = "downApp.";
	private static final String SERVICES_BLOCK = PROPERTY_PREFIX + "services.";
	private static final String ENABLE = ".enable";

    /**
     * Name of the configuration property which enables the frame synchronization feature.
     */
	private static final String ENABLE_FRAME_SYNC_PROPERTY     			 = SERVICES_BLOCK + "frameSync" + ENABLE;
	/**
     * Name of the configuration property which enables the packet extraction feature.
     */
	private static final String ENABLE_PACKET_EXTRACT_PROPERTY			 = SERVICES_BLOCK + "packetExtract" + ENABLE;
	/**
     * Name of the configuration property which enables the packet header channelization feature.
     */
	private static final String ENABLE_PACKET_HEADER_CHANNELIZER_PROPERTY = SERVICES_BLOCK + "packetHeaderChannelizer" + ENABLE;
	/**
     * Name of the configuration property which enables the frame header channelization feature.
     */
	private static final String ENABLE_FRAME_HEADER_CHANNELIZER_PROPERTY	 = SERVICES_BLOCK + "frameHeaderChannelizer" + ENABLE;
	   /**
     * Name of the configuration property which enables the frame header channelization feature.
     */
    private static final String ENABLE_SFDU_HEADER_CHANNELIZER_PROPERTY  = SERVICES_BLOCK + "sfduHeaderChannelizer" + ENABLE;
	/**
     * Name of the configuration property which enables the channel processing feature.
     */
	private static final String ENABLE_EHA_DECOM_PROPERTY    			 = SERVICES_BLOCK + "prechannelizedPacketDecom" + ENABLE;
	/**
     * Name of the configuration property which enables the EVR processing feature.
     */
	private static final String ENABLE_EVR_DECOM_PROPERTY			     = SERVICES_BLOCK + "evrPacketDecom" + ENABLE;
	/**
     * Name of the configuration property which enables the product generation feature.
     */
	private static final String ENABLE_PRODUCT_GEN_PROPERTY 				 = SERVICES_BLOCK + "productGeneration" + ENABLE;
	/**
	 * Name of the configuration property which enables the PDU extraction feature
	 */
	private static final String     ENABLE_PDU_EXTRACT_PROPERTY               = SERVICES_BLOCK + "pduExtraction" + ENABLE;
    /**
     * Name of the configuration property which enables the time correlation feature.
     */	
	private static final String ENABLE_TIME_CORR_PROPERTY		         = SERVICES_BLOCK + "timeCorrelationPublishing" + ENABLE;
	/**
     * Name of the configuration property which enables database ongoing mode feature.
     */ 
	private static final String ENABLE_ONGOING_MODE_PROPERTY   			 = PROPERTY_PREFIX + "ongoingDbLoad" + ENABLE;
	/**
     * Name of the configuration property which enables the generic channel decom feature.
     */ 
	private static final String ENABLE_GENERIC_EHA_DECOM_PROPERTY         = SERVICES_BLOCK + "genericChannelPacketDecom" + ENABLE;
	/**
     * Name of the configuration property which enables the generic channel decom feature.
     */ 
    private static final String ENABLE_GENERIC_EVR_DECOM_PROPERTY         = SERVICES_BLOCK + "genericEvrPacketDecom" + ENABLE;
	/**
     * Name of the configuration property which enables the channel alarm feature.
     */ 
	private static final String ENABLE_ALARMS_PROPERTY         = SERVICES_BLOCK + "alarmProcessing" + ENABLE;
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
	
	private static final boolean DEFAULT_REMOTE_MODE_PROPERTY = false;

	private boolean enableFrameSync     	        = true;
	private boolean enablePacketExtract    		    = true;
	private boolean enablePacketHeaderChannelizer   = true;
	private boolean enableFrameHeaderChannelizer    = true;
    private boolean enableSfduHeaderChannelizer     = false;
	private boolean enableEvrDecom        		    = true;
	private boolean enableEhaDecom        		    = true;
	private boolean enableProductGen      		    = true;
    private boolean enablePduExtract                = false;
	private boolean enableOngoingMode      		    = DEFAULT_REMOTE_MODE_PROPERTY;
	private boolean enableTimeCorr       		    = true;
	private boolean enableGenericChannelDecom       = true;
	private boolean enableGenericEvrDecom           = true;
	private boolean enableAlarms                    = true;
    
    private List<String> miscFeaturesClassNames;
    private boolean enableMiscFeatures = false;  
    private final MissionProperties missionProps;
	
	/**
	 * Constructor that loads the default property file, which will be located using the 
     * standard configuration search.
	 * 
     * @param props the current mission properties object
     * @param sseContextFlag the current mission's SSE context flag
     * 
     * 01/23/19 - added sseContextFlag to constructor arguments.
     *          This way sse_chill_down can actually load the SSE downlink properties...
	 */
	public DownlinkAppProperties(final MissionProperties props, final SseContextFlag sseContextFlag) {
        super(PROPERTY_FILE, true, sseContextFlag);

	    missionProps = props;
	    load();
	}
	
	/**
	 * Loads the feature enable/disable flags from the properties.
	 *
	 */
	private void load() {

		enableFrameSync = getBooleanProperty(ENABLE_FRAME_SYNC_PROPERTY, true);
		enablePacketExtract = getBooleanProperty(ENABLE_PACKET_EXTRACT_PROPERTY, true);
		enablePacketHeaderChannelizer = getBooleanProperty(ENABLE_PACKET_HEADER_CHANNELIZER_PROPERTY, true);
		enableFrameHeaderChannelizer = getBooleanProperty(ENABLE_FRAME_HEADER_CHANNELIZER_PROPERTY, true);
		enableSfduHeaderChannelizer = getBooleanProperty(ENABLE_SFDU_HEADER_CHANNELIZER_PROPERTY, false);
		enableEvrDecom = getBooleanProperty(ENABLE_EVR_DECOM_PROPERTY, true);
		enableEhaDecom = getBooleanProperty(ENABLE_EHA_DECOM_PROPERTY, true);
		enableProductGen = getBooleanProperty(ENABLE_PRODUCT_GEN_PROPERTY, true);
        enablePduExtract = getBooleanProperty(ENABLE_PDU_EXTRACT_PROPERTY, false);
		enableTimeCorr = getBooleanProperty(ENABLE_TIME_CORR_PROPERTY, true);
		enableAlarms = getBooleanProperty(ENABLE_ALARMS_PROPERTY, true);
		enableOngoingMode = getBooleanProperty(ENABLE_ONGOING_MODE_PROPERTY, DEFAULT_REMOTE_MODE_PROPERTY);
        enableGenericChannelDecom = getBooleanProperty(ENABLE_GENERIC_EHA_DECOM_PROPERTY, true);
        enableGenericEvrDecom = getBooleanProperty(ENABLE_GENERIC_EVR_DECOM_PROPERTY, true);
		miscFeaturesClassNames = getListProperty(MISC_DOWNLINK_SERVICES_LIST_PROPERTY, null, ",");
	
		if (miscFeaturesClassNames != null && !miscFeaturesClassNames.isEmpty()) {
		    enableMiscFeatures = getBooleanProperty(ENABLE_MISC_DOWNLINK_SERVICES_PROPERTY, true);
		}	

	}
  
	/**
	 * Gets the enable generic channel decom flag, which controls the channelization 
     * of input telemetry using a decom map.
     * 
	 * @return true if generic decom is enabled, false if disabled
	 */
	public boolean isEnableGenericChannelDecom() {
		return enableGenericChannelDecom && missionProps.isEhaEnabled();
	}
	
	/**
     * Gets the enable generic EVR decom flag, which controls the EVR extraction from 
     * input telemetry using a decom map.
     * 
     * @return true if generic decom is enabled, false if disabled
     */
    public boolean isEnableGenericEvrDecom() {
        return enableGenericEvrDecom && missionProps.areEvrsEnabled();
    }
	
	/**
     * Gets the enable generic decom flag, which controls the channelization 
     * of input telemetry using a decom map.
     * 
     * @return true if generic decom is enabled, false if disabled
     */
    public boolean isEnableAlarms() {
        return enableAlarms && missionProps.isEhaEnabled();
    }

	/**
	 * Gets the ongoing mode flag, which controls whether the database venue is to wait
     * forever for data.
     * 
	 * @return true if ongoing database mode enabled, false if disabled
	 */
	public boolean isOngoingDbMode()
	{
		return enableOngoingMode;
	}

	/**
	 * Gets the enable frame sync flag, which controls the synchronization of
     * raw frame data during processing.
     * 
	 * @return true if frame sync enabled, false if not
	 */
	public boolean isEnableFrameSync() {
		return enableFrameSync;
	}

	/**
	 * Gets the enable EHA decom flag, which controls the decom of pre-channelized
     * packets during processing.
     * 
	 * @return true if EHA decom enabled, false if not
	 */
	public boolean isEnablePreChannelizedDecom() {
		return enableEhaDecom && missionProps.isEhaEnabled();
	}

	/**
	 * Gets the enable EVR decom flag, which controls the decom of EVR packets
     * during processing.
     * 
	 * @return true if EVR decom is enabled, false if not
	 */
	public boolean isEnableEvrDecom() {
		return enableEvrDecom && missionProps.areEvrsEnabled();
	}

	/**
	 * Gets the enable packet extract flag, which controls packet extraction from
     * frames during processing.
     * 
	 * @return true if packet extraction is enabled, false if disabled
	 */
	public boolean isEnablePacketExtract() {
		return enablePacketExtract;
	}

	/**
	 * Gets the enable packet header channelization flag, which controls channelization
	 * of value in the packet headers.
	 * 
	 * @return true if packet header channelization enabled, false if disabled
	 */
	public boolean isEnablePacketHeaderChannelizer() {
		return enablePacketHeaderChannelizer && missionProps.isEhaEnabled();
	}


	/**
     * Gets the enable frame header channelization flag, which controls channelization
     * of value in the frame headers.
     * 
     * @return true if frame header channelization enabled, false if disabled
     */
	public boolean isEnableFrameHeaderChannelizer() {
		return enableFrameHeaderChannelizer && missionProps.isEhaEnabled();
	}


	/**
     * Gets the enable frame header channelization flag, which controls channelization
     * of value in the frame headers.
     * 
     * @return true if frame header channelization enabled, false if disabled
     */
    public boolean isEnableSfduHeaderChannelizer() {
        return enableSfduHeaderChannelizer && missionProps.isEhaEnabled();
    }

    /**
     * Indicates if any header channel capability is enabled.
     * 
     * @return true if ANY header channels enabled, false if not
     */
    public boolean isEnableAnyHeaderChannelizer() {
        return this.enableFrameHeaderChannelizer || this.enablePacketHeaderChannelizer ||
                this.enableSfduHeaderChannelizer;
    }

	/**
	 * Gets the enable product generation flag, which controls the generation
     * of data products during processing.
     * 
	 * @return true if data product generation is enabled, false if not
	 */
	public boolean isEnableProductGen() {
		return enableProductGen && missionProps.areProductsEnabled();
	}

	/**
	 * Gets the enable PDU extraction flag, which controls the extraction
	 * of PDUs from applicable data during processing.
	 * 
	 * @return true if PDU extraction is enabled, false if not
	 */
	public boolean isEnablePduExtract() {
	    return this.enablePduExtract;
	}

	/**
	 * Gets the enable time correlation flag, which controls the correlation of time packets
	 * to reference frames and the generation of time correlation messages.
	 * 
	 * @return true if time correlation is enabled, false if not
	 */
	public boolean isEnableTimeCorr() {
		return enableTimeCorr;
	}

	/**
	 * Gets the list of miscellaneous downlink feature managers' class names that are
	 * configured.
	 * 
	 * @return list of DownlinkFeatureManager to use with downlink session
	 */
	public List<String> getMiscFeatures() {
		return miscFeaturesClassNames;
	}

	/**
	 * Gets the enable flag for miscellaneous downlink features. 
	 * @return true if miscellaneous downlink feature is enabled, false otherwise
	 */
	public boolean isEnableMiscFeatures() {
		return enableMiscFeatures;
	}
	
	@Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }    
}
