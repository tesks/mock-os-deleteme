/*
 *  Copyright 2006-2018. California Institute of Technology.
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
package jpl.gds.automation.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * This class facilitates retrieval of automation application related properties
 * (including mtak) Automation properties are not accessed in the Java code,
 * therefore no property names or get functions have been added
 */
public class AutomationAppProperties extends GdsHierarchicalProperties {
	
	/** default property file name */
	public static final String PROPERTY_FILE = "automation_app.properties";
	
	private static final String PROPERTY_PREFIX = "automationApp.";
	
	private static final String MTAK_PROPERTY_PREFIX = PROPERTY_PREFIX + "mtak.";
	
	private static final String MTAK_UPLINK_BLOCK = MTAK_PROPERTY_PREFIX + "uplink.";
	private static final String MTAK_UPLINK_PREFIX_BLOCK = MTAK_UPLINK_BLOCK + "prefixes.";
	
	private static final String MTAK_DELIMITER_PROPERTY = MTAK_UPLINK_BLOCK + "delimiter";
	private static final String MTAK_TERMINATOR_PROPERTY = MTAK_UPLINK_BLOCK + "terminator";
	
	private static final String MTAK_COMMAND_PREFIX_PROPERTY = MTAK_UPLINK_PREFIX_BLOCK + "command";
	private static final String MTAK_COMMAND_LIST_PREFIX_PROPERTY = MTAK_UPLINK_PREFIX_BLOCK + "cmdlist";
	private static final String MTAK_FILE_PREFIX_PROEPRTY = MTAK_UPLINK_PREFIX_BLOCK + "file";
	private static final String MTAK_SCMF_PREFIX_PROPERTY = MTAK_UPLINK_PREFIX_BLOCK + "scmf";
	private static final String MTAK_RAW_FILE_PREFIX_PROPERTY = MTAK_UPLINK_PREFIX_BLOCK + "raw";
	private static final String MTAK_LOG_PREFIX_PROPERTY = MTAK_UPLINK_PREFIX_BLOCK + "log";
	private static final String MTAK_UPLINK_RATE_PREFIX_PROPERTY = MTAK_UPLINK_PREFIX_BLOCK + "uplinkRate";
	private static final String MTAK_SHUTDOWN_DELAY_PROPERTY = MTAK_PROPERTY_PREFIX + "shutdownDelay";

    /**
     * Default constructor. Retrieves properties from "automationApp.properties"
     * file. The standard AMPCS hierarchical property retrieval and declaration
     * will be utilized
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public AutomationAppProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
	}
	
	/**
	 * Get the Mtak delimiter character
	 * @return the Mtak delimiter character
	 */
	public String getMtakDelimiter(){
		return this.getProperty(MTAK_DELIMITER_PROPERTY,",");
	}
	
	/**
	 * Get the Mtak terminator String
	 * @return the Mtak terminator String
	 */
	public String getMtakTerminator(){
		return this.getProperty(MTAK_TERMINATOR_PROPERTY,";;;");
	}
	
	/**
	 * Get the Mtak command prefix String
	 * @return the Mtak command prefix String
	 */
	public String getMtakCommandPrefix(){
		return this.getProperty(MTAK_COMMAND_PREFIX_PROPERTY,"CMD");
	}
	
	/**
	 * Get the Mtak command list prefix String
	 * @return the Mtak command list prefix String
	 */
	public String getMtakCommandListPrefix(){
		return this.getProperty(MTAK_COMMAND_LIST_PREFIX_PROPERTY,"CMDLIST");
	}
	
	/**
	 * Get the Mtak file prefix String
	 * @return the Mtak file prefix String
	 */
	public String getMtakFilePrefix(){
		return this.getProperty(MTAK_FILE_PREFIX_PROEPRTY,"FILE");
	}
	
	/**
	 * Get the Mtak SCMF prefix String
	 * @return the Mtak SCMF prefix String
	 */
	public String getMtakScmfPrefix(){
		return this.getProperty(MTAK_SCMF_PREFIX_PROPERTY,"SCMF");
	}
	
	/**
	 * Get the Mtak raw file prefix String
	 * @return the Mtak raw file prefix String
	 */
	public String getMtakRawFilePrefix(){
		return this.getProperty(MTAK_RAW_FILE_PREFIX_PROPERTY,"RAW");
	}
	
	/**
	 * Get the Mtak  String
	 * @return the Mtak  String
	 */
	public String getMtakLogPrefix(){
		return this.getProperty(MTAK_LOG_PREFIX_PROPERTY,"LOG");
	}
	
	/**
	 * Get the Mtak uplink rate prefix String
	 * @return the Mtak uplink rate prefix String
	 */
	public String getMtakUplinkRatePrefix(){
		return this.getProperty(MTAK_UPLINK_RATE_PREFIX_PROPERTY,"UPLINKRATE");
	}

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

	/**
	 * Get shutdown delay in MS
	 * @return Shutdown delay
	 */
	public int getShutdownDelay(){
	    return this.getIntProperty(MTAK_SHUTDOWN_DELAY_PROPERTY, 200);
    }
}