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
package jpl.gds.eha.api.channel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.channel.ChannelListRange;
import jpl.gds.shared.channel.ChannelListRangeException;

/**
 * A class used to filter channel values.  Originally designed for use by the MTAK
 * downlink proxy's filtering of what channel values to pass onto MTAK, but can be modified
 * and used for general purposes as well.
 * 
 * I realize there's a lot of very similar code in all of the add*** methods, but I chose to
 * split them out separately in case the way we filter them needs to change on an individual
 * field basis.  It's also just way easier to read the code this way.
 * 
 */
public class ChannelValueFilter
{
	private boolean filterSet;
	private final Set<String> channelIds;
	private final Set<String> modules;
	private final Set<String> subsystems;
	private final Set<String> opsCategories;
	private final Set<ChannelDefinitionType> sources;
	
	/**
	 * Creates an instance of ChannelValueFilter that by
	 * default accepts all channel values.
	 */
	public ChannelValueFilter()
	{
		channelIds = new HashSet<String>();
		modules = new HashSet<String>();
		subsystems = new HashSet<String>();
		opsCategories = new HashSet<String>();
		sources = new HashSet<ChannelDefinitionType>();
		filterSet = false;
	}
	
	/**
	 * Returns true if the input channel value passes the filter or false otherwise. Note that if you
	 * input "null" or if the channel value has a "null" ChannelDefinition, this method will just
	 * return false.
	 * 
	 * @param value The channel value to test against the filter
	 * 
	 * @return True if the input channel value passes the filter. False otherwise.
	 */
	public boolean accept(final IClientChannelValue value)
	{
		if(!filterSet)
		{
			return(true);
		}
		
	    if(value == null)
	    {
		    return(false);
	    }
				
		/* 3/2/2016 - Added definition type check */
		/* 6/22/2022 - Fix MTAK filtering */
		if ( (!channelIds.isEmpty() && channelIds.contains(value.getChanId()))
				|| (!modules.isEmpty() && modules.contains(value.getCategory(ICategorySupport.MODULE)))
				|| (!subsystems.isEmpty() && subsystems.contains(value.getCategory(ICategorySupport.SUBSYSTEM)))
				|| (!opsCategories.isEmpty() && opsCategories.contains(value.getCategory(ICategorySupport.OPS_CAT))) )
		{
			return !sources.isEmpty() && sources.contains(value.getDefinitionType());
		}
		// no filters specified, still need to check sources
		if (channelIds.isEmpty() && modules.isEmpty() && subsystems.isEmpty() && opsCategories.isEmpty()) {
			return !sources.isEmpty() && sources.contains(value.getDefinitionType());
		}
		return false;
	}
	
	/**
	 * Add all the channel IDs in the CSV string to this filter.
	 * 
	 * @param csv A comma-separated list of channel IDs to be accepted by this filter.
	 * 
	 * @throws ChannelListRangeException If the parser encounters a malformed channel ID or channel ID range.
	 */
	public void addChannelIds(final String csv) throws ChannelListRangeException
	{
		addChannelIds(csv.split(",{1}"));
	}
	
	/**
	 * Add the list of channel IDs to this filter.
	 * 
	 * @param ids The list of channel IDs & channel ID ranges (e.g. TIME-0001..TIME-0009) to add to the filter
	 * as acceptable channel IDs.
	 * 
	 * @throws ChannelListRangeException If the parser encounters a malformed channel ID or channel ID range.
	 */
	public void addChannelsIds(final List<String> ids) throws ChannelListRangeException
	{
		addChannelIds(ids.toArray(new String[ids.size()]));
	}
	
	/**
	 * Add the array of channel IDs to this filter.
	 * 
	 * DEV NOTE: This method is the only method for adding channel IDs that sets the "filterSet" flag
	 * to true (since the other "addChannelIds" methods just call this one).  If you add a new one that
	 * doesn't call this one, make sure you set the "filterSet" flag also.
	 * 
	 * DEV NOTE: "trim()" is called on each array entry before it is added
	 *  
	 * @param ids The list of channel IDs & channel ID ranges (e.g. TIME-0001..TIME-0009) to add to the filter
	 * as acceptable channel IDs.
	 * 
	 * @throws ChannelListRangeException If the parser encounters a malformed channel ID or channel ID range.
	 */
	public void addChannelIds(final String[] ids) throws ChannelListRangeException
	{
		filterSet = true;
		
		final ChannelListRange range = new ChannelListRange();
		for(final String entry : range.genChannelListFromRange(ids))
    	{
    		channelIds.add(entry.trim());
    	}
	}
	
	/**
	 * Add all the modules in the CSV string to this filter.
	 * 
	 * @param csv A comma-separated list of modules to be accepted by this filter.
	 */
	public void addModules(final String csv)
	{
		addModules(csv.trim().split(",{1}"));
	}
	
	/**
	 * Add the list of modules to this filter.
	 * 
	 * @param modules The list of modules to add to the filter as acceptable modules.
	 */
	public void addModules(final List<String> modules)
	{
		addModules(modules.toArray(new String[modules.size()]));
	}
	
	/**
	 * Add the array of modules to this filter.
	 * 
	 * DEV NOTE: This method is the only method for adding modules that sets the "filterSet" flag
	 * to true (since the other "addModules" methods just call this one).  If you add a new one that
	 * doesn't call this one, make sure you set the "filterSet" flag also.
	 * 
	 * DEV NOTE: "trim()" is called on each array entry before it is added
	 *  
	 * @param modules The list of modules to add to the filter as acceptable modules.
	 */
	public void addModules(final String[] modules)
	{
		filterSet = true;
		
		for(final String module : modules)
		{
			this.modules.add(module.trim());
		}
	}
	
	/**
	 * Add all the subsystems in the CSV string to this filter.
	 * 
	 * @param csv A comma-separated list of subsystems to be accepted by this filter.
	 */
	public void addSubsystems(final String csv)
	{
		addSubsystems(csv.trim().split(",{1}"));
	}
	
	/**
	 * Add the list of subsystems to this filter.
	 * 
	 * @param subsystems The list of subsystems to add to the filter as acceptable subsystems.
	 */
	public void addSubsystems(final List<String> subsystems)
	{
		addSubsystems(subsystems.toArray(new String[subsystems.size()]));
	}
	
	/**
	 * Add the array of subsystems to this filter.
	 * 
	 * DEV NOTE: This method is the only method for adding subsystems that sets the "filterSet" flag
	 * to true (since the other "addSubsystems" methods just call this one).  If you add a new one that
	 * doesn't call this one, make sure you set the "filterSet" flag also.
	 * 
	 * DEV NOTE: "trim()" is called on each array entry before it is added
	 *  
	 * @param subsystems The list of subsystems to add to the filter as acceptable subsystems.
	 */
	public void addSubsystems(final String[] subsystems)
	{
		filterSet = true;
		
		for(final String subsystem : subsystems)
		{
			this.subsystems.add(subsystem.trim());
		}
	}
	
	/**
	 * Add all the operational categories in the CSV string to this filter.
	 * 
	 * @param csv A comma-separated list of operational categories to be accepted by this filter.
	 */
	public void addOpsCategories(final String csv)
	{
		addOpsCategories(csv.trim().split(",{1}"));
	}
	
	/**
	 * Add the list of operational categories to this filter.
	 * 
	 * @param opsCategories The list of operational categories to add to the filter as acceptable operational categories.
	 */
	public void addOpsCategories(final List<String> opsCategories)
	{
		addOpsCategories(opsCategories.toArray(new String[opsCategories.size()]));
	}
	
	/**
	 * Add the array of ops categories to this filter.
	 * 
	 * DEV NOTE: This method is the only method for adding ops categories that sets the "filterSet" flag
	 * to true (since the other "addOpsCategories" methods just call this one).  If you add a new one that
	 * doesn't call this one, make sure you set the "filterSet" flag also.
	 * 
	 * DEV NOTE: "trim()" is called on each array entry before it is added
	 *  
	 * @param opsCategories The list of ops categories to add to the filter as acceptable ops categories.
	 */
	public void addOpsCategories(final String[] opsCategories)
	{
		filterSet = true;
		
		for(final String opsCategory : opsCategories)
		{
			this.opsCategories.add(opsCategory.trim());
		}
	}
	
	/**
	 * Add a telemetry source to this filter.
	 */
	public void addSource(final ChannelDefinitionType defType) {
		filterSet = true;
		
		this.sources.add(defType);
	}

	/**
	 * Accessor for the filter set flag
	 * 
	 * @return True if any fields have been set on this filter, false if the filter still accepts everything
	 */
	public boolean isFilterSet()
	{
		return filterSet;
	}

	/**
	 * Accessor for channel IDs
	 * 
	 * @return The set of channel IDs accepted by this filter.
	 */
	public Set<String> getChannelIds()
	{
		return channelIds;
	}

	/**
	 * Accessor for modules
	 * 
	 * @return The set of modules accepted by this filter.
	 */
	public Set<String> getModules()
	{
		return modules;
	}

	/**
	 * Accessor for subsystems
	 * 
	 * @return The set of subsystems accepted by this filter.
	 */
	public Set<String> getSubsystems()
	{
		return subsystems;
	}

	/**
	 * Accessor for opsCategories
	 * 
	 * @return The set of ops categories accepted by this filter.
	 */
	public Set<String> getOpsCategories()
	{
		return opsCategories;
	}
	
	/**
	 * Accessor for sources
	 * 
	 * @return The set of telmetry sources accepted by this filter.
	 */
	public Set<ChannelDefinitionType> getSources()
	{
		return sources;
	}
	
	/**
	 * Reset this filter to empty so that it will accept every
	 * channel value.
	 */
	public void clear()
	{
		filterSet = false;
		channelIds.clear();
		modules.clear();
		subsystems.clear();
		opsCategories.clear();
		sources.clear();
	}
}
