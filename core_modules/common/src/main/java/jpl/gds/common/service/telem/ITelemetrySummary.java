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
package jpl.gds.common.service.telem;

import jpl.gds.shared.time.IAccurateDateTime;

/**
 * The Interface for a POJO to return Telemetry Summary Status via RESTful interface
 * 
 */
public interface ITelemetrySummary {

    /**
     * Gets the context id.
     * 
     * @return the context database key (number).
     */
    long getContextKey();


    /**
     * Sets the context database key (number).
     *
     * @param dbKey The key to set.
     */
    void setContextKey(final long dbKey);

    /**
     * Gets the context full name.
     * @return the full name text.
     */
    String getFullName();


    /**
     * Sets the context full name.
     *
     * @param name The full name text to set.
     */
    void setFullName(final String name);

    /**
     * Gets the processing start time.
     * 
     * @return the start time
     */
    IAccurateDateTime getStartTime();

    /**
     * Gets the processing end time.
     * 
     * @return the stop time
     */
    IAccurateDateTime getStopTime();

    /**
     * Gets the output directory.
     * 
     * @return the output directory path
     */
    String getOutputDirectory();


    /**
     * Sets the output directory
     *
     * @param dir The directory path to set.
     */
    void setOutputDirectory(final String dir);


    /**
     * A version of toString() whose output contains no line feeds suitable for publication in a log message.
     *
     * @param prefix string to prefix the output with
     * @return text version of this object
     */
    String toStringNoBlanks(final String prefix);

    /**
     * Populates common metadata for an ITelemetrySummary
     * 
     * @param start
     *            start time
     * @param end
     *            end time
     * @param name
     *            context name
     * @param outputDir
     *            output directory
     * @param key
     *            context id
     */
    void populateBasicSummary(IAccurateDateTime start, IAccurateDateTime end, String name, String outputDir, Long key);

}