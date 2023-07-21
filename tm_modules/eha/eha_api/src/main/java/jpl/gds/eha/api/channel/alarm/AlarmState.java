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

package jpl.gds.eha.api.channel.alarm;

/**
 * Enumerates the three different possible states for an alarm condition.
 *
 * 
 * @since AMPCS R6.1
 */
public enum AlarmState {

	/**
	 * Alarm condition cannot be determined. This occurs particularly with the
	 * combination alarms, for some combinations require that all underlying
	 * source alarms' conditions have been determined at the time of evaluation.
	 */
	UNKNOWN,

	/**
	 * Alarm is on.
	 */
	IN_ALARM,

	/**
	 * Alarm is off. 
	 */
	NOT_IN_ALARM
}
