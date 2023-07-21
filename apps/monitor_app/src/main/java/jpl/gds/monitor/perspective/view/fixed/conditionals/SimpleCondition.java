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
import jpl.gds.monitor.perspective.view.fixed.ISimpleCondition;
import jpl.gds.shared.log.TraceManager;

/**
 * A SimpleCondition is composed of an optional "NOT" and a Condition 
 * Configuration
 */
public class SimpleCondition implements ISimpleCondition {
    
    private static final String NOT_STRING = "NOT ";
    
    private IConditionConfiguration condition;
    private boolean isNot;
    
    /**
     * Constructor: calls the parse method which in turn sets all the member 
     *              variables
     * 
     * @param simpleCondition is made up of an optional NOT and a condition ID
     */
    public SimpleCondition(final String simpleCondition) {
        parse(simpleCondition);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void parse(String simpleCondition) {
    	//remove leading whitespace
    	simpleCondition.replaceAll("^\\s+", "");
    	
    	if(simpleCondition.startsWith(NOT_STRING)) {
    		isNot = true;
    		simpleCondition = simpleCondition.substring(NOT_STRING.length());
    	}
        
        //remove any whitespace
        simpleCondition = simpleCondition.replace(" ", "");
        
        // simpleCondition should now be a conditionID.
        // Get the Condition Configuration from the table using the ID
        condition = ConditionTable.getInstance().getCondition(simpleCondition);
        if(condition == null) {
            TraceManager.getDefaultTracer ().error

            ("Invalid condition ID specified for a fixed field element: " + simpleCondition);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(final ApplicationContext appContext) {
        return isNot ? !condition.evaluate(appContext) : condition.evaluate(appContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluateStale(final ApplicationContext appContext, final int stalenessInterval) {
        return isNot ? !condition.evaluate(appContext, stalenessInterval) : condition.evaluate(appContext, stalenessInterval);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(final ApplicationContext appContext, final int stalenessInterval) {
        if(condition.getComparison().equals(IConditionConfiguration.Comparison.STALE)) {
            return evaluateStale(appContext, stalenessInterval);
        }
        else {
            return evaluate(appContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNot() {
        return isNot;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IConditionConfiguration getConditionConfiguration() {
        return condition;
    }
    
    /**
     * Gets the string representation of a simple condition
     * 
     * @return a NOT (if present) concatenated with a conditionID
     */
    @Override
    public String toString() {
        if(isNot) {
            return NOT_STRING + condition.getConditionId();
        }
        else {
            return condition.getConditionId();
        }
        
    }
}
