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
import java.util.Map;

import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Jira;
import jpl.gds.shared.annotation.Mutator;

/**
 * The IAlarmDictionary interface is to be implemented by all alarm dictionary
 * adaptation classes.
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b><p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * The JPL document corresponding to the multimission alarm dictionary schema
 * is D-001139, the AMPCS Multimission Alarm Dictionary SIS, in the JPL MGSS
 * Document Management System (DMS).
 * <p>
 * The Alarm dictionary is used by the telemetry processing system to determine
 * whether channelized telemetry values are off-nominal. Channel values that are
 * off-nominal can be alarmed based upon a number of possible conditions. These
 * conditions, and the channels they apply to, are defined in the project's
 * alarm dictionary. Every mission may have a different format for representing
 * the alarm dictionary. An appropriate dictionary parser must be used in order
 * to create the mission-specific IAlarmDictionary object, which must implement
 * this interface. IAlarmDictionary objects should only be created via the
 * AlarmDictionaryFactory. Direct creation of an IAlarmDictionary object is a
 * violation of multimission development practices.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the dictionary contents, respectively.
 * <p>
 * The primary job of the IAlarmDictionary object is to parse an alarm
 * dictionary file and produce a set of IAlarmDefinition objects and a set of
 * ICombinationAlarmDefinition objects. Each IAlarmDefinition is the
 * multimission representation of a simple (single channel) alarm defined in
 * the dictionary. Each ICombinationAlarmDefinition is the multimission
 * representation of a multi-channel alarm defined in the dictionary.
 * <p>
 * 
 *
 * 
 * @see IAlarmDictionaryFactory
 * @see jpl.gds.dictionary.api.IBaseDictionary
 */
@CustomerAccessible(immutable = false)
public interface IAlarmDictionary extends IBaseDictionary, IAlarmDefinitionProvider {

	/**
	 * Retrieves a list of parsed single channel IAlarmDefinitions from the
	 * IAlarmDictionary. Does not include combination (multi-channel) alarm
	 * definitions, only the single channel alarm definitions.
	 * 
	 * @return list of IAlarmDefinition objects; list will be empty if no alarm
	 *         definitions exist, but not null
	 */
	public List<IAlarmDefinition> getSingleChannelAlarmDefinitions();

	/**
	 * Retrieves a map of parsed single-channel alarm definitions, keyed by
	 * alarm ID. No objects related to combination (multi-channel) alarms are
	 * returned.
	 * 
	 * @return Map of Lists of IAlarmDefinition objects, keyed by channel ID
	 */
	public Map<String, IAlarmDefinition> getSingleChannelAlarmMapByAlarmId();

	/**
	 * Sets the mapping of channel IDs to IChannelDefinition objects to be used
	 * as the channel reference when parsing the alarm dictionary. These are the
	 * only channels for which IAlarmDefinitions can be created by the parser.
	 * Should be called before the parse() method is invoked.
	 * 
	 * @param channelMap
	 *            Map of IChannelDefinition objects, keyed by the channel ID;
	 *            may not be null
	 */
    @Mutator
	public void setChannelMap(Map<String, IChannelDefinition> channelMap);
	
	/**
     * Gets the list of off control objects. These are used to represent any
     * alarms disabled by the dictionary file last parsed by this
     * IAlarmDictionary object. Note that an alarm that is disabled and then
     * re-enabled in the same alarm file should NOT be returned as an off
     * control. Off Control objects should only be returned by the parser if the
     * last state for the specified alarm or channel is disabled.
     * 
     * @return List of IAlarmOffControl objects; may be empty, but never null
     */
    public List<IAlarmOffControl> getOffControls();

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    default public boolean isLoaded() {
        return true;
    }

}
