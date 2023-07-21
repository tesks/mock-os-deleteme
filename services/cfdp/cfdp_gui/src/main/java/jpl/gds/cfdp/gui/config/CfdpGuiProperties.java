/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.cfdp.gui.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;

/**
 * The properties to be used by CFDP when being displayed through
 * any traditional AMPCS GUI element (SWT).
 *
 */
public class CfdpGuiProperties extends GdsHierarchicalProperties {
	 /**
     * CFDP GUI properties file
     */
    protected static final String PROPERTY_FILE = "cfdp_gui.properties";
    
    private static final String PROPERTY_PREFIX = "cfdpGui.";
    
    private static final String INTERNAL_PREFIX = PROPERTY_PREFIX + "internal.";
    
    private static final String INTERNAL_UPLINK_BLOCK = INTERNAL_PREFIX + "up.";
    
    private static final String INT_FILE_UP_BLOCK = INTERNAL_UPLINK_BLOCK + "file.";

    private static final String FILE_UP_FIELD_BLOCK = INT_FILE_UP_BLOCK + "field.";
    
    private static final String MTU_BLOCK = FILE_UP_FIELD_BLOCK + "userMessage.";
    
    private static final String MTU_TITLE_PROPERTY = MTU_BLOCK + "title";

    private static final String MTU_DEFAULT_VALUE_PROPERTY = MTU_BLOCK + "default";
    
    /**
     * Constructor
     */
    public CfdpGuiProperties() {
    	super(PROPERTY_FILE, true);
    }
    
    /**
     * Get the title of the "Message to user" field
     * @return the title of the user message field
     */
    public String getUserMessageFieldTitle() {
    	return getProperty(MTU_TITLE_PROPERTY, "Message to User: ");
    }
    
    /**
     * Get the default value of the "Message to user" field
     * @return the default value of the user message field
     */
    public String getUserMessageFieldDefault() {
    	return getProperty(MTU_DEFAULT_VALUE_PROPERTY, "");
    }
    
    @Override
    public String getPropertyPrefix() {
    	return PROPERTY_PREFIX;
    }

}
