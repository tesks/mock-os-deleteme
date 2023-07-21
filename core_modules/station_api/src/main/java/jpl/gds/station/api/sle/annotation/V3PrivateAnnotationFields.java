/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.station.api.sle.annotation;

/**
 * DSN Version 3 private annotation fields. See DSN doc 820-013 0243-Telecomm Rev B, Table A-1
 *
 */
public enum V3PrivateAnnotationFields {
    VERSION(0, 1),
    RECORD_SEQUENCE_NUMBER(1, 4),
    FRAME_DECODING(5, 1),
    DATA_POLARITY_REVERSAL(6, 1),
    REED_SOLOMON_SYMBOL_ECC(7, 1),
    PREDICTS_MODE(8, 1),
    DOWNLINK_FREQ_BAND(9, 1),
    UPLINK_FREQ_BAND(10, 1),
    UPLINK_ANTENNA_ID(11, 1),
    ERT_STATUS(12, 1),
    BITRATE(13, 4),
    VALIDITY_FLAGS(17, 1),
    RX_SIGNAL_LEVEL(18, 4),
    SYMBOL_SNR(22, 4),
    SYSTEM_NOISE_TEMP(26, 4),
    ARRAY_STATIONS(30, 1),
    PASS_NUMBER(31, 2),
    EQUIP_TYPE_ID(33, 1),
    EQUIP_INSTANCE_ID(34, 1),
    TELEM_LOCK_STATUS(35, 2),
    RESERVED(37, 11);

    int offset;
    int length;

    /**
     * Constructor
     *
     * @param offset byte offset
     * @param length byte length
     */
    V3PrivateAnnotationFields(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    /**
     * Get the byte offset
     *
     * @return byte offset
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Get the byte length
     *
     * @return byte length
     */
    public int getLength() {
        return this.length;
    }
}
