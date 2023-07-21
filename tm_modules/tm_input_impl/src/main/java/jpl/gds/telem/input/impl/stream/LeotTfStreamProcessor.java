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
package jpl.gds.telem.input.impl.stream;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.impl.message.RawTransferFrameMessage;
import org.springframework.context.ApplicationContext;

import java.io.DataInputStream;
import java.io.IOException;


/**
 * This class is an implementation of IRawStreamProcessor that processes LEO-T
 * transfer frame streams.
 *
 * We rely on the algorithm defined in the abstract class and functionality specific
 * to NEN processing and the LEO-T format.
 *
 */
public class LeotTfStreamProcessor extends AbstractCustomFrameHeaderStreamProcessor
{
    private static final String NAME = "LeotTfStreamProcessor";
    private final int nenStatusDataClass;

    /**
     * Constructor.
     * @param serviceContext the current application context
     *
     * @throws RawInputException if there is a problem creating the processor
     */
    public LeotTfStreamProcessor(final ApplicationContext serviceContext) throws RawInputException
    {
        super(serviceContext, IStationTelemHeader.LEOT_HEADER_SIZE, StreamType.LEOT_TF);
        nenStatusDataClass = rawConfig.getNenStatusDataClass();
    }

    /**
     * Overriding the default success messsage behavior
     * to include NEN status packet case
     *
     * @param metadataClone
     *          metadata
     * @param buff
     *          the raw frame data as a byte buffer
     * @param header
     *          header in HeaderHolder format
     * @param stationTelemHeader
     *          additional header metadata
     */
    @Override
    protected void publishSuccessMessage(RawInputMetadata metadataClone,
                                         byte[] buff, HeaderHolder header,
                                         IStationTelemHeader stationTelemHeader) {
        /*
         * 11/21/13 - MPCS-5550. Check the data class here and decide
         * whether to publish a raw NEN status packet message or a raw
         * transfer frame message.
         */
        if (stationTelemHeader.getDataClass() == nenStatusDataClass) {
            logger.trace(NAME, " processRawData: publishing RawNenStatusPacketMessage");

            context.publish(
                    stationMessageFactory.createNenMonitorMessage(metadataClone.getDsnInfo(),
                            stationTelemHeader,
                            buff));

        } else {
            logger.trace(NAME, "processRawData: publishing RawTransferFrameMessage");

            context.publish(
                    new RawTransferFrameMessage(metadataClone,
                            buff,
                            header,
                            TrailerHolder.NULL_HOLDER));
        }

    }

    /**
     * Read an entire frame from the stream. Update the metadata from the
     * LEO-T header, but do not return header.
     *
     * @param dis      Input stream
     * @param rim      Metadata
     * @param deltaMin Amount I need, at least
     * @param deltaMax Amount to ask for
     * @param extra    Bytes left from last time or null
     *
     * @return Triplet of frame bytes, extra or null, header
     *
     * @throws IOException Error reading into buffer
     */
    @Override
    protected FrameBytes getNextFrame(final DataInputStream dis,
                                      final RawInputMetadata rim,
                                      final int deltaMin,
                                      final int deltaMax,
                                      final byte[] extra)
            throws IOException
    {
        sb.setLength(0);
        sb.append("LeotTfStreamProcessor DIS getNextFrame: start method; deltaMin = ");
        sb.append(deltaMin);
        sb.append(", deltaMax = ");
        sb.append(deltaMax);
        logger.trace(sb.toString());

        final IStationTelemHeader head = stationHeaderFactory.createLeotHeader();

        byte[] block   = extra;
        int    next    = 0;
        int    skipped = 0;

        while (true)
        {
            logger.trace("LeotTfStreamProcessor DIS getNextFrame: calling initial getDataBlock");
            block = getDataBlock(dis, block, next, deltaMin, deltaMax);

            if (block == null)
            {
                // No more data

                if (skipped > 0)
                {
                    logger.debug(NAME         +
                            ": Skipped " +
                            skipped      +
                            " bytes searching for LEO-T header");
                }

                logger.trace("LeotTfStreamProcessor DIS getNextFrame: block == null so returning null");
                return null;
            }

            // We can't step through a partial header; get last usable index
            // where we have a full LEO-T and ASM

            final int last = block.length - head.getSizeBytes()
                    - _asmLength;
            boolean found = false;

            // Walk down buffer looking for a match

            boolean scanMode = false;

            for (int i = 0; i <= last; ++i)
            {
                sb.setLength(0);
                sb.append("LeotTfStreamProcessor DIS getNextFrame: searching LEO-T header; i = ");
                sb.append(i);
                logger.trace(sb.toString());

                final int asmAt = head.load(block, i);

                if (head.isValid())
                {
                    logger.trace("LeotTfStreamProcessor DIS getNextFrame: found LEO-T header");

                    if (scanMode) {
                        logger.info("Scan on buffer found LEO-T frame header at offset " + i);
                        scanMode = false;
                    }

                    /*
                     * 11/21/13 - MPCS-5550. Check to see if the frame actually
                     * contains a NEN status packet. If so, process the header as usual,
                     * but do not look for an ASM. Break out of the loop just as we would
                     * if we found an ASM.
                     */
                    if (head.getDataClass() == nenStatusDataClass) {
                        sb.setLength(0);
                        sb.append("LeotTfStreamProcessor DIS getNextFrame: found NEN_STATUS packet");
                        logger.trace(sb.toString());
                        processHeader(head, rim);
                        next = asmAt;
                        /*
                         * 11/25/13 - MPCS-4932. Fix bug with status packet processing.
                         */
                        found = true;
                        break;

                    } else {
                        for (final byte[] asm : _asms)
                        {
                            if (checkASM(block, asmAt, asm))
                            {
                                sb.setLength(0);
                                sb.append("LeotTfStreamProcessor DIS getNextFrame: found ASM at ");
                                sb.append(asmAt);
                                logger.trace(sb.toString());

                                // Found it, so process and set up to get frame

                                processHeader(head, rim);

                                found = true;
                                next  = asmAt;

                                break;
                            }
                        }

                        if (found)
                        {
                            break;
                        }
                    }
                } else if (i == 0) {
                    logger.warn("Incoming stream does not have LEO-T frame header. Starting scan on rest of buffer.");
                    scanMode = true;
                }

                // No match on header and/or ASM
                ++skipped;
            }

            if (found)
            {
                break;
            }

            // Keep trailing portion and keep going

            next = last + 1;
        }

        if (skipped > 0)
        {
            logger.debug(NAME         +
                    ": Skipped " +
                    skipped      +
                    " bytes searching for LEO-T header");
        }

        final int tduLength = head.getDataLength();

        // Read the rest of the frame, which immediately follows

        logger.trace("LeotTfStreamProcessor DIS getNextFrame: calling second getDataBlock to read rest of the frame");

        block = getDataBlock(dis,
                block,
                next,
                tduLength,
                Math.max(deltaMax, tduLength));

        if (block == null)
        {
            logger.trace("LeotTfStreamProcessor DIS getNextFrame: block == null so returning null");
            return null;
        }

        sb.setLength(0);
        sb.append("LeotTfStreamProcessor DIS getNextFrame: end of method; return split(block, ");
        sb.append(tduLength);
        sb.append(")");
        logger.trace(sb.toString());

        /*
         * 11/21/13 - MPCS-5550. FrameBytes now carries the LEOT header
         * instead of a header holder.
         */
        return split(block, tduLength, head);
    }


}