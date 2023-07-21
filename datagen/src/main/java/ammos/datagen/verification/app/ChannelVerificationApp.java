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
package ammos.datagen.verification.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.ParseException;

import ammos.datagen.channel.app.ChannelGeneratorApp;
import ammos.datagen.channel.config.ChannelMissionConfiguration;
import ammos.datagen.cmdline.DatagenOptions;
import ammos.datagen.config.DatagenProperties;
import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;

/**
 * This is an application that will compare a channel truth file to the output
 * of the AMPCS chill_get_chanvals query utility. It takes an input directory,
 * which corresponds to the output directory from a channel packet generator
 * run, and a set of query options to pass to chill_get_chanvals as command line
 * options.
 * 
 *
 * MPCS-7750 - 10/23/15. Changed to use new BaseCommandOptions
 *          and new command line option strategy throughout.
 * 
 */
public class ChannelVerificationApp extends AbstractVerificationApp {

    private String getChannelsPath;
    private String dictFile;
    private String configFile;
    private ChannelMissionConfiguration channelMissionConfig;
    private final Map<String, IChannelDefinition> channelMap = new HashMap<String, IChannelDefinition>();
    private DatagenProperties datagenProps;
	private DictionaryProperties dictConfig;

    /**
     * Initializes the application by verifying that the needed AMPCS
     * applications can be found and creating the temporary query file.
     * 
     * 
     * @return true if initialization successful, false if not.
     */
    @Override
    public boolean init() {

        if (!super.init()) {
            return false;
        }

        /*
         * Make sure we can find AMPCS channel query utility.
         */
        this.getChannelsPath = GdsSystemProperties.getGdsDirectory()
                + File.separator
                + datagenProps.getChannelQueryScript();
        if (!new File(this.getChannelsPath).exists()) {
            log.error("Cannot locate chill_get_chanvals in "
                    + this.getChannelsPath);
            return false;
        }

        this.channelMissionConfig = new ChannelMissionConfiguration();
        if (!this.channelMissionConfig.load(this.configFile)) {
            return false;
        }

        configureAmpcsMission();

        if (!loadDictionary()) {
            return false;
        }

        return true;
    }

    /**
     * Uses AMPCS libraries to load the channel dictionary. Must be called after
     * configureAmpcsMission().
     * 
     * @see #configureAmpcsMission()
     * 
     * @return an IChannelDictionary object, or null if the load failed
     */
    private boolean loadDictionary() {

        /*
         * Create and load the channel dictionary using the AMPCS factory. The
         * add all the definitions to a hashmap.
         */
        try {
            final IChannelDictionary channelDict = appContext.getBean(IChannelDictionaryFactory.class)
                    .getNewInstance(dictConfig, this.dictFile);
            for (final IChannelDefinition def : channelDict
                    .getChannelDefinitions()) {
                this.channelMap.put(def.getId(), def);
            }
            return true;
        } catch (final DictionaryException e) {
            log.error("Unable to load channel dictionary");
            return false;
        }
    }

    /**
     * Pulls values from the data generator configuration to populate the
     * configuration necessary to use AMPCS classes we are dependent upon. This
     * method handles the setting of channel-specific adaptation in AMPCS.
     */
    private void configureAmpcsMission() {

        dictConfig = appContext.getBean(DictionaryProperties.class);
        
        /*
         * Configure the AMPCS channel adaptation. First read the necessary
         * values out of the mission configuration, then set them into the AMPCS
         * GDS configuration.
         */
        final String chanDictClass = this.channelMissionConfig
                .getStringProperty(
                        ChannelMissionConfiguration.CHANNEL_DICTIONARY_CLASS,
                        null);
        /*
         * If this is null, we default to the value in the data generator's
         * GdsSystemConfig.xml file.
         */
        if (chanDictClass != null) {
            dictConfig.setDictionaryClass(DictionaryType.CHANNEL, new DictionaryClassContainer(chanDictClass));
        }
    }

    /**
     * Executes the main logic for this application: Runs chill_get_chanvals and
     * verifies results against the data generator truth file.
     * 
     * @return application exit code: SUCCESS or FAILURE
     */
    public int run() {

        /*
         * Run the chill_get_chanvals query and save results to temp file.
         */
        if (!runChillQuery()) {
            return FAILURE;
        }
        if (!compareChannelTruthToQueryOutput()) {
            return FAILURE;
        }

        return SUCCESS;
    }

    /**
     * Executes chill_get_chanvals using the data generator verification
     * template for output and writes results to a temporary file.
     * 
     * @return true if the query was successful, false if not
     */
    private boolean runChillQuery() {

        return super.runChillQuery(this.getChannelsPath, datagenProps.getChannelQueryOptions());
    }

    /**
     * Compares channel query output to the data generator channel truth file.
     * For channels, there is no means to get the query records out of the
     * database in the order the channels were generated. So a straight
     * comparison between truth and query records will never work. Instead,
     * values written to one packet are gathered from truth, and corresponding
     * values from one packet are pulled from the query result, and then the
     * sets per packet are compared.
     * 
     * @return true if comparison succeeded, false if there was an error or
     *         differences were found.
     */
    private boolean compareChannelTruthToQueryOutput() {

        /*
         * The truth file should be in the input directory supplied on the
         * command line.
         */
        final String truthFile = this.inputDir + File.separator
                + ChannelGeneratorApp.TRUTH_FILE_NAME;

        /*
         * Assume everything matches.
         */
        boolean success = true;
        final String samplePrefix = "Sample:";
        final String packetPrefix = "Packet:";

        try {
            /*
             * Open both truth and query files. Use a buffered reader to get one
             * line at a time.
             */
            final BufferedReader truthReader = new BufferedReader(
                    new FileReader(truthFile));
            final BufferedReader queryReader = new BufferedReader(
                    new FileReader(this.outputFile));

            /*
             * Read an initial line from each file and start line number
             * counters for both.
             */
            String truthLine = truthReader.readLine();
            String queryLine = queryReader.readLine();
            int truthLineNum = 1;
            int queryLineNum = 1;

            /*
             * Find the first sample line in the truth file.
             */
            while (truthLine != null && !truthLine.startsWith(samplePrefix)) {
                truthLineNum++;
                truthLine = truthReader.readLine();
            }

            /*
             * Now start comparing clusters of records between truth and query.
             * Stop when we run out of data in one file or the other.
             */
            while (truthLine != null && queryLine != null) {

                /*
                 * Save truth lines to a temporary list until a non-sample line
                 * is found, whacking off the sample prefix from each line. This
                 * should give is all the truth lines for one packet.
                 */
                final List<String> truthLines = new LinkedList<String>();
                while (truthLine != null && truthLine.startsWith(samplePrefix)) {
                    truthLines
                            .add(truthLine.substring(samplePrefix.length() + 1));
                    truthLineNum++;
                    truthLine = truthReader.readLine();
                }

                /*
                 * If the next entry is not a packet line, there was no packet
                 * generated for these samples. Skip them and move on.
                 * Otherwise, whack the packet prefix off the line and save the
                 * packet record.
                 */
                String packetLine = null;
                if (!truthLine.startsWith(packetPrefix)) {
                    continue;
                } else {
                    packetLine = truthLine.substring(packetPrefix.length() + 1);
                }

                /*
                 * We need the packet SCLK.
                 */
                final String sclk = packetLine.substring(packetLine
                        .lastIndexOf(',') + 1);

                /*
                 * For each truth line found for this packet, grab that many
                 * lines from the query file and save to a temporary list, and
                 * build a list of sample objects for the queries samples.
                 * Derived channels must be skipped in the query output.
                 */
                final Map<String, Sample> querySamples = new HashMap<String, Sample>();
                while (queryLine != null
                        && querySamples.size() < truthLines.size()) {
                    final Sample s = new Sample(queryLine);
                    /*
                     * If the SCLK in the query results does not match what we
                     * want, there is a mismatch.
                     */
                    if (!s.getSclk().equals(sclk)) {
                        log.error("There are more records for SCLK " + sclk
                                + " in the truth file than in the query file");
                        success = false;
                        break;
                    }
                    final IChannelDefinition def = this.channelMap.get(s
                            .getChannelId());
                    if (def == null) {
                        log.warn("Found channel "
                                + s.getChannelId()
                                + " in query results that is not in the dictionary");
                        querySamples.put(s.getChannelId(), s);
                    } else if (!def.isDerived()) {
                        querySamples.put(s.getChannelId(), s);
                    }

                    queryLineNum++;
                    queryLine = queryReader.readLine();
                }

                /*
                 * If we ran out of query lines, the query file is too short
                 * compared to truth. Log and error and stop comparison.
                 */
                if (querySamples.size() != truthLines.size()
                        && queryLine == null) {
                    log.error("There are more records in the truth file than in the query file");
                    success = false;
                    break;
                }

                while (queryLine != null && queryLine.startsWith(sclk)) {
                    final Sample s = new Sample(queryLine);
                    final IChannelDefinition def = this.channelMap.get(s
                            .getChannelId());
                    if (def == null) {
                        log.warn("Found channel "
                                + s.getChannelId()
                                + " in query results that is not in the dictionary");
                        querySamples.put(s.getChannelId(), s);
                    } else if (!def.isDerived()) {
                        log.error("There are more records for SCLK " + sclk
                                + " in the query file than in the truth file");
                        querySamples.put(s.getChannelId(), s);
                        success = false;
                    }
                    queryLineNum++;
                    queryLine = queryReader.readLine();
                }

                /*
                 * Extract the packet SCLK from the truth line that contained
                 * the packet record.
                 */
                final String[] packetPieces = packetLine.split(",");
                final String sclkStr = packetPieces[3];

                /*
                 * Build a list of Samples for each truth sample entry, using
                 * the packet sclk just determined.
                 */
                final Map<String, Sample> truthSamples = new HashMap<String, Sample>();
                for (final String truthSample : truthLines) {
                    final Sample s = new Sample(truthSample, sclkStr);
                    truthSamples.put(s.getChannelId(), s);
                }

                /*
                 * Compare the truth and query sample lists.
                 */
                if (!compareSamples(truthSamples, querySamples, truthLineNum,
                        queryLineNum)) {
                    success = false;
                }

                /*
                 * Find the next sample line in the truth file.
                 */
                while (truthLine != null && !truthLine.startsWith(samplePrefix)) {
                    truthLineNum++;
                    truthLine = truthReader.readLine();
                }
            }

            /*
             * When we get here, we ran out of data in one file or the other. If
             * we did not run out of both, we have a mismatch.
             */
            if (success && truthLine == null && queryLine != null) {
                log.error("There are more records in the query file than in the truth file");
                success = false;
            } else if (success && queryLine == null && truthLine != null) {
                log.error("There are more records in the truth file than in the query file");
                success = false;
            }
            /*
             * Done. Close both files.
             */
            truthReader.close();
            queryReader.close();
        } catch (final FileNotFoundException e) {
            log.error("Truth file " + truthFile + " was not found");
            success = false;
        } catch (final IOException e) {
            e.printStackTrace();
            log.error("I/O error reading either truth or query file");
            success = false;
        }

        /*
         * Log success. Failures will already be logged.
         */
        if (success) {
            log.info("Output from query matches the truth file " + truthFile);
        }
        return success;

    }

    /**
     * Compares the channel samples in a truth map to the samples in a query
     * map.
     * 
     * @param truthSamples
     *            Sample objects for one packet, from the truth file, in the
     *            form of a map accessed by channel ID.
     * @param querySamples
     *            Sample objects for one packet, from the query file, in the
     *            form of a map accessed by channel ID.
     * @param truthLineNum
     *            current line number in the truth file
     * @param queryLineNum
     *            current line number in the query file
     * @return true if the comparison showed no differences, false if it did
     */
    private boolean compareSamples(final Map<String, Sample> truthSamples,
            final Map<String, Sample> querySamples, final int truthLineNum,
            final int queryLineNum) {

        boolean success = true;
        for (final Sample ts : truthSamples.values()) {
            final Sample qs = querySamples.get(ts.getChannelId());
            if (qs == null) {
                log.error("Difference found at truth line " + truthLineNum
                        + ", query line " + queryLineNum);
                log.error("Truth sample " + ts.toString()
                        + " not found in query results");
                success = false;
                continue;
            }
            if (!qs.equals(ts)) {
                log.error("Difference found at truth line " + truthLineNum
                        + ", query line " + queryLineNum);
                log.error("Expected: " + ts.toString());
                log.error("Found: " + qs.toString());
                success = false;
            }
        }
        for (final Sample qs : querySamples.values()) {
            final Sample ts = querySamples.get(qs.getChannelId());
            if (ts == null) {
                log.error("Difference found at truth line " + truthLineNum
                        + ", query line " + queryLineNum);
                log.error("Query sample " + qs.toString()
                        + " not found in truth data");
                success = false;
            }
        }
        return success;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {

        final DatagenOptions options = super.createOptions();

        options.addOption(DatagenOptions.DICTIONARY);
        options.addOption(DatagenOptions.MISSION_CONFIG);
        return options;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.verification.app.AbstractVerificationApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        this.dictFile = DatagenOptions.DICTIONARY.parse(commandLine, true);
        this.configFile = DatagenOptions.MISSION_CONFIG
                .parse(commandLine, true);
        datagenProps = new DatagenProperties();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --dictionary <file> --missionConfig <file> --inputDir <directory> --queryOptions <chill options>");
        pw.println("                   ");

        final OptionSet options = createOptions().getOptions();

        options.printOptions(pw);
        pw.println("This is a test verification application that will compare the truth");
        pw.println("files generated by the channel data generator with the AMPCS channel");
        pw.println("query output for an AMPCS session.\n");

        pw.flush();
    }

    /**
     * Class used to hold data for one channel sample and provide the ability to
     * compare samples.
     * 
     *
     */
    private static class Sample {
        private final String channelId;
        private final String channelName;
        private final String dnVal;
        private final String sclk;

        /**
         * Creates a Sample from a CSV query output line.
         * 
         * @param sampleLine
         *            query result record in the form: sclk,channel ID, channel
         *            name,data number
         */
        public Sample(final String sampleLine) {

            final String[] pieces = sampleLine.split(",");
            this.sclk = pieces[0];
            this.channelId = pieces[1];
            this.channelName = pieces[2];
            // The DN may have had commas in it, so just grab everything
            // after the 3rd comma
            int index = sampleLine.indexOf(',');
            index = sampleLine.indexOf(',', index + 1);
            index = sampleLine.indexOf(',', index + 1);
            this.dnVal = sampleLine.substring(index + 1).trim();
        }

        /**
         * Returns the sample SCLK as a string.
         * 
         * @return SCLK string.
         */
        public String getSclk() {

            return this.sclk;
        }

        /**
         * Creates a Sample from a SCLK and truth output line.
         * 
         * @param sampleLine
         *            query result record in the form: channel ID, channel
         *            name,data number
         * @param sclk
         *            SCLK value for this sample, as a string
         */
        public Sample(final String sampleLine, final String sclkStr) {

            final String[] pieces = sampleLine.split(",");
            this.sclk = sclkStr;
            this.channelId = pieces[0];
            this.channelName = pieces[1];
            // The DN may have had commas in it, so just grab everything
            // after the 2nd comma
            int index = sampleLine.indexOf(',');
            index = sampleLine.indexOf(',', index + 1);
            this.dnVal = sampleLine.substring(index + 1).trim();
        }

        /**
         * Gets the channel ID for this sample.
         * 
         * @return channel ID
         */
        public String getChannelId() {

            return this.channelId;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object o) {

            if (!(o instanceof Sample)) {
                return false;
            }
            final Sample s = (Sample) o;
            if (!this.channelId.equals(s.channelId)) {
                return false;
            }
            if (!this.channelName.equals(s.channelName)) {
                return false;
            }

            if (!this.sclk.equals(s.sclk)) {
                return false;
            }

            if (!this.dnVal.equals(s.dnVal)) {
                try {
                    /*
                     * See if this sample has a valid floating point DN value.
                     */
                    final Double thisVal = Double.valueOf(this.dnVal);
                    final Double thatVal = Double.valueOf(s.dnVal);

                    /*
                     * If the difference is very small, it is a rounding issue.
                     */
                    if (Math.abs(thisVal.doubleValue() - thatVal.doubleValue()) < 1.0e-6) {
                        return true;
                    }

                    return false;

                } catch (final NumberFormatException e) {
                    /*
                     * It wasn't a float DN value, and it didn't match.
                     */
                    return false;
                }
            }
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {

            return this.channelId.hashCode() + this.channelName.hashCode()
                    + this.dnVal.hashCode() + this.sclk.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {

            return "(" + this.sclk + "," + this.channelId + ","
                    + this.channelName + "," + this.dnVal + ")";
        }
    }

    /**
     * Main application entry point.
     * 
     * @param args
     *            Command line arguments from the user
     */
    @CoverageIgnore
    public static void main(final String[] args) {

        final ChannelVerificationApp app = new ChannelVerificationApp();

        // Parse the command line arguments
        try {
            final ICommandLine commandLine = app.createOptions()
                    .parseCommandLine(args, true);
            app.configure(commandLine);
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());
            System.exit(1);
        }

        // Initialize the application
        final boolean ok = app.init();
        if (!ok) {
            System.exit(1);
        }
        try {
            final int code = app.run();
            System.exit(code);
        } catch (final Exception e) {
            // something totally unexpected happened
            e.printStackTrace();
            System.exit(1);
        }
    }
}
