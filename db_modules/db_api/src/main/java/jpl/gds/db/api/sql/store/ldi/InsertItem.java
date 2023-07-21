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
package jpl.gds.db.api.sql.store.ldi;

import java.io.File;

import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.Quintuplet;

/**
 * The gatherer puts these on the queue for the inserter. There's a file
 * stream, a table name, a field list, a count of the number of insertions
 * in the file stream, and a set clause.
 */
public class InsertItem extends Quintuplet<File, String, String, Long, String> {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param file
     *            File stream
     * @param table
     *            Table name
     * @param fields
     *            Field list
     * @param count
     *            Insertion count
     * @param setClause
     *            Set clause
     */
    public InsertItem(final File file, final String table, final String fields, final Long count, final String setClause) {
        super(file, table, fields, count, StringUtil.safeTrim(setClause));
    }

    /**
     * Getter for set clause.
     *
     * @return The set clause
     */
    public String getSetClause() {
        return getFive();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("InsertItem [file=").append(getOne()).append(", table=").append(getTwo()).append(", fields()=").append(getThree())
        .append(", count=").append(getFour()).append(", setClause=").append(getFive()).append("]");
        return builder.toString();
    }
}

