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

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.alarm.serialization.AlarmValue.Proto3AlarmValue;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;

/**
 * The IAlarmValue interface is to be implemented by all alarm value instance
 * classes.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * An IAlarmValue object is the multi-mission representation of an alarm
 * instance. Sets of alarm instances are attached to channel values through the
 * IChannelValue interface. In order to isolate the mission adaptation from
 * changes in the multi-mission core, IAlarmValue objects should always be
 * created with the AlarmValueFactory and should always be access through this
 * interface.. Interaction with the actual Alarm Value implementation classes is
 * contrary to multi-mission development standards.
 * 
 *
 */
public interface IAlarmValue {

	/**
	 * Gets the in alarm flag, indicating if the alarm is triggered.
	 * 
	 * @return true if the alarm has been triggered, false if not.
	 */
	public abstract boolean isInAlarm();

	/**
	 * Sets the flag indicating whether the alarm has been triggered.
	 * 
	 * @param inAlarm true if the alarm has been triggered; false if not
	 */
	public abstract void setInAlarm(boolean inAlarm);

	/**
	 * Sets the state string, which is used to display alarm type and 
	 * state if the alarm is set.
	 *
	 * @param state The state to set.
	 */
	public abstract void setState(String state);

	/**
	 * Gets the state string, which is used to display alarm type 
	 * and state if the alarm is set.
	 *
	 * @return the state text
	 */
	public abstract String getState();

	/**
	 * Gets the severity level of the alarm.
	 * 
	 * @return the level of the alarm
	 */
	public abstract AlarmLevel getLevel();

	/**
	 * Gets the alarm definition type.
	 * 
	 * @return the type of the alarm
	 */
	public abstract AlarmType getType();

	/**
	 * Get the alarm id
	 * @return the id
	 */
	public abstract String getAlarmId();

	/**
	 * Returns the flag indicating if this alarm on on EU as opposed to DN.
	 * 
	 * @return true if this alarm on on EU, false if on DN
	 */
	public abstract boolean isOnEu();

	/**
	 * Sets the level of this alarm.
	 * 
	 * @param level the AlarmLevel to set
	 */
	public abstract void setLevel(AlarmLevel level);

	/**
	 * Sets the type of this alarm.
	 * 
	 * @param type the AlarmType to set
	 */
	public abstract void setType(AlarmType type);

	/**
	 * Sets the EU flag for this alarm, indicating that it is alarming the channel
	 * EU rather than the channel DN.
	 * 
	 * @param onEu true if alarming EU, false for DN
	 */
	public abstract void setOnEu(boolean onEu);
	
	
	/**
	 * Set the alarm id
	 * @param id the alarm id from the definition
	 */
	public abstract void setAlarmId(String id);
	
	
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
	public Proto3AlarmValue getProto();

	/**
     * Deserialize from the buffer.
     * 
     * @param buffer
     *            the binary data of the Alarm value to be loaded
     * @throws InvalidProtocolBufferException
     *             an exception occurred reading the buffer
     */
	public default void deserialize(final byte[] buffer) throws InvalidProtocolBufferException {
		deserialize(Proto3AlarmValue.parseFrom(buffer));
	}
	
	/**
     * Deserialize from the proto alarm object.
     * 
     * @param value
     *            the message to be loaded
     */
    public void deserialize(Proto3AlarmValue value);
}