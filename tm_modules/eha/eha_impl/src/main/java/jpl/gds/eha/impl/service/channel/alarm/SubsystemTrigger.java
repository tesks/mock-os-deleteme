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
package jpl.gds.eha.impl.service.channel.alarm;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.IServiceChannelValue;

/**
 * A channel alarm trigger based upon flight subsystem.
 *
 */
public class SubsystemTrigger extends AbstractAlarmTrigger
{
	private final List<String> subsystems;

    /**
     * Constructor.
     */
	public SubsystemTrigger()
	{
		super();

		subsystems = new ArrayList<String>(32);
	}

	    /**
     * Gets the list of subsystems this trigger is based upon.
     * 
     * @return the list of subsystems
     */
	public List<String> getSubsystems()
	{
		return (subsystems);
	}

    /**
     * Adds a subsystem to the list of subsystems in this trigger.
     * 
     * @param subsystem
     *            subsystem to add
     */
	public void addSubsystem(final String subsystem)
	{
		if(subsystems.contains(subsystem) == false)
		{
			subsystems.add(subsystem);
		}
	}
	
	@Override
    public boolean matches(final IServiceChannelValue value)
	{
		final String inSubsystem = value.getCategory(IChannelDefinition.SUBSYSTEM);
		for(final String subsystem : subsystems)
		{
			if(subsystem.equalsIgnoreCase(inSubsystem))
			{
				return(true);
			}
		}
		
		return(false);
	}
}
