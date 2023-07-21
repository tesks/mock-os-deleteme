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
package jpl.gds.tm.service.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * A configuration properties class for reading and managing configuration for the
 * tm_service projects.
 * 
 * @since R8
 */
public class TelemetryServiceProperties extends GdsHierarchicalProperties {

    private static final String PROPERTY_FILE = "telem_service.properties";
    
    private static final String PROPERTY_PREFIX = "telemService.";
    
    private static final String PACKET_BLOCK = PROPERTY_PREFIX + "packet.";       
    private static final String PACKET_TRACKING_BLOCK = PACKET_BLOCK + "tracking.";    
    private static final String PACKET_TRACKING_SUMMARY_INTERVAL_PROPERTY = PACKET_TRACKING_BLOCK + "reportInterval"; 

    private static final String FRAME_BLOCK = PROPERTY_PREFIX + "frame.";
  
    private static final String SYNC_BLOCK = FRAME_BLOCK + "synchronizer.";
    private static final String OUT_OF_SYNC_THRESHOLD_PROPERTY = SYNC_BLOCK + "outOfSyncThreshold";
    private static final String DO_CHECKSUM_PROPERTY = SYNC_BLOCK + "validateChecksums";
    
    private static final String FRAME_TRACKING_BLOCK = FRAME_BLOCK + "tracking.";    
    private static final String FRAME_TRACKING_SUMMARY_INTERVAL_PROPERTY = FRAME_TRACKING_BLOCK + "reportInterval";
    
    private static final int OUT_OF_SYNC_MIN = 128;
    private static final int OUT_OF_SYNC_DEFAULT = 800;
    private static final int OUT_OF_SYNC_MAX = 65535;

    private static final int REPORT_INTERVAL_MIN = 2;
    private static final int REPORT_INTERVAL_DEFAULT = 15000;
    
    /**
     * Test constructor
     */
    public TelemetryServiceProperties() {
        this(new SseContextFlag());
    }

    /**
     * Constructor that loads the default property file, which will be found using
     * a standard configuration search.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public TelemetryServiceProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }
    
    /**
     * Gets the packet tracking report interval, or the time between packet
     * summary messages.
     * 
     * @return tracking interval, milliseconds
     */
    public int getPacketTrackingReportInterval() {
        final int result = getIntProperty(PACKET_TRACKING_SUMMARY_INTERVAL_PROPERTY, REPORT_INTERVAL_DEFAULT);
        if (result < REPORT_INTERVAL_MIN) {
            return REPORT_INTERVAL_MIN;
        }
        return result;
    }
    
    /**
     * Gets the flag indicating whether frame checksums should be validated
     * during frame synchronization.
     * 
     * @return true to enable checksums, false to disable
     */
    public boolean doFramesyncChecksum() {
        return getBooleanProperty(DO_CHECKSUM_PROPERTY, false);
    }
    
    /**
     * Gets the upper bound, in bytes, of the size of an "out of sync" data chunk t
     * be included in an out of sync bytes event message. Out of sync data is
     * always reported. This just indicates the maximum amount that will be included
     * in each event message.
     * 
     * @return upper bound, in bytes, of out of sync data chunks
     */
    public int getOutOfSyncReportThreshold() {
        final int result = getIntProperty(OUT_OF_SYNC_THRESHOLD_PROPERTY, OUT_OF_SYNC_DEFAULT);
        if (result < OUT_OF_SYNC_MIN || result > OUT_OF_SYNC_MAX) {
            return OUT_OF_SYNC_DEFAULT;
        }
        return result;
    }
    
    /**
     * Gets the frame tracking report interval, or the time between frame
     * summary messages.
     * 
     * @return tracking interval, milliseconds
     */
    public int getFrameTrackingReportInterval() {
        final int result = getIntProperty(FRAME_TRACKING_SUMMARY_INTERVAL_PROPERTY, REPORT_INTERVAL_DEFAULT);
        if (result < REPORT_INTERVAL_MIN) {
            return REPORT_INTERVAL_MIN;
        }
        return result;
    }
     
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
