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

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.shared.log.TraceManager;

/**
 * Evaluates alarm conditions by checking current level for a channel
 * (RED, YELLOW, NONE)
 */
public class AlarmCondition extends ConditionConfiguration {

	/**
	 * List of possible values for an Alarm Condition
	 */
	public enum Value {
		/**
		 * Red alarm
		 */
		RED,

		/**
		 * Yellow alarm
		 */
		YELLOW,

		/**
		 * Red or yellow alarm
		 */
		ANY;
	};

	/**
	 * The String value stored in Condition Configuration must be in the 
	 * Value enum
	 */
	private Value givenValue;

	/**
	 * Constructor: creates a new Condition Configuration object of type 
	 * Alarm Condition and sets the member variables
	 * 
	 * @param conditionId is the unique identifier for the condition
	 * @param channelId identifies the channel whose value is to be used for 
	 *                  evaluation
	 * @param value is the value to be compared against (RED YELLOW or ANY)
	 * @param comparison is the comparison operator
	 */
	public AlarmCondition(final String conditionId, final String channelId, 
			final Comparison comparison, final String value) {
		super(conditionId, channelId, comparison, value);
	}

	/**
	 * Performs equality check between channel level and given level
	 * 
	 * @return true if alarm level associated with channel id matches the 
	 * given alarm level, false otherwise
	 */
	@Override
	public boolean evaluate(final ApplicationContext appContext) {
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
		final MonitorChannelSample data = appContext.getBean(MonitorChannelLad.class).getMostRecentValue(channelId, viewConfig.
				getRealtimeRecordedFilterType(), viewConfig.getStationId());

		if(data == null) {
			if(comparison.equals(Comparison.NOT_SET)) {
				return true;
			}
			return false;
		}

		// get the current alarm level
		final AlarmLevel level = getAlarmLevel(data);

		// convert string given in xml file to enum value
		try {
			givenValue = Enum.valueOf(Value.class, value);
		} catch (final Exception e) {
			//this should be caught in ViewConfigParseHandler but if for 
			//some reason it wasn't, it will print an error message
            TraceManager.getDefaultTracer(appContext).error

			("ALARM Condition " + conditionId + " has invalid value attribute "
					+ value);
			return false;
		}

		// do a comparison between the current value and the given value 
		// for the selected operator
		switch(comparison) {
		case SET:
			switch(givenValue) {
			case RED:
				return (level != null && 
				(level.compareTo(AlarmLevel.RED) == 0 ? true : false));
			case YELLOW:
				return (level != null && 
				(level.compareTo(AlarmLevel.YELLOW) == 0 ? true : false));
			case ANY:
				return (level != null && 
				(level.compareTo(AlarmLevel.NONE) != 0 ? true : false));
			}
			break;
		case NOT_SET:
			switch(givenValue) {
			case RED:
				return (level != null && 
				(level.compareTo(AlarmLevel.RED) != 0 ? true : false));
			case YELLOW:
				return (level != null && 
				(level.compareTo(AlarmLevel.YELLOW) != 0 ? true : false));
			case ANY:
				return (level != null && 
				(level.compareTo(AlarmLevel.NONE) == 0 ? true : false));
			}
			break;
		}
		return false;
	}

	/**
	 * Helper function: Determines the current alarm level of a given channel
	 * 
	 * @param data is the latest received data for the channel that is to be 
	 *             compared
	 * @return level is the current alarm status (RED, YELLOW or NONE)
	 */
	private AlarmLevel getAlarmLevel(final MonitorChannelSample data) {
		final AlarmLevel dnLevel = data.getDnAlarmLevel();
		final AlarmLevel euLevel = data.getEuAlarmLevel();

		if(dnLevel == AlarmLevel.RED || euLevel == AlarmLevel.RED) {
			return AlarmLevel.RED;
		}
		else if (dnLevel == AlarmLevel.YELLOW || euLevel == AlarmLevel.YELLOW) {
			return AlarmLevel.YELLOW;
		}
		else {
			return AlarmLevel.NONE;
		}
	}

	@Override
	public boolean evaluate(final ApplicationContext appContext, final int stalenessInterval) {
		return false;
	}
}
