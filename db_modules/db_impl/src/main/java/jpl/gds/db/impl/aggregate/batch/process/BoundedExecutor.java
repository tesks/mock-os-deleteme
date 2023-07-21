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
package jpl.gds.db.impl.aggregate.batch.process;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.IBoundedExecutor;

/**
 * This class functions as a gate between the Query Stream Processor and
 * Batch Processor Thread Pool. Controls the task submission flow based on the
 * specified bound parameter. Without this gate the Query Stream Processor will
 * overrun the Batch Processor.
 *
 */
public class BoundedExecutor implements IBoundedExecutor {
    private final Executor exec;
    private final Semaphore semaphore;

    /**
     * Constructor
     * 
     * @param exec the Executor service
     * @param bound the bound used to control task submission
     */
    public BoundedExecutor(final Executor exec, final int bound) {
        this.exec = exec;
        this.semaphore = new Semaphore(bound);
    }

    @Override
    public void submitTask(final Runnable command) throws AggregateFetchException {
        
        try {
            semaphore.acquire();
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } finally {
                        semaphore.release();
                    }
                }
            });
        } catch (final RejectedExecutionException e) {
            semaphore.release();
            throw new AggregateFetchException("Bounded Executor encountered an unexpected RejectedExecutionException: " + e.getMessage());
        } catch (final InterruptedException e) {
            throw new AggregateFetchException("Bounded Executor encountered an unexpected InterruptedException: " + e.getMessage());
        }
    }
}

