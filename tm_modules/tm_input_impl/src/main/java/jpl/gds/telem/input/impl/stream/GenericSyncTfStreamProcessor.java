/*
 * Copyright 2006-2019. California Institute of Technology.
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
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.telem.input.api.data.ITransferFrameDataProcessor;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IFrameStreamParser;
import jpl.gds.telem.input.api.stream.IParsedFrame;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.message.RawTransferFrameMessage;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

/**
 * Transfer frame stream Processor for custom formats defined in adaptations
 * (DAPHNE_TF, Cortex, and future formats)
 *
 * Assumptions:
 * - The stream contains transfer frames with station headers
 * - A frame header is always the first thing in the stream
 * - There is no data between station headers/frames
 * - The design must account for frame trailers
 * - If the frame should not be processed, that must be possible to determine from the station or frame header
 * - There is no station monitor data in the stream
 * - There is no need to monitor the connection (like there is in the SfduTf processor, which handles TDS heartbeats).
 * - The type of a frame can be determined. This means:
 *   a. The station header/trailer can tell you the length of the frame, AND
 *   b.	Either: The  station header can tell you the frame encoding, OR The frame has an ASM which can be used to
 *   determine the encoding.
 * - Either:
 *   a. There is never any other data in the stream, and there are no sync events the frame sequence you want to tell
 *   the user about, OR
 *   b. There can be frame sync issues: Out of sync data (suggest NOT), AND/OR Out of sequence frames, Loss of lock
 *   events
 * - Either:
 *   a. The quality of the frame can be determined from the station header, OR
 *   b.	Only good frames are present in the stream
 * - If the station header has no ERT, either:
 *   a. The format adaptor generates the ERT, OR
 *   b. The AMPCS framework generates the ERT
 *
 */
public class GenericSyncTfStreamProcessor extends AbstractRawStreamProcessor{
    private static final String PROC_NAME = "GenericSyncTfStreamProcessor";
    private static final double DEF_BIT_RATE = 10000.0D;

    //this will be provided by an adaptation
    private IFrameStreamParser parser;

    /**
     * Constructor
     * @param serviceContext Application context
     */
    public GenericSyncTfStreamProcessor(final ApplicationContext serviceContext) {
        super(serviceContext);
        this.parser = appContext.getBean(IFrameStreamParser.class);
    }

    @Override
    public void processRawData(final IRawInputStream  inputStream, final RawInputMetadata metadata) throws IOException {
        logger.trace(PROC_NAME, " processRawData: start of method");

        if (metadata.shouldDiscard()) {
            logger.debug(PROC_NAME, " processRawData: metadata.shouldDiscard() = true");
            return;
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("Null data input source");
        }

        final DataInputStream dis = inputStream.getDataInputStream();

        int          lenBits    = 0;
        byte[]       buff       = null;
        double       bitRate    = DEF_BIT_RATE;
        long         startTime  = System.currentTimeMillis();

        IStationTelemHeader customHeader = null;
        int neededBytes = parser.getHeaderBits() / Byte.SIZE +
                parser.getFrameBits() / Byte.SIZE +
                parser.getTrailerBits() / Byte.SIZE;

        //main loop
        while (! isStopped()) {
            final RawInputMetadata metadataClone = cloneRawInputMetadata(metadata);

            IParsedFrame parsedFrame = null;

            //read header + frame + trailer bytes from input stream
            final byte[] data =  new byte[neededBytes];
            readFromStream(dis, data, neededBytes);

            //read next frame from the input stream
            parsedFrame = parser.readNextFrame(data, metadataClone);
            if (parsedFrame == null) {
                // Stopped
                logger.trace(PROC_NAME, " processRawData: parsedFrame == null after readNextFrame; return");
                return;
            }

            buff       = parsedFrame.getFrame();
            int len        = buff.length;

            customHeader = parsedFrame.getHeader();

            lenBits = len * Byte.SIZE;

            messenger.incrementReadCount();
            logger.trace(PROC_NAME, " processRawData: read count incremented to ",  messenger.getReadCount());

            final long endTime = System.currentTimeMillis();

            //compute bitrate
            if (metadataClone.getBitRate() == null) {
                logger.trace(PROC_NAME, " processRawData: metadataClone.getBitRate() == null");

                // If this is not true, assume bit rate hasn't changed
                if (endTime > startTime) {
                    bitRate = lenBits / ((endTime - startTime) / 1000.0D);
                }

                logger.trace(PROC_NAME, " processRawData: computed our bitrate of ", bitRate);

                metadataClone.setBitRate(bitRate);
            }
            else {
                bitRate = metadata.getBitRate();
                logger.trace(PROC_NAME, " processRawData: obtained metadata bitrate of ", bitRate);
            }

            if (! isPaused()) {
                // If first read, send start of data message
                if (awaitingFirstData()) {
                    messenger.sendStartOfDataMessage();
                    setAwaitingFirstData(false);
                }

                final IStationTelemInfo dsnInfo = stationInfoFactory.create(bitRate, lenBits, metadataClone.getErt(),
                                                                            getConfiguredStation());

                metadataClone.setDsnInfo(dsnInfo);
                metadataClone.setDataLength(len);

                if (metadataClone.isOutOfSync()) {
                    logger.debug(PROC_NAME, ": Out of sync");

                    // If it was not out of sync before, but now it is, send loss of sync message
                    if (! isOutOfSync) {
                        String outOfSyncReason = metadataClone.getOutOfSyncReason();

                        if (outOfSyncReason == null) {
                            outOfSyncReason = "Received out-of-sync frame data";
                        }

                        logger.trace(PROC_NAME, "processRawData: sending out of sync message, with last frame ERT = ",
                                     ((ITransferFrameDataProcessor) rawDataProc).getLastFrameErt(), ", and reason = ", outOfSyncReason);

                        messenger.sendLossOfSyncMessage(dsnInfo, outOfSyncReason,
                                ((ITransferFrameDataProcessor)rawDataProc).getLastTfInfo(),
                                ((ITransferFrameDataProcessor)rawDataProc).getLastFrameErt());
                    }

                    isOutOfSync = true;
                }
                else if (isOutOfSync) {
                    logger.debug(PROC_NAME, ": In sync");

                    // If it was out of sync, but now it is back in sync, tell data processor to send in sync message
                    // we need the data processor to send it because we do not have the TF info

                    metadataClone.setNeedInSyncMessage(true);
                    isOutOfSync = false;
                }

                if (isOutOfSync) {
                    messenger.sendOutOfSyncBytesMessage(dsnInfo, buff);
                    continue;
                }

                logger.trace(PROC_NAME, " processRawData: publishing RawTransferFrameMessage");

                //holder objects needed for publishing RawTransferFrameMessage
                HeaderHolder headerHolder = null;
                TrailerHolder trailerHolder = null;
                try {
                    headerHolder = HeaderHolder.valueOf(customHeader.getHeader(), 0, customHeader.getSizeBytes());
                    trailerHolder = TrailerHolder.valueOf(parsedFrame.getTrailer());
                }
                catch (HolderException e){
                    logger.error(PROC_NAME + "processRawData: Cound not parse header / trailer");
                    throw new IOException(PROC_NAME + "processRawData()", e);
                }

                //create message and publish it
                context.publish(new RawTransferFrameMessage(metadataClone, buff, headerHolder, trailerHolder));

            }
            //paused
            else {
                bytesDiscarded += len;

                /*
                 * If stopping, don't suppress "bytes discarded" message until
                 * threshold is reached, since it could stop before we reach
                 * it. But otherwise, only send message when threshold is
                 * reached, so the message bus doesn't get overloaded.
                 */
                if (isStopped() || bytesDiscarded > bytesDiscardedWhilePausedThreshold) {
                    logger.warn(PROC_NAME  + ": Processing of raw input is paused: " + bytesDiscarded  + " bytes discarded.");
                    bytesDiscarded = 0;
                }
            }

            startTime = endTime;
            doMetering();
        }

        logger.trace(PROC_NAME, " processRawData: end of method; exited while loop");
    }

    private void readFromStream(final DataInputStream dis, byte[] data, final int neededBytes) throws IOException{
        int tmp = 0;
        int len        = 0;
        while (len < neededBytes) {
            setEofOnStreamStatus(false);
            tmp = dis.read(data, len, neededBytes - len);
            if (tmp < 0) {
                //Needed for client socket input: End-of-file
                setEofOnStreamStatus(true);
                setAwaitingFirstData(true);
                if (this.stopped) {
                    return;
                }
                throw new EOFException("Error reading buffer");
            }
            len += tmp;
        }
    }

    private static RawInputMetadata cloneRawInputMetadata(final RawInputMetadata rim) {
        RawInputMetadata metadataClone = null;

        try {
            metadataClone = rim.clone();
        }
        catch (final CloneNotSupportedException cnse) {
            metadataClone = new RawInputMetadata();

            logger.error( PROC_NAME + ": Cannot clone RawInputMetadata object", cnse);
        }

        return metadataClone;
    }
}
