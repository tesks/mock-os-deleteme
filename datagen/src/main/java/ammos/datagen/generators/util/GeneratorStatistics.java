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
package ammos.datagen.generators.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.ISclk;

/**
 * This class tracks general statistics common to multiple types of data
 * generators, such as packet APID counts, generator run time, packet SCLK
 * range, average packet size, etc. It includes flags that can be set to
 * indicate whether at least one invalid data value has been generated for each
 * type of invalid data, since it is a requirement of some generators to
 * generate a minimal set of invalid values. These flags do not function until
 * enableGetAndSetInvalidFlags(true) is called. Until then, methods to get these
 * flags will always return true.
 * <p>
 * This class also contains a static reference to the global statistics object
 * established for each data generator run.
 * 
 *
 */
public class GeneratorStatistics {

    private static GeneratorStatistics globalStats = new GeneratorStatistics();

    private final Map<Integer, Long> apidCounts = new HashMap<Integer, Long>();
    private int minPacketSize = Integer.MAX_VALUE;
    private int maxPacketSize = Integer.MIN_VALUE;
    private int averagePacketSize;
    private long totalPacketSize;
    private long totalPacketCount;
    private long invalidPacketCount;
    private long fillPacketCount;
    private long totalFillSize;
    private long startTime;
    private long endTime;
    private ISclk startSclk;
    private ISclk endSclk;

    /**
     * Flag that controls whether the booleans for tracking whether certain
     * invalid values have been generated will function.
     */
    protected boolean invalidFlagsDisabled = true;

    /**
     * Constructor.
     */
    public GeneratorStatistics() {

        super();
    }

    /**
     * Enables or disables operations of the boolean semaphores that enforce
     * minimal generation of selected invalid data. Until the invalid flags are
     * enabled, the getAndSet() methods will always return true, making the
     * system think it does not yet need to generate minimal invalid data.
     * 
     * @param enable
     *            true to enable operation of the semaphores, false to not
     */
    public void enableGetAndSetInvalidFlags(final boolean enable) {

        this.invalidFlagsDisabled = !enable;
    }

    /**
     * Updates statistics given information about the latest packet that was
     * generated.
     * 
     * @param packetSize
     *            total size of the packet, including header
     * @param apid
     *            the packet APID
     * @param sclk
     *            the packet SCLK
     * @param time
     *            the current time, milliseconds since 1970
     */
    public synchronized void updatePacketStatistics(final int packetSize,
            final int apid, final ISclk sclk, final long time) {

        this.minPacketSize = Math.min(this.minPacketSize, packetSize);
        this.maxPacketSize = Math.max(this.maxPacketSize, packetSize);
        this.totalPacketCount++;
        this.totalPacketSize += packetSize;
        this.averagePacketSize = (int) (this.totalPacketSize / this.totalPacketCount);
        if (this.startSclk == null) {
            this.startSclk = sclk;
        }
        this.endSclk = sclk;
        if (this.startTime == 0) {
            this.startTime = time;
        }
        this.endTime = time;
        final Long count = this.apidCounts.get(apid);
        if (count == null) {
            this.apidCounts.put(apid, 1L);
        } else {
            this.apidCounts.put(apid, count.longValue() + 1);
        }
    }

    /**
     * Updates statistics given information about the latest packet that was
     * generated, where the packet is a fill packet. This will call
     * updatePacketStatistics() as well, so there is no need to call both
     * methods.
     * 
     * @param packetSize
     *            total size of the packet, including header
     * @param apid
     *            the packet APID
     * @param sclk
     *            the packet SCLK
     * @param time
     *            the current time, milliseconds since 1970
     */
    public synchronized void updateFillPacketStatistics(final int packetSize,
            final int apid, final ISclk sclk, final long time) {

        updatePacketStatistics(packetSize, apid, sclk, time);
        this.fillPacketCount++;
        this.totalFillSize += packetSize;
    }

    /**
     * Gets the total number of bytes generated so far.
     * 
     * @return total byte count
     */
    public synchronized long getTotalPacketSize() {

        return this.totalPacketSize;
    }

    /**
     * Gets the total number of packets generated so far.
     * 
     * @return total packet count
     */
    public synchronized long getTotalPacketCount() {

        return this.totalPacketCount;
    }

    /**
     * Gets the average size of packets generated so far.
     * 
     * @return average packet size
     */
    public synchronized long getAveragePacketSize() {

        return this.averagePacketSize;
    }

    /**
     * Increments the count of invalid packets generated.
     */
    public void incrementInvalidPacketCount() {

        this.invalidPacketCount++;
    }

    /**
     * Gets the count of invalid packets generated.
     * 
     * @return the number of invalid packets created
     */
    public long getInvalidPacketCount() {

        return this.invalidPacketCount;
    }

    /**
     * Writes the summary of statistics to the given file. Overwrites any
     * current file.
     * 
     * @param theFile
     *            path of file to write to
     */
    public synchronized void writeToFile(final String theFile) {

        try {
            final FileWriter fw = new FileWriter(theFile, false);
            fw.write(toString());
            fw.close();
        } catch (final IOException e) {
            TraceManager.getDefaultTracer().error("I/O Error writing file:", e);

        }
    }

    /**
     * Gets the percentage of fill data generated so far.
     * 
     * @return percentage of fill data, between 0.0 and 100.0.
     */
    public double getFillPercent() {

        if (this.totalPacketSize == 0) {
            return 0.0;
        }
        return ((double) this.totalFillSize / (double) this.totalPacketSize) * 100.0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {

        final StringBuilder sb = new StringBuilder(1024);
        sb.append("Generator Start Time: " + new AccurateDateTime(this.startTime) + "\n");
        sb.append("Generator End Time: " + new AccurateDateTime(this.endTime) + "\n");
        sb.append("Packet Start SCLK: " + this.startSclk + "\n");
        sb.append("Packet End SCLK: " + this.endSclk + "\n");
        sb.append("Minimum Packet Size: "
                + (this.minPacketSize == Integer.MAX_VALUE ? 0
                        : this.minPacketSize) + "\n");
        sb.append("Maximum Packet Size: "
                + (this.maxPacketSize == Integer.MIN_VALUE ? 0
                        : this.maxPacketSize) + "\n");
        sb.append("Average Packet Size: " + this.averagePacketSize + "\n");
        sb.append("Total Packet Size: " + this.totalPacketSize + "\n");
        sb.append("Total Packet Count: " + this.totalPacketCount + "\n");
        sb.append("Total Fill Packet Size: " + this.totalFillSize + "\n");
        sb.append("Total Fill Packet Count: " + this.fillPacketCount + "\n");
        sb.append("Actual Fill %: " + String.format("%.2f", getFillPercent())
                + "\n");
        sb.append("Invalid Packet Count: " + this.invalidPacketCount + "\n");
        sb.append("Packet Counts by APID:\n");
        final SortedSet<Integer> sortedKeys = new TreeSet<Integer>(
                this.apidCounts.keySet());
        for (final Integer apid : sortedKeys) {
            sb.append("  " + apid + ": " + this.apidCounts.get(apid) + "\n");
        }

        return sb.toString();
    }

    /**
     * Gets the global statistics object for the current data generator. This
     * object is initialized to a general GeneratorStatistics object, but may be
     * overridden with a more specific object by a particular generator.
     * 
     * @return GeneratorStatistics object
     */
    public synchronized static GeneratorStatistics getGlobalStatistics() {

        return globalStats;
    }

    /**
     * Sets the global statistics object for the current data generator. This
     * object is initialized to a general GeneratorStatistics object, but may be
     * overridden with a more specific object by a particular generator.
     * 
     * @param genStats
     *            GeneratorStatistics object to set
     */
    public synchronized static void setGlobalStatistics(
            final GeneratorStatistics genStats) {

        globalStats = genStats;
    }

}