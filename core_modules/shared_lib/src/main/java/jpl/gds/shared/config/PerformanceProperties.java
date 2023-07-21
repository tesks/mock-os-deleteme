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
package jpl.gds.shared.config;

import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Loads and provides information from the performance.properties file to GUI
 * components that need it. The properties file is searched for using the
 * standard AMPCS configuration file search. 
 * 
 */
public class PerformanceProperties extends GdsHierarchicalProperties {

    /** The property file */
    private static final String PROPERTY_FILE = "performance.properties";

    private static final String PROPERTY_PREFIX = "performance.";
    // Property Names
    private static final String BOUNDED_QUEUE_YELLOW = PROPERTY_PREFIX + "boundedQueueYellowLevel";
    private static final String BOUNDED_QUEUE_RED = PROPERTY_PREFIX + "boundedQueueRedLevel";
    private static final String HEAP_YELLOW = PROPERTY_PREFIX + "heapYellowLevel";
    private static final String HEAP_RED = PROPERTY_PREFIX + "heapRedLevel";
    private static final String SUMMARY_INTERVAL = PROPERTY_PREFIX + "summaryInterval";
    private static final String SHUTDOWN_SUMMARY_INTERVAL = PROPERTY_PREFIX + "shutdownSummaryInterval";

    /**
     * Test constructor
     */
    public PerformanceProperties() {
        this(new SseContextFlag());
    }

    /**
     * Creates and loads the object.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public PerformanceProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }


    /**
     * Gets the percentage level at which a bounded queue should be considered
     * in YELLOW health state.  A value of 0 should disable YELLOW health checking.
     * 
     * @return percentage (0 - 100)
     */
    public int getBoundedQueueYellowLevel() {
        int val = getIntProperty(BOUNDED_QUEUE_YELLOW, 80);
        if (val < 0 || val > 100) {
            log.warn("Value for " + BOUNDED_QUEUE_YELLOW + " in the " + PROPERTY_FILE + " file is not a valid percentage; setting to 0");
            val = 0;
        }
        return val;
    }

    /**
     * Gets the percentage level at which a bounded queue should be considered
     * in RED health state.  A value of 0 should disable RED health checking.
     * 
     * @return percentage (0 - 100)
     */
    public int getBoundedQueueRedLevel() {
        int val = getIntProperty(BOUNDED_QUEUE_RED, 100);
        if (val < 0 || val > 100) {
            log.warn("Value for " + BOUNDED_QUEUE_RED + " in the " + PROPERTY_FILE + " file is not a valid percentage; setting to 0");
            val = 0;
        }
        return val;
    }


    /**
     * Gets the percentage level at which the heap should be considered
     * in YELLOW health state.  A value of 0 should disable YELLOW health checking.
     * 
     * @return percentage (0 - 100)
     */
    public int getHeapYellowLevel() {
        int val = getIntProperty(HEAP_YELLOW, 70);
        if (val < 0 || val > 100) {
            log.warn("Value for " + HEAP_YELLOW + " in the " + PROPERTY_FILE + " file is not a valid percentage; setting to 0");
            val = 0;
        }
        return val;
    }

    /**
     * Gets the percentage level at which the heap should be considered
     * in RED health state.  A value of 0 should disable YELLOW health checking.
     * 
     * @return percentage (0 - 100)
     */
    public int getHeapRedLevel() {
        int val = getIntProperty(HEAP_RED, 70);
        if (val < 0 || val > 100) {
            log.warn("Value for " + HEAP_RED + " in the " + PROPERTY_FILE + " file is not a valid percentage; setting to 0");
            val = 0;
        }
        return val;
    }

    /**
     * Gets the time interval between issuance of performance summary messages
     * during normal operations, in milliseconds.
     * 
     * @return milliseconds
     */
    public int getSummaryInterval() {
        return getIntProperty(SUMMARY_INTERVAL, 5000);
    }

    /**
     * Gets the time interval between issuance of performance summary messages
     * during shutdown, in milliseconds.
     * 
     * @return milliseconds
     */
    public int getShutdownSummaryInterval() {
        return getIntProperty(SHUTDOWN_SUMMARY_INTERVAL, 3000);
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
