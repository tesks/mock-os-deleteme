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

package jpl.gds.tc.legacy.impl.session;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.exception.*;
import jpl.gds.tc.api.frame.ITcTransferFrameFactory;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.SessionLocationType;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.session.ISessionBuilder;

/**
 * The building of an uplink session is a frame-level operation.  This class accepts telecommand data
 * frames as input and builds the resulting session by adding delimiter frames where needed and setting
 * the sequence number of each data frames.
 *
 *
 */
public class SessionBuilder implements ISessionBuilder
{
	/** Logging interface */
	protected final Tracer trace; 

	
	/**
	 * The list of data frames in this session
	 */
	private final List<ITcTransferFrame> frameList;

	/**
	 * The list of commands used to build this session
	 */
	private final List<IFlightCommand> commandList;

	private final ApplicationContext appContext;
    final ITcTransferFrameFactory frameFactory;
    final ITewUtility legacyTewUtility;

	final CommandFrameProperties frameConfig;

	private ITcTransferFrameSerializer serializer;
	
	/**
	 * Creates an instance of SessionBuilder.
	 * 
	 * @param appContext
	 *            the application context in which this class is being
	 *            instantiated
	 */
	public SessionBuilder(final ApplicationContext appContext) {
		this.appContext = appContext;
        this.trace = TraceManager.getDefaultTracer(appContext);
		this.frameList = new ArrayList<>(MAX_SESSION_SIZE);
		this.commandList = new ArrayList<>(MAX_SESSION_SIZE);

        frameFactory = appContext.getBean(ITcTransferFrameFactory.class);
        legacyTewUtility = appContext.getBean(TcApiBeans.LEGACY_TEW_UTILITY, ITewUtility.class);

		frameConfig = appContext.getBean(CommandFrameProperties.class);

		// MPCS-11509 - use legacy frame serializer
		serializer = appContext.getBean(TcApiBeans.LEGACY_COMMAND_FRAME_SERIALIZER, ITcTransferFrameSerializer.class);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void addFrame(final ITcTransferFrame inputFrame) {
		if(inputFrame == null) {
			throw new IllegalArgumentException("Input frame was null.");
		}

		this.frameList.add(inputFrame);
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void addCommand(final IFlightCommand command) throws BlockException, CommandParseException, CommandFileParseException {
		if(command == null) {
			throw new IllegalArgumentException("Null input command");
		}

        this.frameList.add(frameFactory.createCommandFrame(command));
		this.commandList.add(command);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void addFrames(final List<ITcTransferFrame> frames)
	{
		this.frameList.addAll(frames);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<ITcTransferFrame> getSessionFrames() throws SessionOverflowException {
		
		//Read the configured values for session construction
		final int repeatCount = frameConfig.getSessionRepeatCount();

		//worst case scenario for one session is 2 delimiters for each data frame (session size * 3)
		//(the repeat count, however, can cause us to make N sessions)
		final List<ITcTransferFrame> sessionFrames = new ArrayList<ITcTransferFrame>(MAX_SESSION_SIZE*3);
		final List<ITcTransferFrame> dataFrames = new ArrayList<ITcTransferFrame>(MAX_SESSION_SIZE);

		boolean firstSessionFrame = false;
		byte firstSessionFrameType = -1;

		//get the locations of delimiter frames in the session
		final SessionLocationType overrideBeginSession = frameConfig.getOverrideBeginType();
		final SessionLocationType overrideEndSession = frameConfig.getOverrideEndType();

		//construct a single session (ignoring the repeat count for now)
		short sequenceCounter = 0;
		for(int i=0; i < this.frameList.size(); i++) {
			final ITcTransferFrame dataFrame = this.frameList.get(i);
			final VirtualChannelType vct = VirtualChannelType.getTypeFromNumber(frameConfig, dataFrame.getVirtualChannelNumber());

			SessionLocationType beginUplinkSession = overrideBeginSession;
			if(beginUplinkSession == null) {
				beginUplinkSession = frameConfig.getBeginSession(vct);
			}

			SessionLocationType endUplinkSession = overrideEndSession;
			if(endUplinkSession == null) {
				endUplinkSession = frameConfig.getEndSession(vct);
			}
			
			// MPCS-8534 01/13/17 - put a begin delimiter frame before the data
			//check if there's a delimiter that's supposed to show up before the next data frame
			//and if so, add it in
            // final int scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();

			switch(beginUplinkSession) {
				case FIRST:
					if(i == 0) {
						if (!dataFrames.isEmpty()) {
							for(int a=0; a <= repeatCount; a++) {
								sessionFrames.addAll(dataFrames);
							}
							dataFrames.clear();
						}
						sessionFrames.add(frameFactory.createBeginDelimiterFrame());
						sequenceCounter = 0;
						firstSessionFrame = true;
					}
					break;

				case ALL:
					if (!dataFrames.isEmpty()) {
						for(int a=0; a <= repeatCount; a++) {
							sessionFrames.addAll(dataFrames);
						}
						dataFrames.clear();
					}
					sessionFrames.add(frameFactory.createBeginDelimiterFrame());
					firstSessionFrame = true;
					sequenceCounter = 0;
					break;

				case NONE:
				default:
					if(i == 0) {
						firstSessionFrame = true;
						sequenceCounter = 0;
					}
					break;
			}

			//add the data frame and check its sequence number
			if(vct != null && frameConfig.hasSequenceCounter(vct)) {
				if(sequenceCounter == MAX_SESSION_SIZE) {
					throw new SessionOverflowException("The sequence counter has determined that there are already " + sequenceCounter + " frames in the current uplink session, which is " +
							"the maximum allowable size of an uplink session, but another frame of type VC-" + dataFrame.getVirtualChannelNumber() + " was attempted to be added to the uplink session.");
				}
				dataFrame.setSequenceNumber(sequenceCounter);
				sequenceCounter++;

				//have to recalculate FECF since we've changed data...
				if(frameConfig.hasFecf(vct)) {
					dataFrame.setFecf(serializer.calculateFecf(dataFrame, frameConfig.getChecksumCalcluator(dataFrame.getVirtualChannelNumber())));
				}
			} else {
				dataFrame.setSequenceNumber((short)0);
			}

			if(firstSessionFrame == true) {
				firstSessionFrame = false;
				firstSessionFrameType = dataFrame.getVirtualChannelNumber();
			} else if(dataFrame.getVirtualChannelNumber() != firstSessionFrameType) {
				trace.warn("Only one virtual channel type is allowed within an uplink session, but the current configuration created a scenario where "
						+ " the first transfer frame in the session was a VC-" + firstSessionFrameType + " frame, but the frame with sequence number "
						+ sequenceCounter + " in the same session was a VC-" + dataFrame.getVirtualChannelNumber() + " frame.  Check your input "
						+ " and check the uplink session-related configuration settings. This may result in undesirable behavior by the receiving software.");
			}

			dataFrames.add(dataFrame);

			// MPCS-8534 01/13/17 - put and end delimiter frame after the data
			//check if there's a delimiter that's supposed to show up after the data frame
			//and if so, add it in
			switch(endUplinkSession) {
				case LAST:
					if(i == this.frameList.size()-1) {
						if(dataFrames.size() > 0) {
							for(int a=0; a <= repeatCount; a++) {
								sessionFrames.addAll(dataFrames);
							}
							dataFrames.clear();
						}
						sessionFrames.add(frameFactory.createEndDelimiterFrame());
						sequenceCounter = 0;
						firstSessionFrame = true;
					}
					break;

				case ALL:
					if(dataFrames.size() > 0) {
						for(int a=0; a <= repeatCount; a++) {
							sessionFrames.addAll(dataFrames);
						}
						dataFrames.clear();
					}
					sessionFrames.add(frameFactory.createEndDelimiterFrame());
					sequenceCounter = 0;
					firstSessionFrame = true;
					break;

				case NONE:
				default:
					break;
			}
		}

		//now that a single session is built, add in multiple copies of the session,
		//once for each repeat count
		if(dataFrames.size() > 0) {
			for(int a=0; a <= repeatCount; a++) {
				sessionFrames.addAll(dataFrames);
			}
			dataFrames.clear();
		}

		return(sessionFrames);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		this.frameList.clear();
		this.commandList.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<IFlightCommand> getCommandList() {
		return commandList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return(this.frameList.isEmpty());
	}
}