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
/**
 * 
 */
package jpl.gds.telem.input.impl.data.helper;

import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.ccsds.api.tm.frame.TelemetryFrameHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.types.IDbFrameProvider;
import jpl.gds.db.api.types.IDbPacketProvider;
import jpl.gds.db.api.types.IDbRawData;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.data.helper.IPktProcessorHelper;
import jpl.gds.telem.input.api.data.helper.ITfProcessorHelper;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.impl.message.RawDatabaseMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;


/**
 * This is a helper adapter class that is used by <code>IRawDataProcessor</code>
 * classes to process raw data from MPCS database. The raw data from the
 * database is in the form of <code>DatabaseFrame</code> or
 * <code>DatabaseRawData</code> objects
 * 
 *
 * MPCS-7039 - 7/10/15. No longer need map of transfer frame types.
 *          Associated member field removed. 
 */
public class DbDataProcessorHelper implements ITfProcessorHelper,
IPktProcessorHelper
{
	private final boolean setSolTimes;
	private final ITransferFrameDefinitionProvider frameDict;	
	private final ISecondaryPacketHeaderLookup secPktHeaderLookup;
	private final int scid;
	private final ITelemetryPacketInfoFactory pktInfoFactory;
	private final MissionProperties missionProps;
    private final CcsdsProperties ccsdsProps;
    private final ITelemetryFrameInfoFactory frameInfoFactory;
    private final SseContextFlag                   sseFlag;
	
	/**
	 * Constructor.
	 * 
	 * @param serviceContext the current application context
	 *
	 */
	public DbDataProcessorHelper(final ApplicationContext serviceContext) {
        setSolTimes = serviceContext.getBean(EnableLstContextFlag.class).isLstEnabled();
        frameDict = serviceContext.getBean(ITransferFrameDefinitionProvider.class);
        scid =  serviceContext.getBean(IContextIdentification.class).getSpacecraftId();
        secPktHeaderLookup = serviceContext.getBean(ISecondaryPacketHeaderLookup.class);
        pktInfoFactory = serviceContext.getBean(ITelemetryPacketInfoFactory.class);
        missionProps = serviceContext.getBean(MissionProperties.class);
        ccsdsProps = serviceContext.getBean(CcsdsProperties.class);
        frameInfoFactory = serviceContext.getBean(ITelemetryFrameInfoFactory.class);
        sseFlag = serviceContext.getBean(SseContextFlag.class);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IDataProcessorHelper#init(jpl.gds.common.config.types.TelemetryInputType)
	 */
	@Override
	public void init(final TelemetryInputType inputType) throws RawInputException
	{
	    // do nothing
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getPacketHeader(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ISpacePacketHeader getPacketHeader(final IRawDataMessage message)
			throws RawInputException {
		final RawDatabaseMessage rdm = getRawDatabaseMessage(message);
        final IDbPacketProvider dp = getDatabasePacket(rdm.getDbRawData());

		final ISpacePacketHeader packetHeader = PacketHeaderFactory.create(ccsdsProps.getPacketHeaderFormat());
		packetHeader.setHeaderValuesFromBytes(dp.getRecordBytes(), 0);


		String errMsg = null;
		LogMessageType logMsgType = null;
		// MPCS-8198 - 06/15/2016: Packet secondary header information moved to PacketHeaderFactory.
		final boolean enoughBytes = secPktHeaderLookup
				.lookupExtractor(packetHeader)
				.hasEnoughBytes(dp.getRecordBytes(), packetHeader.getPrimaryHeaderLength());
		if (!enoughBytes) {
			errMsg = "Packet abandoned because it is too short " + "apid="
					+ packetHeader.getApid() + " isValid=false" + " len="
					+ packetHeader.getPacketDataLength() + " version="
					+ packetHeader.getVersionNumber() + " type="
					+ packetHeader.getPacketType();
			logMsgType = LogMessageType.INVALID_PKT_DATA;
		}

		if (!packetHeader.isValid()) {
			errMsg = "Packet abandoned because header was corrupt " + "apid="
					+ packetHeader.getApid() + " len="
					+ packetHeader.getPacketDataLength();
			logMsgType = LogMessageType.INVALID_PKT_HEADER;
		}

		if (errMsg != null) {
			throw new RawInputException(errMsg, logMsgType);
		}

		return packetHeader;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getPacketInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryPacketInfo getPacketInfo(final IRawDataMessage message)
			throws RawInputException {
		final RawDatabaseMessage rdm = getRawDatabaseMessage(message);
        final IDbPacketProvider dp = getDatabasePacket(rdm.getDbRawData());

		final ISpacePacketHeader packetHeader = getPacketHeader(message);
		final ITelemetryPacketInfo packetInfo = pktInfoFactory.create(packetHeader, dp.getRecordLength());

        packetInfo.setFromSse(sseFlag.isApplicationSse());
		packetInfo.setErt(dp.getErt());
		packetInfo.setSclk(dp.getSclk());
		packetInfo.setSecondaryHeaderLength(dp.getSclk().getByteLength());

		final IAccurateDateTime scet = dp.getScet();

		packetInfo.setScet(scet);

		packetInfo.setVcid(dp.getVcid() == null ? 0 : dp.getVcid());

		if (dp.getVcfcs() != null && dp.getVcfcs().size() > 0) {
			packetInfo.addSourceVcfc(dp.getVcfcs().get(0));
		}

		if (setSolTimes)
		{
			packetInfo.setLst(LocalSolarTimeFactory.getNewLst(scet, scid));
		}

		return packetInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getSourceFrameInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameInfo getSourceFrameInfo(final IRawDataMessage message)
			throws RawInputException {
		final RawDatabaseMessage rdm = getRawDatabaseMessage(message);
        final IDbPacketProvider dp = getDatabasePacket(rdm.getDbRawData());

		/* 
		 * MPCS-3923 - 11/3/14. Removed setting of frame version.
		 * It's a packet. We do not know the frame version. And there is
		 * no need to set header fields to 0. They will default to 0.
		 * Use factory method to create the IFrameInfo.
		 */

		List<Long> vcfcs = dp.getVcfcs();
		if (vcfcs != null && vcfcs.isEmpty()) {
			vcfcs = null;
		}
		final ITelemetryFrameInfo tfInfo = frameInfoFactory.create(scid, dp.getVcid() == null ? 0 : dp.getVcid(), 
				vcfcs == null ? 0 : vcfcs.get(0).intValue());

		return tfInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getFrameInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameInfo getFrameInfo(final IRawDataMessage message)
			throws RawInputException {
		final RawDatabaseMessage rdm = getRawDatabaseMessage(message);
        final IDbFrameProvider df = getDatabaseFrame(rdm.getDbRawData());

		final ITransferFrameDefinition tff = getTransferFrameFormat(message);

		/* 
		 * MPCS-7993 - 3/31/16. Removed handling for actual ASM size vs
		 * ASM size.  The frame fetch feeding this helper no longer re-attaches
		 * ASM to frames without it, and the dictionary no longer contains two
		 * ASM sizes.
		 */

		final ITelemetryFrameHeader fh = getFrameHeader(message);

		/*
		 * MPCS-3923 - 11/3/14. Use new factory method to create the FrameInfo object
		 * and set the header and format.
		 */
		final ITelemetryFrameInfo tfInfo = frameInfoFactory.create(fh, tff);

        /* 
         * MPCS-7993 - 3/31/16. Removed adjustment of frame size to account
         * for re-attached ASM. The frame info object now knows the proper frame
         * size from the dictionary.
         */
		
		tfInfo.setBad(df.getIsBad());
		tfInfo.setBadReason(df.getBadReason());

		return tfInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getFrameHeader(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameHeader getFrameHeader(final IRawDataMessage message)
			throws RawInputException {
	    
	    final ITransferFrameDefinition tff = getTransferFrameFormat(message);
		
		/* 
         * MPCS-7993 - 3/31/16. Removed handling for actual ASM size vs
         * ASM size.  The frame fetch feeding this helper no longer re-attaches
         * ASM to frames without it, and the dictionary no longer contains two
         * ASM sizes.
         */
		final ITelemetryFrameHeader fh = TelemetryFrameHeaderFactory.create(missionProps, tff);

		fh.load(message.getData(), tff.getASMSizeBytes());

		return fh;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getTransferFrameFormat(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITransferFrameDefinition getTransferFrameFormat(final IRawDataMessage message)
			throws RawInputException {
		final RawDatabaseMessage rdm = getRawDatabaseMessage(message);
        final IDbFrameProvider df = getDatabaseFrame(rdm.getDbRawData());
		
		/* 
		 * MPCS-7993 - 3/31/16. Removed mapping of frame type to
		 * _WITH_ASM frame type. The query for the frames no longer re-attaches 
		 * ASM.
		 */

		return this.frameDict.findFrameDefinition(df.getType());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getNumFrameBytes(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public int getNumFrameBytes(final IRawDataMessage message)
			throws RawInputException {
		final RawDatabaseMessage rdm = getRawDatabaseMessage(message);
        final IDbFrameProvider df = getDatabaseFrame(rdm.getDbRawData());
		return df.getRecordLength();
	}

	private RawDatabaseMessage getRawDatabaseMessage(final IRawDataMessage message)
			throws RawInputException {
		RawDatabaseMessage rdm = null;
		if (message instanceof RawDatabaseMessage) {
			rdm = (RawDatabaseMessage) message;
		} else {
			throw new RawInputException("Error creating TransferFrameDefinition. Need RawDatabaseMessage, got "
					+ message.getClass().getName()
					+ ". Perhaps there is a subscription discrepancy.");
		}

		return rdm;
	}

    private IDbFrameProvider getDatabaseFrame(final IDbRawData drd)
			throws RawInputException {
        IDbFrameProvider df = null;

        if (drd instanceof IDbFrameProvider) {
            df = (IDbFrameProvider) drd;
		} else {
			throw new RawInputException("Error creating TransferFrameDefinition. RawDatabaseMessage does not contain DatabaseFrame, instead, it contains "
					+ drd.getClass().getName()
					+ ". Check your system configuration to ensure that the right types are configured.");
		}

		return df;
	}

    private IDbPacketProvider getDatabasePacket(final IDbRawData drd)
			throws RawInputException {
        IDbPacketProvider dp = null;

        if (drd instanceof IDbPacketProvider) {
            dp = (IDbPacketProvider) drd;
		} else {
            throw new RawInputException(
                    "Error creating TransferFrameDefinition. RawDatabaseMessage does not contain DatabaseRawData, instead, it contains "
					+ drd.getClass().getName()
					+ ". Check your system configuration to ensure that the right types are configured.");
		}

		return dp;
	}
}
