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
package ammos.datagen.frame.generators.seeds;

import java.util.HashMap;
import java.util.Map;

import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.frame.config.FrameRunConfiguration;
import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.AbstractSeedMaker;
import ammos.datagen.generators.seeds.IPacketSeedMaker;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;

/**
 * This is a utility class that builds generator seeds necessary for generating Frames
 * 
 *
 */
public class FrameSeedMaker extends AbstractSeedMaker implements IPacketSeedMaker {	
    private final GeneralMissionConfiguration missionConfig;
    private final FrameRunConfiguration       runConfig;
    private final ITransferFrameDictionary          frameDict;

    /**
     * Constructor
     * 
     * @param missionConfig
     *            reference to a loaded general mission configuration
     * @param runConfig
     *            reference to a loaded Frame run configuration
     * @param dict
     *            reference to a loaded transfer frame dictionary
     * 
     */
    public FrameSeedMaker(final GeneralMissionConfiguration missionConfig, final FrameRunConfiguration runConfig,
            final ITransferFrameDictionary dict) {
        super(missionConfig, runConfig);
        this.missionConfig = missionConfig;
        this.runConfig = runConfig;
        this.frameDict = dict;
	}
	
    /**
     * Creates a FrameBodyGeneratorSeed object from current configuration
     * 
     * @param fillGen
     *            reference to a fill packet generator
     * 
     * @return a FrameBodyGeneratorSeed seed object
     */
    public FrameBodyGeneratorSeed createFrameBodySeed(final PacketHeaderGenerator fillGen) {
		final FrameBodyGeneratorSeed bodySeed = new FrameBodyGeneratorSeed();
		
        // read run config
        bodySeed.setVcid(runConfig.getIntProperty(FrameRunConfiguration.VCID, 1));
        bodySeed.setScid(runConfig.getIntProperty(FrameRunConfiguration.SCID, 1));
        bodySeed.setStartVcfc(runConfig.getIntProperty(FrameRunConfiguration.START_VCFC, 0));
        bodySeed.setFrameType(runConfig.getStringProperty(FrameRunConfiguration.FRAME_TYPE, null));
        bodySeed.setFrameDict(frameDict);
        bodySeed.setPacketSpanFrames(runConfig.getBooleanProperty(FrameRunConfiguration.PACKET_SPAN_FRAMES, false));
        bodySeed.setFillPacketGenerator(fillGen);
		
		return bodySeed;
	}

	@Override
	public Map<Integer, PacketHeaderGeneratorSeed> createPacketHeaderGeneratorSeeds() {
		return new HashMap<>();
	}
}
