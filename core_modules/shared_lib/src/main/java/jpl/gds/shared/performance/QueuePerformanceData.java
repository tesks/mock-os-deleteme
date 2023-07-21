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
 * A performance data class that represents queue status and health by supplying
 * max and current queue lengths. Uses config values to determine the queue
 * lengths that represent RED and YELLOW health status, though these can be
 * overridden. Tracks the high water mark, so long as the same instance is used
 * and updated. Allows the queue to be identified as a throttling queue (one
 * that operates internal to the application, is bounded, and stalls processing
 * if blocked), and/or backlogging (one that will result in tasks that must be
 * worked off before the application can exit).
 * 
 */
public class QueuePerformanceData extends AbstractPerformanceData implements
		IPerformanceData {
  
	/** Constant string representing the component type */
	public static final String COMPONENT_TYPE = "Queue ";

	/** Indicates a queue value that has not been set */
	private static final long NO_MAX = -1;

	/** Indicates if the queue throttles the application */
	private boolean isThrottle;

	/** Indicates if the queue generates backlog */
	private final boolean isBacklog;

	/** Maximum queue size. NO_MAX means the queue is unbounded. */
	private long maxQueueSize = NO_MAX;

	/** Current queue length */
	private long currentQueueSize = NO_MAX;

	/** Queue high water length */
	private long highWater = NO_MAX;
	
	/** Unit - type of entries in the queue. */
	private String unit = "entries";

	/**
	 * Constructor for a non-bounded queue. A non-bounded queue cannot throttle
	 * and the provider must tell us the yellow and red lengths.
	 * 
	 * @param props PerformanceProperties object containing configuration
	 * @param source
	 *            name of the queue. The constant COMPONENT_TYPE will be
	 *            prepended.
	 * @param isBacklog
	 *            indicates if the queue creates application backlog
	 * @param yellow
	 *            the queue length at which health status becomes yellow
	 * @param red
	 *            the queue length at which health status becomes red
	 * @param unit the type of items in the queue
	 * 
	 */
	public QueuePerformanceData(final PerformanceProperties props, final String source, final boolean isBacklog, final long yellow,
			final long red, final String unit) {
		super(props, COMPONENT_TYPE + source, yellow, red);
		this.isBacklog = isBacklog;
		this.isThrottle = false;
		if (unit != null) {
		    this.unit = unit;
		}
	}

	/**
	 * Constructor for a bounded queue. A bounded queue can throttle or generate
	 * backlog. Red and yellow boundary values will be set to the configured
	 * defaults.
	 * 
	 * @param props PerformanceProperties object containing configuration
	 * @param source
	 *            name of the queue. The constant COMPONENT_TYPE will be
	 *            prepended.
	 * @param maxQueue maximum queue length           
	 * @param isBacklog
	 *            indicates if the queue creates application backlog
	 * @param isThrottle
	 *            indicates if the queue throttles the application
	 *            
	 * @param unit the type of items in the queue
     * 
	 */
	public QueuePerformanceData(final PerformanceProperties props, final String source, final long maxQueue,
			final boolean isBacklog, final boolean isThrottle, final String unit) {
		this(props, source, isBacklog, props.getBoundedQueueYellowLevel(), props.getBoundedQueueRedLevel(), unit);
		this.isThrottle = isThrottle;
		this.maxQueueSize = maxQueue;
	}

	/**
	 * Gets the current queue size. A value of NO_MAX means no current length is
	 * set.
	 * 
	 * @return current queue size
	 */
	public long getCurrentQueueSize() {
		return currentQueueSize;
	}

	/**
	 * Indicates whether this queue is bounded.
	 * 
	 * @return true if bounded, false if not
	 */
	public boolean isBounded() {
		return this.maxQueueSize != NO_MAX;
	}

	/**
	 * Gets the percentage of the maximum queue length that is currently in use,
	 * for bounded queues only.
	 * 
	 * @return percent of queue used (0 to 100)
	 */
    public int getPercentUsed() {
        /*
         *  Changed so that it does not throw, but returns -1 for percent used.
         * The problem is that this object is being passed as a POJO to a RESTful controller interface.
         * It is being called through reflection, and cannot handle exceptions gracefully.
         * 
         * After searching for direct calls to this method, I found only one, in this class in the method
         * setCurrentQueueSize(), and it is protected by a check for "(this.maxQueueSize != NO_MAX)"
         */
        return (this.maxQueueSize == NO_MAX) ? (int) NO_MAX
                : (int) Math.round(((double) this.currentQueueSize / (double) this.maxQueueSize) * 100);
	}

	/**
	 * Sets the current queue size and recomputes health status.
	 * 
	 * @param currentQueueSize current queue size
	 */
	public void setCurrentQueueSize(final long currentQueueSize) {
		assert currentQueueSize >= 0 : "queue size cannot be negative";

		this.currentQueueSize = currentQueueSize;
        
		/*
		 * If the queue is bounded, then the percentage of max queue size is
		 * computed and checked against RED and YELLOW bounds to set health
		 * status. If a red/yellow bound value is 0, it means to disable that 
		 * health state.
		 */
		if (this.maxQueueSize != NO_MAX) {
			final int percentage = getPercentUsed();

			/* Fixed broken logic for red level when yellow level is 0.
			 * Reworking logic again because it just does not seem to work.
			 */
			setHealthStatus(HealthStatus.GREEN);

			if (yellowLevel != 0 && percentage >= yellowLevel) {
				setHealthStatus(HealthStatus.YELLOW);
			}

			if (redLevel != 0 && percentage >= redLevel) {
				setHealthStatus(HealthStatus.RED);
			}

		} else {
			/*
			 * The queue is not bounded, then the provider must set the red and
			 * yellow bounds. The queue size is compared to these bounds and the
			 * health status is set. If a red/yellow bound value is 0, it means
			 * to disable that health state.
			 */

			/* Fixed broken logic for red level when yellow level is 0.
			 * Reworking logic again because it just does not seem to work.
			 */
			setHealthStatus(HealthStatus.GREEN);

			if (yellowLevel != 0 && this.currentQueueSize >= yellowLevel) {
				setHealthStatus(HealthStatus.YELLOW);
			}

			if (redLevel != 0 && this.currentQueueSize >= redLevel) {
				setHealthStatus(HealthStatus.RED);
			}

		}
	}

	/**
	 * Gets the queue high water mark, which is the highest length the queue has
	 * reached. A value of NO_MAX means no value is set.
	 * 
	 * @return queue high water length
	 */
	public long getHighWaterMark() {
		return highWater;
	}

	/**
	 * Sets the queue high water mark.
	 * 
	 * @param highWater
	 *            the highest length the queue has reached
	 */
	public void setHighWaterMark(final long highWater) {
		assert highWater >= 0 : "high water level cannot be less than 0";

		this.highWater = highWater;
	}

	/**
	 * Indicates if this is a throttling queue. This does not mean it is
	 * currently throttling, just that it can.
	 * 
	 * @return true if throttling queue, false if not
	 */
	public boolean isThrottle() {
		return isThrottle;
	}

	/**
	 * Indicates if this is a backlog queue. This does not mean it is currently
	 * backlogging, just that it can.
	 * 
	 * @return true if backlogging queue, false if not
	 */
	public boolean isBacklog() {
		return isBacklog;
	}

	/**
	 * Gets the maximum queue length. Will be NO_MAX for an unbounded queue.
	 * 
	 * @return max queue length
	 */
	public long getMaxQueueSize() {
		return maxQueueSize;
	}
	
	/**
	 * Sets a new maximum queue size. Must be a non-negative value.
	 * Health status is recalculated afterwards.
	 * 
	 * @param maxQueueSize The new maximum size of the queue
	 * 
	 */
	public void setMaxQueueSize(final long maxQueueSize){
		assert maxQueueSize > 0: "max queue size cannot be less than 0";
		
		this.maxQueueSize = maxQueueSize;
		
		//recalculate health status
		setCurrentQueueSize(this.currentQueueSize);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceData#toLogString()
	 */
	@Override
	public String toLogString() {
		final StringBuilder b = new StringBuilder(getComponentName());
		b.append(": Health=");
		b.append(getHealthStatus());
		b.append(", Current=");
		b.append(this.currentQueueSize);
		b.append(", Max=");
		b.append(this.maxQueueSize == NO_MAX ? "N/A" : this.maxQueueSize);
		b.append(", High Water=");
		b.append(this.highWater == NO_MAX ? "N/A" : this.highWater);
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceData#copy()
	 */
	@Override
	public IPerformanceData copy() {
		final String baseComponentName = getComponentName().substring(
				COMPONENT_TYPE.length());
		final QueuePerformanceData newCopy = new QueuePerformanceData(
				properties, baseComponentName, this.maxQueueSize, this.isBacklog,
				this.isThrottle, this.unit);
		super.copyMembersTo(newCopy);
		newCopy.currentQueueSize = this.currentQueueSize;
		newCopy.highWater = this.highWater;
		return newCopy;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#setYellowBound(long)
	 */
	@Override
	public void setYellowBound(final long bound) {
		if (this.maxQueueSize != NO_MAX && bound > 100) {
			throw new IllegalArgumentException("yellow bound may not be more than 100 for bounded queues");
		}
		super.setYellowBound(bound);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#setRedBound(long)
	 */
	@Override
	public void setRedBound(final long bound) {
		if (this.maxQueueSize != NO_MAX && bound > 100) {
			throw new IllegalArgumentException("red bound may not be more than 100 for bounded queues");
		}
		super.setRedBound(bound);
	}
	
	/**
	 * Gets the units (type of objects stored in this queue).
	 * @return unit string
	 * 
	 */
	public String getUnits() {
	    return this.unit;
	}
}
