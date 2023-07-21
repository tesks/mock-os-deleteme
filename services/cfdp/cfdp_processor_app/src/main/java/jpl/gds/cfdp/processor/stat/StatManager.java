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

package jpl.gds.cfdp.processor.stat;

import cfdp.engine.ID;
import cfdp.engine.TransID;
import cfdp.engine.ampcs.IStatManager;
import cfdp.engine.ampcs.OrderedProperties;
import cfdp.engine.ampcs.RequestResult;
import cfdp.engine.ampcs.TransIdUtil;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.util.CfdpFileUtil;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.TimeUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static jpl.gds.shared.log.TraceSeverity.*;

@Service
@DependsOn("configurationManager")
public class StatManager implements IStatManager {

    // Handled in getStatus()
    static final String LAST_STATE_SAVE_TIME_KEY = "last.state.save.time";
    static final String FILE_SYSTEM_ACCESS_KEY = "file.system.access";
    static final String PDU_OUT_KEY = "pdu.out";
    static final String CONFIG_PDU_UPLINK_KEY = "config.pdu.uplink";
    static final String CONFIG_LOG_LEVEL_KEY = "config.log.level";
    static final String VERSION_AMPCS_KEY = "version.ampcs";
    // Handled in getStatistics() using SerializableSet
    static final String TOTAL_FILE_DATA_BYTES_UPLINKED_KEY = "total.file.data.bytes.uplinked";
    static final String TOTAL_FILE_DATA_PDU_BYTES_SENT_KEY = "total.file.data.pdu.bytes.sent";
    static final String TOTAL_EOF_PDU_BYTES_SENT_KEY = "total.eof.pdu.bytes.sent";
    static final String TOTAL_FINISHED_PDU_BYTES_SENT_KEY = "total.finished.pdu.bytes.sent";
    static final String TOTAL_ACK_PDU_BYTES_SENT_KEY = "total.ack.pdu.bytes.sent";
    static final String TOTAL_METADATA_PDU_BYTES_SENT_KEY = "total.metadata.pdu.bytes.sent";
    static final String TOTAL_NAK_PDU_BYTES_SENT_KEY = "total.nak.pdu.bytes.sent";
    static final String TOTAL_ALL_PDU_BYTES_SENT_KEY = "total.all.pdu.bytes.sent";
    static final String TOTAL_ALL_PDU_BYTES_RECEIVED_KEY = "total.all.pdu.bytes.received";
    // Handled by "process...TransactionStatistics" methods
    static final String UPLINK_TX_KEY_PREFIX = "uplink.tx.";
    static final String DOWNLINK_TX_KEY_PREFIX = "downlink.tx.";
    // Handled in getStatistics() using TransactionStatisticsAggregator
    static final String ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_BEING_UPLINKED_KEY
            = "all.open.tx.file.data.bytes.being.uplinked";
    static final String ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_UPLINKED_KEY
            = "all.open.tx.file.data.bytes.uplinked";
    static final String ALL_OPEN_TRANSACTIONS_ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_UPLINKED_KEY
            = "all.open.tx.estimated.file.data.bytes.left.to.be.uplinked";
    static final String ALL_OPEN_TRANSACTIONS_EOF_PDUS_QUEUED_KEY
            = "all.open.tx.eof.pdus.queued";
    static final String ALL_OPEN_TRANSACTIONS_FINISHED_PDUS_QUEUED_KEY
            = "all.open.tx.finished.pdus.queued";
    static final String ALL_OPEN_TRANSACTIONS_ACK_PDUS_QUEUED_KEY
            = "all.open.tx.ack.pdus.queued";
    static final String ALL_OPEN_TRANSACTIONS_METADATA_PDUS_QUEUED_KEY
            = "all.open.tx.metadata.pdus.queued";
    static final String ALL_OPEN_TRANSACTIONS_NAK_PDUS_QUEUED_KEY
            = "all.open.tx.nak.pdus.queued";
    static final String ALL_OPEN_TRANSACTIONS_ALL_FILE_DIRECTIVE_PDUS_QUEUED_KEY
            = "all.open.tx.all.file.directive.pdus.queued";
    static final String OPEN_UPLINK_TRANSACTIONS_COUNT_KEY
            = "open.uplink.tx.count";
    static final String OPEN_UPLINK_TRANSACTIONS_COUNT_BY_REMOTE_ENTITY_KEY_PREFIX
            = "open.uplink.tx.count.by.remote.entity.";
    static final String ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_BEING_DOWNLINKED_KEY
            = "all.open.tx.file.data.bytes.being.downlinked";
    static final String ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_DOWNLINKED_KEY
            = "all.open.tx.file.data.bytes.downlinked";
    static final String ALL_OPEN_TRANSACTIONS_ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_DOWNLINKED_KEY
            = "all.open.tx.estimated.file.data.bytes.left.to.be.downlinked";
    static final String OPEN_DOWNLINK_TRANSACTIONS_COUNT_KEY
            = "open.downlink.tx.count";
    static final String OPEN_DOWNLINK_TRANSACTIONS_COUNT_BY_REMOTE_ENTITY_KEY_PREFIX
            = "open.downlink.tx.count.by.remote.entity.";
    static final String TRANSACTIONS_WITH_PAUSED_TIMERS_COUNT_KEY = "tx.with.paused.timers";
    // Per-transaction statistics suffixes
    static final String TIMER_PAUSED_KEY_SUFFIX = "timer.paused";
    static final String ACK_PDU_QUEUED_KEY_SUFFIX = "ack.pdu.queued";
    static final String ACK_PDU_BYTES_SENT_KEY_SUFFIX = "ack.pdu.bytes.sent";
    static final String FILE_SIZE_KEY_SUFFIX = "file.size";
    static final String FILE_DATA_BYTES_UPLINKED_KEY_SUFFIX = "file.data.bytes.uplinked";
    static final String ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_UPLINKED_KEY_SUFFIX
            = "estimated.file.data.bytes.left.to.be.uplinked";
    static final String FILE_DATA_PDU_BYTES_SENT_KEY_SUFFIX
            = "file.data.pdu.bytes.sent";
    static final String EOF_PDU_QUEUED_KEY_SUFFIX
            = "eof.pdu.queued";
    static final String EOF_PDU_BYTES_SENT_KEY_SUFFIX
            = "eof.pdu.bytes.sent";
    static final String METADATA_PDU_QUEUED_KEY_SUFFIX
            = "metadata.pdu.queued";
    static final String METADATA_PDU_BYTES_SENT_KEY_SUFFIX
            = "metadata.pdu.bytes.sent";
    static final String FILE_DATA_BYTES_DOWNLINKED_KEY_SUFFIX
            = "file.data.bytes.downlinked";
    static final String ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_DOWNLINKED_KEY_SUFFIX
            = "estimated.file.data.bytes.left.to.be.downlinked";
    static final String FINISHED_PDU_QUEUED_KEY_SUFFIX
            = "finished.pdu.queued";
    static final String FINISHED_PDU_BYTES_SENT_KEY_SUFFIX
            = "finished.pdu.bytes.sent";
    static final String NAK_PDU_QUEUED_KEY_SUFFIX
            = "nak.pdu.queued";
    static final String NAK_PDU_BYTES_SENT_KEY_SUFFIX
            = "nak.pdu.bytes.sent";

    private final StringBuilder sb = new StringBuilder();
    private Tracer log;
    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private ConfigurationManager configurationManager;
    @Autowired
    private CfdpFileUtil cfdpFileUtil;
    private SerializableStat statData = new SerializableStat();

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    /**
     * Retrieve the current status properties.
     *
     * @return current status properties
     */
    public OrderedProperties getStatus() {
        final OrderedProperties p = new OrderedProperties();
        final DateFormat dateFormatter = TimeUtility.getFormatterFromPool();

        if (statData.lastStateSaveTime > 0) {
            p.setProperty(LAST_STATE_SAVE_TIME_KEY, dateFormatter.format(new Date(statData.lastStateSaveTime)));
        } else {
            p.setProperty(LAST_STATE_SAVE_TIME_KEY, "");
        }

        p.setProperty(FILE_SYSTEM_ACCESS_KEY, isFileSystemAccessOk() ? "OK" : "ERROR");
        p.setProperty(PDU_OUT_KEY, statData.pduOutOk ? "OK" : "ERROR");
        p.setProperty(CONFIG_PDU_UPLINK_KEY, configurationManager.isOutboundPduEnabled() ? "ON" : "OFF");
        // MPCS-9929  - 8/24/2018 - Removed ability to configure CFDP Processor's log level dynamically
        TraceSeverity currentLogLevel = null;

        if (log.isEnabledFor(ALL)) {
            currentLogLevel = ALL;
        } else if (log.isEnabledFor(ALL)) {
            currentLogLevel = TRACE;
        } else if (log.isEnabledFor(TRACE)) {
            currentLogLevel = DEBUG;
        } else if (log.isEnabledFor(DEBUG)) {
            currentLogLevel = INFO;
        } else if (log.isEnabledFor(INFO)) {
            currentLogLevel = WARN;
        } else if (log.isEnabledFor(WARN)) {
            currentLogLevel = ERROR;
        } else if (log.isEnabledFor(ERROR)) {
            currentLogLevel = OFF;
        } else {
            currentLogLevel = OFF;
        }

        p.setProperty(CONFIG_LOG_LEVEL_KEY, currentLogLevel.toString());
        p.setProperty(VERSION_AMPCS_KEY, ReleaseProperties.getProductLine() + " "
                + ApplicationConfiguration.getApplicationName() + " " + ReleaseProperties.getVersion());

        TimeUtility.releaseFormatterToPool(dateFormatter);
        return p;
    }

    /**
     * Retrieve the current statistics properties.
     *
     * @return current statistics properties
     */
    public OrderedProperties getStatistics() {
        final OrderedProperties p = new OrderedProperties();
        p.setProperty(TOTAL_FILE_DATA_BYTES_UPLINKED_KEY, statData.totalFileDataBytesUplinked.toString());
        p.setProperty(TOTAL_FILE_DATA_PDU_BYTES_SENT_KEY, statData.totalFileDataPduBytesSent.toString());
        p.setProperty(TOTAL_EOF_PDU_BYTES_SENT_KEY, statData.totalEofPduBytesSent.toString());
        p.setProperty(TOTAL_FINISHED_PDU_BYTES_SENT_KEY, statData.totalFinishedPduBytesSent.toString());
        p.setProperty(TOTAL_ACK_PDU_BYTES_SENT_KEY, statData.totalAckPduBytesSent.toString());
        p.setProperty(TOTAL_METADATA_PDU_BYTES_SENT_KEY, statData.totalMetadataPduBytesSent.toString());
        p.setProperty(TOTAL_NAK_PDU_BYTES_SENT_KEY, statData.totalNakPduBytesSent.toString());
        p.setProperty(TOTAL_ALL_PDU_BYTES_SENT_KEY,
                statData.totalFileDataPduBytesSent.add(statData.totalEofPduBytesSent)
                        .add(statData.totalFinishedPduBytesSent).add(statData.totalAckPduBytesSent)
                        .add(statData.totalMetadataPduBytesSent).add(statData.totalNakPduBytesSent).toString());
        p.setProperty(TOTAL_ALL_PDU_BYTES_RECEIVED_KEY, statData.totalAllPduBytesReceived.toString());

        final TransactionStatisticsAggregator agg = new TransactionStatisticsAggregator();
        statData.uplinks.forEach((k, v) -> processIndividualUplinkTransactionStatistics(
                UPLINK_TX_KEY_PREFIX + TransIdUtil.INSTANCE.convertEntity(k.getSource()) + "." + k.getNumber(), v, p,
                agg));
        statData.downlinks.forEach((k, v) -> processIndividualDownlinkTransactionStatistics(
                DOWNLINK_TX_KEY_PREFIX + TransIdUtil.INSTANCE.convertEntity(k.getSource()) + "." + k.getNumber(), v, p,
                agg));

        p.setProperty(ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_BEING_UPLINKED_KEY, agg.getAllOpenTransactionsFileDataBytesBeingUplinked().toString());
        p.setProperty(ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_UPLINKED_KEY,
                agg.getAllOpenTransactionsFileDataBytesUplinked().toString());
        p.setProperty(ALL_OPEN_TRANSACTIONS_ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_UPLINKED_KEY,
                agg.getAllOpenTransactionsEstimatedFileDataBytesLeftToBeUplinked().toString());
        p.setProperty(ALL_OPEN_TRANSACTIONS_EOF_PDUS_QUEUED_KEY,
                Integer.toString(agg.getAllOpenTransactionsEofPdusQueued()));
        p.setProperty(ALL_OPEN_TRANSACTIONS_FINISHED_PDUS_QUEUED_KEY,
                Integer.toString(agg.getAllOpenTransactionsFinishedPdusQueued()));
        p.setProperty(ALL_OPEN_TRANSACTIONS_ACK_PDUS_QUEUED_KEY,
                Integer.toString(agg.getAllOpenTransactionsAckPdusQueued()));
        p.setProperty(ALL_OPEN_TRANSACTIONS_METADATA_PDUS_QUEUED_KEY,
                Integer.toString(agg.getAllOpenTransactionsMetadataPdusQueued()));
        p.setProperty(ALL_OPEN_TRANSACTIONS_NAK_PDUS_QUEUED_KEY,
                Integer.toString(agg.getAllOpenTransactionsNakPdusQueued()));
        p.setProperty(ALL_OPEN_TRANSACTIONS_ALL_FILE_DIRECTIVE_PDUS_QUEUED_KEY,
                Integer.toString(agg.getAllOpenTransactionsEofPdusQueued() + agg.getAllOpenTransactionsFinishedPdusQueued()
                        + agg.getAllOpenTransactionsAckPdusQueued() + agg.getAllOpenTransactionsMetadataPdusQueued() + agg.getAllOpenTransactionsNakPdusQueued()));
        p.setProperty(OPEN_UPLINK_TRANSACTIONS_COUNT_KEY,
                Integer.toUnsignedString(agg.getTotalOpenUplinkTransactionsCount()));
        agg.getOpenUplinkTransactionsByRemoteEntity()
                .forEach((remoteEntity, count) -> p.setProperty(
                        OPEN_UPLINK_TRANSACTIONS_COUNT_BY_REMOTE_ENTITY_KEY_PREFIX + remoteEntity,
                        Integer.toUnsignedString(count)));
        p.setProperty(ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_BEING_DOWNLINKED_KEY,
                agg.getAllOpenTransactionsFileDataBytesBeingDownlinked().toString());
        p.setProperty(ALL_OPEN_TRANSACTIONS_FILE_DATA_BYTES_DOWNLINKED_KEY,
                agg.getAllOpenTransactionsFileDataBytesDownlinked().toString());
        p.setProperty(ALL_OPEN_TRANSACTIONS_ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_DOWNLINKED_KEY,
                agg.getAllOpenTransactionsEstimatedFileDataBytesLeftToBeDownlinked().toString());
        p.setProperty(OPEN_DOWNLINK_TRANSACTIONS_COUNT_KEY,
                Integer.toUnsignedString(agg.getTotalOpenDownlinkTransactionsCount()));
        agg.getOpenDownlinkTransactionsByRemoteEntity()
                .forEach((remoteEntity, count) -> p.setProperty(
                        OPEN_DOWNLINK_TRANSACTIONS_COUNT_BY_REMOTE_ENTITY_KEY_PREFIX + remoteEntity,
                        Integer.toUnsignedString(count)));
        p.setProperty(TRANSACTIONS_WITH_PAUSED_TIMERS_COUNT_KEY,
                Integer.toUnsignedString(agg.getPausedTransactionsCount()));

        return p;
    }

    private void processCommonIndividualTransactionStatistics(final int prefixLen, final ATransactionStatistics stat,
                                                              final Properties p,
                                                              final TransactionStatisticsAggregator agg) {
        sb.append(TIMER_PAUSED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Boolean.toString(stat.isTimerPaused()));
        if (stat.isTimerPaused()) {
            agg.incrementPausedTransactionsCount();
        }
        sb.setLength(prefixLen);

        /* Don't add the file size property here, but do it in the uplink/downlink-specific process methods because
        they behave differently */

        sb.append(ACK_PDU_QUEUED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Boolean.toString(stat.isAckPduQueued()));
        if (stat.isAckPduQueued()) {
            agg.incrementQueuedAckPdus();
        }
        sb.setLength(prefixLen);

        sb.append(ACK_PDU_BYTES_SENT_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getAckPduBytesSent()));
        sb.setLength(prefixLen);
    }

    private void processIndividualUplinkTransactionStatistics(final String keyPrefix, final UplinkTransactionStatistics stat,
                                                              final Properties p, final TransactionStatisticsAggregator agg) {
        sb.setLength(0);
        sb.append(keyPrefix);
        sb.append(".");
        final int prefixLen = sb.length();

        processCommonIndividualTransactionStatistics(prefixLen, stat, p, agg);

        // Add the file size property here instead of in the common process method
        sb.append(FILE_SIZE_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getFileSize()));
        agg.addFileDataBytesBeingUplinked(stat.getFileSize());
        sb.setLength(prefixLen);

        sb.append(FILE_DATA_BYTES_UPLINKED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getFileDataBytesUplinked()));
        agg.addFileDataBytesUplinked(stat.getFileDataBytesUplinked());
        sb.setLength(prefixLen);

        sb.append(ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_UPLINKED_KEY_SUFFIX);
        long remaining = 0;
        if (stat.getFileSize() - stat.getFileDataBytesUplinked() > 0) {
            remaining = stat.getFileSize() - stat.getFileDataBytesUplinked();
        }
        p.setProperty(sb.toString(), Long.toUnsignedString(remaining));
        agg.addEstimatedFileDataBytesLeftToBeUplinked(remaining);
        sb.setLength(prefixLen);

        sb.append(FILE_DATA_PDU_BYTES_SENT_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getFileDataPduBytesSent()));
        sb.setLength(prefixLen);

        sb.append(EOF_PDU_QUEUED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Boolean.toString(stat.isEofPduQueued()));
        if (stat.isEofPduQueued()) {
            agg.incrementQueuedEofPdus();
        }
        sb.setLength(prefixLen);

        sb.append(EOF_PDU_BYTES_SENT_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getEofPduBytesSent()));
        sb.setLength(prefixLen);

        sb.append(METADATA_PDU_QUEUED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Boolean.toString(stat.isMetadataPduQueued()));
        if (stat.isMetadataPduQueued()) {
            agg.incrementQueuedMetadataPdus();
        }
        sb.setLength(prefixLen);

        sb.append(METADATA_PDU_BYTES_SENT_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getMetadataPduBytesSent()));
        sb.setLength(prefixLen);

        agg.incrementOpenUplinkTransactionsCount(stat.getRemoteEntity());
    }

    private void processIndividualDownlinkTransactionStatistics(final String keyPrefix, final DownlinkTransactionStatistics stat,
                                                                final Properties p, final TransactionStatisticsAggregator agg) {
        sb.setLength(0);
        sb.append(keyPrefix);
        sb.append(".");
        final int prefixLen = sb.length();

        processCommonIndividualTransactionStatistics(prefixLen, stat, p, agg);

        // Add the file size property here instead of in the common process method
        sb.append(FILE_SIZE_KEY_SUFFIX);
        if (stat.isFileSizeDetermined()) {
            p.setProperty(sb.toString(), Long.toUnsignedString(stat.getFileSize()));
            agg.addFileDataBytesBeingDownlinked(stat.getFileSize());
        } else {
            p.setProperty(sb.toString(), "unknown");
        }
        sb.setLength(prefixLen);

        sb.append(FILE_DATA_BYTES_DOWNLINKED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getFileDataBytesDownlinked()));
        agg.addFileDataBytesDownlinked(stat.getFileDataBytesDownlinked());
        sb.setLength(prefixLen);

        sb.append(ESTIMATED_FILE_DATA_BYTES_LEFT_TO_BE_DOWNLINKED_KEY_SUFFIX);
        if (stat.isFileSizeDetermined()) {
            long remaining = 0;
            if (stat.getFileSize() - stat.getFileDataBytesDownlinked() > 0) {
                remaining = stat.getFileSize() - stat.getFileDataBytesDownlinked();
            }
            p.setProperty(sb.toString(), Long.toUnsignedString(remaining));
            agg.addEstimatedFileDataBytesLeftToBeDownlinked(remaining);
        } else {
            p.setProperty(sb.toString(), "unknown");
        }
        sb.setLength(prefixLen);

        sb.append(FINISHED_PDU_QUEUED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Boolean.toString(stat.isFinishedPduQueued()));
        if (stat.isFinishedPduQueued()) {
            agg.incrementQueuedFinishedPdu();
        }
        sb.setLength(prefixLen);

        sb.append(FINISHED_PDU_BYTES_SENT_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getFinishedPduBytesSent()));
        sb.setLength(prefixLen);

        sb.append(NAK_PDU_QUEUED_KEY_SUFFIX);
        p.setProperty(sb.toString(), Boolean.toString(stat.isNakPduQueued()));
        if (stat.isNakPduQueued()) {
            agg.incrementQueuedNakPdu();
        }
        sb.setLength(prefixLen);

        sb.append(NAK_PDU_BYTES_SENT_KEY_SUFFIX);
        p.setProperty(sb.toString(), Long.toUnsignedString(stat.getNakPduBytesSent()));
        sb.setLength(prefixLen);

        agg.incrementOpenDownlinkTransactionsCount(stat.getRemoteEntity());
    }

    /**
     * Return the current count of open uplink transactions by remote entity.
     *
     * @param remoteEntity remote entity to filter the count of open uplink transactions for
     * @return count of currently open uplink transactions for the remote entity
     */
    public long getOpenUplinkTransactionsCountByRemoteEntity(final long remoteEntity) {
        return statData.uplinks.values().stream().filter(t -> t.getRemoteEntity() == remoteEntity).count();
    }

    /**
     * Return the current aggregate of all open uplink transactions' file data bytes for a remote entity.
     *
     * @param remoteEntity remote entity to filter for
     * @return aggregate total size of files being uplinked to remote entity
     */
    public BigInteger getTotalFileDataBytesBeingUplinkedToRemoteEntity(final long remoteEntity) {
        return statData.uplinks.values().stream().filter(t -> t.getRemoteEntity() == remoteEntity)
                .mapToLong(t -> t.getFileSize()).mapToObj(BigInteger::valueOf).reduce(BigInteger.ZERO, BigInteger::add);
    }

    /**
     * Reset statistics for total figures.
     *
     * @param result the request result object to populate with useful message of the reset action
     */
    public void reset(final RequestResult result) {
        statData.totalFileDataPduBytesSent = BigInteger.valueOf(0);
        statData.totalFileDataBytesUplinked = BigInteger.valueOf(0);
        statData.totalEofPduBytesSent = BigInteger.valueOf(0);
        statData.totalFinishedPduBytesSent = BigInteger.valueOf(0);
        statData.totalAckPduBytesSent = BigInteger.valueOf(0);
        statData.totalMetadataPduBytesSent = BigInteger.valueOf(0);
        statData.totalNakPduBytesSent = BigInteger.valueOf(0);
        statData.totalAllPduBytesReceived = BigInteger.valueOf(0);

        result.setMessage("Total statistics has been reset");
        log.info("Total statistics reset");
    }

    /**
     * Inform the stat manager whether or not a PDU was successfully pushed out to sink.
     *
     * @param ok true to indicate that PDU output is working okay, false otherwise
     */
    public void setPduOutOk(final boolean ok) {
        statData.pduOutOk = ok;
    }

    boolean isFileSystemAccessOk() {
        return statData.fileSystemAccessFailureCount < configurationManager
                .getConsecutiveFilestoreFailuresBeforeDeclaringErrorThreshold();
    }

    private boolean transactionIsUplink(final TransID transId) {
        return TransIdUtil.INSTANCE.convertEntity(transId.getSource())
                == Long.parseUnsignedLong(configurationManager.getLocalCfdpEntityId());
    }

    @Override
    public void setTimerState(final TransID transId, final boolean paused) {

        if (transactionIsUplink(transId)) {

            if (statData.uplinks.containsKey(transId)) {
                statData.uplinks.get(transId).setTimerPaused(paused);
            } else {
                log.error("Transaction ", transId, " doesn't exist in uplinks statistics map");
            }

        } else {

            if (statData.downlinks.containsKey(transId)) {
                statData.downlinks.get(transId).setTimerPaused(paused);
            } else {
                log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
            }

        }

    }

    @Override
    public void setAckPduQueued(final TransID transId, final boolean ackPduQueued) {

        if (transactionIsUplink(transId)) {

            if (statData.uplinks.containsKey(transId)) {
                statData.uplinks.get(transId).setAckPduQueued(ackPduQueued);
            } else {
                log.error("Transaction ", transId, " doesn't exist in uplinks statistics map");
            }

        } else {

            if (statData.downlinks.containsKey(transId)) {
                statData.downlinks.get(transId).setAckPduQueued(ackPduQueued);
            } else {
                log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
            }

        }

    }

    @Override
    public void sentAckPdu(final TransID transId, final int size) {
        statData.totalAckPduBytesSent = statData.totalAckPduBytesSent.add(BigInteger.valueOf(size));

        if (transactionIsUplink(transId)) {

            if (statData.uplinks.containsKey(transId)) {
                statData.uplinks.get(transId).addAckPduBytesSent(size);
            } else {
                log.error("Transaction ", transId, " doesn't exist in uplinks statistics map");
            }

        } else {

            if (statData.downlinks.containsKey(transId)) {
                statData.downlinks.get(transId).addAckPduBytesSent(size);
            } else {
                log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
            }

        }

    }

    @Override
    public void newUplinkTransaction(final TransID transId, final ID remoteEntityId, final boolean timerPaused,
                                     final long fileSize) {
        final UplinkTransactionStatistics stat = new UplinkTransactionStatistics();
        stat.setRemoteEntity(TransIdUtil.INSTANCE.convertEntity(remoteEntityId));
        stat.setTimerPaused(timerPaused);
        stat.setFileSize(fileSize);
        statData.uplinks.put(transId, stat);
    }

    @Override
    public void sentFileDataPdu(final TransID transId, final int fileDataSize, final int pduSize) {
        statData.totalFileDataBytesUplinked = statData.totalFileDataBytesUplinked.add(BigInteger.valueOf(fileDataSize));
        statData.totalFileDataPduBytesSent = statData.totalFileDataPduBytesSent.add(BigInteger.valueOf(pduSize));

        if (statData.uplinks.containsKey(transId)) {
            final UplinkTransactionStatistics uts = statData.uplinks.get(transId);
            uts.addFileDataBytesUplinked(fileDataSize);
            uts.addFileDataPduBytesSent(pduSize);
        } else {
            log.error("Transaction ", transId, " doesn't exist in uplinks statistics map");
        }


    }

    @Override
    public void setEofPduQueued(final TransID transId, final boolean eofPduQueued) {

        if (statData.uplinks.containsKey(transId)) {
            statData.uplinks.get(transId).setEofPduQueued(eofPduQueued);
        }

    }

    @Override
    public void sentEofPdu(final TransID transId, final int size) {
        statData.totalEofPduBytesSent = statData.totalEofPduBytesSent.add(BigInteger.valueOf(size));

        if (statData.uplinks.containsKey(transId)) {
            statData.uplinks.get(transId).addEofPduBytesSent(size);
        } else {
            log.error("Transaction ", transId, " doesn't exist in uplinks statistics map");
        }

    }

    @Override
    public void setMetadataPduQueued(final TransID transId, final boolean metadataPduQueued) {

        if (statData.uplinks.containsKey(transId)) {
            statData.uplinks.get(transId).setMetadataPduQueued(metadataPduQueued);
        }

    }

    @Override
    public void sentMetadataPdu(final TransID transId, final int size) {
        statData.totalMetadataPduBytesSent = statData.totalMetadataPduBytesSent.add(BigInteger.valueOf(size));

        if (statData.uplinks.containsKey(transId)) {
            statData.uplinks.get(transId).addMetadataPduBytesSent(size);
        } else {
            log.error("Transaction ", transId, " doesn't exist in uplinks statistics map");
        }

    }

    @Override
    public void deleteUplinkTransaction(final TransID transId) {

        if (statData.uplinks.containsKey(transId)) {
            statData.uplinks.remove(transId);
        } else {
            log.error("Transaction ", transId, " doesn't exist in uplinks statistics map");
        }

    }

    @Override
    public void receivedPdu(final long size) {
        statData.totalAllPduBytesReceived = statData.totalAllPduBytesReceived.add(BigInteger.valueOf(size));
    }

    @Override
    public void newDownlinkTransaction(final TransID transId, final ID remoteEntityId, final boolean timerPaused) {
        final DownlinkTransactionStatistics stat = new DownlinkTransactionStatistics();
        stat.setRemoteEntity(TransIdUtil.INSTANCE.convertEntity(remoteEntityId));
        stat.setTimerPaused(timerPaused);
        statData.downlinks.put(transId, stat);
    }

    @Override
    public void setNakPduQueued(final TransID transId, final boolean nakPduQueued) {

        if (statData.downlinks.containsKey(transId)) {
            statData.downlinks.get(transId).setNakPduQueued(nakPduQueued);
        }

    }

    @Override
    public void sentNakPdu(final TransID transId, final int size) {
        statData.totalNakPduBytesSent = statData.totalNakPduBytesSent.add(BigInteger.valueOf(size));

        if (statData.downlinks.containsKey(transId)) {
            statData.downlinks.get(transId).addNakPduBytesSent(size);
        } else {
            log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
        }

    }

    @Override
    public void setFinishedPduQueued(final TransID transId, final boolean finishedPduQueued) {

        if (statData.downlinks.containsKey(transId)) {
            statData.downlinks.get(transId).setFinishedPduQueued(finishedPduQueued);
        }

    }

    @Override
    public void sentFinishedPdu(final TransID transId, final int size) {
        statData.totalFinishedPduBytesSent = statData.totalFinishedPduBytesSent.add(BigInteger.valueOf(size));

        if (statData.downlinks.containsKey(transId)) {
            statData.downlinks.get(transId).addFinishedPduBytesSent(size);
        } else {
            log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
        }

    }

    @Override
    public void determinedFileSize(final TransID transId, final long fileSize) {

        if (statData.downlinks.containsKey(transId)) {
            statData.downlinks.get(transId).setFileSize(fileSize);
            statData.downlinks.get(transId).setFileSizeDetermined(true);
        } else {
            log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
        }

    }

    @Override
    public void downlinkedFileData(TransID transId, final int size) {

        if (statData.downlinks.containsKey(transId)) {
            statData.downlinks.get(transId).addFileDataBytesDownlinked(size);
        } else {
            log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
        }

    }

    @Override
    public void deleteDownlinkTransaction(final TransID transId) {

        if (statData.downlinks.containsKey(transId)) {
            statData.downlinks.remove(transId);
        } else {
            log.error("Transaction ", transId, " doesn't exist in downlink statistics map");
        }

    }

    @Override
    public void filestoreAccessOk(final boolean ok) {

        if (!ok) {
            statData.fileSystemAccessFailureCount++;
        } else {
            statData.fileSystemAccessFailureCount = 0;
        }

    }

    @Override
    public void save(final String filename) {

        try (FileOutputStream fos = new FileOutputStream(filename)) {

            // MPCS-10634 4/9/19 Create directories if missing
            cfdpFileUtil.createParentDirectoriesIfNotExist(filename);

            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(this.statData);
            } catch (final IOException ie) {
                log.error("ObjectOutputStream failure trying to save StatManager: " + ExceptionTools.getMessage(ie), ie);
            }

        } catch (final IOException ie) {
            log.error("FileOutputStream failure trying to save StatManager: " + ExceptionTools.getMessage(ie), ie);
        }

    }

    @Override
    public void restore(final String filename) {

        try (FileInputStream fin = new FileInputStream(filename)) {

            try (ObjectInputStream ois = new ObjectInputStream(fin)) {
                statData = (SerializableStat) ois.readObject();
            } catch (IOException | ClassNotFoundException ie) {
                log.error("ObjectInputStream failure trying to restore StatManager: " + ExceptionTools.getMessage(ie), ie);
            }

        } catch (final IOException ie) {
            log.error("FileInputStream failure trying to restore StatManager: " + ExceptionTools.getMessage(ie), ie);
        }

    }

    @Override
    public void setStateSaveTime(final long millis) {
        statData.lastStateSaveTime = millis;
    }

    /**
     * {@code SerializableStat} is the persist-able, restorable, overall data holder for statistics.
     *
     */
    private static class SerializableStat implements Serializable {

        private static final long serialVersionUID = 1L;
        private final Map<TransID, UplinkTransactionStatistics> uplinks = new HashMap<>();
        private final Map<TransID, DownlinkTransactionStatistics> downlinks = new HashMap<>();
        // Status
        private long lastStateSaveTime = -1;
        private int fileSystemAccessFailureCount = 0;
        private boolean pduOutOk = true;
        // Statistics
        private BigInteger totalFileDataBytesUplinked = BigInteger.valueOf(0);
        private BigInteger totalFileDataPduBytesSent = BigInteger.valueOf(0);
        private BigInteger totalEofPduBytesSent = BigInteger.valueOf(0);
        private BigInteger totalFinishedPduBytesSent = BigInteger.valueOf(0);
        private BigInteger totalAckPduBytesSent = BigInteger.valueOf(0);
        private BigInteger totalMetadataPduBytesSent = BigInteger.valueOf(0);
        private BigInteger totalNakPduBytesSent = BigInteger.valueOf(0);
        private BigInteger totalAllPduBytesReceived = BigInteger.valueOf(0);
    }

    private static class TransactionStatisticsAggregator {
        int pausedTransactionsCount = 0;
        // Uplink statistics
        BigInteger allOpenTransactionsFileDataBytesBeingUplinked = BigInteger.valueOf(0);
        BigInteger allOpenTransactionsFileDataBytesUplinked = BigInteger.valueOf(0);
        BigInteger allOpenTransactionsEstimatedFileDataBytesLeftToBeUplinked = BigInteger.valueOf(0);
        int allOpenTransactionsEofPdusQueued;
        int allOpenTransactionsFinishedPdusQueued;
        int allOpenTransactionsAckPdusQueued;
        int allOpenTransactionsMetadataPdusQueued;
        int allOpenTransactionsNakPdusQueued;
        Map<Long, Integer> openUplinkTransactionsByRemoteEntity = new HashMap<>();
        // Downlink statistics
        BigInteger allOpenTransactionsFileDataBytesBeingDownlinked = BigInteger.valueOf(0);
        BigInteger allOpenTransactionsFileDataBytesDownlinked = BigInteger.valueOf(0);
        BigInteger allOpenTransactionsEstimatedFileDataBytesLeftToBeDownlinked = BigInteger.valueOf(0);
        Map<Long, Integer> openDownlinkTransactionsByRemoteEntity = new HashMap<>();

        private int getPausedTransactionsCount() {
            return pausedTransactionsCount;
        }

        private void incrementPausedTransactionsCount() {
            pausedTransactionsCount++;
        }

        private BigInteger getAllOpenTransactionsFileDataBytesBeingUplinked() {
            return allOpenTransactionsFileDataBytesBeingUplinked;
        }

        private void addFileDataBytesBeingUplinked(final long size) {
            allOpenTransactionsFileDataBytesBeingUplinked = allOpenTransactionsFileDataBytesBeingUplinked.add(BigInteger.valueOf(size));
        }

        private BigInteger getAllOpenTransactionsFileDataBytesUplinked() {
            return allOpenTransactionsFileDataBytesUplinked;
        }

        private void addFileDataBytesUplinked(final long size) {
            allOpenTransactionsFileDataBytesUplinked = allOpenTransactionsFileDataBytesUplinked.add(BigInteger.valueOf(size));
        }

        private BigInteger getAllOpenTransactionsEstimatedFileDataBytesLeftToBeUplinked() {
            return allOpenTransactionsEstimatedFileDataBytesLeftToBeUplinked;
        }

        private void addEstimatedFileDataBytesLeftToBeUplinked(final long size) {
            allOpenTransactionsEstimatedFileDataBytesLeftToBeUplinked = allOpenTransactionsEstimatedFileDataBytesLeftToBeUplinked.add(BigInteger.valueOf(size));
        }

        private int getAllOpenTransactionsEofPdusQueued() {
            return allOpenTransactionsEofPdusQueued;
        }

        private void incrementQueuedEofPdus() {
            allOpenTransactionsEofPdusQueued++;
        }

        private int getAllOpenTransactionsFinishedPdusQueued() {
            return allOpenTransactionsFinishedPdusQueued;
        }

        private void incrementQueuedFinishedPdu() {
            allOpenTransactionsFinishedPdusQueued++;
        }

        private int getAllOpenTransactionsAckPdusQueued() {
            return allOpenTransactionsAckPdusQueued;
        }

        private void incrementQueuedAckPdus() {
            allOpenTransactionsAckPdusQueued++;
        }

        private int getAllOpenTransactionsMetadataPdusQueued() {
            return allOpenTransactionsMetadataPdusQueued;
        }

        private void incrementQueuedMetadataPdus() {
            allOpenTransactionsMetadataPdusQueued++;
        }

        private int getAllOpenTransactionsNakPdusQueued() {
            return allOpenTransactionsNakPdusQueued;
        }

        private void incrementQueuedNakPdu() {
            allOpenTransactionsNakPdusQueued++;
        }

        private int getTotalOpenUplinkTransactionsCount() {
            return openUplinkTransactionsByRemoteEntity.values().stream().mapToInt(Integer::intValue).sum();
        }

        private Map<Long, Integer> getOpenUplinkTransactionsByRemoteEntity() {
            return openUplinkTransactionsByRemoteEntity;
        }

        private void incrementOpenUplinkTransactionsCount(final long remoteEntity) {

            if (!openUplinkTransactionsByRemoteEntity.containsKey(remoteEntity)) {
                openUplinkTransactionsByRemoteEntity.put(remoteEntity, 1);
            } else {
                openUplinkTransactionsByRemoteEntity.put(remoteEntity,
                        openUplinkTransactionsByRemoteEntity.get(remoteEntity).intValue() + 1);
            }

        }

        private BigInteger getAllOpenTransactionsFileDataBytesBeingDownlinked() {
            return allOpenTransactionsFileDataBytesBeingDownlinked;
        }

        private void addFileDataBytesBeingDownlinked(final long size) {
            allOpenTransactionsFileDataBytesBeingDownlinked = allOpenTransactionsFileDataBytesBeingDownlinked.add(BigInteger.valueOf(size));
        }

        private BigInteger getAllOpenTransactionsFileDataBytesDownlinked() {
            return allOpenTransactionsFileDataBytesDownlinked;
        }

        private void addFileDataBytesDownlinked(final long size) {
            allOpenTransactionsFileDataBytesDownlinked = allOpenTransactionsFileDataBytesDownlinked.add(BigInteger.valueOf(size));
        }

        private BigInteger getAllOpenTransactionsEstimatedFileDataBytesLeftToBeDownlinked() {
            return allOpenTransactionsEstimatedFileDataBytesLeftToBeDownlinked;
        }

        private void addEstimatedFileDataBytesLeftToBeDownlinked(final long size) {
            allOpenTransactionsEstimatedFileDataBytesLeftToBeDownlinked = allOpenTransactionsEstimatedFileDataBytesLeftToBeDownlinked.add(BigInteger.valueOf(size));
        }

        private int getTotalOpenDownlinkTransactionsCount() {
            return openDownlinkTransactionsByRemoteEntity.values().stream().mapToInt(Integer::intValue).sum();
        }

        private Map<Long, Integer> getOpenDownlinkTransactionsByRemoteEntity() {
            return openDownlinkTransactionsByRemoteEntity;
        }

        private void incrementOpenDownlinkTransactionsCount(final long remoteEntity) {

            if (!openDownlinkTransactionsByRemoteEntity.containsKey(remoteEntity)) {
                openDownlinkTransactionsByRemoteEntity.put(remoteEntity, 1);
            } else {
                openDownlinkTransactionsByRemoteEntity.put(remoteEntity,
                        openDownlinkTransactionsByRemoteEntity.get(remoteEntity).intValue() + 1);
            }

        }

    }

}