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
package jpl.gds.db.app.util;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.db.mysql.impl.removal.AbstractRemover;
import org.springframework.context.ApplicationContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Removes test sessions from database.
 *
 */
public class ContextRemover extends AbstractRemover {
    private static final String DELETE_SQL_START = "DELETE FROM ";
    private static final String DELETE_SQL_END   = " WHERE sessionId = ";

    private final List<String> tables;

    /**
     * Constructor.
     *
     * @param appContext the Spring Application Context
     */
    public ContextRemover(final ApplicationContext appContext) {
        super(appContext);

        this.tables = new ArrayList<>();

        addTable(tables, IDbTableNames.DB_PRODUCT_DATA_TABLE_NAME);
        addTable(tables, IDbTableNames.DB_CHANNEL_AGGREGATE_TABLE_NAME, IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                 IDbTableNames.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME,
                 IDbTableNames.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME,
                 IDbTableNames.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME);
        addTable(tables, IDbTableNames.DB_COMMAND_MESSAGE_DATA_TABLE_NAME, IDbTableNames.DB_COMMAND_STATUS_TABLE_NAME);
        addTable(tables, IDbTableNames.DB_EVR_DATA_TABLE_NAME, IDbTableNames.DB_EVR_METADATA_TABLE_NAME,
                 IDbTableNames.DB_SSE_EVR_DATA_TABLE_NAME, IDbTableNames.DB_SSE_EVR_METADATA_TABLE_NAME);
        addTable(tables, IDbTableNames.DB_FRAME_DATA_TABLE_NAME, IDbTableNames.DB_FRAME_BODY_TABLE_NAME);
        addTable(tables, IDbTableNames.DB_LOG_MESSAGE_DATA_TABLE_NAME);
        addTable(tables, IDbTableNames.DB_PACKET_DATA_TABLE_NAME, IDbTableNames.DB_PACKET_BODY_TABLE_NAME,
                 IDbTableNames.DB_SSE_PACKET_DATA_TABLE_NAME, IDbTableNames.DB_SSE_PACKET_BODY_TABLE_NAME);
        addTable(tables, IDbTableNames.DB_CFDP_FILE_GENERATION_DATA_TABLE_NAME,
                 IDbTableNames.DB_CFDP_FILE_UPLINK_FINISHED_DATA_TABLE_NAME,
                 IDbTableNames.DB_CFDP_INDICATION_DATA_TABLE_NAME, IDbTableNames.DB_CFDP_PDU_RECEIVED_DATA_TABLE_NAME,
                 IDbTableNames.DB_CFDP_PDU_SENT_DATA_TABLE_NAME, IDbTableNames.DB_CFDP_REQUEST_RECEIVED_DATA_TABLE_NAME,
                 IDbTableNames.DB_CFDP_REQUEST_RESULT_DATA_TABLE_NAME);

        tables.add(IDbTableNames.DB_END_SESSION_DATA_TABLE_NAME);
        tables.add(IDbTableNames.DB_SESSION_DATA_TABLE_NAME);
    }

    /**
     * Add tables to list if not seen before.
     *
     * @param table     List of tables being built
     * @param allTables List to add
     */
    private void addTable(final List<String> table, final String... allTables) {
        for (final String all : allTables) {
            // Get table name actually in use

            final String actualTableName = getActualTableName(all);

            if (!table.contains(actualTableName)) {
                table.add(actualTableName);
            }

            // If using extended tables, delete from old tables
            // as well.

            if (!all.equals(actualTableName) && !table.contains(all)) {
                table.add(all);
            }
        }
    }

    /**
     * Get tables.
     *
     * @return List of tables
     */
    public List<String> getTables() {
        return (this.tables);
    }

    /**
     * Remove test sessions from database.
     *
     * @param sessions List of sessions
     * @return Totals as an array
     * @throws DatabaseException exception
     */
    public long[] removeContext(final List<IDbSessionProvider> sessions) throws DatabaseException {
        try {
            this.statement = getStatement();

            for (final IDbSessionProvider session : sessions) {
                addRemoveToBatch(session);
            }

            final long[] totals = new long[this.tables.size()];
            Arrays.fill(totals, 0);

            final int[] results = this.statement.executeBatch();
            for (int i = 0; i < results.length; i++) {
                totals[i % this.tables.size()] += results[i];
            }

            this.statement.clearBatch();

            return (totals);
        }
        catch (final SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private void addRemoveToBatch(final IDbSessionProvider dsc) throws DatabaseException {
        final long testKey = dsc.getSessionId();
        final Integer hostKey = dsc.getSessionHostId();
        String hostWhereClause = "";

        if (hostKey != null) {
            hostWhereClause = " AND hostId = '" + hostKey + "'";
        }

        try {
            for (int i = 0; i < this.tables.size(); i++) {
                this.statement
                        .addBatch(DELETE_SQL_START + this.tables.get(i) + DELETE_SQL_END + testKey + hostWhereClause);
            }
        }
        catch (final SQLException e) {
            throw new DatabaseException(e);
        }
    }
}
