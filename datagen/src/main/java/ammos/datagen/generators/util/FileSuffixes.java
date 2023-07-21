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

/**
 * This enumeration defines common file suffixes used by the data generation
 * tools. Each enum value has an attached String that defines the suffix text.
 * 
 *
 */
public enum FileSuffixes {
    /**
     * Suffix for truth file names.
     */
    TRUTH("_truth.txt"),
    /**
     * Suffix for raw (CCSDS) packet file names.
     */
    RAW_PKT(".RAW_PKT"),
    /**
     * Suffix for raw (CCSDS) transfer frame file names.
     */
    RAW_TF(".RAW_TF"),
    /**
     * Suffix for statistics file names.
     */
    STATISTICS("_stats.txt"),
    /**
     * Suffix for log file names.
     */
    LOG(".log");

    private String suffix;

    /**
     * Constructor.
     * 
     * @param suffix
     *            the suffix string to be associated with this file suffix
     *            enumeration value.
     */
    private FileSuffixes(final String suffix) {

        this.suffix = suffix;
    }

    /**
     * Gets the suffix string associated with this file suffix enumeration
     * value.
     * 
     * @return file suffix string
     */
    public String getSuffix() {

        return this.suffix;
    }
}
