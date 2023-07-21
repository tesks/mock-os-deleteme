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

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;

/**
 * Evaluates datatype conditions by comparing the channel type
 */
public class DataTypeCondition extends ConditionConfiguration {
    
    /**
     * Constructor: creates a new Condition Configuration object of type 
     * Data Type Condition and sets the member variables
     * 
     * @param conditionId is the unique identifier for this condition object
     * @param channelId identifies the channel whose value is to be used for 
     *                  evaluation
     * @param value is the value to compare against (allowed values are
     *              UNKNOWN, SIGNED_INT, UNSIGNED_INT, UNSIGNED_INT, DIGITAL, 
     *              STATUS, FLOAT, ASCII, DOUBLE, BOOLEAN
     */
    public DataTypeCondition(final String conditionId, final String channelId, final String value) {
        super(conditionId, channelId, Comparison.TYPE, value);
    }

    /**
     * Performs equality check between channel id type and given type
     * 
     * @return true if type associated with channel id is the same as the 
     *         given type, false otherwise
     */
    @Override
    public boolean evaluate(final ApplicationContext appContext) {
        
        final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(channelId);
        if (def == null) {
            return false;
        }
        
        return ((def.getChannelType().toString()).equals(value) ? 
                true : false);
    }

    @Override
    public boolean evaluate(final ApplicationContext appContext, final int stalenessInterval) {
        return false;
    }
}
