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
import jpl.gds.db.api.sql.fetch.ICommandFetch;
import jpl.gds.db.api.sql.order.ICommandOrderByType;
import jpl.gds.db.api.sql.order.IDbOrderByType;


/**
 * This class is an enumeration of all the possible output orderings for
 * queries done on the HardwareCommandMessage database table.
 *
 * @version MPCS-6808 Add RCT
 */
public class CommandOrderByType extends AbstractOrderByType implements ICommandOrderByType
{
	// Integer value constants

    /** String value constants */
	@SuppressWarnings({"MS_MUTABLE_ARRAY","MS_PKGPROTECT"})
	public static final String orderByTypes[] =
	{ 
		"SessionId",
	    "EventTime", 
		"MessageType",
        "None",
        "RequestId",
        "RCT"
	};

	/**
     * Database field names for each value. Ordered by the int values
     * corresponding to orderByTypes. Each one is an array of the column
     * names to be used for that order-by (may be empty.)
     */
	private static final String[] databaseFieldNames[] =
	{
        /** SessionId columns */
		{"sessionId",
         "hostId",
         "requestId",
         "eventTimeCoarse",
         "eventTimeFine"}, 

        /** EventTime columns */
		{"eventTimeCoarse",
         "eventTimeFine",
         "sessionId",
         "hostId",
         "requestId"}, 

        /** MessageType columns */
		{"type",
         "sessionId",
         "hostId",
         "requestId",
         "eventTimeCoarse",
         "eventTimeFine"},

        /** "None" columns */
        {}, // No field, no order-by

        /** RequestId columns */
		{"requestId",
         "sessionId",
         "hostId",
         "eventTimeCoarse",
         "eventTimeFine"},

        {"rctCoarse",
         "rctFine",
         "requestId"}
	};

	/**
     * Database field abbreviations for each value. Ordered by the int values
     * corresponding to orderByTypes. Each one is an array of the table
     * abbreviations to be used for each column of that order-by
     * (may be empty.) This table is needed because the CommandMessage and
     * CommandStatus tables are joined, and the columns may come from either.
     */
	private static final String[] databaseFieldAbbrevs[] =
	{ 
        /** SessionId columns */
        {ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV},

        /** EventTime columns */
		{ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV},

        /** MessageType columns */
		{ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV},

        /** "None" columns */
        {}, // No field, no order-by

        /** RequestId columns */
        {ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV},

        {ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
         ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
         ICommandFetch.COMMAND_MESSAGE_TABLE_ABBREV}
	};
    
	/**
     * Marks the database fields that must be cast to char.
     * Those are the ENUMs. We want the order-by to use the
     * string values, not the int ENUM values.
     *
     * Ordered by the int values corresponding to orderByTypes. Each one is an
     * array of booleans (may be empty.)
     *
     * MPCS-7362  New
     */
	private static final boolean[] databaseFieldCasts[] =
    {
        /** SessionId columns */
        {false,
         false,
         false,
         false,
         false},

        /** EventTime columns */
		{false,
         false,
         false,
         false,
         false},

        /** MessageType columns */
		{true, /** CommandMessage.type */
         false,
         false,
         false,
         false,
         false},

        /** "None" columns */
        {}, // No field, no order-by

        /** RequestId columns */
        {false,
         false,
         false,
         false,
         false},

        // MPCS-10869  - missing this entry. adding it is the fix for chill_get_commands --timeType RCT
		/** RCT columns */
		{false,
		false,
		false}
	};

	/**
	 *  Static instances of each value
	 */
    /** TEST_SESSION_ID */
	public static final CommandOrderByType TEST_SESSION_ID = new CommandOrderByType(TEST_SESSION_ID_TYPE);

    /** EVENT_TIME */
    public static final CommandOrderByType EVENT_TIME = new CommandOrderByType(EVENT_TIME_TYPE);

    /** MESSAGE_TYPE */
    public static final CommandOrderByType MESSAGE_TYPE = new CommandOrderByType(MESSAGE_TYPE_TYPE);

    /** NONE */
    public static final CommandOrderByType NONE = new CommandOrderByType(NONE_TYPE);

    /** REQUEST_ID */
    public static final CommandOrderByType REQUEST_ID = new CommandOrderByType(REQUEST_ID_TYPE);

    /** RCT */
    public static final CommandOrderByType RCT = new CommandOrderByType(RCT_TYPE);
    

	private static Map<Object, ? extends IDbOrderByType> orderByMap = new HashMap<Object, CommandOrderByType>() {
		private static final long serialVersionUID = 8026584546364244939L;
	{
		/** Store static instances by Ordinal */
		put(TEST_SESSION_ID_TYPE, TEST_SESSION_ID);
		put(EVENT_TIME_TYPE, EVENT_TIME);
		put(MESSAGE_TYPE_TYPE, MESSAGE_TYPE);
		put(NONE_TYPE, NONE);
		put(REQUEST_ID_TYPE, REQUEST_ID);
		put(RCT_TYPE, RCT);

		/** Store static instances by Name */
		put("TEST_SESSION_ID", TEST_SESSION_ID);
		put("EVENT_TIME", EVENT_TIME);
		put("MESSAGE_TYPE", MESSAGE_TYPE);
		put("NONE", NONE);
		put("REQUEST_ID", REQUEST_ID);
		put("RCT", RCT);
	}};

	
	/**
	 * Make the default order for this OrderBype availabable statically
	 */
	public static final CommandOrderByType DEFAULT = REQUEST_ID;

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
     *            The OrderBy Type Name
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
     * Creates an instance of CommandMessageOrderByType.
     * 
     * @param strVal The initial value
     */
    public CommandOrderByType(final String strVal)
	{
		super(strVal);
	}
    
	/**
	 * Creates an instance of CommandMessageOrderByType.
	 * 
	 * @param intVal The initial value
	 */
	public CommandOrderByType(final int intVal)
	{
		super(intVal);
	}
	   
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
	 */
	@Override
	protected String getStringValue(final int index)
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
     *
     * @version MPCS-7362  Add casts
	 */
	@Override
	public String getOrderByClause()
	{
        final String[] fields  = databaseFieldNames[getValueAsInt()];
        final String[] abbrevs = databaseFieldAbbrevs[getValueAsInt()];

        if (fields.length == 0)
        {
            return ""; // No field, no order-by
        }

        final boolean[]     casts = databaseFieldCasts[getValueAsInt()];
        final StringBuilder sb    = new StringBuilder(ORDER_BY_PREFIX);
        boolean             first = true;

        for (int i = 0; i < fields.length; ++i)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            if (casts[i])
            {
                sb.append("CAST(");
            }

            sb.append(abbrevs[i]).append('.').append(fields[i]);

            if (casts[i])
            {
                sb.append(" AS CHAR)");
            }
        }

		return sb.toString();
	}

	
	/**
	 * Return an SQL "order by" clause based on the value
	 * of this enumerated type.
	 * 
	 * @return Array of column names
	 */
	@Override
    public String[] getOrderByColumns()
    {
        return databaseFieldNames[getValueAsInt()];
    }
}
