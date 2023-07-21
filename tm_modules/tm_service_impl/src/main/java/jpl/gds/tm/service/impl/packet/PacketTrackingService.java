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
package jpl.gds.tm.service.impl.packet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.station.api.StationMessageType;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.IPacketMessageFactory;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;
import jpl.gds.tm.service.api.packet.IPacketTrackingService;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;
import jpl.gds.tm.service.api.packet.PacketSummaryRecord;
import jpl.gds.tm.service.config.TelemetryServiceProperties;


/**
 * PacketTrackingService keeps track of statistics related to packets by
 * examining message flow.
 *
 *
 */
public class PacketTrackingService implements IPacketTrackingService {

    // Apids that have been reported as unrecognized.
    private final ArrayList<Integer> reportedApids = new ArrayList<Integer>();
    private final IMessagePublicationBus messageContext;
    private long validPackets;
    private long idlePackets;
    private long invalidPackets;
    private long productPackets;
    private long cfdpPackets;
    private long ehaPackets;
    private long evrPackets;
    private long frameGaps;
    private long frameRepeats;
    private long frameRegressions;
    private long dataByteCount;
    private final long summaryInterval;
    private SortedSet<Integer> ehaApids;
    private SortedSet<Integer> productApids;
    private SortedSet<Integer> evrApids;
    private SortedSet<Integer> cfdpApids;
    private Timer summaryTimer;
    private boolean started;
    private boolean finalSummaryMessageSent;
    private MessageSubscriber logSubscriber;
    private MessageSubscriber packetSubscriber;
    /*
     * 12/5/13 - MPCS-5555. Added subscriber for station messages.
     */
    private MessageSubscriber stationSubscriber;
    private MessageSubscriber endSubscriber;
    private final IApidDefinitionProvider apidDefs;
    private final Map<String, PacketSummaryRecord> summaryMap = new HashMap<String, PacketSummaryRecord>();
    private Integer sessionDss = null;
    private Integer sessionVcid = null;
    /*
     * 11/25/13 - MPCS-5554. Add station packet counter.
     */
    private long stationPackets;
    private final Tracer tracer;
    private final IPacketMessageFactory pktMessageFactory;
    private final IStatusMessageFactory statusMessageFactory;

    /**
     * Creates an instance of PacketTrackingService. 
     * 
     * @param serviceContext the current application context
     */
    public PacketTrackingService(final ApplicationContext serviceContext) {
        
        final TelemetryServiceProperties config = serviceContext.getBean(TelemetryServiceProperties.class);
        summaryInterval = config.getPacketTrackingReportInterval();
        apidDefs = serviceContext.getBean(IApidDefinitionProvider.class);
        sessionDss = serviceContext.getBean(IContextFilterInformation.class).getDssId();
        sessionVcid = serviceContext.getBean(IContextFilterInformation.class).getVcid();   
        messageContext = serviceContext.getBean(IMessagePublicationBus.class);
        pktMessageFactory = serviceContext.getBean(IPacketMessageFactory.class);
        statusMessageFactory = serviceContext.getBean(IStatusMessageFactory.class);
        tracer = TraceManager.getTracer(serviceContext, Loggers.TLM_INPUT);
    }

    private void initApids() {
        try {
            if (apidDefs != null) {
                /*
                 * 11/25/13 - MPCS-5553. EHA counter was including
                 * only pre-channelized packets. Add decom packets to the count
                 * by including them on the list of EHA APIDs to watch for.
                 */
                final SortedSet<Integer> tempEhaApids = apidDefs.getChannelApids();
                final SortedSet<Integer> tempDecomApids = apidDefs.getDecomApids();
                ehaApids = new TreeSet<>();
                if (tempEhaApids != null) {
                    ehaApids.addAll(tempEhaApids);
                } 
                if (tempDecomApids != null) {
                    ehaApids.addAll(tempDecomApids);
                }
                productApids = apidDefs.getProductApids();
                evrApids = apidDefs.getEvrApids();
                cfdpApids = apidDefs.getCfdpApids();
            } else {
                ehaApids = new TreeSet<>();
                productApids = new TreeSet<>();
                evrApids = new TreeSet<>();
                cfdpApids = new TreeSet<>();
            }
        } catch (final Exception e) {
            tracer.error("Error in packet meter initialization: ", ExceptionTools.getMessage(e), e);
        }
    }

    private void startSummaryTimer() {
        if (!started) {
            if (summaryInterval > 0) {
                /* MPCS-7135 - 3/17/15. Name the timer thread. */
                summaryTimer = new Timer("Packet Summary");
                summaryTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        sendSummaryMessage();
                    }
                }, summaryInterval, summaryInterval);
            }
            started = true;
        }
    }

    @Override
    public boolean startService() {
        validPackets = 0;
        idlePackets = 0;
        invalidPackets = 0;
        productPackets = 0;
        ehaPackets = 0;
        evrPackets = 0;
        cfdpPackets = 0; // 12/14/18 - MPCS-10266. Added cfdp packet counter.
        stationPackets = 0; // 11/25/13 - MPCS-5554. Add station packet counter.
        frameGaps = 0;
        frameRepeats = 0;
        frameRegressions = 0;
        dataByteCount = 0;
        started = false;
        finalSummaryMessageSent = false;
        if (summaryTimer != null) {
            summaryTimer.cancel();
            summaryTimer = null;
        }
        summaryMap.clear();
        initApids();
        subscribeAll();
        return true;
    }

    /**
     * Subscribes to packet-related messages.
     */
    private void subscribeAll() {
        logSubscriber = new LogMessageSubscriber();
        packetSubscriber = new PacketMessageSubscriber();

        stationSubscriber = new StationMessageSubscriber();
        endSubscriber = new EndOfDataMessageSubscriber();
    }


    /**
     * Send unrecognized APID Log message.
     */
    private void sendUnrecognizedApidMessage(final ITelemetryPacketMessage pm) {
        if (!reportedApids.contains(pm.getPacketInfo().getApid())) {
            final String msg = "Unrecognized or unsupported packet APID: " + pm.getPacketInfo().getApid() +
                    ", ERT=" + pm.getPacketInfo().getErt().getFormattedErt(true) + 
                    ", SCLK=" + pm.getPacketInfo().getSclk().toString() + 
                    ", Seq=" + pm.getPacketInfo().getSeqCount() + 
                    ". Packet will not be processed.";
            final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARNING,
                    msg);
            messageContext.publish(logm);
            tracer.log(logm);
            reportedApids.add(pm.getPacketInfo().getApid());
        }
    }

    /**
     * Send unrecognized VCID Log message.
     */
    private void sendUnrecognizedVcidMessage(final ITelemetryPacketMessage pm) {
        final String msg = "Filtered packet VCID: " + pm.getPacketInfo().getVcid() + ", APID=" + pm.getPacketInfo().getApid()
                + ", ERT=" + pm.getPacketInfo().getErt().getFormattedErt(true) + ", SCLK="
                + pm.getPacketInfo().getSclk().toString() + ", Seq=" + pm.getPacketInfo().getSeqCount()
                + ". Packet will not be processed.";
        final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARNING,
                msg);
        messageContext.publish(logm);
        tracer.log(logm);
    }

    /**
     * Send unrecognized station Log message.
     */
    private void sendUnrecognizedDssMessage(final ITelemetryPacketMessage pm) {
        /* MPCS-7289 - 4/30/15.  Get DSSID from packet info instead of DSN info */
        final String msg = "Filtered packet DSS ID: " + pm.getPacketInfo().getDssId() +
                ", APID=" + pm.getPacketInfo().getApid() + 
                ", ERT=" + pm.getPacketInfo().getErt().getFormattedErt(true) + 
                ", SCLK=" + pm.getPacketInfo().getSclk().toString() + 
                ", Seq=" + pm.getPacketInfo().getSeqCount() + 
                ". Packet will not be processed.";
        final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARNING,
                msg);
        messageContext.publish(logm);
        tracer.log(logm);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
    public void stopService() {
        if (summaryTimer != null) {
            summaryTimer.cancel();
            summaryTimer = null;
        }
        // MPCS-9396 - 01/28/18 - prevent this summary message if the final one has been sent already
        if (!finalSummaryMessageSent) {
            sendSummaryMessage();
            finalSummaryMessageSent = true;
        }
        if (endSubscriber != null) {
            messageContext.unsubscribeAll(endSubscriber);
        }
        if (packetSubscriber != null) {
            messageContext.unsubscribeAll(packetSubscriber);
        }
        if (stationSubscriber != null) {
            messageContext.unsubscribeAll(stationSubscriber);
        }
        if (logSubscriber != null) {
            messageContext.unsubscribeAll(logSubscriber);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.packet.IPacketTrackingService#getDataByteCount()
     */
    @Override
    public long getDataByteCount() {
        return dataByteCount;
    }

    /**
     * Sends a packet extract summary message.
     *
     */
    private void sendSummaryMessage() {
        final IPacketSummaryMessage pm = pktMessageFactory.createPacketSummaryMessage(getFrameGaps(), getFrameRegressions(),
                getFrameRepeats(), getFillPackets(), getInvalidPackets(), getValidPackets(), getStationPackets(), getCfdpPackets() ,summaryMap);
        messageContext.publish(pm);
        tracer.log(pm);
        
    }


    @Override
    public long getEhaPackets() {
        return ehaPackets;
    }


    @Override
    public long getEvrPackets() {
        return evrPackets;
    }

    @Override
    public long getCfdpPackets() {
        return cfdpPackets;
    }

    @Override
    public long getFillPackets() {
        return idlePackets;
    }

    @Override
    public long getInvalidPackets() {
        return invalidPackets;
    }

    @Override
    public long getProductPackets() {
        return productPackets;
    }

    @Override
    public long getValidPackets() {
        return validPackets;
    }

    @Override
    public long getStationPackets() {
        return stationPackets;
    }

    @Override
    public long getFrameGaps() {
        return frameGaps;
    }

    @Override
    public long getFrameRegressions() {
        return frameRegressions;
    }

    @Override
    public long getFrameRepeats() {
        return frameRepeats;
    }

    /**
     * PacketMessageSubscriber listens for and responds to packet messages.
     *
     *
     */
    private class PacketMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of PacketMessageSubscriber.
         */
        public PacketMessageSubscriber() {
            messageContext.subscribe(TmServiceMessageType.TelemetryPacket, this);
            /*
             * 11/25/13 - MPCS-5555. Move station packet subscriptions to
             * their own subscriber.
             */
        }

        /**
         * Handles the packet message by updating appropriate packet counters.
         * @param genericMessage the PacketMessage
         */
        @Override
        public void handleMessage(final IMessage genericMessage) {
            startSummaryTimer();
            /*
             * 11/25/13 - MPCS-5555. Move station packet handling to
             * its own subscriber.
             */
            final ITelemetryPacketMessage pm = (ITelemetryPacketMessage)genericMessage;
            String apidName = "Unknown";
            if (pm.getPacketInfo().isFill()) {
                apidName = "Fill";
            } else {
                try {
                    if (apidDefs != null) {
                        /* MPCS-6111 - 5/15/14 - Go through APID definition to get name. */
                        final IApidDefinition def = apidDefs.getApidDefinition(pm.getPacketInfo().getApid());
                        apidName = def == null? "Unknown" : def.getName();
                    } else {
                        apidName = "";
                    }
                } catch (final IllegalArgumentException e) {}
            }
            
            /*
             * MPCS-7289 - 4/30/15. Get VCID from packet info rather than
             * frame info, and DSSID from packet info instead of DSN info below.
             */

            final int apid = pm.getPacketInfo().getApid();
            final String key = pm.getPacketInfo().getVcid() + "/" + apid;
            PacketSummaryRecord sum = summaryMap.get(key);
            final ITelemetryPacketInfo pi = pm.getPacketInfo();
            if (sum == null) {
                sum = new PacketSummaryRecord(apid, apidName, 
                        pi.getScet(), pi.getSclk(), pi.getErt(), pi.getLst(), pm.getPacketInfo().getVcid(),                
                        pm.getPacketInfo().getSeqCount(), 1);
                summaryMap.put(key, sum);
            } else {
                sum.increment(1, pi.getSclk(), pi.getScet(), pi.getErt(), pi.getLst(), pm.getPacketInfo().getSeqCount());
            }

            if (pm.getPacketInfo().isFill()) {
                pm.getPacketInfo().getVcid();                
                idlePackets++;
                validPackets++;
            } else {
                validPackets++;
                if (isEvrApid(apid)) {
                    evrPackets++;
                }
                if (isEhaApid(apid)) {
                    ehaPackets++;
                }
                if (isProductApid(apid)) {
                    productPackets++;
                }
                if (isCfdpApid(apid)) {
                    cfdpPackets++;
                }

                if (!apidDefs.isDefinedApid(apid)) {
                    sendUnrecognizedApidMessage(pm);
                }
                else if (! pm.getPacketInfo().isFromSse() &&
                        !isDefinedVcid(pm.getPacketInfo().getVcid()))
                {
                    sendUnrecognizedVcidMessage(pm);
                }
                else if (!isSessionDss(pm.getPacketInfo().getDssId())) {
                    sendUnrecognizedDssMessage(pm);
                }
            }
            dataByteCount += pm.getPacketInfo().getSize();

        }

        /**
         * Determines whether an APID is an EVR APID
         * 
         * @param apid
         *            the APID to check
         * @return true if it's an EVR APID
         */
        private boolean isEvrApid(final int apid) {
            if (evrApids == null) {
                return false;
            }
            return evrApids.contains(Integer.valueOf(apid));
        }

        /**
         * Determines whether an APID is a product APID
         * 
         * @param apid
         *            the APID to check
         * @return true if it's a Product APID
         */
        private boolean isProductApid(final int apid) {
            if (productApids == null) {
                return false;
            }
            return productApids.contains(Integer.valueOf(apid));
        }

        /**
         * Determines whether an APID is a CFDP APID
         * 
         * @param apid
         *            the APID to check
         * @return true if it's a CFDP APID
         */
        private boolean isCfdpApid(final int apid) {
            return cfdpApids == null ? false : cfdpApids.contains(Integer.valueOf(apid));
        }

        /**
         * Determines whether an APID is a EHA/channel APID
         * 
         * @param apid
         *            the APID to check
         * @return true if it's an EHA/channel APID
         */
        private boolean isEhaApid(final int apid) {
            if (ehaApids == null) {
                return false;
            }
            return ehaApids.contains(Integer.valueOf(apid));
        }
    }

    /**
     * StationMessageSubscriber listens for and responds to station messages.
     *
     *
     */
    private class StationMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of StationMessageSubscriber.
         */
        public StationMessageSubscriber() {
            messageContext.subscribe(StationMessageType.NenStationMonitor, this);
            messageContext.subscribe(StationMessageType.DsnStationMonitor, this);
        }

        /**
         * Handles station message by updating appropriate packet counters.
         * @param genericMessage the station message
         */
        @Override
        public void handleMessage(final IMessage genericMessage) {
            startSummaryTimer();
            stationPackets++; 
        }
    }

    private boolean isDefinedVcid(final Integer vcid)
    {
        return (vcid == null) || (vcid == -1) || sessionVcid == null || vcid == sessionVcid;
    }

    private boolean isSessionDss(final Integer dssid) {
        if (sessionDss == null) { 
            return true;
        }
        return sessionDss == dssid;
    }

    /**
     * EndOfDataMessageSubscriber listens for End of Data and End of Test messages.
     *
     *
     */
    private class EndOfDataMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of EndOfDataMessageSubscriber.
         */
        public EndOfDataMessageSubscriber() {
            messageContext.subscribe(CommonMessageType.EndOfData, this);
        }

        /**
         * Handles a received message by publishing a summary message. This message
         * is only published once for each meter run.
         * @param genericMessage the End of Test or End of Data message to handler
         */
        @Override
        public void handleMessage(final IMessage genericMessage) {
            if (!finalSummaryMessageSent) {
                sendSummaryMessage();
                finalSummaryMessageSent = true;
            }
        }
    }

    /**
     * LogMessageSubscriber listens for External Log messages.
     *
     *
     */
    private class LogMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of LogMessageSubscriber.
         */
        public LogMessageSubscriber() {
            messageContext.subscribe(CommonMessageType.Log, this);
            messageContext.subscribe(TmServiceMessageType.FrameSequenceAnomaly, this);
        }

        /**
         * Handles a received message by extracting relevant meter information from the
         * log message.
         * @param genericMessage the PublishableLogMessage to handle
         */
        @Override
        public void handleMessage(final IMessage genericMessage) {
            final IPublishableLogMessage lm = (IPublishableLogMessage)genericMessage;
            switch(lm.getLogType()) {
            case INVALID_PKT_DATA:
            case INVALID_PKT_HEADER:
                invalidPackets++;
                break;
            case TF_GAP:
                frameGaps++;
                break;
            case TF_REGRESSION:
                frameRegressions++;
                break;
            case TF_REPEAT:
                frameRepeats++;
                break;
            default:
                break;
            }
        }
    }
}


