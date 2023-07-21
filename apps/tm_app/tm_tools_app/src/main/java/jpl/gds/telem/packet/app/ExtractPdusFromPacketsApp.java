/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.telem.packet.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.cfdp.ICfdpEofPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpFileDataPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpMetadataPdu;
import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.options.VcidOption;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;
import jpl.gds.tm.service.api.cfdp.IPduExtractService;
import jpl.gds.tm.service.api.packet.IPacketMessageFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * An application class for extracting CFDP PDUs from telemetry packets.
 * 
 * @since R8
 */
public class ExtractPdusFromPacketsApp extends AbstractCommandLineApp implements MessageSubscriber {

    private final FileOption         packetFileOpt  = new FileOption(null, "packetFile", "file path",
                                                                     "input packet file", true, true);
    private VcidOption               vcidOpt;
    private final FileOption         outputFileOpt  = new FileOption(null, "outputFile", "file path",
                                                                     "binary PDU output file", false, false);
    /*
     * Josh Choi 5/31/2016: Adding an option to save PDU files to a directory
     * instead. User will only be able to select either the outputFile option or
     * the outputDir option. When outputDir option is selected, each PDU will be
     * saved as an individual file in the output directory.
     */
    private final DirectoryOption    outputDirOpt   = new DirectoryOption(null, "outputDir", "directory path",
                                                                          "directory to save PDU output files", false,
                                                                          false);
    private final FlagOption         dropDataOption = new FlagOption(null, "dropData", "randomly drop file data PDUs",
                                                                     false);
    private final FlagOption         dropMetaOption = new FlagOption(null, "dropMetadata", "drop all metadata PDUs",
                                                                     false);
    private final FlagOption         dropEofOption  = new FlagOption(null, "dropEof", "drop all EOF PDUs", false);

    private String                   inputFile;
    private int                      vcid;
    private FileOutputStream         outputFile;
    private File                     outputDirFile;
    /*
     * Josh Choi 5/31/2016: fileCounter will keep track of the number of PDU files created.
     */
    private int                      fileCounter;
    private boolean                  dropData;
    private boolean                  dropMetadata;
    private boolean                  dropEof;
    private final Random             random         = new Random();
    private boolean                  first          = true;
    private final ApplicationContext commonContext;
    private final Tracer                   tracer;

    /**
     * Constructor.
     */
    public ExtractPdusFromPacketsApp() {
        fileCounter = 0;

        commonContext = SpringContextFactory.getSpringContext(true);
        this.tracer = TraceManager.getDefaultTracer(commonContext);
    }

    /**
     * Creates command line options enclosed in a BaseCommandOptions object. This default
     * implementation adds HELP and VERSION options and defines the BaseCommandOptions
     * to support aliasing.
     * 
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.app.ICommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {

        /*
         * Change to use and return new
         * BaseCommandOptions object, in aliasing mode.
         */

        if (optionsCreated.get()) {
            return options;
        }

        options = super.createOptions();
        options.addOption(packetFileOpt);
        vcidOpt = new VcidOption(commonContext.getBean(MissionProperties.class), true);
        options.addOption(vcidOpt);
        options.addOption(outputFileOpt);
        options.addOption(outputDirOpt);
        options.addOption(dropDataOption);
        options.addOption(dropMetaOption);
        options.addOption(dropEofOption);

        return options;

    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        inputFile = packetFileOpt.parse(commandLine);
        vcid = vcidOpt.parse(commandLine).intValue();

        final String outputFileName = outputFileOpt.parse(commandLine);
        final String outputDirName = outputDirOpt.parse(commandLine);

        if (outputFileName != null && outputDirName != null) {
            throw new ParseException("Cannot supply both " + outputFileOpt.getLongOpt() + " and "
                    + outputDirOpt.getLongOpt() + " options");
        }

        if (outputFileName != null) {
            try {
                outputFile = new FileOutputStream(outputFileName);
            }
            catch (final IOException e) {
                throw new ParseException("IO Error opening output file " + outputFileName + ": " + e.toString());
            }
        }
        else if (outputDirName != null) {
            outputDirFile = new File(outputDirName);

            if (!outputDirFile.exists() && !outputDirFile.mkdir()) {
                throw new ParseException("Failed to create directory " + outputDirName);
            }
        }

        this.dropData = dropDataOption.parse(commandLine);
        this.dropMetadata = dropMetaOption.parse(commandLine);
        this.dropEof = dropEofOption.parse(commandLine);

        try {
            commonContext.getBean(FlightDictionaryLoadingStrategy.class).enableApid()
                         .loadAllEnabled(commonContext, false);
        }
        catch (final DictionaryException e) {
            throw new ParseException("Could not load Frame dictionary: " + e.getMessage());
        }

    }

    /**
     * Executes the main body of logic: extracts PDUs from input frame file and writes them out.
     */
    public void run() {

        tracer.info("Processing packet file " + inputFile + " as VC " + vcid);

        final IMessagePublicationBus bus = commonContext.getBean(IMessagePublicationBus.class);
        bus.subscribe(TmServiceMessageType.CfdpPdu, this);
        bus.subscribe(TmServiceMessageType.TelemetryPacketSummary, this);
        bus.subscribe(CommonMessageType.EndOfData, this);

        final List<IService> services = new ArrayList<>();

        commonContext.getBean(IContextFilterInformation.class).setVcid(vcid);

        try {

            final String type = commonContext.getBean(CcsdsProperties.class).getPacketHeaderFormat().getType()
                                             .toString();

            services.add(commonContext.getBean(IPduExtractService.class, type, vcid));

            for (final IService service : services) {
                service.startService();
            }

        }
        catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        final IPacketMessageFactory msgFactory = commonContext.getBean(IPacketMessageFactory.class);
        final IStatusMessageFactory statusMsgFactory = commonContext.getBean(IStatusMessageFactory.class);

        final CcsdsProperties packetProps = commonContext.getBean(CcsdsProperties.class);

        final int primaryHeaderLength = PacketHeaderFactory.create(packetProps.getPacketHeaderFormat())
                                                           .getPrimaryHeaderLength();

        try (FileInputStream fis = new FileInputStream(inputFile)) {

            int entirePacketLength;
            Double bitRate = 0.0;
            final int scid = commonContext.getBean(MissionProperties.class).getDefaultScid();
            Integer configuredStation = commonContext.getBean(IContextFilterInformation.class).getDssId();
            if (configuredStation == null) {
                configuredStation = StationIdHolder.UNSPECIFIED_VALUE;
            }

            final byte[] headerBuffer = new byte[primaryHeaderLength];
            byte[] packetBuffer = null;
            ISpacePacketHeader iPacketHeader;
            ISecondaryPacketHeader secHeader;

            int len = 0;
            final Long startTime = System.currentTimeMillis();

            boolean endOfData = false;

            /*
             * Unfortunately we have nothing as clean/simple as IFrameMessageFactory.createPresyncFrameMessage for
             * getting packets out of a file. Copied over simplified code from PacketStreamProcessor but use
             * ITelemetryPacketMessage, to streamline the process.
             */

            while (!endOfData) {
                len = 0;
                int tmp = 0;

                // read the packet header into a buffer
                while (len < primaryHeaderLength && !endOfData) {

                    tmp = fis.read(headerBuffer, len, primaryHeaderLength - len);
                    if (tmp >= 0) {
                        len += tmp;
                    }
                    else {
                        endOfData = true;
                    }
                }

                if (endOfData) {
                    break;
                }

                final long endTime = System.currentTimeMillis();
                final IAccurateDateTime nominalErt = new AccurateDateTime();

                // if this is not true, assume the bit rate hasn't changed
                if (endTime > startTime) {
                    bitRate = (len * 8) / ((endTime - startTime) / 1000.0);
                }

                // create and populate a packet header object
                iPacketHeader = PacketHeaderFactory.create(packetProps.getPacketHeaderFormat());
                iPacketHeader.setPrimaryValuesFromBytes(headerBuffer, 0);

                if (!iPacketHeader.isValid()) {
                    final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN,
                                                                                                     "Packet abandoned because header was corrupt "
                                                                                                             + "apid="
                                                                                                             + iPacketHeader.getApid()
                                                                                                             + " isValid=false"
                                                                                                             + " len="
                                                                                                             + iPacketHeader.getPacketDataLength()
                                                                                                             + " version="
                                                                                                             + iPacketHeader.getVersionNumber()
                                                                                                             + " type="
                                                                                                             + iPacketHeader.getPacketType(),
                                                                                                     LogMessageType.INVALID_PKT_HEADER);

                    bus.publish(logm);
                    continue;
                }

                // fill in the entire byte representation of the packet
                entirePacketLength = primaryHeaderLength + iPacketHeader.getPacketDataLength() + 1;
                packetBuffer = new byte[entirePacketLength];
                final ISecondaryPacketHeaderExtractor extractor = commonContext.getBean(ISecondaryPacketHeaderLookup.class)
                                                                               .lookupExtractor(iPacketHeader);
                if (!extractor.hasEnoughBytes(packetBuffer, iPacketHeader.getPrimaryHeaderLength())) {
                    final IPublishableLogMessage logm = statusMsgFactory.createPublishableLogMessage(TraceSeverity.WARN,
                                                                                                     "Packet abandoned because it is too short "
                                                                                                             + "apid="
                                                                                                             + iPacketHeader.getApid()
                                                                                                             + " isValid=false"
                                                                                                             + " len="
                                                                                                             + iPacketHeader.getPacketDataLength()
                                                                                                             + " version="
                                                                                                             + iPacketHeader.getVersionNumber()
                                                                                                             + " type="
                                                                                                             + iPacketHeader.getPacketType(),
                                                                                                     LogMessageType.INVALID_PKT_DATA);

                    bus.publish(logm);
                    this.tracer.warn(logm);
                    continue;
                }

                secHeader = extractor.extract(packetBuffer, iPacketHeader.getPrimaryHeaderLength());

                System.arraycopy(headerBuffer, 0, packetBuffer, 0, primaryHeaderLength);

                // packet header extracted, get packet body

                len = 0;
                int offset = primaryHeaderLength;
                tmp = 0;
                while (len < (iPacketHeader.getPacketDataLength() + 1) && !endOfData) {

                    tmp = fis.read(packetBuffer, offset, iPacketHeader.getPacketDataLength() + 1 - len);
                    if (tmp >= 0) {
                        len += tmp;
                        offset += tmp;
                    }
                    else {
                        endOfData = true;
                    }
                }

                if (endOfData) {
                    break;
                }

                final ITelemetryPacketInfo pktInfo = commonContext.getBean(ITelemetryPacketInfoFactory.class)
                                                                  .create(iPacketHeader, entirePacketLength, secHeader);
                pktInfo.setErt(new AccurateDateTime());

                // Have to set the scet directly.
                pktInfo.setScet(SclkScetUtility.getScet(secHeader.getSclk(), null, scid));
                pktInfo.setScid(scid);
                pktInfo.setVcid(vcid);
                pktInfo.setErt(nominalErt);
                pktInfo.setBitRate(bitRate);
                final ITelemetryPacketMessage pktMsg = msgFactory.createTelemetryPacketMessage(pktInfo, null,
                                                                                               HeaderHolder.NULL_HOLDER,
                                                                                               TrailerHolder.NULL_HOLDER,
                                                                                               FrameIdHolder.UNSUPPORTED);
                pktMsg.setPacket(packetBuffer, packetBuffer.length);
                bus.publish(pktMsg);
            }

            final IPublishableLogMessage msg = commonContext.getBean(IStatusMessageFactory.class)
                                                            .createEndOfDataMessage();
            bus.publish(msg);
            this.tracer.log(msg);

        }
        catch (final IOException e) {
            e.printStackTrace();
            return;

        }
        finally {
            for (final IService service : services) {
                if (service != null) {
                    service.stopService();
                }
            }
            if (outputFile != null) {
                try {
                    outputFile.close();
                }
                catch (final IOException e2) {
                }

            }
            bus.unsubscribeAll();
        }

        tracer.info("Done processing packet file " + inputFile + " as VC " + vcid);

    }

    @Override
    public void handleMessage(final IMessage m) {
        if (m instanceof ICfdpPduMessage) {
            final ICfdpPduMessage pduM = (ICfdpPduMessage) m;
            if (dropMetadata && pduM.getPdu() instanceof ICfdpMetadataPdu) {
                System.out.println("DROPPED: " + m.getOneLineSummary());
                return;
            }
            if (dropEof && pduM.getPdu() instanceof ICfdpEofPdu) {
                System.out.println("DROPPED: " + m.getOneLineSummary());
                return;
            }
            if (dropData && pduM.getPdu() instanceof ICfdpFileDataPdu) {
                // We have to make sure we drop at least one and we don't know
                // how many there are, so we always drop the first one
                if (first) {
                    first = false;
                    System.out.println("DROPPED: " + m.getOneLineSummary());
                    return;
                }
                else {
                    // Otherwise, drop them about 25% of the time
                    if (random.nextInt(100) <= 25) {
                        System.out.println("DROPPED: " + m.getOneLineSummary());
                        return;
                    }
                }
            }
            if (outputFile != null) {
                try {

                    outputFile.write(pduM.getPdu().getData());
                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            else if (outputDirFile != null) {
                FileOutputStream out = null;

                try {
                    out = new FileOutputStream(outputDirFile.getAbsolutePath() + File.separator
                            + String.format("%03d", fileCounter++) + ".pdu");
                    out.write(pduM.getPdu().getData());
                }
                catch (final FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                }
                catch (final IOException ie) {
                    ie.printStackTrace();
                }
                finally {

                    try {
                        out.close();
                    }
                    catch (final IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        }
        System.out.println(m.getOneLineSummary());
    }

    /**
     * The main application entry point.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {
        final ExtractPdusFromPacketsApp theApp = new ExtractPdusFromPacketsApp();
        try {
            /*
             * Use createOptions() rather than
             * creating a new reserved/base options object.
             */
            final ICommandLine commandLine = theApp.createOptions().parseCommandLine(args, true);
            theApp.configure(commandLine);

            theApp.run();

        }
        catch (final ParseException e) {
            if (e.getMessage() == null) {
                TraceManager.getDefaultTracer().error(e.toString());
            }
            else {
                TraceManager.getDefaultTracer().error(e.getMessage());
            }
            System.exit(1);
        }
        catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

}
