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

package jpl.gds.product.processors.descriptions;

import jpl.gds.common.config.ConfigurationConstants;

/**
 * Abstract implementation of IPdppDescription containing utilities that can be reused by inheriting
 * Description classes.
 */
public class AbstractPdppProcessorDescription implements IPdppDescription {

    private static final int SESSION_TABLE_NAME_FIELD_MAX_LENGTH = ConfigurationConstants.NAME_LENGTH;
    private static final int SESSION_TABLE_DESCRIPTION_FIELD_MAX_LENGTH	= ConfigurationConstants.DESC_LENGTH;

    private final String sessionSuffix;
    private final String backlinkExplanation;

    public String getSessionSuffix() {
        return sessionSuffix;
    }

    public String getBacklinkExplanation() {
        return backlinkExplanation;
    }

    /**
     * Constructor allowing inheriting classes to set sessionSuffix and backlinkExplanation
     * @param sessionSuffix
     * @param backlinkExplanation
     * @return
     */
    public AbstractPdppProcessorDescription(String sessionSuffix, String backlinkExplanation) {
        this.sessionSuffix = sessionSuffix;
        this.backlinkExplanation = backlinkExplanation;
    }

    /**
     * Generates the name of the new PDPP session off of the parent session name.
     * @param parentSessionName
     * @return
     */
    public String generateName(String parentSessionName) {
        return truncateToFieldLengthWithAppendage(parentSessionName,
                "_",
                null,
                getSessionSuffix(),
                null,
                SESSION_TABLE_NAME_FIELD_MAX_LENGTH);
    }

    /**
     * Builds the description from the parent description.
     * @param description
     * @return
     */
    public String generateDescription(String description) {
        return truncateToFieldLengthWithAppendage(description,
                " ",
                "(",
                getSessionSuffix(),
                ")",
                SESSION_TABLE_DESCRIPTION_FIELD_MAX_LENGTH);
    }

    /**
     * Builds the type from the parent session and the backlink Explanation.
     * @param sessionNumber
     * @return
     */
    public String generateType(Long sessionNumber) {
        return String.format("%s %d", getBacklinkExplanation(), sessionNumber == null ? -1 : sessionNumber);

    }

    /**
     * 07/09/2012 - MPCS-3916
     * Truncate database fields to fit in database schema
     *
     * @param srcValue
     * @param separator
     * @param prefix
     * @param appendage
     * @param postfix
     * @param maxLength
     *
     * @return
     */
    private String truncateToFieldLengthWithAppendage(final String srcValue, final String separator,
                                                      final String prefix, final String appendage,
                                                      final String postfix, final int maxLength) {
        final StringBuilder sb = new StringBuilder((null == srcValue) ? "" : srcValue);
        final String sep = ((null != separator) && (sb.length() > 0)) ? separator : "";
        final String prepend = (null == prefix) ? "" : prefix;
        final String append = (null == postfix) ? "" : postfix;
        final int overage = (sb.length() + sep.length() + prepend.length() + appendage.length() + append.length()) - maxLength;
        if (overage > 0) {
            sb.setLength(sb.length() - overage);
        }
        sb.append(sep);
        sb.append(prepend);
        sb.append(appendage);
        sb.append(append);
        return sb.toString();
    }

}
