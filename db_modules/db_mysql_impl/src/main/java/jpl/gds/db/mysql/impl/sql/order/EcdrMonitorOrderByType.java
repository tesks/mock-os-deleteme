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
import jpl.gds.db.api.sql.order.IEcdrMonitorOrderByType;

/**
 * This class is an enumeration of all the possible output orderings for queries
 * done on the MonitorChannelValue database table for ECDR monitor output. See
 * EcdrOrderByType for others.
 *
 * For ECDR monitor there is only one time-type available for ordering, and the
 * actual time is followed by station. Channel-id is added at the end so that
 * the ECDR files are consistent from run to run.
 *
 */
public class EcdrMonitorOrderByType extends AbstractOrderByType implements IEcdrMonitorOrderByType {
    // Integer value constants

    /** String value constants */
    @SuppressWarnings({ "MS_MUTABLE_ARRAY", "MS_PKGPROTECT" })
    public static final String orderByTypes[] = { "ERT" };

    // Database field names for each value
    private static final String databaseFieldNames[] = { "ertCoarse_mstCoarse,ertFine_mstFine,dssId,channelId" };

    /**
     * Static instances of each value
     */
    /** ERT */
    public static final EcdrMonitorOrderByType ERT = new EcdrMonitorOrderByType(ERT_TYPE);

    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, EcdrMonitorOrderByType>() {
        private static final long serialVersionUID = 8026584546364244939L;
        {
            /** Store static instances by Ordinal */
            put(ERT_TYPE, ERT);

            /** Store static instances by Name */
            put("ERT", ERT);
        }
    };

    /**
     * Make the default order for this OrderBype availabable statically
     */
    public static final EcdrMonitorOrderByType DEFAULT = ERT;

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
    public static <T extends IDbOrderByType> T getOrderBy(final int orderByTypeOrdinal) {
        @java.lang.SuppressWarnings("unchecked")
        final
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
     *            the Name of the OrderByType (key)
     * 
     * @return the specified OrderBy
     */
    public static <T extends IDbOrderByType> T getOrderBy(final String orderByTypeKey) {
        @java.lang.SuppressWarnings("unchecked")
        final
        T retVal = (T) orderByMap.get(orderByTypeKey);
        if (null == retVal) {
            throw new IllegalArgumentException("Illegal Frame Order Specified: " + orderByTypeKey);
        }
        return retVal;
    }

    /**
     * Creates an instance of EcdrMonitorOrderByType.
     *
     * @param strVal
     *            The initial value
     */
    public EcdrMonitorOrderByType(final String strVal) {
        super(strVal);
    }

    /**
     * Creates an instance of EcdrMonitorOrderByType.
     *
     * @param intVal
     *            The initial value
     */
    public EcdrMonitorOrderByType(final int intVal) {
        super(intVal);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
     */
    @Override
    protected String getStringValue(final int index) {
        return orderByTypes[index];
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

        if (fields.isEmpty()) {
            return ""; // No fields, no order-by
        }

        final StringBuilder result = new StringBuilder(ORDER_BY_PREFIX);
        boolean first = true;

        for (final String column : fields.split(",")) {
            if (first) {
                first = false;
            }
            else {
                result.append(',');
            }

            result.append(column);
        }

        return result.toString();
    }

    /**
     * Get order-by clause, but allow for it to be ascending or descending.
     *
     * @param descending
     *            True if order-by should be descending
     *
     * @return Order-by clause
     */
    public String getOrderByClause(final boolean descending) {
        final String fields = databaseFieldNames[getValueAsInt()];

        if (fields.isEmpty()) {
            return ""; // No fields, no order-by
        }

        final StringBuilder result = new StringBuilder(ORDER_BY_PREFIX);
        boolean first = true;

        for (final String column : fields.split(",")) {
            if (first) {
                first = false;
            }
            else {
                result.append(',');
            }

            result.append(column);

            if (descending) {
                result.append(" DESC");
            }
        }

        return result.toString();
    }

    /**
     * Return an SQL "order by" clause based on the value of this enumerated
     * type.
     *
     * @return Array of column names
     */
    @Override
    public String[] getOrderByColumns() {
        final String fields = databaseFieldNames[getValueAsInt()];

        if (fields.isEmpty()) {
            return new String[0];
        }

        return fields.split(",");
    }
}
