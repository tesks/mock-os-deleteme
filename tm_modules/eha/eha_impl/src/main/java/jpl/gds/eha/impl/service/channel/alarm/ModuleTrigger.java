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
 * A channel alarm trigger based upon flight software module.
 *
 *
 */
public class ModuleTrigger extends AbstractAlarmTrigger
{
	private final List<String> modules;

    /**
     * Constructor.
     */
	public ModuleTrigger()
	{
		super();

		modules = new ArrayList<String>(32);
	}

	    /**
     * Gets the list of modules this trigger is based upon.
     * 
     * @return the module list
     */
	public List<String> getModules()
	{
		return (modules);
	}

    /**
     * Adds a module to the trigger.
     * 
     * @param module
     *            module name to add
     */
	public void addModule(final String module)
	{
		if(modules.contains(module) == false)
		{
			modules.add(module);
		}
	}
	
	@Override
    public boolean matches(final IServiceChannelValue value)
	{
		final String inModule = value.getCategory(IChannelDefinition.MODULE);
		for(final String module : modules)
		{
			if(module.equalsIgnoreCase(inModule))
			{
				return(true);
			}
		}
		
		return(false);
	}
}
