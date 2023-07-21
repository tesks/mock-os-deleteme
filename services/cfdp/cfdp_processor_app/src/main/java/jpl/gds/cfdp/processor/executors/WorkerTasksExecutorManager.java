/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.executors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

@Service
@DependsOn("configurationManager")
public class WorkerTasksExecutorManager {

    @Autowired
    ApplicationContext                     appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    private final ExecutorService nonScheduledExecutorService = Executors.newCachedThreadPool();

    // IMPORTANT: Increase the below pool size if new tasks are introduced
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    private final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();

    private final Tracer log;

    public WorkerTasksExecutorManager() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    /**
     * @return the nonScheduledExecutorService
     */
    public ExecutorService getNonScheduledExecutorService() {
        return nonScheduledExecutorService;
    }

    /**
     * @return the scheduledExecutorService
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public void addScheduledFuture(final ScheduledFuture<?> future) {
        scheduledFutures.add(future);
    }

    public void shutdown() throws InterruptedException {

        // System.out.println("Shutting down Non-Scheduled Worker Tasks Executor");
        getNonScheduledExecutorService().shutdown();

        // Wait for non-scheduled worker tasks executor to really terminate
        if (!getNonScheduledExecutorService()
                .awaitTermination(configurationManager.getWorkerTasksExecutorShutdownTimeoutMillis(), MILLISECONDS)) {

            // Non-Scheduled worker tasks executor did not terminate in time, so force
            // shutdown
            log.error("Non-scheduled Worker Tasks Executor did not shut down in time so forcing shutdown");
            getNonScheduledExecutorService().shutdownNow();

            // Wait just one more time
            if (!getNonScheduledExecutorService()
                    .awaitTermination(configurationManager.getWorkerTasksExecutorShutdownTimeoutMillis(), MILLISECONDS))
                log.error("Non-Scheduled Worker Tasks Executor failed to terminate");
        }

        for (final ScheduledFuture<?> future : scheduledFutures) {
            future.cancel(true);
        }

        // System.out.println("Shutting down Scheduled Worker Tasks Executor");
        getScheduledExecutorService().shutdown();

        // Wait for scheduled worker tasks executor to really terminate
        if (!getScheduledExecutorService()
                .awaitTermination(configurationManager.getWorkerTasksExecutorShutdownTimeoutMillis(), MILLISECONDS)) {

            // Scheduled worker tasks executor did not terminate in time, so force shutdown
            log.error("Scheduled Worker Tasks Executor did not shut down in time so forcing shutdown");
            getScheduledExecutorService().shutdownNow();

            // Wait just one more time
            if (!getScheduledExecutorService()
                    .awaitTermination(configurationManager.getWorkerTasksExecutorShutdownTimeoutMillis(), MILLISECONDS))
                log.error("Scheduled Worker Tasks Executor failed to terminate");
        }

    }

    public void shutdownNow() {
        getNonScheduledExecutorService().shutdownNow();
        getScheduledExecutorService().shutdownNow();
    }

}
