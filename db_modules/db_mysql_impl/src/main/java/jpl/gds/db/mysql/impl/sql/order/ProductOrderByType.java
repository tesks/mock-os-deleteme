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
import jpl.gds.db.api.sql.fetch.IDbProductFetch;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IProductOrderByType;

/**
 * This class is an enumeration of all the possible output orderings for queries
 * done on the Product database table.
 *
 */
public class ProductOrderByType extends AbstractOrderByType implements IProductOrderByType {
    // Integer value constants

    /** String value constants */
    @SuppressWarnings({ "MS_MUTABLE_ARRAY", "MS_PKGPROTECT" })
    public static final String orderByTypes[] = { "SessionID", "CreationTime", "DVTSCET", "APID", "RequestId",
            "DVTSCLK", "ERT", "DVTLST", "None", "RCT" };

    // Database field names for each value
    private static final String databaseFieldNames[] = { "SessionId", "creationTimeCoarse,creationTimeFine",
            "dvtScetCoarse,dvtScetFine", "apid", "requestId", "dvtSclkCoarse,dvtSclkFine", "ertCoarse,ertFine",
            "dvtSclkCoarse,dvtSclkFine", "", // No fields, no order-by
            "rctCoarse,rctFine" };

    /**
     * Static instances of each value
     */
    /** TEST_SESSION_ID */
    public static final ProductOrderByType TEST_SESSION_ID = new ProductOrderByType(TEST_SESSION_ID_TYPE);

    /** CREATION_TIME */
    public static final ProductOrderByType CREATION_TIME = new ProductOrderByType(CREATION_TIME_TYPE);

    /** DVT_SCET */
    public static final ProductOrderByType DVT_SCET = new ProductOrderByType(DVT_SCET_TYPE);

    /** APID */
    public static final ProductOrderByType APID = new ProductOrderByType(APID_TYPE);

    /** REQUESTID */
    public static final ProductOrderByType REQUEST_ID = new ProductOrderByType(REQUEST_ID_TYPE);

    /** DVT_SCLK */
    public static final ProductOrderByType DVT_SCLK = new ProductOrderByType(DVT_SCLK_TYPE);

    /** ERT */
    public static final ProductOrderByType ERT = new ProductOrderByType(ERT_TYPE);

    /** DVT_LST */
    public static final ProductOrderByType DVT_LST = new ProductOrderByType(DVT_LST_TYPE);

    /** NONE */
    public static final ProductOrderByType NONE = new ProductOrderByType(NONE_TYPE);

    /** RCT */
    public static final ProductOrderByType RCT = new ProductOrderByType(RCT_TYPE);

    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, ProductOrderByType>() {
        private static final long serialVersionUID = 8026584546364244939L;
        {
            /** Store static instances by Ordinal */
            put(TEST_SESSION_ID_TYPE, TEST_SESSION_ID);
            put(CREATION_TIME_TYPE, CREATION_TIME);
            put(DVT_SCET_TYPE, DVT_SCET);
            put(APID_TYPE, APID);
            put(REQUEST_ID_TYPE, REQUEST_ID);
            put(DVT_SCLK_TYPE, DVT_SCLK);
            put(ERT_TYPE, ERT);
            put(DVT_LST_TYPE, DVT_LST);
            put(NONE_TYPE, NONE);
            put(RCT_TYPE, RCT);

            /** Store static instances by Name */
            put("TEST_SESSION_ID", TEST_SESSION_ID);
            put("CREATION_TIME", CREATION_TIME);
            put("DVT_SCET", DVT_SCET);
            put("APID", APID);
            put("REQUEST_ID", REQUEST_ID);
            put("DVT_SCLK", DVT_SCLK);
            put("ERT", ERT);
            put("DVT_LST", DVT_LST);
            put("NONE", NONE);
            put("RCT", RCT);
        }
    };

    /**
     * Make the default order for this OrderBype availabable statically
     */
    public static final ProductOrderByType DEFAULT = DVT_SCET;

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
     * Creates an instance of ProductOrderByType.
     * 
     * @param strVal
     *            The initial value
     */
    public ProductOrderByType(String strVal) {
        super(strVal);
    }

    /**
     * 
     * Creates an instance of ProductOrderByType.
     * 
     * @param intVal
     *            The initial value
     */
    public ProductOrderByType(int intVal) {
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
        final String fields = databaseFieldNames[getValueAsInt()];

        if (fields.length() == 0) {
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

            result.append(IDbProductFetch.DB_PRODUCT_TABLE_ABBREV);
            result.append('.').append(column);
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
