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
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IStringCommandArgument;

/**
 * This is the base implementation for all string command arguments.
 * <p>
 * A string argument is an argument whose value is a human readable string. This
 * class assumes a string argument of fixed length. If the argument value
 * specified by the user is not long enough to match the fixed length defined in
 * the dictionary, it is padded with 0 bits until it matches the desired length.
 * <p>
 * The character encoding is US-ASCII.
 * 
 *
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc, static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 7/1/19 - MPCS-10745 removed toBitString, parseFromBitString
 */
public class BaseStringArgument extends AbstractCommandArgument implements IStringCommandArgument {

    /**
     * Creates an instance of BaseStringArgument.
     * 
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseStringArgument(final ApplicationContext appContext, final ICommandArgumentDefinition def) {

        super(appContext, def);
    }

    @Override
    public String toString() {

        return (this.argumentValue != null ? this.argumentValue : "");
    }

    @Override
    public ICommandArgument copy() {

        final BaseStringArgument arg = new BaseStringArgument(appContext, this.getDefinition());
        setSharedValues(arg);
        return (arg);
    }

    @Override
    public boolean isValueTransmittable() {

        if (this.argumentValue == null) {
            return (false);
        }

        // Note: Empty string values are illegal.
        /*
         * MPCS-5652 - 1/6/14. Change multiply by 8 to divide by 8 to
         * properly get bytes from bits.
         */
        if (this.argumentValue.isEmpty()
                || this.argumentValue.length() > (this.getDefinition().getBitLength() / 8)) {
            return (false);
        }

        return (true);
    }

    @Override
    public boolean isValueValid() {

        if (!isValueTransmittable()) {
            return (false);
        }

        if (this.getDefinition().getValueRegexp() == null) {
            return (true);
        }

        return (this.argumentValue.matches(this.getDefinition().getValueRegexp()));
    }
}
