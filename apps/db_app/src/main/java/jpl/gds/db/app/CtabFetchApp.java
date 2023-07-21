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
package jpl.gds.db.app;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.ChannelTypeSelect;
import jpl.gds.db.api.sql.fetch.IChannelValueFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.api.types.IDbChannelSampleProvider;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.app.ctab.CtabException;
import jpl.gds.db.app.ctab.CtabSequence;
import jpl.gds.db.mysql.impl.sql.order.ChannelValueOrderByType;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.channel.ChannelIdUtility;
import jpl.gds.shared.channel.ChannelListRangeException;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;


/**
 * The CTAB fetch app is the command line application used to retrieve
 * channel values from the database. It is similar to channel value fetch app,
 * but uses CTAB format for output, and does not duplicate functionality already
 * in channel value fetch app.
 *
 */
public class CtabFetchApp extends AbstractFetchApp
{
    /** Short option for CTAB template */
    public static final String TEMPLATE_SHORT      = "k";

    /** Long option for CTAB template */
    public static final String TEMPLATE_LONG       = "ctab";

    /** Short option for CTAB template file*/
    public static final String TEMPLATE_FILE_SHORT = "l";

    /** Long option for CTAB template file*/
    public static final String TEMPLATE_FILE_LONG  = "ctabFile";

    /** Long option for channel types */
    public static final String CHANNEL_TYPES_LONG = "channelTypes";

    private static final String APP_NAME =
        ApplicationConfiguration.getApplicationName("chill_get_ctab");
    
    /** Added opts to configure column width for data and time headers & columns */
    private static final char WILDCARD = '%';
    /** Short option for CTAB data output spacing */
    private static final String DATA_SPACING_SHORT = "s";
    /** Long option for CTAB data output spacing */
    private static final String DATA_SPACING_LONG = "dataSpacing";
    /** Default data output spacing */
    private static final String DEFAULT_DATA_OUTPUT_SPACING = WILDCARD + "15s";
    /** Short option for CTAB time output spacing */
    private static final String TIME_SPACING_SHORT = "T";
    /** Long option for CTAB time output spacing */
    private static final String TIME_SPACING_LONG = "timeSpacing";
    /** Default time output spacing */
    private static final String DEFAULT_TIME_OUTPUT_SPACING = WILDCARD +"-25s";

    /**
     * CTAB processor
     */
    private CtabSequence ctabSequence = null;

    /**
     * The list of channel IDs to query for
     */
    private List<String> channelIds = new ArrayList<String>(32);

    /**
     * True if all channel values are required
     */
    private boolean allChannelValues = true;

    /** Channel types desired */
    private final ChannelTypeSelect channelTypeSelect = new ChannelTypeSelect();

    /** VCIDs to query for */
    private Set<Integer> vcids = null;

    /** DSS ids to query for */
    private Set<Integer> dssIds = null;
    
    private final SclkFormatter sclkFmt;


    /**
     * Creates an instance of CtabFetchApp.
     *
     */
    public CtabFetchApp()
    {
        super(IDbTableNames.DB_CHANNEL_VALUE_TABLE_NAME, APP_NAME, null);
        sclkFmt = TimeProperties.getInstance().getSclkFormatter();

        suppressInfo();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {    	
    	super.createRequiredOptions();
        requiredOptions.add(BEGIN_TIME_LONG);
        requiredOptions.add(END_TIME_LONG);
        requiredOptions.add(CHANNEL_ID_FILE_LONG);
        requiredOptions.add(CHANNEL_IDS_LONG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        super.configureApp(cmdline);

        // Are all channel values required or just changes?
        allChannelValues = !cmdline.hasOption(CHANGE_VALUES_SHORT);

        String ctabTemplate = null;
        if (cmdline.hasOption(TEMPLATE_SHORT))
        {
            ctabTemplate = cmdline.getOptionValue(TEMPLATE_SHORT);
            if (ctabTemplate == null)
            {
                throw new MissingArgumentException("-" + TEMPLATE_SHORT + " requires a command line value");
            }
        }

        String ctabTemplateFile = null;
        if(cmdline.hasOption(TEMPLATE_FILE_SHORT))
        {
            if(cmdline.hasOption(TEMPLATE_SHORT))
            {
                throw new ParseException("Cannot specify template as both a command-line argument and a file");
            }

            ctabTemplateFile = cmdline.getOptionValue(TEMPLATE_FILE_SHORT);
            if (ctabTemplateFile == null)
            {
                throw new MissingArgumentException("-" + TEMPLATE_FILE_SHORT + " requires a command line value");
            }

            ctabTemplate = readTemplateFile(ctabTemplateFile);
        }

        // Parse and store the channel id list from the command line (if it exists)
        String chanIdString = null;
        if (cmdline.hasOption(CHANNEL_IDS_SHORT))
        {
            chanIdString = cmdline.getOptionValue(CHANNEL_IDS_SHORT);
            if (chanIdString == null)
            {
                throw new MissingArgumentException("-" + CHANNEL_IDS_SHORT + " requires a command line value");
            }
            chanIdString = chanIdString.trim();
        }

        // Parse and store the list of channel IDs from a file (if it exists)
        String chanIdFile = null;
        if (cmdline.hasOption(CHANNEL_ID_FILE_SHORT))
        {
            chanIdFile = cmdline.getOptionValue(CHANNEL_ID_FILE_SHORT);
            if (chanIdFile == null)
            {
                throw new MissingArgumentException("-" + CHANNEL_ID_FILE_SHORT + " requires a command line value");
            }
            chanIdFile = chanIdFile.trim();
        }
        
        // Parse and store spacing format for DATA (if it exists)
        String dataSpaceFormat = null;
        if (cmdline.hasOption(DATA_SPACING_SHORT)) { 
        	dataSpaceFormat = cmdline.getOptionValue(DATA_SPACING_SHORT).trim();
        	if (dataSpaceFormat == null) { 
        		throw new MissingArgumentException("-" + DATA_SPACING_SHORT + " requires a command line value");
        	}
        } else {
        	dataSpaceFormat = DEFAULT_DATA_OUTPUT_SPACING;
        }
        // Check option only uses String formatting character 's' and a single wild card '%'
        if (dataSpaceFormat != null && !dataSpaceFormat.equals(DEFAULT_DATA_OUTPUT_SPACING)) { 
        	final int wildCardCount = StringUtil.count(dataSpaceFormat, WILDCARD);
        	
        	if (wildCardCount == 0) { 
        		throw new ParseException("Invalid value '" + dataSpaceFormat + "' found for option -" + DATA_SPACING_LONG + 
        				". Formatting character '" + WILDCARD + "' expected.");
        	}
        	
        	if (wildCardCount > 1) { 
        		throw new ParseException("Invalid value '" + dataSpaceFormat + "' found for option -" + DATA_SPACING_LONG + 
        				". This option only supports a single wildard '" + WILDCARD + "' character.");
        	}
        	
        	final String chars = dataSpaceFormat.replaceAll("[^a-zA-Z ]", "");
        	if (chars.length() > 1 || !chars.contains("s")) { 
        		throw new ParseException("Invalid value '" + dataSpaceFormat + "' found for option -" + DATA_SPACING_LONG +
        				". This option only supports String formatting (" + WILDCARD + "s).");
        	}
        }
        
        // Parse and store the spacing format for TIMES (if it exists)
        String timeSpaceFormat = null;
        if (cmdline.hasOption(TIME_SPACING_SHORT)) { 
        	timeSpaceFormat = cmdline.getOptionValue(TIME_SPACING_SHORT).trim();
        	if (timeSpaceFormat == null) { 
        		throw new MissingArgumentException("-" + TIME_SPACING_SHORT + " requires a command line value");
        	}
        } else { 
        	timeSpaceFormat = DEFAULT_TIME_OUTPUT_SPACING;
        }
        
        // Check option only uses String formatting character 's' and a single wild card '%'
        if (timeSpaceFormat != null && !timeSpaceFormat.equals(DEFAULT_TIME_OUTPUT_SPACING)) { 
        	final int wildCardCount = StringUtil.count(timeSpaceFormat, WILDCARD);
        	
        	if (wildCardCount == 0) { 
        		throw new ParseException("Invalid value '" + timeSpaceFormat + "' found for option -" + TIME_SPACING_LONG + 
        				". Formatting character '" + WILDCARD + "' expected.");
        	}

        	if (wildCardCount > 1) { 
        		throw new ParseException("Invalid value '" + timeSpaceFormat + "' found for option -" + TIME_SPACING_LONG + 
        				". This option only supports a single wildard '" + WILDCARD + "' character.");
        	}
        	
        	final String chars = timeSpaceFormat.replaceAll("[^a-zA-Z ]", "");
        	if (chars.length() > 1 || !chars.contains("s")) { 
        		throw new ParseException("Invalid value '" + timeSpaceFormat + "' found for option -" + TIME_SPACING_LONG +
        				". This option only supports String formatting (" + WILDCARD + "s).");
        	}
        }

        // Finish parsing command line list of channel IDs

        if (chanIdString != null)
        {
            for (String temp : chanIdString.split(",{1}"))
            {
                temp = temp.trim().toUpperCase();

                if (! ChannelIdUtility.isChanIdString(temp))
                {
                    throw new ParseException(
                                  "The input channel ID '"                 +
                                  temp + "' is not a valid channel ID. "   +
                                  "Channel IDs should follow the regular " +
                                  "expression "                            +
                                  ChannelIdUtility.CHANNEL_ID_REGEX               +
                                  ". Wildcards and ranges are not allowed.");
                }

                if (! channelIds.contains(temp))
                {
                    channelIds.add(temp);
                }
            }
        }

        // Retrieve the channel Ids from a file if necessary, and append to list
        if(chanIdFile != null)
        {
            final List<String> newChannelIds = parseChannelIdFile(chanIdFile);
            if (newChannelIds.size() < 1)
            {
                throw new ParseException("Empty channel id file '" + chanIdFile + "'");
            }
            for (final String id: newChannelIds) {
                if (!channelIds.contains(id)) {
                    channelIds.add(id);
                }
            }
        }

        if(channelIds.isEmpty())
        {
            throw new ParseException("No channel ids specified");
        }

        try
		{
            channelIds = Arrays.asList(IChannelValueFetch.purify(asArray(channelIds)));
		}
		catch (final ChannelListRangeException clre)
		{
			throw new ParseException("Bad channel range: " +
					clre.getMessage());
		}

        // Parse template (or default) and create sequencer
        try
        {
            ctabSequence = new CtabSequence(ctabTemplate, channelIds, null, 
            		appContext.getBean(DictionaryProperties.class).useChannelFormatters(),
            		appContext.getBean(SprintfFormat.class));
        }
        catch(final CtabException ce)
        {
        	final ParseException pe = new ParseException("CTAB error: " + ce.getMessage());
        	pe.initCause(ce);

        	throw pe;
        }

        final List<ChannelValueOrderByType> dtr =
            ctabSequence.getSortOrdering();

        // Process channel type selection arguments
        getChannelTypes(cmdline, channelTypeSelect, dtr);

        // Start and stop times are in primary sort order format

        times = convert(beginTimeString, endTimeString, dtr.get(0));

        vcids  = DssVcidOptions.parseVcid(
                     missionProps, 
                     cmdline,
                     channelTypeSelect.monitor ? 'M' : null,
                     channelTypeSelect.sse     ? 'S' : null,
                     CHANNEL_TYPES_LONG);

        dssIds = DssVcidOptions.parseDssId(cmdline,
                                           channelTypeSelect.sse ? 'S' : null,
                                           CHANNEL_TYPES_LONG);


        processIndexHints(cmdline);
    }

    /**
     * Process arguments for selecting the channel types.
     *
     * NB: Called from CTab, etc.
     *
     * @param cl
     *            Command line
     * @param cts
     *            Object to populate with selections
     * @param obt
     *            List of order-by types
     *
     * @throws ParseException
     *             Thrown on parameter error
     * @throws MissingArgumentException
     *             Thrown on missing value
     */
    public void getChannelTypes(final CommandLine cl, final ChannelTypeSelect cts,
                                final List<ChannelValueOrderByType> obt)
            throws ParseException, MissingArgumentException {
        computeChannelTypes(cl, cts);

        final boolean ob = ((obt != null) && !obt.isEmpty());
        final boolean tt = cl.hasOption(TIME_TYPE_LONG);

        if (cts.monitor && tt) {
            final String type = StringUtil.safeTrim(cl.getOptionValue(TIME_TYPE_LONG));

            if ((type.length() > 0) && !type.equalsIgnoreCase("ERT") && !type.equalsIgnoreCase("RCT")) {
                throwTimeTypeException("M", type);
            }
        }

        if (cts.monitor && ob) {
            for (final ChannelValueOrderByType cvobt : obt) {
                switch (cvobt.getValueAsInt()) {
                    case ChannelValueOrderByType.SCLK_TYPE:
                    case ChannelValueOrderByType.SCET_TYPE:
                    case ChannelValueOrderByType.LST_TYPE:
                    case ChannelValueOrderByType.MODULE_TYPE:

                        throwOrderException("M", cvobt);
                        break;

                    default:
                        break;
                }
            }
        }

        if (cts.header && ob) {
            for (final ChannelValueOrderByType cvobt : obt) {
                if (cvobt.getValueAsInt() == ChannelValueOrderByType.MODULE_TYPE) {
                    throwOrderException("H", cvobt);
                }
            }
        }

        if (cts.sseHeader && ob) {
            for (final ChannelValueOrderByType cvobt : obt) {
                if (cvobt.getValueAsInt() == IChannelValueOrderByType.MODULE_TYPE) {
                    throwOrderException("G", cvobt);
                }
            }
        }
    }

    /**
     * Process arguments for selecting the channel types.
     * 
     * @param cl
     *            Command line
     * @param cts
     *            Object to populate with selections
     *
     * @throws ParseException
     *             Thrown on parameter error
     * @throws MissingArgumentException
     *             Thrown on missing value
     */
    protected void computeChannelTypes(final CommandLine cl, final ChannelTypeSelect cts)
            throws ParseException, MissingArgumentException {
        // --channelTypes (or default) MUST contain at least one
        // valid flag and CANNOT contain unknown flags

        String channelTypes = StringUtil.safeCompressAndUppercase(cl.hasOption(CHANNEL_TYPES_LONG)
                ? cl.getOptionValue(CHANNEL_TYPES_LONG) : dbProperties.getChannelTypesDefault());

        if (channelTypes.isEmpty()) {
            throw new MissingArgumentException("You must provide a value for --" + CHANNEL_TYPES_LONG);
        }

        cts.monitor = channelTypes.contains("M");
        cts.header = channelTypes.contains("H");
        cts.fswRealtime = channelTypes.contains("F");
        cts.fswRecorded = channelTypes.contains("R");
        cts.sse = channelTypes.contains("S");

        cts.sseHeader = channelTypes.contains("G");

        channelTypes = channelTypes.replaceAll("[MHFRSG]", "");

        if (!channelTypes.isEmpty()) {
            throw new ParseException("Unknown flags '" + channelTypes + "' in --" + CHANNEL_TYPES_LONG);
        }

        final boolean tt = cl.hasOption(TIME_TYPE_LONG);
        final boolean module = cl.hasOption(MODULE_LONG);

        if (cts.monitor && tt) {
            final String type = StringUtil.safeTrim(cl.getOptionValue(TIME_TYPE_LONG));

            if (!type.equalsIgnoreCase("ERT") && !type.equalsIgnoreCase("RCT")) {
                throwTimeTypeException("M", type);
            }
        }

        if (cts.monitor && module) {
            throwModuleException("M");
        }

        if (cts.header && module) {
            throwModuleException("H");
        }

        if (cts.sseHeader && module) {
            throwModuleException("G");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
		return (APP_NAME + " --" + CHANNEL_TYPES_LONG + " <hmsfr>" +
                "\n--" + CHANNEL_IDS_LONG + " <string,...> | "      +
                "--" + CHANNEL_ID_FILE_LONG + " <name>\n"           +
                "[--" + TEMPLATE_LONG + " <string>\n"               +
                " --" + TEMPLATE_FILE_LONG + " <name>\n"            +
                " --" + CHANGE_VALUES_LONG                          +
                "]\n[Session search options - Not required]\n");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        /*
         * Because of the removal of statics from
         * App classes, I made the CtabFetcApp extend the ChannelValueFetchApp
         * class. The only problem with this appears to be the fact that the
         * showHelp() hierarchy got messed up, and the chill_get_ctabs help
         * dialog had extra stuff in it from ChannelValueFetchApp, and
         * AbstractChannelValueFetchApp. I chose the quick and dirty rout of
         * copying the showHelp() contents from AbstractFetchApp into the
         * showHelp method of CtabFetchApp. 20 lashes for me.
         */
        System.out.println(getUsage());
        final PrintWriter pw = new PrintWriter(System.out);
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printOptions(pw,
                               80,
                               options,
                               7,
                               2);
        pw.flush();
        System.out.println("\nMultiple query parameters will be ANDed together. Query values");
        System.out.println("are NOT case sensitive. All time values except SCLK should use");
        System.out.println("the format YYYY-MM-DDThh:mm:ss.ttt or  YYYY-DOYThh:mm:ss.ttt. Timezone for all times is GMT.");
        System.out.println("All string parameters whose long option name contains the word");
        System.out.println("\"Pattern\" may be entered using SQL pattern matching syntax such as");
        System.out.println("-" + ReservedOptions.TESTNAME_SHORT_VALUE
                + " %MyTestName%, which would find all sessions with names");
        System.out.println("that contain the string \"MyTestName\"");

        /*
         * The orginal showHelp for chill_get_ctabs
         */
        System.out.println("\nTimes and values for selected channel "         +
            "values will be written to standard output "                      +
            "in CTAB format. Note that you can specify "                      +
            "channel ids on the command line AND from a file, "               +
            "and the superset is used. But if the template is "               +
            "taken from a file, the command line template is ignored."        +
            "\nNote also that wildcards and ranges are not "                  +
            "allowed for channel ids."                                        +
            "\n\nThe CTAB template is a sequence of elements, "               +
            "separated by whitespace."                                        +
            "\nThe possible elements are:"                                    +
            "\n\nERT SCET SCLK LST C1(DN) ... Cn(EU)"                         +
            "\n\nin any order, where Cn is the index of the last channel id." +
            "\n\nIn other words, if you specify five channel "                +
            "ids, you must specify C1 through C5."                            +
            "\nYou can repeat "                                               +
            "elements as many times as you wish, but you must refer to "      +
            "\neach channel at least once."                                   +
            "\n\nExample: ERT C1(DN) C3(EU) C2(DN) C5(EU) "                   +
            "SCLK C4(EU) C1(DN)"                                              +
            "\n\nIt would be an error to specify C6, say, or "                +
            "to fail to specify C3."                                          +
            "\n\nThe default CTAB template is: ERT C1(DN) ... Cn(DN)"		  +
            "\n\nThe output column width can be configured two ways using "   +
            "Sprintf formatting (ie. %10s)." 								  +
            "\n--dataSpacing controls the data points (DN/EU) column width. " +
            "Default dataSpacing is '" + DEFAULT_DATA_OUTPUT_SPACING + "'."   + 
            "\n--timeSpacing controls the time value's column width. " 	  	  + 
            "Default timeSpacing is '" + DEFAULT_TIME_OUTPUT_SPACING + "'.");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions()
    {
        super.addAppOptions();
        
        addOption(CHANNEL_IDS_SHORT, CHANNEL_IDS_LONG, "string","A comma-separated list of channel IDs (e.g., A-0051,B-1951)");
        addOption(BEGIN_TIME_SHORT, BEGIN_TIME_LONG, "Time","Begin time of range");
        addOption(END_TIME_SHORT, END_TIME_LONG, "Time", "End time of range");
        addOption(CHANNEL_ID_FILE_SHORT, CHANNEL_ID_FILE_LONG, "string","The name of a file containing a list of channel IDs");
        addOption(TEMPLATE_SHORT, TEMPLATE_LONG, "string", "A CTAB template");
        addOption(TEMPLATE_FILE_SHORT,TEMPLATE_FILE_LONG,"string","A CTAB template file name");
        addOption(CHANGE_VALUES_SHORT, CHANGE_VALUES_LONG, null, "Channel values should only be reported on change; default is all channel values");
        addOption(DATA_SPACING_SHORT, DATA_SPACING_LONG, "dataSpacing", "Sprintf style string for formatting DATA columns");
        addOption(TIME_SPACING_SHORT, TIME_SPACING_LONG, "timeSpacing", "Sprintf style string for formatting TIME (header) columns");

		addOption(null,
                  CHANNEL_TYPES_LONG,
                  "string",
                  ChannelTypeSelect.RETRIEVE);

        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);

		addOption(null,
                  USE_INDEX_LONG,
                  "string,...",
                  "Use index");

		addOption(null,
                  FORCE_INDEX_LONG,
                  "string,...",
                  "Force index");

		addOption(null,
                  IGNORE_INDEX_LONG,
                  "string,...",
                  "Ignore index");
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run()
    {
        setExitCode(SUCCESS);

        int recordCount = 0;
        final IDbSqlFetch fetch = getFetch(sqlStmtOnly);

        // Make sure we connected to the database
        if(!fetch.isConnected())
        {
            setExitCode(OTHER_ERROR);
            return;
        }

        try
        {
            List<? extends IDbQueryable> out = (List<? extends IDbQueryable>) fetch.get(dbSessionInfo,times,defaultBatchSize,getFetchParameters());

            final PrintWriter pw = ctabSequence.getPrintWriter();
            pw.println();
            ctabSequence.printHeader();

            while (out.size() > 0)
            {
                for (final IDbQueryable samp : out)
                {
                    ctabSequence.provideChannel((IDbChannelSampleProvider) samp);
                    ++recordCount;
                }
                out = (List<? extends IDbQueryable>) fetch.getNextResultBatch();
            }
            ctabSequence.purgeChannel();

            pw.println();
            pw.flush();

            trace.debug("Retrieved " + recordCount + " records.");
        }
        catch (final Exception e)
        {
            trace.error("Problem encountered while retrieving channel " +
                             "value records: " + e);

            setExitCode(OTHER_ERROR);
        }
        finally
        {
            fetch.close();
            ctabSequence.getPrintWriter().close();
        }
    }


    /**
     * The main method to run the application
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args)
    {
        final CtabFetchApp app = new CtabFetchApp();
        app.runMain(args);
    }

    /**
     * Reads the template from a file.
     *
     * @param ctabTemplateFile
     *
     * @return Template string
     *
     * @throws ParseException
     */
    private String readTemplateFile(final String ctabTemplateFile)
        throws ParseException
    {
        final StringBuilder sb           = new StringBuilder();
        long                line_counter = 0L;
        BufferedReader      myReader     = null;

        try
        {
            try
            {
                myReader = new BufferedReader(new FileReader(ctabTemplateFile));
            }
            catch (final FileNotFoundException fnfe)
            {
                throwParseExceptionWithCause(
                    "Could not find specified template file '" +
                        ctabTemplateFile                       +
                        "'",
                    fnfe);
            }

            // Loop over the lines in the file and concatenate them

            String currentLine = null;

            while (true)
            {
                ++line_counter;

                try
                {
                    currentLine = myReader.readLine();
                }
                catch (final IOException ioe)
                {
                    throwParseExceptionWithCause("Failed to read line "    +
                                                     line_counter          +
                                                     " of template file '" +
                                                     ctabTemplateFile      +
                                                     "'",
                                                 ioe);
                }

                if (currentLine == null)
                {
                    break;
                }

                sb.append(currentLine).append(' ');
            }
        }
        finally
        {
            if (myReader != null)
            {
                try
                {
                    myReader.close();
                }
                catch (final IOException ioe)
                {
                    throwParseExceptionWithCause(
                        "Could not close template file '" +
                            ctabTemplateFile              +
                            "'",
                        ioe);
                }
            }
        }

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return new String[] {ChannelValueOrderByType.ERT.getValueAsString(), 
        		             ChannelValueOrderByType.SCET.getValueAsString(),
        		             ChannelValueOrderByType.SCLK.getValueAsString(),
        		             ChannelValueOrderByType.LST.getValueAsString()};
    }
    
    /**
     * Convert a collection to an array
     *
     * @param target Collection to convert
     *
     * @return Array
     */
    private String[] asArray(final Collection<String> target)
    {
        return ((target != null) ? target.toArray(new String[target.size()])
                                 : null);
    }


    /**
     * Convert start and stop times as strings to database time-range object.
     *
     * @param start Start time string
     * @param stop  Stop time string
     * @param type  Primary order-by
     *
     * @return DatabaseTimeRange for use in query
     *
     * @throws ParseException
     */
    private DatabaseTimeRange convert(final String                  start,
                                      final String                  stop,
                                      final ChannelValueOrderByType type)
        throws ParseException
    {
        if (type == null)
        {
            throw new ParseException("Null order-by type");
        }

        // Convert order-by type to DB time type

        DatabaseTimeType dtt = null;

        switch (type.getValueAsInt())
        {
            case ChannelValueOrderByType.ERT_TYPE:

                dtt = DatabaseTimeType.ERT;
                break;

            case ChannelValueOrderByType.SCET_TYPE:

                dtt = DatabaseTimeType.SCET;
                break;
                
            case ChannelValueOrderByType.LST_TYPE:

                dtt = DatabaseTimeType.LST;
                break;

            case ChannelValueOrderByType.SCLK_TYPE:

                dtt = DatabaseTimeType.SCLK;
                break;

            default:
                throw new ParseException("Unsupported order-by type: " + type);
        }

        final DatabaseTimeRange dtr = new DatabaseTimeRange(dtt);

        if ((start == null) && (stop == null))
        {
            return dtr;
        }

        String mode = null;

        switch (dtt.getValueAsInt())
        {
            case DatabaseTimeType.ERT_TYPE:

                try
                {
                    if (start != null)
                    {
                        mode = "start";
                        dtr.setStartTime(new AccurateDateTime(start));
                    }

                    if (stop != null)
                    {
                        mode = "stop";
                        // Updated to get all values to the very end of the millisecond
                        final IAccurateDateTime stopTime = new AccurateDateTime(stop);
                        dtr.setStopTime(new AccurateDateTime(stopTime, maxMicroTenths, true));
                    }
                }
                catch (final java.text.ParseException pe)
                {
                    throwParseExceptionWithCause(
                        "Channel value "            +
                            mode                    +
                            " time has an invalid " +
                            "format. Should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:sss.ttt",
                        pe);
                }

                break;

            case DatabaseTimeType.SCET_TYPE:

                try
                {
                    if (start != null)
                    {
                        mode = "start";
                        // Changed to IAccurateDateTime for better parsing
                        dtr.setStartTime(new AccurateDateTime(start));
                    }

                    if (stop != null)
                    {
                        mode = "stop";
                        // Changed to IAccurateDateTime for better parsing
                        dtr.setStopTime(new AccurateDateTime(stop));
                    }
                }
                catch (final java.text.ParseException pe)
                {
                    throwParseExceptionWithCause(
                        "Channel value "            +
                            mode                    +
                            " time has an invalid " +
                            "format. Should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:sss.ttt",
                        pe);
                }

                break;

            case DatabaseTimeType.LST_TYPE:

                try
                {
                    if (start != null)
                    {
                        mode = "start";
                        dtr.setStartSclk((LocalSolarTimeFactory.getNewLst(start, missionProps.getDefaultScid())).toSclk());
                    }

                    if (stop != null)
                    {
                        mode = "stop";
                        dtr.setStopSclk((LocalSolarTimeFactory.getNewLst(stop, missionProps.getDefaultScid())).toSclk());
                    }
                }
                catch (final java.text.ParseException pe)
                {
                    throwParseExceptionWithCause(
                        "Channel value "            +
                            mode                    +
                            " time has an invalid " +
                            "format. Should be SOL-DDDDMhh:mm:ss.ttt",
                        pe);
                }

                break;
            case DatabaseTimeType.SCLK_TYPE:

                try
                {
                    if (start != null)
                    {
                        mode = "start";
                        dtr.setStartSclk(sclkFmt.valueOf(start));
                    }

                    if (stop != null)
                    {
                        mode = "stop";
                        dtr.setStopSclk(sclkFmt.valueOf(stop));
                    }
                }
                catch (final NumberFormatException nfe)
                {
                    throwParseExceptionWithCause(
                        "Channel value "            +
                            mode                    +
                            " time has an invalid " +
                            "format. Should be CCCCCCCCCC.fffff",
                        nfe);
                }

                break;

            default:

                throw new ParseException("Time type is not one of " +
                                         "ERT, SCET, LST, or SCLK");
        }

        return dtr;
    }


    /**
     * Utility method to throw a ParseException with a cause.
     *
     * @param text
     * @param cause
     *
     * @throws ParseException
     */
    private static void throwParseExceptionWithCause(final String text,final Throwable cause) throws ParseException
    {
        final ParseException pe = new ParseException(text);
        pe.initCause(cause);
        throw pe;
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public void checkTimeType(final DatabaseTimeRange range) throws ParseException
	{
		//irrelevant
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public DatabaseTimeType getDefaultTimeType() {
        // Fix default time type
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
	{
		fetch = appContext.getBean(IDbSqlFetchFactory.class).getChannelValueFetch(sqlStmtOnly,
                        channelIds.toArray(new String[channelIds.size()]),
                        null,
                        false,
                        false);

		return fetch;
	}


    /**
     * {@inheritDoc}
     */
	@Override
	public Object[] getFetchParameters()
	{
        final Object[] params =
            new Object[ChannelValueFetchApp.NUM_QUERY_PARAMS];

    	params[0]  = channelIds.toArray(new String[channelIds.size()]);
    	params[1]  = null;
    	params[2]  = channelTypeSelect;
		params[3]  = null;
    	params[4]  = allChannelValues;
    	params[5]  = null;
        params[6]  = Boolean.FALSE; // Descending
        params[7]  = vcids;
        params[8]  = dssIds;
        params[9]  = null;
        params[10] = getIndexHints();

        // Add extra sorts to make sure that the data always comes out the
        // same way.
        //
        // Must add these at the last minute because the rest of the code
        // expects only time types.

        final List<IChannelValueOrderByType> allSorts =
            new ArrayList<IChannelValueOrderByType>(
                    ctabSequence.getSortOrdering());

        allSorts.add(ChannelValueOrderByType.HOST_ID);
        allSorts.add(ChannelValueOrderByType.TEST_SESSION_ID);
        allSorts.add(ChannelValueOrderByType.CHANNEL_ID);

    	params[3] = allSorts;

    	return(params);
	}
}
