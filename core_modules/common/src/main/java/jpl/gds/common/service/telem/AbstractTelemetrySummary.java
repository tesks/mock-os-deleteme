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

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;

/**
 * AbstractTelemetrySummary contains common statistics collected for a downlink run.
 * This class serves as the source of information for the Telemetry Ingestor, Telemetry Processor,
 * and legacy chill_down applications to display to the console at the end of command line downlink sessions,
 * or in the summary window of the downlink GUI.
 *
 *
 */
public class AbstractTelemetrySummary implements ITelemetrySummary
{
    /**
     * For debugging only.
     */
    private static final boolean DISPLAY_PID = false;


    private IAccurateDateTime startTime;
    private IAccurateDateTime stopTime;

    private String outputDirectory;

    private String contextName;

    private Long                 contextKey;


    @Override
    public long getContextKey() {
        return this.contextKey;
    }


    @Override
    public void setContextKey(final long dbKey) {
        this.contextKey = dbKey;
    }


    @Override
    public String getFullName() {
        return this.contextName;
    }

    @Override
    public void setFullName(final String name) {
        this.contextName = name;
    }

    @Override
    public IAccurateDateTime getStartTime() {
        return this.startTime;
    }

    /**
     * Sets the processing start time.
     *
     * @param startTime The time to set.
     */
    public void setStartTime(final IAccurateDateTime startTime) {
        this.startTime = startTime;
    }
    

    @Override
    public IAccurateDateTime getStopTime() {
        return this.stopTime;
    }


    /**
     * Sets the processing end time.
     *
     * @param stopTime The time to set.
     */
    public void setStopTime(final IAccurateDateTime stopTime) {
        this.stopTime = stopTime;
    }


    @Override
    public String getOutputDirectory() {
        return this.outputDirectory;
    }


    /**
     * Sets the output directory
     *
     * @param dir The directory path to set.
     */
    public void setOutputDirectory(final String dir) {
        this.outputDirectory = dir;
    }


    /**
     * Append required separator.
     *
     * @param sb      StringBuilder to append to
     * @param newline True for newline, otherwise comma
     */
    protected static void appendSeparator(final StringBuilder sb,
                                        final boolean       newline)
    {
        // Redo and add fragment check

        if (newline)
        {
            sb.append('\n');
        }
        else
        {
            sb.append(", ");
        }
    }


    /**
     * Append required separator.
     *
     * @param sb  StringBuilder to append to
     * @param add True to add summary
     *
     * @return The string builder
     */
    StringBuilder appendSummary(final StringBuilder sb,
                                               final boolean       add)
    {

        if (add)
        {
            sb.append("SUMMARY: ");
        }

        return sb;
    }

    @Override
    public String toString() {
        return toString("SESSION SUMMARY: Session ID: ");
    }

    /**
     * Outputs contents as a string.
     * @param prefix text to prefix the info with
     * @return text representation of object contents
     */
    public String toString(final String prefix)
    {

        // Want a newline
        return innerToString(true, prefix);
    }


    /**
     * Inner toString so we can control the separators.
     * We put out extra SUMMARY only if we are doing each
     * part on a line by itself.
     *
     * @param newline True for newline at end of each segment
     *
     * @return Constructed string
     */
    private String innerToString(final boolean newline, final String prefix)
    {
        // Redo and add fragment check

        final StringBuilder sb = new StringBuilder();

        sb.append(prefix);
        sb.append(contextName);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Start Time: ");
        sb.append(TimeUtility.getFormatter().format(startTime));
        appendSeparator(sb, newline);
        
        appendSummary(sb, newline).append("Output Directory: ");
        sb.append(outputDirectory);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Database Key: ");
        sb.append(contextKey);
        appendSeparator(sb, newline);

        if (DISPLAY_PID)
        {
            appendSummary(sb, newline).append("Java VM pid: ");
            sb.append(GdsSystemProperties.getJavaVmPid());
            appendSeparator(sb, newline);
        }

        return sb.toString();
    }


    @Override
    public String toStringNoBlanks(final String prefix)
    {

        // Want a comma
        return innerToString(false, prefix);
    }

    @Override
    public void populateBasicSummary(final IAccurateDateTime start, final IAccurateDateTime end, final String name,
                                     final String outputDir, final Long key) {
        setStartTime(start);
        setStopTime(end);
        setFullName(name);
        setOutputDirectory(outputDir);

        if (key != null) {
            setContextKey(key);
        }
    }

}
