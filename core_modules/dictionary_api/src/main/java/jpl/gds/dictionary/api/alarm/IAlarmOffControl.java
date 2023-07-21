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

/**
 * The IAlarmOffControl interface is to be implemented by alarm off control
 * classes, which are used to disable alarms. <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * <p>
 * IAlarmOffControl objects are generated by IAlarmDictionary instance in order
 * to identify alarms that have been disabled in the alarm file.
 * IAlarmOffControl objects should only be created via the
 * AlarmDefinitionFactory.
 * 
 *
 *
 * @see AlarmDefinitionFactory
 */
public interface IAlarmOffControl {

	/**
	 * Gets the scope of this control.
	 * 
	 * @return an OffScope enum value
	 */
	public abstract OffScope getScope();

	/**
	 * Gets the channel ID of the channel this control applies to; will be null
	 * if the scope is OffScope.ALARM.
	 * 
	 * @return channel ID; may be null
	 */
	public abstract String getChannelId();

	/**
	 * Gets the alarm ID of the alarm this control applies to; will be null if
	 * the scope is OffScope.CHANNEL or OffScope.CHANNEL_AND_LEVEL.
	 * 
	 * @return alarm ID; may be null
	 */
	public abstract String getAlarmId();

	/**
	 * Gets the level of the alarms this control applies to; will be
	 * AlarmLevel.NONE if the scope is not OffScope.CHANNEL_AND_LEVEL.
	 * 
	 * @return alarm level
	 */
	public abstract AlarmLevel getLevel();

}