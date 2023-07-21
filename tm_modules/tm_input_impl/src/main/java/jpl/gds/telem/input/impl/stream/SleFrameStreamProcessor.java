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
package jpl.gds.telem.input.impl.stream;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.sle.ISleHeader;
import jpl.gds.station.api.sle.annotation.ISlePrivateAnnotation;
import jpl.gds.station.api.sle.annotation.IV3SlePrivateAnnotation;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.impl.message.RawTransferFrameMessage;
import org.springframework.context.ApplicationContext;

/**
 * This class is an implementation of IRawStreamProcessor that processes SLE transfer frame streams.
 *
 */
public class SleFrameStreamProcessor extends AbstractCustomFrameHeaderStreamProcessor {

    /**
     * Constructor.
     *
     * @param serviceContext the current application context
     * @throws RawInputException if there is a problem creating the processor
     */
    public SleFrameStreamProcessor(final ApplicationContext serviceContext) throws RawInputException {
        super(serviceContext, ISleHeader.SLE_HEADER_SIZE, StreamType.SLE_TF);
    }

    /**
     * @return SLE Header
     */
    protected IStationTelemHeader getHeader() {
        return stationHeaderFactory.createSleHeader();
    }


    /**
     * @param metadataClone      metadata
     * @param buff               the raw frame data as a byte buffer
     * @param header             header in HeaderHolder format
     * @param stationTelemHeader
     */
    protected void publishSuccessMessage(RawInputMetadata metadataClone,
                                         byte[] buff, HeaderHolder header,
                                         IStationTelemHeader stationTelemHeader) {
        logger.trace(" processRawData: publishing RawTransferFrameMessage");

        context.publish(
                new RawTransferFrameMessage(metadataClone,
                        buff,
                        header,
                        TrailerHolder.NULL_HOLDER));

    }

    @Override
    protected int checkAsmAtIndex(byte[] asm, byte[] block, int asmAt, IStationTelemHeader head, RawInputMetadata rim) {
        // 2021-12-07  - MPCS-12335/PSYCHEMCS-141 - overriding to check for private annotations, and obtain
        // the bitrate if present.
        // it's possible that there are no private annotations included after the SLE_TF header. check
        // the current location for the ASM, and fall back to the end of the SLE_TF header if not found.
        // if found, update the bitrate and return the index of the ASM.
        boolean    found        = false;
        int        asmLoc       = asmAt;
        ISleHeader header       = (ISleHeader) head;
        int        backtrackLoc = asmLoc - header.getPrivateAnnotation().getPrivateAnnotationSizeBytes();
        if (checkASM(block, asmLoc, asm)) { // check current
            found = true;
            ISlePrivateAnnotation slePa = header.getPrivateAnnotation();
            // extract bitrate from private annotations
            if (slePa instanceof IV3SlePrivateAnnotation) {
                IV3SlePrivateAnnotation dsnV3Pa = (IV3SlePrivateAnnotation) slePa;
                double                  bitrate = dsnV3Pa.getBitrate();
                rim.setBitRate(bitrate);
            }
        } else if (checkASM(block, backtrackLoc, asm)) { // backtrack if not found
            found = true;
            header.getPrivateAnnotation().setValid(false);
            header.getPrivateAnnotation().setPresent(false);
            asmLoc = backtrackLoc;
        }

        if (found) {
            logger.trace(" DIS getNextFrame: found ASM at ", asmLoc);
            // Found it, so process and set up to get frame
            processHeader(head, rim);
            return asmLoc;
        }
        return -1;
    }

    @Override
    protected IStationTelemInfo getStationTelemInfo(int lenBits, double bitRate, RawInputMetadata metadataClone,
                                                    IStationTelemHeader header) {
        final ISleHeader sleHeader = (ISleHeader) header;

        // MPCS-12387 - pass SLE metadata into station telem info object
        final IStationTelemInfo info = stationInfoFactory.create(bitRate, lenBits,
                metadataClone.getErt(), getConfiguredStation(), sleHeader.getMetadata());

        info.setDssId(sleHeader.getIntAntennaId());

        return info;
    }
}