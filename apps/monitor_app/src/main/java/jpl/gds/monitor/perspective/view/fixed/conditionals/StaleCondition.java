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
package jpl.gds.monitor.perspective.view.fixed.conditionals;

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;

/**
 * Evaluates stale conditions based on a given staleness interval
 *
 */
public class StaleCondition extends ConditionConfiguration {

    private long timeDataBecameNull = 0;
	
	/**
     * Constructor: creates a new Condition Configuration object of type 
     * Stale Condition and sets the member variables
     * 
     * @param conditionId is the unique identifier for this condition object
     * @param channelId identifies the channel whose value is to be used for 
     *                  evaluation
     */
    public StaleCondition(final String conditionId, final String channelId) {
        super(conditionId, channelId, Comparison.STALE);
    }

    /**
     * Compares time channel has been stale with the staleness interval
     * 
     * @param staleInterval is the length of time an element goes w/out 
     *                          updates before being considered stale
     * @return true if channel is stale, false otherwise
     */
    @Override
    public boolean evaluate(final ApplicationContext appContext, final int staleInterval) {
        boolean stale;
        /*
         * Realtime recorded filter in the perspective and
         * is now enum rather than boolean and DSS ID is required. 
         * However, there is currently no way to get the station ID in this object, so I have 
         * had to set it to 0 temporarily. Also, the method used to set the rt/rec filter type 
         * here will not work once fixed view preferences are made modifiable at runtime,
         * because it is set only upon parsing the perspective.
         * get current RT/Rec flag and station
    	 * filter from config
         */
        final MonitorChannelSample latestData = appContext.getBean(MonitorChannelLad.class).getMostRecentValue(channelId, 
        		viewConfig.getRealtimeRecordedFilterType(), 
        		viewConfig.getStationId());
        
        final long currentTime = System.currentTimeMillis();

        /* If data becomes null, save the time
         * and check if it's stale. The stale data dimming indicator marks 
         * null data as stale and stale conditions should too to be consistent 
         */
        if(latestData == null) {
        	if(timeDataBecameNull == 0) {
        		timeDataBecameNull = currentTime;	
        	}
        	
        	if((currentTime - timeDataBecameNull) > (staleInterval * 1000)) {
        		stale = true;
        	}
        	else {
        		stale = false;
        	}
        }
        else {
	        timeDataBecameNull = 0;
	        
	        final long lastUpdateTime = latestData.getTimestamp();
	        
	        //compare time this element has been stale with staleness interval
	        if((currentTime - lastUpdateTime) > (staleInterval * 1000) ) {
	            stale = true;
	        }
	        else {
	            stale = false;
	        }
        }
        
        return stale;
    }

    @Override
    public boolean evaluate(final ApplicationContext appContext) {
        // intentionally left empty
        return false;
    }

}
