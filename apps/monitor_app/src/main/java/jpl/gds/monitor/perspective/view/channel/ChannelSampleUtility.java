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
package jpl.gds.monitor.perspective.view.channel;


import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;

/**
 * ChannelId is used to represent the identification of a single telemetry
 * channel. Channel IDs take the form of one letter, followed by zero or more
 * alphanumeric characters (this is the channel source), followed by a dash,
 * followed by 1 or more numbers (this is the channel number). In general,
 * ChannelIDs are now just represented as Strings in the remaining code. This
 * class exists for compatibility with old interfaces only, and to provide
 * several static methods that are useful for manipulating Channel ID strings.
 * 
 * Class is final so that copy constructor is sufficient to clone.
 */
public final class ChannelSampleUtility
{
	/**
	 * Compares two channel samples by timestamp, according to the current primary
	 * time system in the global LAD configuration, to determine whether the
	 * timestamp for sampleTwo is later than (newer) than the timestamp
	 * on sampleOne. It is assumed that these samples are for the same channel.
	 * If not, this method will throw. No assumption is made about the actual receipt
	 * order of the two samples.
	 * 
	 * @param first the first channel sample
	 * @param second the second channel sample
	 * @return true if the second sample has a later time than the first sample; equality
	 * of timestamps will result in a true return value
	 * 
	 */
	public static boolean timestampIsLater(TimeComparisonStrategyContextFlag timeStrategy, final MonitorChannelSample sampleOne, final MonitorChannelSample sampleTwo) {
		if (sampleOne == null || sampleTwo == null) {
			throw new IllegalArgumentException("Cannot accept null channel values as arguments");
		}
		if (!sampleOne.getChanId().equalsIgnoreCase(sampleTwo.getChanId())) {
			throw new IllegalArgumentException("Input channel values must be for the same channel");
		}

		final TimeComparisonStrategy strategy = timeStrategy.getTimeComparisonStrategy();
		
		/*
		 * Monitor channels are always compared based upon ERT.
		 *
		 * Headers and monitor are checked the same, only by ERT.  Also moved things
		 * into some case statements and switch the check to see if the new value time was greater than equal and 
		 * returned those comparisons directly.
		 */
		switch (sampleOne.getChanDef().getDefinitionType()) {
		case M:
			/**
			 * Monitor should be compared base on ERT or last received time only.  
			 */
			return strategy == TimeComparisonStrategy.LAST_RECEIVED || 
				   sampleTwo.getErt().compareTo(sampleOne.getErt()) >= 0;
		case H:
		case SSE:
		case FSW:
		default:
			/**
			 * Get the strategy from the enum class because it could be changed any time.  
			 */
			switch(strategy) {
			case ERT:
				return sampleTwo.getErt().compareTo(sampleOne.getErt()) >= 0;
			case SCET:
				return sampleTwo.getScet().compareTo(sampleOne.getScet()) >= 0;
			case SCLK:
				return sampleTwo.getSclk().compareTo(sampleOne.getSclk()) >= 0;
			case LAST_RECEIVED:
			default:
				/**
				 * Use timestamp comparison for
				 * last received time strategy. Used to always return true but 
				 * that does not work when source=BOTH
				 */
				return sampleTwo.getTimestamp() >= sampleOne.getTimestamp();
			}
		}
	}



	/**
	 * Compares two channel values by channel ID, DN, and ERT.  Monitor 
	 * channels will additionally compare station ID; all other channel types 
	 * will also compare SCET.
	 * 
	 * @param chanType channel definition enumeration type
	 * @param oldVal the old (previous) channel sample
	 * @param newVal the new (current) channel sample
	 * @return true if channel values passed in are identical, false otherwise
	 */
	public static boolean sameChannelValue(final ChannelDefinitionType chanType, 
			final MonitorChannelSample oldVal, final MonitorChannelSample newVal) {

		// Compare channel ID, DN and ERT for all channel types
		if(oldVal.getChanId().equals(newVal.getChanId()) && 
				oldVal.getDnValue().getStringValue().equals(
						newVal.getDnValue().getStringValue()) && 
						oldVal.getErt().compareTo(newVal.getErt()) == 0) {

			// Compare station ID for Monitor channels
			if(chanType == ChannelDefinitionType.M) {
				if(oldVal.getDssId() == newVal.getDssId()) {
					return true;
				}
			} 
			// Compare SCET for Flight, SSE and Header channels
			else {
				if(oldVal.getScet().compareTo(newVal.getScet()) == 0) {
					return true;
				}
			}
		}

		return false;
	}
}
