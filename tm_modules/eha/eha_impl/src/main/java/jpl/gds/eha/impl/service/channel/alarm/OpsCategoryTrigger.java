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
 * A channel alarm trigger based upon
 *
 */
public class OpsCategoryTrigger extends AbstractAlarmTrigger
{
	private final List<String> categories;

    /**
     * Constructor.
     */
	public OpsCategoryTrigger()
	{
		super();

		categories = new ArrayList<String>(32);
	}

	    /**
     * Gets the list of operational categories this trigger is based upon.
     * 
     * @return list of ops categories.
     */
	public List<String> getCategories()
	{
		return (categories);
	}

    /**
     * Adds an operational category to the list for this trigger.
     * 
     * @param category
     *            the category to add
     */
	public void addCategory(final String category)
	{
		if(categories.contains(category) == false)
		{
			categories.add(category);
		}
	}
	
	@Override
    public boolean matches(final IServiceChannelValue value)
	{
		final String inCategory = value.getCategory(IChannelDefinition.OPS_CAT);
		for(final String category : categories)
		{
			if(category.equalsIgnoreCase(inCategory))
			{
				return(true);
			}
		}
		
		return(false);
	}
}
