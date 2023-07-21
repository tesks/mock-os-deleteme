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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import ammos.datagen.channel.config.ApidRotationCount;
import ammos.datagen.channel.config.ChannelMissionConfiguration;
import ammos.datagen.channel.config.ChannelPacketMode;
import ammos.datagen.channel.config.ChannelPacketType;
import ammos.datagen.channel.config.ChannelRunConfiguration;
import ammos.datagen.channel.config.CustomPacket;
import ammos.datagen.config.IMissionConfiguration;
import ammos.datagen.config.IRunConfiguration;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.seeds.AbstractSeedMaker;
import ammos.datagen.generators.seeds.BooleanGeneratorSeed;
import ammos.datagen.generators.seeds.IPacketSeedMaker;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.shared.annotation.ToDo;

/**
 * This is a utility class that builds generator seeds necessary for generating
 * channel packets. It implements IPacketSeedMaker because it will generate the
 * packet header and packet SCLK generator seeds for the channel generator. It
 * also builds the other seeds necessary for channel generation.
 * 
 *
 */
public class ChannelSeedMaker extends AbstractSeedMaker implements
        IPacketSeedMaker {

    private static final String FOUND_APID = "Found APID ";
    private static final String IN_THE_MISSION_CONFIG = "in the mission configuration";

    private final ChannelMissionConfiguration missionConfig;
    private final ChannelRunConfiguration runConfig;
    private final IChannelDictionary channelDict;

    /**
     * Constructor
     * 
     * @param missionConfig
     *            reference to a loaded channel mission configuration
     * @param runConfig
     *            reference to a loaded channel run configuration
     * @param dict
     *            reference to a loaded channel dictionary
     */
    public ChannelSeedMaker(final ChannelMissionConfiguration missionConfig,
            final ChannelRunConfiguration runConfig,
            final IChannelDictionary dict) {

        super(missionConfig, runConfig);
        this.missionConfig = missionConfig;
        this.runConfig = runConfig;
        this.channelDict = dict;
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
         * Maximum channel packet size may not exceed mission maximum packet
         * size.
         */
        final int missionMax = this.missionConfig.getIntProperty(
                IMissionConfiguration.PACKET_MAX_LEN, 65535);
        final int channelMax = this.runConfig.getIntProperty(
                ChannelRunConfiguration.MAX_CHANNEL_PACKET_SIZE, 65535);
        if (channelMax > missionMax) {
            throw new InvalidConfigurationException(
                    "Configured maximum channel packet size " + channelMax
                            + " exceeds mission maximum packet size "
                            + missionMax);
        }

        /*
         * Create a simple string set of all the non-derived channels IDs in the
         * dictionary.
         */
        final SortedSet<String> allChans = new TreeSet<String>();
        for (final IChannelDefinition def : this.channelDict
                .getChannelDefinitions()) {
            if (!def.isDerived()) {
                allChans.add(def.getId());
            }
        }

        /*
         * These are the packet types defined in the mission configuration file.
         * Put the APIDs into a TreeMap.
         */
        final List<ChannelPacketType> packets = this.missionConfig
                .getChannelPacketTypes();
        final SortedMap<Integer, ChannelPacketType> missionApids = new TreeMap<Integer, ChannelPacketType>();
        for (final ChannelPacketType type : packets) {
            missionApids.put(type.getApid(), type);
        }

        /*
         * Get the map of channels per APID.
         */
        final Map<Integer, List<String>> channelsPerApid = this.runConfig
                .getChannelsPerApid();

        /*
         * Every packet APID in the list of packet APID rotation counts must
         * match one of the packet types defined in the mission configuration,
         * and at this time that packet type must be pre-channelized.
         */
        @ToDo("Examine this block when decom packet support added")
        final List<ApidRotationCount> apidPairs = this.runConfig
                .getPacketApidCounts();
        for (final ApidRotationCount count : apidPairs) {
            final ChannelPacketType type = missionApids.get(count.getApid());
            if (type == null) {
                throw new InvalidConfigurationException(FOUND_APID
                        + count.getApid() + " in the packet APID counts "
                        + "but it is not listed as an APID "
                        + IN_THE_MISSION_CONFIG);
            }
            if (!type.isPrechannelized()) {
                throw new InvalidConfigurationException(FOUND_APID
                        + count.getApid() + " in the packet APID counts "
                        + "that is not defined as pre-channelized "
                        + IN_THE_MISSION_CONFIG);
            }
            /*
             * If in BY_APID mode, then there must be channels defined for each
             * APID for which we will be producing any packets.
             */
            if (this.runConfig.getPacketMode() == ChannelPacketMode.BY_APID
                    && count.getCount() != 0
                    && channelsPerApid.get(count.getApid()) == null) {
                throw new InvalidConfigurationException(
                        FOUND_APID
                                + count.getApid()
                                + " in the packet APID counts "
                                + "that has no channels defined in the channels-per-APID configuration");

            }
        }

        /*
         * Every packet APID in the custom packet list must match one of the
         * packet types defined in the mission configuration, and at this time
         * that packet type must be pre-channelized.
         */
        @ToDo("Examine this block when decom packet support added")
        final List<CustomPacket> customPackets = this.runConfig
                .getCustomPackets();
        for (final CustomPacket p : customPackets) {
            final ChannelPacketType type = missionApids.get(p.getApid());
            if (type == null) {
                throw new InvalidConfigurationException(FOUND_APID
                        + p.getApid() + " in the custom packet list "
                        + "but it is not listed as an APID "
                        + IN_THE_MISSION_CONFIG);
            }
            if (!type.isPrechannelized()) {
                throw new InvalidConfigurationException(FOUND_APID
                        + p.getApid() + " in the custom packet list "
                        + "that is not defined as pre-channelized "
                        + IN_THE_MISSION_CONFIG);
            }
            /*
             * Every channel listed in the custom packet must match a
             * non-derived channel in the dictionary.
             */
            for (final String cid : p.getChannelIds()) {
                if (!allChans.contains(cid)) {
                    throw new InvalidConfigurationException(
                            "Found channel "
                                    + cid
                                    + " in custom packet definition that is not defined as a non-derived channel in the dictionary");
                }
            }
        }

        /*
         * Every packet APID in the channels per APID list must match one of the
         * packet types defined in the mission configuration, and at this time
         * that packet type must be pre-channelized.
         * 
         * @ToDo("Examine this block when decom packet support added")
         */
        for (final Integer a : channelsPerApid.keySet()) {
            final ChannelPacketType type = missionApids.get(a);
            if (type == null) {
                throw new InvalidConfigurationException(FOUND_APID + a
                        + " in the channels-per-APID list "
                        + "but it is not listed as an APID "
                        + IN_THE_MISSION_CONFIG);
            }
            if (!type.isPrechannelized()) {
                throw new InvalidConfigurationException(FOUND_APID + a
                        + " in the channels-per-APID list "
                        + "that is not defined as pre-channelized "
                        + IN_THE_MISSION_CONFIG);
            }
            /*
             * Every channel listed in the APID's channel list must match a
             * non-derived channel in the dictionary.
             */
            for (final String cid : channelsPerApid.get(a)) {
                if (!allChans.contains(cid)) {
                    throw new InvalidConfigurationException(
                            "Found channel "
                                    + cid
                                    + " in channels-per-apid definition that is not defined as a non-derived channel in the dictionary");
                }
            }
        }

        /*
         * If configured to generate invalid indices, Every index in the list of
         * invalid channel indices must NOT be a valid index in the channel
         * dictionary for a non-derived channel.
         */
        if (this.runConfig.getBooleanProperty(
                ChannelRunConfiguration.INCLUDE_INVALID_INDICES, false)) {

            /*
             * Put the list of invalid indices into a tree map.
             */
            final List<Integer> indices = this.runConfig.getInvalidIndices();
            final SortedSet<Integer> sortedIndices = new TreeSet<Integer>(
                    indices);
            final List<IChannelDefinition> defs = this.channelDict
                    .getChannelDefinitions();
            /*
             * Look for these indices in the channel dictionary by checking each
             * definition.
             */
            for (final IChannelDefinition def : defs) {
                /*
                 * Derived channels do not have an index. Skip them.
                 */
                if (def.isDerived()) {
                    continue;
                }
                if (sortedIndices.contains(def.getIndex())) {
                    throw new InvalidConfigurationException(
                            "The configuration defines invalid index "
                                    + def.getIndex()
                                    + " but it is a valid index in the channel dictionary");
                }
            }
        }

        if (getFilteredChannelDefinitions().isEmpty()) {
            throw new InvalidConfigurationException(
                    "No channels meet all the filtering criteria in the current configuration");
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
        final List<ChannelPacketType> packets = this.missionConfig
                .getChannelPacketTypes();
        for (final ChannelPacketType l : packets) {
            final int apid = l.getApid();
            final PacketHeaderGeneratorSeed pseed = new PacketHeaderGeneratorSeed();
            pseed.setApid(apid);
            seeds.put(apid, pseed);
        }
        return seeds;
    }

    /**
     * Creates a ChannelBodyGeneratorSeed object from current configuration and
     * channel dictionary content.
     * 
     * @return a ChannelBodyGenerator seed object
     */
    public ChannelBodyGeneratorSeed createChannelBodyGeneratorSeed() {

        final ChannelBodyGeneratorSeed chanSeed = new ChannelBodyGeneratorSeed();
        chanSeed.setChannelDefs(getFilteredChannelDefinitions());
        if (chanSeed.getChannelDefs().isEmpty()) {
            throw new InvalidSeedDataException(
                    "No channels meet all the filtering criteria in the current configuration");
        }

        chanSeed.setIncludeInvalidPackets(this.runConfig.getBooleanProperty(
                ChannelRunConfiguration.INCLUDE_INVALID_PACKETS, false));
        chanSeed.setIncludeInvalidIndices(this.runConfig.getBooleanProperty(
                ChannelRunConfiguration.INCLUDE_INVALID_INDICES, false));
        chanSeed.setInvalidIndexPercent(this.runConfig.getFloatProperty(
                ChannelRunConfiguration.INVALID_INDEX_PERCENT, (float) 0.0));
        chanSeed.setTraversalType(this.runConfig.getTraversalTypeProperty(
                ChannelRunConfiguration.CHANNEL_TRAVERSAL_TYPE,
                TraversalType.SEQUENTIAL));
        chanSeed.setMinSamples(this.runConfig.getIntProperty(
                ChannelRunConfiguration.MIN_PACKET_SAMPLES, 1));
        chanSeed.setMaxSamples(this.runConfig.getIntProperty(
                ChannelRunConfiguration.MAX_PACKET_SAMPLES, 1));
        chanSeed.setInvalidIndices(this.runConfig.getInvalidIndices());
        chanSeed.setChannelsPerApid(this.runConfig.getChannelsPerApid());
        chanSeed.setCustomPackets(this.runConfig.getCustomPackets());
        setPacketApidCounts(chanSeed);
        makeEnumSeeds(chanSeed, this.channelDict.getEnumDefinitions());
        makeIntegerSeeds(chanSeed);
        makeUnsignedSeeds(chanSeed);
        makeFloatSeeds(chanSeed);
        makeBooleanSeed(chanSeed);
        makeStringSeed(chanSeed);

        return chanSeed;
    }

    /**
     * Filters the list of channel definitions in the dictionary according to
     * the channel run configuration and returns the filtered list.
     * 
     * @return List of IChannelDefinition, or the empty list if all definitions
     *         are filtered
     */
    private List<IChannelDefinition> getFilteredChannelDefinitions() {

        /*
         * Start with the list of all channel definitions in the dictionary.
         */
        final List<IChannelDefinition> origDefs = this.channelDict
                .getChannelDefinitions();
        /*
         * Result list we will add to.
         */
        final List<IChannelDefinition> filteredDefs = new ArrayList<IChannelDefinition>(
                1024);

        /**
         * In RANDOM generation mode, the list of channel definitions is
         * filtered using a series of regular expressions.
         */
        if (this.runConfig.getPacketMode() == ChannelPacketMode.RANDOM) {
            /*
             * Extract the configuration values used to filter the dictionary.
             * These are regexp patterns.
             */
            final String idPattern = this.runConfig.getStringProperty(
                    ChannelRunConfiguration.ID_PATTERN, null);
            final String modulePattern = this.runConfig.getStringProperty(
                    ChannelRunConfiguration.MODULE_PATTERN, null);
            final String namePattern = this.runConfig.getStringProperty(
                    ChannelRunConfiguration.NAME_PATTERN, null);
            final String subsystemPattern = this.runConfig.getStringProperty(
                    ChannelRunConfiguration.SUBSYSTEM_PATTERN, null);
            final String categoryPattern = this.runConfig.getStringProperty(
                    ChannelRunConfiguration.OPSCAT_PATTERN, null);

            /* MPCS-6333 - 7/1/14. Add filter by channel type. */
            final List<ChannelType> selectedTypes = this.runConfig
                    .getChannelTypes();

            /*
             * Loop through the original list of definitions and find those that
             * match the patterns in the configuration.
             */
            for (final IChannelDefinition def : origDefs) {
                /*
                 * Never want to generate derived channels.
                 */
                if (def.isDerived()) {
                    continue;
                }

                /*
                 * Channels pass if they match any one of the patterns, or if
                 * none of the patterns are set (no filter).
                 */
                boolean match = idPattern == null && namePattern == null
                        && modulePattern == null && subsystemPattern == null
                        && categoryPattern == null;

                if (def.getId() != null && idPattern != null) {
                    match = match || def.getId().matches(idPattern);
                }

                if (def.getName() != null && namePattern != null) {
                    match = match || def.getName().matches(namePattern);
                }
                /* MHT - MPCS-7033 - 11/5/15 - new call to category */
                if (def.getCategory(IChannelDefinition.MODULE) != null
                        && modulePattern != null) {
                    match = match
                            || def.getCategory(IChannelDefinition.MODULE)
                                    .matches(modulePattern);
                }
                if (def.getCategory(IChannelDefinition.SUBSYSTEM) != null
                        && subsystemPattern != null) {
                    match = match
                            || def.getCategory(IChannelDefinition.SUBSYSTEM)
                                    .matches(subsystemPattern);
                }
                if (def.getCategory(IChannelDefinition.OPS_CAT) != null
                        && categoryPattern != null) {
                    match = match
                            || def.getCategory(IChannelDefinition.OPS_CAT)
                                    .matches(categoryPattern);
                }

                /*
                 * MPCS-6333 - 7/1/14. Add filter by channel type. This
                 * filter is an AND. If the channel does not match, it is
                 * discarded, even if it matches the other filters.
                 */
                match = match
                        && (selectedTypes.isEmpty() || selectedTypes
                                .contains(def.getChannelType()));

                /*
                 * Got a match so add to the result list.
                 */
                if (match) {
                    filteredDefs.add(def);
                }
            }
        } else if (this.runConfig.getPacketMode() == ChannelPacketMode.BY_APID) {
            /*
             * In BY_APID mode, we want all the channel definitions for channels
             * that are listed as belonging to some packet apid. Create a list
             * of all the channels desired for the various APIDs.
             */
            final Set<String> desiredChans = new TreeSet<String>();
            final Map<Integer, List<String>> apidChannels = this.runConfig
                    .getChannelsPerApid();
            for (final List<String> channelList : apidChannels.values()) {
                desiredChans.addAll(channelList);
            }

            /*
             * Loop through the original list of definitions and find those that
             * match one in the desired channels list.
             */
            for (final IChannelDefinition def : origDefs) {
                if (desiredChans.contains(def.getId())) {
                    filteredDefs.add(def);
                }
            }

        } else {
            /*
             * In CUSTOM mode, we want all the channel definitions for channel
             * IDs listed as belonging to any custom packet. Make a desired
             * channel list by looping through the custom packet definitions.
             */
            final Set<String> desiredChans = new TreeSet<String>();
            final List<CustomPacket> packets = this.runConfig
                    .getCustomPackets();
            for (final CustomPacket p : packets) {
                desiredChans.addAll(p.getChannelIds());
            }

            /*
             * Loop through the original list of definitions and find those that
             * match one in the desired channels list.
             */
            for (final IChannelDefinition def : origDefs) {
                if (desiredChans.contains(def.getId())) {
                    filteredDefs.add(def);
                }
            }

        }

        return filteredDefs;
    }

    /**
     * Creates a boolean generator seed from the current configuration and
     * attaches it to the channel body generator seed.
     * 
     * @param parentSeed
     *            the parent ChannelBodyGeneratorSeed to attach the boolean
     *            generator seed to
     */
    private void makeBooleanSeed(final ChannelBodyGeneratorSeed parentSeed) {

        final BooleanGeneratorSeed boolSeed = new BooleanGeneratorSeed();
        final TraversalType traverse = this.runConfig.getTraversalTypeProperty(
                IRunConfiguration.BOOL_TRAVERSAL_TYPE, TraversalType.RANDOM);
        final boolean includeNonBoolean = this.runConfig.getBooleanProperty(
                IRunConfiguration.INCLUDE_NON_ZERO_ONE_BOOL, false);
        boolSeed.setTraversalType(traverse);
        boolSeed.setIncludeExtraValue(includeNonBoolean);
        parentSeed.setBooleanSeed(boolSeed);
    }

    /**
     * Constructs the packet APID counts (rotation count by APID) and attaches
     * them to the channel body generator seed.
     * 
     * @param parentSeed
     *            the parent ChannelBodyGeneratorSeed to attach the packet APID
     *            counts to
     */
    private void setPacketApidCounts(final ChannelBodyGeneratorSeed parentSeed) {

        /*
         * In RANDOM and BY APID channel generation modes, the packet APID
         * counts may be specified in the configuration, as an option, so try
         * getting the counts from the run configuration first.
         */
        List<ApidRotationCount> apidCounts = this.runConfig
                .getPacketApidCounts();

        if (apidCounts.isEmpty()) {
            /*
             * There weren't any counts in the configuration.
             */
            apidCounts = new LinkedList<ApidRotationCount>();

            if (this.runConfig.getPacketMode() == ChannelPacketMode.CUSTOM) {
                /*
                 * We're in custom mode. So we have a list of specific packets
                 * to generate. Build the list of rotation counts from the APIDs
                 * in the list of custom packets, with the count for each being
                 * 1.
                 */
                final List<CustomPacket> customPackets = this.runConfig
                        .getCustomPackets();
                for (final CustomPacket p : customPackets) {
                    apidCounts.add(new ApidRotationCount(p.getApid(), 1));
                }
            } else {
                /*
                 * Did not have custom packets, and no configured counts. The
                 * packet APID rotation defaults to rotation through the
                 * pre-channelized packet APIDs in the mission configuration,
                 * with a count of 1 for each.
                 */
                @ToDo("Examine this block when decom packet support added")
                final List<ChannelPacketType> types = this.missionConfig
                        .getChannelPacketTypes();
                for (final ChannelPacketType type : types) {
                    if (type.isPrechannelized()) {
                        apidCounts
                                .add(new ApidRotationCount(type.getApid(), 1));
                    }
                }
            }
        }
        /*
         * Set the packet APID counts into the parent seed.
         */
        parentSeed.setApidCounts(apidCounts);
    }

}
