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
package ammos.datagen.generators;

import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;

/**
 * This is the generator class for CCSDS packet header objects. It generates
 * sequential packet headers for a single APID.
 * 
 *
 */
public class PacketHeaderGenerator implements ISeededGenerator {

	private PacketHeaderGeneratorSeed seedData;
	private int spsc;
	
	private IPacketFormatDefinition packetFormat;
	
	public PacketHeaderGenerator(IPacketFormatDefinition pktFormat) {
	    this.packetFormat = pktFormat;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed)
			throws InvalidSeedDataException, IllegalArgumentException {

		if (!(seed instanceof PacketHeaderGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type PacketHeaderGeneratorSeed");
		}
		this.seedData = (PacketHeaderGeneratorSeed) seed;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#reset()
	 */
	@Override
	public void reset() {

		this.seedData = null;
		this.spsc = 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type from this method is ISpacePacketHeader.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null) {
			throw new IllegalStateException(
					"Packet header generator is unseeded.");
		}

		final ISpacePacketHeader header = PacketHeaderFactory.create(packetFormat);
		header.setApid((short) this.seedData.getApid());
		header.setSourceSequenceCount((short) this.spsc++);
		header.setGroupingFlags((byte) 3);
		header.setPacketType((byte) 0);
		header.setSecondaryHeaderFlag((byte) 1);
		header.setVersionNumber((byte) 0);

		// Packet sequence numbers wrap when they reach the maximum size
		if (this.spsc > header.getMaxSequenceNumber()) {
			this.spsc = 0;
		}
		return header;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is unsupported in this class.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public Object getRandom() {

		throw new UnsupportedOperationException(
				"Random generation of packet headers is not supported");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is ISpacePacketHeader.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		return getNext();
	}
}
