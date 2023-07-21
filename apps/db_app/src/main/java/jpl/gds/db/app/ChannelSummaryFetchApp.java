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

import java.io.File;
import java.io.IOException;
import java.util.*;

import jpl.gds.common.options.querycommand.ChannelTypesOption;
import jpl.gds.db.mysql.impl.sql.order.ChannelAggregateOrderByType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.LineHandler;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;

/**
 * Application to fetch a summary of channel values from the database.
 *
 */
public class ChannelSummaryFetchApp extends AbstractFetchApp
{
	private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_chan_summary");
	static final String VELOCITY_TEMPLATE = "chan_summary";
	static final String CHANNEL_STEM_SHORT = "S";
	static final String CHANNEL_STEM_LONG = "stem";
	static final String STEM_ONLY_LONG = "stemOnly";
	static final String COUNT_LONG = "count";
	private static final int NUM_QUERY_PARAMS = 5;
	private String stem;
	private boolean stemOnly;
	private boolean count;

	/** VCIDs to query for */
	private Set<Integer> vcids = null;

	/** DSS ids to query for */
	private Set<Integer> dssIds = null;

	final Tracer log = TraceManager.getTracer(appContext, Loggers.DB_FETCH);

	/**
	 * Constructor.
     *
	 */
	public ChannelSummaryFetchApp() {
		super(IChannelValueLDIStore.DB_CHANNEL_DATA_TABLE_NAME,
              APP_NAME,
              null);

        suppressInfo();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.IDbFetchApp#checkTimeType(jpl.gds.db.api.types.DatabaseTimeRange)
	 */
	@Override
	public void checkTimeType(final DatabaseTimeRange range) throws ParseException {
		// times are not used in this fetch
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.IDbFetchApp#addAppOptions()
	 */
	@Override
	protected void addAppOptions() {
		super.addAppOptions();
		addOption(CHANNEL_STEM_SHORT, CHANNEL_STEM_LONG, "string", "The stem of the channels to search for");
		addOption(ReservedOptions.createOption(STEM_ONLY_LONG, null, "Shows only the distinct channel stems for the session"));
		addOption(ReservedOptions.createOption(COUNT_LONG, null, "In addition to the channel data, also display the count of channel values"));
		addOption(SHOW_COLUMNS_SHORT, SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
		// This app puts out summary data in 2 columns - does not need formatting options
		// addOption(OUTPUT_FORMAT_SHORT, OUTPUT_FORMAT_LONG, "format", OUTPUT_FORMAT_DESC);
		addOrderOption();

      DssVcidOptions.addVcidOption(options);
      DssVcidOptions.addDssIdOption(options);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.IDbFetchApp#configureApp(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void configureApp(final CommandLine cmdline) throws ParseException {
		super.configureApp(cmdline);

		if (cmdline.hasOption(CHANNEL_STEM_SHORT)) {
			stem = cmdline.getOptionValue(CHANNEL_STEM_SHORT);
		}

		if (cmdline.hasOption(STEM_ONLY_LONG)) {
			stemOnly = true;
		} else {
			stemOnly = false;
		}

		if (cmdline.hasOption(COUNT_LONG)) {
			count = true;
		} else {
			count = false;
		}

		if (cmdline.hasOption(SHOW_COLUMNS_SHORT)) {
			this.showColHeaders = true;
		} else {
			this.showColHeaders = false;
		}

		vcids = DssVcidOptions.parseVcid(missionProps, cmdline, null);
		
		if (!count && vcids != null && !vcids.isEmpty()) {
		     throw new ParseException("The --" + DssVcidOptions.VCID_OPTION_LONG + " option only has meaning in conjunction with the --" + COUNT_LONG + " option");	
		}

		dssIds = DssVcidOptions.parseDssId(cmdline, null);
		
		if (!count && dssIds != null && !dssIds.isEmpty()) {
		     throw new ParseException("The --" + DssVcidOptions.DSS_ID_OPTION_LONG + " option only has meaning in conjunction with the --" + COUNT_LONG + " option");	
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.IDbFetchApp#getDefaultTimeType()
	 */
	@Override
	public DatabaseTimeType getDefaultTimeType() {
		return (DatabaseTimeType.ERT);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.IDbFetchApp#getFetch(boolean)
	 */
	@Override
	public IDbSqlFetch getFetch(final boolean sqlStmtOnly) {
		fetch =  appContext.getBean(IDbSqlFetchFactory.class).getChannelSummaryFetch(isSqlStmtOnly());
		return fetch;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.IDbFetchApp#getFetchParameters()
	 */
	@Override
	public Object[] getFetchParameters() {
		final Object[] params = new Object[NUM_QUERY_PARAMS];

		params[0] = stem;
		params[1] = stemOnly;
		params[2] = count;
		params[3] = vcids;
        params[4] = dssIds;

		return params;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.IDbFetchApp#getOrderByValues()
	 */
	@Override
	public String[] getOrderByValues() {
		return new String[] {};
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.db.api.app.AbstractDatabaseApp#getUsage()
	 */
	@Override
	public String getUsage() {
		return APP_NAME + " --testKey <number> --testHostPattern <host>\n"
		        + "[--" + CHANNEL_STEM_LONG + " <stem>]\n";
	}


	/**
	 * This method is used when the --count option is included in the command line.
	 * When channel count information is needed, this tool now call chill_get_chanvals
	 * instead of querying the database directly. Direct query to channel aggregate
	 * tables no longer works for counts so aggregate structures need to be unpacked
	 * to collect channel count information.
	 *
	 * @param args the command line arguments passed in for this application
	 */
    public void runWithCount(final String[] args) {

		final List<String> csvColumns;
		List<String> argList = new ArrayList<>();

		try {
			final CommandLine cmdline = parseCommandLine(args);
			configure(cmdline);
			configureApp(cmdline);

			final ChanvalLineHandler chanvalLineHandler = new ChanvalLineHandler(stem, stemOnly, log);

			// Set up the process launcher for chill_get_chanvals command
			final ProcessLauncher processLauncher = new ProcessLauncher();
			processLauncher.setOutputHandler(chanvalLineHandler);
			processLauncher.setErrorHandler(new StderrLineHandler());

			// Modify command line args so we can pass them on to chill_get_chanvals
			argList = constructChanvalsCommandLine(args);

			// Launch the chill_get_chanvals process
			if (!processLauncher.launch(
					argList.toArray(new String[argList.size()]),
					GdsSystemProperties.getSystemProperty(GdsSystemProperties.JAVA_IO_TEMPDIR))) {
				log.error("Unable to launch command: " + String.join(" ", argList));
			}
			log.debug("Launched command: " + String.join(" ", argList));

			// Wait for the process to complete
			processLauncher.waitForExit();

			// Show column headers if option '-m,--showColumns' specified
			if (showColHeaders) {
				if (stemOnly) {
					System.out.println("channelStem,count");
				} else {
					System.out.println("channelId,channelName,count");
				}
			}

			// Output results with count information
			for (final Map.Entry<String, ChannelSampleCount> entry : chanvalLineHandler.getChannelCountMap().entrySet()) {
				if (stemOnly) {
					System.out.println(entry.getValue().outputStemOnlyCount());
				} else {
					System.out.println(entry.getValue().outputCount());
				}
			}

			exitCode = 0;
		} catch (ParseException e) {
			log.error("Encountered unexpected parse exception: " + e.toString());
			exitCode = 1;
		} catch (IOException e) {
			log.error("Encountered unexpected IO exception: " + e.getMessage()
					+ " when attempting to execute: " + String.join(" ", argList));
			exitCode = 1;
		}

		System.exit(getExitCode());
	}


	/**
	 * This method takes the input arguments of the Channel Summary Fetch App
	 * and makes the necessary adjustments to pass them on to chill_get_chanvals.
	 * Arguments that are unique to chill_get_chan_summary are filtered out
	 * and '--channelTypes frhmsg' added since channel count is done on all
	 * channel types.
	 *
	 * @param args arguments passed in for chill_get_chan_summary
	 * @return arguments that are suitable for chill_get_chanvals
	 */
	public List<String> constructChanvalsCommandLine(final String[] args) {
		boolean skipValue = false;
		List<String> argList = new ArrayList<>();

		final String scriptName =
				GdsSystemProperties.getSystemProperty(
						GdsSystemProperties.DIRECTORY_PROPERTY) +
						File.separator +
						"bin/chill_get_chanvals";
		argList.add(scriptName);

		// Filter out any command line arguments we don't want to pass to 'chill_get_chanvals'
		for (String arg : Arrays.asList(args)) {

			// Filter out arguments which don't have a value
			if (arg.equalsIgnoreCase("-" + SHOW_COLUMNS_SHORT)
					|| arg.equalsIgnoreCase("--" + SHOW_COLUMNS_LONG)
					|| arg.equalsIgnoreCase("--" + COUNT_LONG)
					|| arg.equalsIgnoreCase("--" + STEM_ONLY_LONG)) {
				continue;
			}

			// Arguments that do have a value, need the value removed as well
			if (arg.equalsIgnoreCase("-" + CHANNEL_STEM_SHORT)
					|| arg.equalsIgnoreCase("--" + CHANNEL_STEM_LONG)) {
				skipValue = true;
				continue;
			}

			if (skipValue) {
				skipValue = false;
				continue;
			}

			argList.add(arg);
		}

		// We want to count all channel types
		argList.add("--" + CHANNEL_TYPES_LONG);
		argList.add(ChannelTypesOption.ALL_CHANNEL_TYPES);

		// chill_get_chan_summary --count throws java.lang.OutOfMemoryError
		// when dealing with a large data set.
		//
		// Data time sort order is irrelevant, so use NONE for more efficient processing by chill_get_chanvals
		argList.add("--" + ORDER_BY_LONG);
		argList.add(ChannelAggregateOrderByType.NONE.toString());

		// chill_get_chan_summary --count throws java.lang.OutOfMemoryError
		// when dealing with a large data set.
		//
		// Added a new velocity template for transferring data from chill_get_chanvals to this tool.
		// We only need the Channel ID and Channel Name to do the counts.
		// This significantly reduced data volume and improved performance.
		// Biggest impact to performance and memory management was the elimination of
		// IDbChannelSampleProvider.parseCsv(line, csvColumns) which was a major bottleneck.
		// With this change, the incoming EHA sample handling rate went from 50,000 - 100,000 records/sec
		// to ~1,500,000 records/sec
		argList.add("--"+ OUTPUT_FORMAT_LONG);
		argList.add(VELOCITY_TEMPLATE);

		return argList;
	}


	/**
	 * This class is used to parse the CSV records output by the
	 * chill_get_chanvals tool. Output produced by chill_get_chanvals
	 * is parsed to compute channel count information.
	 *
	 */
	public static final class ChanvalLineHandler implements LineHandler {

		/**
		 * Map that holds the ChannelSampleCount object keyed by the channel ID
		 */
		private Map<String, ChannelSampleCount> channelCountMap = new TreeMap<>();

		/**
		 * Channel Stem to do the count on
		 */
		private final String channelStemFilter;

		/**
		 * Flag set to true when the --stemOnly argument is included
		 */
		private final boolean stemOnlyFlag;

		/**
		 * Variables used for input parsing
		 */
		private String[] recArray;
		private String channelId;
		private String channelName;
		private String channelStem;
		private String mapKey;
		
		private Tracer log;

		/**
		 * Constructor
		 * @param channelStemFilter the channel stem passed in at the command line
		 * @param stemOnlyFlag the --stemOnly flag, true is specified, false otherwise
		 * @param log the tracer
		 */
		ChanvalLineHandler(final String channelStemFilter, final boolean stemOnlyFlag, final Tracer log) {
			this.channelStemFilter = channelStemFilter;
			this.stemOnlyFlag = stemOnlyFlag;
			this.log = log;
		}

		@Override
		public void handleLine(final String line) {

			// Parse chill_get_chanvals output line formatted with the use of the 'chan_summary' template.
			// Expected format is 'ChannelId,ChannelName'
			try {
				recArray = line.split(",");
				channelId = recArray[0];
				channelName = recArray[1];
				channelStem = channelId.substring(0, channelId.indexOf("-"));
			} catch (Exception e) {
				log.warn("Unable to parse the following line, will be skipped: " + line);
				return;
			}

			// If the stem of the channel is specified using options -S,--stem
			// make sure the current channel id stem matches before including
			// it in the count
			if (channelStemFilter != null && !channelStem.equalsIgnoreCase(channelStemFilter)) {
				// skip
				return;
			}

			ChannelSampleCount channelSampleCount = new ChannelSampleCount();
			channelSampleCount.setChannelId(channelId);
			channelSampleCount.setChannelStem(channelStem);
			channelSampleCount.setChannelName(channelName);

			// If we have the --stemOnly option specified
			// use the stem as the map key, otherwise the channel Id
			// should be the map key.
			if (stemOnlyFlag) {
				mapKey = channelStemFilter;
			} else {
				mapKey = channelId;
			}

			if (channelCountMap.containsKey(mapKey)) {
				channelCountMap.get(mapKey).incrementCount();
			} else {
				channelCountMap.put(mapKey, channelSampleCount);
			}
		}

		/**
		 * Gets the map which contains all count information.
		 * When --stemOnly is not specified, the map key is based
		 * on the channel IDs. If it is present then the map key
		 * is based on the channel stem.
		 *
		 * @return the map holding the count information
		 */
		public Map<String, ChannelSampleCount> getChannelCountMap() {
			return channelCountMap;
		}
	}

	/**
	 * Simple POJO used to contain channel information such as
	 * Channel ID, Channel Name, Channel Stem and Count
	 */
	public static final class ChannelSampleCount {
		private String channelId;
		private String channelName;
		private String channelStem;
		private Integer count = 1;

		/**
		 * Gets the channel ID
		 *
		 * @return the channel ID
		 */
		public String getChannelId() {
			return channelId;
		}

		/**
		 * Sets the channel ID
		 *
		 * @param channelId the channel ID to set
		 */
		public void setChannelId(final String channelId) {
			this.channelId = channelId;
		}

		/**
		 * Gets the channel name
		 *
		 * @return the channel name
		 */
		public String getChannelName() {
			return channelName;
		}

		/**
		 * Sets the channel name
		 *
		 * @param channelName the channel name to set
		 */
		public void setChannelName(final String channelName) {
			this.channelName = channelName;
		}

		/**
		 * Gets the channel stem
		 *
		 * @return the channel stem
		 */
		public String getChannelStem() {
			return channelStem;
		}

		/**
		 * Sets the channel stem
		 *
		 * @param channelStem the channel stem to set
		 */
		public void setChannelStem(final String channelStem) {
			this.channelStem = channelStem;
		}

		/**
		 * Increment the count. Initial value is set to 1 and
		 * incremented by 1 every time this method gets called.
		 */
		public void incrementCount() {
			this.count++;
		}

		/**
		 * Returns a CSV string in the form of 'ChannelId,ChannelName,Count'
		 * Ex: A-0001,TEST_CHANNEL,10
		 *
		 * @return the CSV string 'ChannelId,ChannelName,Count'
		 */
		public String outputCount() {
			return this.channelId + "," + this.channelName + "," + this.count;
		}

		/**
		 * Returns a CSV string in the form of 'ChannelStem,Count'
		 * Ex: FSW,12
		 *
		 * @return the CSV string 'ChannelStem,Count'
		 */
		public String outputStemOnlyCount() {
			return this.channelStem + "," + this.count;
		}
	}

	/**
	 * The main method to run the application
	 *
	 * @param args The command line arguments
	 */
	public static void main(final String[] args) {
		final ChannelSummaryFetchApp app = new ChannelSummaryFetchApp();
		boolean runWithCount = false;

		// Check whether or not --count is included in the arguments
		// The official argument handling is done farther downstream
		for (String arg : Arrays.asList(args)) {
			if (arg.equalsIgnoreCase("--" + COUNT_LONG)) {
				runWithCount = true;
				break;
			}
		}

		// When the --count argument is included we need to call
		// chill_get_chanvals so we can collect channel count information
		if (runWithCount) {
			app.runWithCount(args);
		} else {
			app.runMain(args);
		}
	}
}
