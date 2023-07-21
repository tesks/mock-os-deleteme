/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.api.command;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;
import jpl.gds.shared.template.Templatable;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.CommandParseException;

/**
 * The IFlightCommand is the interface class that dictates how a command that will be radiated to a spacecraft
 * and the arguments it contains are to be interacted with.
 *
 * MPCS-10473 - 04/05/19 - Updated significantly. An IFlightCommand object now maintains posession
 * of arguments and does not allow them to be passed around. Functions for retrieving the arguments have been
 * removed and functions to interact with the arguments through the IFlightCommand have been added.
 *
 * MPCS-10473  - 04/05/19 - removed the IBinaryRepresentable interface. This functionality was already present
 * in UplinkInputParser. That implementation included validation and is more easily overriden if necessary.
 *
 * MPCS-10745 - 06/360/19 - removed functions to get or set arguments as bit string, removed
 * getOpcodeBits and toBitString for IFlightCommand
 */

public interface IFlightCommand extends ICommand, Templatable {

    ApplicationContext getApplicationContext();

    /**
     * Retrieves the dictionary definition object for this command.
     * 
     * @return the definition object
     */
    ICommandDefinition getDefinition();

    /**
     * Sets the dictionary definition object for this command.
     * 
     * @param definition
     *            the definition to set
     */
    void setDefinition(ICommandDefinition definition);

    /**
     * Clears the user-specified values for all command arguments.
     */
    void clearArgumentValues();
    
    /**
     * Clear the user-specified value for the specified argument
     * @param index the index for the argument to have its user-specified value cleared
     */
    void clearArgumentValue(int index);
    
    /**
     * Clear the user-specified value for the specified argument in a repeated argument
     * @param index the index for the IRepeatCommandArgument
     * @param subIndex the index for the argument in the IRepeatCommandArgument to have its user-specified value cleared
     */
    void clearArgumentValue(int index, int subIndex);
    
    /**
     * Get the number of arguments currently in this flight command
     * @return the the number of arguments present
     */
    int getArgumentCount();
    
    /**
     * Get the CommandArgumentType of the argument at the supplied index
     * @param index the index of the argument to be queried
     * @return the CommandArgumentType of the argument in question
     */
    CommandArgumentType getArgumentType(int index);
    
    /**
     * Get the CommandArgumentType of the argument at the supplied indecies
     * @param index the index of the repeat argument to be queried
     * @param subIndex the index of the argument in the repeat argument to be queried
     * @return the CommandArgumentType of the argument in question
     */
    CommandArgumentType getArgumentType(int index, int subIndex);
    
    /**
     * Get the number of repeat arguments present in a repeat argument
     * @param index the index of the repeat argument to be checked
     * @return the number of repeat arguments present
     */
    int getArgumentCount(int index);
    
    /**
     * Get the ICommandArgumentDefinition for an ICommandArgument in this FlightCommand
     * @param index the index of the argument to be queried
     * @return the ICommandArgumentDefinition of the specified argument
     */
    ICommandArgumentDefinition getArgumentDefinition(int index);
    
    /**
     * Get the ICommandArgumentDefinition for an ICommandArgument in an IRepeatCommandArgument
     * in this FlightCommand
     * @param index the index of the IRepeatCommandArgument to be queried
     * @param subIndex the index of the ICommandArgument in the repeat argument to be queried
     * @return the ICommandArgumentDefinition of the specified argument
     */
    ICommandArgumentDefinition getArgumentDefinition(int index, int subIndex);
    
    /**
     * Get the IRepeatCommandArgumentDefinition for an IRepeatCommandArgument in this FlightCommand.
     * An error will be encountered if this function is called on an argument that is not a repeat argument.
     * @param index the index of the IRepeatCommandARgument to be queried
     * @return the IRepeatCommandArgumentDefinition of the specified repeat argument
     */
    IRepeatCommandArgumentDefinition getRepeatArgumentDefinition(int index);

    /**
     * Set the value for a specific argument in this FlightCommand
     * @param index the index of the ICommandArgument to be set
     * @param value the String value to be set in the argument
     */
    void setArgumentValue(int index, String value);
    
    /**
     * Set the value for a specific argument of a repeat argument in this FlightCommand
     * @param index the index of the IRepeatCommandArgument to be set
     * @param subIndex the index in the repeat argument of the ICommandArgument to be set
     * @param value the String value to be set in the argument
     */
    void setArgumentValue(int index, int subIndex, String value);

    /**
     * Set the values for all arguments in a repeat argument
     * @param index the index of the IRepeatCommandArgument to be set
     * @param values the String value to be set in the arguments
     */
    void setArgumentValues(int index, String values);
    
    /**
     * Get the ICommandEnumerationValue of the specified argument
     * @param index the index of the IEnumeratedCommandArgument to be queried
     * @return the ICommnadEnumerationValue of the specified argument
     */
    ICommandEnumerationValue getArgumentEnumValue(int index);
    
    /**
     * Get the ICommandEnumerationValue of the specified argument in a repeat argument
     * @param index the index of the IRepeatCommandArgument with the argument to be queried
     * @param subIndex the index of the IEnumeratedCommandArgument to be queried
     * @return the ICommnadEnumerationValue of the specified argument
     */
    ICommandEnumerationValue getArgumentEnumValue(int index, int subIndex);
    
    /**
     * Check if the current argument is valid for an ICommandArgument.
     * A value is valid if it can be transmitted and is a valid value or within any valid ranges.
     * @param index the index of the command argument to perform the check
     * @return TRUE if the current value is valid for the argument, FALSE otherwise
     */
    boolean isArgumentValueValid(int index) ;
    
    /**
     * Check if a singular argument within a repeat argument is valid for an ICommandArgument.
     * A value is valid if it can be transmitted and is a valid value or within any valid ranges.
     * @param index the index of the repeat command argument containing the argument to be checked
     * @param subIndex the index of the command argument to perform the check
     * @return TRUE if the current value is valid for the argument, FALSE otherwise
     */
    boolean isArgumentValueValid(int index, int subIndex);
    
    /**
     * Check if the current argument can be transmitted for an ICommandArgument.
     * The returned value of this function does not indicate the validity of the value.
     * @param index the index of the command argument to perform the check
     * @return TRUE if the current value can be transmitted as a value of this 
     */
    boolean isArgumentValueTransmittable(int index);
    
    /**
     * Check if a singular argument within a repeat argument can be transmitted for an ICommandArgument.
     * The returned value of this function does not indicate the validity of the value.
     * @param index the index of the command argument to perform the check
     * @param subIndex the index of the command argument to perform the check
     * @return TRUE if the current value can be transmitted as a value of this 
     */
    boolean isArgumentValueTransmittable(int index, int subIndex);
    
    /**
     * Get if the value of an ICommandArgument is input by the user
     * @param index the index of the command argument to be checked
     * @return TRUE if the argument value is input by the user, FALSE otherwise
     */
    boolean isUserEntered(int index);
    
    /**
     * Get the specified ICommandArgument value as a string
     * @param index the index of the command argument to be checked
     * @return the String representation of the argument's value
     */
    String getArgumentValue(int index);
    
    /**
     * Get a single ICommandArgument value from an IRepeatCommandArgument 
     * @param index the index of the repeat argument with the argument to be checked
     * @param subIndex the index of the command argument to be checked
     * @return the String representation of the argument's value
     */
    String getArgumentValue(int index, int subIndex);

    /**
     * Get a String representation of the specified repeat argument with all its sub-argument
     * values separated by commas.
     * @param index the index of the repeat argument to be retrieved
     * @return The string representation of the specified repeat argument
     */
    String getRepeatArgumentString(int index);
    
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
     * A simple function for trying to determine what name to display in the
     * Command Builder for this particular argument. This function is generally
     * used to populate the "Prefix Control".
     * 
     * Currently we try the dictionary name, then the FSW name and then
     * "Unknown" if neither of those are set.
     * 
     * @param index the index of the repeat argument to be queried
     * @param subIndex the index of the individual argument to be queried
     * 
     * @return A String containing the name of this argument for displaying to
     *         the end user.
     */
    String getArgumentDisplayName(int index, int subIndex);
    
    /**
     * Adds an additional set of arguments to this IRepeatCommandArgument object as
     * defined in its IRepeatCommandArgumentDefinition.
     * 
     * @param index the index of the repeat argument to be augmented
     * @param subIndex the index at which the argument set is to be added
     * 
     * @throws IllegalArgumentException when the supplied index would
     * insert the set of arguments in the middle of another set (the index is not a
     * multiple of the number of arguments present)
     */
    void addRepeatArguments(int index, int subIndex);
    
    
    /**
     * Removes ones set of arguments form this IRepeatCommandArgument object as defined
     * in its IRepeatCommandArgumentDefintion.
     * 
     * @param index the index of the repeat argument to be augmented
     * @param subIndex the index at which the argument set is to be deleted
     * 
     * @throws IllegalArgumentException when the supplied index would delete a set
     * of arguments in the middle of another set (the index is not a multiple of the
     * number of arguments present) or any argument to be deleted does not have a definition
     * that matches what is present in the IRepeatCommandArgumentDefinition
     */
    void removeRepeatArguments(int index, int subIndex);

    /**
     * Creates a deep copy of this command object. Shallow copies the definition
     * object.
     * 
     * @return new instance of FlightCommand initialized to match this one.
     */
    IFlightCommand copy();

    /**
     * Sets the opcode value entered by the user. Although the opcode is
     * actually in the definition object, it can be modified during fault
     * injection, so all runtime command processing should use this method to
     * set opcode, rather than the method in the dictionary object.
     * 
     * @param val
     *            opcode value to set; Should not be null. The value
     *            ICommandDefinition.NULL_OPCODE_VALUE represents a undefined
     *            opcode.
     * 
     * @throws CommandParseException
     *             if the opcode is not a valid hex or binary string, or if it
     *             is of improper bit length for the current mission
     */
    void setEnteredOpcode(String val) throws CommandParseException;

    /**
     * Gets the opcode value entered by the user. Although the opcode is
     * actually in the definition object, it can be modified during fault
     * injection, so all runtime command processing should use this method to
     * get opcode, rather than the method in the dictionary object.
     * 
     * @return
     *            opcode value, as a hex string. The value
     *            ICommandDefinition.NULL_OPCODE_VALUE represents a undefined
     *            opcode
     */
    String getEnteredOpcode();
    
    /**
     * Get the string used to separate the arguments in this repeat argument
     * @param index the index of the repeat argument
     * @return the string used to separate the arguments, 
     */
    String getRepeatArgumentSeparator(int index);

}