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
package jpl.gds.automation.inbox;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import jpl.gds.automation.inbox.InboxProperties.RunConfig;
import jpl.gds.cli.legacy.app.AbstractCommandLineApp;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;


/**
 * The DownlinkInboxMonitorApp is the command line interface to the DownlinkInboxMonitor.
 */
public class DownlinkInboxMonitorApp extends AbstractCommandLineApp
{
    /**
     * Name of the command line script which starts this process
     */
    public static final String APP_NAME = ApplicationConfiguration.getApplicationName();

    private static Tracer log = TraceManager.getDefaultTracer();


    private final String procName = APP_NAME; 

    /**
     * constructor
     */
    public DownlinkInboxMonitorApp()
    {
        super();

    }

    /**
     * Run a DownlinkDirectoryMonitor for each [valid] RunConfig defined in the
     * configuration file.
     * 
     * @param sConfig2
     */
    private void start(final InboxProperties sConfig)
    {
        final List<DownlinkInboxMonitor> monitors = new ArrayList<>();
        DownlinkInboxMonitor monitor;
        RunConfig rc;

        final String[] configurations = sConfig.getMonitorList();
        for (int i = 0; i < configurations.length; i++) // a test configuration:
                                                        // i=configurations.length)
        {
            if ((rc = sConfig.load(i)) == null)
            {
                log.error(procName + " " + configurations[i]
                        + " was not correctly setup/loaded from the inbox configuration properties.  This process will terminate.");

                continue;
            }

            monitor = new DownlinkInboxMonitor(rc);
            if (monitor.inboxMonitorConfigured())
            {
                monitors.add(monitor);
                monitor.monitorInbox();
            } else
            {
                log.error(procName + " Not able to run "
                        + rc.getActiveConfigBlockName());
            }
        }
    }

    /**
     * Main entry point.
     * 
     * @param args
     *            Command line arguments from the user
     */
    public static void main(final String[] args)
    {
        try
        {
            final DownlinkInboxMonitorApp app = new DownlinkInboxMonitorApp();

            final CommandLine commandLine = ReservedOptions.parseCommandLine(args,
                    app);
            app.configure(commandLine);

            final InboxProperties sConfig = new InboxProperties();
            sConfig.init();

            if (sConfig.isInboxesDefined()) {
                app.start(sConfig);
            } else {
                log.error(APP_NAME + "Configuration value " + InboxProperties.INBOXES_EXIST
                        + " is set to false, or no inboxes are defined in the configuration.");
                log.error(
                        APP_NAME + "To configure this application, you must enable inbox usage in your project configuration file "
                                + " by setting " + InboxProperties.INBOXES_EXIST
                                + " to true and also define inbox names and their associated directories");
                log.error(APP_NAME + "Will now terminate");
                System.exit(1);
            }
        } catch (final Exception e)
        {
            log.error(e.getMessage() == null ? e.toString() : e.getMessage());
            System.exit(1);
        }

    }

    /******************** [begin] command line processing ******************/

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp()
    {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final Options options = createOptions();
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(70, APP_NAME + "\n", null, options, null);
    }

    /******************** [end] command line processing ******************/

} // end of class DownlinkInboxMonitorApp

