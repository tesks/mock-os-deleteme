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

import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;

/**
 * This is the seed data class for the FrameBodyGenerator. It contains all the
 * data necessary to initialize the generator.
 * 
 */
public class FrameBodyGeneratorSeed implements ISeedData {
    private int    vcid;            // AOS 0-63 (6 bits), TM 3 bits (0-7)
    private int    scid;            // AOS 8 bits (0-255), TM 10 bits(0-1023)
    private int    startVcfc;       // AOS 24 bits (0-16777215), TM 8 bits (0-255)
    private String frameType;       // LONG_RS, etc
    private boolean packetSpanFrames;

    private ITransferFrameDictionary frameDict;
    private PacketHeaderGenerator    fillPacketGenerator;

    /**
     * Gets the configured VCID.
     * 
     * @return VCID
     */
    public int getVcid() {
        return vcid;
    }

    /**
     * Sets the configured VCID.
     * 
     * @param vcid VCID, 0-63
     */
    public void setVcid(final int vcid) {
        if (vcid < 0 || vcid > 63) {
            throw new IllegalArgumentException("VCID must be between 0 and 63");
        }
        this.vcid = vcid;
    }
    
    /**
     * Gets the configured Spacecraft ID.
     * 
     * @return Spacecraft ID
     */
    public int getScid() {
        return scid;
    }

    /**
     * Sets the Spacecraft ID.
     * 
     * @param scid
     *            Spacecraft ID, 0-511 (9 bits)
     */
    public void setScid(final int scid) {
        if (scid < 0 || scid > 255) {
            throw new IllegalArgumentException("SCID must be between 0 and 255");
        }
        this.scid = scid;
    }

    /**
     * Gets the configured Start VCFC.
     * 
     * @return Start VCFC
     */
    public int getStartVcfc() {
        return startVcfc;
    }

    /**
     * Sets the configured Start VCFC.
     * 
     * @param startVcfc
     *            Start VCFC, TM frames 8 bits (255), AOS frames 24 bits (16777215)
     *            TODO check after loading dictionary, rolling
     */
    public void setStartVcfc(final int startVcfc) {
        if (startVcfc < 0 || startVcfc > 16777215) {
            throw new IllegalArgumentException("startVcfc must be between 0 and 16777215");
        }
        this.startVcfc = startVcfc;
    }

    /**
     * Gets the configured frame type.
     * 
     * @return Frame type
     */
    public String getFrameType() {
        return frameType;
    }

    /**
     * Sets the configured frame type.
     * 
     * @param frameType Frame Type
     */
    public void setFrameType(final String frameType) {
        this.frameType = frameType;
    }

    /**
     * Gets the configured frame dictionary.
     * 
     * @return Frame Dictionary
     */
    public ITransferFrameDictionary getFrameDict() {
        return frameDict;
    }

    /**
     * Sets the frame dictionary.
     * 
     * @param frameDict
     *            Frame Dictionary
     */
    public void setFrameDict(final ITransferFrameDictionary frameDict) {
        this.frameDict = frameDict;
    }

    /**
     * Gets the fill packet generator.
     * 
     * @return Fill packet generator
     */
    public PacketHeaderGenerator getFillPacketGenerator() {
        return fillPacketGenerator;
    }

    /**
     * Sets the fill packet generator.
     * 
     * @param fillPacketGenerator
     *            Fill packet generator
     */
    public void setFillPacketGenerator(final PacketHeaderGenerator fillPacketGenerator) {
        this.fillPacketGenerator = fillPacketGenerator;
    }

    /**
     * Gets the packet span frames flag
     * @return Packet span frames flag
     */
    public boolean isPacketSpanFrames() {
        return packetSpanFrames;
    }

    /**
     * Sets the packet span frames flag
     * @param packetSpanFrames Packet span frames flag
     */
    public void setPacketSpanFrames(final boolean packetSpanFrames) {
        this.packetSpanFrames = packetSpanFrames;
    }
}
