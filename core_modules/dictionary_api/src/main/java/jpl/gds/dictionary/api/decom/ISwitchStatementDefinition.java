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
package jpl.gds.dictionary.api.decom;

import java.util.List;

/**
 * The ISwitchStatementDefinition interface is to be implemented by all switch
 * statement definition objects found in IDecomMapDefinitions.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An ISwitchStatementDefinition object is the multi-mission representation of a
 * switch statement specification in a decommutation map. A switch statement
 * specifically directs the decom processor to execute alternate paths through
 * the data based upon a variable value. IChannelDecomDictionary implementations
 * must parse mission-specific channel decom dictionary files and create
 * IDecomMapDefinitions with attached ISwitchStatementsDefinition objects for
 * the switch statements found therein. In order to isolate the mission
 * adaptation from changes in the multi-mission core, IChannelDecomDictionary
 * implementations define a mission-specific class that implements this
 * interface. All interaction with these objects in mission adaptations should
 * use the ISwitchStatementDefinition interface, rather than directly
 * interacting with the objects themselves.
 * 
 *
 */
public interface ISwitchStatementDefinition extends IDecomStatement, IStatementContainer {

    /**
     * Adds a pending case value to the switch. From the time this method is
     * invoked until endPendingCases() is invoked, any IDecomStatement added to
     * the pending statement list with the addStatement() method applies to
     * cases started here. It is possible to invoke this method multiple times,
     * in order to establish multiple case values that execute the same
     * statement list. Only statements added after this call and before the
     * endPendingCases() call apply to the pending cases started using this
     * method.
     * 
     * @param c
     *            the unsigned integer case value to add
     *            
     * @see #addStatement(IDecomStatement)
     * @see #endPendingCases()
     */
    public void startPendingCase(long c);
    
    /**
     * Ends parsing (finalizes) all pending non-default cases. Terminates the
     * pending statement list and assigns all pending statements added via
     * addStatement() to the pending cases added via startPendingCase(). Clears
     * the pending case list and the pending statement list. Will produce an
     * error if any of the pending cases have already been finalized.
     */
    public void endPendingCases();

    /**
     * Indicates whether the switch has any defined cases. Applies only to
     * final, not pending, cases. Does not include the default case.
     * 
     * @return true if the switch has cases, false if not
     */
    public boolean hasCases();

    /**
     * Gets the name of the switch variable.
     * 
     * @return variable name; never null
     */
    public String getVariableToSwitchOn();

    /**
     * Sets the name of the switch variable.
     * 
     * @param varName the name of the switch variable
     */
    public void setVariableToSwitchOn(String varName);

    /**
     * Gets the list of statements in this switch for a specified case value. if
     * the case value is not one of the valid cases, and a default case was
     * defined, the default statement list is returned.
     * 
     * @param value
     *            the case value
     * @return the list of IDecomStatement objects under the case
     */
    public List<IDecomStatement> getStatementsToExecute(long value);

    /**
     * Gets the list of all valid cases values (excluding the default case).
     * 
     * @return list of case values; may be empty but never null
     * 
     */
    public List<Long> getCaseValues();

    /**
     * Starts the default case for the switch. This case is pending until
     * endDefaultCase() is invoked.
     */
    public void startDefaultCase();

    /**
     * Ends parsing (finalizes) the default case. Terminates the pending statement
     * list and assigns all pending statements added via addStatement() to
     * the pending cases added via startCase(). Clears the pending case list
     * and the pending statement list. 
     */
    public void endDefaultCase();

    /**
     * Specifically gets the list of statements for the default case.
     * 
     * @return a non-modifiable list of IDecomStatement, or null if no default case defined
     * 
     */
    public List<IDecomStatement> getDefaultStatements();
    
    /**
     * Sets the case modulus value. The value of the variable used 
     * for the switch will be modulo'd by this value.
     * 
     * @param modulus modulus value, unsigned integer
     */
    public void setModulus(int modulus);
    
    /**
     * Gets the case modulus value. The value of the variable used 
     * for the switch will be modulo'd by this value.
     * 
     * @return modulus value, unsigned integer
     */
    public int getModulus();

}