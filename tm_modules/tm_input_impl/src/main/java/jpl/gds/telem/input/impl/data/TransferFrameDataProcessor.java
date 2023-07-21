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
package jpl.gds.telem.input.impl.data;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.data.ITransferFrameDataProcessor;
import jpl.gds.telem.input.api.data.helper.IDataProcessorHelper;
import jpl.gds.telem.input.api.data.helper.ITfProcessorHelper;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.impl.message.RawSfduTfMessage;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;

/**
 * This is an implementation of <code>IRawDataProcessor</code> that processes
 * raw transfer frame data
 * 
 *
 */
public class TransferFrameDataProcessor extends AbstractRawDataProcessor implements ITransferFrameDataProcessor {

	private static final String ME = "TransferFrameDataProcessor: ";
	private ITelemetryFrameInfo lastTfInfo = null;
	private IAccurateDateTime lastFrameErt = null;

	private List<Integer> vcids;
	private ITfProcessorHelper helper;
    private final IFrameMessageFactory frameMsgFactory;
    private final IStatusMessageFactory statusMsgFactory;
    private final Tracer                      frameLog;

	/**
	 * Constructor.
	 * 
	 * @param serviceContext the current application context
	 */
	public TransferFrameDataProcessor(final ApplicationContext serviceContext) {
		super(serviceContext);
		
		this.frameMsgFactory = serviceContext.getBean(IFrameMessageFactory.class);
		this.statusMsgFactory = serviceContext.getBean(IStatusMessageFactory.class);
        this.frameLog = TraceManager.getTracer(serviceContext, Loggers.FRAME_SYNC);
		 
		final MissionProperties missionProps = serviceContext.getBean(MissionProperties.class);
		vcids = missionProps.getAllDownlinkVcids();
		if (vcids == null) {
			vcids = new ArrayList<Integer>(1);
			vcids.add(0);
		} 
        synchronized (TransferFrameDataProcessor.class)
        {
            lastTfInfo   = null;
            lastFrameErt = null;
        }
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.data.AbstractRawDataProcessor#init(jpl.gds.telem.input.api.data.helper.IDataProcessorHelper, jpl.gds.telem.input.api.config.StreamType, jpl.gds.common.config.types.TelemetryConnectionType)
	 */
	@Override
	public void init(final IDataProcessorHelper helper, final StreamType streamType,
	        final TelemetryConnectionType connType) throws RawInputException {
		super.init(helper, streamType, connType);
		if (helper instanceof ITfProcessorHelper) {
			this.helper = (ITfProcessorHelper) helper;
		} else {
			throw new RawInputException("Invalid helper class assigned to data processor. Expecting ITfProcessorHelper, got "
			        + helper.getClass().getName());
		}
	}

	private void sendTransferFrameMessage(final ITelemetryFrameMessage tfm) {
		lastTfInfo = tfm.getFrameInfo();
		lastFrameErt = tfm.getStationInfo().getErt();
		context.publish(tfm);
        frameLog.debug(tfm.getOneLineSummary());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.BaseMessageHandler#handleMessage(jpl.gds.shared.message.IMessage)
	 */
	@Override
	public void handleMessage(final IMessage message) {
		if (this.helper == null) {
			logger.debug("No helper class set. Need to initialize TransferFrameDataProcessor with a helper class before it can process packet data");
			logger.error("Unable to process frame. Transfer Frame data processing was not properly initialized.");
			return;
		}

		if (message instanceof IRawDataMessage) {
			final IRawDataMessage msg = (IRawDataMessage) message;

			final RawInputMetadata metadata = msg.getMetadata();
			final byte[] data = msg.getData();

			final IStationTelemInfo dsnInfo = metadata.getDsnInfo();

			ITelemetryFrameInfo tfInfo = null;
			ITelemetryFrameHeader fh = null;
			try {
				tfInfo = helper.getFrameInfo(msg);
				fh = helper.getFrameHeader(msg);
			} catch (final RawInputException e) {
				final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(
                        TraceSeverity.ERROR, e.getMessage());
				this.context.publish(lm);
                logger.log(lm);
				return;
			}

			boolean isBad = metadata.isBad();
			boolean badVcid = false;

            // Get bad reason from metadata if it's bad
            final InvalidFrameCode metaBadReason =
                (isBad ? metadata.getBadFrameReason() : null);

			InvalidFrameCode badReason = (metaBadReason != null)
                                             ? metaBadReason
                                             : InvalidFrameCode.UNKNOWN;

			if (!isBad && !vcids.contains(fh.getVirtualChannelId())) {
				isBad = true;
				badReason = InvalidFrameCode.BAD_VCID;
				badVcid = true;
			}

			tfInfo.setBad(isBad);

			if (isBad) {
				tfInfo.setBadReason(badReason);
			}

			if (metadata.needsInSyncMessage()) {
				messenger.sendInSyncMessage(dsnInfo, tfInfo);
			}

			IChdoSfdu sfdu = null;
			
			if (message instanceof RawSfduTfMessage) {
			    sfdu = ((RawSfduTfMessage)message).getSfdu();
			}
			ITelemetryFrameMessage tfm = null;

			try {
				tfm =  frameMsgFactory.createTelemetryFrameMessage(dsnInfo, tfInfo, helper.getNumFrameBytes(msg), data, 0,
                                               msg.getRawHeader(),
                                               msg.getRawTrailer());
				tfm.setChdoObject(sfdu);
			} catch (final RawInputException e) {
				messenger.sendBadFrameMessage(dsnInfo, null, "Frame abandoned, unable to create transfer frame message: "
				        + e.getMessage());
                return;
			}

            logger.trace(ME + " publishing Transfer Frame Message " + tfm.getOneLineSummary());

			this.sendTransferFrameMessage(tfm);
			if (badVcid) {
				final IFrameEventMessage bfm = frameMsgFactory.createBadFrameMessage(dsnInfo, tfInfo);
				this.context.publish(bfm);
                frameLog.log(bfm);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.ITransferFrameDataProcessor#getLastTfInfo()
	 */
	@Override
	public ITelemetryFrameInfo getLastTfInfo() {
		return lastTfInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.ITransferFrameDataProcessor#getLastFrameErt()
	 */
	@Override
	public IAccurateDateTime getLastFrameErt() {
		return lastFrameErt;
	}
}
