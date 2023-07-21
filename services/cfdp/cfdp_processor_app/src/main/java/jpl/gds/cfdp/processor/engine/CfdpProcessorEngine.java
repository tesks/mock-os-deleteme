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

package jpl.gds.cfdp.processor.engine;

import cfdp.engine.Client;
import cfdp.engine.Engine;
import cfdp.engine.ID;
import cfdp.engine.Manager;
import cfdp.engine.ampcs.*;
import com.lmax.disruptor.TimeoutException;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.processor.TransactionSequenceNumberGenerator;
import jpl.gds.cfdp.processor.action.disruptor.ActionEvent;
import jpl.gds.cfdp.processor.action.disruptor.ActionRingBufferManager;
import jpl.gds.cfdp.processor.ampcs.product.CfdpAmpcsProductPluginHolder;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.controller.action.InternalActionRequest;
import jpl.gds.cfdp.processor.engine.cycler.CfdpProcessorEngineCycler;
import jpl.gds.cfdp.processor.executors.WorkerTasksExecutorManager;
import jpl.gds.cfdp.processor.gsfc.client.impl.GsfcUserImpl;
import jpl.gds.cfdp.processor.gsfc.util.FinishedTransactionsHistoryManager;
import jpl.gds.cfdp.processor.gsfc.util.GsfcUtil;
import jpl.gds.cfdp.processor.in.ingest.PduIngestWorker;
import jpl.gds.cfdp.processor.in.ingest.disruptor.IngestActionDisruptorManager;
import jpl.gds.cfdp.processor.message.MessageServiceWorker;
import jpl.gds.cfdp.processor.message.disruptor.MessageDisruptorManager;
import jpl.gds.cfdp.processor.mib.MibManager;
import jpl.gds.cfdp.processor.stat.StatManager;
import jpl.gds.cfdp.processor.util.CfdpFileUtil;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static jpl.gds.cfdp.common.action.EActionCommandType.SAVE_STATE;

@Service
@DependsOn(value = {"configurationManager", "transactionSequenceNumberGenerator", "gsfcUserImpl"})
public class CfdpProcessorEngine {

    private Tracer log;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private TransactionSequenceNumberGenerator transactionSequenceNumberGenerator;

    @Autowired
    private ActionRingBufferManager actionDisruptor;

    @Autowired
    private ApplicationContext appContext;

    // GSFC CFDP Library client implementation beans
    @Autowired
    private Client gsfcClientImpl;

    @Autowired
    private MibManager mibManager;

    private Manager gsfcManager;

    @Autowired
    private GsfcUtil gsfcUtil;

    @Autowired
    private FinishedTransactionsHistoryManager removedTransactionsHistoryManager;

    // Task executors

    @Autowired
    private WorkerTasksExecutorManager workerTasksExecutorManager;

    private final ExecutorService engineCyclerTaskExecutor = Executors.newSingleThreadExecutor();

    @Autowired
    private CfdpProcessorEngineCycler engineCycler;
    private Future<?> engineCyclerTaskFuture;

    @Autowired
    private IngestActionDisruptorManager ingestActionDisruptorManager;

    @Autowired
    private MessageDisruptorManager messageDisruptorManager;

    @Autowired
    private PduIngestWorker pduIngestWorker;

    @Autowired
    private MessageServiceWorker messageServiceWorker;

    @Autowired
    private StatManager statManager;

    @Autowired
    private CfdpAmpcsProductPluginHolder productPluginHolder;

    @Autowired
    private GsfcUserImpl gsfcUserImpl;

    @Autowired
    private CfdpFileUtil cfdpFileUtil;

    public void start() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);

        initGsfcCfdpLibrary();

        if (configurationManager.getFinishedTransactionsHistoryPurgePeriodMillis() > 0) {
            workerTasksExecutorManager.addScheduledFuture(workerTasksExecutorManager.getScheduledExecutorService()
                    .scheduleAtFixedRate(() -> removedTransactionsHistoryManager.purgeExpiredTransactions(),
                            configurationManager.getFinishedTransactionsHistoryPurgePeriodMillis(),
                            configurationManager.getFinishedTransactionsHistoryPurgePeriodMillis(),
                            TimeUnit.MILLISECONDS));
        }

        if (configurationManager.getAutoStateSavePeriodMillis() > 0) {
            // Only used for collecting, not reading. Emptied every time.
            final BlockingQueue<RequestResult> periodicStateSaveResultsQueue = new LinkedBlockingQueue<>();
            workerTasksExecutorManager.addScheduledFuture(
                    workerTasksExecutorManager.getScheduledExecutorService().scheduleAtFixedRate(() -> {
                                periodicStateSaveResultsQueue.clear();
                                actionDisruptor.getRingBuffer().publishEvent(ActionEvent::translateGenericAction,
                                        new InternalActionRequest<GenericRequest>(SAVE_STATE, null,
                                                periodicStateSaveResultsQueue, null));
                            }, configurationManager.getAutoStateSavePeriodMillis(),
                            configurationManager.getAutoStateSavePeriodMillis(), TimeUnit.MILLISECONDS));
        }

        // Init INGEST action consumer worker
        ingestActionDisruptorManager.getDisruptor().handleEventsWith(pduIngestWorker);
        ingestActionDisruptorManager.getDisruptor().start();

        // Init message consumer worker
        messageDisruptorManager.getDisruptor().handleEventsWith(messageServiceWorker);
        messageDisruptorManager.getDisruptor().start();

        // Init and start engine cycler task
        engineCycler.setGsfcManager(gsfcManager);

        // MPCS-10094 1/6/2019
        gsfcUserImpl.setSessionStore(configurationManager.getSessionStore());
        gsfcUserImpl.setHostStore(configurationManager.getHostStore());

        engineCyclerTaskFuture = engineCyclerTaskExecutor.submit(engineCycler);
    }

    /**
     * Set up the required user-supplied modules for GSFC CFDP Library.
     */
    private void initGsfcCfdpLibrary() {

        FileUtil.INSTANCE.setTopLevelDirectoriesConfigurationLookup(configurationManager);
        StatUtil.INSTANCE.setStatManager(statManager);
        MetadataFileUtil.INSTANCE.setMetadataConfigurationLookup(configurationManager);
        TransactionSequenceNumbersGenerationUtil.INSTANCE.setGenerator(transactionSequenceNumberGenerator);
        FinishedTransactionsHistoryUtil.INSTANCE.setRemovedTransactionsHistory(removedTransactionsHistoryManager);
        ProductPluginUtil.INSTANCE.setPlugin(productPluginHolder.getPlugin());

        new cfdp.engine.Logger(gsfcClientImpl.getUser());

        final ID id = gsfcUtil.convertEntityId(Long.parseUnsignedLong(configurationManager.getLocalCfdpEntityId()));
        mibManager.setLocalEntityId(configurationManager.getLocalCfdpEntityId());

        // TODO
        // client.restoreDir = options.restoreDir;

        gsfcManager = Engine.createManager(id, gsfcClientImpl);

        log.info("GSFC CFDP Library setup complete with local ID "
                + Long.toUnsignedString(gsfcUtil.convertEntityId(id)));
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down...");

        try {

            // Prevent engine cycler task from publishing to disruptors
            engineCycler.setShuttingDown();

            /*
             * Immediately stop the consumer, but this doesn't interrupt the consuming
             * thread. So also explicitly interrupt after the halt.
             */
            ingestActionDisruptorManager.getDisruptor().halt();
            messageDisruptorManager.getDisruptor().halt();
            pduIngestWorker.getThread().interrupt();

            try {
                ingestActionDisruptorManager.getDisruptor()
                        .shutdown(configurationManager.getIngestActionDisruptorShutdownTimeoutMillis(), MILLISECONDS);
            } catch (final TimeoutException te) {
                log.error(ExceptionTools.getMessage(te));
            }

            try {
                messageDisruptorManager.getDisruptor()
                        .shutdown(configurationManager.getMessageDisruptorShutdownTimeoutMillis(), MILLISECONDS);
            } catch (final TimeoutException te) {
                log.error(ExceptionTools.getMessage(te));
            }

            workerTasksExecutorManager.shutdown();

            // System.out.println("Stopping Engine Cycler Task and shutting down its
            // Executor");
            engineCyclerTaskFuture.cancel(true);
            engineCyclerTaskExecutor.shutdown();

            // Wait for engine cycler task executor to really terminate
            if (!engineCyclerTaskExecutor.awaitTermination(
                    configurationManager.getEngineCyclerTaskExecutorShutdownTimeoutMillis(), MILLISECONDS)) {

                // Engine cycler task executor did not terminate in time, so force shutdown
                log.error("Engine Cycler Task Executor did not shut down in time so forcing shutdown");
                engineCyclerTaskExecutor.shutdownNow();

                // Wait just one more time
                if (!engineCyclerTaskExecutor.awaitTermination(
                        configurationManager.getEngineCyclerTaskExecutorShutdownTimeoutMillis(), MILLISECONDS))
                    log.error("Engine Cycler Task Executor failed to terminate");
            }

        } catch (final InterruptedException ie) {
            log.error(ExceptionTools.getMessage(ie));

            // (Re-)Cancel if current thread also interrupted
            log.error(
                    "Forcing shutdown of Worker Tasks Executor and Engine Cycler Task Executor due to InterruptException during normal shutdown");
            workerTasksExecutorManager.shutdownNow();
            engineCyclerTaskExecutor.shutdownNow();

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

    }

}