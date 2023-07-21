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

package jpl.gds.db.mysql.impl.sql.fetch;

import jpl.gds.db.api.IDbInteractor;

import java.util.Set;
import java.util.TreeSet;

/**
 * Class to implement hostId and associated ids from DB.
 *
 * The where sub-clause will look something like this:
 * (ts.hostId=65536) AND (((ts.sessionId>=1) AND (ts.sessionId<=4)) OR
 *                        ((ts.sessionId>=6) AND (ts.sessionId<=8)) OR
 *                        (ts.sessionId IN (10,13)))
 *
 * The number of clauses on the right side will vary. You can have zero or
 * many interval clauses, and zero or one "IN" clause.
 *
 * If the "IN" clause contains only a single id it will look like: (ts.sessionId=10))
 *
 * Parentheses will be optimized when there is only a single clause:
 * (ts.hostId=65536) AND ((ts.sessionId>=1) AND (ts.sessionId<=4))
 * or
 * (ts.hostId=65536) AND (ts.sessionId IN (10,13))
 * or
 * (ts.hostId=65536) AND (ts.sessionId=10)
 *
 * Special case: if there are no ids:
 * 1=0
 *
 * Note that parentheses wrapping the whole thing are NOT supplied.
 */
public class HostAggregate {
    private final int       _hostId;
    private final Set<Long> _ids = new TreeSet<>();


    /**
     * Constructor.
     *
     * @param hostId
     *            the integer host id
     */
    public HostAggregate(final int hostId)
    {
        super();

        _hostId = hostId;
    }


    /**
     * Add additional id to list.
     *
     * @param contextID
     *            an additional context ID
     */
    public void addId(final long contextID)
    {
        _ids.add(contextID);
    }


    /**
     * Produce an optimized where sub-clause corresponding to this host and
     * id set.
     *
     * @param abbrev
     *            table 1 abbreviation
     * @param sb
     *            the StringBuilder that will contain the resulting where clause
     * @param keyField (sessionID or contextId)
     * @param hostField (host ID or context host ID)
     */
    public void produceWhere(final String        abbrev,
                             final StringBuilder sb,
                             final String keyField,
                             final String hostField)
    {
        final int size = _ids.size();

        if (size == 0)
        {
            sb.append("1=0"); // Impossible where

            return;
        }

        produceWhere(abbrev, _ids, sb, keyField, hostField);
    }

    private void produceWhere(final String        abbrev,
                              final Set<Long>     singles,
                              final StringBuilder sb,
                              final String keyField,
                              final String hostField)
    {
        final String  sid     = abbrev + "." + keyField;
        final int     phrases = singles.isEmpty() ? 0 : 1;
        final boolean extra   = phrases > 1;

        // Build host side

        sb.append('(').append(abbrev).append('.').append(hostField).append('=');
        sb.append(_hostId).append(") AND ");


        // Don't need extra parentheses if there is only one id phrase
        // or if we don't have the AND

        if (extra)
        {
            sb.append('(');
        }

        boolean firstClause = true;

        if (! singles.isEmpty())
        {
            if (firstClause)
            {
                firstClause = false;
            }
            else
            {
                sb.append(" OR ");
            }

            boolean       firstSingle = true;
            final boolean oneSingle   = (singles.size() == 1);

            sb.append('(').append(sid);

            if (oneSingle)
            {
                sb.append('=');
            }
            else
            {
                sb.append(" IN (");
            }

            for (final long id : singles)
            {
                if (firstSingle)
                {
                    firstSingle = false;
                }
                else
                {
                    sb.append(',');
                }

                sb.append(id);
            }

            if (! oneSingle)
            {
                // Close off the IN
                sb.append(')');
            }

            sb.append(')');
        }

        if (extra)
        {
            sb.append(')');
        }
    }
}