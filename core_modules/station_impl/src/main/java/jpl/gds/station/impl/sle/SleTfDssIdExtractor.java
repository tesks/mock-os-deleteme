/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.station.impl.sle;

/**
 * DSS ID extractor for the SLE_TF header.
 * <p>
 * The format of the DSS ID in DSN's SLE RAF and RCF is defined in 3.1.1.6 of 0243-Telecomm. This has been confirmed
 * using a Psyche DSN SLE RAF OFFLINE provider, which transferred the DSS ID as a string in the format "DSS-05", which
 * represents the DTF-21 antenna ID. All antenna IDs can be looked up at the DSN Station Identification and Registry
 * Tool (DSIRT) at https://dsnpct.jpl.nasa.gov/dsirt/
 *
 */
public class SleTfDssIdExtractor {

    protected static final String DSS_ID_REGEX   = "^DSS-[\\d]{2}$";
    protected static final int    DSS_ID_INT_IDX = 4;

    /**
     * Private constructor
     */
    private SleTfDssIdExtractor() {

    }

    /**
     * Extracts a numerical DSS ID from string, in the form "DSS-NN" where NN is the returned integer ID
     *
     * @param dssIdStr "DSS-NN"
     * @return integer DSS ID, or 0 for unknown
     */
    public static int extractDssId(String dssIdStr) {
        // DSS-NN
        String dssStr = dssIdStr.trim();
        if (dssStr.matches(DSS_ID_REGEX)) {
            return Integer.parseInt(dssStr.substring(DSS_ID_INT_IDX));
        } else {
            return 0;
        }
    }
}
