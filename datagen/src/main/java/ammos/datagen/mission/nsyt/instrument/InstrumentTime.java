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
package ammos.datagen.mission.nsyt.instrument;

/**
 * This interface must be implemented by NSYT instrument time classes to be used
 * in NSYT continuous packets.
 * 
 * MPCS-6864 - 12/1/14. Added class.
 * 
 */
public interface InstrumentTime {
	/**
	 * Writes the time to a byte array, which must be padded to the given number
	 * of bytes.
	 * 
	 * @param padToLength
	 *            the length to pad the byte array to; the method will throw if
	 *            this length is not long enough for the time.
	 * 
	 * @return byte array containing the time
	 */
	public byte[] getPaddedBytes(int padToLength);
}
