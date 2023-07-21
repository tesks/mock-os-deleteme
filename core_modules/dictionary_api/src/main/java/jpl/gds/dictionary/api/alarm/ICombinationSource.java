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
 * The ICombinationSource interface is to be implemented by combination alarm
 * definition classes that wrap a single source channel alarm and can be
 * utilized as a member of a boolean combination group. It is considered
 * a proxy definition because the alarm is not applied to the source channel
 * directly, but rather to the targets of the combination.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * ICombinationSource defines methods needed to interact with Combination Alarm
 * Source Definition objects that can be used in combination groups, as required
 * by the IAlarmDictionary interface. It is primarily used by alarm file parser
 * implementations in conjunction with the AlarmDefinitionFactory, which is used
 * to create actual Alarm Definition objects in the parsers.
 * 
 *
 * @see AlarmDefinitionFactory
 */
public interface ICombinationSource extends IAlarmDefinition, ICombinationGroupMember {

	/**
	 * Gets the actual source alarm definition this source proxy wraps.
	 * 
	 * @return IAlarmDefinition object; never null
	 */
	public abstract IAlarmDefinition getActualAlarmDefinition();

}