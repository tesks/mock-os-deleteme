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
package jpl.gds.globallad.options;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.globallad.rest.resources.QueryOutputFormat;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.app.ICommandLineApp;
import jpl.gds.shared.cli.options.ChannelOption;
import jpl.gds.shared.cli.options.CsvStringOption;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.CsvUnsignedIntOption;
import jpl.gds.shared.cli.options.numeric.IntegerOption;

/**
 * Options for global lad applications.
 */
public class GlobalLadOptions extends BaseCommandOptions implements ICommandLineOptionsGroup {
    private static final String COMMAND_LINE_WILDCARD_SYMBOL = "%";
    /** The REST server uses the * character as the wild card **/
    private static final String REGEX_WILDCARD_SYMBOL = "*";

    /** The port command line option and description */
    private static final String SHORT_PORT_OPTION = "p";
    private static final String LONG_PORT_OPTION = "restServerPort";
    private static final String DESCRIPTION_PORT =
            "The port for the global LAD "
                    + "server to listen to for incoming requests. Defaults to "
                    + GlobalLadProperties.getGlobalInstance().getRestPort();

    private static final String SHORT_SERVER_HOST_OPTION = "H";
    private static final String LONG_SERVER_HOST_OPTION = "restServerHost";
    private static final String DESCRIPTION_SERVER_HOST = "The REST server host to use when constructing the query URL.  Current value is "
            + GlobalLadProperties.getGlobalInstance().getServerHost();

    /** Data source option */
    private static final String SHORT_MODE_OPTION = "s";
    private static final String LONG_MODE_OPTION = "serverMode";
    private static final String DESCRIPTION_MODE = "Override configured server data source mode. Use SOCKET or JMS";

    /** Socket Server options **/
    private static final String SHORT_SOCKET_PORT_OPTION = "P";
    private static final String LONG_SOCKET_PORT_OPTION = "socketServerPort";
    private static final String DESCRIPTION_SOCKET_PORT = "The port the socket server listens to for new "
    		+ "connections from chill_downs to feed data into the global LAD.  Current value is "
            + GlobalLadProperties.getGlobalInstance().getSocketServerPort();

    /** JMS Data Source options **/
    private static final String SHORT_JMS_TOPICS_LIST_OPTION = "t";
    private static final String LONG_JMS_TOPICS_LIST_OPTION = "topics";
    private static final String DESCRIPTION_JMS_TOPICS_LIST = "List of root JMS topics to listen to. Application topics for " +
            "EHA and EVRs are derived from the root.";

    // jms venue type
    private static final String LONG_JMS_VENUE_TYPE_OPTION = "venueType";
    private static final String SHORT_JMS_VENUE_TYPE_OPTION = "V";
    private static final String DESCRIPTION_JMS_VENUE_TYPE = "operational or test venue to use. only applicable in JMS mode";

    // jms downlink stream id
    private static final String LONG_JMS_DOWNLINK_STREAM_TYPE_OPTION = "downlinkStreamId";
    private static final String SHORT_JMS_DOWNLINK_STREAM_TYPE_OPTION = "E";
    private static final String DESCRIPTION_JMS_DOWNLINK_STREAM_TYPE = "downlink stream ID for TESTBED or ATLO; defaults based upon venue type. only applicable in JMS mode";

    // jms testbed name
    private static final String LONG_JMS_TESTBED_NAME_OPTION = "testbedName";
    private static final String SHORT_JMS_TESTBED_NAME_OPTION = "G";
    private static final String DESCRIPTION_JMS_TESTBED_NAME = "the name of the testbed; only applicable if venue type supports testbed names. only applicable in JMS mode";

    // jms host name
    private static final String SHORT_JMS_HOST_NAME_OPTION = "D";
    private static final String LONG_JMS_HOST_NAME_OPTION = "downlinkHostName";
    private static final String DESCRIPTION_JMS_HOST_NAME    = "JMS Downlink host name, for creating default JMS topics in JMS mode";

    private static final String SHORT_RESTORE_FILE_OPTION = "f";
    private static final String LONG_RESTORE_FILE_OPTION = "restoreBackupFile";
    private static final String DESCRIPTION_RESTORE_FILE = "Override the backup file for the global lad to initialze to at start up.  "
    		+ "The restoreBackupFile option  must be set or this option is ignored.";

    private static final String SHORT_DO_RESTORE_OPTION = "r";
    private static final String LONG_DO_RESTORE_OPTION = "restoreFromBackup";
    private static final String DESCRIPTION_DO_RESTORE = "If set will initialize the global lad using the latest configured backup file";

	// Query options.
    /** Short begin-time option */
    private static final String BEGIN_TIME_SHORT = "b";

    /** Long begin-time option */
    private static final String BEGIN_TIME_LONG = "beginTime";

    private static final String BEGIN_DESC = "Start time for global lad search bracket.  This is a greater than equal to value.";

    /** Short end-time option */
    private static final String END_TIME_SHORT = "e";

    /** Long end-time option */
    private static final String END_TIME_LONG = "endTime";
    private static final String END_DESC = "End time for global lad search bracket.  This is a less than value.";

    /** Short S/C id option */
    private static final String SCID_SHORT = "s";

    /** Short S/C id option */
    private static final String SCID_LONG = "scid";
    private static final String SCID_DESC = "Spacecraft ID for the query.";

    /** Short S/C id option */
    private static final String VCID_SHORT = "c";

    /** Short S/C id option */
    private static final String VCID_LONG = "vcid";
    private static final String VCID_DESC = "Session VCID.";


    /** Short S/C id option */
    private static final String DSSID_SHORT = "d";

    /** Short S/C id option */
    private static final String DSSID_LONG = "dssId";
    private static final String DSSID_DESC = "CSV of Session dssIds.";



    private static final String EVR_NAME_SHORT = "E";
    /** Long EVR name option */
    private static final String EVR_NAME_LONG = "evrNamePattern";
    private static final String EVR_NAME_DESC = "CSV list of EVR name regexes.";

    /** Short level option */
    private static final String EVR_LEVEL_SHORT = "l";
    /** Long level option */
    private static final String EVR_LEVEL_LONG = "evrLevel";
    private static final String EVR_LEVEL_DESC = "CSV list of EVR levels to query.";

    private static final String MESSAGE_REGEXX_LONG = "evrMessageRegex";
    private static final String MSG_REGES = "CSV list of REGEXES to search through EVR messages.";

    private static final String SESSION_KEY_SHORT = "K";
    private static final String SESSION_KEY_LONG = "testKey";
    private static final String SESSION_KEY_DESC = "CSV list of session keys or session key ranges.";

    private static final String SESSION_HOST_SHORT = "O";
    private static final String SESSION_HOST_LONG = "testHost";
    private static final String SESSION_HOST_DESC = "CSV list of host regexes.";

    private static final String VENUE_SHORT = "V";
    private static final String VENUE_LONG = "venueType";
    private static final String VENUE_DESC = "Venue of the test session to query.";

    /** Source of the data.  fsw|monitor|header|sse|all **/
    private static final String SOURCE_SHORT = "S";
    private static final String SOURCE_LONG = "dataSource";
    private static final String SOURCE_DESC = "Specify the data source for the query.  Value is not case sensitive";

//    /** Long alarm-only option */
//    public static final String ALARM_ONLY_LONG  = "alarmOnly";
//    public static final String BEGIN_DESC = "";

    /** The time type command line option and description */
    private static final String TIMESTAMP_TYPE_SHORT_OPTION = "t";
    private static final String TIMESTAMP_TYPE_LONG_OPTION = "timestampType";
    private static final String TIMESTAMP_TYPE_DESCRIPTION = "Timetype of the query. " +
    		"In the case of all every internal time buffer will be "
    		+ "combined and the primary timetype will be used to sort the merged lists and the number of requested latest values will be returned.  Default value is EVENT";

    /** The realtime command line option and description */
    private static final String REALTIME_SHORT_OPTION = "q";
    private static final String REALTIME_LONG_OPTION = "recordedState";
    private static final String REALTIME_DESCRIPTION =
            "The recorded state of the data to be queried. If not specified will return both";

    /** The channel IDs command line option and description */
    private static final String CHANNEL_IDS_SHORT_OPTION = "z";
    private static final String CHANNEL_IDS_LONG_OPTION = "channelIds";
    private static final String CHANNEL_IDS_DESCRIPTION =
            "A comma-separated list of channel names or ranges or wildcards (e.g. A-0051,B-%,C-0123..C-0200)";


    private static final String MAX_RESULTS_KEY_SHORT = "M";
    private static final String MAX_RESULTS_KEY_LONG = "maxResults";
    private static final String MAX_RESULTS_DESC = "Maximum number of results for each data sample matching the query parameters.  Default value if not specified is 1.";

    private static final String OUTPUT_FORMAT_SHORT = "o";
    private static final String OUTPUT_FORMAT_LONG = "outputFormat";
    private static final String OUTPUT_FORMAT_DESC = "The formatting style for the query output with a Velocity template";

    private static final String SHOW_COLUMNS_SHORT = "m";
    private static final String SHOW_COLUMNS_LONG = "showColumns";
    private static final String SHOW_COLUMNS_DESC =
                                   "Include column names in CSV output";

    private static final String VERIFY_LONG = "verify";
    private static final String VERIFY_DESC =
                                   "If set the output data will be verified.";


    /**
     * Below are the configured static options for the global lad.
     */

    /**
     * Server options
     */
	public static final IntegerOption REST_SERVER_PORT_OPTION = new IntegerOption(SHORT_PORT_OPTION, LONG_PORT_OPTION, "restServerPort", DESCRIPTION_PORT, false);
	public static final StringOption REST_SEVER_HOST_OPTION = new StringOption(SHORT_SERVER_HOST_OPTION, LONG_SERVER_HOST_OPTION, "restServerHost", DESCRIPTION_SERVER_HOST, false);

	public static final IntegerOption SOCKET_SERVER_PORT_OPTION = new IntegerOption(SHORT_SOCKET_PORT_OPTION, LONG_SOCKET_PORT_OPTION, "socketServerPort", DESCRIPTION_SOCKET_PORT, false);
	public static final FileOption RESTORE_FILE_OPTION = new FileOption(SHORT_RESTORE_FILE_OPTION, LONG_RESTORE_FILE_OPTION,
			"restoreFile", DESCRIPTION_RESTORE_FILE, false, true);
	public static final FlagOption                                      DO_RESTORE_OPTION          = new FlagOption(SHORT_DO_RESTORE_OPTION, LONG_DO_RESTORE_OPTION, DESCRIPTION_DO_RESTORE, false);
    public static final EnumOption<IGlobalLadDataSource.DataSourceType> MODE_OPTION                = new EnumOption<>(
            IGlobalLadDataSource.DataSourceType.class,
            SHORT_MODE_OPTION, LONG_MODE_OPTION, "serverMode", DESCRIPTION_MODE, false, null);
    public static final StringOption                                    JMS_TOPICS_OVERRIDE_OPTION = new StringOption(SHORT_JMS_TOPICS_LIST_OPTION, LONG_JMS_TOPICS_LIST_OPTION, "topics", DESCRIPTION_JMS_TOPICS_LIST, false);
    public static final EnumOption<VenueType> JMS_VENUE_TYPE_OPTION = new EnumOption<>(VenueType.class,
            SHORT_JMS_VENUE_TYPE_OPTION, LONG_JMS_VENUE_TYPE_OPTION, "venue", DESCRIPTION_JMS_VENUE_TYPE, false, null);
    public static final EnumOption<DownlinkStreamType> JMS_DOWNLINK_STREAM_TYPE_OPTION = new EnumOption<>(
            DownlinkStreamType.class, SHORT_JMS_DOWNLINK_STREAM_TYPE_OPTION, LONG_JMS_DOWNLINK_STREAM_TYPE_OPTION,
            "stream", DESCRIPTION_JMS_DOWNLINK_STREAM_TYPE, false, null);
    public static final StringOption JMS_TESTBED_NAME_OPTION = new StringOption(SHORT_JMS_TESTBED_NAME_OPTION,
            LONG_JMS_TESTBED_NAME_OPTION, "testbed", DESCRIPTION_JMS_TESTBED_NAME, false, null);
    public static final StringOption JMS_HOST_NAME_OPTION = new StringOption(SHORT_JMS_HOST_NAME_OPTION,
            LONG_JMS_HOST_NAME_OPTION, "jmsHostName", DESCRIPTION_JMS_HOST_NAME, false, null);
	/**
	 * Basic query options.
	 */
	public static final CsvUnsignedIntOption  SESSION_KEY_OPTION = new CsvUnsignedIntOption(SESSION_KEY_SHORT, SESSION_KEY_LONG, "sessionKey", SESSION_KEY_DESC, true, true, false);
	public static final CsvStringOption SESSION_HOST_OPTION = new CsvStringOption(SESSION_HOST_SHORT, SESSION_HOST_LONG, "sessionHost", SESSION_HOST_DESC, true, true, false);
	public static final CsvStringOption VENUE_OPTION = new CsvStringOption(VENUE_SHORT, VENUE_LONG, "venue", VENUE_DESC, true, true, false);
	public static final CsvUnsignedIntOption VCID_OPTION = new CsvUnsignedIntOption(VCID_SHORT, VCID_LONG, "vcid", VCID_DESC, true, true, false);
	public static final IntegerOption SCID_OPTION = new IntegerOption(SCID_SHORT, SCID_LONG, "scid", SCID_DESC, false);
	public static final CsvUnsignedIntOption DSSID_OPTION = new CsvUnsignedIntOption(DSSID_SHORT, DSSID_LONG, "dssId", DSSID_DESC, true, true, false);
	public static final IntegerOption MAX_RESULTS_OPTION = new IntegerOption(MAX_RESULTS_KEY_SHORT, MAX_RESULTS_KEY_LONG, "maximumResults", MAX_RESULTS_DESC, false);

	/**
	 * Time strings are taken at face value.  The GLAD will check the requested time type and verify if the input time value is correct.
	 */
	public static final StringOption BEGIN_TIME_OPTION = new StringOption(BEGIN_TIME_SHORT, BEGIN_TIME_LONG, "startTime", BEGIN_DESC, false);
	public static final StringOption END_TIME_OPTION = new StringOption(END_TIME_SHORT, END_TIME_LONG, "endTime", END_DESC, false);

	public static final EnumOption<DataSource> DATA_SOURCE_OPTION = new EnumOption<DataSource>(DataSource.class,
			SOURCE_SHORT, SOURCE_LONG, "dataSource", SOURCE_DESC, false);

	public static final EnumOption<RecordedState> RECORDED_STATE_OPTION = new EnumOption<RecordedState>(RecordedState.class,
			REALTIME_SHORT_OPTION, REALTIME_LONG_OPTION, "recordedState", REALTIME_DESCRIPTION, false);

	public static final EnumOption<GlobalLadPrimaryTime> TIME_TYPE_OPTION = new EnumOption<GlobalLadPrimaryTime>(GlobalLadPrimaryTime.class,
					TIMESTAMP_TYPE_SHORT_OPTION, TIMESTAMP_TYPE_LONG_OPTION, "timestampType", TIMESTAMP_TYPE_DESCRIPTION, false);

	public static final FlagOption SHOW_COLUMN_OPTION = new FlagOption(SHOW_COLUMNS_SHORT, SHOW_COLUMNS_LONG, SHOW_COLUMNS_DESC, false);
	public static final FlagOption VERIFIED_QUERY_OPTION = new FlagOption(null, VERIFY_LONG, VERIFY_DESC, false);

    public static final EnumOption<QueryOutputFormat> OUTPUT_FORMAT_OPTION = new EnumOption<QueryOutputFormat>(QueryOutputFormat.class,
    		OUTPUT_FORMAT_SHORT, OUTPUT_FORMAT_LONG, "outputFormat", 	OUTPUT_FORMAT_DESC, false);

    /**
     * Channel value specific options.
     */
    public static final ChannelOption CHANNEL_VALUE_OPTION = new ChannelOption(CHANNEL_IDS_SHORT_OPTION, CHANNEL_IDS_LONG_OPTION, "channelValues", CHANNEL_IDS_DESCRIPTION, COMMAND_LINE_WILDCARD_SYMBOL, REGEX_WILDCARD_SYMBOL, false);

    /**
     * EVR specific options.
     */
    public static final CsvStringOption EVR_NAME_OPTION = new CsvStringOption(EVR_NAME_SHORT, EVR_NAME_LONG, "evrName", EVR_NAME_DESC, true, true, false);
    public static final CsvStringOption EVR_LEVEL_OPTION = new CsvStringOption(EVR_LEVEL_SHORT, EVR_LEVEL_LONG, "evrLevel", EVR_LEVEL_DESC, true, true, false);
    public static final CsvStringOption EVR_MSG_OPTION = new CsvStringOption(null, MESSAGE_REGEXX_LONG, "evrMessageRegex", MSG_REGES, true, true, false);



    public GlobalLadOptions(final ICommandLineApp app) {
		super(app);
        addHelpOption();
        addVersionOption();
	}
}
