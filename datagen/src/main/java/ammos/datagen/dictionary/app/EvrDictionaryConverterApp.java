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
import java.util.SortedMap;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.ParseException;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DictionaryClassContainer.ClassType;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is an application that reads an EVR dictionary for the current AMPCS
 * mission and writes an EVR dictionary in the multimission XML format.
 * 
 *
 * MPCS-6235 - 6/11/14. Now extends new superclass. Some methods
 *          moved there.
 * MPCS-6366 - 7/15/2014. Revised output of ops category,
 *          module, and subsystem
 * MPCS-6387 - 7/21/2014. Ensure that each writer.writeStart is
 *          balanced by a writer.writeEnd
 * MPCS-7279 - 8/3/2015. Added key/value attributes.
 * MPCS-7750 - 10/23/15. Changed to use new command line option
 *          strategy throughput.
 * MPCS-7929 - 2/4/16. Deprecated numeric_args.
 */
public class EvrDictionaryConverterApp extends AbstractDictionaryConverterApp {

    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String BYTE_LENGTH = "byte_length";

    private IEvrDictionary missionDictionary;

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
            throw new ParseException("There is no monitor EVR schema");
        }
        else if (sseFlag.isApplicationSse() && dictConfig.getSseDictionaryClass(DictionaryType.EVR).getClassType() == ClassType.SSE_EVR) {
            throw new ParseException(
                    "JPL SSE format EVR dictionaries cannot be converted");
        }
    }

    /**
     * Parses the mission-specific EVR dictionary.
     * 
     * @throws DictionaryException
     *             if there is a problem reading the dictionary
     */
    public void readMissionDictionary() throws DictionaryException {

        this.missionDictionary = appContext.getBean(IEvrDictionaryFactory.class)
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

        final Map<String, EnumerationDefinition> enums = this.missionDictionary
                .getEnumDefinitions();
        final List<IEvrDefinition> evrs = this.missionDictionary
                .getEvrDefinitions();

        writer.writeStartDocument();
        writer.writeStartElement("evr_dictionary");

        /*
         * MPCS-6235 - 6/11/14.Use common method to write header.

        /* MPCS-7434 - 1/29/16. Get schema version from dictionary properties, mission and scid
         * from the parsed dictionary
         */
        writeHeaderElement(
                writer,
                DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.EVR),
                this.missionDictionary.getGdsVersionId(),
                this.missionDictionary.getMission(),
                this.missionDictionary.getSpacecraftIds());

        // Write the enumeration tables
        if (!enums.isEmpty()) {
            writer.writeStartElement("enum_definitions");
            for (final EnumerationDefinition table : enums.values()) {
                writer.writeStartElement("enum_table");
                writer.writeAttribute(NAME, table.getName());
                writer.writeStartElement("values");
                final SortedMap<Long, String> enumVals = table
                        .getAllAsSortedMap();
                for (final Map.Entry<Long, String> val : enumVals.entrySet()) {
                    writer.writeStartElement("enum");
                    writer.writeAttribute("symbol", val.getValue());
                    writer.writeAttribute("numeric",
                            String.valueOf(val.getKey()));
                    writer.writeEndElement(); // enum
                }
                writer.writeEndElement(); // values

                writer.writeEndElement(); // enum_table
            }
            writer.writeEndElement(); // enum_definitions
        }

        // Write the EVR definitions
        writer.writeStartElement("evrs");
        for (final IEvrDefinition evr : evrs) {

            writer.writeStartElement("evr");
            writer.writeAttribute("id", String.valueOf(evr.getId()));
            writer.writeAttribute(NAME, evr.getName());
            writer.writeAttribute("level", evr.getLevel());
            writer.writeAttribute("source", convertSource(evr
                    .getDefinitionType().toString()));

            /* MHT - MPCS-7572 - 1/27/16 - Unify categories */         
            final Categories cat = evr.getCategories();
            writeCategory(writer, cat);

            XmlUtility.writeSimpleCDataElement(writer, "format_message",
                    evr.getFormatString());
            XmlUtility.writeSimpleElement(writer, "number_of_arguments",
                    evr.getNargs());
            final List<IEvrArgumentDefinition> args = evr.getArgs();

            /* MPCS-6254 - 6/13/14. Add check for 0 length arg list */
            if (args != null && !args.isEmpty()) {
                writer.writeStartElement("args");
                for (final IEvrArgumentDefinition arg : args) {
                    int len = arg.getLength();
                    if (len <= 0) {
                        len = arg.getType().getByteLength();
                    }
                    switch (arg.getType()) {
                    case SEQID:
                        writer.writeStartElement("seqid_arg");
                        if (arg.getName() != null) {
                            writer.writeAttribute(NAME, arg.getName());
                        }
                        writer.writeAttribute(BYTE_LENGTH, String.valueOf(len));
                        writer.writeEndElement(); // seqid_arg
                        break;
                    case OPCODE:
                        writer.writeStartElement("opcode_arg");
                        if (arg.getName() != null) {
                            writer.writeAttribute(NAME, arg.getName());
                        }
                        writer.writeAttribute(BYTE_LENGTH, String.valueOf(len));
                        writer.writeEndElement(); // opcode_arg
                        break;
                    case ENUM:
                        writer.writeStartElement("enum_arg");
                        if (arg.getName() != null) {
                            writer.writeAttribute(NAME, arg.getName());
                        }
                        writer.writeAttribute(BYTE_LENGTH, String.valueOf(len));
                        writer.writeAttribute("enum_name",
                                arg.getEnumTableName());
                        writer.writeEndElement(); // enum_arg
                        break;
                    case VAR_STRING:
                        writer.writeStartElement("string_arg");
                        if (arg.getName() != null) {
                            writer.writeAttribute(NAME, arg.getName());
                        }
                        writer.writeEndElement(); // string_arg
                        break;
                    case I8:
                    case I16:
                    case I32:
                    case I64:
                        writer.writeStartElement("integer_arg");
                        if (arg.getName() != null) {
                            writer.writeAttribute(NAME, arg.getName());
                        }
                        writer.writeAttribute(BYTE_LENGTH, String.valueOf(len));
                        writer.writeEndElement(); // numeric_arg
                        break;
                    case U8:
                    case U16:
                    case U32:
                    case U64:
                        writer.writeStartElement("unsigned_arg");
                        if (arg.getName() != null) {
                            writer.writeAttribute(NAME, arg.getName());
                        }
                        writer.writeAttribute(BYTE_LENGTH, String.valueOf(len));
                        writer.writeEndElement(); // numeric_arg
                        break;
                    case F32:
                    case F64:
                        writer.writeStartElement("float_arg");
                        if (arg.getName() != null) {
                            writer.writeAttribute(NAME, arg.getName());
                        }
                        writer.writeAttribute(BYTE_LENGTH, String.valueOf(len));
                        writer.writeEndElement(); // numeric_arg
                        break;
                    default:
                        break;
                    }
                }
                writer.writeEndElement(); // args
            }
            final KeyValueAttributes kvaMap = evr.getKeyValueAttributes();
            writeKeyValue(writer, kvaMap);
            writer.writeEndElement(); // evr
        }
        writer.writeEndElement(); // evrs
        writer.writeEndElement(); // evr_dictionary
        writer.writeEndDocument();
    }

    /**
     * Main method.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {

        final EvrDictionaryConverterApp theApp = new EvrDictionaryConverterApp();

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
