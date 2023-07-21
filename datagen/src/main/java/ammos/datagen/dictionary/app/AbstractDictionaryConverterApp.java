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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import ammos.datagen.cmdline.DatagenOptions;
import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;

/**
 * This is an abstract command line application class to be extended by all
 * dictionary converter classes.
 * 
 * MPCS-6235 - 6/11/14. Added class, factored out of what are now
 *          subclasses.
 * MPCS-7279 - 8/12/15. Added write category and key/value
 *          attribute classes.
 * MPCS-7750 - 10/23/15. Changed to use new command line option
 *          strategy throughput.
 */
public abstract class AbstractDictionaryConverterApp extends
        AbstractCommandLineApp implements StaxSerializable, IQuitSignalHandler {

    /** The output file location. */
    protected String outputPath;

    /** the input dictionary file location. */
    protected String dictionaryPath;
    
    /**
     * The dictionary configuration for the current mission, NOT the one for
     * the multimission configuration.
     */
    protected DictionaryProperties dictConfig;
    
    protected final ApplicationContext appContext;

    protected final SseContextFlag     sseFlag;

    /**
     * Constructor.
     */
    public AbstractDictionaryConverterApp() {

        super();
        appContext = SpringContextFactory.getSpringContext(true);
        sseFlag = appContext.getBean(SseContextFlag.class);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {

        /*
         * MPCS-7750 = 10/23/15. Change return type. Use new strategy for
         * creating options.
         */

        final DatagenOptions options = new DatagenOptions(this);
        options.addOption(DatagenOptions.DICTIONARY);
        options.addOption(DatagenOptions.OUTPUT_FILE);
        options.addOption(DatagenOptions.SOURCE_SCHEMA);
        return options;

    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        /*
         * MPCS-7750 = 10/23/15. Change return type. Use new strategy for
         * parsing options throughout.
         */

        /*
         * MPCS-7477 - 7/15/15. Mission passed for source schema may
         * contain an "sse" extension. The system mission no longer does. Strip
         * it off before setting system mission, and set the SSE application
         * flag instead.
         */
        final String schemaName = DatagenOptions.SOURCE_SCHEMA.parse(
                commandLine, true);

        /*
         * MPCS-7663 - 9/10/15. Schema name is no longer mission + sse. It
         * is just sse. We pretend the schema name is the mission so the right
         * configuration will be loaded. In the sse case, there IS no mission
         * name. We do not want to load an SSE config that is mission specific.
         * Believe it or not, setting the mission name to nothing works.
         */
        if (schemaName.equals("sse")) {
            sseFlag.setApplicationIsSse(true);
            GdsSystemProperties.setSystemMission("");
        } else {
            GdsSystemProperties.setSystemMission(schemaName);
        }

        /* 9/18/14 - MPCS-6641. Add validation of source schema */
        validateSourceSchema(schemaName);
        
        dictConfig = appContext.getBean(DictionaryProperties.class);

        this.dictionaryPath = DatagenOptions.DICTIONARY
                .parse(commandLine, true);
        this.outputPath = DatagenOptions.OUTPUT_FILE.parse(commandLine, true);
    }

    /**
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     * 
     */
    @Override
    public void showHelp() {

        /* MPCS-7750 - 10/23/15. Get nested Options object */

        final OptionSet options = createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --dictionary <file path> --outputFile <file path> --sourceSchema <schema-name>");
        pw.println("                   ");

        options.printOptions(pw);

        /*
         * MPCS-7679 - 9/16/15. Update help text to reflect new usage of
         * the source schema option.
         */
        pw.println("The input dictionary will be converted to multimission format.\n");
        pw.println("The source schema for the input dictionary must be specified using");
        pw.println("the --" + DatagenOptions.SOURCE_SCHEMA.getLongOpt()
                + " command line option, and indicates the XML format to be");
        pw.println("expected by the tool. Use 'mm' if the source schema is the multimission");
        pw.println("schema, 'sse' to indicate the source schema is the JPL SSE schema, 'header'");
        pw.println("to indicate the source schema is the old-style AMPCS header schema, or");
       pw.println("'monitor' to indicate the source schema is the old-style AMPCS monitor");
        pw.println("schema. If the input schema is 'mm' then this utility just has the effect");
        pw.println("of updating and reformatting the dictionary.");

        /* MPCS-6524 - 1/28/15. Use flush instead of close. */

        pw.flush();

    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#toXml()
     */
    @Override
    public String toXml() {

        String output = "";
        try {
            output = StaxStreamWriterFactory.toPrettyXml(this);
        } catch (final XMLStreamException e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().error(

                    "Could not write multimission dictionary XML"
                            + e.getMessage());
        }

        return (output);
    }

    /**
     * Writes the multimission dictionary. The actual work is done in
     * generateStaxXml() in the subclass.
     * 
     * @throws IOException
     *             if there is a problem writing to the file
     */
    public void writeMultimissionDictionary() throws IOException {

        final String toSave = this.toXml();
        final FileOutputStream fos = new FileOutputStream(this.outputPath);
        final PrintStream ps = new PrintStream(fos, true, "UTF-8");
        ps.println(toSave);
        ps.close();
        TraceManager.getDefaultTracer().info(

                "Output Dictionary written to " + this.outputPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public abstract void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException;

    /**
     * Writes a multimission &lt;header&gt; element to the given XML writer.
     * 
     * @param writer
     *            the stream to write to
     * @param schemaVersion
     *            the schema version to write in the header
     * @param version
     *            the dictionary version to write in the header
     * @param mission
     *            the mission name
     * @param spacecraftIds
     *            the list of numeric spacecraft IDs
     * @throws XMLStreamException
     *             if there is a problem writing the XML
     * MPCS-7434 - 1/29/16 - Added mission and scid params
     */
    protected void writeHeaderElement(final XMLStreamWriter writer,
            final String schemaVersion, final String version, final String mission,
                                      final List<Integer> spacecraftIds)
            throws XMLStreamException {

        /*
         * MPCS-6235 - 6/11/14. To decouple from the core project, use
         * GdsSystemProperties below instead of GdsConfiguration. Set SCID to 0.
         * There is no way to know the SCID without access to the GDS
         * configuration.
         */
        writer.writeStartElement("header");
        writer.writeAttribute("mission_name", mission);


        writer.writeAttribute("schema_version", schemaVersion);
        writer.writeAttribute("version", version);
        writer.writeStartElement("spacecraft_ids");
        for (final Integer scid : spacecraftIds) {
            writer.writeStartElement("spacecraft_id");
            if (scid.intValue() == IBaseDictionary.UNKNOWN_SCID) {
                writer.writeAttribute("value", "0");
            }
            else {
                writer.writeAttribute("value", scid.toString());
            }
            writer.writeEndElement();
        }
        writer.writeEndElement(); // spacecraft_ids
        writer.writeEndElement(); // header
    }
    
    /**
     * Convert a channel source string to a string expected in the MM
     * output XML
     * 
     * @param inSource input source type
     * @return the converted value
     */
    protected String convertSource(final String inSource) {

        // We expect upper case input, but can ensure this
        /* 9/17/14 - MPCS-6641. Add monitor case */
        switch (inSource.toUpperCase()) {
        case "FSW":
            return "flight";
        case "SSE":
            return "simulation";
        case "M":
            return "monitor";
        case "H":
            return "header"; /* MHT - MPCS-7646 */
        default:
            return "flight"; 
        }

    }

    /**
     * Write categories to output.
     * 
     * @param writer XML stream to write to
     * @param cat the Categories object to write
     * @throws XMLStreamException if there is an error generating the output
     */
    protected void writeCategory(final XMLStreamWriter writer,
            final Categories cat) throws XMLStreamException {
        if (!cat.isEmpty()) {
            writer.writeStartElement("categories");
            /* Make sure order is consistent in expected output XML. */
            final SortedSet<String> keys = new TreeSet<String>(cat.getKeys());
            for (final String key : keys) {
                writer.writeStartElement("category");
                if (key.equalsIgnoreCase(ICategorySupport.OPS_CAT)) {
                    writer.writeAttribute("name", ICategorySupport.OPS_CAT); /* Handle old name */
                } else {
                    writer.writeAttribute("name", key);
                }
                writer.writeAttribute("value", cat.getCategory(key));
                writer.writeEndElement(); // category
            }
            writer.writeEndElement(); // categories
        }
    }

    /**
     * Write key-value attributes to output.
     * 
     * @param writer XML stream to write to
     * @param kvaMap the KeyValueAttributes object to write
     * @throws XMLStreamException if there is an error generating the output
     */
    protected void writeKeyValue(final XMLStreamWriter writer,
            final KeyValueAttributes kvaMap) throws XMLStreamException {

        if (!kvaMap.isEmpty()) {
            final Set<String> keys = kvaMap.getKeys();
            writer.writeStartElement("attributes");
            for (final String key : keys) {
                writer.writeStartElement("keyword_value");
                writer.writeAttribute("key", key);
                final String value = kvaMap.getValueForKey(key);
                if (value != null) {
                    writer.writeCharacters(value);
                }
                writer.writeEndElement(); // keyword_value
            }
            writer.writeEndElement(); // attributes
        }
    }

    /**
     * Validates the source schema designator.
     * 
     * @param schemaName
     *            the input schema name from the command line
     * @throws ParseException
     *             if the entry is not valid
     * 
     * 9/18/14 - MPCS-6641. Added method.
     */
    protected void validateSourceSchema(final String schemaName)
            throws ParseException {
        if (GdsSystemProperties.getSystemProperty("datagen.test") != null) {
            return;
        }
        final String configPath = GdsSystemProperties
                .getSystemConfigDir()
                + File.separator
                + schemaName
                + File.separator + GdsHierarchicalProperties.CONSOLIDATED_PROPERTY_FILE_NAME;
        if (!new File(configPath).exists()) {
            throw new ParseException("There is no "
                    + GdsHierarchicalProperties.CONSOLIDATED_PROPERTY_FILE_NAME
                    + " for the specified source schema " + schemaName);

        }
    }

}
