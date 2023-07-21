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
import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.ccsds.api.tm.frame.TelemetryFrameHeaderFactory;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.data.helper.IPktProcessorHelper;
import jpl.gds.telem.input.api.data.helper.ITfProcessorHelper;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.impl.UnknownTransferFrameException;
import jpl.gds.telem.input.impl.message.RawSfduPktMessage;
import jpl.gds.telem.input.impl.message.RawSfduTfMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;

/**
 * This is a helper adapter class that is used by <code>IRawDataProcessor</code>
 * classes to process raw SFDU data.
 * 
 *
 */
public class SfduDataProcessorHelper implements ITfProcessorHelper,
IPktProcessorHelper {

	private final ITransferFrameDefinitionProvider frameDict;
	private final boolean setSolTimes;
	private final int scid;
	private final ITelemetryPacketInfoFactory pktInfoFactory;
	private final MissionProperties missionProps;
    private final ITelemetryFrameInfoFactory frameInfoFactory;

    private final SseContextFlag                   sseFlag;
	
	/**
	 * MPCS-7014 DE 8/27/2015 - add tracer to warn user of fallback to byte length
	 * 
	 * Instantiate the tracer so it can be used anywhere in this class. 
	 */
	private final Tracer	log; 


	/**
	 * Constructor
	 * 
	 * @param serviceContext the current application context
	 */
	public SfduDataProcessorHelper(final ApplicationContext serviceContext) {
        log = TraceManager.getDefaultTracer(serviceContext);
		setSolTimes = serviceContext.getBean(EnableLstContextFlag.class).isLstEnabled();
		frameDict = serviceContext.getBean(ITransferFrameDefinitionProvider.class);
		scid = serviceContext.getBean(IContextIdentification.class).getSpacecraftId();		
		pktInfoFactory = serviceContext.getBean(ITelemetryPacketInfoFactory.class);
        missionProps = serviceContext.getBean(MissionProperties.class);
        frameInfoFactory = serviceContext.getBean(ITelemetryFrameInfoFactory.class);
        sseFlag = serviceContext.getBean(SseContextFlag.class);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IDataProcessorHelper#init(jpl.gds.common.config.types.TelemetryInputType)
	 */
	@Override
	public void init(final TelemetryInputType inputType) throws RawInputException {
       // do nothing
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getTransferFrameFormat(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITransferFrameDefinition getTransferFrameFormat(final IRawDataMessage message)
			throws RawInputException {
		final RawInputMetadata metadata = message.getMetadata();
		final int dataLen = metadata.getDataLength();
		/**
		 * MPCS-7014 DE 8/27/2015 added bitSize variable for finding the proper frame
		 * 
		 * the bit size value, defined in CHDO 069, is now utilized instead of dataLength
		 *  and the now defunct isPadded and.
		 */
		final int bitSize = metadata.getBitSize();


		/**
		 * MPCS-7014 DE 8/27/2015 removed dataLen adjustment based on isPadded
		 * 
		 * isPadded is defunct. While the dataLen could be removed, it is kept in case
		 * there is an issue with the bit length value.
		 */

		ITransferFrameDefinition tff = null;

		/**
		 * MPCS-7014 DE 8/27/2015 find frame format/definition based upon 
		 * 
		 * Find the proper frame definition based upon the number of bits
		 * in the frame as per the bit_size field of the CHDO.
		 */
		
		if(bitSize > 0) {
			if (metadata.isTurbo()) {
			 // if this is true, the data area of the CHDO was padded with an
		        // extra byte to make an even number of bytes

				final String rate = metadata.getTurboEncodingRate();
	
				tff = this.frameDict.findFrameDefinition(bitSize, rate);
			} else {
				tff = this.frameDict.findFrameDefinition(bitSize);
			}
		}
		else {
			//MPCS-7664 DE 9/3/2015 - changed warning message to debug message
			log.debug(String.format("Invalid bit size received, using data length to create packet: %d", dataLen));
			if (metadata.isTurbo()) {
				final String rate = metadata.getTurboEncodingRate();
	
				tff = this.frameDict.findFrameDefinition( (dataLen*8), rate);
			} else {
				tff = this.frameDict.findFrameDefinition( (dataLen*8) );
			}
		}

		if (tff == null) {
			throw new UnknownTransferFrameException("Unrecognized transfer frame size: "
					+ metadata.getDataLength());
		} else {
			return tff;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getFrameInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameInfo getFrameInfo(final IRawDataMessage message)
			throws RawInputException {
		final RawSfduTfMessage sfduMsg = getRawSfduTfMessage(message);

		final ITransferFrameDefinition tff = getTransferFrameFormat(message);

		final ITelemetryFrameHeader fh = getFrameHeader(message);

		/*
		 * MPCS-3923 - 11/3/14. Use new factory method to create the FrameInfo object
		 * and set the header and format.
		 */

		final ITelemetryFrameInfo tfInfo = frameInfoFactory.create(fh, tff);

		/* MPCS-7993 - 3/30/16.  Removed override of header and
		 * packet store size in the frame info object to account for
		 * frames without ASM. The frame info object now knows this from
		 * the dictionary and will set these values accordingly.
		 */
	
		/* Take into account both frame header and SFDU idle flags. */		
		tfInfo.setIdle(fh.isIdle() || sfduMsg.isIdle());

		return tfInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getFrameHeader(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameHeader getFrameHeader(final IRawDataMessage message)
			throws RawInputException {
		final byte[] data = message.getData();

		final ITransferFrameDefinition tff = getTransferFrameFormat(message);

		final ITelemetryFrameHeader fh = TelemetryFrameHeaderFactory.create(missionProps, tff);
		/*
		 * MPCS-7993 - 3/30/16. If frame arrives without ASM, then
		 * loading of the header must start at data offset 0, now that there are
		 * not two ASM sizes in the dictionary.
		 */
		fh.load(data, tff.arrivesWithASM() ? tff.getASMSizeBytes() : 0);

		return fh;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getPacketHeader(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ISpacePacketHeader getPacketHeader(final IRawDataMessage message)
			throws RawInputException {
		final RawSfduPktMessage sfduMsg = getRawSfduPktMessage(message);

		return sfduMsg.getPacketHeader();
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getPacketInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryPacketInfo getPacketInfo(final IRawDataMessage message)
			throws RawInputException {
		final RawSfduPktMessage sfduMsg = getRawSfduPktMessage(message);

		final RawInputMetadata metadata = message.getMetadata();
		final ISpacePacketHeader pktHeader = getPacketHeader(message);
		final ITelemetryFrameInfo tfInfo = getSourceFrameInfo(message);
		final IAccurateDateTime scet = sfduMsg.getScet();

		/**
         * MPCS-7674 DE 9/11/2015 - isPadded has been returned for SFDU packets but not frames (removed by MPCS-7014).
         * Is padded is a valid value for an SFDU packet CHDO, which does not have a bit size.
         * Use the received packet length as the data length and subtract one byte if the CHDO states it is padded.
         */
		int dataLength = message.getData().length;
		
		// if this is true, the data area of the CHDO was padded with an
		// extra byte to make an even number of bytes
		if (metadata.isPadded()) {
			dataLength--;
		}
		
		/* MPCS-7289 - 4/30/15.  Use factory method that takes packet header. */
		final ITelemetryPacketInfo pktInfo = pktInfoFactory.create(pktHeader, dataLength);
        pktInfo.setFromSse(sseFlag.isApplicationSse());
		pktInfo.setSclk(sfduMsg.getSclk());
		pktInfo.setSecondaryHeaderLength(sfduMsg.getSecondaryHeaderLen());
		pktInfo.setErt(metadata.getErt());
		pktInfo.setScet(scet);
		pktInfo.setVcid(tfInfo.getVcid());
		pktInfo.setDssId(metadata.getDsnInfo().getDssId());
		pktInfo.addSourceVcfc(tfInfo.getSeqCount());

		if (setSolTimes) {
			pktInfo.setLst(LocalSolarTimeFactory.getNewLst(scet, scid));
		}

		return pktInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.IPktProcessorHelper#getSourceFrameInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameInfo getSourceFrameInfo(final IRawDataMessage message)
			throws RawInputException {
		final RawSfduPktMessage sfduMsg = getRawSfduPktMessage(message);

		/*
		 * MPCS-3923 - 11/3/14. We have an SFDU packet. There were a bunch
		 * of statements setting fields in the IFrameInfo object. First of all,
		 * there is no need to set IFrameInfo fields to 0. They default to 0, so
		 * I removed calls that set things to 0. As for the other set calls,
		 * they all set something into the FrameInfo that is really NOT KNOWN
		 * here. For instance, the frame size was being set to the size of this
		 * SFDU. Why? It's a packet.
		 */
		return frameInfoFactory.create(sfduMsg.getScid(), sfduMsg.getVcid(), sfduMsg.getVfc());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getNumFrameBytes(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public int getNumFrameBytes(final IRawDataMessage message)
			throws RawInputException {
		final ITransferFrameDefinition tff = getTransferFrameFormat(message);
		return tff.getCADUSizeBytes();
	}

	private RawSfduTfMessage getRawSfduTfMessage(final IRawDataMessage message)
			throws RawInputException {
		RawSfduTfMessage sfduMsg = null;

		if (message instanceof RawSfduTfMessage) {
			sfduMsg = (RawSfduTfMessage) message;
		} else {
			throw new RawInputException("Error creating IFrameINfo. Need RawSfduMessage, got "
					+ message.getClass().getName()
					+ ". Perhaps there is a subscription discrepancy.");
		}

		return sfduMsg;
	}

	private RawSfduPktMessage getRawSfduPktMessage(final IRawDataMessage message)
			throws RawInputException {
		RawSfduPktMessage sfduMsg = null;

		if (message instanceof RawSfduPktMessage) {
			sfduMsg = (RawSfduPktMessage) message;
		} else {
			throw new RawInputException("Error checking frame validity. Need RawSfduMessage, got "
					+ message.getClass().getName()
					+ ". Perhaps there is a subscription discrepancy.");
		}

		return sfduMsg;
	}
}
