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
package jpl.gds.globallad;

/**
 * This ENUM is used by the global lad reaper to indicate what type of reaping should be 
 * performed.  
 *
 * Fixed touch time and depth reduction settings. Was reducing TO the value and not BY it.
 */
public enum GlobalLadReapSettings {

	/**
     * This is used as the first stage of a memory deal.
     */
	NORMAL(true, false, 0, 0),

    /**
     * Normal reaping, nothing special.
     */
    MEM_NORMAL(true, false, 0, 0),

	/**
	 * Do reaping but reduce the touch time by 25%.
	 */
	REDUCED_TOUCH_TIME_25(true, false, 25, 0),

	/**
	 * Do reaping but reduce the touch time by 50%.
	 */
	REDUCED_TOUCH_TIME_50(true, false, 50, 0),

	/**
	 * Do reaping but reduce the touch time by 75%.
	 */
	REDUCED_TOUCH_TIME_75(true, false, 75, 0),

	/**
	 * Do reaping but reduce the touch time by 90%.  Don't want to go 
	 * too high on this because it could reap sessions that are active.  
	 */
	REDUCED_TOUCH_TIME_90(true, false, 90, 0),

	/**
	 * Do reaping but ignore the level restriction.  This means that all data will
	 * be reaped based on touch time.  This is like NORMAL but includes all targets.
	 */
	IGNORE_LEVEL_RESTRICTIONS(true, true, 0, 0),


	/**
	 * Do reaping but ignore the level restriction and reduce touch time by 10%.
	 * This means that all data will be reaped based on the reduced touch time.
	 */
	REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_25(true, true, 25, 0),

	/**
	 * Do reaping but ignore the level restriction and reduce touch time by 10%.
	 * This means that all data will be reaped based on the reduced touch time.
	 */
	REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_50(true, true, 50, 0),

	/**
	 * Do reaping but ignore the level restriction and reduce touch time by 10%.
	 * This means that all data will be reaped based on the reduced touch time.
	 */
	REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_75(true, true, 75, 0),

	/**
	 * Do reaping but ignore the level restriction and reduce touch time by 10%.
	 * This means that all data will be reaped based on the reduced touch time.
	 */
	REDUCED_TOUCH_TIME_IGNORE_LEVEL_RESTRICTIONS_90(true, true, 90, 0),

	/**
	 * Last resort, start reducing the data depths by some percent.
	 */
	REDUCE_DATA_DEPTHS_10(false, false, 0, 10),
	REDUCE_DATA_DEPTHS_25(false, false, 0, 25),
	REDUCE_DATA_DEPTHS_50(false, false, 0, 50),
	REDUCE_DATA_DEPTHS_75(false, false, 0, 75),
	REDUCE_DATA_DEPTHS_90(false, false, 0, 90),
	REDUCE_DATA_DEPTHS_99(false, false, 0, 99),
	
	/**
     * The value below will change the default depth value in the configuration.
     */
    REDUCE_DATA_DEPTHS_PERM(false, false, 0, 2);

	// If reaping should be done.
	public final boolean doReaping;

	// If any of the restrictions from the configuration should be ignored when reaping.
	public final boolean ignoreGladVenueReapingRestriction;

	// Percent to reduce the touch time when calculating if a target should be reaped.
	public final double touchTimeRatio;

	// Depth reduction.
	public final double depthRatio;

	/**
	 * @param doReaping If reaping should be done
	 * @param ignoreGladVenueReapingRestriction if venue restrictions should be ignored
	 * @param touchTimeReductionPercentage percent from 0 - 100
	 * @param depthReductionPercentage percent from 0 - 100
	 */
	private GlobalLadReapSettings(final boolean doReaping, final boolean ignoreGladVenueReapingRestriction,
			final double touchTimeReductionPercentage, final double depthReductionPercentage) {
		this.doReaping = doReaping;
		this.ignoreGladVenueReapingRestriction = ignoreGladVenueReapingRestriction;
		this.touchTimeRatio = (100.0 - touchTimeReductionPercentage) / 100;
		this.depthRatio = ( 100.0 - depthReductionPercentage) / 100; 
	}

	@Override
	public String toString() {
		return new StringBuilder(this.name())
				.append(" Last access time is reduced to ")
				.append((touchTimeRatio * 100))
				.append("% and data depth is reduced to ")
				.append((depthRatio * 100))
				.append("%")
				.toString();
	}


}
