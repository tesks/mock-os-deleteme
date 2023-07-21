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

import java.util.List;

import jpl.gds.dictionary.api.command.CommandEnumerationDefinition;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Convert a bitmask argument value to/from a binary string
 *
 */
public class LegacyBaseBitmaskArgumentTranslator implements ICommandArgumentTranslator {

    private static final String SEPARATOR_STRING = "|";

    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {

        String[] args = argValue.split("\\" + SEPARATOR_STRING);


        ICommandEnumerationValue enumVal;
        String bitVal;

        long tmpVal = 0;

        for(String arg : args) {
           enumVal = getEnumValue(arg.trim(), def.getEnumeration());
           bitVal = enumVal != null ? enumVal.getBitValue() : "0";
           tmpVal |= Long.parseLong(bitVal);
        }

        String bitString = Long.toBinaryString(tmpVal);
        int bitLength = def.getBitLength();

        if (bitString.length() > bitLength) {
            bitString = bitString.substring(bitString.length() - bitLength);
        } else {
            bitString = GDR.fillStr(bitString, bitLength, '0');
        }

        return (bitString);
    }

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString) {

        int bitLength = def.getBitLength();
        String valueBits = bitString.read(bitLength);

        if (valueBits.length() < bitLength) {
            valueBits = GDR.fillStr(valueBits,Long.SIZE,'0');

        } else {
            valueBits = GDR.fillStr(valueBits,Long.SIZE,valueBits.charAt(0));
        }

        byte[] valueBytes = BinOctHexUtility.toBytesFromBin(valueBits);

        return String.valueOf(GDR.get_i64(valueBytes,0));
    }

    private ICommandEnumerationValue getEnumValue(String argValue, final CommandEnumerationDefinition enumDef) {
        List<ICommandEnumerationValue> enumValues = enumDef.getEnumerationValues();
        for(ICommandEnumerationValue enumeration : enumValues) {
            if(enumeration.getDictionaryValue().equalsIgnoreCase(argValue) || enumeration.getFswValue().equalsIgnoreCase(argValue) || enumeration.getBitValue().equalsIgnoreCase(argValue)) {
                return enumeration;
            }
        }
        return null;
    }
}
