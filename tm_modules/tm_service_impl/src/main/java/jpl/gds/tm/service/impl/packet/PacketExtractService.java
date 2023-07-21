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
package jpl.gds.tm.service.impl.packet;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IFrameSequenceAnomalyMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import jpl.gds.tm.service.api.packet.IPacketExtractService;
import jpl.gds.tm.service.api.packet.IPacketMessageFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * A telemetry packet extraction service, which takes in telemetry frames and 
 * extracts space packets. Replies upon the CCSDS standard frame headers 
 * and behaviors for inclusions of space packets in frames.
 *
 * Packet extract builds packets from CCSDS transfer frames. It subscribes to
 * transfer frame messages and publishes packet message. As a side effect it
 * detects transfer frame gaps.
 *
 * 10/26/2011  - MPCS-2594:  FPP Handling
 * Added proper verification and processing of FPP, Idle Frames, and Frame Continuations.
 * Fixed some ancillary bugs that contributed to occasional non-deterministic handling of TF --> Packet Extraction.
 *
 * 10/28/2011 - MPCS-2523 ERT Packet Tagging was being taken from last TF rather than first TF for packets
 * that span multiple TFs. Now being taken correctly from first TF.
 *
 */
public class PacketExtractService implements IPacketExtractService {
/**
	 * Loggers. Use pktExLog for FPP processing specific messages. Use pktExLog for general PacketExtract messages.
	 */
	private final Tracer	pktExLog;


	/**
	 * Maximum size of a packet
	 */
	private static final int	MAX_PACKET_LENGTH					= ISpacePacketHeader.MAX_PACKET;

	/* MPCS-7993 - 3/30/16. Removed constants for IDLE frame and CONTINUATION
	 * indicators. These may differ by frame type and are now fetched 
	 * from the frame header object.
	 */

	/**
	 * simple state machine for packet extraction
	 * 
	 * state machine states
	 */
	private State				machineState						= State.FIRST_PACKET;

	private enum State {
		FIRST_PACKET,	// find packet from first packet pointer
		NEED_HEADER,	// need remaining packet header         
		NEED_DATA,		// need remaining data                  
		NEXT_PACKET		// get next packet in transfer frame    
	}

	private boolean					sclkScetWarningIssued	= false;

	/**
	 * Internal message destination
	 */
	private final IMessagePublicationBus	bus;

	/**
	 * statistics
	 */
	private long					numberOfTFs				= 0;
	private int						numberOfTFGaps			= 0;
	private int						numberOfTFRegressions	= 0;
	private int						numberOfTFRepeats		= 0;
	private int						numberOfTFBad			= 0;
	private long					numberOfPackets			= 0;
	private long					numberOfPacketsIdle		= 0;
	private long					numberOfPacketsInvalid	= 0;
	private int						allowedVcid				= 0;
	private ISclk					lastValidSclk			= new Sclk(0, 0);
	private final List<Long>		sourceVcfcs				= new ArrayList<Long>();
	private int						sourceVcid				= -1;
	private int						sourceDssId				= 0;

	/**
	 * info on current TF
	 */
	private ITelemetryFrameMessage currentTFMessage = null;

	/**
	 * packet extract
	 */

	/**
	 * Offset into TF of first byte past TF header
	 */
	private int						offsetTFBody			= 0;

	/**
	 * Current read offset into current Transfer Frame
	 */
	private int						offsetCurrentTFRead		= 0;

	/**
	 * Virtual Frame Counter
	 */
	private int						counterVirtualFrame		= -1;

	/**
	 * First Packet Pointer (FPP) bookkeeping.
	 */
	private int						offsetLastTFRead		= -1;							// last offset to data in tf
	private int						packetContinuation		= 0;

	/**
	 * info on current packet
	 */
	private ISecondaryPacketHeader	secondaryPacketHeader;
	private final ISpacePacketHeader		packetHeader;
	private boolean					packetHeaderValid       = false;
	private int						packetLength            = 0;
	private int						packetBytesRead			= 0;							// offset into packet; amount received
	private int						packetTFs				= 0;							// number of TFs used to create packet
	private int						offsetPacketHeader;										// offset into tf of packet header
	private IStationTelemInfo					packetDSNInfo			= null;							// DSN Info (for recording ERT for packet)
	private final byte[]			packetBuffer			= new byte[MAX_PACKET_LENGTH];
    private FrameIdHolder           firstFrameId            = FrameIdHolder.UNSUPPORTED;
	private MessageSubscriber		subscriber;

	private final boolean			setSolTimes;
	
	private final ISecondaryPacketHeaderLookup secPktHeaderLookup;
	private final ITelemetryPacketInfoFactory pktInfoFactory;
	private final IFrameMessageFactory frameMsgFactory;
    private final SseContextFlag               sseFlag;
	
	/**
	 * Configuration variable that:
	 * 
	 * When set to false, causes packet extraction
	 * to function in a legacy manner, similar to pre MPCS 5.8.0 behavior.
	 * 
	 * When set to true, attempts to correct invalid FPP. This will cause 
	 * some packets that were otherwise processed in legacy (pre-MPCS 5.8.0)
	 * versions to be discarded as invalid.
	 */
	private final boolean					correctFPPErrors		= true;						// set to false to ignore FPP errors.

    private final IPacketMessageFactory packetMsgFactory;

    private final IStatusMessageFactory statusMessageFactory;

    private final IContextConfiguration contextConfig;

	/**
	 * Creates an instance of PacketExtractService for the given virtual channel.
	 * 
	 * @param serviceContext the current application context
	 * @param ourVcid
	 *            ID of the virtual channel to process packets on
	 */
	public PacketExtractService(final ApplicationContext serviceContext, final int ourVcid) {
		this.bus = serviceContext.getBean(IMessagePublicationBus.class);
        this.pktExLog = TraceManager.getTracer(serviceContext, Loggers.PACKET_EXTRACTOR);
	    allowedVcid = ourVcid;	
		final CcsdsProperties config = serviceContext.getBean(CcsdsProperties.class);
		packetHeader = PacketHeaderFactory.create(config.getPacketHeaderFormat());
		packetHeader.setIdlePacketSecondaryHeaderAllowed(config.isIdlePacketSecondaryHeaderAllowed());
		setSolTimes = serviceContext.getBean(EnableLstContextFlag.class).isLstEnabled();
		secPktHeaderLookup = serviceContext.getBean(ISecondaryPacketHeaderLookup.class);
		pktInfoFactory = serviceContext.getBean(ITelemetryPacketInfoFactory.class);
		frameMsgFactory = serviceContext.getBean(IFrameMessageFactory.class);
		packetMsgFactory = serviceContext.getBean(IPacketMessageFactory.class);
		secondaryPacketHeader = secPktHeaderLookup.getNullSecondaryHeaderInstance();
		statusMessageFactory = serviceContext.getBean(IStatusMessageFactory.class);
		contextConfig = serviceContext.getBean(IContextConfiguration.class);
        sseFlag = serviceContext.getBean(SseContextFlag.class);
	}

	/**
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 */
	@Override
    public boolean startService() {
		subscriber = new PacketExtractMessageSubscriber();
		return true;
	}

	/**
	 * @see jpl.gds.shared.interfaces.IService#stopService()
	 */
	@Override
    public void stopService() {
		flushCurrentPacket(false);
		if (subscriber != null) {
			bus.unsubscribeAll(subscriber);
		}
	}

	/**
	 * Gets the virtual channel ID of the virtual channel handled by the PacketExtract instance.
	 * 
	 * @return vcid
	 */
	public long getVcid() {
		return allowedVcid;
	}

	/**
	 * Gets the number of packets constructed, excluding idle packets.
	 * 
	 * @return the number of packets
	 */
	public long getNumPackets() {
		return numberOfPackets;
	}

	/**
	 * Gets the number of idle packets found.
	 * 
	 * @return the number of idle packets
	 */
	public long getNumIdlePackets() {
		return numberOfPacketsIdle;
	}

	/**
	 * Gets the number of invalid packets found.
	 * 
	 * @return the number of invalid packets
	 */
	public long getNumInvalidPackets() {
		return numberOfPacketsInvalid;
	}

	/**
	 * Gets the number of frame gaps found.
	 * 
	 * @return the number of gaps
	 */
	public int getNumFrameGaps() {
		return numberOfTFGaps;
	}

	/**
	 * Gets the number of frames received.
	 * 
	 * @return the number of frames
	 */
	public long getNumFrames() {
		return numberOfTFs;
	}

	/**
	 * Gets the number of duplicate frames found.
	 * 
	 * @return the number of repeated frame
	 */
	public int getNumRepeatedFrames() {
		return numberOfTFRepeats;
	}

	/**
	 * Gets the number of frame regressions found.
	 * 
	 * @return the number of regressions.
	 */
	public int getNumFrameRegressions() {
		return numberOfTFRegressions;
	}

	/**
	 * Gets the number of bad frames found.
	 * 
	 * @return the number of bad frames.
	 */
	public int getNumBadFrames() {
		return numberOfTFBad;
	}

	/**
	 * The internal message bus subscribe invokes this method to consume a transfer frame message
	 * 
	 * @param tfm
	 *            TransferFrameMessage
	 */
	public void consume(final ITelemetryFrameMessage tfm) {
	    
	    /*
	     * Ignore TFs for non-session VCID or wrong SCID or wrong DSSID.
	     */
	    if (!contextConfig.accept(tfm)) {
	        return;
	    }
	    
		/*
		 * Ignore TFs that are not on this PacketExtract's Virtual Channel
		 */
		if (allowedVcid != tfm.getFrameInfo().getVcid()) {
			return;
		}	
		
		try {
		    tfm.getFrameInfo().getDataPointer();
		} catch (final UnsupportedOperationException e) {
		    final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN,
                    "PacketExtract received frame on VCID " +
                            allowedVcid + " does not have a valid data pointer (reason=" + e.getMessage() + "). Frame will not be processed.");
		    bus.publish(lm);
            pktExLog.log(lm);
		    return;
		}
		

		/*
		 * Valid frame for this PacketExtract. Keep count of TFs processed
		 */
		++numberOfTFs;

		/*
		 * If TF is "bad", send Bad Frame Message and abort processing.
		 */
		if (tfm.getFrameInfo().isBad()) {
			sendBadFrameMessage(tfm);
			return;
		}

		/*
		 * If TF is "dead code", abort processing. (No message sent).
		 */
		if (tfm.getFrameInfo().isDeadCode()) {
			return;
		}

		final int seqCount = tfm.getFrameInfo().getSeqCount();

		/*
		 * If the frame count is the same as the current frame's sequence number, then the frame is a repeat, and is discarded.
		 */
		if ((currentTFMessage != null) && (counterVirtualFrame == seqCount)) {
			/*
			 * repeated transfer frame -- discard
			 */
			sendRepeatedFrameMessage(tfm);
			counterVirtualFrame = seqCount;
			currentTFMessage = tfm;
            return;
		}

		/*
		 * Check that frame count is either the first, zero, or sequential
		 */
		if ((counterVirtualFrame == -1) || (seqCount == 0) || (seqCount == counterVirtualFrame + 1)) {
			/*
			 * Sequence of transfer frames is not broken, continue building packets
			 * 
			 * Check for VCFC roll over
			 */
			if ((seqCount == 0) && (counterVirtualFrame != 0)) {
				final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                        TraceSeverity.WARN, "Transfer Frame VCFC rollover on VC " + tfm.getFrameInfo().getVcid() +
                        " at ERT " + tfm.getStationInfo().getErtString() + " last VCFC=" + seqCount +
                        ", DSSID=" + tfm.getStationInfo().getDssId() + ", Frame Type=" + tfm.getFrameInfo().getType());
				bus.publish(lm);
                pktExLog.log(lm);
			}

		}
		else
        {
            /*
             * Found a non-sequential transfer frame. This indicates either there is a frame gap, or a frame regression.
             *
             * KLUDGE. Idle frames are winding up here and thus escape the special check in the THEN. The check either
             * needs to be added here, or the code fixed so that idle frames never get here.
             */

			if (counterVirtualFrame < seqCount) {
				/*
				 * Processing a frame gap anomaly.
				 */
				// TODO: smarter logic on sequences that span MAX_FRAME_COUNT
				++numberOfTFGaps;
				final IFrameEventMessage msg = frameMsgFactory.createFrameSequenceAnomalyMessage(tfm.getStationInfo(), tfm.getFrameInfo(), LogMessageType.TF_GAP,
	                       (counterVirtualFrame + 1) % (tfm.getFrameInfo().getMaxSeqCount() + 1), tfm.getFrameInfo().getSeqCount());
				bus.publish(msg);
				pktExLog.log(msg);
			}
			else {
				/*
				 * Processing a frame regression anomaly.
				 */
				++numberOfTFRegressions;
				final IFrameEventMessage msg = frameMsgFactory.createFrameSequenceAnomalyMessage(tfm.getStationInfo(), tfm.getFrameInfo(), 
                        LogMessageType.TF_REGRESSION, (counterVirtualFrame + 1) % (tfm.getFrameInfo().getMaxSeqCount() + 1), tfm.getFrameInfo().getSeqCount());
				bus.publish(msg);
				pktExLog.log(msg);
			}

			/*
			 * If in the middle of processing a packet that spans transfer frames, then send an invalid packet message.
			 */
			if (packetBytesRead > 0) {
				/*
				 * if we have received any part of a packet, generate bad packet messages.
				 */
				flushCurrentPacket(false);
			}

			/*
			 * Reset state machine to start of packet.
			 */
			resetCurrentPacket();
			machineState = State.FIRST_PACKET;
		}

		/*
		 * Reset TF bookkeeping to start of transfer frame
		 */
		currentTFMessage = tfm;
		offsetTFBody = currentTFMessage.getFrameInfo().getHdrSize();
		offsetCurrentTFRead = offsetTFBody;
		offsetLastTFRead = offsetCurrentTFRead + currentTFMessage.getFrameInfo().getDataAreaSize();
		counterVirtualFrame = seqCount;
	
		/*
		 * MPCS-7993 - 3/30/16.  Handle old to-do for idle frame check that was not
		 * happening correctly. The only required check is now right here. Note that this
		 * change results in extraction of fewer SMAP fill packets from idle frames
		 * that were slipping through.
		 * 
         * If TF is an idle frame, then reset state machine to State.FIRST_PACKET and discard
         * frame.
         * 
         * NOTE: tfm cannot be dead code here because all dead code frames are discarded above.
         */
        if (tfm.getFrameInfo().isIdle()) {
            if (pktExLog.isDebugEnabled()) {
                pktExLog.debug("    XXXX  (vcfc=" , seqCount , "): FIRST_PACKET: FPP == IDLE: DISCARDING TF #" , seqCount);
            }
            machineState = State.FIRST_PACKET;
            return;
        }
      
        /*
         * Process all packets in transfer frame.
         */

		boolean firstPacket = true;
		while (offsetCurrentTFRead < offsetLastTFRead) {
			buildNextPacket(firstPacket);
			firstPacket = false;
		}
	}

	/**
	 * Build the next packet based on the state machine Consume all of the packet in current tf (currentTFMessage)
	 * 
	 */
	private void buildNextPacket(final boolean firstPacket) {
		/*
		 * Locally store the First Packet Pointer for the current Transfer Frame.
		 */
		final int seqCount = currentTFMessage.getFrameInfo().getSeqCount();
		final int fpp = currentTFMessage.getFrameInfo().getDataPointer();

		/*
		 * Log VCFCs seen.
		 * 12/19/2011 MPCS-3106
		 */
		sourceVcfcs.add((long) seqCount);
		
		/*
		 * Get VCID for the current frame.
		 */
		sourceVcid = currentTFMessage.getFrameInfo().getVcid();

        // Get DSS
        sourceDssId = currentTFMessage.getStationInfo().getDssId();

		/*
		 * DEBUG
		 */
		if (pktExLog.isDebugEnabled()) {
			if (firstPacket) {
				final String msg = String.format("==============> NEW TF: vcfc=%-6d, vcid=%-6d, dssid=%-6d fpp=%-6d, packetContinuation=%-6d, ERT=%s", seqCount, sourceVcid, sourceDssId, fpp, packetContinuation,
						currentTFMessage.getStationInfo().getErtString());
				final StringBuilder hr = new StringBuilder();
				for (int i = 0; i < msg.length(); i++) {
					hr.append('=');
				}
				pktExLog.debug("");
				pktExLog.debug(hr);
				pktExLog.debug(msg);
				pktExLog.debug(hr);
			}
			pktExLog.debug(String.format("  STATE: %-12.12s (firstPacket=%-5.5s): vcfc=%-6d, offsetCurrentTFRead=%-6d, packetBytesRead=%-6d, offsetLastTFRead=%-6d", machineState,
					Boolean.toString(firstPacket), seqCount, offsetCurrentTFRead, packetBytesRead, offsetLastTFRead));
		}
		
		/*
		 * State Machine Switch
		 */
		switch (machineState) {
			case FIRST_PACKET: {
				/*
				 * This state is the initial state before any data is accepted, or after an invalid, idle, discarded, or no-code
				 * packet has been processed.
				 * 
				 * DEBUG: State Machine consistency check: Should never be in the State.FIRST_PACKET state unless the firstPacket
				 * flag is also true!
				 */
				if (pktExLog.isDebugEnabled()) {
					if (!firstPacket) {
						pktExLog.debug("    XXXX  (vcfc=" ,seqCount , "): FIRST_PACKET: DISCREPENCY!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				}

				/*
				 * Perform verification of FPP when beginning to process transfer frame.
				 * 
				 * MPCS-7993 - 3/30/16. Go through the frame info to determine
				 * if this frame contains continuation data. 
				 */
				if (currentTFMessage.getFrameInfo().getHeader().isContinuation()) {
					/*
					 * Check for CONTINUATION. If we are in the FIRST_PACKET state, then CONTINUATION is an error. Discard TF.
					 */
					if (pktExLog.isDebugEnabled()) {
						pktExLog.debug("    XXXX  (vcfc=" , seqCount , "): FIRST_PACKET: FPP == PACKET CONTINUATION: DISCARDING TF #" ,seqCount);
					}

					/*
					 * If fpp is set to FPP_PACKET_CONTINUATION_INDICTOR in the FIRST PACKET of a TF, discard all packets in this
					 * transfer frame.
					 */
					offsetCurrentTFRead = offsetLastTFRead;
					machineState = State.FIRST_PACKET;
					break;
				}

				/*
				 * Use first packet pointer to go to the start of the first packet.
				 * 
				 * FPP should be 0 in the FIRST_PACKET state, but may not be. In either case
				 * processing of the TF will begin at the FPP offset into the TF.
				 */
				offsetCurrentTFRead = offsetTFBody + fpp;

				/*
				 * Reset packet -- cannot continue a packet in the FIRST_PACKET state.
				 */
				resetCurrentPacket();

				/*
				 * Set State Machine state to NEXT_PACKET
				 */
				machineState = State.NEXT_PACKET;
				break;
			}

			case NEXT_PACKET: {
				/*
				 * This state indicates that we are not currently processing a packet, and there is currently data left in the TF.
				 * The current offset into the TF is used to begin looking for the next packet header.
				 *
				 * Handle Next Packet in Transfer Frame.
				 */
				offsetPacketHeader = offsetCurrentTFRead;

				/*
				 * Check to see if the transfer frame has enough room to contain the entire first packet's header
				 */
				pktExLog.debug("      PROCESSING PACKET HEADER...");

				/*
				 * Check the availability of data in the current TF
				 */
				if ((offsetLastTFRead - offsetPacketHeader) >= packetHeader.getPrimaryHeaderLength()) {
					/*
					 * There is enough room in transfer frame for packet header, so get it.
					 */
					if (copyBytesFromTF2PKTBuffer(packetHeader.getPrimaryHeaderLength())) {
						/*
						 * Header completely processed. Packet has been initialized. Set state to NEED_DATA to complete building
						 * packet.
						 */
						pktExLog.debug("        PACKET HEADER COMPLETE IN SINGLE TF");
						buildPacketHeader();
						packetContinuation = packetLength - packetBytesRead;
						if (pktExLog.isDebugEnabled() && (offsetCurrentTFRead == offsetLastTFRead)) {
							pktExLog.debug("          NO MORE DATA IN CURRENT TF. packetContinuation = " , packetContinuation);
						}

						/*
						 * Set next state
						 */
						machineState = State.NEED_DATA;
						break;
					}
				}
				else {
					/*
					 * There is NOT enough room in transfer frame for packet header. Packet Header may span TF.
					 *
					 * Check to see if ANY of the packet header is in this TF
					 */
					if (offsetCurrentTFRead == offsetLastTFRead) {
						/* 
						 * None of the header is in this TF, therefore the header is not actually split between TFs, but will appear
						 * in its entirety in the next TF.
						 * 
						 * This is the normal operation of the NEXT_PACKET state, so resetting state machine to NEXT_PACKT will
						 * successfully process the next packet.
						 *
						 * Set ERT from first frame containing this packet's data.
						 * 
						 * ==> EXCEPTIONAL CASE: The packet header length is 0, so in reality, this TF does not contain ANY of the
						 * current packet, therefore we do NOT log the ERT for this TF, but defer to the next one.
						 * 
						 * Header cannot be processed, it does not exist in this TF. Set state to NEXT_PACKET
						 */
						resetCurrentPacket();
						machineState = State.NEXT_PACKET;
						pktExLog.debug("      PACKET HEADER ZERO LENGTH");
						break;
					}
					else {
						/*
						 * Packet Header actually DOES span the TF. Grab what there is, change state to NEEDS_HEADER, and exit.
						 */
						if (copyBytesFromTF2PKTBuffer(offsetLastTFRead - offsetCurrentTFRead)) {
							/*
							 * When packet spans TF, keep track of where the continuation of the packet should start. This value is
							 * compared to the FPP in at the beginning of the next frame.
							 */
							packetContinuation = packetHeader.getPrimaryHeaderLength() - packetBytesRead;

							/*
							 * Partial header has been copied. Cannot start processing packet yet. Set state to NEED_HEADER to
							 * complete packet header.
							 */
							machineState = State.NEED_HEADER;
							if (pktExLog.isDebugEnabled()) {
								pktExLog.debug("      PACKET HEADER SPANS TO NEXT FRAME BY " , packetContinuation , " BYTES...");
							}
							break;
						}
					}
				}
				break;
			}

			case NEED_HEADER: {
				/*
				 * This state indicates that a partial packet header was seen at the end of the previous TF, and that the packet
				 * header is expected to be continued/completed at the beginning of the current TF.
				 * 
				 * packetBytesRead = number of bytes of the packet header already read and contained in the packetBuffer.
				 * offsetCurrentTFRead --> first byte of remainder of packet header.
				 * 
				 * DEBUG: State Machine consistency check: Should never be in the State.FIRST_PACKET state unless the firstPacket
				 * flag is also true!
				 */
				if (pktExLog.isDebugEnabled()) {
					if (!firstPacket) {
						pktExLog.debug("    XXXX  (vcfc=" , seqCount , "): NEED_HEADER: DISCREPENCY!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				}

				/*
				 * MPCS-11755 - 05/13/20 : Packet HEADER extraction across transfer frames doesn't properly
				 * check FHP for packet continuation
				 *
				 * See: AOS SPACE DATALINK PROTOCOL CCSDS 732.0-B-3, section 4.1.4.2.3.3
				 * https://public.ccsds.org/Pubs/732x0b3e1.pdf
				 *
				 * If the last Packet in the M_PDU Packet Zone of Transfer Frame N spills over into
				 * Frame M of the same Virtual Channel (N<M), then the First Header Pointer in Frame M
				 * ignores the residue of the split Packet and indicates the start of the next Packet that
				 * starts in Frame M.
				 *
				 * This specific case handles the Packet Header scenario
				 */
				if (firstPacket) {
					/*
					 * FPP specifies start of first packet in TF. Check for validity.
					 */
					if (packetContinuation > fpp) {
						if (pktExLog.isDebugEnabled()) {
							pktExLog.debug("    XXXX  (vcfc=" , seqCount , "): NEED_HEADER: packetContinuation(" , packetContinuation , ") > FPP(" , fpp , ")");
							pktExLog.debug("          packetContinuation=" , packetContinuation , ", packetHeaderRemainingBytes=" , (packetHeader.getPrimaryHeaderLength() - packetBytesRead) , ", bytesLeftInTF=" , (offsetLastTFRead - offsetCurrentTFRead));
						}

						offsetCurrentTFRead = offsetTFBody + fpp;
						if (pktExLog.isDebugEnabled()) {
							pktExLog.debug("          DISCARDING ", packetBytesRead, " from previous TF, and resetting 'offsetCurrentTFRead' to fpp(", offsetCurrentTFRead, ")");
						}
						flushCurrentPacket(true);
						machineState = State.NEXT_PACKET;
						break;

					}
				}

				/* MPCS-7993 - 3/30/16. Remove check for idle frame. Cannot get here if idle. */
				
				/*
				 * Handle for HEADER CONTINUATION.
				 *
				 * All state is set correctly to read current packet header from TF starting at offsetCurrentTFRead.
				 *
				 * NOTE: If a header is split across transfer frames, the second one always has enough room for the 6 byte
				 * header (or whatever remains of it).
				 */
				if (copyBytesFromTF2PKTBuffer(packetHeader.getPrimaryHeaderLength() - packetBytesRead)) {
					/*
					 * Header completely processed. Packet has been initialized. Set state to NEED_DATA to complete building
					 * packet.
					 */
					pktExLog.debug("        PACKET HEADER COMPLETE IN 2 TFs");
					buildPacketHeader();
					packetContinuation = packetLength - packetBytesRead;
					if (pktExLog.isDebugEnabled() && (offsetCurrentTFRead == offsetLastTFRead)) {
						pktExLog.debug("          NO MORE DATA IN CURRENT TF. packetContinuation = " , packetContinuation);
					}
					
					machineState = State.NEED_DATA;
					break;
				}
				break;
			}

			case NEED_DATA: {
				/*
				 * This state indicates that a complete packet header has been processed, and the data portion of the packet is
				 * ready for processing.
				 * 
				 * offsetCurrentTFRead --> first byte of packet data (the byte after the end of the packet header) packetBytesRead =
				 * number of bytes contained in the packetBuffer (the complete packet header).
				 */
				buildPacketHeader();

				/*
				 * If processing first packet of a TF, check stateful validity of FPP
				 */
				if (firstPacket) {
					/*
					 * Check for CONTINUATION.
					 * 
					 * MPCS-7993 - Go through frame info to determine if the frame
					 * contains continuation data.
					 */
					if (currentTFMessage.getFrameInfo().getHeader().isContinuation()) {
						pktExLog.debug("      PACKET BODY CONTINUED...");

						/*
						 * The FPP indicates that no packet begins in this TF, but that all data in the TF should be appended to the
						 * packet currently being
						 * 
						 * In the NEED_DATA state, this means that all of the data in this TF should be appended to the packet
						 * currently being built.
						 * 
						 * When packet spans TF, keep track of where the continuation of the packet should start. This value is
						 * compared to the FPP in at the beginning of the next frame.
						 */
						if (pktExLog.isDebugEnabled()) {
							pktExLog.debug("        READING CONTINUING PARTIAL PACKET DATA FROM TF. packetContinuation=" , packetContinuation);
						}
						buildPacket();
						
						/*
						 * Set continuation equal to whatever is left to be read of the packet's body.
						 */
						packetContinuation = packetLength - packetBytesRead;

						/*
						 * Remain in the NEED_DATA state.
						 */
						break;
					}

					/*
					 * FPP specifies start of first packet in TF. Check for validity.	
					 */
					if ((packetContinuation != fpp) && ((packetLength - packetBytesRead) != fpp)) {
						if (pktExLog.isDebugEnabled()) {
							pktExLog.debug("    XXXX  (vcfc=" , seqCount , "): NEED_DATA: FPP(" , fpp , ") != packetContinuation(" , packetContinuation , ")");
							pktExLog.debug("          packetContinuation=" , packetContinuation , ", packetRemainingBytes=" , (packetLength - packetBytesRead) , ", bytesLeftInTF=" , (offsetLastTFRead - offsetCurrentTFRead));
						}
						
						/*
						 * Corrective action for invalid FPP: - Send invalid packet message (FPP style). - Set
						 * offsetCurrentTFRead to fpp. - Set state machine to NEXT_PACKET.
						 */
						if (correctFPPErrors) {
							offsetCurrentTFRead = offsetTFBody + fpp;
							if (pktExLog.isDebugEnabled()) {
								pktExLog.debug("          DISCARDING " , packetBytesRead , " from previous TF, and resetting 'offsetCurrentTFRead' to fpp(" , offsetCurrentTFRead , ")");
							}
							flushCurrentPacket(true);
							machineState = State.NEXT_PACKET;
							break;
						}
					}
				}

				/*
				 * Build as much of the rest of the packet's data as there is data left in the frame.
				 */
				buildPacket();
				
				/*
				 * Set continuation equal to whatever is left to be read of the packet's body.
				 */
				packetContinuation = packetLength - packetBytesRead;
				
				/*
				 * Check to see if we're done with this packet.
				 */
				if (0 == packetContinuation) {
					/*
					 * Packet is completely processed within current TF. Set state to NEXT_PACKET to read next packet if it exists.
					 */
					machineState = State.NEXT_PACKET;
				}
				else {
					/*
					 * Packet is still incomplete because it spans into the next TF.
					 */
					if (pktExLog.isDebugEnabled()) {
						pktExLog.debug("      READ PARTIAL PACKET DATA FROM TF. packetContinuation=" , packetContinuation);
					}

					/*
					 * Remain in NEED_DATA state.
					 */
				}
				break;
			}
		} // end switch stateMachine
	}
	
	/**
	 * Build a packet or a packet part
	 * 
	 * This method may not be called unless and until the entire primary header of the packet being built has been completely read
	 * from one (or more) TFs.
	 * 
	 * This method requires that the contents of the packetHeader global variable has already been set.
	 * This method may be called multiple times for the same packet.
	 */
	private void buildPacket() {
		/*
		 * Check integrity of data structures before attempting to build a packet.
		 */
		if (!packetHeaderValid) {
			pktExLog.error("Internal Packet Extraction Error: Packet Header is not valid when attempting to build a packet.");
			machineState = State.NEXT_PACKET;
			
			/*
			 * Abort further processing of current TF
			 */
			offsetCurrentTFRead = offsetLastTFRead;
			return;
		}
		
		/*
		 * Check for Fill Packets
		 */
		if (packetHeader.isFill()) {
			sendIdlePacketMessage();
		}

		/*
		 * DEBUG
		 */
		pktExLog.debug("      PROCESSING PACKET BODY...");

		/*
		 * MPCS-11759 - 06/04/20: Idle packets from frames are not being correctly validated.
		 *
		 * Cleaned up the packet sanity check logic. The prior 'if' condition was a bit convoluted. See below.
		 * Packet length validation is done as part of the header validation.
		 *
		 * During code review we also decided to eliminate the MAX_DATA_APID check. One specific reason for this
		 * change is because it prevents us from using the CCSDS standard CFDP APID (2045).
		 *
		 * Old Logic:
		 * if ((!packetHeader.isFill() && (!packetHeader.isValid() || (packetLength > MAX_PACKET_LENGTH) || (packetHeader.getApid() > ISpacePacketHeader.MAX_DATA_APID)))
		 *		|| (packetHeader.isFill() && (packetLength > MAX_PACKET_LENGTH))) {
		 *
		 */
		if (!packetHeader.isValid()) {
			// If the Packet Header is not valid, get the specific reason for the validation failure
			sendInvalidMessage(packetHeader.getInvalidReason());
			machineState = State.NEXT_PACKET;

			pktExLog.debug("          PACKET FAILED SANITY CHECKS");
			pktExLog.debug("            Reason=" , packetHeader.getInvalidReason() , ", apid=" , packetHeader.getApid() , ", idle=" + packetHeader.isFill() , ", valid=" , packetHeader.isValid() , ", packetLength=" , packetLength);

			flushCurrentPacket(false);

			/*
			 * Abort further processing of current TF
			 */
			offsetCurrentTFRead = offsetLastTFRead;
			return;
		}


		/*
		 * Keep track of number of TFs used to complete current packet.
		 */
		packetTFs++;

		/*
		 * Check to see if there are enough bytes to complete the current packet based upon its length.
		 */
		if (offsetCurrentTFRead + (packetLength - packetBytesRead) <= offsetLastTFRead) {
			/*
			 * Packet is complete in this TF
			 */
			machineState = State.NEXT_PACKET;
			if (copyBytesFromTF2PKTBuffer(packetLength - packetBytesRead)) {
				/** MPCS-8198 - 06/15/2016: move check on secondary header until full packet is complete */
				final ISecondaryPacketHeaderExtractor secHdrExtractor = secPktHeaderLookup.lookupExtractor(packetHeader);
				if (! secHdrExtractor.hasEnoughBytes(packetBuffer, packetHeader.getPrimaryHeaderLength())) {
					/*
					 * Packet is invalid because it has a secondary header whose length is shorter than the length of the data in the packet
					 */

					final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN, "Packet abandoned because it is too short " + "apid=" + packetHeader.getApid() + " isValid=false" + " len="
							+ packetHeader.getPacketDataLength() + " version=" + packetHeader.getVersionNumber() + " type=" + packetHeader.getPacketType(), LogMessageType.INVALID_PKT_DATA);
					bus.publish(logm);
                    pktExLog.log(logm);
					machineState = State.NEXT_PACKET;
					numberOfPacketsInvalid++;

					pktExLog.debug("          PACKET DATA TOO SHORT FOR SECONDARY HEADER");
					flushCurrentPacket(false);
					return;
				}
				secondaryPacketHeader = secHdrExtractor.extract(packetBuffer, packetHeader.getPrimaryHeaderLength());
                pktExLog.debug("        PACKET COMPLETELY PROCESSED IN " , packetTFs , " TF(s):\n\n" , packetHeader);

				sendPacketMessage();
				machineState = State.NEXT_PACKET;
			}
		}
		else {
			/*
			 * Packet spans TF
			 */
			if (copyBytesFromTF2PKTBuffer(offsetLastTFRead - offsetCurrentTFRead)) {

				/*
				 * When packet spans TF, keep track of where the continuation of the packet should start. This value is compared to
				 * the FPP in at the beginning of the next frame.
				 */
				packetContinuation = (packetLength - packetBytesRead) - (offsetLastTFRead - offsetCurrentTFRead);
				if (pktExLog.isDebugEnabled()) {
					pktExLog.debug("        PACKET CONTINUES IN NEXT FRAME. " , packetContinuation , " BYTES LEFT TO READ...");
				}
				machineState = State.NEED_DATA;
			}
		}
	}

	/**
	 * Copy specified number of bytes from TF to Packet Buffer
	 * 
	 * @return true on success, false on failure
	 */
	private boolean copyBytesFromTF2PKTBuffer(final int count) {
		try {
			if (pktExLog.isDebugEnabled()) {
				pktExLog.debug("        COPYING " , count , " BYTES TO PACKET BUFFER...");
			}
			System.arraycopy(currentTFMessage.getFrame(), offsetCurrentTFRead, packetBuffer, packetBytesRead, count);
			offsetCurrentTFRead += count;
			packetBytesRead += count;
			
			/*
			 * If this is the first data put into this packet, set the packet's DSN info.
			 */
			if ((count > 0) && (packetBytesRead == count)) {
				/* 12/29/2011 - MPCS-2523
				 * Set ERT from first frame containing this packet's data.
				 */
				if (pktExLog.isDebugEnabled()) {
					pktExLog.debug("          >>> ERT set to \"" , currentTFMessage.getStationInfo().getErtString() , "\" from TF vcfc=" , currentTFMessage.getFrameInfo().getSeqCount());
				}
				packetDSNInfo = currentTFMessage.getStationInfo();

                firstFrameId = currentTFMessage.getFrameId();
			}
			return true;
		}
		catch (final ArrayIndexOutOfBoundsException e) {
			if (pktExLog.isDebugEnabled()) {
				pktExLog.debug("          PACKET BUFFER OVERRUN (PACKET SIZE EXCEEDS MAX_PACKET_LENGTH): TF=" , currentTFMessage.getFrameInfo().getSeqCount() , ", offsetCurrentTFRead=" , offsetCurrentTFRead
						, ", packetBytesRead=" , packetBytesRead , ", count=" , count);
			}
			
			/*
			 * Output an external log message.
			 */
			final String msg = "Packet buffer overrun. (Packet size exceeds MAX_PACKET_LENGTH): vcfc=" + currentTFMessage.getFrameInfo().getSeqCount() + ", offsetCurrentTFRead=" + offsetCurrentTFRead
					+ ", packetBytesRead=" + packetBytesRead + ", count=" + count;
			final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN,
                    msg, LogMessageType.INVALID_PKT_DATA);
			bus.publish(logm);
            pktExLog.log(logm);
			numberOfPacketsInvalid++;

			if (pktExLog.isDebugEnabled()) {
				e.printStackTrace();
			}
			flushCurrentPacket(false);

			/*
			 * Abort further processing of current TF
			 */
			offsetCurrentTFRead = offsetLastTFRead;
			return false;
		}
	}

	/**
	 * Build the packet header from the current values in packetBuffer.
	 * Sets the global 'packetHeaderValid' flag to true.
	 */
	private void buildPacketHeader() {
		packetHeader.setPrimaryValuesFromBytes(packetBuffer, 0);
		packetLength = packetHeader.getPrimaryHeaderLength() + packetHeader.getPacketDataLength() + 1;
		packetHeaderValid = true;
	}

	/**
	 * Clear any current packet statistics and bookkeeping that may have been in progress.
	 */
	private void resetCurrentPacket() {
		packetHeaderValid = false;
		packetLength = 0;
		packetBytesRead = 0;
		packetTFs = 0;
		packetContinuation = 0;
		sourceVcfcs.clear();
		sourceVcid = -1;
		sourceDssId = 0;
		/*
		 * MPCS-11755 - 05/13/20 : Packet HEADER extraction across transfer frames doesn't properly
		 * check FHP for packet continuation
		 *
		 * While investigating issue related to this JIRA I noticed that our packetBuffer is never
		 * cleared even when we extract a complete packet and move on to the next one (we keep overwriting the buffer).
		 * This probably has not been an issue since the buffer offsets are being used correctly.
		 */
		Arrays.fill(packetBuffer, (byte)0);
	}

	/**
	 * flushCurrentPacket() is called when some condition means we discard the current packet that we are building.
	 */
	private void flushCurrentPacket(final boolean fppFlag) {
		if (packetBytesRead > 0) {
			if (machineState == State.NEED_HEADER) {
				if (pktExLog.isDebugEnabled()) {
					pktExLog.debug("\n\n*** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET ***\n" , "<<< NO HEADER >>>\n"
							, "*** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET ***\n");
				}
				if (fppFlag) {
					sendInvalidPacketMessageFPP();
				}
				else {
					sendInvalidPacketMessage();
				}
			}
			else if (machineState == State.NEED_DATA) {
				if (pktExLog.isDebugEnabled()) {
					pktExLog.debug("\n\n*** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET ***\n" ,packetHeader
							, "*** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET *** FLUSHING PACKET ***\n");
				}
				if (fppFlag) {
					sendInvalidDataMessageFPP();
				}
				else {
					sendInvalidDataMessage();
				}
			}
		}

		/*
		 * Clear any current packet statistics and bookkeeping that may have been in progress.
		 */
		resetCurrentPacket();
	}

	/**
	 * Publish packet as a packet message, reset for next packet
	 */
	private void sendPacketMessage() {
		final int packetLength = packetHeader.getPrimaryHeaderLength() + packetHeader.getPacketDataLength() + 1;
		
		/*  MPCS-7289 - 4/30/15. Use the factory method that loads IPacketInfo from the header. */
		final ITelemetryPacketInfo pktInfo = pktInfoFactory.create(packetHeader, packetLength, secondaryPacketHeader);

		if (sourceVcfcs.size() == 0) {
			pktExLog.warn("Writing packet with no source VCFCs: " , pktInfo);
		}
		for (final Long vcfc : sourceVcfcs) {
			pktInfo.addSourceVcfc(vcfc);
		}
		pktInfo.setVcid(sourceVcid);
		pktInfo.setScid(currentTFMessage.getFrameInfo().getScid());
		/* 
		 * 06/10/14 - MPCS-6164 : chill_monitor shows wrong station
		 * ID for certain stations.
		 * Removed (byte) cast to resolve issue
		 */
		pktInfo.setDssId(sourceDssId);
		// do sclk/scet conversion
        final ISclk sclk = pktInfo.getSclk();
		
		// do sclk/scet conversion
        IAccurateDateTime scet = SclkScetUtility.getScet(sclk, currentTFMessage.getStationInfo().getErt(),
                                                         currentTFMessage.getFrameInfo().getScid(), pktExLog);
		if (scet == null) {
			if (!sclkScetWarningIssued) {
                pktExLog.warn(Markers.TIME_CORR,
                        "Could not find SCLK/SCET correlation file for spacecraft ID "
                                , currentTFMessage.getFrameInfo().getScid()
						, ".  Packet SCET values are set to the beginning of the epoch.");
				sclkScetWarningIssued = true;
			}
			scet = new AccurateDateTime(0);

		}
		else {
			if (setSolTimes) {
				pktInfo.setLst(LocalSolarTimeFactory.getNewLst(scet, pktInfo.getScid()));
			}
		}

		pktInfo.setScet(scet);
        pktInfo.setFromSse(sseFlag.isApplicationSse());
		numberOfPackets++;
		pktInfo.setErt(packetDSNInfo.getErt());
		
		/* MPCS-7289 - 4/30/15.  Set new fields in packet info from DSN and frame info. */
		pktInfo.setBitRate(packetDSNInfo.getBitRate());
		pktInfo.setFrameType(currentTFMessage.getFrameInfo().getFrameFormat().getName());
		
        /* MPCS-7289 - 4/30/15.  Use factory to create packet message */
		final ITelemetryPacketMessage pktM = packetMsgFactory.createTelemetryPacketMessage(
                                               pktInfo,
                                               null,
                                               HeaderHolder.NULL_HOLDER,
                                               TrailerHolder.NULL_HOLDER,
                                               ! pktInfo.isFill()
                                                   ? firstFrameId
                                                   : FrameIdHolder.UNSUPPORTED);
		pktM.setPacket(packetBuffer, packetLength);
		try {
			bus.publish(pktM);
            pktExLog.debug(pktM);
		}
		catch (final Exception e) {
			e.printStackTrace();
			pktExLog.error("Error processing valid packet: APID=" , packetHeader.getApid() , ", Seq=" , packetHeader.getSourceSequenceCount() , ", SCLK=" , pktInfo.getSclk().toString());
			pktExLog.error(e.toString());
		}
		resetCurrentPacket();
		lastValidSclk = pktInfo.getSclk();
	}

	/**
	 * Send invalid packet; currently just log a message
	 */
	private void sendInvalidPacketMessageFPP() {
		final String msg = "A packet header could not be constructed because the " +
				"remaining bytes required to complete the last packet header is not " +
				"consistent with the location indicated by the First Header Pointer of the current frame, "
				+ "Bytes Required To Complete Last Packet Header=" + packetContinuation
				+ ", First Header Pointer=" + currentTFMessage.getFrameInfo().getDataPointer()
				+ ", VC=" + allowedVcid + ", ERT=" + currentTFMessage.getStationInfo().getErtString()
				+ ", frame VCFC=" + counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN,
                msg, LogMessageType.INVALID_PKT_HEADER);
		bus.publish(logm);
        pktExLog.log(logm);
		numberOfPacketsInvalid++;
	}

	/**
	 * Send invalid data, currently just log a message
	 */
	private void sendInvalidDataMessageFPP() {
		final int packetLength = packetHeader.getPrimaryHeaderLength() + packetHeader.getPacketDataLength() + 1;
		final String msg = "All data for a packet could not be found because the " +
				"remaining bytes required to complete the last packet data is not " +
				"consistent with the location indicated by the First Header Pointer of the current frame, "
				+ "Bytes Required To Complete Last Packet Data=" + packetContinuation
				+ ", First Header Pointer=" + currentTFMessage.getFrameInfo().getDataPointer()
				+ ", VC=" + allowedVcid + ", APID=" + packetHeader.getApid() + ", isValid=" + packetHeader.isValid() + ", length="
				+ packetLength + ", SEQ=" + packetHeader.getSourceSequenceCount() + ", SCLK=" + lastValidSclk.toString() + ", ERT=" + currentTFMessage.getStationInfo().getErtString() + ", frame VCFC="
				+ counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN,
                msg, LogMessageType.INVALID_PKT_DATA);
		bus.publish(logm);
        pktExLog.log(logm);
		numberOfPacketsInvalid++;
	}

	/**
	 * Send invalid packet; currently just log a message
	 */
	private void sendInvalidPacketMessage() {
		final String msg = "A packet header could not be constructed because of a Transfer Frame Gap, " + "VC=" + allowedVcid + ", ERT=" + currentTFMessage.getStationInfo().getErtString()
				+ ", frame VCFC=" + counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN,
                msg, LogMessageType.INVALID_PKT_HEADER);
		bus.publish(logm);
        pktExLog.log(logm);
		numberOfPacketsInvalid++;
	}

	/**
	 * Send invalid data, currently just log a message
	 */
	private void sendInvalidDataMessage() {
		final int packetLength = packetHeader.getPrimaryHeaderLength() + packetHeader.getPacketDataLength() + 1;
		final String msg = "All data for a packet could not be found because of a Transfer Frame Gap, " + "VC=" + allowedVcid + ", APID=" + packetHeader.getApid() + ", isValid=" + packetHeader.isValid()
				+ ", length=" + packetLength + ", SEQ=" + packetHeader.getSourceSequenceCount() + ", SCLK=" + lastValidSclk.toString() + ", ERT=" + currentTFMessage.getStationInfo().getErtString()
				+ ", frame VCFC=" + counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN,
                msg, LogMessageType.INVALID_PKT_DATA);
		bus.publish(logm);
        pktExLog.log(logm);
		numberOfPacketsInvalid++;
	}

	/**
	 * Send idle packet, currently just a log message
	 * 
	 */
	private void sendIdlePacketMessage() {
		numberOfPacketsIdle++;
	}
	
	/**
	 * Send bad frame message.
	 * 
	 */
	private void sendBadFrameMessage(final ITelemetryFrameMessage tfm) {
		pktExLog.debug("Bad transfer frame");
		final IFrameEventMessage bfm = frameMsgFactory.createBadFrameMessage(tfm.getStationInfo(), tfm.getFrameInfo());
		numberOfTFBad++;
		bus.publish(bfm);
        pktExLog.log(bfm);
	}

	/**
	 * Send message indicating repeated frame.
	 * 
	 * @param tfm
	 *            the current TransferFrameMessage
	 */
	private void sendRepeatedFrameMessage(final ITelemetryFrameMessage tfm) {
		pktExLog.debug("Repeated transfer frame");
		numberOfTFRepeats++;
		final IFrameSequenceAnomalyMessage msg = frameMsgFactory.createFrameSequenceAnomalyMessage(tfm.getStationInfo(), tfm.getFrameInfo(), 
                LogMessageType.TF_REPEAT, (counterVirtualFrame + 1) % (tfm.getFrameInfo().getMaxSeqCount() + 1), tfm.getFrameInfo().getSeqCount());
		bus.publish(msg);
        pktExLog.log(msg);
	}

	/**
	 * Send message indicating bad packet.
	 *
	 * @param invalidReason
	 */
	private void sendInvalidMessage(final String invalidReason) {
		final int packetLength = packetHeader.getPrimaryHeaderLength() + packetHeader.getPacketDataLength() + 1;
		final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARNING,
                "Packet abandoned because header was corrupt. Reason=" + invalidReason + ", VC=" + allowedVcid + ", APID=" + packetHeader.getApid() + ", isValid="
                		+ packetHeader.isValid() + ", length=" + packetLength + ", SEQ=" + packetHeader.getSourceSequenceCount() + ", SCLK=" + lastValidSclk.toString() + ", ERT="
                		+ currentTFMessage.getStationInfo().getErtString() + ", frame VCFC=" + counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead, LogMessageType.INVALID_PKT_HEADER);
		bus.publish(logm);
		numberOfPacketsInvalid++;
        pktExLog.log(logm);
	}

	/**
	 * 
	 * PacketExtractMessageSubscriber is the listener for internal frame and related= messages.
	 * 
	 *
	 */
	private class PacketExtractMessageSubscriber extends BaseMessageHandler {

		/**
		 * Creates an instance of PacketExtractMessageSubscriber.
		 */
		public PacketExtractMessageSubscriber() {
			bus.subscribe(TmServiceMessageType.TelemetryFrame, this);
			bus.subscribe(CommonMessageType.EndOfData, this);
		}

		/**
		 * {@inheritDoc}
		 * @see jpl.gds.shared.message.BaseMessageHandler#handleMessage(jpl.gds.shared.message.IMessage)
		 */
		@Override
		public void handleMessage(final IMessage m) {
			if (m.isType(CommonMessageType.EndOfData)) {
				flushCurrentPacket(false);

				/*
				 * Abort further processing of current TF
				 */
				offsetCurrentTFRead = offsetLastTFRead;
				return;
			}
			else {
				consume((ITelemetryFrameMessage) m);
			}
		}
	}
}
