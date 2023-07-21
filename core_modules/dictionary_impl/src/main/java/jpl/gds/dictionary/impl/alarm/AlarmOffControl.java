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
package jpl.gds.dictionary.impl.alarm;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.IAlarmOffControl;
import jpl.gds.dictionary.api.alarm.OffScope;

/**
 * This class is used to turn alarms off. It is created by IAlarmDictionary
 * objects when a statement that disables an alarm is encountered in the alarm
 * dictionary file. The scope member indicates the range of alarms it disables.
 * <p>
 * The previous implementation represented off controls using the OffAlarm
 * class.
 * 
 *
 */
public class AlarmOffControl implements IAlarmOffControl {
	private final OffScope scope;
	private String channelId;
	private String alarmId;
	private AlarmLevel level = AlarmLevel.NONE;

	/**
	 * Creates an off control scoped to a single channel. Optionally, it can
	 * also be scoped to a specific alarm level. This type Off Control does not
	 * affect the operation of combination (multi-channel) alarms. For those,
	 * use the constructor that takes an alarm ID.
	 * 
	 * @param channelId
	 *            the ID of the channel for which to disable alarms; may not be
	 *            null
	 * @param level
	 *            the level of alarms to disable for the channel; may be null,
	 *            in which case all alarms on the channel are to be disabled.
	 */
	AlarmOffControl(final String channelId, final AlarmLevel level) {

		if (channelId == null) {
			throw new IllegalArgumentException("ChannelId may not be null");
		}
		this.channelId = channelId;
		this.level = level == null ? AlarmLevel.NONE : level;
		/*  Was checking level rather than
		 * this.level below, resulting in failure to properly catch the 
		 * NONE case when null was passed in.
		 */
		this.scope = this.level == AlarmLevel.NONE ? OffScope.CHANNEL
				: OffScope.CHANNEL_AND_LEVEL;
	}

	/**
	 * Creates an off control scoped to a single alarm ID. This may be a simple
	 * (single-channel) alarm ID, or a combination (multi-channel) alarm ID.
	 * 
	 * @param alarmId
	 *            the ID of the alarm to disable; may not be null
	 */
	AlarmOffControl(final String alarmId) {
		if (alarmId == null) {
			throw new IllegalArgumentException("AlarmId may not be null");
		}
		this.alarmId = alarmId;
		this.scope = OffScope.ALARM;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmOffControl#getScope()
	 */
	@Override
	public OffScope getScope() {
		return scope;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmOffControl#getChannelId()
	 */
	@Override
	public String getChannelId() {
		return channelId;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmOffControl#getAlarmId()
	 */
	@Override
	public String getAlarmId() {
		return alarmId;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmOffControl#getLevel()
	 */
	@Override
	public AlarmLevel getLevel() {
		return level;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		switch (this.scope) {
		case ALARM:
			return this.scope + "/" + this.alarmId;
		case CHANNEL:
			return this.scope + "/" + this.channelId;
		case CHANNEL_AND_LEVEL:
			return this.scope + "/" + this.channelId + "/" + this.level;
		}
		return "";
	}
}
