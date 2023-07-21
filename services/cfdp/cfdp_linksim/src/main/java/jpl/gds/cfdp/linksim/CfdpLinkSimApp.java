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
package jpl.gds.cfdp.linksim;

import jpl.gds.cfdp.common.commandline.CfdpCommandLineUtil;
import jpl.gds.cfdp.linksim.config.CfdpLinkSimAmpcsProperties;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.types.UnsignedInteger;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.*;

import static org.springframework.boot.Banner.Mode.OFF;

@SpringBootApplication
@EnableScheduling
@ComponentScan("${springScanPath}")
public class CfdpLinkSimApp {

    @SuppressWarnings("unused")
    private static final List<String> SCAN_PATH = buildScanPath(CfdpLinkSimApp.class.getPackage());

    public static String CYCLE_DELAY_MILLIS_PROPERTY_KEY = "cycle.delay.millis";
    public static String PDU_DUPLICATION_RATE_PROPERTY_KEY = "pdu.duplication.rate";
    public static String PDU_REORDER_PROPERTY_KEY = "pdu.reorder";
    public static String PDU_DROP_METADATA_COUNT_PROPERTY_KEY = "pdu.drop.metadata.count";
    public static String PDU_DROP_EOF_COUNT_PROPERTY_KEY = "pdu.drop.eof.count";
    public static String PDU_DROP_FIN_COUNT_PROPERTY_KEY = "pdu.drop.fin.count";
    public static String PDU_DROP_EOF_ACK_COUNT_PROPERTY_KEY = "pdu.drop.eof.ack.count";
    public static String PDU_DROP_FIN_ACK_COUNT_PROPERTY_KEY = "pdu.drop.fin.ack.count";
    public static String PDU_DROP_NAK_COUNT_PROPERTY_KEY = "pdu.drop.nak.count";
    public static String PDU_DROP_ALL_EXCEPT_METADATA_COUNT_PROPERTY_KEY = "pdu.drop.all.except.metadata.count";
    public static String PDU_DROP_ALL_EXCEPT_EOF_COUNT_PROPERTY_KEY = "pdu.drop.all.except.eof.count";
    public static String PDU_DROP_FILEDATA_RATE_PROPERTY_KEY = "pdu.drop.filedata.rate";
    public static String PDU_DROP_ALL_DURATION_SECONDS_PROPERTY_KEY = "pdu.drop.all.duration.seconds";
    public static String PDU_CORRUPT_METADATA_COUNT_PROPERTY_KEY = "pdu.corrupt.metadata.count";
    public static String PDU_CORRUPT_FILEDATA_RATE_PROPERTY_KEY = "pdu.corrupt.filedata.rate";
    public static String PDU_CORRUPT_EOF_COUNT_PROPERTY_KEY = "pdu.corrupt.eof.count";
    public static String PDU_CORRUPT_FIN_COUNT_PROPERTY_KEY = "pdu.corrupt.fin.count";
    public static String PDU_CORRUPT_NAK_COUNT_PROPERTY_KEY = "pdu.corrupt.nak.count";
    public static String PDU_CORRUPT_EOF_ACK_COUNT_PROPERTY_KEY = "pdu.corrupt.eof.ack.count";
    public static String PDU_CORRUPT_FIN_ACK_COUNT_PROPERTY_KEY = "pdu.corrupt.fin.ack.count";
    public static String PDU_ALTER_METADATA_CRC_COUNT_PROPERTY_KEY = "pdu.alter.metadata.crc.count";
    public static String PDU_ALTER_FILEDATA_CRC_RATE_PROPERTY_KEY = "pdu.alter.filedata.crc.rate";
    public static String PDU_ALTER_EOF_CRC_COUNT_PROPERTY_KEY = "pdu.alter.eof.crc.count";
    public static String PDU_ALTER_FIN_CRC_COUNT_PROPERTY_KEY = "pdu.alter.fin.crc.count";
    public static String PDU_ALTER_NAK_CRC_COUNT_PROPERTY_KEY = "pdu.alter.nak.crc.count";
    public static String PDU_ALTER_EOF_ACK_CRC_COUNT_PROPERTY_KEY = "pdu.alter.eof.ack.crc.count";
    public static String PDU_ALTER_FIN_ACK_CRC_COUNT_PROPERTY_KEY = "pdu.alter.fin.ack.crc.count";
    public static String PDU_ALTER_EOF_CHECKSUM_COUNT_PROPERTY_KEY = "pdu.alter.eof.checksum.count";
    public static String PDU_ALTER_EOF_FILE_SIZE_COUNT_PROPERTY_KEY = "pdu.alter.eof.file.size.count";
    public static String OUTPUT_TO_REST_API_PROPERTY_KEY = "output.to.rest.api";
    public static String URL_PROPERTY_KEY = "url";
    public static String INPUT_FROM_JMS_PROPERTY_KEY = "input.from.jms";
    public static String MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC = "message.service.inbound.pdu.root.topic";
    public static String MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC = "message.service.outbound.pdu.root.topic";

    public static List<String> buildScanPath(final Package bootStrapPackage) {
        final List<String> scanPath = new ArrayList<String>();
        scanPath.add(bootStrapPackage.getName());
        scanPath.addAll(SpringContextFactory.getSpringBootstrapScanPath(true));
        GdsSpringSystemProperties.setScanPath(scanPath);
        return scanPath;
    }

    /**
     * This method is separated out so that it can be mocked.
     *
     * @return actual {@code SpringApplicationBuilder} object to run later
     */
    private static SpringApplicationBuilder getAppBuilder() {
        return new SpringApplicationBuilder(CfdpLinkSimApp.class).bannerMode(OFF);
    }

    /**
     * This method is separated out so that it can be mocked.
     */
    private static void exit(final int status) {
        System.exit(status);
    }

    public static void main(final String[] args) {
        final AmpcsStyleCommandLineHandler commandLineHandler = new AmpcsStyleCommandLineHandler();
        final List<String> optionArgs = new ArrayList<>(args.length);
        final List<String> nonOptionArgs = new ArrayList<>(args.length);

        try {
            CfdpCommandLineUtil.divideArgs(new DefaultParser(), commandLineHandler.createCommonsCliOptions(), args, optionArgs,
                    nonOptionArgs);
            final ICommandLine commandLine = commandLineHandler.createOptions()
                    .parseCommandLine(optionArgs.toArray(new String[0]), true);
            commandLineHandler.configure(commandLine);

        } catch (final ParseException pe) {
            System.err.println("Error parsing arguments: " + pe.getLocalizedMessage());
            exit(1);
        }

        SpringApplicationBuilder appBuilder = getAppBuilder();

        appBuilder = appBuilder.properties("server.servlet.contextPath=/linksim");

        if (commandLineHandler.getOverriddenPort() != null) {
            appBuilder = appBuilder.properties("server.port=" + commandLineHandler.getOverriddenPort().intValue());
        } else {
            appBuilder = appBuilder.properties("server.port=" + new CfdpLinkSimAmpcsProperties().getPort());
        }

        appBuilder = appBuilder.properties(PDU_DUPLICATION_RATE_PROPERTY_KEY + "="
                + (commandLineHandler.getDuplicationRate() != null
                ? commandLineHandler.getDuplicationRate().intValue()
                : 0));

        appBuilder = appBuilder.properties(CYCLE_DELAY_MILLIS_PROPERTY_KEY + "="
                + (commandLineHandler.getCycleDelay() != null
                ? commandLineHandler.getCycleDelay().intValue()
                : 50));

        appBuilder = appBuilder.properties(PDU_REORDER_PROPERTY_KEY + "="
                + (commandLineHandler.getReorder() != null
                ? commandLineHandler.getReorder().booleanValue()
                : false));

        appBuilder = appBuilder.properties(PDU_DROP_METADATA_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropMetadataCount() != null
                ? commandLineHandler.getDropMetadataCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_EOF_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropEofCount() != null
                ? commandLineHandler.getDropEofCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_FIN_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropFinCount() != null
                ? commandLineHandler.getDropFinCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_EOF_ACK_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropEofAckCount() != null
                ? commandLineHandler.getDropEofAckCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_FIN_ACK_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropFinAckCount() != null
                ? commandLineHandler.getDropFinAckCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_NAK_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropNakCount() != null
                ? commandLineHandler.getDropNakCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_ALL_EXCEPT_METADATA_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropAllExceptMetadataCount() != null
                ? commandLineHandler.getDropAllExceptMetadataCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_ALL_EXCEPT_EOF_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getDropAllExceptEofCount() != null
                ? commandLineHandler.getDropAllExceptEofCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_FILEDATA_RATE_PROPERTY_KEY + "="
                + (commandLineHandler.getDropFiledataRate() != null
                ? commandLineHandler.getDropFiledataRate().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_DROP_ALL_DURATION_SECONDS_PROPERTY_KEY + "="
                + (commandLineHandler.getDropAllDurationSeconds() != null
                ? commandLineHandler.getDropAllDurationSeconds().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_CORRUPT_METADATA_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getCorruptMetadataCount() != null
                ? commandLineHandler.getCorruptMetadataCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_CORRUPT_FILEDATA_RATE_PROPERTY_KEY + "="
                + (commandLineHandler.getCorruptFiledataRate() != null
                ? commandLineHandler.getCorruptFiledataRate().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_CORRUPT_EOF_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getCorruptEofCount() != null
                ? commandLineHandler.getCorruptEofCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_CORRUPT_FIN_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getCorruptFinCount() != null
                ? commandLineHandler.getCorruptFinCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_CORRUPT_NAK_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getCorruptNakCount() != null
                ? commandLineHandler.getCorruptNakCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_CORRUPT_EOF_ACK_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getCorruptEofAckCount() != null
                ? commandLineHandler.getCorruptEofAckCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_CORRUPT_FIN_ACK_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getCorruptFinAckCount() != null
                ? commandLineHandler.getCorruptFinAckCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_METADATA_CRC_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterMetadataCrcCount() != null
                ? commandLineHandler.getAlterMetadataCrcCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_FILEDATA_CRC_RATE_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterFiledataCrcRate() != null
                ? commandLineHandler.getAlterFiledataCrcRate().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_EOF_CRC_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterEofCrcCount() != null
                ? commandLineHandler.getAlterEofCrcCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_FIN_CRC_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterFinCrcCount() != null
                ? commandLineHandler.getAlterFinCrcCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_NAK_CRC_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterNakCrcCount() != null
                ? commandLineHandler.getAlterNakCrcCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_EOF_ACK_CRC_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterEofAckCrcCount() != null
                ? commandLineHandler.getAlterEofAckCrcCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_FIN_ACK_CRC_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterFinAckCrcCount() != null
                ? commandLineHandler.getAlterFinAckCrcCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_EOF_CHECKSUM_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterEofChecksumCount() != null
                ? commandLineHandler.getAlterEofChecksumCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(PDU_ALTER_EOF_FILE_SIZE_COUNT_PROPERTY_KEY + "="
                + (commandLineHandler.getAlterEofFileSizeCount() != null
                ? commandLineHandler.getAlterEofFileSizeCount().intValue()
                : 0));

        appBuilder = appBuilder.properties(OUTPUT_TO_REST_API_PROPERTY_KEY + "="
                + (commandLineHandler.getOutputToRestApi() != null
                ? commandLineHandler.getOutputToRestApi().booleanValue()
                : false));

        appBuilder = appBuilder.properties(URL_PROPERTY_KEY + "="
                + commandLineHandler.getUrl());

        appBuilder = appBuilder.properties(INPUT_FROM_JMS_PROPERTY_KEY + "="
                + (commandLineHandler.getInputFromJms() != null
                ? commandLineHandler.getInputFromJms().booleanValue()
                : false));

        appBuilder = appBuilder.properties(MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC + "="
                + commandLineHandler.getMessageServiceInboundPduRootTopic());

        appBuilder = appBuilder.properties(MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC + "="
                + commandLineHandler.getMessageServiceOutboundPduRootTopic());

        final SpringApplication app = appBuilder.build();
        // MPCS-11493 - Allow bean override
        app.setAllowBeanDefinitionOverriding(true);
        app.run(args);
    }

    public static class AmpcsStyleCommandLineHandler extends AbstractCommandLineApp {

        private static final String PORT_SHORT = "p";
        private static final String PORT_LONG = "port";

        private static final String CYCLE_DELAY_MILLIS_SHORT = "c";
        private static final String CYCLE_DELAY_MILLIS_LONG = "cycleDelay";

        private static final String PDU_DUPLICATION_RATE_SHORT = "dup";
        private static final String PDU_DUPLICATION_RATE_LONG = "duplication";
        private static final String PDU_REORDER_SHORT = "r";
        private static final String PDU_REORDER_LONG = "reorder";
        private static final String PDU_DROP_METADATA_COUNT_SHORT = "dm";
        private static final String PDU_DROP_METADATA_COUNT_LONG = "dropMetadata";
        private static final String PDU_DROP_EOF_COUNT_SHORT = "de";
        private static final String PDU_DROP_EOF_COUNT_LONG = "dropEof";
        private static final String PDU_DROP_FIN_COUNT_SHORT = "df";
        private static final String PDU_DROP_FIN_COUNT_LONG = "dropFin";
        private static final String PDU_DROP_EOF_ACK_COUNT_SHORT = "dea";
        private static final String PDU_DROP_EOF_ACK_COUNT_LONG = "dropEofAck";
        private static final String PDU_DROP_FIN_ACK_COUNT_SHORT = "dfa";
        private static final String PDU_DROP_FIN_ACK_COUNT_LONG = "dropFinAck";
        private static final String PDU_DROP_NAK_COUNT_SHORT = "dn";
        private static final String PDU_DROP_NAK_COUNT_LONG = "dropNak";
        private static final String PDU_DROP_ALL_EXCEPT_METADATA_COUNT_SHORT = "daem";
        private static final String PDU_DROP_ALL_EXCEPT_METADATA_COUNT_LONG = "dropAllExceptMetadata";
        private static final String PDU_DROP_ALL_EXCEPT_EOF_COUNT_SHORT = "daee";
        private static final String PDU_DROP_ALL_EXCEPT_EOF_COUNT_LONG = "dropAllExceptEof";
        private static final String PDU_DROP_FILEDATA_RATE_SHORT = "dfr";
        private static final String PDU_DROP_FILEDATA_RATE_LONG = "dropFiledataRate";
        private static final String PDU_DROP_ALL_DURATION_SECONDS_SHORT = "dad";
        private static final String PDU_DROP_ALL_DURATION_SECONDS_LONG = "dropAllDuration";
        private static final String PDU_CORRUPT_METADATA_COUNT_SHORT = "cm";
        private static final String PDU_CORRUPT_METADATA_COUNT_LONG = "corruptMetadata";
        private static final String PDU_CORRUPT_FILEDATA_RATE_SHORT = "cfr";
        private static final String PDU_CORRUPT_FILEDATA_RATE_LONG = "corruptFiledataRate";
        private static final String PDU_CORRUPT_EOF_COUNT_SHORT = "ce";
        private static final String PDU_CORRUPT_EOF_COUNT_LONG = "corruptEof";
        private static final String PDU_CORRUPT_FIN_COUNT_SHORT = "cf";
        private static final String PDU_CORRUPT_FIN_COUNT_LONG = "corruptFin";
        private static final String PDU_CORRUPT_NAK_COUNT_SHORT = "cn";
        private static final String PDU_CORRUPT_NAK_COUNT_LONG = "corruptNak";
        private static final String PDU_CORRUPT_EOF_ACK_COUNT_SHORT = "cea";
        private static final String PDU_CORRUPT_EOF_ACK_COUNT_LONG = "corruptEofAck";
        private static final String PDU_CORRUPT_FIN_ACK_COUNT_SHORT = "cfa";
        private static final String PDU_CORRUPT_FIN_ACK_COUNT_LONG = "corruptFinAck";
        private static final String PDU_ALTER_METADATA_CRC_COUNT_SHORT = "amc";
        private static final String PDU_ALTER_METADATA_CRC_COUNT_LONG = "alterMetadataCrc";
        private static final String PDU_ALTER_FILEDATA_CRC_RATE_SHORT = "afcr";
        private static final String PDU_ALTER_FILEDATA_CRC_RATE_LONG = "alterFiledataCrcRate";
        private static final String PDU_ALTER_EOF_CRC_COUNT_SHORT = "aec";
        private static final String PDU_ALTER_EOF_CRC_COUNT_LONG = "alterEofCrc";
        private static final String PDU_ALTER_FIN_CRC_COUNT_SHORT = "afc";
        private static final String PDU_ALTER_FIN_CRC_COUNT_LONG = "alterFinCrc";
        private static final String PDU_ALTER_NAK_CRC_COUNT_SHORT = "anc";
        private static final String PDU_ALTER_NAK_CRC_COUNT_LONG = "alterNakCrc";
        private static final String PDU_ALTER_EOF_ACK_CRC_COUNT_SHORT = "aeac";
        private static final String PDU_ALTER_EOF_ACK_CRC_COUNT_LONG = "alterEofAckCrc";
        private static final String PDU_ALTER_FIN_ACK_CRC_COUNT_SHORT = "afac";
        private static final String PDU_ALTER_FIN_ACK_CRC_COUNT_LONG = "alterFinAckCrc";
        private static final String PDU_ALTER_EOF_CHECKSUM_COUNT_SHORT = "aex";
        private static final String PDU_ALTER_EOF_CHECKSUM_COUNT_LONG = "alterEofChecksum";
        private static final String PDU_ALTER_EOF_FILE_SIZE_COUNT_SHORT = "aefs";
        private static final String PDU_ALTER_EOF_FILE_SIZE_COUNT_LONG = "alterEofFileSize";
        private static final String OUTPUT_TO_REST_API_SHORT = "ora";
        private static final String OUTPUT_TO_REST_API_LONG = "outputToRestApi";
        private static final String URL_SHORT = "u";
        private static final String URL_LONG = "url";
        private static final String INPUT_FROM_JMS_SHORT = "ifj";
        private static final String INPUT_FROM_JMS_LONG = "inputFromJms";
        private static final String MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC_SHORT = "irt";
        private static final String MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC_LONG = "inRootTopic";
        private static final String MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC_SHORT = "ort";
        private static final String MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC_LONG = "outRootTopic";

        private final Map<String, CommandLineOption<?>> optionMap;

        private UnsignedInteger overriddenPort;
        private UnsignedInteger cycleDelay;
        private UnsignedInteger duplicationRate;
        private Boolean reorder;
        private UnsignedInteger dropMetadataCount;
        private UnsignedInteger dropEofCount;
        private UnsignedInteger dropFinCount;
        private UnsignedInteger dropEofAckCount;
        private UnsignedInteger dropFinAckCount;
        private UnsignedInteger dropNakCount;
        private UnsignedInteger dropAllExceptMetadataCount;
        private UnsignedInteger dropAllExceptEofCount;
        private UnsignedInteger dropFiledataRate;
        private UnsignedInteger dropAllDurationSeconds;
        private UnsignedInteger corruptMetadataCount;
        private UnsignedInteger corruptFiledataRate;
        private UnsignedInteger corruptEofCount;
        private UnsignedInteger corruptFinCount;
        private UnsignedInteger corruptNakCount;
        private UnsignedInteger corruptEofAckCount;
        private UnsignedInteger corruptFinAckCount;
        private UnsignedInteger alterMetadataCrcCount;
        private UnsignedInteger alterFiledataCrcRate;
        private UnsignedInteger alterEofCrcCount;
        private UnsignedInteger alterFinCrcCount;
        private UnsignedInteger alterNakCrcCount;
        private UnsignedInteger alterEofAckCrcCount;
        private UnsignedInteger alterFinAckCrcCount;
        private UnsignedInteger alterEofChecksumCount;
        private UnsignedInteger alterEofFileSizeCount;
        private Boolean outputToRestApi;
        private String url;
        private Boolean inputFromJms;
        private String messageServiceInboundPduRootTopic;
        private String messageServiceOutboundPduRootTopic;

        private CfdpLinkSimAmpcsProperties ampcsProperties;

        public AmpcsStyleCommandLineHandler() {
            super();
            optionMap = new HashMap<>();
            optionMap.put(PORT_LONG, new UnsignedIntOption(PORT_SHORT, PORT_LONG, "port",
                    "Port number to use for accepting CFDP PDUs", false,
                    UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(65535)));
            optionMap.put(CYCLE_DELAY_MILLIS_LONG, new UnsignedIntOption(CYCLE_DELAY_MILLIS_SHORT, CYCLE_DELAY_MILLIS_LONG, "millis",
                    "Amount of time to delay between cycles (minimum 50)", false,
                    UnsignedInteger.valueOfIntegerAsUnsigned(50), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DUPLICATION_RATE_LONG, new UnsignedIntOption(PDU_DUPLICATION_RATE_SHORT, PDU_DUPLICATION_RATE_LONG, "rate",
                    "Rate of duplicating PDUs, 0 to 100, where 0 is no duplication at all and 100 is always duplicate", false,
                    UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(100)));
            optionMap.put(PDU_REORDER_LONG, new FlagOption(PDU_REORDER_SHORT, PDU_REORDER_LONG,
                    "If present, reorders PDUs",
                    false));
            optionMap.put(PDU_DROP_METADATA_COUNT_LONG, new UnsignedIntOption(PDU_DROP_METADATA_COUNT_SHORT, PDU_DROP_METADATA_COUNT_LONG, "count",
                    "Number of Metadata PDUs to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_EOF_COUNT_LONG, new UnsignedIntOption(PDU_DROP_EOF_COUNT_SHORT, PDU_DROP_EOF_COUNT_LONG, "count",
                    "Number of EOF PDUs to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_FIN_COUNT_LONG, new UnsignedIntOption(PDU_DROP_FIN_COUNT_SHORT, PDU_DROP_FIN_COUNT_LONG, "count",
                    "Number of Finished PDUs to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_EOF_ACK_COUNT_LONG, new UnsignedIntOption(PDU_DROP_EOF_ACK_COUNT_SHORT, PDU_DROP_EOF_ACK_COUNT_LONG, "count",
                    "Number of ACK PDUs for EOF to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_FIN_ACK_COUNT_LONG, new UnsignedIntOption(PDU_DROP_FIN_ACK_COUNT_SHORT, PDU_DROP_FIN_ACK_COUNT_LONG, "count",
                    "Number of ACK PDUs for Finished PDUs to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_NAK_COUNT_LONG, new UnsignedIntOption(PDU_DROP_NAK_COUNT_SHORT, PDU_DROP_NAK_COUNT_LONG, "count",
                    "Number of NAK PDUs to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_ALL_EXCEPT_METADATA_COUNT_LONG, new UnsignedIntOption(PDU_DROP_ALL_EXCEPT_METADATA_COUNT_SHORT, PDU_DROP_ALL_EXCEPT_METADATA_COUNT_LONG, "count",
                    "Number of PDUs other than Metadata PDUs to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_ALL_EXCEPT_EOF_COUNT_LONG, new UnsignedIntOption(PDU_DROP_ALL_EXCEPT_EOF_COUNT_SHORT, PDU_DROP_ALL_EXCEPT_EOF_COUNT_LONG, "count",
                    "Number of PDUs other than EOF PDUs to drop initially (after which dropping stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_DROP_FILEDATA_RATE_LONG, new UnsignedIntOption(PDU_DROP_FILEDATA_RATE_SHORT, PDU_DROP_FILEDATA_RATE_LONG, "rate",
                    "Rate of dropping File Data PDUs, 0 to 100, where 0 is no dropping at all and 100 is always drop", false,
                    UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(100)));
            optionMap.put(PDU_DROP_ALL_DURATION_SECONDS_LONG, new UnsignedIntOption(PDU_DROP_ALL_DURATION_SECONDS_SHORT, PDU_DROP_ALL_DURATION_SECONDS_LONG, "seconds",
                    "Duration of time to drop all PDUs. Timer will start at the first PDU seen. After the duration has passed, all other options (if any) will take effect.", false,
                    UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_CORRUPT_METADATA_COUNT_LONG, new UnsignedIntOption(PDU_CORRUPT_METADATA_COUNT_SHORT, PDU_CORRUPT_METADATA_COUNT_LONG, "count",
                    "Number of Metadata PDUs to corrupt initially (after which corruption stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_CORRUPT_FILEDATA_RATE_LONG, new UnsignedIntOption(PDU_CORRUPT_FILEDATA_RATE_SHORT, PDU_CORRUPT_FILEDATA_RATE_LONG, "rate",
                    "Rate of corrupting File Data PDUs, 0 to 100, where 0 is no corrupting at all and 100 is always corrupt", false,
                    UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(100)));
            optionMap.put(PDU_CORRUPT_EOF_COUNT_LONG, new UnsignedIntOption(PDU_CORRUPT_EOF_COUNT_SHORT, PDU_CORRUPT_EOF_COUNT_LONG, "count",
                    "Number of EOF PDUs to corrupt initially (after which corruption stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_CORRUPT_FIN_COUNT_LONG, new UnsignedIntOption(PDU_CORRUPT_FIN_COUNT_SHORT, PDU_CORRUPT_FIN_COUNT_LONG, "count",
                    "Number of Finished PDUs to corrupt initially (after which corruption stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_CORRUPT_NAK_COUNT_LONG, new UnsignedIntOption(PDU_CORRUPT_NAK_COUNT_SHORT, PDU_CORRUPT_NAK_COUNT_LONG, "count",
                    "Number of NAK PDUs to corrupt initially (after which corruption stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_CORRUPT_EOF_ACK_COUNT_LONG, new UnsignedIntOption(PDU_CORRUPT_EOF_ACK_COUNT_SHORT, PDU_CORRUPT_EOF_ACK_COUNT_LONG, "count",
                    "Number of ACK PDUs for EOF to corrupt initially (after which corruption stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_CORRUPT_FIN_ACK_COUNT_LONG, new UnsignedIntOption(PDU_CORRUPT_FIN_ACK_COUNT_SHORT, PDU_CORRUPT_FIN_ACK_COUNT_LONG, "count",
                    "Number of ACK PDUs for Finished PDUs to corrupt initially (after which corruption stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_METADATA_CRC_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_METADATA_CRC_COUNT_SHORT, PDU_ALTER_METADATA_CRC_COUNT_LONG, "count",
                    "Number of Metadata PDUs to alter the CRC value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_FILEDATA_CRC_RATE_LONG, new UnsignedIntOption(PDU_ALTER_FILEDATA_CRC_RATE_SHORT, PDU_ALTER_FILEDATA_CRC_RATE_LONG, "rate",
                    "Rate of altering File Data PDUs' CRC value, 0 to 100, where 0 is no altering at all and 100 is always alter", false,
                    UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(100)));
            optionMap.put(PDU_ALTER_EOF_CRC_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_EOF_CRC_COUNT_SHORT, PDU_ALTER_EOF_CRC_COUNT_LONG, "count",
                    "Number of EOF PDUs to alter the CRC value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_FIN_CRC_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_FIN_CRC_COUNT_SHORT, PDU_ALTER_FIN_CRC_COUNT_LONG, "count",
                    "Number of Finished PDUs to alter the CRC value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_NAK_CRC_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_NAK_CRC_COUNT_SHORT, PDU_ALTER_NAK_CRC_COUNT_LONG, "count",
                    "Number of NAK PDUs to alter the CRC value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_EOF_ACK_CRC_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_EOF_ACK_CRC_COUNT_SHORT, PDU_ALTER_EOF_ACK_CRC_COUNT_LONG, "count",
                    "Number of ACK PDUs for EOF to alter the CRC value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_FIN_ACK_CRC_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_FIN_ACK_CRC_COUNT_SHORT, PDU_ALTER_FIN_ACK_CRC_COUNT_LONG, "count",
                    "Number of ACK PDUs for Finished PDUs to alter the CRC value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_EOF_CHECKSUM_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_EOF_CHECKSUM_COUNT_SHORT, PDU_ALTER_EOF_CHECKSUM_COUNT_LONG, "count",
                    "Number of EOF PDUs to alter the checksum value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(PDU_ALTER_EOF_FILE_SIZE_COUNT_LONG, new UnsignedIntOption(PDU_ALTER_EOF_FILE_SIZE_COUNT_SHORT, PDU_ALTER_EOF_FILE_SIZE_COUNT_LONG, "count",
                    "Number of EOF PDUs to alter the file size value initially (after which altering stops)",
                    false, UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(Integer.MAX_VALUE)));
            optionMap.put(OUTPUT_TO_REST_API_LONG, new FlagOption(OUTPUT_TO_REST_API_SHORT, OUTPUT_TO_REST_API_LONG,
                    "If present, output PDUs to Auto Proxy REST API using the configured URL instead of publishing to JMS",
                    false));
            optionMap.put(URL_LONG, new StringOption(URL_SHORT, URL_LONG, "url",
                    "URL to send the outbound PDUs to (i.e. Auto Proxy URL). When supplied, PDUs will be output to REST API and not published to JMS",
                    false));
            optionMap.put(INPUT_FROM_JMS_LONG, new FlagOption(INPUT_FROM_JMS_SHORT, INPUT_FROM_JMS_LONG,
                    "If present, input PDUs should be consumed from JMS and not REST API",
                    false));
            optionMap.put(MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC_LONG, new StringOption(MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC_SHORT, MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC_LONG, "rootTopic",
                    "JMS root topic for subscribing to inbound PDUs",
                    false));
            optionMap.put(MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC_LONG, new StringOption(MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC_SHORT, MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC_LONG, "rootTopic",
                    "JMS root topic for publishing outbound PDUs",
                    false));

            ampcsProperties = new CfdpLinkSimAmpcsProperties();
        }

        /*
         * (non-Javadoc)
         *
         * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
         */
        @Override
        public BaseCommandOptions createOptions() {
            if (optionsCreated.get()) {
                return options;
            }
            super.createOptions();

            for (final CommandLineOption<?> clo : optionMap.values()) {
                options.addOption(clo);
            }

            return options;
        }

        public Options createCommonsCliOptions() {
            final Options options = new Options();
            final OptionSet optionSet = createOptions().getOptions();

            for (final ICommandLineOption<?> clo : optionSet.getAllOptions()) {
                options.addOption(clo.getOpt(), clo.getLongOpt(), clo.hasArg(), clo.getDescription());
            }

            return options;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.
         * cmdline.ICommandLine)
         */
        @Override
        public void configure(final ICommandLine commandLine) throws ParseException {
            super.configure(commandLine);
            overriddenPort = (UnsignedInteger) optionMap.get(PORT_LONG).parse(commandLine);
            cycleDelay = (UnsignedInteger) optionMap.get(CYCLE_DELAY_MILLIS_LONG).parse(commandLine);
            duplicationRate = (UnsignedInteger) optionMap.get(PDU_DUPLICATION_RATE_LONG).parse(commandLine);
            reorder = (Boolean) optionMap.get(PDU_REORDER_LONG).parse(commandLine);
            dropMetadataCount = (UnsignedInteger) optionMap.get(PDU_DROP_METADATA_COUNT_LONG).parse(commandLine);
            dropEofCount = (UnsignedInteger) optionMap.get(PDU_DROP_EOF_COUNT_LONG).parse(commandLine);
            dropFinCount = (UnsignedInteger) optionMap.get(PDU_DROP_FIN_COUNT_LONG).parse(commandLine);
            dropEofAckCount = (UnsignedInteger) optionMap.get(PDU_DROP_EOF_ACK_COUNT_LONG).parse(commandLine);
            dropFinAckCount = (UnsignedInteger) optionMap.get(PDU_DROP_FIN_ACK_COUNT_LONG).parse(commandLine);
            dropNakCount = (UnsignedInteger) optionMap.get(PDU_DROP_NAK_COUNT_LONG).parse(commandLine);
            dropAllExceptMetadataCount = (UnsignedInteger) optionMap.get(PDU_DROP_ALL_EXCEPT_METADATA_COUNT_LONG).parse(commandLine);
            dropAllExceptEofCount = (UnsignedInteger) optionMap.get(PDU_DROP_ALL_EXCEPT_EOF_COUNT_LONG).parse(commandLine);
            dropFiledataRate = (UnsignedInteger) optionMap.get(PDU_DROP_FILEDATA_RATE_LONG).parse(commandLine);
            dropAllDurationSeconds = (UnsignedInteger) optionMap.get(PDU_DROP_ALL_DURATION_SECONDS_LONG).parse(commandLine);
            corruptMetadataCount = (UnsignedInteger) optionMap.get(PDU_CORRUPT_METADATA_COUNT_LONG).parse(commandLine);
            corruptFiledataRate = (UnsignedInteger) optionMap.get(PDU_CORRUPT_FILEDATA_RATE_LONG).parse(commandLine);
            corruptEofCount = (UnsignedInteger) optionMap.get(PDU_CORRUPT_EOF_COUNT_LONG).parse(commandLine);
            corruptFinCount = (UnsignedInteger) optionMap.get(PDU_CORRUPT_FIN_COUNT_LONG).parse(commandLine);
            corruptNakCount = (UnsignedInteger) optionMap.get(PDU_CORRUPT_NAK_COUNT_LONG).parse(commandLine);
            corruptEofAckCount = (UnsignedInteger) optionMap.get(PDU_CORRUPT_EOF_ACK_COUNT_LONG).parse(commandLine);
            corruptFinAckCount = (UnsignedInteger) optionMap.get(PDU_CORRUPT_FIN_ACK_COUNT_LONG).parse(commandLine);
            alterMetadataCrcCount = (UnsignedInteger) optionMap.get(PDU_ALTER_METADATA_CRC_COUNT_LONG).parse(commandLine);
            alterFiledataCrcRate = (UnsignedInteger) optionMap.get(PDU_ALTER_FILEDATA_CRC_RATE_LONG).parse(commandLine);
            alterEofCrcCount = (UnsignedInteger) optionMap.get(PDU_ALTER_EOF_CRC_COUNT_LONG).parse(commandLine);
            alterFinCrcCount = (UnsignedInteger) optionMap.get(PDU_ALTER_FIN_CRC_COUNT_LONG).parse(commandLine);
            alterNakCrcCount = (UnsignedInteger) optionMap.get(PDU_ALTER_NAK_CRC_COUNT_LONG).parse(commandLine);
            alterEofAckCrcCount = (UnsignedInteger) optionMap.get(PDU_ALTER_EOF_ACK_CRC_COUNT_LONG).parse(commandLine);
            alterFinAckCrcCount = (UnsignedInteger) optionMap.get(PDU_ALTER_FIN_ACK_CRC_COUNT_LONG).parse(commandLine);
            alterEofChecksumCount = (UnsignedInteger) optionMap.get(PDU_ALTER_EOF_CHECKSUM_COUNT_LONG).parse(commandLine);
            alterEofFileSizeCount = (UnsignedInteger) optionMap.get(PDU_ALTER_EOF_FILE_SIZE_COUNT_LONG).parse(commandLine);
            outputToRestApi = (Boolean) optionMap.get(OUTPUT_TO_REST_API_LONG).parse(commandLine);
            url = (String) optionMap.get(URL_LONG).parse(commandLine);
            inputFromJms = (Boolean) optionMap.get(INPUT_FROM_JMS_LONG).parse(commandLine);
            messageServiceInboundPduRootTopic = (String) optionMap.get(MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPIC_LONG).parse(commandLine);
            messageServiceOutboundPduRootTopic = (String) optionMap.get(MESSAGE_SERVICE_OUTBOUND_PDU_ROOT_TOPIC_LONG).parse(commandLine);

            if (dropAllExceptMetadataCount != null && dropAllExceptMetadataCount.intValue() > 0 && dropAllExceptEofCount != null && dropAllExceptEofCount.intValue() > 0) {
                System.err.println("Arguments conflict - Only one of the following options may be provided: " + Arrays.asList(PDU_DROP_ALL_EXCEPT_METADATA_COUNT_LONG, PDU_DROP_ALL_EXCEPT_EOF_COUNT_LONG));
                exit(1);
            }

            int clashingOptionsCounter = 0;

            if (dropMetadataCount != null && dropMetadataCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (corruptMetadataCount != null && corruptMetadataCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterMetadataCrcCount != null && alterMetadataCrcCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (clashingOptionsCounter > 1) {
                System.err.println("Arguments conflict - Only one of the following options may be provided: "
                        + Arrays.asList(PDU_DROP_METADATA_COUNT_LONG,
                        PDU_CORRUPT_METADATA_COUNT_LONG,
                        PDU_ALTER_METADATA_CRC_COUNT_LONG));
                exit(1);
            }

            clashingOptionsCounter = 0;

            if (dropEofCount != null && dropEofCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (corruptEofCount != null && corruptEofCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterEofCrcCount != null && alterEofCrcCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterEofChecksumCount != null && alterEofChecksumCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterEofFileSizeCount != null && alterEofFileSizeCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (clashingOptionsCounter > 1) {
                System.err.println("Arguments conflict - Only one of the following options may be provided: "
                        + Arrays.asList(PDU_DROP_EOF_COUNT_LONG,
                        PDU_CORRUPT_EOF_COUNT_LONG,
                        PDU_ALTER_EOF_CRC_COUNT_LONG,
                        PDU_ALTER_EOF_CHECKSUM_COUNT_LONG,
                        PDU_ALTER_EOF_FILE_SIZE_COUNT_LONG));
                exit(1);
            }

            clashingOptionsCounter = 0;

            if (dropFinCount != null && dropFinCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (corruptFinCount != null && corruptFinCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterFinCrcCount != null && alterFinCrcCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (clashingOptionsCounter > 1) {
                System.err.println("Arguments conflict - Only one of the following options may be provided: "
                        + Arrays.asList(PDU_DROP_FIN_COUNT_LONG,
                        PDU_CORRUPT_FIN_COUNT_LONG,
                        PDU_ALTER_FIN_CRC_COUNT_LONG));
                exit(1);
            }

            clashingOptionsCounter = 0;

            if (dropNakCount != null && dropNakCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (corruptNakCount != null && corruptNakCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterNakCrcCount != null && alterNakCrcCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (clashingOptionsCounter > 1) {
                System.err.println("Arguments conflict - Only one of the following options may be provided: "
                        + Arrays.asList(PDU_DROP_NAK_COUNT_LONG,
                        PDU_CORRUPT_NAK_COUNT_LONG,
                        PDU_ALTER_NAK_CRC_COUNT_LONG));
                exit(1);
            }

            clashingOptionsCounter = 0;

            if (dropEofAckCount != null && dropEofAckCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (corruptEofAckCount != null && corruptEofAckCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterEofAckCrcCount != null && alterEofAckCrcCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (clashingOptionsCounter > 1) {
                System.err.println("Arguments conflict - Only one of the following options may be provided: "
                        + Arrays.asList(PDU_DROP_EOF_ACK_COUNT_LONG,
                        PDU_CORRUPT_EOF_ACK_COUNT_LONG,
                        PDU_ALTER_EOF_ACK_CRC_COUNT_LONG));
                exit(1);
            }

            clashingOptionsCounter = 0;

            if (dropFinAckCount != null && dropFinAckCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (corruptFinAckCount != null && corruptFinAckCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (alterFinAckCrcCount != null && alterFinAckCrcCount.intValue() > 0) {
                clashingOptionsCounter++;
            }

            if (clashingOptionsCounter > 1) {
                System.err.println("Arguments conflict - Only one of the following options may be provided: "
                        + Arrays.asList(PDU_DROP_FIN_ACK_COUNT_LONG,
                        PDU_CORRUPT_FIN_ACK_COUNT_LONG,
                        PDU_ALTER_FIN_ACK_CRC_COUNT_LONG));
                exit(1);
            }

            if (outputToRestApi && (url == null || "null".equalsIgnoreCase(url))) {
                url = ampcsProperties.getRestApiOutputUrl();

                if (url == null) {
                    System.err.println("No default REST API URL configured for outbound PDUs");
                    System.exit(1);
                }

            }

        }

        public UnsignedInteger getOverriddenPort() {
            return overriddenPort;
        }

        public UnsignedInteger getCycleDelay() {
            return cycleDelay;
        }

        public UnsignedInteger getDuplicationRate() {
            return duplicationRate;
        }

        public Boolean getReorder() {
            return reorder;
        }

        public UnsignedInteger getDropMetadataCount() {
            return dropMetadataCount;
        }

        public UnsignedInteger getDropEofCount() {
            return dropEofCount;
        }

        public UnsignedInteger getDropFinCount() {
            return dropFinCount;
        }

        public UnsignedInteger getDropEofAckCount() {
            return dropEofAckCount;
        }

        public UnsignedInteger getDropFinAckCount() {
            return dropFinAckCount;
        }

        public UnsignedInteger getDropNakCount() {
            return dropNakCount;
        }

        public UnsignedInteger getDropAllExceptMetadataCount() {
            return dropAllExceptMetadataCount;
        }

        public UnsignedInteger getDropAllExceptEofCount() {
            return dropAllExceptEofCount;
        }

        public UnsignedInteger getDropFiledataRate() {
            return dropFiledataRate;
        }

        public UnsignedInteger getDropAllDurationSeconds() {
            return dropAllDurationSeconds;
        }

        public UnsignedInteger getCorruptMetadataCount() {
            return corruptMetadataCount;
        }

        public UnsignedInteger getCorruptFiledataRate() {
            return corruptFiledataRate;
        }

        public UnsignedInteger getCorruptEofCount() {
            return corruptEofCount;
        }

        public UnsignedInteger getCorruptFinCount() {
            return corruptFinCount;
        }

        public UnsignedInteger getCorruptNakCount() {
            return corruptNakCount;
        }

        public UnsignedInteger getCorruptEofAckCount() {
            return corruptEofAckCount;
        }

        public UnsignedInteger getCorruptFinAckCount() {
            return corruptFinAckCount;
        }

        public UnsignedInteger getAlterMetadataCrcCount() {
            return alterMetadataCrcCount;
        }

        public UnsignedInteger getAlterFiledataCrcRate() {
            return alterFiledataCrcRate;
        }

        public UnsignedInteger getAlterEofCrcCount() {
            return alterEofCrcCount;
        }

        public UnsignedInteger getAlterFinCrcCount() {
            return alterFinCrcCount;
        }

        public UnsignedInteger getAlterNakCrcCount() {
            return alterNakCrcCount;
        }

        public UnsignedInteger getAlterEofAckCrcCount() {
            return alterEofAckCrcCount;
        }

        public UnsignedInteger getAlterFinAckCrcCount() {
            return alterFinAckCrcCount;
        }

        public UnsignedInteger getAlterEofChecksumCount() {
            return alterEofChecksumCount;
        }

        public UnsignedInteger getAlterEofFileSizeCount() {
            return alterEofFileSizeCount;
        }

        public Boolean getOutputToRestApi() {
            return outputToRestApi;
        }

        public String getUrl() {
            return url;
        }

        public Boolean getInputFromJms() {
            return inputFromJms;
        }

        public String getMessageServiceInboundPduRootTopic() {
            return messageServiceInboundPduRootTopic;
        }

        public String getMessageServiceOutboundPduRootTopic() {
            return messageServiceOutboundPduRootTopic;
        }

        /*
         * (non-Javadoc)
         *
         * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
         */
        @Override
        public void showHelp() {
            /*
             * MPCS-8798 caused help text to be displayed twice, so applying the new check
             * required to make sure it doesn't do that.
             */

            if (helpDisplayed.getAndSet(true)) {
                return;
            }

            super.showHelp();
        }

    }

}