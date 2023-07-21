/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.globallad.data;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.alarm.serialization.Proto3AlarmValueSet;
import jpl.gds.alarm.serialization.Proto3AlarmValueSet.Builder;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IAlarmValueSet;

/**
 * 
 * AlarmValueSet is a class that holds and provides methods for manipulating a
 * list of IAlarmValues. The IAlarmValueSet for a channel defined the total
 * state of all alarms on that channel.
 */
public class GladAlarmValueSet implements IAlarmValueSet
{
	/**
	 * The IAlarmValues in the set.
	 */
	private List<IAlarmValue> alarmList;

	/**
	 * Creates an instance of IAlarmValueSet.
	 * 
	 */
	/*package*/ public GladAlarmValueSet()
	{
		alarmList = new ArrayList<IAlarmValue>(1);
	}

	/*package*/ public GladAlarmValueSet(Proto3AlarmValueSet set)
	{
		alarmList = new ArrayList<IAlarmValue>(1);
		
		deserialize(set);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getAlarmValueList()
	 */
	@Override
	public List<IAlarmValue> getAlarmValueList()
	{
		return(alarmList);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#addAlarm(jpl.gds.eha.api.channel.IAlarmValue)
	 */
	@Override
	public void addAlarm(final IAlarmValue IAlarmValue)
	{
		if(IAlarmValue == null)
		{
			throw new IllegalArgumentException("Null input alarm value");
		}
		alarmList.add(IAlarmValue);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#addAlarmSet(jpl.gds.eha.api.channel.IAlarmValueSet)
	 */
	@Override
	public void addAlarmSet(final IAlarmValueSet IAlarmValueSet)
	{
		if(IAlarmValueSet == null)
		{
			throw new IllegalArgumentException("Null input alarm value set");
		}

		alarmList.addAll(IAlarmValueSet.getAlarmValueList());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getAlarmSet(jpl.gds.dictionary.alarm.adaptation.AlarmLevel, boolean)
	 */
	@Override
	public IAlarmValueSet getAlarmSet(final AlarmLevel level, final boolean onEu)
	{
		IAlarmValueSet alarmValueSet = new GladAlarmValueSet();
		for (int index = 0; index < alarmList.size(); index++) {
			IAlarmValue val = alarmList.get(index);
			if (val.getLevel().equals(level) && val.isOnEu() == onEu) {
				alarmValueSet.addAlarm(val);
			}
		}
		return alarmValueSet;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getAlarmSet(boolean)
	 */
	@Override
	public IAlarmValueSet getAlarmSet(final boolean onEu)
	{
		IAlarmValueSet alarmValueSet = new GladAlarmValueSet();
		for (int index = 0; index < alarmList.size(); index++) {
			IAlarmValue val = alarmList.get(index);
			if (val.isOnEu() == onEu) {
				alarmValueSet.addAlarm(val);
			}
		}
		return alarmValueSet;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#inAlarm()
	 */
	@Override
	public boolean inAlarm()
	{
		for(IAlarmValue val : alarmList)
		{
			if (val.isInAlarm() == true)
			{
				return(true);
			}
		}

		return(false);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#inAlarm(jpl.gds.dictionary.alarm.adaptation.AlarmLevel)
	 */
	@Override
	public boolean inAlarm(final AlarmLevel level)
	{
		for(IAlarmValue val : alarmList)
		{
			if(val.getLevel().equals(level))
			{
				return(true);
			}
		}

		return(false);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getWorstLevel()
	 */
	@Override
	public AlarmLevel getWorstLevel()
	{
		AlarmLevel worstLevel = AlarmLevel.getMinLevel();
		for(IAlarmValue val : alarmList)
		{
			AlarmLevel testLevel = val.getLevel();
			if(testLevel.compareTo(worstLevel) > 0)
			{
				worstLevel = testLevel;
			}
		} 

		return(worstLevel);
	}
	
	@Override
    public IAlarmValueSet copy() {
	    GladAlarmValueSet result = new GladAlarmValueSet();
	    result.alarmList = new ArrayList<IAlarmValue>(alarmList);
	    return result;
	}

	@Override
	public Proto3AlarmValueSet getProto() {
		Builder builder = Proto3AlarmValueSet.newBuilder();
		
		this.alarmList.forEach(alarm -> {
			builder.addAlarms(alarm.getProto());
		});
		
		return builder.build();
	}

	@Override
	public void deserialize(Proto3AlarmValueSet alarms) {
		synchronized (this.alarmList) {
			alarms.getAlarmsList().forEach(alarm -> {
				alarmList.add(new GladAlarmValue(alarm));
			});
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
		result = prime * result + ((alarmList == null) ? 0 : alarmList.hashCode());
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
		GladAlarmValueSet other = (GladAlarmValueSet) obj;
		if (alarmList == null) {
			if (other.alarmList != null)
				return false;
		} else if (!alarmList.equals(other.alarmList))
			return false;
		return true;
	}
}
