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

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.dictionary.api.command.IValidationRange;
import jpl.gds.dictionary.api.command.SignedEnumeratedValue;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IEnumeratedCommandArgument;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.CommandProperties.BitValueFormat;
import jpl.gds.tc.api.config.OutputFormatType;

/**
 * This class is the base implementation for all enumerated (look) arguments.
 * 
 * An enumerated argument (a.k.a. look argument) is an enumeration restricted to
 * a specific list of known values (i.e. an enumeration). A simple enumeration
 * might consist of two values such as ON and OFF, but there is no limit for how
 * many possible values an enum argument can have (the only limiting factor is
 * the bit length of the argument).
 * <p>
 * Each potential value for the enumerated argument, called an Enumerated Value,
 * has a unique (to the argument) dictionary name and an associated numeric
 * value. The numeric value is what is actually transmitted to the spacecraft.
 * <p>
 * Unlike most enumerations in AMPCS, which use a numeric value and a symbolic
 * value for each entry in the table, command enumerations support three string
 * values: 1) dictionary value (what AMPCS normally thinks of as the symbolic
 * values, and generally uses for displays and other human-readable
 * representations, 2) a FSW value, which is also a symbolic name, and is used
 * in the flight software, and 3) a bit value, which is the numeric value.
 * <p>
 * This class can be used for both signed and unsigned enumerations. Which is
 * used is entirely dictated by what type of value objects are added to the
 * enumeration in the command argument definition object: SignedEnumeratedValue
 * or UnsignedEnumeratedValue.
 * 
 *
 * 11/5/13 - MPCS-5512. Correct static analysis and javadoc
 *          issues.
 * 11/8/13 - MPCS-5521. Some methods and members moved to
 *          superclass. General cleanup, javadoc, static analysis changes.
 * 6/22/14 - MPCS-6304. Dictionary-related members throughout
 *          removed to CommandArgumentDefinition class. This class has been
 *          stripped down to support runtime commanding capabilities only.
 * 07/01/19 - MPCS-10745 moved BitValueFormat enumeration to CommandProperties, removed toBitString and
 *          parseFromBitString. Refactored toString.
 */
public class BaseEnumeratedArgument extends AbstractCommandArgument implements IEnumeratedCommandArgument {

    /** The enum value of this argument */
    private ICommandEnumerationValue argumentEnumValue;

    /** The output type of this argument */
    private OutputFormatType format;
    
    /** A reference to the current Command Configuration in this context */ 
    protected final CommandProperties cmdConfig;

    /**
     * Creates an instance of BaseEnumeratedArgument.
     * 
     * @param appContext the ApplicationContext that in which this object is being used
     * 
     * @param def
     *            the command argument definition object for this argument.
     * 
     */
    public BaseEnumeratedArgument(final ApplicationContext appContext, final ICommandArgumentDefinition def) {

        super(appContext, def);

        this.cmdConfig = appContext.getBean(CommandProperties.class);
        this.format = this.cmdConfig.getEnumerationOutputFormat();
        this.argumentEnumValue = null;
    }

    @Override
    public void clearArgumentValue() {

        setArgumentValue(this.getDefinition().getDefaultValue() != null ? this
                .getDefinition().getDefaultValue() : "");
    }

    /**
     * Given a dictionary value, FSW value or bit value, get the corresponding
     * enum value
     * 
     * @param value
     *            The dictionary value, FSW value or bit value of the enum value
     *            to get
     * 
     * @return The enum value corresponding to the input value or null if it
     *         can't be found
     * 
     *  11/13/13 - MPCS-5521. Changed return type to use interface type and made private.
     */
    private ICommandEnumerationValue lookupByAll(final String value) {

        return (lookupByAll(value, false));
    }

    /**
     * Given a dictionary value, FSW value or bit value, get the corresponding
     * enum value
     * 
     * @param value
     *            The dictionary value, FSW value or bit value of the enum value
     *            to get
     * 
     * @param ignoreConfig
     *            Some missions wanted the ability to only use a particular name
     *            to lookup an enum value. If "ignoreConfig" is false, then we
     *            check the configuration file to make sure that the user is
     *            allowed to use the particular type of value to get the enum
     *            value (e.g. MSL only allows dictionary value to be used, not
     *            FSW value or bit value).
     * 
     * @return The enum value corresponding to the input value or null if it
     *         can't be found
     * 
     *         11/13/13 - MPCS-5521. Changed return type to use interface
     *         type and made protected.
     */
    protected ICommandEnumerationValue lookupByAll(final String value,
            final boolean ignoreConfig) {

         /*
         * 11/13/13 - MPCS-5521. Changes on the three if statements below
         * to go through the enumeration object.
         */
        ICommandEnumerationValue returnVal = this.getDefinition()
                .getEnumeration().lookupByBitValue(value);
        if (returnVal != null
                && (cmdConfig.isAllowEnumBitValue() || ignoreConfig)) {
            return (returnVal);
        }

        returnVal = this.getDefinition().getEnumeration()
                .lookupByDictionaryValue(value);
        if (returnVal != null
                && (cmdConfig.isAllowEnumDictValue() || ignoreConfig)) {
            return (returnVal);
        }

        returnVal = this.getDefinition().getEnumeration()
                .lookupByFswValue(value);
        if (returnVal != null
                && (cmdConfig.isAllowEnumFswValue() || ignoreConfig)) {
            return (returnVal);
        }
        
        //need to cleanse the palette
        return (null);
    }

    /**
     * Accessor for the argument value.
     * 
     * @return The current value of this argument
     * 
     *         11/13/13 - MPCS-5521. Changed return type to use interface type.
     */
    @Override
	public ICommandEnumerationValue getArgumentEnumValue() {

        return (this.argumentEnumValue);
    }

    /**
     * Mutator for the argument value.
     * 
     * @param lookupValue
     *            The new value for this argument
     * 
     *  11/13/13 - MPCS-5521. Changed argument to use interface type.
     */
    @Override
	public void setArgumentEnumValue(final ICommandEnumerationValue lookupValue) {

        // if an invalid lookup is passed in, we don't handle it here...it's
        // handled
        // at a higher level
        /*
         * 11/13/13 - MPCS-5521. Changed to use enumeration value
         * interface type.
         */
        ICommandEnumerationValue newValue = lookupValue;
        if (newValue == null) {
            newValue = new SignedEnumeratedValue();
        }
        /*
         * 11/13/13 - MPCS-5521. Changed to go through the enumeration
         * object.
         */
        else if (this.getDefinition().getEnumeration()
                .lookupByDictionaryValue(lookupValue.getDictionaryValue()) == null) {
            newValue = new SignedEnumeratedValue();
        }

        this.argumentEnumValue = newValue;
        this.argumentValue = newValue.getDictionaryValue();
    }

    /**
     * Get the output format for this argument.
     * 
     * @return The defined output format for this argument
     * 
     */
    protected OutputFormatType getFormat() {

        return (this.format);
    }

    /**
     * Set the output format for this argument.
     * 
     * @param fmt
     *            The new output format for this argument
     * 
     */
    public void setFormat(final OutputFormatType fmt) {

        if (fmt == null) {
            throw new IllegalArgumentException("Null input format");
        }

        this.format = fmt;
    }

    @SuppressWarnings("PMD.UnnecessaryWrapperObjectCreation")
    @Override
    public String toString() {

        if (this.format == null) {
            throw new IllegalStateException(
                    "The IO Format is null.  A string for this argument (dictionary name = \""
                            + this.getDefinition().getDictionaryName()
                            + "\") cannot be generated.");
        }
        /*
         * 11/13/13 - MPCS-5521. Changed to use enumeration value
         * interface type.
         */
        final ICommandEnumerationValue value = this.argumentEnumValue;
        int bitLength = getDefinition().getBitLength();
        if (value == null) {
            if (this.argumentValue != null) {
                return (this.argumentValue);
            }

            return ("NULL");
        }

        String bitString = "";

        String enumBitVal = value.getBitValue();
        BitValueFormat bitFormat = cmdConfig.getEnumBitValueFormat();


        try {

            if(bitFormat.equals(BitValueFormat.UNSPECIFIED)) {
                if(BinOctHexUtility.hasHexPrefix(enumBitVal) || BinOctHexUtility.isValidHex(enumBitVal)) {
                    bitFormat = BitValueFormat.HEX;
                } else if(enumBitVal.matches("[0-9]*")) {
                    bitFormat = BitValueFormat.DECIMAL;
                } else if(BinOctHexUtility.hasBinaryPrefix(enumBitVal) || BinOctHexUtility.isValidBin(enumBitVal)) {
                    bitFormat = BitValueFormat.BINARY;
                }
            }

            switch(bitFormat) {

                case DECIMAL:
                    bitString = Integer.toBinaryString(Integer.parseInt(enumBitVal));
                    boolean positive = enumBitVal.charAt(0) != '-';
                    bitString = StringUtils.leftPad(bitString, bitLength, positive ? '0' : '1');
                    break;
                case HEX:
                    bitString = BinOctHexUtility.toBinFromHex(enumBitVal);
                    bitString = StringUtils.leftPad(bitString, bitLength, '0');
                    break;
                case BINARY:
                case UNSPECIFIED:
                default:
                    bitString = StringUtils.leftPad(enumBitVal, bitLength, '0');
            }

            switch(format) {

                case BINARY:
                    //do nothing, it's already binary
                    break;
                case OCTAL:
                    bitString = BinOctHexUtility.toOctFromBin(bitString);
                    break;
                case DECIMAL:
                    bitString = String.valueOf(GDR.parse_long("0b"+ bitString));
                    break;
                case HEXADECIMAL:
                    bitString = BinOctHexUtility.toHexFromBin(bitString);
                    break;
                case STRING:
                    bitString = value.getDictionaryValue();
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown/Unusable format type "
                                    + this.format.toString()
                                    + " for this argument (dictionary name = \""
                                    + this.getDefinition().getDictionaryName()
                                    + "\") cannot be generated.");
            }

        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Cannot convert argument (dictionary name = \""
                            + this.getDefinition().getDictionaryName()
                            + "\") + to a string value: " + e.getMessage(), e);
        }

        return bitString;
    }

    @Override
    public ICommandArgument copy() {

        final BaseEnumeratedArgument la = new BaseEnumeratedArgument(appContext,
                this.getDefinition());
        setSharedValues(la);
        return (la);
    }

    /**
     * Copy the values from this enumerated argument onto the input enumerated
     * argument.
     * 
     * @param la
     *            The argument whose values should be set to the values of this
     *            argument
     */
    protected void setSharedValues(final BaseEnumeratedArgument la) {

        super.setSharedValues(la);

        /*
         * 11/5/13 - MPCS-5521. Moved copy of argument type to the super
         * method, and added copy of new member fields and enumeration object
         * members. Copy of the enumeration object is not in
         * CommandEnumerationDefinition because it uses interfaces and does not
         * know the real types of the values for copy.
         */

        /*
         * 12/19/13 - MPCS-5620. Added null checks. Copy threw NPE when
         * format not set.
         */
        la.format = this.format;
        la.setArgumentEnumValue(this.argumentEnumValue);
    }

    @Override
    public void setArgumentValue(final String value) {

        if (value == null) {
            throw new IllegalArgumentException("Null input argument value");
        }

        this.argumentValue = value;
        this.argumentEnumValue = value.isEmpty() ? null : lookupByAll(this.argumentValue);
    }

    @Override
    public boolean isValueTransmittable() {
    	    	
    	
    	// If the enumeration object doesn't exist (because of a bad enumeration input string), exit
        if (this.argumentEnumValue == null) { 
            return (false);
        }

        final String bv = this.argumentEnumValue.getBitValue();

        String argBits;

        try {
            argBits = BinOctHexUtility.getBitsFromNumericString(bv);
        } catch (IllegalArgumentException e) {
            return false;
        }


        argBits = StringUtils.reverse(argBits);

        if(this.argumentEnumValue.getBitValue().charAt(0)=='-') {
            return argBits.lastIndexOf('0') < this.getDefinition().getBitLength();
        }

        else {
            return argBits.lastIndexOf('1') < this.getDefinition().getBitLength();
        }

    }

    @Override
    public boolean isValueValid() {

        if (!isValueTransmittable()) {
            return false;
        }

        /*
         * 1/13/14 - Moved the remaining code in this method here from
         * MslEnumeratedArgument. It is harmless if no ranges are defined, but
         * gives us multimission capability to support ranges on enum args,
         * which is desired. MslEnumeratedArgument then goes away, because it
         * adds no other capability.
         */
        if (this.getDefinition().getRanges().isEmpty()) {
            return (true);
        }

        /*
         * 11/13/13 - MPCS-5521. Changes to code below to use the
         * interface type for enumeration values.
         */
        final ICommandEnumerationValue enumVal = lookupByAll(this.argumentValue, true);

        // check ranges
        for (final IValidationRange range : this.getDefinition().getRanges()) {
            final ICommandEnumerationValue minVal = lookupByAll(range.getMinimum(),
                    true);
            final ICommandEnumerationValue maxVal = lookupByAll(range.getMaximum(),
                    true);

            // ignore invalid ranges or null value on this argument
            if (minVal == null || maxVal == null || enumVal == null) {
                continue;
            }

            final long min = Long.parseLong(minVal.getBitValue());
            final long max = Long.parseLong(maxVal.getBitValue());
            final long val = Long.parseLong(enumVal.getBitValue());

            if (min <= val && val <= max) {
                return (true);
            }
        }

        return (false);
    }
}
