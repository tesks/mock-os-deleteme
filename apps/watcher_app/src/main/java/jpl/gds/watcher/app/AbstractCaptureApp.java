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
package jpl.gds.watcher.app;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.options.SpacecraftIdOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.HexadecimalStringOptionParser;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.watcher.app.handler.ICaptureHandler;

/**
 * This class is used to consolidate all of the functionality that is identical
 * between capture applications.
 * The CaptureApp sets up and handles passing messages to the CaptureHandler
 */
public abstract class AbstractCaptureApp extends AbstractCommandLineApp implements IQuitSignalHandler {
    /** filename option */
    public static final String                   BASE_FILENAME          = "filename";
    /** dataFilename option */
    public static final String                   DATA_FILENAME          = "dataFilename";
    /** metadataFilename option */
    public static final String                   META_FILENAME          = "metadataFilename";
    /** queueSize option */
    public static final String                   QUEUE_SIZE             = "queueSize";
    /** fwdHost option */
    public static final String                   FWD_HOST               = "fwdHost";
    /** fwdPort option */
    public static final String                   FWD_PORT               = "fwdPort";
    /** syncMark option */
    public static final String                   SYNC_MARK              = "syncMark";
    /** captureMessages option */
    public static final String                   CAPTURE_MESSAGES       = "captureMessages";

    /** return value for successful application execution */
    protected static final int                   SUCCESS                = 0;
    /** return value for unsuccessful application execution */
    protected static final int                   FAILURE                = 1;
    
    /** the spring application context */
    protected final ApplicationContext           appContext;
    /** Message handlers listen to each topic and retrieve the associated messages  */
    protected final List<IQueuingMessageHandler> messageHandlers        = new LinkedList<>();
    /** Number of messages that can be held in a messageHandler */
    protected UnsignedInteger                    queueSize;
    /** Trace logger */
    protected final Tracer                       log;
    /** current status value of the application execution */
    protected int                                exitStatus             = SUCCESS;

    /** The capture handler performs all actions necessary on caputred messages */
    protected ICaptureHandler                    capture                = null;
    /** The types of messages to be captured */
    protected final Collection<IMessageType>     messageTypes           = new LinkedList<>();
    /** The spacecraft ID to be used as part of the message filtering */
    protected UnsignedInteger                    scid;
    
    
    private static final int                   DEFAULT_QUEUE_SIZE     = 65536;

    // station header flag
    private MessageServiceCommandOptions       jmsOpts;
    private ContextCommandOptions              contextOpts;
    private final OutputFormatOption             formatOpt              = new OutputFormatOption(false);
    private boolean                              useOutputFormatOption  = false;
    private String                               outputFormat;
    private boolean                              captureMessages;
    private String                               fwdHost;
    private UnsignedInteger                      fwdPort;
    private String                               syncMark;
    private String                               baseName;
    private String                               tempDataName;
    private String                               tempMetaName;

    private final FileOption                   captureFileOpt         = new FileOption("f", BASE_FILENAME,
            BASE_FILENAME,
            "The supplied filename is used to create a captured data file and a metadata file. Captured data is written to these files as they are received",
            false, false);
    private final FileOption                   captureDataFileOpt     = new FileOption("d", DATA_FILENAME,
            DATA_FILENAME, "Writes the captured data to a single file as messages containing it are received.", false, false);
    private final FileOption                   captureMetadataFileOpt = new FileOption("m", META_FILENAME,
            META_FILENAME, "Writes the metadata of captured data to a single file as messages containing data are received.", false, false);
    /** flag option to capture protobuf messages instead of the object alone */
    protected final FlagOption                   captureMessagesOpt     = new FlagOption(null, CAPTURE_MESSAGES,
            "Capture binary (Protobuf3) formatted messages, containing the received data, instead of data alone.");

    private final StringOption                   hostOption             = new StringOption(null, FWD_HOST, "host",
            "The hostname of the machine that will receive the forwarded data", false);
    private final PortOption                     portOption             = new PortOption(null, FWD_PORT, "port",
            "The port of the machine that will recevie the forwarded data", false);
    private final StringOption                   syncMarkOption         = new StringOption(null, SYNC_MARK, "mark",
            "Hex value to be placed between each message sent on the socket.", false);

    private final UnsignedIntOption            queueSizeOpt           = new UnsignedIntOption(null, QUEUE_SIZE, "size",
            "Length of the internal message queue as number of elements. MUST BE A POWER OF 2. Defaults to "
                    + DEFAULT_QUEUE_SIZE,
            false);

    private SpacecraftIdOption                 scidOption;

    private final AtomicBoolean                stopping               = new AtomicBoolean(false);

    private Socket                             clientSocket;
    private DataOutputStream                   socketOutput;
    /** the String name of the message type to be captured */
    protected final String                     captureType;
    
    /**
     * Constructor
     * 
     * @param msgType
     *            the type of messages the capture client will be handling
     * @param captureType
     *            The name of the message type, and subtopic, to be received.
     * @param useOutputFormatOption
     *            enable the output format option if the message type to be captured has a template
     *            directory.
     */
    public AbstractCaptureApp(final TmServiceMessageType msgType, final String captureType,
            final boolean useOutputFormatOption) {
        appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getTracer(appContext, Loggers.WATCHER);
        messageTypes.add(msgType);
        this.captureType = captureType;
        this.useOutputFormatOption = useOutputFormatOption;
    }
    
    @Override
    public BaseCommandOptions createOptions() {
        
        if (optionsCreated.get()) {
            return options;
        }

        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        jmsOpts = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
        options.addOptions(jmsOpts.getAllOptionsWithoutNoJms());

        contextOpts = new ContextCommandOptions(appContext.getBean(IContextConfiguration.class));
        options.addOption(contextOpts.SUBSCRIBER_TOPICS);

        scidOption = new SpacecraftIdOption(appContext.getBean(MissionProperties.class), false);
        options.addOption(scidOption);

        if (this.useOutputFormatOption) {
            options.addOption(formatOpt);
        }
        options.addOption(captureFileOpt);
        options.addOption(captureDataFileOpt);
        options.addOption(captureMetadataFileOpt);
        options.addOption(captureMessagesOpt);

        options.addOption(hostOption);
        options.addOption(portOption);
        options.addOption(syncMarkOption);
        syncMarkOption.setParser(new HexadecimalStringOptionParser(true));

        options.addOption(queueSizeOpt);
        queueSizeOpt.setDefaultValue(UnsignedInteger.valueOf(DEFAULT_QUEUE_SIZE));

        return options;
    }
    
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        if (capture == null) {
            throw new IllegalStateException("Capture handler must be instantiated before this configure is called.");
        }
        super.configure(commandLine);
        
        contextOpts.SUBSCRIBER_TOPICS.parse(commandLine);

        jmsOpts.parseAllOptionsAsOptional(commandLine);

        capture.setCaptureMessageFilter(messageTypes);

        if (this.useOutputFormatOption) {
            // Parse option for default message output format style
            outputFormat = formatOpt.parse(commandLine);
            capture.setCaptureMessageStyle(captureType, outputFormat);
        }

        // If the output to directory option is supplied, check and save
        // directory name
        baseName = captureFileOpt.parse(commandLine);
        tempDataName = captureDataFileOpt.parse(commandLine);
        tempMetaName = captureMetadataFileOpt.parse(commandLine);

        if ((tempDataName != null || tempMetaName != null) && baseName != null) {
            throw new ParseException(
                    "You cannot specify --" + captureFileOpt.getLongOpt() + " when using the options --"
                            + captureDataFileOpt.getLongOpt() + " or -- " + captureMetadataFileOpt.getLongOpt() + ".");
        }

        final String dataFileName = (baseName == null) ? tempDataName : baseName + ".data";
        final String metaFileName = (baseName == null) ? tempMetaName : baseName + ".meta";

        if (dataFileName != null && !capture.setDataOutputFile(dataFileName)) {
            throw new ParseException("Invalid output file: " + dataFileName);
        }
        if (metaFileName != null && !capture.setMetadataOutputFile(metaFileName)) {
            throw new ParseException("Invalid output file: " + metaFileName);
        }

        captureMessages = captureMessagesOpt.parse(commandLine);
        capture.setCaptureMessages(captureMessages);
        

        syncMark = syncMarkOption.parse(commandLine);
        capture.setSyncMarker(syncMark);

        fwdHost = hostOption.parse(commandLine);
        fwdPort = portOption.parse(commandLine);

        if (fwdHost != null || fwdPort != null) {
            if (fwdHost == null || fwdPort == null) {
                throw new ParseException(
                        portOption.getLongOpt() + "and " + hostOption.getLongOpt() + " must both be supplied for message forwarding.");
            }

            capture.setForwardingStream(fwdHost, fwdPort.intValue());
        }

        this.queueSize = queueSizeOpt.parseWithDefault(commandLine, false, true);
        final int size = this.queueSize.intValue();
        if ((size & -size) != size) {
            throw new ParseException("The value of the --" + queueSizeOpt.getLongOpt() + " must be a power of 2");
        }

        scid = scidOption.parse(commandLine);
    }
    
    /**
     * Initializes the class for execution.
     * 
     * @return true if success, false if failure
     */
    public boolean init() {
        final String topicSuffix = "." + this.captureType.toLowerCase();
        try {
            for (String topic : appContext.getBean(IGeneralContextInformation.class).getSubscriptionTopics()) {
                if (!topic.endsWith(topicSuffix)) {
                    topic += topicSuffix;
                }
                final IQueuingMessageHandler messageHandler = appContext.getBean(IQueuingMessageHandler.class,
                        this.queueSize.intValue());
                final String filter = MessageFilterMaker.createSubscriptionFilter(scid, messageTypes, null,
                                                                                  null);
                log.info("Subscribing to topic " + topic + " with filter '" + filter + "'");
                messageHandler.setSubscription(topic, filter, false);
                messageHandler.addListener(capture);
                messageHandlers.add(messageHandler);
            }
        } catch (final Exception e) {
            log.error("Unexpected error in initialization: " + ExceptionTools.getMessage(e), e);
            this.exitStatus = FAILURE;
            return false;
        }
        return (!messageHandlers.isEmpty());
    }
    
    /**
     * Executes the main logic. Does not stop until the application is signaled.
     */
    public void run() {
        for (final IQueuingMessageHandler mh : messageHandlers) {
            try {
                mh.start();
            } catch (final MessageServiceException e) {
                log.error("Unexpected error starting up message handlers: " + ExceptionTools.getMessage(e), e);
                exitStatus = FAILURE;
                return;
            }
        }

        synchronized (stopping) {
            while (!stopping.get()) {
                try {
                    stopping.wait();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * Gets the exit status of the application.
     * 
     * @return exit code
     */
    public int getExitStatus() {
        return this.exitStatus;
    }
    
    @Override
    public void exitCleanly() {
        log.debug("Shutting down application");

        synchronized (stopping) {
            stopping.set(true);
            stopping.notifyAll();
        }

        for (final IQueuingMessageHandler mh : messageHandlers) {
            mh.shutdown(false, true);
        }

        if (this.socketOutput != null) {
            try {
                this.socketOutput.close();
            } catch (final IOException e) {
                // whatevs
            }
            this.socketOutput = null;
        }
        if (this.clientSocket != null) {
            try {
                this.clientSocket.close();
            } catch (final IOException e) {
                // whatevs
            }
            this.clientSocket = null;
        }
    }
    
    @Override
    public void showHelp() {        
        final PrintWriter pw = new PrintWriter(System.out);
        final String appName = ApplicationConfiguration.getApplicationName();
        
        pw.println("Usage: " + appName + " --topics <topic[,topic...]> [options]\n");
        createOptions().getOptions().printOptions(pw);
         
        pw.println("\nTopics to subscribe to must be supplied on the command line.");
        pw.println("These may be root topics or " + captureType.toLowerCase() + " topics. If these are root topics,");
        pw.println("then \"." + captureType.toLowerCase() + "\" will be appended to the topic for subscritpion.");
        
        if (this.useOutputFormatOption) {
            final StringBuilder types = new StringBuilder("Available message types are:\n   ");
            final List<String> availTypes = new LinkedList<>();
            try {
                availTypes.addAll(Arrays.asList(MissionConfiguredTemplateManagerFactory.getNewDatabaseTemplateManager(appContext.getBean(SseContextFlag.class))
                                                                                       .getStyleNames(captureType)));
            }
            catch (final TemplateException e) {
                log.warn("Unable to determine template types");
            }
            availTypes.add("csv");
            for (int i = 0; i < availTypes.size(); i++) {
                types.append(availTypes.get(i) + ' ');
                if (i != 0 && i % 5 == 0) {
                    types.append("\n   ");
                }
            }

            types.append("\n\nTemplate directories searched are:\n");
            try {
                for (final String dir : MissionConfiguredTemplateManagerFactory.getNewDatabaseTemplateManager(appContext.getBean(SseContextFlag.class))
                                                                               .getTemplateDirectories(captureType)) {
                    types.append("   " + dir + "\n");
                }
            }
            catch (final TemplateException e) {
                log.warn("Unable to determine template directories\n");
            }

            pw.println("\nMessage output format defaults to 'csv'.");
            pw.print(types.toString());
        }

        pw.flush();
        pw.close();
    }
    
    // package private getters to use for tests

    ApplicationContext getAppContext() {
        return appContext;
    }

    String getOutputFormat() {
        return outputFormat;
    }

    boolean isCaptureMessages() {
        return captureMessages;
    }

    String getFwdHost() {
        return fwdHost;
    }

    UnsignedInteger getFwdPort() {
        return fwdPort;
    }

    String getSyncMark() {
        return syncMark;
    }

    String getBaseName() {
        return baseName;
    }

    String getTempDataName() {
        return tempDataName;
    }

    String getTempMetaName() {
        return tempMetaName;
    }

}
