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

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.types.Triplet;
import jpl.gds.station.api.IStationMessageFactory;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.data.ITransferFrameDataProcessor;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.message.RawTransferFrameMessage;
import org.springframework.context.ApplicationContext;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class is an implementation of IRawStreamProcessor that processes transfer frame streams. A header simply
 * precedes a normal transfer frame. We must locate the header in order to find a transfer frame.
 * <p>
 * This code is similar to TransferFrameStreamProcessor, except all frames are presumed to be synchronized. Valid frames
 * are a header followed by an ASM header followed by the rest of the transfer frame. Theoretically there can be
 * multiple frame types supported, each with a different ASM header. We don't want to get in the business of analyzing
 * the transfer frames themselves. The constructor reads the dictionary and gets all ASM headers. We presume that a
 * valid header followed by one of the ASM sequences is sufficient to find a transfer frame.
 * <p>
 * <p>
 * To avoid false synchronization, we check the fields of the header as best we can, but that is a weak test. That is
 * why the ASM is so important.
 * <p>
 * We do extract some information from the header, such as error conditions and ERT.
 * <p>
 * Transfer frames are stripped of the header but not of the ASM header.
 * <p>
 * To avoid excessive memory use while synchronizing we read blocks of bytes and then attempt to synchronize within
 * them. We need a block at least long enough to hold the header and the longest ASM header. If a synchronization point
 * is found, we then read enough to get to the end of the transfer frame.
 *
 */
public class AbstractCustomFrameHeaderStreamProcessor extends AbstractRawStreamProcessor {
    private static final String                 SEQ_ERROR    =
            "Master channel sequence error";
    private static final double                 DEF_BIT_RATE = 10000.0D;
    // MPCS-5013 07/24/13 Made non-static and final
    protected final      StringBuilder          sb           = new StringBuilder(1024);
    /**
     * ASMs for all configured frame types
     */
    protected final      byte[][]               _asms;
    /**
     * Length of longest ASM
     */
    protected final      int                    _asmLength;
    protected final      IStationMessageFactory stationMessageFactory;
    /**
     * Default StreamType (aka frame format) is LEO-T
     **/
    protected            StreamType             _streamType  = StreamType.LEOT_TF;
    private              int                    _headerSize;


    public AbstractCustomFrameHeaderStreamProcessor(final ApplicationContext serviceContext, final int headerSize,
                                                    StreamType type) throws RawInputException {
        this(serviceContext);
        _headerSize = headerSize;
        _streamType = type;
    }

    /**
     * Constructor.
     *
     * @param serviceContext the current application context
     * @throws RawInputException if there is a problem creating the processor
     */
    public AbstractCustomFrameHeaderStreamProcessor(final ApplicationContext serviceContext) throws RawInputException {
        super(serviceContext);

        // Get ASMs for all supported frame types

        List<byte[]> asms;
        try {
            asms = loadAsm(serviceContext);
        } catch (final DictionaryException e) {
            e.printStackTrace();
            throw new RawInputException(e);
        }

        int asmLength = 0;

        for (final byte[] asm : asms) {
            asmLength = Math.max(asmLength, asm.length);
        }

        if (asmLength == 0) {
            throw new RawInputException(
                    _streamType + ": All supported frame types lack ASM");
        }

        _asmLength = asmLength;
        _asms = asms.toArray(new byte[asms.size()][]);

        this.stationMessageFactory = serviceContext.getBean(IStationMessageFactory.class);
    }

    /**
     * Clone the metadata.
     *
     * @param rim The old metadata
     * @return The new metadata
     */
    private static RawInputMetadata cloneRawInputMetadata(
            final RawInputMetadata rim) {
        RawInputMetadata metadataClone = null;

        try {
            metadataClone = rim.clone();
        } catch (final CloneNotSupportedException cnse) {
            metadataClone = new RawInputMetadata();

            logger.error("Cannot clone RawInputMetadata object",
                    cnse);
        }

        return metadataClone;
    }

    /**
     * Extract frame header and process.
     *
     * @param head frame header object
     * @param rim  Working metadata
     */
    protected static void processHeader(final IStationTelemHeader head,
                                        final RawInputMetadata rim) {
        if (head.isBadFrame()) {
            rim.setIsBad(true, head.getBadReason());
        }

        if (head.isOutOfSequence()) {
            rim.setOutOfSync(true, SEQ_ERROR);
        } else {
            rim.setOutOfSync(false, null);
        }

        // Instant stores nanos of second, but AccurateDateTime wants nanos of millisecond
        //
        // 2021-10-08
        //
        // This can be a bit confusing so here is an example:
        // Lets say the data/time with nanosecond precision is 2021-271T16:52:09.642291555
        // Instant getNano() will return the full 9 digits after the second (642291555) however
        // AccurateDateTime wants the 6 digits after the milliseconds which in this
        // will be 291555 excluding the first 3 digits of milliseconds (642)
        int lastSixDigitsOfNano = head.getErt().getNano() % 1000000;

        rim.setErt(new AccurateDateTime(head.getErt().toEpochMilli(), lastSixDigitsOfNano));

        logger.trace("Frame header : " + head);
    }

    /**
     * Check buffer against ASM.
     *
     * @param block  Byte buffer
     * @param offset Where to start looking
     * @param asm    ASM to check against
     * @return True if ASM matches
     */
    protected static boolean checkASM(final byte[] block,
                                      final int offset,
                                      final byte[] asm) {
        if ((block.length - offset) < asm.length) {
            throw new IllegalArgumentException("Too small for ASM");
        }

        for (int i = 0; i < asm.length; ++i) {
            if (block[offset + i] != asm[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get all unique non-empty ASMs for all supported frame types.
     *
     * @return List of ASM
     * @throws DictionaryException Problem loading dictionary
     */
    private static List<byte[]> loadAsm(final ApplicationContext serviceContext) throws DictionaryException {
        final List<byte[]>                     asms = new ArrayList<byte[]>();
        final ITransferFrameDefinitionProvider tfd  = serviceContext.getBean(ITransferFrameDefinitionProvider.class);

        for (final ITransferFrameDefinition tff : tfd.getFrameDefinitions()) {
            final byte[] nextAsm = tff.getASM();

            if ((nextAsm == null) || (nextAsm.length == 0)) {
                logger.error("Frame type " +
                        tff.getName() +
                        " has no ASM header");
                continue;
            }

            boolean found = false;

            for (final byte[] old : asms) {
                if (Arrays.equals(old, nextAsm)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                asms.add(nextAsm);
            }
        }

        return asms;
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#processRawData(jpl.gds.telem.input.api.stream.IRawInputStream,
     * jpl.gds.telem.input.api.message.RawInputMetadata)
     */
    @Override
    public void processRawData(final IRawInputStream inputStream,
                               final RawInputMetadata metadata)
            throws RawInputException, IOException {
        logger.trace(" processRawData: start of method");

        if (metadata.shouldDiscard()) {
            logger.debug(" processRawData: metadata.shouldDiscard() = true");
            return;
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("Null data input source");
        }

        final DataInputStream dis      = inputStream.getDataInputStream();
        final byte[]          data     = inputStream.getData();
        final int             deltaMin = _headerSize + _asmLength;
        final int             deltaMax = Math.max(rawConfig.getReadBufferSize(), deltaMin);

        int          len        = 0;
        int          lenBits    = 0;
        byte[]       buff       = null;
        byte[]       extra      = null;
        double       bitRate    = DEF_BIT_RATE;
        long         startTime  = System.currentTimeMillis();
        boolean      singleRead = false;
        HeaderHolder header     = HeaderHolder.ZERO_HOLDER;
        /*
         * 11/21/13 - MPCS-5550. Now that we are processing NEN
         * status packets we need the header here.
         */
        IStationTelemHeader telemHeader = null;

        sb.setLength(0);
        sb.append(" processRawData: deltaMin = ");
        sb.append(deltaMin);
        sb.append(", deltaMax = ");
        sb.append(deltaMax);
        logger.trace(sb.toString());

        while (!isStopped()) {
            final RawInputMetadata metadataClone =
                    cloneRawInputMetadata(metadata);

            FrameBytes fb = null;
            if (data == null) {
                fb = getNextFrame(dis,
                        metadataClone,
                        deltaMin,
                        deltaMax,
                        extra);
                if (fb == null) {
                    // Stopped

                    logger.trace(" processRawData: fb == null after DIS getNextFrame; return");
                    return;
                }

                extra = fb.getExtra();

            } else {
                fb = getNextFrame(data, metadataClone);

                if (fb == null) {
                    // Stopped

                    logger.trace(" processRawData: fb == null after byte[] getNextFrame; return");
                    return;
                }

                singleRead = true; // We don't have a stream of data
            }

            /*
             * 11/21/13 - MPCS-5550. Moved outside if the if-else above.
             */
            buff = fb.getFrame();
            len = buff.length;
            /*
             *  11/21/13 - MPCS-5550. Now that we are processing NEN
             * status packets we need the header here.
             */
            telemHeader = fb.getHeader();
            try {
                header = HeaderHolder.valueOf(telemHeader.getHeader(), 0, _headerSize);
            } catch (HolderException e) {
                logger.error(" processRawData: Cound not parse header / trailer");
                throw new IOException(" processRawData()", e);
            }

            lenBits = len * Byte.SIZE;

            messenger.incrementReadCount();
            sb.setLength(0);
            sb.append(" processRawData: read count incremented to ");
            sb.append(messenger.getReadCount());
            logger.trace(sb.toString());

            final long endTime = System.currentTimeMillis();

            if (metadataClone.getBitRate() == null) {
                logger.trace(" processRawData: metadataClone.getBitRate() == null");

                // If this is not true, assume bit rate hasn't changed

                if (endTime > startTime) {
                    bitRate = lenBits / ((endTime - startTime) / 1000.0D);
                }

                sb.setLength(0);
                sb.append(" processRawData: computed our bitrate of ");
                sb.append(bitRate);
                logger.trace(sb.toString());

                metadataClone.setBitRate(bitRate);
            } else {
                bitRate = metadataClone.getBitRate();

                sb.setLength(0);
                sb.append(" processRawData: obtained metadata bitrate of ");
                sb.append(bitRate);
                logger.trace(sb.toString());
            }

            if (!isPaused()) {
                if (awaitingFirstData()) {
                    messenger.sendStartOfDataMessage();

                    setAwaitingFirstData(false);
                }

                // MPCS-5013 07/03/13 Use configured station
                final IStationTelemInfo dsnInfo = getStationTelemInfo(lenBits, bitRate, metadataClone, telemHeader);

                metadataClone.setDsnInfo(dsnInfo);
                metadataClone.setDataLength(len);

                if (metadataClone.isOutOfSync()) {
                    logger.debug(_streamType + ": Out of sync");

                    // If it was not out of sync before, but now it is, send
                    // loss of sync message
                    if (!isOutOfSync) {
                        String outOfSyncReason =
                                metadataClone.getOutOfSyncReason();

                        if (outOfSyncReason == null) {
                            outOfSyncReason =
                                    "Received out-of-sync frame data";
                        }

                        sb.setLength(0);
                        sb.append(" processRawData: sending out of sync message, with last frame ERT = ");

                        /**
                         * MPCS-9947 6/27/18: Removed toString() on ERT because it can be null
                         * a NPE here throws off processing because outOfSync does not get set
                         */
                        sb.append(((ITransferFrameDataProcessor) rawDataProc).getLastFrameErt());
                        sb.append(", and reason = ");
                        sb.append(outOfSyncReason);
                        logger.trace(sb.toString());

                        messenger.sendLossOfSyncMessage(
                                dsnInfo,
                                outOfSyncReason,
                                ((ITransferFrameDataProcessor) rawDataProc).getLastTfInfo(),
                                ((ITransferFrameDataProcessor) rawDataProc).getLastFrameErt());
                    }

                    isOutOfSync = true;
                } else if (isOutOfSync) {
                    logger.debug(_streamType + ": In sync");

                    // If it was out of sync, but now it is back in sync,
                    // tell data processor to send in sync message
                    // we need the data processor to send it because we do
                    // not have the TF info

                    metadataClone.setNeedInSyncMessage(true);

                    isOutOfSync = false;
                }

                if (isOutOfSync) {
                    messenger.sendOutOfSyncBytesMessage(dsnInfo, buff);

                    if (singleRead) {
                        break;
                    }

                    continue;
                }

                publishSuccessMessage(metadataClone, buff, header, telemHeader);
            } else {
                bytesDiscarded += len;

                /*
                 * If stopping, don't suppress "bytes discarded" message until
                 * threshold is reached, since it could stop before we reach
                 * it. But otherwise, only send message when threshold is
                 * reached, so the message bus doesn't get overloaded.
                 */
                if (isStopped() ||
                        (bytesDiscarded > bytesDiscardedWhilePausedThreshold)) {
                    logger.warn(_streamType +
                            ": Processing of raw input is paused: " +
                            bytesDiscarded +
                            " bytes discarded.");

                    bytesDiscarded = 0;
                }
            }

            startTime = endTime;

            doMetering();

            if (singleRead) {
                logger.trace(" processRawData: end of method; singleRead, so return");
                return;
            }
        }

        logger.trace(" processRawData: end of method; exited while loop");
    }

    protected IStationTelemInfo getStationTelemInfo(int lenBits, double bitRate, RawInputMetadata metadataClone, IStationTelemHeader header) {
        final IStationTelemInfo dsnInfo = stationInfoFactory.create(bitRate,
                lenBits,
                metadataClone.getErt(),
                getConfiguredStation());
        return dsnInfo;
    }

    /**
     * Default behavior for publishing a successfully-parsed RawTransferFrame message to the JMS. This method can be
     * overridden to change the behavior.
     *
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

    /**
     * Read from stream until data block is large enough. If an old block is supplied, augment it.
     *
     * @param dis      Input stream
     * @param soFar    Existing data block or null
     * @param keep     Where to start in old block
     * @param required Number of bytes I must have
     * @param askFor   Amount to ask for
     * @return Augmented block
     * @throws IOException Error reading into buffer
     */
    protected byte[] getDataBlock(final DataInputStream dis,
                                  final byte[] soFar,
                                  final int keep,
                                  final int required,
                                  final int askFor)
            throws IOException {
        sb.setLength(0);
        sb.append(" getDataBlock: start method; keep = ");
        sb.append(keep);
        sb.append(", required = ");
        sb.append(required);
        sb.append(", askFor = ");
        sb.append(askFor);
        logger.trace(sb.toString());

        if (askFor < required) {
            throw new IllegalArgumentException("Inconsistent arguments");
        }

        final int soFarLen = (soFar != null)
                ? (soFar.length - keep)
                : 0;
        final byte[] buffer = new byte[Math.max(askFor, soFarLen)];
        int          bufLen = 0;

        if (soFarLen > 0) {
            // Seed with previous value

            bufLen = soFarLen;

            sb.setLength(0);
            sb.append(" getDataBlock: soFarLen > 0; System.arraycopy(soFar, ");
            sb.append(keep);
            sb.append(", buffer, 0, ");
            sb.append(soFarLen);
            sb.append(")");
            logger.trace(sb.toString());

            System.arraycopy(soFar, keep, buffer, 0, soFarLen);
        }

        if (bufLen >= askFor) {
            sb.setLength(0);
            sb.append(" getDataBlock: returning truncate; bufLen >= askFor (");
            sb.append(bufLen);
            sb.append(" >= ");
            sb.append(askFor);
            sb.append(")");
            logger.trace(sb.toString());
            return truncate(buffer, bufLen);
        }

        while (bufLen < askFor) {
            // MPCS-5013 07/23/13
            setEofOnStreamStatus(false);

            final int tmplen = dis.read(buffer, bufLen, askFor - bufLen);
            sb.setLength(0);
            sb.append(" getDataBlock: bufLen = ");
            sb.append(bufLen);
            sb.append(", askFor - bufLen = ");
            sb.append(askFor - bufLen);
            sb.append(", tmplen = ");
            sb.append(tmplen);
            logger.trace(sb.toString());

            if (tmplen < 0) {
                // End-of-file

                // MPCS-5013 07/23/13 Needed for client socket input
                setEofOnStreamStatus(true);

                if (bufLen >= required) {
                    // I didn't get all, but I got enough

                    sb.setLength(0);
                    sb.append(" getDataBlock: returning truncate; bufLen >= required (");
                    sb.append(bufLen);
                    sb.append(" >= ");
                    sb.append(required);
                    sb.append(")");
                    logger.trace(sb.toString());
                    return truncate(buffer, bufLen);
                }

                if (!isStopped()) {
                    logger.debug(_streamType +
                            ": Final read short " +
                            (required - bufLen) +
                            " bytes");
                }

                logger.trace(" getDataBlock: returning null");
                return null;
            }

            sb.setLength(0);
            sb.append(" getDataBlock: bufLen += tmplen (");
            sb.append(bufLen);
            sb.append(" += ");
            sb.append(tmplen);
            sb.append(")");
            logger.trace(sb.toString());
            bufLen += tmplen;
        }

        logger.trace(" getDataBlock: returning truncate at end of method");
        return truncate(buffer, bufLen);
    }

    /**
     * Read an entire frame from the stream. Update the metadata from the header, but do not return header.
     *
     * @param dis      Input stream
     * @param rim      Metadata
     * @param deltaMin Amount I need, at least
     * @param deltaMax Amount to ask for
     * @param extra    Bytes left from last time or null
     * @return Triplet of frame bytes, extra or null, header
     * @throws IOException Error reading into buffer
     */
    protected FrameBytes getNextFrame(final DataInputStream dis,
                                      final RawInputMetadata rim,
                                      final int deltaMin,
                                      final int deltaMax,
                                      final byte[] extra)
            throws IOException {
        sb.setLength(0);
        sb.append(" DIS getNextFrame: start method; deltaMin = ");
        sb.append(deltaMin);
        sb.append(", deltaMax = ");
        sb.append(deltaMax);
        logger.trace(sb.toString());

        final IStationTelemHeader head = getHeader();

        byte[] block   = extra;
        int    next    = 0;
        int    skipped = 0;

        while (true) {
            logger.trace(" DIS getNextFrame: calling initial getDataBlock");
            block = getDataBlock(dis, block, next, deltaMin, deltaMax);

            if (block == null) {
                // No more data

                if (skipped > 0) {
                    logger.debug(_streamType +
                            ": Skipped " +
                            skipped +
                            " bytes searching for header");
                }

                logger.trace(" DIS getNextFrame: block == null so returning null");
                return null;
            }

            // We can't step through a partial header; get last usable index
            // where we have a full header and ASM

            final int last = block.length - head.getSizeBytes()
                    - _asmLength;
            boolean found = false;

            // Walk down buffer looking for a match

            boolean scanMode = false;

            for (int i = 0; i <= last; ++i) {
                sb.setLength(0);
                sb.append(" DIS getNextFrame: searching header; i = ");
                sb.append(i);
                logger.trace(sb.toString());

                final int asmAt = head.load(block, i);


                if (head.isValid()) {
                    logger.trace(" DIS getNextFrame: found header");

                    if (scanMode) {
                        logger.info("Scan on buffer found frame header at offset " + i);
                        scanMode = false;
                    }

                    for (final byte[] asm : _asms) {
                        int loc = checkAsmAtIndex(asm, block, asmAt, head, rim);
                        if (loc > -1) {
                            found = true;
                            next = loc;
                            break;
                        }
                    }

                    if (found) {
                        break;
                    }
                } else if (i == 0) {
                    logger.warn("Incoming stream does not have frame header. Starting scan on rest of buffer.");
                    scanMode = true;
                }

                // No match on header and/or ASM
                ++skipped;
            }

            if (found) {
                break;
            }

            // Keep trailing portion and keep going

            next = last + 1;
        }

        if (skipped > 0) {
            logger.debug(_streamType +
                    ": Skipped " +
                    skipped +
                    " bytes searching for frame header");
        }

        final int tduLength = head.getDataLength();

        // Read the rest of the frame, which immediately follows

        logger.trace(" DIS getNextFrame: calling second getDataBlock to read rest of the frame");

        block = getDataBlock(dis,
                block,
                next,
                tduLength,
                Math.max(deltaMax, tduLength));

        if (block == null) {
            logger.trace(" DIS getNextFrame: block == null so returning null");
            return null;
        }

        sb.setLength(0);
        sb.append(" DIS getNextFrame: end of method; return split(block, ");
        sb.append(tduLength);
        sb.append(")");
        logger.trace(sb.toString());

        /*
         * 11/21/13 - MPCS-5550. FrameBytes now carries the header
         * instead of a header holder.
         */
        return split(block, tduLength, head);
    }

    /**
     * Check for an ASM at the given index, update the raw input metadata if found
     * <p>
     * 2021-12-07 - MPCS-12335/PSYCHEMCS-141 - extracted from #getNextFrame for SLE_TF processing
     *
     * @param asm   attached sync marker
     * @param block block of bytes to check
     * @param asmAt index of asm
     * @param head  transfer frame header
     * @param rim   raw input metadata
     * @return index of ASM, or -1 if not found
     */
    protected int checkAsmAtIndex(byte[] asm, byte[] block, int asmAt, IStationTelemHeader head, RawInputMetadata rim) {
        if (checkASM(block, asmAt, asm)) {
            sb.setLength(0);
            sb.append(" DIS getNextFrame: found ASM at ");
            sb.append(asmAt);
            logger.trace(sb.toString());

            // Found it, so process and set up to get frame
            processHeader(head, rim);

            return asmAt;
        }
        return -1;
    }

    /**
     * LEO-T is our default header type. This method should be overridden in extensions of this abstract class.
     *
     * @return LEO-T header
     */
    protected IStationTelemHeader getHeader() {
        return stationHeaderFactory.createLeotHeader();
    }

    /**
     * Extract an entire frame. Update the metadata from the frame header, but do not return header.
     *
     * @param data Raw bytes
     * @param rim  Metadata
     * @return Frame bytes with no extra but a header
     * @throws IOException If unable to get next frame
     */
    private FrameBytes getNextFrame(final byte[] data,
                                    final RawInputMetadata rim)
            throws IOException {
        logger.trace(" byte[] getNextFrame: start method");


        final IStationTelemHeader head = getHeader();

        if (data.length <= (head.getSizeBytes() + _asmLength)) {
            logger.error(
                    _streamType + " byte[] getNextFrame: Data length too short for frame header (length = " + data.length + ", header = " + head.getSizeBytes() + _asmLength);

            return null;
        }
        head.load(data, 0);

        if (!head.isValid()) {
            logger.error(_streamType + " byte[] getNextFrame: Unable to find frame header");

            return null;
        }

        // We found a header
        logger.trace(" byte[] getNextFrame: found frame header");
        logger.trace(head.toString());

        processHeader(head, rim);

        final int tduLength = head.getDataLength();
        final int frmLength = data.length - head.getSizeBytes();

        if (tduLength != frmLength) {
            logger.error(
                    _streamType + " byte[] getNextFrame: Length mismatch for frame header (tduLength = " + tduLength + ", frmLength = " + frmLength);

            return null;
        }

        sb.setLength(0);
        sb.append(" byte[] getNextFrame: tduLength = frmLength = ");
        sb.append(frmLength);
        logger.trace(sb.toString());

        // Return the frame, which immediately follows

        final byte[] frame = new byte[frmLength];

        System.arraycopy(data,
                head.getSizeBytes(),
                frame,
                0,
                frmLength);

        // Check ASM, which must be first

        boolean found = false;

        for (final byte[] asm : _asms) {
            if (checkASM(frame, 0, asm)) {
                found = true;

                logger.trace(" byte[] getNextFrame: found ASM");

                break;
            }
        }

        if (!found) {
            logger.error(_streamType + ": ASM mismatch after frame header");

            return null;
        }

        logger.trace(" byte[] getNextFrame: end of method; returning frame");

        /*
         * 11/21/13 - MPCS-5550. FrameBytes now carries the header
         * instead of a header holder.
         */
        return new FrameBytes(frame, null, head);
    }

    /**
     * Split block into frame and extra bytes and header.
     *
     * @param block     Byte array to be split
     * @param frameSize Number of bytes in frame
     * @param header    IStationTelemHeader header object
     * @return Triplet of frame, extra blocks, header
     */
    protected FrameBytes split(final byte[] block,
                               final int frameSize,
                               final IStationTelemHeader header) {
        // MPCS-5013 07/24/13 Made method non-static

        if (block.length < frameSize) {
            throw new IllegalArgumentException("Block too small");
        }

        if (block.length == frameSize) {
            sb.setLength(0);
            sb.append(" split: block.length = frameSize = ");
            sb.append(frameSize);
            sb.append("; returning new FrameBytes(block, null)");
            logger.trace(sb.toString());

            return new FrameBytes(block, null, header);
        }

        final int    extraSize = block.length - frameSize;
        final byte[] frame     = new byte[frameSize];
        final byte[] extra     = new byte[extraSize];

        sb.setLength(0);
        sb.append(" split: end of method; returning new FrameBytes(frame, extra); frameSize = ");
        sb.append(frameSize);
        sb.append(", extraSize = ");
        sb.append(extraSize);
        logger.trace(sb.toString());

        System.arraycopy(block, 0, frame, 0, frameSize);
        System.arraycopy(block, frameSize, extra, 0, extraSize);

        return new FrameBytes(frame, extra, header);
    }

    /**
     * Make sure buffer is not too long.
     *
     * @param buffer Byte array
     * @param length Desired length
     * @return Byte array of correct length
     */
    private byte[] truncate(final byte[] buffer,
                            final int length) {
        // MPCS-5013 07/24/13 Method made non-final

        if (buffer.length == length) {
            sb.setLength(0);
            sb.append(" truncate: buffer.length == length == ");
            sb.append(length);
            logger.trace(sb.toString());
            return buffer;
        }

        if (buffer.length < length) {
            throw new IllegalArgumentException("Buffer too short");
        }

        sb.setLength(0);
        sb.append(" truncate: returning Arrays.copyOf(buffer, ");
        sb.append(length);
        sb.append(")");
        logger.trace(sb.toString());
        return Arrays.copyOf(buffer, length);
    }

    /**
     * Holder class for frame bytes and extra bytes and header
     *
     * 11/21/13 - MPCS-5550. Changed class of the third element in the triplet from HeaderHolder to
     * IStationTelemHeader to support NEN status processing.
     */
    protected static class FrameBytes
            extends Triplet<byte[], byte[], IStationTelemHeader> {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param frame     Frame bytes
         * @param extra     Extra bytes
         * @param headerObj IStationTelemHeader header object
         */
        public FrameBytes(final byte[] frame,
                          final byte[] extra,
                          final IStationTelemHeader headerObj) {
            super(frame, extra, headerObj);
        }


        /**
         * Get frame bytes.
         *
         * @return Frame bytes
         */
        public byte[] getFrame() {
            return getOne();
        }


        /**
         * Get extra bytes.
         *
         * @return Extra bytes
         */
        public byte[] getExtra() {
            return getTwo();
        }


        /**
         * Get IStationTelemHeader header.
         *
         * @return IStationTelemHeader object
         */
        public IStationTelemHeader getHeader() {
            return getThree();
        }

    }
}