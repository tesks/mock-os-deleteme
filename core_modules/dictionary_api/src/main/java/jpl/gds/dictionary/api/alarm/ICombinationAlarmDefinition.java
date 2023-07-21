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

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.IAttributesSupport;

/**
 * The ICombinationAlarmDefinition interface is to be implemented by combination
 * alarm definition classes. A combination alarm is a mutli-channel alarm
 * consisting of a number of source alarms combined in boolean groups to obtain
 * a final alarm state, which is then applied to a set of target channels, which
 * may or may not match the source channels.
* <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * ICombinationAlarmDefinition defines methods needed to interact with
 * Combination Alarm Definition objects as required by the IAlarmDictionary
 * interface. It is primarily used by alarm file parser implementations in
 * conjunction with the AlarmDefinitionFactory, which is used to create actual
 * Alarm Definition objects in the parsers. IAlarmDictionary objects should
 * interact with Alarm Definition objects only through the Factory and the
 * IAlarmDefinition interfaces. Interaction with the actual Alarm Definition
 * implementation classes in an IAlarmDictionary implementation is contrary to
 * multi-mission development standards.
 * 
 *
 * @see AlarmDefinitionFactory
 */
public interface ICombinationAlarmDefinition extends IAttributesSupport {

	/**
	 * Returns the unique identifier string of this combination alarm.
	 * 
	 * @return the alarmId unique identifier string
	 */
	public abstract String getAlarmId();

	/**
	 * Returns the alarm level of this combination alarm. This is the level
	 * at which targets will be alarmed.
	 *
	 * @return the alarmLevel the alarm level
	 */
	public abstract AlarmLevel getAlarmLevel();
	/**
	 * Sets the alarm description of a channel.
	 * @param desc The string description of the alarm.
	 */
	public void setAlarmDescription(String desc);
	
	/**
	 * Gets the alarm description of a channel.
	 * 
	 * @return Returns the alarm description.
	 */
	public String getAlarmDescription();
	
	/**
	 * Sets the categories.
	 * 
	 * @param map  the map of all the categories
	 */
 	public void setCategories(Categories map);

    /**
     * Gets the map of all the defined categories.  
     * 
     * @return the map of all the defined categories.  
     */
	public Categories getCategories();

	/**
	 * Sets the category name and value.
	 * 
	 * @param catName  the category name
	 * @param catValue  the category value
	 */
	public void setCategory(String catName, String catValue); 
	
	/**
	 * Retrieves the category value from the category name.
	 * 
	 * @param catName  the category name
	 *
	 * @return catValue  the category value
	 */
	public String getCategory(String catName);
	
	/**
	 * Sets the top source element for this combination alarm, which must be an
	 * ICombinationGroup.
	 * 
	 * @param src
	 *            source element
	 * @throws IllegalStateException
	 *             thrown when setting the source element is not allowed
	 */
	public abstract void setSourceGroup(ICombinationGroup src)
			throws IllegalStateException;

	/**
	 * Adds a new target proxy to this combination alarm.
	 * 
	 * @param target
	 *            new target proxy definition to add to the list of alarm
	 *            targets
	 * @throws IllegalStateException if the combination alarm is not in a state
	 *         to add target alarms
	 */
	public abstract void addTarget(ICombinationTarget target)
			throws IllegalStateException;

	/**
	 * Complete the definition of this combination alarm definition following
	 * parsing. If no target proxies have been specified, build the target
	 * proxies from the source elements. Source and targets will be frozen after
	 * this call and can no longer be modified. Successful completion of this
	 * method call gives the caller assurance that the combination alarm
	 * definition is validated to be safe, and can be added to the alarm tables.
	 * 
	 * @throws IllegalStateException
	 *             thrown when the combination alarm defined thus far cannot
	 *             function properly as is (e.g. missing the source element)
	 */
	public abstract void build() throws IllegalStateException;

	/**
	 * Gets the top level source group combination for this combination alarm.
	 * 
	 * @return ICombinationGroup object
	 */
	public ICombinationGroup getSourceGroup();

	/**
	 * Returns all the source proxy definitions associated with this combination
	 * alarm as a single flattened list. (Note: "build" has to be called first.)
	 * 
	 * @return list of source alarm proxy definitions
	 */
	public abstract List<ICombinationSource> getFlatSources();

	/**
	 * Returns the target proxy definitions associated with this combination
	 * alarm. (Note: "build" has to be called first.)
	 * 
	 * @return list of target alarm proxy definitions
	 */
	public abstract List<ICombinationTarget> getTargets();

	/**
	 * Gets the flag indicating whether all the source channels for this
	 * combination are also the target channels. If this is the case, there
	 * will be no target definitions following the dictionary parse until
	 * after build() is invoked.
	 * 
	 * @return true if target channels are the same as source channels, false if not
	 */
	public boolean getSourcesAreTargets();
}