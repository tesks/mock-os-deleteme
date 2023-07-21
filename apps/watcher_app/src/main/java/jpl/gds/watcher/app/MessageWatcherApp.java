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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.context.api.options.SubscriberTopicsOption;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.common.filtering.VcidFilter;
import jpl.gds.common.options.DssIdListOption;
import jpl.gds.common.options.SpacecraftIdOption;
import jpl.gds.common.options.VcidListOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.message.api.MessageUtility;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.MessageHeaderMode;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.util.MessageCaptureHandler;
import jpl.gds.message.api.util.MessageCaptureHandler.CaptureType;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.MessageTypesOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * This is the main class for a message watcher application that does not use the
 * old message responder infrastructure.  It is intended to replace the command-line
 * message capture capability in chill_monitor.
 */
public class MessageWatcherApp extends AbstractCommandLineApp implements IQuitSignalHandler {
    
    private static final int DEFAULT_QUEUE_SIZE = 65536;
    private static final int SUCCESS = 0;
    private static final int FAILURE = 1;
    
    //directory option
    public static final String DIRECTORY = "directory";
    
    //filename option
    public static final String FILENAME = "filename";
    
    private String fileName;
    private String dirName;
    
    private MessageServiceCommandOptions jmsOpts;
    private ContextCommandOptions contextOpts;
    private final OutputFormatOption formatOpt = new OutputFormatOption(false);
    private final DirectoryOption captureDirOpt = new DirectoryOption("l", DIRECTORY,
            DIRECTORY, "Writes messages to separate files in the given directory as they "
                    + "are received. Only one of --" + DIRECTORY + " or --" + FILENAME + " can be specified.",
            false, true);
    private final FileOption captureFileOpt = new FileOption("f", FILENAME, FILENAME,
            "Writes all messages to a single file as they are received.", false, false);
    private final MessageTypesOption msgTypeOpt = new MessageTypesOption(true);
    private final FlagOption propertiesOpt = new FlagOption("r", "properties", "enables message header property capture", false);
    private final UnsignedIntOption queueSizeOpt = new UnsignedIntOption(null, "queueSize", 
            "size", "Length of the internal message queue as number of messages. MUST BE A POWER OF 2. Defaults to " + DEFAULT_QUEUE_SIZE, false);
    private DssIdListOption dssIdsOption;
    private VcidListOption vcidsOption;
    private SpacecraftIdOption scidOption;
    
    private final ApplicationContext appContext;
    private final List<IQueuingMessageHandler> messageHandlers = new LinkedList<>();
    private UnsignedInteger queueSize;
    private final Tracer log;
    private int exitStatus = SUCCESS;
    
    private MessageCaptureHandler capture;
    private Collection<IMessageType> messageTypes;
    private DssIdFilter dssFilter;
    private VcidFilter vcFilter;
    private UnsignedInteger scid;
    
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    /**
     * Constructor.
     */
    public MessageWatcherApp() {
        appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getTracer(appContext, Loggers.WATCHER);
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
        contextOpts.SUBSCRIBER_TOPICS.setRequired(true);
        options.addOption(contextOpts.SUBSCRIBER_TOPICS);
        options.addOption(contextOpts.EXPAND_TOPICS);

        dssIdsOption = new DssIdListOption(false, true, appContext.getBean(MissionProperties.class));
        options.addOption(dssIdsOption);
        
        vcidsOption = new VcidListOption(false, true, appContext.getBean(MissionProperties.class));
        options.addOption(vcidsOption);
        
        scidOption = new SpacecraftIdOption(appContext.getBean(MissionProperties.class), false);
        options.addOption(scidOption);
        
        options.addOption(msgTypeOpt);
        options.addOption(formatOpt);
        options.addOption(captureDirOpt);
        options.addOption(captureFileOpt);
        options.addOption(propertiesOpt);
        options.addOption(queueSizeOpt);
        queueSizeOpt.setDefaultValue(UnsignedInteger.valueOf(DEFAULT_QUEUE_SIZE));

        return options;
    }
    
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);
        
        capture = appContext.getBean(MessageCaptureHandler.class);
        
        contextOpts.SUBSCRIBER_TOPICS.parse(commandLine);        
        
        dssFilter = dssIdsOption.parse(commandLine);
        
        vcFilter = vcidsOption.parse(commandLine);
        
        jmsOpts.parseAllOptionsAsOptional(commandLine);

        // If the output to directory option is supplied, check and save directory name
        dirName = captureDirOpt.parse(commandLine);
        if (dirName != null && !capture.setOutputDir(dirName)) {
            throw new ParseException("Invalid output directory: " + dirName);
        }

        // If the output to file option is supplied, check and save filename
        fileName = captureFileOpt.parse(commandLine);
        if (fileName != null && !capture.getWriteMode().equals(CaptureType.WRITE_NONE)) {
            throw new ParseException("You cannot specify both --" + captureFileOpt.getLongOpt() + "and --"
                    + captureDirOpt.getLongOpt() + " options.");
        }
        if (fileName != null && !capture.setOutputFile(fileName)) {
            throw new ParseException("Invalid output file: " + fileName);
        }

        // Parse the option that filters for given message types
        messageTypes = msgTypeOpt.parse(commandLine);
        if (!messageTypes.isEmpty()) {
            capture.setCaptureMessageFilter(commandLine.getOptionValue(msgTypeOpt.getLongOpt()));
        }
        
        // Parse the option enabling message header property display
        if (propertiesOpt.parse(commandLine)) {
            capture.setHeaderMode(MessageHeaderMode.HEADERS_ON);
        }

        // Parse option for default message output format style
        final String outputFmt = formatOpt.parse(commandLine);
        if (outputFmt != null) {
            capture.setCaptureMessageStyle(outputFmt);
        }
        
        this.queueSize = queueSizeOpt.parseWithDefault(commandLine, false, true);       
        if (this.queueSize != null) {
            final int size = this.queueSize.intValue();
            if ((size & -size) != size) {
                throw new ParseException("The value of the --" + queueSizeOpt.getLongOpt() +
                        " must be a power of 2");
            }
        }
        
        scid = scidOption.parse(commandLine);
        
    }
    
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        
        final List<IMessageConfiguration> availTypes = MessageRegistry.getAllMessageConfigs(true);
        final StringBuilder types = new StringBuilder("Available message types are:\n   ");
        for (int i = 0; i < availTypes.size(); i++) {
            types.append(availTypes.get(i).getSubscriptionTag() + ' ');
            if (i != 0 && i % 5 == 0) {
                types.append("\n   ");
            }
        }
        
        final List<String> availStyles = MessageUtility.getMessageStyles(availTypes.toArray(new IMessageConfiguration[] {}));
        final StringBuilder styles = new StringBuilder(1024);
        styles.append("Known available message styles are:\n   ");
        for (final String s: availStyles) {
            styles.append(s + " ");
        }
        styles.append("\n\nNote that not all styles apply to all message types.");
        styles.append('\n');
        
        final String appName = ApplicationConfiguration.getApplicationName();
        
        pw.println("Usage: " + appName + " --topics <topic[,topic...]> --types [type,[type...] [options]\n");
        createOptions().getOptions().printOptions(pw);
         
        pw.println("\nTopics to subscribe to must be supplied on the command line.");
        pw.println("These may be root topics or data topics. If these are root topics,");
        pw.println("then only the root topics will be subscribed to unless --" + contextOpts.EXPAND_TOPICS.getLongOpt());
        pw.println("is supplied, in which case all data subtopics of the root topic will be subscribed");
        pw.println("to, in addition to the root topic. For most efficient operation, provide data topics");
        pw.println("that correspond only to the types of messages being watched. This will minimize");
        pw.println("traffic to this client.");
        
        pw.println("\nMessage types to watch for must be supplied on the commmand line.");
        pw.println(types);
        
        pw.println("\nMessage output format defaults to 'onelinesummary'.");
        pw.print(styles.toString());
        pw.print(MessageUtility.getTemplateDirectories());
        
        pw.flush();
        
    }
    
    /**
     * Initializes the class for execution.
     * 
     * @return true if success, false if failure
     */
    public boolean init() {
        try {
            for (final String topic: appContext.getBean(IGeneralContextInformation.class).getSubscriptionTopics()) {
                final IQueuingMessageHandler messageHandler = appContext.getBean(IQueuingMessageHandler.class, this.queueSize.intValue()); 
                final String filter = MessageFilterMaker.createSubscriptionFilter(scid, messageTypes, dssFilter, vcFilter);
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
        return true;
    }

    /**
     * Executes the main logic. Does not stop until the application is signaled.
     */
    public void run() {
        for ( final IQueuingMessageHandler mh: messageHandlers) {
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
    
    /**
     * Gets the application context
     * 
     * @return Spring ApplicationContext
     */
    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    /**
     * Getter for queue size - internal message queue as number of messages
     * Must be a power of 2. Defaults to 65535
     * 
     * @return Queue size as unsigned integer
     */
    public UnsignedInteger getQueueSize() {
        return queueSize;
    }
    
    /**
     * Getter for capture directory name
     * Can be null if not capturing
     * 
     * @return Capture directory as string
     */
    public String getDirName() {
        return dirName;
    }

    /**
     * Getter for the capture file name
     * Can be null if not capturing
     * 
     * @return Capture file name as string
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Main application entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        // Interpret all times in the app as GMT. This is very critical.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        final MessageWatcherApp app = new MessageWatcherApp();
        try {
            final ICommandLine cmdline = app.createOptions().parseCommandLine(args, true);
            app.configure(cmdline);
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
            System.exit(FAILURE);
        }
        
        if (app.init()) {
            app.run();
        }
        
        System.exit(app.getExitStatus());
    }
    
    @Override
    public void exitCleanly() {
        log.debug("Shutting down application");
        
        synchronized (stopping) {
            stopping.set(true);
            stopping.notifyAll();
        }
        
        for (final IQueuingMessageHandler mh: messageHandlers) {
            mh.shutdown(false, true);
        }
    }
}
