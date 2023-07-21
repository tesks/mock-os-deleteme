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
package ammos.datagen.dictionary.app;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.ParseException;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.command.CommandDefinitionType;
import jpl.gds.dictionary.api.command.CommandEnumerationDefinition;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandDictionary;
import jpl.gds.dictionary.api.command.ICommandDictionaryFactory;
import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;
import jpl.gds.dictionary.api.command.IValidationRange;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is an application that reads a command dictionary for the current AMPCS
 * mission and writes a command dictionary in the multimission XML format.
 * 
 *
 * MPCS-6235 - 6/11/14. Now extends new superclass. Some methods
 *          moved there.
 * 
 * MPCS-6387 - 7/21/2014. Ensure that each writer.writeStart is
 *          balanced by a writer.writeEnd
 * MPCS-7279 - 8/3/15 Added key/value attributes.
 * MPCS-7602 - 9/14/15 Added key/value attributes to
 *          "lesser class objects".
 * MPCS-7750 =- 10/23/15. Changed to use new command line option
 *          strategy throughput.
 */
public class CommandDictionaryConverterApp extends
        AbstractDictionaryConverterApp {

    private static final String ONE_OR_MORE_BLANKS = "[ ]+";
    private static final String UNDERSCORE = "_";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String UNITS = "units";
    private static final String DEFAULT_VALUE = "default_value";
    private static final String BIT_LENGTH = "bit_length";
    private static final String NONE = "none";

    private ICommandDictionary missionDictionary;

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.dictionary.app.AbstractDictionaryConverterApp#validateSourceSchema(java.lang.String)
     */
    @Override
    protected void validateSourceSchema(final String schemaName)
            throws ParseException {
        super.validateSourceSchema(schemaName);
        if (schemaName.equalsIgnoreCase("monitor")) {
            throw new ParseException("There is no monitor command schema");
        }
        else if (sseFlag.isApplicationSse()) {
            throw new ParseException("There is no SSE command schema");
        }
    }

    /**
     * Parses the mission-specific command dictionary.
     * 
     * @throws DictionaryException
     *             if there is a problem reading the dictionary
     */
    public void readMissionDictionary() throws DictionaryException {

        this.missionDictionary = appContext.getBean(ICommandDictionaryFactory.class)
                .getNewInstance(dictConfig, this.dictionaryPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {

        final List<ICommandDefinition> defs = this.missionDictionary
                .getCommandDefinitions();

        writer.writeStartDocument();
        writer.writeStartElement("command_dictionary");

        /*
         * MPCS-6235 - 6/11/14.Use common method to write header.

        /* MPCS-7434 - 1/29/16. Get schema version from dictionary properties, mission and scid
         * from the parsed dictionary 
         */
        writeHeaderElement(writer,
                DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.COMMAND),
                this.missionDictionary.getGdsVersionId(),
                this.missionDictionary.getMission(),
                this.missionDictionary.getSpacecraftIds());

        /* 
         * MHT - MPCS-7929 - 2/3/16 Write uplink file types.
         */
        final Set<String> fileTypes = this.missionDictionary.getUplinkFileTypes();
        if (!fileTypes.isEmpty()) {
            writer.writeStartElement("uplink_file_types");
            for (final String key : fileTypes) {
                writer.writeStartElement("file_type");
                writer.writeAttribute("name", key);                
                writer.writeAttribute("id", Integer.toString(this.missionDictionary.getUplinkFileIdForType(key))); 
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
                    

        // Write the enumeration tables
        final Map<String, CommandEnumerationDefinition> enums = this.missionDictionary
                .getArgumentEnumerations();

        if (!enums.isEmpty()) {
            writer.writeStartElement("enum_definitions");
            for (final CommandEnumerationDefinition table : enums.values()) {
                writer.writeStartElement("enum_table");
                String name = table.getName().substring(0,
                        Math.min(63, table.getName().length()));
                name = name.replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE);
                XmlUtility.writeSimpleAttribute(writer, NAME, name);
                writer.writeStartElement("values");
                final List<ICommandEnumerationValue> enumVals = table
                        .getEnumerationValues();
                for (final ICommandEnumerationValue val : enumVals) {
                    writer.writeStartElement("enum");
                    XmlUtility.writeSimpleAttribute(writer, "symbol",
                            val.getDictionaryValue());
                    XmlUtility.writeSimpleAttribute(writer, "numeric",
                            String.valueOf(val.getBitValue()));
                    writer.writeEndElement(); // enum
                }
                writer.writeEndElement(); // values
                writer.writeEndElement(); // enum_table
            }
            writer.writeEndElement(); // enum_definitions
        }

        // Write the command definitions
        writer.writeStartElement("command_definitions");
        for (final ICommandDefinition cmd : defs) {

            if ((cmd.getType() == CommandDefinitionType.FLIGHT) || 
                (cmd.getType() == CommandDefinitionType.SEQUENCE_DIRECTIVE)) {
                writer.writeStartElement("fsw_command");
            } else if (cmd.getType() == CommandDefinitionType.HARDWARE) {
                writer.writeStartElement("hw_command");
            } else {
                /*
                 * MPCS-6304 - 6/30/14. Warn that we do not generate other
                 * command types.
                 */
                TraceManager

                        .getDefaultTracer()
                        .warn("Found sequence directive or SSE command "
                                + cmd.getStem()
                                + ". The multimission dictionary does not support these, so it has been omitted");
                /*
                 * Do not write sequence directives for now, and certainly not
                 * SSE commands.
                 */
                continue;
            }
            final String opcodeStr = cmd.getOpcode();
            if (BinOctHexUtility.hasHexPrefix(opcodeStr)) {
                XmlUtility.writeSimpleAttribute(writer, "opcode", opcodeStr);
            } else {
                /*
                 * MPCS-6403 - 8/4/15. The MSL parser will set the opcode
                 * to "null" (the actual string). The MM schema will not support
                 * that, so set it to 0x0000 in that case.
                 */
                XmlUtility.writeSimpleAttribute(writer, "opcode", "0x"
                        + (opcodeStr.equalsIgnoreCase("null") ? "0000"
                                : opcodeStr));
            }

            XmlUtility.writeSimpleAttribute(writer, "stem", cmd.getStem());
            
            /* MHT - MPCS-7887 - 3/1/16 - Handle class attribute */
            if (cmd.getType() == CommandDefinitionType.FLIGHT) {
                XmlUtility.writeSimpleAttribute(writer, "class", "FSW");
                writeArguments(writer, cmd.getArguments());
            } else if (cmd.getType() == CommandDefinitionType.SEQUENCE_DIRECTIVE) {
                XmlUtility.writeSimpleAttribute(writer, "class", "SEQ");
                writeArguments(writer, cmd.getArguments());               
            }

            /* MHT - MPCS-7572 - 1/27/16 - Unify categories */         
            final Categories cat = cmd.getCategories();
            writeCategory(writer, cat);
           
            XmlUtility.writeSimpleCDataElement(writer, DESCRIPTION,
                    cmd.getDescription());
            final KeyValueAttributes kvaMap = cmd.getKeyValueAttributes();
            writeKeyValue(writer, kvaMap);

            writer.writeEndElement(); // whichever command, fsw or hw
        }
        writer.writeEndElement(); // command_definitions
        writer.writeEndElement(); // command_dictionary
        writer.writeEndDocument();
    }

    /**
     * Writes the XML for a repeat (variable array) command argument.
     * 
     * @param writer
     *            XMLStreamWriter to write to
     * @param arg
     *            the definition of the repeat argument
     * @throws XMLStreamException
     *             if there is an error while writing
     */
    private void writeRepeatArgument(final XMLStreamWriter writer,
            final ICommandArgumentDefinition arg) throws XMLStreamException {

        writer.writeStartElement("repeat_arg");
        XmlUtility.writeSimpleAttribute(writer, NAME, arg.getDictionaryName()
                .replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE));
        XmlUtility.writeSimpleAttribute(writer, "prefix_bit_length",
                String.valueOf(arg.getBitLength()));
        XmlUtility.writeSimpleCDataElement(writer, DESCRIPTION,
                arg.getDescription());
        writer.writeStartElement("repeat");
        final List<IValidationRange> ranges = arg.getRanges();
        XmlUtility.writeSimpleAttribute(writer, "min", ranges.get(0)
                .getMinimum());
        XmlUtility.writeSimpleAttribute(writer, "max", ranges.get(0)
                .getMaximum());
        writeArguments(writer,
                ((IRepeatCommandArgumentDefinition) arg)
                .getDictionaryArguments());
        writer.writeEndElement(); // repeat
        final KeyValueAttributes kvaMap = arg.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        writer.writeEndElement(); // repeat_arg
    }

    /**
     * Writes the XML for a numeric command argument.
     * 
     * @param writer
     *            XMLStreamWriter to write to
     * @param arg
     *            the definition of the numeric argument
     * @throws XMLStreamException
     *             if there is an error while writing
     */
    private void writeNumericArgument(final XMLStreamWriter writer,
            final ICommandArgumentDefinition arg, final String type)
            throws XMLStreamException {
        writer.writeStartElement(type.concat("_arg")); /* MHT - MPCS-7929 2/4/16 */
        XmlUtility.writeSimpleAttribute(writer, NAME, arg.getDictionaryName()
                .replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE));
        XmlUtility.writeSimpleAttribute(writer, BIT_LENGTH,
                String.valueOf(arg.getBitLength()));
        if (arg.getUnits() == null) {
            XmlUtility.writeSimpleAttribute(writer, UNITS, NONE);
        } else {
            XmlUtility.writeSimpleAttribute(writer, UNITS, arg.getUnits());
        }
        XmlUtility.writeSimpleAttribute(writer, DEFAULT_VALUE,
                arg.getDefaultValue());
        writeRanges(writer, arg.getRanges());
        XmlUtility.writeSimpleCDataElement(writer, DESCRIPTION,
                arg.getDescription());
        final KeyValueAttributes kvaMap = arg.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        writer.writeEndElement(); // numeric type arg
    }

    /**
     * Writes the XML for a string command argument.
     * 
     * @param writer
     *            XMLStreamWriter to write to
     * @param arg
     *            the definition of the string argument
     * @throws XMLStreamException
     *             if there is an error while writing
     */
    private void writeStringArgument(final XMLStreamWriter writer,
            final ICommandArgumentDefinition arg) throws XMLStreamException {

        writer.writeStartElement(arg.isVariableLength() ? "var_string_arg"
                : "fixed_string_arg");
        XmlUtility.writeSimpleAttribute(writer, NAME, arg.getDictionaryName()
                .replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE));

        if (!arg.isVariableLength()) {
            XmlUtility.writeSimpleAttribute(writer, BIT_LENGTH,
                    String.valueOf(arg.getBitLength()));
        } else {
            XmlUtility.writeSimpleAttribute(writer, "prefix_bit_length",
                    String.valueOf(arg.getPrefixBitLength()));
            /* MPCS-6304 - 6/23/14. Use bit length instead of max chars. */
            XmlUtility.writeSimpleAttribute(writer, "max_bit_length",
                    String.valueOf(arg.getBitLength()));
        }
        XmlUtility.writeSimpleAttribute(writer, DEFAULT_VALUE,
                arg.getDefaultValue());
        XmlUtility.writeSimpleCDataElement(writer, "valid_regex",
                arg.getValueRegexp());
        XmlUtility.writeSimpleCDataElement(writer, DESCRIPTION,
                arg.getDescription());
        final KeyValueAttributes kvaMap = arg.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        writer.writeEndElement(); // var/fixed_string_arg

    }

    /**
     * Writes the XML for a time command argument.
     * 
     * @param writer
     *            XMLStreamWriter to write to
     * @param arg
     *            the definition of the time argument
     * @throws XMLStreamException
     *             if there is an error while writing
     */
    private void writeTimeArgument(final XMLStreamWriter writer,
            final ICommandArgumentDefinition arg) throws XMLStreamException {

        if (arg.getType().isFloatTime()) {
            TraceManager

                    .getDefaultTracer()
                    .warn("Found floating point time argument"
                            + arg.getDictionaryName()
                            + ". The multimission schema does not support these");
            TraceManager.getDefaultTracer().warn(

                    "Argument will be declared as floating point");
            this.writeNumericArgument(writer, arg, "float");
            return;
        }
        writer.writeStartElement("time_arg");
        XmlUtility.writeSimpleAttribute(writer, NAME, arg.getDictionaryName()
                .replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE));
        XmlUtility.writeSimpleAttribute(writer, BIT_LENGTH,
                String.valueOf(arg.getBitLength()));
        XmlUtility.writeSimpleAttribute(writer, UNITS, arg.getUnits());
        XmlUtility.writeSimpleAttribute(writer, DEFAULT_VALUE,
                arg.getDefaultValue());
        writeRanges(writer, arg.getRanges());
        XmlUtility.writeSimpleCDataElement(writer, DESCRIPTION,
                arg.getDescription());
        final KeyValueAttributes kvaMap = arg.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        writer.writeEndElement(); // time_arg
    }

    /**
     * Writes the XML for an enumerated command argument.
     * 
     * @param writer
     *            XMLStreamWriter to write to
     * @param arg
     *            the definition of the enumerated argument
     * @throws XMLStreamException
     *             if there is an error while writing
     */
    private void writeEnumArgument(final XMLStreamWriter writer,
            final ICommandArgumentDefinition arg) throws XMLStreamException {

        writer.writeStartElement("enum_arg");
        XmlUtility.writeSimpleAttribute(writer, NAME, arg.getDictionaryName()
                .replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE));
        XmlUtility.writeSimpleAttribute(writer, BIT_LENGTH,
                String.valueOf(arg.getBitLength()));
        String name = arg.getTypeName().substring(0,
                Math.min(63, arg.getTypeName().length()));
        name = name.replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE);
        XmlUtility.writeSimpleAttribute(writer, "enum_name", name);
        XmlUtility.writeSimpleAttribute(writer, DEFAULT_VALUE,
                arg.getDefaultValue());
        writeRanges(writer, arg.getRanges());
        XmlUtility.writeSimpleCDataElement(writer, DESCRIPTION,
                arg.getDescription());
        final KeyValueAttributes kvaMap = arg.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        writer.writeEndElement(); // enum_arg
    }

    /**
     * Writes the XML for a boolean command argument.
     * 
     * @param writer
     *            XMLStreamWriter to write to
     * @param arg
     *            the definition of the boolean argument
     * @throws XMLStreamException
     *             if there is an error while writing
     */
    private void writeBooleanArgument(final XMLStreamWriter writer,
            final ICommandArgumentDefinition arg) throws XMLStreamException {

        writer.writeStartElement("boolean_arg");
        XmlUtility.writeSimpleAttribute(writer, NAME, arg.getDictionaryName()
                .replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE));
        XmlUtility.writeSimpleAttribute(writer, BIT_LENGTH,
                String.valueOf(arg.getBitLength()));
        XmlUtility.writeSimpleAttribute(writer, DEFAULT_VALUE,
                arg.getDefaultValue());
        final CommandEnumerationDefinition enumDef = arg.getEnumeration();
        if (enumDef != null) {
            writer.writeStartElement("boolean_format");
            ICommandEnumerationValue val = enumDef.lookupByFswValue("TRUE");
            XmlUtility.writeSimpleAttribute(writer, "true_str",
                    val.getDictionaryValue());
            val = enumDef.lookupByFswValue("FALSE");
            XmlUtility.writeSimpleAttribute(writer, "false_str",
                    val.getDictionaryValue());
            writer.writeEndElement(); // boolean_format
        }
        XmlUtility.writeSimpleCDataElement(writer, DESCRIPTION,
                arg.getDescription());
        final KeyValueAttributes kvaMap = arg.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        writer.writeEndElement(); // boolean_arg
    }

    /**
     * Writes the XML for a fill command argument.
     * 
     * @param writer
     *            XMLStreamWriter to write to
     * @param arg
     *            the definition of the fill argument
     * @throws XMLStreamException
     *             if there is an error while writing
     */
    private void writeFillArgument(final XMLStreamWriter writer,
            final ICommandArgumentDefinition arg) throws XMLStreamException {

        writer.writeStartElement("fill_arg");
        XmlUtility.writeSimpleAttribute(writer, NAME, arg.getDictionaryName()
                .replaceAll(ONE_OR_MORE_BLANKS, UNDERSCORE));
        XmlUtility.writeSimpleAttribute(writer, BIT_LENGTH,
                String.valueOf(arg.getBitLength()));
        XmlUtility.writeSimpleAttribute(writer, "fill_value",
                arg.getDefaultValue());
        final KeyValueAttributes kvaMap = arg.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        writer.writeEndElement(); // fill_arg
    }

    /**
     * Writes command arguments to the command dictionary stream.
     * 
     * @param writer
     *            the XMLStreamWriter to write output to the command dictionary
     *            stream
     * @param args
     *            the list of ICommandArgumentDefinitions to write
     * @throws XMLStreamException
     *             if anything goes wrong in the writing
     */
    private void writeArguments(final XMLStreamWriter writer,
            final List<ICommandArgumentDefinition> args)
            throws XMLStreamException {

        if (args != null && !args.isEmpty()) {
            writer.writeStartElement("arguments");
            for (final ICommandArgumentDefinition arg : args) {
                switch (arg.getType()) {
                case REPEAT:
                    writeRepeatArgument(writer, arg);
                    break;
                case INTEGER:
                    writeNumericArgument(writer, arg, "integer");
                    break;
                case FLOAT:
                    writeNumericArgument(writer, arg, "float");
                    break;
                case VAR_STRING:
                case FIXED_STRING:
                    writeStringArgument(writer, arg);
                    break;
                case TIME:
                case FLOAT_TIME:
                    writeTimeArgument(writer, arg);
                    break;
                case SIGNED_ENUMERATION:
                case UNSIGNED_ENUMERATION:
                    writeEnumArgument(writer, arg);
                    break;
                case UNSIGNED:
                    writeNumericArgument(writer, arg, "unsigned");
                    break;
                case BOOLEAN:
                    writeBooleanArgument(writer, arg);
                    break;
                case FILL:
                    writeFillArgument(writer, arg);
                    break;

                default:
                    TraceManager.getDefaultTracer().error(

                            "Unrecognized command argument type "
                                    + arg.getType());
                    break;
                }
            }
            writer.writeEndElement(); // args
        }

    }

    /**
     * Writes an argument range to the command dictionary stream.
     * 
     * @param writer
     *            the XML StreamWriter to write the range to
     * @param ranges
     *            the list of IValidationRanges to write
     * @throws XMLStreamException
     *             if anything goes wrong in the writing
     */
    private void writeRanges(final XMLStreamWriter writer,
            final List<IValidationRange> ranges) throws XMLStreamException {

        if (ranges == null || ranges.isEmpty()) {
            return;
        }
        writer.writeStartElement("range_of_values");
        for (final IValidationRange range : ranges) {
            writer.writeStartElement("include");
            XmlUtility.writeSimpleAttribute(writer, "min", range.getMinimum());
            XmlUtility.writeSimpleAttribute(writer, "max", range.getMaximum());
            writer.writeEndElement(); // include
        }
        writer.writeEndElement(); // range_of_values
    }

    /**
     * Main method.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {

        final CommandDictionaryConverterApp theApp = new CommandDictionaryConverterApp();

        try {
            /*
             * MPCS-7750 - 10/23/15. Use createOptions() rather than
             * creating a new reserved/base options object.
             */
            final ICommandLine commandLine = theApp.createOptions()
                    .parseCommandLine(args, true);
            theApp.configure(commandLine);

            theApp.readMissionDictionary();
            theApp.writeMultimissionDictionary();

        } catch (final DictionaryException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());

            System.exit(1);

        } catch (final ParseException e) {
            if (e.getMessage() == null) {
                TraceManager.getDefaultTracer().error(e.toString());

            } else {
                TraceManager.getDefaultTracer().error(e.getMessage());

            }
            System.exit(1);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }
}
