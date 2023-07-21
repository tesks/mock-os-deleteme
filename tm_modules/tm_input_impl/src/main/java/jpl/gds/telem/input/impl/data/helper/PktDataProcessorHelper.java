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
package jpl.gds.telem.input.impl.data.helper;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.data.helper.IPktProcessorHelper;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.impl.message.RawPacketMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;

/**
 * This is a helper adapter class that is used by <code>IRawDataProcessor</code>
 * classes to process raw packet data.
 * 
 *
 */
public class PktDataProcessorHelper implements IPktProcessorHelper {
	private final Tracer logger ;

	private boolean sclkScetWarningIssued = false;
	private final boolean setSolTimes;
	private final int scid;

	private final ISecondaryPacketHeaderLookup secHeaderLookup;
	private final ITelemetryPacketInfoFactory pktInfoFactory;
    private final ITelemetryFrameInfoFactory frameInfoFactory;

    private final SseContextFlag               sseFlag;

	/**
	 * Constructor.
	 * @param serviceContext the current application context
	 */
	public PktDataProcessorHelper(final ApplicationContext serviceContext) {
        logger = TraceManager.getDefaultTracer(serviceContext);
		setSolTimes = serviceContext.getBean(EnableLstContextFlag.class).isLstEnabled();
        scid = serviceContext.getBean(IContextIdentification.class).getSpacecraftId();
        secHeaderLookup = serviceContext.getBean(ISecondaryPacketHeaderLookup.class);
        pktInfoFactory = serviceContext.getBean(ITelemetryPacketInfoFactory.class);
        frameInfoFactory = serviceContext.getBean(ITelemetryFrameInfoFactory.class);
        sseFlag = serviceContext.getBean(SseContextFlag.class);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IDataProcessorHelper#init(jpl.gds.common.config.types.TelemetryInputType)
	 */
	@Override
	public void init(final TelemetryInputType inputType) throws RawInputException {
		// dp nothing
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getPacketHeader(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ISpacePacketHeader getPacketHeader(final IRawDataMessage message)
			throws RawInputException {
		return checkAndCast(message).getPacketHeader();
	}
	
	private RawPacketMessage checkAndCast(final IRawDataMessage message) throws RawInputException {
		RawPacketMessage pktMsg = null;

		if (message instanceof RawPacketMessage) {
			pktMsg = (RawPacketMessage) message;
		} else {
			throw new RawInputException("Error creating ISpacePacketHeader. Need RawPktMessage, got "
					+ message.getClass().getName()
					+ ". Perhaps there is a subscription discrepancy.");
		}
		return pktMsg;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getSourceFrameInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameInfo getSourceFrameInfo(final IRawDataMessage message)
			throws RawInputException {

		/* 
		 * MPCS-3923 - 11/3/14. Removed setting of frame version.
		 * It's a packet. We do not know the frame version. And there
		 * is no need to set IFrameInfo fields to 0. They will default
		 * to 0.
		 */

		final ITelemetryFrameInfo tfInfo = frameInfoFactory.create();
		tfInfo.setScid(scid);

		return tfInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getPacketInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryPacketInfo getPacketInfo(final IRawDataMessage message)
	        throws RawInputException {
	    final RawInputMetadata metadata = message.getMetadata();

	    final ISpacePacketHeader pktHeader = getPacketHeader(message);
	    final ISecondaryPacketHeader secHeader = secHeaderLookup
	            .lookupExtractor(pktHeader)
	            .extract(message.getData(), pktHeader.getPrimaryHeaderLength());
	    final ITelemetryFrameInfo tfInfo = getSourceFrameInfo(null);

		final ITelemetryPacketInfo packetInfo = pktInfoFactory.create(pktHeader, metadata.getDataLength(), secHeader);

        packetInfo.setFromSse(sseFlag.isApplicationSse());

		packetInfo.setErt(metadata.getErt());

		/** MPCS-5190 08/20/13 */
		packetInfo.setDssId(metadata.getDsnInfo().getDssId());

		// do sclk/scet conversion
        final ISclk sclk = packetInfo.getSclk();
		IAccurateDateTime scet = SclkScetUtility.getScet(sclk, metadata.getErt(), tfInfo.getScid());
		if (scet == null) {
			if (!this.sclkScetWarningIssued) {
                logger.warn(Markers.TIME_CORR, "Could not find SCLK/SCET correlation file for spacecraft ID "
                        + tfInfo.getScid() + ".  Packet SCET values are set to the beginning of the epoch.");
				this.sclkScetWarningIssued = true;
			}
			scet = new AccurateDateTime(0);
		}
		packetInfo.setScet(scet);

		if (setSolTimes) {
			packetInfo.setLst(LocalSolarTimeFactory.getNewLst(scet, tfInfo.getScid()));
		}

		/* MPCS-7289 - 4/30/15.  Set new fields in packet info */
		packetInfo.setBitRate(metadata.getBitRate());
		packetInfo.setRelayScid(metadata.getDsnInfo().getRelayScid());
		return packetInfo;
	}

}
