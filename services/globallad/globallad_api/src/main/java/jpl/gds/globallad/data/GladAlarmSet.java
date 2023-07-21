///*
// * Copyright 2006-2018. California Institute of Technology.
// * ALL RIGHTS RESERVED.
// * U.S. Government sponsorship acknowledged.
// *
// * This software is subject to U. S. export control laws and
// * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
// * extent that the software is subject to U.S. export control laws
// * and regulations, the recipient has the responsibility to obtain
// * export licenses or other export authority as may be required
// * before exporting such information to foreign countries or
// * providing access to foreign nationals.
// */
//package jpl.gds.globallad.data;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.List;
//
//import jpl.gds.dictionary.impl.impl.api.alarm.AlarmLevel;
//import jpl.gds.eha.api.channel.IAlarmValue;
//import jpl.gds.eha.api.channel.IAlarmValueSet;
//
///**
// * 
// * AlarmValueSet is a class that holds and provides methods for manipulating a
// * list of IAlarmValues. The IAlarmValueSet for a channel defined the total
// * state of all alarms on that channel.
// */
//public class GladAlarmSet implements IByteBufferManipulator, IAlarmValueSet
//{
//	private static final int NUMBER_OF_ALARMS_SIZE = 4; 
//	
//	/**
//	 * The IAlarmValues in the set.
//	 */
//	private List<IAlarmValue> alarmList;
//
//	GladAlarmSet()  {
//		alarmList = new ArrayList<IAlarmValue>(1);
//	}
//
//	/**
//	 * Creates an instance of IAlarmValueSet from buffer. 
//	 * 
//	 * Expected binary format
//	 * 1. Number of alarms - 4 bytes
//	 * 2. Alarm data.
//	 */
//	GladAlarmSet(ByteBuffer buffer) throws GlobalLadDataException {
//		int numberOfAlarms = getNextInt(buffer, NUMBER_OF_ALARMS_SIZE);
//
//		alarmList = new ArrayList<IAlarmValue>(numberOfAlarms);
//		
//		for (int an = numberOfAlarms; an > 0; an--) {
//			GladAlarmValue ga = new GladAlarmValue(buffer);
//			alarmList.add(ga);
//		}
//	}
//	
//	public ByteBuffer serialize() {
//		if (alarmList.isEmpty()) {
//			return ByteBuffer.wrap(new byte[NUMBER_OF_ALARMS_SIZE]);
//		} else {
//			
//		}
//		
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getAlarmValueList()
//	 */
//	@Override
//	public List<IAlarmValue> getAlarmValueList()
//	{
//		return(alarmList);
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#addAlarm(jpl.gds.eha.api.channel.IAlarmValue)
//	 */
//	@Override
//	public void addAlarm(final IAlarmValue IAlarmValue)
//	{
//		if(IAlarmValue == null)
//		{
//			throw new IllegalArgumentException("Null input alarm value");
//		}
//		alarmList.add(IAlarmValue);
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#addAlarmSet(jpl.gds.eha.api.channel.IAlarmValueSet)
//	 */
//	@Override
//	public void addAlarmSet(final IAlarmValueSet IAlarmValueSet)
//	{
//		if(IAlarmValueSet == null)
//		{
//			throw new IllegalArgumentException("Null input alarm value set");
//		}
//
//		alarmList.addAll(IAlarmValueSet.getAlarmValueList());
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getAlarmSet(jpl.gds.dictionary.impl.impl.api.alarm.AlarmLevel, boolean)
//	 */
//	@Override
//	public IAlarmValueSet getAlarmSet(final AlarmLevel level, final boolean onEu)
//	{
//		IAlarmValueSet alarmValueSet = new GladAlarmSet();
//		for (int index = 0; index < alarmList.size(); index++) {
//			IAlarmValue val = alarmList.get(index);
//			if (val.getLevel().equals(level) && val.isOnEu() == onEu) {
//				alarmValueSet.addAlarm(val);
//			}
//		}
//		return alarmValueSet;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getAlarmSet(boolean)
//	 */
//	@Override
//	public IAlarmValueSet getAlarmSet(final boolean onEu)
//	{
//		IAlarmValueSet alarmValueSet = new GladAlarmSet();
//		for (int index = 0; index < alarmList.size(); index++) {
//			IAlarmValue val = alarmList.get(index);
//			if (val.isOnEu() == onEu) {
//				alarmValueSet.addAlarm(val);
//			}
//		}
//		return alarmValueSet;
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#inAlarm()
//	 */
//	@Override
//	public boolean inAlarm()
//	{
//		for(IAlarmValue val : alarmList)
//		{
//			if (val.isInAlarm() == true)
//			{
//				return(true);
//			}
//		}
//
//		return(false);
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#inAlarm(jpl.gds.dictionary.impl.impl.api.alarm.AlarmLevel)
//	 */
//	@Override
//	public boolean inAlarm(final AlarmLevel level)
//	{
//		for(IAlarmValue val : alarmList)
//		{
//			if(val.getLevel().equals(level))
//			{
//				return(true);
//			}
//		}
//
//		return(false);
//	}
//
//	/**
//	 * {@inheritDoc}
//	 * @see jpl.gds.eha.api.channel.IAlarmValueSet#getWorstLevel()
//	 */
//	@Override
//	public AlarmLevel getWorstLevel()
//	{
//		AlarmLevel worstLevel = AlarmLevel.getMinLevel();
//		for(IAlarmValue val : alarmList)
//		{
//			AlarmLevel testLevel = val.getLevel();
//			if(testLevel.compareTo(worstLevel) > 0)
//			{
//				worstLevel = testLevel;
//			}
//		} 
//
//		return(worstLevel);
//	}
//	
//	@Override
//    public IAlarmValueSet copy() {
//	    GladAlarmSet result = new GladAlarmSet();
//	    result.alarmList = new ArrayList<IAlarmValue>(alarmList);
//	    return result;
//	}
//}
