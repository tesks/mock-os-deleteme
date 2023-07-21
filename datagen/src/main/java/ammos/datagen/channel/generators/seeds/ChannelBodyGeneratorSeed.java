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
package ammos.datagen.channel.generators.seeds;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ammos.datagen.channel.config.ApidRotationCount;
import ammos.datagen.channel.config.CustomPacket;
import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.seeds.BooleanGeneratorSeed;
import ammos.datagen.generators.seeds.EnumGeneratorSeed;
import ammos.datagen.generators.seeds.FloatGeneratorSeed;
import ammos.datagen.generators.seeds.IBasicFieldSeedHolder;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.IntegerGeneratorSeed;
import ammos.datagen.generators.seeds.StringGeneratorSeed;
import jpl.gds.dictionary.api.channel.IChannelDefinition;

/**
 * This is the seed data class for the ChannelBodyGenerator. It contains all the
 * data necessary to initialize the generator. <br>
 * It is important to note that the channel generator currently runs in three
 * basic modes: a RANDOM mode, in which channels are placed into packets
 * randomly regardless of APID, a BY_APID mode, which the the channels to be
 * placed into each packet are configured by APID by the channel values are
 * variable, and a CUSTOM mode, in which a specific sequence of custom packets
 * is generated, each having a specified APID and exact list of channel values.
 * In the first two modes, the configuration may also specify a packet rotation
 * count by APID. The mode can be quickly determined using getPacketMode(). <br>
 * In the RANDOM mode, getApidCounts() may return a non-empty list, but both
 * getCustomPackets() and getChannelsPerApid() will return empty lists. <br>
 * In the BY_APID mode, getApidCounts() may return a non-empty list,
 * getCustomPackets() will return an empty list, and getChannelsPerApid() will
 * return a non-empty list. <br>
 * In the CUSTOM mode, getApidCounts() will return an empty list,
 * getCustomPackets() will return a non-empty list, and getChannelsPerApid()
 * will return an empty list.
 * 
 * 
 */
public class ChannelBodyGeneratorSeed implements ISeedData,
		IBasicFieldSeedHolder {

	private List<IChannelDefinition> chanDefs = new LinkedList<IChannelDefinition>();
	private boolean includeInvalidPackets;
	private boolean includeInvalidIndices;
	private float invalidIndexPercent;

	private List<Integer> invalidIndices = new LinkedList<Integer>();
	private final Map<Integer, IntegerGeneratorSeed> integerSeeds = new HashMap<Integer, IntegerGeneratorSeed>(
			4);
	private final Map<Integer, IntegerGeneratorSeed> unsignedSeeds = new HashMap<Integer, IntegerGeneratorSeed>(
			4);
	private final Map<Integer, FloatGeneratorSeed> floatSeeds = new HashMap<Integer, FloatGeneratorSeed>();
	private Map<String, EnumGeneratorSeed> enumSeeds = new HashMap<String, EnumGeneratorSeed>();
	private BooleanGeneratorSeed booleanSeed;
	private StringGeneratorSeed stringSeed;
	private int minSamples;
	private int maxSamples;
	private List<ApidRotationCount> apidCounts = new LinkedList<ApidRotationCount>();
	private Map<Integer, List<String>> channelsPerApid = new HashMap<Integer, List<String>>();
	private List<CustomPacket> customPackets = new LinkedList<CustomPacket>();
	private TraversalType traversalType = TraversalType.SEQUENTIAL;

	/**
	 * Gets the list of packet rotation counts by APID.
	 * 
	 * @return List of ApidRotationCount objects; non-modifiable.
	 */
	public List<ApidRotationCount> getApidCounts() {

		return Collections.unmodifiableList(this.apidCounts);
	}

	/**
	 * Sets the list of packet rotation counts by APID.
	 * 
	 * @param apidCounts2
	 *            List of ApidRotationCount objects to set; may not be null.
	 */
	public void setApidCounts(final List<ApidRotationCount> apidCounts2) {

		if (apidCounts2 == null) {
			throw new IllegalArgumentException(
					"apid counts may not be null; set an empty list");
		}
		this.apidCounts = apidCounts2;
	}

	/**
	 * Gets the desired percentage of invalid channel index packets to generate.
	 * 
	 * @return percentage
	 */
	public float getInvalidIndexPercent() {

		return this.invalidIndexPercent;
	}

	/**
	 * Sets the desired percentage of invalid channel index packets to generate.
	 * 
	 * @param invalidIndexPercent
	 *            percentage to set, between 0 and 100.
	 */
	public void setInvalidIndexPercent(final float invalidIndexPercent) {

		if (invalidIndexPercent < 0.0 || invalidIndexPercent > 100.0) {
			throw new IllegalArgumentException(
					"invalid index percentage must be between 0 abd 100");
		}
		this.invalidIndexPercent = invalidIndexPercent;
	}

	/**
	 * Gets the list of invalid channel indices.
	 * 
	 * @return List of Integer, or an empty list if no invalid indices
	 *         configured; non-modifiable.
	 */
	public List<Integer> getInvalidIndices() {

		return Collections.unmodifiableList(this.invalidIndices);
	}

	/**
	 * Sets the list of invalid channel indices.
	 * 
	 * @param invalidIndices
	 *            List of Integer; may not be null
	 */
	public void setInvalidIndices(final List<Integer> invalidIndices) {

		if (invalidIndices == null) {
			throw new IllegalArgumentException(
					"invalid index list cannot be null; set an empty list");
		}

		this.invalidIndices = invalidIndices;
	}

	/**
	 * Gets the traversal type for channel body generation: RANDOM or
	 * SEQUENTIAL.
	 * 
	 * @return TraversalType enumeration value
	 */
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for channel body generation: RANDOM or
	 * SEQUENTIAL.
	 * 
	 * @param traverse
	 *            TraversalType enumeration value; may not be null
	 */
	public void setTraversalType(final TraversalType traverse) {

		if (traverse == null) {
			throw new IllegalArgumentException("traverse may not be null");
		}
		this.traversalType = traverse;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getEnumSeeds()
	 */
	@Override
	public Map<String, EnumGeneratorSeed> getEnumSeeds() {

		return Collections.unmodifiableMap(this.enumSeeds);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setEnumSeeds(java.util.Map)
	 */
	@Override
	public void setEnumSeeds(final Map<String, EnumGeneratorSeed> enumSeeds) {

		if (enumSeeds == null) {
			throw new IllegalArgumentException(
					"enumSeeds may not be null. Set an empty map.");
		}

		this.enumSeeds = Collections.unmodifiableMap(enumSeeds);
	}

	/**
	 * Indicates whether to include invalid (corrupted) channel bodies in the
	 * result file.
	 * 
	 * @return true to generate invalid channel packets, false to not
	 */
	public boolean isIncludeInvalidPackets() {

		return this.includeInvalidPackets;
	}

	/**
	 * Sets the flag indicating whether to include invalid (corrupted) channel
	 * bodies in the result file.
	 * 
	 * @param includeInvalid
	 *            true to generate invalid packets, false to not
	 */
	public void setIncludeInvalidPackets(final boolean includeInvalid) {

		this.includeInvalidPackets = includeInvalid;
	}

	/**
	 * Indicates whether to include invalid channel indices in the result file.
	 * 
	 * @return true to generate invalid channel indices, false to not
	 */
	public boolean isIncludeInvalidIndices() {

		return this.includeInvalidIndices;
	}

	/**
	 * Sets the flag indicating whether to include invalid channel indices in
	 * the result file.
	 * 
	 * @param includeInvalid
	 *            true to generate invalid packets, false to not
	 */
	public void setIncludeInvalidIndices(final boolean includeInvalid) {

		this.includeInvalidIndices = includeInvalid;
	}

	/**
	 * Gets the list of channel definition objects for channels to include in
	 * the result file. This list has been filtered per the channel seed
	 * configuration in the run configuration file.
	 * 
	 * @return List of IChannelDefinition, or the empty list if none configured;
	 *         non-modifiable.
	 */
	public List<IChannelDefinition> getChannelDefs() {

		return Collections.unmodifiableList(this.chanDefs);
	}

	/**
	 * Sets the list of channel definition objects for channels to include in
	 * the result file. This list has been filtered per the channel seed
	 * configuration in the run configuration file.
	 * 
	 * @param chanDefs
	 *            List of IChannelDefinition, or the empty list if none
	 *            configured; non-modifiable.
	 */
	public void setChannelDefs(final List<IChannelDefinition> chanDefs) {

		if (chanDefs == null) {
			throw new IllegalArgumentException(
					"chanDefs may not be null. Set an empty list.");
		}
		this.chanDefs = Collections.unmodifiableList(chanDefs);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getIntegerSeed(int)
	 */
	@Override
	public IntegerGeneratorSeed getIntegerSeed(final int byteSize) {

		return this.integerSeeds.get(byteSize);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setIntegerSeed(ammos.datagen.generators.seeds.IntegerGeneratorSeed,
	 *      int)
	 */
	@Override
	public void setIntegerSeed(final IntegerGeneratorSeed integerSeed,
			final int byteSize) {

		if (integerSeed == null) {
			throw new IllegalArgumentException("integer seed may not be null");
		}
		if (byteSize != 1 && byteSize != 2 && byteSize != 4 && byteSize != 8) {
			throw new IllegalArgumentException("invalid byte size");
		}
		this.integerSeeds.put(byteSize, integerSeed);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getUnsignedSeed(int)
	 */
	@Override
	public IntegerGeneratorSeed getUnsignedSeed(final int byteSize) {

		return this.unsignedSeeds.get(byteSize);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setUnsignedSeed(ammos.datagen.generators.seeds.IntegerGeneratorSeed,
	 *      int)
	 */
	@Override
	public void setUnsignedSeed(final IntegerGeneratorSeed unsignedSeed,
			final int byteSize) {

		if (unsignedSeed == null) {
			throw new IllegalArgumentException("integer seed may not be null");
		}
		if (byteSize != 1 && byteSize != 2 && byteSize != 4 && byteSize != 8) {
			throw new IllegalArgumentException("invalid byte size");
		}
		this.unsignedSeeds.put(byteSize, unsignedSeed);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getFloatSeed(int)
	 */
	@Override
	public FloatGeneratorSeed getFloatSeed(final int byteSize) {

		return this.floatSeeds.get(byteSize);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setFloatSeed(ammos.datagen.generators.seeds.FloatGeneratorSeed,
	 *      int)
	 */
	@Override
	public void setFloatSeed(final FloatGeneratorSeed floatSeed,
			final int byteSize) {

		if (floatSeed == null) {
			throw new IllegalArgumentException("float seed may not be null");
		}
		if (byteSize != 4 && byteSize != 8) {
			throw new IllegalArgumentException("invalid byte size");
		}
		this.floatSeeds.put(byteSize, floatSeed);
	}

	/**
	 * Sets the boolean generator seed.
	 * 
	 * @param boolSeed
	 *            the BooleanGeneratorSeed to set; may not be null
	 */
	public void setBooleanSeed(final BooleanGeneratorSeed boolSeed) {

		if (boolSeed == null) {
			throw new IllegalArgumentException("boolean seed may not be null");
		}
		this.booleanSeed = boolSeed;
	}

	/**
	 * Gets the boolean generator seed.
	 * 
	 * @return the BooleanGeneratorSeed object
	 */
	public BooleanGeneratorSeed getBooleanSeed() {

		return this.booleanSeed;
	}

	/**
	 * Sets the string generator seed.
	 * 
	 * @param stringSeed
	 *            the StringGeneratorSeed to set; may not be null
	 */
	public void setStringSeed(final StringGeneratorSeed stringSeed) {

		if (stringSeed == null) {
			throw new IllegalArgumentException("string seed may not be null");
		}
		this.stringSeed = stringSeed;
	}

	/**
	 * Gets the string generator seed.
	 * 
	 * @return the StringGeneratorSeed object
	 */
	public StringGeneratorSeed getStringSeed() {

		return this.stringSeed;
	}

	/**
	 * Gets the minimum number of channel samples in a packet.
	 * 
	 * @return minimum number of samples
	 */
	public int getMinSamples() {

		return this.minSamples;
	}

	/**
	 * Sets the minimum number of channel samples in a packet.
	 * 
	 * @param minSamples
	 *            minimum number of samples to set. Must be >= 1;
	 */
	public void setMinSamples(final int minSamples) {

		if (minSamples < 1) {
			throw new IllegalArgumentException(
					"minimum number of samples must be >= 1");
		}
		this.minSamples = minSamples;
	}

	/**
	 * Gets the maximum number of channel samples in a packet.
	 * 
	 * @return maximum number of samples
	 */
	public int getMaxSamples() {

		return this.maxSamples;
	}

	/**
	 * Sets the maximum number of channel samples in a packet.
	 * 
	 * @param maxSamples
	 *            maximum number of samples to set. Must be >= 1;
	 */
	public void setMaxSamples(final int maxSamples) {

		if (maxSamples < 1) {
			throw new IllegalArgumentException(
					"maximum number of samples must be >= 1");
		}
		this.maxSamples = maxSamples;
	}

	/**
	 * Gets the map of the channels to include per packet APID.
	 * 
	 * @return Map of Integer (packet APID) to list of channel ID strings;
	 *         non-modifiable.
	 */
	public Map<Integer, List<String>> getChannelsPerApid() {

		return Collections.unmodifiableMap(this.channelsPerApid);
	}

	/**
	 * Sets the map of the channels to include per packet APID.
	 * 
	 * @param channelsPerApid
	 *            Map of Integer (packet APID) to list of channel ID strings;
	 *            may not be null.
	 */
	public void setChannelsPerApid(
			final Map<Integer, List<String>> channelsPerApid) {

		if (channelsPerApid == null) {
			throw new IllegalArgumentException(
					"channels per apid may not be null; set an empty list");
		}
		this.channelsPerApid = channelsPerApid;
	}

	/**
	 * Gets the list of custom packet definitions.
	 * 
	 * @return List of CustomPacket, or the empty list if none defined;
	 *         non-modifiable.
	 */
	public List<CustomPacket> getCustomPackets() {

		return this.customPackets;
	}

	/**
	 * Sets the list of custom packet definitions.
	 * 
	 * @param customPackets
	 *            List of CustomPacket, or the empty list if none defined; may
	 *            not be null.
	 */
	public void setCustomPackets(final List<CustomPacket> customPackets) {

		if (customPackets == null) {
			throw new IllegalArgumentException(
					"custom packet list may not be null; set an empty list");
		}
		this.customPackets = customPackets;
	}
}
