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

import java.sql.PreparedStatement;

import jpl.gds.db.api.sql.fetch.WhereControl.WhereControlException;

interface IWhereType {
    /**
     * Generates portion of where clause.
     *
     * @param sb
     *            Where clause in progress
     * @param abbrev
     *            Table abbreviation
     * @param joinAbbrev
     *            Join table abbreviation
     *
     * @throws WhereControlException
     *             if there is an error
     */
    public void addToWhereClause(final StringBuilder sb, final String abbrev, final String joinAbbrev)
            throws WhereControlException;

    /**
     * Sets parameter in prepared statement as required
     *
     * @param ps
     *            Prepared statement
     * @param index
     *            Beginning index for parameters
     *
     * @return True if parameter was set (and index used)
     *
     * @throws WhereControlException
     *             if there is an error
     */
    public boolean setParameter(final PreparedStatement ps, final int index) throws WhereControlException;
}