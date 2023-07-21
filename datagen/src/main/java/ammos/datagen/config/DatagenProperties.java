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
package ammos.datagen.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

public class DatagenProperties extends GdsHierarchicalProperties {

 private static final String PROPERTY_FILE = "datagen.properties";
    
    private static final String PROPERTY_PREFIX = "datagen.";
    
    private static final String INTERNAL_PROPERTY_BLOCK = PROPERTY_PREFIX + "internal.";
    
    private static final String EVR_SCRIPT_PROPERTY = INTERNAL_PROPERTY_BLOCK + "getEvrsLocation";
    private static final String EVR_OPTIONS_PROPERTY = INTERNAL_PROPERTY_BLOCK + "getEvrsOptions";
    private static final String CHANNEL_SCRIPT_PROPERTY = INTERNAL_PROPERTY_BLOCK + "getChanvalsLocation";
    private static final String CHANNEL_OPTIONS_PROPERTY = INTERNAL_PROPERTY_BLOCK + "getChanvalsOptions";
    private static final String PACKET_SCRIPT_PROPERTY = INTERNAL_PROPERTY_BLOCK + "getPacketsLocation";
    private static final String PACKET_OPTIONS_PROPERTY = INTERNAL_PROPERTY_BLOCK + "getPacketsOptions";
    
    
    public DatagenProperties() {
        super(PROPERTY_FILE, new SseContextFlag());
    }
    
    public String getEvrQueryScript() {
        return getProperty(EVR_SCRIPT_PROPERTY);
    }
    
    public String getEvrQueryOptions() {
        return getProperty(EVR_OPTIONS_PROPERTY);
    }
    
    public String getChannelQueryScript() {
        return getProperty(CHANNEL_SCRIPT_PROPERTY);
    }
    
    public String getChannelQueryOptions() {
        return getProperty(CHANNEL_OPTIONS_PROPERTY);
    }
    
    public String getPacketQueryScript() {
        return getProperty(PACKET_SCRIPT_PROPERTY);
    }

    
    public String getPacketQueryOptions() {
        return getProperty(PACKET_OPTIONS_PROPERTY);
    }

    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
