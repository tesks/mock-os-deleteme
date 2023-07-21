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

package jpl.gds.tc.api.config;

/**
 * An enumeration indicating where delimiter frames should be placed in an uplink session.  There's generally
 * a "begin" location and an "end" location.  The "begin" location applies to putting delimiters in front of
 * frames and the "end" location applies to putting delimiters after frames.
 * 
 * ALL - A delimiter frame should be placed next to every single data frame
 * FIRST - A delimiter frame should be placed next to the first data frame
 * END - A delimiter frame should be placed next to the last data frame
 * NONE - A delimiter frame should be placed next to no data frame
 * 
 * So in the SessionBuilder, we have two variables:
 * 
 * SessionLocationType beginUplinkSession
 * SessionLocationType endUplinkSession
 * 
 * In this scenario:
 * 
 * SessionLocationType beginUplinkSession = FIRST
 * SessionLocationType endUplinkSession = LAST 
 * 
 * there would be a delimiter before the first data frame and after the last data frame.
 * 
 * In this scenario:
 * 
 * SessionLocationType beginUplinkSession = FIRST
 * SessionLocationType endUplinkSession = FIRST
 * 
 * there would be a delimiter before the first data frame and after the first data frame.
 * 
 * In this scenario:
 * 
 * SessionLocationType beginUplinkSession = ALL
 * SessionLocationType endUplinkSession = LAST
 * 
 * there would be a delimiter before every single data frame and one after the last data frame.
 * 
 * etc.
 * 
 */
public enum SessionLocationType
{
	/** A delimiter frame should be placed next to every single data frame */
	ALL,
	/** A delimiter frame should be placed next to the first data frame */
	FIRST,
	/** A delimiter frame should be placed next to the last data frame */
	LAST,
	/** A delimiter frame should be placed next to no data frame */
	NONE;

}