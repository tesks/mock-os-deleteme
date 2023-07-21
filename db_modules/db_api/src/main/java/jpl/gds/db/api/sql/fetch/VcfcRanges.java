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
 * Keeps track of desired VCFC ranges and implements useful methods.
 */
public class VcfcRanges extends LongRanges
{
    private static final String COLUMN = "vcfc";


    /**
     * Constructor VcfcRanges.
     */
    public VcfcRanges()
    {
        super();
    }


    /**
     * Process option arguments.
     *
     * @param vcfcStr VCFC string
     * @param option  Option string
     *
     * @throws MissingOptionException Missing option exception
     * @throws ParseException         Parse exception
     */
    public void processOptions(final String vcfcStr,
                               final String option)
        throws MissingOptionException, ParseException
    {
        processOptions(vcfcStr, option, COLUMN, Long.MAX_VALUE);
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
