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
package jpl.gds.dictionary.impl.command;

import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.SignedEnumeratedValue;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * A utility class for static functions related to command dictionary 
 * processing.
 * 
 *
 */
public final class CommandParserUtil {

    /**
     * Default bit value representing a "true" enumeration value.
     * Comes out as 0xFF... when encoded. 
     */
    private static final String TRUE_VALUE = "-1"; 
    /**
     * Default bit value representing a "false" enumeration value. 
     */
    private static final String FALSE_VALUE = "0";

    /** 
     * Default dictionary value for "true" enumeration value
     */
    public static final String TRUE_STRING = "TRUE";

    /** 
     * Default dictionary value for "false" enumeration value
     */
    public static final String FALSE_STRING = "FALSE";

    /**
     * Private constructor to enforce static nature.
     */
    private CommandParserUtil() {
        SystemUtilities.doNothing();
    }

    /**
     * Re-creates the boolean enumeration in the given command argument definition.
     * 
     * @param arg the command argument definition for the boolean argument
     * @param trueFormat the string used for display of TRUE values
     * @param falseFormat the string used for display of FALSE values
     */
    public static void setBooleanEnumValues(ICommandArgumentDefinition arg, final String trueFormat,final String falseFormat)
    {

        if (arg.getType() != CommandArgumentType.BOOLEAN) {
            throw new IllegalArgumentException("Definition is not for a boolean argument");
        }

        arg.getEnumeration().clearAllValues();

        SignedEnumeratedValue trueValue = new SignedEnumeratedValue();
        trueValue.setBitValue(TRUE_VALUE);
        trueValue.setDictionaryValue(trueFormat);
        trueValue.setFswValue(TRUE_STRING);
        arg.getEnumeration().addEnumerationValue(trueValue);

        SignedEnumeratedValue falseValue = new SignedEnumeratedValue();
        falseValue.setBitValue(FALSE_VALUE);
        falseValue.setDictionaryValue(falseFormat);
        falseValue.setFswValue(FALSE_STRING);
        arg.getEnumeration().addEnumerationValue(falseValue);
    }

}
