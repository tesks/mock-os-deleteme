package jpl.gds.db.mysql.impl.sql.order;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;
import jpl.gds.db.api.sql.order.IDbOrderByType;

public class ChannelAggregateOrderByType extends AbstractOrderByType implements IChannelAggregateOrderByType {

    /** String value constants */
	@SuppressWarnings({"MS_MUTABLE_ARRAY","MS_PKGPROTECT"})
	public static final String orderByTypes[] =
	{ 
		"None",
		"ERT",
		"SCLK",
		"SCET",
		"RCT",
		"LST",
		"ChannelId",
		"Module"
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
		"",	// Indicates no fields and hence no sort
		"ertCoarse_mstCoarse,ertFine_mstFine",
		"beginSclkCoarse",
		"beginScetCoarse",
		"beginRctCoarse",
		"beginSclkCoarse", //LST
		"", //ChannelId
		""  //Module
	};	
	
	/** NONE */
	public static final ChannelAggregateOrderByType NONE = new ChannelAggregateOrderByType(NONE_TYPE);
	
	/** ERT */
	public static final ChannelAggregateOrderByType ERT = new ChannelAggregateOrderByType(ERT_TYPE);

	/** SCLK */
	public static final ChannelAggregateOrderByType SCLK = new ChannelAggregateOrderByType(SCLK_TYPE);

	/** SCET */
	public static final ChannelAggregateOrderByType SCET = new ChannelAggregateOrderByType(SCET_TYPE);
	
	/** LST */
	public static final ChannelAggregateOrderByType LST = new ChannelAggregateOrderByType(LST_TYPE);
	
	/** RCT */
	public static final ChannelAggregateOrderByType RCT = new ChannelAggregateOrderByType(RCT_TYPE);

	/** CHANNEL_ID */
	public static final ChannelAggregateOrderByType CHANNEL_ID = new ChannelAggregateOrderByType(CHANNEL_ID_TYPE);
	

	
//    private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, ChannelAggregateOrderByType>() {
//		private static final long serialVersionUID = 8026584546364244939L;
//	{
//		/** Store static instances by Ordinal */
//		put(NONE_TYPE, NONE);
//		put(ERT_TYPE, ERT);
//		put(SCLK_TYPE, SCLK);
//		put(SCET_TYPE, SCET);
//		
//
//		/** Store static instances by Name */
//		put("NONE", NONE);
//		put("ERT", ERT);
//		put("SCLK", SCLK);
//		put("SCET", SCET);
//	}};
	
	public ChannelAggregateOrderByType(int intVal) {
		super(intVal);
	}
	
	public ChannelAggregateOrderByType(final String strVal) {
		super(strVal);
	}

	/**
	 * Make the default order for this OrderBype availabable statically
	 */
	public static final IDbOrderByType DEFAULT = NONE;

    /**
     * Get the static instance of the default OrderBy for this type
     * 
     * @return the Default OrderBy
     */
    @java.lang.SuppressWarnings("unchecked")
    public static <T extends IDbOrderByType> T getOrderBy() {
        return (T) DEFAULT;
    }
	
	@Override
	public String getOrderByClause() {
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

            result.append(column);
        }

        return result.toString();
	}

	@Override
	public String[] getOrderByColumns() {
        final String fields = databaseFieldNames[getValueAsInt()];

        if (fields.isEmpty())
        {
            return new String[0];
        }

        return fields.split(",");
	}

	@Override
	protected String getStringValue(int index) {
		if(index < 0 || index > getMaxIndex())
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		
		return(orderByTypes[index]);
	}

	@Override
	protected int getMaxIndex() {
		return(orderByTypes.length-1);
	}

}
