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


/**
 * The ICompoundAlarmDefinition interface is to be implemented by compound alarm
 * definition classes. Compound alarms combine alarms on the same channel using
 * a boolean operator.
 * <br>
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b>
 * <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable change requests
 * being filed, and approval of project management. A new version tag must be added below 
 * with each revision, and both ECR number and author must be included with the version 
 * number.</b>
 * <br>
 * 
 * ICompoundAlarmDefinition defines methods needed to interact with Compound
 * Alarm Definition objects as required by the IAlarmDictionary interface. It is
 * primarily used by alarm file parser implementations in conjunction with the
 * AlarmDefinitionFactory, which is used to create actual Alarm Definition
 * objects in the parsers. IAlarmDictionary objects should interact with Alarm
 * Definition objects only through the Factory and the IAlarmDefinition
 * interfaces. Interaction with the actual Alarm Definition implementation
 * classes in an IAlarmDictionary implementation is contrary to multi-mission
 * development standards.
 * 
 *
 * @see IAlarmDictionary
 * @see AlarmDefinitionFactory
 * @see IAlarmDefinition
 */
public interface ICompoundAlarmDefinition extends IAlarmDefinition {
	/**
	 * Adds an alarm definition to the list of alarms that are part of this 
	 * compound alarm definition.
	 * 
	 * @param alarm the IAlarmDefinition to add
	 */
	public void addChildAlarmDefinition(IAlarmDefinition alarm);

	/**
	 * Gets the type of logical operation used to combine the child alarms when
	 * computing the compound or combination alarm.
	 * 
	 * @return AlarmCombinationType
	 */
	public AlarmCombinationType getCombinationOperator();

	/**
	 * Sets the type of logical operation used to combine the child alarms when
	 * computing the compound or combination alarm.
	 * 
	 * @param operator the AlarmCombinationType to set
	 */
	public void setCombinationOperator(AlarmCombinationType operator);

	/**
	 * Gets the list of child alarm definitions in this compound alarm.
	 * 
	 * @return the list of AlarmDefinitions, or null if no child alarms defined
	 */
	public List<IAlarmDefinition> getChildAlarms();
}
