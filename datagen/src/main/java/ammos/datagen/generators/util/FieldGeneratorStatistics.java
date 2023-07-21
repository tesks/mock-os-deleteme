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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is an intermediate statistics class that extends the basis statistics
 * class beyond packet-related counts, to include statistics for field types
 * common to several data generators (boolean and enum fields, for instance).
 * 
 *
 */
public class FieldGeneratorStatistics extends GeneratorStatistics {

    private long invalidEnumCount;
    private long totalEnumCount;
    private long totalBooleanCount;
    private final AtomicBoolean invalidEnumGenerated = new AtomicBoolean(false);

    /**
     * Increments the count of invalid enumeration values generated.
     */
    public void incrementInvalidEnumCount() {

        this.invalidEnumCount++;
        this.invalidEnumGenerated.set(true);
    }

    /**
     * Gets the count of invalid enumeration values generated.
     * 
     * @return the number of invalid enums created
     */
    public long getInvalidEnumCount() {

        return this.invalidEnumCount;
    }

    /**
     * If invalid flags are enabled in this instance, simultaneously sets the
     * flag indicating whether at least one invalid enumeration value has been
     * generated and gets (returns) the previous value of the flag. If invalid
     * flags are not enabled in this instance, this method returns true.
     * 
     * @param invalidEnumGenerated
     *            value to set the flag to
     * @return true if one invalid enumeration was previously generated or if
     *         invalid flags disabled, false if not
     */
    public boolean getAndSetInvalidEnumGenerated(
            final boolean invalidEnumGenerated) {

        return this.invalidFlagsDisabled
                || this.invalidEnumGenerated.getAndSet(invalidEnumGenerated);
    }

    /**
     * Increments the count of all enumeration values generated.
     */
    public void incrementTotalEnumCount() {

        this.totalEnumCount++;
    }

    /**
     * Gets the count of all enumeration values generated.
     * 
     * @return the number of enums created
     */
    public long getTotalEnumCount() {

        return this.totalEnumCount;
    }

    /**
     * Increments the count of all boolean values generated.
     */
    public void incrementTotalBooleanCount() {

        this.totalBooleanCount++;
    }

    /**
     * Gets the count of all boolean values generated.
     * 
     * @return the number of booleans created
     */
    public long getTotalBooleanCount() {

        return this.totalBooleanCount;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {

        final StringBuilder sb = new StringBuilder(super.toString());

        sb.append("Total Enum Values Generated: " + this.totalEnumCount + "\n");
        sb.append("Invalid Enum Values Generated: " + this.invalidEnumCount
                + "\n");
        sb.append("Total Boolean Values Generated: " + this.totalBooleanCount
                + "\n");

        return sb.toString();
    }
}
