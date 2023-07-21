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

import jpl.gds.common.config.TimeComparisonStrategy;

/**
 * Holder for the current time comparison strategy, which is mutable. This used
 * to be handled by static members in @see TimeComparisonStrategy, but it has
 * been moved into a non-static object so it can be used as a bean in the Spring
 * context.
 * 
 *
 * @since R8
 *
 */
public class TimeComparisonStrategyContextFlag {

	private TimeComparisonStrategy strategy = TimeComparisonStrategy.LAST_RECEIVED;

	/**
	 * Constructor. Sets the current strategy that defaults to LAST_RECEIVED.
	 * 
	 */
	public TimeComparisonStrategyContextFlag() {

		// do nothing
	}
	
	/**
	 * Constructor. Sets the current strategy to the supplied default.
	 * 
	 * @param defStrategy
	 *            default TimeComparisonStrategy value
	 */
	public TimeComparisonStrategyContextFlag(TimeComparisonStrategy defStrategy) {

		if (defStrategy == null) {
			throw new IllegalArgumentException("strategy cannot be null");
		}
		strategy = defStrategy;
	}

	/**
	 * Mutator for the current TimeComparisonStrategy value
	 * 
	 * @param inStrategy
	 *            TimeComparisonStrategy value to assign
	 */
	public void setTimeComparisonStrategy(
			final TimeComparisonStrategy inStrategy) {
		if (inStrategy == null) {
			throw new IllegalArgumentException("strategy cannot be null");
		}
		strategy = inStrategy;
	}

	/**
	 * Accessor for the current TimeComparisonStrategy value
	 * 
	 * @return current TimeComparisonStrategy value
	 */
	public TimeComparisonStrategy getTimeComparisonStrategy() {
		return strategy;
	}
}
