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
package jpl.gds.evr.api;


/**
 * Spring bean names for the EVR projects.
 * 
 * @since R8
 */
public interface EvrApiBeans {
    
    /**
     * Factory bean that creates EVR objects.
     */
    public static final String EVR_FACTORY = "EVR_FACTORY";
    /**
     * Factory bean for creating EVR message objects.
     */
    public static final String EVR_MESSAGE_FACTORY = "EVR_MESSAGE_FACTORY";
    /**
     * EVR notification service bean.
     */
    public static final String EVR_NOTIFIER_SERVICE  = "EVR_NOTIFIER_SERVICE";
    /**
     * EVR publication service bean.
     */
    public static final String EVR_PUBLISHER_SERVICE = "EVR_PUBLISHER_SERVICE";
    /**
     * Factory bean that creates EVR raw data field objects.
     */
    public static final String EVR_RAW_DATA_FACTORY = "EVR_RAW_DATA_FACTORY";
    /**
     * EVR properties bean, for access to EVR-related configuration.
     */
    public static final String EVR_PROPERTIES        = "EVR_PROPERTIES";
    /**
     * EVR extractor bean, for extracting EVRs from packets.
     */
    public static final String EVR_EXTRACTOR         = "EVR_EXTRACTOR";
    /**
     * EVR extractor utility bean, providing utility functions for EVR
     * extraction.
     */
    public static final String EVR_EXTRACTOR_UTILITY = "EVR_EXTRACTOR_UTILITY";

}
