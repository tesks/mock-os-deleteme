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


/**
 *
 * Database time ranges is a utility class for database query utilities that allows a user
 * to specify a start and/or stop time to use for restricting a query.
 *
 *
 * Updated to startTime and stopTime to use IAccurateDateTime.
 *          This allows increased resolution of query times and to match input arguments.
 */
public class DatabaseTimeRange
{
    /**
     * The start time for non-SCLK types
     */
    private IAccurateDateTime startTime = null;

    /**
     * The stop time for non-SCLK types
     */
    private IAccurateDateTime stopTime = null;

    /**
     * The start time for a SCLK type
     */
    private ISclk            startSclk = null;

    /**
     * The stop time for a SCLK type
     */
    private ISclk            stopSclk  = null;

    /**
     * The type of time used by this range (e.g. SCLK, ERT, etc.)
     */
    private DatabaseTimeType timeType = null;


    /**
     *
     * Creates an instance of DatabaseTimeRange.
     *
     * @param timeType The type of time used by this range
     */
    public DatabaseTimeRange(final DatabaseTimeType timeType)
    {
        this.timeType = timeType;
    }


    /**
     * Get start time.
     *
     * @return Returns the startTime.
     */
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getStartTime()
    {
        return startTime;
    }


    /**
     * Get start time as a long.
     *
     * @return long
     */
    public long getStartTimeLong()
    {
        return ((startTime != null) ? startTime.getTime() : 0L);
    }


    /**
     * Sets the startTime
     *
     * @param startTime
     *            The startTime to set.
     */
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setStartTime(final IAccurateDateTime startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Get stop time.
     *
     * @return Returns the stopTime.
     */
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getStopTime()
    {
        return stopTime;
    }


    /**
     * Get stop time as a long.
     *
     * @return long
     */
    public long getStopTimeLong()
    {
        return ((stopTime != null) ? stopTime.getTime() : Long.MAX_VALUE);
    }


    /**
     * Sets the stopTime
     *
     * @param stopTime
     *            The stopTime to set.
     */
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setStopTime(final IAccurateDateTime stopTime)
    {
        this.stopTime = stopTime;
    }

    /**
     * Get start SCLK.
     *
     * @return Returns the startSclk.
     */
    public ISclk getStartSclk()
    {
        return startSclk;
    }


    /**
     * Get start SCLK as a long.
     *
     * @return long
     */
    public long getStartSclkLong()
    {
        return ((startSclk != null) ? startSclk.getBinaryGdrLong() : 0L);
    }


    /**
     * Sets the startSclk
     *
     * @param startSclk
     *            The startSclk to set.
     */
    public void setStartSclk(final ISclk startSclk)
    {
        this.startSclk = startSclk;
    }

    /**
     * Get stop SCLK.
     *
     * @return Returns the stopSclk.
     */
    public ISclk getStopSclk()
    {
        return stopSclk;
    }


    /**
     * Get stop SCLK as a long.
     *
     * @return long
     */
    public long getStopSclkLong()
    {
        return ((stopSclk != null)
                    ? stopSclk.getBinaryGdrLong()
                    : Long.MAX_VALUE);
    }


    /**
     * Sets the stopSclk
     *
     * @param stopSclk
     *            The stopSclk to set.
     */
    public void setStopSclk(final ISclk stopSclk)
    {
        this.stopSclk = stopSclk;
    }

    /**
     * Get time-type.
     *
     * @return Returns the type.
     */
    public DatabaseTimeType getTimeType()
    {
        return this.timeType;
    }
    
    /**
     * Indicates if one or both of the time range qualifiers is defined.
     * @return true if upper or lower time bound is set
     */
    public boolean isRangeSpecified()
    {
    	if (this.timeType == null)
    	{
    		return false;
    	}
    	
    	if (this.timeType.isSclkRange())
    	{
    		return this.startSclk != null || this.stopSclk != null;
    	}
    	else
    	{
    		return this.startTime != null || this.stopTime != null;
    	}
    }
    

    /**
     * Returns false if both the begin and end times are specified,
     * and the end time is before the begin time.
     *
     * @return True if non-empty or not a range
     */
    public boolean isRangeNonEmpty()
    {

    	if (timeType == null)
    	{
    		return true;
    	}
    	
    	if (timeType.isSclkRange())
    	{
            return ((startSclk == null) ||
                    (stopSclk  == null) ||
                    (startSclk.compareTo(stopSclk) <= 0));
    	}

        return ((startTime == null) ||
                (stopTime  == null) ||
                (startTime.compareTo(stopTime) <= 0));
    }
}
