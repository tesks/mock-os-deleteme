/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.common.mib;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum ERemoteEntityMibPropertyKey {

    /*
     * These MIB properties reflect GSFC Library's implementation, not CFDP Blue
     * Book's.
     */
    POSITIVE_ACK_TIMER_EXPIRATION_LIMIT_PROPERTY(
            "positive.ack.timer.expiration.limit"), POSITIVE_ACK_TIMEOUT_INTERVAL_SECONDS_PROPERTY(
            "positive.ack.timeout.interval.seconds"), TRANSACTION_INACTIVITY_TIMEOUT_SECONDS_PROPERTY(
            "transaction.inactivity.timeout.seconds"), MAXIMUM_FILE_DATA_PER_OUTGOING_PDU_BYTES_PROPERTY(
            "maximum.file.data.per.outgoing.pdu.bytes"), SAVE_INCOMPLETE_FILES_PROPERTY(
            "save.incomplete.files"), NAK_TIMER_EXPIRATION_LIMIT_PROPERTY(
            "nak.timer.expiration.limit"), NAK_TIMEOUT_INTERVAL_SECONDS_PROPERTY(
            "nak.timeout.interval.seconds"), TIMERS_ENABLED_PROPERTY(
            "timers.enabled"), ONE_WAY_LIGHT_TIME_SECONDS_PROPERTY(
            "one-way.light.time.seconds",
            true), TOTAL_ROUND_TRIP_ALLOWANCE_FOR_QUEUEING_DELAY_SECONDS_PROPERTY(
            "total.round-trip.allowance.for.queuing.delay.seconds",
            true),

    // MPCS-9750 - 5/17/2018
    // Add new remote entity MIB items introduced in JavaCFDP v1.2.1-crc
    REQUIRE_ACKNOWLEDGMENT_PROPERTY("require.acknowledgment"),
    ADD_PDU_CRC_PROPERTY("add.pdu.crc"),
    CHECK_PDU_CRC_PROPERTY("check.pdu.crc");

    private String keyStr;
    private boolean required;

    private ERemoteEntityMibPropertyKey(String keyStr, boolean required) {
        this.keyStr = keyStr;
        this.required = required;
    }

    private ERemoteEntityMibPropertyKey(String keyStr) {
        this(keyStr, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return getKeyStr();
    }

    public static Set<String> getAllKeyStrings() {
        return Arrays.stream(ERemoteEntityMibPropertyKey.values()).map(key -> key.toString())
                .collect(Collectors.toSet());
    }

    public static Set<String> getAllRequiredKeyStrings() {
        return Arrays.stream(ERemoteEntityMibPropertyKey.values()).filter(key -> key.required)
                .map(key -> key.toString()).collect(Collectors.toSet());
    }

    public String getKeyStr() {
        return keyStr;
    }

}