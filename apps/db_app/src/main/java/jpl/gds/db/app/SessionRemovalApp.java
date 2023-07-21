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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.db.app.util.ContextRemover;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * This application removes entire sessions from the database.
 */
public class SessionRemovalApp extends AbstractDatabaseApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_remove_sessions");

    /**
     * An object encompassing all the test session related information that
     * will be used during a query to join other tables with the test session
     * table
     */
    private final IDbSessionInfoUpdater dbSessionInfo;

    private boolean doDeleteFiles;
    private boolean doDeleteDatabase;
    private boolean prompt;


    /**
     * Constructor.
     */
    public SessionRemovalApp()
    {
        super(APP_NAME);

        final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        dbSessionInfo = dbSessionInfoFactory.createQueryableUpdater();
        doDeleteDatabase = true;
        doDeleteFiles = true;
        prompt = true;
    }

    private static final String ONLY_DELETE_FILES_SHORT = "f";
    private static final String ONLY_DELETE_FILES_LONG = "onlyFiles";
    private static final String ONLY_DELETE_DATABASE_SHORT = "b";
    private static final String ONLY_DELETE_DATABASE_LONG = "onlyDatabase";
    private static final String NO_PROMPT_SHORT = "p";
    private static final String NO_PROMPT_LONG = "noPrompt";
    private static final String TEST_START_LOWER_LONG = "fromTestStart";
    private static final String TEST_START_UPPER_LONG = "toTestStart";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions()
    {
        AbstractFetchApp.createCommonOptions(options);
        AbstractFetchApp.createTestOptions(options);

        addOption(ONLY_DELETE_FILES_SHORT,ONLY_DELETE_FILES_LONG,null,"Only delete the output directory for this session (application will remove this entry by default).");
        addOption(ONLY_DELETE_DATABASE_SHORT,ONLY_DELETE_DATABASE_LONG,null,"Only delete the database entry for this session (application will remove this entry by default).");
        addOption(NO_PROMPT_SHORT,NO_PROMPT_LONG,null,"Do not prompt before deleting sessions (application will prompt by default).");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        //read in the unique test key
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

        //read in the test user
        if (cmdline.hasOption(ReservedOptions.TESTUSER_SHORT_VALUE))
        {
            final String userStr = cmdline.getOptionValue(ReservedOptions.TESTUSER_SHORT_VALUE);
            if (userStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTUSER_SHORT_VALUE + " requires a " +
                "comma-separated list of user patterns as an argument");
            }

            final String[] userStrings = userStr.split(",");
            for(int i=0; i < userStrings.length; i++)
            {
                dbSessionInfo.addUserPattern(userStrings[i].trim());
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

        //read in the test name
        if (cmdline.hasOption(ReservedOptions.TESTNAME_SHORT_VALUE))
        {
            final String nameStr = cmdline.getOptionValue(ReservedOptions.TESTNAME_SHORT_VALUE);
            if (nameStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTNAME_SHORT_VALUE + " requires a " +
                "comma-separated list of name patterns as an argument");
            }

            final String[] nameStrings = nameStr.split(",");
            for(int i=0; i < nameStrings.length; i++)
            {
                dbSessionInfo.addNamePattern(nameStrings[i].trim());
            }
        }

        //read in the test description
        if (cmdline.hasOption(ReservedOptions.TESTDESCRIPTION_SHORT_VALUE))
        {
            final String descStr = cmdline.getOptionValue(ReservedOptions.TESTDESCRIPTION_SHORT_VALUE);
            if (descStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTDESCRIPTION_SHORT_VALUE + " requires a " +
                "comma-separated list of description patterns as an argument");
            }

            final String[] descStrings = descStr.split(",");
            for(int i=0; i < descStrings.length; i++)
            {
                dbSessionInfo.addDescriptionPattern(descStrings[i].trim());
            }
        }

        //read in the test sse version
        if (cmdline.hasOption(ReservedOptions.SSEVERSION_SHORT_VALUE))
        {
            final String sseStr = cmdline.getOptionValue(ReservedOptions.SSEVERSION_SHORT_VALUE);
            if (sseStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.SSEVERSION_SHORT_VALUE + " requires a " +
                "comma-separated list of SSE versions as an argument");
            }

            final String[] sseStrings = sseStr.split(",");
            for(int i=0; i < sseStrings.length; i++)
            {
                dbSessionInfo.addSseVersionPattern(sseStrings[i].trim());
            }
        }

        //read in the test fsw version
        if (cmdline.hasOption(ReservedOptions.FSWVERSION_SHORT_VALUE))
        {
            final String fswStr = cmdline.getOptionValue(ReservedOptions.FSWVERSION_SHORT_VALUE);
            if (fswStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.FSWVERSION_SHORT_VALUE + " requires a " +
                "comma-separated list of FSW versions as an argument");
            }

            final String[] fswStrings = fswStr.split(",");
            for(int i=0; i < fswStrings.length; i++)
            {
                dbSessionInfo.addFswVersionPattern(fswStrings[i].trim());
            }
        }

        //read in the lower bound on start of test time
        if (cmdline.hasOption(AbstractFetchApp.TEST_START_LOWER_SHORT))
        {
            final String beginTestTimeStr = cmdline.getOptionValue(AbstractFetchApp.TEST_START_LOWER_SHORT);
            if (beginTestTimeStr == null)
            {
                throw new MissingArgumentException("-" + AbstractFetchApp.TEST_START_LOWER_SHORT + " requires a time argument");
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
        if (cmdline.hasOption(AbstractFetchApp.TEST_START_UPPER_SHORT))
        {
            final String endTestTimeStr = cmdline.getOptionValue(AbstractFetchApp.TEST_START_UPPER_SHORT);
            if (endTestTimeStr == null)
            {
                throw new MissingArgumentException("-" + AbstractFetchApp.TEST_START_UPPER_SHORT + " requires a time argumentor YYYY-DOYThh:mm:sss.ttt");
            }

            try
            {
                final IAccurateDateTime toTestTime = new AccurateDateTime(endTestTimeStr.trim());
                dbSessionInfo.setStartTimeUpperBound(toTestTime);
            }
            catch (final java.text.ParseException e)
            {
                throw new ParseException("Time start time has invalid format; should be YYYY-MM-DDThh:mm:ss.ttt");
            }
        }

        //read in the test downlink stream ID
        if (cmdline.hasOption(ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE))
        {
            final String streamIdStr = cmdline.getOptionValue(ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE);
            if (streamIdStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE + " requires a " +
                "comma-separated list of downlink stream IDs as an argument");
            }

            final String[] streamStrings = streamIdStr.split(",");
            for(int i=0; i < streamStrings.length; i++)
            {
                dbSessionInfo.addDownlinkStreamId(streamStrings[i].trim());
            }
        }

        //read in the test type
        if(cmdline.hasOption(ReservedOptions.TESTTYPE_SHORT_VALUE))
        {
            final String testTypeStr = cmdline.getOptionValue(ReservedOptions.TESTTYPE_SHORT_VALUE);
            if(testTypeStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTTYPE_SHORT_VALUE + " requires a " +
                "comma-separated list of test types as an argument");
            }

            final String[] typeStrings = testTypeStr.split(",");
            for(int i=0; i < typeStrings.length; i++)
            {
                dbSessionInfo.addTypePattern(typeStrings[i].trim());
            }
        }

        ReservedOptions.parseDatabaseHost(cmdline,false);
        ReservedOptions.parseDatabasePort(cmdline,false);
        ReservedOptions.parseDatabaseUsername(cmdline,false);
        ReservedOptions.parseDatabasePassword(cmdline,false);

        //make sure we have enough information to search for a test
        if (!dbSessionInfo.isSearchCriteriaSet())
        {
            throw new MissingOptionException("You must specify at least one test search criterion");
        }

        doDeleteDatabase = !cmdline.hasOption(ONLY_DELETE_FILES_SHORT);
        doDeleteFiles = !cmdline.hasOption(ONLY_DELETE_DATABASE_SHORT);
        prompt = !cmdline.hasOption(NO_PROMPT_SHORT);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return
        APP_NAME + " [" +
        "--" + ONLY_DELETE_DATABASE_LONG + " --"+ ONLY_DELETE_FILES_LONG + " --" + NO_PROMPT_LONG + "\n                      " +
        "--" + ReservedOptions.TESTKEY_LONG_VALUE + " <number> " +
        "--" + ReservedOptions.TESTNAME_LONG_VALUE + " <name> " +
        "--" + ReservedOptions.TESTUSER_LONG_VALUE + " <username> " +
        "--" + ReservedOptions.TESTHOST_LONG_VALUE + " <hostname>\n" + "                      " +
        "--" + ReservedOptions.SSEVERSION_LONG_VALUE + " <version> " +
        "--" + ReservedOptions.FSWVERSION_LONG_VALUE + " <version> " +
        "--" + TEST_START_LOWER_LONG+ " <time> " +
        "--" + TEST_START_UPPER_LONG + " <time>\n" + "                      " +
        "--" + ReservedOptions.TESTDESCRIPTION_LONG_VALUE + " <description>]\n";
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Runnable#run()
     */
    @SuppressWarnings("unchecked")
    @Override
	public void run()
    {
        setExitCode(SUCCESS);

        final ContextRemover tsr = new ContextRemover(appContext);
        final ISessionFetch tsf = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch();

        try
        {
            List<IDbSessionProvider> configsOut = (List<IDbSessionProvider>) tsf.get(dbSessionInfo,
                                                                                  null,
                                                                                  AbstractFetchApp.defaultBatchSize,
                                                                                  (ISessionOrderByType) null);
        	if(configsOut.isEmpty())
        	{
        		trace.warn("Could not find any sessions in the database that matched the input parameters.");
        		setExitCode(NO_ACTION);
        		return;
        	}

        	while (!configsOut.isEmpty()) {

        		if(doDeleteDatabase)
        		{

        			StringBuilder sb = new StringBuilder();
        			sb.append("The following sessions will be removed from the database:").append("\n")
                      .append("========================================").append("\n");
                    for (final IDbSessionProvider session : configsOut)
        			{
        				sb.append("Session Key=").append(session.getSessionId())
                          .append(" Name=").append(session.getName())
                          .append(" FullName=").append(session.getFullName())
                          .append("\n");
        			}
                    sb.append("========================================");
                    trace.info(sb.toString());
        		}

        		if (doDeleteFiles)
        		{

                    StringBuilder sb = new StringBuilder();
                    sb.append("The following session output directories will be removed from disk:").append("\n")
                      .append("========================================").append("\n");
                    for (final IDbSessionProvider session : configsOut)
        			{
        				sb.append(session.getOutputDirectory()).append("\n");
        			}
                    sb.append("========================================");
                    trace.info(sb.toString());
        		}

        		if (prompt)
        		{
        			final boolean doDelete = promptUser();

        			if(!doDelete)
        			{
        				trace.info("Action canceled.  No deletions will occur.");
        				setExitCode(NO_ACTION);
        				return;
        			}

        		}

        		if(doDeleteDatabase)
        		{

        			final long[] totals = tsr.removeContext(configsOut);

        			if(debug)
        			{
        				final List<String> tables = tsr.getTables();
        				for(int i=0; i < tables.size(); i++)
        				{
        					trace.debug("Removed " + totals[i] + " total " + tables.get(i) + " entries from the database.");
        				}
        			}
        		}

        		if (doDeleteFiles)
        		{
        			int deleteCount = 0;

                    for (final IDbSessionProvider session : configsOut)
        			{
        				final File outputDir = new File(session.getOutputDirectory());

        				if (! outputDir.exists())
        				{
        					trace.warn("Directory remove failed: The output " +
        							"directory \"" + session.getOutputDirectory() +
        							"\" for the session with key " +
        							session.getSessionId() + " does not exist.");
        				}
        				else if (! deleteAll(outputDir))
        				{
        					trace.warn("Directory remove failed: The output " +
        							"directory \"" + session.getOutputDirectory() +
        							"\" for the session with key " +
        							session.getSessionId() +
        					" could not be removed");
        				}
        				else
        				{
        					++deleteCount;
        				}
        			}

        			trace.debug("Removed " + deleteCount + " total session output directories.");
        		}
        		
                configsOut = (List<IDbSessionProvider>) tsf.getNextResultBatch();
        	}

        }
        catch(final Exception e)
        {
            trace.error("Exception encountered while removing test sessions: " + e.getMessage());
            setExitCode(OTHER_ERROR);
        }
        finally
        {
            tsf.close();
            tsr.close();
        }
    }






    private boolean promptUser()
    {
 
        trace.info("Are you sure you want to remove these test sessions? (y/n)");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        char answer = '\0';
        try
        {
            answer = (char)reader.read();
        }
        catch(final IOException e)
        {
            trace.error("Error reading user input.  No removal will be done.");
            answer = 'n';
            setExitCode(OTHER_ERROR);
        }

        System.out.println("\n");
        return(answer == 'Y' || answer == 'y');
    }

    @Override
    public void showHelp()
    {
        System.out.println(getUsage());
        final PrintWriter pw = new PrintWriter(System.out);
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printOptions(pw, 80, options, 7, 2);
        pw.flush();
        System.out
                .println("\nUse of the -" + ReservedOptions.TESTKEY_SHORT_VALUE + " option excludes use of any other test qualifiers.");
        System.out
                .println("Multiple query parameters will be ANDed together. Query values");
        System.out.println("are NOT case sensitive. All time values except SCLK should use");
        System.out.println("the format YYYY-MM-DDThh:mm:ss.ttt. Timezone for all times is GMT.");
        System.out.println("All string parameters whose long option name contains the word");
        System.out.println("\"Pattern\" may be entered using SQL pattern matching syntax such as");
        System.out.println("-" + ReservedOptions.TESTNAME_SHORT_VALUE + " %MyTestName%, which would find all tests with names");
        System.out.println("that contain the string \"MyTestName\"");
    }


    /**
     * Delete a file, and, if it is a directory, all files under it.
     *
     * @param file
     *
     * @return boolean True if everything was deleted
     */
    private boolean deleteAll(final File file)
    {
        final String path = file.getAbsolutePath();

        if (! file.exists())
        {
            trace.warn("File does not exist '" + path + "'");
            return false;
        }

        if (! file.isDirectory())
        {
            if (! file.delete())
            {
                trace.warn("Could not delete '" + path + "'");
                return false;
            }

            return true;
        }

        // Directory, so delete all files under it, and then the directory
        // itself. Try to delete as much as possible. If the directory cannot be
        // emptied, do not try to delete it.

        boolean ok = true;

        for (final File i : file.listFiles())
        {
            // The ordering here is important; we want to try to delete even if
            // previous deletes failed.

            ok = deleteAll(i) && ok;
        }

        // But here we try to delete the directory only if it is now empty

        if (ok && ! file.delete())
        {
            trace.warn("Could not delete '" + path + "'");

            ok = false;
        }

        return ok;
    }


    /**
     * The main method to run the application
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args)
    {
        SessionRemovalApp app = null;
        try {
            app = new SessionRemovalApp();
            final int status = app.runAsApp(args);
            System.exit(status);
        } catch (final Exception e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().error("Unexpected error: " + e.toString());
        }
        System.exit(app == null ? 1 : app.getExitCode());
    }
}
