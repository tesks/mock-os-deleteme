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
package jpl.gds.eha.impl.service.channel.alarm;

/**
 * An enumeration of alarm notification state values.
 * 
 */
public enum AlarmNotificationState
{
    /** Notify on any yellow alarm */
	YELLOW,
    /** Notify on first yellow alarm */
	YELLOW_FIRST,
    /** Notify on any red alarm */
	RED,
    /** Notify on first red alarm. */
	RED_FIRST,
    /** Notify on any alarm state change */
	CHANGE_ANY,
    /** Notify any time an alarm is cleared */
	CHANGE_CLEAR,
    /** Notify any time an alarm is set */
	CHANGE_SET
}