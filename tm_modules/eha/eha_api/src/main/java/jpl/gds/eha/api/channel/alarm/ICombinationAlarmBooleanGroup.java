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

import java.util.List;

import jpl.gds.dictionary.api.alarm.AlarmCombinationType;
import jpl.gds.dictionary.api.alarm.ICombinationGroup;

/**
 * An interface to be implemented by combination alarm boolean groups that
 * assist in the computation of combination alarms.
 * 
 * @since R8
 */
public interface ICombinationAlarmBooleanGroup extends ICombinationAlarmSourceElement {

    /**
     * Returns the boolean logic group ID.
     *
     * @return the groupId
     */
    public String getGroupId();

    /**
     * Gets the dictionary definition object for this group.
     * 
     * @return ICombinationGroup object
     * 
     */
    public ICombinationGroup getDefinition();

    /**
     * Gets the combination operator type for this group.
     * 
     * @return AlarmCombinationType object
     * 
     */
    public AlarmCombinationType getOperator();
    
    /**
     * Returns the boolean logic group's operands
     * 
     * @return the operands of this group
     */
    public List<ICombinationAlarmSourceElement> getOperands(); 

}