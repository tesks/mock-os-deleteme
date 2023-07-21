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
package jpl.gds.telem.input.impl.data;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.IMessage;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.data.helper.IDataProcessorHelper;
import jpl.gds.telem.input.api.data.helper.IPktProcessorHelper;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.impl.message.RawSfduPktMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.packet.IPacketMessageFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This is an implementation of <code>IRawDataProcessor</code> for processing
 * raw packet data
 * 
 *
 */
public class PacketDataProcessor extends AbstractRawDataProcessor {

	private IPktProcessorHelper helper;
    private final IPacketMessageFactory packetMsgFactory;
    private final IStatusMessageFactory statusMsgFactory;
    
	
	/**
	 * Constructor.
	 * 
	 * @param serviceContext the current application context
	 */
	public PacketDataProcessor(final ApplicationContext serviceContext) {
	    super(serviceContext);
	    packetMsgFactory = serviceContext.getBean(IPacketMessageFactory.class);
	    statusMsgFactory = serviceContext.getBean(IStatusMessageFactory.class);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.impl.data.AbstractRawDataProcessor#init(jpl.gds.telem.input.api.data.helper.IDataProcessorHelper, jpl.gds.telem.input.api.config.StreamType, jpl.gds.common.config.types.TelemetryConnectionType)
	 */
	@Override
	public void init(final IDataProcessorHelper helper, final StreamType streamType,
	        final TelemetryConnectionType connType) throws RawInputException {
		super.init(helper, streamType, connType);
		if (helper instanceof IPktProcessorHelper) {
			this.helper = (IPktProcessorHelper) helper;
		} else {
			throw new RawInputException("Invalid helper class assigned to data processor. Expecting IPktProcessorHelper, got "
			        + helper.getClass().getName());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.BaseMessageHandler#handleMessage(jpl.gds.shared.message.IMessage)
	 */
	@Override
	public void handleMessage(final IMessage message) {
		if (this.helper == null) {
			logger.error("No helper class set. Need to initialize PacketDataProcessor with a helper class before it can process packet data");
			return;
		}

		if (message instanceof IRawDataMessage) {
			final IRawDataMessage msg = (IRawDataMessage) message;

			final RawInputMetadata metadata = msg.getMetadata();
			final byte[] data = msg.getData();

			ITelemetryFrameInfo tfInfo = null;
			try {
				tfInfo = helper.getSourceFrameInfo(msg);
			} catch (final RawInputException e) {
				logger.error("Unable to get IFrameInfo object", e);
				return;
			}

			ITelemetryPacketInfo pktInfo = null;
			try {
				pktInfo = helper.getPacketInfo(msg);
			} catch (final RawInputException e) {
				logger.error("Unable to get IPacketInfo object", e);
				if (e.getLogMessageType() != null) {
					final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(
                            TraceSeverity.WARNING, e.getMessage(), e.getLogMessageType());
					context.publish(logm);
                    // logger.error(logm);
				}
				return;
			}

			/**
             * MPCS-7674 DE 9/11/2015 - isPadded has been returned for SFDU packets but not frames (removed by MPCS-7014).
             * Is padded is a valid value for an SFDU packet CHDO, use it to determine the actual packet size.
             */
			final int pktLen = metadata.isPadded() ? data.length - 1 : data.length;

            PacketIdHolder packetId = null;

            HeaderHolder  header  = HeaderHolder.NULL_HOLDER;
            TrailerHolder trailer = TrailerHolder.NULL_HOLDER;
            
            IChdoSfdu sfdu = null;

            if (message instanceof RawSfduPktMessage)
            {
                final RawSfduPktMessage rspm =
                    (RawSfduPktMessage) message;
                
                sfdu = rspm.getSfdu();

                header  = rspm.getRawHeader();
                trailer = rspm.getRawTrailer();

                // In this case, may be previously calculated

                packetId = rspm.getPacketId();
            }

            /*
             * MPCS-7289 - 4/30/15. Set new packet info fields from frame
             * info and use factory to create the packet message.
             */
            if (tfInfo.getFrameFormat() != null) {
                pktInfo.setFrameType(tfInfo.getFrameFormat().getName());
                pktInfo.addSourceVcfc(tfInfo.getSeqCount());
            }
            pktInfo.setScid(tfInfo.getScid());

            /** MPCS-5932 09/10/15 Add frame id as NULL */
            final ITelemetryPacketMessage pm = packetMsgFactory.createTelemetryPacketMessage(
                    pktInfo,
                    packetId,
                    header,
                    trailer,
                    FrameIdHolder.UNSUPPORTED);
			pm.setPacket(data, 0, pktLen);
			pm.setChdoObject(sfdu);
			this.context.publish(pm);
		}
	}

}
