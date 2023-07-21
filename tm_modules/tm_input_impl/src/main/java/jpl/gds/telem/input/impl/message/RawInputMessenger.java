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
package jpl.gds.telem.input.impl.message;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.telem.input.api.InternalTmInputMessageType;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.api.message.ITelemetryInputMessageFactory;
import jpl.gds.telem.input.api.message.ITelemetrySummaryMessage;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * A singleton class that handles messaging during raw input acquisition.
 * 
 */
public class RawInputMessenger {
	private static final String ME = "RawInputMessenger ";
    private final Tracer                        logger;
	private final TelemetryInputProperties rawConfig;
	private final IMessagePublicationBus context;
	private ITelemetrySummaryMessage summaryMessage;
	private long dataFlowTimeout;
	private long summaryInterval;
	private Timer summaryTimer;
	private final ITelemetryInputMessageFactory inputMsgFactory;
	
	// MPCS-8083  06/06/16 - Added to intercept BufferedRawInputStream messages
	RawMessageSubscriber rms = new RawMessageSubscriber();

	private final TelemetryConnectionType connectionType;
    private final IFrameMessageFactory frameMsgFactory;
    private final IStatusMessageFactory statusMsgFactory;
    private final SseContextFlag                sseFlag;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public RawInputMessenger(final ApplicationContext appContext) {

	    this.context = appContext.getBean(IMessagePublicationBus.class);
	    this.rawConfig = appContext.getBean(TelemetryInputProperties.class);
	    this.connectionType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getDownlinkConnectionType();
	    this.inputMsgFactory = appContext.getBean(ITelemetryInputMessageFactory.class);
	    this.frameMsgFactory = appContext.getBean(IFrameMessageFactory.class);
	    this.statusMsgFactory = appContext.getBean(IStatusMessageFactory.class);
        this.logger = TraceManager.getTracer(appContext, Loggers.TLM_INPUT);
        this.sseFlag = appContext.getBean(SseContextFlag.class);
	    
	    reset();

	}

	private void init() {
		summaryMessage = inputMsgFactory.createInputSummaryMessage();
		summaryInterval = rawConfig.getRawSummaryTimerInterval();

        dataFlowTimeout = rawConfig.getDataFlowTimeout(connectionType);
        logger.log(summaryMessage);
		
		// MPCS-8083  06/06/16 - subscribe to buffer messages
		context.subscribe(InternalTmInputMessageType.LoggingDiskBackedBufferedInputStream, rms);

		startSummaryTimer();
	}
	
	/**
     * Stops the instance.
     */
    public void stop() {
        stopSummaryTimer();
    }

	/**
	 * Resets the instance.
	 */
	public void reset() {
		stopSummaryTimer();
		init();
	}

	private void startSummaryTimer() {
	    if (summaryInterval > 0) {
	        /* MPCS-7135 - 3/17/15. Name the timer thread. */
            summaryTimer = new Timer("Status Publisher");
	        summaryTimer.scheduleAtFixedRate(new TimerTask() {
	            @Override
	            public void run() {
	                sendSummaryMessage();
	            }
	        }, summaryInterval, summaryInterval);
	    }
	}

	private void stopSummaryTimer() {
		if (summaryTimer != null) {
			summaryTimer.cancel();
		}
		summaryTimer = null;
	}

	/**
	 * Should be called by the Raw Input Adapter each time is does a successful
	 * read of data from the input source. Increments the summary read count,
	 * flowing indicator, and last start time.
	 */
	public void incrementReadCount() {
		summaryMessage.incrementReadCount();
	}
	
	/**
	 * Sets the current telemetry source connection state.
	 * 
	 * @param state true if connected, false if not
	 */
	public void setConnected(final boolean state) {
	    summaryMessage.setConnected(state);
	}
	
	/**
	 * Sets the current telemetry data flowing state.
	 * 
	 * @param state true if flowing, false if not
	 */
	public void setFlowing(final boolean state) {
	    summaryMessage.setFlowing(state);
	}

	/**
	 * Returns the summary read count.
	 *
	 * @return summary read count
	 */
	public long getReadCount() {
		return summaryMessage.getReadCount();
	}
	
	/**
	 * Send a start of data message.
	 */
	public void sendStartOfDataMessage() {
		final IPublishableLogMessage sodm = statusMsgFactory.createStartOfDataMessage();
		context.publish(sodm);
        logger.log(sodm);
		summaryMessage.setFlowing(true);
		sendSummaryMessage();
	}

	/**
	 * Send an end of data message.
	 */
	public void sendEndOfDataMessage() {
		final IPublishableLogMessage eodm = statusMsgFactory.createEndOfDataMessage();
		context.publish(eodm);
        logger.log(eodm);
	}

	/**
	 * Sends a summary message containing the status of raw input acquisition
	 * and processing
	 */
	public void sendSummaryMessage() {
		final Date lastRead = summaryMessage.getLastDataReadTime();
		if (lastRead != null) {
			final long lastReadTime = lastRead.getTime();
			final long curTime = System.currentTimeMillis();
			if (dataFlowTimeout != 0
			        && curTime - lastReadTime > dataFlowTimeout) {
				summaryMessage.setFlowing(false);
			} else {
				summaryMessage.setFlowing(true);
			}
		} else {
			summaryMessage.setFlowing(false);
		}
		summaryMessage.setEventTime(new AccurateDateTime());
		context.publish(summaryMessage);
        logger.log(summaryMessage);

		final ITelemetrySummaryMessage old = summaryMessage;
		summaryMessage = inputMsgFactory.createInputSummaryMessage();
        summaryMessage.setFromSse(sseFlag.isApplicationSse());
		summaryMessage.setReadCount(old.getReadCount());
		summaryMessage.setLastDataReadTime(old.getLastDataReadTime());
		summaryMessage.setConnected(old.isConnected());
		summaryMessage.setFlowing(old.isFlowing());
	}

	/**
	 * Sends a connect message
	 * 
	 * @param connectionString the connect string that describes the connection
	 */
    public void sendConnectMessage(final String connectionString) {
		if (connectionString == null) {
		    /* MPCS-7993 - 3/30/16. Removed throw. Sometimes this
		     * caused unit tests to fail owing to timing and other issues.
		     * There is really no issue with just not sending the message;
		     */
			return;
		} 

		final IPublishableLogMessage cm = statusMsgFactory.createConnectMessage(connectionString);
		context.publish(cm);
		summaryMessage.setConnected(true);
        logger.log(cm);
		sendSummaryMessage();
	}

	/**
	 * Sends a pause message to notify the system that raw input has been paused
	 * 
	 * @param pauseTime the time raw input was paused
	 */
	public void sendPauseMessage(final Date pauseTime) {
		final IPublishableLogMessage m = statusMsgFactory.createPauseMessage(pauseTime);
        logger.debug("Pause Processing Input: " , m.getMessage());
		context.publish(m);
        logger.log(m);
	}

	/**
	 * Sends a resume message to notify the system that raw input has been
	 * resume
	 * 
	 * @param pauseTime the time raw input was last paused
	 */
	public void sendResumeMessage(final Date pauseTime) {
	    final IPublishableLogMessage m = statusMsgFactory.createResumeMessage(pauseTime);
        logger.debug("Resume Processing Input: " , m.getMessage());
		context.publish(m);
        logger.log(m);
	}

	/**
	 * Sends a stop message to notify the system that raw input has been stopped
	 */
	public void sendStopMessage() {
	    final IPublishableLogMessage m = statusMsgFactory.createStopMessage(new AccurateDateTime());
        logger.debug("Stop Processing Input: " , m.getMessage());
		context.publish(m);
        logger.log(m);
		stopSummaryTimer();
	}

	/**
	 * Sends a disconnect message to notify the system that the connection to
	 * the raw input source has been disconnected
	 * 
	 * @param connectionString the string that describes the connection
	 */
	public void sendDisconnectMessage(final String connectionString) {
		if (connectionString == null) {
			// MPCS-3702. There never was a connection. Used to throw.
			return;
		}

		final IPublishableLogMessage cm = statusMsgFactory.createDisconnectMessage(connectionString);
        // MPCS-9396 - 01/31/18 - moved the sendSummaryMessage call up, to print it before the disconnect message.
        sendSummaryMessage();
		context.publish(cm);
		summaryMessage.setConnected(false);
		summaryMessage.setFlowing(false);
        logger.log(cm);
	}

	/**
	 * Sends a loss of sync message to notify the system that the raw input has
	 * reach an out of sync state.
	 * 
	 * @param dsnInfo the <code>DSNInfo</code> for the chunk of telemetry that is out of sync
	 * @param reason the reason that it is out of sync
	 * @param lastTfInfo the last processed transfer frame info
	 * @param lastFrameErt the ERT of the last processed transfer frame
	 */
	public void sendLossOfSyncMessage(final IStationTelemInfo dsnInfo, final String reason,
	        final ITelemetryFrameInfo lastTfInfo, final IAccurateDateTime lastFrameErt) {
		final IFrameEventMessage msg = frameMsgFactory.createLossOfSyncMessage(dsnInfo, lastTfInfo, reason, lastFrameErt);
		context.publish(msg);
        logger.log(msg);
	}

	/**
	 * Send out of sync bytes message. The message contains the bytes that are
	 * out of sync
	 * 
	 * @param dsnInfo the <code>DSNInfo</code> for the chunk of out of sync telemetry
	 * @param data the telemetry data
	 */
	public void sendOutOfSyncBytesMessage(final IStationTelemInfo dsnInfo, final byte[] data) {
		final IOutOfSyncDataMessage msg = frameMsgFactory.createOutOfSyncMessage(dsnInfo, data);
		context.publish(msg);
        logger.log(msg);
	}

	/**
	 * Send an in sync message.
	 * 
	 * @param dsnInfo The DSN information to put in the message
	 * @param tfInfo The <code>ITelemetryFrameInfo</code> to put in the message
	 */
	public void sendInSyncMessage(final IStationTelemInfo dsnInfo, final ITelemetryFrameInfo tfInfo) {
		logger.debug(ME , "Sending in sync message");
		if (dsnInfo == null) {
			throw new IllegalArgumentException("Null input DSN Info");
		} else if (tfInfo == null) {
			throw new IllegalArgumentException("Null input TF Info");
		}
		final IFrameEventMessage frameMsg = frameMsgFactory.createInSyncMessage(dsnInfo, tfInfo);
		context.publish(frameMsg);
        logger.log(frameMsg);
	}

	/**
	 * Sends a bad frame message to notify the system of a bad frame
	 * @param dsnInfo the <code>DSNInfo</code> of the bad frame
	 * @param tfI the <code>ITelemetryFrameInfo</code> of the bad frame
	 * @param message the message
	 */
	public void sendBadFrameMessage(final IStationTelemInfo dsnInfo,
	        final ITelemetryFrameInfo tfI, final String message) {
        logger.debug(ME , message);
		final IFrameEventMessage msg = frameMsgFactory.createBadFrameMessage(dsnInfo, tfI);
        context.publish(msg);
        logger.log(msg);
	}
	
	
	// MPCS-8083  06/06/16 - Added to handle messages RawInputMessenger subscribed to.
	private class RawMessageSubscriber implements MessageSubscriber{

		@Override
		public void handleMessage(final IMessage message) {
		    if (message.isType(InternalTmInputMessageType.LoggingDiskBackedBufferedInputStream)) {
				summaryMessage.setBuffereredRawInputStreamInfo(message.getOneLineSummary());
		    }
		}
		
	}
}
