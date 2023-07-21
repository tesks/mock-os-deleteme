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
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IEvrOrderByType;

/**
 * This class is an enumeration of all the possible output orderings for queries
 * done on the EVR database table.
 *
 * Note that the final ordering supports the whole primary key. That is
 * necessary for the fetch logic to properly associate the Evr metadata to the
 * Evr.
 *
 */
public class EvrOrderByType extends AbstractOrderByType implements IEvrOrderByType {
    // Integer value constants

    /** String value constants */
    @SuppressWarnings({ "MS_MUTABLE_ARRAY", "MS_PKGPROTECT" })
    public static final String orderByTypes[] = { "SessionID", "EventID", "ERT", "SCET", "RCT", "SCLK", "Level",
            "Module", "Name", "LST", "None" };

    /**
     * Database field names for each value.
     *
     * The base order is necessary to enable EVR fetch to group the metadata for
     * each EVR.
     *
     * We cannot have a true NONE because we must have BASE_ORDER at least.
     *
     * BASE_ORDER must be last in the list. id muat be last in BASE_ORDER.
     *
     * NB: The DB cannot use any EVR index for sorting because we have more
     * columns in the order-by than in the keys.
     *
     * MPCS-5990  Redo with BASE_ORDER with sessionFragment
     */

    private static final String BASE_ORDER = "sessionId,hostId,fromSse,sessionFragment,id";

    private static final String databaseFieldNames[] = { BASE_ORDER, "eventId," + BASE_ORDER,
            "ertCoarse,ertFine," + BASE_ORDER, "sclkCoarse,sclkFine," + BASE_ORDER, "rctCoarse,rctFine," + BASE_ORDER,
            "sclkCoarse,sclkFine," + BASE_ORDER, "level," + BASE_ORDER, "module," + BASE_ORDER, "name," + BASE_ORDER,
            "sclkCoarse,sclkFine," + BASE_ORDER, BASE_ORDER };

    /**
     * Static instances of each value
     */
    /** TEST_SESSION_ID */
    public static final EvrOrderByType TEST_SESSION_ID = new EvrOrderByType(TEST_SESSION_ID_TYPE);

    /** EVENT_ID */
    public static final EvrOrderByType EVENT_ID = new EvrOrderByType(EVENT_ID_TYPE);

    /** ERT */
    public static final EvrOrderByType ERT = new EvrOrderByType(ERT_TYPE);

    /** SCET */
    public static final EvrOrderByType SCET = new EvrOrderByType(SCET_TYPE);

    /** RCT */
    public static final EvrOrderByType RCT = new EvrOrderByType(RCT_TYPE);

    /** SCLK */
    public static final EvrOrderByType SCLK = new EvrOrderByType(SCLK_TYPE);

    /** LEVEL */
    public static final EvrOrderByType LEVEL = new EvrOrderByType(LEVEL_TYPE);

    /** MODULE */
    public static final EvrOrderByType MODULE = new EvrOrderByType(MODULE_TYPE);

    /** NAME */
    public static final EvrOrderByType NAME = new EvrOrderByType(NAME_TYPE);

    /** LST */
    public static final EvrOrderByType LST = new EvrOrderByType(LST_TYPE);

    /** NONE */
    public static final EvrOrderByType NONE = new EvrOrderByType(NONE_TYPE);

    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, EvrOrderByType>() {
        private static final long serialVersionUID = 8026584546364244939L;
        {
            /** Store static instances by Ordinal */
            put(TEST_SESSION_ID_TYPE, TEST_SESSION_ID);
            put(EVENT_ID_TYPE, EVENT_ID);
            put(ERT_TYPE, ERT);
            put(SCET_TYPE, SCET);
            put(RCT_TYPE, RCT);
            put(SCLK_TYPE, SCLK);
            put(LEVEL_TYPE, LEVEL);
            put(MODULE_TYPE, MODULE);
            put(NAME_TYPE, NAME);
            put(LST_TYPE, LST);
            put(NONE_TYPE, RCT);

            /** Store static instances by Name */
            put("TEST_SESSION_ID", TEST_SESSION_ID);
            put("EVENT_ID", EVENT_ID);
            put("ERT", ERT);
            put("SCET", SCET);
            put("RCT", RCT);
            put("SCLK", SCLK);
            put("LEVEL", LEVEL);
            put("MODULE", MODULE);
            put("NAME", NAME);
            put("LST", LST);
            put("NONE", NONE);
        }
    };

    /**
     * Make the default order for this OrderBype availabable statically
     */
    public static final EvrOrderByType DEFAULT = ERT;

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
     * @param orderByTypeOrdinal
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
     * Creates an instance of EvrOrderByType.
     *
     * @param strVal
     *            The initial value
     */
    public EvrOrderByType(final String strVal) {
        super(strVal);
    }

    /**
     *
     * Creates an instance of EvrOrderByType.
     *
     * @param intVal
     *            The initial value
     */
    public EvrOrderByType(final int intVal) {
        super(intVal);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
     */
    @Override
    protected String getStringValue(final int index) {
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
        final String fields = databaseFieldNames[getValueAsInt()];
        final StringBuilder result = new StringBuilder(ORDER_BY_PREFIX);
        boolean first = true;

        for (final String column : fields.split(",")) {
            if (first) {
                first = false;
            }
            else {
                result.append(',');
            }

            // XXX
            // result.append(IEvrFetch.tableAbbrev).append('.').append(column);
            result.append(column);
        }

        return result.toString();
    }

    /**
     * Return an SQL "order by" clause based on the value of this enumerated
     * type.
     * 
     * @return Array of column names
     */
    public String[] getOrderByColumns() {
        final String fields = databaseFieldNames[getValueAsInt()];

        if (fields.isEmpty()) {
            return new String[0];
        }

        return fields.split(",");
    }
}
