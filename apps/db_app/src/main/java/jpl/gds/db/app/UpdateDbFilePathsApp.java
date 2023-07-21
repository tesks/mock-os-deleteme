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
/**
 * File: UpdateDbFilePathsApp.java
 *
 */
package jpl.gds.db.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import jpl.gds.db.api.sql.order.ISessionOrderByType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.db.app.util.ProductUpdater;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;


/**
 * UpdateDbFilePathsApp updates filepaths in the database after a migration of MPCS
 * output data and databases to another host or filesystem.  At this time, it just
 * has to update the output directory in the session table.  In the future, more updates
 * may be required.
 *
 */
public class UpdateDbFilePathsApp extends AbstractDatabaseApp {
    private static final String            APP_NAME                = ApplicationConfiguration.getApplicationName("chill_update_paths");

    private static final int               ABORTED                 = OTHER_ERROR + 1;

    /** Test start lower short option */
    public static final String             TEST_START_LOWER_SHORT  = "w";

    /** Test start lower long option */
    public static final String             TEST_START_LOWER_LONG   = "fromTestStart";

    /** Test start upper short option */
    public static final String             TEST_START_UPPER_SHORT  = "x";

    /** Test start upper long option */
    public static final String             TEST_START_UPPER_LONG   = "toTestStart";

    /** From session path short option */
    public static final String             FROM_SESSION_PATH_SHORT = "s";

    /** From session path long option */
    public static final String             FROM_SESSION_PATH_LONG  = "fromSessionPath";

    /** To session path short option */
    public static final String             TO_SESSION_PATH_SHORT   = "f";

    /** To session path long option */
    public static final String             TO_SESSION_PATH_LONG    = "toSessionPath";

    private static final String            FROM_PRODUCT_PATH_SHORT = "p";
    private static final String            FROM_PRODUCT_PATH_LONG  = "fromProductPath";
    private static final String            TO_PRODUCT_PATH_SHORT   = "t";
    private static final String            TO_PRODUCT_PATH_LONG    = "toProductPath";
    private static final String            PRODUCT_SAME_SHORT      = "e";
    private static final String            PRODUCT_SAME_LONG       = "productAlso";

    private final IDbSessionInfoUpdater    dbSessionInfo;
    private String                         toFilePath;
    private String                         fromFilePath;
    private String                         toProductFilePath;
    private String                         fromProductFilePath;
    private boolean                        autorun;
    private final List<IDbSessionProvider> affectedSessions        = new ArrayList<IDbSessionProvider>(128);
    private boolean                        productSameAsSession    = false;
    private final IOrderByTypeFactory      orderByTypeFactory;
    private final SseContextFlag           sseFlag;

    /**
     * Creates an instance of UpdateDbFilePathsApp.
     */
    public UpdateDbFilePathsApp() {
        super(IDbTableNames.DB_SESSION_DATA_TABLE_NAME, APP_NAME);

        final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        dbSessionInfo = dbSessionInfoFactory.createQueryableUpdater();
        sseFlag = appContext.getBean(SseContextFlag.class);
        orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);
    }

    /**
     * The main application entry point
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        UpdateDbFilePathsApp app = null;
        try {
            app = new UpdateDbFilePathsApp();
            final int status = app.runAsApp(args);
            System.exit(status);
        } catch (final Exception e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().fatal("Unexpected error: " + e.toString());

        }
        System.exit(app.getExitCode());

    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.AbstractDatabaseApp#addAppOptions()
     */
    @Override
    protected void addAppOptions() {

        AbstractFetchApp.createHostPortUserPwdOptions(options);

        Option opt;
        final String desc2String = " Multiple values may be supplied in a comma-separated value (CSV) or range (start-stop) format.";
        final String desc1String = ". Individual entries may be specified using an SQL LIKE pattern with wildcards like % and _. ";
        final String desc3String = " Multiple values may be supplied in a comma-separated value (CSV) format.";
        final String descString = desc1String + desc3String;

        final Option keyOpt = ReservedOptions.getOption(ReservedOptions.TESTKEY_SHORT_VALUE);
        opt = new Option(keyOpt.getOpt(),keyOpt.getLongOpt(), keyOpt.hasArg(), keyOpt.getDescription() + desc2String);
        opt.setArgName(ReservedOptions.TESTKEY_ARGNAME);
        addOption(opt);

        addOption(ReservedOptions.createOption(TEST_START_LOWER_SHORT,TEST_START_LOWER_LONG,"time","Lower bound on start time of session"));
        addOption(ReservedOptions.createOption(TEST_START_UPPER_SHORT,TEST_START_UPPER_LONG,"time","Upper bound on start time of session"));

        addOption(ReservedOptions.createOption(FROM_SESSION_PATH_SHORT,FROM_SESSION_PATH_LONG,"old-path","Absolute session path to replace; defaults to whole session path"));
        addOption(ReservedOptions.createOption(TO_SESSION_PATH_SHORT,TO_SESSION_PATH_LONG,"new-path","New absolute session path"));

        addOption(ReservedOptions.createOption(FROM_PRODUCT_PATH_SHORT,FROM_PRODUCT_PATH_LONG,"old-path","Absolute product path to replace"));
        addOption(ReservedOptions.createOption(TO_PRODUCT_PATH_SHORT,TO_PRODUCT_PATH_LONG,"new-path","New absolute product path"));
        addOption(ReservedOptions.createOption(PRODUCT_SAME_SHORT,PRODUCT_SAME_LONG,null,"use session path options for products"));

        addOption(ReservedOptions.getOption(ReservedOptions.AUTORUN_SHORT_VALUE));

        final Option hostOpt = ReservedOptions.getOption(ReservedOptions.TESTHOST_SHORT_VALUE);
        opt = new Option(hostOpt.getOpt(),hostOpt.getLongOpt() + "Pattern", hostOpt.hasArg(), hostOpt.getDescription() + descString);
        opt.setArgName(ReservedOptions.TESTHOST_ARGNAME);
        addOption(opt);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.AbstractDatabaseApp#configureApp(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException {

        autorun = cmdline.hasOption(ReservedOptions.AUTORUN_SHORT_VALUE);


        if (cmdline.hasOption(ReservedOptions.TESTKEY_SHORT_VALUE))
        {
            final String testKeyStr = cmdline.getOptionValue(ReservedOptions.TESTKEY_SHORT_VALUE);
            if (testKeyStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTKEY_SHORT_VALUE + " requires a " +
                "comma-separated list of numeric test keys as an argument");
            }

            final String[] testKeyStrings = testKeyStr.split(",");
            for(int i=0; i < testKeyStrings.length; i++)
            {
                try
                {
                    final String keyStr = testKeyStrings[i].trim();
                    if (keyStr.indexOf("..") != -1) {
                        final String[] rangeKeys = keyStr.split("\\.\\.");
                        if (rangeKeys.length != 2) {
                            throw new ParseException("Test key ranges must be specified as 'start..stop'");
                        }
                        dbSessionInfo.addSessionKeyRange(Long.valueOf(rangeKeys[0]), Long.valueOf(rangeKeys[1]));
                    } else {
                        dbSessionInfo.addSessionKey(Long.valueOf(keyStr));
                    }
                }
                catch (final NumberFormatException e1)
                {
                    throw new ParseException("Value of -" + ReservedOptions.TESTKEY_SHORT_VALUE + " option must be a " +
                            "list of comma-separated integer values, but the value \"" + testKeyStrings[i] + "\" is invalid.");
                }
            }
        }

        //read in the test host
        if (cmdline.hasOption(ReservedOptions.TESTHOST_SHORT_VALUE))
        {
            final String hostStr = cmdline.getOptionValue(ReservedOptions.TESTHOST_SHORT_VALUE);
            if (hostStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTHOST_SHORT_VALUE + " requires a " +
                "comma-separated list of hostname patterns as an argument");
            }

            final String[] hostStrings = hostStr.split(",");
            for(int i=0; i < hostStrings.length; i++)
            {
                dbSessionInfo.addHostPattern(hostStrings[i].trim());
            }
        }

        //read in the lower bound on start of test time
        if (cmdline.hasOption(TEST_START_LOWER_SHORT))
        {
            final String beginTestTimeStr = cmdline.getOptionValue(TEST_START_LOWER_SHORT);
            if (beginTestTimeStr == null)
            {
                throw new MissingArgumentException("-" + TEST_START_LOWER_SHORT + " requires a time argument");
            }

            try
            {
                final IAccurateDateTime fromTestTime = new AccurateDateTime(beginTestTimeStr.trim());
                dbSessionInfo.setStartTimeLowerBound(fromTestTime);
            }
            catch (final java.text.ParseException e)
            {
                throw new ParseException("Test start time has invalid format; should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:sss.ttt");
            }
        }

        //read in the upper bound on start of test time
        if (cmdline.hasOption(TEST_START_UPPER_SHORT))
        {
            final String endTestTimeStr = cmdline.getOptionValue(TEST_START_UPPER_SHORT);
            if (endTestTimeStr == null)
            {
                throw new MissingArgumentException("-" + TEST_START_UPPER_SHORT + " requires a time argument");
            }

            try
            {
                final IAccurateDateTime toTestTime = new AccurateDateTime(endTestTimeStr.trim());
                dbSessionInfo.setStartTimeUpperBound(toTestTime);
            }
            catch (final java.text.ParseException e)
            {
                throw new ParseException("Time start time has invalid format; should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:sss.ttt");
            }
        }

        // read in the from path
        if (cmdline.hasOption(FROM_SESSION_PATH_SHORT))
        {
            fromFilePath = cmdline.getOptionValue(FROM_SESSION_PATH_SHORT);
            if (fromFilePath == null)
            {
                throw new MissingArgumentException("-" + FROM_SESSION_PATH_SHORT + " requires a path argument");
            }
        }

        // read in the to path
        if (cmdline.hasOption(TO_SESSION_PATH_SHORT))
        {
            toFilePath = cmdline.getOptionValue(TO_SESSION_PATH_SHORT);
            if (toFilePath == null)
            {
                throw new MissingArgumentException("-" + TO_SESSION_PATH_SHORT + " requires a path argument");
            }
        } else {
            throw new MissingOptionException("A new (to) session path is a required command line option");
        }

        productSameAsSession = cmdline.hasOption(PRODUCT_SAME_SHORT);
        
        if (productSameAsSession) {
            fromProductFilePath = fromFilePath;
            toProductFilePath = toFilePath;
            if (fromFilePath == null) {
                throw new ParseException("If --" + PRODUCT_SAME_LONG + " is specified, --" +
                        FROM_SESSION_PATH_LONG + " must also be specified.");                       
            }
        }
        
        // read in the from path
        if (cmdline.hasOption(FROM_PRODUCT_PATH_SHORT))
        {
            if (productSameAsSession) {
                throw new ParseException("You cannot specify both --" + PRODUCT_SAME_LONG + " and --" +
                        FROM_PRODUCT_PATH_LONG);
            }
            fromProductFilePath = cmdline.getOptionValue(FROM_PRODUCT_PATH_SHORT);
            if (fromProductFilePath == null)
            {
                throw new MissingArgumentException("-" + FROM_PRODUCT_PATH_SHORT + " requires a path argument");
            }
        }

        // read in the to path
        if (cmdline.hasOption(TO_PRODUCT_PATH_SHORT))
        {
            if (productSameAsSession) {
                throw new ParseException("You cannot specify both --" + PRODUCT_SAME_LONG + " and --" +
                        TO_PRODUCT_PATH_LONG);
            }
            toProductFilePath = cmdline.getOptionValue(TO_PRODUCT_PATH_SHORT);
            if (toProductFilePath == null)
            {
                throw new MissingArgumentException("-" + TO_PRODUCT_PATH_SHORT + " requires a path argument");
            }
        } else if (cmdline.hasOption(FROM_PRODUCT_PATH_SHORT)) {
            throw new MissingOptionException("A new (to) product path is a required command line option");
        }
        
        if (cmdline.hasOption(TO_PRODUCT_PATH_SHORT) && !cmdline.hasOption(FROM_PRODUCT_PATH_SHORT) ||
            !cmdline.hasOption(TO_PRODUCT_PATH_SHORT) && cmdline.hasOption(FROM_PRODUCT_PATH_SHORT) ) {
            throw new ParseException("If one of --" + TO_PRODUCT_PATH_LONG + " or --" + FROM_PRODUCT_PATH_LONG +
                    " is specified, both must be");
        }
        
        ReservedOptions.parseDatabaseHost(cmdline,false);
        ReservedOptions.parseDatabasePort(cmdline,false);
        ReservedOptions.parseDatabaseUsername(cmdline,false);
        ReservedOptions.parseDatabasePassword(cmdline,false);
    
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.AbstractDatabaseApp#getUsage()
     */
    @Override
    public String getUsage() {
        return APP_NAME + " --toFilePath <new-path> [--fromFilePath <old-path> " +
        "--testKey <keys...> --fromTestStart <time> --toTestStart <time> " +
        "--testHostPattern <host-list> --databaseHost <host> --databasePort <port> " +
        "--dbUser <username> --dbPwd <password>]\n";
    }

    /**
     * Asks the user to confirm database updates.
     * @param recordCount the number of sessions to be updated
     * @return true if the user confirms, false otherwise
     */
    private boolean getUserConfirmation(final int recordCount) {
        if (recordCount == 0) {
            return false;
        }

        if (fromFilePath == null) {
            System.out.println("\nThe session output directory name for the " + recordCount + " sessions listed above will be replaced by\n"
                    + "path '" + toFilePath + "' in the "
                    + GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse())
                    + " database.");
        } else {
            System.out.println("\nThe session output directory prefix (" + fromFilePath + ") for the " + recordCount +
                    " sessions listed will be replaced by\nprefix '" + toFilePath + "' in the " +
                    GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()) + " database.");
        }
        
        if (toProductFilePath != null) {
            if (fromFilePath == null) {
                System.out.println("\nThe product filenames for the " + recordCount + " sessions listed above will be replaced by\n"
                        + "path '" + toProductFilePath + "' in the "
                        + GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()) + " database.");
            } else {
                System.out.println("\nThe product filename prefix (" + fromProductFilePath + ") for the " + recordCount +
                        " sessions listed will be replaced by\nprefix '" + toProductFilePath + "' in the " +
                        GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()) + " database.");
            }
        } else {
            System.out.println("Product filenames will NOT be updated for these sessions.");
        }

        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while(true) {
            System.out.println("\nIs this what you want to do? [y/n]: ");
            String userResponse = null;

            //  read the user response from the command line
            try {
                userResponse = br.readLine();
                if (userResponse.equalsIgnoreCase("n") || userResponse.equalsIgnoreCase("no")) {
                    trace.info("Update operation aborted at user request");
                    setExitCode(ABORTED);
                    return false;
                } else if (userResponse.equalsIgnoreCase("y") || userResponse.equalsIgnoreCase("yes")) {
                    break;
                } else {
                    System.out.println("I did not understand your response. Please answer y or n.");
                }
            } catch (final IOException ioe) {
                trace.fatal("I/O Error reading user response from console");
                setExitCode(OTHER_ERROR);
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a list of session records that meet the user's input search criteria.
     *
     */
    @SuppressWarnings("unchecked")
    private void getAffectedSessions() {
         final ISessionFetch tsf = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch();

         // Make sure we connected to the database
         if (!tsf.isConnected()) {
             setExitCode(OTHER_ERROR);
             return;
         }

        try
        {
            // execute the query for test configurations based on the command line input
            List<IDbSessionProvider> configsOut = (List<IDbSessionProvider>) tsf.get(dbSessionInfo, null, 100,
                                                                                     orderByTypeFactory.getOrderByType(OrderByType.SESSION_ORDER_BY,
                                                                                         ISessionOrderByType.ID_TYPE));

            //loop through all the results and write them out to standard output
            while(configsOut.size() > 0)
            {
                final ListIterator<IDbSessionProvider> iter = configsOut.listIterator();
                while(iter.hasNext() == true)
                {
                    final IDbSessionProvider dsc = iter.next();
                    affectedSessions.add(dsc);
                    if (!autorun) {
                        final String dateStr = TimeUtility.getFormatter().format(dsc.getStartTime());
                        System.out.println("Session Key=" + dsc.getSessionId() + " Host=" + dsc.getSessionHost() + " Name=" + dsc.getName() + " Start Time=" + dateStr);
                    }
                }

                configsOut = (List<IDbSessionProvider>) tsf.getNextResultBatch();
            }
        }
        catch(final Exception e)
        {
            trace.error("Problem encountered while retrieving session records");
            trace.fatal(e.getMessage());
            setExitCode(OTHER_ERROR);
        }
        finally
        {
            tsf.close();
        }
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        setExitCode(SUCCESS);

        // Query for all the affected sessions.

        getAffectedSessions();
        if (affectedSessions.size() == 0) {
            trace.info("No sessions satisfied the input criteria");
            return;
        }

        // Get user confirmation if required; autorun means do not ask
        if (!autorun && !getUserConfirmation(affectedSessions.size())) {
            return;
        }

        // Create Database Controller
        final IDbSqlArchiveController archiveController = appContext.getBean(IDbSqlArchiveController.class);
        archiveController.addNeededStore(StoreIdentifier.Session);
        archiveController.init();
        final boolean ok = archiveController.startSessionStores();
        if (!ok) {
            trace.error("Unable to start session database store");
            return;
        }

        final ISessionStore tsf = archiveController.getSessionStore();
        
        // Now update all the affected session and product records in turn
        tsf.start();
        ProductUpdater ps = null;
        
        if (toProductFilePath != null) {
            try {
                ps = new ProductUpdater(appContext);
            } catch (final Exception e) {
                e.printStackTrace();
                trace.error("Unable to create product database adaptor for the current mission");
                setExitCode(OTHER_ERROR);
                return;
            }
        }        
        try
        {
            tsf.updateOutputDirectory(dbSessionInfo, fromFilePath, toFilePath);
            trace.info("Session paths updated.");
            if (ps != null) {
                ps.updateOutputDirectory(dbSessionInfo, fromProductFilePath, toProductFilePath);
                trace.info("Product paths updated.");
            }
 
        }
        catch(final Exception e)
        {
            e.printStackTrace();
            trace.error("Problem encountered while updating session records");
            trace.fatal(e.getMessage());
            setExitCode(OTHER_ERROR);
        }
        finally
        {
            tsf.stop();
            archiveController.shutDown();
            if (ps != null) {
                ps.close();
            }
        }
    }
}
