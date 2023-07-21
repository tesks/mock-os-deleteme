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
package jpl.gds.telem.common.feature;

import jpl.gds.common.service.telem.*;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.client.frame.ITransferFrameUtilityDictionaryManager;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.tm.service.api.frame.IFrameSyncService;
import jpl.gds.tm.service.api.frame.IFrameTrackingService;

/**
 * FramesyncFeatureManager handles everything required to initialize and shutdown the 
 * frame synchronization capability in the downlink process.
 *
 */
public class FrameFeatureManager extends AbstractTelemetryFeatureManager {
    
    private IService frameSync;
    private IFrameTrackingService frameTracker;
    private boolean enableFrameSync;
    private boolean enableFrameTracking;

	/**
	 * {@inheritDoc}
	 */
	@Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);
        setValid(false);
		if (!this.isEnabled()) {
			return true;
		}
	
		setValid(true);
		
		try
		{	  
		    if (isEnableFrameSync()) {
                final boolean isLoaded = springContext.getBean(ITransferFrameUtilityDictionaryManager.class).isLoaded();
                if (!isLoaded) {
                    springContext.getBean(ITransferFrameUtilityDictionaryManager.class).load();
                }
		        frameSync = springContext.getBean(IFrameSyncService.class);
		        addService(frameSync);
		    }
		    if (isEnableFrameTracking()) {
		        frameTracker = springContext.getBean(IFrameTrackingService.class);
		        addService(frameTracker);
		    }
		}
		catch(final Exception e)
		{
			log.error("Unable to load transfer frame definitions");
			setValid(false);
			return false;
		}

		setValid(startAllServices());
		
		if (this.isValid()) {
			log.debug("Frame features successfully initialized");
		}
		
		return isValid();
	}
	
	/**
	 * Gets the framesync service object.
	 * 
	 * @return the framesync service object, or null if not initialized
	 */
	public IService getFrameSync() {
		return this.frameSync;
	}

	/**
	 * Indicates if frame sync is enabled.
	 * 
	 * @return true if enabled
	 */
    public boolean isEnableFrameSync() {
        return enableFrameSync;
    }

    /**
     * Sets the enable frame sync flag.
     * 
     * @param enableFrameSync true to enable, false to disable
     */
    public void setEnableFrameSync(final boolean enableFrameSync) {
        this.enableFrameSync = enableFrameSync;
    }

    /**
     * Gets the enable frame tracking flag.
     * 
     * @return true if enabled
     */
    public boolean isEnableFrameTracking() {
        return enableFrameTracking;
    }

    /**
     * Sets the enable frame tracking flag.
     * 
     * @param enableFrameTracking true to enable, false to disable
     */
    public void setEnableFrameTracking(final boolean enableFrameTracking) {
        this.enableFrameTracking = enableFrameTracking;
    }
	
    
    @Override
    public void populateSummary(final ITelemetrySummary summary) {
        if (summary != null && frameTracker != null) {
            if (summary instanceof IDownlinkSummary) {
                final DownlinkSummary sum = (DownlinkSummary) summary;

                sum.setBadFrames(frameTracker.getBadFrames());
                sum.setOutOfSyncData(frameTracker.getOutOfSyncBytes());
                sum.setDeadFrames(frameTracker.getDeadFrames());
                sum.setInSyncFrames(frameTracker.getValidFrames());
                sum.setIdleFrames(frameTracker.getIdleFrames());
                sum.setOutOfSyncCount(frameTracker.getOutOfSyncCount());
            }
            else if (summary instanceof ITelemetryIngestorSummary) {
                final TelemetryIngestorSummary sum = (TelemetryIngestorSummary) summary;

                sum.setBadFrames(frameTracker.getBadFrames());
                sum.setOutOfSyncData(frameTracker.getOutOfSyncBytes());
                sum.setDeadFrames(frameTracker.getDeadFrames());
                sum.setInSyncFrames(frameTracker.getValidFrames());
                sum.setIdleFrames(frameTracker.getIdleFrames());
                sum.setOutOfSyncCount(frameTracker.getOutOfSyncCount());
            }

        }
    }
	
}
