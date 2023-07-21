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
package jpl.gds.cfdp.linksim;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jpl.gds.ccsds.api.CcsdsApiBeans;
import jpl.gds.ccsds.api.cfdp.CfdpPduDirection;
import jpl.gds.ccsds.api.cfdp.ICfdpEofAckPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpEofPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpFileDataPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpFinAckPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpFinishedPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpMetadataPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpNakPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduFactory;
import jpl.gds.cfdp.linksim.datastructures.ReceivedPduContainer;
import jpl.gds.cfdp.linksim.out.CfdpLinkSimOutboundPduJmsSink;
import jpl.gds.cfdp.linksim.out.CfdpLinkSimOutboundPduRestApiSink;
import jpl.gds.cfdp.linksim.out.ICfdpLinkSimOutboundPduSink;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;

/**
 * {@code CfdpLinkSimPduTransformer} applies the unreliable, indeterministic, and lossy link properties specified by the
 * user before publishing the PDUs.
 *
 */
@Service
public class CfdpLinkSimPduTransformer {

    private Tracer log;

    @Autowired
    CfdpLinkSimPduQueueManager queueManager;

    private ICfdpPduFactory pduFactory;

    private int cycleDelay;
    private int duplicationRate;
    private boolean reorder;
    private int dropMetadataCount;
    private int metadataPdusDropped;
    private int dropEofCount;
    private int eofPdusDropped;
    private int dropFinCount;
    private int finPdusDropped;
    private int dropEofAckCount;
    private int eofAckPdusDropped;
    private int dropFinAckCount;
    private int finAckPdusDropped;
    private int dropNakCount;
    private int nakPdusDropped;
    private int dropAllExceptMetadataCount;
    private int pdusExceptMetadataDropped;
    private int dropAllExceptEofCount;
    private int pdusExceptEofDropped;
    private int dropFiledataRate;
    private int dropAllDurationSeconds;
    private long dropAllDurationTimeoutSystemMilliseconds;
    private boolean dropAllDurationOver;
    private int corruptMetadataCount;
    private int metadataPdusCorrupted;
    private int corruptFiledataRate;
    private int corruptEofCount;
    private int eofPdusCorrupted;
    private int corruptFinCount;
    private int finPdusCorrupted;
    private int corruptNakCount;
    private int nakPdusCorrupted;
    private int corruptEofAckCount;
    private int eofAckPdusCorrupted;
    private int corruptFinAckCount;
    private int finAckPdusCorrupted;
    private int alterMetadataCrcCount;
    private int metadataPduCrcsAltered;
    private int alterFiledataCrcRate;
    private int alterEofCrcCount;
    private int eofPduCrcsAltered;
    private int alterFinCrcCount;
    private int finPduCrcsAltered;
    private int alterNakCrcCount;
    private int nakPduCrcsAltered;
    private int alterEofAckCrcCount;
    private int eofAckPduCrcsAltered;
    private int alterFinAckCrcCount;
    private int finAckPduCrcsAltered;
    private int alterEofChecksumCount;
    private int eofPduChecksumsAltered;
    private int alterEofFileSizeCount;
    private int eofPduFileSizesAltered;
    private boolean outputToRestApi;
    private String url;

    private ICfdpLinkSimOutboundPduSink outSink;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private Environment env;

    @PostConstruct
    private void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);

        pduFactory = appContext.getBean(CcsdsApiBeans.CFDP_PDU_FACTORY, ICfdpPduFactory.class);

        cycleDelay = Integer.parseInt(env.getProperty(CfdpLinkSimApp.CYCLE_DELAY_MILLIS_PROPERTY_KEY));
        duplicationRate = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DUPLICATION_RATE_PROPERTY_KEY));
        reorder = Boolean.parseBoolean(env.getProperty(CfdpLinkSimApp.PDU_REORDER_PROPERTY_KEY));
        dropMetadataCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_METADATA_COUNT_PROPERTY_KEY));
        dropEofCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_EOF_COUNT_PROPERTY_KEY));
        dropFinCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_FIN_COUNT_PROPERTY_KEY));
        dropEofAckCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_EOF_ACK_COUNT_PROPERTY_KEY));
        dropFinAckCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_FIN_ACK_COUNT_PROPERTY_KEY));
        dropNakCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_NAK_COUNT_PROPERTY_KEY));
        dropAllExceptMetadataCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_ALL_EXCEPT_METADATA_COUNT_PROPERTY_KEY));
        dropAllExceptEofCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_ALL_EXCEPT_EOF_COUNT_PROPERTY_KEY));
        dropFiledataRate = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_FILEDATA_RATE_PROPERTY_KEY));
        dropAllDurationSeconds = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_DROP_ALL_DURATION_SECONDS_PROPERTY_KEY));
        corruptMetadataCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_CORRUPT_METADATA_COUNT_PROPERTY_KEY));
        corruptFiledataRate = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_CORRUPT_FILEDATA_RATE_PROPERTY_KEY));
        corruptEofCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_CORRUPT_EOF_COUNT_PROPERTY_KEY));
        corruptFinCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_CORRUPT_FIN_COUNT_PROPERTY_KEY));
        corruptNakCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_CORRUPT_NAK_COUNT_PROPERTY_KEY));
        corruptEofAckCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_CORRUPT_EOF_ACK_COUNT_PROPERTY_KEY));
        corruptFinAckCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_CORRUPT_FIN_ACK_COUNT_PROPERTY_KEY));
        alterMetadataCrcCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_METADATA_CRC_COUNT_PROPERTY_KEY));
        alterFiledataCrcRate = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_FILEDATA_CRC_RATE_PROPERTY_KEY));
        alterEofCrcCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_EOF_CRC_COUNT_PROPERTY_KEY));
        alterFinCrcCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_FIN_CRC_COUNT_PROPERTY_KEY));
        alterNakCrcCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_NAK_CRC_COUNT_PROPERTY_KEY));
        alterEofAckCrcCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_EOF_ACK_CRC_COUNT_PROPERTY_KEY));
        alterFinAckCrcCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_FIN_ACK_CRC_COUNT_PROPERTY_KEY));
        alterEofChecksumCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_EOF_CHECKSUM_COUNT_PROPERTY_KEY));
        alterEofFileSizeCount = Integer.parseInt(env.getProperty(CfdpLinkSimApp.PDU_ALTER_EOF_FILE_SIZE_COUNT_PROPERTY_KEY));
        outputToRestApi = Boolean.parseBoolean(env.getProperty(CfdpLinkSimApp.OUTPUT_TO_REST_API_PROPERTY_KEY));
        url = env.getProperty(CfdpLinkSimApp.URL_PROPERTY_KEY);

        log.info("Cycle delay (milliseconds): ", cycleDelay);

        if (duplicationRate > 0) {
            log.info("Rate (out of 100) of PDU duplication: ", duplicationRate);
        }

        if (reorder) {
            log.info("Reorder PDUs: ", reorder);
        }

        if (dropAllDurationSeconds > 0) {
            log.info("Drop all PDUs duration (seconds): ", dropAllDurationSeconds);
        }

        if (dropAllExceptMetadataCount > 0) {
            log.info("Number of initial PDUs except Metadata PDUs to drop: ", dropAllExceptMetadataCount);
        } else if (dropAllExceptEofCount > 0) {
            log.info("Number of initial PDUs except EOF PDUs to drop: ", dropAllExceptEofCount);
        } else {

            if (dropMetadataCount > 0) {
                log.info("Number of initial Metadata PDUs to drop: ", dropMetadataCount);
            } else if (corruptMetadataCount > 0) {
                log.info("Number of initial Metadata PDUs to corrupt: ", corruptMetadataCount);
            } else if (alterMetadataCrcCount > 0) {
                log.info("Number of initial Metadata PDUs' CRC values to alter: ", alterMetadataCrcCount);
            }

            if (dropEofCount > 0) {
                log.info("Number of initial EOF PDUs to drop: ", dropEofCount);
            } else if (corruptEofCount > 0) {
                log.info("Number of initial EOF PDUs to corrupt: ", corruptEofCount);
            } else if (alterEofCrcCount > 0) {
                log.info("Number of initial EOF PDUs' CRC values to alter: ", alterEofCrcCount);
            } else if (alterEofChecksumCount > 0) {
                log.info("Number of initial EOF PDUs' checksum values to alter: ", alterEofChecksumCount);
            } else if (alterEofFileSizeCount > 0) {
                log.info("Number of initial EOF PDUs' file size values to alter: ", alterEofFileSizeCount);
            }

            if (dropFinCount > 0) {
                log.info("Number of initial Finished PDUs to drop: ", dropFinCount);
            } else if (corruptFinCount > 0) {
                log.info("Number of initial Finished PDUs to corrupt: ", corruptFinCount);
            } else if (alterFinCrcCount > 0) {
                log.info("Number of initial Finished PDUs' CRC values to alter: ", alterFinCrcCount);
            }

            if (dropNakCount > 0) {
                log.info("Number of initial NAK PDUs to drop: ", dropNakCount);
            } else if (corruptNakCount > 0) {
                log.info("Number of initial NAK PDUs to corrupt: ", corruptNakCount);
            } else if (alterNakCrcCount > 0) {
                log.info("Number of initial NAK PDUs' CRC values to alter: ", alterNakCrcCount);
            }

            if (dropEofAckCount > 0) {
                log.info("Number of initial ACK PDUs for EOF to drop: ", dropEofAckCount);
            } else if (corruptEofAckCount > 0) {
                log.info("Number of initial ACK PDUs for EOF to corrupt: ", corruptEofAckCount);
            } else if (alterEofAckCrcCount > 0) {
                log.info("Number of initial ACK PDUs for EOF CRC values to alter: ", alterEofAckCrcCount);
            }

            if (dropFinAckCount > 0) {
                log.info("Number of initial ACK PDUs for Finished PDU to drop: ", dropFinAckCount);
            } else if (corruptFinAckCount > 0) {
                log.info("Number of initial ACK PDUs for Finished PDU to corrupt: ", corruptFinAckCount);
            } else if (alterFinAckCrcCount > 0) {
                log.info("Number of initial ACK PDUs for Finished PDU CRC values to alter: ", alterFinAckCrcCount);
            }

        }

        log.info("Rate (out of 100) of dropping File Data PDUs: ", dropFiledataRate);
        log.info("Rate (out of 100) of corrupting File Data PDUs: ", corruptFiledataRate);
        log.info("Rate (out of 100) of altering File Data PDUs' CRC values: ", alterFiledataCrcRate);

        dropAllDurationTimeoutSystemMilliseconds = 0; // Initialize when first PDU is seen
        dropAllDurationOver = dropAllDurationSeconds == 0;

        if (url != null && !"null".equalsIgnoreCase(url)) {
            log.info("PDU output to REST API endpoint: " + url);
            final CfdpLinkSimOutboundPduRestApiSink restApiOutSink = appContext.getBean(CfdpLinkSimOutboundPduRestApiSink.class);
            restApiOutSink.setUrl(url);
            outSink = restApiOutSink;
        } else {
            log.info("PDU output to JMS");
            outSink = appContext.getBean(CfdpLinkSimOutboundPduJmsSink.class);
        }

    }

    @Scheduled(fixedDelayString = "${cycle.delay.millis}")
    public void applyTransformations() {
        final List<ReceivedPduContainer> drainedPdus = new LinkedList<>();

        final int numDrained = queueManager.getQueue().drainTo(drainedPdus);
        log.trace("Drained ", numDrained, " PDUs from inbound queue");

        if (numDrained > 0) {

            if (dropAllDurationTimeoutSystemMilliseconds == 0) {
                // Initialize once when the first PDU is obtained
                dropAllDurationTimeoutSystemMilliseconds = ((long) dropAllDurationSeconds) * 1000 + System.currentTimeMillis();
            }

            if (dropAllDurationSeconds > 0 && System.currentTimeMillis() < dropAllDurationTimeoutSystemMilliseconds) {
                // Still inside "drop all PDUs" duration in time

                for (final ReceivedPduContainer inPdu : drainedPdus) {
                    final byte[] pduData = inPdu.getData();

                    if (pduData == null) {
                        log.warn("AutoPduHolder object had no PDU data!");
                    } else {
                        final ICfdpPdu outPdu = pduFactory.createPdu(pduData);
                        log.info("↓ DROPPING ", outPdu);
                    }

                }

            } else {

                if (!dropAllDurationOver) {
                    log.info("Drop all duration is over. Start applying other options (if any).");
                    dropAllDurationOver = true;
                }

                if (reorder) {
                    log.info("Shuffling order of ", numDrained, " PDUs");
                    Collections.shuffle(drainedPdus);
                }

                for (final ReceivedPduContainer originalPduContainer : drainedPdus) {
                    final byte[] pduData = originalPduContainer.getData();

                    if (pduData == null) {
                        log.warn("AutoPduHolder object had no PDU data!");
                    } else {
                        final ICfdpPdu outPdu = pduFactory.createPdu(pduData);

                        if (dropAllExceptMetadataCount > 0) {

                            if (!(outPdu instanceof ICfdpMetadataPdu) && pdusExceptMetadataDropped < dropAllExceptMetadataCount) {
                                log.info("↓ DROPPING ", outPdu);
                                pdusExceptMetadataDropped++;
                            } else {
                                duplicateAndPushOut(outPdu, originalPduContainer);
                            }

                        } else if (dropAllExceptEofCount > 0) {

                            if (!(outPdu instanceof ICfdpEofPdu) && pdusExceptEofDropped < dropAllExceptEofCount) {
                                log.info("↓ DROPPING ", outPdu);
                                pdusExceptEofDropped++;
                            } else {
                                duplicateAndPushOut(outPdu, originalPduContainer);
                            }

                        } else {

                            if (outPdu instanceof ICfdpMetadataPdu) {

                                if (metadataPdusDropped < dropMetadataCount) {
                                    log.info("↓ DROPPING ", outPdu);
                                    metadataPdusDropped++;
                                } else if (metadataPdusCorrupted < corruptMetadataCount) {
                                    metadataPdusCorrupted++;
                                    corruptDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (metadataPduCrcsAltered < alterMetadataCrcCount) {

                                    if (alterCrcDuplicateAndPushOut(pduData, outPdu, originalPduContainer)) {
                                        metadataPduCrcsAltered++;
                                    }

                                } else {
                                    duplicateAndPushOut(outPdu, originalPduContainer);
                                }

                            } else if (outPdu instanceof ICfdpEofPdu) {

                                if (eofPdusDropped < dropEofCount) {
                                    log.info("↓ DROPPING ", outPdu);
                                    eofPdusDropped++;
                                } else if (eofPdusCorrupted < corruptEofCount) {
                                    eofPdusCorrupted++;
                                    corruptDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (eofPduCrcsAltered < alterEofCrcCount) {

                                    if (alterCrcDuplicateAndPushOut(pduData, outPdu, originalPduContainer)) {
                                        eofPduCrcsAltered++;
                                    }

                                } else if (eofPduChecksumsAltered < alterEofChecksumCount) {
                                    eofPduChecksumsAltered++;
                                    alterEofChecksumDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (eofPduFileSizesAltered < alterEofFileSizeCount) {
                                    eofPduFileSizesAltered++;
                                    alterEofFileSizeDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else {
                                    duplicateAndPushOut(outPdu, originalPduContainer);
                                }

                            } else if (outPdu instanceof ICfdpFinishedPdu) {

                                if (finPdusDropped < dropFinCount) {
                                    log.info("↓ DROPPING ", outPdu);
                                    finPdusDropped++;
                                } else if (finPdusCorrupted < corruptFinCount) {
                                    finPdusCorrupted++;
                                    corruptDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (finPduCrcsAltered < alterFinCrcCount) {

                                    if (alterCrcDuplicateAndPushOut(pduData, outPdu, originalPduContainer)) {
                                        finPduCrcsAltered++;
                                    }

                                } else {
                                    duplicateAndPushOut(outPdu, originalPduContainer);
                                }

                            } else if (outPdu instanceof ICfdpEofAckPdu) {

                                if (eofAckPdusDropped < dropEofAckCount) {
                                    log.info("↓ DROPPING ", outPdu);
                                    eofAckPdusDropped++;
                                } else if (eofAckPdusCorrupted < corruptEofAckCount) {
                                    eofAckPdusCorrupted++;
                                    corruptDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (eofAckPduCrcsAltered < alterEofAckCrcCount) {

                                    if (alterCrcDuplicateAndPushOut(pduData, outPdu, originalPduContainer)) {
                                        eofAckPduCrcsAltered++;
                                    }

                                } else {
                                    duplicateAndPushOut(outPdu, originalPduContainer);
                                }

                            } else if (outPdu instanceof ICfdpFinAckPdu) {

                                if (finAckPdusDropped < dropFinAckCount) {
                                    log.info("↓ DROPPING ", outPdu);
                                    finAckPdusDropped++;
                                } else if (finAckPdusCorrupted < corruptFinAckCount) {
                                    finAckPdusCorrupted++;
                                    corruptDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (finAckPduCrcsAltered < alterFinAckCrcCount) {

                                    if (alterCrcDuplicateAndPushOut(pduData, outPdu, originalPduContainer)) {
                                        finAckPduCrcsAltered++;
                                    }

                                } else {
                                    duplicateAndPushOut(outPdu, originalPduContainer);
                                }

                            } else if (outPdu instanceof ICfdpNakPdu) {

                                if (nakPdusDropped < dropNakCount) {
                                    log.info("↓ DROPPING ", outPdu);
                                    nakPdusDropped++;
                                } else if (nakPdusCorrupted < corruptNakCount) {
                                    nakPdusCorrupted++;
                                    corruptDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (nakPduCrcsAltered < alterNakCrcCount) {

                                    if (alterCrcDuplicateAndPushOut(pduData, outPdu, originalPduContainer)) {
                                        nakPduCrcsAltered++;
                                    }

                                } else {
                                    duplicateAndPushOut(outPdu, originalPduContainer);
                                }

                            } else if (outPdu instanceof ICfdpFileDataPdu) {

                                if (ThreadLocalRandom.current().nextInt(100) < dropFiledataRate) {
                                    log.info("↓ DROPPING ", outPdu);
                                } else if (ThreadLocalRandom.current().nextInt(100) < corruptFiledataRate) {
                                    corruptDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else if (ThreadLocalRandom.current().nextInt(100) < alterFiledataCrcRate) {
                                    alterCrcDuplicateAndPushOut(pduData, outPdu, originalPduContainer);
                                } else {
                                    duplicateAndPushOut(outPdu, originalPduContainer);
                                }

                            } else {
                                duplicateAndPushOut(outPdu, originalPduContainer);
                            }

                        }

                    }

                }

            }

        }

    }

    private void corruptDuplicateAndPushOut(final byte[] pduData,
                                            final ICfdpPdu outPdu,
                                            final ReceivedPduContainer originalPduContainer) {
        log.info("✖︎ CORRUPTING ", corruptPdu(outPdu), " BYTES\nORIGINAL:\n",
                BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(pduData), 40),
                "\nCORRUPTED:\n",
                BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(outPdu.getData()), 40));
        duplicateAndPushOut(outPdu, originalPduContainer);
    }

    private boolean alterCrcDuplicateAndPushOut(final byte[] pduData,
                                                final ICfdpPdu outPdu,
                                                final ReceivedPduContainer originalPduContainer) {

        // Check if PDU contains a CRC value by examining the flag at 7th bit
        final int crcFlagVal = (pduData[0] >> 1) & 1;

        ICfdpPdu pduToPublish = outPdu;
        boolean retVal = false;

        if (crcFlagVal > 0) {
            // CRC value exists in PDU, so alter it
            final ICfdpPdu alteredCrcPdu = pduFactory.createPdu(pduData);
            alterPduCrc(alteredCrcPdu);
            log.info("✖︎ ALTERING PDU'S CRC\nORIGINAL:\n",
                    BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(pduData), 40),
                    "\nCRC ALTERED:\n",
                    BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(alteredCrcPdu.getData()), 40));
            pduToPublish = alteredCrcPdu;
            retVal = true;
        }

        duplicateAndPushOut(pduToPublish, originalPduContainer);
        return retVal;
    }

    private void alterEofChecksumDuplicateAndPushOut(final byte[] pduData,
                                                     final ICfdpPdu outPdu,
                                                     final ReceivedPduContainer originalPduContainer) {
        alterEofChecksum(outPdu);
        log.info("✖︎ ALTERING EOF PDU'S CHECKSUM\nORIGINAL:\n",
                BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(pduData), 40),
                "\nCHECKSUM ALTERED:\n",
                BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(outPdu.getData()), 40));
        duplicateAndPushOut(outPdu, originalPduContainer);
    }

    private void alterEofFileSizeDuplicateAndPushOut(final byte[] pduData,
                                                     final ICfdpPdu outPdu,
                                                     final ReceivedPduContainer originalPduContainer) {
        alterEofFileSize(outPdu);
        log.info("✖︎ ALTERING EOF PDU'S FILE SIZE\nORIGINAL:\n",
                BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(pduData), 40),
                "\nFILE SIZE ALTERED:\n",
                BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(outPdu.getData()), 40));
        duplicateAndPushOut(outPdu, originalPduContainer);
    }

    /**
     * This method corrupts the {@code ICfdpPdu} object's data attribute. Nature of corrupt is random:
     * (1) Number of bytes in the data attribute to corrupt is chosen randomly; and
     * (2) Positions of those bytes to corrupt are selected randomly.
     *
     * @param cfdpPdu the CFDP PDU object to corrupt
     * @return number of bytes corrupted
     */
    private int corruptPdu(final ICfdpPdu cfdpPdu) {
        final byte[] corruptedData = cfdpPdu.getData();

        // Determine how many bytes to corrupt
        final Boolean[] corruptFlags = new Boolean[corruptedData.length];
        Arrays.fill(corruptFlags, false);
        final int numBytesToCorrupt = ThreadLocalRandom.current().nextInt(1, corruptedData.length);
        Arrays.fill(corruptFlags, 0, numBytesToCorrupt, true);
        // Randomize the corruption series
        final List<Boolean> corruptFlagsList = Arrays.asList(corruptFlags);
        Collections.shuffle(corruptFlagsList);

        // Corrupt the data
        for (int i = 0; i < corruptedData.length; i++) {

            if (corruptFlagsList.get(i).booleanValue() == true) {
                corruptedData[i] = (byte) ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
            }

        }

        cfdpPdu.setData(corruptedData);
        return numBytesToCorrupt;
    }

    private void alterPduCrc(final ICfdpPdu cfdpPdu) {
        final byte[] alteredCrcData = cfdpPdu.getData();

        // Per CCSDS 727.0-B-4, CRC value will be 2 octets at the end
        alteredCrcData[alteredCrcData.length - 2] = (byte) ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        alteredCrcData[alteredCrcData.length - 1] = (byte) ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);

        cfdpPdu.setData(alteredCrcData);
    }

    private void alterEofChecksum(final ICfdpPdu eof) {
        final byte[] checksumAlteredData = eof.getData();
        final long newChecksum = ThreadLocalRandom.current().nextLong(0, 0xFFFFFFFFL);
        final int checksumOffset = eof.getHeader().getHeaderLength() + 2;
        checksumAlteredData[checksumOffset] = (byte) (newChecksum >> 3 & 0xFF);
        checksumAlteredData[checksumOffset + 1] = (byte) (newChecksum >> 2 & 0xFF);
        checksumAlteredData[checksumOffset + 2] = (byte) (newChecksum >> 1 & 0xFF);
        checksumAlteredData[checksumOffset + 3] = (byte) (newChecksum & 0xFF);
        eof.setData(checksumAlteredData);
    }

    private void alterEofFileSize(final ICfdpPdu eof) {
        final byte[] fileSizeAlteredData = eof.getData();
        final long newFileSize = ThreadLocalRandom.current().nextLong(0, 0xFFFFFFFFL);
        final int fileSizeOffset = eof.getHeader().getHeaderLength() + 6;
        fileSizeAlteredData[fileSizeOffset] = (byte) (newFileSize >> 3 & 0xFF);
        fileSizeAlteredData[fileSizeOffset + 1] = (byte) (newFileSize >> 2 & 0xFF);
        fileSizeAlteredData[fileSizeOffset + 2] = (byte) (newFileSize >> 1 & 0xFF);
        fileSizeAlteredData[fileSizeOffset + 3] = (byte) (newFileSize & 0xFF);
        eof.setData(fileSizeAlteredData);
    }

    private void duplicateAndPushOut(final ICfdpPdu outPdu, final ReceivedPduContainer originalPduContainer) {
        log.trace("SEND PDU DATA\n", BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(outPdu.getData()), 40));
        outSink.send(outPdu, originalPduContainer);

        // Duplicate?
        if (ThreadLocalRandom.current().nextInt(100) < duplicationRate) {
            log.info("[DUPLICATE] Publishing PDU toward entity ", outPdu.getHeader().getDirection()
                            == CfdpPduDirection.TOWARD_RECEIVER
                            ? Long.toUnsignedString(outPdu.getHeader().getDestinationEntityId())
                            : Long.toUnsignedString(outPdu.getHeader().getSourceEntityId()),
                    ": ",
                    outPdu);
            log.trace("SEND PDU DATA\n", BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(outPdu.getData()), 40));
            outSink.send(outPdu, originalPduContainer);
        }

    }

}