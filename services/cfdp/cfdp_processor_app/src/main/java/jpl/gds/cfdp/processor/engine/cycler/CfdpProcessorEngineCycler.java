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

package jpl.gds.cfdp.processor.engine.cycler;

import cfdp.engine.*;
import cfdp.engine.ampcs.RequestResult;
import cfdp.engine.ampcs.TransIdUtil;
import com.lmax.disruptor.EventPoller;
import jpl.gds.cfdp.common.action.ETransactionIdentificationType;
import jpl.gds.cfdp.common.action.ingest.EIngestSource;
import jpl.gds.cfdp.processor.action.disruptor.ActionEvent;
import jpl.gds.cfdp.processor.action.disruptor.ActionRingBufferManager;
import jpl.gds.cfdp.processor.ampcs.session.CfdpAmpcsSessionManager;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.gsfc.util.FinishedTransactionsHistoryManager;
import jpl.gds.cfdp.processor.gsfc.util.GsfcUtil;
import jpl.gds.cfdp.processor.in.disruptor.InboundPduEvent;
import jpl.gds.cfdp.processor.in.disruptor.InboundPduRingBufferManager;
import jpl.gds.cfdp.processor.in.ingest.disruptor.IngestActionDisruptorManager;
import jpl.gds.cfdp.processor.mtu.MessagesToUserMapManager;
import jpl.gds.cfdp.processor.out.GenericOutboundPduSinkWrapper;
import jpl.gds.cfdp.processor.stat.StatManager;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static cfdp.engine.RequestScope.*;
import static cfdp.engine.RequestType.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static jpl.gds.cfdp.common.action.EActionCommandType.ABANDON;
import static jpl.gds.cfdp.common.action.EActionCommandType.INGEST;

@Service
@DependsOn(value = {"configurationManager", "cfdpAmpcsSessionManager"})
public class CfdpProcessorEngineCycler implements Runnable {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    private Manager gsfcManager;

    private volatile boolean shuttingDown = false;

    // For consuming
    @Autowired
    private InboundPduRingBufferManager inboundPduRingBufferManager;

    // For consuming
    @Autowired
    private ActionRingBufferManager actionRingBufferManager;

    // For publishing
    @Autowired
    private IngestActionDisruptorManager ingestActionDisruptorManager;

    // For monitoring PDU send activity
    @Autowired
    private GenericOutboundPduSinkWrapper gsfcCommLink;

    @Autowired
    private GsfcUtil gsfcUtil;

    @Autowired
    private StatManager statManager;

    @Autowired
    private FinishedTransactionsHistoryManager finishedTransactionsHistoryManager;

    private IDatabaseProperties databaseProperties;

    @Autowired
    private IContextConfiguration parentContext;

    private ISessionStore sessionStore;
    private IHostStore hostStore;

    @Autowired
    private CfdpAmpcsSessionManager cfdpAmpcsSessionManager;

    @Autowired
    private MessagesToUserMapManager mtuManager;

    @Override
    public void run() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        databaseProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);

        final EventPoller<InboundPduEvent> inboundPduPoller = inboundPduRingBufferManager.getRingBuffer().newPoller();
        inboundPduRingBufferManager.getRingBuffer().addGatingSequences(inboundPduPoller.getSequence());
        final EventPoller<ActionEvent> actionPoller = actionRingBufferManager.getRingBuffer().newPoller();
        actionRingBufferManager.getRingBuffer().addGatingSequences(actionPoller.getSequence());

        final AtomicBoolean canSleepThisCycle = new AtomicBoolean();

        try {

            while (!Thread.currentThread().isInterrupted()) {
                final long cycleStartTimeMillis = Instant.now().toEpochMilli();

                log.trace("cycle");

                canSleepThisCycle.set(true);

                /*
                 * Step 0: Outbound PDU sink type and URI may have changed. Set the sink wrapper
                 * to use the currently configured sink. We will not dynamically change any of
                 * the filesystem sink's parameters however.
                 */
                gsfcCommLink.setSinkType(configurationManager.getOutboundPduSinkType());
                gsfcCommLink.setUri(configurationManager.getOutboundPduUri());

                // Reset the "PDU sent this cycle?" flag
                gsfcCommLink.setPduSentThisCycle(false);

                // Step 1: Check for a single inbound PDU and feed it to the GSFC CFDP Library

                try {

                    inboundPduPoller.poll((final InboundPduEvent event, final long sequence, final boolean endOfBatch) -> {

                        log.trace("consumed inbound PDU, giving to GSFC CFDP Library: sequence=" + sequence + ", endOfBatch=" + endOfBatch);

                        final byte[] pdu = new byte[event.getPduLength()];
                        System.arraycopy(event.getPduBuffer(), 0, pdu, 0, event.getPduLength());
                        final Data pduData = new Data(pdu);
                        gsfcManager.givePDU(pduData, event.getAmpcsOutputDirectory(), event.getSclk(), event.getScet(),
                                event.getLst(), event.getErt(), event.getSourcePacketSeqCount(),
                                event.getSessionId(),
                                event.getSessionName(),
                                event.getFswDictionaryDir(),
                                event.getFswVersion(),
                                event.getVenueType(),
                                event.getTestbedName(),
                                event.getUser(),
                                event.getHost(),
                                event.getScid(),
                                event.getApid(),
                                event.getProductType(),
                                event.getVcid(),
                                event.getSequenceId(),
                                event.getSequenceVersion(),
                                event.getCommandNumber(),
                                event.getRelayScid()
                        );

                        // If just consumed last one, set "can sleep" flag
                        canSleepThisCycle.set(canSleepThisCycle.get() && endOfBatch);

                        // Return false to consume one at a time, not the whole batch
                        return false;
                    });

                } catch (final Exception e) {
                    log.error("Polling inbound PDU ring buffer threw exception: " + ExceptionTools.getMessage(e), e);
                }

                // Step 2: Process all actions

                try {

                    actionPoller.poll((final ActionEvent event, final long sequence, final boolean endOfBatch) -> {

                        log.trace("consumed an action: actionCommandType=" + event.getActionCommandType() +
                                ", sequence=" + sequence + ", endOfBatch=" + endOfBatch);

                        Request gsfcRequest = null;
                        RequestResult result = null;

                        switch (event.getActionCommandType()) {

                            case PUT:
                                result = new RequestResult();

                                // Check if reached max of open uplink transaction for the remote entity
                                if (statManager.getOpenUplinkTransactionsCountByRemoteEntity(
                                        event.getDestinationEntity()) >= configurationManager
                                        .getMaximumOpenUplinkTransactionsPerRemoteEntity()) {
                                    result.setInternalError(true);
                                    result.setMessage("No more uplink transactions allowed for remote entity "
                                            + event.getDestinationEntity() + " (max of "
                                            + configurationManager.getMaximumOpenUplinkTransactionsPerRemoteEntity()
                                            + " open transactions reached)");
                                    break;
                                }

                                gsfcRequest = new Request();
                                gsfcRequest.resultRef = result;
                                gsfcRequest.type = REQ_PUT;
                                gsfcRequest.put.file_transfer = event.getSourceFileName() != null;

                                final String uplinkDirectory = configurationManager.getUplinkFilesTopLevelDirectory();
                                final Path sourcePath =
                                        Paths.get(uplinkDirectory, event.getSourceFileName()).normalize();

                                // If user uploaded a local file, first save it to the uplink directory
                                if (event.getUploadedFile() != null) {
                                    Files.write(sourcePath, event.getUploadedFile());
                                    log.info("Saved ", event.getUploadedFile().length, " bytes of user-uploaded file " +
                                                    "as " + sourcePath);
                                }

                                if (!(Files.exists(sourcePath) && sourcePath.startsWith(uplinkDirectory))) {
                                    result.setBadRequest(true);
                                    result.setMessage("Source file " + event.getSourceFileName() + " does not exist under "
                                            + configurationManager.getUplinkFilesTopLevelDirectory());
                                    break;
                                }

                                // Check that file size is under limit
                                long sourceFileSize = -1;

                                try (FileChannel sourceFileChannel = FileChannel
                                        .open(Paths.get(configurationManager.getUplinkFilesTopLevelDirectory(),
                                                event.getSourceFileName()))) {
                                    sourceFileSize = sourceFileChannel.size();
                                } catch (final IOException ie) {
                                    log.error("Unexpected IOException from FileChannel of " +
                                                    Paths.get(configurationManager.getUplinkFilesTopLevelDirectory(),
                                                            event.getSourceFileName()).toString() + ": " + ExceptionTools.getMessage(ie),
                                            ie);
                                    result.setInternalError(true);
                                    result.setMessage("Unexpected IOException while working with the source file");
                                    break;
                                }

                                if (Long.compareUnsigned(sourceFileSize,
                                        configurationManager.getMaximumUplinkFileSizeBytes()) > 0) {
                                    result.setBadRequest(true);
                                    result.setMessage("File size " + sourceFileSize + " is over the configured limit of "
                                            + configurationManager.getMaximumUplinkFileSizeBytes() + " bytes");
                                    break;
                                }

                                if (statManager
                                        .getTotalFileDataBytesBeingUplinkedToRemoteEntity(
                                                event.getDestinationEntity())
                                        .add(BigInteger.valueOf(sourceFileSize)).compareTo(configurationManager
                                                .getMaximumUplinkFileSizesTotalBytesPerRemoteEntity()) > 0) {
                                    // Accepting this file to uplink will put us over the configured limit
                                    result.setInternalError(true);
                                    result.setMessage("Uplink of " + event.getSourceFileName()
                                            + " violates the maximum allowed total file sizes limit per remote entity (max: "
                                            + configurationManager
                                            .getMaximumUplinkFileSizesTotalBytesPerRemoteEntity().toString()
                                            + ", total: "
                                            + statManager
                                            .getTotalFileDataBytesBeingUplinkedToRemoteEntity(
                                                    event.getDestinationEntity())
                                            .add(BigInteger.valueOf(sourceFileSize)).toString()
                                            + ")");
                                    break;
                                }

                                gsfcRequest.put.ack_required = event.getServiceClass() == 2 || (event.getServiceClass() != 1
                                        && configurationManager.getDefaultServiceClass() == 2);
                                gsfcRequest.put.source_file_name = event.getSourceFileName();
                                if (gsfcRequest.put.source_file_name.length()
                                        > configurationManager.getMaximumSourceFilenameLength()) {
                                    result.setBadRequest(true);
                                    result.setMessage("Source filename " + gsfcRequest.put.source_file_name
                                                              + " is over the maximum allowed length limit ("
                                                              + configurationManager.getMaximumSourceFilenameLength() +
                                                              " characters)");
                                    break;
                                }

                                gsfcRequest.put.dest_id = gsfcUtil.convertEntityId(event.getDestinationEntity());
                                gsfcRequest.put.dest_file_name = event.getDestinationFileName() != null
                                        ? event.getDestinationFileName()
                                        : event.getSourceFileName();

                                if (gsfcRequest.put.dest_file_name.length() > configurationManager
                                        .getMaximumDestinationFilenameLength()) {
                                    result.setBadRequest(true);
                                    result.setMessage("Destination filename " + gsfcRequest.put.dest_file_name
                                            + " is over the maximum allowed length limit ("
                                            + configurationManager.getMaximumDestinationFilenameLength() + " characters)");
                                    break;
                                }

                                if (event.getSessionKey() != null) {
                                    gsfcRequest.put.user_provided_session_key = event.getSessionKey().longValue();
                                }

                                // MPCS-10886 5/9/19
                                if (event.getMessagesToUser() != null && event.getMessagesToUser().size() > 0) {
                                    boolean ok = true;

                                    for (final String mtu : event.getMessagesToUser()) {
                                        byte[] mtuBytes = mtuManager.getBytesForMnemonic(mtu);

                                        if (mtuBytes == null && !configurationManager.isMessagesToUserDirectInputEnabled()) {
                                            /* If mapping failed and users are not allowed to specify their own
                                            Message to User, reject the request and abort it */
                                            result.setBadRequest(true);
                                            result.setMessage("Message to User " + mtu + " is not defined");
                                            ok = false;
                                            break;
                                        }

                                        if (mtuBytes == null) {

                                            // Users may specify their own Message to User
                                            try {
                                                mtuBytes = BinOctHexUtility.toBytesFromHex(mtu);
                                            } catch (final Exception e) {
                                                result.setBadRequest(true);
                                                result.setMessage("Message to User '" + mtu + "' is not a valid " +
                                                        "hexadecimal string");
                                                ok = false;
                                                break;
                                            }

                                        }

                                        // At this point, mtuBytes is not null
                                        gsfcRequest.put.addMessage(MessageToUser.create(LV.create(mtuBytes)));
                                    }

                                    if (!ok) {
                                        break;
                                    }

                                } else if (configurationManager.isMessagesToUserAlwaysRequired()) {
                                    result.setBadRequest(true);
                                    result.setMessage("A Message to User is required");
                                    break;
                                }

                                gsfcManager.giveRequest(gsfcRequest);
                                break;

                            case CANCEL:
                            case ABANDON:
                            case SUSPEND:
                            case RESUME:
                            case REPORT:
                            case FORCE_GEN:
                            case PAUSE_TIMER:
                            case RESUME_TIMER:
                                result = new RequestResult();

                                try {
                                    gsfcRequest = createTransactionIdentifyingRequest(event);
                                    gsfcRequest.resultRef = result;
                                    gsfcManager.giveRequest(gsfcRequest);
                                } catch (final IllegalArgumentException iae) {
                                    /*
                                     * cfdp.engine.ID#create(...) throws if makeID parameter is not made of numbers
                                     * between 0 and 255
                                     */
                                    result.setBadRequest(true);
                                    result.setMessage(iae.getLocalizedMessage());
                                }

                                break;

                            case RESET_STAT:
                                result = new RequestResult();
                                statManager.reset(result);
                                break;

                            case SAVE_STATE:
                                result = new RequestResult();
                                gsfcRequest = new Request();
                                gsfcRequest.resultRef = result;
                                gsfcRequest.type = REQ_SAVE;
                                gsfcRequest.dir = configurationManager.getSavedStateDirectory();
                                gsfcManager.giveRequest(gsfcRequest);
                                break;

                            case INGEST:

                                if (!Files.exists(Paths.get(event.getIngestFileName()))) {
                                    result = new RequestResult();
                                    result.setBadRequest(true);
                                    result.setMessage("Ingest file " + event.getIngestFileName() + " does not exist");
                                } else if (shuttingDown) {
                                    result = new RequestResult();
                                    result.setInternalError(true);
                                    result.setMessage(
                                            "Will not ingest file " + event.getIngestFileName() + " because shutting down");

                                    // If shutting down flag is set, don't publish any more
                                    log.warn("Discarding " + INGEST + " because shutting down: source=" +
                                            event.getIngestSource() + (event.getIngestSource() == EIngestSource.FILE
                                            ? " file=" + event.getSourceFileName()
                                            : ""));
                                    // TODO add archive service source to log above

                                } else {
                                    ingestActionDisruptorManager.getDisruptor().getRingBuffer()
                                            .publishEvent(ActionEvent::copyIngest, event);

                                }

                                break;

                            case CLEAR:
                                result = new RequestResult();

                                try {
                                    // Perform Abandon All
                                    event.setTransactionIdentificationType(ETransactionIdentificationType.ALL);
                                    event.setActionCommandType(ABANDON);
                                    gsfcRequest = createTransactionIdentifyingRequest(event);
                                    gsfcRequest.resultRef = result;
                                    gsfcManager.giveRequest(gsfcRequest);

                                    // Perform Reset Stat
                                    statManager.reset(result);

                                    // Clear finished transactions history, too
                                    finishedTransactionsHistoryManager.clear();

                                    // Set message
                                    result.setMessage("CFDP Processor cleared of all states");

                                } catch (final IllegalArgumentException iae) {
                                    /*
                                     * cfdp.engine.ID#create(...) throws if makeID parameter is not made of numbers
                                     * between 0 and 255
                                     */
                                    result.setBadRequest(true);
                                    result.setMessage(iae.getLocalizedMessage());
                                }

                                break;

                            default:
                                log.error("Action type " + event.getActionCommandType() + " is not supported");
                        }

                        // Offer the result to the response queue
                        if (result != null && !event.getResponseQueue().offer(result)) {
                            log.error("Failed to add result of request " + event.getRequestId() + " to response queue");
                        }

                        // If just consumed last one, set "can sleep" flag
                        canSleepThisCycle.set(canSleepThisCycle.get() && endOfBatch);

                        // Return false to consume one at a time, not the whole batch
                        return false;
                    });

                } catch (final Exception e) {
                    log.error("Processing user action threw exception: " + ExceptionTools.getMessage(e), e);
                }

                // Step 3:

                // TODO

                // Step 4: Cycle the GSFC CFDP Library

                gsfcManager.cycle();

                // If no PDU was sent, set "can sleep" flag
                canSleepThisCycle.set(canSleepThisCycle.get() && !gsfcCommLink.hasPduBeenSentThisCycle());

                // Sleep if there is some idle time so that we're not wastefully busy-waiting

                final long millisLeftInThisCycle = configurationManager.getEngineCycleMinimumIntervalWhenIdleMillis()
                        - (Instant.now().toEpochMilli() - cycleStartTimeMillis);

                if (canSleepThisCycle.get() && millisLeftInThisCycle > 0) {
                    MILLISECONDS.sleep(millisLeftInThisCycle);
                }

            }

        } catch (final InterruptedException ie) {
            log.debug(ExceptionTools.getMessage(ie));

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        log.debug("Exited engine cycle loop, now doing last consume of all items remaining in ring buffers");
        // TODO Consume and process remaining stuff one last time

    }

    /**
     * @param gsfcManager the gsfcManager to set
     */
    public void setGsfcManager(final Manager gsfcManager) {
        this.gsfcManager = gsfcManager;
    }

    public void setShuttingDown() {
        this.shuttingDown = true;
    }

    private Request createTransactionIdentifyingRequest(final ActionEvent event) {
        final Request gsfcRequest = new Request();

        switch (event.getTransactionIdentificationType()) {
            case ALL:
                gsfcRequest.scope = ALL;
                break;

            case SERVICE_CLASS:

                if (event.getServiceClass() == 1) {
                    gsfcRequest.scope = CLASS_1;
                } else if (event.getServiceClass() == 2) {
                    gsfcRequest.scope = CLASS_2;
                } else {
                    throw new RuntimeException("Service class " + event.getServiceClass()
                            + " is not supported for ETransactionIdentificationType."
                            + event.getTransactionIdentificationType().name());
                }

                break;

            case REMOTE_ENTITY_ID:
                gsfcRequest.scope = ENTITY;
                gsfcRequest.entityID = gsfcUtil.convertEntityId(event.getRemoteEntityId());
                break;

            case TRANSACTION_IDS:
                gsfcRequest.scope = TRANSACTION;

                /*
                 * MPCS-9750 - 5/15/2018
                 *
                 * JavaCFDP 1.2.1-crc does not support multiple transaction numbers. In 1.1, I had to manually
                 * implemented the feature in JavaCFDP. Do not want to reimplement over and over again. So disable the
                 * multiple transaction numbers feature and just accept one at a time.
                 */
                gsfcRequest.transID = TransID.create(TransIdUtil.INSTANCE.convertEntity(event.getTransactionEntityId()),
                        event.getTransactionSequenceNumbers().get(0).get(0).longValue());
                break;

            default:
                throw new RuntimeException("ETransactionIdentificationType."
                        + event.getTransactionIdentificationType().name() + " is unsupported");
        }

        switch (event.getActionCommandType()) {
            case CANCEL:
                gsfcRequest.type = REQ_CANCEL;
                break;
            case ABANDON:
                gsfcRequest.type = REQ_ABANDON;
                break;
            case SUSPEND:
                gsfcRequest.type = REQ_SUSPEND;
                break;
            case RESUME:
                gsfcRequest.type = REQ_RESUME;
                break;
            case REPORT:
                gsfcRequest.type = REQ_REPORT;
                break;
            case FORCE_GEN:
                gsfcRequest.type = REQ_FORCE_GENERATE;
                break;
            case PAUSE_TIMER:
                gsfcRequest.type = REQ_FREEZE;
                break;
            case RESUME_TIMER:
                gsfcRequest.type = REQ_THAW;
                break;
            default:
                throw new RuntimeException("EActionCommandType." + event.getActionCommandType().name()
                        + " is unsupported");
        }

        return gsfcRequest;
    }

    public void setSessionRefs(final ISessionStore sessionStore, final IHostStore hostStore) {
        this.sessionStore = sessionStore;
        this.hostStore = hostStore;
    }
}