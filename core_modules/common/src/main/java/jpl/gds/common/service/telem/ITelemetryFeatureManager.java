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
package jpl.gds.common.service.telem;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.interfaces.IService;

/**
 * The IFeatureManager interface is implemented by all classes which
 * manage the life cycle of a single downlink feature. Features can be enabled
 * or disabled, initialized, or shutdown. Work is accomplished for features by
 * Downlink Service classes. Every feature has one or more attached services.
 * 
 */
public interface ITelemetryFeatureManager {

	/**
     * Enables or disables the feature.
	 * @param isEnabled true to enable the feature; false to disable
	 */
	public void enable(boolean isEnabled);
       
	/**
     * Initializes the feature for use.
     * 
	 * @param springContext the current ApplicationContext
	 * 
	 * @return true if the feature is successfully initialized
	 */
	public boolean init(ApplicationContext springContext);
	
	/**
	 * Cleans up the feature after use.
	 */
	public void shutdown();
	
   /**
    * Indicates whether the feature has been properly initialized
    * @return true if the feature is ready to execute; false if not
    */
	public boolean isValid();
	
	/**
	 * Sets the flag indicating whether the feature has been properly initialized
	 * @param isValid true if the feature is ready to execute; false if not
	 */
	public void setValid(boolean isValid);
	    
	
	/**
     * Indicates whether the feature is enabled.
	 * @return true if the feature is enabled; false if not.
	 */
	public boolean isEnabled();
	
	/**
	 * Adds a downlink service to this feature.
	 * 
	 * @param service the DownlinkService feature to add.
	 */
	public void addService(IService service);

	/**
	 * Starts all the downlink services attached to this feature.
	 * 
	 * @return true if services started successfully, false if not
	 */
	public boolean startAllServices();

    /**
     * Stops all the downlink services attached to this feature.
     */
	public void stopAllServices();

	/**
     * Removes all the downlink services attached to this feature.
     */
	public void clearAllServices();

	/**
	 * Gets a specific downlink service attached to this feature by class name.
	 * 
	 * @param c the class of the DownlinkService to find
	 * @return the matching DownlinkService instance, or null if no match found
	 */
	public IService getService(Class<?> c);
	
	/**
	 * Populate the session summary with data from this feature manager.
	 * 
	 * @param sum SessionSummary to update
	 */
    public void populateSummary(ITelemetrySummary sum);
}
