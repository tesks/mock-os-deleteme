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

package jpl.gds.db.mysql.impl.sql.order;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.db.api.sql.fetch.ILogFetch;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.ILogOrderByType;

/**
 * This class is an enumeration of all the possible output orderings for queries
 * done on the LogMessage database table.
 *
 */
public class LogOrderByType extends AbstractOrderByType implements ILogOrderByType {
    // Integer value constants

    /** String value constants */
    @SuppressWarnings({ "MS_MUTABLE_ARRAY", "MS_PKGPROTECT" })
    public static final String orderByTypes[] = { "SessionID", "EventTime", "Classification", "MessageType", "None",
            "RCT", "EventTimeDesc" };

    /**
     * Database field names for each value. Ordered by the int values
     * corresponding to orderByTypes. Each one is an array of the column names
     * to be used for that order-by (may be empty.)
     */
    private static final String[] databaseFieldNames[] = {
            /** SessionId columns */
            { "sessionId" },

            /** EventTime columns */
            { "eventTimeCoarse", "eventTimeFine" },

            /** Classification columns */
            { "classification" },

            /** MessageType columns */
            { "type" },

            /** "None" columns */
            {}, // No field, no order-by

            // MPCS-10869 - missing this entry. adding it is part of the fix for chill_get_logs --timeType RCT
            /** RCT columns */
            { "rctCoarse", "rctFine" },

            // MPCS-11890 - adding for event time descending
            { "eventTimeCoarse", "eventTimeFine"}
    };

    /**
     * Marks the database fields that must be cast to char. Those are the ENUMs.
     * We want the order-by to use the string values, not the int ENUM values.
     *
     * Ordered by the int values corresponding to orderByTypes. Each one is an
     * array of booleans (may be empty.)
     *
     * MPCS-7362  New
     */
    private static final boolean[] databaseFieldCasts[] = {
            /** SessionId columns */
            { false },

            /** EventTime columns */
            { false, false },

            /** Classification columns */
            { true }, /** LogMessage.classification */

            /** MessageType columns */
            { false },

            /** "None" columns */
            {}, // No field, no order-by

            // MPCS-10869  - missing this entry. adding it is part of the fix for chill_get_logs --timeType RCT
            /** RCT columns */
            { false, false },

            // MPCS-11890 - added for event time desc
            { false, false}
    };

    /**
     * Static instances of each value
     */
    /** TEST_SESSION_ID */
    public static final LogOrderByType TEST_SESSION_ID = new LogOrderByType(TEST_SESSION_ID_TYPE);

    /** EVENT_TIME */
    public static final LogOrderByType EVENT_TIME = new LogOrderByType(EVENT_TIME_TYPE);

    /** CLASSIFICATION */
    public static final LogOrderByType CLASSIFICATION = new LogOrderByType(CLASSIFICATION_TYPE);

    /** MESSAGE_TYPE */
    public static final LogOrderByType MESSAGE_TYPE = new LogOrderByType(MESSAGE_TYPE_TYPE);

    /** NONE */
    public static final LogOrderByType NONE = new LogOrderByType(NONE_TYPE);

    /** RCT */
    public static final LogOrderByType RCT = new LogOrderByType(RCT_TYPE);

    /** ID_DESC */
    public static final LogOrderByType EVENT_TIME_DESC = new LogOrderByType(EVENT_TIME_DESC_TYPE) {
        @Override
        public String getOrderByClause() {

            final String[] fields = databaseFieldNames[getValueAsInt()];

            if (fields.length == 0) {
                return ""; // No field, no order-by
            }

            final boolean[] casts = databaseFieldCasts[getValueAsInt()];
            final StringBuilder sb = new StringBuilder(ORDER_BY_PREFIX);
            boolean first = true;
            int i = 0;

            for (final String field : fields) {
                if (first) {
                    first = false;
                }
                else {
                    sb.append(',');
                }

                sb.append(ILogFetch.DB_LOG_MESSAGE_TABLE_ABBREV).append('.').append(field);

                sb.append(" DESC");

                ++i;
            }

            return sb.toString();
        }
    };

    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, LogOrderByType>() {
        private static final long serialVersionUID = 8026584546364244939L;
        {
            /** Store static instances by Ordinal */
            put(TEST_SESSION_ID_TYPE, TEST_SESSION_ID);
            put(EVENT_TIME_TYPE, EVENT_TIME);
            put(CLASSIFICATION_TYPE, CLASSIFICATION);
            put(MESSAGE_TYPE_TYPE, MESSAGE_TYPE);
            put(NONE_TYPE, NONE);
            put(RCT_TYPE, RCT);
            put(EVENT_TIME_DESC_TYPE, EVENT_TIME_DESC);

            /** Store static instances by Name */
            put("TEST_SESSION_ID", TEST_SESSION_ID);
            put("EVENT_TIME", EVENT_TIME);
            put("CLASSIFICATION", CLASSIFICATION);
            put("MESSAGE_TYPE", MESSAGE_TYPE);
            put("NONE", NONE);
            put("RCT", RCT);
            put("EVENT_TIME_DESC", EVENT_TIME_DESC);
        }
    };

    /**
     * Make the default order for this OrderBype availabable statically
     */
    public static final LogOrderByType DEFAULT = EVENT_TIME;

    /**
     * Get the static instance of the default OrderBy for this type
     * 
     * @return the Default OrderBy
     */
    @java.lang.SuppressWarnings("unchecked")
    public static <T extends IDbOrderByType> T getOrderBy() {
        return (T) DEFAULT;
    }

    /**
     * Get the static instance of the order by type by ordinal
     * 
     * @param orderByTypeOrdinal
     *            The OrderBy Type Name
     * @return the specified OrderBy
     */
    public static <T extends IDbOrderByType> T getOrderBy(int orderByTypeOrdinal) {
        @java.lang.SuppressWarnings("unchecked")
        T retVal = (T) orderByMap.get(orderByTypeOrdinal);
        if (null == retVal) {
            throw new IllegalArgumentException("Illegal Frame Order Specified: " + orderByTypeOrdinal);
        }
        return retVal;
    }

    /**
     * Get the static instance of the order by type by String
     * 
     * @param orderByTypeKey
     *            The OrderBy Type Name
     * @return the specified OrderBy
     */
    public static <T extends IDbOrderByType> T getOrderBy(String orderByTypeKey) {
        @java.lang.SuppressWarnings("unchecked")
        T retVal = (T) orderByMap.get(orderByTypeKey);
        if (null == retVal) {
            throw new IllegalArgumentException("Illegal Frame Order Specified: " + orderByTypeKey);
        }
        return retVal;
    }

    /**
     * 
     * Creates an instance of LogMessageOrderByType.
     * 
     * @param strVal
     *            The initial value
     */
    public LogOrderByType(String strVal) {
        super(strVal);
    }

    /**
     * 
     * Creates an instance of LogMessageOrderByType.
     * 
     * @param intVal
     *            The initial value
     */
    public LogOrderByType(int intVal) {
        super(intVal);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
     */
    @Override
    protected String getStringValue(int index) {
        if (index < 0 || index > getMaxIndex()) {
            throw new ArrayIndexOutOfBoundsException();
        }

        return (orderByTypes[index]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.types.EnumeratedType#getMaxIndex()
     */
    @Override
    protected int getMaxIndex() {
        return (orderByTypes.length - 1);
    }

    /**
     * Get default value.
     *
     * @return Default value
     */
    public static LogOrderByType getDefaultValue() {
        return (EVENT_TIME);
    }

    /**
     * {@inheritDoc}
     *
     * @version MPCS-7362  Add casts
     */
    @Override
    public String getOrderByClause() {
        final String[] fields = databaseFieldNames[getValueAsInt()];

        if (fields.length == 0) {
            return ""; // No field, no order-by
        }

        final boolean[] casts = databaseFieldCasts[getValueAsInt()];
        final StringBuilder sb = new StringBuilder(ORDER_BY_PREFIX);
        boolean first = true;
        int i = 0;

        for (final String field : fields) {
            if (first) {
                first = false;
            }
            else {
                sb.append(',');
            }

            if (casts[i]) {
                sb.append("CAST(");
            }

            sb.append(ILogFetch.DB_LOG_MESSAGE_TABLE_ABBREV).append('.').append(field);

            if (casts[i]) {
                sb.append(" AS CHAR)");
            }

            ++i;
        }

        return sb.toString();
    }

    /**
     * Return an SQL "order by" clause based on the value of this enumerated
     * type.
     * 
     * @return Array of column names
     */
    public String[] getOrderByColumns() {
        return databaseFieldNames[getValueAsInt()];
    }
}
