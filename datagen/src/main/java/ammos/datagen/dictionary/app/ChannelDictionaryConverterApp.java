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
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.channel.BitRange;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.IAlgorithmicChannelDerivation;
import jpl.gds.dictionary.api.channel.IBitUnpackChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.eu.EUType;
import jpl.gds.dictionary.api.eu.IAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.dictionary.api.eu.IParameterizedAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IPolynomialEUDefinition;
import jpl.gds.dictionary.api.eu.ITableEUDefinition;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxSerializable;

/**
 * This is an application that reads a channel dictionary for the current AMPCS
 * mission and writes a channel dictionary in the multimission XML format.
 * 
 *
 * MPCS-6354, made each parameter for parameterized
 *          algorithms a separate element. Original version generated parsing
 *          errors.
 * 
 * MPCS-6387 - 7/21/2014. Ensure that each writer.writeStart is
 *          balanced by a writer.writeEnd
 * MPCS-7279 - 8/3/2015. Added key/value attributes.
 * MPCS-7750 - 10/23/15. Changed to use new command line option
 *          strategy throughput.
 */
public class ChannelDictionaryConverterApp extends
        AbstractDictionaryConverterApp implements StaxSerializable {

    private IChannelDictionary missionDictionary;

    /**
     * Parses the mission-specific Channel dictionary.
     * 
     * @throws DictionaryException
     *             if there is a problem reading the dictionary
     */

    public void readMissionDictionary() throws DictionaryException {

        this.missionDictionary = appContext.getBean(IChannelDictionaryFactory.class)
                .getNewInstance(dictConfig, this.dictionaryPath);

    }

    /**
     * Need to convert a Channel Type string to a string expected in the MM
     * output XML
     * 
     * @param inChannelType
     */
    private static String toOutChannelType(final String inChannelType) {

        // We expect upper case input, but can ensure this
        switch (inChannelType.toUpperCase()) {
        case "SIGNED_INT":
            return "integer";
        case "STATUS":
            return "enum";
        case "UNSIGNED_INT":
        case "DIGITAL":
            return "unsigned";
        case "FLOAT":
            return "float";
        case "ASCII":
            return "string";
        case "BOOLEAN":
            return "boolean";
        case "TIME":
            return "time";
            // default would include an input value of "UNKNOWN"
        default:
            return "string"; // better than an unrecognized value?
        }
    }
    /* MHT - 2/2/16 Renamed and moved to AbstractDictionaryConverterApp to be shared with EVR. */

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
        final List<IChannelDefinition> telemetries = this.missionDictionary
                .getChannelDefinitions();
        final List<IChannelDerivation> derivations = this.missionDictionary
                .getChannelDerivations();

        writer.writeStartDocument();
        writer.writeStartElement("telemetry_dictionary");

        /*
         * MPCS-6235 - 6/11/14.Use common method to write header.

        /* MPCS-7434 - 1/29/16. Get schema version from dictionary properties, mission and scid
         * from the parsed dictionary
         */
        writeHeaderElement(writer,
                DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.CHANNEL),
                this.missionDictionary.getGdsVersionId(),
                this.missionDictionary.getMission(),
                this.missionDictionary.getSpacecraftIds());

        // Write the enumeration tables
        if (!enums.isEmpty()) {
            writer.writeStartElement("enum_definitions");
            for (final EnumerationDefinition table : enums.values()) {
                writer.writeStartElement("enum_table");
                writer.writeAttribute("name", table.getName());
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

        // Write the derivation definitions
        writer.writeStartElement("derivation_definitions");
        for (final IChannelDerivation derivation : derivations) {

            // Only write derivation data if this is an algorithmic derivation
            // definition

            if (derivation.getDerivationType() == DerivationType.ALGORITHMIC) {

                final IAlgorithmicChannelDerivation algoDeriDef = (IAlgorithmicChannelDerivation) derivation;
                writer.writeStartElement("derivation");
                writer.writeAttribute("derivation_id", derivation.getId());

                writer.writeStartElement("parameterized_algorithm");
                writer.writeAttribute("name", algoDeriDef.getAlgorithmName());

                // returnType is not output, but still specified in the RNC file
                // as optional -- it would have been hard-coded as
                // "channel_list"

                // JIRA MPCS-6354 placed parameter element start/end within
                // the loop below -- Braun 7/2014

                // MPCS-6450 corrects mix-up of parameter name (the key)
                // and value. MPCS-6455 changes the Map below to a TreeMap,
                // created in the core dictionary's
                // AlgorithmicDerivationDefinition.java class, --braun, 8/2014

                final Map<String, String> parmMap = algoDeriDef
                        .getParametersMap();
                for (final String parmName : parmMap.keySet()) {
                    writer.writeStartElement("parameter");
                    String value = "";
                    writer.writeAttribute("name", parmName);
                    value = parmMap.get(parmName);
                    if (value != null) {
                        writer.writeCharacters(value);
                    }
                    writer.writeEndElement(); // parameter
                }
                writer.writeEndElement(); // parameterized_algorithm

                final List<String> parents = algoDeriDef.getParents();
                for (final String parent : parents) {
                    XmlUtility.writeSimpleElement(writer, "parent_id", parent);
                }

                XmlUtility.writeSimpleElement(writer, "trigger_id",
                        algoDeriDef.getTriggerId());

                writer.writeEndElement(); // derivation

            } // fi on being algorithmic

        }
        writer.writeEndElement(); // derivation_definitions

        // Write the telemetry definitions
        writer.writeStartElement("telemetry_definitions");
        for (final IChannelDefinition telemetry : telemetries) {

            writer.writeStartElement("telemetry");

            writer.writeAttribute("abbreviation", telemetry.getId());
            writer.writeAttribute("name", telemetry.getName());
            // We assign a String variable to use later when deciding on
            // format-def
            final String outChannelType = toOutChannelType(telemetry
                    .getChannelType().toString());
            writer.writeAttribute("type", outChannelType);
            writer.writeAttribute("source", convertSource(telemetry
                    .getDefinitionType().toString()));
            final int bitcount = telemetry.getSize(); // assume multiple of 8?
            final int bytecount = bitcount / 8;
            writer.writeAttribute("byte_length", Integer.toString(bytecount));
            if (telemetry.getGroupId() != null) { /* MHT - MPCS-7929 R7.3 schema changes */
                writer.writeAttribute("group_id", telemetry.getGroupId()); 
            }
            // elements
            if (telemetry.getIndex() > 0) {
                XmlUtility.writeSimpleElement(writer, "measurement_id",
                        Integer.toString(telemetry.getIndex()));
            }
            // MPCS-6450 corrects logic of determining source derivation id
            if (telemetry.isDerived()
                    && telemetry.getDerivationType() == DerivationType.ALGORITHMIC) {
                XmlUtility.writeSimpleElement(writer, "source_derivation_id",
                        telemetry.getSourceDerivationId());
            }
            XmlUtility
                    .writeSimpleElement(writer, "title", telemetry.getTitle());
            XmlUtility.writeSimpleElement(writer, "description",
                    telemetry.getDescription());

            // format based on channel type
            final EnumerationDefinition enumDef = telemetry.getLookupTable();
            switch (outChannelType) { // set above
            case "boolean":
                writer.writeStartElement("boolean_format");
                writer.writeAttribute("true_str", enumDef.getValue(1));
                writer.writeAttribute("false_str", enumDef.getValue(0));
                XmlUtility.writeSimpleElement(writer, "numeric_format",
                        telemetry.getDnFormat());
                XmlUtility.writeSimpleElement(writer, "string_format",
                        telemetry.getEuFormat());
                writer.writeEndElement();
                break;
            case "enum":
                writer.writeStartElement("enum_format");
                writer.writeAttribute("enum_name", enumDef.getName());
                XmlUtility.writeSimpleElement(writer, "numeric_format",
                        telemetry.getDnFormat());
                XmlUtility.writeSimpleElement(writer, "string_format",
                        telemetry.getEuFormat());
                writer.writeEndElement();
                break;
            default: // including integer, unsigned, float, string, time
                XmlUtility.writeSimpleElement(writer, "format",
                        telemetry.getDnFormat());
            }

            XmlUtility.writeSimpleElement(writer, "raw_units",
                    telemetry.getDnUnits());

            // raw_to_eng
            if (telemetry.hasEu()) {

                writer.writeStartElement("raw_to_eng");
                final IEUDefinition iEuCalc = telemetry.getDnToEu();
                // The decision below was originally implemented as an
                // enum implemented by classes that implement the IEUCalculation
                // interface. But this would have broken customer-supplied
                // algorithms.
                /*
                 * 9/17/14 - MPCS-6642. USe new EU for EU type in the
                 * switch EU.
                 */
                final EUType euType = iEuCalc.getEuType();

                switch (euType) {

                case SIMPLE_ALGORITHM:
                    final IAlgorithmicEUDefinition algoDnToEu = (IAlgorithmicEUDefinition) iEuCalc;
                    writer.writeStartElement("simple_algorithm");
                    writer.writeAttribute("name", algoDnToEu.getClassName());
                    writer.writeEndElement(); // simple_algorithm
                    break;
                case PARAMETERIZED_ALGORITHM:
                    /*
                     * 9/17/14 - MPCS-6642. Add handling of parameterized
                     * EU type.
                     */
                    final IParameterizedAlgorithmicEUDefinition palgoDnToEu = (IParameterizedAlgorithmicEUDefinition) iEuCalc;
                    writer.writeStartElement("parameterized_algorithm");
                    writer.writeAttribute("name", palgoDnToEu.getClassName());
                    final Map<String, String> parmMap = palgoDnToEu
                            .getParameters();

                    for (final String parmName : parmMap.keySet()) {
                        writer.writeStartElement("parameter");
                        String value = "";
                        writer.writeAttribute("name", parmName);
                        value = parmMap.get(parmName);
                        if (value != null) {
                            writer.writeCharacters(value);
                        }
                        writer.writeEndElement(); // parameter
                    }
                    writer.writeEndElement(); // parameterized_algorithm
                    break;
                case POLYNOMIAL:
                    final IPolynomialEUDefinition polyDnToEu = (IPolynomialEUDefinition) iEuCalc;
                    writer.writeStartElement("polynomial_expansion");
                    final int plen = polyDnToEu.getLength();
                    for (int i = 0; i < plen; i++) {
                        writer.writeStartElement("factor");
                        writer.writeAttribute("index", Integer.toString(i));
                        writer.writeAttribute("coeff",
                                Double.toString(polyDnToEu.getCoefficient(i)));
                        writer.writeEndElement(); // factor
                    }
                    writer.writeEndElement(); // polynomial_expansion
                    break;
                case TABLE:
                    final ITableEUDefinition tableDnToEu = (ITableEUDefinition) iEuCalc;
                    writer.writeStartElement("table_lookup");
                    final int tlen = tableDnToEu.getLength();
                    for (int i = 0; i < tlen; i++) {
                        writer.writeStartElement("point");
                        writer.writeAttribute("raw",
                                Double.toString(tableDnToEu.getDn(i)));
                        writer.writeAttribute("eng",
                                Double.toString(tableDnToEu.getEu(i)));
                        writer.writeEndElement(); // point
                    }
                    writer.writeEndElement(); // table_lookup
                    break;
                case NONE:
                default:
                    throw new XMLStreamException("Found unrecognized EU Type: "
                            + euType);
                }

                XmlUtility.writeSimpleElement(writer, "format",
                        telemetry.getEuFormat());

                XmlUtility.writeSimpleElement(writer, "eng_units",
                        telemetry.getEuUnits());

                writer.writeEndElement(); // raw_to_eng

            }

            // Need to see if this telemetry corresponds to a BIT_UNPACK
            // derivation by finding a match based on getId and the ID of the
            // derivation
            IBitUnpackChannelDerivation bitUnpackDeriDef = null;
            for (final IChannelDerivation derivation : derivations) {
                if (derivation.getDerivationType() == DerivationType.BIT_UNPACK
                        && derivation.getId().equals(telemetry.getId())) {
                    bitUnpackDeriDef = (IBitUnpackChannelDerivation) derivation;
                    break;
                }
            }
            if (bitUnpackDeriDef != null) {
                writer.writeStartElement("bit_extract");
                XmlUtility.writeSimpleElement(writer, "parent_id",
                        bitUnpackDeriDef.getParent());
                final List<BitRange> brList = bitUnpackDeriDef.getBitRanges();
                for (final BitRange bitRange : brList) {
                    writer.writeStartElement("bits");
                    writer.writeAttribute("offset",
                            Integer.toString(bitRange.getStartBit()));
                    writer.writeAttribute("length",
                            Integer.toString(bitRange.getLength()));
                    writer.writeEndElement(); // bits
                }
                writer.writeEndElement(); // bit_extract
            }

            /* MHT - MPCS-7572 - 1/27/16 - Unify categories */         
            final Categories cat = telemetry.getCategories();
            writeCategory(writer, cat);

            final KeyValueAttributes kvaMap = telemetry.getKeyValueAttributes();
            writeKeyValue(writer, kvaMap);

            // change-log-def

            writer.writeEndElement(); // telemetry
        }
        writer.writeEndElement(); // telemetry_definitions
        writer.writeEndElement(); // telemetry_dictionary
        writer.writeEndDocument();
    }

    /**
     * Main method.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {

        final ChannelDictionaryConverterApp theApp = new ChannelDictionaryConverterApp();

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
