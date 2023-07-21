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
package jpl.gds.dictionary.api.alarm;

import java.util.List;

import jpl.gds.dictionary.api.IAttributesSupport;
import jpl.gds.dictionary.api.ICategorySupport;


/**
 * The IAlarmDefinition interface is to be implemented by all single-channel
 * alarm definition classes. <p>
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p><p>
 * 
 * IAlarmDefinition defines methods needed to interact with Alarm Definition
 * objects as required by the IAlarmDictionary interface. It is primarily used
 * by alarm file parser implementations in conjunction with the
 * AlarmDefinitionFactory, which is used to create actual Alarm Definition
 * objects in the parsers. IAlarmDictionary objects should interact with Alarm
 * Definition objects only through the Factory and the IAlarmDefinition
 * interfaces. Interaction with the actual Alarm Definition implementation
 * classes in an IAlarmDictionary implementation is contrary to multi-mission
 * development standards.
 * <p>
 * The supported alarm types and their descriptions:
 * <p>
 * HIGH: Represents an inclusive high value compare alarm, in which a channel is
 * considered to be in alarm if its value is equal to or greater than an alarm
 * threshold. This alarm works only for numeric channels.
 * <p>
 * LOW: Represents an inclusive low value compare alarm, in which a channel is
 * considered to be in alarm if its value is equal to or less than an alarm
 * threshold. This alarm works only for numeric channels.
 * <p>
 * INCLUSIVE RANGE: Represents an inclusive range compare alarm, in which a
 * channel is considered to be in alarm if its value is within (inclusive of)
 * the lower and upper thresholds. This alarm works only for numeric channels.
 * <p>
 * EXCLUSIVE_RANGE: Represents an exclusive range compare alarm, in which a
 * channel is considered to be in alarm if its value is outside of both the
 * lower and upper thresholds. This alarm works only for numeric channels.
 * <p>
 * MASK: This class represents a mask compare alarm for channel values. The mask
 * is a 16-bit, 4-digit hexadecimal number. Therefore, a mask can only be used
 * for a channel with possible values of 0-15 only. The low order bit 0x1
 * (binary 0000 0000 0000 0001) always represents state zero (count the first
 * bit from the right, or the power of 2), the high order bit 0x8000 (binary
 * 1000 0000 0000 0000)represents state 15 (count the sixteenth bit from the
 * right). For example, set red_alarm_state=0x1. The binary number is 0000 0000
 * 0000 0001 and will cause a zero (0) channel value to be in alarm. This is why
 * when the state to be alarmed is 2, the bit mask = 0x4, a seemingly
 * incongruous situation.
 * <p>
 * For another example, set red_alarm_state=0x42A3. The binary number is 0100
 * 0010 1010 0011 and will cause the following states to generate an alarm
 * (count the first bit from the right as zero and proceed to the left): 0, 1,
 * 5, 7, 9, and 14.
 * 
 * This alarm works only for integer and status channels.
 * <p>
 * DIGITAL: Represents a digital mask alarm for channel values. This is a legacy
 * alarm, and will likely be deprecated in the future.
 * 
 * valueMask is a hexadecimal number identifying the position of the bits to be
 * checked. When you set valueMask to 0, no bits will be checked and no alarm
 * processing will occur. When valueMask equals any non-zero hexadecimal number,
 * then converting the mask to a binary number indicates by its pattern of ones
 * and zeros which bits are to be checked and which are not. For example, 0xC
 * (=12, decimal) is 1100, binary, specifying that the third and fourth bits
 * from the right will be checked.
 * 
 * validMask is a hexadecimal number identifying the values of the bits to be
 * checked. If any bit being checked (valueMask) is equal to the value of the
 * same bit in validMask, then the channel is in alarm. Following the values
 * given in the mask1 example, above, if validMask is 0x8, (8-decimal or
 * 1000-binary), then if the fourth bit from the right = 1, the channel is in
 * alarm, and if the third bit from the right (the only ones specified in the
 * above example) = 0, the channel would also be in alarm. Any one bit in alarm
 * puts the whole channel in alarm.
 * 
 * For example, red_alarm=0xC,0x8 (1100 and 1000 binary, respectively),
 * specifies that only the leftmost two bits of a 4-bit sequence will be checked
 * against 1000 so for a channel value of 0xC (1100 binary) the leftmost digit
 * matches (alarm) the next does not (no alarm). The channel is therefore in
 * alarm. On the other hand, for 0x6 channel value (0110 binary) the leftmost
 * bit does not match and neither does the next, so the channel is not in alarm.
 * <p>
 * STATE: Represents a channel state compare alarm, in which a channel is
 * considered to be in alarm if its value matches one value in a list of numeric
 * states. This alarm works only for integer and status channels.
 * <p>
 * CHANGE: Represents a value change alarm, in which a channel is considered to
 * be in alarm if its value is not equal to its previously reported value. This
 * alarm works for all types of channels.
 * <p>
 * DELTA: Represents a value delta alarm, in which a channel is considered to be
 * in alarm if its value changes by an amount equal to or greater than the delta
 * threshold value. This alarm works only on numeric channels.
 * <p>
 * COMPOUND: represents an alarm that is actually groups of alarms on the same
 * channel meant to be associated together. The child alarms are combined using
 * a logical OR, AND, or XOR to determine the compound alarm state. See the
 * ICompoundAlarmDefinition interface for addoital methods that apply to these
 * alarms.
 *
 * 
 * @see IAlarmDictionary
 * @see AlarmDefinitionFactory
 */
public interface IAlarmDefinition extends IAttributesSupport, ICategorySupport {

	/**
	 * Gets the alarm identifier, which should be unique.
	 * @return the alarm ID
	 * 
	 */
	public String getAlarmId();

	/**
	 * Sets the alarm identifier, which should be unique.
	 * @param alarmId the ID to set
	 */
	public void setAlarmId(final String alarmId);

	/**
	 * Gets the channel ID associated with this alarm, indicating which
	 * telemetry channel the alarm applies to.
	 * 
	 * @return the channel ID string
	 */
	public String getChannelId();

	/**
	 * Sets the channel ID associated with this alarm, indicating which
	 * telemetry channel the alarm applies to.
	 * 
	 * @param channelId the channel ID string to set
	 */
	public void setChannelId(String channelId);

	/**
	 * Gets the type of this alarm.
	 * 
	 * @return the AlarmType
	 */
	public AlarmType getAlarmType();

	/**
	 * Retrieves the alarm level.
	 * 
	 * @return Returns the alarm level
	 */
	public AlarmLevel getAlarmLevel();

	/**
	 * Sets the alarm level.
	 * 
	 * @param alarmLevel the level to set
	 */
	public void setAlarmLevel(AlarmLevel alarmLevel);

	/**
	 * Sets the flag indicating the alarm check is on the channel engineering units
	 * as opposed to the data number.
	 * 
	 * @param enable true if the check is on EU; false if on DN
	 */
	public void setCheckOnEu(boolean enable);

	/**
	 * Indicates whether the alarm check is on the channel data number.
	 * 
	 * @return true if the check is on DN; false if on EU
	 */
	public boolean isCheckOnDn();

	/**
	 * Indicates whether the alarm check is on the channel engineering units.
	 * 
	 * @return true if the check is on EU; false if on DN
	 */
	public boolean isCheckOnEu();

	/**
	 * Gets the IN hysteresis count for this alarm.
	 * 
	 * @return In hysteresis count
	 */
	public int getHysteresisInValue();

	/**
	 * Sets the IN hysteresis count for this alarm.
	 * 
	 * @param inHysteresisValue hysteresis count to set
	 */
	public void setHysteresisInValue(int inHysteresisValue);

	/**
	 * Gets the OUT hysteresis count for this alarm.
	 * 
	 * @return Out hysteresis count
	 */
	public int getHysteresisOutValue();

	/**
	 * Sets the OUT hysteresis count for this alarm.
	 * 
	 * @param outHysteresisValue hysteresis count to set
	 */
	public void setHysteresisOutValue(int outHysteresisValue);

	/**
	 * Indicates whether this alarm includes hysteresis checking
	 * 
	 * @return true if alarm has hysteresis IN or OUT counts greater than 1
	 */
	public boolean hasHysteresis();

	/**
	 * Gets the number of channel values that must be kept in alarm history to
	 * calculate this alarm. This is calculated as a function of the alarm type
	 * and hysteresis settings.
	 * 
	 * @return required history count
	 */
	public int getHistoryCount();

	/**
	 * Gets the display string for this alarm, suitable for use in user 
	 * interfaces.
	 * 
	 * @return the display text
	 */
	public String getDisplayString();

	/**
	 * Retrieves the delta limit for a value delta alarm. Used
	 * only for delta alarms.
	 * 
	 * @return the delta value
	 */
	public double getDeltaLimit();

	/**
	 * Sets the delta limit for a value delta alarm. Used
	 * only for delta alarms.
	 * 
	 * @param deltaLimit the delta value to set
	 */
	public void setDeltaLimit(final double deltaLimit);

	/**
	 * Adds a list of alarm states to the current list of states in a state alarm. Used 
	 * only for state alarms.
	 * 
	 * @param newAlarmStates the list of state values to add
	 */
	public void addAlarmStates(final List<Long> newAlarmStates); 

	/**
	 * Gets the list of alarm states for a state alarm. The returned list of states is not modifiable. Used only
	 * for state alarms.
	 * 
	 * @return a List of longs, or null if no states defined
	 */
	public List<Long> getAlarmStates();

	/**
	 * Gets the value mask. Used only for mask and digital alarms.
	 * 
	 * @return the value mask
	 */
	public long getValueMask();

	/**
	 * Sets the value mask. Used only for mask and digital alarms.
	 * 
	 * @param mask the value mask to set
	 */
	public void setValueMask(final long mask);

	/**
	 * Retrieves the lower bound on a low value or range alarm. Used only for 
	 * those alarm types
	 * 
	 * @return the lower limit
	 */
	public double getLowerLimit();

	/**
	 * Sets the lower bound on a low value or range alarm. Used only for 
	 * those alarm types
	 * 
	 * @param lowerLimit the limit to set
	 */
	public void setLowerLimit(final double lowerLimit);

	/**
	 * Retrieves the upper bound on high value or range alarm. Used only
	 * for those alarm types.
	 * 
	 * @return the upper limit
	 */
	public double getUpperLimit();

	/**
	 * Sets the upper bound on a high value or range alarm. Used only for
	 * those alarm types.
	 * 
	 * @param upperLimit the upperLimit to set
	 */
	public void setUpperLimit(final double upperLimit);

	/**
	 * Returns the valid mask on a digital alarm. Used only for digital alarms.
	 * 
	 * @return the valid mask.
	 */
	public long getDigitalValidMask();

	/**
	 * Sets the valid mask on a digital alarm. Used only for digital alarms.
	 * @param mask the mask to set
	 */
	public void setDigitalValidMask(final long mask);

	/**
	 * Produces human-consumable text describing the alarm, with or without
	 * alarm ID.
	 * 
	 * @param includeId true to include alarm ID in the output, false if not
	 * 
	 * @return text describing the alarm
	 */
	public String toString(boolean includeId);

	/**
	 * Sets the alarm description of a channel.
	 * @param desc the string description of the alarm
	 */
	public void setAlarmDescription(String desc);
	
	/**
	 * Gets the alarm description of a channel.
	 * 
	 * @return Returns the alarm description.
	 */
	public String getAlarmDescription();
		
		
}