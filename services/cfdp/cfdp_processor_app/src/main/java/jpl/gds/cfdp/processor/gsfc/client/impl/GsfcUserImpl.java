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

package jpl.gds.cfdp.processor.gsfc.client.impl;

import cfdp.engine.*;
import cfdp.engine.ampcs.MetadataFileUtil;
import cfdp.engine.ampcs.PduLog;
import cfdp.engine.ampcs.RequestResult;
import cfdp.engine.ampcs.TransIdUtil;
import jpl.gds.cfdp.data.api.*;
import jpl.gds.cfdp.message.api.*;
import jpl.gds.cfdp.processor.ampcs.product.CfdpAmpcsProductPlugin;
import jpl.gds.cfdp.processor.ampcs.session.CfdpAmpcsSessionManager;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.gsfc.util.GsfcUtil;
import jpl.gds.cfdp.processor.message.disruptor.MessageDisruptorManager;
import jpl.gds.cfdp.processor.message.disruptor.MessageEvent;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.config.OrderedProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.TimeUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.DateFormat;
import java.util.List;

/**
 * Implementation for JavaCFDP's User interface
 */
@Service
public class GsfcUserImpl implements User {
    private final boolean[] subjects = initializeSubjects();
    public boolean was_last_transaction_abandoned = false;
    private Tracer log;
    private DateFormat dateFormatter;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private GsfcUtil gsfcUtil;

    @Autowired
    private MessageDisruptorManager messageDisruptorManager;

    @Autowired
    private CfdpAmpcsProductPlugin productPlugin;

    @Autowired
    private CfdpAmpcsSessionManager cfdpAmpcsSessionManager;

    @Autowired
    private IContextConfiguration parentContext;

    @Autowired
    private IMySqlAdaptationProperties dbProperties;

    private ISessionStore sessionStore;
    private IHostStore hostStore;

    private static boolean[] initializeSubjects() {
        final boolean[] tmp = new boolean[Subject.values().length];
        tmp[Subject.FILE_DIRECTIVE_IN_OR_OUT.ordinal()] = true;
        tmp[Subject.PERSIST.ordinal()] = true;
        tmp[Subject.DEBUG_STATE.ordinal()] = true;
        return tmp;
    }

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        dateFormatter = TimeUtility.getFormatterFromPool();
    }

    @PreDestroy
    public void release() {
        TimeUtility.releaseFormatterToPool(dateFormatter);
    }

    // = method *****************************************************************
    @SuppressWarnings("unchecked")
    @Override
    public void indication(final IndicationType type, final TransStatus status, final RequestResult resultRef) {

        /*
         * Divide the indications into two types: Those that publish, those that don't.
         * In this outer switch statement, those that publish are all captured in the
         * default block.
         */
        switch (type) {
            case IND_ACK_TIMER_EXPIRED:
                log.warn("<ack_timer_expired> " + status.getTransID());
                break;

            case IND_FILE_SEGMENT_SENT:
                log.debug("<file_segment_sent> " + status.getTransID() + " offset=" + status.getFdOffset() + ", length="
                        + status.getFdLength());
                break;

            case IND_INACTIVITY_TIMER_EXPIRED:
                log.warn("<inactivity_timer_expired> " + status.getTransID());
                break;

            case IND_METADATA_SENT:
                log.debug("<metadata_sent> " + status.getTransID());
                break;

            case IND_NAK_TIMER_EXPIRED:
                log.warn("<nak_timer_expired> " + status.getTransID());
                break;

            default:

                switch (type) {

                    case IND_ABANDONED:
                        log.info("<abandoned> " + status.getTransID());
                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.ABANDONED),
                                resultRef, status);
                        break;

                    case IND_EOF_RECV:

                        if (configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            log.info("<eof_recv> " + status.getTransID());
                        } else {
                            log.debug("<eof_recv> " + status.getTransID());
                        }

                        if (!configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            return;
                        }

                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.EOF_RECV),
                                resultRef, status);
                        break;

                    case IND_EOF_SENT:

                        if (configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            log.info("<eof_sent> " + status.getTransID());
                        } else {
                            log.debug("<eof_sent> " + status.getTransID());
                        }

                        if (!configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            return;
                        }

                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.EOF_SENT),
                                resultRef, status);
                        break;

                    case IND_FAULT:
                        log.warn("<fault> " + status.getTransID() + " " + status.getConditionCode());
                        final ICfdpIndicationMessage cfdpIndicationMessage = appContext.getBean(ICfdpIndicationMessage.class)
                                .setIndicationType(ECfdpIndicationType.FAULT);
                        ICfdpCondition condition = null;

                        try {
                            condition = ECfdpFaultCondition.valueOf(status.getConditionCode().name());
                            cfdpIndicationMessage.setCondition(condition);
                        } catch (final IllegalArgumentException iae1) {

                            try {
                                condition = ECfdpNonFaultCondition.valueOf(status.getConditionCode().name());
                                cfdpIndicationMessage.setCondition(condition);
                            } catch (final IllegalArgumentException iae2) {
                                log.error("Could not map condition code " + status.getConditionCode().name() +
                                        " for fault indication: " + ExceptionTools.getMessage(iae2), iae2);
                            }

                        }

                        publishMessage(cfdpIndicationMessage, resultRef, status);
                        break;

                    case IND_FILE_SEGMENT_RECV:

                        if (configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            log.info("<file_segment_recv> " + status.getTransID() + " offset="
                                    + Integer.toUnsignedString(status.getFdOffset()) + ", length="
                                    + Integer.toUnsignedString(status.getFdLength()));
                        } else {
                            log.debug("<file_segment_recv> " + status.getTransID() + " offset="
                                    + Integer.toUnsignedString(status.getFdOffset())
                                    + ", length=" + Integer.toUnsignedString(status.getFdLength()));
                        }

                        if (!configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            return;
                        }

                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                .setIndicationType(ECfdpIndicationType.FILE_SEGMENT_RECV), resultRef, status);
                        break;

                    case IND_MACHINE_ALLOCATED:
                        log.info("<machine_allocated> " + status.getTransID() + " " + status.getRole());

                        if (status.getRole() != Role.CLASS_1_SENDER && status.getRole() != Role.CLASS_2_SENDER) {
                            // If I'm sender, then IND_TRANSACTION will be called
                            publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                    .setIndicationType(ECfdpIndicationType.NEW_TRANSACTION_DETECTED), resultRef, status);
                        }

                        break;

                    case IND_MACHINE_DEALLOCATED:

                        if (status.isAbandoned()) {
                            log.info("<machine_deallocated> " + status.getTransID() + " " + status.getRole() + " (abandoned)");
                            was_last_transaction_abandoned = true;
                        } else if (status.isCancelled()) {
                            log.info("<machine_deallocated> " + status.getTransID() + " " + status.getRole() + " (cancelled)");
                            was_last_transaction_abandoned = false;
                        } else {
                            log.info("<machine_deallocated> " + status.getTransID() + " " + status.getRole() + " (successful)");
                            was_last_transaction_abandoned = false;
                        }

                        // MPCS-10094 1/6/2019 Remove the session from the session map
                        cfdpAmpcsSessionManager.removeTransaction(status.getTransID());

                        break;

                    case IND_METADATA_RECV:

                        if (configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            log.info("<metadata_recv> " + status.getTransID());
                        } else {
                            log.debug("<metadata_recv> " + status.getTransID());
                        }

                        if (!configurationManager.getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty()) {
                            return;
                        }

                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.METADATA_RECV),
                                resultRef, status);
                        break;

                    case IND_REPORT:

                        if (resultRef != null) {

                            if (status.getRole() == Role.CLASS_1_SENDER || status.getRole() == Role.CLASS_2_SENDER) {
                                resultRef.getLiveTransactionsReportMap().put(
                                        TransIdUtil.INSTANCE.toString(status.getTransID()),
                                        MetadataFileUtil.INSTANCE.getUplinkReport(status));
                            } else if (status.getRole() == Role.CLASS_1_RECEIVER || status.getRole() == Role.CLASS_2_RECEIVER) {
                                resultRef.getLiveTransactionsReportMap().put(
                                        TransIdUtil.INSTANCE.toString(status.getTransID()),
                                        MetadataFileUtil.INSTANCE.getDownlinkReport(status));
                            }

                        }

                        break;

                    case IND_RESUMED:
                        log.info("<resumed> " + status.getTransID());
                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.RESUMED),
                                resultRef, status);
                        break;

                    case IND_SUSPENDED:
                        log.info("<suspended> " + status.getTransID());
                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.SUSPENDED),
                                resultRef, status);
                        break;

                    case IND_TRANSACTION:
                        log.info("<transaction> " + status.getTransID() + " " + status.getRole());

                        // MPCS-10094 1/3/2019
                        // New uplink transaction started, so map the session. But skip if database stores are not
                        // set up.
                        if (sessionStore != null && hostStore != null) {
                            cfdpAmpcsSessionManager.addTransaction(status.getTransID());

                            try {
                                final IContextConfiguration config;
                                // Check if user supplied a session key
                                if (status.getUserSuppliedSessionKey() != null) {
                                    config = queryForSession(status.getUserSuppliedSessionKey());
                                }
                                else {
                                    // This creates a new session
                                    config = new SessionConfiguration(
                                            parentContext.getMissionProperties(),
                                            parentContext.getConnectionConfiguration().getConnectionProperties(), false);
                                    config.getContextId().setNumber(sessionStore.insertTestConfig(config, 0L));
                                    // MPCS-11879 - Write LDI
                                    if (dbProperties.getExportLDIAny()) {
                                        sessionStore.writeLDI(config);
                                    }
                                }

                                updateContextAndTransaction(status.getTransID(), config);
                            }
                            catch (DatabaseException e) {
                                log.error("Failed to create new session for uplink transaction " + status.getTransID() + ": " + ExceptionTools.getMessage(e));
                            }

                        }

                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.TRANSACTION),
                                resultRef, status);
                        break;

                    case IND_TRANSACTION_FINISHED:
                        log.info("<transaction_finished> " + status.getTransID());
                        publishMessage(appContext.getBean(ICfdpIndicationMessage.class)
                                        .setIndicationType(ECfdpIndicationType.TRANSACTION_FINISHED),
                                resultRef, status);

                        if ((status.getRole() == Role.CLASS_1_SENDER || status.getRole() == Role.CLASS_2_SENDER)
                                && status.getDestinationFile() != null) {
                            /*
                             * This was a file uplink transaction, so publish an uplink finished message,
                             * too.
                             */
                            final ICfdpFileUplinkFinishedMessage uplinkFinishedMessage = appContext
                                    .getBean(ICfdpFileUplinkFinishedMessage.class);
                            uplinkFinishedMessage.getHeader().setCfdpProcessorInstanceId(configurationManager.getInstanceId());
                            final OrderedProperties p = new OrderedProperties();
                            p.putAll(MetadataFileUtil.INSTANCE.getUplinkFileMetadataFileContent(status));
                            publishMessage(uplinkFinishedMessage.setUplinkFileMetadata(p)
                                            .setUplinkFileMetadataFileLocation(status.getMetadataFilePath())
                                            .setUplinkFileLocation(p.getProperty("source.file.actual")),
                                    resultRef, status);
                        }

                        break;

                    case IND_FILE_DOWNLINKED:
                        log.info("<file_downlinked> " + status.getTransID());
                        final OrderedProperties p = new OrderedProperties();
                        p.putAll(MetadataFileUtil.INSTANCE.getDownlinkFileMetadataFileContent(status));
                        publishMessage(appContext.getBean(ICfdpFileGenerationMessage.class).setDownlinkFileMetadata(p)
                                        .setDownlinkFileMetadataFileLocation(status.getMetadataFilePath())
                                        .setDownlinkFileLocation(p.getProperty("downlink.file.actual")),
                                resultRef, status);
                        break;

                    default:
                        // Do nothing
                }

        }

    }

    private void publishMessage(final ICfdpMessage m, final RequestResult resultRef, final TransStatus status) {
        if (m == null) {
            return;
        }

        m.getHeader().setCfdpProcessorInstanceId(configurationManager.getInstanceId());

        // MPCS-10094 1/6/2019 Set session as the message context if it exists
        if (cfdpAmpcsSessionManager.getSession(status.getTransID()) != null) {

            m.setContextKey(cfdpAmpcsSessionManager.getSession(status.getTransID()).getContextId().getContextKey());

        } else if (status.getSessionId() > 0) {
            // Downlink session exists
            if (cfdpAmpcsSessionManager.getSession(status.getTransID()) == null) {
                cfdpAmpcsSessionManager.addTransaction(status.getTransID());
            }

           updateContextAndTransaction(status.getTransID(), queryForSession(status.getSessionId()));
        }

        if (m instanceof ICfdpIndicationMessage) {
            final ICfdpIndicationMessage cfdpIndicationMessage = (ICfdpIndicationMessage) m;
            cfdpIndicationMessage.setSourceEntityId(gsfcUtil.convertEntityId(status.getTransID().getSource()));
            cfdpIndicationMessage
                    .setTransactionSequenceNumber(status.getTransID().getNumber());
            cfdpIndicationMessage.setInvolvesFileTransfer(
                    status.getDestinationFile() != null || status.getSourceFile() != null
                            || status.getFdLength() > 0 || status.getTempFileName() != null);
            cfdpIndicationMessage.setTotalBytesSentOrReceived(status.getBytesTransferred());

            // TODO temporarily for now
            cfdpIndicationMessage.setPduId("0");

            cfdpIndicationMessage.setTriggeringType(
                    resultRef == null ? ECfdpTriggeredByType.PDU : ECfdpTriggeredByType.REQUEST);

            switch (status.getRole()) {

                case CLASS_1_SENDER:
                    cfdpIndicationMessage.setTransactionDirection(ECfdpTransactionDirection.OUT)
                            .setServiceClass((byte) 1)
                            .setDestinationEntityId(gsfcUtil.convertEntityId(status.getPartnerID()));
                    break;

                case CLASS_1_RECEIVER:
                    cfdpIndicationMessage.setTransactionDirection(ECfdpTransactionDirection.IN)
                            .setServiceClass((byte) 1)
                            .setDestinationEntityId(gsfcUtil.convertEntityId(status.getLocalEntityID()));
                    break;

                case CLASS_2_SENDER:
                    cfdpIndicationMessage.setTransactionDirection(ECfdpTransactionDirection.OUT)
                            .setServiceClass((byte) 2)
                            .setDestinationEntityId(gsfcUtil.convertEntityId(status.getPartnerID()));
                    break;

                case CLASS_2_RECEIVER:
                    cfdpIndicationMessage.setTransactionDirection(ECfdpTransactionDirection.IN)
                            .setServiceClass((byte) 2)
                            .setDestinationEntityId(gsfcUtil.convertEntityId(status.getLocalEntityID()));
                    break;

                default:
                    log.warn("Processing indication with service class neither 1 nor 2");
            }

        }

        messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate, m);
    }

    @Override
    public boolean ofInterest(final Subject s) {

        return subjects[s.ordinal()];
    }

    public void enable(final Subject s) {
        if (s == null) {
            for (int i = 0; i < subjects.length; ++i) {
                subjects[i] = true;
            }
        } else
            subjects[s.ordinal()] = true;
    }

    public void disable(final Subject s) {
        if (s == null) {
            for (int i = 0; i < subjects.length; ++i) {
                subjects[i] = false;
            }
        } else
            subjects[s.ordinal()] = false;
    }

    private String stripTrailingNewline(final String sourceStr) {
        String newStr = sourceStr;

        while (newStr.endsWith("\n")) {
            newStr = newStr.substring(0, newStr.length() - 1);
        }

        return newStr;
    }

    @Override
    public void debug(final String text) {
        log.debug(stripTrailingNewline(text));
    }

    @Override
    public void info(final String text) {
        log.info(stripTrailingNewline(text));
    }

    @Override
    public void warning(final String text) {
        log.warn(stripTrailingNewline(text));
    }

    @Override
    public void error(final String text) {
        log.error(stripTrailingNewline(text));
    }

    @Override
    public void pdu(final TransStatus status, final boolean incomingPdu, final String pduId, final PduLog pduLog, final long sessionId,
                    final String sessionHost) {

        if (configurationManager.getMessageServicePduMessagePublishingEnabledProperty()) {
            ICfdpPduMessage m = null;

            if (incomingPdu) {
                m = appContext.getBean(ICfdpPduReceivedMessage.class);
            } else {
                m = appContext.getBean(ICfdpPduSentMessage.class);
            }

            m.getHeader().setCfdpProcessorInstanceId(configurationManager.getInstanceId());

            // MPCS-11974 ACK/FIN missing from received CFDP PDU query by session ID
            if (cfdpAmpcsSessionManager.getSession(status.getTransID()) == null) {
                cfdpAmpcsSessionManager.addTransaction(status.getTransID());
                final IContextConfiguration config = cfdpAmpcsSessionManager.getSessionById(sessionId);
                updateContextAndTransaction(status.getTransID(), config);
            }

            // MPCS-10094 1/7/2019 Set session as the message context if it exists
            if (cfdpAmpcsSessionManager.getSession(status.getTransID()) != null) {
                m.setContextKey(cfdpAmpcsSessionManager.getSession(status.getTransID()).getContextId().getContextKey());
            }

            messageDisruptorManager.getDisruptor().getRingBuffer().publishEvent(MessageEvent::translate,
                    m.setPduId(pduId).addAllMetadata(pduLog.getLogEntries()));
        }

        if (incomingPdu
                /* MPCS-11075  - 9/3/2019: Any inbound PDUs for uplink transaction should not be published
                as product parts. */
                && gsfcUtil.convertEntityId(status.getTransID().getSource())
                != Long.parseUnsignedLong(configurationManager.getLocalCfdpEntityId())) {
            productPlugin.publishProductPartMessage(status, pduLog, sessionId, sessionHost);
        }

    }

    /**
     * Sets session store
     * @param sessionStore ISessionStore
     */
    public void setSessionStore(ISessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    /**
     * Sets host store
     * @param hostStore IHostStore
     */
    public void setHostStore(IHostStore hostStore) {
        this.hostStore = hostStore;
    }

    /**
     * Query for session in the DB and update context
     * modeled after jpl.gds.tcapp.app.AbstractUplinkApp.queryForSession
     * @return IContextConfiguration or null if not found
     */
    private IContextConfiguration queryForSession(final long key)  {
        //use this constructor to avoid running overwriting the CFDP context ID with the current session ID
        final IContextConfiguration config = new SessionConfiguration(parentContext.getMissionProperties(),
              parentContext.getConnectionConfiguration().getConnectionProperties(), false);

        final IDbSqlFetch tsf = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch(false);
        try {
            final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);

            final IDbSessionInfoUpdater tsi = dbSessionInfoFactory.createQueryableUpdater();
            tsi.addSessionKey(key);

            final List<? extends IDbRecord> testSessions = tsf.get(tsi,null,1,(IDbOrderByType)null);
            if(testSessions.isEmpty()){
                tsf.close();
                return null;
            }
            final IDbSessionUpdater dsc = (IDbSessionUpdater) testSessions.get(0);

            dsc.setIntoContextConfiguration(config);
        } catch(final DatabaseException e) {
            tsf.close();
            log.error("Failed to set the session for downlink transaction " + key + ": " + ExceptionTools.getMessage(e));
            return null;
        } finally {
            tsf.close();
        }

        return config;
    }

    /**
     * Update parent context and transaction in the session manager
     * @param transId Transaction ID
     * @param config Context Configuration
     */
    private void updateContextAndTransaction(final TransID transId, final IContextConfiguration config){
        if(config == null){
            return;
        }

        // Set the context and session hierarchy
        config.getContextId().getContextKey().setParentNumber(parentContext.getContextId().getNumber());
        config.getContextId().getContextKey().setParentHostId(parentContext.getContextId().getHostId());

        cfdpAmpcsSessionManager.updateTransaction(transId, config);
    }
}