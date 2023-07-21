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
package jpl.gds.tc.api.command.args;

import jpl.gds.dictionary.api.command.ICommandEnumerationValue;

/**
 * The interface for Enumerated commands. The functions specified by this interface allow the enumeration value to be
 * retrieved and set.
 *
 */
public interface IEnumeratedCommandArgument extends ICommandArgument {
	
	/**
     * Accessor for the argument value.
     * 
     * @return The current value of this argument
     * 
     *  11/13/13 - MPCS-5521. Changed return type to use interface type.
     */
    ICommandEnumerationValue getArgumentEnumValue();
    
    /**
     * Mutator for the argument value.
     * 
     * @param lookupValue
     *            The new value for this argument
     * 
     * 11/13/13 - MPCS-5521. Changed argument to use interface type.
     */
    void setArgumentEnumValue(final ICommandEnumerationValue lookupValue);
}
