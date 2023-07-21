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
package ammos.datagen.evr.util;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import ammos.datagen.generators.util.FieldGeneratorStatistics;

/**
 * This class tracks statistics for the EVR generator. It extends the field
 * generator statistics object, which tracks items common to all generators such
 * as packet APID and enum field counts. This class adds tracking of items like
 * counts per EVR ID, invalid opcode counts, etc.
 * 
 *
 */
public class EvrGeneratorStatistics extends FieldGeneratorStatistics {

    private final AtomicBoolean invalidOpcodeGenerated = new AtomicBoolean(
            false);
    private final AtomicBoolean invalidIdGenerated = new AtomicBoolean(false);
    private final AtomicBoolean invalidSeqIdGenerated = new AtomicBoolean(false);
    private long invalidOpcodeCount;
    private long totalOpcodeCount;
    private long invalidSeqIdCount;
    private long totalSeqIdCount;
    private final SortedMap<Long, Long> evrIdCounts = new TreeMap<Long, Long>();
    private final SortedMap<Long, Long> invalidEvrIdCounts = new TreeMap<Long, Long>();

    /**
     * Private constructor to enforce singleton nature.
     */
    public EvrGeneratorStatistics() {

        super();
        // do nothing
    }

    /**
     * Increments the total generation count for a specific EVR ID.
     * 
     * @param id
     *            the EVR event ID for the EVR count to increment
     */
    public void incrementTotalForEvrId(final long id) {

        synchronized (this.evrIdCounts) {
            final Long count = this.evrIdCounts.get(id);
            if (count == null) {
                this.evrIdCounts.put(id, 1L);
            } else {
                this.evrIdCounts.put(id, count.longValue() + 1);
            }
        }
    }

    /**
     * Gets the total generation count for a specific EVR ID.
     * 
     * @param id
     *            the EVR event ID for the EVR count to fetch
     * @return the EVR count
     */
    public long getTotalForEvrId(final long id) {

        synchronized (this.evrIdCounts) {
            final Long count = this.evrIdCounts.get(id);
            if (count == null) {
                return 0;
            } else {
                return count;
            }
        }
    }

    /**
     * Increments the total generation count for a specific invalid EVR ID.
     * 
     * @param id
     *            the EVR event ID for the EVR count to increment
     */
    public void incrementTotalForInvalidEvrId(final long id) {

        synchronized (this.invalidEvrIdCounts) {
            final Long count = this.invalidEvrIdCounts.get(id);
            if (count == null) {
                this.invalidEvrIdCounts.put(id, 1L);
            } else {
                this.invalidEvrIdCounts.put(id, count.longValue() + 1);
            }
        }
        this.invalidIdGenerated.set(true);
    }

    /**
     * Gets the total generation count for a specific invalid EVR ID.
     * 
     * @param id
     *            the EVR event ID for the EVR count to fetch
     * @return the EVR count
     */
    public long getTotalForInvalidEvrId(final long id) {

        synchronized (this.invalidEvrIdCounts) {
            final Long count = this.invalidEvrIdCounts.get(id);
            if (count == null) {
                return 0;
            } else {
                return count;
            }
        }
    }

    /**
     * Simultaneously sets the flag indicating whether at least one invalid
     * opcode value has been generated and gets the previous value of the flag.
     * 
     * @param invalidOpcodeGenerated
     *            value to set the flag to
     * @return true if one invalid opcode was previously generated, false if not
     */
    public boolean getAndSetInvalidOpcodeGenerated(
            final boolean invalidOpcodeGenerated) {

        return this.invalidFlagsDisabled
                || this.invalidOpcodeGenerated
                        .getAndSet(invalidOpcodeGenerated);
    }

    /**
     * Simultaneously sets the flag indicating whether at least one invalid EVR
     * ID packet has been generated and gets the previous value of the flag.
     * 
     * @param invalidIdGenerated
     *            value to set the flag to
     * @return true if one invalid ID was previously generated, false if not
     */
    public boolean getAndSetInvalidIdGenerated(final boolean invalidIdGenerated) {

        return this.invalidFlagsDisabled
                || this.invalidIdGenerated.getAndSet(invalidIdGenerated);
    }

    /**
     * Simultaneously sets the flag indicating whether at least one invalid
     * SEQID value has been generated and gets the previous value of the flag.
     * 
     * @param invalidSeqIdGenerated
     *            value to set the flag to
     * @return true if one invalid SEQID was previously generated, false if not
     */
    public boolean getAndSetInvalidSeqIdGenerated(
            final boolean invalidSeqIdGenerated) {

        return this.invalidFlagsDisabled
                || this.invalidSeqIdGenerated.getAndSet(invalidSeqIdGenerated);
    }

    /**
     * Increments the count of invalid opcode values generated.
     */
    public synchronized void incrementInvalidOpcodeCount() {

        this.invalidOpcodeCount++;
        this.invalidOpcodeGenerated.set(true);
    }

    /**
     * Gets the count of invalid opcode values generated.
     * 
     * @return the number of invalid opcodes created
     */
    public synchronized long getInvalidOpcodeCount() {

        return this.invalidOpcodeCount;
    }

    /**
     * Increments the count of total opcode values generated.
     */
    public synchronized void incrementTotalOpcodeCount() {

        this.totalOpcodeCount++;
    }

    /**
     * Gets the count of total opcode values generated.
     * 
     * @return the number of opcodes created
     */
    public synchronized long getTotalOpcodeCount() {

        return this.totalOpcodeCount;
    }

    /**
     * Increments the count of invalid SeqID values generated.
     */
    public synchronized void incrementInvalidSeqIdCount() {

        this.invalidSeqIdCount++;
        this.invalidSeqIdGenerated.set(true);
    }

    /**
     * Gets the count of invalid SEQID values generated.
     * 
     * @return the number of invalid SEQIDs created
     */
    public synchronized long getInvalidSeqIdCount() {

        return this.invalidSeqIdCount;
    }

    /**
     * Increments the count of total SEQID values generated.
     */
    public synchronized void incrementTotalSeqIdCount() {

        this.totalSeqIdCount++;
    }

    /**
     * Gets the count of total SEQID values generated.
     * 
     * @return the number of SEQIDs created
     */
    public synchronized long getTotalSeqIdCount() {

        return this.totalSeqIdCount;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {

        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Total Opcode Values Generated: " + this.totalOpcodeCount
                + "\n");
        sb.append("Invalid Opcode Values Generated: " + this.invalidOpcodeCount
                + "\n");
        sb.append("Total SeqId Values Generated: " + this.totalSeqIdCount
                + "\n");
        sb.append("Invalid SeqId Values Generated: " + this.invalidSeqIdCount
                + "\n");
        sb.append("Counts per Valid EVR ID:\n");
        for (final Long id : this.evrIdCounts.keySet()) {
            final long count = this.evrIdCounts.get(id).longValue();
            sb.append("  " + id + " (0x" + Long.toHexString(id).toUpperCase()
                    + "): " + count + "\n");
        }
        if (!this.invalidEvrIdCounts.isEmpty()) {
            sb.append("Counts per Invalid EVR ID:\n");
            for (final Long id : this.invalidEvrIdCounts.keySet()) {
                final long count = this.invalidEvrIdCounts.get(id).longValue();
                sb.append("  " + id + " (0x"
                        + Long.toHexString(id).toUpperCase() + "): " + count
                        + "\n");
            }
        }
        return sb.toString();
    }

}
