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

public interface ICompoundCondition {
    
    /**
     * Enumeration of possible operators
     *
     */
    public enum Operator {
        /**
         * And operator
         */
        AND,
        
        /**
         * Or operator
         */
        OR,
        
        /**
         * Xor operator
         */
        XOR,
        
        /**
         * no operator
         */
        NONE;
    };

    /**
     * Boolean "and" string representation used for parsing conditions
     */
    public static final String AND_STRING = " AND ";
    /**
     * Boolean "or" string representation used for parsing conditions
     */
    public static final String OR_STRING = " OR ";
    /**
     * Boolean "xor" string representation used for parsing conditions
     */
    public static final String XOR_STRING = " XOR ";

    /**
     * Stores condition objects and operators
     * 
     * @param conditionSet is the string that will be parsed
     */
    public void parse(String conditionSet);

    /**
     * Recursive method: base case is when there is no operator
     * Evaluates the condition in the context of any surrounding ANDs, ORs, XORs
     * 
     * @return true if evaluates to true, false otherwise
     */
    public boolean evaluate(ApplicationContext appContext);

    /**
     * Recursive method: base case is when there is no operator
     * Evaluates the condition in the context of any surrounding ANDs, ORs, XORs
     * 
     * @param stalenessInterval is true if element is stale, false otherwise
     * @return true if evaluates to true, false otherwise
     */
    public boolean evaluate(ApplicationContext appContext, int stalenessInterval);

    /**
     * Gets the operator associated with this condition
     * 
     * @return AND, OR, XOR or NONE
     */
    public Operator getOperator();

    /**
     * Gets the left hand side of this condition which is a compound condition
     * 
     * @return a compound condition made up of 1 or more conditions
     */
    public ICompoundCondition getLeftCondition();

    /**
     * Gets the right hand side of this condition
     * 
     * @return a simple condition which is optional
     */
    public ISimpleCondition getRightCondition();

}