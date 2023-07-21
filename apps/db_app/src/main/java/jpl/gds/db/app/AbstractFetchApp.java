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

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.velocity.Template;

import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.common.config.CsvQueryProperties;
import jpl.gds.common.config.CsvQueryPropertiesException;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.io.ServerSocketException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.types.IDbAccessItem;
import jpl.gds.db.api.types.IDbContextInfoFactory;
import jpl.gds.db.api.types.IDbContextInfoUpdater;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.shared.channel.ChannelIdUtility;
import jpl.gds.shared.cli.CliUtility;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.FlushBool;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.template.DatabaseTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.Pair;

/**
 * AbstractFetchApp is the base class for all command line applications that
 * query the database.
 *
 */
public abstract class AbstractFetchApp extends AbstractDatabaseApp
{
	private static final String DESC_1_STRING  = ". Individual entries may be specified using an SQL LIKE pattern with "
			+ "wildcards like % and _. ";
	private static final String DESC_2_STRING  = " Multiple values may be supplied in a comma-separated value (CSV) "
			+ "format.";
	private static final String DESC_STRING    = DESC_1_STRING + DESC_2_STRING;

	/** Pattern string */
	static final String PATTERN_STRING = "Pattern";


	private static final String REQUIRES_A = " requires a ";

	/**
	 * The default size of result batches to return. (Too many places to change to
	 * make PMD happy.)
	 */
	@SuppressWarnings("PMD.VariableNamingConventions")
	protected static final int defaultBatchSize = 500;

	/** Comma, used to split on */
	protected static final String COMMA = ",";

	/**
	 * An object encompassing all the test session related information that will be
	 * used during a query to join other tables with the test session table
	 */
	protected IDbSessionInfoUpdater dbSessionInfo;

	/**
	 * An object encompassing all the test context related information that will be
	 * used during a query to join other tables with the context table
	 */
	protected IDbContextInfoUpdater dbContextInfo;

	/**
	 * The string input specifying what database field query results should be
	 * ordered by
	 */
	protected String orderByString = null;

	/**
	 * The string input specifying what time type should be used
	 */
	private DatabaseTimeType forcedTimeType = null;

	/** The currently selected Velocity template for output formatting */
	protected Template template;

	/** Indicates whether to include column names with CSV output. */
	protected boolean showColHeaders;

	/** Indicates whether to simply print the SQL statement rather than execute */
	protected boolean sqlStmtOnly;

	/**
	 * True if we only want SSE data, false if we only want FSW data, or null if we
	 * want both
	 */
	protected Boolean fromSse;

	/** Optiom command-line object */
	protected CommandLine cmdline;

	/** set true if there must be at least one "required" option */
	protected boolean checkRequiredOptions = true;

	/** Flag indicating the app should report progress info to the console */
	protected boolean reportRows;

	/** Indicates whether to use Context or Session for join queries
	  */
	protected boolean useContext = false;



	/** The output filename for queried data */
	private String outputFilename = null;



	/**
	 * The time range to use when querying (also includes time type)
	 */
	protected DatabaseTimeRange times;

	/** Begin-time as a string */
	protected String beginTimeString;

	/** End-time as a string */
	protected String endTimeString;

	/** Global context for templates */
	protected Map<String, Object> globalContext = new HashMap<>(32);

	/** Configured CSV columns. Not final. */
	protected List<String> csvColumns = null;

	/** Configured CSV column headers. Not final. */
	protected List<String> csvHeaders = null;

	/**
	 * List of command line options this app must have
	 */
	protected List<String> requiredOptions;

	/**
	 * List of extra command line options this app must have
	 */
	protected final List<String> extraRequiredOptions = new ArrayList<>();

	/** Database fetch object */
	protected IDbSqlFetch fetch;

	/** True if application is shutting down */
	protected boolean shutdown;

	private String indexHints = "";

	protected int recordCount;

	/**
	 * Track start time(nano) for syslogging elapsed runtime
	 */
	private long nanoStartTime;

	/** Track start time (ms) for syslogging start date  */
	private long msStartTime;

	/*
	 * The following an alphabetized list of all the command line parameters that
	 * are used by all the subclasses of this class.
	 */

	/** Short output-directory option */
	public static final String OUTPUT_DIR_SHORT = "a";
	// ("a" is also a ReservedOption)

	/** Long output-directory option */
	public static final String OUTPUT_DIR_LONG = "outputDir";

	/** Short begin-time option */
	public static final String BEGIN_TIME_SHORT = "b";

	/** Long begin-time option */
	public static final String BEGIN_TIME_LONG = "beginTime";

	/** Short classification option */
	public static final String CLASSIFICATION_SHORT = "c";

	/** Long classification option */
	public static final String CLASSIFICATION_LONG = "classification";

	/** Short command-string option */
	public static final String CMD_STRING_SHORT = "c";

	/** Long command-string option */
	public static final String CMD_STRING_LONG = "commandStringPattern";

	/** Long CSV option */
	public static final String CSV_LONG = "csv";

	/** Short context ID option */
	public static final String CONTEXT_ID_SHORT = "C";

	/** Long context ID option */
	public static final String CONTEXT_ID_LONG = "contextId";

	/** Short changes-only option */
	public static final String CHANGE_VALUES_SHORT = "I";

	/** Long changes-only option */
	public static final String CHANGE_VALUES_LONG = "changesOnly";

	/** Long do-ERT option */
	public static final String DO_ERT_LONG = "doErt"; // May want to replace this with -y

	/** Long do-SCET option */
	public static final String DO_SCET_LONG = "doScet";

	/** Short end-time option */
	public static final String END_TIME_SHORT = "e";

	/** Long end-time option */
	public static final String END_TIME_LONG = "endTime";

	/** Long Excel option */
	public static final String EXCEL_LONG = "excel";

	/** Short filename option */
	public static final String FILE_SHORT = "f";

	/** Long filename option */
	public static final String FILE_LONG = "filename";

	/** Long file-1553 option */
	public static final String FILE_1553_LONG = "file1553";

	/** Short product-partial-only option */
	public static final String PRODUCT_PARTIAL_SHORT = "f";

	/** Long product-partial-only option */
	public static final String PRODUCT_PARTIAL_LONG = "partialOnly";

	/** Short product-complete-only option */
	public static final String PRODUCT_COMPLETE_SHORT = "g";

	/** Long product-complete-only option */
	public static final String PRODUCT_COMPLETE_LONG = "completeOnly";

	/** Short bad-only option */
	public static final String BAD_ONLY_SHORT = "G";

	/** Long bad-only option */
	public static final String BAD_ONLY_LONG = "badOnly";

	/** Short alarm-only option */
	public static final String ALARM_ONLY_SHORT = "G";

	/** Long alarm-only option */
	public static final String ALARM_ONLY_LONG = "alarmOnly";

	/** Short log-type option */
	public static final String LOG_TYPE_SHORT = "G";

	/** Long log-type option */
	public static final String LOG_TYPE_LONG = "logType";

	/** Short SPSC option */
	public static final String PACKET_SPSC_SHORT = "G";

	/** Long SPSC option */
	public static final String PACKET_SPSC_LONG = "spsc";

	// h is reserved for help

	/** Short good-only option */
	public static final String GOOD_ONLY_SHORT = "H";

	/** Long good-only option */
	public static final String GOOD_ONLY_LONG = "goodOnly";

	/** Short event-id option */
	public static final String EVENT_ID_SHORT = "i";

	/** Long event-id option */
	public static final String EVENT_ID_LONG = "eventId";

	/** Short level option */
	public static final String LEVEL_SHORT = "l";

	/** Long level option */
	public static final String LEVEL_LONG = "level";

	/** Short show-columns option */
	public static final String SHOW_COLUMNS_SHORT = "m";

	/** Long show-columns option */
	public static final String SHOW_COLUMNS_LONG = "showColumns";

	/** Show-columns description */
	public static final String SHOW_COLUMNS_DESC = "Include column names in CSV output";

	/** Long no-EHA option */
	public static final String NO_EHA_LONG = "noEha";

	/** Long no-EVR option */
	public static final String NO_EVR_LONG = "noEvr";

	/** Long no-cmd option */
	public static final String NO_CMD_LONG = "noCmd";

	/** Long no-prod option */
	public static final String NO_PROD_LONG = "noProd";

	/** Long no-log option */
	public static final String NO_LOG_LONG = "noLog";

	/** Short output format option */
	public static final String OUTPUT_FORMAT_SHORT = "o";

	/** Long output format option */
	public static final String OUTPUT_FORMAT_LONG = "outputFormat";

	/** Output format description */
	public static final String OUTPUT_FORMAT_DESC = "The formatting style for the query output with a Velocity template.";

	/** Short packet-APID option */
	public static final String PACKET_APID_SHORT = "p";

	/** Long packet-APID option */
	public static final String PACKET_APID_LONG = "packetApid";

	/** Short product-APID option */
	public static final String PRODUCT_APID_SHORT = "p";

	/** Long product-APID option */
	public static final String PRODUCT_APID_LONG = "productApid";

	/** Short channel-id file option */
	public static final String CHANNEL_ID_FILE_SHORT = "p";

	/** Long channel-id file option */
	public static final String CHANNEL_ID_FILE_LONG = "channelIdFile";

	/** Short add-no-asm option */
	public static final String ADD_NO_ASM_SHORT = "p";

	/** Long add-no-asm option */
	public static final String ADD_NO_ASM_LONG = "addNoAsm";

	/** Short report option */
	public static final String REPORT_SHORT = "r";

	/** Long report option */
	public static final String REPORT_LONG = "report";

	/** Short request-id option */
	public static final String REQUEST_ID_SHORT = "r";

	/** Long request-id option */
	public static final String REQUEST_ID_LONG = "productRequestId";

	/** Long RT option */
	public static final String RT_LONG = "RTs";

	/** Long SA option */
	public static final String SA_LONG = "SAs";

	/** Long SCLK start option */
	public static final String SCLK_START_LONG = "sclkStart";

	/** Long SCLK end option */
	public static final String SCLK_END_LONG = "sclkEnd";

	/** Long SQL-statement-only option */
	public static final String SQL_STATEMENT_ONLY_LONG = "sqlStatementOnly";

	/** Long sys-time start option */
	public static final String SYS_TIME_START_LONG = "sysTimeStart";

	/** Long sys-time end option */
	public static final String SYS_TIME_END_LONG = "sysTimeEnd";

	/** Short time type option */
	public static final String TIME_TYPE_SHORT = "t";

	/** Long time type option */
	public static final String TIME_TYPE_LONG = "timeType";

	/** Time type argument */
	public static final String TIME_TYPE_ARG = "timeType";

	/** Short frame type option */
	public static final String FRAME_TYPE_SHORT = "u";

	/** Long frame type option */
	public static final String FRAME_TYPE_LONG = "frameType";

	/** Short command type option */
	public static final String CMD_TYPE_SHORT = "u";

	/** Long command type option */
	public static final String CMD_TYPE_LONG = "commandType";

	/** Long status option */
	public static final String STATUS_TYPE_LONG = "statusType";

	/** Long request-id option */
	public static final String CMD_REQUEST_ID_LONG = "requestId";

	/** Short module option */
	public static final String MODULE_SHORT = "u";

	/** Long module option */
	public static final String MODULE_LONG = "modulePattern";

	// v is reserved for version

	/** Short from-test-start option */
	public static final String TEST_START_LOWER_SHORT = "w";

	/** Long from-test-start option */
	public static final String TEST_START_LOWER_LONG = "fromTestStart";

	/** Short to-test-start option */
	public static final String TEST_START_UPPER_SHORT = "x";

	/** Long to-test-start option */
	public static final String TEST_START_UPPER_LONG = "toTestStart";

	/** Short VCFCS option */
	public static final String VCFCS_SHORT = "X";

	/** Long VCFCS option */
	public static final String VCFCS_LONG = "vcfcs";

	/** Short order-by option */
	public static final String ORDER_BY_SHORT = "y";

	/** Long order-by option */
	public static final String ORDER_BY_LONG = "orderBy";

	/** Short relay S/C id option */
	public static final String RELAY_SCID_SHORT = "z";

	/** Long relay S/C id option */
	public static final String RELAY_SCID_LONG = "relayScid";

	/** Short channel ids option */
	public static final String CHANNEL_IDS_SHORT = "z";

	/** Long channel ids option */
	public static final String CHANNEL_IDS_LONG = "channelIds";

	/** Short EVR name option */
	public static final String EVR_NAME_SHORT = "z";

	/** Long EVR name option */
	public static final String EVR_NAME_LONG = "namePattern";

	private static final String CHANNEL_FILE_COMMENT_PREFIX = "#";

	/** Long option for channel types */
	public static final String CHANNEL_TYPES_LONG = "channelTypes";

	/** Long option for index hinting  */
	public static final String USE_INDEX_LONG = "useIndex";

	/** Long option for index hinting  */
	public static final String FORCE_INDEX_LONG = "forceIndex";

	/** Long option for index hinting  */
	public static final String IGNORE_INDEX_LONG = "ignoreIndex";

	/** Long option for restoring headers and trailers to bodies */
	public static final String RESTORE_LONG = "restoreBodies";

	/** Short option for session fragment */
	public static final String FRAGMENT_SHORT = "F";

	/** Long option for session fragment */
	public static final String FRAGMENT_LONG = "sessionFragment";

	/** Session fragment description */
	public static final String FRAGMENT_DESC = "Session fragment to filter for";

	/** Long option for show all session fragments */
	public static final String SHOW_FRAGMENTS_LONG = "showAllFragments";

	/** Show all fragments description */
	public static final String SHOW_FRAGMENTS_DESC = "Whether to show all session fragments";

	/** Creates SCLK time objects from supplied valid arguments */
	public final SclkFormatter sclkFmt;

	/* Order-by object factory.
	 * moved here from the superclass.
	 */
	protected IOrderByTypeFactory orderByTypeFactory;


	/** Long value for the maximum number of tenths of a microsecond in a millisecond */
	protected static final long     maxMicroTenths              = 9999;

	protected final SseContextFlag  sseFlag;

	protected DataOutputStream dos = null;
	protected PrintWriter pw = null;

	/** keep track of whether or not the header has been written out*/
	protected boolean headerWritten = false;

	protected List<? extends IDbAccessItem> out = null;

	/**
	 * Creates an instance of IDbFetchApp. If there are problems retrieving
	 * the CSV lists associated with the provided CSV application type, a fatal
	 * message will be displayed to the user and System.exit(1) will be called to
	 * immediately end the application.
	 *
	 * @param tableName
	 *            Indicates the name of the database table that this application
	 *            will primarily access
	 * @param appName
	 *            Application name
	 * @param app
	 *            CSV application type
	 */
	public AbstractFetchApp(final String tableName, final String appName, final String app) {
		super(tableName, appName);

		this.orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);
		this.sseFlag = appContext.getBean(SseContextFlag.class);

		sclkFmt = TimeProperties.getInstance().getSclkFormatter();


		try {

			final Pair<List<String>, List<String>> lists = CsvQueryProperties.instance().getCsvLists(app);

			csvColumns = lists.getOne();
			csvHeaders = lists.getTwo();
		}
		catch (final CsvQueryPropertiesException e) {
			trace.error(e.getMessage());
			System.exit(1);
		}

		init();
	}

	/**
	 * Overridden to perform application-specific
	 * startup activities.
	 *
	 * @throws ServerSocketException
	 *             If problem starting
	 */
	protected void specificStartup() throws ServerSocketException {
		SystemUtilities.doNothing();
	}

	/**
	 * Overridden to perform application-specific
	 * shutdown activities.
	 */
	protected void specificShutdown() {
		SystemUtilities.doNothing();
	}

	/**
	 * Overridden to supply output stream if any.
	 *
	 * @return Output stream
	 */
	protected OutputStream getOverridingOutputStream() {

		return null;
	}

	/**
	 * Overridden to supply print writer from command-line if any.
	 *
	 * @param autoFlush
	 *            True if we want to auto-flush
	 *
	 * @return Print writer
	 */
	protected PrintWriter getOverridingPrintWriter(final FlushBool autoFlush) {

		return null;
	}

	/**
	 * Shutdown, abort any active query, and close application.
	 */
	@Override
	public void exitCleanly() {
		trace.debug(appName, " received a shutdown request.  Shutting down gracefully...");
		try {
			shutdown = true;
			if (fetch != null) {
				fetch.abortQuery();
				fetch.close();
			}
		} catch (final Exception e) {
			// don't care - can't do anything at this point anyway
		}

		super.exitCleanly();

	}

	/**
	 * Initialize member variables of this class
	 */
	private void init() {
		dbSessionInfo = appContext.getBean(IDbSessionInfoFactory.class).createQueryableUpdater();
		dbContextInfo = appContext.getBean(IDbContextInfoFactory.class).createQueryableUpdater();
		orderByString = null;
		forcedTimeType = null;
		template = null;
		fromSse = null;
		quiet = false;
		showColHeaders = false;
		sqlStmtOnly = false;
		template = null;
		times = null;
		reportRows = true;
		outputFilename = null;

		nanoStartTime = System.nanoTime();
		msStartTime = System.currentTimeMillis();
	}

	/**
	 * Set check-required-options state.
	 *
	 * @param enable
	 *            Check-required-options state.
	 */
	public void setCheckRequiredOptions(final boolean enable) {
		checkRequiredOptions = enable;
	}

	/**
	 * Get time range.
	 *
	 * @return Time range
	 */
	public DatabaseTimeRange getTimes() {
		return times;
	}

	/**
	 * Get database session info.
	 *
	 * @return Test info object
	 */
	public IDbSessionInfoProvider getTestInfo() {
		return dbSessionInfo;
	}

	/**
	 * Get order-by string.
	 *
	 * @return Order-by string.
	 */
	public String getOrderByString() {
		return orderByString;
	}

	/**
	 * Get is-show-column-headers state.
	 *
	 * @return Is-show-column-headers state.
	 */
	public boolean isShowColHeaders() {
		return showColHeaders;
	}

	/**
	 * Get is-sql_stmt-only state.
	 *
	 * @return Is-sql_stmt-only state.
	 */
	public boolean isSqlStmtOnly() {
		return sqlStmtOnly;
	}

	/**
	 * Retrieves the currently defined Velocity template for formatting output.
	 *
	 * @return the Template object, or null if none selected/defined
	 */
	protected Template getTemplate() {
		return (template);
	}

	/**
	 * Get output file name.
	 *
	 * @return Returns the outputFilename.
	 */
	public String getOutputFilename() {
		return outputFilename;
	}


	/**
	 * Set output file name.
	 *
	 * @param target
	 *            Path to file
	 *
	 * @throws FileNotFoundException
	 *             If file cannot be created
	 */
	protected void setOutputFilename(final String target) throws FileNotFoundException {
		outputFilename = target;

		// See if it can be created

		OutputStream os = null;

		try {
			os = new FileOutputStream(target);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (final IOException ioe) {
					SystemUtilities.doNothing();
				}
			}
		}
	}


	/**
	 * Get report-rows state.
	 *
	 * @return Returns the reportRows.
	 */
	public boolean isReportRows() {
		return reportRows;
	}

	/**
	 * Get begin-time as string.
	 *
	 * @return Begin-time
	 */
	public String getBeginTimeString() {
		return beginTimeString;
	}

	/**
	 * Get end-time as string.
	 *
	 * @return End-time
	 */
	public String getEndTimeString() {
		return endTimeString;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void showHelp() {
		System.out.println(getUsage());
		final PrintWriter pw = new PrintWriter(System.out);
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printOptions(pw, 80, options, 7, 2);
		pw.flush();
		System.out.println("\nMultiple query parameters will be ANDed together. Query values");
		System.out.println("are NOT case sensitive. All time values except SCLK should use");
		System.out.println(
				"the format YYYY-MM-DDThh:mm:ss.ttt or  YYYY-DOYThh:mm:ss.ttt. Timezone for all times is GMT.");
		System.out.println("All string parameters whose long option name contains the word");
		System.out.println("\"Pattern\" may be entered using SQL pattern matching syntax such as");
		System.out.println(
				"-" + ReservedOptions.TESTNAME_SHORT_VALUE + " %MyTestName%, which would find all sessions with names");
		System.out.println("that contain the string \"MyTestName\"");
	}

	/**
	 * Populates the requiredOptions member variable with the required option name
	 * strings. Adds the long option name by default unless there is none, otherwise
	 * it adds the short option name.
	 *
	 * @throws ParseException
	 *             Parse error
	 */
	public void createRequiredOptions() throws ParseException {
		requiredOptions = new ArrayList<>();
		requiredOptions.add(ReservedOptions.FSWVERSION_LONG_VALUE + PATTERN_STRING);
		requiredOptions.add(ReservedOptions.DOWNLINKSTREAM_LONG_VALUE);
		requiredOptions.add(ReservedOptions.TESTKEY_LONG_VALUE);
		requiredOptions.add(ReservedOptions.TESTDESCRIPTION_LONG_VALUE + PATTERN_STRING);
		requiredOptions.add(ReservedOptions.TESTNAME_LONG_VALUE + PATTERN_STRING);
		requiredOptions.add(ReservedOptions.TESTHOST_LONG_VALUE + PATTERN_STRING);
		requiredOptions.add(ReservedOptions.TESTUSER_LONG_VALUE + PATTERN_STRING);
		requiredOptions.add(ReservedOptions.TESTTYPE_LONG_VALUE + PATTERN_STRING);
		requiredOptions.add(ReservedOptions.SSEVERSION_LONG_VALUE + PATTERN_STRING);
		requiredOptions.add(TEST_START_LOWER_LONG);
		requiredOptions.add(TEST_START_UPPER_LONG);

		// chill_get_everything needs some extra ones

		for (final String extra : extraRequiredOptions) {
			if (!requiredOptions.contains(extra)) {
				requiredOptions.add(extra);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void configureApp(final CommandLine cmdline) throws ParseException {
		if (cmdline == null) {
			throw new IllegalArgumentException("Null input command line");
		}

		this.cmdline = cmdline;

		if (checkRequiredOptions) {
			requiredOptionsCheck(cmdline);
		}

		setTemplate(cmdline.getOptionValue(OUTPUT_FORMAT_SHORT, null));

		// Read in the unique test key

		if (cmdline.hasOption(ReservedOptions.TESTKEY_SHORT_VALUE)) {
			final String testKeyStr = cmdline.getOptionValue(ReservedOptions.TESTKEY_SHORT_VALUE);

			if (testKeyStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.TESTKEY_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list or range of numeric test keys as an argument");
			}

			for (final String testKey : testKeyStr.split(COMMA)) {
				for (final String next : CliUtility.expandRange(testKey.trim())) {
					try {
						dbSessionInfo.addSessionKey(Long.valueOf(next.trim()));
					} catch (final NumberFormatException e1) {
						throw new ParseException("Value of -" + ReservedOptions.TESTKEY_SHORT_VALUE
								+ " option must be a " + "list of comma-separated integer values, but the value '"
								+ next + "' is invalid.");
					}
				}
			}
		}

		//read the context ID
		if (cmdline.hasOption(CONTEXT_ID_SHORT)) {
			final String testKeyStr = cmdline.getOptionValue(CONTEXT_ID_SHORT);

			if (testKeyStr == null) {
				throw new MissingArgumentException("-" + CONTEXT_ID_SHORT + REQUIRES_A
						+ "comma-separated list or range of numeric context IDs "
						+ "as an argument");
			}

			for (final String testKey : testKeyStr.split(COMMA)) {
				for (final String next : CliUtility.expandRange(testKey.trim())) {
					try {
						dbContextInfo.addSessionKey(Long.valueOf(next.trim()));
						useContext = true;
					} catch (final NumberFormatException e1) {
						throw new ParseException("Value of -" + CONTEXT_ID_SHORT
								+ " option must be a " + "list of comma-separated integer values, but the value '"
								+ next + "' is invalid.");
					}
				}
			}
		}

		// read in the test host
		if (cmdline.hasOption(ReservedOptions.TESTHOST_SHORT_VALUE)) {
			final String hostStr = cmdline.getOptionValue(ReservedOptions.TESTHOST_SHORT_VALUE);
			if (hostStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.TESTHOST_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of hostname patterns as an argument");
			}

			final String[] hostStrings = hostStr.split(COMMA);
			for(String host: hostStrings){
				dbSessionInfo.addHostPattern(host.trim());
				dbContextInfo.addHostPattern(host.trim());
			}
		}

		// read in the test user
		if (cmdline.hasOption(ReservedOptions.TESTUSER_SHORT_VALUE)) {
			final String userStr = cmdline.getOptionValue(ReservedOptions.TESTUSER_SHORT_VALUE);
			if (userStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.TESTUSER_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of user patterns as an argument");
			}

			final String[] userStrings = userStr.split(COMMA);
			for(String user: userStrings){
				dbSessionInfo.addUserPattern(user.trim());
				dbContextInfo.addUserPattern(user.trim());
			}
		}

		// read in the test name
		if (cmdline.hasOption(ReservedOptions.TESTNAME_SHORT_VALUE)) {
			final String nameStr = cmdline.getOptionValue(ReservedOptions.TESTNAME_SHORT_VALUE);
			if (nameStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.TESTNAME_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of name patterns as an argument");
			}

			final String[] nameStrings = nameStr.split(COMMA);
			for(String name: nameStrings){
				dbSessionInfo.addNamePattern(name.trim());
				dbContextInfo.addNamePattern(name.trim());
			}
		}

		// read in the test description
		if (cmdline.hasOption(ReservedOptions.TESTDESCRIPTION_SHORT_VALUE)) {
			final String descStr = cmdline.getOptionValue(ReservedOptions.TESTDESCRIPTION_SHORT_VALUE);
			if (descStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.TESTDESCRIPTION_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of description patterns as an argument");
			}

			final String[] descStrings = descStr.split(COMMA);
			for(String desc: descStrings){
				dbSessionInfo.addDescriptionPattern(desc.trim());
			}
		}

		// read in the test sse version
		if (cmdline.hasOption(ReservedOptions.SSEVERSION_SHORT_VALUE)) {
			final String sseStr = cmdline.getOptionValue(ReservedOptions.SSEVERSION_SHORT_VALUE);
			if (sseStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.SSEVERSION_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of SSE versions as an argument");
			}

			final String[] sseStrings = sseStr.split(COMMA);
			for(String sse: sseStrings){
				dbSessionInfo.addSseVersionPattern(sse.trim());
			}
		}

		// read in the test fsw version
		if (cmdline.hasOption(ReservedOptions.FSWVERSION_SHORT_VALUE)) {
			final String fswStr = cmdline.getOptionValue(ReservedOptions.FSWVERSION_SHORT_VALUE);
			if (fswStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.FSWVERSION_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of FSW versions as an argument");
			}

			final String[] fswStrings = fswStr.split(COMMA);
			for(String fsw: fswStrings){
				dbSessionInfo.addFswVersionPattern(fsw.trim());
			}
		}

		// read in the lower bound on start of test time
		if (cmdline.hasOption(TEST_START_LOWER_SHORT)) {
			final String beginTestTimeStr = cmdline.getOptionValue(TEST_START_LOWER_SHORT);
			if (beginTestTimeStr == null) {
				throw new MissingArgumentException("-" + TEST_START_LOWER_SHORT + " requires a time argument");
			}

			try {
				final IAccurateDateTime fromTestTime = new AccurateDateTime(beginTestTimeStr.trim());
				dbSessionInfo.setStartTimeLowerBound(fromTestTime);
			} catch (final java.text.ParseException e) {
				throw new ParseException(
						"Test start time has invalid format; should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:ss.ttt");
			}
		}

		// read in the upper bound on start of test time
		if (cmdline.hasOption(TEST_START_UPPER_SHORT)) {
			final String endTestTimeStr = cmdline.getOptionValue(TEST_START_UPPER_SHORT);
			if (endTestTimeStr == null) {
				throw new MissingArgumentException("-" + TEST_START_UPPER_SHORT + " requires a time argument");
			}

			try {
				final IAccurateDateTime toTestTime = new AccurateDateTime(endTestTimeStr.trim());
				dbSessionInfo.setStartTimeUpperBound(toTestTime);
			} catch (final java.text.ParseException e) {
				throw new ParseException(
						"Time start time has invalid format; should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:ss.ttt");
			}
		}

		// parse and store the time type (if it exists) or default it to ERT

		if (cmdline.hasOption(TIME_TYPE_SHORT)) {
			final String timeTypeStr = cmdline.getOptionValue(TIME_TYPE_SHORT);
			if (timeTypeStr == null) {
				throw new MissingArgumentException("The option -" + TIME_TYPE_SHORT + " requires a value");
			}

			DatabaseTimeType timeType = null;
			try {
				timeType = new DatabaseTimeType(timeTypeStr.trim());
			} catch (final IllegalArgumentException iae) {
				throw new ParseException("Illegal time type value: \"" + timeTypeStr
						+ "\".  Allowable values are: SCET, ERT, RCT, SCLK, CREATION_TIME, LST");
			}

			times = new DatabaseTimeRange(timeType);
			checkTimeType(times);
		} else if (forcedTimeType != null) {
			times = new DatabaseTimeRange(forcedTimeType);
		} else {
			final DatabaseTimeType timeType = getDefaultTimeType();
			times = timeType != null ? new DatabaseTimeRange(getDefaultTimeType()) : null;
		}

		if (cmdline.hasOption(BEGIN_TIME_SHORT)) {
			beginTimeString = cmdline.getOptionValue(BEGIN_TIME_SHORT);
			if (beginTimeString == null) {
				throw new MissingOptionException("The option -" + BEGIN_TIME_SHORT + " requires a value");
			}
			beginTimeString = beginTimeString.trim();
		}

		if (cmdline.hasOption(END_TIME_SHORT)) {
			endTimeString = cmdline.getOptionValue(END_TIME_SHORT);
			if (endTimeString == null) {
				throw new MissingOptionException("The option -" + END_TIME_SHORT + " requires a value");
			}
			endTimeString = endTimeString.trim();
		}

		// finish filling out the time range
		if (times != null) {
			switch (times.getTimeType().getValueAsInt()) {
				case DatabaseTimeType.CREATION_TIME_TYPE:
				case DatabaseTimeType.EVENT_TIME_TYPE:
				case DatabaseTimeType.RCT_TYPE:

					try {
						if (beginTimeString != null) {
							times.setStartTime(new AccurateDateTime(beginTimeString));
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException(
								"Begin time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:ss.ttt");
					}

					try {
						if (endTimeString != null) {
							times.setStopTime(new AccurateDateTime(endTimeString));
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException(
								"End time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:ss.ttt");
					}

					break;

				case DatabaseTimeType.SCET_TYPE:

					try {
						if (beginTimeString != null) {
							times.setStartTime(new AccurateDateTime(beginTimeString));
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException(
								"Begin time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt[tttttt] or YYYY-DOYThh:mm:ss.ttt[tttttt]");
					}

					try {
						if (endTimeString != null) {
							// Updated to match ERT behavior, ie. include any records with sub-microsecond
							// SCET data in the results for a query with an end time cutoff of only microsecond precision
							final IAccurateDateTime endTime = new AccurateDateTime(endTimeString);
							times.setStopTime(new AccurateDateTime(endTime, maxMicroTenths, true));
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException(
								"End time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt[tttttt] or YYYY-DOYThh:mm:ss.ttt[tttttt]");
					}

					break;

				case DatabaseTimeType.ERT_TYPE:

					try {
						if (beginTimeString != null) {
							times.setStartTime(new AccurateDateTime(beginTimeString));
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException(
								"Begin time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt[tttt] or YYYY-DOYThh:mm:ss.ttt[tttt]");
					}

					try {
						if (endTimeString != null) {
							final IAccurateDateTime endTime = new AccurateDateTime(endTimeString);
							times.setStopTime(new AccurateDateTime(endTime, maxMicroTenths, true));
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException(
								"End time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt[tttt] or YYYY-DOYThh:mm:ss.ttt[tttt]");
					}

					break;

				case DatabaseTimeType.SCLK_TYPE:


					ISclk fromSclk = null;
					ISclk thruSclk = null;

					try {
						if (beginTimeString != null) {
							fromSclk = sclkFmt.valueOf(beginTimeString.trim());
							times.setStartSclk(fromSclk);
						}
					} catch (final IllegalArgumentException e1) {
						throw new ParseException("Begin time SCLK range has invalid format: " + e1.getMessage()
								+ constructExampleMessage(TimeProperties.getInstance().getSclkFormatter(), fromSclk));
					}

					try {
						if (endTimeString != null) {

							thruSclk = sclkFmt.valueOf(endTimeString.trim());
							times.setStopSclk(thruSclk);
						}
					} catch (final IllegalArgumentException e1) {
						throw new ParseException("End time SCLK range has invalid format: " + e1.getMessage()
								+ constructExampleMessage(TimeProperties.getInstance().getSclkFormatter(), thruSclk));
					}

					break;

				case DatabaseTimeType.LST_TYPE:

					try {
						if (beginTimeString != null) {
							final ILocalSolarTime fromLst = LocalSolarTimeFactory.getNewLst(beginTimeString.trim(),
									missionProps.getDefaultScid());
							times.setStartSclk(fromLst.toSclk());
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException(
								"Begin time LST range has invalid format. Should be SOL-NNNNMhh:mm:ss.ttt.");
					}

					try {
						if (endTimeString != null) {
							final ILocalSolarTime thruLst = LocalSolarTimeFactory.getNewLst(endTimeString.trim(),
									missionProps.getDefaultScid());
							times.setStopSclk(thruLst.toSclk());
						}
					} catch (final java.text.ParseException e1) {
						throw new ParseException("End time LST range has invalid format. Should be SOL-NNNNMhh:mm:ss.ttt");
					}

					break;

				default:

					throw new MissingOptionException("TimeType is not one of: SCET, ERT, RCT, SCLK, LST");
			}

			if (!times.isRangeNonEmpty()) {
				throw new ParseException("End time is before begin time");
			}
		}

		// read in the test downlink stream ID
		if (cmdline.hasOption(ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE)) {
			final String streamIdStr = cmdline.getOptionValue(ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE);
			if (streamIdStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of downlink stream IDs as an argument");
			}

			final String[] streamStrings = streamIdStr.split(COMMA);
			for(String stream: streamStrings){
				dbSessionInfo.addDownlinkStreamId(stream.trim());
			}
		}

		// read in the test type
		if (cmdline.hasOption(ReservedOptions.TESTTYPE_SHORT_VALUE)) {
			final String testTypeStr = cmdline.getOptionValue(ReservedOptions.TESTTYPE_SHORT_VALUE);
			if (testTypeStr == null) {
				throw new MissingArgumentException("-" + ReservedOptions.TESTTYPE_SHORT_VALUE + REQUIRES_A
						+ "comma-separated list of test type as an argument");
			}

			final String[] typeStrings = testTypeStr.split(COMMA);
			//add type info to both session and context since we need to be able to join with either
			for(String type: typeStrings){
				dbSessionInfo.addTypePattern(type.trim());
				dbContextInfo.addTypePattern(type.trim());
			}
		}

		// read in the order by field name
		if (cmdline.hasOption(ORDER_BY_SHORT)) {
			final String obString = cmdline.getOptionValue(ORDER_BY_SHORT);
			if (obString == null) {
				throw new MissingArgumentException(
						"-" + ORDER_BY_SHORT + " requires an argument specifying the value to order by");
			}
			orderByString = obString.trim();
		}

		ReservedOptions.parseDatabaseHost(cmdline, false);
		ReservedOptions.parseDatabasePort(cmdline, false);
		ReservedOptions.parseDatabaseUsername(cmdline, false);
		ReservedOptions.parseDatabasePassword(cmdline, false);

		showColHeaders = cmdline.hasOption(SHOW_COLUMNS_SHORT);
		sqlStmtOnly = cmdline.hasOption(SQL_STATEMENT_ONLY_LONG);
	}

	/**
	 * Create options common to both queries by session and context
	 * @param og Options object to be configured
	 */
	public static void createCommonOptions(final Options og){
		createHostPortUserPwdOptions(og);

		final Option nameOpt = ReservedOptions.getOption(ReservedOptions.TESTNAME_SHORT_VALUE);
		Option opt = new Option(nameOpt.getOpt(), nameOpt.getLongOpt() + PATTERN_STRING, nameOpt.hasArg(),
				nameOpt.getDescription() + DESC_STRING);
		opt.setArgName(ReservedOptions.TESTNAME_ARGNAME);
		og.addOption(opt);

		final Option hostOpt = ReservedOptions.getOption(ReservedOptions.TESTHOST_SHORT_VALUE);
		opt = new Option(hostOpt.getOpt(), hostOpt.getLongOpt() + PATTERN_STRING, hostOpt.hasArg(),
				hostOpt.getDescription() + DESC_STRING);
		opt.setArgName(ReservedOptions.TESTHOST_ARGNAME);
		og.addOption(opt);

		final Option userOpt = ReservedOptions.getOption(ReservedOptions.TESTUSER_SHORT_VALUE);
		opt = new Option(userOpt.getOpt(), userOpt.getLongOpt() + PATTERN_STRING, userOpt.hasArg(),
				userOpt.getDescription() + DESC_STRING);
		opt.setArgName(ReservedOptions.TESTUSER_ARGNAME);
		og.addOption(opt);

		final Option typeOpt = ReservedOptions.getOption(ReservedOptions.TESTTYPE_SHORT_VALUE);
		opt = new Option(typeOpt.getOpt(), typeOpt.getLongOpt() + PATTERN_STRING, typeOpt.hasArg(),
				typeOpt.getDescription() + DESC_STRING);
		opt.setArgName(ReservedOptions.TESTTYPE_ARGNAME);
		og.addOption(opt);

		final Option keyOpt = ReservedOptions.getOption(ReservedOptions.TESTKEY_SHORT_VALUE);
		opt = new Option(keyOpt.getOpt(), keyOpt.getLongOpt(), keyOpt.hasArg(),
				keyOpt.getDescription() + DESC_2_STRING + " (A range separated by \"..\" also accepted.)");
		opt.setArgName(ReservedOptions.TESTKEY_ARGNAME);
		og.addOption(opt);
	}


	/**
	 * Create test options.
	 *
	 * @param og
	 *            Options object to be configured
	 */
	public static void createTestOptions(final Options og) {
		// some of the reserved options are slightly edited in this section to make
		// their long argument names contain
		// the word "Pattern" to indicate that they can use SQL pattern matching syntax
		// because they are queried using
		// the keyword LIKE instead of a simple = (strangely enough we can edit the long
		// option name, but there's no
		// mutator for the description)

		final Option sseVersionOpt = ReservedOptions.getOption(ReservedOptions.SSEVERSION_SHORT_VALUE);
		Option opt = new Option(sseVersionOpt.getOpt(), sseVersionOpt.getLongOpt() + PATTERN_STRING,
				sseVersionOpt.hasArg(), sseVersionOpt.getDescription() + DESC_STRING);
		opt.setArgName(ReservedOptions.SSEVERSION_ARGNAME);
		og.addOption(opt);

		final Option fswVersionOpt = ReservedOptions.getOption(ReservedOptions.FSWVERSION_SHORT_VALUE);
		opt = new Option(fswVersionOpt.getOpt(), fswVersionOpt.getLongOpt() + PATTERN_STRING, fswVersionOpt.hasArg(),
				fswVersionOpt.getDescription() + DESC_STRING);
		opt.setArgName(ReservedOptions.FSWVERSION_ARGNAME);
		og.addOption(opt);

		final Option descOpt = ReservedOptions.getOption(ReservedOptions.TESTDESCRIPTION_SHORT_VALUE);
		opt = new Option(descOpt.getOpt(), descOpt.getLongOpt() + PATTERN_STRING, descOpt.hasArg(),
				descOpt.getDescription() + DESC_STRING);
		opt.setArgName(ReservedOptions.TESTDESCRIPTION_ARGNAME);
		og.addOption(opt);

		final Option streamOpt = ReservedOptions.getOption(ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE);
		opt = new Option(streamOpt.getOpt(), streamOpt.getLongOpt(), streamOpt.hasArg(),
				streamOpt.getDescription() + DESC_2_STRING);
		opt.setArgName(ReservedOptions.DOWNLINKSTREAM_ARGNAME);
		og.addOption(opt);

		og.addOption(ReservedOptions.createOption(TEST_START_LOWER_SHORT, TEST_START_LOWER_LONG, "time",
				"Lower bound on start time of test"));
		og.addOption(ReservedOptions.createOption(TEST_START_UPPER_SHORT, TEST_START_UPPER_LONG, "time",
				"Upper bound on start time of test"));
	}

	/**
	 * Create database host/port/user/password options.
	 *
	 * @param og
	 *            Options object to be configured
	 */
	public static void createHostPortUserPwdOptions(final Options og) {
		og.addOption(ReservedOptions.DATABASE_HOST);
		og.addOption(ReservedOptions.DATABASE_PORT);
		og.addOption(ReservedOptions.DATABASE_USERNAME);
		og.addOption(ReservedOptions.DATABASE_PASSWORD);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void addAppOptions() {
		createCommonOptions(options);
		createTestOptions(options);
		final Option sqlStmtOpt = ReservedOptions.createOption(SQL_STATEMENT_ONLY_LONG, null,
				"Instead of executing the database query, print the SQL statement (useful for debugging)");
		options.addOption(sqlStmtOpt);
	}

	/**
	 * Adds an orderBy option to the command line, including specific ordering
	 * criteria allowed by the subclass.
	 */
	protected void addOrderOption() {
		final StringBuilder orderByString = new StringBuilder();
		final String[] orderByValues = getOrderByValues();

		if (orderByValues.length > 0) {
			orderByString.append(orderByValues[0]);

			for (int i = 1; i < orderByValues.length; i++) {
				orderByString.append(", ").append(orderByValues[i]);
			}

			// --orderBy is not a valid option for chill_get_ctab tool
			if (!appName.equals("chill_get_ctab")) {
				addOption(ORDER_BY_SHORT, ORDER_BY_LONG, "orderValue",
						"The field that the output should be ordered by. " + "Allowable values are: " + orderByString);
			}
		}
	}

	/**
	 * Sets the velocity template for output formatting by locating the template
	 * identified by the given format string.
	 *
	 * @param formatString
	 *            the template/style name (Xml, Csv, etc)
	 */
	protected void setTemplate(final String formatString) {
		template = null;
		if (formatString != null) {
			final String fs = formatString.toLowerCase();
			try {
				if (tableName == null) {
					throw new TemplateException("Database name is not set in app");
				}

				final DatabaseTemplateManager templateManager = MissionConfiguredTemplateManagerFactory
						.getNewDatabaseTemplateManager(sseFlag);
				template = templateManager.getTemplateForStyle(tableName, fs);
			} catch (final TemplateException e) {
				trace.warn("Error retrieving style template: '" + fs + "'. Defaulting to no formatting: "
						+ rollUpMessages(e));
			}
		}
	}

	/**
	 * Sets the velocity template for output formatting by locating the template
	 * identified by the given format string and database table name.
	 *
	 * @param formatString
	 *            the template/style name (Xml, Csv, etc)
	 * @param dbTableName
	 *            the database table name of the objects to be formatted
	 */
	protected void setTemplate(final String formatString, final String dbTableName) {
		template = null;
		if (formatString != null) {
			final String fs = formatString.toLowerCase();
			try {
				final DatabaseTemplateManager templateManager = MissionConfiguredTemplateManagerFactory
						.getNewDatabaseTemplateManager(sseFlag);
				template = templateManager.getTemplateForStyle(dbTableName, fs);
			} catch (final TemplateException e) {
				trace.warn("Error retrieving style template: '" + fs + "'. Defaulting to no formatting: "
						+ rollUpMessages(e));
			}
		}
	}

	/**
	 * Returns an array of available template/style names (or empty if there are
	 * none)
	 *
	 * @return Style array
	 */
	protected String[] getTemplateStyles() {
		DatabaseTemplateManager templateManager = null;
		try {
			templateManager = MissionConfiguredTemplateManagerFactory.getNewDatabaseTemplateManager(sseFlag);
			if (tableName == null) {
				throw new TemplateException("Database Name is not set in application");
			}

			return (templateManager.getStyleNames(tableName));
		} catch (final TemplateException e) {
			trace.warn("Unable to determine available output formats\n");
		}

		return (new String[0]);
	}

	/**
	 * Displays valid formatting styles for the current database table name.
	 */
	public void printTemplateStyles() {
		final String[] styles = getTemplateStyles();
		if (styles.length == 0) {
			// OK to system.out this rather than trace; it's part of the help text
			System.out.println("\nA list of formatting styles in not currently available.");
			return;
		}

		System.out.print("\nAvailable formatting styles are:");
		for (int i = 0; i < styles.length; i++) {
			if (i % 4 == 0) {
				System.out.println();
				System.out.print("   ");
			}
			System.out.print(styles[i] + " ");
		}
		System.out.println();
		printTemplateDirectories();
	}

	/**
	 * Print out all searched template directories.
	 */
	public void printTemplateDirectories() {
		DatabaseTemplateManager templateManager = null;
		try {
			templateManager = MissionConfiguredTemplateManagerFactory.getNewDatabaseTemplateManager(sseFlag);
			if (tableName == null) {
				throw new TemplateException("Database Name is not set in application");
			}

			final List<String> directories = templateManager.getTemplateDirectories(tableName);

			System.out.println("\nTemplate directories searched are:");
			for (final String d : directories) {
				System.out.println("   " + d);
			}
		} catch (final TemplateException e) {
			trace.warn("Unable to determine template directories\n");
		}
	}

	/**
	 * Sets the orderByString, which specifies the ordering of database query
	 * results
	 *
	 * @param order
	 *            Order-by string value
	 */
	protected void setOrderByString(final String order) {

		orderByString = StringUtil.emptyAsNull(order);
	}

	/**
	 * Sets the forcedTimeType
	 *
	 * @param force
	 *            Forced time-type
	 */
	protected void setForcedTimeType(final DatabaseTimeType force) {
		forcedTimeType = force;
	}

	/**
	 * Writes header metadata to a metadata file using Velocity to format the output
	 */
	protected void addGlobalContext() {
		DateFormat df = null;
		try {
			df = TimeUtility.getFormatterFromPool();

			globalContext.put("productCreationTime", df.format(new AccurateDateTime()));
			globalContext.put("missionName", missionProps.getMissionLongName());
			globalContext.put("missionId", missionProps.getMissionId());
		} finally {
			if (df != null) {
				TimeUtility.releaseFormatterToPool(df);
			}
		}
	}

	/**
	 * Writes metadata for a given object to a metadata file using Velocity to
	 * format the output
	 *
	 * @param writer
	 *            the output writer object
	 * @param contextObject
	 *            the Templatable object to write
	 */
	protected void writeMetaData(final PrintWriter writer, final IDbQueryable contextObject) {
		if (template != null) {
			final SprintfFormat formatter = new SprintfFormat();
			final HashMap<String, Object> context = new HashMap<String, Object>();
			context.putAll(globalContext);
			context.put("body", true);
			context.put("formatter", formatter);
			contextObject.setTemplateContext(context);
			if (writer != null) {
				writer.write(TemplateManager.createText(template, context));
			}
		} else {
			writer.write(contextObject.toCsv(csvColumns));
		}
	}

	/**
	 * Perform extra writes to context as needed.
	 *
	 * @param context
	 *            Context to write to
	 */
	protected void writeHeaderMetaDataExtra(final Map<String, Object> context) {
		// Nothing to do unless overridden
	}

	/**
	 * Writes header metadata to a metadata file using Velocity to format the output
	 *
	 * @param writer
	 *            the output writer object
	 */
	protected void writeHeaderMetaData(final PrintWriter writer) {
		final HashMap<String, Object> context = new HashMap<String, Object>();

		context.putAll(globalContext);
		context.put("header", true);

		writeHeaderMetaDataExtra(context);

		context.put("vcidColumn", missionProps.getVcidColumnName());

		if (writer != null) {
			writer.write(TemplateManager.createText(template, context));
			writer.flush();
		}
	}

	/**
	 * Writes header metadata to a metadata file using Velocity to format the output
	 *
	 * @param writer
	 *            the output writer object
	 */
	protected void writeTrailerMetaData(final PrintWriter writer) {
		final HashMap<String, Object> context = new HashMap<String, Object>();
		context.put("trailer", true);
		if (writer != null) {
			writer.write(TemplateManager.createText(template, context));
			writer.flush();
		}
	}

	/**
	 * Writes a data blob to a DataOutputStream.
	 *
	 * @param dos
	 *            the DataOutputStream to write to
	 * @param data
	 *            the data to write
	 * @param length
	 *            the length of the data to write
	 *
	 * @return the length of data written
	 *
	 * @throws IOException
	 *             If there is an error writing out the meta data
	 */
	public int writeData(final DataOutputStream dos, final byte[] data, final int length) throws IOException {
		dos.write(data, 0, length);
		dos.flush();
		return (length);
	}

	/**
	 * Checks if the command line has the required options for this app.
	 *
	 * @param cmdline
	 *            Command line
	 * @throws MissingArgumentException
	 *             Missing argument
	 * @throws ParseException
	 *             Parse error
	 */
	public void requiredOptionsCheck(final CommandLine cmdline) throws ParseException {
		createRequiredOptions();

		if (!requiredOptions.isEmpty()) {
			final Option[] options = cmdline.getOptions();
			boolean hasRequiredOption = false;
			for (final Option op : options) {
				if (requiredOptions.contains(op.hasLongOpt() ? op.getLongOpt() : op.getOpt())) {
					hasRequiredOption = true;
					break;
				}
			}

			if (!hasRequiredOption) {
				throw new MissingArgumentException(
						"You have provided no search options to qualify your query.  Please provide"
								+ " at least one search option.");
			}

		}

	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "DM_EXIT" })
	@Override
	public CommandLine parseCommandLine(final String[] args) throws ParseException {
		options = createOptions();

		addAppOptions();
		CommandLine commandLine = null;

		try {
			@java.lang.SuppressWarnings("deprecation")
			final CommandLineParser parser = new PosixParser();
			commandLine = parser.parse(options, args);
		} catch (final MissingOptionException e) {
			boolean helpUsed = false;
			for (int i = 0; i < args.length; ++i) {
				if (args[i].equals(ReservedOptions.HELP_SHORT_VALUE)
						|| args[i].equals(ReservedOptions.HELP_LONG_VALUE)) {
					showHelp();
					helpUsed = true;
				}

				if (args[i].equals(ReservedOptions.VERSION_SHORT_VALUE)
						|| args[i].equals(ReservedOptions.VERSION_LONG_VALUE)) {
					showVersion();
					helpUsed = true;
				}
			}

			if (helpUsed) {
				System.exit(1);
			}
		}
		return commandLine;
	}

	/**
	 * Main run method.
	 *
	 */
	@java.lang.SuppressWarnings("unchecked")
	@Override
	public void run() {
		setExitCode(SUCCESS);

		addGlobalContext();

		recordCount = 0;

		// Make sure we connected to the database
		if (!setupFetch()) {
			setExitCode(OTHER_ERROR);
			return;
		}

		try {

			/** Start-up anything special */

			specificStartup();

			startupOutput();

			getInitialFetch();

			writeToLog(out.isEmpty());

			writeHeader();

			writeBody();

			writeTrailer();

			/** Shut down anything special */
			/**
			 * Shutdown call moved to after trailer data
			 * write.
			 */
			specificShutdown();

			trace.debug("Retrieved " + recordCount + " records.");

			/** Flush printwriter if shutdown flag was set */
			if (shutdown) {
				checkPrintWriter(pw);
			}

		} catch (final DatabaseException e) {
			if (debug) {
				e.printStackTrace();
			}

			if (shutdown) {
				setExitCode(OTHER_ERROR);
			} else {
				trace.error("SQL problem encountered while retrieving records: " + e.getMessage());
				setExitCode(OTHER_ERROR);
			}
		} catch (final Exception e) {
			if (debug) {
				e.printStackTrace();
			}
			final String message = e.getMessage() == null ? "" : ": " + e.getMessage();
			e.printStackTrace(System.out);
			trace.error("Problem encountered while retrieving records" + message);
			setExitCode(OTHER_ERROR);
		} finally {
			teardownFetch();

			shutdownOutput();
		}
	}

	/**
	 * Create the IDbSqlFetch object and report if it can communicate with the database
	 * @return TRUE if the fetch is connected to the database, FALSE if not
	 */
	protected boolean setupFetch() {
		fetch = getFetch(sqlStmtOnly);
		return fetch.isConnected();
	}

	/**
	 * Disconnect the IDbSqlFetch object from the database
	 */
	protected void teardownFetch() {
		fetch.close();
	}

	/**
	 * Initialize any objects that are used for outputting data. By default
	 * this is the DataOutputStream and PrintWriter (if specified)
	 * @throws FileNotFoundException if the file for the DataOutputStream could not be found
	 */
	protected void startupOutput() throws FileNotFoundException {

		if (reportRows) {
			pw = getOverridingPrintWriter(FlushBool.YES);

			if (pw == null) {
				pw = new PrintWriter(System.out, true);
			}
		}


		dos = determineDataOutputStream();
	}

	/**
	 * Safely shuts down any objects that were used for outputting data.
	 */
	protected void shutdownOutput() {
		if (pw != null) {
			pw.flush();
			pw.close();
		}
		if (dos != null) {
			try {
				dos.flush();
			} catch (final IOException e) {
				// don't care
			}

			try {
				dos.close();
			} catch (final IOException e) {
				// don't care
			}
		}
	}

	/**
	 * Retrieve the first batch of entries
	 * @throws DatabaseException if a database error occurs
	 */
	protected void getInitialFetch() throws DatabaseException {
		/**
		 * This cast may be dangerous. Not all IDbRecord
		 * subclasses implement the ICsvSupport interface. IDbAccessItem implements both
		 * IDbRecord and ICsvSuppor.
		 */

		out = (List<? extends IDbAccessItem>) fetch.get(
				useContext ? dbContextInfo: dbSessionInfo , times, defaultBatchSize, useContext,
				getFetchParameters());
	}

	/**
	 * Retrieve any batches of entries after the first
	 * @throws DatabaseException if a database error occurs
	 */
	protected void getNextFetch() throws DatabaseException {
		/**
		 * This cast may be dangerous. Not all IDbRecord
		 * subclasses implement the ICsvSupport interface. IDbAccessItem implements both
		 * IDbRecord and ICsvSuppor.
		 */
		out = (List<? extends IDbAccessItem>) fetch.getNextResultBatch();
	}

	/**
	 * Write the header of the CSV table or template to the output source(s)
	 * @throws IOException an error occurs while writing to one of the output objects
	 */
	protected void writeHeader() throws IOException {
		if(headerWritten){
			return;
		}

		if ((pw != null) && !out.isEmpty() && !shutdown) {
			if (template == null && showColHeaders) {

				pw.write(out.get(0).getCsvHeader(csvHeaders));

			} else if (template != null) {
				writeHeaderMetaData(pw);
				headerWritten = true;
			}
			checkPrintWriter(pw);
		}
	}

	/**
	 * Write the currently retrieved batch of entries to the output source(s)
	 * @throws IOException An errors occurs while writing to one of the output objects
	 * @throws DatabaseException if a database error occurs
	 */
	protected void writeBody() throws IOException, DatabaseException {

		long byteOffset = 0;

		while (!out.isEmpty() && !shutdown) {
			final ListIterator<? extends IDbRecord> iter = out.listIterator();
			while (iter.hasNext() && !shutdown) {
				final IDbQueryable dq = (IDbQueryable) iter.next();
				dq.setRecordOffset(byteOffset);
				if (pw != null) {
					writeMetaData(pw, dq);
				}

				if (dos != null) {
					final byte[] bytes = dq.getRecordBytes();

					byteOffset += writeData(dos, bytes, bytes.length);
				}

				recordCount++;
			}
			checkPrintWriter(pw);

			if (!shutdown) {
				getNextFetch();
			}
		}
	}

	/**
	 * Write the trailer of the template to the output source
	 * @throws IOException
	 */
	protected void writeTrailer() throws IOException {
		if (pw != null) {
			if (template != null && headerWritten) {
				writeTrailerMetaData(pw);
			}
			checkPrintWriter(pw);
		}
	}

	/**
	 * Write log statements to tracer
	 *
	 * @param noOutput
	 *            true if fetch returned nothing, false if fetch returned results
	 */
	protected void writeToLog(final boolean noOutput) {
		SystemUtilities.doNothing();
	}

	/**
	 * retrieves the channel ids from a file and returns them in an array of
	 * strings. This array is empty( not null ) if a problem was found.
	 *
	 * @param channelIdFile
	 *            -- the file containing the channel ids to be processed
	 * @return fileChannelIds -- the array of channel ids which were found in the
	 *         channel Id file. This array is concatenated with channelIds to form
	 *         the complete set of channelIds to fetch
	 *
	 * @throws ParseException
	 *             Parse error
	 */
	@SuppressWarnings("PMD.UseStringBufferForStringAppends")
	protected List<String> parseChannelIdFile(final String channelIdFile) throws ParseException {

		final List<String> channelIds = new ArrayList<String>();
		long line_counter = 0L;
		BufferedReader myReader = null;

		try {
			try {
				myReader = new BufferedReader(new FileReader(channelIdFile));
			} catch (final FileNotFoundException fnfe) {
				throw new ParseException("Could not find specified channel id file '" + channelIdFile + "'");
			}

			// Loop over the lines in the file

			String currentLine = null;

			while (true) {
				++line_counter;

				try {
					currentLine = myReader.readLine();
				} catch (final IOException ioe) {
					throw new ParseException("Failed to read line " + line_counter + " of channel id file '"
							+ channelIdFile + "': " + ioe.toString());
				}

				if (currentLine == null) {
					break;
				}

				currentLine = currentLine.trim();

				if (currentLine.length() == 0 || currentLine.startsWith(CHANNEL_FILE_COMMENT_PREFIX)) {
					continue;
				}

				final String[] currentIds = currentLine.split(",{1}");

				for (int i = 0; i < currentIds.length; ++i) {
					// Strip off junk at end of line
					if (currentIds[i].trim().indexOf('#') != -1) {
						currentIds[i] = currentIds[i].trim().substring(0, currentIds[i].indexOf('#'));
					}
					final String currentId = currentIds[i].trim().toUpperCase();

					if (!ChannelIdUtility.isChanIdString(currentId)) {
						throw new ParseException("Bad channel id " + currentId + " read from '" + channelIdFile + "'");
					}

					if (!channelIds.contains(currentId)) {
						channelIds.add(currentId);
					}
				}
			}
		} finally {
			if (myReader != null) {
				try {
					myReader.close();
				} catch (final IOException ioe) {
					// ignore
				}
			}
		}

		return channelIds;
	}

	/**
	 * Get appropriate database fetch for this application.
	 *
	 * @param sqlStmtOnly
	 *            True if SQL is only to be displayed, not executed
	 *
	 * @return Fetch object
	 */
	public abstract IDbSqlFetch getFetch(final boolean sqlStmtOnly);

	/**
	 * Get all fetch parameters for the application.
	 *
	 * @return Array of parameters, specific for application
	 */
	public abstract Object[] getFetchParameters();

	/**
	 * Validate time-type for this application.
	 *
	 * @param range
	 *            Database time-range
	 *
	 * @throws ParseException
	 *             Time-range is not valid for this application
	 */
	public abstract void checkTimeType(final DatabaseTimeRange range) throws ParseException;

	/**
	 * Get the default time-type for the application.
	 *
	 * @return Database time-type
	 */
	public abstract DatabaseTimeType getDefaultTimeType();

	/**
	 * Get a list of allowable values for the --orderBy command line option
	 *
	 * @return An array of all the allowable values or an empty array if there are
	 *         none
	 */
	public abstract String[] getOrderByValues();

	/**
	 * Convenience method to throw exception.
	 *
	 * @param flag
	 *            Channel type flag
	 * @param type
	 *            Time type
	 *
	 * @throws ParseException
	 *             On parameter conflict
	 */
	protected static void throwTimeTypeException(final String flag, final String type) throws ParseException {
		throw new ParseException(
				"Cannot set both --" + CHANNEL_TYPES_LONG + " " + flag + " and --" + TIME_TYPE_LONG + " of " + type);
	}

	/**
	 * Convenience method to throw exception.
	 *
	 * @param flag
	 *            Channel type flag
	 * @param type
	 *            Order-by type
	 *
	 * @throws ParseException
	 *             On parameter conflict
	 */
	protected static void throwOrderException(final String flag, final IDbOrderByType type) throws ParseException {
		throw new ParseException("Cannot set both --" + CHANNEL_TYPES_LONG + " " + flag + " and order-by of " + type);
	}

	/**
	 * Convenience method to throw exception.
	 *
	 * @param flag
	 *            Channel type flag
	 *
	 * @throws ParseException
	 *             On parameter conflict
	 */
	protected static void throwModuleException(final String flag) throws ParseException {
		throw new ParseException("Cannot set both --" + CHANNEL_TYPES_LONG + " " + flag + " and --" + MODULE_LONG);
	}

	/**
	 * Get restore option.
	 *
	 * @param cl
	 *            Command line
	 *
	 * @return True if option set
	 */
	public static boolean parseRestoreOption(final CommandLine cl) {
		return cl.hasOption(RESTORE_LONG);
	}

	/**
	 * Figure out where the data output stream comes from.
	 *
	 * @return Data output stream
	 *
	 * @throws FileNotFoundException
	 *             If file cannot be created
	 */
	private DataOutputStream determineDataOutputStream() throws FileNotFoundException {

		OutputStream os = getOverridingOutputStream();

		if (os == null) {
			final String name = getOutputFilename();

			if (name != null) {
				// Not overridden, use the file

				os = new FileOutputStream(name);
			}
		}

		return ((os != null) ? new DataOutputStream(new BufferedOutputStream(os)) : null);
	}

	/**
	 * Called from main programs to start an application.
	 *
	 * @param args
	 *            Command-line arguments
	 */
	@SuppressWarnings({ "DM_EXIT" })
	public void runMain(final String[] args) {
		try {
			final int status = runAsApp(args);
			printSyslogSummary(msStartTime, nanoStartTime, status);
			System.exit(status);
		} catch (final Exception e) {
			e.printStackTrace();
			TraceManager.getDefaultTracer().error("Unexpected error: " + e.toString());
		}

		System.exit(getExitCode());
	}

	/**
	 * Constructs a JSON Obect containing properties about this fetch application.
	 * The JSON object is then logged to the 'SysLogTracer'. SysLogTracer will print
	 * the JSON to a configured syslog from Log*Configuration.xml.
	 *
	 * @param msStartTime
	 *            Application start time(ms)
	 * @param nanoStartTime
	 *            Application start time(ns)
	 * @param status
	 *            Application status
	 */
	private void printSyslogSummary(final long msStartTime, final long nanoStartTime, final int status) {
		final long msEndTime = System.currentTimeMillis(); // Use millis to calculate start and end timestamp
		final long nanoEndTime = System.nanoTime(); // Use nano to calculate elapsed time

		/** Parse command line arguments into separate JSON */
		final JsonObjectBuilder commandLineOptions = Json.createObjectBuilder();
		commandLineOptions.add("COMMAND", ApplicationConfiguration.getApplicationName());

		if (cmdline != null && cmdline.getOptions() != null) {
			for (final Option o : cmdline.getOptions()) {
				if (o.getLongOpt().equals(ReservedOptions.DATABASE_PASSWORD.getLongOpt())) {
					commandLineOptions.add(o.getLongOpt(), "XXX");
				} else {
					commandLineOptions.add(o.getLongOpt(), (o.getValue() == null ? "true" : o.getValue()));
				}
			}
		}

		/** Create JSON Object */
		final JsonObject syslogSummary = Json.createObjectBuilder()
				.add("USER", GdsSystemProperties.getSystemUserName())
				.add("SUBSYSTEM", "MPCS")
				.add("START_TIME", new AccurateDateTime(msStartTime).getFormattedErt(true))
				.add("END_TIME", new AccurateDateTime(msEndTime).getFormattedErt(true))
				.add("DELTA_MS", ((nanoEndTime - nanoStartTime) / 1000000)).add("ROW_COUNT", recordCount)
				.add("CHILL_EXIT_STATUS", (status > 0 ? ("INTERUPT=" + String.valueOf(status)) : String.valueOf(status)))
				.add("PARAMS", commandLineOptions) // Add command line options to inner JSON array
				.build();

		final Tracer log = TraceManager.getTracer(appContext, Loggers.SYSLOG); // Also writes to application log file
		// Fetch apps intentionally suppress INFO messages
		// Re-enable INFO level messages on the special SYSLOG Tracer for the summary
		if (!log.isEnabledFor(TraceSeverity.INFO)) {
			log.setLevel(TraceSeverity.INFO);
		}
		log.info(Markers.SYS, "chill_sys_info : " + syslogSummary);
	}

	/**
	 * Check print writer status and throw on error.
	 *
	 * @param pw
	 *            Print writer
	 *
	 * @throws IOException
	 *             On any error
	 */
	protected static void checkPrintWriter(final PrintWriter pw) throws IOException {
		if ((pw != null) && pw.checkError()) {
			throw new IOException("I/O error on print writer");
		}
	}

	/**
	 * Construct a message describing the syntax of a SCLK value. Note that input
	 * may be in either subticks or fractional mode, no matter the configured output
	 * mode.
	 *
	 * @param sclk
	 *            A SCLK value to use to extract bounds, etc.
	 *
	 * @return Message
	 *
	 */
	private static String constructExampleMessage(final SclkFormatter sclkFmt, final ISclk sclk) {
		if (sclk == null) {
			return "";
		}

		final StringBuilder sb = new StringBuilder();

		sb.append("\n\nShould be COARSE");
		sb.append(sclkFmt.getTicksSep());
		sb.append("FINE in subtick format where COARSE is [0,");
		sb.append(sclk.getCoarseUpperLimit());
		sb.append("] and FINE is [0,");
		sb.append(sclk.getFineUpperLimit());
		sb.append("].\n");
		sb.append("COARSE and FINE may be padded with any number of 0s on the left.\n");

		sb.append("\nOr, COARSE");
		sb.append(sclkFmt.getFracSep());
		sb.append("FINE in fractional format where COARSE is [0,");
		sb.append(sclk.getCoarseUpperLimit());
		sb.append("] and FINE is one or more digits.\n");
		sb.append("COARSE may be padded ");
		sb.append("with any number of 0s on the left ");
		sb.append("and FINE on the right.\n");

		return sb.toString();
	}

	/**
	 * Added to allow the application type to change queries and pick up a different
	 * set of CSV columns. If the new application type is invalid, it will not be
	 * updated.
	 *
	 * @param newApp
	 *            new CSV application type
	 *
	 */
	protected void resetApplicationType(final String newApp) {
		if (newApp == null || newApp.isEmpty()) {
			return;
		}
		try {

			final Pair<List<String>, List<String>> lists = CsvQueryProperties.instance().getCsvLists(newApp);

			csvColumns = lists.getOne();
			csvHeaders = lists.getTwo();
		}

		catch (final CsvQueryPropertiesException e) {
			trace.warn("Cannot chagne application type to " + newApp
					+ " results will be displayed in original format.\r" + e.getMessage());
		}
	}

	/**
	 * Process arguments for index hints.
	 *
	 * @param sb
	 *            Put result here
	 * @param cl
	 *            Command line
	 * @param option
	 *            Option string
	 * @param allowEmpty
	 *            True if an empty list is allowed
	 * @param prefix
	 *            SQL prefix
	 * @param suffix
	 *            SQL suffix
	 *
	 * @throws ParseException
	 *             Thrown on parameter error
	 * @throws MissingArgumentException
	 *             Thrown on missing value
	 *
	 */
	private static void getIndexHint(final StringBuilder sb, final CommandLine cl, final String option,
									 final boolean allowEmpty, final String prefix, final String suffix)
			throws ParseException, MissingArgumentException {
		if (!cl.hasOption(option)) {
			return;
		}

		if (sb.length() > 0) {
			sb.append(' ');
		}

		final String values = StringUtil.safeTrim(cl.getOptionValue(option));

		sb.append(prefix);

		if (values.isEmpty()) {
			if (!allowEmpty) {
				throw new MissingArgumentException("Option --" + option + " requires a value");
			}
		} else {
			boolean first = true;

			for (final String s : values.split(COMMA, -1)) {
				final String index = StringUtil.safeTrim(s);

				if (index.isEmpty()) {
					throw new ParseException("Option --" + option + " requires non-empty values");
				}

				if (first) {
					first = false;
				} else {
					sb.append(',');
				}

				sb.append(index);
			}
		}

		sb.append(suffix);
	}

	/**
	 * Process arguments for all index hints. Called by all apps that may use hints.
	 *
	 * @param cmdline
	 *            Command line
	 *
	 * @throws ParseException
	 *             Thrown on parameter error
	 * @throws MissingArgumentException
	 *             Thrown on missing value
	 *
	 */
	protected void processIndexHints(final CommandLine cmdline) throws ParseException {
		final StringBuilder hints = new StringBuilder();

		getIndexHint(hints, cmdline, USE_INDEX_LONG, true, "USE INDEX(", ")");

		getIndexHint(hints, cmdline, FORCE_INDEX_LONG, false, "FORCE INDEX(", ")");

		getIndexHint(hints, cmdline, IGNORE_INDEX_LONG, false, "IGNORE INDEX(", ")");

		indexHints = hints.toString();
	}

	/**
	 * Get index hints (if any).
	 *
	 * @return String Index hints or empty
	 *
	 */
	protected String getIndexHints() {
		return indexHints;
	}

	/**
	 * Keep INFO log messages from chill_get output. If the level is already WARNING
	 * or stricter, leave it alone. But if it is looser than INFO, leave it alone to
	 * allow debugging.
	 *
	 */
	protected void suppressInfo() {
		if (!(TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).isDebugEnabled()
				|| TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).isEnabledFor(TraceSeverity.TRACE))) {
			TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.WARN);
		}
	}
}
