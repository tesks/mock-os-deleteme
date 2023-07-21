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
package jpl.gds.evr.api.service.extractor;

import java.util.List;

import jpl.gds.dictionary.api.evr.IEvrDefinition;

/**
 * An interface to be implemented by EVR extraction utilities, which provide
 * utility functions for use in decommutation of EVRs.
 * 
 * @since R8
 */
public interface IEvrExtractorUtility {

    /**
     * Formats an EVR message string by inserting parameters into it, replaces
     * values in the given list of EVR parameters as defined by sequence ID,
     * enum, and OPCODE replacement arguments, and returns a formatted EVR
     * message string.
     * 
     * @param parameters
     *            list of parameter RawEvrData values extracted from the EVR
     * @param inFormat
     *            the EVR format (message) string
     * @return the complete EVR message string with all parameters inserted and
     *         replacements made
     * @param evrDefinition the dictionary definition object for the EVR
     * @throws EvrExtractorException
     *             if there is a problem of any type with this operation
     */
    public String replaceParameters(List<IRawEvrData> parameters,
            String inFormat, IEvrDefinition evrDefinition)
            throws EvrExtractorException;

    /**
     * Inserts the given text in the format string after the current formatter.
     * If there is no Nth format specifier, the string is left unchanged.
     * 
     * @param format
     *            the entire EVR format string
     * @param textToBeInserted
     *            the string that will be inserted into the format
     * @param inIndex
     *            the index (starting at 1) of the format specifier to replace
     * @return the new format string
     * 
     * 8/28/13  Rewrote this method to use SprintfUtil
     *          method rather than pattern matcher, which was buggy.
     * 5/28/14. Moved here from AbstractEvrDefinition
     */
    public String insertTextAfterParameter(String format,
            String textToBeInserted, int inIndex);

    /**
     * Replace the Nth %<format> specifier in the given format string with a
     * generic %s formatter. If there is no Nth format specifier, or an error is
     * found in the format string, the string is left unchanged, though the
     * condition is logged.
     * 
     * @param format
     *            the entire EVR format string
     * @param inIndex
     *            the index (starting at 1) of the format specifier to replace
     * @return the new format string
     * 
     *  8/28/13  Rewritten to use SprintfUtil instead
     *          of pattern matcher, because the old implementation was buggy and
     *          replaced the wrong things at times.
     * 5/28/14. Moved here from AbstractEvrDefinition
     */
    public String replaceFormat(String format, int inIndex);

}