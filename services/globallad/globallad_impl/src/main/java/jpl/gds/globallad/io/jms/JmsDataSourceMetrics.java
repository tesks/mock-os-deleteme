/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.globallad.io.jms;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collector for the JMS Data Source
 */
public class JmsDataSourceMetrics extends TimerTask {
    private final Tracer            log                 = TraceManager.getTracer(Loggers.GLAD);
    private final AtomicLong        jmsMessagesReceived = new AtomicLong();
    private final AtomicLong        ehaMessagesReceived = new AtomicLong();
    private final AtomicLong        evrMessagesReceived = new AtomicLong();
    private final AtomicLong        tlmMessagesReceived = new AtomicLong();
    private final AtomicLong        jmsProcessed        = new AtomicLong();
    private final SummaryStatistics jmsQueueStats       = new SummaryStatistics();
    private       Timer             timer;

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug(getJmsMetrics().toString());
        }
    }

    /**
     * Get a JSON object that includes all metrics collected
     *
     * @return JMS Metrics JSON Object
     */
    public JsonObject getJmsMetrics() {
        final JsonObjectBuilder statsObj = Json.createObjectBuilder();
        final JsonObject msgStats = Json.createObjectBuilder()
                .add("jms_rx", getJmsRx())
                .add("jms_proc", getJmsProc())
                .add("tlm_rx", getTlmRx())
                .add("eha_rx", getEhaRx())
                .add("evr_rx", getEvrRx())
                .build();
        statsObj.add("messages", msgStats);
        if (jmsQueueStats.getN() > 0) {
            final JsonObject queueStats = Json.createObjectBuilder()
                    .add("n", jmsQueueStats.getN())
                    .add("min", jmsQueueStats.getMin())
                    .add("max", jmsQueueStats.getMax())
                    .add("mean", jmsQueueStats.getMean())
                    .add("stdev", jmsQueueStats.getStandardDeviation())
                    .build();
            statsObj.add("queue", queueStats);
        } else {
            statsObj.add("queue", Json.createObjectBuilder().build());
        }
        return statsObj.build();
    }

    /**
     * Add a sample of JMS queue waiting time in seconds
     *
     * @param time time in seconds
     */
    public void addJmsQueueWaitTime(final double time) {
        jmsQueueStats.addValue(time);
    }

    /**
     * Increment number of JMS messages received
     */
    public void incJmsRx() {
        jmsMessagesReceived.incrementAndGet();
    }

    /**
     * Get number of JMS messages received
     *
     * @return number of JMS messages received
     */
    public long getJmsRx() {
        return jmsMessagesReceived.get();
    }

    /**
     * Increment number of EHA messages received
     */
    public void incEhaRx() {
        ehaMessagesReceived.incrementAndGet();
    }

    /**
     * Get number of EHA messages received
     *
     * @return number of EHA messages received
     */
    public long getEhaRx() {
        return ehaMessagesReceived.get();
    }

    /**
     * Increment number of EVR messages received
     */
    public void incEvrRx() {
        evrMessagesReceived.incrementAndGet();
    }

    /**
     * Get number of EVR messages received
     *
     * @return number of EVR messages received
     */
    public long getEvrRx() {
        return evrMessagesReceived.get();
    }

    /**
     * Increment number of telemetry messages received (EHA and EVR)
     */
    public void incTlmRx() {
        tlmMessagesReceived.incrementAndGet();
    }

    /**
     * Get number of telemetry messages received (EHA and EVR)
     *
     * @return number of telemetry messages received (EHA and EVR)
     */
    public long getTlmRx() {
        return tlmMessagesReceived.get();
    }

    /**
     * Increment number of telemetry messages received by the provided amount
     *
     * @param amount number of telemtry messages to add
     */
    public void addTlmRx(final int amount) {
        tlmMessagesReceived.addAndGet(amount);
    }

    /**
     * Increment number of JMS messages processed
     */
    public void incJmsProc() {
        jmsProcessed.incrementAndGet();
    }

    /**
     * Get number of JMS messages processed
     *
     * @return number of JMS messages processed
     */
    public long getJmsProc() {
        return jmsProcessed.get();
    }

    /**
     * Start the metrics reporting thread. Only outputs messages on DEBUG
     */
    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(this, 0, 5000);
    }

    /**
     * Stop the metrics reporting thread
     */
    public void stop() {
        timer.cancel();
    }
}
