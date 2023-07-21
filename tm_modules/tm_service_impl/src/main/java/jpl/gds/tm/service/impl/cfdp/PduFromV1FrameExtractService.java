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
package jpl.gds.tm.service.impl.cfdp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduFactory;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpMessageFactory;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;
import jpl.gds.tm.service.api.cfdp.IPduExtractService;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IFrameSequenceAnomalyMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;


/**
 * A service that extracts CFDP PDUs from CCSDS V1 transfer frames.  It subscribes to
 * transfer frame messages and publishes PDU messages. As a side effect it
 * detects transfer frame gaps.
 *
 *
 * @since R8
 *
 */
public class PduFromV1FrameExtractService implements IPduExtractService {

	private enum State {
		FIRST_PDU,  // find pdu from first pdu pointer
		NEED_FIXED_HEADER,    // need remaining fixed pdu header
		NEED_VARIABLE_HEADER, // need remaining variable pdu header
		NEED_DATA,      // need remaining data
		NEXT_PDU        // get next pdu in transfer frame
	}

	/**
	 * Logger for all messages.
	 */
	private final Tracer pduExLog;

	/**
	 * Maximum size of a pdu
	 */
	private static final int	MAX_PDU_LENGTH = ICfdpPduHeader.MAX_PDU_DATA_LENGTH;

	/**
	 * If the first byte of a PDU header following an existing PDU matches this,
	 * discard the rest of the transfer frame. NOTE: APL MESSENGER SPECIFIC.
	 */
	private static final int    STOP_PDU_FLAG = 0xFF;


	/**
	 * simple state machine for pdu extraction
	 *
	 * state machine states
	 */
	private State				    machineState            = State.FIRST_PDU;


	/**
	 * Internal message destination
	 */
	private final IMessagePublicationBus	bus;

	/**
	 * Statistics and tracking
	 */
	private long					        numberOfTFs				= 0;
	private int						numberOfTFGaps			= 0;
	private int						numberOfTFRegressions	= 0;
	private int						numberOfTFRepeats		= 0;
	private int						numberOfTFBad			= 0;
	private final long					        numberOfPdus			= 0;
	private long					        numberOfPdusInvalid	    = 0;
	private int						allowedVcid				= 0;
	private final List<ITelemetryFrameMessage>    pduFrames           = new ArrayList<>();
	private int                          sourceVcid            = -1;
	private int						sourceDssId				= 0;

	/**
	 * info on current TF
	 */
	private ITelemetryFrameMessage currentTFMessage = null;

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
	 * First Packet Pointer (DP) bookkeeping.
	 */
	private int						offsetLastTFRead		= -1;							// last offset to data in tf
	private int						pduContinuation		    = 0;

	/**
	 * info on current pdu
	 */
	private ICfdpPduHeader          pduHeader			    = null;
	private boolean					pduHeaderValid          = false;
	private int						pduLength               = 0;
	private int						pduBytesRead			= 0;							// offset into pdu; amount received
	private int						pduTFs				    = 0;							// number of TFs used to create pdu
	private int						offsetPduHeader         = 0;							// offset into tf of pdu header
	private final byte[]			pduBuffer			    = new byte[MAX_PDU_LENGTH];
	private MessageSubscriber            subscriber;
	private final ICfdpMessageFactory     cfdpMsgFactory;

	/**
	 * Configuration variable that:
	 *
	 * When set to false, causes PDU extraction
	 * to function in a legacy manner, similar to pre MPCS 5.8.0 behavior.
	 *
	 * When set to true, attempts to correct invalid DP. This will cause
	 * some pdus that were otherwise processed in legacy (pre-MPCS 5.8.0)
	 * versions to be discarded as invalid.
	 */
	private final boolean correctFPPErrors = true; // set to false to ignore DP errors.

	private final IFrameMessageFactory frameMsgFactory;

	private final IStatusMessageFactory statusMsgFactory;

	private final ICfdpPduFactory pduFactory;

	private final IContextConfiguration contextConfig;

	private final List<Integer> allowedCfdpVcids;

	/**
	 * Creates an instance of the service for the given virtual channel.
	 *
	 * @param serviceContext the current application context
	 *
	 * @param vcid2View
	 *            ID of the virtual channel to process PDUs on
	 */
	public PduFromV1FrameExtractService(final ApplicationContext serviceContext, final int vcid2View) {

		this.bus = serviceContext.getBean(IMessagePublicationBus.class);
		this.frameMsgFactory = serviceContext.getBean(IFrameMessageFactory.class);
		this.cfdpMsgFactory = serviceContext.getBean(ICfdpMessageFactory.class);
		this.statusMsgFactory = serviceContext.getBean(IStatusMessageFactory.class);
		allowedVcid = vcid2View;
		if (vcid2View < 0) {
			allowedVcid = 0;
		}
        pduExLog = TraceManager.getTracer(serviceContext, Loggers.PDU_EXTRACTOR);
		pduFactory = serviceContext.getBean(ICfdpPduFactory.class);
		contextConfig = serviceContext.getBean(IContextConfiguration.class);
		allowedCfdpVcids = serviceContext.getBean(MissionProperties.class).getCfdpPduExtractVcids();

	}

	/**
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 */
	@Override
	public boolean startService() {
		subscriber = new PduExtractMessageSubscriber();
		return true;
	}

	/**
	 * @see jpl.gds.shared.interfaces.IService#stopService()
	 */
	@Override
	public void stopService() {
		flushCurrentPdu(false);
		if (subscriber != null) {
			bus.unsubscribeAll(subscriber);
		}
	}

	/**
	 * Gets the virtual channel ID of the virtual channel handled by the PacketExtract instance.
	 *
	 * @return VCID
	 */
	public long getVcid() {
		return allowedVcid;
	}

	/**
	 * Gets the number of PDUs constructed, excluding idle PDUs.
	 *
	 * @return the number of PDUs
	 */
	public long getNumPdus() {
		return numberOfPdus;
	}

	/**
	 * Gets the number of invalid PDUs found.
	 *
	 * @return the number of invalid PDUs
	 */
	public long getNumInvalidPdus() {
		return numberOfPdusInvalid;
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

		// MPCS-10788 - 4/15/19: Ignore TF's if VCID is not allowed for CFDP
		if (!allowedCfdpVcids.contains(tfm.getFrameInfo().getVcid())) {
			return;
		}

		/* Cannot process AOS (V2) frames here. */
		if (tfm.getFrameInfo().getFrameFormat() != null &&
				tfm.getFrameInfo().getFrameFormat().getFormat().getType() != IFrameFormatDefinition.TypeName.CCSDS_TM_1) {
			final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN,
			                                                                               "PduFromFrameExtractor received frame on VCID " + allowedVcid + " that is not a V1 frame format. Frame will not be processed.");
			pduExLog.log(lm);
			return;
		}

		/*
		 * If fetching the data pointer throws, this frame is not of a type that can be processed here.
		 */
		try {
			tfm.getFrameInfo().getDataPointer();
		}catch (final UnsupportedOperationException e) {
			final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN,
			                                                                               "PduFromFrameExtractor received frame on VCID " +
					                                                                               allowedVcid + " that does not have a valid first header data pointer (reason=" +
					                                                                               e.getMessage() + "). Frame will not be processed.");
			pduExLog.log(lm);
			return;
		}


		/*
		 * Valid frame for this extractor. Keep count of TFs processed
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
			 * Sequence of transfer frames is not broken, continue building PDUs
			 *
			 * Check for VCFC roll over
			 */
			if ((seqCount == 0) && (counterVirtualFrame != 0)) {
				final IPublishableLogMessage lm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN,
				                                                                               "Transfer Frame VCFC rollover at ERT " + tfm.getStationInfo().getErtString());
				pduExLog.log(lm);
			}

		}
		else
		{
			/*
			 * Found a non-sequential transfer frame. This indicates either there is a frame gap, or a frame regression.
			 */

			if (counterVirtualFrame < seqCount) {
				/*
				 * Processing a frame gap anomaly.
				 */
				// TODO: smarter logic on sequences that span MAX_FRAME_COUNT
				++numberOfTFGaps;
				final IFrameSequenceAnomalyMessage logm =  frameMsgFactory.createFrameSequenceAnomalyMessage(tfm.getStationInfo(),
				                                                                                             tfm.getFrameInfo(), LogMessageType.TF_GAP,
				                                                                                             ((counterVirtualFrame + 1) % (tfm.getFrameInfo().getMaxSeqCount() + 1)),
				                                                                                             tfm.getFrameInfo().getSeqCount());
				bus.publish(logm);
				pduExLog.debug(logm.getOneLineSummary());
			}
			else {
				/*
				 * Processing a frame regression anomaly.
				 */
				++numberOfTFRegressions;
				final IFrameSequenceAnomalyMessage logm = frameMsgFactory.createFrameSequenceAnomalyMessage(tfm.getStationInfo(),
				                                                                                            tfm.getFrameInfo(), LogMessageType.TF_REGRESSION,
				                                                                                            ((counterVirtualFrame + 1) % (tfm.getFrameInfo().getMaxSeqCount() + 1)),
				                                                                                            tfm.getFrameInfo().getSeqCount());
				bus.publish(logm);
				pduExLog.debug(logm.getOneLineSummary());
			}

			/*
			 * If in the middle of processing a pdu that spans transfer frames, then send an invalid pdu message.
			 */
			if (pduBytesRead > 0) {
				/*
				 * if we have received any part of a pdu, generate bad pdu messages.
				 */
				flushCurrentPdu(false);
			}

			/*
			 * Reset state machine to start of pdu.
			 */
			resetCurrentPdu();
			machineState = State.FIRST_PDU;
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
		 * If TF is an idle frame, then reset state machine to State.FIRST_PDU and discard
		 * frame.
		 *
		 * NOTE: tfm cannot be dead code here because all dead code frames are discarded above.
		 */
		if (tfm.getFrameInfo().isIdle()) {
			machineState = State.FIRST_PDU;
			return;
		}

		/*
		 * Process all pdus in transfer frame.
		 */

		boolean firstPdu = true;
		while (offsetCurrentTFRead < offsetLastTFRead) {
			buildNextPdu(firstPdu);
			firstPdu = false;
		}
	}

	/**
	 * Build the next pdu based on the state machine Consume all of the pdu in current tf (currentTFMessage)
	 *
	 */
	private void buildNextPdu(final boolean firstPdu) {
		/*
		 * Locally store the data (in this case, first PDU) pointer for the current Transfer Frame.
		 */
		final int seqCount = currentTFMessage.getFrameInfo().getSeqCount();
		final int dataPointer = currentTFMessage.getFrameInfo().getDataPointer();

		/*
		 * Log VCFCs seen.
		 * 12/19/2011 - MPCS-3106
		 */
		pduFrames.add(currentTFMessage);

		/*
		 * Get VCID for the current frame.
		 */
		sourceVcid = currentTFMessage.getFrameInfo().getVcid();

		// Get DSS
		sourceDssId = currentTFMessage.getStationInfo().getDssId();

		/*
		 * DEBUG
		 */
		if (pduExLog.isDebugEnabled()) {
			if (firstPdu) {
				final String msg = String.format("==============> NEW TF: vcfc=%-6d, vcid=%-6d, dssid=%-6d dataPointer=%-6d, pduContinuation=%-6d, ERT=%s", seqCount, sourceVcid, sourceDssId, dataPointer, pduContinuation,
				                                 currentTFMessage.getStationInfo().getErtString());
				final StringBuilder hr = new StringBuilder();
				for (int i = 0; i < msg.length(); i++) {
					hr.append('=');
				}
				pduExLog.debug("\n" + hr);
				pduExLog.debug(msg);
				pduExLog.debug(hr);
			}
			pduExLog.debug(String.format("  STATE: %-20.20s (firstPdu=%-5.5s): vcfc=%-6d, offsetCurrentTFRead=%-6d, pduBytesRead=%-6d, offsetLastTFRead=%-6d", machineState,
			                             Boolean.toString(firstPdu), seqCount, offsetCurrentTFRead, pduBytesRead, offsetLastTFRead));
		}

		/*
		 * State Machine Switch
		 */
		switch (machineState) {
			case FIRST_PDU: {
				/*
				 * This state is the initial state before any data is accepted, or after an invalid, idle, discarded, or no-code
				 * pdu has been processed.
				 *
				 * DEBUG: State Machine consistency check: Should never be in the State.FIRST_PDU state unless the firstPacket
				 * flag is also true!
				 */
				if (pduExLog.isDebugEnabled()) {
					if (!firstPdu) {
						pduExLog.debug("    XXXX  (vcfc=" + seqCount + "): FIRST_PDU: DISCREPENCY!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				}


				/*
				 * Check for CONTINUATION. If we are in the FIRST_PDU state, then CONTINUATION is an error. Discard TF.
				 */
				if (currentTFMessage.getFrameInfo().getHeader().isContinuation()) {
					if (pduExLog.isDebugEnabled()) {
						pduExLog.debug("    XXXX  (vcfc=" + seqCount + "): FIRST_PDU: DP == PDU CONTINUATION: DISCARDING TF #" + seqCount);
					}

					offsetCurrentTFRead = offsetLastTFRead;
					machineState = State.FIRST_PDU;
					break;
				}

				/*
				 * Use DP to go to the start of the first pdu.
				 *
				 * DP should be 0 in the FIRST_PDU state, but may not be. In either case
				 * processing of the TF will begin at the DP offset into the TF.
				 */
				offsetCurrentTFRead = offsetTFBody + dataPointer;

				/*
				 * Reset pdu -- cannot continue a pdu in the FIRST_PDU state.
				 */
				resetCurrentPdu();

				/*
				 * Set State Machine state to NEXT_PDU
				 */
				machineState = State.NEXT_PDU;
				break;
			}

			case NEXT_PDU: {
				/*
				 * This state indicates that we are not currently processing a pdu, and there is currently data left in the TF.
				 * The current offset into the TF is used to begin looking for the next pdu header.
				 *
				 * Handle Next PDU in Transfer Frame.
				 */
				offsetPduHeader = offsetCurrentTFRead;

				/*
				 * Check to see if the transfer frame has enough room to contain the entire first pdu's header
				 */
				pduExLog.debug("      PROCESSING PDU HEADER...");

				/*
				 * Check the availability of data in the current TF
				 */

				if ((offsetLastTFRead - offsetPduHeader) >= ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH) {

					/*
					 * There is enough room in transfer frame for the fixed pdu header */

					if (copyBytesFromTFtoPDUBuffer(ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH)) {
						/*
						 * Header completely processed. PDU has been initialized. Set state to NEED_DATA to complete building
						 * pdu.
						 */
						pduExLog.debug("        FIXED PDU HEADER COMPLETE IN SINGLE TF");
						buildPduHeader();
						pduContinuation = pduLength - pduBytesRead;
						if (pduExLog.isDebugEnabled() && (offsetCurrentTFRead == offsetLastTFRead)) {
							pduExLog.debug("          NO MORE DATA IN CURRENT TF. pduContinuation = " + pduContinuation);
						}

						/*
						 * Set next state
						 */
						machineState = State.NEED_VARIABLE_HEADER;
						break;
					}
				}
				else {
					/*
					 * There is NOT enough room in transfer frame for the fixed pdu header. PDU Header may span TF.
					 *
					 * Check to see if ANY of the pdu header is in this TF
					 */
					if (offsetCurrentTFRead == offsetLastTFRead) {
						/*
						 * None of the header is in this TF, therefore the header is not actually split between TFs, but will appear
						 * in its entirety in the next TF.
						 *
						 * This is the normal operation of the NEXT_PDU state, so resetting state machine to NEXT_PDU will
						 * successfully process the next pdu.
						 *
						 * Set ERT from first frame containing this pdu's data.
						 *
						 * ==> EXCEPTIONAL CASE: The pdu header length is 0, so in reality, this TF does not contain ANY of the
						 * current pdu, therefore we do NOT log the ERT for this TF, but defer to the next one.
						 *
						 * Header cannot be processed, it does not exist in this TF. Set state to NEXT_PDU
						 */
						resetCurrentPdu();
						machineState = State.NEXT_PDU;
						pduExLog.debug("      PDU HEADER ZERO LENGTH");
						break;
					}
					else {
						/*
						 * Fixed PDU Header actually DOES span the TF. Grab what there is, change state to NEED_FIXED_HEADER, and exit.
						 */
						if (copyBytesFromTFtoPDUBuffer(offsetLastTFRead - offsetCurrentTFRead)) {
							/*
							 * When pdu spans TF, keep track of where the continuation of the pdu should start. This value is
							 * compared to the DP in at the beginning of the next frame.
							 */
							pduContinuation = ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH - pduBytesRead;

							/*
							 * Partial header has been copied. Cannot start processing pdu yet. Set state to NEED_FIXED_HEADER to
							 * complete the fixed pdu header.
							 */
							machineState = State.NEED_FIXED_HEADER;
							if (pduExLog.isDebugEnabled()) {
								pduExLog.debug("      PDU HEADER SPANS TO NEXT FRAME BY " + pduContinuation + " BYTES...");
							}
							break;
						}
					}
				}
				break;
			}

			case NEED_FIXED_HEADER: {
				/*
				 * This state indicates that a partial fixed pdu header was seen at the end of the previous TF, and that the fixed pdu
				 * header is expected to be continued/completed at the beginning of the current TF.
				 *
				 * pduBytesRead = number of bytes of the pdu header already read and contained in the pduBuffer.
				 * offsetCurrentTFRead --> first byte of remainder of pdu header.
				 *
				 * DEBUG: State Machine consistency check: Should never be in the State.FIRST_PDU state unless the firstPacket
				 * flag is also true!
				 */
				if (pduExLog.isDebugEnabled()) {
					if (!firstPdu) {
						pduExLog.debug("    XXXX  (vcfc=" + seqCount + "): NEED_HEADER: DISCREPENCY!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				}

				/*
				 * Handle for FIXED HEADER CONTINUATION.
				 *
				 * All state is set correctly to read current pdu header from TF starting at offsetCurrentTFRead.
				 *
				 * NOTE: If a header is split across transfer frames, the second one always has enough room for the
				 * header (or whatever remains of it).
				 */
				if (copyBytesFromTFtoPDUBuffer(ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH - pduBytesRead)) {
					/*
					 * Header completely processed. Packet has been initialized. Set state to NEED_DATA to complete building
					 * pdu.
					 */
					pduExLog.debug("        FIXED PDU HEADER COMPLETE IN 2 TFs");
					buildPduHeader();
					pduContinuation = pduLength - pduBytesRead;
					if (pduExLog.isDebugEnabled() && (offsetCurrentTFRead == offsetLastTFRead)) {
						pduExLog.debug("          NO MORE DATA IN CURRENT TF. pduContinuation = " + pduContinuation);
					}

				}

				/*
				 * Fixed header complete. Is there room in this frame for the variable PDU header?
				 */

				if ((offsetLastTFRead - offsetCurrentTFRead) >= pduHeader.getHeaderLength() - ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH) {
					/*
					 * There is enough room in transfer frame for the variable pdu header */

					if (copyBytesFromTFtoPDUBuffer(pduHeader.getHeaderLength() - ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH)) {
						/*
						 * Variable header completely processed. PDU has been initialized. Set state to NEED_DATA to complete building
						 * pdu.
						 */
						pduExLog.debug("        VARIABLE PDU HEADER COMPLETE IN SINGLE TF");
						populateVariablePduHeader();
						pduContinuation = pduLength - pduBytesRead;
						if (pduExLog.isDebugEnabled() && (offsetCurrentTFRead == offsetLastTFRead)) {
							pduExLog.debug("          NO MORE DATA IN CURRENT TF. pduContinuation = " + pduContinuation);
						}

						/*
						 * Set next state
						 */
						machineState = State.NEED_DATA;
						break;
					}
				} else {
					/*
					 * Variable PDU Header actually DOES span the TF. Grab what there is, change state to NEED_VARIABLE_HEADER, and exit.
					 */
					if (copyBytesFromTFtoPDUBuffer(offsetLastTFRead - offsetCurrentTFRead)) {
						/*
						 * When pdu spans TF, keep track of where the continuation of the pdu should start. This value is
						 * compared to the DP in at the beginning of the next frame.
						 */
						pduContinuation = pduLength - pduBytesRead;

						/*
						 * Partial header has been copied. Cannot start processing pdu yet. Set state to NEED_HEADER to
						 * complete pdu header.
						 */
						machineState = State.NEED_VARIABLE_HEADER;
						if (pduExLog.isDebugEnabled()) {
							pduExLog.debug("      VARIABLE PDU HEADER SPANS TO NEXT FRAME BY " + pduContinuation + " BYTES...");
						}
						break;
					}
				}
				break;

			}

			case NEED_VARIABLE_HEADER: {
				/*
				 * This states indicates we have read the fixed PDU header, but still need the variable portion
				 * of the header.
				 */

				/*
				 * Check the availability of variable header in the current TF
				 */

				if ((offsetLastTFRead - offsetCurrentTFRead) >= pduHeader.getHeaderLength() - ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH) {
					/*
					 * There is enough room in transfer frame for the variable pdu header */

					if (copyBytesFromTFtoPDUBuffer(pduHeader.getHeaderLength() - ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH)) {
						/*
						 * Header completely processed. PDU has been initialized. Set state to NEED_DATA to complete building
						 * pdu.
						 */
						pduExLog.debug("        VARIABLE PDU HEADER COMPLETE IN 2 TFs");
						populateVariablePduHeader();
						pduContinuation = pduLength - pduBytesRead;
						if (pduExLog.isDebugEnabled() && (offsetCurrentTFRead == offsetLastTFRead)) {
							pduExLog.debug("          NO MORE DATA IN CURRENT TF. pduContinuation = " + pduContinuation);
						}

						/*
						 * Set next state
						 */
						machineState = State.NEED_DATA;
						break;
					}
				} else {

					/* Not enough room in this TF for the variable header. Copy what there is. */

					if (copyBytesFromTFtoPDUBuffer(offsetLastTFRead - offsetCurrentTFRead)) {
						/*
						 * When pdu spans TF, keep track of where the continuation of the pdu should start. This value is
						 * compared to the DP in at the beginning of the next frame.
						 */
						pduContinuation = pduLength - pduBytesRead;

						/*
						 * Partial header has been copied. Cannot start processing pdu yet. Set state to NEED_VARAIBLE_HEADER to
						 * complete pdu header.
						 */
						machineState = State.NEED_VARIABLE_HEADER;
						if (pduExLog.isDebugEnabled()) {
							pduExLog.debug("      VARIABLE PDU HEADER SPANS TO NEXT FRAME BY " + pduContinuation + " BYTES...");
						}
						break;
					}
				}
				break;

			}

			case NEED_DATA: {
				/*
				 * This state indicates that a complete pdu header has been processed, and the data portion of the pdu is
				 * ready for processing.
				 *
				 * offsetCurrentTFRead --> first byte of pdu data (the byte after the end of the pdu header) pduBytesRead =
				 * number of bytes contained in the pduBuffer (the complete pdu header).
				 */


				/*
				 * If processing first pdu of a TF, check stateful validity of DP
				 */
				if (firstPdu) {
					/*
					 * Check for CONTINUATION.
					 */
					if (currentTFMessage.getFrameInfo().getHeader().isContinuation()) {
						pduExLog.debug("      PDU BODY CONTINUED...");

						/*
						 * The DP indicates that no pdu begins in this TF, but that all data in the TF should be appended to the
						 * pdu currently being
						 *
						 * In the NEED_DATA state, this means that all of the data in this TF should be appended to the pdu
						 * currently being built.
						 *
						 * When pdu spans TF, keep track of where the continuation of the pdu should start. This value is
						 * compared to the DP in at the beginning of the next frame.
						 */
						if (pduExLog.isDebugEnabled()) {
							pduExLog.debug("        READING CONTINUING PARTIAL PDU DATA FROM TF. pduContinuation=" + pduContinuation);
						}
						buildPduBody();

						/*
						 * Set continuation equal to whatever is left to be read of the pdu's body.
						 */
						pduContinuation = pduLength - pduBytesRead;

						/*
						 * Remain in the NEED_DATA state.
						 */
						break;
					}

					/*
					 * DP specifies start of first pdu in TF. Check for validity.
					 */
					if ((pduContinuation != dataPointer) && ((pduLength - pduBytesRead) != dataPointer)) {
						if (pduExLog.isDebugEnabled()) {
							pduExLog.debug("    XXXX  (vcfc=" + seqCount + "): NEED_DATA: DP(" + dataPointer + ") != pduContinuation(" + pduContinuation + ")");
							pduExLog.debug("          pduContinuation=" + pduContinuation + ", pduRemainingBytes=" + (pduLength - pduBytesRead) + ", bytesLeftInTF=" + (offsetLastTFRead - offsetCurrentTFRead));
						}

						/*
						 * Corrective action for invalid DP: - Send invalid pdu message (DP style). - Set
						 * offsetCurrentTFRead to dataPointer. - Set state machine to NEXT_PDU.
						 */
						if (correctFPPErrors) {
							offsetCurrentTFRead = offsetTFBody + dataPointer;
							if (pduExLog.isDebugEnabled()) {
								pduExLog.debug("          DISCARDING " + pduBytesRead + " from previous TF, and resetting 'offsetCurrentTFRead' to dataPointer(" + offsetCurrentTFRead + ")");
							}
							flushCurrentPdu(true);
							machineState = State.NEXT_PDU;
							break;
						}
					}
				}

				/*
				 * Build as much of the rest of the pdu's data as there is data left in the frame.
				 */
				buildPduBody();

				/*
				 * Set continuation equal to whatever is left to be read of the pdu's body.
				 */
				pduContinuation = pduLength - pduBytesRead;

				/*
				 * Check to see if we're done with this pdu.
				 */
				if (0 == pduContinuation) {

					/* If we have at least one byte of the next PDU header, check for the stop flag. */
					if (offsetCurrentTFRead < offsetLastTFRead && currentTFMessage.getFrame()[offsetCurrentTFRead] == STOP_PDU_FLAG) {

						if (pduExLog.isDebugEnabled()) {
							pduExLog.debug("    XXXX  (vcfc=" + seqCount + "): NEXT_PDU: PDU STOP FLAG FOUND: DISCARDING TF #" + seqCount);
						}

						/*
						 * Discard all remaining data in this
						 * transfer frame.
						 */
						offsetCurrentTFRead = offsetLastTFRead;
						machineState = State.FIRST_PDU;

					} else {

						/*
						 * PDU is completely processed within current TF. Set state to NEXT_PDU to read next pdu if it exists.
						 */
						machineState = State.NEXT_PDU;
					}
				}
				else {
					/*
					 * PDU is still incomplete because it spans into the next TF.
					 */
					if (pduExLog.isDebugEnabled()) {
						pduExLog.debug("      READ PARTIAL PDU DATA FROM TF. pduContinuation=" + pduContinuation);
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
	 * Builds onto the current PDU (i.e., builds the data block)
	 *
	 * This method may not be called unless and until the entire header of the pdu being built has been completely read
	 * from one (or more) TFs.
	 *
	 * This method requires that the contents of the pduHeader global variable has already been set.
	 * This method may be called multiple times for the same pdu.
	 */
	private void buildPduBody() {
		/*
		 * Check integrity of data structures before attempting to build a pdu.
		 */
		if (!pduHeaderValid) {
			pduExLog.error("Internal PDU Extraction Error: PDU Header is not valid when attempting to build a pdu.");
			machineState = State.NEXT_PDU;

			/*
			 * Abort further processing of current TF
			 */
			offsetCurrentTFRead = offsetLastTFRead;
			return;
		}


		/*
		 * DEBUG
		 */
		pduExLog.debug("      PROCESSING PDU BODY...");

		/*
		 * some pdu sanity checks; very mission specific
		 */
		if ( !pduHeader.isValid()) {
			sendInvalidMessage();
			machineState = State.NEXT_PDU;

			if (pduExLog.isDebugEnabled()) {
				pduExLog.debug("          PDU FAILED SANITY CHECKS");
				pduExLog.debug("            valid=" + pduHeader.isValid() + ", pduLength=" + pduLength
						               + " (pktlen > MAX_PDU_LENGTH: " + (pduLength > MAX_PDU_LENGTH) + ")");
			}
			flushCurrentPdu(false);

			/*
			 * Abort further processing of current TF
			 */
			offsetCurrentTFRead = offsetLastTFRead;
			return;
		}

		/*
		 * Keep track of number of TFs used to complete current pdu.
		 */
		pduTFs++;

		/*
		 * Check to see if there are enough bytes to complete the current pdu based upon its length.
		 */
		if (offsetCurrentTFRead + (pduLength - pduBytesRead) <= offsetLastTFRead) {
			/*
			 * PDU is complete in this TF
			 */
			machineState = State.NEXT_PDU;

			if (copyBytesFromTFtoPDUBuffer(pduLength - pduBytesRead)) {
				if (pduExLog.isDebugEnabled()) {
					pduExLog.debug("        PDU COMPLETELY PROCESSED IN " + pduTFs + " TF(s):\n\n" + pduHeader);
				}
				sendPduMessage();
				machineState = State.NEXT_PDU;
			}
		}
		else {
			/*
			 * PDU spans TF
			 */
			if (copyBytesFromTFtoPDUBuffer(offsetLastTFRead - offsetCurrentTFRead)) {

				/*
				 * When pdu spans TF, keep track of where the continuation of the pdu should start. This value is compared to
				 * the DP in at the beginning of the next frame.
				 */
				pduContinuation = (pduLength - pduBytesRead) - (offsetLastTFRead - offsetCurrentTFRead);
				if (pduExLog.isDebugEnabled()) {
					pduExLog.debug("        PDU CONTINUES IN NEXT FRAME. " + pduContinuation + " BYTES LEFT TO READ...");
				}
				machineState = State.NEED_DATA;
			}
		}
	}

	/**
	 * Copy specified number of bytes from TF to PDU Buffer
	 *
	 * @return true on success, false on failure
	 */
	private boolean copyBytesFromTFtoPDUBuffer(final int count) {
		try {
			if (pduExLog.isDebugEnabled()) {
				pduExLog.debug("        COPYING " + count + " BYTES TO PDU BUFFER...");
			}
			System.arraycopy(currentTFMessage.getFrame(), offsetCurrentTFRead, pduBuffer, pduBytesRead, count);
			offsetCurrentTFRead += count;
			pduBytesRead += count;

			/*
			 * If this is the first data put into this pdu, set the pdu's DSN info.
			 */
			if ((count > 0) && (pduBytesRead == count)) {
				/* 12/29/2011 - MPCS-2523
				 * Set ERT from first frame containing this pdu's data.
				 */
				if (pduExLog.isDebugEnabled()) {
					pduExLog.debug("          >>> ERT set to \"" + currentTFMessage.getStationInfo().getErtString() + "\" from TF vcfc=" + currentTFMessage.getFrameInfo().getSeqCount());
				}
			}
			return true;
		}
		catch (final ArrayIndexOutOfBoundsException e) {
			if (pduExLog.isDebugEnabled()) {
				pduExLog.debug("          PDU BUFFER OVERRUN (PDU SIZE EXCEEDS MAX_PDU_LENGTH): TF=" + currentTFMessage.getFrameInfo().getSeqCount() + ", offsetCurrentTFRead=" + offsetCurrentTFRead
						               + ", pduBytesRead=" + pduBytesRead + ", count=" + count);
			}

			/*
			 * Output an external log message.
			 */
			final String msg = "PDU buffer overrun. (PDU size exceeds MAX_PDU_LENGTH): vcfc=" + currentTFMessage.getFrameInfo().getSeqCount() + ", offsetCurrentTFRead=" + offsetCurrentTFRead
					+ ", pduBytesRead=" + pduBytesRead + ", count=" + count;
			final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
			                                                                                 LogMessageType.INVALID_PKT_DATA);
			pduExLog.log(logm);
			numberOfPdusInvalid++;

			if (pduExLog.isDebugEnabled()) {
				pduExLog.debug(e);
			}
			flushCurrentPdu(false);

			/*
			 * Abort further processing of current TF
			 */
			offsetCurrentTFRead = offsetLastTFRead;
			return false;
		}
	}

	/**
	 * Build the fixed portions of the pdu header from the current values in pduBuffer.
	 */
	private void buildPduHeader() {
		pduHeader = pduFactory.createPduHeader();
		pduHeader.loadFixedHeader(pduBuffer, 0);
		pduLength = pduHeader.getHeaderLength() + pduHeader.getDataLength();
		if (pduExLog.isDebugEnabled()) {
			pduExLog.debug("FIXED PDU HEADER DUMP: " + pduHeader);
		}
	}


	/**
	 * Build the variable pdu header from the current values in pduBuffer.
	 * Sets the global 'pduHeaderValid' flag to true.
	 */
	private void populateVariablePduHeader() {
		pduHeader.load(pduBuffer, 0);
		pduLength = pduHeader.getHeaderLength() + pduHeader.getDataLength();
		pduHeaderValid = true;
		if (pduExLog.isDebugEnabled()) {
			pduExLog.debug("VARIABLE PDU HEADER DUMP: " + pduHeader);
		}
	}

	/**
	 * Clear any current pdu, statistics, and bookkeeping that may have been in progress.
	 */
	private void resetCurrentPdu() {
		pduHeaderValid = false;
		pduHeader = null;
		pduLength = 0;
		pduBytesRead = 0;
		pduTFs = 0;
		pduContinuation = 0;
		this.pduFrames.clear();
		sourceVcid = -1;
		sourceDssId = 0;
		Arrays.fill(pduBuffer, (byte) 0);

	}

	/**
	 * flushCurrentPdu() is called when some condition means we discard the current pdu that we are building.
	 */
	private void flushCurrentPdu(final boolean dataPointer) {
		if (pduBytesRead > 0) {
			if (machineState == State.NEED_FIXED_HEADER || machineState == State.NEED_VARIABLE_HEADER) {
				if (pduExLog.isDebugEnabled()) {
					pduExLog.debug("\n\n*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n" + "<<< NO HEADER >>>\n"
							               + "*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n");
				}
				if (dataPointer) {
					sendInvalidPduMessageFPP();
				}
				else {
					sendInvalidPduMessage();
				}
			}
			else if (machineState == State.NEED_DATA) {
				if (pduExLog.isDebugEnabled()) {
					pduExLog.debug("\n\n*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n" + pduHeader
							               + "*** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU *** FLUSHING PDU ***\n");
				}
				if (dataPointer) {
					sendInvalidDataMessageFPP();
				}
				else {
					sendInvalidDataMessage();
				}
			}
		}

		/*
		 * Clear any current pdu statistics and bookkeeping that may have been in progress.
		 */
		resetCurrentPdu();
	}

	/**
	 * Publish pdu as a pdu message, reset for next pdu
	 */
	private void sendPduMessage() {

		final ICfdpPdu pdu = pduFactory.createPdu(pduHeader, pduBuffer);

		// MPCS-9950 - 07/16/18 - Pass frame objects and context
		final ICfdpPduMessage pduM = cfdpMsgFactory.createPduMessage(this.pduFrames, pdu, contextConfig);

		try {
			bus.publish(pduM);
			pduExLog.debug(pduM);
		}
		catch (final Exception e) {
			pduExLog.error("Error processing valid pdu: " + pdu.toString(), e);
		}
		resetCurrentPdu();
	}

	/**
	 * Send invalid pdu; currently just log a message
	 */
	private void sendInvalidPduMessageFPP() {
		final String msg = "A PDU header could not be constructed because of an invalid DP, " + "VC=" + allowedVcid + ", ERT=" + currentTFMessage.getStationInfo().getErtString() + ", frame VCFC="
				+ counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
		                                                                                 LogMessageType.INVALID_PKT_HEADER);
		pduExLog.log(logm);
		numberOfPdusInvalid++;
	}

	/**
	 * Send invalid data, currently just log a message
	 */
	private void sendInvalidDataMessageFPP() {
		final int pduLength = pduHeader.getHeaderLength() + pduHeader.getDataLength() + 1;
		final String msg = "All data for a PDU could not be found because of an invalid DP, " + "VC=" + allowedVcid + ", isValid=" + pduHeader.isValid() + ", length="
				+ pduLength + ", ERT=" + currentTFMessage.getStationInfo().getErtString() + ", frame VCFC="
				+ counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
		                                                                                 LogMessageType.INVALID_PKT_DATA);
		pduExLog.log(logm);
		numberOfPdusInvalid++;
	}

	/**
	 * Send invalid pdu; currently just log a message
	 */
	private void sendInvalidPduMessage() {
		final String msg = "A PDU header could not be constructed because of a Transfer Frame Gap, " + "VC=" + allowedVcid + ", ERT=" + currentTFMessage.getStationInfo().getErtString()
				+ ", frame VCFC=" + counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
		                                                                                 LogMessageType.INVALID_PKT_HEADER);
		pduExLog.log(logm);
		numberOfPdusInvalid++;
	}

	/**
	 * Send invalid data, currently just log a message
	 */
	private void sendInvalidDataMessage() {
		final int pduLength = pduHeader.getHeaderLength() + pduHeader.getDataLength() + 1;
		final String msg = "All data for a PDU could not be found because of a Transfer Frame Gap, " + "VC=" + allowedVcid + ", isValid=" +
				pduHeader.isValid()
				+ ", length=" + pduLength + ", ERT=" + currentTFMessage.getStationInfo().getErtString()
				+ ", frame VCFC=" + counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead;
		final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN, msg,
		                                                                                 LogMessageType.INVALID_PKT_DATA);
		pduExLog.log(logm);
		numberOfPdusInvalid++;
	}


	/**
	 * Send bad frame message.
	 *
	 */
	private void sendBadFrameMessage(final ITelemetryFrameMessage tfm) {
		pduExLog.debug("Bad transfer frame");
		final IFrameEventMessage bfm = frameMsgFactory.createBadFrameMessage(tfm.getStationInfo(), tfm.getFrameInfo());
		numberOfTFBad++;
		pduExLog.log(bfm);
	}

	/**
	 * Send message indicating repeated frame.
	 *
	 * @param tfm
	 *            the current TransferFrameMessage
	 */
	private void sendRepeatedFrameMessage(final ITelemetryFrameMessage tfm) {
		pduExLog.debug("Repeated transfer frame");
		final IFrameSequenceAnomalyMessage logm = frameMsgFactory.createFrameSequenceAnomalyMessage(tfm.getStationInfo(),
		                                                                                            tfm.getFrameInfo(), LogMessageType.TF_REPEAT,
		                                                                                            ((counterVirtualFrame + 1) % (tfm.getFrameInfo().getMaxSeqCount() + 1)),
		                                                                                            tfm.getFrameInfo().getSeqCount());
		numberOfTFRepeats++;
		pduExLog.log(logm);
	}

	/**
	 * Send message indicating bad pdu.
	 *
	 */
	private void sendInvalidMessage() {
		final int pduLength = pduHeader.getHeaderLength() + pduHeader.getDataLength() + 1;
		final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN,
		                                                                                 "PDU abandoned because header was corrupt, "
				                                                                                 + "VC=" + allowedVcid + ", isValid="
				                                                                                 + pduHeader.isValid() + ", length=" + pduLength + ", ERT="
				                                                                                 + currentTFMessage.getStationInfo().getErtString() + ", frame VCFC=" + counterVirtualFrame + ", frame offset=" + offsetCurrentTFRead,
		                                                                                 LogMessageType.INVALID_PDU_HEADER);
		numberOfPdusInvalid++;
		pduExLog.log(logm);
	}

	/**
	 *
	 * PduExtractMessageSubscriber is the listener for internal frame and related messages.
	 *
	 *
	 */
	private class PduExtractMessageSubscriber extends BaseMessageHandler {

		/**
		 * Creates an instance of PduExtractMessageSubscriber.
		 */
		public PduExtractMessageSubscriber() {
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
				flushCurrentPdu(false);

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
