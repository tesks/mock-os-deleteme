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
import java.util.TimeZone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.TraceManager;

/**
 * The purpose of this class is to take a given set of session configuration
 * information and insert it into the database session configuration table.
 *
 */
public class SessionConfigInsertApp extends AbstractDatabaseApp
{
	private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_session_insert");

	/**
	 * The internal representation of the test configuration to store
	 */
    private SessionConfiguration testConfig;

	/**
	 * Parameter indicating whether the test key should be written back to the test config
	 * file after the test config has been inserted into the database
	 * (true will write the key back, false will not)
	 */
    private boolean updateConfig;

    /**
     * True if the test key should be written to the console, false otherwise
     */
    private boolean report;

    /**
     * Creates an instance of SessionConfigInsertApp.
     */
    public SessionConfigInsertApp()
    {
        super(APP_NAME);
    }

    //command line parameter constants
    private static final String UPDATE_CONFIG_SHORT = "u";
    /** update config long option */
    public static final String UPDATE_CONFIG_LONG = "updateConfig";
    private static final String PRINT_SHORT = "p";
    private static final String PRINT_LONG = "print";

    @Override
    protected void addAppOptions()
    {
        addOption(ReservedOptions.getOption(ReservedOptions.TESTCONFIG_SHORT_VALUE));
        addOption(UPDATE_CONFIG_SHORT,UPDATE_CONFIG_LONG, null,"update session configuration file with DB information");
        addOption(PRINT_SHORT,PRINT_LONG, null,"display session database key to stdout");
        addOption(ReservedOptions.DATABASE_HOST);
        addOption(ReservedOptions.DATABASE_PORT);
    }

    @Override
    public void configureApp(final CommandLine commandLine) throws ParseException
    {
        super.configure(commandLine);

        testConfig = new SessionConfiguration(appContext);

        // Load test config file
        if (commandLine.hasOption(ReservedOptions.TESTCONFIG_SHORT_VALUE))
        {
            final String testConfigFile = commandLine.getOptionValue(ReservedOptions.TESTCONFIG_SHORT_VALUE);
            final File testFile = new File(testConfigFile);
            if (!testFile.exists())
            {
                throw new ParseException("Session configuration file " + testFile + " does not exist");
            }
            testConfig.load(testConfigFile);
        }
        else
        {
            throw new ParseException("You must supply a session configuration file");
        }

        // Update config file option
        if (commandLine.hasOption(UPDATE_CONFIG_SHORT)) {
            updateConfig = true;
        }

        // Report to console option
        if (commandLine.hasOption(PRINT_SHORT)) {
            report = true;
        }
        
        ReservedOptions.parseDatabaseHost(commandLine, false);
        ReservedOptions.parseDatabasePort(commandLine, false);
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        setExitCode(SUCCESS);
        IDbSqlArchiveController archiveController = appContext.getBean(IDbSqlArchiveController.class);

        archiveController.addNeededStore(StoreIdentifier.Session);
        archiveController.init();

        final boolean ok = archiveController.startSessionStoresWithoutInserting();
        if (!ok) {
            trace.error("Unable to start session database store");
            return;
        }

        final ISessionStore tss = archiveController.getSessionStore();
    	tss.start();
    	
        if (! tss.isConnected())
        {
            trace.error("Could not start test session store");
            return;
        }

        IHostStore hs = null;

    	try
		{
            hs = archiveController.getHostStore();

    		// Reset instance-unique fields, so we get a new test session record.
    		testConfig.getContextId().clearFieldsForNewConfiguration();
			tss.insertTestConfig(testConfig);

            if (dbProperties.getExportLDIAny())
            {
                // Create LDI file and write it directly to the export
                // directory. It is needed when exporting.
                hs.writeLDI(testConfig);
                tss.writeLDI(testConfig);
            }
		}
        catch (final DatabaseException e)
		{
			trace.error("Could not insert session configuration into database: " + e.getMessage());
			setExitCode(OTHER_ERROR);
		}
        finally
        {
            if (hs != null)
            {
                hs.close();
            }
        }

		tss.stop();

		//NOTE: The test key value is automatically set on the input TestConfiguration object
		//when it is inserted into the database

		if(updateConfig)
		{
			try
			{
				testConfig.save();
			}
			catch(final IOException ioe)
			{
				trace.error("I/O Exception while writing session configuration file: " + ioe.getMessage());
				setExitCode(OTHER_ERROR);
			}
		}

		if(report)
		{
                    // OK to report as non-trace because it is part of requested user output
		    System.out.println("Database Key: " + testConfig.getContextId().getNumber().toString());
		}
    }

    /**
     * Main entry method
     * @param args command line arguments
     */
    public static void main(final String[] args)
    {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        SessionConfigInsertApp app = null;
        try {
            app = new SessionConfigInsertApp();
            final int status = app.runAsApp(args);
            System.exit(status);
        } catch (final Exception e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().fatal("Unexpected error: " + e.toString());
        }
        System.exit(app.getExitCode());
    }

	/**
     * Get is-report state
     *
	 * @return Returns the report.
	 */
	public boolean isReport()
	{
		return report;
	}

	/**
     * Get test configuration.
     *
	 * @return Returns the sessionConfig.
	 */
	public SessionConfiguration getTestConfig()
	{
		return testConfig;
	}

	/**
     * Get update configuration state.
     *
	 * @return Returns the updateConfig.
	 */
	public boolean isUpdateConfig()
	{
		return updateConfig;
	}

	@Override
	public String getUsage()
	{
		return(appName + "--" + ReservedOptions.TESTCONFIG_LONG_VALUE + " <filename> [--" +
                        UPDATE_CONFIG_LONG + " --" + PRINT_LONG +"]");
	}
	
    /**
     * Getter for application context
     * 
     * @return ApplicationContext object
     */
    public ApplicationContext getAppContext() {
        return appContext;
    }
	
}
