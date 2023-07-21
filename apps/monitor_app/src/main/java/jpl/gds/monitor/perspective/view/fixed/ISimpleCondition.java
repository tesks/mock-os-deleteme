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

public interface ISimpleCondition {

    /**
     * Takes in a string and finds the associated Condition Configuration.  
     * It may or may not have a NOT.
     * 
     * @param simpleCondition is a conditionId with an optional NOT prepended
     */
    public void parse(String simpleCondition);

    /**
     * Calls the evaluation() method specific to the Condition Configuration
     * (may be relational, datatype or alarm)
     * 
     * Applies the NOT if this condition has it.
     * 
     * @return true if evaluates to true, false otherwise
     */
    public boolean evaluate(ApplicationContext appContext);

    /**
     * Calls the evaluation() method specific to the Condition Configuration
     * (may be relational, datatype or alarm)
     * 
     * Applies the NOT if this condition has it.
     * 
     * @param stalenessInterval is the length of time an element goes w/out 
     *                          updates before being considered stale
     * @return true if evaluates to true, false otherwise
     */
    public boolean evaluateStale(ApplicationContext appContext, int stalenessInterval);

    /**
     * Calls the evaluation() method specific to the Condition Configuration
     * (may be relational, datatype, alarm or stale)
     * 
     * Applies the NOT if this condition has it.
     * 
     * @param stalenessInterval is the length of time an element goes w/out 
     *                          updates before being considered stale
     * @return true if evaluates to true, false otherwise
     */
    public boolean evaluate(ApplicationContext appContext, int stalenessInterval);

    /**
     * Determines if this condition has a NOT prefix
     * 
     * @return true if this condition has a NOT, false otherwise
     */
    public boolean hasNot();

    /**
     * Gets the configuration for this condition which contains the comparison 
     * type, etc.
     * 
     * @return the condition configuration associated with this condition
     */
    public IConditionConfiguration getConditionConfiguration();

}