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
import jpl.gds.db.api.sql.fetch.IFrameFetch;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IFrameOrderByType;

/**
 * This class is an enumeration of all the possible output orderings for
 * queries done on the Frame database table.
 *
 * ID is for use by remoting. It assumes a single hostId and sessionId.
 *
 * @version MPCS-6808  Add RCT
 */
public class FrameOrderByType extends AbstractOrderByType implements IFrameOrderByType
{
	// Integer value constants

    /** String value constants */
	@SuppressWarnings({"MS_MUTABLE_ARRAY","MS_PKGPROTECT"})
	public static final String orderByTypes[] =
	{ 
		"SessionID",
	    "FrameType", 
		"ERT",
		"RelaySpacecraftId",
		"VCID",
		"VCFC",
		"DSS",
        "None",
        "Id",
        "RCT"
	};
    
	// Database field names for each value
	private static final String databaseFieldNames[] =
	{ 
		"sessionId", 
		"type", 
		"ertCoarse,ertFine,id,vcid,vcfc", // Last two needed for frame gaps
		"relaySpacecraftId",
		"vcid",
		"vcfc",
		"dssId",
        "", // No fields, no order-by
        "id",
        "rctCoarse,rctFine,id"
	};
    
	/**
	 *  Static instances of each value
	 */
    /** TEST_SESSION_ID */
	public static final FrameOrderByType TEST_SESSION_ID = new FrameOrderByType(TEST_SESSION_ID_TYPE);

    /** FRAME_TYPE */
    public static final FrameOrderByType FRAME = new FrameOrderByType(FRAME_TYPE);

    /** ERT */
    public static final FrameOrderByType ERT = new FrameOrderByType(ERT_TYPE);

    /** RELAY_SCID */
    public static final FrameOrderByType RELAY_SCID = new FrameOrderByType(RELAY_SCID_TYPE);

    /** VCID */
    public static final FrameOrderByType VCID = new FrameOrderByType(VCID_TYPE);

    /** VCFC */
    public static final FrameOrderByType VCFC = new FrameOrderByType(VCFC_TYPE);

    /** DSS */
    public static final FrameOrderByType DSS = new FrameOrderByType(DSS_TYPE);

    /** NONE */
    public static final FrameOrderByType NONE = new FrameOrderByType(NONE_TYPE);

    /** ID */
    public static final FrameOrderByType ID = new FrameOrderByType(ID_TYPE);

    /** RCT */
    public static final FrameOrderByType RCT = new FrameOrderByType(RCT_TYPE);

	private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, FrameOrderByType>() {
		private static final long serialVersionUID = -993043658850052956L;
	{
		/** Store static instances by Ordinal */
		put(TEST_SESSION_ID_TYPE, TEST_SESSION_ID);
		put(FRAME_TYPE, FRAME);
		put(ERT_TYPE, ERT);
		put(RELAY_SCID_TYPE, RELAY_SCID);
		put(VCID_TYPE, VCID);
		put(VCFC_TYPE, VCFC);
		put(DSS_TYPE, DSS);
		put(NONE_TYPE, NONE);
		put(ID_TYPE, ID);
		put(RCT_TYPE, RCT);

		/** Store static instances by Name */
		put("TEST_SESSION_ID", TEST_SESSION_ID);
		put("FRAME", FRAME);
		put("ERT", ERT);
		put("RELAY_SCID", RELAY_SCID);
		put("VCID", VCID);
		put("VCFC", VCFC);
		put("DSS", DSS);
		put("NONE", NONE);
		put("ID", ID);
		put("RCT", RCT);
	}};

	/**
	 * Make the default order for this OrderBype availabable statically
	 */
	public static final FrameOrderByType DEFAULT = ERT;

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
     * Creates an instance of FrameOrderByType.
     * 
     * @param strVal The initial value
     */
    public FrameOrderByType(String strVal)
	{
		super(strVal);
	}
	
    /**
     * 
     * Creates an instance of FrameOrderByType.
     * 
     * @param intVal The initial value
     */
	public FrameOrderByType(int intVal)
	{
		super(intVal);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
	 */
	@Override
	protected String getStringValue(int index)
	{
		if(index < 0 || index > getMaxIndex())
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		
		return(orderByTypes[index]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getMaxIndex()
	 */
	@Override
	protected int getMaxIndex()
	{
		return(orderByTypes.length-1);
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    public String getOrderByClause()
    {
        final String fields = databaseFieldNames[getValueAsInt()];

        if (fields.length() == 0)
        {
            return ""; // No fields, no order-by
        }

        final StringBuilder result = new StringBuilder(ORDER_BY_PREFIX);
        boolean             first  = true;

        for (final String column : fields.split(","))
        {
            if (first)
            {
                first = false;
            }
            else
            {
                result.append(',');
            }

            result.append(IFrameFetch.DB_FRAME_TABLE_ABBREV);
            result.append('.').append(column);
        }

        return result.toString();
    }

	
	/**
	 * Return an SQL "order by" clause based on the value
	 * of this enumerated type.
	 * 
	 * @return Array of column names
	 */
	public String[] getOrderByColumns()
    {
        final String fields = databaseFieldNames[getValueAsInt()];

        if (fields.isEmpty())
        {
            return new String[0];
        }

        return fields.split(",");
    }
}
