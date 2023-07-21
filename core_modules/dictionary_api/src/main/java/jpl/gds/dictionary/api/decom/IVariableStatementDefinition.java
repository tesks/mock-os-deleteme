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

/**
 * The IVariableStatementDefinition interface is to be implemented by all switch
 * statement definition objects found in IDecomMapDefinitions. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IVariableStatementDefinition object is the multi-mission representation of
 * a variable statement specification in a decommutation map. A variable
 * statement specifically directs the decom processor to extract the value of a
 * variable from the data, generally for later use in an
 * ISwitchStatementDefinition. IChannelDecomDictionary implementations must
 * parse mission-specific channel decom dictionary files and create
 * IDecomMapDefinitions with attached IVariableStatementsDefinition objects for
 * the variable statements found therein. In order to isolate the mission
 * adaptation from changes in the multi-mission core, IChannelDecomDictionary
 * implementations define a mission-specific class that implements this
 * interface. All interaction with these objects in mission adaptations should
 * use the IVariableStatementDefinition interface, rather than directly
 * interacting with the objects themselves.
 * 
 *
 *
 */
public interface IVariableStatementDefinition extends IDecomStatement {

    /**
     * Indicates whether this statement defines a variable to be extracted
     * from the data, as opposed to a variable that just references 
     * another variable.
     * 
     * @return true if the variable value must be extracted from the data, false
     *         if this variable is a reference to another one
     */
    public boolean isExtractionVariable();

    /**
     * Indicates whether the offset of the variable's value in the data is
     * specified by the decom map. If it is not specified, the decommutation
     * logic will extract the variable from the data at the current data offset.
     * Offset should not be defined if the statement defines a reference
     * variable rather than an extraction variable.
     * 
     * @return true if offset specified, false if not
     * 
     * @see #isExtractionVariable()
     */
    public boolean isOffsetSpecified();

    /**
     * Gets the name of the variable.
     * 
     * @return the name; never null
     */
    public String getVariableName();

    /**
     * Sets the name of the variable. 
     * 
     * @param var the variable name
     */
    public void setVariableName(String var);

    /**
     * Gets the name of the variable to get the value from, if this variable is
     * merely a reference to another variable.
     * 
     * @return the name of the variable this statement's variable refers to;
     *         null if this statement does not refer to another variable
     *         
     * @see #isExtractionVariable()        
     */
    public String getReferenceVariableName();

    /**
     * Sets the name of the variable to get the value from, if this variable is
     * merely a reference to another variable.
     * 
     * @param refVarName
     *            name of the variable this statement's variable refers to
     */
    public void setReferenceVariableName(String refVarName);

    /**
     * Gets the width of the variable field in bits. This is how
     * many bits will be extracted to get the varaible's value.
     * 
     * @return the variable field width in bits; will be &lt; 0 if
     *         this statement does not specify offset
     * 
     * @see #isExtractionVariable()
     * @see #isOffsetSpecified()
     */
    public int getWidthToExtract();

    /**
     * Sets the width of the variable field in bits. This is how
     * many bits will be extracted to get the variable's value.
     * 
     * @param w the field width in bits
     */
    public void setWidthToExtract(int w);

    /**
     * Gets the offset of the variable field, relative to the current offset.
     * 
     * @return the field offset in bits; will be &lt; 1 if this statement is for a
     *         reference variable rather than an extraction variable, or if no
     *         offset was supplied in the decom map
     * 
     * @see #isExtractionVariable()
     * @see #isOffsetSpecified()
     */
    public int getOffsetToExtract();

    /**
     * Sets the offset of the variable field, relative to the current offset.
     * 
     * @param o the offset in bits
     */
    public void setOffsetToExtract(int o);

}