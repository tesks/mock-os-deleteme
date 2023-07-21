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

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.holders.VcidHolder;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.sfdu.SfduException;
import jpl.gds.shared.sfdu.SfduId;
import jpl.gds.shared.sfdu.SfduVersionException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.dsn.chdo.ChdoConfigurationException;
import jpl.gds.station.api.dsn.chdo.IChdo;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.dsn.message.IDsnMonitorMessage;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.message.RawSfduPktMessage;


/**
 * This class is an implementation of <code>IRawStreamProcessor</code> that
 * processes SFDU wrapped packet data
 *
 * NB: An EOFException is a subclass of IOException, so you MUST
 * catch it first if you care about the difference.
 *
 * NB: For SSE header channels:
 *
 * The VCID might well be returned although we would normally expect NULL for SSE.
 * That is because there is no way to signal "no value" in the SFDU fields.
 * It can still be NULL if there is no CHDO for it.
 *
 * If VCID is not NULL, then a zero value is expected. We check for that and warn if
 * it's something else. In any case, VCID is forced to NULL. 
 *
 * DSS id is similar, except that we force to UNSPECIFIED_DSSID (which is zero).
 *
 * See jpl.gds.eha.channel.raw.SfduHeaderChannelizer
 *
 * dssId defaults to -1 instead of UNSPECIFIED_DSSID and it is left that way to
 * avoid having to change downstream logic.
 *
 *
 * MPCS-7610 - 11/10/16 - Changed to extend
 *          AbstractSfduStreamProcessor and use the integrated heartbeat logic
 */
@ToDo("VCID should be replaced with a proper object that holds unsigned int")
public class SfduPktStreamProcessor extends AbstractSfduStreamProcessor {


    /** MPCS-5008 08/29/13 */
    private final boolean                      isSse;
    

    private final ISecondaryPacketHeaderLookup secHeaderLookup;

    private final CcsdsProperties packetProps;
    private final IStatusMessageFactory statusMsgFactory;


    /**
     * Constructor
     * @param serviceContext the current application context
     */
    public SfduPktStreamProcessor(final ApplicationContext serviceContext) {
        super(serviceContext);

        this.setAwaitingFirstData(true);
        packetProps = serviceContext.getBean(CcsdsProperties.class);
        secHeaderLookup = serviceContext.getBean(ISecondaryPacketHeaderLookup.class);
        statusMsgFactory = serviceContext.getBean(IStatusMessageFactory.class);

        isSse = appContext.getBean(SseContextFlag.class).isApplicationSse();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.impl.stream.AbstractRawStreamProcessor#init(jpl.gds.common.config.types.TelemetryInputType, boolean)
     */
    @Override
    public void init(final TelemetryInputType inputType, final boolean isRemoteMode) {
        super.init(inputType, isRemoteMode);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#processRawData(jpl.gds.telem.input.api.stream.IRawInputStream, jpl.gds.telem.input.api.message.RawInputMetadata)
     */
    @Override
    public void processRawData(final IRawInputStream inputStream, RawInputMetadata metadata)
    		throws RawInputException, IOException, EOFException {

        if (inputStream == null) {
            throw new IllegalArgumentException("Null input SFDU source");
        }

        // MPCS-7610 11/10/16 - moved dis to AbstractSfduStreamProcessor
        dis = inputStream.getDataInputStream();
        if (metadata == null) {
            metadata = new RawInputMetadata();
        }

        IChdoSfdu sfdu = null;
        try {
            sfdu = stationHeaderFactory.createChdoSfdu();
        } catch (final ChdoConfigurationException e) {
            logger.error("Could not parse CHDO dictionary: ", ExceptionTools.getMessage(e));
            throw new RawInputException("Could not parse CHDO dictionary: "
                    + e.getMessage(), e);
        }
        
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

                final PacketIdHolder packetId = null;

                /** MPCS-4693 10/28/13 Get dataChdo here */

                final IChdo dataChdo = sfdu.getDataChdo();

                final ISpacePacketHeader pktHeader = PacketHeaderFactory.create(packetProps.getPacketHeaderFormat());

           
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

                if (sfdu.isAnomaly().booleanValue() == true) {
                    logger.warn("Received anomaly packet (skipping).");
                    continue;
                }

                if (sfdu.isQqc().booleanValue() == true) {
                    logger.warn("Received QQC packet (skipping).");
                    continue;
                }

                if (sfdu.isCdr().booleanValue() == true) {
                    logger.warn("Received CDR packet (skipping).");
                    continue;
                }

                if (sfdu.isEcdr().booleanValue() == true) {
                    logger.warn("Received ECDR packet (skipping).");
                    continue;
                }

                if (sfdu.isPacket().booleanValue() == false) {
                    logger.warn("SFDU doesn't match DDP IDs for packets in CHDO dictionary (skipping).");
                    continue;
                }

                if(sfdu.isIdle().booleanValue() == true) {
                    logger.debug("Received idle packet (skipping).");
                    continue;
                }

                if (sfdu.isPacketFull().booleanValue() == false) {
                    logger.warn("Received partially full packet (skipping).");
                    continue;
                }

                /** MPCS-4693 10/28/13 Move creation of dataChdo up */

                final byte[] encapsulatedPacketBytes = dataChdo.getBytesWithoutChdoHeader();
                pktHeader.setHeaderValuesFromBytes(encapsulatedPacketBytes, 0);
                final ISecondaryPacketHeaderExtractor extractor = secHeaderLookup.lookupExtractor(pktHeader);
                // MPCS-8198 - 06/15/2016: Packet secondary header processing now goes through PacketHeaderFactory.
                if (! extractor.hasEnoughBytes(encapsulatedPacketBytes, pktHeader.getPrimaryHeaderLength())) {
               
                    final IPublishableLogMessage logm = statusMsgFactory
                            .createPublishableLogMessage(TraceSeverity.WARNING,
                                    "Packet abandoned because it is too short "
                                            + "apid="
                                            + pktHeader.getApid()
                                            + " isValid=false"
                                            + " len="
                                            + pktHeader.getPacketDataLength()
                                            + " version="
                                            + pktHeader.getVersionNumber()
                                            + " type=" + pktHeader.getPacketType(), LogMessageType.INVALID_PKT_DATA);

                    context.publish(logm);
                    logger.log(logm);
                    continue;
                }
                final ISecondaryPacketHeader secPktHdr = extractor.extract(encapsulatedPacketBytes, pktHeader.getPrimaryHeaderLength());

                final SfduId sfduId = sfdu.getSfduId();
                final int apid = sfduId.getFormatId();
                int scid = 0;
                final Long tempScid = sfdu.getFieldValueAsUnsignedInt("spacecraft_id");
                if (tempScid != null) {
                    scid = tempScid.intValue();
                } 

                ISclk sclk = sfdu.getFieldValueAsSclk("sclk");
                if (sclk == null) {
                    sclk = new Sclk(0, 0);
                }

                IAccurateDateTime scet = sfdu.getFieldValueAsDate("scet");
                if (scet == null) {
                    scet = new AccurateDateTime(0);
                }

                IAccurateDateTime ert = sfdu.getFieldValueAsDate("ert");
                if (ert == null) {
                    ert = new AccurateDateTime(0);
                }
                metadataClone.setErt(ert);

                // Get the VCID from the SFDU
                /** MPCS-5008 08/29/13 */
                Long vcid = sfdu.getFieldValueAsUnsignedInt("virtual_channel_id");

                int vfc = 0;
                final Long tempVfc = sfdu.getFieldValueAsUnsignedInt("virtual_frame_count");
                if (tempVfc != null) {
                    vfc = tempVfc.intValue();
                }

                double bitRate = 0.0;
                final Double tempBitRate = sfdu.getFieldValueAsFloatingPoint("bit_rate");
                if (tempBitRate != null) {
                    bitRate = tempBitRate.doubleValue();
                }

                int relayScid = 0;
                final Long tempRelayScid = sfdu.getFieldValueAsUnsignedInt("relay_scft_id");
                if (tempRelayScid != null) {
                    relayScid = tempRelayScid.intValue();
                }

                // Get the DSS ID from the SFDU
                /** MPCS-5008 08/29/13 */
                final Long tempDss = sfdu.getFieldValueAsUnsignedInt("data_source");
                int        dssId   = (tempDss != null) ? tempDss.intValue() : -1;

                /** MPCS-5008 08/29/13 */
                if (isSse)
                {
                    dssId = StationIdHolder.UNSPECIFIED_VALUE;
                    vcid = null;
                }

                final IStationTelemInfo dsnInfo = stationInfoFactory.create(bitRate, dataChdo.getLength() * 8, ert, dssId);
                dsnInfo.setRelayScid(relayScid);
                metadataClone.setDsnInfo(dsnInfo);
                metadataClone.setDataLength(dataChdo.getLength());
                /**
                 * MPCS-7674 DE 9/11/2015 - isPadded has been returned for SFDU packets but not frames (removed by MPCS-7014).
                 * Is padded is a valid value for an SFDU packet CHDO, pass it into the packet metadata.
                 */
                metadataClone.setPadded(sfdu.isPadded());
                
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

                if (!isPaused()) {
                    if (awaitingFirstData()) {
                        messenger.sendStartOfDataMessage();
                        setAwaitingFirstData(false);
                    }

                    final byte[] bytes = dataChdo.getBytesWithoutChdoHeader();

                    /**
                     * MPCS-7674 DE 9/11/2015 - isPadded has been returned for SFDU packets but not frames (removed by MPCS-7014).
                     * Is padded is a valid value for an SFDU packet CHDO, use it to determine if the pad byte is to be put into
                     * a trailer
                     */
                    final RawSfduPktMessage rspm =
                            new RawSfduPktMessage(
                                    sfdu,
                                    metadataClone,
                                    bytes,
                                    sfdu.getEntireHeader(),
                                    sfdu.isPadded()
                                    ? TrailerHolder.valueOf(bytes,
                                    		bytes.length - 1,
                                    		1)
                                    		: TrailerHolder.ZERO_HOLDER);

                    /** MPCS-5008 08/29/13 VCID should be Long not int */
                    int restrictedVcid = 0;

                    try
                    {
                        // NULL will be changed to 0, since setVcid takes int.
                        restrictedVcid = VcidHolder.restrictSfduVcid(vcid,
                                false);
                    }
                    catch (final IllegalArgumentException iae)
                    {
                        logger.warn("SFDU pkt stream: VCID of ", vcid, " forced to 0");
                    }

                    rspm.setApid(apid);
                    rspm.setScid(scid);
                    rspm.setSclk(sclk);
                    rspm.setScet(scet);
                    rspm.setVcid(restrictedVcid);
                    rspm.setVfc(vfc);
                    rspm.setPacketHeader(pktHeader);
                    rspm.setSecondaryHeaderLen(secPktHdr.getSecondaryHeaderLength());

                    rspm.setPacketId(packetId);

                    context.publish(rspm);
                } else {
                    bytesDiscarded += dataChdo.getBytesWithoutChdoHeader().length;

                    /*
                     * If stopping, don't suppress "bytes discarded" message
                     * until threshold is reached, since it could stop before we
                     * reach it. But otherwise, only send message when threshold
                     * is reached, so the message bus doesn't get overloaded.
                     */
                    if (isStopped()
                            || bytesDiscarded > bytesDiscardedWhilePausedThreshold) {
                        logger.warn("Processing of raw input is paused: ", bytesDiscarded, " bytes discarded.");
                        bytesDiscarded = 0;
                    }

                }

            }
            //MPCS-7674 DE 9/11/2015 - reinserted catch due to change of trailer declaration in RawSfduPktMessage above
            catch (final HolderException he){
                logger.error("Could not process trailer: ", ExceptionTools.getMessage(he));
            } catch (final SfduVersionException sve){
            	// MPCS-5013 07/31/13 PMD. Split out. Must come before SfduException.
            	// MPCS-7610 11/14/16 - Determine and report what type of non-processable SFDU was received
            	registerUnhandledSFDUVersion(sve);
            } catch (final SfduException e){
            	// MPCS-5013 07/31/13 PMD. Split out.
                logger.error("Could not process SFDU: ", ExceptionTools.getMessage(e));
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
                }else {
                    throw new IOException("I/O Exception encountered", e);
                }
            } catch (final CloneNotSupportedException e) {
                logger.error("Cannot clone RawInputMetadata object.", ExceptionTools.getMessage(e));
            }
            finally {
            	stopHeartbeatTimer();
            }
            
            doMetering();
        }
    }
}
