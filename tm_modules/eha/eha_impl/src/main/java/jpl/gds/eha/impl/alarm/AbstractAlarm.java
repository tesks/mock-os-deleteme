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
package jpl.gds.eha.impl.alarm;

import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.channel.alarm.IAlarmStatistics;
import jpl.gds.eha.api.channel.alarm.IChannelAlarm;
import jpl.gds.eha.impl.ChannelTimeComparator;

/**
 * 
 * Base run-time alarm calculation class corresponding to an alarm definition
 * object, for use as a parent class to various runtime alarm classes.
 * 
 * 
 *
 */
public abstract class AbstractAlarm implements IChannelAlarm {

	/**
	 * The dictionary definition of this alarm.
	 */
	protected final IAlarmDefinition definition;
	
	/**
	 * Shared time comparator object.
	 */
	protected ChannelTimeComparator timeCompare;

	/**
	 * Constructor.
	 *
	 * @param def
	 *            the alarm definition object for this instance
	 * @param timeStrategy
	 *            the CurrentTimeComparisonStrategy object, dictating how
	 *            channel samples are compared in time
	 *
	 */
	public AbstractAlarm(final IAlarmDefinition def, final TimeComparisonStrategyContextFlag timeStrategy) {

		if (def == null) {
			throw new IllegalArgumentException("Alarm definition may not be null");
		}

		this.timeCompare = new ChannelTimeComparator(timeStrategy);
		this.definition = def;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IChannelAlarm#getDefinition()
     */
	@Override
    public IAlarmDefinition getDefinition() {
		return this.definition;
	}

	@Override
    public IAlarmValue check(final IAlarmHistoryProvider history, final IServiceChannelValue value)
	{
		IAlarmValue newValue = calculateAlarm(history, value);
		if (this.definition.hasHysteresis())
		{
			/*
			 * 01/16/2014 - If there is a time
			 * regression according to the configured time system, the value
			 * does not count toward the hysteresis statistics nor should it be
			 * saved in the history.
			 */
			final IServiceChannelValue lastValue = history
					.getLastValueInStatistics(this,
							value.isRealtime(),
							value.getDefinitionType() == ChannelDefinitionType.M,
							value.getDssId());
			if (lastValue != null && !timeCompare.timestampIsLater(lastValue, value)) {
				return null;
			}

			final IAlarmStatistics stats = history
					.getStatistics(
							this,
							value.isRealtime(),
							value.getDefinitionType() == ChannelDefinitionType.M,
							value.getDssId());
			stats.updateCounts(newValue);


			if(newValue != null && newValue.isInAlarm() && stats.getCurrentValue() == null) //do in-hysteresis
			{
				if((newValue.getLevel() == AlarmLevel.YELLOW && stats.getConsecutiveYellowAlarms() < this.definition.getHysteresisInValue()) ||
						(newValue.getLevel() == AlarmLevel.RED && stats.getConsecutiveRedAlarms() < this.definition.getHysteresisInValue()))
				{
					//we are not really in yellow/red alarm
					newValue = null;
				}
			}
			else if((newValue == null || !newValue.isInAlarm()) && stats.getCurrentValue() != null) //do out-hysteresis
			{
				if(stats.getConsecutiveNoAlarms() < this.definition.getHysteresisOutValue())
				{
					//we should actually still stay in alarm...which means that instead of the "null" we got back from
					//the "calculateAlarm" method, we actually need to get the previous state of this alarm and hand that back.
					newValue = stats.getCurrentValue();
				}
			}
			stats.setCurrentValue(newValue);

			/*
			 * 10/28/2013 - Added new parameters to
			 * allow updating of station monitor data statistics table.
			 */
			history
			.updateStatistics(
					this,
					stats,
					value.isRealtime(),
					value.getDefinitionType() == ChannelDefinitionType.M,
					value.getDssId());

			/*
			 * 01/16/2014 - Just worked on a channel
			 * value that is not time-regressed. Save it so that we can compare
			 * the timestamp at a later run, when new value comes in.
			 */
			history
			.setLastValueInStatistics(this,
					value.isRealtime(),
					value.getDefinitionType() == ChannelDefinitionType.M,
					value.getDssId(),
					value);
		}
		return(newValue);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.definition.toString();
	}

	    /**
     * Calculates the actual alarm condition on the given channel value,
     * exclusive of the hysteresis calculation, and returns an AlarmValue object
     * indicating the alarm state. Must be overridden by subclasses.
     * 
     * @param history
     *            alarm history provider
     * @param value
     *            channel value to calculate the alarm for
     * @return AlarmValue object, if the alarm is triggered, or null if the
     *         alarm could not be calculated or is not triggered
     */
	protected abstract IAlarmValue calculateAlarm(IAlarmHistoryProvider history, final IServiceChannelValue value);

}
