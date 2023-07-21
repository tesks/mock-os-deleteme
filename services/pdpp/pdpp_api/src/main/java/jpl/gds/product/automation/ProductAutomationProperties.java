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
package jpl.gds.product.automation;

import java.util.*;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * ProductAutomationConfig manages configuration information specific to classes
 * in the product automation package. It gets its data from the
 * product_automation.properties file. It is a singleton class.
 * 
 * 06/07/16 - MPCS-8179  - Added to AMPCS, updated from original version in MPCS for MSL G9.
 * 07/19/16 - MPCS-8180  - Updated by adding numerous properties
 * 07/05/17 - MPCS-8608  - changed name to ProductAutomationProperties, changed PROPERTY_FILE value to "product_automation.properties", changed PROPERTY_PREFIX value to "productAutomation."
 */
public final class ProductAutomationProperties extends GdsHierarchicalProperties
{
	
	/**
	 * name of the properties file that ProductAutomationConfig will load
	 */
	public static final String PROPERTY_FILE = "product_automation.properties";
	
	private static final String PROPERTY_PREFIX = "productAutomation.";
	
	private static final String CHILL_DOWN_BLOCK = PROPERTY_PREFIX + "chilldown.";
	private static final String MAX_ERROR_PROPERTY = CHILL_DOWN_BLOCK + "maxerrors";
	
	private static final String HIBERNATE_BLOCK = PROPERTY_PREFIX + "hibernate.";
	private static final String CLASSES_PROPERTY = HIBERNATE_BLOCK + "annotatedclasses";
	
	private static final String DBURL_BLOCK = HIBERNATE_BLOCK + "dburl.";
	private static final String DATABASE_NAME_PROPERTY = DBURL_BLOCK + "name";
	private static final String DATABASE_PORT_PROPERTY = DBURL_BLOCK + "port";
	

	// MPCS-8382  8/22/2016 - Adding PDPP db host and password.
	private static final String DATABASE_HOST_PROPERTY = DBURL_BLOCK + "host";
	private static final String DATABASE_USER_PROPERTY = DBURL_BLOCK + "user";
	private static final String DATABASE_PASSWORD_PROPERTY = DBURL_BLOCK + "password";
	
	private static final String HIBERNATE_CONFIG_BLOCK = HIBERNATE_BLOCK + "config.";
	
	//used by AbstractAutomationDAO
	//  - 11/21/2012 - MPCS-4387 - Property values used to manage the number of db connections.
	private static final String CONNECTION_CHANGE_VALUE_PROPERTY =  HIBERNATE_BLOCK + "dbconnectionschange";
	private static final String MIN_MAX_CONNECTION_VALUE_PROPERTY = HIBERNATE_BLOCK + "minmaxconnections";
	private static final String MAX_MAX_CONNECTION_VALUE_PROPERTY = HIBERNATE_BLOCK + "maxmaxconnections";
	private static final String DO_ADJUSTMENTS_PROPERTY =           HIBERNATE_BLOCK + "doconnectionadjustments";
	
	
	
	private static final String GUI_BLOCK = PROPERTY_PREFIX + "automationgui.";
	private static final String USE_CACHE_PROPERTY = GUI_BLOCK + "usecache";
	
	
	
	//used by ProductAutomationActionDAO
	private static final String SUBPROCESS_BLOCK = PROPERTY_PREFIX + "subprocesses.";
	private static final String MAX_QUERY_RESULTS_PROPERTY = SUBPROCESS_BLOCK + "maxacceptedresult";
	/* MPCS-8180 07/19/16 - added self kill, startup delay, process cycle time, max consecutive errors,
	 * process lookup retry count, and close db connections properties
	 */
	private static final String SELF_KILL_TIME_MS = SUBPROCESS_BLOCK + "selfkilltime";
	private static final String STARTUP_DELAY_MS = SUBPROCESS_BLOCK + "startupdelay";
	private static final String PROCESS_CYCLE_TIME_MS = SUBPROCESS_BLOCK + "cycletime";
	private static final String MAX_CONSECUTIVE_ERRORS = SUBPROCESS_BLOCK + "maxconsecutiveerrors";
	private static final String PROCESS_LOOKUP_RETRY_COUNT = SUBPROCESS_BLOCK + "lookupretrycount";
	private static final String CLOSE_DB_CONNECTION_IDLE_TIME_MS = SUBPROCESS_BLOCK + "idletimeclosedbconnections";
	
	private static final String PRODUCT_ADDER_BLOCK = PROPERTY_PREFIX + "productadder.";
	private static final String PRODUCT_ADDER_CLASSNAME = PRODUCT_ADDER_BLOCK + "classname";
	
	private static final String DOWNLINK_SERVICE_BLOCK = PROPERTY_PREFIX + "downlinkservice.";
	private static final String DOWNLINK_SERVICE_CLASSNAME = DOWNLINK_SERVICE_BLOCK + "classname";
	
	// MPCS-8180 07/19/16 -added arbiter block and properties under it
	// used by the PDPP arbiter and sub processes
	private static final String ARBITER_BLOCK = PROPERTY_PREFIX + "arbiter.";
	private static final String MAX_ARBITER_ERRORS = ARBITER_BLOCK + "maxerrors";
	private static final String ARBITER_HOST = ARBITER_BLOCK + "host";
	
	private static final String ACTION_CATEGORIZER_BLOCK = ARBITER_BLOCK + "actioncategorizer.";
	// MPCS-8180 07/26/16 - Added process idle kill time
	private static final String PROCESS_IDLE_KILL_TIME_MS = ACTION_CATEGORIZER_BLOCK + "processidlekilltime";
	private static final String CATEGORIZER_CYCLE_TIME_MS = ACTION_CATEGORIZER_BLOCK + "cycletime";
	private static final String CATEORIZER_PROCESS_SCRIPT = ACTION_CATEGORIZER_BLOCK + "processscript";
	// MPCS-8180 07/26/16 - Added transaction block size, process dead time, and load difference properties
	private static final String TRANSACTION_BLOCK_SIZE = ACTION_CATEGORIZER_BLOCK + "transactionblocksize";
	private static final String PROCESS_DEAD_TIME = ACTION_CATEGORIZER_BLOCK + "processdeadtime";
	private static final String LOAD_DIFFERENCE = ACTION_CATEGORIZER_BLOCK + "loaddifference";
	
	private static final String ARBITER_CHECKERS = ARBITER_BLOCK + "checkers";
	private static final String ARBITER_CHECKER_BLOCK = ARBITER_CHECKERS + ".";
	//private static final String CHECKER_ORDER = ARBITER_CHECKER_BLOCK + "order";
	
	private static final String PARALLEL_PROCESSORS_BLOCK = ARBITER_BLOCK + "parallelprocessors.";
	private static final String MAX_PROCESSOR_BACKLOG = PARALLEL_PROCESSORS_BLOCK + "backlog";
	private static final String PARALLEL_PROCESSOR_COUNT_BLOCK = PARALLEL_PROCESSORS_BLOCK + "counts.";
	
	// MPCS-8180 07/14/16 Added this block and the associated DEFAULT values
	//used by SessionFetchAndAddIfAbsent.java
	private static final String SESSION_FETCH_ADD_BLOCK = PROPERTY_PREFIX + "sessionfetchandadd.";
	private static final String SLEEP_TIME_AFTER_BLOCK = SESSION_FETCH_ADD_BLOCK + "sleepafter.";
	private static final String SLEEP_TIME_AFTER_CLOSE_MS = SLEEP_TIME_AFTER_BLOCK + "closeMS";
	private static final String SLEEP_TIME_AFTER_SESSION_STORE_MS = SLEEP_TIME_AFTER_BLOCK + "sessionstoreMS";
	private static final String RELOAD_SESSION_AFTER_CREATION_RETRIES = SESSION_FETCH_ADD_BLOCK + "reloadSessionAfterCreationRetries";
	
	//default values
	private static final int DEFAULT_MAX_QUERY_RESULTS = 20;
	private static final long DEFAULT_SELF_KILL_TIME_MS = 500000;
	private static final long DEFAULT_STARTUP_DELAY_MS = 2000;
	private static final long DEFAULT_CYCLE_TIME_MS = 5000;
	private static final int DEFAULT_MAX_CONSECUTIVE_ERRORS = 50;
	private static final int DEFAULT_PROCESS_LOOKUP_RETRY_COUNT = 40;
	private static final long DEFAULT_CLOSE_DB_CONNECTION_IDLE_TIME_MS = 30000;
	private static final int DEFAULT_MAX_ARBITER_ERRORS = 20;
	private static final int DEFAULT_MAX_PROCESSOR_BACKLOG = 500;
	private static final int DEFAULT_PARALLEL_PROCESSOR_COUNT = 1;
	// MPCS-8180 07/26/16 - added default process idle kill time
	private static final int DEFAULT_PROCESS_IDLE_KILL_TIME_MS = 30000;
	private static final String DEFAULT_CATEGORIZER_PROCESS_SCRIPT = "chill_pdpp_automation";
	// MPCS-8180 07/26/16 - Added defaults for transaction block size, process dead time, and load difference
	private static final int DEFAULT_TRANSACTION_BLOCK_SIZE = 50;
	private static final int DEFAULT_PROCESS_DEAD_TIME = 60000;
	private static final int DEFAULT_LOAD_DIFFERENCE = 200;
	private static final int DEFAULT_SLEEP_TIME_AFTER_CLOSE_MS = 5000;
	private static final int DEFAULT_SLEEP_TIME_AFTER_SESSION_STORE_MS = 200;
	private static final int DEFAULT_RELOAD_SESSION_AFTER_CREATION_RETRIES = 10;
	
	private static final String LIST_DELIMITER = ",";
	

    /**
     * Test constructor
     */
	public ProductAutomationProperties(){
        this(new SseContextFlag());
	}
	
    /**
     * Constructor for ProductAutomationProperties
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public ProductAutomationProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }

	/**
	 * Gets the value to increase or decrease when changing the number of
	 * connections
	 * 
	 * @return value to increase or decrease when changing the number of
	 *         connections
	 */
	public int getConnectionChanges(){
		return getIntProperty(CONNECTION_CHANGE_VALUE_PROPERTY, 0);
	}
	
	/**
	 * Gets the the minimum value of the number of connections
	 * 
	 * @return the minimum value of the number of connections
	 */
	public int getMinMaxConnections(){
		return getIntProperty(MIN_MAX_CONNECTION_VALUE_PROPERTY,0);
	}
	
	/**
	 * Gets the the upper bound of the number of connections
	 * 
	 * @return the upper bound of the number of connections
	 */
	public int getMaxMaxConnections(){
		return getIntProperty(MAX_MAX_CONNECTION_VALUE_PROPERTY,getMinMaxConnections());
	}
	
	/**
	 * Get if changes to the number of simultaneous database connections are
	 * allowed.
	 * 
	 * @return True if allow changes to the number of simultaneous database
	 *         connections, false otherwise
	 */
	public boolean getDoAdjustments(){
		return getBooleanProperty(DO_ADJUSTMENTS_PROPERTY,false);
	}
	
	/**
	 * Gets the maximum number of actions that can be claimed and worked on at once.
	 * 
	 * @return number of actions that can be claimed and worked on at once
	 */
	public int getMaxQueryResults(){
		return getIntProperty(MAX_QUERY_RESULTS_PROPERTY,DEFAULT_MAX_QUERY_RESULTS);
	}
	
	/**
	 * @return
	 */
	public long getSelfKillTimeMS(){
		return getLongProperty(SELF_KILL_TIME_MS, DEFAULT_SELF_KILL_TIME_MS);
	}
	
	public long getStartUpDelayMS(){
		return getLongProperty(STARTUP_DELAY_MS, DEFAULT_STARTUP_DELAY_MS);
	}
	
	public long getProcessCycleTimeMS(){
		return getLongProperty(PROCESS_CYCLE_TIME_MS, DEFAULT_CYCLE_TIME_MS);
	}
	
	public int getMaxConsecutiveErrors(){
		return getIntProperty(MAX_CONSECUTIVE_ERRORS, DEFAULT_MAX_CONSECUTIVE_ERRORS);
	}
	
	public int getProcessLookupRetryCount(){
		return getIntProperty(PROCESS_LOOKUP_RETRY_COUNT, DEFAULT_PROCESS_LOOKUP_RETRY_COUNT);
	}
	
	public long getIdleTimeToCloseDbConnectionsMS(){
		long val = getLongProperty(CLOSE_DB_CONNECTION_IDLE_TIME_MS, DEFAULT_CLOSE_DB_CONNECTION_IDLE_TIME_MS);
		
		//The amount of passing without doing any work before closing processors store controllers (db connection)
		// This can not be zero, so do a check first.
		if(val <= 0){
			val = DEFAULT_CLOSE_DB_CONNECTION_IDLE_TIME_MS;
		}
		
		return val;
	}
	
	public int getMaxArbiterErrors(){
		int val = getIntProperty(MAX_ARBITER_ERRORS, DEFAULT_MAX_ARBITER_ERRORS);
		
		// MPCS-4244 - Need an error count for the arbiter to shutdown so that it does not get into a state that it 
		// just spins forever creating errors.
		if (val <= 0){
			val = DEFAULT_MAX_ARBITER_ERRORS;
		}
		
		return val;
	}
	
	public String getArbiterHost(){
		return getProperty(ARBITER_HOST);
	}
	
	public long getActionCategorizerCycleTimeMS(){
		return getLongProperty(CATEGORIZER_CYCLE_TIME_MS, DEFAULT_CYCLE_TIME_MS);
	}
	
	//MPCS-8180 07/26/16 - Added getter method
	/**
	 * Idle time for a processor to initialize before the arbiter will kill it.
	 * @return Idle time for a processor to initialize before the arbiter will kill it.
	 */
	public int getProcessIdleKillTimeMS(){
	    return getIntProperty(PROCESS_IDLE_KILL_TIME_MS, DEFAULT_PROCESS_IDLE_KILL_TIME_MS);
	}
	
	public String getProcessScript(){
		return getProperty(CATEORIZER_PROCESS_SCRIPT,DEFAULT_CATEGORIZER_PROCESS_SCRIPT);
	}
	
	//MPCS-8180 07/26/16 - Added getter method
	/**
	 * Returns the number of transactions pulled in any given group
	 * 
	 * @return The number of transactions pulled in any given group
	 */
	public int getTransactionBlockSize(){
	    return getIntProperty(TRANSACTION_BLOCK_SIZE, DEFAULT_TRANSACTION_BLOCK_SIZE);
	}
	
	//MPCS-8180 07/26/16 - Added getter method
	/**
	 * Returns the time, in milliseconds, that a process is given to work on a
	 * particular action before it is assumed dead
	 * 
	 * @return time, in milliseconds, that a process is given to work on a
	 * particular action before it is assumed dead
	 */
	public int getProcessDeadTime(){
	    return getIntProperty(PROCESS_DEAD_TIME, DEFAULT_PROCESS_DEAD_TIME);
	}
	
	//MPCS-8180 07/26/16 - Added getter method
	/**
	 * If more than one process of a given type is running, the number of
	 * actions any product can have more than another before it is load balanced
	 * 
	 * @return The number of actions one process must have over another before
	 *         load balancing occurs
	 */
	public int getLoadDifference(){
	    return getIntProperty(LOAD_DIFFERENCE, DEFAULT_LOAD_DIFFERENCE);
	}
	
	public List<String> getCheckOrder(){
		return getListProperty(ARBITER_CHECKERS,null,LIST_DELIMITER);
	}
	
	public String getCheckerClass(final String mnemonic){
		return getProperty(ARBITER_CHECKER_BLOCK + mnemonic);
	}
	
	public int getMaxProcessorBacklog(){
		return getIntProperty(MAX_PROCESSOR_BACKLOG, DEFAULT_MAX_PROCESSOR_BACKLOG);
	}
	
	public int getParallelProcessorsCount(final String mnemonic){
		return getIntProperty(PARALLEL_PROCESSOR_COUNT_BLOCK + mnemonic, DEFAULT_PARALLEL_PROCESSOR_COUNT);
	}
	
	/**
	 * Gets the pdpp database user.
	 * 
	 * MPCS-8382  8/22/2016 - Adding PDPP db host, user and password.
	 * 
	 * @return the user, if none found "mpcs"
	 */
	public String getUser() {
		return getProperty(DATABASE_USER_PROPERTY, "mpcs");
	}

	/**
	 * Gets the password which is expected to be encrypted using chill_encrypt_password.
	 * 
	 * MPCS-8382  8/22/2016 - Adding PDPP db host and password.
	 * 
	 * @return the raw password.  If none found returns an empty string.
	 */
	public String getEncryptedPassword() {
		return getProperty(DATABASE_PASSWORD_PROPERTY, "");
	}

	/**
	 * Gets the database host 
	 * 
	 * MPCS-8382  8/22/2016 - Adding PDPP db host.
	 * 
	 * @return the database host, if not found "localhost"
	 */
	public String getDatabaseHost() {
		return getProperty(DATABASE_HOST_PROPERTY, "localhost");
	}
	
	/**
	 * Gets the name of the database utilized
	 * 
	 * @return name of the database utilized
	 */
	public String getDatabaseName(){
		return GdsSystemProperties.getSystemMission() + "_" + getProperty(DATABASE_NAME_PROPERTY);
	}
	
	/**
	 * Gets the port number to be utilized to access the database
	 * 
	 * @return port number to be used to access the database
	 */
	public String getDatabasePort(){
		return getProperty(DATABASE_PORT_PROPERTY);
	}
	
	/**
	 * Get if the cache should be utilized for the GUI
	 * 
	 * @return true if the GUI can utilize the cache, false otherwise
	 */
	public boolean getUseCacheForGui(){
		return getBooleanProperty(USE_CACHE_PROPERTY,false);
	}
	
	/**
	 * Gets the list of classes utilized to hold and represent database data sets
	 * 
	 * @return The classes that hold and represent database data sets
	 */
	public List<String> getClassesList(){
		return getListProperty(CLASSES_PROPERTY, null, LIST_DELIMITER);
	}
	
	/**
	 * Gets the max consecutive errors allowed before the handler will shut
	 * itself down
	 * 
	 * @return Max consecutive errors allowed before the handler will shut
	 *         itself down
	 */
	public int getChillDownMaxErrors(){
		return getIntProperty(MAX_ERROR_PROPERTY,10);
	}
	
	
	// MPCS-8180 07/14/16 - added getSleepTimeAfterCloseMS, getSleepTimeAfterSessionStoreMS, and getReloadSessionAfterCreationRetries
	/**
	 * Get how long to wait, in milliseconds, after closing the store controller
	 * 
	 * @return how long to wait, in milliseconds, after closing the store controller
	 */
	public int getSleepTimeAfterCloseMS(){
		return getIntProperty(SLEEP_TIME_AFTER_CLOSE_MS, DEFAULT_SLEEP_TIME_AFTER_CLOSE_MS);
	}
	
	/**
	 * Get how long to wait, in milliseconds, after storing to reload from the database
	 * 
	 * @return How long to wait, in milliseconds, after storing to reload from the database
	 */
	public int getSleepTimeAfterSessionStoreMS(){
		return getIntProperty(SLEEP_TIME_AFTER_SESSION_STORE_MS, DEFAULT_SLEEP_TIME_AFTER_SESSION_STORE_MS);
	}
	
	/**
	 * Get the number of attempts to reload a session after it has been created
	 * 
	 * @return Number of attempts to reload a session after it has been created
	 */
	public int getReloadSessionAfterCreationRetries(){
		return getIntProperty(RELOAD_SESSION_AFTER_CREATION_RETRIES, DEFAULT_RELOAD_SESSION_AFTER_CREATION_RETRIES);
	}
	
    /*
     * MPCS-9529 03/20/18
     * Removed getMatchingProperties (redundant and identical to version in GdsHierarchicalProperties) and added
     * getHibernateProperties.
     */
	/**
     * Returns a Hashmap of the Hibernate properties with the productAutomation
     * prefixes removed.
     * 
     * @return a HashMap of the Hibernate configuraiton properties
     */
    public synchronized Map<String, String> getHibernateProperties() {
		final String configRegex = HIBERNATE_CONFIG_BLOCK + "+";
		final Set<String> keySet = properties.stringPropertyNames();
        final Map<String, String> result = new HashMap<>();
		final Iterator<String> it = keySet.iterator();
		while (it.hasNext()) {
			final String key = it.next();
			if (key.matches(configRegex)) {
                result.put(key.replace(HIBERNATE_CONFIG_BLOCK, ""), properties.get(key).toString());
			}
		}
		
		return result;
	}
	
	/**
	 * Gets the name of the AutomationProductAdder class
	 * 
	 * @return the AutomationProductAdder class name
	 */
	public String getAutomationProductAdderClassName(){
		return getProperty(PRODUCT_ADDER_CLASSNAME);
	}
	
	/**
	 * Gets the name of the AutomationDownlinkService class
	 * 
	 * @return the AutomationDownlinkService class name
	 */
	public String getAutomationDownlinkServiceClassName(){
		return getProperty(DOWNLINK_SERVICE_CLASSNAME);
	}
	
	@Override
    public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}

	/**
	 * Looks for a checker that matches the name of a mnemonic
	 *
	 * @param mnemonic mnemonic to find checker for
	 * @return Whether or not the supplied mnemonic has a matching checker
	 */
	public boolean hasMappedMnemonic(final String mnemonic){
		List<String> configuredCheckers = getCheckOrder();
		return configuredCheckers != null && configuredCheckers.contains(mnemonic);
	}
}
