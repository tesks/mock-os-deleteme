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
package jpl.gds.shared.performance;


/**
 * This interface should be implemented by all classes that contain
 * performance data.
 * 
 */
public interface IPerformanceData {

	/**
	 * Gets the name of the component this performance data is for. 
	 * 
	 * @return the component name
	 */
	public String getComponentName();

	/**
	 * Gets the health status represented by this performance data.
	 * 
	 * @return HealthStatus
	 */
	public HealthStatus getHealthStatus();

	/**
	 * Sets the health status represented by this performance data.
	 * 
	 * @param toSet the status to set
	 */
	public void setHealthStatus(HealthStatus toSet);

	/**
	 * Generates a one line summary of the contents and/or health status of this
	 * performance data, suitable for writing to a log file.
	 * 
	 * @return log string
	 */
	public String toLogString();

	/**
	 * Creates and returns a copy of this performance data object. Avoids
	 * having to make everything Cloneable, which is a pain.
	 * 
	 * @return copy of current IPerformanceData object
	 */
	public IPerformanceData copy();
	
	/**
	 * Sets the boundary value at which the health status of this performance
	 * data becomes YELLOW. What this boundary value represents (queue length,
	 * percentage, etc) is determined by the implementing class.
	 * 
	 * @param bound
	 *            boundary value to set; a value of 0 turns off the YELLOW
	 *            health level.
	 */
	public void setYellowBound(long bound);
	
	/**
	 * Sets the boundary value at which the health status of this performance
	 * data becomes RED. What this boundary value represents (queue length,
	 * percentage, etc) is determined by the implementing class.
	 * 
	 * @param bound
	 *            boundary value to set; a value of 0 turns off the RED health
	 *            level.
	 */
	public void setRedBound(long bound);
	
	/**
     * Gets the boundary value at which the health status of this performance
     * data becomes YELLOW. What this boundary value represents (queue length,
     * percentage, etc) is determined by the implementing class.
     * 
     * @return boundary value; a value of 0 means the YELLOW
     *         health level is turned off.
     */
    public long getYellowBound();
    
    /**
     * Gets the boundary value at which the health status of this performance
     * data becomes RED. What this boundary value represents (queue length,
     * percentage, etc) is determined by the implementing class.
     * 
     * @return boundary value; a value of 0 means the YELLOW
     *         health level is turned off.
     */
    public long getRedBound();
}
