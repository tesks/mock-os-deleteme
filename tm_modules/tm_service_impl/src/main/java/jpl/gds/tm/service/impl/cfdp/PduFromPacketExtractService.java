/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.tm.service.impl.cfdp;

import java.util.Arrays;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduFactory;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.client.apid.IApidUtilityDictionaryManager;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpMessageFactory;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;
import jpl.gds.tm.service.api.cfdp.IPduExtractService;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This is a PDU extractor that builds PDUs from CCSDS packets. It subscribes to packet messages and publishes PDU
 * messages.
 * Due to the structure of V2 M_PDU frames, this service is also used to publish PDU messages from the packets contained
 * within.
 *
 *
 */
public class PduFromPacketExtractService implements IPduExtractService {

    private final IMessagePublicationBus bus;
    private final ICfdpMessageFactory    cfdpMsgFactory;
    private final IStatusMessageFactory  statusMsgFactory;
    private final Tracer                 pduExLog;
    private final ICfdpPduFactory        pduFactory;
    private final IContextConfiguration  contextConfig;
    private MessageSubscriber            subscriber;

    /**
     * Maximum size of a pdu
     */
    private static final int             MAX_PDU_LENGTH = ICfdpPduHeader.MAX_PDU_DATA_LENGTH;

    private int allowedVcid;

    /**
     * Statistics and tracking
     */
    private long                         numberOfPkts         = 0;
    private long                         numberOfPdus         = 0;
    private long                         numberOfPdusInvalid  = 0;
    private int                          sourceVcid           = -1;
    private int                          sourceApid           = -1;
    private int                          sourceDssId          = 0;

    /**
     * info on current Pkt
     */
    private ITelemetryPacketMessage      currentPktMessage    = null;

    /**
     * Current read offset into current packet
     */
    private int                          offsetCurrentPktRead = 0;

    // Virtual Frame Counter
    private int                          counterSourcePacket  = -1;

    // The size of the current packet*/
    private int                          numPacketBytes       = -1;

    /**
     * info on current pdu
     */
    private ICfdpPduHeader               pduHeader            = null;
    private boolean                      pduHeaderValid       = false;
    // expected number of bytes for current PDU
    private int                          pduLength            = 0;
    // number of bytes read of current PDU
    private int                          pduBytesRead         = 0;
    private final byte[]                 pduBuffer            = new byte[MAX_PDU_LENGTH];

    private IApidUtilityDictionaryManager dictManager;

    /**
     * constructor for this service.
     *
     * @param serviceContext
     *            the application context in which this service is to be used
     * @param vcid2View
     *            the VCID this service will monitor
     */
    public PduFromPacketExtractService(final ApplicationContext serviceContext, final int vcid2View) {

        this.bus = serviceContext.getBean(IMessagePublicationBus.class);
        this.cfdpMsgFactory = serviceContext.getBean(ICfdpMessageFactory.class);
        this.statusMsgFactory = serviceContext.getBean(IStatusMessageFactory.class);
        allowedVcid = vcid2View;
        if (vcid2View < 0) {
            allowedVcid = 0;
        }
        pduExLog = TraceManager.getTracer(serviceContext, Loggers.PDU_EXTRACTOR);
        pduFactory = serviceContext.getBean(ICfdpPduFactory.class);
        contextConfig = serviceContext.getBean(IContextConfiguration.class);
        dictManager = serviceContext.getBean(IApidUtilityDictionaryManager.class);
    }

    @Override
    public boolean startService() {
        subscriber = new PduExtractMessageSubscriber();
        return true;
    }

    @Override
    public void stopService() {
        flushCurrentPdus();
        if (subscriber != null) {
            bus.unsubscribeAll(subscriber);
        }
    }

    /**
     * Gets the virtual channel ID of the virtual channel handled by the PduExtract instance.
     *
     * @return VCID
     */
    public long getVcid() {
        return allowedVcid;
    }

    private void flushCurrentPdus() {
        flushCurrentPdu(true);
    }

    /**
     * Gets the number of PDUs constructed, excluding idle PDUs.
     *
     * @return the number of PDUs
     */
    public long getNumPdus() {
        return numberOfPdus;
    }

    /**
     * Gets the number of invalid PDUs found.
     *
     * @return the number of invalid PDUs
     */
    public long getNumInvalidPdus() {
        return numberOfPdusInvalid;
    }

    /**
     * Gets the number of packets received.
     *
     * @return the number of packets
     */
    public long getNumPackets() {
        return numberOfPkts;
    }

    /**
     * The internal message bus subscribe invokes this method to consume a packet message and attempt to extract a PDU
     *
     * @param pktMsg
     *            an ITelemetryPacketMessage containing a CFDP PDU
     */
    public void consume(final ITelemetryPacketMessage pktMsg) {

        /*
         * Ignore packets for non-session VCID or wrong SCID or wrong DSSID.
         */
        if (!contextConfig.accept(pktMsg)) {
            return;
        }

        /*
         * Ignore packets that are not on this PacketExtract's VirtualChannel
         */
        if (allowedVcid != pktMsg.getVcid()) {
            return;
        }

        //MPCS-10788 - 4/15/19: If packet APID is not CFDP APID, discard
        //Note dictionary has to be loaded first before calling getCfdpApids()
        if (dictManager.getCfdpApids() == null || !dictManager.getCfdpApids().contains(pktMsg.getPacketInfo().getApid())) {
            return;
        }

        /*
         * Valid packet for this extractor. Keep count of packets processed
         */
        ++numberOfPkts;

        /*
         * If Pkt is fill, then discard
         */
        if (pktMsg.getPacketInfo().isFill()) {
            return;
        }

        /*
         * Reset packet bookkeeping to start of packet
         */
        currentPktMessage = pktMsg;
        offsetCurrentPktRead = pktMsg.getPacketInfo().getPrimaryHeaderLength();
        if (pktMsg.getPacketInfo().getSecondaryHeaderFlag()) {
            offsetCurrentPktRead += pktMsg.getPacketInfo().getSecondaryHeaderLength();
        }
        counterSourcePacket = currentPktMessage.getPacketInfo().getSeqCount();
        numPacketBytes = currentPktMessage.getNumBytes();

        // only one PDU per packet
        buildNextPdu();
    }

    private void buildNextPdu() {

        final int spsc = currentPktMessage.getPacketInfo().getSeqCount();

        sourceVcid = currentPktMessage.getVcid();
        sourceApid = currentPktMessage.getPacketInfo().getApid();

        if (pduExLog.isDebugEnabled()) {
            final String msg = String.format("==============> NEW PKT: vcfc(s)=%s, SPSC=%-6d, vcid=%-6d, apid=%-6d, dssid=%-6d, ERT=%s",
                                             currentPktMessage.getPacketInfo().getSourceVcfcs(), spsc, sourceVcid,
                                             sourceApid, sourceDssId, currentPktMessage.getPacketInfo().getErt());
            final StringBuilder hr = new StringBuilder();
            for (int i = 0; i < msg.length(); i++) {
                hr.append('=');
            }
            pduExLog.debug("\n" + hr);
            pduExLog.debug(msg);
            pduExLog.debug(hr);

            pduExLog.debug(String.format("  vcfc(s)=%s, apid=%-6d, spsc=%-6d",
                                         currentPktMessage.getPacketInfo().getSourceVcfcs(), sourceApid, spsc));
        }

        /*
         * Verify the packet has enough room to contain the pdu header
         */
        pduExLog.debug("      PROCESSING PDU HEADER...");

        /*
         * Check the availability of data in the current Pkt
         */

        if ((numPacketBytes - offsetCurrentPktRead) >= ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH) {

            // There is enough room

            if (copyBytesFromPkttoPDUBuffer(ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH)) {
                // Header completely processed.
                pduExLog.debug("        FIXED PDU HEADER COMPLETE");
                buildPduHeader();
            }
        }
        else {
            /**
             * There is NOT enough room in the packet for the fixed PDU header. PDUs don't span packets.
             */
            offsetCurrentPktRead = numPacketBytes;
            resetCurrentPdu();
            pduExLog.debug("      incomplete PDU, aborting...");
            return;
        }

        /*
         * Fixed header complete. Is there room in this packet for the variable PDU header?
         */

        if ((numPacketBytes - offsetCurrentPktRead) >= pduHeader.getHeaderLength()
                - ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH) {
            // There is enough room

            if (copyBytesFromPkttoPDUBuffer(pduHeader.getHeaderLength() - ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH)) {
                /*
                 * Variable header completely processed. PDU has been initialized. Time to build PDU body.
                 */
                pduExLog.debug("        VARIABLE PDU HEADER COMPLETE");
                populateVariablePduHeader();
            }
        }
        else {
            /**
             * There is NOT enough room in the packet for the fixed PDU header. PDUs don't span packets.
             */
            offsetCurrentPktRead = numPacketBytes;
            resetCurrentPdu();
            pduExLog.debug("      incomplete PDU, aborting...");
            return;
        }

        /*
         * When here, a complete pdu header has been processed, and the data portion of the pdu is
         * ready for processing.
         *
         * offsetCurrentPktRead --> first byte of pdu data (the byte after the end of the pdu header) pduBytesRead =
         * number of bytes contained in the pduBuffer (the complete pdu header).
         */

        /*
         * Attempt to build the full PDU data with the remaining data in the packet.
         */
        buildPduBody();
    }

    /**
     * Builds onto the current PDU (i.e., builds the data block)
     *
     * This method may not be called unless and until the entire header of the pdu being built has been completely read.
     *
     * This method requires that the contents of the pduHeader global variable has already been set.
     */
    private boolean buildPduBody() {
        /*
         * Check integrity of data structures before attempting to build a pdu.
         */
        if (!pduHeaderValid) {
            pduExLog.error("Internal PDU Extraction Error: PDU Header is not valid when attempting to build a pdu.");

            /*
             * Abort further processing of current Pkt
             */
            offsetCurrentPktRead = numPacketBytes;
            return false;
        }

        /*
         * DEBUG
         */
        pduExLog.debug("      PROCESSING PDU BODY...");

        /*
         * some PDU sanity checks; Verifies some constant values in PDU header are as expected.
         * pduHeaderValid just validates that the correct number of header bytes are present
         */
        if (!pduHeader.isValid()) {
            sendInvalidMessage();

            if (pduExLog.isDebugEnabled()) {
                pduExLog.debug("          PDU FAILED SANITY CHECKS");
                pduExLog.debug("            valid=" + pduHeader.isValid() + ", pduLength=" + pduLength
                                       + " (pktlen > MAX_PDU_LENGTH: " + (pduLength > MAX_PDU_LENGTH) + ")");
            }
            flushCurrentPdu(false);

            /*
             * Abort further processing of current Pkt
             */
            offsetCurrentPktRead = numPacketBytes;
            return false;
        }

        /*
         * Check to see if there are enough bytes to complete the current pdu based upon its length.
         */
        if (offsetCurrentPktRead + (pduLength - pduBytesRead) <= numPacketBytes) {
            /*
             * PDU is complete in this Pkt
             */

            if (copyBytesFromPkttoPDUBuffer(pduLength - pduBytesRead)) {
                if (pduExLog.isDebugEnabled()) {
                    pduExLog.debug("        PDU COMPLETELY PROCESSED IN Pkt:\n\n" + pduHeader);
                }
                sendPduMessage();
            }
        }
        /*
         * If not, the PDU needs to be discarded
         */
        else {
            flushCurrentPdu(true);
        }
        /*
         * PDUs don't span packets
         */
        offsetCurrentPktRead = numPacketBytes;
        return true;
    }

    /**
     * Copy specified number of bytes from Pkt to PDU Buffer
     *
     * @return true on success, false on failure
     */
    private boolean copyBytesFromPkttoPDUBuffer(final int count) {
        try {
            if (pduExLog.isDebugEnabled()) {
                pduExLog.debug("        COPYING " + count + " BYTES TO PDU BUFFER...");
            }
            System.arraycopy(currentPktMessage.getPacket(), offsetCurrentPktRead, pduBuffer, pduBytesRead, count);
            offsetCurrentPktRead += count;
            pduBytesRead += count;

            /*
             * If this is the first data put into this pdu, set the pdu's DSN info.
             */
            if ((count > 0) && (pduBytesRead == count)) {
                if (pduExLog.isDebugEnabled()) {
                    pduExLog.debug("          >>> ERT set to \"" + currentPktMessage.getPacketInfo().getErt()
                                           + "\" from originating vcfc(s)=" + currentPktMessage.getPacketInfo().getSourceVcfcs());
                }
            }
            return true;
        }
        catch (final ArrayIndexOutOfBoundsException e) {
            if (pduExLog.isDebugEnabled()) {
                pduExLog.debug("          PDU BUFFER OVERRUN (PDU SIZE EXCEEDS MAX_PDU_LENGTH): Pkt="
                                       + currentPktMessage.getPacketInfo().getSeqCount() + ", offsetCurrentPktRead="
                                       + offsetCurrentPktRead + ", pduBytesRead=" + pduBytesRead + ", count=" + count);
            }

            /*
             * Output an external log message.
             */
            final String msg = "PDU buffer overrun. (PDU size exceeds MAX_PDU_LENGTH): spsc="
                    + currentPktMessage.getPacketInfo().getSeqCount() + ", offsetCurrentPktRead=" + offsetCurrentPktRead
                    + ", pduBytesRead=" + pduBytesRead + ", count=" + count;
            final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
                                                                                             LogMessageType.INVALID_PKT_DATA);
            pduExLog.log(logm);
            numberOfPdusInvalid++;

            if (pduExLog.isDebugEnabled()) {
                pduExLog.debug(e);
            }
            flushCurrentPdu(true);

            /*
             * Abort further processing of current Pkt
             */
            offsetCurrentPktRead = numPacketBytes;
            return false;
        }
    }

    /**
     * Build the fixed portions of the pdu header from the current values in pduBuffer.
     */
    private void buildPduHeader() {
        pduHeader = pduFactory.createPduHeader();
        pduHeader.loadFixedHeader(pduBuffer, 0);
        pduLength = pduHeader.getHeaderLength() + pduHeader.getDataLength();
        if (pduExLog.isDebugEnabled()) {
            pduExLog.debug("FIXED PDU HEADER DUMP: " + pduHeader);
        }
    }

    /**
     * Build the variable pdu header from the current values in pduBuffer.
     * Sets the global 'pduHeaderValid' flag to true.
     */
    private void populateVariablePduHeader() {
        pduHeader.load(pduBuffer, 0);
        pduHeaderValid = true;
        if (pduExLog.isDebugEnabled()) {
            pduExLog.debug("VARIABLE PDU HEADER DUMP: " + pduHeader);
        }
    }

    /**
     * Clear any current pdu statistics and bookkeeping that may have been in progress.
     */
    private void resetCurrentPdu() {
        pduHeaderValid = false;
        pduHeader = null;
        pduLength = 0;
        pduBytesRead = 0;
        sourceVcid = -1;
        sourceApid = -1;
        sourceDssId = 0;
        Arrays.fill(pduBuffer, (byte)0);
    }

    /**
     * flushCurrentPdu() is called when some condition means we discard the current pdu that we are building.
     */
    private void flushCurrentPdu(final boolean goodHeader) {
        if (pduBytesRead > 0) {
            if (!goodHeader) {
                if (pduExLog.isDebugEnabled()) {
                    pduExLog.debug("\n\n*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n"
                                           + "<<< NO HEADER >>>\n"
                                           + "*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n");
                }
                sendInvalidPduMessage();
            }
            else {
                if (pduExLog.isDebugEnabled()) {
                    pduExLog.debug("\n\n*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n"
                                           + pduHeader + "*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n");
                }
                sendInvalidDataMessage();
            }
        }

        /*
         * Clear any current pdu statistics and bookkeeping that may have been in progress.
         */
        resetCurrentPdu();
    }

    /**
     * Publish pdu as a pdu message, reset for next pdu
     */
    private void sendPduMessage() {

        final ICfdpPdu pdu = pduFactory.createPdu(pduHeader, pduBuffer);

        // MPCS-9950 - 07/16/18 - Pass current packet object and context
        final ICfdpPduMessage pduM = cfdpMsgFactory.createPduMessage(currentPktMessage,
                                                                     pdu, contextConfig);

        try {
            bus.publish(pduM);
            pduExLog.debug(pduM);
            this.numberOfPdus++;
        }
        catch (final Exception e) {
            pduExLog.error("Error processing valid pdu: " + pdu.toString(),e);
        }
        resetCurrentPdu();
    }

    /**
     * Send invalid pdu; currently just log a message
     */
    private void sendInvalidPduMessage() {
        final String msg = "A PDU could not be constructed because all data could not be found, " + "VC=" + allowedVcid
                + ", APID=" + currentPktMessage.getPacketInfo().getApid() + ", ERT="
                + currentPktMessage.getPacketInfo().getErt() + ", packet SPSC=" + counterSourcePacket;
        final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
                                                                                         LogMessageType.INVALID_PDU_HEADER);
        pduExLog.log(logm);
        numberOfPdusInvalid++;
    }

    /**
     * Send invalid data, currently just log a message
     */
    private void sendInvalidDataMessage() {
        final int pduLength = pduHeader.getHeaderLength() + pduHeader.getDataLength() + 1;
        final String msg = "All data for a PDU could not be found, " + "VC=" + allowedVcid + ", APID="
                + currentPktMessage.getPacketInfo().getApid() + ", isValid=" + pduHeader.isValid() + ", length="
                + pduLength + ", ERT=" + currentPktMessage.getPacketInfo().getErt() + ", packet SPSC="
                + counterSourcePacket;
        final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
                                                                                         LogMessageType.INVALID_PDU_DATA);
        pduExLog.log(logm);
        numberOfPdusInvalid++;
    }

    /**
     * Send message indicating bad pdu.
     *
     */
    private void sendInvalidMessage() {
        final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN,
                                                                                         "PDU abandoned because header was corrupt, "
                                                                                                 + "VC=" + allowedVcid
                                                                                                 + ", isValid="
                                                                                                 + pduHeader.isValid()
                                                                                                 + ", length="
                                                                                                 + pduLength + ", ERT="
                                                                                                 + currentPktMessage.getPacketInfo()
                                                                                                                    .getErt()
                                                                                                 + ", frame VCFC(s)="
                                                                                                 + currentPktMessage.getPacketInfo()
                                                                                                                    .getSourceVcfcs()
                                                                                                 + ", packet SPSC="
                                                                                                 + currentPktMessage.getPacketInfo()
                                                                                                                    .getSeqCount(),
                                                                                         LogMessageType.INVALID_PKT_HEADER);
        numberOfPdusInvalid++;
        pduExLog.log(logm);
    }

    /**
     *
     * PduExtractMessageSubscriber is the listener for internal packet and related messages.
     *
     *
     */
    private class PduExtractMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of PduExtractMessageSubscriber.
         */
        public PduExtractMessageSubscriber() {
            bus.subscribe(TmServiceMessageType.TelemetryPacket, this);
            bus.subscribe(CommonMessageType.EndOfData, this);
        }

        /**
         * {@inheritDoc}
         * @see jpl.gds.shared.message.BaseMessageHandler#handleMessage(jpl.gds.shared.message.IMessage)
         */
        @Override
        public void handleMessage(final IMessage m) {

            if (m.isType(CommonMessageType.EndOfData)) {
                flushCurrentPdus();

                return;
            }
            else {
                final ITelemetryPacketMessage pktMsg = (ITelemetryPacketMessage) m;
                consume(pktMsg);
            }
        }
    }

}
