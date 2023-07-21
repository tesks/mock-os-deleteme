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
package ammos.datagen.evr.generators.seeds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.config.TraversalType;
import ammos.datagen.evr.config.EvrLevel;
import ammos.datagen.evr.config.EvrMissionConfiguration;
import ammos.datagen.evr.config.EvrRunConfiguration;
import ammos.datagen.generators.seeds.AbstractSeedMaker;
import ammos.datagen.generators.seeds.IPacketSeedMaker;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionary;

/**
 * This is a utility class that builds generator seeds necessary for generating
 * EVRs.
 * 
 *
 */
public class EvrSeedMaker extends AbstractSeedMaker implements IPacketSeedMaker {

    private final EvrMissionConfiguration missionConfig;
    private final EvrRunConfiguration runConfig;
    private final IEvrDictionary evrDict;

    /**
     * Constructor
     * 
     * @param missionConfig
     *            reference to a loaded EVR mission configuration
     * @param runConfig
     *            reference to a loaded EVR run configuration
     * @param dict
     *            reference to a loaded EVR dictionary
     */
    public EvrSeedMaker(final EvrMissionConfiguration missionConfig,
            final EvrRunConfiguration runConfig, final IEvrDictionary dict) {

        super(missionConfig, runConfig);

        this.missionConfig = missionConfig;
        this.runConfig = runConfig;
        this.evrDict = dict;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.seeds.AbstractSeedMaker#validate()
     */
    @Override
    public void validate() throws InvalidConfigurationException {

        super.validate();

        /*
         * For every EVR definition included in the set for data generation,
         * there must be an EVR level entry in the mission configuration.
         */
        final List<EvrLevel> levels = this.missionConfig.getEvrLevels();
        final SortedSet<String> levelStrings = new TreeSet<String>();
        for (final EvrLevel l : levels) {
            levelStrings.add(l.getLevelName());
        }
        for (final IEvrDefinition def : getFilteredEvrDefinitions()) {
            if (!levelStrings.contains(def.getLevel())) {
                throw new InvalidConfigurationException(
                        "EVR with level "
                                + def.getLevel()
                                + " included in the EVR set for data generation "
                                + "has no corresponding EVR level APID defined in the mission configuration");
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.seeds.IPacketSeedMaker#createPacketHeaderGeneratorSeeds()
     */
    @Override
    public Map<Integer, PacketHeaderGeneratorSeed> createPacketHeaderGeneratorSeeds() {

        final Map<Integer, PacketHeaderGeneratorSeed> seeds = new HashMap<Integer, PacketHeaderGeneratorSeed>();
        final List<EvrLevel> levels = this.missionConfig.getEvrLevels();
        for (final EvrLevel l : levels) {
            final int apid = l.getLevelApid();
            if (seeds.get(apid) == null) {
                final PacketHeaderGeneratorSeed pseed = new PacketHeaderGeneratorSeed();
                pseed.setApid(apid);
                seeds.put(apid, pseed);
            }
        }
        return seeds;
    }

    /**
     * Creates an ChannelBodyGeneratorSeed object from current configuration and
     * EVR dictionary content.
     * 
     * @return an EvrBodyGenerator seed object
     */
    public EvrBodyGeneratorSeed createEvrBodyGeneratorSeed() {

        final EvrBodyGeneratorSeed evrSeed = new EvrBodyGeneratorSeed();

        evrSeed.setEvrLevels(this.missionConfig.getEvrLevels());
        evrSeed.setEvrDefs(getFilteredEvrDefinitions());

        evrSeed.setIncludeInvalidEvrs(this.runConfig.getBooleanProperty(
                EvrRunConfiguration.INCLUDE_INVALID_EVRS, false));
        evrSeed.setIncludeInvalidIds(this.runConfig.getBooleanProperty(
                EvrRunConfiguration.INCLUDE_INVALID_IDS, false));
        evrSeed.setInvalidIdPercent(this.runConfig.getFloatProperty(
                EvrRunConfiguration.INVALID_ID_PERCENT, (float) 0.0));
        evrSeed.setInvalidIds(this.runConfig.getInvalidIds());
        evrSeed.setTaskName(this.runConfig.getStringProperty(
                EvrRunConfiguration.EVR_TASK_NAME, "TASK"));
        evrSeed.setStackDepth(this.runConfig.getIntProperty(
                EvrRunConfiguration.EVR_STACK_DEPTH, 0));
        evrSeed.setTraversalType(this.runConfig.getTraversalTypeProperty(
                EvrRunConfiguration.EVR_TRAVERSAL_TYPE,
                TraversalType.SEQUENTIAL));

        makeEnumSeeds(evrSeed, this.evrDict.getEnumDefinitions());
        makeIntegerSeeds(evrSeed);
        makeUnsignedSeeds(evrSeed);
        makeFloatSeeds(evrSeed);
        makeOpcodeSeed(evrSeed);
        makeSeqIdSeed(evrSeed);
        makeStringSeed(evrSeed);

        return evrSeed;
    }

    /**
     * Builds the opcode generator seed and attaches it to the EVR generator
     * seed.
     * 
     * @param evrSeed
     *            ChannelBodyGeneratorSeed to attach opcode seed to
     */
    private void makeOpcodeSeed(final EvrBodyGeneratorSeed evrSeed) {

        final OpcodeGeneratorSeed opcodeSeed = new OpcodeGeneratorSeed();
        opcodeSeed.setInvalidOpcodes(this.runConfig.getInvalidOpcodes());
        opcodeSeed.setValidOpcodes(this.runConfig.getValidOpcodes());
        opcodeSeed.setUseInvalid(this.runConfig.getBooleanProperty(
                EvrRunConfiguration.INCLUDE_INVALID_OPCODES, false));
        opcodeSeed.setInvalidPercent(this.runConfig.getFloatProperty(
                EvrRunConfiguration.INVALID_OPCODE_PERCENT, (float) 0.0));
        evrSeed.setOpcodeSeed(opcodeSeed);
    }

    /**
     * Builds the SEQID generator seed and attaches it to the EVR generator
     * seed.
     * 
     * @param evrSeed
     *            ChannelBodyGeneratorSeed to attach SeqID seed to
     */
    private void makeSeqIdSeed(final EvrBodyGeneratorSeed evrSeed) {

        final SeqIdGeneratorSeed seqIdSeed = new SeqIdGeneratorSeed();
        seqIdSeed.setInvalidSeqIds(this.runConfig.getInvalidSeqIds());
        seqIdSeed.setValidSeqIds(this.runConfig.getValidSeqIds());
        seqIdSeed.setUseInvalid(this.runConfig.getBooleanProperty(
                EvrRunConfiguration.INCLUDE_INVALID_SEQIDS, false));
        seqIdSeed.setInvalidPercent(this.runConfig.getFloatProperty(
                EvrRunConfiguration.INVALID_SEQID_PERCENT, (float) 0.0));
        evrSeed.setSeqIdSeed(seqIdSeed);
    }

    /**
     * Filters the list of EVR definitions in the dictionary according to the
     * EVR run configuration and returns the filtered list.
     * 
     * @return List of IEvrDefinition, or the empty list if all definitions are
     *         filtered
     */
    private List<IEvrDefinition> getFilteredEvrDefinitions() {

        final List<IEvrDefinition> origDefs = this.evrDict.getEvrDefinitions();
        final List<IEvrDefinition> filteredDefs = new ArrayList<IEvrDefinition>(
                1024);
        final String levelPattern = this.runConfig.getStringProperty(
                EvrRunConfiguration.LEVEL_PATTERN, null);
        final String modulePattern = this.runConfig.getStringProperty(
                EvrRunConfiguration.MODULE_PATTERN, null);
        final String namePattern = this.runConfig.getStringProperty(
                EvrRunConfiguration.NAME_PATTERN, null);
        final String subsystemPattern = this.runConfig.getStringProperty(
                EvrRunConfiguration.SUBSYSTEM_PATTERN, null);
        final String categoryPattern = this.runConfig.getStringProperty(
                EvrRunConfiguration.OPSCAT_PATTERN, null);

        for (final IEvrDefinition def : origDefs) {
            boolean match = levelPattern == null && namePattern == null
                    && subsystemPattern == null && modulePattern == null
                    && categoryPattern == null;

            if (def.getLevel() != null && levelPattern != null) {
                match = match || def.getLevel().matches(levelPattern);
            }
            if (def.getName() != null && namePattern != null) {
                match = match || def.getName().matches(namePattern);
            }
            /* MHT - MPCS-7033 - 11/4/15 - New call to categories. */
            if (def.getCategory(IEvrDefinition.MODULE) != null
                    && modulePattern != null) {
                match = match
                        || def.getCategory(IEvrDefinition.MODULE).matches(
                                modulePattern);
            }
            if (def.getCategory(IEvrDefinition.SUBSYSTEM) != null
                    && subsystemPattern != null) {
                match = match
                        || def.getCategory(IEvrDefinition.SUBSYSTEM).matches(
                                subsystemPattern);
            }
            if (def.getCategory(IEvrDefinition.OPS_CAT) != null
                    && categoryPattern != null) {
                match = match
                        || def.getCategory(IEvrDefinition.OPS_CAT).matches(
                                categoryPattern);
            }
            if (match) {
                filteredDefs.add(def);
            }
        }

        return filteredDefs;
    }
}
