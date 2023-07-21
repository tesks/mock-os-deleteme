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
package ammos.datagen.channel.util;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import ammos.datagen.generators.util.FieldGeneratorStatistics;

/**
 * This class tracks statistics specific to the channel generator. It extends
 * the field generator statistics object, which tracks items common to all
 * generators such as packet APID and enum field counts. This class adds
 * tracking of items like sample counts per channel ID, invalid sample counts,
 * invalid index counts, etc. It also includes methods for controlling
 * "invalid flags" unique to the channel generator.
 * 
 *
 */
public class ChannelGeneratorStatistics extends FieldGeneratorStatistics {

    private final AtomicBoolean invalidIndexGenerated = new AtomicBoolean(false);
    private long invalidIndexCount;
    private long totalChannelCount;
    private long totalInvalidChannelCount;
    private final SortedMap<String, Long> channelIdCounts = new TreeMap<String, Long>();
    private final SortedMap<Integer, Long> invalidIndexCounts = new TreeMap<Integer, Long>();
    private final SortedMap<String, Long> invalidChannelIdCounts = new TreeMap<String, Long>();
    private int minChannels = Integer.MAX_VALUE;
    private int maxChannels = Integer.MIN_VALUE;
    private long averageChannels = 0;

    /**
     * Private constructor to enforce singleton nature.
     */
    public ChannelGeneratorStatistics() {

        super();
        // do nothing
    }

    /**
     * Increments the total generation count for a specific channel ID.
     * 
     * @param id
     *            the channel ID for the count to increment
     */
    public void incrementTotalForChannelId(final String id) {

        synchronized (this.channelIdCounts) {
            final Long count = this.channelIdCounts.get(id);
            if (count == null) {
                this.channelIdCounts.put(id, 1L);
            } else {
                this.channelIdCounts.put(id, count.longValue() + 1);
            }
        }
        this.totalChannelCount++;
    }

    /**
     * Increments the total invalid generation count for a specific channel ID.
     * A channel sample is considered invalid if it has a valid channel index in
     * the output packet, but will for some reason not be processable by the
     * ground system.
     * 
     * @param id
     *            the channel ID for the count to increment
     */
    public void incrementTotalForInvalidChannelId(final String id) {

        synchronized (this.invalidChannelIdCounts) {
            final Long count = this.invalidChannelIdCounts.get(id);
            if (count == null) {
                this.invalidChannelIdCounts.put(id, 1L);
            } else {
                this.invalidChannelIdCounts.put(id, count.longValue() + 1);
            }
        }
        this.totalInvalidChannelCount++;
    }

    /**
     * Gets the total generation count for a specific channel ID.
     * 
     * @param id
     *            the channel ID for the count to fetch
     * @return the channel count
     */
    public long getTotalForChannelId(final String id) {

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
     * Gets the total invalid generation count for a specific channel ID. A
     * channel sample is considered invalid if it has a valid channel index in
     * the output packet, but will for some reason not be processable by the
     * ground system.
     * 
     * @param id
     *            the channel ID for the count to fetch
     * @return the channel count
     */
    public long getTotalForInvalidChannelId(final String id) {

        long result = 0;
        synchronized (this.invalidChannelIdCounts) {
            final Long count = this.invalidChannelIdCounts.get(id);
            if (count != null) {
                result = count;
            }
        }
        return result;
    }

    /**
     * Increments the total generation count for a specific invalid channel
     * index.
     * 
     * @param id
     *            the invalid index for the count to increment
     */
    public void incrementTotalForInvalidIndex(final int id) {

        synchronized (this.invalidIndexCounts) {
            final Long count = this.invalidIndexCounts.get(id);
            if (count == null) {
                this.invalidIndexCounts.put(id, 1L);
            } else {
                this.invalidIndexCounts.put(id, count.longValue() + 1);
            }
        }
        this.invalidIndexCount++;
        this.invalidIndexGenerated.set(true);
    }

    /**
     * Gets the total generation count for a specific invalid channel index.
     * 
     * @param id
     *            the invalid index for the count to fetch
     * @return the invalid index count
     */
    public long getTotalForInvalidIndex(final int id) {

        long result = 0;
        synchronized (this.invalidIndexCounts) {
            final Long count = this.invalidIndexCounts.get(id);
            if (count != null) {
                result = count;
            }
        }
        return result;
    }

    /**
     * Simultaneously sets the flag indicating whether at least one invalid
     * index value has been generated and gets the previous value of the flag.
     * 
     * @param invalidIndexGenerated
     *            value to set the flag to
     * @return true if one invalid index was previously generated, false if not
     */
    public boolean getAndSetInvalidIndexGenerated(
            final boolean invalidIndexGenerated) {

        return this.invalidFlagsDisabled
                || this.invalidIndexGenerated.getAndSet(invalidIndexGenerated);
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
     * Gets the total number of invalid channel samples generated. A channel
     * sample is considered invalid if it has a valid channel index in the
     * output packet, but will for some reason not be processable by the ground
     * system.
     * 
     * @return total invalid channel value count
     */
    public long getTotalInvalidChannelCount() {

        return this.totalInvalidChannelCount;
    }

    /**
     * Gets the total number of invalid indices generated (which corresponds to
     * the total number of packets that contain invalid indices).
     * 
     * @return total invalid index count
     */
    public long getTotalInvalidIndexCount() {

        return this.invalidIndexCount;
    }

    /**
     * Updates statistics for channel packets, based upon the given number of
     * channel samples in the latest packet.
     * 
     * @param numChannels
     *            number of channel samples in the latest packet
     */
    public void updateChannelPacketStatistics(final int numChannels) {

        this.maxChannels = Math.max(this.maxChannels, numChannels);
        this.minChannels = Math.min(this.minChannels, numChannels);
        this.averageChannels = this.totalChannelCount
                / (this.getTotalPacketCount() + 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {

        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Total Channel Samples Generated: " + this.totalChannelCount
                + "\n");
        sb.append("Invalid Channel Samples Generated: "
                + this.totalInvalidChannelCount + "\n");
        sb.append("Total Packets with Invalid Index Values: "
                + this.invalidIndexCount + "\n");
        sb.append("Minimum Samples per Packet: " + this.minChannels + "\n");
        sb.append("Maximum Samples per Packet: " + this.maxChannels + "\n");
        sb.append("Average Samples per Packet: " + this.averageChannels + "\n");
        sb.append("Counts of Valid Samples per Channel ID:\n");
        for (final String id : this.channelIdCounts.keySet()) {
            final long count = this.channelIdCounts.get(id).longValue();
            sb.append("  " + id + ": " + count + "\n");
        }
        if (!this.invalidChannelIdCounts.isEmpty()) {
            sb.append("Counts of Invalid Samples per Channel ID:\n");
            for (final String id : this.invalidChannelIdCounts.keySet()) {
                final long count = this.invalidChannelIdCounts.get(id)
                        .longValue();
                sb.append("  " + id + ": " + count + "\n");
            }
        }
        if (!this.invalidIndexCounts.isEmpty()) {
            sb.append("Usage Counts per Invalid Channel Index:\n");
            for (final Integer id : this.invalidIndexCounts.keySet()) {
                final long count = this.invalidIndexCounts.get(id).longValue();
                sb.append("  " + id + " (0x"
                        + Long.toHexString(id).toUpperCase() + "): " + count
                        + "\n");
            }
        }
        return sb.toString();
    }

}
