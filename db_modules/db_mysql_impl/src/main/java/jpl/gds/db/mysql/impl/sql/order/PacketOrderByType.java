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
import jpl.gds.db.api.sql.order.IPacketOrderByType;

/**
 * This class is an enumeration of all the possible output orderings for queries
 * done on the Packet database table.
 *
 * ID is for use by remoting. It assumes a single hostId and sessionId.
 *
 */
public class PacketOrderByType extends AbstractOrderByType implements IPacketOrderByType {
    // Integer value constants

    /** String value constants */
    @SuppressWarnings({ "MS_MUTABLE_ARRAY", "MS_PKGPROTECT" })
    public static final String orderByTypes[] = { "SessionID", "RCT", "SCET", "ERT", "SCLK", "APID", "SPSC", "VCID",
            "LST", "None", "Id" };

    // Database fields name for each value
    // (vcid,apid,spsc needed for gap detection, harmless otherwise)
    private static final String databaseFieldNames[] = { "sessionId", "rctCoarse,rctFine,id,vcid,apid,spsc",
            "scetCoarse,scetFine,id,vcid,apid,spsc", "ertCoarse,ertFine,id,vcid,apid,spsc",
            "sclkCoarse,sclkFine,id,vcid,apid,spsc", "apid", "spsc", "vcid", "sclkCoarse,sclkFine,id,vcid,apid,spsc", "" // No
                                                                                                                         // fields,
                                                                                                                         // no
                                                                                                                         // order-by
            , "id" };

    /**
     * Static instances of each value
     */
    /** TEST_SESSION_ID */
    public static final PacketOrderByType TEST_SESSION_ID = new PacketOrderByType(TEST_SESSION_ID_TYPE);

    /** RCT */
    public static final PacketOrderByType RCT = new PacketOrderByType(RCT_TYPE);

    /** SCET */
    public static final PacketOrderByType SCET = new PacketOrderByType(SCET_TYPE);

    /** ERT */
    public static final PacketOrderByType ERT = new PacketOrderByType(ERT_TYPE);

    /** SCLK */
    public static final PacketOrderByType SCLK = new PacketOrderByType(SCLK_TYPE);

    /** APID */
    public static final PacketOrderByType APID = new PacketOrderByType(APID_TYPE);

    /** SPSC */
    public static final PacketOrderByType SPSC = new PacketOrderByType(SPSC_TYPE);

    /** VCID */
    public static final PacketOrderByType VCID = new PacketOrderByType(VCID_TYPE);

    /** LST */
    public static final PacketOrderByType LST = new PacketOrderByType(LST_TYPE);

    /** NONE */
    public static final PacketOrderByType NONE = new PacketOrderByType(NONE_TYPE);

    /** ID */
    public static final PacketOrderByType ID = new PacketOrderByType(ID_TYPE);

    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, PacketOrderByType>() {
        private static final long serialVersionUID = -2139348929073273078L;
        {
            /** Store static instances by Ordinal */
            put(TEST_SESSION_ID_TYPE, TEST_SESSION_ID);
            put(RCT_TYPE, RCT);
            put(SCET_TYPE, SCET);
            put(ERT_TYPE, ERT);
            put(SCLK_TYPE, SCLK);
            put(APID_TYPE, APID);
            put(SPSC_TYPE, SPSC);
            put(VCID_TYPE, VCID);
            put(LST_TYPE, LST);
            put(NONE_TYPE, NONE);
            put(ID_TYPE, ID);

            /** Store static instances by Name */
            put("TEST_SESSION_ID", TEST_SESSION_ID);
            put("RCT", RCT);
            put("SCET", SCET);
            put("ERT", ERT);
            put("SCLK", SCLK);
            put("APID", APID);
            put("SPSC", SPSC);
            put("VCID", VCID);
            put("LST", LST);
            put("NONE", NONE);
            put("ID", ID);
        }
    };

    /**
     * Make the default order for this OrderBype availabable statically
     */
    public static final PacketOrderByType DEFAULT = ERT;

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
     * Creates an instance of PacketOrderByType.
     * 
     * @param strVal
     *            The initial value
     */
    public PacketOrderByType(String strVal) {
        super(strVal);
    }

    /**
     * 
     * Creates an instance of PacketOrderByType.
     * 
     * @param intVal
     *            The initial value
     */
    public PacketOrderByType(int intVal) {
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

            // result.append(PacketFetch.tableAbbrev).append('.');
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
