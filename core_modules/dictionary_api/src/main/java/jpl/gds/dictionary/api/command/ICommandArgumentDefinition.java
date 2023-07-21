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

import java.util.List;

import jpl.gds.dictionary.api.IAttributesSupport;

/**
 * The ICommandArgumentDefinition interface is to be implemented by all command
 * argument classes. <br>
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * The Command dictionary is used by uplink applications to formulate and
 * validate spacecraft or SSE commands. A particular command's arguments,
 * format, and requirements are defined in the project's command dictionary.
 * Every mission may have a different format for representing the command
 * dictionary. An appropriate dictionary parser must be used in order to create
 * the mission-specific ICommandArgumentDefinition objects, which MUST implement
 * this interface.
 * <p>
 *
 *
 *
 * @see ICommandDefinition
 */
public interface ICommandArgumentDefinition extends IAttributesSupport {

    /** The special for a numeric range that indicates the minimum conceivable value is the
     * minimum that should be used for validation.
     */
    public static final String MINIMUM_STRING = "MIN";

    /** 
     * The special for a numeric range that indicates the maximum conceivable value is the
     * maximum that should be used for validation.
     */
    public static final String MAXIMUM_STRING = "MAX";

    /**
     * Gets the data type of this argument.
     * <p>
     * In the multimission command dictionary, the data type of a command
     * argument is first determined by which XML element is used to define the
     * argument, e.g., &lt;boolean_arg&gt;, &lt;numeric_arg&gt;. In the case of
     * numeric arguments, the type is further classified via the "type"
     * attribute on the &lt;numeric_arg&gt; element.
     * 
     * @return the data type as an CommandArgumentType enumeration value.
     */
    public abstract CommandArgumentType getType();

    /**
     * Sets the data type of this argument.
     * <p>
     * In the multimission command dictionary, the data type of a command
     * argument is first determined by which XML element is used to define the
     * argument, e.g., &lt;boolean_arg&gt;, &lt;numeric_arg&gt;, etc. In the case of
     * numeric arguments, the type is further classified via the "type"
     * attribute on the &lt;numeric_arg&gt; element.
     *
     * @param type the CommandArgumentType enumeration value to set
     */
    public abstract void setType(CommandArgumentType type);

    /**
     * Gets the default value for this argument. A default value is optional.
     * The value is a string, but must be parseable as the proper data type for
     * the argument. Default value applies to all types of arguments except
     * repeat arguments, for which it will be null. For fill arguments, the
     * default value is the fill value.
     * <p>
     * In the multimission command dictionary, the data type of a command
     * argument is specified using the &lt;default_value&gt; element under the
     * specific command argument element.
     * 
     * @return The default value of this argument, or null if it doesn't have
     *         one
     */
    public abstract String getDefaultValue();

    /**
     * Sets the default value for this argument. A default value is optional.
     * The value is a string, but must be parseable as the proper data type for
     * the argument. Default value applies to all types of arguments except
     * repeat arguments, for which it will be null. For fill arguments, the
     * default value is the fill value.
     * <p>
     * In the multimission command dictionary, the data type of a command
     * argument is specified using the &lt;default_value&gt; element under the
     * specific command argument element.
     * 
     * @param value the default value this argument should have, or null
     * if it shouldn't have one
     */
    public abstract void setDefaultValue(final String value);

    /**
     * Returns the dictionary name of this argument, which is used for argument
     * display. The dictionary name is mandatory for all arguments.
     * <p>
     * In the multimission command dictionary, the dictionary name of a command
     * argument is specified using the "name" attribute on the specific command
     * argument element.
     * 
     * 
     * @return the dictionary name; never null
     */
    public abstract String getDictionaryName();

    /**
     * Sets the dictionary name of this argument, which is used for argument
     * display. The dictionary name is mandatory for all arguments.
     * <p>
     * In the multimission command dictionary, the dictionary name of a command
     * argument is specified using the "name" attribute on the specific command
     * argument element.
     *
     * @param dictionaryName the dictionary name to set; may not be null
     */
    public abstract void setDictionaryName(final String dictionaryName);

    /**
     * Gets the bit length of this argument, indicating the number of encoded
     * bits that must be transmitted to the spacecraft for this argument's
     * value. For string arguments, this is the number of characters multiplied
     * by 8. Bit length does not apply to repeat arguments.
     * <p>
     * In the multimission command dictionary, for most types of arguments, the
     * dictionary name of a command argument is specified using the "bit_length"
     * attribute on the specific command argument element. In the case of
     * variable length string arguments, it is "max_bit_length" instead.
     * 
     * @return The length in bits of this argument
     */
    public abstract int getBitLength();

    /**
     * Sets the bit length of this argument, indicating the number of encoded
     * bits that must be transmitted to the spacecraft for this argument's
     * value. For string arguments, this is the number of characters multiplied
     * by 8. Bit length does not apply to repeat arguments.
     * <p>
     * In the multimission command dictionary, for most types of arguments, the
     * dictionary name of a command argument is specified using the "bit_length"
     * attribute on the specific command argument element. In the case of
     * variable length string arguments, it is "max_bit_length" instead.
     *
     * @param length
     *            the length in bits of the argument, which must be greater than 0
     */
    public abstract void setBitLength(final int length);

    /**
     * Indicates whether or not this argument has a variable length.
     * <p>
     * In the multimission command dictionary, variable length string arguments
     * and repeat arguments are considered variable in length;
     * 
     * @return true if this argument is variable length (e.g. string, array,
     *         etc.), or false otherwise (e.g. numeric).
     */
    public abstract boolean isVariableLength();

    /**
     * Sets the units of this argument. Units are mandatory for numeric and time
     * arguments. For other argument types, the value may or may not be null.
     * <p>
     * In the multimission command dictionary, the units of a command argument
     * are specified using the "units" attribute on the specific command
     * argument element.
     * 
     * @param units
     *            a string representation of the units for this argument; may be
     *            null
     */
    public abstract void setUnits(String units);

    /**
     * Gets the units of this argument. Units are mandatory for numeric and time
     * arguments. For other argument types, the value may or may not be null.
     * <p>
     * In the multimission command dictionary, the units of a command argument
     * are specified using the "units" attribute on the specific command
     * argument element.
     * 
     * @return a string representation of the units for this argument; may be null
     */
    public abstract String getUnits();

    /**
     * Gets the description of this argument. Description is optional (may be null)
     * for all command arguments. It may be multi-line, and is unlimited in length.
     * <p>
     * In the multimission command dictionary, the description of a command argument
     * is specified using the &lt;description&gt; element under the specific command
     * argument element.
     * 
     * @return the description string; may be null
     */
    public abstract String getDescription();

    /**
     * Sets the description of this argument. Description is optional (may be null)
     * for all command arguments. It may be multi-line, and is unlimited in length.
     * <p>
     * In the multimission command dictionary, the description of a command argument
     * is specified using the &lt;description&gt; element under the specific command
     * argument element.
     * 
     * @param desc the description string to set; may be null
     */
    public abstract void setDescription(String desc);

    /**
     * Gets the validation regular expression for a string argument. If this is
     * set, the argument value must match this regular expression. Applies only
     * to string arguments, and will be null for all other argument types.
     * <p>
     * In the multimission command dictionary, the valid_regex of a command
     * argument is specified using the &lt;valid_regex&gt; element under the
     * &lt;fixed_string_arg&gt; and &lt;var_string_arg&gt; elements.
     * 
     * @return the validation regular expression for this argument; may be null
     */
    public abstract String getValueRegexp();

    /**
     * Sets the validation regular expression for a string argument. If this is
     * set, the argument value must match this regular expression. Applies only
     * to string arguments, and will be null for all other argument types.
     * <p>
     * In the multimission command dictionary, the valid_regex of a command
     * argument is specified using the &lt;valid_regex&gt; element under the
     * &lt;fixed_string_arg&gt; and &lt;var_string_arg&gt; elements.
     *
     * @param valueRegexp the regular expression to set; may be null
     */
    public abstract void setValueRegexp(final String valueRegexp);

    /**
     * Get the minimum allowable value for this argument. This does not include
     * the validation ranges defined for the argument. This field is solely
     * based on the type and the bit length of the argument.
     * <p>
     * There is no XML corresponding to the minimum argument value in the
     * multimission command dictionary. This value is always computed.
     * 
     * @return The minimum conceivable value for this argument. Data type
     *         returned will be appropriate for argument type, and may be null
     *         if there is no minimum.
     */
    public abstract Object getMinimumValue();

    /**
     * Get the maximum allowable value for this argument. This does not include
     * the validation ranges defined for the argument. This field is solely
     * based on the type and the bit length of the argument.
     * <p>
     * There is no XML corresponding to the maximum argument value in the
     * multimission command dictionary. This value is always computed.
     * 
     * @return The maximum conceivable value for this argument. Data type
     *         returned will be appropriate for argument type, and may be null
     *         if there is no maximum.
     */
    public abstract Object getMaximumValue();

    /**
     * Gets the valid ranges for this argument's value. Ranges apply to numeric,
     * time, and enumerated arguments. For a repeat argument, there will be one
     * range, indicating the minimum and maximum number of times the argument
     * block can repeat. Ranges do not apply to other argument types.
     * <p>
     * In the multimission command dictionary, ranges are specified using the
     * &lt;range_of_values&gt; element under the specific command argument
     * element. For repeat arguments, the range is specified using the "min" and
     * "max" attributes under the &lt;repeat&gt; element.
     * 
     * @return The list of valid ranges for this argument; list will be empty,
     *         rather than null, if no ranges are defined.
     */
    public abstract List<IValidationRange> getRanges();

    /**
     * Adds a valid range to this argument. Ranges apply to numeric, time, and
     * enumerated arguments. For a repeat argument, there will be one range,
     * indicating the minimum and maximum number of times the argument block can
     * repeat, but it is not necessary to explicitly add this range (will be
     * done automatically). Ranges do not apply to other argument types.
     * <p>
     * If the minimum value in this range is set to MINIMUM_STRING, the minimum
     * value will be converted to the minimum value implied by the data type and
     * bit length of the argument. If the maximum value is set to
     * MAXIMUM_STRING, the maximum value will be converted to the maximum value
     * implied by the data type and bit length. For an enumeration, these values
     * will be converted to the maximum and minimum enumeration values in the
     * enumeration table (minimum and maximum bit values).
     * <p>
     * In the multimission command dictionary, ranges are specified using the
     * &lt;range_of_values&gt; element under the specific command argument
     * element. For repeat arguments, the range is specified using the "min" and
     * "max" attributes under the &lt;repeat&gt; element.
     * 
     * @param r
     *            the new range to add 
     */
    public abstract void addRange(final IValidationRange r);

    /**
     * Clears the ranges attached to this command argument. The getRanges() method
     * will then return an empty list.
     * 
     */
    public abstract void clearRanges();

    /**
     * A utility method to get a string representing the valid range and units
     * of this argument.
     * 
     * @param includeUnits true to include units in the result string, false to not
     * @return a string indicating the valid argument range, e.g., "(1 to 100)".
     */
    public abstract String getRangeString(final boolean includeUnits);

    /**
     * Sets the dictionary "typedef" or "enumeration" name associated
     * with this argument, if any. Applies only to enumerated arguments.
     * Will be "Default" for boolean arguments, and null for all other types.
     * <p>
     * In the multimission command dictionary, the enumeration name of
     * a command argument is specified using the "enum_name" attribute
     * on the &lt;enum_arg&gt; element.
     * 
     * @param type the type name to set; may be null
     */
    public abstract void setTypeName(String type);

    /**
     * Gets the dictionary "typedef" or "enumeration" name associated
     * with this argument, if any. Applies only to enumerated arguments.
     * Will be "Default" for boolean arguments, and null for all other types.
     * <p>
     * In the multimission command dictionary, the enumeration name of
     * a command argument is specified using the "enum_name" attribute
     * on the &lt;enum_arg&gt; element.
     * 
     * @return the type name; may be null
     */
    public abstract String getTypeName();


    /**
     * Gets the command enumeration definition object associated with this
     * argument. Applies only to enumerated and boolean arguments and will be
     * null for all others.
     * <p>
     * In the multimission command dictionary, an enumeration is defined using
     * the &lt;enum_table&gt; element.
     * 
     * @return CommandEnumerationDefinition object, or null, since not all
     *         arguments have enumerations.
     */
    public abstract CommandEnumerationDefinition getEnumeration();

    /**
     * Sets the command enumeration definition object associated with this
     * argument. Applies only to enumerated and boolean arguments and will be
     * null for all others.
     * <p>
     * In the multimission command dictionary, an enumeration is defined using
     * the &lt;enum_table&gt; element.
     * 
     * @param def CommandEnumerationDefinition object to set; may be null
     * 
     */
    public abstract void setEnumeration(CommandEnumerationDefinition def);

    /**
     * Sets the flight software name of this command argument. The flight
     * software name is an optional name used to identify the argument. It is
     * not supported by the multimission command dictionary (AMPCS uses
     * the dictionary name, not the flight software name) but it remains here
     * for backward compatibility with other command dictionary schemas.
     * 
     * @param name
     *            the FSW name to set; may be null
     * 
     */
    public abstract void setFswName(String name);

    /**
     * Gets the flight software name of this command argument. The flight
     * software name is an optional name used to identify the argument. It is
     * not supported by the multimission command dictionary (AMPCS uses
     * the dictionary name, not the flight software name) but it remains here
     * for backward compatibility with other command dictionary schemas.
     * 
     * @return the FSW name; may be null
     * 
     */
    public String getFswName();


    /**
     * Gets the bit length of the string prefix for a variable length string.
     * The string prefix contains an unsigned integer that supplied the actual
     * length of the string argument in bytes. Not applicable to other types of
     * arguments. Supported lengths are 8, 16, and 32 bits.
     * <p>
     * In the multimission command dictionary, the string prefix length is
     * defined using the "prefix_bit_length" attribute on the
     * &lt;var_string_arg&gt; element.
     * 
     * @return bit length of the prefix
     */
    public abstract int getPrefixBitLength();

    /**
     * Sets the bit length of the string prefix for a variable length string.
     * The string prefix contains an unsigned integer that supplied the actual
     * length of the string argument in bytes. Not applicable to other types of
     * arguments. Supported lengths are 8, 16, and 32 bits.
     * <p>
     * In the multimission command dictionary, the string prefix length is
     * defined using the "prefix_bit_length" attribute on the
     * &lt;var_string_arg&gt; element.
     * 
     * @param prefixBitLength
     *            the bit length to set
     */
    public abstract void setPrefixBitLength(final int prefixBitLength);
}