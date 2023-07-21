/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.tc.mps.impl.properties;

import jpl.gds.shared.config.GdsHierarchicalProperties;

/**
 * {@code MpsTcProperties} is the AMPCS properties class for retrieving configured properties for the MPSA
 * telecommand implementation module.
 *
 * @since 8.2.0
 */
public class MpsTcProperties extends GdsHierarchicalProperties {

    private static final String PROPERTY_FILE = "mps_tc.properties";

    private static final String PROPERTY_PREFIX = "mpsTc.";

    // SCMF blocks
    private static final String SCMF_HEADER_BLOCK_PREFIX = PROPERTY_PREFIX + "scmf.header.";
    private static final String SCMF_DATA_RECORD_BLOCK_PREFIX = PROPERTY_PREFIX + "scmf.dataRecord.";

    // SCMF header properties
    private static final String SCMF_HEADER_DEFAULT_COMMENT_PROPERTY = SCMF_HEADER_BLOCK_PREFIX + "defaultComment";
    private static final String SCMF_HEADER_DEFAULT_TITLE_PROPERTY = SCMF_HEADER_BLOCK_PREFIX + "defaultTitle";

    // SCMF data record properties
    private static final String SCMF_DATA_RECORD_DEFAULT_FIRST_RECORD_COMMENT_PROPERTY =
            SCMF_DATA_RECORD_BLOCK_PREFIX + "defaultFirstRecordComment";
    private static final String SCMF_DATA_RECORD_DEFAULT_MARKER_COMMENT_PROPERTY =
            SCMF_DATA_RECORD_BLOCK_PREFIX + "defaultMarkerComment";
    private static final String SCMF_DATA_RECORD_DEFAULT_COMMAND_COMMENT_PROPERTY =
            SCMF_DATA_RECORD_BLOCK_PREFIX + "defaultCommandComment";
    private static final String SCMF_DATA_RECORD_DEFAULT_LAST_RECORD_COMMENT_PROPERTY =
            SCMF_DATA_RECORD_BLOCK_PREFIX + "defaultLastRecordComment";

    /**
     * Constructor that loads the default property file, which will be found using a
     * standard configuration search.
     */
    public MpsTcProperties() {
        super(PROPERTY_FILE, true);
    }

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    /**
     * Retrieve the configured SCMF header default comment String.
     *
     * @return SCMF header default comment
     */
    public String getScmfHeaderDefaultCommentProperty() {
        return getProperty(SCMF_HEADER_DEFAULT_COMMENT_PROPERTY, "SCMF created by AMPCS via MPSA");
    }

    /**
     * Retrieve the configured SCMF header default title String.
     *
     * @return SCMF header default title
     */
    public String getScmfHeaderDefaultTitleProperty() {
        return getProperty(SCMF_HEADER_DEFAULT_TITLE_PROPERTY, "SCMF created by AMPCS via MPSA");
    }

    /**
     * Retrieve the configured SCMF first data record comment String.
     *
     * @return default SCMF first data record comment
     */
    public String getScmfDataRecordDefaultFirstRecordCommentProperty() {
        return getProperty(SCMF_DATA_RECORD_DEFAULT_FIRST_RECORD_COMMENT_PROPERTY, "Start sequence plus marker");
    }

    /**
     * Retrieve the configured SCMF marker data record comment String.
     *
     * @return default SCMF marker data record comment
     */
    public String getScmfDataRecordDefaultMarkerCommentProperty() {
        return getProperty(SCMF_DATA_RECORD_DEFAULT_MARKER_COMMENT_PROPERTY, "Marker");
    }

    /**
     * Retrieve the configured SCMF command data record comment String.
     *
     * @return default SCMF command data record comment
     */
    public String getScmfDataRecordDefaultCommandCommentProperty() {
        return getProperty(SCMF_DATA_RECORD_DEFAULT_COMMAND_COMMENT_PROPERTY, "Command bits");
    }

    /**
     * Retrieve the configured SCMF last data record comment String.
     *
     * @return default SCMF last data record comment
     */
    public String getScmfDataRecordDefaultLastRecordCommentProperty() {
        return getProperty(SCMF_DATA_RECORD_DEFAULT_LAST_RECORD_COMMENT_PROPERTY, "Final marker");
    }

}