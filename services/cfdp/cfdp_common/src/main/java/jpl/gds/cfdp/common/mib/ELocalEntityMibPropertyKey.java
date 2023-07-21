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

public enum ELocalEntityMibPropertyKey {

    /*
     * These MIB properties reflect GSFC Library's implementation, not CFDP Blue
     * Book's.
     */
    EOF_RECV_INDICATION_PROPERTY("eof.recv.indication"), EOF_SENT_INDICATION_PROPERTY(
            "eof.sent.indication"), FILE_SEGMENT_RECV_INDICATION_PROPERTY(
            "file.segment.recv.indication"), FILE_SEGMENT_SENT_INDICATION_PROPERTY(
            "file.segment.sent.indication"), DEFAULT_HANDLER_FOR_NO_ERROR_PROPERTY(
            "default.handler.for.no.error"), DEFAULT_HANDLER_FOR_POSITIVE_ACK_LIMIT_REACHED_PROPERTY(
            "default.handler.for.positive.ack.limit.reached"), DEFAULT_HANDLER_FOR_KEEP_ALIVE_LIMIT_REACHED_PROPERTY(
            "default.handler.for.keep.alive.limit.reached"), DEFAULT_HANDLER_FOR_INVALID_TRANSMISSION_MODE_PROPERTY(
            "default.handler.for.invalid.transmission.mode"), DEFAULT_HANDLER_FOR_FILESTORE_REJECTION_PROPERTY(
            "default.handler.for.filestore.rejection"), DEFAULT_HANDLER_FOR_FILE_CHECKSUM_FAILURE_PROPERTY(
            "default.handler.for.file.checksum.failure"), DEFAULT_HANDLER_FOR_FILE_SIZE_ERROR_PROPERTY(
            "default.handler.for.file.size.error"), DEFAULT_HANDLER_FOR_NAK_LIMIT_REACHED_PROPERTY(
            "default.handler.for.nak.limit.reached"), DEFAULT_HANDLER_FOR_INACTIVITY_DETECTED_PROPERTY(
            "default.handler.for.inactivity.detected"), DEFAULT_HANDLER_FOR_INVALID_FILE_STRUCTURE_PROPERTY(
            "default.handler.for.invalid.file.structure"), DEFAULT_HANDLER_FOR_SUSPEND_REQUEST_RECEIVED_PROPERTY(
            "default.handler.for.suspend.request.received"), DEFAULT_HANDLER_FOR_CANCEL_REQUEST_RECEIVED_PROPERTY(
            "default.handler.for.cancel.request.received"), MAXIMUM_CONCURRENT_TRANSACTIONS_PROPERTY(
            "maximum.concurrent.transactions"), MAXIMUM_FILE_DATA_PER_INCOMING_PDU_BYTES_PROPERTY(
            "maximum.file.data.per.incoming.pdu.bytes"), MAXIMUM_GAPS_PER_NAK_PDU_PROPERTY(
            "maximum.gaps.per.nak.pdu"), MAXIMUM_INCOMING_METADATA_PDU_FILENAME_LENGTH_PROPERTY(
            "maximum.incoming.metadata.pdu.filename.length"), REPORT_UNKNOWN_TRANSACTIONS_PROPERTY(
            "report.unknown.transactions"), FREEZE_TIMERS_ON_NEW_CLASS_1_TRANSACTIONS_PROPERTY(
            "freeze.timers.on.new.class.1.transactions"), FREEZE_TIMERS_ON_NEW_CLASS_2_TRANSACTIONS_PROPERTY(
            "freeze.timers.on.new.class.2.transactions"),

    // MPCS-9750 - 5/17/2018
    // Add new local entity MIB items introduced in JavaCFDP v1.2.1-crc
    TRANSACTION_SEQUENCE_NUMBER_LENGTH_PROPERTY("transaction.sequence.number.length"),
    CYCLE_MODE_PROPERTY("cycle.mode");

    private String keyStr;
    private boolean required;

    private ELocalEntityMibPropertyKey(String keyStr, boolean required) {
        this.keyStr = keyStr;
        this.required = required;
    }

    private ELocalEntityMibPropertyKey(String keyStr) {
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
        return Arrays.stream(ELocalEntityMibPropertyKey.values()).map(key -> key.toString())
                .collect(Collectors.toSet());
    }

    public static Set<String> getAllRequiredKeyStrings() {
        return Arrays.stream(ELocalEntityMibPropertyKey.values()).filter(key -> key.required).map(key -> key.toString())
                .collect(Collectors.toSet());
    }

    public String getKeyStr() {
        return keyStr;
    }

}