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
 * This is the base class that can be extended by classes that implement
 * IPerformanceData.
 * 
 */
public abstract class AbstractPerformanceData implements IPerformanceData {

	/** The name of the component supplying this performance data. */
	private final String component;
	
	/** The current health status represented by this performance data. */
	private HealthStatus currentHealth = HealthStatus.NONE;
	
	/** Boundary value at which health becomes YELLOW. */
	protected long yellowLevel;
	
	/** Boundary value at which health becomes RED. */
	protected long redLevel;
	
	/** Performance configuration properties */
	protected PerformanceProperties properties;

	/**
	 * Constructor.
	 * 
	 * @param props PerformanceProperties to get configuration from
	 * @param sourceComponent
	 *            the name of the component this data is for
	 * @param yellow yellow performance boundary
	 * @param red red performance boundary
	 */
	public AbstractPerformanceData(PerformanceProperties props, String sourceComponent, long yellow, long red) {
		assert sourceComponent != null : "Source cannot be null";
		
		properties = props;
		
		if (yellow > red) {
			throw new IllegalArgumentException("yellow health level must be less than or equal to the red level");
		}

		this.component = sourceComponent;
		this.yellowLevel = yellow;
		this.redLevel = red;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceData#getHealthStatus()
	 */
	@Override
	public HealthStatus getHealthStatus() {
		return currentHealth;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceData#setHealthStatus(jpl.gds.shared.performance.HealthStatus)
	 */
	@Override
	public void setHealthStatus(HealthStatus currentHealth) {
		this.currentHealth = currentHealth;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceData#getComponentName()
	 */
	@Override
	public String getComponentName() {
		return component;
	}

	/**
	 * Copies all non-final members of this instance to the given instance of
	 * IPerformanceData.
	 * 
	 * @param toCopy
	 *            the object to copy data to
	 */
	protected void copyMembersTo(IPerformanceData toCopy) {
		toCopy.setHealthStatus(this.currentHealth);
		toCopy.setRedBound(this.redLevel);
		toCopy.setYellowBound(this.yellowLevel);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#setYellowBound(long)
	 */
	@Override
	public void setYellowBound(long bound) {
		if (bound < 0) {
			throw new IllegalArgumentException("yellow bound may not be less than 0");
		}
		this.yellowLevel = bound;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#setRedBound(long)
	 */
	@Override
	public void setRedBound(long bound) {
		if (bound < 0) {
			throw new IllegalArgumentException("red bound may not be less than 0");
		}
		this.redLevel = bound;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#getRedBound()
	 */
	@Override
    public long getRedBound() {
	    return this.redLevel;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#getYellowBound()
	 */
	@Override
    public long getYellowBound() {
	    return this.yellowLevel;
	}
}
