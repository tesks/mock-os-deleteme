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
package jpl.gds.globallad.memory;

public class GladMemoryThresholdChecker implements IMemoryThresholdChecker {

    private final double maxMemoryUsedPercentage;

    /**
     * @param maxMemoryUsedPercentage
     */
    public GladMemoryThresholdChecker(final double maxMemoryUsedPercentage) {
        this.maxMemoryUsedPercentage = maxMemoryUsedPercentage;
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
    public boolean isOverMemoryThreshold() {
        final double percent = calculateCurrentMemoryUsedPercentage();
        return percent >= maxMemoryUsedPercentage;
    }

    /**
     * Uses Runtime to calculate the percent of memory used of the max heap size.
     * 
     * @return percent of memory used
     */
    private double calculateCurrentMemoryUsedPercentage() {
        final Runtime rt = Runtime.getRuntime();

        final long free = rt.freeMemory();
        final long total = rt.totalMemory();
        final double used = total - free;
        final double max = rt.maxMemory();
        final double percentUsed = (used / max) * 100;

        return percentUsed;
    }
}
