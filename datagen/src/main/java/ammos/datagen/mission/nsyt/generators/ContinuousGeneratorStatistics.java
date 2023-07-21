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
package ammos.datagen.mission.nsyt.generators;

import java.util.SortedMap;
import java.util.TreeMap;

import ammos.datagen.generators.util.FieldGeneratorStatistics;
import ammos.datagen.mission.nsyt.instrument.InstrumentTime;

/**
 * This class tracks statistics specific to the NSYT continuous packet
 * generator. It extends the field generator statistics object, which tracks
 * items common to all generators such as packet APID. This class adds tracking
 * of items like sample counts per channel ID and instrument time range.
 * 
 *
 * MPCS-6864 - 12/2/14. Added class.
 * 
 */
public class ContinuousGeneratorStatistics extends FieldGeneratorStatistics {

	private long totalChannelCount;
	private final SortedMap<Integer, Long> channelIdCounts = new TreeMap<Integer, Long>();
	private int minChannels = Integer.MAX_VALUE;
	private int maxChannels = Integer.MIN_VALUE;
	private long averageChannels = 0;
	private InstrumentTime startTime;
	private InstrumentTime endTime;

	/**
	 * Increments the total generation count for a specific instrument channel
	 * ID by the specified delta.
	 * 
	 * @param id
	 *            the channel ID for the count to increment
	 * @param delta
	 *            the amount by which to increment
	 * 
	 * MPCS-6864 - 12/2/14. Added method.
	 */
	public void incrementTotalForChannelId(final int id, final long delta) {

		synchronized (this.channelIdCounts) {
			final Long count = this.channelIdCounts.get(id);
			if (count == null) {
				this.channelIdCounts.put(id, delta);
			} else {
				this.channelIdCounts.put(id, count.longValue() + delta);
			}
		}
		this.totalChannelCount += delta;
	}

	/**
	 * Gets the total generation count for a specific instrument channel ID.
	 * 
	 * @param id
	 *            the channel ID for the count to fetch
	 * @return the channel count
	 */
	public long getTotalForChannelId(final int id) {

		long result = 0;
		synchronized (this.channelIdCounts) {
			final Long count = this.channelIdCounts.get(id);
			if (count != null) {
				result = count;
			}
		}
		return result;
	}

	/**
	 * Gets the total number of channel samples generated.
	 * 
	 * @return total channel value count
	 */
	public long getTotalChannelCount() {

		return this.totalChannelCount;
	}

	/**
	 * Updates statistics for channel packets, based upon the given number of
	 * channel samples in the latest packet.
	 * 
	 * @param numSamples
	 *            number of channel samples in the latest packet
	 */
	public void updateChannelPacketStatistics(final int numSamples) {

		this.maxChannels = Math.max(this.maxChannels, numSamples);
		this.minChannels = Math.min(this.minChannels, numSamples);
		this.averageChannels = this.totalChannelCount
				/ (this.getTotalPacketCount() + 1);
	}

	/**
	 * Updates the start and end instrument time range.
	 * 
	 * @param time
	 *            packet instrument time
	 */
	public void updateInstrumentTime(final InstrumentTime time) {
		if (this.startTime == null) {
			this.startTime = time;
		}
		this.endTime = time;
	}

	/**
	 * Gets the starting instrument time.
	 * 
	 * @return start time, or null if none set
	 */
	public InstrumentTime getStartInstrumentTime() {
		return this.startTime;
	}

	/**
	 * Gets the ending instrument time.
	 * 
	 * @return end time, or null if none set
	 */
	public InstrumentTime getEndInstrumentTime() {
		return this.endTime;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public synchronized String toString() {

		final StringBuilder sb = new StringBuilder(super.toString());

		if (this.startTime != null) {
			sb.append("Start Instrument Time: " + this.startTime + "\n");
			sb.append("End Instrument Time: " + this.endTime + "\n");
		}
		sb.append("Total Channel Samples Generated: " + this.totalChannelCount
				+ "\n");
		sb.append("Minimum Samples per Packet: " + this.minChannels + "\n");
		sb.append("Maximum Samples per Packet: " + this.maxChannels + "\n");
		sb.append("Average Samples per Packet: " + this.averageChannels + "\n");
		sb.append("Counts of Valid Samples per Instrument Channel ID:\n");
		for (final int id : this.channelIdCounts.keySet()) {
			final long count = this.channelIdCounts.get(id).longValue();
			sb.append("  " + id + ": " + count + "\n");
		}
		return sb.toString();
	}

}
