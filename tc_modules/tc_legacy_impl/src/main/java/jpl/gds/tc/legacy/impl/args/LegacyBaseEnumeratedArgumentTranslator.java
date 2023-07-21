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
package jpl.gds.tc.legacy.impl.args;

import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.CommandEnumerationDefinition;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.config.CommandProperties.BitValueFormat;
import jpl.gds.tc.api.exception.BlockException;

/**
 * Convert an enumerated argument to/from a bit string
 *
 */
public class LegacyBaseEnumeratedArgumentTranslator implements ICommandArgumentTranslator {

    private final BitValueFormat bitValueFormat;

    public LegacyBaseEnumeratedArgumentTranslator(final BitValueFormat bitValueFormat) {
        this.bitValueFormat = bitValueFormat;
    }

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) {
        
        String dictVal;

        if (bitString == null) {
            throw new IllegalArgumentException(
                    "Null input bit string to enum argument (dictionary name = \""
                            + def.getDictionaryName() + "\")");
        }

        // extract the part of the bit string we care about
        final int bitLength = def.getBitLength();

        String binString = bitString.read(bitLength);

        // make the assumption it's just missing leading zeroes
        binString = GDR.fillStr(binString, bitLength, '0');

        /*
         * MPCS-4889 - 8/29/13: This logic is flawed. For example: if
         * you have a mission that stores the values in decimal format, a value
         * of 105 in decimal is 69 in hex. Therefore, this would match the
         * decimal 69 of another enum value instead of the correct 105, since it
         * checks the 3 different formats for each enum value from 0 to n and
         * exits after it thinks it finds the correct one.
         *
         * To fix the issue SMAP encountered because of this, I'm adding a
         * config value to the project config to explcitly state whether or not
         * the enum values are decimal, binary, or hex. If it is not specified,
         * it will fall back to this flawed logic in order to limit the effects
         * of this change.
         */
        // the bit value is defined differently for different missions, so we'll
        // check it
        // as binary, decimal and hex
        //
        // in retrospect, it seems like converting it to int would be easier...

        String decString;
        if(def.getType().equals(CommandArgumentType.UNSIGNED_ENUMERATION)) {
            decString = Long.toUnsignedString(GDR.parse_unsigned("0b" + binString));
        }
        else {
            decString = String.valueOf(GDR.parse_long("0b" + binString));
        }
        final String hexString = BinOctHexUtility.toHexFromBin(binString);
        String[] matchStrings;

        // also prep dictVal in case we can't find an actual enum value.
        // use the default bit value format if the value isn't valid.
        switch(bitValueFormat) {
            case DECIMAL:
                matchStrings = new String[] { decString };
                dictVal = decString;
                break;
            case BINARY:
                matchStrings = new String[] { binString };
                dictVal = BinOctHexUtility.BINARY_STRING_PREFIX1 + hexString;
                break;
            case HEX:
                matchStrings = new String[] { hexString };
                dictVal = BinOctHexUtility.HEX_STRING_PREFIX1 + hexString;
                break;
            case UNSPECIFIED:
            default:
                matchStrings = new String[] { binString, decString, hexString };
                //use hex if we have no idea
                dictVal = BinOctHexUtility.HEX_STRING_PREFIX1 + hexString;
        }


        //MPCS-10745 - 17/15/19 - replaced the complicated lookup with the now easier one.

        for (final String candidate : matchStrings) {
            if(def.getEnumeration().lookupByBitValue(candidate) != null) {
                return def.getEnumeration().lookupByBitValue(candidate).getDictionaryValue();
            }
        }

        return dictVal;
    }


    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {
        return toBitString(def, null, argValue);
    }

    /**
     * Convert the specified enumeration value into a bit string
     * @param def the ICommandArgumentDefinition of the argument
     * @param enumValue the ICommandEnumeration of the argument value
     * @return the bitString of the argument value
     * @throws BlockException if there's an error converting the argument value into a bit string
     */
    public String toBitString(final ICommandArgumentDefinition def, final ICommandEnumerationValue enumValue) throws BlockException {
        return toBitString(def, enumValue, null);
    }

    /**
     * Convert the specified argument values into a bit string. This is the core toBitString
     * logic brought over from BaseEnumeratedArgument
     * @param def the ICommandArgumentDefinition of the argument
     * @param argValue the String representation of the argument value
     * @param enumValue the ICommandEnumeration of the argument value
     * @return the bitString of the argument value
     * @throws BlockException if there's an error converting the argument value into a bit string
     */
    public String toBitString(final ICommandArgumentDefinition def, final ICommandEnumerationValue enumValue, final String argValue) throws BlockException {
        if (enumValue == null && argValue == null) {
            throw new BlockException(
                    "Cannot get the bit representation of the enumerated argument (dictionary name = \""
                            + def.getDictionaryName()
                            + "\") because its value is NULL.");
        }

        // get the numeric value of the selected enum value
        String bitValueStr;
        if (enumValue != null) {
            bitValueStr = enumValue.getBitValue();
        } else if (argValue != null) {
            ICommandEnumerationValue enumVal = getEnumValue(argValue, def.getEnumeration());
            bitValueStr = enumVal != null ? enumVal.getBitValue() : argValue;
        } else {
            throw new BlockException(
                    "Cannot get the bit represenation of the enumerated argument (dictionary name = \""
                            + def.getDictionaryName()
                            + "\") because the argument value is NULL.");
        }

        // convert the bit value to an int
        long value;
        try {
            /*
             * GDR.parse_int only worked if the supplied value was a valid signed integer value.
             * If it was given an unsigned integer value over (2^31)-1 or a long that was not a valid
             * integer, then it would choke.
             */
            if(def.getType().equals(CommandArgumentType.UNSIGNED_ENUMERATION)) {
                value = GDR.parse_unsigned(bitValueStr);
            } else {
                value = GDR.parse_long(bitValueStr);
            }
        } catch (final NumberFormatException nfe) {
            throw new BlockException(
                    "Cannot get the bit represenation of the enumerated argument (dictionary name = \""
                            + def.getDictionaryName()
                            + "\") because the bit value of  \""
                            + bitValueStr
                            + "\" is not a valid value.", nfe);
        }

        // get the bit string and pad it out if need be
        String bitString = Long.toBinaryString(value);
        final int bitLength = def.getBitLength();
        if (bitString.length() > bitLength) {
            bitString = bitString.substring(bitString.length() - bitLength);
        } else {
            bitString = GDR.fillStr(bitString, bitLength, '0');
        }

        return (bitString);
    }

    // helper function for converting an argument string to an ICommandEnumerationValue
    private ICommandEnumerationValue getEnumValue(String argValue, final CommandEnumerationDefinition enumDef) {

        ICommandEnumerationValue retVal = enumDef.lookupByDictionaryValue(argValue);

        if(retVal != null) {
            return retVal;
        }

        retVal = enumDef.lookupByBitValue(argValue);

        if(retVal != null) {
            return retVal;
        }

        return enumDef.lookupByFswValue(argValue);
    }

}
