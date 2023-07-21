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

import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.types.AmpcsStringBuffer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.IRepeatCommandArgument;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.exception.ArgumentParseException;
import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 * Converts a repeat argument block to/from a bit string.
 * A repeat argument definition specifies a list of arguments.
 * A repeat argument consists of a value specifying the number of times these arguments are repeated
 * and the instances of all of the repeated arguments.
 *
 */
public class LegacyBaseRepeatArgumentTranslator implements ICommandArgumentTranslator {

    private final LegacyArgumentTranslator lat;

    public LegacyBaseRepeatArgumentTranslator(final ApplicationContext appContext) {
       lat = new LegacyArgumentTranslator(appContext);
    }

    @Override
    public String parseFromBitString(final ICommandArgumentDefinition def, final AmpcsStringBuffer bitString)
            throws UnblockException {

        IRepeatCommandArgumentDefinition repeatArgDef = (IRepeatCommandArgumentDefinition)def;

        String argValue;
        StringBuilder argumentString = new StringBuilder();
        

        final int bitLength = def.getBitLength();
        if (bitString.available() < bitLength) {
            throw new UnblockException("Could not read in the argument "
                    + def.getDictionaryName()
                    + " because it has a bit length of "
                    + def.getBitLength() + " bits, but only "
                    + (bitString.available()) + " bits were present.");
        }

        // need to see how many times the argument repeats before we know how
        // much
        // of the rest of the bit string to read in
        final String repeatBits = bitString.read(bitLength);

        argValue = ((Integer.valueOf(Integer.parseInt(repeatBits,
                BinOctHexUtility.BINARY_RADIX))).toString());

        argumentString.append(argValue);

        Integer argumentIntValue;
        try {
            argumentIntValue = formatValueAsComparable(
                    argValue, def.getBitLength()).intValue();
        } catch (final ArgumentParseException e) {
            throw new UnblockException(e);
        }

        // read in all of the sub-argument values
        final List<ICommandArgumentDefinition> dictionaryArguments = ((IRepeatCommandArgumentDefinition)def)
                .getDictionaryArguments();

        final int totalValues = dictionaryArguments.size() * argumentIntValue;

        if (bitString.available() > 0) {

            ICommandArgumentDefinition subDef;
            final int repeatCount = repeatArgDef.getDictionaryArguments().size();

            for (int i = 0; i < totalValues; i++) {

                try {
                    subDef = repeatArgDef.getDictionaryArguments().get(i%repeatCount);

                    String subArgValue = lat.parseFromBitString(subDef, bitString);

                    argumentString.append(IRepeatCommandArgument.SEPARATOR_STRING);
                    argumentString.append(subArgValue);
                } catch (final UnblockException ue) {
                    throw new UnblockException(
                            "Could not read in the argument "
                                    + def.getDictionaryName()
                                    + " for the array/repeat argument "
                                    + def.getDictionaryName()
                                    + ": " + ue.getMessage(), ue);
                }
            }
        }

        return argumentString.toString();
    }

    @Override
    public String toBitString(final ICommandArgumentDefinition def, final String argValue) throws BlockException {

        if(argValue == null) {
            throw new BlockException("Supplied argument value must not be null");
        }

        IRepeatCommandArgumentDefinition repeatArgDef = (IRepeatCommandArgumentDefinition)def;
        final int repeatCount = repeatArgDef.getDictionaryArgumentCount(false);

        String[] args = null;
        String[] subArgValues;

        try {
                    args = UplinkInputParser.splitCommandString(argValue, IRepeatCommandArgument.SEPARATOR_STRING);
        } catch (CommandParseException e) {
            //do nothing, it'll be caught below
        }

        String mainArgValue = args[0];

        StringBuilder buffer = new StringBuilder(8192);
        Integer argumentIntValue;
        try {
            argumentIntValue = formatValueAsComparable(
                    mainArgValue, def.getBitLength()).intValue();
        } catch (final ArgumentParseException e) {
            throw new BlockException(e);
        }

        String tmpBits = Integer.toBinaryString(argumentIntValue);
        if (tmpBits.length() < def.getBitLength()) {
            tmpBits = GDR.fillStr(tmpBits, def
                    .getBitLength(), '0');
        }
        buffer.append(tmpBits);

        if (args.length > 1) {

            subArgValues = Arrays.copyOfRange(args, 1, args.length);

            for (int i = 0; i < subArgValues.length; i++) {
                ICommandArgumentDefinition subArgDef = repeatArgDef.getDictionaryArguments().get(i % repeatCount);

                tmpBits = lat.toBitString(subArgDef, subArgValues[i]);
                buffer.append(tmpBits);
            }
        }

        return (buffer.toString());
    }


    protected Number formatValueAsComparable(final String inputVal, final int bitLength) throws ArgumentParseException
    {
        try
        {
            if(bitLength <= Byte.SIZE)
            {
                return(Byte.valueOf(GDR.parse_byte(inputVal)));
            }
            else if(bitLength <= Short.SIZE)
            {
                return(Short.valueOf(GDR.parse_short(inputVal)));
            }
            else if(bitLength <= Integer.SIZE)
            {
                return(Integer.valueOf(GDR.parse_int(inputVal)));
            }

            return(Long.valueOf(GDR.parse_long(inputVal)));
        }
        catch(final Exception e)
        {
            throw new ArgumentParseException("Could not interpret input value "
                    + inputVal,e);
        }
    }

}
