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
package jpl.gds.ccsds.api.tm.packet;

import jpl.gds.shared.algorithm.IGenericAlgorithm;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The ISecondaryPacketHeaderExtractor interface is to be implemented by any classes
 * that handle the secondary header from a telemetry space packet.
 * 
 * Secondary header formats are allowed to vary per-APID, and there is no standard
 * for what the format of data within the secondary header may contain. This interface
 * supports the varying format by offering a generic way for packet processors to handle
 * secondary headers.
 * 
 * Classes implementing this interface should be thread-safe, or more preferably,
 * stateless.
 */
@CustomerAccessible(immutable = true)
public interface ISecondaryPacketHeaderExtractor extends IGenericAlgorithm {
	
	/**
	 * This method should be used to extract a secondary header from a byte array. 
	 * The operation assumes the appropriate amount of data is available, so a runtime
	 * exception may occur if calling this without first checking the return value of
	 * {@link #hasEnoughBytes(byte[], int)}.
	 * @param data the raw data which contains a secondary header.
	 * @param offset the offset into the data which marks the beginning of the secondary header.
	 * @return an extracted secondary header
	 */
    public ISecondaryPacketHeader extract(final byte[] data, final int offset);
	
	/**
	 * Checks if enough bytes remain between the given offset and the end
	 * of the array to successfully extract a secondary header.  Intended to be used
	 * to determine whether it is safe to call the extract(byte[], int) method.
	 * @param data the raw data which contains either a partial or complete secondary header
	 * @param offset the offset into the data which marks the beginning of the secondary header 
	 * @return false if calling extract(byte[], int) would result in an error, true if not.
	 */
    public boolean hasEnoughBytes(final byte[] data, final int offset);
		
}
