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
package ammos.datagen.generators.seeds;

/**
 * This is the seed class for the packet header generator. It contains all the
 * information necessary to initialize the generator.
 * 
 *
 */
public class PacketHeaderGeneratorSeed implements ISeedData {
	private int apid;

	/**
	 * Gets the packet APID for generated packets.
	 * 
	 * @return the APID
	 */
	public int getApid() {

		return this.apid;
	}

	/**
	 * Sets the packet APID for generated packets.
	 * 
	 * @param apid
	 *            the APID to set
	 */
	public void setApid(final int apid) {

		if (apid < 0) {
			throw new IllegalArgumentException("Apid must be >= 0");
		}
		this.apid = apid;
	}
}
