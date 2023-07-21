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

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.sfdu.SfduException;
import jpl.gds.shared.sfdu.SfduVersionException;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.station.api.dsn.chdo.ChdoConfigurationException;
import jpl.gds.station.api.dsn.chdo.IChdo;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.dsn.message.IDsnMonitorMessage;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.data.ITransferFrameDataProcessor;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.UnknownTransferFrameException;
import jpl.gds.telem.input.impl.message.RawSfduTfMessage;


/**
 * This class is an implementation of <code>IRawStreamProcessor</code> that
 * processes SFDU wrapped transfer frame and GIF data
 *
 * NB: An EOFException is a subclass of IOException, so you MUST
 * catch it first if you care about the difference.
 * 
 *
 * MPCS-7610 - 11/10/16 - Changed to extend
 *          AbstractSfduStreamProcessor and use the integrated heartbeat logic
 */
public class SfduTfStreamProcessor extends AbstractSfduStreamProcessor {
    private static final String ME = "SfduTfStreamProcessor: ";

    /** Need this to get CADU size for trailer */
    private ITransferFrameDefinitionProvider tfDictionary = null;

    /** Cache the CADU size lookup */
    private final Map<Integer, Integer> caduSizeMap =
            new HashMap<>();

    /** Cache the turbo CADU size lookup */
    private final Map<Integer, Map<String, Integer>> turboCaduSizeMap =
            new HashMap<>();


    /**
     * Constructor 
     * @param serviceContext the current application context
     */
    public SfduTfStreamProcessor(final ApplicationContext serviceContext)
    {
        super(serviceContext);
        
        /*
         * 12/6/13 - MPCS-5555. Removed init of members for monitor channelization.
         */

        this.setAwaitingFirstData(true);
        
        tfDictionary = serviceContext.getBean(ITransferFrameDefinitionProvider.class);

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#processRawData(jpl.gds.telem.input.api.stream.IRawInputStream, jpl.gds.telem.input.api.message.RawInputMetadata)
     */
    @Override
    public void processRawData(final IRawInputStream inputStream,
            RawInputMetadata metadata) throws RawInputException, IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Null input SFDU source");
        }

        this.dis = inputStream.getDataInputStream();
        if (metadata == null) {
            metadata = new RawInputMetadata();
        }

        IChdoSfdu sfdu = null;
        try {
            sfdu = stationHeaderFactory.createChdoSfdu();
        } catch (final ChdoConfigurationException e) {
            logger.fatal("Could not parse CHDO dictionary: " + e.getMessage(), e);
            throw new RawInputException("Could not parse CHDO dictionary: "
                    + e.getMessage(), e);
        }

        IAccurateDateTime ert = null;
        double bitRate = 0.0;
        int relayScid = 0;
        
        heartbeatPerformance.setGood(true);

        while (!isStopped()) {
            try {
                final RawInputMetadata metadataClone = metadata.clone();

                // MPCS-5013 07/30/13
                setEofOnStreamStatus(false);

                startHeartbeatTimer();
                sfdu.readSfdu(dis);
                stopHeartbeatTimer();
                
                /* 
                 * The heartbeat status messages are just a safeguard to make sure data is flowing.
                 * Any valid response means the heartbeat status is good.
                 */ 
                this.heartbeatPerformance.setGood(true);
                if (sfdu.getSfduLabel().getVersionId() == 3){
                	continue;
                }
                
                messenger.incrementReadCount();

                if (sfdu.isMonitor()) {
                    logger.trace("Received monitor packet (processing).");

                    /*
                     * 12/6/13 -MPCS-5555. Pubish monitor SFDU message rather
                     * than invoking the channelizer directly.
                     */
                    final IDsnMonitorMessage monMessage = stationMessageFactory.createDsnMonitorMessage(sfdu);
                    context.publish(monMessage);

                    /*
                     * 11/25/13 - MPCS-5452. Send out start of data message
                     * so the chill_down indicators will change.
                     */
                    if (awaitingFirstData()) {
                        messenger.sendStartOfDataMessage();
                        setAwaitingFirstData(false);
                    }

                    continue;
                }

                final IChdo dataChdo = sfdu.getDataChdo();

                final int dataLen = dataChdo.getLength();
                metadataClone.setDataLength(dataLen);
                
                /**
                 * MPCS-7014 DE 8/27/2015 - Set the bit size in the metadata
                 * 
                 * setPadded removed because it is no longer received or valid.
                 * CHDO 069 specifies a field that specifies the number of bits
                 * in a message. the isPadded method is no longer utilized
                 * or reliable. Instead, now the number of bits is directly
                 * used where the byte size and isPadded utilized
                 */
                metadataClone.setBitSize(sfdu.getNumberOfDataBits());

                ert = sfdu.getFieldValueAsDate("ert");
                if (ert == null) {
                    ert = new AccurateDateTime(0);
                }

                metadataClone.setErt(ert);

                final Double tempBitRate = sfdu.getFieldValueAsFloatingPoint("bit_rate");
                if (tempBitRate != null) {
                    bitRate = tempBitRate.doubleValue();
                    metadataClone.setBitRate(bitRate);
                }

                // Get the DSS ID from the SFDU
                int dssId = 0;
                final Long tempDss = sfdu.getFieldValueAsUnsignedInt("data_source");
                if (tempDss != null) {
                    dssId = tempDss.intValue();
                }

                final Long tempRelayScid = sfdu.getFieldValueAsUnsignedInt("relay_scft_id");
                if (tempRelayScid != null) {
                    relayScid = tempRelayScid.intValue();
                }

                final IStationTelemInfo dsnInfo = stationInfoFactory.create(bitRate, dataLen * 8, ert, dssId);
                dsnInfo.setRelayScid(relayScid);

                metadataClone.setDsnInfo(dsnInfo);

                if (sfdu.isTurbo()
                        && metadataClone.getTurboEncodingRate() == null) {
                    metadataClone.setTurboEncodingRate(sfdu.getTurboRate());
                }

                if (!isPaused()) {
                    if (awaitingFirstData()) {
                        messenger.sendStartOfDataMessage();
                        setAwaitingFirstData(false);
                    }

                    /* MPCS-7677 - 9/15/15. Removed SFDU_GIF support. */

                    if (metadataClone.isOutOfSync()
                            || (sfdu.isOutOfSync() != null && sfdu.isOutOfSync())) {

                        // if it was not out of sync before, but now it is, send
                        // loss of sync message
                        if (!isOutOfSync)
                        {
                            String outOfSyncReason = metadataClone.getOutOfSyncReason();

                            if (outOfSyncReason == null)
                            {
                                outOfSyncReason = "Received out-of-sync frame data";
                            }

                            messenger.sendLossOfSyncMessage(
                                    dsnInfo,
                                    outOfSyncReason,
                                    ((ITransferFrameDataProcessor)rawDataProc).getLastTfInfo(),
                                    ((ITransferFrameDataProcessor)rawDataProc).getLastFrameErt());
                        }

                        isOutOfSync = true;
                    } else if (isOutOfSync) {
                        // if it was out of sync, but now it is back in sync,
                        // tell
                        // data processor to send in sync message
                        // we need the data processor to send it because we do
                        // not
                        // have the TF info

                        metadataClone.setNeedInSyncMessage(true);
                        isOutOfSync = false;
                    }

                    if (isOutOfSync) {
                        messenger.sendOutOfSyncBytesMessage(dsnInfo, dataChdo.getBytesWithoutChdoHeader());
                    } else {
                        if (inputType.equals(TelemetryInputType.SFDU_TF)
                                && !sfdu.isFrame()) {
                            logger.warn("SFDU does not match frame DDP IDs configured in the CHDO dictionary; it will be discarded. (DDP ID = "
                                    + sfdu.getSfduLabel().getDataDescriptionPackageId()
                                    + ")");
                            continue;
                        } 

                        if (! metadataClone.isBad() && sfdu.isInvalid())
                        {
                            metadataClone.setIsBad(true, InvalidFrameCode.UNKNOWN);
                        }

                        final byte[] dataBytes = dataChdo.getBytesWithoutChdoHeader();

                        final RawSfduTfMessage srdm = new RawSfduTfMessage(
                                sfdu,
                                metadataClone,
                                dataBytes,
                                sfdu.getEntireHeader(),
                                getTrailer(metadataClone, dataBytes));

                        srdm.setIdle(metadata.isIdle() || sfdu.isIdle());
                        srdm.setSfduLabel(sfdu.getSfduLabel());

                        logger.debug(ME + "sending SfduRawDataMessage " + srdm);
                        this.context.publish(srdm);
                    }
                } else {
                    this.bytesDiscarded += sfdu.getDataChdo().getLength();

                    /*
                     * If stopping, don't suppress "bytes discarded" message
                     * until threshold is reached, since it could stop before we
                     * reach it. But otherwise, only send message when threshold
                     * is reached, so the message bus doesn't get overloaded.
                     */
                    if (this.stopped
                            || this.bytesDiscarded > bytesDiscardedWhilePausedThreshold) {
                        logger.warn("Processing of raw input is paused: "
                                + this.bytesDiscarded + " bytes discarded.");
                        this.bytesDiscarded = 0;
                    }
                }
            } catch (final SfduVersionException sve) {
            	// MPCS-7610 11/14/16 - Determine and report what type of non-processable SFDU was received
            	registerUnhandledSFDUVersion(sve);
            } catch (final UnknownTransferFrameException utfe) {
            	logger.warn("Could not find TransferFrame in 'transfer_frame.xml': " + utfe.getMessage());
            } catch (final SfduException e) {
                logger.error("Could not process SFDU: " + e.getMessage(), e);
            } catch (final EOFException ee) {
                // MPCS-5013 07/30/13
                // MUST be before IOException

                // MPCS-5013 07/30/13
                setEofOnStreamStatus(true);

                if (this.stopped)
                {
                    return;
                }

                throw ee;
            } catch (final IOException e) {
                if (this.stopped) {
                    return;
                } else {
                    throw new IOException("I/O Exception encountered", e);
                }
            } catch (final CloneNotSupportedException e) {
                logger.error("Cannot clone RawInputMetadata object.", e);
            }
            finally {
            	stopHeartbeatTimer();
            }

            doMetering();
        }
    }


    /**
     * Extract the trailer. We have to get the frame dictionary so we can get
     * the format so we can get the configured CADU size. If that is less than
     * the actual body size, we extract the bytes at the end as the trailer.
     *
     * @param rim       Raw input metadata
     * @param dataBytes Frame body bytes
     *
     * @return Trailer holder
     *
     *
     * @throws RawInputException If unable to find CADU size
     * @throws UnknownTransferFrameException If unable to find CADU size
     */
    private TrailerHolder getTrailer(final RawInputMetadata rim,
            final byte[]           dataBytes)
                    throws RawInputException
                    {

        /**
         * MPCS-8144 05/16/16 - replaced dataLength with bitLength, get metadata value. Updated calls
         *           to get CADU size that utilize this value.
         *           SFDU TF are not padded and its metadata will always indicate it is not padded. However,
         *           getting the bit size indicated in the SFDU TF metadata WILL allow the data to be properly
         *           extracted.
         */
        final int bitLength = rim.getBitSize();
        final int caduSize   = (rim.isTurbo() ? getTurboCaduSize(
                bitLength,
                rim.getTurboEncodingRate())
                : getCaduSize(bitLength));
        final int delta      = Math.max(dataBytes.length - caduSize, 0);

        try
        {
            return ((delta > 0) ? TrailerHolder.valueOf(dataBytes, dataBytes.length - delta, delta)
                    : TrailerHolder.ZERO_HOLDER);
        }
        catch (final HolderException he)
        {
            throw new RawInputException("SfduTfStreamProcessor.getTrailer", he);
        }
    }


	/**
	 * Look up CADU size in map by data length, and create if not present.
	 *
	 * @param bitLength
	 *            Data length, in bits, to look up
	 *
	 * @return CADU size
	 *
	 * @throws UnknownTransferFrameException
	 *             if Transfer Frame for given data length cannot be found.
	 * 
	 * 05/16/16 - MPCS-8144 - changed length argument from bytes
	 *          to bits, adjusted parameter usage accordingly.
	 */
    private int getCaduSize(final int bitLength) throws UnknownTransferFrameException
    {
        Integer caduSize = caduSizeMap.get(bitLength);

        if (caduSize == null)
        {
        	/**
             * MPCS-7014 DE 8/31/2015 findFrameDefinition call updated
             * 
             * The findFrameDefinition function has been updated. It no longer takes the "matchOdd" boolean argument and now
             * requires the length argument to be in number of bits.
             */
        	
            final ITransferFrameDefinition tff = tfDictionary.findFrameDefinition( bitLength );

            if (tff == null)
            {
                throw new UnknownTransferFrameException("Unrecognized transfer frame size: " +
                        bitLength);
            }

            caduSize = tff.getCADUSizeBytes();

            caduSizeMap.put(bitLength, caduSize);
        }

        return caduSize.intValue();
    }


	/**
	 * Look up CADU size in map by data length and rate, and create if not
	 * present.
	 *
	 * @param bitLength
	 *            Data length, in bits, to lookup
	 * @param rate
	 *            Rate for second lookup
	 *
	 * @return CADU size
	 *
	 * @throws RawInputException
	 *             If unable to find CADU size
	 * 
	 * 05/16/16 MPCS-8144 - changed length argument from bytes to
	 *          bits, adjusted usage accordingly.
	 */
    private int getTurboCaduSize(final int    bitLength,
            final String rate)
                    throws RawInputException
                    {
        Integer              caduSize = null;
        Map<String, Integer> innerMap = turboCaduSizeMap.get(bitLength);

        if (innerMap == null)
        {
            innerMap = new HashMap<>();

            turboCaduSizeMap.put(bitLength, innerMap);
        }
        else
        {
            caduSize = innerMap.get(rate);
        }

        if (caduSize == null)
        {
        	/**
             * MPCS-7014 DE 8/31/2015 findFrameDefinition call updated
             * 
             * The findFrameDefinition function has been updated. It no longer takes the "matchOdd" boolean argument and now
             * requires the length argument to be in number of bits.
             */
        	
            final ITransferFrameDefinition tff =
                    tfDictionary.findFrameDefinition(bitLength, rate);

            if (tff == null)
            {
                throw new UnknownTransferFrameException(
                        "Unrecognized turbo transfer frame size: " +
                                bitLength                                 +
                                "/"                                        +
                                rate);
            }

            caduSize = tff.getCADUSizeBytes();

            innerMap.put(rate, caduSize);
        }

        return caduSize.intValue();
    }
}
