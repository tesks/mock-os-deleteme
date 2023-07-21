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
package ammos.datagen.channel.generators;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.util.TruthFile;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.types.Pair;

/**
 * This is the data generator for channel packet bodies when configured to
 * generate pre-channelized packets using random channel selection. It creates
 * the portion of an pre-channelized packet from the end of the secondary header
 * onwards. It must be supplied a seed object that tells it which channels and
 * APIDs to generate.
 * 
 *
 */
public class RandomChannelBodyGenerator extends AbstractChannelBodyGenerator
		implements ISeededGenerator {

	private int chanDefIterator;

	/**
	 * Basic constructor.
	 */
	public RandomChannelBodyGenerator() {

		super();
	}

	/**
	 * Constructor that sets the truth file writer
	 * 
	 * @param truthFile
	 *            TruthFile object for writing truth data to
	 */
	public RandomChannelBodyGenerator(final TruthFile truthFile) {

		super(truthFile);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed) {

		super.setSeedData(seed);
		this.chanDefIterator = 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#reset()
	 */
	@Override
	public void reset() {

		super.reset();
		this.chanDefIterator = 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Pair<Integer, byte[]>, where the
	 * integer is the packet APID and the byte[] is the channel packet body.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null || this.chanDefs.isEmpty()) {
			throw new IllegalStateException(
					"channel body generator is not seeded or contains no channel definitions");
		}

		return new Pair<Integer, byte[]>(getNextApid(),
				getChannelBody(getSequentialDefs()));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Pair<Integer, byte[]>, where the
	 * integer is the packet APID and the byte[] is the channel packet body.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public Object getRandom() {

		if (this.seedData == null || this.chanDefs.isEmpty()) {
			throw new IllegalStateException(
					"channel body generator is not seeded or contains no channel definitions");
		}

		return new Pair<Integer, byte[]>(getNextApid(),
				getChannelBody(getRandomDefs()));
	}

	/**
	 * Selects the next N channel definitions from the overall list of available
	 * channel definitions, where N is between the configured minimum and
	 * maximum number of channel samples per packet. Ensures that no channel is
	 * selected more than once.
	 * 
	 * @return List of selected IChannelDefinition objects
	 */
	private List<IChannelDefinition> getSequentialDefs() {

		/*
		 * Number of samples must be between configured min and max.
		 */
		int numSamples = this.randGenerator.nextInt(this.seedData
				.getMaxSamples() - this.seedData.getMinSamples() + 1)
				+ this.seedData.getMinSamples();

		/*
		 * BUT, the number cannot be greater than the total number of channel
		 * definitions we have.
		 */
		numSamples = Math.min(numSamples, this.chanDefs.size());
		final List<IChannelDefinition> selectedChans = new LinkedList<>();

		/*
		 * Start where we left off the last time in the list of channel
		 * definitions from the seed, and grab N channel definitions from there.
		 */
		for (int i = 0; i < numSamples; i++) {
			selectedChans.add(this.chanDefs.get(this.chanDefIterator++));
			if (this.chanDefIterator == this.chanDefs.size()) {
				this.chanDefIterator = 0;
			}
		}
		return selectedChans;
	}

	/**
	 * Selects random N channel definitions from the overall list of available
	 * channel definitions, where N is between the configured minimum and
	 * maximum number of channel samples per packet. Ensures that no channel is
	 * selected more than once.
	 * 
	 * @return List of selected IChannelDefinition objects
	 */
	private List<IChannelDefinition> getRandomDefs() {

		/*
		 * Number of samples must be between configured min and max.
		 */
		int numSamples = this.randGenerator.nextInt(this.seedData
				.getMaxSamples() - this.seedData.getMinSamples() + 1)
				+ this.seedData.getMinSamples();

		/*
		 * BUT, the number cannot be greater than the total number of channel
		 * definitions we have.
		 */
		numSamples = Math.min(numSamples, this.chanDefs.size());

		final List<IChannelDefinition> selectedChans = new LinkedList<>();
		final Set<String> channelIds = new HashSet<>();

		/*
		 * Select N random definitions from the list of channel definitions
		 * taken from the seed data, taking care not to generate duplicates.
		 */
		for (int i = 0; i < numSamples; i++) {
			int randomDef = this.randGenerator.nextInt(this.chanDefs.size());
			while (channelIds.contains(this.chanDefs.get(randomDef).getId())) {
				randomDef = this.randGenerator.nextInt(this.chanDefs.size());
			}
			final IChannelDefinition def = this.chanDefs.get(randomDef);
			selectedChans.add(def);
			channelIds.add(def.getId());
		}
		return selectedChans;
	}
}
