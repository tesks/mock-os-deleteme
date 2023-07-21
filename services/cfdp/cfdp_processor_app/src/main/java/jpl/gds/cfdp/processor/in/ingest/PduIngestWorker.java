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

package jpl.gds.cfdp.processor.in.ingest;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;

import cfdp.engine.ampcs.RequestResult;
import jpl.gds.cfdp.common.CfdpPduConstants;
import jpl.gds.cfdp.processor.action.disruptor.ActionEvent;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.in.disruptor.InboundPduEvent;
import jpl.gds.cfdp.processor.in.disruptor.InboundPduRingBufferManager;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

@Service
@DependsOn("configurationManager")
public class PduIngestWorker implements EventHandler<ActionEvent>, LifecycleAware {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private InboundPduRingBufferManager inboundPduRingBufferManager;

    private static final int PDU_HEADER_DATA_LENGTH_FIELD_OFFSET_IN_BITS = 8;
    private static final int PDU_HEADER_ENTITY_ID_LENGTH_FIELD_OFFSET_IN_BITS = 25;
    private static final int PDU_HEADER_ENTITY_ID_LENGTH_FIELD_LENGTH_IN_BITS = 3;
    private static final int PDU_HEADER_TX_SEQ_NUM_LENGTH_FIELD_OFFSET_IN_BITS = 29;
    private static final int PDU_HEADER_TX_SEQ_NUM_LENGTH_FIELD_LENGTH_IN_BITS = 3;

    private Thread thread;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    @Override
    public void onEvent(final ActionEvent ingestActionEvent, final long sequence, final boolean endOfBatch) throws Exception {
        String resultMessage;

        switch (ingestActionEvent.getIngestSource()) {

            case FILE:

                try {

                    long pduCount = 0;

                    try (FileInputStream fis = new FileInputStream(
                            Paths.get(ingestActionEvent.getIngestFileName()).toRealPath().toFile())) {

                        final byte[] readBuf = new byte[configurationManager.getPduFileReadBufferSize()];
                        int numBytesRead;

                        final byte[] pduBuf = new byte[CfdpPduConstants.THEORETICAL_MAX_PDU_SIZE_IN_BYTES];
                        int pduBufNextOffset = 0;

                        // Using -1 below as "unknown"
                        int headerVariablePartLength = -1;
                        // Using -1 below as "unknown"
                        int originalDataLength = -1;
                        // Using -1 below as "unknown"
                        int remainingDataLength = -1;

                        while ((numBytesRead = fis.read(readBuf)) != -1 && !Thread.currentThread().isInterrupted()) {
                            int readNextOffset = 0;
                            int bytesLeftInReadBuf = numBytesRead;

                            while (bytesLeftInReadBuf > 0) {
                                // As long as there are more bytes to read from the read buffer, loop

                                int copyLength;

                                if (pduBufNextOffset < 4) {
                                    /*
                                     * readBuf[readNextOffset] is starting, middle, or last byte of the first 4
                                     * Fixed PDU Header bytes
                                     */
                                    copyLength = Math.min(4 - pduBufNextOffset, bytesLeftInReadBuf);

                                    log.trace(
                                            "Copy stage, pduBufNextOffset=" , pduBufNextOffset , ": will capture "
                                                    , copyLength
                                                    , " bytes of the first 4 bytes of the header (min of 4 - "
                                                    , "pduBufNextOffset = "
                                                    , (4 - pduBufNextOffset) , " and bytesLeftInReadBuf = "
                                                    , bytesLeftInReadBuf , ")");

                                } else if (headerVariablePartLength >= 0
                                        && pduBufNextOffset < 4 + headerVariablePartLength) {
                                    /*
                                     * readBuf[readNextOffset] is starting, middle, or last byte of the variable
                                     * portion of the Fixed PDU Header
                                     */
                                    copyLength = Math.min(headerVariablePartLength - (pduBufNextOffset - 4),
                                            bytesLeftInReadBuf);

                                    log.trace(
                                            "Copy stage, pduBufNextOffset=", pduBufNextOffset
                                                    , ": will capture " , copyLength
                                                    , " bytes of the variable portion of the header (min of "
                                                    , "headerVariablePartLength - (pduBufNextOffset - 4) = "
                                                    , (headerVariablePartLength - (pduBufNextOffset - 4))
                                                    , " and bytesLeftInReadBuf = " + bytesLeftInReadBuf , ")");
                                } else {
                                    /*
                                     * readBuf[readNextOffset] is starting, middle, or last byte of the PDU's data
                                     * field
                                     */
                                    copyLength = Math.min(remainingDataLength, bytesLeftInReadBuf);

                                    log.trace(
                                            "Copy stage, pduBufNextOffset=" ,
                                            pduBufNextOffset , ": will capture " , copyLength
                                                    , " bytes of the data field (min of remainingDataLength = "
                                                    , remainingDataLength + " and bytesLeftInReadBuf = "
                                                    , bytesLeftInReadBuf , ")");
                                }

                                System.arraycopy(readBuf, readNextOffset, pduBuf, pduBufNextOffset, copyLength);
                                pduBufNextOffset += copyLength;
                                readNextOffset += copyLength;
                                bytesLeftInReadBuf -= copyLength;

                                // Now, process the newly copied bytes

                                if (pduBufNextOffset == 4) {

                                    log.trace(
                                            "Process stage, pduBufNextOffset=" , pduBufNextOffset , ": processing the"
                                                    , " first 4 bytes of the header");

                                    // There is now a complete copy of the first 4 Fixed PDU Header bytes

                                    // Parse PDU Data field length
                                    originalDataLength = GDR.get_u16(pduBuf,
                                            PDU_HEADER_DATA_LENGTH_FIELD_OFFSET_IN_BITS / 8);
                                    remainingDataLength = originalDataLength;

                                    // Parse Length of entity IDs ('0' means that entity ID is one octet)
                                    final int entityIDLengthInBytes = GDR.get_u8(pduBuf,
                                            PDU_HEADER_ENTITY_ID_LENGTH_FIELD_OFFSET_IN_BITS / 8,
                                            PDU_HEADER_ENTITY_ID_LENGTH_FIELD_OFFSET_IN_BITS % 8,
                                            PDU_HEADER_ENTITY_ID_LENGTH_FIELD_LENGTH_IN_BITS) + 1;

                                    /*
                                     * Parse Length of Transaction sequence number ('0' means that sequence number
                                     * is one octet)
                                     */
                                    final int txSeqNumLengthInBytes = GDR.get_u8(pduBuf,
                                            PDU_HEADER_TX_SEQ_NUM_LENGTH_FIELD_OFFSET_IN_BITS / 8,
                                            PDU_HEADER_TX_SEQ_NUM_LENGTH_FIELD_OFFSET_IN_BITS % 8,
                                            PDU_HEADER_TX_SEQ_NUM_LENGTH_FIELD_LENGTH_IN_BITS) + 1;

                                    headerVariablePartLength = (entityIDLengthInBytes * 2) + txSeqNumLengthInBytes;

                                    log.trace(
                                            "Process stage: data field length = " , remainingDataLength , ", "
                                                    , "entityIDLengthInBytes = "
                                                    , entityIDLengthInBytes , ", txSeqNumLengthInBytes = "
                                                    , txSeqNumLengthInBytes , "; variable portion of the header = " ,
                                                    headerVariablePartLength);

                                } else if (pduBufNextOffset > 4 + headerVariablePartLength) {
                                    // Just copied a portion or all of the data field

                                    remainingDataLength -= copyLength;

                                    log.trace(
                                            "Process stage, pduBufNextOffset=" , pduBufNextOffset , ": "
                                                    , remainingDataLength , " bytes of the data field remain to be "
                                                    , "captured");

                                    if (remainingDataLength == 0) {

                                        log.trace("Process stage: Captured a full PDU of size " , pduBufNextOffset ,
                                                " bytes");

                                        // Finished capturing the PDU
                                        inboundPduRingBufferManager.getRingBuffer().publishEvent(InboundPduEvent::translate,
                                                pduBuf, pduBufNextOffset, null, null, null, null, null, -1,
                                                // MPCS-9870
                                                -1L,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                -1,
                                                -1,
                                                null,
                                                -1,
                                                -1,
                                                -1,
                                                -1,
                                                -1 // relayScid
                                        );
                                        pduCount++;

                                        // Apply metering
                                        Thread.sleep(
                                                configurationManager.getInboundPduFileIngestionMinimumReadIntervalMillis());

                                        // Reset
                                        headerVariablePartLength = -1;
                                        originalDataLength = -1;
                                        remainingDataLength = -1;
                                        pduBufNextOffset = 0;
                                    }

                                }

                            }

                        }

                        if (!Thread.currentThread().isInterrupted()) {

                            if (pduBufNextOffset > 0) {

                                // pduBuf contains a partially captured PDU for some reason

                                log.warn(
                                        "EOF reached but PDU only partially captured: Last PDU is only "
                                                + pduBufNextOffset +
                                                " bytes instead of "
                                                + (4 + headerVariablePartLength + originalDataLength));

                                //  MPCS-12208: Use correct arguments per MPCS-9870
                                inboundPduRingBufferManager.getRingBuffer().publishEvent(InboundPduEvent::translate, pduBuf,
                                        pduBufNextOffset, null, null, null, null, null, -1,
                                        // MPCS-9870
                                        -1L,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        -1,
                                        -1,
                                        null,
                                        -1,
                                        -1,
                                        -1,
                                        -1,
                                        -1 // relayScid
                                );
                                pduCount++;

                                log.warn("Ingested " + pduCount + " PDU(s) from file "
                                        + ingestActionEvent.getIngestFileName() + " with last one only partially");
                                resultMessage = "Ingested " + pduCount + " PDU(s) from file "
                                        + ingestActionEvent.getIngestFileName() + " with last one only partially";

                            } else {

                                log.info("Successfully ingested all " + pduCount + " PDU(s) from file "
                                        + ingestActionEvent.getIngestFileName());
                                resultMessage = "Successfully ingested all " + pduCount + " PDU(s) from file "
                                        + ingestActionEvent.getIngestFileName();

                            }

                        } else {
                            log.warn("Ingestion of PDUs file " + ingestActionEvent.getIngestFileName() + " interrupted");
                            resultMessage = "Ingestion of PDUs file " + ingestActionEvent.getIngestFileName()
                                    + " interrupted";

                        }

                    } catch (final IOException ie) {

                        log.warn("Ingested " + pduCount + " PDU(s) from file "
                                + ingestActionEvent.getIngestFileName() + " until exception: " + ExceptionTools.getMessage(ie), ie);
                        resultMessage = "Ingested " + pduCount + " PDU(s) from file "
                                + ingestActionEvent.getIngestFileName() + " until exception: " + ExceptionTools.getMessage(ie);

                    }

                } catch (final InterruptedException ie) {
                    log.warn("Ingestion of PDUs file " + ingestActionEvent.getIngestFileName()
                            + " interrupted: " + ExceptionTools.getMessage(ie), ie);
                    resultMessage = "Ingestion of PDUs file " + ingestActionEvent.getIngestFileName() + " interrupted: "
                            + ExceptionTools.getMessage(ie);
                    Thread.currentThread().interrupt();
                }

                break;

            default:

                log.error("Ingest source of " + ingestActionEvent.getIngestSource() + " not supported");
                resultMessage = "Ingest source of " + ingestActionEvent.getIngestSource() + " not supported";

        }

        final RequestResult result = new RequestResult();
        result.setMessage(resultMessage);

        if (!ingestActionEvent.getResponseQueue().offer(result)) {
            log.error("Failed to add INGEST request result to response queue");
        }

    }

    @Override
    public void onStart() {
        thread = Thread.currentThread();
        log.debug("Saving current thread as " + thread.getName());
    }

    /**
     * @return the thread
     */
    public Thread getThread() {
        return thread;
    }

    @Override
    public void onShutdown() {
        // Do nothing
    }

}