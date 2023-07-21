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

/**
 * TimeUnit is an enumeration of the possible time units for decom fields that
 * can be applied as channel timestamps.
 *  
 *
 */
public enum TimeUnit {
	/**
	 * Field value is a SCET time.
	 */
    SCET,
    /**
     * Field value is a SCLK time.
     */
    SCLK,
    /**
     * Field value is an LST time.
     */
    LST,
    /**
     * Field value is an ERT or wall-clock time in milliseconds.
     */
    MS;
    
    /**
     * Indicates whether this time type value can be derived from SCLK time.
     * @return true if time can be obtained from SCLK, false if not
     */
    public boolean isSclkBased() {
    	return this.equals(TimeUnit.SCET) || this.equals(TimeUnit.SCLK) ||
    	this.equals(TimeUnit.LST);
    }
}
