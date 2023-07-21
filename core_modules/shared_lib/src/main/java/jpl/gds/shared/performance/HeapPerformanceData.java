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
 * A performance data class that represents heap status and health by supplying
 * max and current heap sizes. Uses config values to determine the heap levels
 * that represent RED and YELLOW health status by itself. Tracks the high water
 * mark, so long as the same instance is used and updated.
 * 
 */
public class HeapPerformanceData extends AbstractPerformanceData {

	/** Size of a megabyte. */
	private static final long MB = 1024 * 1024;

    /** The maximum heap size. This would correspond to a -Xmx JVM option */
	private long maxHeap;

	/** The current heap size */
	private long currentHeap;

	/** The largest heap size recorded by this object. */
	private long highWater;

    /**
     * The currently allocated heap.
     * This is NOT the used heap, but rather the limit at which Java will reserve more memory.
     */
    private long              totalHeap;

	/**
	 * Constructor. Automatically sets the component name to "Heap" and computes
	 * the current heap status.
	 * 
	 * @param props PerformanceProperties object containing configuration
	 */
	public HeapPerformanceData(final PerformanceProperties props) {
		super(props, "Heap", props.getHeapYellowLevel(), props.getHeapRedLevel());
		updateHeapData();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceData#copy()
	 */
	@Override
	public IPerformanceData copy() {
		final HeapPerformanceData newCopy = new HeapPerformanceData(properties);
		super.copyMembersTo(newCopy);
		newCopy.highWater = this.highWater;
		newCopy.maxHeap = this.maxHeap;
		newCopy.currentHeap = this.currentHeap;
		return newCopy;
	}

	/**
	 * Updates the heap statistics in the current object from the Runtime class
	 * and recomputes health status.
	 */
	public void updateHeapData() {
		final Runtime r = Runtime.getRuntime();

        /**
         *
         * Modified to use r.totalMemory(). This is the total heap owned by Java
         * currently, as opposed to r.maxMemory() which is the limit after which Java
         * will allocate no more memory.
         */
        this.totalHeap = r.totalMemory();
		this.maxHeap = r.maxMemory();
        this.currentHeap = this.totalHeap - r.freeMemory();

		this.highWater = Math.max(this.currentHeap, this.highWater);

		final double percentage = getPercentUsed();

		/* Set the health level based upon % of heap used. A bound value of
		 * 0 means to disable that health level.
		 */
		/*  Fixed broken logic for red level when yellow level is 0.
		 * Reworking logic again because it just does not seem to work.
		 */
		setHealthStatus(HealthStatus.GREEN);

		if (yellowLevel != 0 && percentage >= yellowLevel) {
			setHealthStatus(HealthStatus.YELLOW);
		}

		if (redLevel != 0 && percentage >= redLevel) {
			setHealthStatus(HealthStatus.RED);
		}

	}

	/**
	 * Gets the maximum heap size in megabytes.
	 * 
	 * @return max heap size in bytes
	 */
	public long getMaxHeap() {
		return maxHeap / MB;
	}

	/**
	 * Gets the current heap size in megabytes.
	 * 
	 * @return heap size in bytes
	 */
	public long getCurrentHeap() {
		return currentHeap / MB;
	}

	/**
	 * Gets the heap high water mark in megabytes.
	 * 
	 * @return heap size in bytes
	 */
	public long getHighWaterMark() {
		return highWater / MB;
	}

	/**
	 * Gets the heap percentage used.
	 * 
	 * @return used heap, 0 to 100%
	 */
	public int getPercentUsed() {
		/* Do not return value > 100 */
		return (int) Math.min(100, Math
				.round(((double) this.currentHeap / (double) this.maxHeap) * 100));
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
		b.append(", Used=");
		b.append(getCurrentHeap());
		b.append("Mb, Max=");
		b.append(getMaxHeap());
		b.append("Mb, High Water=");
		b.append(getHighWaterMark());
		b.append("Mb, Percentage=");
		b.append(getPercentUsed());
		b.append('%');
		return b.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#setYellowBound(long)
	 */
	@Override
	public void setYellowBound(final long bound) {
		if (bound > 100) {
			throw new IllegalArgumentException("yellow bound may not be more than 100");
		}
		super.setYellowBound(bound);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.performance.IPerformanceData#setRedBound(long)
	 */
	@Override
	public void setRedBound(final long bound) {
		if (bound > 100) {
			throw new IllegalArgumentException("red bound may not be more than 100");
		}
		super.setRedBound(bound);
	}
	
	/**
     * Gets the units (type of objects stored in this queue).
     * @return unit string
     * 
     */
    public String getUnits() {
        return "Mb";
    }

}
