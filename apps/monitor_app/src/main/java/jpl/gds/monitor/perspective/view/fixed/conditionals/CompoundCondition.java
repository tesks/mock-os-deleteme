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

import jpl.gds.monitor.perspective.view.fixed.ICompoundCondition;
import jpl.gds.monitor.perspective.view.fixed.ISimpleCondition;


/**
 * A compound condition is composed of 1 or more conditions joined by and, or, xor
 * 
 * CompoundCondition := Condition (AND|OR|XOR (Condition|CompoundCondition))+
 */
public class CompoundCondition implements ICompoundCondition {
    
    //LHS of the compound condition
    private ICompoundCondition leftCondition;
    
    private Operator operator = Operator.NONE;      // optional
    private ISimpleCondition rightCondition;         // optional
    
    /**
     * Constructor: takes in a condition string and parses it
     * 
     * @param conditionSet string of conditions (may contain operators and 
     *              operands)
     */
    public CompoundCondition (final String conditionSet) {
        parse(conditionSet);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void parse(final String conditionSet) {
        
        // store index of last AND OR XOR encountered in condition
        final int andIndex = conditionSet.lastIndexOf(AND_STRING);
        final int orIndex = conditionSet.lastIndexOf(OR_STRING);
        final int xorIndex = conditionSet.lastIndexOf(XOR_STRING);
        
        // there is no AND OR XOR, this is just a simple condition
        if(andIndex == -1 && orIndex == -1 && xorIndex == -1) {
            rightCondition = new SimpleCondition(conditionSet);
            return;
        }
        
        // get the last occurence of AND OR XOR
        int biggestIndex = -1;
        if(andIndex != -1) {
            biggestIndex = andIndex;  
        }
        if(orIndex != -1) {
            if(biggestIndex == -1) {
                biggestIndex = orIndex;
            } else {
                biggestIndex = Math.max(biggestIndex, orIndex);
            }
        }
        if(xorIndex != -1) {
            if(biggestIndex == -1) {
                biggestIndex = xorIndex;
            } else {
                biggestIndex = Math.max(biggestIndex, xorIndex);
            }
        }
        
        // Store the left hand side
        leftCondition = new CompoundCondition(conditionSet.substring(0, biggestIndex));
        
        // Store the operator and the right hand side of the condition
        if(conditionSet.substring(biggestIndex).startsWith(AND_STRING)) {
            operator = Operator.AND;
            rightCondition = new SimpleCondition(conditionSet.substring(biggestIndex + AND_STRING.length()));
        } else if(conditionSet.substring(biggestIndex).startsWith(OR_STRING)) {
            operator = Operator.OR;
            rightCondition = new SimpleCondition(conditionSet.substring(biggestIndex + OR_STRING.length()));
        } else {
            operator = Operator.XOR;
            rightCondition = new SimpleCondition(conditionSet.substring(biggestIndex + XOR_STRING.length()));
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(final ApplicationContext appContext) {
        switch(operator) {
        case NONE:
            return rightCondition.evaluate(appContext);
        case AND:
            return leftCondition.evaluate(appContext) && rightCondition.evaluate(appContext);
        case OR:
            return leftCondition.evaluate(appContext) || rightCondition.evaluate(appContext);
        case XOR:
            return leftCondition.evaluate(appContext) ^ rightCondition.evaluate(appContext);
        default:
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(final ApplicationContext appContext, final int stalenessInterval) {
        switch(operator) {
        case NONE:
            return rightCondition.evaluate(appContext, stalenessInterval);
        case AND:
            return leftCondition.evaluate(appContext, stalenessInterval) && rightCondition.evaluate(appContext, stalenessInterval);
        case OR:
            return leftCondition.evaluate(appContext, stalenessInterval) || rightCondition.evaluate(appContext, stalenessInterval);
        case XOR:
            return leftCondition.evaluate(appContext, stalenessInterval) ^ rightCondition.evaluate(appContext, stalenessInterval);
        default:
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Operator getOperator() {
        return operator;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ICompoundCondition getLeftCondition() {
        return leftCondition;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ISimpleCondition getRightCondition() {
        return rightCondition;
    }
    
    /**
     * Gets the string representation of a compound condition
     * 
     * @return a compound condition concatenated with an operator and a simple condition
     */
    @Override
    public String toString() {
        //NO OPERATOR
        if(operator.equals(Operator.NONE)) {
            return rightCondition.toString();
        }
        //AND
        else if(operator.equals(Operator.AND)) {
            return leftCondition.toString() + AND_STRING + rightCondition.toString();
        }
        //OR
        else if(operator.equals(Operator.OR)) {
            return leftCondition.toString() + OR_STRING + rightCondition.toString();
        }
        //XOR
        else {
            return leftCondition.toString() + XOR_STRING + rightCondition.toString();
        }
    }
}
