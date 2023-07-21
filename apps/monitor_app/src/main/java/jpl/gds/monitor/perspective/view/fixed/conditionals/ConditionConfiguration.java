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
package jpl.gds.monitor.perspective.view.fixed.conditionals;

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.fixed.IConditionConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;

/**
 * Configuration class for conditions which are tied to channels
 *
 */
public abstract class ConditionConfiguration implements IConditionConfiguration {
    
    //no alarm_state
  
    
    /**
     * Unique identifier for this condition object
     */
    protected String conditionId;
    
    /**
     * The channel whose value is used for the evaluation of the condition
     */
    protected String channelId;
    
    /**
     * The source field in the channel to examine
     */
    protected SourceField source;
    
    /**
     * The comparison operator
     */
    protected Comparison comparison;
    
    /**
     * The value to compare against for binary and alarm conditions (not unary)
     */
    protected String value;


    /** Current view configuration */
    protected IFixedLayoutViewConfiguration viewConfig;

    
    /**
     * Constructor: creates a new Condition object
     * 
     * @param conditionId is the unique identifier for this condition object
     * @param channelId identifies the channel whose value is to be used for 
     *                  evaluation
     * @param source is the source field in the channel to examine
     * @param comparison is the comparison operator
     * @param value is the value to compare against
     */
    public ConditionConfiguration(final String conditionId, final String channelId, 
            final SourceField source, final Comparison comparison, final String value) {
        this.conditionId = conditionId;
        this.channelId = channelId;
        this.source = source;
        this.comparison = comparison;
        this.value = value;
    }
    
    /**
     * Default constructor
     */
    public ConditionConfiguration() {
        
    }
    
    /**
     * Constructor: creates a new Condition object w/out the source variable
     * 
     * @param conditionId the unique identifier for this condition object
     * @param channelId identifies the channel whose value is to be used for
     *              evaluation
     * @param comparison the comparison operator
     * @param value the value to compare against
     */
    public ConditionConfiguration(final String conditionId, final String channelId, 
            final Comparison comparison, final String value) {
        this.conditionId = conditionId;
        this.channelId = channelId;
        this.comparison = comparison;
        this.value = value;
    }
    
    /**
     * Constructor: creates a new Condition object w/out the source and value 
     *              variables
     * 
     * @param conditionId the unique identifier for this condition object
     * @param channelId identifies the channel whose value is to be used for
     *              evaluation
     * @param comparison the comparison operator
     */
    public ConditionConfiguration(final String conditionId, final String channelId, 
            final Comparison comparison) {
        this.conditionId = conditionId;
        this.channelId = channelId;
        this.comparison = comparison;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toXML() {
        final StringBuilder result = new StringBuilder();
        result.append("   <" + CONDITION_TAG + " ");
        getAttributeXML(result);
        result.append("/>\n");
        return result.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getAttributeXML(final StringBuilder toAppend) {
        if (conditionId != null) {
            toAppend.append(CONDITION_ID_TAG + "=\"" + conditionId + "\" ");
        }
        if (channelId != null) {
            toAppend.append(CHANNEL_ID_TAG + "=\"" + channelId + "\" ");
        }
        if (source != null) {
            toAppend.append(SOURCEFIELD_TAG + "=\"" + source.toString() + "\" ");
        }
        if (comparison != null) {
            toAppend.append(COMPARISON_TAG + "=\"" + comparison.toString() + "\" ");
        }
        if(value != null) {
            toAppend.append(VALUE_TAG + "=\"" + value + "\" ");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setConditionId(final String conditionId) {
        this.conditionId = conditionId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getConditionId() {
        return conditionId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setChannelId(final String channelId) {
        this.channelId = channelId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getChannelId() {
        return channelId;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setSource(final SourceField source) {
        this.source = source;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public SourceField getSource() {
        return source;
    }   

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComparison(final Comparison comparison) {
        this.comparison = comparison;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Comparison getComparison() {
        return comparison;
    } 

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(final String value) {
        this.value = value;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue() {
        return value;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean evaluate(ApplicationContext appContext);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean evaluate(ApplicationContext appContext, int stalenessInterval);


    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewConfig(final IFixedLayoutViewConfiguration viewConfig) {
    	this.viewConfig = viewConfig;
    }
}
