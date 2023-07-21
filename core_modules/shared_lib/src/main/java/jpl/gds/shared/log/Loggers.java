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
package jpl.gds.shared.log;

/**
 * Enum class for AMPCS logging (Tracer) names
 * 
 *
 */
public enum Loggers {

    /** AMPCS 'Root' Tracer name */
    AMPCS_ROOT_TRACER(LoggingConstants.ROOT_LOGGER),

    /** Default Tracer name */
    DEFAULT(LoggingConstants.PACKAGE_PREFIX + "default"),

    /** Watcher Tracer name */
    WATCHER(LoggingConstants.PACKAGE_PREFIX + "watchers"),

    /** Utility Tracer name */
    UTIL(LoggingConstants.PACKAGE_PREFIX + "util"),

    /** SYSLOG Tracer name */
    SYSLOG(UTIL + LoggingConstants.DOT + "sys"),

    /** User adaptation Tracer name */
    ADAPTATION(LoggingConstants.PACKAGE_PREFIX + "adaptation"),

    /** Datagen Tracer name */
    DATAGEN(LoggingConstants.PACKAGE_PREFIX + "datagen"),

    /** Datagen Status Tracer name */
    DATAGEN_STATUS(DATAGEN + LoggingConstants.DOT + "status"),

    /** Performance Tracer name */
    PERFORMANCE(UTIL + LoggingConstants.DOT + "performance"),

    /** AMPCS Configuration/Properties Tracer name */
    CONFIG(UTIL + LoggingConstants.DOT + "config"),

    /** Tracking service Tracer name */
    TRACKING(LoggingConstants.SERVICE_BLOCK + "tracking"),

    /** Notifier service Tracer name */
    NOTIFIER(LoggingConstants.SERVICE_BLOCK + "notifier"),

    /** Alarm service Tracer name */
    ALARM(LoggingConstants.SERVICE_BLOCK + "alarm"),

    /** GlobalLAD service Tracer name */
    GLAD(LoggingConstants.SERVICE_BLOCK + "globallad"),

    /** Uplink service Tracer name */
    UPLINK(LoggingConstants.SERVICE_BLOCK + "uplink"),

    /** AUTO Uplink service Tracer name */
    AUTO_UPLINK(UPLINK + LoggingConstants.DOT + "auto"),

    /** CPD Uplink service Tracer name */
    CPD_UPLINK(UPLINK + LoggingConstants.DOT + "cpd"),

    /** Command Echo service Tracer name */
    CMD_ECHO(UPLINK + LoggingConstants.DOT + "echo"),

    /** AUTO Uplink CFDP service Tracer name */
    AUTO_CFDP(UPLINK + LoggingConstants.DOT + "cfdp"),

    /** Downlink service Tracer name */
    DOWNLINK(LoggingConstants.SERVICE_BLOCK + "downlink"),

    /** Telemetry Ingestor service Tracer name */
    INGEST(LoggingConstants.SERVICE_BLOCK + "ingest"),

    /** Telemetry Processor service Tracer name */
    PROCESSOR(LoggingConstants.SERVICE_BLOCK + "processor"),

    /** Monitor Data Service Tracer name */
    MDS(LoggingConstants.SERVICE_BLOCK + "mds"),

    /** MTAK Downlink service Tracer name */
    MTAK_DOWNLINK(DOWNLINK + LoggingConstants.DOT + "mtak"),

    /** SLE Downlink service Tracer name */
    SLE_DOWNLINK(DOWNLINK + LoggingConstants.DOT + "sle"),

    /** Recorded Engineerin Watcher Tracer name */
    RECORDED_ENG(DOWNLINK + LoggingConstants.DOT + "recordedEngWatcher"),

    /** Raw data Downlink service Tracer name */
    DOWNLINK_SERVICE_RAW(DOWNLINK + LoggingConstants.DOT + "raw"),

    /** FrameSync service Tracer name */
    FRAME_SYNC(DOWNLINK_SERVICE_RAW + LoggingConstants.DOT + "frameSync"),

    /** PDU Extract service Tracer name */
    PDU_EXTRACTOR(DOWNLINK_SERVICE_RAW + LoggingConstants.DOT + "pduExtractor"),
    
    /** Packet Extract service Tracer name */
    PACKET_EXTRACTOR(DOWNLINK_SERVICE_RAW + LoggingConstants.DOT + "packetExtractor"),

    /** Database service Tracer name */
    DATABASE(LoggingConstants.DB_BLOCK),

    /** Database fetch service Tracer name */
    DB_FETCH(LoggingConstants.DB_BLOCK + LoggingConstants.DOT + "fetch"),

    /** Database LDI service Tracer name */
    DB_LDI(LoggingConstants.DB_BLOCK + LoggingConstants.DOT + "ldi"),

    /** Database LDI Inserter service Tracer name */
    LDI_INSERTER(DB_LDI + LoggingConstants.DOT + "inserter"),

    /** Database LDI Gatherer service Tracer name */
    LDI_GATHERER(DB_LDI + LoggingConstants.DOT + "gatherer"),

    /** Message service Tracer name */
    BUS(LoggingConstants.SERVICE_BLOCK + "message"),

    /** JMS message service Tracer name */
    JMS(BUS + LoggingConstants.DOT + "jms"),

    /** Dictionary service Tracer name */
    DICTIONARY(LoggingConstants.SERVICE_BLOCK + "dictionary"),

    /** PDPP Product automation Tracer name */
    PDPP(LoggingConstants.SERVICE_BLOCK + "pdpp"),

    /** CFDP service Tracer name */
    CFDP(LoggingConstants.SERVICE_BLOCK + "cfdp"),

    /** SLE service Tracer Name */
    SLE(LoggingConstants.SERVICE_BLOCK + "sle"),

    /** Telemetry input Tracer name */
    TLM_INPUT(LoggingConstants.TLM_BLOCK + "input"),

    /** EHA Telemetry Tracer name */
    TLM_EHA(LoggingConstants.TLM_BLOCK + "eha"),

    /** EHA Monitor Tracer name */
    TLM_MONITOR(TLM_EHA + LoggingConstants.DOT + "monitor"),

    /** Eha Header Tracer name */
    TLM_HEADER(TLM_EHA + LoggingConstants.DOT + "header"),

    /** Evr Telemetry Tracer name */
    TLM_EVR(LoggingConstants.TLM_BLOCK + "evr"),

    /** Product Telemetry Tracer name */
    TLM_PRODUCT(LoggingConstants.TLM_BLOCK + "product"),

    /** Product Builder Tracer name */
    PRODUCT_BUILDER(TLM_PRODUCT + LoggingConstants.DOT + "builder"),

    /** Product Decom Tracer name */
    PRODUCT_DECOM(TLM_PRODUCT + LoggingConstants.DOT + "decom"),

    /** Derivation telemetry Tracer name */
    TLM_DERIVATION(LoggingConstants.TLM_BLOCK + "derivation"),

    /** ECDR telemetry Tracer name */
    TLM_ECDR(LoggingConstants.TLM_BLOCK + "ecdr"),

    /** DPO telemetry builder Tracer name */
    DPO_BUILDER(LoggingConstants.TLM_BLOCK + "dpoBuilder"),

    /** Websocket Tracer name */
    WEBSOCKET(LoggingConstants.SERVICE_BLOCK + "websocket");

    Loggers(final String name) {
        this.me = name;
    }

    private final String me;

    @Override
    public String toString() {
        return me;
    }

}
