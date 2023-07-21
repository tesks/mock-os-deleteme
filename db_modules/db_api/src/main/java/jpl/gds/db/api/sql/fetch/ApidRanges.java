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
package jpl.gds.db.api.sql.fetch;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;


/**
 * Keeps track of desired APID ranges and implements useful methods.
 *
 */
public class ApidRanges extends LongRanges
{
    private static final String COLUMN = "apid";
    /* MPCS-7298 -Removed dependency upon packet header factory.
     * The code dependency is not worth the benefit. 
     */
    private static final int MAX_PACKET_APID = 65535;

    /**
     * Constructor ApidRanges.
     */
    public ApidRanges()
    {
        super();
    }


    /**
     * Process option arguments.
     *
     * @param apidStr Option value to parse
     * @param option  Option name
     *
     * @throws MissingOptionException Missing option exception
     * @throws ParseException         Parse exception
     */
    public void processOptions(final String apidStr,
                               final String option)
        throws MissingOptionException, ParseException
    {
        processOptions(apidStr,
                       option,
                       COLUMN,
                       MAX_PACKET_APID);
    }


    /**
     * Construct where clause for all implemented ranges.
     *
     * @param tableAbbrev Table abbreviation
     *
     * @return String
     */
    public String whereClause(final String tableAbbrev)
    {
        return whereClause(tableAbbrev, COLUMN);
    }
}
