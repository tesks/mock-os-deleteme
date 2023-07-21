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
package jpl.gds.eha.api.channel;

import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.alarm.serialization.Proto3AlarmValueSet;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
/**
 * The IAlarmValueSet interface is to be implemented by all alarm value set
 * classes.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * An IAlarmValueSet object is the multi-mission representation of collection of 
 * IAlarmValue instances. It is attached to channel values through the
 * IChannelValue interface. In order to isolate the mission adaptation from
 * changes in the multi-mission core, IAlarmValueSet objects should always be
 * created with the AlarmValueSetFactory and should always be access through this
 * interface.. Interaction with the actual Alarm Value Set implementation classes is
 * contrary to multi-mission development standards.
 * 
 *
 * @see IAlarmValue
 */
public interface IAlarmValueSet {

    /**
     * Gets the entire list of IAlarmValues in the set.
     * 
     * @return List of IAlarmValue, or an empty list if no alarm values are set.
     */
    public abstract List<IAlarmValue> getAlarmValueList();

    /**
     * Adds an IAlarmValue to the current list of IAlarmValues in the set. Values are added
     * at the end of the list.
     * 
     * @param IAlarmValue the value to add
     */
    public abstract void addAlarm(final IAlarmValue IAlarmValue);

    /**
     * Concatenate this set with another IAlarmValueSet.
     * 
     * @param IAlarmValueSet the value set to add
     */
    public abstract void addAlarmSet(final IAlarmValueSet IAlarmValueSet);

    /**
     * Gets a new IAlarmValueSet containing only the alarms with the given level and
     * "on EU" value.  This method returns an IAlarmValueSet so the return result can
     * be manipulated the same as the original IAlarmValueSet.
     * 
     * @param level the AlarmLevel to look for
     * @param onEu the "alarm is on EU" value to look for
     * @return A new IAlarmValueSet with the matching IAlarmValues, or an empty IAlarmValueSet
     */
    public abstract IAlarmValueSet getAlarmSet(final AlarmLevel level,
            final boolean onEu);

    /**
     * Gets a new IAlarmValueSet containing only the alarms with the given 
     * "on EU" value.  This method returns an IAlarmValueSet so the return result can
     * be manipulated the same as the original IAlarmValueSet.
     * 
     * @param onEu the "alarm is on EU" value to look for
     * @return A new IAlarmValueSet with the matching IAlarmValues, or an empty IAlarmValueSet
     */
    public abstract IAlarmValueSet getAlarmSet(final boolean onEu);

    /**
     * Indicates whether any IAlarmValues in this IAlarmValueSet are in a true
     * alarm state.
     * 
     * @return true if any alarm values are set; false otherwise
     */
    public abstract boolean inAlarm();

    /**
     * Indicates whether any IAlarmValues with the given level in this
     * IAlarmValueSet are in a true alarm state.
     * 
     * @param level the AlarmLevel to check for
     * @return true if any alarm values with the given level are set; false
     *         otherwise
     */
    public abstract boolean inAlarm(AlarmLevel level);

    /**
     * Gets the worst (most severe) AlarmLevel in this alarm value set.
     * 
     * @return worst AlarmLevel
     */
    public abstract AlarmLevel getWorstLevel();
    
    /**
     * Makes a copy of the current alarm value set.
     * 
     * @return new alarm value set instance
     */
    public IAlarmValueSet copy();
    
	/**
	 * Calls getProto() and gets the byte array representation of the 
	 * result.
	 * @return binary representation
	 */
	public default byte[] serialize() {
		return getProto().toByteArray();
	}
	
	/**
	 * Serialize this into a proto object
	 * @return
	 */
	public Proto3AlarmValueSet getProto();
	
	/**
	 * Deserialize from the buffer.
	 * @param buffer
	 * @throws InvalidProtocolBufferException 
	 */
	public default void deserialize(byte[] buffer) throws InvalidProtocolBufferException {
		deserialize(Proto3AlarmValueSet.parseFrom(buffer));
	}
	
	/**
	 * deserialize from the proto alarm object.
	 * @param value
	 */
	public void deserialize(Proto3AlarmValueSet valueSet);

}