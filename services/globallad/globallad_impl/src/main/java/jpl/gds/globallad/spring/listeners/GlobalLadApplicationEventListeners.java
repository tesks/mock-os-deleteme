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
package jpl.gds.globallad.spring.listeners;

import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.storage.DataInsertionManager;
import jpl.gds.globallad.data.storage.GlobalLadDataStore;
import jpl.gds.globallad.io.GlobalLadDataMessageConstructor;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.globallad.spring.beans.BeanNames;
import jpl.gds.globallad.spring.beans.GlobalLadDataSourceProvider;
import jpl.gds.globallad.spring.cli.GlobalLadCommandLineParser;
import jpl.gds.globallad.workers.GlobalLadPersister;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A set of methods set up as Spring listeners.  These are used to start and stop the global lad servers / processes
 * once everything is initialized as well as tearing down these processes once the application has been terminated.
 * <p>
 * This class uses all autowiring since it would never be used outside of a spring application.
 */
@Component
@Scope(value = "prototype")
public class GlobalLadApplicationEventListeners {

    /**
     * This is a prototype so it will only be created when it is being used, but still only going to auto wire the
     * components that are always used.
     */
    @Autowired
    private GlobalLadDataSourceProvider provider;
    private IGlobalLadDataSource        dataSource;

    @Autowired
    private DataInsertionManager inserter;

    @Autowired
    @Qualifier(BeanNames.GLAD_EXECUTOR)
    private ExecutorService executorService;

    private Future<?> dataSourceTask;

    /**
     * Shuts down all services one the Spring context is closed, meaning the application has shut down.
     *
     * @param evt closed event.
     * @throws InterruptedException
     * @throws IOException
     */
    @EventListener
    public void stopLadServers(final ContextClosedEvent evt) throws
                                                             InterruptedException,
                                                             IOException,
                                                             ExecutionException {
        TraceManager.getTracer(Loggers.GLAD).debug("Interrupting the data source.");
        executorService.shutdownNow();

        if (dataSource != null) {
            dataSource.close();
        }

        if (dataSourceTask != null && !dataSourceTask.isDone()) {
            dataSourceTask.get();
        }

        TraceManager.getTracer(Loggers.GLAD).debug("Shutting down the global lad data inserter manager.");
        inserter.stop();

        final ScheduledExecutorService worker = evt.getApplicationContext()
                .getBean(BeanNames.GLAD_WORKER_EXECUTOR, ScheduledExecutorService.class);
        worker.shutdown();
    }

    /**
     * Once the spring application has been fully initialized this will start the global lad socket server and the main
     * global lad data store.  It will also attempt to initialize the data store from the backup if necessary.
     *
     * @param evt ready event.
     * @throws GlobalLadException   Socket server was not able to start
     * @throws InterruptedException
     */
    @EventListener
    public void startUpServers(final ApplicationReadyEvent evt) throws Exception {
        dataSource = provider.getDataSource();
        TraceManager.getTracer(Loggers.GLAD).debug("Starting the data source: ");
        dataSourceTask = executorService.submit(dataSource);

        /**
         * MPCS-8126 - triviski 4/21/2016 - I tried to do this a better way, but
         * there just doesn't seem to be one.  Adding a short delay here to let the
         * server start up before checking if it is alive.  If this fails we
         * close the context which will shutdown the application.
         */

        /**
         * MPCS-12089 - refactored to use an executor service
         */
        try {
            dataSourceTask.get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // timeout waiting for thread to stop, should mean the service is alive and well
        }

        if (dataSourceTask.isDone()) {
            TraceManager.getTracer(Loggers.GLAD).error("Failed to start data source.  Shutting down application.");
            final ConfigurableApplicationContext c = evt.getApplicationContext();
            c.close();
        } else {

            /**
             * MPCS-8126 triviski 4/21/2016 - Starting the inserter once the application is ready.
             */
            inserter.start();

            doPersist(evt.getApplicationContext());
        }
    }

    /**
     * Finds the back up file to load into the lad if it has been configured to restore from backup.  Also adds the
     * persister to the global lad worker executor if persistence has been enabled.
     */
    private void doPersist(final ApplicationContext ctx) {
        final ScheduledExecutorService   worker    = ctx
                .getBean(BeanNames.GLAD_WORKER_EXECUTOR, ScheduledExecutorService.class);
        final GlobalLadCommandLineParser cli       = ctx
                .getBean(BeanNames.GLAD_COMMAND_LINE_OVERRIDES, GlobalLadCommandLineParser.class);
        final GlobalLadProperties        config    = ctx.getBean(BeanNames.GLAD_CONFIG_NAME, GlobalLadProperties.class);
        final GlobalLadDataStore         lad       = ctx.getBean(BeanNames.GLAD_DATA_STORE, GlobalLadDataStore.class);
        GlobalLadPersister               persister = null;


        /**
         * Check if persistance is enabled, meaning periodic backups of the global lad will be
         * created, or the command line restore from backup flag is set. If either case is true,
         * we must create a persister.
         */
        if (cli.restoreFromBackup || config.isPersistenceEnabled()) {
            persister = new GlobalLadPersister(lad.getMasterContainer(), config,
                    TraceManager.getTracer(ctx, Loggers.GLAD));

            if (!persister.init()) {
                TraceManager.getTracer(ctx, Loggers.GLAD).error("Failed to initialize global lad persister.");
            } else {
                /**
                 * To restore from a file the entire server must be running.
                 */
                if (persister != null) {
                    /**
                     * Figure out the dump file situation to use.  It will be from the cli, or
                     * we get the latest backup from the persister.  If no file has been supplied
                     * on the command line and no backup file was found in the persistence directory,
                     * no initialization from backup will be done.
                     */
                    final File df = cli.backupFile == null ?
                            persister.getNewestBackup() : new File(cli.backupFile);

                    if (df != null) {
                        inserter.initializeLadFromBackup(df, true);
                    }
                }
            }
        }

        /**
         * Check to see if persistence is enabled, which will backup the lad.  If so add to the
         * worker executor.
         */
        if (persister != null && config.isPersistenceEnabled()) {
            worker.scheduleWithFixedDelay(persister,
                    config.getPersistenceIntervalSeconds(), // Delay before starting.
                    config.getPersistenceIntervalSeconds(),
                    TimeUnit.SECONDS);
        }
    }
}
