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

import java.text.ParseException;

import jpl.gds.tc.api.command.args.ITimeArgument;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.CoarseFineEncoding;
import jpl.gds.shared.time.CoarseFineExtractor;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * 
 * This class is the base implementation of a time-based command argument (an
 * argument whose value is a spacecraft clock, a.k.a. SCLK, time).
 * <p>
 * A time argument represents a value defined in the spacecraft clock (SCLK)
 * domain. A value entered by the user may be a SCET or SCLK time,
 * but the value that is ultimately transmitted to the spacecraft is formatted
 * as a SCLK. The SCLK format will vary from mission to mission. If the user
 * enters a SCET time, a conversion to SCLK must be done on the argument
 * before it can be translated to binary.
 * <p>
 * Because this time argument represents a SCLK, the value transmitted is
 * essentially an unsigned integer.
 * 
 *
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc, static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 09/18/19 - MPCS-11080. The command translation table currently
 *          only accepts time arguments as SCET. Added function to return the
 *          time as SCET. Also fixed returning the time as SCET when a SCLK
 *          has been supplied.
 * 
 */
public class BaseTimeArgument extends AbstractNumericArgument implements ITimeArgument {

	protected final SclkFormatter sclkFmt;

    /**
     * Creates an instance of BaseTimeArgument.
     *
     * @param appContext App context
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseTimeArgument(final ApplicationContext appContext, final ICommandArgumentDefinition def) {

        super(appContext, def);
        sclkFmt = TimeProperties.getInstance().getSclkFormatter();
    }


    @Override
    public ICommandArgument copy() {

        final BaseTimeArgument ta = new BaseTimeArgument(appContext, this.getDefinition());
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
        try {
            sclk = getValueAsSclk(inputVal);
            if (sclk == null) {
                throw new ArgumentParseException("For value " + inputVal);
            }
            return (Long.valueOf(sclk.getBinaryGdrLong()));
        } catch (final ArgumentParseException e) {
            throw new ArgumentParseException("Could not interpret input value "
                    + inputVal, e);
        }
    }

    /**
     * Return the input argument value as a SCLK object according to the rules
     * of this particular argument
     * 
     * @param inputVal
     *            The string value to be converted to a SCLK
     * 
     * @return The SCLK object corresponding to the input string value
     * 
     * @throws ArgumentParseException
     *             If the input string value cannot be converted to a SCLK
     */
    protected ISclk getValueAsSclk(final String inputVal)
            throws ArgumentParseException {

        if (inputVal == null) {
            throw new IllegalArgumentException("Null input value");
        }

        ISclk sclk = null;
        if (sclkFmt.matches(inputVal)) {
            sclk = sclkFmt.valueOf(inputVal);
        } else {
            final IAccurateDateTime scet = getValueAsScet(inputVal);
            if (scet != null) {
                sclk = SclkScetUtility.getSclk(scet, null,
                                               appContext.getBean(IContextIdentification.class).getSpacecraftId(), log);
                if (sclk == null) {
                    throw new ArgumentParseException(
                            "Error reading SCLK/SCET correlation file for Spacecraft ID "
                                    + appContext.getBean(IContextIdentification.class).getSpacecraftId());
                }
            } else {
                throw new ArgumentParseException("Could not parse input value "
                        + inputVal);
            }
        }

        return (sclk);
    }

    /**
     * Return the input argument value as a SCET (IAccurateDateTime) object
     * according to the rules of this particular argument.
     * 
     * @param inputVal
     *            The input value to be converted to a SCET
     * 
     * @return The IAccurateDateTime according to the SCET value of the input
     *         value
     * 
     * @throws ArgumentParseException
     *             If the input value cannot be converted to a SCET
     */
    protected IAccurateDateTime getValueAsScet(final String inputVal)
            throws ArgumentParseException {

        if (inputVal == null) {
            throw new IllegalArgumentException("Null input value");
        }

        IAccurateDateTime d = null;
        if (sclkFmt.matches(inputVal)) {
            ISclk sclk = sclkFmt.valueOf(inputVal);
            d = SclkScetUtility.getScet(sclk,null, appContext.getBean(IContextIdentification.class).getSpacecraftId(), log);
        }
        else {
            try {
                d = new AccurateDateTime(inputVal);
            } catch (final ParseException pe) {
                throw new ArgumentParseException(pe);
            }
        }

        return (d);
    }


    @Override
    public String toString() {
        return toString(this.getDefinition().getUnits());
    }

    private String toString(final String unit) {
        if (this.argumentValue == null) {
            throw new IllegalStateException(
                    "No argument value exists (value is NULL).");
        }

        String value = null;

        try {

            if (unit.equalsIgnoreCase("SCLK")) {
                value = sclkFmt.fmt(getValueAsSclk(this.argumentValue));
            } else if (unit.equalsIgnoreCase("SCET")) {
                value = TimeUtility.getFormatterFromPool().format(
                        getValueAsScet(this.argumentValue));
            } else if (unit.equalsIgnoreCase("LST")) {
                throw new UnsupportedOperationException(
                        "The LST format is currently unsupported for command time arguments.");
            } else {
                value = getValueAsSclk(this.argumentValue).toDecimalString();
            }

        } catch (final ArgumentParseException e) {
            return ("");
        }

        return (value);
    }

    @Override
    public String getUplinkValue() {
        return toString(SCET);
    }

}
