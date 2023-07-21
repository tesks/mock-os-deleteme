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
 * The ICombinationGroup interface is to be implemented by combination alarm
 * group classes, which combine source alarms using a boolean operator.
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
 * ICombinationGroup defines methods needed to interact with Combination Alarm
 * Group Definition objects as required by the IAlarmDictionary interface. It is
 * primarily used by alarm file parser implementations in conjunction with the
 * AlarmDefinitionFactory, which is used to create actual Alarm Definition
 * objects in the parsers. Combination groups are used to combine
 * ICombinationGroupMember objects using boolean operators to compute alarm
 * state.
 * 
 *
 * @see AlarmDefinitionFactory
 */
public interface ICombinationGroup extends ICombinationGroupMember {
	/**
	 * Add an operand to this boolean logic group.
	 * 
	 * @param operand
	 *            the operand to add
	 */
	public void addOperand(final ICombinationGroupMember operand);

	/**
	 * Gets the unique ID for this group.
	 * 
	 * @return group ID string
	 */
	public String getGroupId();

	/**
	 * Returns the boolean logic group's operands (i.e., source alarms).
	 * 
	 * @return the operands of this group
	 */
	public List<ICombinationGroupMember> getOperands();

	/**
	 * Gets the boolean operator used to combine source alarms in this group.
	 * @return AlarmCombinationType enum value
	 */
	public AlarmCombinationType getOperator();
}
