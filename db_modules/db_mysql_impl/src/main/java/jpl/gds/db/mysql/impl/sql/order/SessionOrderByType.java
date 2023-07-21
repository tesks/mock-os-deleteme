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

import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.ISessionOrderByType;

/**
 * This class is an enumeration of all the possible output orderings for queries
 * done on the TestSession database table.
 *
 */
public class SessionOrderByType extends AbstractOrderByType implements ISessionOrderByType {
    /**
     *  Database field names for each value
     *  (No field, no order-by)
     */
    private static final String[] databaseFieldNames[] = {
    	{"sessionId" },
    	{ "name" },
		{ "fswVersion" },
		{ "sseVersion" },
		{ "startTimeCoarse", "startTimeFine" },
		{ "endTimeCoarse", "endTimeFine" },
		{},
    	{"sessionId" }
    };

    /**
     * Static instances of each value
     */
    /** ID */
    public static final SessionOrderByType ID = new SessionOrderByType(ID_TYPE);

    /** NAME */
    public static final SessionOrderByType NAME = new SessionOrderByType(NAME_TYPE);

    /** FSW_VERSION */
    public static final SessionOrderByType FSW_VERSION = new SessionOrderByType(FSW_VERSION_TYPE);

    /** SSE_VERSION */
    public static final SessionOrderByType SSE_VERSION = new SessionOrderByType(SSE_VERSION_TYPE);

    /** START_TIME */
    public static final SessionOrderByType START_TIME = new SessionOrderByType(START_TIME_TYPE);

    /** END_TIME */
    public static final SessionOrderByType END_TIME = new SessionOrderByType(END_TIME_TYPE);

    /** NONE */
    public static final SessionOrderByType NONE = new SessionOrderByType(NONE_TYPE);

    /** ID_DESC */
    public static final SessionOrderByType ID_DESC = new SessionOrderByType(ID_DESC_TYPE) {
        @Override
        public String getOrderByClause() {
            final String orderBy = super.getOrderByClause();

            /** MPCS-6494 : Removed LIMIT */
            /** MPCS-6494 Add LIMIT */
            return orderBy + " DESC";
        }
    };

    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, SessionOrderByType>() {
        private static final long serialVersionUID = 8026584546364244939L;
        {
            /** Store static instances by Ordinal */
            put(ID_TYPE, ID);
            put(NAME_TYPE, NAME);
            put(FSW_VERSION_TYPE, FSW_VERSION);
            put(SSE_VERSION_TYPE, SSE_VERSION);
            put(START_TIME_TYPE, START_TIME);
            put(END_TIME_TYPE, END_TIME);
            put(NONE_TYPE, NONE);
            put(ID_DESC_TYPE, ID_DESC);

            /** Store static instances by Name */
            put("ID", ID);
            put("NAME", NAME);
            put("FSW_VERSION", FSW_VERSION);
            put("SSE_VERSION", SSE_VERSION);
            put("START_TIME", START_TIME);
            put("END_TIME", END_TIME);
            put("NONE", NONE);
            put("ID_DESC", ID_DESC);
        }
    };

    /**
     * Make the default order for this OrderBype availabable statically
     */
    public static final SessionOrderByType DEFAULT = ID;

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
            throw new IllegalArgumentException("Illegal Session Order Specified: " + orderByTypeOrdinal);
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
            throw new IllegalArgumentException("Illegal Session Order Specified: " + orderByTypeKey);
        }
        return retVal;
    }

    /**
     *
     * Creates an instance of TestSessionOrderByType.
     *
     * @param strVal
     *            The initial value
     */
    public SessionOrderByType(String strVal) {
        super(strVal);
    }

    /**
     * Creates an instance of TestSessionOrderByType.
     *
     * @param intVal
     *            The initial value
     *
     */
    public SessionOrderByType(int intVal) {
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
     * {@inheritDoc}
     */
    @Override
    public String getOrderByClause() {
        final int value = getValueAsInt();
        final String[] fields = databaseFieldNames[value];

        if (fields.length == 0) {
            return ""; // No fields, no order-by
        }

        final StringBuilder sb = new StringBuilder(ORDER_BY_PREFIX);

        boolean first = true;

        for (final String field : fields) {
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }

            final String abbrev = ((value == END_TIME_TYPE) ? ISessionFetch.DB_END_SESSION_TABLE_ABBREV : ISessionFetch.DB_SESSION_TABLE_ABBREV);

            sb.append(abbrev).append('.').append(field);
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
