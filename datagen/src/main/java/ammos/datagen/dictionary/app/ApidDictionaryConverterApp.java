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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.ParseException;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.dictionary.api.apid.IApidDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is an application that reads an APID dictionary for the current AMPCS
 * mission and writes an APID dictionary in the multimission XML format.
 * 
 *
 * MPCS-6235 - 6/11/14. Now extends new superclass. Some methods
 *          and members moved there.
 * MPCS-6387 - 7/21/2014. Ensure that each writer.writeStart is
 *          balanced by a writer.writeEnd
 * MPCS-7279 - 8/3/2015. Added key/value attributes.
 * MPCS-7750 =- 10/23/15. Changed to use new command line option
 *          strategy throughput.
 */
public class ApidDictionaryConverterApp extends AbstractDictionaryConverterApp {

    private IApidDictionary missionDictionary;

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
            throw new ParseException("There is no monitor APID schema");
        }
    }

    /**
     * Parses the mission-specific APID dictionary.
     * 
     * @throws DictionaryException
     *             if there is a problem reading the dictionary
     */
    public void readMissionDictionary() throws DictionaryException {

        this.missionDictionary = appContext.getBean(IApidDictionaryFactory.class)
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

        final List<IApidDefinition> apids = this.missionDictionary
                .getApidDefinitions();

        writer.writeStartDocument();
        writer.writeStartElement("apid_dictionary");

        /*
         * MPCS-6235 - 6/11/14.Use common method to write header.

        /* MPCS-7434 - 1/29/16. Get schema version from dictionary properties, mission and scid
         * from the parsed dictionary 
         */
        writeHeaderElement(writer,
                DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.APID),
                this.missionDictionary.getGdsVersionId(),
                this.missionDictionary.getMission(),
                           this.missionDictionary.getSpacecraftIds());

        // Write the APID definitions
        writer.writeStartElement("apids");
        for (final IApidDefinition apid : apids) {

            writer.writeStartElement("apid_definition");
            writer.writeAttribute("apid", String.valueOf(apid.getNumber())); // any
            // additional
            // check
            // for
            // 2047
            // max?
            writer.writeAttribute("format", apid.getContentType().toString());
            writer.writeAttribute("recorded", String.valueOf(apid.isRecorded()));

            /*
             * MPCS-6235 - 6/11/14. Use CDATA for description, not name.
             */
            XmlUtility.writeSimpleElement(writer, "name", apid.getName());
            XmlUtility.writeSimpleElement(writer, "title", apid.getTitle());
            final String desc = apid.getDescription();
            if (desc != null) {
                XmlUtility.writeSimpleCDataElement(writer, "description", desc);
            }
            /* MHT - MPCS-7572 - 1/27/16 - Unify categories */         
            final Categories cat = apid.getCategories();
            writeCategory(writer, cat);

            final KeyValueAttributes kvaMap = apid.getKeyValueAttributes();
            writeKeyValue(writer, kvaMap);

            writer.writeEndElement(); // apid_definition
        }

        writer.writeEndElement(); // apids
        writer.writeEndElement(); // apid_dictionary
        writer.writeEndDocument();
    }

    /**
     * Main method.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {

        final ApidDictionaryConverterApp theApp = new ApidDictionaryConverterApp();

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
