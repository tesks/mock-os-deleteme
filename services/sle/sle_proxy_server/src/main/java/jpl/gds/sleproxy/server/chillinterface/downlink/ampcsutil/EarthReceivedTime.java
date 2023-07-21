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
package jpl.gds.sleproxy.server.chillinterface.downlink.ampcsutil;

import java.util.Date;

/**
 * Wrapper for java.util.Date for retrieving Earth Received Time UTC label
 * values. This code is taken from AMPCS core's, but because we don't want to
 * introduce a dependency on AMPCS core just for this class, it was basically
 * copied over. In AMPCSR8, when SLE capability is integrated into AMPCS
 * architecture itself, this duplication can then be removed.
 * 
 */
public class EarthReceivedTime extends Date {

	private static final long serialVersionUID = 1L;

	/**
	 * Constant integer for number of seconds in a day.
	 */
	private static final int SEC_PER_DAY = 60 * 60 * 24;

	/**
	 * Constant integer for number of milliseconds in a day.
	 */
	private static final int MS_PER_DAY = 1000 * SEC_PER_DAY;

	/**
	 * Construct ERT using current UTC time.
	 */
	public EarthReceivedTime() {
		super();
	}

	/**
	 * Construct ERT using the specified time.
	 * 
	 * @param date
	 *            The initializing date object
	 */
	public EarthReceivedTime(final Date date) {
		super(date.getTime());
	}

	/**
	 *
	 * @param jdref
	 *            Julian Date reference
	 * @param jdoffset
	 *            Julian Date offset
	 * @param seconds
	 *            Seconds
	 * @param millis
	 *            Milliseconds
	 * @return Milliseconds since the epoch
	 */
	private static long reconstruct(final Date jdref, final int jdoffset, final int seconds, final int millis) {
		return ((jdref.getTime() / MS_PER_DAY + jdoffset) * SEC_PER_DAY + seconds) * 1000 + millis;
	}

	/**
	 * Reconstruct a Date from fragments and reference date.
	 * 
	 * @param jdref
	 *            Julian Date reference
	 * @param jdoffset
	 *            Julian Date offset
	 * @param seconds
	 *            Seconds
	 * @param millis
	 *            Milliseconds
	 */
	public EarthReceivedTime(final Date jdref, final int jdoffset, final int seconds, final int millis) {
		super(reconstruct(jdref, jdoffset, seconds, millis));
	}

	/**
	 * Get the Julian Date offset.
	 * 
	 * @param ref
	 *            Reference date.
	 * @return Difference in number of days
	 */
	public final int getJulianOffset(final Date ref) {
		long refjd = ref.getTime() / MS_PER_DAY;
		long day = this.getTime() / MS_PER_DAY;
		return (int) (day - refjd);
	}

	/**
	 * Get the ERT in milliseconds.
	 * 
	 * @return ERT in milliseconds
	 */
	public final int getMilliseconds() {
		return (int) (this.getTime() % 1000);
	}

	/**
	 * Get the ERT in seconds.
	 * 
	 * @return ERT in seconds
	 */
	public final int getSecondsOfDay() {
		return (int) ((this.getTime() / 1000) % SEC_PER_DAY);
	}
	
}
