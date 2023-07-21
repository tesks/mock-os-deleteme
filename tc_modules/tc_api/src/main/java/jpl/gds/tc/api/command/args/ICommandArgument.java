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
import jpl.gds.tc.api.command.IBinaryRepresentable;

/**
 * The ICommandArgument interface explicitly details all of the operations that can be performed
 * on a typical argument.
 * 
 *
 * MPCS-10473 - 04/05/19 - Updated to reflect the changes in FlightCommand, which now
 * "owns" the arguments and does not let them be used freely.
 * Many of the functions were changed from being package protected to public as the command (or repeat argument)
 * that contains them may not be within the same package.
 *
 * MPCS-10745  - 06/30/19 - No longer extends IBinaryRepresentable
 */
public interface ICommandArgument {
	
	/* MPCS-10473 - removed SEPARATOR_STRING - this doesn't belong to an individual argument, but instead
	 * is the concern of UplinkInputParser, the only class that actually checked on this
	 */
	
    
    /**
     * Retrieves the dictionary definition object for this argument.
     * 
     * @return ICommandArgumentDefinition; will never be null
     */
    ICommandArgumentDefinition getDefinition();

    /**
     * A function that indicates whether or not this type of argument is entered
     * by the user when typing in a command.
     * 
     * @return True if the user should enter this argument, false otherwise
     */
    boolean isUserEntered();
    

    /**
     * Accessor for the argument value. The argument value is a string, but it
     * should be a value that can be parsed/formatted to match the data type of
     * the current argument. .
     * 
     * @return The current value of this argument, or null if it hasn't been set.
     */
    String getArgumentValue();

    /**
     * Mutator for the argument value. The argument value is a string, but it
     * should be a value that can be parsed/formatted to match the data type of
     * the current argument. This method will not perform checks to see that it is,
     * but validation will check it later.
     * 
     * @param argumentValue
     *            The new value of this argument
     * 
     */
    void setArgumentValue(final String argumentValue);

    /**
     * A simple function for trying to determine what name to display in the
     * Command Builder for this particular argument. This function is generally
     * used to populate the "Prefix Control".
     * 
     * Currently we try the dictionary name, then the FSW name and then
     * "Unknown" if neither of those are set.
     * 
     * @return A String containing the name of this argument for displaying to
     *         the end user.
     */
    String getDisplayName();
    
    /**
     * Clear out any value that this argument may have by resetting it to its
     * default.
     */
    void clearArgumentValue();
    
    /**
     * Makes a deep copy of this command argument, exclusive of the definition,
     * which should be shallow-copied. Must be implemented by the subclass,
     * which can create the proper class. MUST invoke setSharedValues() to copy
     * the members held in this class.
     * 
     * @return a new instance of the current command argument, initialized to to
     *         match this one
     * 
     * 6/22/14 - MPCS-6304. No longer returns ICommandArgumentDefinition
     */
    ICommandArgument copy();
    
    /**
     * A function that looks at the current value and determines whether or
     * not the argument value is valid for this particular argument according to
     * all the various conditions (e.g. ranges) defined in the command
     * dictionary.
     * 
     * If the function "isValueTransmittable" returns false, this function
     * should return false as well. If a value is not transmittable, it is not
     * valid. However, it is ok for a value to be transmittable, but invalid.
     * 
     * 
     * @return True if the value is valid, false otherwise.
     * 
     * MPCS-10473 - 04/05/19 - removed the argument from the function
     * call and now it uses the stored value. This design change was made primarily
     * to streamline and standardize checking and setting of values and allow the value
     * to persist, even when invalid, through GUI refreshes of the command arguments 
     */
    boolean isValueValid();

    /**
     * A function that looks at the value passed in and determines whether or
     * not the argument value can actually be transmitted to the spacecraft
     * (e.g. does it fit into the number of bits allotted to this argument,
     * etc.). This function is NOT dictionary validation.
     * 
     * 
     * @return True if the value can be transmitted to the spacecraft, false
     *         otherwise.
     * 
     * MPCS-10473 - 04/05/19 - removed the argument from the function
     * call and now it uses the stored value. This design change was made primarily
     * to streamline and standardize checking and setting of values and allow the value
     * to persist, even when invalid, through GUI refreshes of the command arguments 
     */
    boolean isValueTransmittable();
    
}