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

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.command.CommandEnumerationDefinition;
import jpl.gds.dictionary.api.command.CommonValidationRange;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.SignedEnumeratedValue;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.SystemUtilities;


/**
 * This is a static utility class used to parse XML attributes for the
 * multimission command dictionary parser and put the parsed values into
 * appropriate command dictionary objects.
 * 
 */
public final class MultimissionCommandParserUtil {

    private static final String MISSING = "Missing ";
    private static final String FOR = ") for ";
    private static final String ATTR_FOR_ARG = " attribute for argument with name ";

    /** The name for the stem attribute in the XML */
    private static final String STEM_ATTRIBUTE_NAME = "stem";

    /** The name of the opcode attribute in the XML */
    private static final String OPCODE_ATTRIBUTE_NAME = "opcode";

    /** The name of the class attribute in the XML */
    private static final String CLASS_ATTRIBUTE_NAME = "class";

    /** The name of the default value attribute in the XML */
    private static final String DEFAULT_VALUE_ATTRIBUTE_NAME = "default_value";

    /** The name of the units attribute in the XML */
    private static final String UNITS_ATTRIBUTE_NAME = "units";

    /** The name of the bit length attribute in the XML */
    private static final String LENGTH_ATTRIBUTE_NAME = "bit_length";

    /** The name of the prefix bit length attribute in the XML */
    private static final String PREFIX_LENGTH_ATTRIBUTE_NAME = "prefix_bit_length";

    /** The name of the maximum bit length attribute in the XML */
    private static final String MAXIMUM_LENGTH_ATTRIBUTE_NAME = "max_bit_length";

    /** The name of the dictionary name attribute in the XML */
    private static final String DICTIONARY_NAME_ATTRIBUTE_NAME = "name";

    /** The name of the enum dictionary value attribute in the XML */
    private static final String DICTIONARY_VALUE_ATTRIBUTE_NAME = "symbol";

    /** The name of the enum bit value attribute in the XML */
    private static final String BIT_VALUE_ATTRIBUTE_NAME = "numeric";

    /** The name of the range minimum attribute in the XML */
    private static final String MINIMUM_ATTRIBUTE_NAME = "min";

    /** The name of the range maximum value attribute in the XML */
    private static final String MAXIMUM_ATTRIBUTE_NAME = "max";

    /** The name of the enum name attribute for enum args in the XML */
    private static final String ENUM_NAME_ATTRIBUTE_NAME = "enum_name";

    /** The name of the boolean true format attribute in the XML */
    private static final String TRUE_FORMAT_ATTRIBUTE_NAME = "true_str";

    /** The name of the boolean false format attribute in the XML */
    private static final String FALSE_FORMAT_ATTRIBUTE_NAME = "false_str";

    /** The name of the fill argument fill value attribute in the XML */
    private static final String FILL_VALUE_ATTRIBUTE_NAME = "fill_value";

    private static final Tracer log                             = TraceManager.getDefaultTracer();

    /**
     * Private constructor to enforce static nature.
     */
    private MultimissionCommandParserUtil() {

        SystemUtilities.doNothing();
    }

    /**
     * Sets members in a command definition object from the attributes on the
     * XML element for the command.
     * 
     * @param attr
     *            SAX Attributes object
     * @param command
     *            the command definition object to update
     * @param opcodeBitLen   the bit length of the opcodes in the current
     *            mission/dictionary context
     * @param opcodeUtil opcode utility instance for the current dictionary configuration
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     *             
     */
    public static void setCommandValuesFromAttributes(final Attributes attr,
            final ICommandDefinition command, final int opcodeBitLen, final OpcodeUtil opcodeUtil) throws SAXException {

        String value = attr.getValue(STEM_ATTRIBUTE_NAME);
        if (value != null) {
            command.setStem(value);
        } else {
            throw new SAXException(
                    "Found fsw_command or hw_command element without stem attribute");
        }

        value = attr.getValue(OPCODE_ATTRIBUTE_NAME);
        if (value != null) {
            /*
             * Value must be hex before it can be set into the command. It can
             * be specified in hex or decimal in the XML. Convert here if it is
             * decimal.
             */
            if (! OpcodeUtil.hasHexPrefix(value))
            {
                try
                {
                    final int opcode = opcodeUtil.parseOpcodeFromDecimal(value);
                    value = opcodeUtil.formatOpcode(opcode, true);
                    log.trace("Opcode formatted: " , value);
                }
                catch (final NumberFormatException nfe)
                {
                    log.debug("Unable to format opcode " + nfe.getMessage(), nfe.getCause());
                    throw new SAXException(
                            "Invalid opcode attribute value for command with stem "
                                    + command.getStem());
                }
            }

            try {
                command.setOpcode(value, opcodeBitLen);
            } catch (final IllegalArgumentException e) {
                throw new SAXException(
                        "Error parsing command opcode value of " + value
                        + " for command with stem " + command.getStem(),
                        e);
            }
        } else {
            throw new SAXException("Command element for command with stem "
                    + command.getStem() + " has no opcode attribute");
        }
        value = attr.getValue(CLASS_ATTRIBUTE_NAME);
        if (value != null) {
            command.setCommandClass(value);
        } 

    }

    /**
     * Sets common members in a command argument definition object from the
     * attributes on the XML element for the command argument.
     * 
     * @param attr
     *            SAX Attributes object
     * @param arg
     *            the command argument definition object to update
     * @param lengthRequired
     *            true if bit_length is a required attribute; false if not
     * 
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setBasicArgValuesFromAttributes(final Attributes attr,
            final ICommandArgumentDefinition arg, final boolean lengthRequired)
                    throws SAXException {

        String value = attr.getValue(DICTIONARY_NAME_ATTRIBUTE_NAME);
        if (value != null) {
            arg.setDictionaryName(value);
            arg.setFswName(value);
        } else {
            throw new SAXException("Found command argument with missing "
                    + DICTIONARY_NAME_ATTRIBUTE_NAME + " attribute");
        }

        value = attr.getValue(LENGTH_ATTRIBUTE_NAME);
        if (value != null) {
            arg.setBitLength(GDR.parse_int(value));
        } else if (lengthRequired) {
            throw new SAXException(MISSING + LENGTH_ATTRIBUTE_NAME
                    + " attribute for command argument with name "
                    + arg.getDictionaryName());
        }

        value = attr.getValue(UNITS_ATTRIBUTE_NAME);
        if (value != null) {
            arg.setUnits(value);
        }

        value = attr.getValue(DEFAULT_VALUE_ATTRIBUTE_NAME);
        if (value != null) {
            arg.setDefaultValue(value);
        }
    }

    /**
     * Sets members in a variable length string command argument definition
     * object from the attributes on the XML element for the command argument.
     * 
     * @param attr
     *            SAX Attributes object
     * @param arg
     *            the command argument definition object to update
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setVarStringArgValuesFromAttributes(
            final Attributes attr, final ICommandArgumentDefinition arg)
                    throws SAXException {

        setBasicArgValuesFromAttributes(attr, arg, false);
        int prefixLength = 0;

        String value = attr.getValue(PREFIX_LENGTH_ATTRIBUTE_NAME);
        if (value != null) {
            try {
                prefixLength = GDR.parse_int(value);
                if (prefixLength != 8 && prefixLength != 16) {
                    throw new SAXException("Invalid value (" + value + FOR
                            + PREFIX_LENGTH_ATTRIBUTE_NAME
                            + ATTR_FOR_ARG
                            + arg.getDictionaryName());
                }
                arg.setPrefixBitLength(prefixLength);
            } catch (final NumberFormatException e) {
                throw new SAXException("Non-integer value (" + value + FOR
                        + PREFIX_LENGTH_ATTRIBUTE_NAME
                        + ATTR_FOR_ARG
                        + arg.getDictionaryName());
            }
        } else {
            throw new SAXException(MISSING + PREFIX_LENGTH_ATTRIBUTE_NAME
                    + " attribute for var_string_arg element with name "
                    + arg.getDictionaryName());
        }

        value = attr.getValue(MAXIMUM_LENGTH_ATTRIBUTE_NAME);
        if (value != null) {
            final int maxLength = GDR.parse_int(value);
            if (maxLength < 8 && maxLength > (Math.pow(2, prefixLength) - 1)
                    || maxLength % 8 != 0) {
                throw new SAXException("Invalid value (" + value + FOR
                        + MAXIMUM_LENGTH_ATTRIBUTE_NAME
                        + ATTR_FOR_ARG
                        + arg.getDictionaryName());
            }
            arg.setBitLength(maxLength);
        }
    }

    /**
     * Sets members in a repeat command argument definition object from the
     * attributes on the XML element for the command argument.
     * 
     * @param attr
     *            SAX Attributes object
     * @param arg
     *            the command argument definition object to update
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setRepeatArgValuesFromAttributes(final Attributes attr,
            final ICommandArgumentDefinition arg) throws SAXException {

        setBasicArgValuesFromAttributes(attr, arg, false);

        /*
         * For repeat arguments, which are variable length, the bit length is
         * the length of the encoded repeat/array prefix.
         */
        final String value = attr.getValue(PREFIX_LENGTH_ATTRIBUTE_NAME);
        if (value != null) {
            try {
                final int prefixLength = GDR.parse_int(value);
                if (prefixLength != 8 && prefixLength != 16) {
                    throw new SAXException("Invalid value (" + value + FOR
                            + PREFIX_LENGTH_ATTRIBUTE_NAME
                            + ATTR_FOR_ARG
                            + arg.getDictionaryName());
                }
                arg.setBitLength(prefixLength);
            } catch (final NumberFormatException e) {
                throw new SAXException("Non-integer value (" + value + FOR
                        + PREFIX_LENGTH_ATTRIBUTE_NAME
                        + ATTR_FOR_ARG
                        + arg.getDictionaryName());
            }
        } else {
            throw new SAXException(MISSING + PREFIX_LENGTH_ATTRIBUTE_NAME
                    + " attribute for repeat_arg element with name "
                    + arg.getDictionaryName());
        }
    }

    /**
     * Sets members in an enumerated command argument definition object from the
     * attributes on the XML element for the command argument.
     * 
     * @param attr
     *            SAX Attributes object
     * @param arg
     *            the command argument definition object to update
     * @param enumDefs
     *            the map of parsed command enumeration definition objects,
     *            keyed by enum name.
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setEnumArgValuesFromAttributes(final Attributes attr,
            final ICommandArgumentDefinition arg,
            final Map<String, CommandEnumerationDefinition> enumDefs)
                    throws SAXException {

        setBasicArgValuesFromAttributes(attr, arg, true);

        final String value = attr.getValue(ENUM_NAME_ATTRIBUTE_NAME);
        if (value != null) {
            final CommandEnumerationDefinition def = enumDefs.get(value);
            if (def == null) {
                throw new SAXException("Undefined enum table (" + value
                        + ") found for enum_arg with dictionary name "
                        + arg.getDictionaryName());
            }
            arg.setTypeName(value);
            arg.setEnumeration(def);

        } else {
            throw new SAXException(
                    "No enum_name attribute found for enum_arg with dictionary name "
                            + arg.getDictionaryName());
        }
    }

    /**
     * Sets members in a filler command argument definition object from the
     * attributes on the XML element for the command argument.
     * 
     * @param attr
     *            SAX Attributes object
     * @param arg
     *            the command argument definition object to update
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setFillerArgValuesFromAttributes(final Attributes attr,
            final ICommandArgumentDefinition arg) throws SAXException {

        setBasicArgValuesFromAttributes(attr, arg, true);
        final String value = attr.getValue(FILL_VALUE_ATTRIBUTE_NAME);
        if (value != null) {
            arg.setDefaultValue(value);
        }

    }

    /**
     * Sets members in an enumerated value object from the attributes on the XML
     * element defining the value.
     * 
     * @param attr
     *            SAX Attributes object
     * @param v
     *            the enumerated value object to update
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setEnumValuesFromAttributes(final Attributes attr,
            final SignedEnumeratedValue v) throws SAXException {

        String value = attr.getValue(DICTIONARY_VALUE_ATTRIBUTE_NAME);
        if (value != null) {
            /*
             * MM dictionary does not define fsw_name for enum values. Set it to
             * match the dictionary name here.
             */
            v.setDictionaryValue(value);
            v.setFswValue(value);
        } else {
            throw new SAXException("Found enum element with missing "
                    + DICTIONARY_VALUE_ATTRIBUTE_NAME + " attribute");
        }

        value = attr.getValue(BIT_VALUE_ATTRIBUTE_NAME);
        if (value != null) {
            v.setBitValue(value);
        } else {
            throw new SAXException("Found enum element with missing "
                    + BIT_VALUE_ATTRIBUTE_NAME + " attribute");
        }
    }

    /**
     * Sets members in an argument range object from the attributes on the XML
     * element for the range.
     * 
     * @param attr
     *            SAX Attributes object
     * @param r
     *            the argument range object to update
     * 
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setRangeValuesFromAttributes(final Attributes attr,
            final CommonValidationRange r) throws SAXException {

        String value = attr.getValue(MINIMUM_ATTRIBUTE_NAME);
        if (value != null) {
            r.setMinimum(value);
        } else {
            throw new SAXException(MISSING + MINIMUM_ATTRIBUTE_NAME
                    + " attribute on include or repeat range element");
        }

        value = attr.getValue(MAXIMUM_ATTRIBUTE_NAME);
        if (value != null) {
            r.setMaximum(value);
        } else {
            throw new SAXException(MISSING + MAXIMUM_ATTRIBUTE_NAME
                    + " attribute on include or repeat range element");
        }
    }

    /**
     * Sets members in a boolean command argument definition object from the
     * attributes on the XML element for the boolean format element.
     * 
     * @param attr
     *            SAX Attributes object
     * @param arg
     *            the command argument definition object to update
     * 
     * @throws SAXException
     *             if there is a problem with parsing values from XML attributes
     */
    public static void setBooleanFormatValuesFromAttr(final Attributes attr,
            final ICommandArgumentDefinition arg) throws SAXException {

        final String trueValue = attr.getValue(TRUE_FORMAT_ATTRIBUTE_NAME);
        if (trueValue == null) {
            throw new SAXException(
                    "Missing attribute "
                            + TRUE_FORMAT_ATTRIBUTE_NAME
                            + " for boolean_format element on command argument with name "
                            + arg.getDictionaryName());

        }
        final String falseValue = attr.getValue(FALSE_FORMAT_ATTRIBUTE_NAME);
        if (falseValue == null) {
            throw new SAXException(
                    "Missing attribute "
                            + FALSE_FORMAT_ATTRIBUTE_NAME
                            + " for boolean_format element on command argument with name "
                            + arg.getDictionaryName());
        }
        CommandParserUtil.setBooleanEnumValues(arg, trueValue, falseValue);
    }

}
