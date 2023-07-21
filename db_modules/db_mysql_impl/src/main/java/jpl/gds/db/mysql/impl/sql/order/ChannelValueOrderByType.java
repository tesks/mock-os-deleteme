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
import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.api.sql.order.IDbOrderByType;


/**
 * This class is an enumeration of all the possible output orderings for
 * queries done on the ChannelValue database table.
 *
 * RCT will not have an index so it can have as many columns as you want.
 *
 * @version MPCS-4768 Split time types into plain and extended
 * @version MPCS-6808 Add RCT
 */
public class ChannelValueOrderByType extends AbstractOrderByType implements IChannelValueOrderByType
{
	// Integer value constants

    /** String value constants */
	@SuppressWarnings({"MS_MUTABLE_ARRAY","MS_PKGPROTECT"})
	public static final String orderByTypes[] =
	{ 
		"SessionID",
		"ChannelIndex",
	    "ChannelId", 
		"ChannelType",
        "Module",
		"SCLK",
		"ERT",
		"SCET",
		"LST",
		"None",
        "HostId",
        "VCID",
        "Station",
        "APID",
        "SPSC",
        "PacketRCT",
		"SCLK_EXT",
		"ERT_EXT",
		"SCET_EXT",
		"LST_EXT",
        "RCT"
	};

	/**
     * Database field names for each value. Ordered by the int values
     * corresponding to orderByTypes. Each one is an array of the column
     * names to be used for that order-by (may be empty.)
     *
     * Some database fields must be cast to char.
     * Those are the ENUMs. We want the order-by to use the
     * string values, not the int ENUM values.
     */
	private static final String databaseFieldNames[] =
	{ 
		"sessionId,hostId", 
	    "channelIndex,hostId,sessionId", 
		"channelId,hostId,sessionId",

        /** MPCS-7362  Add CAST ChannelData.type*/
        "CAST(type AS CHAR),hostId,sessionId",

        "module,hostId,sessionId",

		"sclkCoarse,sclkFine",
		"ertCoarse_mstCoarse,ertFine_mstFine",
		"scetCoarse,scetFine",
		"sclkCoarse,sclkFine",

        "", // Indicates no fields and hence no sort
        "hostId,sessionId,sessionFragment", // Last for JUnit
        "vcid,hostId,sessionId",
        "dssId,hostId,sessionId",
        "apid,hostId,sessionId",
        "spsc,hostId,sessionId",
        "packetRctCoarse,packetRctFine,hostId,sessionId",

		"sclkCoarse,sclkFine,hostId,sessionId",
		"ertCoarse_mstCoarse,ertFine_mstFine,hostId,sessionId",
		"scetCoarse,scetFine,hostId,sessionId",
		"sclkCoarse,sclkFine,hostId,sessionId",

        "rctCoarse,rctFine,hostId,sessionId"
	};
    
	// Packet dependency
	private static final boolean[] packetRequired =
	{
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        true,
        true,
        true,
        false,
        false,
        false,
        false,
        false
	};
    
	/**
	 *  Static instances of each value
	 */
    /** TEST_SESSION_ID */
	public static final ChannelValueOrderByType TEST_SESSION_ID = new ChannelValueOrderByType(TEST_SESSION_ID_TYPE);

    /** CHANNEL_INDEX */
    public static final ChannelValueOrderByType CHANNEL_INDEX = new ChannelValueOrderByType(CHANNEL_INDEX_TYPE);

    /** CHANNEL_ID */
    public static final ChannelValueOrderByType CHANNEL_ID = new ChannelValueOrderByType(CHANNEL_ID_TYPE);

    /** CHANNEL_TYPE */
    public static final ChannelValueOrderByType CHANNEL_TYPE = new ChannelValueOrderByType(CHANNEL_TYPE_TYPE);

    /** MODULE */
    public static final ChannelValueOrderByType MODULE = new ChannelValueOrderByType(MODULE_TYPE);

    /** SCLK */
    public static final ChannelValueOrderByType SCLK = new ChannelValueOrderByType(SCLK_TYPE);

    /** ERT */
    public static final ChannelValueOrderByType ERT = new ChannelValueOrderByType(ERT_TYPE);

    /** SCET */
    public static final ChannelValueOrderByType SCET = new ChannelValueOrderByType(SCET_TYPE);

    /** LST */
    public static final ChannelValueOrderByType LST = new ChannelValueOrderByType(LST_TYPE);

    /** NONE */
    public static final ChannelValueOrderByType NONE = new ChannelValueOrderByType(NONE_TYPE);

    /** HOST_ID */
    public static final ChannelValueOrderByType HOST_ID = new ChannelValueOrderByType(HOST_ID_TYPE);

    /** VCID */
    public static final ChannelValueOrderByType VCID = new ChannelValueOrderByType(VCID_TYPE);

    /** STATION */
    public static final ChannelValueOrderByType STATION = new ChannelValueOrderByType(STATION_TYPE);

    /** APID */
    public static final ChannelValueOrderByType APID = new ChannelValueOrderByType(APID_TYPE);

    /** SPSC */
    public static final ChannelValueOrderByType SPSC = new ChannelValueOrderByType(SPSC_TYPE);

    /** PACKET_RCT */
    public static final ChannelValueOrderByType PACKET_RCT = new ChannelValueOrderByType(PACKET_RCT_TYPE);

    /** SCLK_EXT */
    public static final ChannelValueOrderByType SCLK_EXT = new ChannelValueOrderByType(SCLK_EXT_TYPE);

    /** ERT_EXT */
    public static final ChannelValueOrderByType ERT_EXT = new ChannelValueOrderByType(ERT_EXT_TYPE);

    /** SCET_EXT */
    public static final ChannelValueOrderByType SCET_EXT = new ChannelValueOrderByType(SCET_EXT_TYPE);

    /** LST_EXT */
    public static final ChannelValueOrderByType LST_EXT = new ChannelValueOrderByType(LST_EXT_TYPE);

    /** RCT */
    public static final ChannelValueOrderByType RCT = new ChannelValueOrderByType(RCT_TYPE);

    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, ChannelValueOrderByType>() {
		private static final long serialVersionUID = 8026584546364244939L;
	{
		/** Store static instances by Ordinal */
		put(TEST_SESSION_ID_TYPE, TEST_SESSION_ID);
		put(CHANNEL_INDEX_TYPE, CHANNEL_INDEX);
		put(CHANNEL_ID_TYPE, CHANNEL_ID);
		put(CHANNEL_TYPE_TYPE, CHANNEL_TYPE);
		put(MODULE_TYPE, MODULE);
		put(SCLK_TYPE, SCLK);
		put(ERT_TYPE, ERT);
		put(SCET_TYPE, SCET);
		put(LST_TYPE, LST);
		put(NONE_TYPE, NONE);
		put(HOST_ID_TYPE, HOST_ID);
		put(VCID_TYPE, VCID);
		put(STATION_TYPE, STATION);
		put(APID_TYPE, APID);
		put(SPSC_TYPE, SPSC);
		put(PACKET_RCT_TYPE, PACKET_RCT);
		put(SCLK_EXT_TYPE, SCLK_EXT);
		put(ERT_EXT_TYPE, ERT_EXT);
		put(SCET_EXT_TYPE, SCET_EXT);
		put(LST_EXT_TYPE, LST_EXT);
		put(RCT_TYPE, RCT);

		/** Store static instances by Name */
		put("TEST_SESSION_ID", TEST_SESSION_ID);
		put("CHANNEL_INDEX", CHANNEL_INDEX);
		put("CHANNEL_ID", CHANNEL_ID);
		put("CHANNEL_TYPE", CHANNEL_TYPE);
		put("MODULE", MODULE);
		put("SCLK", SCLK);
		put("ERT", ERT);
		put("SCET", SCET);
		put("LST", LST);
		put("NONE", NONE);
		put("HOST_ID", HOST_ID);
		put("VCID", VCID);
		put("STATION", STATION);
		put("APID", APID);
		put("SPSC", SPSC);
		put("PACKET_RCT", PACKET_RCT);
		put("SCLK_EXT", SCLK_EXT);
		put("ERT_EXT", ERT_EXT);
		put("SCET_EXT", SCET_EXT);
		put("LST_EXT", LST_EXT);
		put("RCT", RCT);
	}};
	
	/**
	 * Make the default order for this OrderBype availabable statically
	 */
	public static final IDbOrderByType DEFAULT = ERT;

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
     * Creates an instance of ChannelValueOrderByType.
     * 
     * @param strVal The initial value
     */
    public ChannelValueOrderByType(final String strVal)
	{
		super(strVal);
	}


    /**
     * 
     * Creates an instance of ChannelValueOrderByType.
     * 
     * @param intVal The initial value
     */
	public ChannelValueOrderByType(final int intVal)
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

            //result.append(IChannelValueFetch.tableAbbrev);
            //result.append('.').append(column);
            result.append(column);
        }

        return result.toString();
    }


    /**
     * Get order-by clause, but allow for it to be ascending or descending.
     *
     * @param descending True if order-by should be descending
     *
     * @return Order-by clause
     */    
    public String getOrderByClause(boolean descending)
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

            //result.append(IChannelValueFetch.tableAbbrev);
            //result.append('.').append(column);
            result.append(column);
            if (descending) {
            	result.append(" DESC");
            }
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

	
	/* (non-Javadoc)
	 * @see jpl.gds.db.mysql.impl.sql.order.IChannelValueOrderByType#getPacketRequired()
	 */
	@Override
	public boolean getPacketRequired()
    {
        return packetRequired[getValueAsInt()];
    }
}
