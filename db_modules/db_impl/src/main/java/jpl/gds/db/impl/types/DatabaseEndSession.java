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
package jpl.gds.db.impl.types;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.db.api.types.IDbEndSessionUpdater;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;


/**
 * A representation of an EndSession record that is stored in the database.
 */
public class DatabaseEndSession extends AbstractDatabaseItem implements IDbEndSessionUpdater
{
    /** MPCS-6808 String constants pushed up */

    /**
     * @param appContext
     *            the Spring Application Context
     */
    public DatabaseEndSession(final ApplicationContext appContext) {
		super(appContext);
	}


	/** MPCS-6808 Added */
    private static final List<String> csvSkip =
        new ArrayList<String>(0);

    private static final String CSV_COL_HDR = DQ + "Session";

    private IAccurateDateTime         endTime     = null;


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.types.IDatabaseEndSession#getEndTime()
	 */
    @Override
	@SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getEndTime() {
        return this.endTime;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.types.IDatabaseEndSession#getFileData(java.lang.String)
	 */
    @Override
    public Map<String, String> getFileData(final String NO_DATA) {
        return null;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.types.IDatabaseEndSession#parseCsv(java.lang.String, java.util.List)
	 */
    @Override
    public void parseCsv(final String csvStr,
                         final List<String> csvColumns)
    {
		// The following removes the start/end quotes w/ the substring
		// and splits based on ",".  It leaves the trailing empty string in the case that 
		// csvStr ends with "".  The empty strings server as place holders.
        final String[] dataArray = csvStr.substring(1, csvStr.length() - 1).split("\",\"", -1);


        if ((csvColumns.size() + 1) != dataArray.length)
        {
            throw new IllegalArgumentException("CSV column length mismatch, received " +
                                               dataArray.length                        +
                                               " but expected "                        +
                                               (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        sessionId   = null;
        sessionHost = null;
        endTime     = null;

        int    next  = 1; // Skip recordType
        String token = null;

        for (final String cce : csvColumns)
        {
            token = dataArray[next].trim();

            ++next;

            if (token.isEmpty())
            {
                continue;
            }

            final String upcce = cce.toUpperCase();

            try
            {
                switch (upcce)
                {
                    case "SESSIONID":
                        sessionId = Long.valueOf(token);
                        break;

                    case "SESSIONHOSTID":
                        sessionHostId = Integer.valueOf(token);
                        break; 

                    case "ENDTIME":
                        endTime = new AccurateDateTime(token);
                        break;

                    default:
                        if (! csvSkip.contains(upcce))
                        {
                            log.warn("Column " + 
                                     cce       +
                                     " is not supported, skipped");

                            csvSkip.add(upcce);
                        }

                        break;
                }
             }
             catch (final RuntimeException re)
             {
                 re.printStackTrace();

                 throw re;
		     }
             catch (final Exception e)
             {
                 e.printStackTrace();
             }
        }
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.types.IDatabaseEndSession#setEndTime(java.util.Date)
	 */
    @Override
	@SuppressWarnings("EI_EXPOSE_REP2")
    public void setEndTime(final IAccurateDateTime endTime) {
        this.endTime = endTime;
    }


    /*
     * {@inheritDoc}
     *
     * @version MPCS-6808  Massive rewrite
     */
    /* (non-Javadoc)
	 * @see jpl.gds.db.api.types.IDatabaseEndSession#toCsv(java.util.List)
	 */
    @Override
    public String toCsv(final List<String> csvColumns)
    {
        final StringBuilder csv = new StringBuilder(1024);

        final DateFormat df = TimeUtility.getFormatterFromPool();

		csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns)
        {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce)
            {
                case "SESSIONID":
                    if (sessionId != null)
                    {
                        csv.append(sessionId);
                    }
                    break;

                case "SESSIONHOSTID":
                    if (sessionHostId != null)
                    {
                        csv.append(sessionHostId);
                    }
                    break;

                case "ENDTIME":
                   if (endTime != null)
                   {
                       csv.append(df.format(endTime));
                   }
                   break;

                default:

                    if (! csvSkip.contains(upcce))
                    {
                        log.warn("Column " + 
                                 cce       +
                                 " is not supported, skipped");

                        csvSkip.add(upcce);
                    }

                    break;
            }
        }

		csv.append(CSV_COL_TRL);

        TimeUtility.releaseFormatterToPool(df);

        return csv.toString();
    }
}
