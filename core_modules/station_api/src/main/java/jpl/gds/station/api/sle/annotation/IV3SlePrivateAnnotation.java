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
 * Interface for DSN V3 Private Annotations. See 820-013 0243-Telecomm Space Link Extension Return Link Service
 * Interface Rev B.
 *
 */
public interface IV3SlePrivateAnnotation extends ISlePrivateAnnotation {

    /**
     * Version ID, [1, 255]
     *
     * @return
     */
    int getVersionId();

    /**
     * Record sequence number, [0, 2^32 - 1]
     *
     * @return
     */
    long getRecordSequenceNo();

    /**
     * Frame decoding related information, [7, 22]
     *
     * @return
     */
    int getFrameDecodingInfo();

    /**
     * Data polarity reversal indication, [0, 1]
     *
     * @return
     */
    int getDataPolarityReversalIndication();

    /**
     * Reed-Solomon symbol error correction count, [0, 80]
     *
     * @return
     */
    int getReedSolomonSymbolErrorCount();

    /**
     * Predicts mode, [0, 3]
     *
     * @return
     */
    int getPredictsMode();

    /**
     * Downlink frequency band, {U, S, X, K}
     *
     * @return
     */
    char getDownlinkFrequencyBand();

    /**
     * Uplink frequency band, {U, S, X, K}
     *
     * @return
     */
    char getUplinkFrequencyBand();

    /**
     * Uplink antenna ID, [0, 99]
     *
     * @return
     */
    int getUplinkAntennaId();

    /**
     * ERT status, [0, 1]
     *
     * @return
     */
    int getErtStatus();

    /**
     * Return the bitrate, [2.0, 2.0E8] [2.0 - 200,000,000.0]
     *
     * @return bitrate as float
     */
    float getBitrate();

    /**
     * Validity flags, [0, 255]
     *
     * @return
     */
    int getValidityFlags();

    /**
     * Received signal level in dBm, [-300.0, 0]
     *
     * @return
     */
    float getReceivedSignalLevel();

    /**
     * Symbol SNR, [-10.0, 40.0]
     *
     * @return
     */
    float getSymbolSnr();

    /**
     * System noise temperature, [10.0, 9999.9]
     *
     * @return
     */
    float getSystemNoiseTemp();

    /**
     * Array stations, bitmask over lower 8 bits, [0, 255]
     *
     * @return
     */
    int getArrayStations();

    /**
     * Pass number, [0, 9999]
     *
     * @return
     */
    int getPassNo();

    /**
     * Equipment type, [1, 3]
     *
     * @return
     */
    int getEquipmentType();

    /**
     * Equipment instance identifier, [0, 15]
     *
     * @return
     */
    int getEquipmentInstanceId();

    /**
     * Telemetry lock status flags, bitmask over lower 16 bits, [0, 65536]
     *
     * @return
     */
    int getTelemetryLockStatusFlags();

}
