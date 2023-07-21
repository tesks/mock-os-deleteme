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
package jpl.gds.db.mysql.impl.sql.store.ldi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.IAggregateStoreMonitor;
import jpl.gds.db.api.sql.store.IStoreConfiguration;
import jpl.gds.db.api.sql.store.IStoreConfigurationMap;
import jpl.gds.db.api.sql.store.IStoreMonitor;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IGatherer;
import jpl.gds.db.api.sql.store.ldi.InsertItem;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.types.Pair;

/**
 * Wake up every so often and gather the open streams and queue them to the
 * inserter.
 *
 * This is a thread, and run as a singleton. It traps all throwables in
 * order to log.
 *
 * Detailed comments are mostly in-line. See also class comments.
 *
 * The logic involving _gatherer_flush requires an explanation. We want to
 * shut down cleanly, which means that the gatherer must flush out any open
 * streams, even if it is "too soon" or the count is not large enough. So we
 * set _gatherer_flush true, and interrupt the gatherer. We want to clear
 * _gatherer_flush to inform the shutdown logic that the flush has indeed
 * taken place.
 *
 * But we only want to clear _gatherer_flush if it has flushed all tables,
 * so we make a local copy in local_flush, and then clear _gatherer_flush at
 * the end if local_flush is true. This eliminates the possibility of a
 * partial flush when _gatherer_flush happens to become set in the middle of
 * processing.
 *
 * To prevent delays in displaying commands, we accept a wakeup if there are
 * any such to go out. This is OK as there are usually very few commands and
 * statuses.
 */
public class Gatherer extends Thread implements Runnable, IGatherer {

    /**
     * 
     */
    private final IDbSqlArchiveController     archiveController;
    private final IStoreConfigurationMap    storeConfigMap;
    private final Tracer trace;

    /**
     * @param appContext The Spring Application Context
     */
    public Gatherer(final ApplicationContext appContext) {
        super("LDI-Gatherer");
        this.setDaemon(true);
        this.setPriority(Thread.MAX_PRIORITY - 1);
        this.archiveController = appContext.getBean(IDbSqlArchiveController.class);
        this.storeConfigMap = appContext.getBean(IStoreConfigurationMap.class);
        this.trace = TraceManager.getTracer(appContext, Loggers.LDI_GATHERER);
    }

    /**
     * Run method for thread that catches everything.
     */
    @Override
    public void run() {
        try {
            internalRun();
        }
        catch (final Throwable t) {
            trace.error("Gatherer dies: ", t);
            t.printStackTrace();
        }
    }

    /**
     * Real run method for thread.
     *
     * Normally we just sleep, and then process any non-empty streams. But
     * if any stream gets too big, an interrupt is issued. But that will
     * cause the smaller streams to be forced out, and if we get a lot of
     * interrupts, a lot of very small files. So on interrupt we generally
     * want to process only the large streams.
     *
     * However, if we get a lot of interrupts the effect will be to stop the
     * sleeps in the middle, which then means that we will never do the
     * smaller streams at all. So checks are added to monitor the progress
     * of time whether or not we get interrupts, and then process all
     * streams as if the sleeps finished normally.
     *
     * Oh, and we are also interrupted when flushing.
     *
     * @version MPCS-7714 Refactor.
     */
    private void internalRun() {
        /*
         * MPCS-7155 - This never changes so moved it outside
         * the loop.
         */
        final long ft = archiveController.getFlushTime();
        long desired = ft + System.currentTimeMillis();

        while (true) {
            Pair<File, FileOutputStream> metadataStream = null;
            Pair<File, FileOutputStream> valueStream = null;
            long count = 0L;
            long count_body = 0L;
            boolean local_flush = false;

            /*
             *MPCS-7155. Previous logic for computing sleep time
             * could often come up with "0" and make this loop spin. There
             * is no performance advantage for anything other than a
             * straight sleep for flush time interval here.
             */

            long now = System.currentTimeMillis();
            long delta = Math.max(desired - now, 0L);

            try {
                if (delta > 0L) {
                    Thread.sleep(delta);

                    now = desired;
                    delta = 0L;
                }

                trace.trace("Gatherer wakes up after sleep");

            }
            catch (final InterruptedException ie) {
                now = System.currentTimeMillis();
                delta = Math.max(desired - now, 0L);

                trace.trace("Gatherer wakes up on interrupt", ie);

            }

            // We must grab this here to insure consistent processing
            final boolean flush = archiveController.isGathererFlushing();

            if (flush) {
                trace.trace("Gatherer gets flush");
            }

            long minimum = 0L;

            if (delta == 0L) {
                // We "slept" long enough, set up to sleep again
                // and do all non-empty streams
                desired = ft + now;
            }
            else if (!flush) {
                // We haven't "slept" long enough yet, so just do large
                // streams,
                // unless we're flushing.
                minimum = archiveController.getLdiRowLimit();
            }

            /*
             * MPCS-715Check to see if there are any
             * non-empty streams to close and send to the Inserters.
             */
            boolean enoughToFlush = false;
            for (final StoreIdentifier si : StoreIdentifier.values()) {
                final IStoreMonitor monitor = archiveController.getStoreMonitor(si);
                if (monitor == null) {
                    continue;
                }
                enoughToFlush |= monitor.hasEnoughToFlush(minimum);
            }

            /*
             * MPCS-7155 - Only need to do something if we are
             * flushing or one of the streams has records in it.
             */
            if (!flush && !enoughToFlush) {
                // Spurious wakeup; not a problem
                continue;
            }

            // We accept the wakeup
            local_flush = flush;

            /*
             * MPCS-7155 - Wrapped each sync block below with
             * the flag indicating whether there is anything in the stream
             * to avoid sending empty files to the Inserters.
             */

            // Look for activity in streams
            for (final StoreIdentifier si : StoreIdentifier.values()) {
                final IStoreMonitor monitor = archiveController.getStoreMonitor(si);
                if (monitor == null) {
                    continue;
                }
                if (monitor.hasEnoughToFlush(minimum)) {
                    synchronized (monitor.getSyncMonitor()) {
                        
                        // MPCS-10410: Add performance metric debug logs...
                        // If this is an Aggregate Store Monitor, set the ready for insert count
                        // to the in progress count and clear the in progress count to prepare
                        // for the next LDI file. Using the Store Monitor to communicate 
                        // about channel records contained within aggregates between the 
                        // Gatherer and Inserter. The value stream count_body set below would
                        // be the number of aggregates contained in the LDI file.
                        if (monitor instanceof IAggregateStoreMonitor) {
                            final long inProgressCount = ((IAggregateStoreMonitor)monitor).getInProgressRecordCount();
                            ((IAggregateStoreMonitor)monitor).setReadyForInsertCount(inProgressCount);
                            ((IAggregateStoreMonitor)monitor).clearInProgressRecordCount();
                        }
                        
                        metadataStream = monitor.getMetadataStream();
                        count = monitor.getMetadataInStream();
                        monitor.clearMetadataInStream();
                        monitor.setMetadataStream(null);

                        valueStream = monitor.getValueStream();
                        count_body = monitor.getValuesInStream();
                        monitor.clearValuesInStream();
                        monitor.setValueStream(null);
                    }
                    final IStoreConfiguration storeConfig = this.storeConfigMap.get(si);
                    
                    if (valueStream != null) {
                        enqueue(monitor, valueStream, storeConfig.getValueTableName(), storeConfig.getValueFields(), storeConfig.getSetClause(), count_body);
                    }

                    if (metadataStream != null) {
                        enqueue(monitor, metadataStream, storeConfig.getMetadataTableName(), storeConfig.getMetadataFields(), null, count);
                    }
                }
            }

            if (local_flush) {
                // Flag that flushing has been accomplished
                archiveController.setGathererFlushing(false);
                break;
            }
        }
        trace.debug("Gatherer exits");
    }

    /**
     * Utility method to queue to inserter. The stream is closed (and flushed),
     * and the entry created and queued. Note that the input pair and the queued
     * pair are not the same.
     *
     * @param fos
     *            File and associated stream
     * @param table
     *            Database table name
     * @param fields
     *            Field list
     * @param setClause
     *            Extra clause
     * @param count
     *            Insert count
     * @param export
     *            True if exporting is possible for this table
     * @param inserter
     *            Inserter to queue to
     */
    private void enqueue(final IStoreMonitor monitor, final Pair<File, FileOutputStream> fos, final String table, final String fields, final String setClause, final long count) {
        final File file = fos.getOne();
        final String name = file.getAbsolutePath();
        
        try {
            // Flush and close
            fos.getTwo().close();
        }
        catch (final IOException ioe) {
            trace.error("Unable to close '" + name + "': ", ioe);
            return;
        }

        if (monitor.isExport()) {
            // Hard link the LDI file to the export directory

            final String linked = archiveController.getExportDirectory() + File.separator + file.getName();
            final String[] command = new String[] { "/bin/ln", name, linked };

            try {
                final int status = ProcessLauncher.launchSimple(command);

                if (status == 0) {
                    trace.debug("Exported '", name, "' as '", linked, "'");
                }
                else {
                    trace.error("Unable to export '", name, "' as '", linked, "': ", status);
                }
            }
            catch (final IOException ioe) {
                trace.error("Unable to export '", name, "' as '", linked, "': ", ioe.getMessage(), ioe.getCause());
            }
        }

        // Queue entry to inserter
        monitor.getInserter().add(new InsertItem(file, table, fields, count, setClause));
        trace.debug("Queuing '", name, "' for ", table, " with ", count, " rows to ", monitor.getSi());
    }
}