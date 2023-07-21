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

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;

/**
 * Checks if data is null
 */
public class NullCondition extends ConditionConfiguration {
    
    /**
     * Constructor: creates a new Condition Configuration object of type 
     * Relational Condition and sets the member variables
     * 
     * @param conditionId is the unique identifier for this condition object
     * @param channelId identifies the channel whose value is to be used for 
     *                  evaluation
     */
    public NullCondition(final String conditionId, final String channelId){
        super(conditionId, channelId, Comparison.IS_NULL);
    }
    
    /**
     * Default constructor
     */
    public NullCondition() {
        super();
    }


    @Override
    public boolean evaluate(final ApplicationContext appContext) {
        
        final IChannelDefinition def =  appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(channelId);
        if (def == null) {
            return true;
        }
        
        // get latest ChannelSample for channelId associated with this condition
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
        final MonitorChannelSample data =  appContext.getBean(MonitorChannelLad.class).getMostRecentValue(channelId, viewConfig.
        		getRealtimeRecordedFilterType(), viewConfig.getStationId());
        
        return (data == null);
    }

    @Override
    public boolean evaluate(final ApplicationContext appContext, final int stalenessInterval) {
        return false;
    }
}
