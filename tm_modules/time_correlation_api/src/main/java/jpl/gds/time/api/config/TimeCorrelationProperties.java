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
package jpl.gds.time.api.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Configuration properties object for managing properties related to time
 * correlation services.
 * 
 *
 * @since R8
 */
public class TimeCorrelationProperties extends GdsHierarchicalProperties {

    /** Name of the property file */
    public static final String PROPERTY_FILE = "time_correlation.properties";
    
    private static final String PROPERTY_PREFIX = "timeCorrelation.";
    private static final String FLIGHT_BLOCK = PROPERTY_PREFIX + "flight.";
    private static final String SSE_BLOCK = PROPERTY_PREFIX + "sse.";
    private static final String SERVICE_BLOCK = PROPERTY_PREFIX + "service.";
    
    private static final String FSW_APID_PROPERTY = FLIGHT_BLOCK + "packetApid";
    private static final String FSW_RATE_INDEX_SIZE = FLIGHT_BLOCK + "rateIndexSize";   
    private static final String SSE_APID_PROPERTY = SSE_BLOCK + "packetApid";
    private static final String SERVICE_FRAMEBUFF_PROPERTY = SERVICE_BLOCK + "frameBufferLen";
    
    private static final int DEFAULT_APID = 1;
    private static final int DEFAULT_RATE_INDEX_SIZE = 1;
    private static final int DEFAULT_FRAME_BUFF = 4096;
    
    /**
     * Test constructor
     */
    public TimeCorrelationProperties() {
        this(new SseContextFlag());
    }
    
    /**
     * Constructor that loads the default property file, which will be located
     * using a standard configuration search.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public TimeCorrelationProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }

    /**
     * Gets the defined time correlation packet APID.
     * 
     * @param forSse
     *            true if for SSE, false if for flight
     * @return APID
     */
    public int getTcPacketApid(final boolean forSse) {
        return getIntProperty(forSse ? SSE_APID_PROPERTY : FSW_APID_PROPERTY, DEFAULT_APID);
    }
    
    /**
     * Gets the byte length of the rate index in a flight TC packet.
     * @return byte length
     */
    public int getFlightRateIndexSize() {
        return getIntProperty(FSW_RATE_INDEX_SIZE, DEFAULT_RATE_INDEX_SIZE);
    }
    
    /**
     * Gets the length of the reference frame buffer in the time correlation service.
     * 
     * @return frame count
     */
    public int getReferenceFrameBufferLength() {
        return getIntProperty(SERVICE_FRAMEBUFF_PROPERTY, DEFAULT_FRAME_BUFF);
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
