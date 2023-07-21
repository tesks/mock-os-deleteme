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

import java.text.ParseException;

import jpl.gds.serialization.primitives.time.Proto3Lst;

/**
 * A factory that created ILocalSolarTime objects.
 * 
 */
public class LocalSolarTimeFactory 
{

	/**
	 * Gets a new ILocalSolarTime for the given spacecraft ID, set to the
	 * LST matching the given SCET.
	 * 
	 * @param scet
	 *            the SCET to initialize the LST from
	 * @param scid the spacecraft ID
	 * @return the new ILocalSolarTime object
	 */
	public static ILocalSolarTime getNewLst(final IAccurateDateTime scet, int scid) {

		return new LocalSolarTime(scid, scet);
	}

	/**
	 * Gets a new ILocalSolarTime for the given spacecraft ID, set to the
	 * LST matching the given SCLK.
	 * 
	 * @param sclk
	 *            the SCLK to initialize the LST from
	 * @param scid  the spacecraft ID
	 * @return the new ILocalSolarTime object
	 */
	public static ILocalSolarTime getNewLst(final ISclk sclk, int scid) {

		return new LocalSolarTime(scid, sclk);
	}


	/**
	 * Gets a new ILocalSolarTime for the given spacecraft ID, set to the
	 * LST matching the given SOL and time, in milliseconds, which is assumed to
	 * represent local solar hours, minutes, seconds, and milliseconds.
	 * 
	 * @param sol
	 *            SOL number
	 * @param time
	 *            local solar time (delta to supplied SOL) in milliseconds
	 * @param scid the spacecraft ID
	 * @return the new ILocalSolarTime object
	 */
	public static ILocalSolarTime getNewLst(final int sol, final long time, int scid) {

		return new LocalSolarTime(scid, sol, time);
	}
	
	/**
	 * Gets a new ILocalSolarTime for the supplied spacecraft, set to the
	 * current time. 
	 * 
	 * @param scid
	 *           Spacecraft ID
	 * @return the new ILocalSolarTime object
	 * 
	 */
	public static ILocalSolarTime getNewLst(int scid) {

	    return new LocalSolarTime(scid);
	}
	

    /**
     * Gets a new ILocalSolarTime for spacecraft ID 0, set to the
	 * given time.
	 * 
     * @param time time in milliseconds
     * @param solNum sol day number
     * @return the new ILocalSolarTime object
     */
    public static ILocalSolarTime getNewLstNoScid(long time, int solNum) {

        return new LocalSolarTime(time, solNum);
    }

	/**
	 * Gets a new ILocalSolarTime for the given spacecraft ID by parsing
	 * the given LST string.
	 * 
	 * @param timeStr
	 *            LST string to parse
	 * @param scid the spacecraft ID
	 * @return the new ILocalSolarTime object
	 * @throws ParseException if the given LST string cannot be parsed
	 */
	public static ILocalSolarTime getNewLst(final String timeStr, int scid)
			throws ParseException {

		return new LocalSolarTime(scid, timeStr);
	}
	
	/**
     * Gets a new ILocalSolarTime by parsing the given LST string.
     * 
     * @param timeStr
     *            LST string to parse
     * @return the new ILocalSolarTime object
     * @throws ParseException if the given LST string cannot be parsed
     */
    public static ILocalSolarTime getNewLst(final String timeStr)
            throws ParseException {

        return new LocalSolarTime(timeStr);
    }
    
    /**
     * Gets a new ILocalSolar time for the given spacecraft ID by parsing the
     * given protobuf message
     * 
     * @param msg
     *            the LST protobuf message to parse
     * @param scid
     *            the spacecraft ID
     * @return the new ILocalSolarTime object
     * @throws ParseException
     *             if the given protobuf message cannot be parsed
     */
    public static ILocalSolarTime getNewLst(final Proto3Lst msg, int scid)
    		throws ParseException {
    	return new LocalSolarTime(scid, msg.getSol(), msg.getMilliseconds());
    }

}
