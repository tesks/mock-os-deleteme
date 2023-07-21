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
package jpl.gds.product.app.decom.receng;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.velocity.Template;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import jpl.gds.cli.legacy.app.AbstractCommandLineApp;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.product.api.AbortProductAppException;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.product.api.config.RecordedProductProperties;
import jpl.gds.product.api.decom.receng.IRecordedEngProductDecom;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;

/**
 * App for recorded engineering decom
 */
public class RecordedEngProductDecomApp extends AbstractCommandLineApp implements IQuitSignalHandler {

	public static final String		OPTION_SHORT_PRODUCT_DIRECTORY	= "d";
	public static final String		OPTION_LONG_PRODUCT_DIRECTORY	= "directory";
	public static final String		OPTION_SHORT_NOTEXT				= "t";
	public static final String		OPTION_LONG_NOTEXT				= "noText";
	public static final String		OPTION_SHORT_USEDATABASE		= "X";
	public static final String		OPTION_LONG_USEDATABASE			= "useDb";
	public static final String		OPTION_SHORT_USEJMS				= "J";
	public static final String		OPTION_LONG_USEJMS				= "useJms";
	public static final String		OPTION_LONG_DEBUG				= "debug";
    public static final String      OPTION_SHORT_OUTPUT_FORMAT      = "o";
    public static final String      OPTION_LONG_OUTPUT_FORMAT       = "outputFormat";
    // Added noGlobalLad option to disable global LAD
    public static final String      OPTION_LONG_NO_GLOBAL_LAD         = "noGlobalLad";

    private final Tracer                log;

	/*
	 * Local option values
	 */
	private String[]			inputFileNames					= null;
	private String				dictDirOverride					= null;
	private String				dictVersionOverride				= null;
	private boolean				noText							= false;
	private boolean				useDatabase						= false;
	private boolean				useJms							= false;
	private boolean             stopped                         = false;
	private boolean             debug							= false;
	private long                sessionKey                      = -1;
	private String              sessionHost                     = null;
	private String              templateName                    = "csv";
	private boolean             noGlobalLad                     = false;

	private IRecordedEngProductDecom decom = null;
    private final List<Integer> productTypeApidList = new ArrayList<Integer>();
    private MessageTemplateManager templateMgr;

    private final ApplicationContext springContext;

    /**
	 * Creates an instance of RecordedEngProductDecomApp.
	 */
	public RecordedEngProductDecomApp() {
		super();

		springContext = SpringContextFactory.getSpringContext(true);
		new SessionConfiguration(springContext);
		ReservedOptions.setApplicationContext(springContext);
        log = TraceManager.getTracer(springContext, Loggers.PRODUCT_DECOM);

        // Get properties from the context
		final RecordedProductProperties rpc = springContext.getBean(RecordedProductProperties.class);

		final String[] ehaTypes = rpc.getEhaProductApids();
		final String[] evrTypes = rpc.getEvrProductApids();

		if (ehaTypes != null) {
			for (final String exp: ehaTypes) {
   			    this.productTypeApidList.add(Integer.valueOf(exp));
		    }
		}

		if (evrTypes != null) {
			for (final String exp: evrTypes) {
				this.productTypeApidList.add(Integer.valueOf(exp));
			}
		}

	    try {
            this.templateMgr = MissionConfiguredTemplateManagerFactory.getNewMessageTemplateManager(springContext.getBean(SseContextFlag.class));
		} catch (final TemplateException e1) {
			e1.printStackTrace();
		}

	}

	/**
     * Display application arguments and options.
     * @param options the Options object defining possible arguments/options
     */
	@Override
    public void showHelp()
    {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

    	final Options options = createOptions();
    	
        final HelpFormatter formatter = new HelpFormatter();
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [options] <product_input_file_list>");
        pw.println("                   ");
        
        formatter.printOptions(pw, 80, options, 7, 10);
        
        pw.println("       The <product_input_file_list> may be one or more space-delimited,");
        pw.println("       fully-qualified file names. The file names may specify either");
        pw.println("       .dat or .emd files. File name wildcards are supported. If --testKey is supplied");
        pw.println("       <product_input_file_list> should be omitted, and every recorded engineering");
        pw.println("       data product in the specified session will be processed.");
        pw.println("       ");
        pw.println("       The default behavior is to simply output extracted records to the console.");
        pw.println("       Addition of the --useDb flag will cause extracted records to be written to the");
        pw.println("       database under the same session as the original data product. Addition of");
        pw.println("       --useJms flag will cause extracted records to be published to the message");
        pw.println("       service on the same topic as the original session, allowing them to be");
        pw.println("       received by monitoring applications.");
        pw.println();
         
        pw.flush(); 
        printTemplateStyles();
        
    }

    @Override
    public void exitCleanly() {
        shutdown();
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#configure(org.apache.commons. cli.CommandLine)
	 */
	@Override
	public void configure(final CommandLine commandLine) throws ParseException {
		super.configure(commandLine);

		ReservedOptions.parseDatabaseHost(commandLine, false);
		ReservedOptions.parseDatabasePort(commandLine, false);
		ReservedOptions.parseDatabaseUsername(commandLine, false);
		ReservedOptions.parseDatabasePassword(commandLine, false); 
        ReservedOptions.parseJmsHost(commandLine, false);
        ReservedOptions.parseJmsPort(commandLine, false);
        
        if (commandLine.hasOption(ReservedOptions.TESTKEY_LONG_VALUE)) {
        	try {
        		this.sessionKey = Integer.valueOf(commandLine.getOptionValue(ReservedOptions.TESTKEY_LONG_VALUE));
        	} catch (final NumberFormatException e) {
        		throw new ParseException("Session key " + commandLine.getOptionValue(ReservedOptions.TESTKEY_LONG_VALUE) + " is invalid");
        	}
        }
        
        if (commandLine.hasOption(ReservedOptions.TESTHOST_LONG_VALUE)) {
        	if (this.sessionKey == -1) {
        		throw new ParseException("It makes no sense to supply session host without session key");
        	}
        	this.sessionHost = commandLine.getOptionValue(ReservedOptions.TESTHOST_LONG_VALUE);
        }
        
		if (commandLine.hasOption(ReservedOptions.FSW_DICTIONARY_DIRECTORY.getOpt())) {
			dictDirOverride = commandLine.getOptionValue(ReservedOptions.FSW_DICTIONARY_DIRECTORY.getOpt());
		}

		if (commandLine.hasOption(ReservedOptions.FSW_VERSION.getOpt())) {
			dictVersionOverride = commandLine.getOptionValue(ReservedOptions.FSW_VERSION.getOpt());
		}
		
		if (commandLine.hasOption(OPTION_LONG_OUTPUT_FORMAT)) {
			this.templateName = commandLine.getOptionValue(OPTION_LONG_OUTPUT_FORMAT);
		}
		
		// Added noGlobalLad option to disable global LAD
        this.noGlobalLad = commandLine.hasOption(OPTION_LONG_NO_GLOBAL_LAD);
		
		/*
		 * No extra input on command line indicates no input files specified
		 */
		inputFileNames = commandLine.getArgs();
		if (inputFileNames.length == 0 && this.sessionKey == -1) {
			throw new ParseException("Error: No product input files specified on command line");
		} else if (inputFileNames.length != 0 && this.sessionKey != -1) {
			throw new ParseException("You may not specify both a session key and product file names to process");
		}
		
		if (sessionKey != -1) {
			inputFileNames = RecordedEngProductDecomUtility.getSessionProductFiles(springContext, sessionKey, sessionHost, this.productTypeApidList);
			if (inputFileNames == null) {
				throw new ParseException("No products to process");
			}
		}

		noText = commandLine.hasOption(OPTION_LONG_NOTEXT);
		useDatabase = commandLine.hasOption(OPTION_LONG_USEDATABASE);

		useJms = commandLine.hasOption(OPTION_LONG_USEJMS);

		springContext.getBean(MessageServiceConfiguration.class).setUseMessaging(useJms);
	
		debug = commandLine.hasOption(OPTION_LONG_DEBUG);
		if (debug) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.DEBUG);
        }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#createOptions()
	 */
	@Override
	public Options createOptions() {
		final Options options = super.createOptions();

		options.addOption(ReservedOptions.getOption(ReservedOptions.HELP_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.VERSION_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.FSW_DICTIONARYDIR_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.FSWVERSION_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.TESTKEY_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.TESTHOST_SHORT_VALUE));
		options.addOption(ReservedOptions.DATABASE_HOST);
		options.addOption(ReservedOptions.DATABASE_PORT);
		options.addOption(ReservedOptions.DATABASE_USERNAME);
		options.addOption(ReservedOptions.DATABASE_PASSWORD);
		options.addOption(ReservedOptions.JMS_HOST);
		options.addOption(ReservedOptions.JMS_PORT); 

	    final Option noTextOption = ReservedOptions.createOption(OPTION_SHORT_NOTEXT, OPTION_LONG_NOTEXT, null,
				"Suppress text output to console");
		options.addOption(noTextOption);

		final Option useDatabaseOption = ReservedOptions.createOption(OPTION_SHORT_USEDATABASE, OPTION_LONG_USEDATABASE, null,
				"Write extracted telemetry to the database under the same session as the original data product");
		options.addOption(useDatabaseOption);
		
		final Option useJmsOption = ReservedOptions.createOption(OPTION_SHORT_USEJMS, OPTION_LONG_USEJMS, null,
				"Publish extracted telemetry to the message service");
		options.addOption(useJmsOption);
		
		final Option debugOption = ReservedOptions.createOption(null, OPTION_LONG_DEBUG, null, "Enable debug output");
        options.addOption(debugOption);
        
        final Option outputOption = ReservedOptions.createOption(OPTION_SHORT_OUTPUT_FORMAT, OPTION_LONG_OUTPUT_FORMAT, "format", 
        		"The formatting style for the console output with a Velocity template.");
        options.addOption(outputOption);
        
        // Added noGlobalLad option to disable global LAD
        final Option noGlobalLadOption = ReservedOptions.createOption(null, OPTION_LONG_NO_GLOBAL_LAD, "Disable global LAD", 
                "Data extracted by this application will not be put into the global LAD");
        options.addOption(noGlobalLadOption);
        
		return options;
	}

	/*
	 * Initializes the application.
	 * 
	 * @return true if successfully initialized, false if not
	 */
	public boolean init() {
		boolean successful = false;
		
		try {
			decom = springContext.getBean(IRecordedEngProductDecom.class, 
			        useDatabase, !noGlobalLad, useJms, dictDirOverride, dictVersionOverride);

			successful = true;
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		return successful;
	}

	/**
	 * Process all the data products on the input list to extract channelized engineering.
	 */
	public void start() {
		
		final HashMap<String,Boolean> filesProcessed = new HashMap<String,Boolean>();
		
		for (String fileName : inputFileNames) {
			
			// don't process partial products (fileName ends with ".pemd" or ".pdat"
			if ( fileName.endsWith(ProductStorageConstants.PARTIAL_METADATA_SUFFIX)
			  || fileName.endsWith(ProductStorageConstants.PARTIAL_DATA_SUFFIX)) {
				log.warn("Skipping: \"" + fileName + "\" (we do not process partial products)");
				continue;
			}
			
			// if fileName has no extension, append metadata suffix ".emd"
			if ( -1 == fileName.lastIndexOf('.') ) {
				fileName = fileName + ProductStorageConstants.METADATA_SUFFIX;
			}
			
			// do not process any file not ending with .emd or .dat
			if ( !fileName.endsWith(ProductStorageConstants.METADATA_SUFFIX)
			  && !fileName.endsWith(ProductStorageConstants.DATA_SUFFIX)) {
				log.warn("Skipping: \"" + fileName + "\" (not a metadata or data file for a complete product)");
				continue;
			}
			
			// do not process the same file twice, especially the common case of
			// seeing first the .dat, and then the .emd, as what happens when someone
			// uses wildcards to process lots of files in a directory. So we compare
			// against the filename ignoring the extension
			final int dot = fileName.lastIndexOf('.');
			final String fileNameWithoutExtension = fileName.substring(0,dot);
			if ( null != filesProcessed.get(fileNameWithoutExtension))
			{
				log.warn("Skipping: \"" + fileName + "\" (already processed)");
				continue;
			}
			filesProcessed.put(fileNameWithoutExtension, true);
			
			// Ensure the file exists
			final File f = new File(fileName);
			if (!f.exists()) {
				log.warn("Skipping: \"" + fileName + "\" (file not found)");
				continue;
			}

			MessageSubscriber subscriber = null;
			try {
				if (!noText) {
					subscriber = new MessageSubscriber() {
						@Override
						public void handleMessage(final IMessage message) {
							try {
								if (templateMgr != null) {
									final Template t = templateMgr.getTemplateForStyle(MessageRegistry.getMessageConfig(message.getType()),templateName);
									final HashMap<String, Object> map = new HashMap<String, Object>();
									message.setTemplateContext(map);
									map.put("body", "true");									
                                    map.put("formatter", new SprintfFormat(
                                                                           springContext.getBean(IContextIdentification.class)
                                                                                        .getSpacecraftId()));
									final String text = MessageTemplateManager.createText(t, map);
									System.out.println(text);
								} else {
									System.out.println(message.toXml());
								}
							} catch (final TemplateException e) {
								log.warn("Unable to format " + message.getType() + " messge using style " + templateName);
							}
						}
					};
					decom.subscribe(subscriber);
				}
				decom.execute(fileName);
			}
			catch (final ProductException e) {
				log.warn("Did not process data product: " + e.getMessage());
			}
			catch (final Exception e) {
				e.printStackTrace();
				log.error("Cannot process " + fileName + ": " + e.toString());
			}
			finally {
				if ((null != decom) && (null != subscriber)) {
					decom.unSubscribe(subscriber);
				}
			}
		}
	}

	/**
	 * Aborts the application.
	 * 
	 * @param message
	 *            the error message
	 * @param rootCause
	 *            the exception that caused the abort
	 * @throws AbortProductAppException
	 */
	@SuppressWarnings("unused")
	private void abortApp(final String message, final Throwable rootCause) throws AbortProductAppException {
		log.error(message, rootCause);
		throw new AbortProductAppException(message, rootCause);
	}

	/**
	 * Cleans up everything for application exit.
	 */
	private synchronized void shutdown() {
		if (decom != null && !this.stopped) {
			decom.shutdown();
		}
		this.stopped = true;
	}
	
	 /**
     * Displays valid formatting styles for the current database table name.
     */
    private void printTemplateStyles()
    {
        String[] styles = getTemplateStyles(EvrMessageType.Evr);
        if (styles.length == 0)
        {
            // OK to system.out this rather than trace; it's part of the help text
            System.out.println("\nA list of formatting styles in not currently available.");
            return;
        }

        System.out.print("\nAvailable EVR formatting styles are:");
        for (int i = 0; i < styles.length; i++)
        {
            if (i % 4 == 0)
            {
                System.out.println();
                System.out.print("   ");
            }
            System.out.print(styles[i] + " ");
        }
        System.out.println();
        
        printTemplateDirectories(EvrMessageType.Evr);
        
        styles = getTemplateStyles(EhaMessageType.AlarmedEhaChannel);
        if (styles.length == 0)
        {
            // OK to system.out this rather than trace; it's part of the help text
            System.out.println("\nA list of formatting styles in not currently available.");
            return;
        }

        System.out.print("\nAvailable channel formatting styles are:");
        for (int i = 0; i < styles.length; i++)
        {
            if (i % 4 == 0)
            {
                System.out.println();
                System.out.print("   ");
            }
            System.out.print(styles[i] + " ");
        }
        System.out.println();
        
        printTemplateDirectories(EhaMessageType.AlarmedEhaChannel);
    }
    
    /**
     * Returns an array of available template/style names (or empty if there are none)
     *
     * @return Style array
     */
    private String[] getTemplateStyles(final IMessageType messageType)
    {
    	if (templateMgr == null) {
    	    log.warn("Unable to determine available output formats\n");
    	}
        try
        {
            if (messageType == null)
            {
                throw new TemplateException("Message type not supplied for getting template styles");
            }

            return(templateMgr.getStyleNames(MessageRegistry.getMessageConfig(messageType))).toArray(new String[] {});
        }
        catch (final TemplateException e)
        {
        	e.printStackTrace();
            log.warn("Unable to determine available output formats\n");
        }

        return(new String[0]);
    }

    /**
     * Print out all searched template directories.
     */    
    public void printTemplateDirectories(final IMessageType type) {
    	
    	if (templateMgr == null) {
    	    log.warn("Unable to determine template search path\n");
    	}
         try
         {
             if (type == null)
             {
                 throw new TemplateException("message type is not set in application");
             }

             final List<String> directories = templateMgr.getTemplateDirectories(MessageRegistry.getMessageConfig(type));
             
             System.out.println("\nTemplate directories searched are:");
             for (final String d: directories) {
            	 System.out.println("   " + d);
             }
         }
         catch (final TemplateException e)
         {
             log.warn("Unable to determine template directories\n");
         }
    }

	/**
	 * Main application entry point.
	 * @param args Command line arguments from the user
	 */
	public static void main(final String[] args) {
		final RecordedEngProductDecomApp app = new RecordedEngProductDecomApp();
		try {
			final CommandLine commandLine = ReservedOptions.parseCommandLine(args, app);
			app.configure(commandLine);
		}
		catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage(), e.getCause());
			System.exit(1);
		}

		final boolean ok = app.init();
		if (!ok) {
			System.exit(1);
		}
		app.start();
		app.shutdown();
	}

}
