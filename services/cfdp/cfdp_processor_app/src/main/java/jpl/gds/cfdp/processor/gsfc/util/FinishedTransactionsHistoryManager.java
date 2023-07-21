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

package jpl.gds.cfdp.processor.gsfc.util;

import static cfdp.engine.Role.CLASS_1_RECEIVER;
import static cfdp.engine.Role.CLASS_1_SENDER;
import static cfdp.engine.Role.CLASS_2_RECEIVER;
import static cfdp.engine.Role.CLASS_2_SENDER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import cfdp.engine.ID;
import cfdp.engine.Role;
import cfdp.engine.TransID;
import cfdp.engine.TransStatus;
import cfdp.engine.ampcs.IFinishedTransactionsHistory;
import cfdp.engine.ampcs.MetadataFileUtil;
import cfdp.engine.ampcs.OrderedProperties;
import cfdp.engine.ampcs.TransIdUtil;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

@Service
@DependsOn("configurationManager")
public class FinishedTransactionsHistoryManager implements IFinishedTransactionsHistory {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    ConfigurationManager configurationManager;

    private final Map<String, TimeTaggedOrderedProperties> finishedTransactionsMap = new HashMap<>();
    private final List<TimeTaggedOrderedProperties> finishedTransactionsList = new LinkedList<>();

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    static class TimeTaggedOrderedProperties {
        private OrderedProperties properties;
        private long timestamp;
        private String transIdKeyStr;
        private TransStatus status;

        public TimeTaggedOrderedProperties(final OrderedProperties properties, final long timestamp, final String transIdKeyStr,
                                           final TransStatus status) {
            this.properties = properties;
            this.timestamp = timestamp;
            this.transIdKeyStr = transIdKeyStr;
            this.status = status;
        }

        /**
         * @return the properties
         */
        OrderedProperties getProperties() {
            return properties;
        }

        /**
         * @param properties the properties to set
         */
        void setProperties(final OrderedProperties properties) {
            this.properties = properties;
        }

        /**
         * @return the timestamp
         */
        long getTimestamp() {
            return timestamp;
        }

        /**
         * @param timestamp the timestamp to set
         */
        void setTimestamp(final long timestamp) {
            this.timestamp = timestamp;
        }

        /**
         * @return the transIdKeyStr
         */
        String getTransIdKeyStr() {
            return transIdKeyStr;
        }

        /**
         * @param transIdKeyStr the transIdKeyStr to set
         */
        void setTransIdKeyStr(final String transIdKeyStr) {
            this.transIdKeyStr = transIdKeyStr;
        }

        /**
         * @return the status
         */
        TransStatus getStatus() {
            return status;
        }

        /**
         * @param status the status to set
         */
        void setStatus(final TransStatus status) {
            this.status = status;
        }

    }

    @Override
    public synchronized void addFinishedTransaction(final TransStatus status) {
        final String transIdKeyStr = TransIdUtil.INSTANCE.toString(status.getTransID());
        TimeTaggedOrderedProperties timeTagged = null;

        if (status.getRole() == CLASS_1_SENDER || status.getRole() == CLASS_2_SENDER) {
            timeTagged = new TimeTaggedOrderedProperties(MetadataFileUtil.INSTANCE.getUplinkReport(status),
                    status.getFinishTime(), transIdKeyStr, status);
        } else if (status.getRole() == CLASS_1_RECEIVER || status.getRole() == CLASS_2_RECEIVER) {
            timeTagged = new TimeTaggedOrderedProperties(MetadataFileUtil.INSTANCE.getDownlinkReport(status),
                    status.getFinishTime(), transIdKeyStr, status);
        }

        if (timeTagged == null) {
            log.error("Unexpected null TimeTaggedOrderedProperties object");
        } else {
            finishedTransactionsMap.put(transIdKeyStr, timeTagged);
            finishedTransactionsList.add(timeTagged);
        }

    }

    /**
     * Perform expiration check and purge those finished transactions that are deemed expired.
     */
    public synchronized void purgeExpiredTransactions() {
        log.trace("purge check");

        final long thresholdTimestampMillis = System.currentTimeMillis()
                - configurationManager.getFinishedTransactionsHistoryKeepTimeMillis();

        final List<TimeTaggedOrderedProperties> toPurge = new ArrayList<>();

        for (final TimeTaggedOrderedProperties i : finishedTransactionsList) {

            if (i.getTimestamp() < thresholdTimestampMillis) {
                toPurge.add(i);
                log.debug(i.getTransIdKeyStr() + " is being purged");
            } else {
                // Because time-sorted
                break;
            }

        }

        finishedTransactionsList.removeAll(toPurge);
        toPurge.forEach(p -> finishedTransactionsMap.remove(p.getTransIdKeyStr()));
    }

    /**
     * Immediately purge all transactions kept in memory by this manager.
     */
    public synchronized void clear() {
        log.trace("clear");
        finishedTransactionsList.clear();
        finishedTransactionsMap.clear();
    }

    @Override
    public void populateAllFinishedTransactions(final Map<String, OrderedProperties> finishedTransactionsReportMap) {
        finishedTransactionsMap.forEach((k, v) -> finishedTransactionsReportMap.put(k, v.getProperties()));
    }

    @Override
    public void populateMatchingRemoteEntityFinishedTransactions(
            final Map<String, OrderedProperties> finishedTransactionsReportMap, final ID entityId) {
        final Map<String, OrderedProperties> collected = finishedTransactionsMap.entrySet().stream()
                .filter(e -> ((e.getValue().getStatus().getRole() == CLASS_1_RECEIVER
                        || e.getValue().getStatus().getRole() == CLASS_2_RECEIVER)
                        && entityId.equals(e.getValue().getStatus().getTransID().getSource()))
                        || ((e.getValue().getStatus().getRole() == CLASS_1_SENDER
                        || e.getValue().getStatus().getRole() == CLASS_2_SENDER)
                        && entityId.equals(e.getValue().getStatus().getPartnerID())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getProperties()));
        finishedTransactionsReportMap.putAll(collected);
    }

    @Override
    public void populateMatchingServiceClassFinishedTransactions(
            final Map<String, OrderedProperties> finishedTransactionsReportMap, final byte serviceClass) {
        final Map<String, OrderedProperties> collected = finishedTransactionsMap.entrySet().stream()
                .filter(e -> (serviceClass == 1 && (e.getValue().getStatus().getRole() == Role.CLASS_1_RECEIVER
                        || e.getValue().getStatus().getRole() == Role.CLASS_1_SENDER))
                        || (serviceClass == 2 && (e.getValue().getStatus().getRole() == Role.CLASS_2_RECEIVER
                        || e.getValue().getStatus().getRole() == Role.CLASS_2_SENDER)))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getProperties()));
        finishedTransactionsReportMap.putAll(collected);
    }

    @Override
    public void populateMatchingSpecificFinishedTransactions(final Map<String, OrderedProperties> finishedTransactionsReportMap,
                                                             final TransID transactionId) {
        final Map<String, OrderedProperties> collected = finishedTransactionsMap.entrySet().stream()
                .filter(e -> transactionId.equals(e.getValue().getStatus().getTransID()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getProperties()));
        finishedTransactionsReportMap.putAll(collected);
    }

}