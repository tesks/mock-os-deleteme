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
package jpl.gds.tcapp.app;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.cli.options.PortOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.echo.ICommandEchoInput;
import jpl.gds.tc.api.echo.ICommandEchoInputFactory;
import jpl.gds.tc.api.echo.IEchoDecomService;

/**
 * The Command Echo App replaces using chill_down with the CMD_ECHO input format.
 * It connects to the data source provided (currently just the DSN emulator server), reads the CLTU commands provided, and writes
 * them to a log file until no more data is available or the app is disconnected.
 * 
 *
 *
 */
public class CommandEchoApp extends AbstractCommandLineApp {
    /** echoHost option */
    public static final String HOST_LONG = "echoHost";
    /** echoPort option */
    public static final String PORT_LONG = "echoPort";
    /** echoFile option */
    public static final String FILE_LONG = "echoFile";
    /** echoInput option */
    public static final String INPUT_LONG = "echoInput";
    /** logFile option */
    public static final String LOG_FIlE_LONG = "logFile";
    
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_command_echo");
    
    private static final StringOption HOST_OPTION   = new StringOption(null, HOST_LONG, "host", "the host system for command echo transmisison", false);
    private static final PortOption   PORT_OPTION   = new PortOption(null, PORT_LONG, "port", "I/O port used for command echo transmission", false);
    private static final EnumOption<EchoInputType>   INPUT_OPTION = new EnumOption<>(EchoInputType.class, null, INPUT_LONG, "inputType", "The connection protocol for command echo receipt from the source", false); 
    private static final FileOption   FILE_OPTION   = new FileOption(null, FILE_LONG, "file", "command echo input file", false, true);
    private static final FileOption   OUTPUT_OPTION = new FileOption(null, LOG_FIlE_LONG, "logFile", "Override the default log storeage file with a specified filepath", false, false);
    private DictionaryCommandOptions dictOpts;
    
   
    
    enum EchoInputType {
        SERVER_SOCKET,
        CLIENT_SOCKET,
        FILE;
    }
    
    private final Tracer trace;
    
    private final ApplicationContext appContext;
    
    private ICommandEchoInput source;
    private IEchoDecomService decom;
    
    private String host;
    private int    port;
    private File   inputFile;
    private String logFilename;
    private EchoInputType inputType;
    private String inputFilename;
    
    /**
     * Default constructor for CommandEchoApp. Creates the application context and starts decom
     */
    public CommandEchoApp(){
        super();
        appContext = SpringContextFactory.getSpringContext(true);
        trace = TraceManager.getTracer(appContext, Loggers.CMD_ECHO);
        
        setupOutputDir();
        
    }
    
    /**
     * Gets the inputSource from the factory and continually pumps data from the source until no more data is available or the app is terminated
     */
    public void processData(){
        
        try{
            appContext.getBean(FlightDictionaryLoadingStrategy.class).enableCommand().loadAllEnabled(appContext, false, false);
        } catch (BeansException | DictionaryException e){
            trace.error("FSW dictionary parsing error: " + e.getMessage());
            trace.error("Command echo will continue, but will be unable to identify commands");
            
        }
        
        decom = appContext.getBean(IEchoDecomService.class, logFilename);
        
        if(decom.startService()){
            trace.info("Decom started");
        } else {
            trace.error("Decom NOT started");
        }
        
        switch(this.inputType){
            case SERVER_SOCKET:
                source = appContext.getBean(ICommandEchoInputFactory.class).getServerSocketInput(appContext, port);
                break;
            case CLIENT_SOCKET:
                source = appContext.getBean(ICommandEchoInputFactory.class).getClientSocketInput(appContext, host, port);
                break;
            case FILE:
                source = appContext.getBean(ICommandEchoInputFactory.class).getFileInput(appContext, inputFile);
                break;
            default:
                throw new IllegalArgumentException("Invalid input source specified");
        }
        while(!source.isStopping()){
            if(!source.isConnected()){
                source.connect();
            }
            source.ingestData();
        }
        
    }
    
    private void setupOutputDir(){
        final String path = appContext.getBean(IGeneralContextInformation.class).getOutputDir();
        final File outputDir = new File(path);
        if(!outputDir.exists()){
            outputDir.mkdirs();
        }
    }
    
    /**
     * Used By QuitSignalHandler to properly shut things down
     */
    @Override
    public void exitCleanly(){
        if(source != null){
            source.stopSource();
        }
        if(decom != null){
            decom.stopService();
        }
    }
    
    /**
     * Parse the command line options and configure the Command Echo App
     * 
     * @param commandLine the parsed command line
     * @throws ParseException an error was encountered while configuring the applicaiton with the supplied command line
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException{
        super.configure(commandLine);
        
        inputType = CommandEchoApp.INPUT_OPTION.parse(commandLine);
        
        logFilename = CommandEchoApp.OUTPUT_OPTION.parse(commandLine);
        
        if(inputType == null){
            trace.info(CommandEchoApp.INPUT_OPTION.getLongOpt() + " was not specified, starting as " + EchoInputType.SERVER_SOCKET);
            inputType = EchoInputType.SERVER_SOCKET;
        }
        
        switch(this.inputType){
            case SERVER_SOCKET:
                configurePort(commandLine);
                break;
            case CLIENT_SOCKET:
                configureHost(commandLine);
                configurePort(commandLine);
                break;
            case FILE:
                configureFile(commandLine);
                break;
            default:
                //shouldn't get here...
                throw new ParseException(INPUT_OPTION.getLongOpt() + " was specified with an unsupported option. Valid options: " + EchoInputType.values());
        }
        
        dictOpts.parseAllOptionsAsOptionalWithDefaults(commandLine);
        
    }
    
    private void configureHost(final ICommandLine commandLine) throws ParseException{
        host = CommandEchoApp.HOST_OPTION.parse(commandLine);
        if(host == null){
            host = appContext.getBean(CommandProperties.class).getDefaultEchoHost();
            trace.info(CommandEchoApp.HOST_OPTION.getLongOpt() + " was not specified, using default host: " + this.host);
        }
    }
    
    private void configurePort(final ICommandLine commandLine) throws ParseException{
        final UnsignedInteger portInt = CommandEchoApp.PORT_OPTION.parse(commandLine);
        
        if(portInt == null){
            port = appContext.getBean(CommandProperties.class).getDefaultEchoPort();
            trace.info(CommandEchoApp.PORT_OPTION.getLongOpt() + " was not specified, using default port: " + this.port);
        } else{
            port = portInt.intValue();
        }
    }
    
    private void configureFile(final ICommandLine commandLine) throws ParseException{
        inputFilename = CommandEchoApp.FILE_OPTION.parse(commandLine);
        
        if(inputFilename == null){
            throw new ParseException(FILE_OPTION.getLongOpt() + " is a required option");
        }
        inputFile = new File(inputFilename);
        if(!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()){
            throw new ParseException(inputFilename + " is not a valid or readable file");
        }
    }
    
    @Override
    public BaseCommandOptions createOptions(){
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));
        dictOpts = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));
        
        options.addOption(CommandEchoApp.HOST_OPTION);
        options.addOption(CommandEchoApp.PORT_OPTION);
        options.addOption(CommandEchoApp.INPUT_OPTION);
        options.addOption(CommandEchoApp.FILE_OPTION);
        options.addOption(CommandEchoApp.OUTPUT_OPTION);
        
        options.addOption(dictOpts.FSW_DICTIONARY_DIRECTORY);
        options.addOption(dictOpts.FSW_VERSION);
        
        
        return options;
    }
    
    @Override
    public void showHelp(){
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        if(options == null){
            createOptions();
        }
        
        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + APP_NAME + "\n\n");
        options.getOptions().printOptions(pw);
        pw.flush();
    }
    
    /**
     * Main function. Orchestrates all functionality of the application
     * @param args
     */
    public static void main(final String[] args){
        
        final CommandEchoApp app = new CommandEchoApp();
        
        try {
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);
            app.processData();
        } catch (final ParseException e) {
            TraceManager.getTracer(Loggers.CMD_ECHO).error("Command line parsing error: " + e.getMessage());
            System.exit(1);
        }
        
        System.exit(0);
        
    }
    
    // package private getters to use for tests

    ApplicationContext getAppContext() {
        return appContext;
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }

    String getLogFilename() {
        return logFilename;
    }

    String getInputFilename() {
        return inputFilename;
    }

}
