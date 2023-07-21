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

package jpl.gds.station.impl.sle.annotation;

import jpl.gds.station.api.sle.annotation.IV3SlePrivateAnnotation;
import jpl.gds.station.api.sle.annotation.V3PrivateAnnotationFields;
import org.apache.commons.lang.math.DoubleRange;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.LongRange;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static jpl.gds.station.api.sle.annotation.V3PrivateAnnotationFields.*;
/**
 * Private annotation handler for DSN version 3 private annotations. See DSN doc 820-013 0243-Telecomm Rev B, Table A-1
 *
 */
public class V3SlePrivateAnnotationHandler implements IV3SlePrivateAnnotation {

    protected static final int            VERSION_LB                    = 1;
    protected static final int            VERSION_UB                    = 255;
    protected static final IntRange       VERSION_RANGE                 = new IntRange(VERSION_LB, VERSION_UB);
    protected static final int            REC_SEQ_NO_LB                 = 0;
    protected static final long           REC_SEQ_NO_UB                 = 4294967295L;
    protected static final LongRange      REC_SEQ_NO_RANGE              = new LongRange(REC_SEQ_NO_LB, REC_SEQ_NO_UB);
    protected static final int            FRAME_DEC_NFO_LB              = 7;
    protected static final int            FRAME_DEC_NFO_UB              = 22;
    protected static final IntRange       FRAME_DEC_NFO_RANGE           = new IntRange(FRAME_DEC_NFO_LB,
            FRAME_DEC_NFO_UB);
    protected static final int            DATA_POLARITY_REV_IND_LB      = 0;
    protected static final int            DATA_POLARITY_REV_IND_UB      = 1;
    protected static final IntRange       DATA_POLARITY_REV_IND_RANGE   = new IntRange(DATA_POLARITY_REV_IND_LB,
            DATA_POLARITY_REV_IND_UB);
    protected static final int            RS_SYMBOL_EC_LB               = 0;
    protected static final int            RS_SYMBOL_EC_UB               = 80;
    protected static final IntRange       RS_SYMBOL_EC_RANGE            = new IntRange(RS_SYMBOL_EC_LB,
            RS_SYMBOL_EC_UB);
    protected static final int            PREDICTS_MODE_LB              = 0;
    protected static final int            PREDICTS_MODE_UB              = 3;
    protected static final IntRange       PREDICTS_MODE_RANGE           = new IntRange(PREDICTS_MODE_LB,
            PREDICTS_MODE_UB);
    protected static final int            UPL_ANT_ID_LB                 = 0;
    protected static final int            UPL_ANT_ID_UB                 = 99;
    protected static final IntRange       UPL_ANT_ID_RANGE              = new IntRange(UPL_ANT_ID_LB, UPL_ANT_ID_UB);
    protected static final int            ERT_STATUS_LB                 = 0;
    protected static final int            ERT_STATUS_UB                 = 1;
    protected static final IntRange       ERT_STATUS_RANGE              = new IntRange(ERT_STATUS_LB, ERT_STATUS_UB);
    protected static final double         BITRATE_LB                    = 2.0;
    protected static final double         BITRATE_UB                    = 2.0e8;
    protected static final DoubleRange    BITRATE_RANGE                 = new DoubleRange(BITRATE_LB, BITRATE_UB);
    protected static final int            VALIDITY_FLAGS_LB             = 0;
    protected static final int            VALIDITY_FLAGS_UB             = 255;
    protected static final IntRange       VALIDITY_FLAGS_RANGE          = new IntRange(VALIDITY_FLAGS_LB,
            VALIDITY_FLAGS_UB);
    protected static final double         RX_SIGNAL_LVL_LB              = -300.0;
    protected static final double         RX_SIGNAL_LVL_UB              = 0.0;
    protected static final DoubleRange    RX_SIGNAL_LVL_RANGE           = new DoubleRange(RX_SIGNAL_LVL_LB,
            RX_SIGNAL_LVL_UB);
    protected static final double         SYMBOL_SNR_LB                 = -10.0;
    protected static final double         SYMBOL_SNR_UB                 = 40.0;
    protected static final DoubleRange    SYMBOL_SNR_RANGE              = new DoubleRange(SYMBOL_SNR_LB, SYMBOL_SNR_UB);
    protected static final double         SYS_NOISE_TEMP_LB             = 10.0;
    protected static final double         SYS_NOISE_TEMP_UB             = 9999.9;
    protected static final DoubleRange    SYS_NOISE_TEMP_RANGE          = new DoubleRange(SYS_NOISE_TEMP_LB,
            SYS_NOISE_TEMP_UB);
    protected static final int            ARRAY_STATIONS_LB             = 0;
    protected static final int            ARRAY_STATIONS_UB             = 255;
    protected static final IntRange       ARRAY_STATIONS_RANGE          = new IntRange(ARRAY_STATIONS_LB,
            ARRAY_STATIONS_UB);
    protected static final int            PASS_NO_LB                    = 0;
    protected static final int            PASS_NO_UB                    = 9999;
    protected static final IntRange       PASS_NO_RANGE                 = new IntRange(PASS_NO_LB, PASS_NO_UB);
    protected static final int            EQUIP_TYPE_LB                 = 1;
    protected static final int            EQUIP_TYPE_UB                 = 3;
    protected static final IntRange       EQUIP_TYPE_RANGE              = new IntRange(EQUIP_TYPE_LB, EQUIP_TYPE_UB);
    protected static final int            EQUIP_INST_ID_LB              = 0;
    protected static final int            EQUIP_INST_ID_UB              = 15;
    protected static final IntRange       EQUIP_INST_ID_RANGE           = new IntRange(EQUIP_INST_ID_LB,
            EQUIP_INST_ID_UB);
    protected static final int            TELEM_LOCK_STATUS_FLAGS_LB    = 0;
    protected static final int            TELEM_LOCK_STATUS_FLAGS_UB    = 65536;
    protected static final IntRange       TELEM_LOCK_STATUS_FLAGS_RANGE = new IntRange(TELEM_LOCK_STATUS_FLAGS_LB,
            TELEM_LOCK_STATUS_FLAGS_UB);
    private static final   int            PRIVATE_ANNOTATION_SIZE_BYTES = 48;
    private static final   Set<Character> FREQUENCY_BANDS               = new HashSet<>(
            Arrays.asList('U', 'S', 'X', 'K'));
    private                byte[]         annotations;
    private                boolean        valid;
    private                boolean        present = true;

    private static final String PVT_PREFIX = "pvt.";

    @Override
    public int getPrivateAnnotationSizeBytes() {
        return PRIVATE_ANNOTATION_SIZE_BYTES;
    }

    @Override
    public void load(byte[] buffer, int start) {
        if (start < 0 || start > buffer.length || start + PRIVATE_ANNOTATION_SIZE_BYTES > buffer.length) {
            annotations = new byte[0];
            return;
        }
        annotations = new byte[PRIVATE_ANNOTATION_SIZE_BYTES];
        System.arraycopy(buffer, start, annotations, 0, PRIVATE_ANNOTATION_SIZE_BYTES);
        setValid(checkValidity());
    }

    @Override
    public byte[] getBytes() {
        return present ? Arrays.copyOf(annotations, annotations.length) : new byte[0];
    }

    @Override
    public byte[] getBytes(int start, int length) {
        if (!present || start < 0 || start > annotations.length || start + length > annotations.length) {
            return new byte[0];
        }
        return Arrays.copyOfRange(annotations, start, start + length);
    }

    /**
     * Get the bytes for a given field
     *
     * @param field private annotation field
     * @return bytes
     */
    private byte[] getBytes(V3PrivateAnnotationFields field) {
        return getBytes(field.getOffset(), field.getLength());
    }

    /**
     * Check valid values and ranges for private annotations
     *
     * @return validity of private annotations
     */
    private boolean checkValidity() {
        boolean v = VERSION_RANGE.containsInteger(getVersionId());
        v &= REC_SEQ_NO_RANGE.containsLong(getRecordSequenceNo());
        v &= FRAME_DEC_NFO_RANGE.containsInteger(getFrameDecodingInfo());
        v &= DATA_POLARITY_REV_IND_RANGE.containsInteger(getDataPolarityReversalIndication());
        v &= RS_SYMBOL_EC_RANGE.containsInteger(getReedSolomonSymbolErrorCount());
        v &= PREDICTS_MODE_RANGE.containsInteger(getPredictsMode());
        v &= FREQUENCY_BANDS.contains(getDownlinkFrequencyBand());
        v &= FREQUENCY_BANDS.contains(getUplinkFrequencyBand());
        v &= UPL_ANT_ID_RANGE.containsInteger(getUplinkAntennaId());
        v &= ERT_STATUS_RANGE.containsInteger(getErtStatus());
        v &= BITRATE_RANGE.containsDouble(getBitrate());
        v &= VALIDITY_FLAGS_RANGE.containsInteger(getValidityFlags());
        v &= RX_SIGNAL_LVL_RANGE.containsDouble(getReceivedSignalLevel());
        v &= SYMBOL_SNR_RANGE.containsDouble(getSymbolSnr());
        v &= SYS_NOISE_TEMP_RANGE.containsDouble(getSystemNoiseTemp());
        v &= ARRAY_STATIONS_RANGE.containsInteger(getArrayStations());
        v &= PASS_NO_RANGE.containsInteger(getPassNo());
        v &= EQUIP_TYPE_RANGE.containsInteger(getEquipmentType());
        v &= EQUIP_INST_ID_RANGE.containsInteger(getEquipmentInstanceId());
        v &= TELEM_LOCK_STATUS_FLAGS_RANGE.containsInteger(getTelemetryLockStatusFlags());
        return v;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public void setPresent(boolean present) {
        this.present = present;
    }

    @Override
    public int getVersionId() {
        return present ? getBytes(V3PrivateAnnotationFields.VERSION)[0] : 0;
    }

    @Override
    public long getRecordSequenceNo() {
        if (!present) {
            return 0;
        }
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.position(4);
        buf.put(getBytes(V3PrivateAnnotationFields.RECORD_SEQUENCE_NUMBER));
        return buf.getLong(0);
    }

    @Override
    public int getFrameDecodingInfo() {
        return present ? getBytes(V3PrivateAnnotationFields.FRAME_DECODING)[0] : 0;
    }

    @Override
    public int getDataPolarityReversalIndication() {
        return present ? getBytes(V3PrivateAnnotationFields.DATA_POLARITY_REVERSAL)[0] : 0;
    }

    @Override
    public int getReedSolomonSymbolErrorCount() {
        return present ? getBytes(V3PrivateAnnotationFields.REED_SOLOMON_SYMBOL_ECC)[0] : 0;
    }

    @Override
    public int getPredictsMode() {
        return present ? getBytes(V3PrivateAnnotationFields.PREDICTS_MODE)[0] : 0;
    }

    @Override
    public char getDownlinkFrequencyBand() {
        return present ? (char) getBytes(V3PrivateAnnotationFields.DOWNLINK_FREQ_BAND)[0] : '\00';
    }

    @Override
    public char getUplinkFrequencyBand() {
        return present ? (char) getBytes(V3PrivateAnnotationFields.UPLINK_FREQ_BAND)[0] : '\00';
    }

    @Override
    public int getUplinkAntennaId() {
        return present ? getBytes(V3PrivateAnnotationFields.UPLINK_ANTENNA_ID)[0] : 0;
    }

    @Override
    public int getErtStatus() {
        return present ? getBytes(V3PrivateAnnotationFields.ERT_STATUS)[0] : 0;
    }

    @Override
    public float getBitrate() {
        return present ? ByteBuffer.wrap(getBytes(V3PrivateAnnotationFields.BITRATE)).getFloat() : 0;
    }

    @Override
    public int getValidityFlags() {
        return present ? getBytes(V3PrivateAnnotationFields.VALIDITY_FLAGS)[0] : 0;
    }

    @Override
    public float getReceivedSignalLevel() {
        return present ? ByteBuffer.wrap(getBytes(V3PrivateAnnotationFields.RX_SIGNAL_LEVEL)).getFloat() : 0;
    }

    @Override
    public float getSymbolSnr() {
        return present ? ByteBuffer.wrap(getBytes(V3PrivateAnnotationFields.SYMBOL_SNR)).getFloat() : 0;
    }

    @Override
    public float getSystemNoiseTemp() {
        return present ? ByteBuffer.wrap(getBytes(V3PrivateAnnotationFields.SYSTEM_NOISE_TEMP)).getFloat() : 0;
    }

    @Override
    public int getArrayStations() {
        return present ? getBytes(V3PrivateAnnotationFields.ARRAY_STATIONS)[0] : 0;
    }

    @Override
    public int getPassNo() {
        if (present) {
            ByteBuffer b = ByteBuffer.allocate(4);
            b.position(2);
            b.put(getBytes(V3PrivateAnnotationFields.PASS_NUMBER));
            return b.getInt(0);
        }
        return 0;
    }

    @Override
    public int getEquipmentType() {
        return present ? getBytes(V3PrivateAnnotationFields.EQUIP_TYPE_ID)[0] : 0;
    }

    @Override
    public int getEquipmentInstanceId() {
        return present ? getBytes(V3PrivateAnnotationFields.EQUIP_INSTANCE_ID)[0] : 0;
    }

    @Override
    public int getTelemetryLockStatusFlags() {
        if (present) {
            ByteBuffer b = ByteBuffer.allocate(4);
            b.position(2);
            b.put(getBytes(V3PrivateAnnotationFields.TELEM_LOCK_STATUS));
            return b.getInt(0);
        }
        return 0;
    }

    @Override
    public Map<String, String> getMetadata() {
        final Map<String, String> pvtMeta = new LinkedHashMap<>();

        pvtMeta.put(getMetaKey(V3PrivateAnnotationFields.VERSION), getMetaValue(this.getVersionId()));
        pvtMeta.put(getMetaKey(V3PrivateAnnotationFields.RECORD_SEQUENCE_NUMBER), getMetaValue(this.getRecordSequenceNo()));
        pvtMeta.put(getMetaKey(FRAME_DECODING), getMetaValue(this.getFrameDecodingInfo()));
        pvtMeta.put(getMetaKey(DATA_POLARITY_REVERSAL), getMetaValue(this.getDataPolarityReversalIndication()));
        pvtMeta.put(getMetaKey(REED_SOLOMON_SYMBOL_ECC), getMetaValue(this.getReedSolomonSymbolErrorCount()));
        pvtMeta.put(getMetaKey(PREDICTS_MODE), getMetaValue(this.getPredictsMode()));
        pvtMeta.put(getMetaKey(DOWNLINK_FREQ_BAND), getMetaValue(this.getDownlinkFrequencyBand()));
        pvtMeta.put(getMetaKey(UPLINK_FREQ_BAND), getMetaValue(this.getUplinkFrequencyBand()));
        pvtMeta.put(getMetaKey(UPLINK_ANTENNA_ID), getMetaValue(this.getUplinkAntennaId()));
        pvtMeta.put(getMetaKey(ERT_STATUS), getMetaValue(this.getErtStatus()));
        pvtMeta.put(getMetaKey(BITRATE), getMetaValue(this.getBitrate()));
        pvtMeta.put(getMetaKey(VALIDITY_FLAGS), getMetaValue(this.getValidityFlags()));
        pvtMeta.put(getMetaKey(RX_SIGNAL_LEVEL), getMetaValue(this.getReceivedSignalLevel()));
        pvtMeta.put(getMetaKey(SYMBOL_SNR), getMetaValue(this.getSymbolSnr()));
        pvtMeta.put(getMetaKey(SYSTEM_NOISE_TEMP), getMetaValue(this.getSystemNoiseTemp()));
        pvtMeta.put(getMetaKey(ARRAY_STATIONS), getMetaValue(this.getArrayStations()));
        pvtMeta.put(getMetaKey(PASS_NUMBER), getMetaValue(this.getPassNo()));
        pvtMeta.put(getMetaKey(EQUIP_TYPE_ID), getMetaValue(this.getEquipmentType()));
        pvtMeta.put(getMetaKey(EQUIP_INSTANCE_ID), getMetaValue(this.getEquipmentInstanceId()));
        pvtMeta.put(getMetaKey(TELEM_LOCK_STATUS), getMetaValue(this.getTelemetryLockStatusFlags()));

        return pvtMeta;
    }

    // returns metadata key like "pvt.version"
    private String getMetaKey(V3PrivateAnnotationFields field){
        return PVT_PREFIX + field.name().toLowerCase();
    }

    //converts any data to string as medatada value
    private String getMetaValue(Object obj){
        return String.valueOf(obj);
    }
}
