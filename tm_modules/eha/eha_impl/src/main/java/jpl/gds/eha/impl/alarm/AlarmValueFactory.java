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

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.alarm.IAlarmValueFactory;
/**
 * AlarmValueFactory is used to create IAlarmValue objects for use in
 * channel processing and derivation.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * An IAlarmValue object is the multi-mission representation of an alarm instance.
 * Sets of alarm instances are attached to channel values through the IInternalChannelValue
 * interface. In order to isolate the mission adaptation from changes in the
 * multi-mission core, IAlarmValue objects should always be created with
 * this factory. 
 * <p>
 * This class contains only static methods. There is one method per alarm type.
 * Once the IAlarmValue object is returned by this factory, its additional
 * members can be set through the methods in the IAlarmValue interface.
 * 
 *
 * @see IAlarmValue
 * @see AlarmValueSetFactory
 */
public class AlarmValueFactory implements IAlarmValueFactory {

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.alarm.IAlarmValueFactory#create()
	 */
	@Override
	public IAlarmValue create() {
		return new AlarmValue();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.alarm.IAlarmValueFactory#create(jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition, boolean, java.lang.String)
	 */
	@Override
	public IAlarmValue create(final IAlarmDefinition def, final boolean inAlarm, final String displayState) {
		return new AlarmValue(def, inAlarm, displayState);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.channel.alarm.IAlarmValueFactory#create(jpl.gds.dictionary.impl.impl.api.alarm.AlarmType, jpl.gds.dictionary.impl.impl.api.alarm.AlarmLevel, boolean, boolean, java.lang.String)
	 */
	@Override
	public IAlarmValue create(final AlarmType type, final AlarmLevel level, final boolean onEu, final boolean inAlarm, final String displayState, final String alarmId) {
		return new AlarmValue(type, level, onEu, inAlarm, displayState, alarmId);
	}

}
