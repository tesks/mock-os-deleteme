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

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * The IRepeatCommandArgument is the interface class used for interacting
 * with a repeat argument and the arguments that it contains.
 * 
 * MPCS-10473 - 04/05/19 - Updated to reflect the changes in FlightCommand, which now
 * "owns" the arguments and does not let them be used freely. repeat arguments act similarly and
 * own the arguments that they contain.
 *
 * MPCS-10745 - 06/30/19 - removed functions to set and get sub argument value bit strings
 */
public interface IRepeatCommandArgument extends ICommandArgument {
    
    /**
     * The string used in most repeat arguments to separate each value
     */
    String SEPARATOR_STRING = ",";

    /**
     * Set the value for a specific argument in this FlightCommand
     * @param index the index of the ICommandArgument to be set
     * @param value the String value to be set in the argument
     */
    void setArgumentValue(int index, String value);

    /**
     * Set all of the argument values with a single string
     * @param valueString the String value of all of the arguments
     */
    void setArgumentValues(String valueString);
    
    /**
     * Get the ICommandArgumentDefinition for an ICommandArgument in this FlightCommand
     * @param index the index of the argument to be queried
     * @return the ICommandArgumentDefinition of the specified argument
     */
    ICommandArgumentDefinition getArgumentDefinition(int index);
    
    /**
     * Get the specified ICommandArgument value as a string
     * @param index the index of the command argument to be checked
     * @return the String representation of the argument's value
     */
    String getArgumentValue(int index);
    
    /**
     * Get the ICommandEnumerationValue of the specified argument
     * @param index the index of the IEnumeratedCommandArgument to be queried
     * @return the ICommnadEnumerationValue of the specified argument
     */
    ICommandEnumerationValue getArgumentEnumValue(int index);
    
    /**
     * Clear the user-specified value for the specified argument
     * @param index the index for the argument to have its user-specified value cleared
     */
    void clearArgumentValue(int index);
    
    /**
     * Adds an additional set of arguments to this IRepeatCommandArgument object as
     * defined in its IRepeatCommandArgumentDefinition.
     * 
     * @param index the index at which the argument set is to be added
     * 
     * @throws IllegalArgumentException when the supplied index would
     * insert the set of arguments in the middle of another set (the index is not a
     * multiple of the number of arguments present)
     */
    void addRepeatArgumentSet(int index);
    
    /**
     * Removes ones set of arguments form this IRepeatCommandArgument object as defined
     * in its IRepeatCommandArgumentDefintion.
     * 
     * @param index the index at which the argument set is to be deleted
     * 
     * @throws IllegalArgumentException when the supplied index would delete a set
     * of arguments in the middle of another set (the index is not a multiple of the
     * number of arguments present) or any argument to be deleted does not have a definition
     * that matches what is present in the IRepeatCommandArgumentDefinition
     */
    void removeRepeatArgumentSet(int index);
    
    /**
     * Return the number of valued arguments input for this repeat arg. This
     * value is approximately (number of repeats)*getDictionaryArgumentCount.
     * 
     * @param ignoreFillArguments
     *            False if fill arguments should be included in the count, true
     *            otherwise
     * 
     * @return The integer number of valued arguments
     */
    int getValuedArgumentCount(final boolean ignoreFillArguments);
    
    /**
     * Get a String representation of this argument with all its sub-argument
     * values separated by commas.
     * 
     * @return The string representation of this argument
     */
    String getArgumentString();

    /**
     * Get the String representation of this argument with all sub-arguments
     * values separated by commas in a format that is usable for uplink (CTS) purposes
     * @return
     */
    String getUplinkString();
    
    /**
     * Check if the current argument is valid for an ICommandArgument.
     * A value is valid if it can be transmitted and is a valid value or within any valid ranges.
     * @param index the index of the command argument to perform the check
     * @return TRUE if the current value is valid for the argument, FALSE otherwise
     */
    boolean isArgumentValueValid(int index);
    
    /**
     * Check if the current argument can be transmitted for an ICommandArgument.
     * The returned value of this function does not indicate the validity of the value.
     * @param index the index of the command argument to perform the check
     * @return TRUE if the current value can be transmitted as a value of this 
     */
    boolean isArgumentValueTransmittable(int index);
    
    /**
     * A simple function for trying to determine what name to display in the
     * Command Builder for this particular argument. This function is generally
     * used to populate the "Prefix Control".
     * 
     * Currently we try the dictionary name, then the FSW name and then
     * "Unknown" if neither of those are set.
     * 
     * @param index the index of the argument to be queried
     * 
     * @return A String containing the name of this argument for displaying to
     *         the end user.
     */
    String getArgumentDisplayName(int index);
    
    /**
     * Get the string used by this repeat command to separate each argument value.
     * 
     * This function is generally the better way of getting the separator string as
     * the type of separator string can change depending upon the class implementing
     * the IRepeatCommandArgument interface.
     * 
     * @return the string that is used to separate arguments
     * 
     */
    default String getSeparatorString() {
        return SEPARATOR_STRING;
    }
}