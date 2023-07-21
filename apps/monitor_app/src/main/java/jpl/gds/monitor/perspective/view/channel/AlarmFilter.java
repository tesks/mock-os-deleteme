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
package jpl.gds.monitor.perspective.view.channel;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.IChannelDefinition;


/**
 * AlarmFilter is used as to store and test filtering criteria for display or processing 
 * of EHA alarms. Filtering criteria include channel ID, subsystem, module, category, and
 * alarm level.
 *
 */
public class AlarmFilter {
	private ChannelSet channels;
	private String subsystem;
	private String module;
	private String category;
	private AlarmLevel level;

	/**
	 * Creates an instance of AlarmFilter.
	 * @param channelSet the channelSet (channel IDs) to filter on
	 * @param sub the FSW subsystem to filter on
	 * @param mod the FSW module to filter on
	 * @param cat the operational category to filter on
	 * @param level the alarm level to filter on
	 */
	public AlarmFilter(final ChannelSet channelSet, final String sub, final String mod, final String cat, final AlarmLevel level) {
		channels = channelSet;
		subsystem = sub;
		module = mod;
		category = cat;
		this.level = level;
	}

	/**
	 * Gets the channel set that defines any specific channels for this filter.
	 * 
	 * @return the ChannelSet, or null if not filtering for specific channels.
	 */
	public ChannelSet getChannels() {
		return channels;
	}

	/**
	 * Sets the channels set to filter for.
	 *
	 * @param channels The channels to set.
	 */
	public void setChannels(final ChannelSet channels) {
		this.channels = channels;
	}

	/**
	 * Gets the subsystem for this filter.
	 * 
	 * @return the subsystem, or null if not filtering for specific subsystem
	 */
	public String getSubsystem() {
		return subsystem;
	}

	/**
	 * Sets the subsystem for this filter.
	 *
	 * @param subsystem The subsystem to set.
	 */
	public void setSubsystem(final String subsystem) {
		this.subsystem = subsystem;
	}

	/**
	 * Gets the module for this filter.
	 * 
	 * @return the module, or null if not filtering for a specific module
	 */
	public String getModule() {
		return module;
	}

	/**
	 * Sets the module for this filter.
	 *
	 * @param module The module to set.
	 */
	public void setModule(final String module) {
		this.module = module;
	}

	/**
	 * Gets the operations category for this filter.
	 * 
	 * @return the operations category, or null if not filtering for a specific category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Sets the operations category for this filter.
	 *
	 * @param category The category to set.
	 */
	public void setCategory(final String category) {
		this.category = category;
	}

	/**
	 * Gets the alarm level for this filter.
	 * 
	 * @return the alarm level filter, or null if not filtering for level
	 */
	public AlarmLevel getLevel() {
		return level;
	}

	/**
	 * Sets the alarm level filter.
	 *
	 * @param level The alarm level to set.
	 */
	public void setLevel(final AlarmLevel level) {
		this.level = level;
	}

	/**
	 * Checks whether a ChannelSample meets the filtering criteria. All filter criteria are
	 * ANDed together. 
	 * 
	 * @param data the ChannelSample to check for filter match
	 * @return true if the definition meets the filter criterion, false if not
	 */
	public boolean accept(final MonitorChannelSample data) { 

		if (data == null)
		{
			return true;
		}

		if (acceptNoLevel(data) == false) {
			return false;
		}

		final AlarmLevel msgDnLevel = data.getDnAlarmLevel();
		final AlarmLevel msgEuLevel = data.getEuAlarmLevel();

		final boolean accept = acceptLevel(msgDnLevel, msgEuLevel);

		return accept;
	}

	/**
	 * Checks whether a ChannelSample meets the filtering criteria. All filter criteria are
	 * ANDed together. 
	 * 
	 * @param data the ChannelSample to check for filter match
	 * @return true if the definition meets the filter criterion, false if not
	 */
	public boolean acceptNoLevel(final MonitorChannelSample data) { 

		if (data == null)
		{
			return true;
		}

		final IChannelDefinition def = data.getChanDef();

		// If for some reason we do not have a channel definition for the channel
		// just accept the message.
		if (def == null) {
			return true;
		}

        final String msgSubsystem = def.getCategory(IChannelDefinition.SUBSYSTEM);
        final String msgModule = def.getCategory(IChannelDefinition.MODULE);
        final String msgCategory = def.getCategory(IChannelDefinition.OPS_CAT);
		final String msgId = def.getId();


		if (subsystem != null) {
			if (msgSubsystem == null) {
				return false;
			}
			if (!subsystem.equalsIgnoreCase(msgSubsystem)) {
				return false;
			}
		}
		if (category != null) {
			if (msgCategory == null) {
				return false;
			}
			if (!category.equalsIgnoreCase(msgCategory)) {
				return false;
			}
		}
		if (module != null) {
			if (msgModule == null) {
				return false;
			}
			if (!module.equalsIgnoreCase(msgModule)) {
				return false;
			}
		}
		if (channels != null && !channels.isEmpty() && !channels.contains(msgId)) {
			return false;

		}

		return true;
	}

	/**
	 * Checks whether the given alarm levels meets the filtering criteria for AlarmLevel only.  
	 * 
	 * @param msgDnLevel DN alarm level to check
	 * @param msgEuLevel EU alarm level to check
	 * @return true if the input levels match the filter, false if not
	 */
	public boolean acceptLevel(final AlarmLevel msgDnLevel, final AlarmLevel msgEuLevel)
	{
		if(level == AlarmLevel.RED && msgDnLevel != AlarmLevel.RED && msgEuLevel != AlarmLevel.RED)
		{
			return false;
		}

		// A yellow filter only accepts yellow alarms
		if(level == AlarmLevel.YELLOW && msgDnLevel != AlarmLevel.YELLOW && msgEuLevel != AlarmLevel.YELLOW)
		{
			return false;
		}

		// The "NONE" alarm level preferences filter has been removed. Any users
		// that might have had their filter set to NONE will
		// now see their filter behave as "ANY". Their dropdown menu will
		// continue to say NONE until they change it,
		// at which point they will not be able to set NONE anymore.
		if(level == AlarmLevel.NONE)
		{
			level = null;
		}

		if(level == null && msgDnLevel != null && msgEuLevel != null && msgDnLevel != AlarmLevel.RED  && 
				msgEuLevel != AlarmLevel.RED && msgDnLevel != AlarmLevel.YELLOW && msgEuLevel != AlarmLevel.YELLOW)
		{
			return false;
		}

		return true;
	}

	/**
	 * Indicates whether this filter actually filters anything.
	 * 
	 * @return true if any filtering criteria are defined, false if not
	 */
	public boolean isEmpty() {
		return (channels == null || channels.isEmpty()) && module == null && category == null &&
				subsystem == null && level == null;
	}
}
