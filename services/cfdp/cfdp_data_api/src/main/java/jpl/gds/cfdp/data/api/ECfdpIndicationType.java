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

package jpl.gds.cfdp.data.api;

/**
 * This enum contains all of the types of indications that
 * CFDP transaction can specify.
 */
public enum ECfdpIndicationType {

    /* Key indication types */
    TRANSACTION("tx"),
    REPORT,
    SUSPENDED,
    RESUMED,
    FAULT("ft"),
    TRANSACTION_FINISHED("tf"),
    ABANDONED("ab"),
    NEW_TRANSACTION_DETECTED("txd"),

    /* Progress indication types */
    EOF_SENT,
    METADATA_RECV,
    FILE_SEGMENT_RECV,
    EOF_RECV;

    private String mtakCsvKeyword;

    /**
     * default constructor
     */
    ECfdpIndicationType() {
        this(null);
    }

    /**
     * Constructor with an MTAK CSV keyword
     * @param mtakCsvKeyword the MTAK CSV keyword to be specified by this indication
     */
    ECfdpIndicationType(final String mtakCsvKeyword) {
        this.mtakCsvKeyword = mtakCsvKeyword;
    }

    /**
     * Get the MTAK CSV keyword for this indication
     * @return the String specifyin the MTAK CSV keyword for this indication, or null if no value is specified
     */
    public String getMtakCsvKeyword() {
        return mtakCsvKeyword;
    }

    /**
     * Get if the indication is a final type of indication (no other indications for the transaction will be
     * received/created after it)
     * @return TRUE if the indication is a final type, FALSE otherwise
     */
    public boolean isFinal() {
        return this.equals(FAULT) || this.equals(TRANSACTION_FINISHED) || this.equals(ABANDONED);
    }

}
