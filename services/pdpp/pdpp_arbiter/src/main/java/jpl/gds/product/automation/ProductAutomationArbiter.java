/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.product.automation;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.IAutomationLogger;
import jpl.gds.product.automation.hibernate.arbiter.workers.AbstractArbiterWorker;
import jpl.gds.product.automation.hibernate.arbiter.workers.ActionAssignerArbiterWorker;
import jpl.gds.product.automation.hibernate.arbiter.workers.ActionCreatorArbiterWorker;
import jpl.gds.product.automation.hibernate.arbiter.workers.LoadBalancerArbiterWorker;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationClassMapDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.processors.IProductAutomationProcessCache;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.HostPortUtility;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Main executable for the product automation.  This starts up all of the required arbiter worker threads that monitor
 * the state of the PDPP automation to get all data products processed.
 *
 * This is designed to be run as a system service.
 *
 *  MCSADAPT-180 - 12/3/2019 - This class was adapted from M20/MSL to allow for multimission PDPP.
 *  Comments were brought over intact for their historical value.
 *
 */
public class ProductAutomationArbiter extends AbstractCommandLineApp implements IQuitSignalHandler {

    private final IAutomationLogger log;

    // MPCS-8180 - 07/11/16  - removed property names, moved to config. changed getProperty queries to getters


    /** MPCS-4330 -  - Adding the product to the log messges that pertain to products.  Must also include the arbiter processor ID, so tha is currently
     *  always 0, but putting it here as a constant.
     */
    public static final Long ARBITER_PROCESS_ID = new Long(0);
    private static final Long ARBITER_SLEEP_TIME= new Long(10000);

    /**
     * MPCS-6544 -  9/2014 - Did a major refactor of the arbiter.  All of the work that the arbiter previously did
     * was off-loaded to a set of runnable workers.  The arbiter is now just the controlling object that monitors to make
     * sure all of the workers are still running and will clean up everything at exit.
     *
     * The arbiter table is going to be removed from the PDPP database as well, so all calls to that entity have been removed.  This
     * was created to make sure only a single arbiter instance was running at a time for any database, but it caused more problems than
     * it solved.  Since this will be run as a service this added steps are not needed.
     */

    private final Collection<AbstractArbiterWorker> workers;
    private boolean running;

    //  - 2/26/2013 - MPCS-4239 - Arbiter can get stuck assigning actions if there is a large back log.  This limits
    // the number of assigns per cycle.
    private final int maxArbiterAssigns = 100;

    private final int maxArbiterErrors;
    private final Long cycleTime;

    private final ApplicationContext appContext;

    private final ProductAutomationProperties config;
    private String dbHost;
    private String dbPort;


    /**
     * @param appContext
     *            The current application context
     * @throws AutomationException
     */
    public ProductAutomationArbiter(final ApplicationContext appContext) throws AutomationException {
        workers = new ArrayList<AbstractArbiterWorker>();
        running = false;

        this.appContext = appContext;
        this.config = appContext.getBean(ProductAutomationProperties.class);
        this.log = appContext.getBean(IAutomationLogger.class);

        maxArbiterErrors = config.getMaxArbiterErrors();
        cycleTime = config.getActionCategorizerCycleTimeMS();

    }

    @Override
    public void exitCleanly() {
        try {
            shutdown();
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     * @see jpl.gds.shared.cli.AbstractCommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }

        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        final DatabaseCommandOptions dbOptions = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
        options.addOption(dbOptions.DATABASE_HOST);
        options.addOption(dbOptions.DATABASE_PORT);

        return options;
    }

    /*
     * (non-Javadoc)
     * @see jpl.gds.shared.cli.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final OptionSet options = createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [options]");

        pw.println("                   ");
        pw.println("Starts a process that will communicate with the Post Downlink Product Processing database to find dataproducts ");
        pw.println("that require processing.  This process will spawn child processes and assign work based on the processing type ");
        pw.println("and the Dictionary version of the product.");
        pw.println("To exit the process use ctrl-C if the process was not started as a background process.  Starting and stopping of this ");
        pw.println("process can also be handled using the corresponding GUI control tool.  Look at release notes for the specific name of the tool.");
        pw.println("                   ");

        options.printOptions(pw);

        pw.close();
    }

    /**
     * MPCS-6469  9/2014 - Creating a metadata object from the emd files is slow and expensive and not necessary.
     * Everything required is in the product object and the processes will need to parse the EMD file no matter what so it
     * is wasted work.  Taking this out.
     */
    public void run() {
        // This still needs to start as early as possible
        final AutomationSessionFactory sessionFactory = appContext.getBean(AutomationSessionFactory.class);
        sessionFactory.rebuildSessionFactory(appContext, dbHost, dbPort, null);

        // MPCS-8180 07/26/16 Moved properties to config as per TODO that was here
        final int transactionBlockSize = config.getTransactionBlockSize();
        final int deadTime = config.getProcessDeadTime();
        final int loadDiff = config.getLoadDifference();


        /**
         * The action creator creates all actions for new products.  The load balancer monitors dead processes and will take
         * away actions from dead processes and kill the processes.  It will also work to keep the load on parallel processes
         * balanced when the difference between the processes back log of actions is greater than loadDiff.
         *
         * Each action type (PDPP type) has an action assigner assigned to it.  It will start processes up to the max
         * parallel instances as set in the configuration and assign actions to them.  It will pick up actions that have
         * been marked as reassign by the load balancer as well and will create reassigned statuses to track the reassignment.
         *
         * All of the workers are created as daemon threads so no join is done on them before we exit.  It may be that we want to
         * do this in the future if we notice strange artifacts from these threads going down in the middle of something.
         */
        workers.add(new ActionCreatorArbiterWorker(transactionBlockSize, maxArbiterErrors, cycleTime, appContext));
        workers.add(new LoadBalancerArbiterWorker(deadTime, loadDiff, maxArbiterErrors, cycleTime, appContext));

        // Need an assigner for each type of PDPP.
        for (final ProductAutomationClassMap actionName : appContext.getBean(ProductAutomationClassMapDAO.class).getClassMaps()) {
            workers.add(new ActionAssignerArbiterWorker(actionName, transactionBlockSize, maxArbiterAssigns, maxArbiterErrors, cycleTime, dbHost, appContext));
        }

        // Start all of the threads.
        for (final Thread t : workers) {
            t.start();
        }

        running = true;

        // Make sure all of the threads are still running.
        while (running) {
            for (final AbstractArbiterWorker t : workers) {
                if (!t.isAlive()) {
                    log.error(String.format("Arbiter worker %s has died.  Check logs for errors.", t.getWorkerName()),
                            ARBITER_PROCESS_ID);
                    shutdown();
                    break;
                }
            }

            /**
             * Changed this because in previous development I learned that waiting on an
             * object automatically releases that object, so you should only wait / notify
             * on this.
             */
            synchronized(this) {
                try {
                    wait(ARBITER_SLEEP_TIME);
                } catch (final InterruptedException e) {}
            }
        }
    }

    private void shutdown() {
        running = false;

        synchronized(this) {
            notifyAll();
        }

        for (final AbstractArbiterWorker t : workers) {
            t.shutdown();
        }

        for (final AbstractArbiterWorker t : workers) {
            try {
                t.join();
            } catch (final InterruptedException e) {
                // whatever.
            }
        }

        // Kill all the processes in the process cache.
        appContext.getBean(IProductAutomationProcessCache.class).killAllProcesses();
    }

    /*
     * (non-Javadoc)
     * @see jpl.gds.shared.cli.AbstractCommandLineApp#configure(jpl.gds.shared.cli.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        final DatabaseCommandOptions dbOpts = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));

        dbHost = dbOpts.DATABASE_HOST.parse(commandLine, false);

        if (dbHost == null) {
            dbHost = config.getDatabaseHost() != null ?
                    config.getDatabaseHost() : HostPortUtility.getLocalHostName();
        }

        final UnsignedInteger port = dbOpts.DATABASE_PORT.parse(commandLine, false);
        dbPort = port != null ?
                port.toString() : config.getDatabasePort();
    }


    /**
     * @param args
     * @throws InterruptedException
     * @throws RuntimeException
     * @throws ProductException
     * @throws IOException
     * @throws ParseException
     */
    public static void main(final String[] args) throws InterruptedException, ProductException, RuntimeException, IOException, ParseException  {

        ProductAutomationArbiter categorizer = null;
        final ApplicationContext appContext = SpringContextFactory.getSpringContext(true);

        try {
            categorizer = new ProductAutomationArbiter(appContext);
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Could not start daemon process");

            System.exit(1);
        }

        final ICommandLine commandLine = categorizer.createOptions().parseCommandLine(args, false);

        categorizer.configure(commandLine);



        if (!appContext.getBean(AutomationSessionFactory.class).DATABASE_STATUS) {
            // Using log.fatal() here causes ProductAutomationLogsDAO to try and rebuild the session
            // This means an extra stack trace is dumped from the failed session rebuild. We want to exit here instead
            TraceManager.getDefaultTracer()
                    .error("AutomationSessionFatory - Error Creating SessionFactory! Shutting down ...");

            System.exit(1);
        }

        try {
            categorizer.run();
            System.exit(0);
        } catch (final Exception e) {
            TraceManager.getDefaultTracer()
                    .error("Uncaught exception encountered.  Exiting arbiter process: " + e.getMessage(), e);
            e.printStackTrace();
            System.exit(1);
        }
    }


}