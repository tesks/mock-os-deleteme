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
package jpl.gds.eha.api.config;

import java.io.File;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Properties class for loading and managing configuration properties for the
 * EHA projects.
 * 
 * @since R8
 */
public class EhaProperties extends GdsHierarchicalProperties {
    /**
     * Name of the default properties file.
     */
	private static final String PROPERTY_FILE = "eha.properties";
    
	/**
	 * System property that can be used to define the location of the flight
	 * suspect channels file.
	 */
    public final static String SUSPECT_FILE_LOCATION_PROPERTY = "GdsSuspectChannelFile";
    /**
     * System property that can be used to define the location of the SSE
     * suspect channels file.
     */
    public final static String SSE_SUSPECT_FILE_LOCATION_PROPERTY = "GdsSseSuspectChannelFile";
    
    private static final String PROPERTY_PREFIX = "eha";
    private static final String AGGREGATION_BLOCK = PROPERTY_PREFIX + ".aggregation";
    private static final String CHANNEL_PROC_BLOCK = PROPERTY_PREFIX + ".channelProcessing";
    private static final String SUSPECT_CHANNELS_BLOCK = PROPERTY_PREFIX + ".suspectChannels";
    private static final String UTILITY_BLOCK = PROPERTY_PREFIX + ".utility";
    private static final String DERIVATION_BLOCK = CHANNEL_PROC_BLOCK + ".derivation";
    private static final String HEADERS_BLOCK = CHANNEL_PROC_BLOCK + ".headers";

    private static final String AGGREGATED_CHANNEL_GROUP_TTL_PROPERTY = AGGREGATION_BLOCK + ".groupTimeToLive";
    private static final String AGGREGATED_CHANNEL_GROUP_MAX_SIZE_PROPERTY = AGGREGATION_BLOCK + ".maxChannelsPerGroup";

    private static final String USE_TRIGGER_CHANNELS_PROPERTY = DERIVATION_BLOCK + ".useTriggerChannels";
    private static final String DERIVATION_TIMEOUT_PROPERTY = DERIVATION_BLOCK + ".timeout";
    
    private static final String ENABLE_DERIVATION_PROPERTY = DERIVATION_BLOCK + ".enable";
    
    private static final String FILL_HEADERS_ENABLE_PROPERTY = HEADERS_BLOCK + ".enableFillPackets";
    private static final String IDLE_HEADERS_ENABLE_PROPERTY = HEADERS_BLOCK + ".enableIdleFrames";
    
    private static final String SUSPECT_CHANNELS_ENABLE_PROPERTY = SUSPECT_CHANNELS_BLOCK + ".enable";
    private static final String SUSPECT_CHANNELS_PUBLISH_INTERVAL_PROPERTY = SUSPECT_CHANNELS_BLOCK + ".publishInterval";
    private static final String SUSPECT_CHANNELS_FLIGHT_FILE_PROPERTY = SUSPECT_CHANNELS_BLOCK + ".flight.filePath";
    private static final String SUSPECT_CHANNELS_SSE_FILE_PROPERTY = SUSPECT_CHANNELS_BLOCK + ".sse.filePath";

    private static final String FLOAT_CHANGE_FACTOR_PROPERTY = UTILITY_BLOCK + ".floatChangeFactor";
    
    private static final String STRICT_EHA_PROCESSING = CHANNEL_PROC_BLOCK + ".strict";
    
    private static final long DERIVATION_TIMEOUT_DEFAULT = 5000;
    private static final long DEFAULT_SUSPECT_INTERVAL = 30;
    private static final long DEFAULT_AGGREGATED_CHANNEL_GROUP_TTL = 3000;
    private static final int DEFAULT_AGGREGATED_CHANNEL_MAX_GROUP_SIZE = 100;

    /**
     * Test constructor
     */
    public EhaProperties() {
        this(new SseContextFlag());
    }

    /**
     * Constructor that loads the default property file, which will be
     * located using the standard configuration search.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     */
    public EhaProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }
    
    /**
     * Indicates whether channel values will be shown during eha
     * processing.
     * 
     * @return true if channel values should be shown, false otherwise
     */
    public boolean isStrictProcessing() {
    	return getBooleanProperty(STRICT_EHA_PROCESSING, false);
    }
    
    /**
     * Indicates whether derivation are triggered by the arrival of a single
     * channel, or by all parents channels to the derivation.
     * 
     * @return true if derivations triggered by one channel, false if not
     */
    public boolean isUseTriggerChannels() {
        return getBooleanProperty(USE_TRIGGER_CHANNELS_PROPERTY, true);
    }
    

    /**
     * Indicates whether header channels should be extracted from fill packets.
     * 
     * @return true to extract fill header channels, false to not
     */
    public boolean enableFillPacketHeaderChannels() {
        return getBooleanProperty(FILL_HEADERS_ENABLE_PROPERTY, false);
    }
    

    /**
     * Gets the float change factor, which determines how much the value of a
     * floating point channel value can change before it is actually considered
     * a different value.
     * 
     * @return double value representing minimum detected change
     */
    public double getFloatChangeFactor() {
        return Math.min(getDoubleProperty(FLOAT_CHANGE_FACTOR_PROPERTY, 0.0), 1.0);
    }

    /**
     * Gets the timeout/time-to-live for an EHA channel group. This is the
     * maximum amount of time that channels will be accumulated into a group
     * before a grouped EHA channel message is issues.
     * 
     * @return time to live, milliseconds
     */
    public long getChannelGroupTimeToLive() {
        return getLongProperty(AGGREGATED_CHANNEL_GROUP_TTL_PROPERTY, DEFAULT_AGGREGATED_CHANNEL_GROUP_TTL);
    }
    
    /**
     * Gets the maximum number of channels that will be put into a single
     * grouped EHA channel messages.
     * 
     * @return number of channels
     */
    public int getMaxChannelGroupSize() {
        return getIntProperty(AGGREGATED_CHANNEL_GROUP_MAX_SIZE_PROPERTY, DEFAULT_AGGREGATED_CHANNEL_MAX_GROUP_SIZE);
    }
    
    /**
     * Gets the channel derivation timeout.This is the time that a derivation is
     * allowed to run before it will be considered to be hung and be killed.
     * 
     * @return timeout, milliseconds
     */
    public long getDerivationTimeout() {
        return getLongProperty(DERIVATION_TIMEOUT_PROPERTY, DERIVATION_TIMEOUT_DEFAULT);
    }
    
    /**
     * Gets the interval between issuance of suspect channel messages.
     * 
     * @return interval, seconds
     */
    public long getSuspectChannelPublishInterval() {
        return getLongProperty(SUSPECT_CHANNELS_PUBLISH_INTERVAL_PROPERTY, DEFAULT_SUSPECT_INTERVAL);
    }
    
    /**
     * Indicates whether channel derivation is enabled.
     * 
     * @return true if derivation enabled, false if not
     */
    public boolean isDerivationEnabled() {
        return getBooleanProperty(ENABLE_DERIVATION_PROPERTY, true);
    }
    
    /**
     * Indicates whether header channels should be extracted from idle frames.
     * 
     * @return true to extract idle header channels, false to not
     */
    public boolean enableIdleFrameHeaderChannels() {
        return getBooleanProperty(IDLE_HEADERS_ENABLE_PROPERTY, false);
    }
    
    /**
     * Indicates whether suspect channel reporting is enabled.
     * 
     * @return true if enabled, false if not
     */
    public boolean enableSuspectChannels() {
        return getBooleanProperty(SUSPECT_CHANNELS_ENABLE_PROPERTY, true);
    }
    
    /**
     * Gets the path to the suspect channel file.
     * 
     * @param isSse true if this is being invoked for SSE processing
     * @return file path
     */
    public String getSuspectChannelFilePath(final boolean isSse) {
        final String configured = getProperty(isSse ? SUSPECT_CHANNELS_SSE_FILE_PROPERTY : SUSPECT_CHANNELS_FLIGHT_FILE_PROPERTY, null);
        final String environLoc = GdsSystemProperties.getSystemProperty(isSse ? SSE_SUSPECT_FILE_LOCATION_PROPERTY
                : SUSPECT_FILE_LOCATION_PROPERTY);
       
        if (environLoc != null) {
            return environLoc;
        }
        if (configured != null) {
            if (configured.startsWith(File.separator)) {
                return configured;
            } else {
                final String local = GdsSystemProperties.getMostLocalPath(configured, sseFlag);
                if (local == null) {
                    return GdsSystemProperties.getUserConfigDir() + File.separator + configured;
                } else {
                    return local;
                }
            }
        }
        return null;
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX + ".";
    }
}
