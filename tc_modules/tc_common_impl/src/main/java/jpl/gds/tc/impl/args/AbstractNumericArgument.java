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

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.IValidationRange;
import jpl.gds.tc.api.command.args.INumericCommandArgument;
import jpl.gds.tc.api.exception.ArgumentParseException;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 *
 * The AbstractNumericArgument class is the superclass of all the various types
 * of command arguments that have a numeric value (and nothing else) associated
 * with them.
 *
 *
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc, static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 */
public abstract class AbstractNumericArgument extends AbstractCommandArgument implements INumericCommandArgument {
    /**
     *
     * Creates an instance of AbstractNumericArgument.
     *
     * @param appContext App context
     * @param def
     *            the command argument definition object for this argument.
     *
     */
    public AbstractNumericArgument(final ApplicationContext appContext, final ICommandArgumentDefinition def) {

        super(appContext, def);
    }

    /**
     * Format the input string argument value as a value that can be numerically
     * compared against the allowable ranges defined for this argument.
     *
     * @param inputVal
     *            The value to be converted to a numerically comparable value
     *
     * @return A numeric representation of the input value based on this
     *         argument's details
     *
     * @throws ArgumentParseException
     *             The input string value could not be converted to a comparable
     *             number
     */
    protected abstract Number formatValueAsComparable(final String inputVal)
            throws ArgumentParseException;

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.tc.impl.args.AbstractCommandArgument#isValueValid()
     */
    @Override
    public boolean isValueValid() {

        if (!isValueTransmittable()) {
            return (false);
        }

        // get a numeric representation of the value
        // JFWagner - 7/20/2020 - it needs to be BigInteger in case it's an unsigned number that's greater than the size of long
        BigInteger argVal = null;
        try {
            argVal = new BigInteger(formatValueAsComparable(argumentValue).toString());
        } catch (ArgumentParseException e) {
            log.warn("Unable to parse value " + argumentValue + " for type " + this.getDefinition().getTypeName() + ". Exception: " + e.getMessage());
            return false;
        }

        List<IValidationRange> ranges = this.getDefinition().getRanges();
        if (ranges == null || ranges.isEmpty()) {
            return (true);
        }

        // compare the value to make sure it falls within one of the valid
        // ranges defined
        // for this argument
        boolean valid = false;
        /*
         * 11/6/13 - MPCS-5521. Use new interface type for validation ranges.
         */

        for (final IValidationRange range : ranges) {
            BigInteger minVal = BigInteger.valueOf(Long.MIN_VALUE);
            final String minValString = range.getMinimum();
            if (minValString != null && !ICommandArgumentDefinition.MINIMUM_STRING.equalsIgnoreCase(minValString)) {
                try {
                    minVal = new BigInteger(formatValueAsComparable(minValString).toString());
                } catch (final ArgumentParseException e) {
                    log.warn("The minimum range value (" + minValString + ") for "
                            + this.getDisplayName() + " encountered an exception and will not be used. " + e.getMessage());
                    minVal = new BigInteger(this.getDefinition().getMinimumValue().toString());
                }
            } else {
                minVal = new BigInteger(this.getDefinition().getMinimumValue().toString());
            }

            BigInteger maxVal = BigInteger.valueOf(Long.MAX_VALUE);
            final String maxValString = range.getMaximum();
            if (maxValString != null && !ICommandArgumentDefinition.MAXIMUM_STRING.equalsIgnoreCase(maxValString)) {
                try {
                    maxVal = new BigInteger(formatValueAsComparable(maxValString).toString());
                } catch (final ArgumentParseException e) {
                    log.warn("The maximum range value (" + maxValString + ") for "
                            + this.getDisplayName() + " encountered an exception and will not be used. " + e.getMessage());
                    maxVal = new BigInteger(this.getDefinition().getMaximumValue().toString());
                }
            } else {
                maxVal = new BigInteger(this.getDefinition().getMaximumValue().toString());
            }

            // JFWagner - 7/20/2020 - MPCS-11829 - changed comparison logic to work with BigInteger
            if (minVal.compareTo(argVal) <= 0 // if argVal is greater than or equal to minVal AND
                    && argVal.compareTo(maxVal) <= 0 ) { // if argVal is less than or equal to maxVal
                valid = true;
                break;
            }
        }

        return (valid);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.tc.impl.args.AbstractCommandArgument#isValueTransmittable()
     */
    @Override
    public boolean isValueTransmittable() {

        if (this.argumentValue == null) {
            return (false);
        }

        BigInteger argVal;
        try {
            // we're creating a BigInteger from a String because casting from a long could artificially
            // cap the number
            argVal = new BigInteger(formatValueAsComparable(this.argumentValue).toString());
        } catch (final Exception e) {
            return (false);
        }

        /*
         * JFWagner - 5/28/2020 - changed to BigInteger/BigDecimal from long data type, because long was
         * insufficiently large to handle unsigned 64-bit values.
         */
        if (argVal.compareTo(new BigDecimal(this.getDefinition().getMinimumValue().toString()).toBigInteger()) == -1
                || argVal.compareTo(new BigInteger(this.getDefinition().getMaximumValue().toString())) == 1) {
            return (false);
        }

        return (true);
    }
}
