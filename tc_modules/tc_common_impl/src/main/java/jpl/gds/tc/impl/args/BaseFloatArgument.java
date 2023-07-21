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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.IValidationRange;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * This class is the base implementation of a floating point command argument.
 * 
 * A floating argument represents a single floating point numeric value such as
 * 25.78 or -2.434875. Floating point numbers must be defined as either 32 or 64
 * bits long. Float arguments use the IEEE-754 standard for binary
 * representation (see http://babbage.cs.qc.edu/IEEE-754/Decimal.html).
 * 
 *
 * 11/5/13 - MPCS-5512. Correct static analysis and javadoc
 *          issues.
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc, static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 7/1/19 - MPCS-10745 - removed toBitString, parseFromBitString
 */
class BaseFloatArgument extends AbstractNumericArgument {
    /**
     * Creates an instance of BaseFloatArgument.
     * 
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseFloatArgument(ApplicationContext appContext, ICommandArgumentDefinition def) {

        super(appContext, def);
    }

    @Override
    public ICommandArgument copy() {

        final BaseFloatArgument fa = new BaseFloatArgument(appContext, this.getDefinition());
        setSharedValues(fa);
        return (fa);
    }

    @Override
    protected Number formatValueAsComparable(final String inputVal)
            throws ArgumentParseException {

        try {
            final int bitLength = this.getDefinition().getBitLength();
            /*
             * 11/5/13 - MPCS-5512. Suppressed bogus PMD warning here.
             */
            switch (bitLength) {
            case Float.SIZE:

                if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
                    return GDR.getFloatFromBits(inputVal);
                } else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
                    return GDR.getFloatFromHex(inputVal);
                } else {
                    return Float.parseFloat(inputVal);
                }

            case Double.SIZE:

                if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
                    return GDR.getDoubleFromBits(inputVal);
                } else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
                    return GDR.getDoubleFromHex(inputVal);
                } else {
                    return Double.parseDouble(inputVal);
                }

            default:

                throw new IllegalStateException("Invalid bit length of "
                        + bitLength + " found for "
                        + "float argument (dictionary name = "
                        + this.getDefinition().getDictionaryName()
                        + ", fsw name = " + this.getDefinition().getFswName()
                        + ")");
            }
        } catch (final Exception e) {
            throw new ArgumentParseException("Could not interpret input value "
                    + inputVal, e);
        }
    }

    @Override
    public boolean isValueValid() {

        if (!isValueTransmittable()) {
            return (false);
        }

        if (this.getDefinition().getRanges() == null || this.getDefinition().getRanges().isEmpty()) {
            return (true);
        }

        switch (this.getDefinition().getBitLength()) {
        case Float.SIZE:

            return (isFloatValueValid());

        case Double.SIZE:

            return (isDoubleValueValid());

        default:

            return (false);
        }
    }

    @Override
    public boolean isValueTransmittable() {

        if (this.argumentValue == null) {
            return (false);
        }

        switch (this.getDefinition().getBitLength()) {
        case Float.SIZE:

            return (isFloatValueTransmittable());

        case Double.SIZE:

            return (isDoubleValueTransmittable());

        default:

            return (false);
        }
    }

    /**
     * Determine whether or not this particular value is capable of being
     * transmitted as a 32-bit float.
     * 
     * 
     * @return True if the value is transmittable, false otherwise
     */
    protected boolean isFloatValueTransmittable() {

        float argVal;
        try {
            argVal = formatValueAsComparable(this.argumentValue).floatValue();
            if (argVal == Float.NaN) {
                return (false);
            }
        } catch (final Exception e) {
            return (false);
        }
        /*
         * 11/8/13 - MPCS-5521. Add casts now that return type is
         * Object from the min/max methods
         */
        if (argVal < ((Float) this.getDefinition().getMinimumValue())
                && argVal > ((Float) this.getDefinition().getMaximumValue())) {
            return (false);
        }

        return (true);
    }

    /**
     * Determine whether or not this particular value is a valid 32-bit float
     * according to this argument specification.
     * 
     * 
     * @return True if the value is valid, false otherwise
     */
    protected boolean isFloatValueValid() {

        if (!isFloatValueTransmittable()) {
            return (false);
        }

        float argVal;
        try {
            argVal = formatValueAsComparable(this.argumentValue).floatValue();
        } catch (final ArgumentParseException e) {
            return (false);
        }

        boolean valid = false;
        /*
         * 11/6/13 - MPCS-5521. Use new interface type for validation
         * ranges.
         */
        for (final IValidationRange range : this.getDefinition().getRanges()) {
            float minVal = Float.NEGATIVE_INFINITY;
            final String minValString = range.getMinimum();
            if (minValString != null && !ICommandArgumentDefinition.MINIMUM_STRING.equalsIgnoreCase(minValString)) {
                try {
                    minVal = formatValueAsComparable(minValString).floatValue();
                } catch (final ArgumentParseException e) {
                    log.warn("The minimum range value (" + minValString + ") for " 
                            + this.getDisplayName() + " encountered an exception and will not be used. " + e.getMessage());
                    minVal = Float.NEGATIVE_INFINITY;
                }
            }

            float maxVal = Float.POSITIVE_INFINITY;
            final String maxValString = range.getMaximum();
            if (maxValString != null && !ICommandArgumentDefinition.MAXIMUM_STRING.equalsIgnoreCase(maxValString) ) {
                try {
                    maxVal = formatValueAsComparable(maxValString).floatValue();
                } catch (final ArgumentParseException e) {
                    log.warn("The maximum range value (" + maxValString + ") for " 
                            + this.getDisplayName() + " encountered an exception and will not be used. " + e.getMessage());
                    maxVal = Float.POSITIVE_INFINITY;
                }
            }

            if (minVal <= argVal && argVal <= maxVal) {
                valid = true;
                break;
            }
        }

        return (valid);
    }

    /**
     * Determine whether or not this particular value is capable of being
     * transmitted as a 64-bit double.
     * 
     * @return True if the value is transmittable, false otherwise
     */
    @SuppressWarnings({ "FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", "PMD.BadComparison" })
    protected boolean isDoubleValueTransmittable() {

        double argVal;
        try {
            argVal = formatValueAsComparable(this.argumentValue).doubleValue();
            if (argVal == Double.NaN) {
                return (false);
            }
        } catch (final Exception e) {
            return (false);
        }

        /*
         * 11/8/13 - MPCS-5521. Add casts now that return type is
         * Object from the min/max methods
         */
        if (argVal < ((Double) this.getDefinition().getMinimumValue())
                && argVal > ((Double) this.getDefinition().getMaximumValue())) {
            return (false);
        }

        return (true);
    }

    /**
     * Determine whether or not this particular value is a valid 64-bit double
     * according to this argument specification.
     * 
     * @return True if the value is valid, false otherwise
     */
    protected boolean isDoubleValueValid() {

        if (!isDoubleValueTransmittable()) {
            return (false);
        }

        double argVal;
        try {
            argVal = formatValueAsComparable(this.argumentValue).doubleValue();
        } catch (final ArgumentParseException e) {
            return (false);
        }

        boolean valid = false;
        /*
         * 11/6/13 - MPCS-5521. Use new interface type for validation
         * ranges.
         */
        for (final IValidationRange range : this.getDefinition().getRanges()) {
            double minVal = Double.NEGATIVE_INFINITY;
            final String minValString = range.getMinimum();
            if (minValString != null && !ICommandArgumentDefinition.MINIMUM_STRING.equalsIgnoreCase(minValString)) {
                try {
                    minVal = formatValueAsComparable(minValString)
                            .doubleValue();
                } catch (final ArgumentParseException e) {
                    log.warn("The minimum range value (" + minValString + ") for " 
                            + this.getDisplayName() + " encountered an exception and will not be used. " + e.getMessage());
                    minVal = Double.NEGATIVE_INFINITY;
                }
            }

            double maxVal = Double.POSITIVE_INFINITY;
            final String maxValString = range.getMaximum();
            if (maxValString != null && !ICommandArgumentDefinition.MAXIMUM_STRING.equalsIgnoreCase(maxValString)) {
                try {
                    maxVal = formatValueAsComparable(maxValString)
                            .doubleValue();
                } catch (final ArgumentParseException e) {
                    log.warn("The maximum range value (" + maxValString + ") for " 
                            + this.getDisplayName() + " encountered an exception and will not be used. " + e.getMessage());
                    maxVal = Double.POSITIVE_INFINITY;
                }
            }

            if (minVal <= argVal && argVal <= maxVal) {
                valid = true;
                break;
            }
        }

        return valid;
    }
}
