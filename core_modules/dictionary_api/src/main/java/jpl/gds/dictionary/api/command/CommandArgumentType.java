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
package jpl.gds.dictionary.api.command;


/**
 * The CommandArgumentType enumeration defines the data type of an
 * ICommandArgumentDefinition, indicating whether it is numeric (float, integer,
 * etc), boolean, string, time, etc. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 *
 *
 */
public enum CommandArgumentType {
    /**
     * Argument data type is spacecraft time, as SCLK.
     */
    TIME,
    /**
     * Argument data type is spacecraft time, as FLOAT.
     */
    FLOAT_TIME,
    /**
     * Argument data type is floating point.
     */
    FLOAT,
    /**
     * Argument data type is signed integer.
     */
    INTEGER,
    /**
     * Argument data type is unsigned integer.
     */
    UNSIGNED,
    /**
     * Argument data type is a signed enumeration.
     */
    SIGNED_ENUMERATION,
    /**
     * Argument data type is an unsigned enumeration.
     */
    UNSIGNED_ENUMERATION,
    /**
     * Argument data type is fixed length string.
     */
    FIXED_STRING,
    /**
     * Argument data type is variable length string.
     */
    VAR_STRING,
    /**
     * Argument data type is boolean.
     */
    BOOLEAN,
    /**
     * Argument contains fill data.
     */
    FILL,
    /**
     * Argument is a repeating block of sub-arguments.
     */
    REPEAT,
    /**
     * Argument is comprised of multiple values consolidated into a single one
     */
    BITMASK,
    /**
     * Argument type is undefined. Should only be used in uninitialized
     * ICommandArgumentDefinition objects.
     */
    UNDEFINED;

    /**
     * Indicates whether this type refers to a floating point time
     * argument.
     * 
     * @return true if a float time, false otherwise
     */
    public boolean isFloatTime() {
        return this.equals(FLOAT_TIME);
    }

    /**
     * Indicates whether this type refers to a variable length
     * argument.
     * 
     * @return true if a variable length type, false otherwise
     */
    public boolean isVariableLength() {
        return this.equals(REPEAT) || this.equals(VAR_STRING);
    }

    /**
     * Indicates whether this type refers to any type of time
     * argument.
     * 
     * @return true if a time, false otherwise
     */
    public boolean isTime() {
        return this.equals(TIME) || this.equals(FLOAT_TIME);
    }

    /**
     * Indicates whether this type refers to any type of enumerated
     * argument.
     * 
     * @return true if an enumeration, false otherwise
     */
    public boolean isEnumeration() {
        return this.equals(SIGNED_ENUMERATION) || this.equals(UNSIGNED_ENUMERATION) || this.equals(BOOLEAN);
    }

    /**
     * Indicates whether this type refers to any type of string
     * argument.
     * 
     * @return true if a string, false otherwise
     */
    public boolean isString() {
        return this.equals(VAR_STRING) || this.equals(FIXED_STRING);
    }
    
    /**
     * Indicates if this type refers to any type of numeric
     * argument.
     * @return true is a number, false otherwise
     */
    public boolean isNumeric() {
    	return this.equals(INTEGER) || this.equals(FLOAT) || this.equals(UNSIGNED) || this.equals(REPEAT) || isTime() || this.equals(BITMASK);
    }
    
    /**
     * Indicates if this type refers to a repeat argument.
     * @return true is a repeat, false otherwise
     */
    public boolean isRepeat() {
    	return this.equals(REPEAT);
    }
    
    /**
     * indicates if this type refers to a repeat argument.
     * @return true if a fill argument, false otherwise.
     */
    public boolean isFill() {
    	return this.equals(FILL);
    }
    
    /**
     * indicates if this type refers to a float value.
     * @return true if a floating point value, false otherwise.
     */
    public boolean isFloat() {
        return this.equals(FLOAT) || this.equals(FLOAT_TIME);
    }

    /**
     * Static method to return an CommandArgumentType given a string. Differs from
     * the built-in valueOf() method because it attempts conversion to upper
     * case if the supplied value fails to map to one of the values in this
     * class as is, and it also detects some strings from older dictionary
     * schemas, such as UINT for UNSIGNED, ENUM for ENUMERATION, etc.
     * 
     * @param strVal
     *            the string value to convert to an ArgumentType. All new
     *            invocations should only supply arguments whose value matches
     *            the toString() of one of the enumeration values in this class.
     * 
     * @return CommandArgumentType instance
     * 
     * @throws IllegalArgumentException
     *             if the input string value cannot be mapped to one of the
     *             enumeration values.
     */
    public static CommandArgumentType getValueFromString(final String strVal) 
            throws IllegalArgumentException {

        final CommandArgumentType result = null;

        try {
            /*
             * First try a straight valueOf() on the input argument
             */
            return valueOf(strVal);

        } catch (final IllegalArgumentException e1) {
            try {
                /*
                 * That didn't work, so try valueOf with the upper case version
                 * of the input argument.
                 */
                return valueOf(strVal.toUpperCase());

            } catch (final IllegalArgumentException e2) {

                /*
                 * Perform special checks. Loop through all the valid values.
                 */
                final CommandArgumentType[] values = values();
                for (final CommandArgumentType avt : values) {

                    /*
                     * If the input argument (upper case) matches the start of
                     * the value value, or vice-versa, call it a match and
                     * return the current enum value.
                     */
                    if (strVal.toUpperCase().startsWith(avt.toString())
                            || avt.toString().startsWith(strVal.toUpperCase())) {
                        return avt;
                    }
                }

                /*
                 * Special cases for older dictionaries.
                 */
                /*
                 * Added more cases to
                 * account for removed types that still exist in 
                 * older dictionaries.
                 */
                if (strVal.toUpperCase().startsWith("UINT")
                        || strVal.toUpperCase().startsWith("UNSIGNED")) {
                    return UNSIGNED;
                } else if (strVal.toUpperCase().startsWith("SCLK")) {
                    return TIME;
                } else if (strVal.toUpperCase().startsWith("STATUS")) {
                    return SIGNED_ENUMERATION;            
                } else if (strVal.toUpperCase().startsWith("DOUBLE")) {
                    return FLOAT;
                } else if (strVal.toUpperCase().startsWith("SHORT")) {
                    return INTEGER;
                } else if (strVal.toUpperCase().startsWith("LONG")) {
                    return INTEGER;
                }
            }
        }

        return result;

    }
}
