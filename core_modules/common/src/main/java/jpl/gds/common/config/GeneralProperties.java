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
package jpl.gds.common.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.util.HostPortUtility;

/**
 * This is a configuration properties class for general, cross-cutting configuration
 * values that are not associated with any specific module or sub-project.
 * 
 *
 * @since R8
 *
 */
public class GeneralProperties extends GdsHierarchicalProperties {

    /**
     * Name of the default properties file.
     */
    public static final String PROPERTY_FILE = "general.properties";
    
    private static final String PROPERTY_PREFIX = "general.";
    
    private static final String HELP_ADDRESS_PROPERTY = PROPERTY_PREFIX + "helpAddress";
    private static final String BUG_ADDRESS_PROPERTY = PROPERTY_PREFIX + "bugAddress";
    private static final String ENABLE_RT_PUB_PROPERTY = PROPERTY_PREFIX + "enableRealtimePublishing";
    private static final String USER_LIB_DIR_PROPERTY = PROPERTY_PREFIX + "userJarDirectory";
    private static final String TIME_COMPARISON_PROPERTY = PROPERTY_PREFIX + "timeComparisonStrategy";
    
    /**
     * Name of the system variable that specifies which time system to use.
     */
    private static final String TIME_STRATEGY_SYS_VAR = "GdsTimeComparisonStrategy";
    
    private static final String CONTEXT_BLOCK = PROPERTY_PREFIX + "context.";
    
    private static final String CONTEXT_HEARTBEAT_INTERVAL_PROPERTY = CONTEXT_BLOCK + "heartbeatInterval";

    private static final long CONTEXT_DEFAULT_HEARTBEAT_INTERVAL = 5000;

    private static final String DEFAULT_CONFIG_FILE_NAME = "TempTestConfig.xml";

    private static final String DEFAULT_FILE_NAME_PROPERTY = CONTEXT_BLOCK + "defaultConfigFile";
    
    /**
     * Test constructor
     */
    public GeneralProperties() {
        this(new SseContextFlag());
    }

    /**
     * Constructor that reads the default property file, which will be found using
     * the standard configuration search.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public GeneralProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }
    
    /**
     * Gets the configured e-mail address for AMPCS help.
     * @return e-mail address
     */
    public String getHelpAddress() {
        return getProperty(HELP_ADDRESS_PROPERTY, "ampcs_help@list.jpl.nasa.gov");
    }
    
    
    /**
     * Gets the configured e-mail address for AMPCS bug reports.
     * @return bug tracking system URL address
     */
    public String getBugAddress() {
        return getProperty(BUG_ADDRESS_PROPERTY, "https://jira1.jpl.nasa.gov:8443");
    }
    
    /**
     * Gets the configuration flag indicating whether messaging is globally enabled.
     * 
     * @return true if realtime messaging enabled, false if not
     */
    public boolean getUseRealtimePublicationDefault() {
        return getBooleanProperty(ENABLE_RT_PUB_PROPERTY, true);
    }
    
    
    /**
     * Gets the name of the configured user jar subdirectory.
     * 
     * @return subdirectory name, relative to GdsUserConfigDir system property
     */
    public String getUserJarDirectory() {
        return getProperty(USER_LIB_DIR_PROPERTY, "lib");
    }
      
   
    
    /**
     * Gets the default time comparison strategy for all applications.
     * 
     * @return TimeComparisonStrategy
     */
    public TimeComparisonStrategy getDefaultTimeComparisonStrategy() {
        
        TimeComparisonStrategy strategy = TimeComparisonStrategy.LAST_RECEIVED;
        
        final String f = GdsSystemProperties.getSystemProperty(TIME_STRATEGY_SYS_VAR);
        try {
            if (f != null && !f.isEmpty()) {
                strategy = TimeComparisonStrategy.valueOf(f);
                return strategy;
            } 
        } catch (final IllegalArgumentException e) {
            TraceManager.getDefaultTracer().warn("System variable " +  TIME_STRATEGY_SYS_VAR + 

                    " is set to invalid value: " + f + ". Defaulting to configured value");
            
        }
        
        final String val = getProperty(TIME_COMPARISON_PROPERTY, TimeComparisonStrategy.LAST_RECEIVED.name());
        try {
            strategy = TimeComparisonStrategy.valueOf(val);
        } catch (final IllegalArgumentException e) {
            TraceManager.getDefaultTracer().debug("Message provider setting " + val + " found in " + PROPERTY_FILE + " is invalid; defaulting to " + strategy);

        }
        return strategy;
    }
    
    /**
     * Gets the context heartbeat interval.
     * 
     * @return heartbeat interval, milliseconds
     */
    public long getContextHeartbeatInterval() {
        return getLongProperty(CONTEXT_HEARTBEAT_INTERVAL_PROPERTY, CONTEXT_DEFAULT_HEARTBEAT_INTERVAL);
    }
    

    /**
     * Returns a host-unique default context configuration file name (without
     * path) by prepending the local host name to the value in the configuration.
     * 
     * @return context configuration file name, no path
     */
    public String getDefaultContextConfigFileName() {
        return (HostPortUtility.getLocalHostName() + "_" + 
                getProperty(DEFAULT_FILE_NAME_PROPERTY,
                        DEFAULT_CONFIG_FILE_NAME));
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}

