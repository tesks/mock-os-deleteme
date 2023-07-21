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

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * AlarmLevel is an enumeration that defines the valid severity levels for
 * telemetry channel alarms.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * AlarmLevel is an enumeration of telemetry alarm levels. RED alarms have the
 * highest severity. YELLOW alarms are next in severity, with NONE being last,
 * representing no alarm state. If there is both a RED and a YELLOW alarm on the
 * same channel, the RED state will be the displayed state.
 *
 *
 * @see IAlarmDefinition
 */
@CustomerAccessible(immutable = true)
public enum AlarmLevel
{
	/**
	 * No alarm
	 */
	NONE,
	/**
	 * Yellow alarm
	 */
	YELLOW,
	/**
	 * Red alarm
	 */
	RED;

	/**
	 * Retrieves the minimum (least severe) alarm level.
	 * 
	 * @return AlarmLevel
	 */
	public static AlarmLevel getMinLevel()
	{
		return(NONE);
	}

	/**
	 * Retrieves the maximum (most severe) alarm level.
	 * 
	 * @return AlarmLevel
	 */
	public static AlarmLevel getMaxLevel()
	{
		return(RED);
	}
}