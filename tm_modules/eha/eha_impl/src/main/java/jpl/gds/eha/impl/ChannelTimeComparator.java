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
package jpl.gds.eha.impl;


import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.eha.api.channel.IServiceChannelValue;

/**
 * A timestamp comparison utility for channel values.
 * 
 * @since R8
 */
public class ChannelTimeComparator
 {
	private final TimeComparisonStrategyContextFlag timeStrategy;

    /**
     * Constructor.
     * 
     * @param inTimeStrategy
     *            current time comparison strategy
     */
	public ChannelTimeComparator(final TimeComparisonStrategyContextFlag inTimeStrategy) {
		this.timeStrategy = inTimeStrategy;
	}
	
	    /**
     * Compares two channel values by timestamp, according to the current
     * primary time comparison strategy, to determine whether the timestamp for
     * the new sample is later than (newer) than the timestamp on the old
     * sample. It is assumed that these samples are for the same channel, If
     * not, this method will throw. It is also assumed that "old" really was
     * received by AMPCS before "new". This fact is used when the time
     * comparison system is LAST_RECEIVED.
     * 
     * @param oldVal
     *            the old (previous) channel sample
     * @param newVal
     *            the new (current) channel sample
     * @return true if the new sample has a later time than the old sample;
     *         equality of timestamps will result in a true return value
     * 
     * 10/9/13. Added method to support monitor and downlink LAD updates.
     */
	public boolean timestampIsLater(final IServiceChannelValue oldVal, final IServiceChannelValue newVal) {
		if (oldVal == null || newVal == null) {
			throw new IllegalArgumentException("Cannot accept null channel values as arguments");
		}
		if (!oldVal.getChanId().equalsIgnoreCase(newVal.getChanId())) {
			throw new IllegalArgumentException("Input channel values must be for the same channel");
		}

		final TimeComparisonStrategy strategy = timeStrategy.getTimeComparisonStrategy();
		
		/*
		 * Monitor channels are always compared based upon ERT.
		 * 
		 * Change to return true if timestamps are equal.
		 * 
		 * Headers and monitor are checked the same, only by ERT.  Also moved things
		 * into some case statements and switch the check to see if the new value time was greater than equal and 
		 * returned those comparisons directly.
		 */
		switch (oldVal.getDefinitionType()) {
		case M:
			/**
			 * Monitor should be compared base on ERT or last received time only.  
			 */
			return strategy == TimeComparisonStrategy.LAST_RECEIVED || 
				   newVal.getErt().compareTo(oldVal.getErt()) >= 0;
		case H:
		case SSE:
		case FSW:
		default:
			/**
			 * Get the strategy from the enum class because it could be changed any time.  
			 */
			switch(strategy) {
			case ERT:
				return newVal.getErt().compareTo(oldVal.getErt()) >= 0;
			case SCET:
				return newVal.getScet().compareTo(oldVal.getScet()) >= 0;
			case SCLK:
				return newVal.getSclk().compareTo(oldVal.getSclk()) >= 0;
			case LAST_RECEIVED:
			default:
				/**
				 * Added global control of how channel samples and channel values
				 * If the TimeComparisonStrategy is "LAST_RECEIVED", simply return true, indicating that, by definition,
				 * the "newVal" is newer than the "oldVal". 
				 */
				return true;
			}
		}
	}

}
