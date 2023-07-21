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
package jpl.gds.tm.service.impl.frame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.filtering.ScidFilter;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.EncodingSummaryRecord;
import jpl.gds.tm.service.api.frame.FrameSummaryRecord;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;
import jpl.gds.tm.service.api.frame.IFrameTrackingService;
import jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import jpl.gds.tm.service.config.TelemetryServiceProperties;

/**
 * FrameMeter keeps track of statistics related to frames.
 *
 *
 * MPCS-9396 - 01/28/18 - Added finalSummaryMessage sent flag and EndOfDataMessageSubscriber to prevent
 *          the last summary message from being displayed after data source is disconnected
 */
public class FrameTrackingService implements IService, IFrameTrackingService {
    
    private final IMessagePublicationBus messageContext;
    private long validFrames;
    private long idleFrames;
    private long badFrames;
    private long deadFrames;
    private long outOfSyncBytes;
    private long outOfSyncCount;
    private long dataByteCount;
    private boolean inSync;
    private long startTime;
    private long stopTime;
    private final long summaryInterval;
    private Timer summaryTimer;
    private boolean started;
    private boolean finalSummaryMessageSent;
    private final Map<String, FrameSummaryRecord> summaryMap = new HashMap<String, FrameSummaryRecord>();
    private final Map<String, EncodingSummaryRecord> encodingMap = new HashMap<String, EncodingSummaryRecord>();
    private SyncSubscriber syncSubscriber;
    private FrameSubscriber frameSubscriber;
    private EndOfDataMessageSubscriber endSubscriber;
    private List<Integer> vcidList = new ArrayList<Integer>(1);
    private final ScidFilter                         filterScid;
    private final Integer filterDss;
    private double bitrate;
    private final IFrameMessageFactory frameMsgFactory;
    private final Tracer tracer;
    private final IStatusMessageFactory statusMessageFactory;
    private final MissionProperties missionProps;
 
    /**
     * Creates an instance of FrameTrackingService. 
     * @param serviceContext the current application context
     */
    public FrameTrackingService(final ApplicationContext serviceContext) {

    	filterDss = serviceContext.getBean(IContextFilterInformation.class).getDssId();
    	final Integer filterVcid = serviceContext.getBean(IContextFilterInformation.class).getVcid();
    	final TelemetryServiceProperties config = serviceContext.getBean(TelemetryServiceProperties.class);
        this.missionProps = serviceContext.getBean(MissionProperties.class);
        this.frameMsgFactory = serviceContext.getBean(IFrameMessageFactory.class);
        this.statusMessageFactory = serviceContext.getBean(IStatusMessageFactory.class);
    	summaryInterval = config.getFrameTrackingReportInterval();

    	/* MPCS-7993 - 3/30/16. Change fetch of packet extract VCID list
    	 * to fetch the all valid VCID list in the mission properties. There
    	 * is no reason to only report statistics on frames from which we
    	 * extract packets, or to report frames as bad just because they
    	 * do not contain packets.
    	 */
    	vcidList = missionProps.getAllDownlinkVcids();
    	if (filterVcid == null) {
    		vcidList = missionProps.getAllDownlinkVcids();
    		if (vcidList.isEmpty()) {
    			vcidList.add(0);
    		}
    	} else {
    		vcidList.add(filterVcid);
    	}

        /**  MPCS-9203 - 11/9/17. Add SCID filtering */
        filterScid = new ScidFilter(Arrays.asList(UnsignedInteger.valueOf(serviceContext.getBean(IContextIdentification.class)
                                                                                        .getSpacecraftId())));

        messageContext = serviceContext.getBean(IMessagePublicationBus.class);
        tracer = TraceManager.getTracer(serviceContext, Loggers.TRACKING);
        final TelemetryInputType inputType = serviceContext.getBean(IConnectionMap.class).getDownlinkConnection().getInputType();
        inSync = !inputType.needsFrameSync();
    }
    
    private boolean isDefinedVcid(final int vcid) {
        return vcidList.contains(vcid);
    }
    
    private boolean isSessionDss(final int dssid) {
       if (filterDss == null || dssid == 0) { 
           return true;
       }
       return filterDss == dssid;
    }
    
    /**
     * Send unrecognized VCID Log message.
     */
    private void sendUnrecognizedVcidMessage(final ITelemetryFrameMessage tfm) {
        final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARNING,
                "Unconfigured frame VCID: " + tfm.getFrameInfo().getVcid() + 
                ", ERT=" + tfm.getStationInfo().getErtString() + 
                ", VCFC=" +tfm.getFrameInfo().getSeqCount() + ". Frame will not be processed.");
        messageContext.publish(logm);
        tracer.log(logm);
    }
    
    /**
     * Send unrecognized station Log message.
     */
    private void sendUnrecognizedDssMessage(final ITelemetryFrameMessage tfm) {
        final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARNING,
                "Unconfigured frame DSS ID: " + tfm.getStationInfo().getDssId() + 
                ", ERT=" + tfm.getStationInfo().getErtString() + 
                ", VCFC=" + tfm.getFrameInfo().getSeqCount() + ". Frame will not be processed.");
        messageContext.publish(logm);
        tracer.log(logm);
    }
    
    /**
     * Send unrecognized scid log message
     * 
     * @param ITelemetryFrameMessage
     */
    private void sendUnrecognizedScidMessage(final ITelemetryFrameMessage tfm) {
        final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.WARN,
                "Unconfigured frame SCID : " + tfm.getScid() 
                + ", ERT=" + tfm.getStationInfo().getErtString()
                + ", VCFC=" + tfm.getFrameInfo().getSeqCount()
                + ". Frame will not be processed.");
        messageContext.publish(logm);
        tracer.log(logm);
    }

    private void startSummaryTimer() {
        if (!started) {
            if (summaryInterval > 0) {
                /* MPCS-7135 - 3/17/15. Name the timer thread */
                summaryTimer = new Timer("Frame Summary");
                summaryTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        sendSummaryMessage();
                    }
                }, summaryInterval, summaryInterval);
            }
            started = true;
        }
    }
  
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
	public boolean startService() {
        startTime = System.currentTimeMillis();
        if (summaryTimer != null) {
            summaryTimer.cancel();
            summaryTimer = null;
        }
        summaryMap.clear();
        encodingMap.clear();
        frameSubscriber = new FrameSubscriber();
        syncSubscriber = new SyncSubscriber();
        endSubscriber = new EndOfDataMessageSubscriber();
        return true;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
	public void stopService() {
    	if (frameSubscriber != null) {
    		messageContext.unsubscribeAll(frameSubscriber);
    		frameSubscriber = null;
    	}
    	if (syncSubscriber != null) {
    		messageContext.unsubscribeAll(syncSubscriber);
    		syncSubscriber = null;
    	}
        if (endSubscriber != null) {
            messageContext.unsubscribeAll(endSubscriber);
            endSubscriber = null;
        }
        stopTime = System.currentTimeMillis();
        if (!finalSummaryMessageSent) {
            sendSummaryMessage();
            finalSummaryMessageSent = true;
        }
        if (summaryTimer != null) {
            summaryTimer.cancel();
            summaryTimer = null;
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getDataMbps()
     */
    @Override
    public float getDataMbps() {
        long end = stopTime;
        if (end == 0L) {
            end = System.currentTimeMillis();
        }
        return (8.0f * dataByteCount) / (1000.0f * (end - startTime));
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getFramesPerSecond()
     */
    @Override
    public float getFramesPerSecond() {
        long end = stopTime;
        if (end == 0L) {
            end = System.currentTimeMillis();
        }
        return (1000.0f * (validFrames + idleFrames + deadFrames + badFrames)) / (end - startTime);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getDataByteCount()
     */
    @Override
    public long getDataByteCount() {
        return dataByteCount;
    }
    
    private void sendSummaryMessage() {
    	/* MPCS-7348 - 5/18/15. Include bitrate. */
    	final IFrameSummaryMessage m = frameMsgFactory.createFrameSummaryMessage(inSync, 
    			validFrames, 
    			dataByteCount, outOfSyncBytes, outOfSyncCount,
    			idleFrames, deadFrames, badFrames, bitrate);
        
        m.setFrameSummaryMap(summaryMap);
        m.setEncodingSummaryMap(encodingMap);
        messageContext.publish(m);
        tracer.log(m);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getBadFrames()
     */
    @Override
    public long getBadFrames() {
        return badFrames;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getDeadFrames()
     */
    @Override
    public long getDeadFrames() {
        return deadFrames;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getIdleFrames()
     */
    @Override
    public long getIdleFrames() {
        return idleFrames;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getOutOfSyncBytes()
     */
    @Override
    public long getOutOfSyncBytes() {
        return outOfSyncBytes;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getValidFrames()
     */
    @Override
    public long getValidFrames() {
        return validFrames;
    }

	/**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameTrackingService#getOutOfSyncCount()
     */
	@Override
    public long getOutOfSyncCount() {
		return outOfSyncCount;
	}
	
	/**
	 * Subscriber for Transfer Frame messages.
	 *
	 */
	public class FrameSubscriber implements MessageSubscriber {
		/**
		 * Creates a new FrameSubscriber
		 */
		public FrameSubscriber() {
			messageContext.subscribe(TmServiceMessageType.TelemetryFrame, this);
		}
		
		/**
		 * @{inheritDoc}
		 * @see jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared.message.IMessage)
		 */
		@Override
		public void handleMessage(final IMessage m) {
            startSummaryTimer();
            final ITelemetryFrameMessage tfm = (ITelemetryFrameMessage)m;
            bitrate = tfm.getStationInfo().getBitRate();
            String key = tfm.getFrameInfo().getVcid() + "/" +  tfm.getFrameInfo().getType();
            FrameSummaryRecord sum = summaryMap.get(key);
            if (sum == null) {
            	sum = new FrameSummaryRecord( tfm.getFrameInfo().getType(), tfm.getFrameInfo().getSeqCount(),
            			tfm.getStationInfo().getErt(), tfm.getFrameInfo().getVcid(), 1);
            	summaryMap.put(key, sum);
            } else {

                sum.setCount(1 + sum.getCount());
                sum.setLastErt(tfm.getStationInfo().getErt()); 
                sum.setSequenceCount(tfm.getFrameInfo().getSeqCount());
            }

            key = tfm.getFrameInfo().getVcid() + "/" +  tfm.getFrameInfo().getFrameFormat().getEncoding();
            
            EncodingSummaryRecord enc = encodingMap.get(key);
            
            if (enc == null) {
            	enc = new EncodingSummaryRecord(tfm.getFrameInfo().getFrameFormat().getEncoding(), tfm.getFrameInfo().getVcid(), 0);
            	encodingMap.put(key, enc);
            }

            enc.setInstanceCount(enc.getInstanceCount() + 1);
            enc.setLastErt(tfm.getStationInfo().getErt());
            enc.setLastSequence(tfm.getFrameInfo().getSeqCount());
            if (tfm.getFrameInfo().isBad()) {
                enc.setBadFrameCount(enc.getBadFrameCount() + 1);
            }
            enc.setErrorCount(0);
            
            if (tfm.getFrameInfo().isIdle()) {
               idleFrames++;
               validFrames++;
            } else if (tfm.getFrameInfo().isDeadCode()) {
            	deadFrames++;
            	validFrames++;
            } else if (tfm.getFrameInfo().isBad()){
                badFrames++;
                validFrames++;
            } else {
                validFrames++;
            }
            
            dataByteCount += tfm.getFrameInfo().getCADUSize();
            
            if (!tfm.getFrameInfo().isDeadCode() && !tfm.getFrameInfo().isIdle() && !isDefinedVcid(tfm.getFrameInfo().getVcid())) {
                sendUnrecognizedVcidMessage(tfm);
            }
            
            if (!tfm.getFrameInfo().isDeadCode() && !tfm.getFrameInfo().isIdle() && !isSessionDss(tfm.getStationInfo().getDssId())) {
                sendUnrecognizedDssMessage(tfm);
            }

            if (missionProps.getScidChecksEnabled() && !tfm.getFrameInfo().isDeadCode() && !tfm.getFrameInfo().isIdle()
                    && !filterScid.accept(UnsignedInteger.valueOf(tfm.getScid()))) {
                sendUnrecognizedScidMessage(tfm);
            }
		}	
	}

	/**
	 * Subscriber for Frame Sync messages.
	 *
	 */
	public class SyncSubscriber implements MessageSubscriber {
		/**
		 * Creates a new SyncSubscriber
		 */
		public SyncSubscriber() {
			messageContext.subscribe(TmServiceMessageType.InSync, this);
			messageContext.subscribe(TmServiceMessageType.OutOfSyncData, this);
			messageContext.subscribe(TmServiceMessageType.LossOfSync, this);
		}
		
		/**
		 * @{inheritDoc}
		 * @see jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared.message.IMessage)
		 */
		@Override
		public void handleMessage(final IMessage m) {
			if (m.isType(TmServiceMessageType.InSync)) {
                inSync = true;
			} else if (m.isType(TmServiceMessageType.OutOfSyncData)) {
				outOfSyncBytes += ((IOutOfSyncDataMessage)m).getOutOfSyncBytesLength();
			} else {
				inSync = false;
				outOfSyncCount++;
			}
			sendSummaryMessage();
		}	
	}

    /**
     * EndOfDataMessageSubscriber listens for End of Data and End of Test messages.
     *
     *
     */
    private class EndOfDataMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of EndOfDataMessageSubscriber.
         */
        public EndOfDataMessageSubscriber() {
            messageContext.subscribe(CommonMessageType.EndOfData, this);
        }

        /**
         * Handles a received message by publishing a summary message. This message
         * is only published once for each meter run.
         * 
         * @param genericMessage
         *            the End of Test or End of Data message to handler
         */
        @Override
        public void handleMessage(final IMessage genericMessage) {
            if (!finalSummaryMessageSent) {
                sendSummaryMessage();
                finalSummaryMessageSent = true;
            }
        }
    }
}


