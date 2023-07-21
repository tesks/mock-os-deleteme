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
/**
 * 
 */
package jpl.gds.db.api.sql.fetch;

/**
 * Holder class for the five channel types
 * 
 * MPCS-8322 - Extracted this class from AbstractChannelValueFetchApp.
 *
 */
public class ChannelTypeSelect {
    /** MPCS-5008 sseHeader */

    /** Set true for FSW realtime channels */
    public boolean fswRealtime = false;

    /** Set true for FSW recorded channels */
    public boolean fswRecorded = false;

    /** Set true for monitor channels */
    public boolean monitor = false;

    /** Set true for header channels */
    public boolean header = false;

    /** Set true for SSE channels */
    public boolean sse = false;

    /** Set true for SSE header channels */
    public boolean sseHeader = false;

    /** For usage */
    public static final String RETRIEVE = "Retrieve selected types: " + "f=FSW realtime " + "r=FSW recorded "
            + "h=FSW header " + "m=Monitor " + "s=SSE " + "g=SSE header";

    /**
     * Constructor.
     */
    public ChannelTypeSelect() {
        super();
    }

    /**
     * Constructor. Convenience for use in JUnit tests.
     *
     * @param fsw
     *            Sets FSW realtime
     */
    public ChannelTypeSelect(final boolean fsw) {
        super();

        fswRealtime = fsw;
    }

    /**
     * Constructor. Convenience for use in JUnit tests.
     *
     * @param fsw
     *            Sets FSW realtime
     * @param rec
     *            Sets FSW recorded
     */
    public ChannelTypeSelect(final boolean fsw, final boolean rec) {
        super();

        fswRealtime = fsw;
        fswRecorded = rec;
    }

    /**
     * Constructor. Convenience for use in JUnit tests.
     *
     * @param fsw
     *            Sets FSW realtime
     * @param rec
     *            Sets FSW recorded
     * @param mon
     *            Sets monitor
     */
    public ChannelTypeSelect(final boolean fsw, final boolean rec, final boolean mon) {
        super();

        fswRealtime = fsw;
        fswRecorded = rec;
        monitor = mon;
    }

    /**
     * Return true if any types are selected.
     *
     * @return Active status
     */
    public boolean isActive() {
        return (fswRealtime || fswRecorded || monitor || header || sse || sseHeader);
    }
}
