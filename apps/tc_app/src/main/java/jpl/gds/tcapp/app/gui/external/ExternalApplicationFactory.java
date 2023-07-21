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
package jpl.gds.tcapp.app.gui.external;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.tc.api.config.CommandProperties;

/**
 * This factory class creates instances of external application classes to
 * be launched by MPCS.
 * 
 *
 */
public abstract class ExternalApplicationFactory
{
	
	/**
	 * Gets a list of external application objects to be launched by the uplink.
	 * @param appContext the ApplicationContext in which this object is being used
	 * @return list of ExternalApplication objects
	 */
	public static List<ExternalApplication> getUplinkApplications(ApplicationContext appContext)
	{
		final CommandProperties cmdConfig = appContext.getBean(CommandProperties.class);
		
		final List<ExternalApplication> apps = new ArrayList<ExternalApplication>(16);
		final String[] appNames = cmdConfig.getExternalApplications();
		final List<String> appNamesList = new ArrayList<String>(appNames.length);
		
		String clazz = null;
		for(final String appName : appNamesList)
		{
			try
	        {
				clazz = cmdConfig.getExternalApplicationClass(appName);
				if(clazz == null)
				{
					throw new IllegalArgumentException("The application class property for " + appName + " does not exist.");
				}
				
	            final Class<?> c = Class.forName(clazz);
	            final ExternalApplication ea = (ExternalApplication) ReflectionToolkit.createObject(c, new Class[] {ApplicationContext.class}, 
	            		new Object[] {appContext});
	            if(ea.isEnabled())
	            {
	            	apps.add(ea);
	            }
	        }
	        catch (final Exception e)
	        {
	        	TraceManager.getDefaultTracer().error("Could not create instance of " + clazz + ": " + e.getMessage(),e);
	            continue;
	        }
		}
		
		return(apps);
	}
}
