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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.message.RawPacketMessage;


/**
 * This class is an implementation of <code>IRawStreamProcessor</code> that
 * processes packet streams
 * 
 *
 */
public class PacketStreamProcessor extends AbstractRawStreamProcessor {
	private static final double DEF_BIT_RATE = 10000.0;
	private ISpacePacketHeader iPacketHeader;
	private ISecondaryPacketHeader secHeader;
	private int entirePacketLength;
	private IAccurateDateTime nominalErt;
	private RawInputMetadata metadataClone;
    // MPCS-5013 07/30/13 Remove bitRate

	/*
	 * This boolean is a flag to keep track of which parts of the current
	 * packet have been successfully read
	 */
	private boolean headerRead;

    private final ISecondaryPacketHeaderLookup secondaryPacketHeaderLookup;
	private final int primaryHeaderLength;
	private final CcsdsProperties packetProps;
    private final IStatusMessageFactory statusMsgFactory;

	
	/**
	 * Constructor
	 * @param serviceContext the current application context
	 */
	public PacketStreamProcessor(final ApplicationContext serviceContext) {
		super(serviceContext);

		this.headerRead = false;
		packetProps = serviceContext.getBean(CcsdsProperties.class);
		final ISpacePacketHeader header = PacketHeaderFactory.create(packetProps.getPacketHeaderFormat());
		primaryHeaderLength = header.getPrimaryHeaderLength();
		secondaryPacketHeaderLookup = serviceContext.getBean(ISecondaryPacketHeaderLookup.class);
	    statusMsgFactory = serviceContext.getBean(IStatusMessageFactory.class);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#processRawData(jpl.gds.telem.input.api.stream.IRawInputStream, jpl.gds.telem.input.api.message.RawInputMetadata)
	 */
	@Override
	public void processRawData(final IRawInputStream inputStream,
	        final RawInputMetadata metadata) throws RawInputException, IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("Null data input source");
		}

		final DataInputStream dis = inputStream.getDataInputStream();

		int len = 0;
		final byte[] headerBuffer = new byte[primaryHeaderLength];
		byte[] packetBuffer = null;
        // MPCS-5013 07/30/13 Made a local variable
		double bitRate = DEF_BIT_RATE;

		final Long startTime = System.currentTimeMillis();

		while (!isStopped()) {
			len = 0;
			int tmp = 0;

			if (!this.headerRead) {
				try {
					this.metadataClone = metadata.clone();
				} catch (final CloneNotSupportedException e) {
					this.metadataClone = new RawInputMetadata();
					logger.error("Cannot clone RawInputMetadata object.", e);
				}

				// read the packet header into a buffer
				len = 0;
				while (len < primaryHeaderLength) {
                    // MPCS-5013 07/29/13
                    setEofOnStreamStatus(false);

					tmp = dis.read(headerBuffer, len, primaryHeaderLength
					        - len);
					if (tmp < 0) {
                        // MPCS-5013 07/29/13 Needed for client socket input

                        // End-of-file

                        setEofOnStreamStatus(true);

                        // MPCS-5013 07/29/13 Needed for client socket input
                        // Used to break if len >0; made no sense; deleted

						setAwaitingFirstData(true);

						if (this.stopped) {
							return;
						}

						// MPCS-4632 10/13/16  - Changed to EOFException to properly identify it
						throw new EOFException("Error reading buffer");
					}

					len += tmp;
				}

				messenger.incrementReadCount();
				final long endTime = System.currentTimeMillis();
				nominalErt = new AccurateDateTime();
				
				if(metadataClone.getErt() != null) {
					nominalErt = metadataClone.getErt();
				} else {
					metadataClone.setErt(nominalErt);
				}
				
				if(metadataClone.getBitRate() != null) {
					bitRate = metadataClone.getBitRate();
				} else {
					// if this is not true, assume the bit rate hasn't changed
					if (endTime > startTime) {
						bitRate = (len * 8) / ((endTime - startTime) / 1000.0);
					}
					metadataClone.setBitRate(bitRate);
				}

				// create and populate a packet header object
				iPacketHeader = PacketHeaderFactory.create(packetProps.getPacketHeaderFormat());
				iPacketHeader.setPrimaryValuesFromBytes(headerBuffer, 0);

				if (!iPacketHeader.isValid()) {
					final IPublishableLogMessage logm = statusMsgFactory
                            .createPublishableLogMessage(TraceSeverity.WARNING,
                                    "Packet abandoned because header was corrupt "
                                            + "apid="
                                            + iPacketHeader.getApid()
                                            + " isValid=false"
                                            + " len="
                                            + iPacketHeader.getPacketDataLength()
                                            + " version="
                                            + iPacketHeader.getVersionNumber()
                                            + " type="
                                            + iPacketHeader.getPacketType(), LogMessageType.INVALID_PKT_HEADER);

					this.context.publish(logm);
                    logger.warn(logm);
					continue;
				}

				// fill in the entire byte representation of the packet
				entirePacketLength = primaryHeaderLength
				        + iPacketHeader.getPacketDataLength() + 1;
				packetBuffer = new byte[entirePacketLength];
				final ISecondaryPacketHeaderExtractor extractor = secondaryPacketHeaderLookup.lookupExtractor(iPacketHeader);
				if (!extractor.hasEnoughBytes(packetBuffer, iPacketHeader.getPrimaryHeaderLength())) {
				    final IPublishableLogMessage logm = statusMsgFactory
                            .createPublishableLogMessage(TraceSeverity.WARNING,
                                    "Packet abandoned because it is too short "
                                            + "apid="
                                            + iPacketHeader.getApid()
                                            + " isValid=false"
                                            + " len="
                                            + iPacketHeader.getPacketDataLength()
                                            + " version="
                                            + iPacketHeader.getVersionNumber()
                                            + " type=" + iPacketHeader.getPacketType(), LogMessageType.INVALID_PKT_DATA);

					context.publish(logm);
                    logger.warn(logm);
					continue;
				}

				
				secHeader = extractor.extract(packetBuffer, iPacketHeader.getPrimaryHeaderLength());

				System.arraycopy(headerBuffer, 0, packetBuffer, 0, primaryHeaderLength);

				this.headerRead = true;
			}

			len = 0;
			int offset = primaryHeaderLength;
			int tmplen = 0;
			while (len < (iPacketHeader.getPacketDataLength() + 1))
            {
                // MPCS-5013 07/29/13
                setEofOnStreamStatus(false);

				tmplen = dis.read(packetBuffer, offset, iPacketHeader.getPacketDataLength()
				        + 1 - len);
				if (tmplen < 0)
                {
                    // MPCS-5013 07/29/13 Needed for client socket input

                    // End-of-file

                    setEofOnStreamStatus(true);

					setAwaitingFirstData(true);

					if (this.stopped) {
						return;
					}

                    // MPCS-5013 07/29/13
                    // We have an EOF, must resync header.
                    // Causes NPE with null packetBuffer without it.
                    headerRead = false;

                    // MPCS-4632 10/13/16  - Changed to EOFException to properly identify it
					throw new EOFException("Error reading buffer");
				}

				len += tmplen;
				offset += tmplen;
			}

            // MPCS-5013 07/03/13 Use configured station
            final IStationTelemInfo dsnInfo = stationInfoFactory.create(bitRate,
                                                entirePacketLength * 8,
                                                nominalErt,
                                                getConfiguredStation());

			this.metadataClone.setDsnInfo(dsnInfo);

			if (!isPaused()) {

				if (awaitingFirstData()) {
					messenger.sendStartOfDataMessage();
					setAwaitingFirstData(false);
				}

				this.metadataClone.setDataLength(entirePacketLength);
                final RawPacketMessage rpm =
                    new RawPacketMessage(this.metadataClone,
                                         headerBuffer,
                                         packetBuffer,
                                         HeaderHolder.NULL_HOLDER,
                                         TrailerHolder.NULL_HOLDER);
				rpm.setPacketHeader(iPacketHeader);
				rpm.setSecondaryHeader(secHeader);
				this.context.publish(rpm);
			} else {
				this.bytesDiscarded += packetBuffer.length;

				/*
				 * If stopping, don't suppress "bytes discarded" message until
				 * threshold is reached, since it could stop before we reach it.
				 * But otherwise, only send message when threshold is reached,
				 * so the message bus doesn't get overloaded.
				 */
				if (isStopped()
				        || this.bytesDiscarded > bytesDiscardedWhilePausedThreshold) {
					logger.warn("Processing of raw input is paused: "
					        + this.bytesDiscarded + " bytes discarded.");
					this.bytesDiscarded = 0;
				}
			}

			// reset read flags
			this.headerRead = false;
			doMetering();
		}
	}

}
