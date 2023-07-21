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
package jpl.gds.monitor.perspective.view.fixed;

import org.springframework.context.ApplicationContext;

public interface IConditionConfiguration {
    
    /**
     * Enumeration for all channel fields that may have associated conditions
     *
     */
    public enum SourceField {
        /**
         * Data number
         */
        DN,
        
        /**
         * Engineering unit
         */
        EU,
        
        /**
         * Channel status
         */
        STATUS,
        
        /**
         * Raw channel value
         */
        RAW,
        
        /**
         * Channel value (is EU, Value or Raw depending on channel type)
         */
        VALUE,
        
        /**
         * Spacecraft clock
         */
        SCLK,
        
        /**
         * Spacecraft event time
         */
        SCET,
        
        /**
         * Earth receive time
         */
        ERT,
        
        /**
         * Record creation time
         */
        RCT,
        
        /**
         * Local solar time
         */
        LST;
    };
    
    /**
     * Enumeration of all the possible comparison types to be used with the 
     * conditions
     *
     */
    public enum Comparison {
        /**
         * Less than
         */
        LT,
        
        /**
         * Greater than
         */
        GT,
        
        /**
         * Less than or equal
         */
        LE,
        
        /**
         * Greater than or equal
         */
        GE,
        
        /**
         * Equal
         */
        EQ,
        
        /**
         * Not equal
         */
        NE,
        
        /**
         * Set
         */
        SET,
        
        /**
         * Not set
         */
        NOT_SET,
        
        /**
         * Stale data
         */
        STALE,
        
        /**
         * Type comparison
         */
        TYPE,
        
        /**
         * Nullity check
         */
        IS_NULL;
    };

    /**
     * XML channel condition element name
     */
    public static final String CONDITION_TAG = "ChannelCondition";
    /**
     * XML condition ID attribute name
     */
    public static final String CONDITION_ID_TAG = "conditionID";
    /**
     * XML channel ID attribute name
     */
    public static final String CHANNEL_ID_TAG = "channelID";
    /**
     * XML source field attribute name
     */
    public static final String SOURCEFIELD_TAG = "sourceField";
    /**
     * XML comparison attribute name
     */
    public static final String COMPARISON_TAG = "comparison";
    /**
     * XML channel value attribute name
     */
    public static final String VALUE_TAG = "value";

    /**
     * Retrieves the XML perspective representation of this condition object.
     * 
     * @return XML text
     */
    public String toXML();

    /**
     * Retrieves the XML representation of the attributes for this condition object
     * 
     * @param toAppend condition configuration XML will be appended to this buffer
     */
    public void getAttributeXML(StringBuilder toAppend);

    /**
     * Sets the unique condition id
     * 
     * @param conditionId the conditionID for this comparison
     */
    public void setConditionId(String conditionId);

    /**
     * Gets the unique condition id
     * 
     * @return condition ID
     */
    public String getConditionId();

    /**
     * Sets the channel id for the condition
     * 
     * @param channelId identifies the channel whose value is to be used for
     *              evaluation
     */
    public void setChannelId(String channelId);

    /**
     * Gets the channel id for the condition
     * 
     * @return the channel ID whose value is to be used for
     *              evaluation
     */
    public String getChannelId();

    /**
     * Sets the source field that will be compared in the condition
     * 
     * @param source is the channel field that will be used in the comparison
     */
    public void setSource(SourceField source);

    /**
     * Gets the source for the condition
     * 
     * @return source is the channel field that will be used in the comparison
     */
    public SourceField getSource();

    /**
     * Sets the comparison operator
     * 
     * @param comparison is the compare operator
     */
    public void setComparison(Comparison comparison);

    /**
     * Gets the comparison operator
     * 
     * @return comparison is the compare operator
     */
    public Comparison getComparison();

    /**
     * Sets the value
     * 
     * @param value is the static operand in the comparison
     */
    public void setValue(String value);

    /**
     * Gets the value
     * 
     * @return value is the static operand in the comparison
     */
    public String getValue();

    /**
     * Performs logical evaluation of the condition
     * @param appContext current application context
     * 
     * @return true if condition evaluates to be true, false otherwise
     */
    public boolean evaluate(ApplicationContext appContext);

    /**
     * Performs logical evaluation of the condition w/ stale interval
     * @param appContext current application context
     * @param stalenessInterval time that needs to pass for a channel to become stale
     * @return true if condition evaluates to be true, false otherwise
     */
    public boolean evaluate(ApplicationContext appContext, int stalenessInterval);

    /**
     * Sets the view configuration.
     * 
     * @param viewConfig view configuration to set
     */
    public void setViewConfig(IFixedLayoutViewConfiguration viewConfig);

}