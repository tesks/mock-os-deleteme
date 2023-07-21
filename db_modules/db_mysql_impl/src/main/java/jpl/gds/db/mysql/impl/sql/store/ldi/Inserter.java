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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.WrappedConnection;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.IAggregateStoreMonitor;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IInserter;
import jpl.gds.db.api.sql.store.ldi.InsertItem;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * Waits for a queue entry (or shutdown), creates LDI command, and executes it.
 *
 * When anyone needs to know the queue size, he calls our getQueueSize instead
 * of asking the _bq directly. The reason is that we want to report the number
 * of unhandled entries. In other words, if the queue has five entries and we
 * remove one, we want everyone to think that there are still five entries until
 * we have finished processing the one we removed.
 *
 * This is a thread. It traps all throwables in order to log.
 *
 * Detailed comments are mostly in-line.
 */
public class Inserter extends Thread implements Runnable, IInserter {
    private static final String                LDI0             = "LOAD DATA LOCAL INFILE '";
    private static final String                LDI0C            = "LOAD DATA CONCURRENT LOCAL INFILE '";
    // file name
    private static final String                LDI1             = "' INTO TABLE ";
    // table name
    private static final String                LDI2             = ((IDbSqlArchiveController.SQL_5_0_38)
            ? " CHARACTER SET latin1" : "") + " FIELDS TERMINATED BY ','" + " ESCAPED BY '\\\\'";
    private static final String                LDI3             = " (";
    // field list
    private static final String                LDI4             = ")";

    private final long                         DBCONFIG_SLEEP   = 250L;
    
    // MPCS-10410 : Add performance metric debug
    private static final float                 ONE_MILLION      = 1_000_000;
    private static final float                 ONE_BILLION      = 1_000_000_000;

    // Unbounded queue of files and table names and field lists and counts
    private final BlockingQueue<InsertItem>    _bq              = new LinkedBlockingQueue<InsertItem>();

    private WrappedConnection                  connection       = null;
    private int                                reportSize       = 0;
    private boolean                            shutDown         = false;

    // True if LDI files should be run as CONCURRENT
    private boolean                            concurrent       = false;

    // MPCS-7168 - Added members for performance tracking.
    private QueuePerformanceData               queuePerformance = null;
    private long                               high_water_mark  = 0;

    /**
     * 
     */
    private final IDbSqlArchiveController      archiveController;

    /**
     * 
     */
    private final IMySqlAdaptationProperties dbProperties;

    /**
     * 
     */
    private final StoreIdentifier              si;

    /**
     * LDI Inserter Tracer
     */
    private final Tracer                       ldiTracer;

    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param si
     *            the StoreIdentifier for insertion
     * @param logger
     *            The already instantiated LDI Tracer
     *
     */
    public Inserter(final ApplicationContext appContext, final StoreIdentifier si, final Tracer logger) {
        super("LDI-Inserter[" + si + "]");
        this.setDaemon(true);
        this.setPriority(Thread.MAX_PRIORITY);
        this.archiveController = appContext.getBean(IDbSqlArchiveController.class);
        this.dbProperties = appContext.getBean(IMySqlAdaptationProperties.class);
        this.concurrent = dbProperties.getConcurrentLDI();
        this.si = si;
        this.ldiTracer = logger;

        /*
         * MPCS-7168 -  Queue performance object must be created
         * after we get config so we know the RED/YELLOW health levels.
         *
         * MPCS-7198 - Added units to call below
         */
        queuePerformance = new QueuePerformanceData(appContext.getBean(PerformanceProperties.class),
                                                    "LDI Inserter (" + si.name() + ")", true,
                                                    dbProperties.getInserterQueueYellowLength(),
                                                    dbProperties.getInserterQueueRedLength(), "files");

        ldiTracer.debug("Concurrent LDI files: " + this.concurrent);
    }

    @Override
    public void startInserter() {
        super.start();
    }

    @Override
    public void interruptInserter() {
        super.interrupt();
    }

    /**
     * Run method for thread.
     */
    @Override
    public void run() {
        boolean warned = false;

        while (dbProperties == null) {
            if (!warned) {
                ldiTracer.warn("Inserter " + si.name() + " waiting for dbProperties");

                warned = true;
            }

            SleepUtilities.checkedSleep(DBCONFIG_SLEEP);
        }

        try {
            ldiTracer.debug("Creating connection for " + si.name());

            connection = new WrappedConnection(dbProperties, null, false, dbProperties.getReconnectAttempts(),
                                               dbProperties.getReconnectDelayMilliseconds());

            // We will close the connection ourselves
            connection.markControlled();
            internalRun();
        }
        catch (final Throwable t) {
            ldiTracer.error("Inserter " + si.name() + " dies: " + t);
            t.printStackTrace();
        }
        finally {
            if (connection != null) {
                connection.closeAtEnd();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#informShutDown()
     */
    @Override
    public synchronized void informShutDown() {
        shutDown = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#isStarted()
     */
    @Override
    public synchronized boolean isStarted() {
        return isAlive();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#getShutDown()
     */
    @Override
    public synchronized boolean getShutDown() {
        return shutDown;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#getQueueSize()
     */
    @Override
    public synchronized int getQueueSize() {
        return reportSize;
    }

    /**
     * Update the reported queue size to match the actual queue size
     *
     * See Class comments.
     */
    private synchronized void setQueueSize() {
        reportSize = _bq.size();

        /*
         * MPCS-7168 - Record queue high water mark for
         * performance reporting
         */
        high_water_mark = Math.max(reportSize, high_water_mark);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#add(jpl.gds.db.mysql.impl.sql.store.ldi.InsertItem)
     */
    @Override
    public void add(final InsertItem ii) {
        _bq.add(ii);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#size()
     */
    @Override
    public int size() {
        return _bq.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return _bq.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.ldi.IInserter#getPerformanceData()
     */
    @Override
    public List<IPerformanceData> getPerformanceData() {
        if (queuePerformance != null) {
            queuePerformance.setCurrentQueueSize(getQueueSize());
            queuePerformance.setHighWaterMark(high_water_mark);
            return Arrays.asList((IPerformanceData) queuePerformance);
        }
        else {
            return new LinkedList<IPerformanceData>();
        }
    }

    /**
     * Do actual work.
     */
    private void internalRun() {
        final StringBuilder sb = new StringBuilder();

        while (true) {
            // Set the reported queue size
            setQueueSize();

            // Wait a while for next element. It doesn't matter if we wake
            // up early. MPCS-7714  Don't use SleepUtilities.

            InsertItem element = null;

            try {
                element = _bq.poll(IDbSqlArchiveController.INSERTER_WAIT, TimeUnit.MILLISECONDS);
            }
            catch (final InterruptedException ie) {
                element = null;
            }
            final boolean gotElement = (element != null);

            if (getShutDown()) {
                // We're being asked to shut down; the queue should be empty

                ldiTracer.debug("Inserter " + si.name() + " notified of shut-down");

                final int size = getQueueSize() + (gotElement ? 1 : 0);

                if (size > 0) {
                    ldiTracer.error("Inserter " + si.name() + " shut down, but queue is of size " + size
                            + " instead of zero");
                }

                break;
            }

            if (!gotElement) {
                // Nothing to do
                continue;
            }

            final File file = element.getOne();
            final String fileName = file.getAbsolutePath();
            final String table = element.getTwo();

            if (!checkFileExistence(file, fileName)) {
                // Give up on this one
                continue;
            }

            // Build SQL statement
            sb.setLength(0);
            if (this.concurrent) {
                sb.append(LDI0C);
            }
            else {
                sb.append(LDI0);
            }

            sb.append(fileName); // File name (path)
            sb.append(LDI1);
            archiveController.getActualTableName(sb, table);
            sb.append(LDI2);

            if (IDbSqlArchiveController.USE_FIELDS) {
                sb.append(LDI3);
                sb.append(element.getThree()); // Field list
                sb.append(LDI4);
            }

            // Append set clause if there is one
            final String setClause = element.getSetClause();

            if (setClause != null) {
                sb.append(" ").append(setClause);
            }
            
            // MPCS-10410 : Add performance metric debug
            long aggregateRecordCount = 0;
            boolean isAggregate = false;
            IAggregateStoreMonitor storeMonitor = null;
            
            switch (si) {
                case ChannelAggregate:
                case HeaderChannelAggregate:
                case MonitorChannelAggregate:
                case SseChannelAggregate:
                    synchronized (archiveController.getStoreMonitor(si).getSyncMonitor()) {
                        storeMonitor = (IAggregateStoreMonitor) archiveController.getStoreMonitor(si);
                        aggregateRecordCount = storeMonitor.getReadyForInsertCount();
                        isAggregate = true;
                    }
                    break;
                default:
                    break;
            }
           
            final String sql = sb.toString();

            // Issue SQL statement and print errors and warnings
            boolean issued = false;

            if (!IDbSqlArchiveController.NO_LDI) {
                try {
                    
                    // MPCS-10410 : Add performance metric debug
                    final long sqlNumRec = element.getFour();
                    final long sqlStartTime = System.nanoTime();
                    ldiTracer.debug("LDI SQL START FOR TABLE '", table, "'"); 
                    connection.execute(sql);
                    final long sqlDuration = (System.nanoTime() - sqlStartTime);
                    issued = true;
                    if (isAggregate) {
                        ldiTracer.debug("LDI SQL END FOR TABLE '", table, "' : " 
                                , String.format("%.2f",(sqlDuration/ONE_MILLION)), " msecs for " 
                                , sqlNumRec, " aggregates, rate: " 
                                , String.format("%.2f", (sqlNumRec/(sqlDuration/ONE_BILLION)))
                                , " (aggregates/sec) and " 
                                , aggregateRecordCount, " channel records, rate: " 
                                , String.format("%.2f", (aggregateRecordCount/(sqlDuration/ONE_BILLION))) 
                                , " (rec/sec) : ", sql);
                    } else {
                        ldiTracer.debug("LDI SQL END FOR TABLE '", table, "' : " 
                                , String.format("%.2f",(sqlDuration/ONE_MILLION)), " msecs for " 
                                , sqlNumRec, " records, rate: " 
                                , String.format("%.2f", (sqlNumRec/(sqlDuration/ONE_BILLION))) 
                                , " (rec/sec) : ", sql);
                    }
                }
                catch (final DatabaseException de) {
                    ldiTracer.error("Unable to issue LDI '" + sql + "': " + de);
                }
                catch (final IllegalStateException e) {
                    ldiTracer.error("Inserter: " + this + ";" + e.getMessage(), e.getCause());
                    System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
                    System.out.println("Inserter: " + this);
                    e.printStackTrace(System.out);
                    // BufferedReader input = null;
                    // System.out.println(sql);
                    // try {
                    // input = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
                    // String line = input.readLine();
                    // while( line != null ) {
                    // System.out.println(line);
                    // line = input.readLine();
                    // }
                    // }
                    // catch (Exception e1) {
                    // // do nothing
                    // }
                    // finally {
                    // if (input != null) {
                    // try {
                    // input.close();
                    // }
                    // catch (IOException e1) {
                    // // do nothing
                    // }
                    // }
                    // }
                    System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                }
            }

            if (issued && !archiveController.isSaveFiles()) {
                boolean deleted = false;

                for (int i = 0; i < IDbSqlArchiveController.TRY_OPERATION; ++i) {
                    if (i > 0) {
                        SleepUtilities.checkedSleep(IDbSqlArchiveController.ONE_SECOND);
                    }

                    if (file.delete()) {
                        deleted = true;
                        break;
                    }
                }

                if (!deleted) {
                    ldiTracer.warn("Unable to delete '" + si.name() + "', file will probably be deleted at "
                            + "process exit");
                }
            }
        }
        ldiTracer.debug("Inserter " + si.name() + " exits");
    }

    /**
     * Check that the file exists and can be read. Try several times.
     *
     * @param file
     *            File to check for
     * @param name
     *            Name for logging
     *
     * @return True if everything is OK
     */
    private boolean checkFileExistence(final File file, final String name) {
        boolean exists = false;

        for (int i = 0; i < IDbSqlArchiveController.TRY_OPERATION; ++i) {
            if (i > 0) {
                // Sleep a short random time to break synchronization with other
                // processes

                SleepUtilities.randomSleep(IDbSqlArchiveController.ONE_SECOND);
            }

            exists = file.exists();

            if (exists) {
                break;
            }
        }

        if (!exists) {
            ldiTracer.error("Last-minute check failed, no such file '" + name + "'");
        }
        else if (!file.canRead()) {
            exists = false; // It's useless to us

            ldiTracer.error("Last-minute check failed, cannot read: " + archiveController.getFileStatus(file));
        }

        return exists;
    }
}