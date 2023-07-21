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
package jpl.gds.shared.time;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.types.EnumeratedType;

/**
 * This class is an enumeration of all the different time representations
 * used in various tables in the database.  The query utilities use this object
 * to determine what type of time parameter is being queried.
 * 
 */
public class DatabaseTimeType extends EnumeratedType
{
	// The order of the types and the ordinal values bears no relationship
	// to the order of the fields in the actual Table entry.

	// Static int values
    /** SCET */
	public static final int SCET_TYPE          = 0;
    /** ERT */
	public static final int ERT_TYPE           = 1;
    /** RCT */
	public static final int RCT_TYPE           = 2;
    /** SCLK */
	public static final int SCLK_TYPE          = 3;
    /** Creation time */
	public static final int CREATION_TIME_TYPE = 4;
    /** Event time */
	public static final int EVENT_TIME_TYPE    = 5;
    /** ERT Fine */
	public static final int ERTFINE_TYPE       = 6;
    /** LST */
	public static final int LST_TYPE           = 7; 

	/** Static string values */
	@SuppressWarnings({"MALICIOUS_CODE","MS_PKGPROTECT"}) 
	public static final String timeTypes[] = {
		"SCET"
		, "ERT"
		, "RCT"
		, "SCLK"
		, "CREATION_TIME"
		, "EventTime"
		, "ERTFine"
		, "LST"
	};

	// Static instances for each enumerated value
    /** SCET */
	public final static DatabaseTimeType SCET          = new DatabaseTimeType(SCET_TYPE);
    /** ERT */
	public final static DatabaseTimeType ERT           = new DatabaseTimeType(ERT_TYPE);
    /** RCT */
	public final static DatabaseTimeType RCT           = new DatabaseTimeType(RCT_TYPE);
    /** SCLK */
	public final static DatabaseTimeType SCLK          = new DatabaseTimeType(SCLK_TYPE);
    /** Creation time */
	public static final DatabaseTimeType CREATION_TIME = new DatabaseTimeType(CREATION_TIME_TYPE);
    /** Event time */
	public static final DatabaseTimeType EVENT_TIME    = new DatabaseTimeType(EVENT_TIME_TYPE);
    /** ERT Fine */
	public static final DatabaseTimeType ERT_FINE      = new DatabaseTimeType(ERTFINE_TYPE);
    /** LST */
	public static final DatabaseTimeType LST           = new DatabaseTimeType(LST_TYPE);

	/**
	 * 
	 * Creates an instance of DatabaseTimeType.
	 * 
	 * @param strVal The initial value
	 */
	public DatabaseTimeType(final String strVal)
	{
		super(strVal);
	}

	/**
	 * 
	 * Creates an instance of DatabaseTimeType.
	 * 
	 * @param intVal The initial value
	 */
	public DatabaseTimeType(final int intVal)
	{
		super(intVal);
	}

	/*
	 * {@inheritDoc}
	 * @see jpl.gds.util.EnumeratedType#getStringValue(int)
	 */
	@Override
	protected String getStringValue(final int index)
	{
		if(index < 0 || index > getMaxIndex())
		{
			throw new ArrayIndexOutOfBoundsException();
		}

		return(timeTypes[index]);
	}

	/*
	 * {@inheritDoc}
	 * @see jpl.gds.util.EnumeratedType#getMaxIndex()
	 */
	@Override
	protected int getMaxIndex()
	{
		return(timeTypes.length-1);
	}
	
    /**
     * True if is range of Date.
     *
     * @return True if is range of Date
     */	
	public boolean isDateRange()
	{
		switch(this.valIndex)
		{
			case SCET_TYPE:
			case ERT_TYPE:
			case RCT_TYPE:
			case CREATION_TIME_TYPE:
			case EVENT_TIME_TYPE:
			case ERTFINE_TYPE:
				return true;
			
			case SCLK_TYPE:
			case LST_TYPE:
			default:
				return false;
		}
	}


    /**
     * True if is range of SCLK.
     *
     * @return True if is range of SCLK
     */	
	public boolean isSclkRange()
	{
		return(!isDateRange());
	}
}
