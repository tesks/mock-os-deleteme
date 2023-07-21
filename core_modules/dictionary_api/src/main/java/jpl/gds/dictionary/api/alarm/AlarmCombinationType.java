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
 * AlarmCombinationType is an enumeration that defines the possible logical
 * operators for combining child alarms into compound or combination alarms.
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p><p>
 * 
 * AlarmCombinationType is an enumeration of logical alarm operators. These
 * define how the child alarms in combination or compound alarms are computed.
 * 
 *
 * @see ICompoundAlarmDefinition
 * @see ICombinationGroup
 */
public enum AlarmCombinationType {
	/**
	 * Alarm is triggered if one or more child alarms are triggered.
	 */
	OR,
	/**
	 * Alarm is triggered if all child alarms are triggered.
	 */
	AND,
    /**
     * Alarm is triggered if an odd number of children are triggered. XOR is
     * true whenever an odd number of inputs are true. A chain of XORs -- a XOR b
     * XOR c XOR d (and so on) -- is true whenever an odd number of the inputs are
     * true and is false whenever an even number of inputs are true.
     * (http://en.wikipedia.org/wiki/Exclusive_or)
     */
	XOR;
}
