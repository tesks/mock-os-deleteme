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
package jpl.gds.automation.auto.cfdp.service;

import jpl.gds.automation.auto.cfdp.config.AutoProxyProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AUTO proxy Timer task for aggregating PDU's to the command sink
 */
public class AutoPduAggregator implements IAutoCfdpService {

    @SuppressWarnings("unused")
    private final ApplicationContext                       appContext;

    private final Tracer                                   log;
    private final CfdpAdapterCache                         cfdpCache;
    private final AutoProxyProperties                      config;

    private final Map<Integer, Map<Integer, List<byte[]>>> pdus;
    private final AtomicBoolean                            started;
    private final AtomicBoolean                            stopped;

    private Timer                                          sendPduTimer;
    private final String                                   me = this.getClass().getName();

    /**
     * Automation proxy CFDP PDU aggregation manager
     * 
     * @param appContext
     *            The current application context8
     */
    public AutoPduAggregator(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.log = TraceManager.getTracer(appContext, Loggers.AUTO_CFDP);
        this.config = appContext.getBean(AutoProxyProperties.class);
        this.cfdpCache = appContext.getBean(CfdpAdapterCache.class);

        this.pdus = new ConcurrentHashMap<>();
        this.started = new AtomicBoolean(false);
        this.stopped = new AtomicBoolean(false);
    }

    private ResponseEntity<Object> queueAndRespond(final List<byte[]> pduList,
                                                                final Map<Integer, List<byte[]>> vcidMap,
                                                   final byte[] pduData, final Integer vcid, final Integer entityId) {
        pduList.add(pduData);
        vcidMap.put(vcid, pduList);

        pdus.put(entityId, vcidMap);
        return new ResponseEntity<>("Queued " + pduData.length + "bytes  for aggregation to entity id " + entityId
                + " with vcid " + vcid, HttpStatus.OK);

    }

    @Override
    public ResponseEntity<Object> aggregatePdus(final Integer entityId, final byte[] pduData, final Integer vcid) {

        if (!pdus.containsKey(entityId)) {
            return queueAndRespond(new CopyOnWriteArrayList<>(), new ConcurrentHashMap<>(),
                                   pduData, vcid, entityId);
        }
        else {
            final Map<Integer, List<byte[]>> vcidMap = pdus.get(entityId);

            if (!vcidMap.containsKey(vcid)) {
                // vcid map exists but no mapping for this vcid yet
                return queueAndRespond(new CopyOnWriteArrayList<>(), vcidMap, pduData, vcid, entityId);
            }
            else {
                final List<byte[]> pduList;
                pduList = vcidMap.get(vcid);

                if (pduList.isEmpty()) {
                    return queueAndRespond(pduList, vcidMap, pduData, vcid, entityId);
                }

                final int currentAggregationSize = getPduListByteLength(pduList);

                log.debug("Handling ", pduData.length, " bytes of PDU data to VCID ", vcid, " for entityId",
                          entityId);
                log.debug("Current aggregation size: ", currentAggregationSize);
                log.debug("Maximum payload configuration: ", config.getMaxPayloadForEntity(entityId));

                // now check if we aggregate or flush
                if (currentAggregationSize + pduData.length < config.getMaxPayloadForEntity(entityId)) {
                    // Have not reached max pay load size yet. Append PDU to list
                    log.debug("Added ", pduData.length, " bytes to current aggregation. ", pduList.size(),
                              " file(s) as ", (currentAggregationSize + pduData.length),
                              " bytes queued for destination ", entityId);
                    return queueAndRespond(pduList, vcidMap, pduData, vcid, entityId);
                }
                else {
                    // Reached max pay load size. Flush current map and append this pdu for next aggregation
                    log.debug("PDU aggregation exceeds maximum payload config. Flushing queue...");
                    log.debug("Added", pduData.length, " bytes to new pdu aggregation for destination ", entityId);

                    flushPdus();
                    vcidMap.clear();
                    pduList.clear();

                    // Reset timer every time a full PDU is built and pushed
                    startTimer(config.getFlushTimer());

                    return queueAndRespond(pduList, vcidMap, pduData, vcid, entityId);
                }
            }
        }
    }

    /**
     * Timer task executed PDU flusher
     */
    private void flushPdus() {
        if (!started.get()) {
            log.debug(me, " flush triggered but service has been stopped.");
            log.debug(pdus.size(), " PDU entries left in queue ");
            return;
        }
        if (pdus.isEmpty()) {
            log.debug(me, " flush triggered but found no PDU's queued");
            return;
        }
        log.debug(me, " flushing ", pdus.size(), " entries...");

        handleFlush();

        pdus.clear();
    }

    /**
     * Handles flush logic executed by the timer task
     */
    private void handleFlush() {
        for (final Entry<Integer, Map<Integer, List<byte[]>>> entityMap : pdus.entrySet()) {
            if (entityMap.getKey() == null || entityMap.getValue().isEmpty()) {
                return;
            }
            final int entityId = entityMap.getKey().intValue();
            final Map<Integer, List<byte[]>> vcidMap = entityMap.getValue();
            log.debug(me, " flushing ", entityId, " found ", vcidMap.size(), " vcid mappings");

            for (final Entry<Integer, List<byte[]>> entry : vcidMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue().isEmpty()) {
                    return;
                }
                final int vcid = entry.getKey().intValue();
                final List<byte[]> pduList = entry.getValue();
                final int aggregationSize = getPduListByteLength(pduList);

                log.debug(me, " flushing vcid entry ", vcid, " containing ", aggregationSize, " bytes");

                final ByteBuffer byteBuffer = ByteBuffer.allocate(aggregationSize);
                for (final byte[] bytes : pduList) {
                    byteBuffer.put(bytes);
                }

                sendPdu(byteBuffer.array(), entityId, vcid);
                log.debug(me, " flushed ", aggregationSize, " bytes to entityId ", entityId);
            }
        }
    }


    /**
     * Gets the total byte size for a list of pdu's
     * 
     * @param pduList
     *            list of pdu data
     * @return length of pdu list (in bytes)
     */
    private int getPduListByteLength(final List<byte[]> pduList) {
        int aggregationSize = 0;
        for (final byte[] bytes : pduList) {
            aggregationSize += bytes.length;
        }
        return aggregationSize;
    }

    /**
     * Flush logic uses this to forward the PDU aggregation to the output adapter
     * 
     * @param pdu
     *            byte representation of pdu(s)
     * @param entityId
     *            destination entity id
     * @param vcid
     *            destination vcid
     */
    private void sendPdu(final byte[] pdu, final int entityId, final int vcid) {
        final IRawOutputAdapter outputAdapter = cfdpCache.get(entityId);
        try {
            outputAdapter.sendPdus(pdu, vcid, config.getScidForEntity(entityId), config.getApidForEntity(entityId));
            log.debug(me, " forwarded ", pdu.length, " bytes of PDU data to destination entity ID ", entityId, " using ",
                    outputAdapter.getClass().getName());
        }
        catch (final UplinkException e) {
            log.error(ExceptionTools.getMessage(e) + "\n" + e.getUplinkResponse().getDiagnosticMessage(), e);
        }
        catch (final IllegalArgumentException e) {
        	log.error(ExceptionTools.getMessage(e));
        }
        
    }

    /**
     * Starts the timer task on the configured interval
     * 
     * @param interval
     *            flush timer rate in seconds
     */
    private void startTimer(final int interval) {
        if (sendPduTimer != null) {
            sendPduTimer.cancel();
            sendPduTimer = null;
        }
        if (interval == 0) {
            log.error("Unable to start PDU aggregation timer with interval ", interval);
            return;
        }

        log.debug("Starting PDU aggregator on a ", interval, " second interval");
        sendPduTimer = new Timer("AUTO PDU Aggregator");
        sendPduTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    flushPdus();
                }
                catch (final Exception e) {
                    log.error("Unknown exception in CFDP PDU Aggregation run() ", ExceptionTools.getMessage(e), e);
                }

            }
        }, interval * 1000L, interval * 1000L);
    }

    @Override
    public boolean startService() {
        if (started.get()) {
            stopped.getAndSet(false);
            return false;
        }

        if (!started.getAndSet(true)) {
            // FlushTimer is global, not per entity
            final int interval = config.getFlushTimer();

            // TODO: Will all destinations flush with the same interval?
            // will different configs have different intervals?

            log.debug("Starting aggregation timer on a ", interval, " second interval");
            startTimer(interval);
        }
        stopped.getAndSet(false);
        return started.get();
    }

    @PreDestroy
    @Override
    public void stopService() {
        log.trace("Shutting down PDU Aggregator service");
        if (!started.get()) {
            return;
        }
        if (stopped.get()){
            return;
        }

        flushPdus();
        started.getAndSet(false);
        stopped.getAndSet(true);
    }

    @Override
    public boolean isRunning() {
        return started.get() && !stopped.get();
    }
}
