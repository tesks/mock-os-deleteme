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
package jpl.gds.context.api;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.time.TimeProperties;

/**
 * Holder class for the global "enable LST" flag in the application context.
 * 
 *
 * @since R8
 *
 */
public class EnableLstContextFlag {
	
	private final ApplicationContext appContext;
	private Boolean isEnabledOverride;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current ApplicationContext
	 */
	public EnableLstContextFlag(final ApplicationContext appContext)  {
		this.appContext = appContext;
	}
	
	/**
	 * Gets the flag indicating if LST is enabled in the current context. This is computed
	 * based upon the time configuration, the mission configuration, and the current venue
	 * setting.
	 * 
	 * @return true if LSTs should be generated, false if not
	 */
	public boolean isLstEnabled() {
		if (isEnabledOverride != null) {
			return isEnabledOverride;
		}
		return TimeProperties.getInstance().getLstEnabled() &&
			appContext.getBean(MissionProperties.class).getVenueUsesSol(
					appContext.getBean(IVenueConfiguration.class).getVenueType());
	}
	
	/**
	 * Sets the flag indicating if LST is enabled in the current context. Setting this flag
	 * permanently overrides the context configuration.
	 * 
	 * @param enable true to enable LST generation, false if not
	 */
	public void setLstEnabled(final boolean enable) {
		isEnabledOverride = enable;
	}
}
