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

import jpl.gds.alarm.serialization.AlarmStatistics.Proto3AlarmStatistics;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.alarm.IAlarmStatistics;

/**
 * AlarmStatistics is used to track statistics for calculating alarm hysteresis.
 * 
 */
public class AlarmStatistics implements IAlarmStatistics
{
	private int consecutiveNoAlarms;
	private int consecutiveRedAlarms;
	private int consecutiveYellowAlarms;
	private IAlarmValue currentValue;

	/**
	 * Constructor.
	 */
	public AlarmStatistics()
	{
		consecutiveNoAlarms = 0;
		consecutiveRedAlarms = 0;
		consecutiveYellowAlarms = 0;
		currentValue = null;
	}

	/**
	 * @param proto
	 */
	public AlarmStatistics(Proto3AlarmStatistics proto) {
		this();
		load(proto);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmStatistics#updateCounts(jpl.gds.eha.api.channel.IAlarmValue)
     */
	@Override
    public void updateCounts(final IAlarmValue value)
	{
		if(value == null)
		{
			if(consecutiveNoAlarms == 0)
			{
				resetCounts();
			}
			consecutiveNoAlarms++;
		}
		else if(value.getLevel() == AlarmLevel.YELLOW)
		{
			if(consecutiveYellowAlarms == 0)
			{
				resetCounts();
			}
			consecutiveYellowAlarms++;
		}
		else if(value.getLevel() == AlarmLevel.RED)
		{
			if(consecutiveRedAlarms == 0)
			{
				resetCounts();
			}
			consecutiveRedAlarms++;
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmStatistics#resetCounts()
     */
	@Override
    public void resetCounts()
	{
		consecutiveNoAlarms = 0;
		consecutiveRedAlarms = 0;
		consecutiveYellowAlarms = 0;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmStatistics#getConsecutiveNoAlarms()
     */
	@Override
    public int getConsecutiveNoAlarms()
	{
		return consecutiveNoAlarms;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmStatistics#getConsecutiveRedAlarms()
     */
	@Override
    public int getConsecutiveRedAlarms()
	{
		return consecutiveRedAlarms;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmStatistics#getConsecutiveYellowAlarms()
     */
	@Override
    public int getConsecutiveYellowAlarms()
	{
		return consecutiveYellowAlarms;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmStatistics#getCurrentValue()
     */
	@Override
    public IAlarmValue getCurrentValue()
	{
		return currentValue;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.channel.alarm.IAlarmStatistics#setCurrentValue(jpl.gds.eha.api.channel.IAlarmValue)
     */
	@Override
    public void setCurrentValue(final IAlarmValue value)
	{
		currentValue = value;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(128);

		sb.append("Consecutive None=");
		sb.append(consecutiveNoAlarms);
		sb.append(" Yellow=");
		sb.append(consecutiveYellowAlarms);
		sb.append(" Red=");
		sb.append(consecutiveRedAlarms);

		return(sb.toString());
	}

	@Override
	public Proto3AlarmStatistics build() {
		Proto3AlarmStatistics.Builder builder = Proto3AlarmStatistics.newBuilder()
				.setConsecutiveNoAlarms(consecutiveNoAlarms)
				.setConsecutiveRedAlarms(consecutiveRedAlarms)
				.setConsecutiveYellowAlarms(consecutiveYellowAlarms);
		
		if (currentValue != null) {
				builder.setValue(currentValue.getProto());
		}
		
		return builder.build();

	}

	@Override
	public void load(Proto3AlarmStatistics proto) {
		this.consecutiveNoAlarms = proto.getConsecutiveNoAlarms();
		this.consecutiveRedAlarms = proto.getConsecutiveRedAlarms();
		this.consecutiveYellowAlarms = proto.getConsecutiveYellowAlarms();
		
		switch (proto.getCurrentValueCase()) {
		case CURRENTVALUE_NOT_SET:
			this.currentValue = null;
			break;
		case VALUE:
			this.currentValue = new AlarmValue(proto.getValue());
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + consecutiveNoAlarms;
		result = prime * result + consecutiveRedAlarms;
		result = prime * result + consecutiveYellowAlarms;
		result = prime * result + ((currentValue == null) ? 0 : currentValue.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlarmStatistics other = (AlarmStatistics) obj;
		if (consecutiveNoAlarms != other.consecutiveNoAlarms)
			return false;
		if (consecutiveRedAlarms != other.consecutiveRedAlarms)
			return false;
		if (consecutiveYellowAlarms != other.consecutiveYellowAlarms)
			return false;
		if (currentValue == null) {
			if (other.currentValue != null)
				return false;
		} else if (!currentValue.equals(other.currentValue))
			return false;
		return true;
	}
}
