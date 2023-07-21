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

import java.util.Map;

/**
 * This interface is implemented by classes that create seeds used for packet
 * generation.
 * 
 *
 */
public interface IPacketSeedMaker {
	/**
	 * Creates a packet SCLK generator seed from current configuration.
	 * 
	 * @return an ISeedData object, which will be either a
	 *         DeltaSclkGeneratorSeed or a FileSeededSClkGeneratorSeed
	 */
	public ISeedData createSclkGeneratorSeed();

	/**
	 * Creates a map of PacketHeaderGeneratorSeeds, one for each packet APID in
	 * the configuration.
	 * 
	 * @return Map of Integer (APID) to PacketHeaderGeneratorSeed.
	 */
	public Map<Integer, PacketHeaderGeneratorSeed> createPacketHeaderGeneratorSeeds();
}
