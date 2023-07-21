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
package ammos.datagen.pdu.generators.seeds;

import java.util.HashMap;
import java.util.Map;

import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.generators.seeds.AbstractSeedMaker;
import ammos.datagen.generators.seeds.IPacketSeedMaker;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import ammos.datagen.pdu.config.PduRunConfiguration;

/**
 * This is a utility class that builds generator seeds necessary for generating PDUs
 * 
 *
 */
public class PduSeedMaker extends AbstractSeedMaker implements IPacketSeedMaker {
    private final GeneralMissionConfiguration missionConfig;
    private final PduRunConfiguration         runConfig;

    /**
     * Constructor
     * 
     * @param missionConfig
     *            reference to a loaded general mission configuration
     * @param runConfig
     *            reference to a loaded PDU run configuration
     */
    public PduSeedMaker(final GeneralMissionConfiguration missionConfig, final PduRunConfiguration runConfig) {
        super(missionConfig, runConfig);
        this.missionConfig = missionConfig;
        this.runConfig = runConfig;
    }

    @Override
    public Map<Integer, PacketHeaderGeneratorSeed> createPacketHeaderGeneratorSeeds() {
        final Map<Integer, PacketHeaderGeneratorSeed> seeds = new HashMap<Integer, PacketHeaderGeneratorSeed>();

        /*
         * There is only one configured APID in the run configuration, so we
         * need only one packet header seed.
         */
        final int apid = this.runConfig.getIntProperty(PduRunConfiguration.PACKET_APID, 0);

        final PacketHeaderGeneratorSeed pseed = new PacketHeaderGeneratorSeed();
        pseed.setApid(apid);
        seeds.put(apid, pseed);

        return seeds;
    }

    /**
     * Creates a PduBodyGeneratorSeed object from current configuration
     * 
     * @return a PduBodyGeneratorSeed seed object
     */
    public PduBodyGeneratorSeed createPduBodyGeneratorSeed() {

        final PduBodyGeneratorSeed pduSeed = new PduBodyGeneratorSeed();

        pduSeed.setPreferredLength(runConfig.getIntProperty(PduRunConfiguration.PREF_PDU_LENGTH, 65535));
        pduSeed.setEntityIdLength(runConfig.getIntProperty(PduRunConfiguration.ENTITY_ID_LENGTH, 1));
        pduSeed.setSourceEntityId(runConfig.getIntProperty(PduRunConfiguration.SOURCE_ENTITY_ID, 1));
        pduSeed.setDestEntityId(runConfig.getIntProperty(PduRunConfiguration.DEST_ENTITY_ID, 2));
        pduSeed.setTransSeqLength(runConfig.getIntProperty(PduRunConfiguration.TRANS_SEQ_LENGTH, 1));
        pduSeed.setTransmissionMode(runConfig.getBooleanProperty(PduRunConfiguration.TRANSMISSION_MODE, false));
        pduSeed.setSegmentationControl(runConfig.getBooleanProperty(PduRunConfiguration.SEG_CONTROL, false));
        pduSeed.setGenerateCrc(runConfig.getBooleanProperty(PduRunConfiguration.GENERATE_CRC, false));
        pduSeed.setDropMetadata(runConfig.getBooleanProperty(PduRunConfiguration.DROP_META, false));
        pduSeed.setDropData(runConfig.getBooleanProperty(PduRunConfiguration.DROP_DATA, false));
        pduSeed.setDropEof(runConfig.getBooleanProperty(PduRunConfiguration.DROP_EOF, false));

        return pduSeed;
    }

}
