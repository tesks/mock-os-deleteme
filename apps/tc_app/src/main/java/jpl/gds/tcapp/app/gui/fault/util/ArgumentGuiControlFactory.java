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
package jpl.gds.tcapp.app.gui.fault.util;

import jpl.gds.tc.api.command.IFlightCommand;

/**
 * This factory creates a GUI utility object for a command argument based upon
 * the type of the argument. The resulting object is used to create SWT GUI
 * controls for the argument.
 * 
 *
 */
public final class ArgumentGuiControlFactory {

    /**
     * Creates and returns the GUI control object for the given command argument
     * object.
     * 
     * @param cmd the IFlightCommand that will have one of its arguments displayed through
     *            an ICommandArgumentDefinition object
     * @param index
     *            the index of the ICommandArgumentDefinition in the IFlightCommand to create the GUI
     *            control for
     * 
     * @return new instance of the appropriate ICommandArgumentGuiControl object
     */
    public static ICommandArgumentGuiControl getGuiControl(final IFlightCommand cmd, final int index) {

        if (cmd == null) {
        	throw new IllegalArgumentException("ICommandArgumentGuiControl must be supplied a command.");
        }
        	
        if (index >= cmd.getArgumentCount() || index < 0) {
        	throw new IllegalArgumentException("ICommandArgumentGuiControl must be supplied a valid argument index.");
        }

        ICommandArgumentGuiControl control = null;

        switch (cmd.getArgumentType(index)) {
        case TIME:
        case FLOAT_TIME:
            control = new TimeArgumentGuiControl();
            break;

        case FLOAT:
            control = new FloatArgumentGuiControl();
            break;

        case INTEGER:
            control = new IntegerArgumentGuiControl();
            break;

        case UNSIGNED:
            control = new UnsignedArgumentGuiControl();
            break;

        case SIGNED_ENUMERATION:
        case UNSIGNED_ENUMERATION:
        case BOOLEAN:
            control = new EnumeratedArgumentGuiControl();
            break;

        case VAR_STRING:
        case FIXED_STRING:
            control = new StringArgumentGuiControl();
            break;

        case FILL:
            control = new FillerArgumentGuiControl();
            break;

        case REPEAT:
            control = new RepeatArgumentGuiControl();
            break;
            
        case BITMASK:
            control = new BitmaskArgumentGuiControl();
            break;
        default:
            throw new IllegalArgumentException(
                    "Unrecognized command argument type " + cmd.getArgumentType(index));
        }

        control.setArgument(cmd, index);
        return control;
    }
    
    /**
     * Creates and returns the GUI control object for the given command argument
     * object.
     * 
     * @param cmd the IFlightCommand that will have one of its arguments displayed through
     *            an ICommandArgumentDefinition object
     * @param index
     *            the index of the ICommandArgumentDefinition in the IFlightCommand to create the GUI
     *            control for
     * 
     * @return new instance of the appropriate ICommandArgumentGuiControl object
     */
    public static ICommandArgumentGuiControl getGuiControl(final IFlightCommand cmd, final int index, final int subIndex) {

        if (cmd == null) {
        	throw new IllegalArgumentException("ICommandArgumentGuiControl must be supplied a command.");
        }
        	
        if (index >= cmd.getArgumentCount() || subIndex >= cmd.getArgumentCount(index) || index < 0 || subIndex < 0) {
        	throw new IllegalArgumentException("ICommandArgumentGuiControl must be supplied a valid argument index. Index=" + index + " (MAX=" + cmd.getArgumentCount() + "), subIndex=" + subIndex + " (MAX=" + cmd.getArgumentCount(index) + ")");
        }

        ICommandArgumentGuiControl control = null;

        switch (cmd.getArgumentType(index, subIndex)) {
        case TIME:
        case FLOAT_TIME:
            control = new TimeArgumentGuiControl();
            break;

        case FLOAT:
            control = new FloatArgumentGuiControl();
            break;

        case INTEGER:
            control = new IntegerArgumentGuiControl();
            break;

        case UNSIGNED:
            control = new UnsignedArgumentGuiControl();
            break;

        case SIGNED_ENUMERATION:
        case UNSIGNED_ENUMERATION:  
        case BOOLEAN:
            control = new EnumeratedArgumentGuiControl();
            break;

        case VAR_STRING:
        case FIXED_STRING:
            control = new StringArgumentGuiControl();
            break;

        case FILL:
            control = new FillerArgumentGuiControl();
            break;

        case REPEAT:
            control = new RepeatArgumentGuiControl();
            break;
            
        case BITMASK:
            control = new BitmaskArgumentGuiControl();
        default:
            throw new IllegalArgumentException(
                    "Unrecognized command argument type " + cmd.getArgumentType(index, subIndex));
        }

        control.setArgument(cmd, index, subIndex);
        return control;
    }
    
    

}
