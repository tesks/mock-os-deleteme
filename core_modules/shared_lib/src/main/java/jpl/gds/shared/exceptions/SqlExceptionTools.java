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
package jpl.gds.shared.exceptions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jpl.gds.shared.log.Tracer;
//import jpl.gds.shared.sys.SystemUtilities;

/**
 * Class SqlExceptionTools.
 *
 * SQLException has both a "cause" chain (like all other Throwable) and a
 * separate SQLException chain. Each element on the latter can have its own
 * "cause" chain. To make things more complicated, SQLWarning is a
 * SQLException itself and has a warning chain. However, it appears that
 * SQLWarning does not use the SQLException chain. In other words, SQLWarning
 * has a cause chain and a warning chain, SQLException has a cause chain and an
 * exception chain, and all else have just a cause chain.
 *
 * The set is used to make sure that we cannot get into an infinite loop,
 * although it is unlikely that there will be loops for exceptions generated
 * by SQL library code.
 *
 */
public final class SqlExceptionTools extends Object {
    /**
     * Prevent instantiation.
     */
    private SqlExceptionTools() {
        super();
    }

    /**
     * Add single SQL exception with the extra information.
     *
     * @param sb
     *            String builder
     * @param sqle
     *            SQL exception
     */
    private static void rollUpSqlException(final StringBuilder sb, final SQLException sqle) {
        if (sb.length() > 0) {
            sb.append('\n');
        }

        sb.append(sqle);
        sb.append(" code(").append(sqle.getErrorCode()).append(')');
        sb.append(" state(").append(sqle.getSQLState()).append(')');

        int level = 0;

        for (final StackTraceElement ste : sqle.getStackTrace()) {
            sb.append("\n    (").append(level++).append(") ").append(ste);
        }
    }

    /**
     * Add single throwable.
     *
     * @param sb
     *            String builder
     * @param t
     *            Throwable
     */
    private static void rollUpThrowable(final StringBuilder sb, final Throwable t) {
        if (sb.length() > 0) {
            sb.append('\n');
        }

        sb.append(t);

        int level = 0;

        for (final StackTraceElement ste : t.getStackTrace()) {
            sb.append("\n    (").append(level++).append(") ").append(ste);
        }
    }

    /**
     * Process the cause chain.
     *
     * @param sb
     *            String builder
     * @param start
     *            Initial exception
     * @param seen
     *            Set of already processed exceptions
     */
    private static void processCauseChain(final StringBuilder sb, final Throwable start, final Set<Throwable> seen) {
        for (Throwable t = start; t != null; t = t.getCause()) {
            if (seen.contains(t)) {
                // If we've seen it we've processed its chains, so bail
                break;
            }

            seen.add(t);

            if (t instanceof SQLWarning) {
                final SQLWarning sqlw = SQLWarning.class.cast(t);

                // Add this one including the extra information
                rollUpSqlException(sb, sqlw);

                /**
                 * Process its SQL warning chain.
                 * We start with the next guy, not this one.
                 */
                processSqlWarningChain(sb, sqlw.getNextWarning(), seen);
            }
            else if (t instanceof SQLException) {
                final SQLException sqle = SQLException.class.cast(t);

                // Add this one including the extra information
                rollUpSqlException(sb, sqle);

                /**
                 * Process its SQL exception chain.
                 * We start with the next guy, not this one.
                 */
                processSqlExceptionChain(sb, sqle.getNextException(), seen);
            }
            else {
                rollUpThrowable(sb, t);
            }
        }
    }

    /**
     * Process the SQLException chain.
     *
     * @param sb
     *            String builder
     * @param start
     *            SQL exception
     * @param seen
     *            Set of already processed exceptions
     */
    private static void processSqlExceptionChain(final StringBuilder sb, final SQLException start,
                                                 final Set<Throwable> seen) {
        for (SQLException sqle = start; sqle != null; sqle = sqle.getNextException()) {
            if (seen.contains(sqle)) {
                // If we've seen it we've processed its chains, so bail
                break;
            }

            seen.add(sqle);

            // Add this one including the extra information
            rollUpSqlException(sb, sqle);

            /**
             * Process its cause chain.
             * We start with the next guy, not this one.
             */
            processCauseChain(sb, sqle.getCause(), seen);
        }
    }

    /**
     * Process the SQLWarning chain.
     *
     * @param sb
     *            String builder
     * @param start
     *            SQL warning
     * @param seen
     *            Set of already processed exceptions
     */
    private static void processSqlWarningChain(final StringBuilder sb, final SQLWarning start,
                                               final Set<Throwable> seen) {
        for (SQLWarning sqlw = start; sqlw != null; sqlw = sqlw.getNextWarning()) {
            if (seen.contains(sqlw)) {
                // If we've seen it we've processed its chains, so bail
                break;
            }

            seen.add(sqlw);

            // Add this one including the extra information
            rollUpSqlException(sb, sqlw);

            /**
             * Process its cause chain.
             * We start with the next guy, not this one.
             */
            processCauseChain(sb, sqlw.getCause(), seen);
        }
    }

    /**
     * Get the message and all "caused by" names and messages. This method
     * sets everything up for the inner methods.
     *
     * @param sqle
     *            Initial SQL exception
     *
     * @return String
     */
    public static String rollUpExceptions(final SQLException sqle) {
        final StringBuilder sb = new StringBuilder();

        processCauseChain(sb, sqle, new HashSet<Throwable>());

        return sb.toString();
    }

    /**
     * Log message and all "caused by" names and messages as a warning.
     *
     * This is used when we get a list of warnings from a Wrapper. We do not
     * want to follow the secondary chain because the list represents that
     * chain. However, we will follow the secondary chain of the causes.
     *
     * @param list
     *            List of SQL warnings
     * @param tracer
     *            The Tracer logger
     */
    public static void logWarning(final List<SQLWarning> list, final Tracer tracer) {
        if ((list == null) || list.isEmpty()) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        final Set<Throwable> seen = new HashSet<Throwable>();

        for (final SQLWarning sqlw : list) {
            if (seen.contains(sqlw)) {
                continue;
            }

            seen.add(sqlw);

            // Add this one including the extra information
            rollUpSqlException(sb, sqlw);

            /**
             * Process its cause chain.
             * We start with the next guy, not this one.
             */
            processCauseChain(sb, sqlw.getCause(), seen);
        }

        tracer.warn(sb.toString());
    }

    /**
     * Log message and all "caused by" names and messages as a warning.
     *
     * @param trace
     *            The application context tracer
     * @param sqle
     *            SQL exception
     */
    public static void logWarning(final Tracer trace, final SQLException sqle) {
        if (sqle != null) {
            trace.warn(rollUpExceptions(sqle));
        }
    }

    /**
     * Log message and all "caused by" names and messages as an error.
     *
     * @param trace
     *            The application context tracer
     * @param sqle
     *            SQL exception
     */
    public static void logError(final Tracer trace, final SQLException sqle) {
        if (sqle != null) {
            trace.error(rollUpExceptions(sqle));
        }
    }

    /**
     * Log message and all "caused by" names and messages as debnug.
     *
     * @param trace
     *            the application context tracer
     * @param sqle
     *            SQL exception
     */
    public static void logDebug(final Tracer trace, final SQLException sqle) {
        if (sqle != null) {
            trace.debug(rollUpExceptions(sqle));
        }
    }

    /**
     * Log warnings if any. Don't worry if there is a problem,
     * they're only warnings and we tried.
     *
     * @param trace
     *            The application context tracer
     * @param rs
     *            Result set
     */
    public static void logWarning(final Tracer trace, final ResultSet rs) {
        try {
            final SQLWarning sqlw = rs.getWarnings();

            if (sqlw != null) {
                trace.warn(rollUpExceptions(sqlw));

                rs.clearWarnings();
            }
        }
        catch (final SQLException sqle) {
            // Do nothing
        }
    }

    /**
     * Log warnings if any. Don't worry if there is a problem,
     * they're only warnings and we tried.
     *
     * @param trace
     *            The application context tracer
     * @param conn
     *            Connection
     */
    public static void logWarning(final Tracer trace, final Connection conn) {
        try {
            final SQLWarning sqlw = conn.getWarnings();

            if (sqlw != null) {
                trace.warn(rollUpExceptions(sqlw));

                conn.clearWarnings();
            }
        }
        catch (final SQLException sqle) {
            // Do nothing
        }
    }

    /**
     * Log warnings if any. Don't worry if there is a problem,
     * they're only warnings and we tried.
     *
     * @param trace
     *            The application context Tracer
     * @param stat
     *            Statement
     */
    public static void logWarning(final Tracer trace, final Statement stat) {
        try {
            final SQLWarning sqlw = stat.getWarnings();

            if (sqlw != null) {
                trace.warn(rollUpExceptions(sqlw));

                stat.clearWarnings();
            }
        }
        catch (final SQLException sqle) {
            // do nothing
        }
    }
}
