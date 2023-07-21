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
/**
 * 
 */
package jpl.gds.monitor.config;


/**
 * The GlobalChangeParameter class is an enumeration of the global configuration values
 * from the perspective in chill_monitor.
 */
public enum GlobalPerspectiveParameter {
    /**
     * Enumeration name for the rate at which the channel view updates
     */
    CHANNEL_LIST_UPDATE_RATE,
    
    /**
     * Enumeration name for the rate at which the plot view updates
     */
    CHANNEL_PLOT_UPDATE_RATE,
    
    /**
     * Enumeration name for the rate at which the alarm view updates
     */
    CHANNEL_ALARM_UPDATE_RATE,

    /**
     * Enumeration name for the flag indicating that the global view 
     * header should be shown.
     */
    SHOW_HEADER,

    /**
     * Enumeration name for the global fixed page staleness interval.
     */
    FIXED_VIEW_STALENESS_INTERVAL,
    
    /**
     * Length of central EVR history list for NAT EVR views.
     */
    
    /**
     * Global SCLK format.
     */
    SCLK_FORMAT;

    
    /**
     * Gets the XML tag in the perspective that matches the current enum value.
     * @return XML tag name
     */
    public String getXmlTag() {
        switch (this) {
        case CHANNEL_LIST_UPDATE_RATE: return "channelListUpdateRate";
        case CHANNEL_PLOT_UPDATE_RATE: return "channelPlotUpdateRate";
        case CHANNEL_ALARM_UPDATE_RATE: return "channelAlarmUpdateRate";
        case FIXED_VIEW_STALENESS_INTERVAL: return "globalStalenessInterval";
        case SHOW_HEADER: return "showHeader";
        case SCLK_FORMAT: return "sclkFormat";
        }
        return null;
    }
}
