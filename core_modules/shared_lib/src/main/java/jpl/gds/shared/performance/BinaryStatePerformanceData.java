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

import jpl.gds.shared.config.PerformanceProperties;


/**
 * A performance data class that represents a good or bad status of a component.
 * Uses constructor arguments to determine if bad is a yellow or red status
 * 
 */
public class BinaryStatePerformanceData extends AbstractPerformanceData {
    
    private boolean isGood = true;
    private boolean badRed;
	
	/**
	 * Constructor
	 * 
	 * @param props PerformanceProperties object containing configuration
	 * @param source
	 *            Name of the component utilizing this performance data
	 * @param isBadRed
	 *            TRUE if a bad state is to result in a red health status, FALSE
	 *            if a bad state is to result in a yellow health state
	 */
	public BinaryStatePerformanceData(PerformanceProperties props, String source, boolean isBadRed ) {
		super(props, source, 0, 0);
		
		this.badRed = isBadRed;
	}

	/**
	 * Update the good/bad status of this performance data.
	 * 
	 * @param isGood
	 *            TRUE if the reporting component is good/running properly,
	 *            FALSE if there is an issue
	 */
	public void setGood(boolean isGood){
		this.isGood = isGood;
		if(this.isGood){
			this.setHealthStatus(HealthStatus.GREEN);
		}
		else if(this.badRed){
			this.setHealthStatus(HealthStatus.RED);
		}
		else {
			this.setHealthStatus(HealthStatus.YELLOW);
		}
	}
	
	/**
	 * Get the performance status
	 * 
	 * @return TRUE if the component using this performance data is good, FALSE
	 *         if there is an issue
	 */
	public boolean isGood(){
		return this.isGood;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#toLogString()
	 */
	@Override
	public String toLogString() {
		StringBuilder b = new StringBuilder(this.getComponentName());
		
		b.append(": Health=");
		b.append(getHealthStatus());
		
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#copy()
	 */
	@Override
	public IPerformanceData copy() {
		BinaryStatePerformanceData newCopy = new BinaryStatePerformanceData(properties, this.getComponentName(), this.badRed);
		newCopy.setGood(this.isGood);
		
		return newCopy;
	}
	
}