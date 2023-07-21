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
package ammos.datagen.mission.nsyt.config;

/**
 * This enumeration lists the valid formats for NSYT instrument timestamps.
 * 
 *
 * MPCS-6864 - 11/21/14. Added class.
 */
public enum InstrumentTimeFormat {
	/** Time is in SEIS Local Onboard Format */
	LOBT,
	/** Time is in APSS Local Onboard Format */
	AOBT,
	/** Time is a SCLK */
	SCLK;
}
