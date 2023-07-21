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
package jpl.gds.watcher.responder.handlers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import jpl.gds.context.api.ISimpleContextConfiguration;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.context.api.message.IStartOfContextMessage;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.Pair;
import jpl.gds.time.api.message.IFswTimeCorrelationMessage;
import jpl.gds.time.api.message.ISseTimeCorrelationMessage;
import jpl.gds.time.api.message.TimeCorrelationMessageType;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.WatcherProperties;
import jpl.gds.watcher.responder.app.MessageResponderApp;
import jpl.gds.watcher.responder.app.TimeCorrelationWatcherApp;

/**
 * Listens for flight software and SSE time correlation messages, correlates them to one
 * another, and computes and displays the skew of flight SCLK from SSE SCLK .
 */
public class TimeCorrelationMessageHandler extends AbstractMessageHandler {

    /**
     * Bias sign.
     */
    public enum Sign {
        /**
         * Positive sign.
         */
        POSITIVE, 
        /**
         * Negative sign.
         */
        NEGATIVE
    }

    /**
     * Interface for handling last ert and sclk skew.
     */
    public static interface SclkSkewHandler {
        /**
         * Interface handling of sclk skew.
         * 
         * @param fswFirstBitErt
         *            fsw first accurate date time
         * @param expectedSclk
         *            expected sclk value
         * @param actualSclk
         *            actual sclk value
         * @param biasValue
         *            bias vale
         * @param biasSign
         *            bias sign
         */
        void handleSclkSkew(final IAccurateDateTime fswFirstBitErt,
                final ISclk expectedSclk, final ISclk actualSclk,
                final ISclk biasValue, final Sign biasSign);

        /**
         * Returns last ert handled.
         * 
         * @return last accurate date time
         */
        IAccurateDateTime lastErtHandled();
    }

    /**
     * Prints out the handled sclk skew.
     */
    public static class SclkSkewOutputter implements SclkSkewHandler {
        private IAccurateDateTime lastErt;

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleSclkSkew(final IAccurateDateTime fswFirstBitErt,
                final ISclk expectedSclk, final ISclk actualSclk, final ISclk biasValue,
                Sign biasSign) {
            boolean defaultPositiveBiasSignUsed = false;

            final StringBuilder buf = new StringBuilder(512);
            buf.append("FirstBitErt=");
            buf.append(fswFirstBitErt.getFormattedErt(false));
            buf.append(" ExpectedSclk(SSE)=");
            buf.append(expectedSclk);
            buf.append(" ActualSclk(FSW)=");
            buf.append(actualSclk);

            ISclk biasedActualSclk = new Sclk(actualSclk);

            if (biasValue != null) {

                if (biasSign == null) {
                    defaultPositiveBiasSignUsed = true;
                    biasSign = Sign.POSITIVE;
                }

                switch (biasSign) {
                case POSITIVE:
                    biasedActualSclk = biasedActualSclk.increment(biasValue.getCoarse(),
                            biasValue.getFine());
                    break;
                case NEGATIVE:
                    biasedActualSclk = biasedActualSclk.decrement(biasValue.getCoarse(),
                            biasValue.getFine());
                    break;
                }

            }

            buf.append(" Delta=");
            final int sign = biasedActualSclk.compareTo(expectedSclk);
            buf.append(sign < 0 ? "[-]" : (sign == 0 ? "[ ]" : "[+]"));
            ISclk absDiff;

            if (sign < 0) {
                absDiff = new Sclk(expectedSclk);
                absDiff.decrement(biasedActualSclk.getCoarse(),
                        biasedActualSclk.getFine());
            } else {
                absDiff = new Sclk(biasedActualSclk);
                absDiff.decrement(expectedSclk.getCoarse(),
                        expectedSclk.getFine());
            }

            buf.append(absDiff);

            if (biasValue != null) {
                buf.append(" (" + (biasSign == Sign.POSITIVE ? "+" : "-")
                        + "biased" + (defaultPositiveBiasSignUsed ? "*" : "")
                        + ")");
            }

            System.out.println(buf.toString());

            this.lastErt = fswFirstBitErt;
        }

        /**
         * {@inheritDoc}
         * 
         * @see jpl.gds.watcher.responder.handlers.TimeCorrelationMessageHandler.SclkSkewHandler#lastErtHandled()
         */
        @Override
        public IAccurateDateTime lastErtHandled() {
            return lastErt;
        }
    }

    /**
     * Custom exception used to signal an interpolation exception.
     */
    public static class InterpolationException extends Exception {

        /**
         * Default serial id number for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new InterpolationException with the exception message
         * set to the input message
         * 
         * @param inputMessage
         *            -- the exception message
         */
        public InterpolationException(final String inputMessage) {

            super(inputMessage);
        }

    }

    /**
     * Custom exception used to signal a future ert exception.
     */
    public static class FutureErtException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new FutureErtException with the exception message set to
         * the input message
         * 
         * @param inputMessage
         *            -- the exception message
         */
        public FutureErtException(final String inputMessage) {

            super(inputMessage);
        }

    }

    /**
     * Custom exception used to signal no sse data exception.
     */
    public static class NoSSEDataException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new FutureErtException with the exception message set to
         * the input message
         * 
         * @param inputMessage
         *            -- the exception message
         */
        public NoSSEDataException(final String inputMessage) {

            super(inputMessage);
        }

    }

    /**
     * Configuration tag for sclk skew bias value.
     */
    public static final String SCLK_SKEW_BIAS_VALUE = "sclkSkewBiasValue";
    /**
     * Configuration tag for sclk skew bias sign.
     */
    public static final String SCLK_SKEW_BIAS_SIGN = "sclkSkewBiasSign";

    private       ISimpleContextConfiguration         currentSession;
    private       long                                fswTimeCorrelationMessageCount;
    private       long                                sseTimeCorrelationMessageCount;
    private       SortedMap<IAccurateDateTime, ISclk> sseTimeCorrelationMap;
    private       SortedMap<IAccurateDateTime, ISclk> queuedFswTimeCorrelations;
    private final SclkSkewHandler                     sclkSkewHandler;
    private       TimeCorrelationWatcherApp           appHelper;
    private       int                                 maxQueuedFswTimeCorrelations = 500;
    private       ISclk                               sclkSkewBiasValue            = null;
    private       Sign                                sclkSkewBiasSign = Sign.POSITIVE;

    private SclkFormatter sclkFmt;
    
    /**
     * Constructors which creates a basic TimeCorrelationMessageHandler.
     * @param appContext the current application context
     */
    public TimeCorrelationMessageHandler(final ApplicationContext appContext) {
        this(appContext, new SclkSkewOutputter());
        sclkFmt = TimeProperties.getInstance().getSclkFormatter();
        externalMessageUtil = appContext.getBean(IExternalMessageUtility.class);
    }

    /**
     * Configurable message handler with a custom  TimeCorrelationMessageHandler.
     * 
     * @param appContext the current application context
     * @param hndlr
     *            sclk skew handler
     */
    public TimeCorrelationMessageHandler(final ApplicationContext appContext, final SclkSkewHandler hndlr) {
    	super(appContext);
    	sclkFmt = TimeProperties.getInstance().getSclkFormatter();
        this.sseTimeCorrelationMap = new TreeMap<>();
        this.queuedFswTimeCorrelations = new TreeMap<>();
        this.sclkSkewHandler = hndlr;
        final WatcherProperties responderProps = new WatcherProperties("chill_time_correlation_watcher", sseFlag);
        
        this.maxQueuedFswTimeCorrelations = responderProps.getQueueLimit();

        final String biasStr = responderProps.getCustomProperty(SCLK_SKEW_BIAS_VALUE, null);
        this.sclkSkewBiasValue = biasStr != null ? sclkFmt.valueOf(biasStr) : null;
        if (this.sclkSkewBiasValue != null
                && this.sclkSkewBiasValue.getCoarse() == 0L
                && this.sclkSkewBiasValue.getFine() == 0L) {
            this.sclkSkewBiasValue = null;
        }

        final String signStr = responderProps.getCustomProperty(SCLK_SKEW_BIAS_SIGN, null);
        if (signStr == null) {

            if (this.sclkSkewBiasValue != null) {
                writeError(SCLK_SKEW_BIAS_SIGN
                        + " property must be defined if bias value given");
                System.exit(1);
            }

        } else {

            if ("-".equalsIgnoreCase(signStr)) {
                this.sclkSkewBiasSign = Sign.NEGATIVE;
            } else if ("+".equalsIgnoreCase(signStr)) {
                this.sclkSkewBiasSign = Sign.POSITIVE;
            } else {
                writeError(SCLK_SKEW_BIAS_SIGN
                        + " property must have value of \"-\" or \"+\"");
                System.exit(1);
            }

        }
    }

    /**
     * Return sorted sse time correlation map.
     * 
     * @return the sseTimeCorrelationMap correlation map
     */
    public SortedMap<IAccurateDateTime, ISclk> getSseTimeCorrelationMap() {
        return sseTimeCorrelationMap;
    }

    /**
     * Gets the current context configuration known to this handler.
     * 
     * @return  context configuration object
     */
    public ISimpleContextConfiguration getCurrentSession() {
        return this.currentSession;
    }

    /**
     * Gets the number of FSW time correlation messages received by this
     * handler.
     * 
     * @return the FSW time correlation message count
     */
    public long getHandledFswTimeCorrelationMessagesCount() {
        return this.fswTimeCorrelationMessageCount;
    }

    /**
     * Gets the number of SSE time correlation messages received by this
     * handler.
     * 
     * @return the SSE time correlation message count
     */
    public long getHandledSseTimeCorrelationMessagesCount() {
        return this.sseTimeCorrelationMessageCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void handleMessage(final IExternalMessage m) {
        try {
            final jpl.gds.shared.message.IMessage[] messages = externalMessageUtil.instantiateMessages(m);
            if (messages != null) {
                for (int i = 0; i < messages.length; i++) {
                    final IMessage currentMsg = messages[i];
                    if (currentMsg.isType(TimeCorrelationMessageType.FswTimeCorrelation)) {
                      
                        handleFswTimeCorrelationMessage(
                                (IFswTimeCorrelationMessage) messages[i],
                                this.sclkSkewHandler);

                    } else if (currentMsg.isType(TimeCorrelationMessageType.SseTimeCorrelation)) {
                        handleSseTimeCorrelationMessage(
                                (ISseTimeCorrelationMessage) messages[i],
                                this.sclkSkewHandler);

                    } else if (currentMsg instanceof IStartOfContextMessage) {
                        startContext((IStartOfContextMessage) messages[i]);

                    } else if (currentMsg instanceof IContextHeartbeatMessage) {
                        startContext((IContextHeartbeatMessage) messages[i]);

                    } else if (currentMsg instanceof IEndOfContextMessage) {
                        handleEndOfContext((IEndOfContextMessage) messages[i]);

                    } else {
                        writeError("TimeCorrelationMessageHandler got an unrecognized message type: "
                                + currentMsg.getType());
                        continue;
                    }
                }
            }
        } catch (final Exception e) {
            writeError("TimeCorrelationMessageHandler could not process message: "
                    + e.toString(), e);
        }
    }

    private void startContext(final IStartOfContextMessage message) {
        final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
        writeLog("TimeCorrelationMessageHandler got Start of Context message for context "
                + newConfig.getContextId().getNumber());
        this.currentSession = newConfig;
    }

    private void startContext(final IContextHeartbeatMessage message) {
        final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
        if (this.currentSession == null
                || !this.currentSession.getContextId().getNumber().equals(
                        newConfig.getContextId().getNumber())) {
            writeLog("TimeCorrelationMessageHandler got first Heartbeat message for context "
                    + newConfig.getContextId().getNumber());
            this.currentSession = newConfig;
        }
    }

    private void handleFswTimeCorrelationMessage(
            final IFswTimeCorrelationMessage message,
            final SclkSkewHandler ssHandler) {
        this.fswTimeCorrelationMessageCount++;
        writeLog("TimeCorrelationMessageHandler got FSW time correlation message: "
                + message.getOneLineSummary());

        final IAccurateDateTime firstBitErt = message.getFrameErt();

        if (ssHandler.lastErtHandled() != null
                && firstBitErt.compareTo(ssHandler.lastErtHandled()) < 0) {
            writeError("TimeCorrelationMessageHandler aborting processing of FSW time correlation message because its firstBitErt ("
                    + firstBitErt.getFormattedErt(true)
                    + ") is earlier than the last ERT processed ("
                    + ssHandler.lastErtHandled().getFormattedErt(true) + ")");
            return;
        }

        final ISclk actualSclk = message.getExpectedSclk();
        ISclk expectedSclk = null;

        try {
            expectedSclk = getExpectedSclk(firstBitErt);
        } catch (final InterpolationException ie) {
            writeError("TimeCorrelationMessageHandler aborting processing of FSW time correlation message because InterpolationException \""
                    + ie.getMessage() + "\" was caught");
            return;
        } catch (final NoSSEDataException nsde) {
            writeWarn("TimeCorrelationMessageHandler queueing this FSW time correlation (ERT="
                    + firstBitErt.getFormattedErt(true)
                    + " SCLK="
                    + actualSclk
                    + ") until SSE data is received");
            queueFswTimeCorrelationAndPurge(firstBitErt, actualSclk);
            return;
        } catch (final FutureErtException fee) {
            writeLog("TimeCorrelationMessageHandler queueing this FSW time correlation: "
                    + fee.getMessage());
            queueFswTimeCorrelationAndPurge(firstBitErt, actualSclk);
            return;
        }

        ssHandler.handleSclkSkew(firstBitErt, expectedSclk, actualSclk,
                this.sclkSkewBiasValue, this.sclkSkewBiasSign);
    }

    private void handleQueuedFswTimeCorrelations(final SclkSkewHandler ssHandler) {

        if (this.sseTimeCorrelationMap.size() < 1) {
            writeLog("TimeCorrelationMessageHandler doesn't have any entries in the SSE table; aborting QUEUED FSW time correlations processing");
            return;
        }

        // Adding 100 nanoseconds to SSE's last key to get an all-inclusive
        // headMap (bound by "successor(highEndpoint)")
        final SortedMap<IAccurateDateTime, ISclk> fswTCsInWindow = this.queuedFswTimeCorrelations
                .headMap(this.sseTimeCorrelationMap.lastKey()
                        .roll(0, 100, true));

        if (fswTCsInWindow.size() == 0) {
            writeLog("TimeCorrelationMessageHandler doesn't have QUEUED FSW time correlations it can process ("
                    + this.queuedFswTimeCorrelations.size() + " left in queue)");
            return;
        }

        writeLog("TimeCorrelationMessageHandler processing "
                + fswTCsInWindow.size() + " QUEUED FSW time correlations");

        int oks = 0;
        int notOks = 0;

        final Iterator<IAccurateDateTime> iterator = fswTCsInWindow.keySet()
                .iterator();

        while (iterator.hasNext()) {
            final IAccurateDateTime queuedFirstBitErt = iterator.next();

            if (queuedFirstBitErt.compareTo(this.sseTimeCorrelationMap
                    .firstKey()) < 0) {
                writeError("TimeCorrelationMessageHandler found QUEUED FSW time correlation ("
                        + queuedFirstBitErt.getFormattedErt(true)
                        + ") that's earlier than first data in SSE table ("
                        + this.sseTimeCorrelationMap.firstKey()
                                .getFormattedErt(true) + ")");
                notOks++;
            } else {
                ISclk expectedSclk = null;

                try {
                    expectedSclk = getExpectedSclk(queuedFirstBitErt);
                    ssHandler.handleSclkSkew(queuedFirstBitErt, expectedSclk,
                            fswTCsInWindow.get(queuedFirstBitErt),
                            this.sclkSkewBiasValue, this.sclkSkewBiasSign);
                    oks++;
                } catch (final InterpolationException ie) {
                    // THIS SHOULD NOT HAPPEN!
                    writeError("TimeCorrelationMessageHandler aborting processing of QUEUED FSW time correlation because InterpolationException \""
                            + ie.getMessage() + "\" was caught");
                    notOks++;
                } catch (final NoSSEDataException nsde) {
                    // THIS SHOULD NOT HAPPEN! BUT KEEP TC QUEUED
                    writeError("TimeCorrelationMessageHandler aborting processing of QUEUED FSW time correlation because no SSE data exists");
                    notOks++;
                    continue;
                } catch (final FutureErtException fee) {
                    // THIS SHOULD NOT HAPPEN!
                    writeError("TimeCorrelationMessageHandler aborting processing of QUEUED FSW time correlation because it occurs in the future: "
                            + fee.getMessage());
                    notOks++;
                }

            }

            iterator.remove();
        }

        logQueuedFswTimeCorrelationSummary(oks, notOks);
    }

    private void logQueuedFswTimeCorrelationSummary(final int oks,
            final int notOks) {
        writeLog("TimeCorrelationMessageHandler processed "
                + oks
                + " QUEUED FSW time correlations okay"
                + (notOks > 0 ? (", but encountered problems with " + notOks)
                        : "") + " (" + this.queuedFswTimeCorrelations.size()
                + " left in queue)");
    }

    private void handleSseTimeCorrelationMessage(
            final ISseTimeCorrelationMessage message,
            final SclkSkewHandler ssHandler) {
        this.sseTimeCorrelationMessageCount++;
        writeLog("TimeCorrelationMessageHandler got SSE time correlation message: "
                + message.getOneLineSummary());

        final List<Pair<ISclk, IAccurateDateTime>> timeEntries = message
                .getTimeEntries();

        if (timeEntries == null) {
            writeError("TimeCorrelationMessageHandler aborting processing of SSE time correlation message because it doesn't include time entries");
            return;
        }

        int entriesAdded = 0;

        for (final Pair<ISclk, IAccurateDateTime> pair : timeEntries) {
            final IAccurateDateTime key = pair.getTwo();
            final ISclk value = pair.getOne();

            if (key == null) {
                writeError("TimeCorrelationMessageHandler aborting processing of SSE time correlation message because time entry contains null ERT");
                logSseTimeCorrelationSummary(entriesAdded);
                return;
            }

            if (value == null) {
                writeError("TimeCorrelationMessageHandler aborting processing of SSE time correlation message because time entry contains null SCLK");
                logSseTimeCorrelationSummary(entriesAdded);
                return;
            }

            this.sseTimeCorrelationMap.put(key, value);
            entriesAdded++;
        }

        logSseTimeCorrelationSummary(entriesAdded);
        handleQueuedFswTimeCorrelations(ssHandler);
        purgeOldSseTimeCorrelations(ssHandler.lastErtHandled());
    }

    private void purgeOldSseTimeCorrelations(
            final IAccurateDateTime lastErtHandled) {

        if (lastErtHandled == null) {
            return;
        }

        SortedMap<IAccurateDateTime, ISclk> hm;

        try {

            if (this.sseTimeCorrelationMap.containsKey(lastErtHandled)) {
                hm = this.sseTimeCorrelationMap.headMap(lastErtHandled);
            } else {
                hm = this.sseTimeCorrelationMap
                        .headMap(this.sseTimeCorrelationMap.headMap(
                                lastErtHandled).lastKey());
            }

        } catch (final Exception e) {
            writeError("TimeCorrelationMessageHandler encountered exception while attempting to purge old SSE data: "
                    + e.getMessage());
            return;
        }

        if (hm.size() > 0) {
            writeLog("TimeCorrelationMessageHandler purging " + hm.size()
                    + " old SSE data, from "
                    + hm.firstKey().getFormattedErt(true) + " to "
                    + hm.lastKey().getFormattedErt(true));
            hm.clear();
        } else {
            writeLog("TimeCorrelationMessageHandler doesn't have old SSE data to purge");
        }

    }

    private void logSseTimeCorrelationSummary(final int entriesAdded) {
        writeLog("TimeCorrelationMessageHandler added " + entriesAdded
                + " time correlation entries from the SSE message; "
                + this.sseTimeCorrelationMap.size() + " entries now in table");
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.watcher.IMessageHandler#shutdown()
     */
    @Override
    public synchronized void shutdown() {
        writeLog("TimeCorrelationMessageHandler is shutting down");
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.watcher.IMessageHandler#handleEndOfContext(IEndOfContextMessage)
     */
    @Override
    public synchronized void handleEndOfContext(final IEndOfContextMessage m) {
        final IContextKey tc = m.getContextKey();
        if (tc == null || tc.getNumber() == null) {
            writeLog("TimeCorrelationMessageHandler received End of Context message for an unknown context; skipping");
            return;
        }
        writeLog("TimeCorrelationMessageHandler received End of Context message for context "
                + tc.getNumber());
        this.currentSession = null;
        if (this.appHelper != null && this.appHelper.isExitSession()) {
            shutdown();
            MessageResponderApp.getInstance().markDone();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.watcher.IMessageHandler#setAppHelper(jpl.gds.watcher.IResponderAppHelper)
     */
    @Override
    public void setAppHelper(final IResponderAppHelper app) {
        this.appHelper = (TimeCorrelationWatcherApp) app;
    }
    
    /**
     * Returns the expected sclk from the date time.
     * 
     * @param fswErt
     *            Fsw ert date time
     * @return expected sclk
     * @throws InterpolationException
     *             thrown if an interpolation exception is encountered
     * @throws NoSSEDataException
     *             thrown if a no sse data exception is encountered
     * @throws FutureErtException
     *             thrown if a future ert exception is encountered
     */
    public ISclk getExpectedSclk(final IAccurateDateTime fswErt)
            throws InterpolationException, NoSSEDataException,
            FutureErtException {

        if (this.sseTimeCorrelationMap.size() == 0) {
            throw new NoSSEDataException(
                    "No SSE time correlation data exists at this point");
        }

        final ISclk sclk = this.sseTimeCorrelationMap.get(fswErt);

        if (sclk != null) {
            // No need to interpolate
            return sclk;
        }

        final SortedMap<IAccurateDateTime, ISclk> hm = this.sseTimeCorrelationMap
                .headMap(fswErt);

        if (hm.size() < 1) {
            throw new InterpolationException(
                    "FSW ERT falls before earliest SSE time correlation data (fswErt="
                            + fswErt.getFormattedErt(false)
                            + " sseErt="
                            + this.sseTimeCorrelationMap.firstKey()
                                    .getFormattedErt(false));
        }

        final SortedMap<IAccurateDateTime, ISclk> tm = this.sseTimeCorrelationMap
                .tailMap(fswErt);

        if (tm.size() < 1) {
            throw new FutureErtException(
                    "FSW ERT falls after latest SSE time correlation data (fswErt="
                            + fswErt.getFormattedErt(false)
                            + " sseErt="
                            + this.sseTimeCorrelationMap.lastKey()
                                    .getFormattedErt(false) + ")");
        }

        // Can interpolate
        final IAccurateDateTime erth = hm.lastKey();
        final ISclk sclkh = hm.get(erth);
        final IAccurateDateTime ertt = tm.firstKey();
        final ISclk sclkt = tm.get(ertt);

        return linearlyInterpolateBetweenTwoPoints(fswErt, erth, sclkh, ertt,
                sclkt);
    }

    /**
     * Returns a sclk which is interpolated between two points from a starting
     * sclk.
     * 
     * @param x
     *            starting time
     * @param x0
     *            x value of the first point
     * @param y0
     *            y value of the first point
     * @param x1
     *            x value of the second point
     * @param y1
     *            y value of the second point
     * @return interpolated sclk
     * @throws InterpolationException
     *             thrown if an interpolated exception is encountered
     */
    public static ISclk linearlyInterpolateBetweenTwoPoints(final IAccurateDateTime x, final IAccurateDateTime x0,
                                                            final ISclk y0, final IAccurateDateTime x1, final ISclk y1)
            throws InterpolationException {

        if (x1.compareTo(x0) < 0) {
            throw new InterpolationException(
                    "Can't interpolate decreasing ERT values: ert0=" + x0
                            + " ert1=" + x1);
        }

        if (x.compareTo(x0) < 0 || x.compareTo(x1) > 0) {
            throw new InterpolationException(
                    "Can't interpolate; out of range: ert=" + x + " ert0=" + x0
                            + " ert1=" + x1);
        }

        if (x.compareTo(x0) == 0) {
            return new Sclk(y0);
        } else if (x.compareTo(x1) == 0) {
            return new Sclk(y1);
        }

        // Find y1 - y0
        if (y1.compareTo(y0) < 0) {
            throw new InterpolationException(
                    "Can't interpolate decreasing sclk values: sclk0=" + y0
                            + " sclk1=" + y1);
        }

        ISclk num = new Sclk(y1);
        num = num.decrement(y0.getCoarse(), y0.getFine());

        // Find x1 - x0
        final IAccurateDateTime den = x1
                .roll(x0.getTime(), x0.getNanoseconds(), false);

        // Find x - x0
        final IAccurateDateTime mul = x.roll(x0.getTime(), x0.getNanoseconds(), false);

        // Find (x-x0)/(x1-x0)
        final BigDecimal mulBd = BigDecimal.valueOf(mul.getTime());
        final BigDecimal mulMicroTenthsBd = mulBd.movePointRight(4).add(
                BigDecimal.valueOf(mul.getNanoseconds() / 100));
        final BigDecimal denBd = BigDecimal.valueOf(den.getTime());
        final BigDecimal denMicroTenthsBd = denBd.movePointRight(4).add(
                BigDecimal.valueOf(den.getNanoseconds() / 100));
        final BigDecimal facBd = mulMicroTenthsBd.divide(
                denMicroTenthsBd,
                new MathContext(Math.min(mulMicroTenthsBd.precision(),
                        denMicroTenthsBd.precision())));

        // Multiply factor with y1-y0
        final BigDecimal numFineBd = BigDecimal.valueOf(num.getFine());
        final BigDecimal numFineFractionDenominatorBd = BigDecimal.valueOf(
                num.getFineUpperLimit() + 1);
        // Upper bound's precision * 2 should be enough
        final BigDecimal numFineDecBd = numFineBd.divide(
                numFineFractionDenominatorBd, new MathContext(
                        numFineFractionDenominatorBd.precision() * 2));
        final BigDecimal numCoarseBd = BigDecimal.valueOf(num.getCoarse());
        final BigDecimal numBd = numCoarseBd.add(numFineDecBd);
        final BigDecimal rightBd = numBd.multiply(facBd);
        final long addCoarse = rightBd.longValue();
        final BigDecimal rightFineDecBd = rightBd.subtract(BigDecimal.valueOf(addCoarse));
        BigDecimal rightFineBd = rightFineDecBd
                .multiply(numFineFractionDenominatorBd);
        final int decimalPointIndex = rightFineBd.toString().indexOf('.');

        if (decimalPointIndex > -1) {
            rightFineBd = rightFineBd.round(new MathContext(
                    decimalPointIndex == 0 ? 1 : decimalPointIndex));
        }

        final long addFine = rightFineBd.longValue();
        return y0.increment(addCoarse, addFine);
    }

    /**
     * Calculate the first bit of the ert
     * 
     * @param lastBitErt
     *            last bit of the ert used
     * @param frameLengthBytes
     *            length of bytes in the frame
     * @param bitRate
     *            the bitrate of the frame
     * @return the calculated first bit ert
     */
    public static IAccurateDateTime calculateFirstBitErt(
            final IAccurateDateTime lastBitErt, final double frameLengthBytes,
            final double bitRate) {

        if (lastBitErt == null) {
            throw new IllegalArgumentException("null lastBitErt");
        } else if (bitRate <= 0.0) {
            throw new IllegalArgumentException("Illegal bitRate: " + bitRate);
        }

        // Formula: firstBitErt = lastBitErt - length / bitRate

        final BigDecimal lenBd = BigDecimal.valueOf(frameLengthBytes);
        final BigDecimal brBd = BigDecimal.valueOf(bitRate);

        // Note: Multiplying by 80 000 000 to convert to micro-tenths, and from
        // bytes to bits
        // Note: MatchContext(20) sets the precision to 20 digits, which should
        // be big enough
        BigDecimal microTenthsElapsed = lenBd
                .multiply(BigDecimal.valueOf(80000000)).divide(brBd,
                        new MathContext(20));

        if (microTenthsElapsed.compareTo(BigDecimal.valueOf(0.0)) < 0) {
            throw new ArithmeticException("Time elapsed is negative");
        }

        // Round to the nearest whole number, first by counting how many digits
        // are
        // left of the decimal point, if any
        final int decimalPointIndex = microTenthsElapsed.toString().indexOf('.');

        if (decimalPointIndex > -1) {
            microTenthsElapsed = microTenthsElapsed.round(new MathContext(
                    decimalPointIndex == 0 ? 1 : decimalPointIndex));
        }

        // Find the milliseconds
        final long millisElapsed = microTenthsElapsed.movePointLeft(4).longValue();
        final long nanosElapsed = microTenthsElapsed.remainder(BigDecimal.valueOf(10000))
                .longValue() * 100;

        return lastBitErt.roll(millisElapsed, nanosElapsed, false);
    }

    /**
     * Sets the sse time correlation map.
     * 
     * @param sseTimeCorrelationMap
     *            the sseTimeCorrelationMap to set
     */
    public void setSseTimeCorrelationMap(
                                         final SortedMap<IAccurateDateTime, ISclk> sseTimeCorrelationMap) {
        this.sseTimeCorrelationMap = sseTimeCorrelationMap;
    }

    /**
     * Sets the queued fsw time correlations.
     * 
     * @param queuedFswTimeCorrelations
     *            the queuedFswTimeCorrelations to set
     */
    public void setQueuedFswTimeCorrelations(
                                             final SortedMap<IAccurateDateTime, ISclk> queuedFswTimeCorrelations) {
        this.queuedFswTimeCorrelations = queuedFswTimeCorrelations;
    }

    /**
     * Returns the queued fsw time correlations.
     * 
     * @return the queuedFswTimeCorrelations
     */
    public SortedMap<IAccurateDateTime, ISclk> getQueuedFswTimeCorrelations() {
        return queuedFswTimeCorrelations;
    }

    /**
     * Purge the queue of fsw time correlations based on the passed ranges.
     * 
     * @param ertToQueue
     *            the FSW time correlation ERT to queue
     * @param sclkToQueue
     *            the FSW time correlation SCLK to queue
     */
    private void queueFswTimeCorrelationAndPurge(
                                                 final IAccurateDateTime ertToQueue, final ISclk sclkToQueue) {
        this.queuedFswTimeCorrelations.put(ertToQueue, sclkToQueue);

        int purgedCount = 0;

        while (this.queuedFswTimeCorrelations.size() > this.maxQueuedFswTimeCorrelations) {
            this.queuedFswTimeCorrelations
                    .remove(this.queuedFswTimeCorrelations.firstKey());
            purgedCount++;
        }

        writeLog("TimeCorrelationMessageHandler queued 1 FSW time correlation"
                + (purgedCount > 0 ? (" and purged " + purgedCount) : "")
                + "; current queue size is "
                + this.queuedFswTimeCorrelations.size());
    }

}