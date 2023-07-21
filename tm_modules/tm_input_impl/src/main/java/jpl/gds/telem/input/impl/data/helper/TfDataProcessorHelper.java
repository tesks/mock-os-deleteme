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

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.ccsds.api.tm.frame.TelemetryFrameHeaderFactory;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.data.helper.ITfProcessorHelper;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;

/**
 * This is a helper adapter class that is used by <code>IRawDataProcessor</code>
 * classes to process raw transfer frame data.
 * 
 *
 */
public class TfDataProcessorHelper implements ITfProcessorHelper {
	private final IMessagePublicationBus context;
	private final ITransferFrameDefinitionProvider frameDict;
	private final MissionProperties missionProps;
    private final IStatusMessageFactory statusMessageFactory;
    private final ITelemetryFrameInfoFactory frameInfoFactory;
	
	
	/**
	 * Constructor.
	 * 
	 * @param serviceContext the current application context
	 */
	public TfDataProcessorHelper(final ApplicationContext serviceContext) {
        frameDict = serviceContext.getBean(ITransferFrameDefinitionProvider.class);
        context = serviceContext.getBean(IMessagePublicationBus.class);
        missionProps = serviceContext.getBean(MissionProperties.class);
        statusMessageFactory = serviceContext.getBean(IStatusMessageFactory.class);
        frameInfoFactory = serviceContext.getBean(ITelemetryFrameInfoFactory.class);
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getTransferFrameFormat(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITransferFrameDefinition getTransferFrameFormat(final IRawDataMessage message)
			throws RawInputException {

		final RawInputMetadata metadata = message.getMetadata();
		final int len = metadata.getDataLength();
		final String encoding = metadata.getTurboEncodingRate();
		final IAccurateDateTime nominalErt = metadata.getErt();

		/**
         * MPCS-7014 DE 8/31/2015 findFrameDefinition call updated
         * 
         * The findFrameDefinition function has been updated. It no longer take the "matchOdd" boolean argument and now
         * requires the length argument to be in number of bits.
         */
		
		final ITransferFrameDefinition tff = frameDict.findFrameDefinition( (len*8), encoding);
		if (tff == null) {
			final IPublishableLogMessage em = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                    "Unrecognized transfer frame size/encoding combination: "
                    		+ len + "/" + encoding + ", ERT=" + nominalErt);
			context.publish(em);
			throw new RawInputException("Unrecognized transfer frame size: "
					+ len);
		}

		return tff;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getFrameInfo(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public ITelemetryFrameInfo getFrameInfo(final IRawDataMessage message)
			throws RawInputException {
		final RawInputMetadata metadata = message.getMetadata();

		final ITelemetryFrameHeader fh = getFrameHeader(message);
		final ITransferFrameDefinition tff = getTransferFrameFormat(message);

		
        /* MPCS-7993 - 3/30/16.  Removed override of header and
         * packet store size in the frame info object to account for
         * frames without ASM. The frame info object now knows this from
         * the dictionary and will set these values accordingly.
         */
		final ITelemetryFrameInfo tfInfo = frameInfoFactory.create(fh, tff);

		/*
		 * 10/22/13 - MPCS-3740. Added check for frame header idle
		 * because only the frame header is set up to detect idleness
		 * based upon VC. 
		 */
		
		tfInfo.setIdle(fh.isIdle() || metadata.isIdle());

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

		final ITelemetryFrameHeader fh = TelemetryFrameHeaderFactory.create(missionProps, tff);
        /*
         * MPCS-7993 - 3/30/16. If frame arrives without ASM, then
         * loading of the header must start at data offset 0, now that there are
         * not two ASM sizes in the dictionary, and this handling no longer applies
         * to just frames that arrive as DSN SFDUs. 
         */		
		fh.load(message.getData(), tff.arrivesWithASM() ? tff.getASMSizeBytes() : 0);

		return fh;
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
	 * @see jpl.gds.telem.input.api.data.helper.ITfProcessorHelper#getNumFrameBytes(jpl.gds.telem.input.api.message.IRawDataMessage)
	 */
	@Override
	public int getNumFrameBytes(final IRawDataMessage message)
			throws RawInputException {
		final ITransferFrameDefinition tff = getTransferFrameFormat(message);
		return tff.getCADUSizeBytes();
	}
}
