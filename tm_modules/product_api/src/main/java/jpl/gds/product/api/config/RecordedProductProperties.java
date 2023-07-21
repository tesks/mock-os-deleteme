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
package jpl.gds.product.api.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * A configuration properties class for the recorded engineering product
 * processing.
 * 
 *
 * @since R8
 * 1/27/18 - Moved from the SMAP adaptation
 *
 */
public class RecordedProductProperties extends GdsHierarchicalProperties {
	
    /** Porperty file name */
	public static final String PROPERTIES_FILE = "recorded_product.properties";
	
	private static final String PROPERTY_PREFIX = "recordedProduct.";
	
	private static final String APIDS_BLOCK = PROPERTY_PREFIX + "apids.";
	private static final String RECORDED_PROCESS_STARTUP_TIMEOUT_BLOCK = PROPERTY_PREFIX + "startupTimeout.";
	private static final String RECORDED_PROCESS_SHUTDOWN_TIMEOUT_BLOCK = PROPERTY_PREFIX + "shutdownTimeout.";
	private static final String VERSION_ONE_BLOCK = PROPERTY_PREFIX + "versionOne.";
	
	private static final String EHA_APIDS_PROPERTY = APIDS_BLOCK + "eha";
	private static final String EVR_APIDS_PROPERTY = APIDS_BLOCK + "evr";
	
	/** Name of the configuration property which tells the recorded product processing watcher script to insert to the database. */
	private static final String ENABLE_RECORDED_PRODUCT_INSERT_PROPERTY = PROPERTY_PREFIX +  "insertToDatabase";
	
	/**
     * Name of the configuration property which tells the recorded product processing watcher script to publish to the message service.
     */ 
    private static final String ENABLE_RECORDED_PRODUCT_PUBLISH_PROPERTY = PROPERTY_PREFIX + "publishToJms";
    
    /**
     * Name of the configuration property which contains the path to the recorded product processing watcher script.
     */ 
    private static final String RECORDED_PRODUCT_WATCHER_SCRIPT_PROPERTY = PROPERTY_PREFIX + "script.watcher";
    
    private static final String RECORDED_PROCESS_STARTUP_TIMEOUT_PROPERTY = RECORDED_PROCESS_STARTUP_TIMEOUT_BLOCK + "seconds"; 
    private static final String RECORDED_PROCESS_SHUTDOWN_TIMEOUT_PROPERTY = RECORDED_PROCESS_SHUTDOWN_TIMEOUT_BLOCK + "seconds"; 
    
    private static final String USE_VERSION_ONE_PRODUCTS_ONLY = VERSION_ONE_BLOCK + "only";
    
    private static final String DEFAULT_RECORDED_PRODUCT_WATCHER_SCRIPT = "bin/internal/chill_recorded_eng_watcher";

    private static final int  DEFAULT_STARTUP_TIMEOUT_SECONDS       = 30;
    private static final int  DEFAULT_SHUTDOWN_TIMEOUT_SECONDS       = 15000;
   
    /**
     * Test Constructor.
     */
    public RecordedProductProperties() {
        this(new SseContextFlag());
    }

    /**
     * Constructor.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public RecordedProductProperties(final SseContextFlag sseFlag) {
        super(PROPERTIES_FILE, sseFlag);
    }
    
    /**
     * Gets the path to the recorded product processing script.
     *
     * @return Full path below $CHILL_GDS
     */
    public String recordedProductProcessingWatcherScript()
    {
        return this.getProperty(RECORDED_PRODUCT_WATCHER_SCRIPT_PROPERTY,
                DEFAULT_RECORDED_PRODUCT_WATCHER_SCRIPT);
    }


    /**
     * Gets the state of the recorded product processing publish to message service flag.
     *
     * @return True if enabled
     */
    public boolean recordedProductProcessingPublishToMessageService()
    {
        return this.getBooleanProperty(ENABLE_RECORDED_PRODUCT_PUBLISH_PROPERTY, false);
    }


    /**
     * Gets the state of the recorded product processing insert to database flag.
     *
     * @return True if enabled
     */
    public boolean recordedProductProcessingInsertToDatabase()
    {
        return this.getBooleanProperty(ENABLE_RECORDED_PRODUCT_INSERT_PROPERTY, false);
    }

    /**
     * Get process recorded engineering process startup timeout, in seconds.
     * 
     * @return timeout
     */
    public int recordedProcessStartupTimeout()
    {
        return this.getIntProperty(RECORDED_PROCESS_STARTUP_TIMEOUT_PROPERTY,
                DEFAULT_STARTUP_TIMEOUT_SECONDS);
    }
    
    /**
     * Gets an array of EHA product APIDs.
     * 
     * @return array of APIDs
     */
    public String [] getEhaProductApids(){
        final String [] returnArr = {};
        return this.getListProperty(EHA_APIDS_PROPERTY, null, ",").toArray(returnArr);
    }
    
    /**
     * Gets an array of EVR product APIDs.
     * 
     * @return array of APIDs
     */
    public String [] getEvrProductApids(){
        final String [] returnArr = {};
        return this.getListProperty(EVR_APIDS_PROPERTY, null, ",").toArray(returnArr);
    }
    
    /**
     * Indicates if only version one products should be processed to extract
     * telemetry.
     * 
     * @return array of APIDs
     */
    public boolean isUseOnlyVersionOneProducts(){
    	    return this.getBooleanProperty(USE_VERSION_ONE_PRODUCTS_ONLY, true);
    }
    
    @Override
    public String getPropertyPrefix(){
        return PROPERTY_PREFIX;
    }
    
    /**
     * Get process recorded engineering process shutdown timeout, in seconds.
     * This will override the default product handler drain time in the 
     * WatcherProperties.
     * 
     * @return timeout
     */
    public int getRecordedProcessShutdownTimeout()
    {
        return this.getIntProperty(RECORDED_PROCESS_SHUTDOWN_TIMEOUT_PROPERTY,
                DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }

}