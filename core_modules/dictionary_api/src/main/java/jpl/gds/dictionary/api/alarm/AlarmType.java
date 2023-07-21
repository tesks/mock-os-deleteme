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
 * AlarmType is an enumeration that defines all the valid types for telemetry
 * channel alarms.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * AlarmType is an enumeration of supported telemetry channel alarm types. Every
 * AlarmDefinition object has an associated AlarmType. AlarmDefinitions of the
 * proper type can be created using the AlarmDefinitionFactory.
 *
 *
 * @see IAlarmDefinition
 * @see AlarmDefinitionFactory
 */
public enum AlarmType {
	/**
	 * Undefined Alarm type
	 */
	NO_TYPE,
	/**
	 * High value alarm
	 */
	HIGH_VALUE_COMPARE,
	/**
	 * Low value alarm
	 */
	LOW_VALUE_COMPARE,
	/**
	 * State comparison alarm
	 */
	STATE_COMPARE,
	/**
	 * Change alarm
	 */
	VALUE_CHANGE,
	/**
	 * Delta alarm
	 */
	VALUE_DELTA,
	/**
	 * Exclusive range alarm
	 */
	EXCLUSIVE_COMPARE,
	/**
	 * Inclusive range alarm
	 */
	INCLUSIVE_COMPARE,
	/**
	 * Digital mask alarm
	 */
	DIGITAL_COMPARE,
	/**
	 * Plain mask alarm
	 */
	MASK_COMPARE,
	/**
	 * Compound alarm (combines multiple alarms on the same channel)
	 */
	COMPOUND,
	/**
	 * Combination alarm (combines alarms on different channels)
	 */
	COMBINATION, // Included here as placeholder, but not used
	/**
	 * Combination alarm source proxy
	 */
	COMBINATION_SOURCE, 
	/**
	 * Combination alarm target proxy
	 */
	COMBINATION_TARGET;


	/**
	 * Gets the state display text for this alarm type.
	 * 
	 * @return the display text
	 */
	public String getDisplayState() {
		switch (this) {
		case NO_TYPE:
			return "";
		case HIGH_VALUE_COMPARE:
			return "high";
		case LOW_VALUE_COMPARE:
			return "low";
		case STATE_COMPARE:
			return "state";
		case VALUE_CHANGE:
			return "change";
		case VALUE_DELTA:
			return "delta";
		case EXCLUSIVE_COMPARE:
			return "excl";
		case INCLUSIVE_COMPARE:
			return "incl";
		case DIGITAL_COMPARE:
			return "digital";
		case MASK_COMPARE:
			return "mask";
		case COMPOUND:
			return "compound";
		case COMBINATION:
		case COMBINATION_SOURCE:
			return "shouldn't_see_this";
			/*
			 * 09/19/2013 EhaChannelMessage's binary
			 * parser naturally sets the state string to the value returned by
			 * this method. Since COMBINATION_TARGET is one of those types
			 * parsed and because in other processes, such as the LAD Keeper,
			 * AlarmDefinitionTable may not be available for the binary parser,
			 * return a somewhat useful string.
			 */
		case COMBINATION_TARGET:
			return "combination";
		default:
			return "";
		}
	}
}
