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
package jpl.gds.tc.impl.args;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.time.ISclk;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.exception.ArgumentParseException;

/**
 * Represents a floating point time command argument. These arguments are an MSL
 * adaptation to the basic time argument class. The time itself is a double,
 * which actually represents a SCLK in decimal format.
 * 
 *
 * 1/8/14 - MPCS-5667. Renamed from ReferenceTimeArgument.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only. It
 *          also used to support both integer and float time values. Now it only
 *          supports floats. Integer times must be represented using the parent
 *          class.
 */
public class FloatTimeArgument extends BaseTimeArgument {
    /**
     * Creates an instance of FloatTimeArgument.
     * 
     * @param def
     *            the command argument definition object for this argument.
     * @param appContext App context
     *
     */
    public FloatTimeArgument(final ApplicationContext appContext, final ICommandArgumentDefinition def) {

        super(appContext, def);
    }

    @Override
    public ICommandArgument copy() {

        final FloatTimeArgument ta = new FloatTimeArgument(appContext, this.getDefinition());
        setSharedValues(ta);
        return (ta);
    }

    @Override
    protected Number formatValueAsComparable(final String inputVal)
            throws ArgumentParseException {

        if (inputVal == null) {
            throw new ArgumentParseException("Null input value");
        }

        ISclk sclk = null;
        sclk = getValueAsSclk(inputVal);
        if (sclk == null) {
            throw new ArgumentParseException(
                    "Could not interpret input value as SCLK " + inputVal);
        }

        return sclk.getFloatingPointTime();

    }

}
