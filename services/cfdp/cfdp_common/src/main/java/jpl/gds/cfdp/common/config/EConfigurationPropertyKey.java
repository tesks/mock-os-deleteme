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

package jpl.gds.cfdp.common.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code EConfigurationPropertyKey} enumerates all possible CFDP Processor configuration property keys.
 *
 * @since 8.0.1
 */
public enum EConfigurationPropertyKey {

    PORT_PROPERTY("port"), LOCAL_CFDP_ENTITY_ID_PROPERTY("local.cfdp.entity.id"), ACTION_RING_BUFFER_SIZE_PROPERTY(
            "action.ring.buffer.size"), INBOUND_PDU_RING_BUFFER_SIZE_PROPERTY(
            "inbound.pdu.ring.buffer.size"), INGEST_ACTION_DISRUPTOR_RING_BUFFER_SIZE_PROPERTY(
            "ingest.action.disruptor.ring.buffer.size"), MESSAGE_DISRUPTOR_RING_BUFFER_SIZE_PROPERTY(
            "message.disruptor.ring.buffer.size"), INGEST_ACTION_DISRUPTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY(
            "ingest.action.disruptor.shutdown.timeout.millis"), MESSAGE_DISRUPTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY(
            "message.disruptor.shutdown.timeout.millis"), INBOUND_PDU_FILE_INGESTION_MINIMUM_READ_INTERVAL_MILLIS_PROPERTY(
            "inbound.pdu.file.ingestion.minimum.read.interval.millis"), WORKER_TASKS_EXECUTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY(
            "worker.tasks.executor.shutdown.timeout.millis"), ENGINE_CYCLER_TASK_EXECUTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY(
            "engine.cycler.task.executor.shutdown.timeout.millis"), ENGINE_CYCLE_MINIMUM_INTERVAL_WHEN_IDLE_MILLIS_PROPERTY(
            "engine.cycle.minimum.interval.when.idle.millis"), PDU_FILE_READ_BUFFER_SIZE_BYTES_PROPERTY(
            "pdu.file.read.buffer.size.bytes"), DISABLE_CONFIG_AUTOSAVE_PROPERTY(
            "disable.config.autosave"), WRITABLE_MIB_FILE_PROPERTY(
            "writable.mib.file"), DISABLE_MIB_AUTOSAVE_PROPERTY(
            "disable.mib.autosave"), INSTANCE_ID_PROPERTY(
            "instance.id"), OUTBOUND_PDU_ENABLED_PROPERTY(
            "outbound.pdu.enabled"), OUTBOUND_PDU_SINK_TYPE_PROPERTY(
            "outbound.pdu.sink.type"), OUTBOUND_PDU_URI_PROPERTY(
            "outbound.pdu.uri"), OUTBOUND_PDU_URI_SINK_MINIMUM_SEND_INTERVAL_MILLIS_PROPERTY(
            "outbound.pdu.uri.sink.minimum.send.interval.millis"), OUTBOUND_PDU_FILESYSTEM_SINK_DIRECTORY_PROPERTY(
            "outbound.pdu.filesystem.sink.directory"), OUTBOUND_PDU_FILESYSTEM_SINK_FILE_PREFIX_PROPERTY(
            "outbound.pdu.filesystem.sink.file.prefix"), OUTBOUND_PDU_FILESYSTEM_SINK_FILE_EXTENSION_PROPERTY(
            "outbound.pdu.filesystem.sink.file.extension"), OUTBOUND_PDU_FILESYSTEM_SINK_FILE_NUMBER_WIDTH_PROPERTY(
            "outbound.pdu.filesystem.sink.file.number.width"), OUTBOUND_PDU_FILESYSTEM_SINK_MINIMUM_WRITE_INTERVAL_MILLIS_PROPERTY(
            "outbound.pdu.filesystem.sink.minimum.write.interval.millis"), ACTION_RESULT_TIMEOUT_MILLIS_PROPERTY(
            "action.result.timeout.millis"), TRANSACTION_SEQUENCE_NUMBER_FILE_PROPERTY(
            "transaction.sequence.number.file"), DEFAULT_SERVICE_CLASS_PROPERTY(
            "default.service.class"),
    // MPCS-9929 - 8/24/2018 - Removed ability to configure CFDP Processor's log level dynamically
    // LOG_LEVEL_PROPERTY("log.level"),
    FINISHED_DOWNLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY(
            "finished.downlink.files.top.level.directory"), ACTIVE_DOWNLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY(
            "active.downlink.files.top.level.directory"), UNKNOWN_DESTINATION_FILENAME_DOWNLINK_FILES_SUBDIRECTORY_PROPERTY(
            "unknown.destination.filename.downlink.files.subdirectory"), UPLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY(
            "uplink.files.top.level.directory"), CONSECUTIVE_FILESTORE_FAILURES_BEFORE_DECLARING_ERROR_THRESHOLD_PROPERTY(
            "consecutive.filestore.failures.before.declaring.error.threshold"), SAVED_STATE_DIRECTORY_PROPERTY(
            "saved.state.directory"), TEMPORARY_FILES_DIRECTORY_PROPERTY(
            "temporary.files.directory"), FINISHED_TRANSACTIONS_HISTORY_KEEP_TIME_MILLIS_PROPERTY(
            "finished.transactions.history.keep.time.millis"), FINISHED_TRANSACTIONS_HISTORY_PURGE_PERIOD_MILLIS_PROPERTY(
            "finished.transactions.history.purge.period.millis"), MAXIMUM_OPEN_UPLINK_TRANSACTIONS_PER_REMOTE_ENTITY_PROPERTY(
            "maximum.open.uplink.transactions.per.remote.entity"), MAXIMUM_DESTINATION_FILENAME_LENGTH_PROPERTY(
            "maximum.destination.filename.length"), MAXIMUM_UPLINK_FILE_SIZE_BYTES_PROPERTY(
            "maximum.uplink.file.size.bytes"), MAXIMUM_UPLINK_FILE_SIZES_TOTAL_BYTES_PER_REMOTE_ENTITY_PROPERTY(
            "maximum.uplink.file.sizes.total.bytes.per.remote.entity"), AUTO_STATE_SAVE_PERIOD_MILLIS_PROPERTY(
            "auto.state.save.period.millis"), MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPICS_OVERRIDE_PROPERTY(
            "message.service.inbound.pdu.root.topics.override"),
    MESSAGE_SERVICE_INBOUND_PDU_HANDLER_QUEUE_SIZE_PROPERTY(
            "message.service.inbound.pdu.handler.queue.size"), MESSAGE_SERVICE_PROGRESS_CFDP_INDICATION_MESSAGE_PUBLISHING_ENABLED_PROPERTY(
            "message.service.progress.cfdp.indication.message.publishing.enabled"), MESSAGE_SERVICE_PDU_MESSAGE_PUBLISHING_ENABLED_PROPERTY(
            "message.service.pdu.message.publishing.enabled"), DOWNLINK_METADATA_INCLUDE_PDU_LOG_ENABLED_PROPERTY(
            "downlink.metadata.include.pdu.log.enabled"), UPLINK_METADATA_INCLUDE_PDU_LOG_ENABLED_PROPERTY(
            "uplink.metadata.include.pdu.log.enabled"),

    // MPCS-9865 - 6/18/2018
    // Add AMPCS Product Plugin configuration flag
    AMPCS_PRODUCT_PLUGIN_ENABLED_PROPERTY("ampcs.product.plugin.enabled"),
    // MPCS-10002 - 9/24/2018
    // Add configuration option to switch between consuming PDU messages from chill_down or LinkSim
    INBOUND_PDU_FROM_LINKSIM_PROPERTY("inbound.pdu.from.linksim"),
    // MPCS-10094 - 11/5/2018
    // Add configuration option for JMS topic to publish messages to
    // MPCS-10937: Make this property something that just overrides the default
    MESSAGE_SERVICE_PUBLISHING_ROOT_TOPIC_OVERRIDE_PROPERTY("message.service.publishing.root.topic.override"),
    /* MPCS-10886 - 5/9/19 */
    MESSAGES_TO_USER_MAP_FILE_PROPERTY("mtu.map.file"),
    MESSAGES_TO_USER_DIRECT_INPUT_ENABLED_PROPERTY("mtu.direct.input.enabled"),
    MESSAGES_TO_USER_ALWAYS_REQUIRED_PROPERTY("mtu.always.required"),
    // MPCS-9757 - 6/24/2019
    // Add configuration option for CFDP EOF PDU checksum
    EOF_PDU_CHECKSUM_ALGORITHM_PROPERTY("eof.pdu.checksum.algorithm"),
    // Add configuration option for toggling CFDP checksum capability
    EOF_PDU_CHECKSUM_VALIDATION_ENABLED_PROPERTY("eof.pdu.checksum.validation.enabled"),

    // MPCS-11054 - Boris Borelly - 7/25/2019
    // Add configuration option for whether or not it is allowed to query the available files from
    // the uplink directory. This is meant to prevent CFDP from blowing up when it is '/'
    AVAILABLE_UPLINK_FILE_QUERY_PROPERTY("uplink.files.available.query.enabled"),
    // MPCS-11563  - 2/13/2020
    // Add configuration option to allow CFDP-created products to be added to the PDPP database
    PDPP_ADDER_PROPERTY("downlink.pdpp.enabled"),

    // MPCS-12381  - 1/2022
    // Add configuration option for the maximum source filename length per FGICD
    MAXIMUM_SOURCE_FILENAME_LENGTH_PROPERTY("maximum.source.filename.length");

    private static final String PROPERTY_PREFIX = "cfdp.processor.";

    private String fullPropertyKeyStr;
    private String subPropertyKeyStr;

    EConfigurationPropertyKey(String subPropertyKeyStr) {
        this.subPropertyKeyStr = subPropertyKeyStr;
        this.fullPropertyKeyStr = PROPERTY_PREFIX + subPropertyKeyStr;
    }

    public static String getPrefix() {
        return PROPERTY_PREFIX;
    }

    public static Set<String> getAllFullKeyStrings() {
        return Arrays.stream(EConfigurationPropertyKey.values()).map(propertyKey -> propertyKey.fullPropertyKeyStr)
                .collect(Collectors.toSet());
    }

    public static Set<String> getAllSubKeyStrings() {
        return Arrays.stream(EConfigurationPropertyKey.values()).map(propertyKey -> propertyKey.subPropertyKeyStr)
                .collect(Collectors.toSet());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return fullPropertyKeyStr;
    }

    public String getFullPropertyKeyStr() {
        return fullPropertyKeyStr;
    }

    public String getSubPropertyKeyStr() {
        return subPropertyKeyStr;
    }

}
