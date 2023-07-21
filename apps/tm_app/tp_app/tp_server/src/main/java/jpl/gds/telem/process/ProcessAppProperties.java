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
package jpl.gds.telem.process;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.config.AbstractDownlinkProperties;

/**
 * The ProcessAppProperties class encapsulates a set of flags and values
 * indicating which features are enabled in a downlink session. It is
 * initialized from the configuration file and then modified based upon the
 * user's session configuration.
 *
 * This is not the place for general configuration items but only those
 * concerned with enabling features. Anything else should be moved out.
 *
 */
public class ProcessAppProperties extends AbstractDownlinkProperties implements IProcessProperties {

    /** File prefix for the writable config file */
    private static final String      WRITABLE_CONFIG_FILE_PREFIX               = "telem_processor";

	private static final String PROPERTY_FILE = "process_app.properties";
	
	private static final String PROPERTY_PREFIX = "processApp.";
	private static final String SERVICES_BLOCK = PROPERTY_PREFIX + "services.";
	private static final String ENABLE = ".enable";

	private static final String REST_PORT_FSW = SERVICES_BLOCK + "port.fsw";
	private static final String REST_PORT_SSE = SERVICES_BLOCK + "port.sse";

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
	 * Name of the configuration property which enables database ongoing mode feature.
	 */
	private static final String ENABLE_ONGOING_MODE_PROPERTY   			 = PROPERTY_PREFIX + "ongoingDbLoad" + ENABLE;
	/**
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
     * Name of the configuration property which enables the EHA aggregation feature.
     */
    private static final String ENABLE_EHA_AGGREGATION_PROPERTY = SERVICES_BLOCK + "ehaAggregation" + ENABLE;

	/**
	 * New with AMPCS R3: Allow generic downlink features to be added simply by
	 * configuring them.
	 */
	private static final String MISC_DOWNLINK_SERVICES_PROPERTY_STEM = SERVICES_BLOCK + "miscellaneous";
	/**
	 * Name of the configuration property which lists the misc downlink
	 * features.
	 */
	private static final String MISC_DOWNLINK_SERVICES_LIST_PROPERTY = MISC_DOWNLINK_SERVICES_PROPERTY_STEM + ".managerClasses";
	/**
	 * Name of the configuration property which lists the enable/disable flags
	 * for the list of generic downlink features.
	 */
	private static final String ENABLE_MISC_DOWNLINK_SERVICES_PROPERTY = MISC_DOWNLINK_SERVICES_PROPERTY_STEM + ENABLE;

	private static final String MESSAGE_HANDLER_QUEUE_SIZE = PROPERTY_PREFIX + "worker.messageHandler.queueSize";
	private static final String TELEMETRY_WAIT_TIMEOUT = PROPERTY_PREFIX + "worker.telemetry.waitTimeout";
	private static final String SPILL_PROCESSOR_QUEUE_SIZE = PROPERTY_PREFIX + "worker.spillProcessor.queueSize";
	private static final String SPILL_WAIT_TIMEOUT = PROPERTY_PREFIX + "worker.spillProcessor.waitTimeout";

	private boolean enablePacketHeaderChannelizer   = true;
	private boolean enableFrameHeaderChannelizer    = true;
    private boolean enableSfduHeaderChannelizer     = false;
	private boolean enableEvrDecom        		    = true;
	private boolean enableEhaDecom        		    = true;
	private boolean enableProductGen      		    = true;

    private boolean enablePduExtract                = false;

	private boolean enableGenericChannelDecom       = true;
	private boolean enableGenericEvrDecom           = true;
	private boolean enableAlarms                    = true;
    private boolean enableEhaAggregation            = true;

    private int messageHandlerQueueSize;
    private int telemetryWaitTimeout;
	private int spillProcessorQueueSize;
    private long spillWaitTimeout;

    private final MissionProperties missionProps;

	/**
     * Constructor that loads the default property file, which will be located using the
     * standard configuration search.
     *
     * @param props
     *            the current mission properties object
     * @param sseFlag
     *            the SSE context flag
     */
    public ProcessAppProperties(final MissionProperties props, final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);

	    missionProps = props;
	    load();
	}
	
	/**
	 * Loads the feature enable/disable flags from the properties.
	 *
	 */
	private void load() {

        enablePduExtract = getBooleanProperty(ENABLE_PDU_EXTRACT_PROPERTY, false);
        miscFeaturesClassNames = getListProperty(MISC_DOWNLINK_SERVICES_LIST_PROPERTY, null, ",");


		enablePacketHeaderChannelizer = getBooleanProperty(ENABLE_PACKET_HEADER_CHANNELIZER_PROPERTY, true);
		enableFrameHeaderChannelizer = getBooleanProperty(ENABLE_FRAME_HEADER_CHANNELIZER_PROPERTY, true);
		enableSfduHeaderChannelizer = getBooleanProperty(ENABLE_SFDU_HEADER_CHANNELIZER_PROPERTY, false);
		enableEvrDecom = getBooleanProperty(ENABLE_EVR_DECOM_PROPERTY, true);
		enableEhaDecom = getBooleanProperty(ENABLE_EHA_DECOM_PROPERTY, true);
		enableProductGen = getBooleanProperty(ENABLE_PRODUCT_GEN_PROPERTY, true);

		enableAlarms = getBooleanProperty(ENABLE_ALARMS_PROPERTY, true);
		enableEhaAggregation = getBooleanProperty(ENABLE_EHA_AGGREGATION_PROPERTY, false);
		enableOngoingMode = getBooleanProperty(ENABLE_ONGOING_MODE_PROPERTY, DEFAULT_REMOTE_MODE_PROPERTY);
        enableGenericChannelDecom = getBooleanProperty(ENABLE_GENERIC_EHA_DECOM_PROPERTY, true);
        enableGenericEvrDecom = getBooleanProperty(ENABLE_GENERIC_EVR_DECOM_PROPERTY, true);

		if (miscFeaturesClassNames != null && !miscFeaturesClassNames.isEmpty()) {
		    enableMiscFeatures = getBooleanProperty(ENABLE_MISC_DOWNLINK_SERVICES_PROPERTY, true);
		}

		restPortFsw = getIntProperty(REST_PORT_FSW, 8082);
		restPortSse= getIntProperty(REST_PORT_SSE, 8084);

		messageHandlerQueueSize = getIntProperty(MESSAGE_HANDLER_QUEUE_SIZE, 8192);
		telemetryWaitTimeout = getIntProperty(TELEMETRY_WAIT_TIMEOUT, 10);
		spillProcessorQueueSize = getIntProperty(SPILL_PROCESSOR_QUEUE_SIZE, 8192);
		spillWaitTimeout = getIntProperty(SPILL_WAIT_TIMEOUT, 100);
	}


	@Override
    public boolean isEnableAggregatedEha() {
        return enableEhaAggregation && missionProps.isEhaEnabled();
    }

	@Override
	public boolean isEnableGenericChannelDecom() {
		return enableGenericChannelDecom && missionProps.isEhaEnabled();
	}
	
	@Override
    public boolean isEnableGenericEvrDecom() {
        return enableGenericEvrDecom && missionProps.areEvrsEnabled();
    }
	
	@Override
    public boolean isEnableAlarms() {
        return enableAlarms && missionProps.isEhaEnabled();
    }

	@Override
	public boolean isEnablePreChannelizedDecom() {
		return enableEhaDecom && missionProps.isEhaEnabled();
	}

	@Override
	public boolean isEnableEvrDecom() {
		return enableEvrDecom && missionProps.areEvrsEnabled();
	}

	@Override
	public boolean isEnablePacketHeaderChannelizer() {
		return enablePacketHeaderChannelizer && missionProps.isEhaEnabled();
	}


	@Override
	public boolean isEnableFrameHeaderChannelizer() {
		return enableFrameHeaderChannelizer && missionProps.isEhaEnabled();
	}


	@Override
    public boolean isEnableSfduHeaderChannelizer() {
        return enableSfduHeaderChannelizer && missionProps.isEhaEnabled();
    }

    @Override
    public boolean isEnableAnyHeaderChannelizer() {
        return this.enableFrameHeaderChannelizer || this.enablePacketHeaderChannelizer ||
                this.enableSfduHeaderChannelizer;
    }

	@Override
	public boolean isEnableProductGen() {
		return enableProductGen && missionProps.areProductsEnabled();
	}

	@Override
	public boolean isEnablePduExtract() {
	    return this.enablePduExtract;
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
	public int getMessageHandlerQueueSize() {
		return messageHandlerQueueSize;
	}

	@Override
	public int getTelemetryWaitTimeout() {
		return telemetryWaitTimeout;
	}

	@Override
	public int getSpillProcessorQueueSize() {
		return spillProcessorQueueSize;
	}

	@Override
	public long getSpillWaitTimeout(){
		return spillWaitTimeout;
	}
}
