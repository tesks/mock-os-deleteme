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
package jpl.gds.tc.impl.args;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;
import jpl.gds.tc.api.command.args.ICommandArgument;

/**
 * This is a static factory class for creating command argument objects from
 * ICommandArgumentDefinition objects.
 * 
 *
 * 6/23/14 - MPCS-6304. Added class.
 * 03/21/19 - MPCS-10473 - removed interface. This class should only be used
 *          by IFlightCommand classes.
 */
public class CommandArgumentFactory {

    private final ApplicationContext appContext;

    /**
     * Constructor
     */
    public CommandArgumentFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * Creates an instance of a command argument object given its dictionary
     * definition.
     * 
     * @param def
     *            the ICommandArgumentDefinition object for the command argument
     * @return an AbstractCommandArgument object of the appropriate type to
     *         match the argument definition
     */
    public ICommandArgument create(final ICommandArgumentDefinition def) {

        if (def == null) {
            throw new IllegalArgumentException(
                    "command argument definition cannot be null");
        }
        
        switch (def.getType()) {
        case BOOLEAN:
            return new BaseBooleanArgument(appContext, def);
        case SIGNED_ENUMERATION:
        case UNSIGNED_ENUMERATION:
            return new BaseEnumeratedArgument(appContext, def);
        case FILL:
            return new BaseFillerArgument(appContext, def);
        case FLOAT:
            return new BaseFloatArgument(appContext, def);
        case INTEGER:
            return new BaseIntegerArgument(appContext, def);
        case FIXED_STRING:
            return new BaseStringArgument(appContext, def);
        case VAR_STRING:
            return new BaseVarStringArgument(appContext,def);
        case TIME:
            return new BaseTimeArgument(appContext,def);
        case FLOAT_TIME:
            return new FloatTimeArgument(appContext, def);
        case REPEAT:
            if (!(def instanceof IRepeatCommandArgumentDefinition)) {
                throw new IllegalArgumentException(
                        "Expected IRepeatCommandArgumentDefinition in CommandArgumentFactory");
            }
            return new BaseRepeatArgument(
                    appContext, (IRepeatCommandArgumentDefinition) def);
        case UNSIGNED:
            return new BaseUnsignedArgument(appContext, def);
        case BITMASK:
            return new BaseBitmaskArgument(appContext, def);
        case UNDEFINED:
        default:
            throw new IllegalArgumentException("Unsupported argument type "
                    + def.getType() + " in CommandArgumentFactory");

        }
    }

}
