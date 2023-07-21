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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.mysql.impl.sql.order.LogOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;

import static jpl.gds.cli.legacy.options.ReservedOptions.TESTHOST_SHORT_VALUE;
import static jpl.gds.cli.legacy.options.ReservedOptions.TESTKEY_SHORT_VALUE;
import static jpl.gds.cli.legacy.options.ReservedOptions.TESTNAME_SHORT_VALUE;
import static jpl.gds.cli.legacy.options.ReservedOptions.TESTUSER_SHORT_VALUE;
import static jpl.gds.cli.legacy.options.ReservedOptions.TESTDESCRIPTION_SHORT_VALUE;
/**
 * The log message fetch application is the command line application for retrieving log messages from the database.
 *
 */
public class LogFetchApp extends AbstractFetchApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_logs");
    private static final int NUM_QUERY_PARAMS = 5;

    /** Long option for context name pattern  */
    static final String CONTEXT_NAME_PATTERN_LONG = "contextNamePattern";

    /** Context name pattern description */
    private static final String CONTEXT_NAME_PATTERN_DESC = "Context name pattern to filter for";

    /** Long option for context user pattern  */
    static final String CONTEXT_USER_PATTERN_LONG = "contextUserPattern";

    /** Context name pattern description */
    private static final String CONTEXT_USER_PATTERN_DESC = "Context user pattern to filter for";

    /** Long option for context host pattern  */
    static final String CONTEXT_HOST_PATTERN_LONG = "contextHostPattern";

    /** Context host pattern description */
    private static final String CONTEXT_HOST_PATTERN_DESC = "Context host pattern to filter for";

    /**
     * The severity level of the logs to query for
     */
    protected TraceSeverity classification;

    /**
     * The log types to query for
     */
    private final Set<String> logTypes;

    /** Session fragment to filter for */
    private SessionFragmentHolder sessionFragment = null;

    /**
     * Creates an instance of LogFetchApp.
     */
    public LogFetchApp()
    {
        super(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME,
              APP_NAME,
              "LogQuery");

        suppressInfo();

        this.classification = null;
        this.logTypes = new HashSet<>();
    }


    /**
     * Get severity.
     *
     * @return Severity
     */
    public TraceSeverity getClassification()
	{
		return this.classification;
	}

    /**
     * Get log types.
     *
     * @return Set of log types
     */
	public Set<String> getLogTypes()
	{
		return this.logTypes;
	}

    /**
     * Parse the desired log types.
     *
     * @param cl Command line
     *
     * @throws ParseException when there are patsing issues
     */
    private void parseLogTypes(final CommandLine cl) throws ParseException
    {
        final String options = StringUtil.safeTrim(cl.getOptionValue(LOG_TYPE_SHORT));

        if (options.length() == 0)
        {
            throw new MissingOptionException("The option -" + LOG_TYPE_SHORT + " requires a value");
        }

        for(final String next : options.split(",", -1))
        {
            final String type = next.trim();
            if (type.length() == 0)
            {
                throw new ParseException("The log type values cannot be empty");
            }
            logTypes.add(type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {
    	super.createRequiredOptions();
        this.requiredOptions.add(BEGIN_TIME_LONG);
        this.requiredOptions.add(CLASSIFICATION_LONG);
        this.requiredOptions.add(END_TIME_LONG);
        this.requiredOptions.add(LOG_TYPE_LONG);
        this.requiredOptions.add(FRAME_TYPE_LONG);
        this.requiredOptions.add(CONTEXT_ID_LONG);
        this.requiredOptions.add(FRAGMENT_LONG);
        this.requiredOptions.add(CONTEXT_NAME_PATTERN_LONG);
        this.requiredOptions.add(CONTEXT_USER_PATTERN_LONG);
        this.requiredOptions.add(CONTEXT_HOST_PATTERN_LONG);

    }

    /**
     * {@inheritDoc}
     */
	@Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        super.configureApp(cmdline);

        //parse and store the classification (if it exists)
        if(cmdline.hasOption(CLASSIFICATION_SHORT))
        {
            final String classif = cmdline.getOptionValue(CLASSIFICATION_SHORT);
            if(classif == null)
            {
                throw new MissingOptionException("The option -" + CLASSIFICATION_SHORT + " requires a value.");
            }

            try
            {
                this.classification = TraceSeverity.fromStringValue(classif.trim());
            }
            catch(final IllegalArgumentException iae)
            {
                throw new ParseException("The input classification value \"" + classif + "\" is not a valid log message classification.");
            }
        }

        // Parse and store the desired log types
        if(cmdline.hasOption(LOG_TYPE_SHORT))
        {
            parseLogTypes(cmdline);
        }

        //the session patterns were already checked in abstract class, but we have to check conflicting options
        //session and context patterns are not supported at the same time
        if (hasSessionPattern(cmdline)) {
            checkSessionPattern(cmdline);
        }

        // read in the context host
        if (cmdline.hasOption(CONTEXT_HOST_PATTERN_LONG)) {
            final String hostStr = cmdline.getOptionValue(CONTEXT_HOST_PATTERN_LONG);
            if (hostStr == null) {
                throw new MissingArgumentException("-" + CONTEXT_HOST_PATTERN_LONG + " requires acomma-separated list of hostname patterns as an argument");
            }

            checkContextPattern(cmdline);

            useContext = true;
            final String[] hostStrings = hostStr.split(COMMA);
            for (String host: hostStrings) {
                dbContextInfo.addHostPattern(host.trim());
            }
        }

        // read in the context user
        if (cmdline.hasOption(CONTEXT_USER_PATTERN_LONG)) {
            final String userStr = cmdline.getOptionValue(CONTEXT_USER_PATTERN_LONG);
            if (userStr == null) {
                throw new MissingArgumentException("-" + CONTEXT_USER_PATTERN_LONG + " requires a comma-separated list of user patterns as an argument");
            }

            checkContextPattern(cmdline);

            useContext = true;
            final String[] userStrings = userStr.split(COMMA);
            for (String user: userStrings) {
                dbContextInfo.addUserPattern(user.trim());
            }
        }

        // read in the context name
        if (cmdline.hasOption(CONTEXT_NAME_PATTERN_LONG)) {
            final String nameStr = cmdline.getOptionValue(CONTEXT_NAME_PATTERN_LONG);
            if (nameStr == null) {
                throw new MissingArgumentException("-" + CONTEXT_NAME_PATTERN_LONG + " requires a comma-separated list of name patterns as an argument");
            }

            checkContextPattern(cmdline);

            useContext = true;
            final String[] nameStrings = nameStr.split(COMMA);
            for (String name: nameStrings) {
                dbContextInfo.addNamePattern(name.trim());
            }
        }

        //specific to this app:
        //context ID and session ID or patterns, add context IDs as parent keys in the session info and join by session
        if (cmdline.hasOption(CONTEXT_ID_SHORT)){
            if(cmdline.hasOption(TESTKEY_SHORT_VALUE) || hasSessionPattern(cmdline)) {
                for(Long parent : dbContextInfo.getSessionKeyList()) {
                    dbSessionInfo.addParentKey(parent);
                }
                useContext = false;
            }
        }
        //session ID and context patterns, add session IDs as parent keys in the context info and join by context
        if (cmdline.hasOption(TESTKEY_SHORT_VALUE) && hasContextPattern(cmdline)){
            for (Long parent : dbSessionInfo.getSessionKeyList()) {
                dbContextInfo.addParentKey(parent);
            }
            useContext = true;
        }

        if(cmdline.hasOption(FRAGMENT_SHORT)) {
            try {
                sessionFragment = SessionFragmentHolder.valueOf((Integer.parseInt(cmdline.getOptionValue(FRAGMENT_SHORT))));
                dbSessionInfo.setSessionFragment(sessionFragment);
            }
            catch (final HolderException | NumberFormatException e) {
                throw new ParseException("Session Fragment '" + cmdline.getOptionValue(FRAGMENT_SHORT) + "' is not a valid session fragment");
            }
        }
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
    {
        fetch = appContext.getBean(IDbSqlFetchFactory.class).getLogFetch(sqlStmtOnly);
    	return fetch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
    	final Object[] params = new Object[NUM_QUERY_PARAMS];

    	LogOrderByType orderType = LogOrderByType.DEFAULT;

    	if(this.orderByString != null)
    	{
    		try
			{
				orderType = new LogOrderByType(this.orderByString.trim());
			}
			catch(final IllegalArgumentException iae)
			{
				throw new IllegalArgumentException("The value '" + this.orderByString +
                                                   "' is not a legal ordering value for this application.", iae);
			}
    	}
        else
        {
            switch (times.getTimeType().getValueAsInt())
            {
                case DatabaseTimeType.EVENT_TIME_TYPE:
                    orderType = LogOrderByType.EVENT_TIME;
                    break;
                case DatabaseTimeType.RCT_TYPE:
                    orderType = LogOrderByType.RCT;
                    break;
                default:
                    break;
            }
		}

    	params[0] = this.classification !=  null ? this.classification.toString() : null;
    	params[1] = this.logTypes;
    	params[2] = null;
    	params[3] = orderType;
    	params[4] = sessionFragment;

    	return(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range) throws ParseException
    {

        if ((range.getTimeType().getValueAsInt() != DatabaseTimeType.EVENT_TIME_TYPE) &&
            (range.getTimeType().getValueAsInt() != DatabaseTimeType.RCT_TYPE))
    	{
    		throw new ParseException("Time type must be "        +
                                     DatabaseTimeType.EVENT_TIME +
                                     " or "                      +
                                     DatabaseTimeType.RCT        +
                                     " for this application");
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
    	return(DatabaseTimeType.EVENT_TIME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return(LogOrderByType.orderByTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return
        (
                APP_NAME + " [--" + CLASSIFICATION_LONG + " <classification> --" + BEGIN_TIME_LONG + " <time> --" + END_TIME_LONG + " <time>]\n"
                + "               [Session search options - Not  required]\n"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions()
    {
        super.addAppOptions();
        addOption(OUTPUT_FORMAT_SHORT,OUTPUT_FORMAT_LONG,"format", OUTPUT_FORMAT_DESC);
		addOption(SHOW_COLUMNS_SHORT,SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
        addOption(CLASSIFICATION_SHORT, CLASSIFICATION_LONG, "string", "The severity of the log message.  Valid values are: Info, Warning, Error, Fatal");
        addOption(LOG_TYPE_SHORT,LOG_TYPE_LONG,"string,...","The desired log message types");
        addOption(BEGIN_TIME_SHORT,BEGIN_TIME_LONG, "time", "Begin time of log event time range");
        addOption(END_TIME_SHORT,END_TIME_LONG, "time", "End time of log event time range");
        addOption(CONTEXT_ID_SHORT,CONTEXT_ID_LONG, "contextId", "The unique numeric identifier for a context.");

        addOption(TIME_TYPE_SHORT, TIME_TYPE_LONG, TIME_TYPE_ARG,
                  "Time type should be one of (EVENT_TIME, RCT). Default is " + getDefaultTimeType().getValueAsString());

        addOption(FRAGMENT_SHORT, FRAGMENT_LONG, "integer", FRAGMENT_DESC);

        addOption(null, CONTEXT_NAME_PATTERN_LONG, "name", CONTEXT_NAME_PATTERN_DESC);
        addOption(null, CONTEXT_HOST_PATTERN_LONG, "hostname", CONTEXT_HOST_PATTERN_DESC);
        addOption(null, CONTEXT_USER_PATTERN_LONG, "username", CONTEXT_USER_PATTERN_DESC);

        addOrderOption();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        super.showHelp();
        System.out.println("\nMetadata for selected Test Configurations will be written to standard");
        System.out.println("output.  Format of this output can be specified using the -" + OUTPUT_FORMAT_SHORT + " option.");
        printTemplateStyles();
    }

    /**
     * Main entry point to run application.
     *
     * @param args Arguments from command-line.
     */
    public static void main(final String[] args)
    {
        final LogFetchApp app = new LogFetchApp();
        app.runMain(args);
    }

    private void checkContextPattern(final CommandLine cmdline) throws ParseException{
        //session and context pattern parameters cannot be mixed
        if(hasSessionPattern(cmdline)){
            throw new ParseException("Cannot specify both session and context pattern parameters");
        }
    }

    private void checkSessionPattern(final CommandLine cmdline) throws ParseException {
        //session and context pattern parameters cannot be mixed
        if(hasContextPattern(cmdline)){
            throw new ParseException("Cannot specify both session and context pattern parameters");
        }
    }

    private boolean hasSessionPattern(final CommandLine cmdline){
        return  cmdline.hasOption(TESTNAME_SHORT_VALUE) ||
                cmdline.hasOption(TESTUSER_SHORT_VALUE) ||
                cmdline.hasOption(TESTHOST_SHORT_VALUE) ||
                cmdline.hasOption(TESTDESCRIPTION_SHORT_VALUE);
    }

    private boolean hasContextPattern(final CommandLine cmdline){
        return cmdline.hasOption(CONTEXT_NAME_PATTERN_LONG) ||
                cmdline.hasOption(CONTEXT_HOST_PATTERN_LONG) ||
                cmdline.hasOption(CONTEXT_USER_PATTERN_LONG);
    }
}
