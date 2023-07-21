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

import ammos.datagen.generators.seeds.ISeedData;

/**
 * This is the seed data class for the PduBodyGenerator. It contains all the
 * data necessary to initialize the generator.
 * 
 */
public class PduBodyGeneratorSeed implements ISeedData {
    
    private int     preferredLength;
    private int     entityIdLength;
    private int     sourceEntityId;
    private int     destEntityId;
    private int     transSeqLength;
    private boolean transmissionMode;
    private boolean segmentationControl;
    private boolean generateCrc;
    // error injection
    private boolean dropMetadata;
    private boolean dropData;
    private boolean dropEof;

    /**
     * Gets the configured preferred PDU length.
     * 
     * @return Preferred PDU length
     */
    public int getPreferredLength() {
        return preferredLength;
    }

    /**
     * Sets the configured preferred PDU length.
     * 
     * @param preferredLength Preferred PDU length, 1-65535 bytes
     */
    public void setPreferredLength(final int preferredLength) {
        if(preferredLength < 1 || preferredLength > 65535) {
            throw new IllegalArgumentException("preferredLength must be between 1 and 65535");
        }
        this.preferredLength = preferredLength;
    }

    /**
     * Gets the configured entity ID length.
     * 
     * @return Entity ID length
     */
    public int getEntityIdLength() {
        return entityIdLength;
    }

    /**
     * Sets the configured entity ID length
     * 
     * @param entityIdLength Entity ID length  1-8 bytes
     */
    public void setEntityIdLength(final int entityIdLength) {
        if (entityIdLength < 1 || entityIdLength > 8) {
            throw new IllegalArgumentException("entityIdLength must be between 1 and 8");
        }
        this.entityIdLength = entityIdLength;
    }

    /**
     * Gets the configured source entity ID.
     * 
     * @return Source entity ID
     */
    public int getSourceEntityId() {
        return sourceEntityId;
    }

    /**
     * Sets the configured source entity ID.
     * 
     * @param sourceEntityId
     *            Source entity ID, positive
     */
    public void setSourceEntityId(final int sourceEntityId) {
        if (sourceEntityId < 0) {
            throw new IllegalArgumentException("sourceEntityId must be positive");
        }
        this.sourceEntityId = sourceEntityId;
    }

    /**
     * Gets the configured destination entity ID.
     * 
     * @return Destination entity ID
     */
    public int getDestEntityId() {
        return destEntityId;
    }

    /**
     * Sets the configured destination entity ID.
     * 
     * @param destEntityId
     *            Destination entity ID, positive
     */
    public void setDestEntityId(final int destEntityId) {
        if (destEntityId < 0) {
            throw new IllegalArgumentException("destEntityId must be positive");
        }
        this.destEntityId = destEntityId;
    }

    /**
     * Gets the configured transaction sequence length.
     * 
     * @return transaction sequence length
     */
    public int getTransSeqLength() {
        return transSeqLength;
    }

    /**
     * Sets the configured transaction sequence length
     * 
     * @param transSeqLength transaction sequence length, 1-8 bytes
     */
    public void setTransSeqLength(final int transSeqLength) {
        if (transSeqLength < 1 || transSeqLength > 8) {
            throw new IllegalArgumentException("transSeqLength must be between 1 and 8");
        }
        this.transSeqLength = transSeqLength;
    }

    /**
     * Gets the configured transmission mode: true = acknowledged, false = unacknowledged
     * 
     * @return Transmission mode, boolean
     */
    public boolean isTransmissionMode() {
        return transmissionMode;
    }

    /**
     * Sets the configured transmission mode: true = acknowledged, false = unacknowledged
     * 
     * @param transmissionMode
     *            Transmission mode
     */
    public void setTransmissionMode(final boolean transmissionMode) {
        this.transmissionMode = transmissionMode;
    }

    /**
     * Gets the flag for segmentation control - boundaries, false = respected, true = not respected
     * 
     * @return Segmentation control, boolean
     */
    public boolean isSegmentationControl() {
        return segmentationControl;
    }

    /**
     * Sets the flag for segmentation control - boundaries, false = respected, true = not respected
     * 
     * @param segmentationControl
     *            Segmentation control
     */
    public void setSegmentationControl(final boolean segmentationControl) {
        this.segmentationControl = segmentationControl;
    }

    /**
     * Gets the flag for generate CRC: false = not present, true = present
     * 
     * @return generate CRC, boolean
     */
    public boolean isGenerateCrc() {
        return generateCrc;
    }

    /**
     * Sets the flag for generate CRC: false = not present, true = present
     * 
     * @param generateCrc
     *            generate CRC, boolean
     */
    public void setGenerateCrc(final boolean generateCrc) {
        this.generateCrc = generateCrc;
    }

    /**
     * Gets the flag for error generation - drop metadata PDU
     * 
     * @return drop metadata, boolean
     */
    public boolean isDropMetadata() {
        return dropMetadata;
    }

    /**
     * Sets the flag for error generation - drop metadata PDU
     * 
     * @param dropMetadata
     *            drop metadata
     */
    public void setDropMetadata(final boolean dropMetadata) {
        this.dropMetadata = dropMetadata;
    }

    /**
     * Gets the flag for error generation - drop data PDU
     * 
     * @return drop data, boolean
     */
    public boolean isDropData() {
        return dropData;
    }

    /**
     * Sets the flag for error generation - drop data PDU
     * 
     * @param dropData
     *            drop data
     */
    public void setDropData(final boolean dropData) {
        this.dropData = dropData;
    }

    /**
     * Gets the flag for error generation - drop EOF PDU
     * 
     * @return drop EOF, boolean
     */
    public boolean isDropEof() {
        return dropEof;
    }

    /**
     * Sets the flag for error generation - drop EOF PDU
     * 
     * @param dropEof
     *            drop EOF
     */
    public void setDropEof(final boolean dropEof) {
        this.dropEof = dropEof;
    }

}
