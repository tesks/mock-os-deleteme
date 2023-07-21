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
package jpl.gds.product.automation.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jpl.gds.product.automation.hibernate.IAutomationLogger;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.product.api.AbortProductAppException;
import jpl.gds.product.automation.app.options.PostDownlinkProductProcessorAppOptions;
import jpl.gds.product.processors.IPostDownlinkProductProcessor;
import jpl.gds.product.processors.PostDownlinkProductProcessingException;
import jpl.gds.product.processors.PostDownlinkProductProcessorOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.app.ICommandLineApp;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 * MPCS-8379 - 10/03/2016 - Moved all of the options to PostDownlinkProductProcessorAppOptions.java
 */
public abstract class AbstractPostDownlinkProductProcessorApp extends AbstractCommandLineApp implements ICommandLineApp {
	
	// MPCS-8379 10/03/16 - Moved option strings to PostDownlinkProductProcessorAppOptions.java
	
	/** publish output to the default tracer */
	protected final IAutomationLogger 				log;


	/**
	 * The processor to use.
	 */
	private IPostDownlinkProductProcessor			productProcessor				= null;

	/**
	 * General purpose Options container for all Post Downlink Product Processing Applications
	 */
	protected PostDownlinkProductProcessorOptions	processingOptions				= null;

	
	// MPCS-8379 10/03/16 - Moved local options to PostDownlinkProductProcessorAppOptions.java
	
	/**
	 * Local option values
	 */
	protected Collection<String>					inputFileNames;
	
	/**
	 * Debug flag
	 */
	protected boolean								debug							= false;
	
	/**
	 * Shutdown flag
	 */
	protected boolean								shuttingDown					= false;
	
	/**
	 * Force reprocess flag.  
	 */
	protected boolean								forceReprocess					= false;

	protected ApplicationContext appContext;
	
//	/**
//	 * @return the current IApidDictionary object
//	 * @throws DictionaryException
//	 */
//	protected static IApidDictionary getApidDictionary() throws DictionaryException {
//		//changed from return new GenericApidDictionary. currently nothing is calling this method. need to find out what happens due to this.
//		return ApidDictionaryFactory.getStaticInstance();
//	}

	/**
     * @param appContext
     *            The current application context
     */
	public AbstractPostDownlinkProductProcessorApp(final ApplicationContext appContext) {
		super();
		
		this.appContext = appContext;
        log = appContext.getBean(IAutomationLogger.class);
		try {
			shuttingDown = false;
		}
		catch (final IllegalStateException e) {
		}
	}

	/**
	 * Display application arguments and options.
	 * 
	 * @param options
	 *            the Options object defining possible arguments/options
	 */
	@Override
	public void showHelp() {
		final OptionSet options = createOptions().getOptions();

		final PrintWriter pw = new PrintWriter(System.out);
		pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [options] <product_input_file_list>");
		pw.println("       where <product_input_file_list> may be one or more space-delimited,");
		pw.println("       fully-qualified file names. The file names may specify either");
		pw.println("       .dat or .emd files. File name wildcards are supported.");
		pw.println("                   ");
		options.printOptions(pw);
		pw.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#createOptions()
	 */
	@Override
	public BaseCommandOptions createOptions() {
		
		final PostDownlinkProductProcessorAppOptions options = new PostDownlinkProductProcessorAppOptions(this, appContext);
        final DatabaseCommandOptions dbOptions = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
		final MessageServiceCommandOptions jmsOptions = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
		final DictionaryCommandOptions dictOptions = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));

		//removed help and version, redundant. super.createOptions adds them.
		options.addOption(dictOptions.FSW_DICTIONARY_DIRECTORY);
		options.addOption(dbOptions.DATABASE_HOST);
		options.addOption(dbOptions.DATABASE_PORT);
		options.addOption(dbOptions.DATABASE_USERNAME);
		options.addOption(dbOptions.DATABASE_PASSWORD);
		options.addOption(jmsOptions.NO_JMS);
		options.addOption(dbOptions.NO_DATABASE);
		options.addOption(BaseCommandOptions.DEBUG);
		
		options.addOption(PostDownlinkProductProcessorAppOptions.DISPLAY_TO_CONSOLE_OPTION);
		
		options.addOption(PostDownlinkProductProcessorAppOptions.INPUT_FILE_LIST_OPTION);
		
		/*
		 * 4/16/2012 - MPCS-3611 - adding options for fsw build id and fsw version string.
		 */
		options.addOption(dictOptions.FSW_VERSION);
		options.addOption(PostDownlinkProductProcessorAppOptions.FSW_BUILD_ID_OPTION);
		
		/*
		 * 5/30/2012 - MPCS-3768
		 */
		options.addOption(PostDownlinkProductProcessorAppOptions.OVERRIDE_OPTION);
		
		// MPCS-4387
		options.addOption(PostDownlinkProductProcessorAppOptions.FORCE_REPROCESS_OPTION);

		return options;
	}

	/**
	 * @param commandLine
	 * @return the PostDownlinkProductProcessorOptions object used to configure PDPP constructed from the command line options
	 * @throws ParseException
	 * 
	 * MPCS-8379  - 10/04/16 - updated all to the new options paradigm
	 */
	public PostDownlinkProductProcessorOptions getConfigurationOptions(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);
		
		if (commandLine.hasOption(PostDownlinkProductProcessorAppOptions.INPUT_FILE_LIST_OPTION.getLongOpt())) {
			inputFileNames = readFileNamesFromFile(commandLine.getOptionValue(PostDownlinkProductProcessorAppOptions.INPUT_FILE_LIST_OPTION.getLongOpt()));
		}
		else {
			inputFileNames = Arrays.asList(commandLine.getTrailingArguments());
		}
		
		/**
		 * MPCS-8710 3/23/2017 - Check that all files exist and remove the ones that do not.
		 */
		inputFileNames = inputFileNames.stream().filter(fileName -> {
			final File fn = new File(fileName);

			if (!fn.exists()) {
				log.warn(fn.getAbsolutePath()+" cannot be processed because it does not exist.");
			}

			return fn.exists();

			}
		).collect(Collectors.toList());
		
        final DatabaseCommandOptions dbOptions = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
		final DictionaryCommandOptions dictOptions = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));
		
		//handles parsing (and configuring) database host, port, user, and password
		dbOptions.parseAllOptionsAsOptional(commandLine);
		//handle parsing (and configuring) FSW dictionary dir and version
		dictOptions.parseAllOptionsAsOptional(commandLine);
//		
//		SessionConfiguration sessCfg = SessionConfiguration.getGlobalInstance();
//		
//		if(sessCfg == null){
//			sessCfg = new SessionConfiguration(appContext);
//		}
//
//		

		/*
		 * Prepare to set-up PostDownlinkProductProcessorOptions
		 */
		final String fswBuildIdString = commandLine.getOptionValue(PostDownlinkProductProcessorAppOptions.FSW_BUILD_ID_OPTION.getLongOpt());
		final String fswVersionString = commandLine.getOptionValue(dictOptions.FSW_VERSION.getLongOpt());
		
		// MPCS-8568 12/12/16 - get the fswDictionaryDirectoryOption and set it in the options object
		final String fswDictionaryDirectory = commandLine.getOptionValue(dictOptions.FSW_DICTIONARY_DIRECTORY.getLongOpt());
		
		final boolean displayToConsole = commandLine.hasOption(PostDownlinkProductProcessorAppOptions.DISPLAY_TO_CONSOLE_OPTION.getLongOpt());
		final boolean yesDatabase = dbOptions.getDatabaseConfiguration().getUseDatabase();
		
		final MessageServiceCommandOptions jmsOptions = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
		final boolean yesJMS = !jmsOptions.NO_JMS.parse(commandLine);
		
		final boolean forceDictionaryVersion = commandLine.hasOption(PostDownlinkProductProcessorAppOptions.OVERRIDE_OPTION.getLongOpt());
		
		debug = commandLine.hasOption(PostDownlinkProductProcessorAppOptions.DEBUG.getLongOpt());
		
		/*
		 * This is used internally by the MessagePortal
		 */
		appContext.getBean(MessageServiceConfiguration.class).setUseMessaging(yesJMS);

		/*
		 * isDoingMetadataCorrection defaults to false.
		 * It is up to individual apps to set to true 
		 * (chill_correct_product_metadata is currently the only one).
		 */
		final boolean isDoingMetadataCorrection = false;

		Long fswBuildId = null;
		String dictionaryVersion = null;
		if ((null == fswVersionString) && (null == fswBuildIdString)) {
			throw new ParseException("No FSW Build ID or FSW Dictionary Version (-D) was specified. Please specify one or the other.");
		}
		if ((fswVersionString != null) && (fswBuildIdString != null)) {
			throw new ParseException("Both the FSW Build ID and the FSW Dictionary Version (-D) were specified. Please specify one or the other, but not both.");
		}
		
		
		if (fswVersionString != null) {
			dictionaryVersion = fswVersionString;
		}
		else { // (fswVersionString == null)
			// parse the id to long.  Error if it does not work correctly.
			try {
				fswBuildId = Long.parseLong(fswBuildIdString);
			}
			catch (final Exception e) {
				throw new ParseException("FSW Build ID must be an integer.");
			}
		}
		
		final PostDownlinkProductProcessorOptions options = new PostDownlinkProductProcessorOptions(	
															displayToConsole, 
															yesDatabase, 
															yesJMS, 
															isDoingMetadataCorrection,
															forceDictionaryVersion, 
															dictionaryVersion,
															fswBuildId,
															fswDictionaryDirectory,
															new StoreIdentifier[] {ILogMessageLDIStore.STORE_IDENTIFIER, IProductLDIStore.STORE_IDENTIFIER},
															debug);
		
		// MPCS-4387 - Just set the force flag based on the input options.
		forceReprocess = commandLine.hasOption(PostDownlinkProductProcessorAppOptions.FORCE_REPROCESS_OPTION.getLongOpt());
		
		return options;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#configure(org.apache.commons. cli.CommandLine)
	 */
	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		throw new RuntimeException("Cannot call configure() directly from within a PostDownlinkProductProcessor Application. Use getConfigurationOptions() instead.");
	}

	/**
	 * Read file names out of the specified file, one fully qualified path/filename per line.
	 * 
	 * @param filename
	 *            the filename to read.
	 * @return an array of Strings representing all the files to process.
	 */
	protected Collection<String> readFileNamesFromFile(final String filename) {
		final List<String> fileNameList = new ArrayList<String>();
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(filename));
			String line;
			while (null != (line = rdr.readLine())) {
				fileNameList.add(line);
			}
		}
		catch (final IOException e) {
			log.warn("Error reading from input file: " + filename);
		}
		finally {
			if (null != rdr) {
				try {
					rdr.close();
				}
				catch (final IOException e) {
					// ignore
				}
			}
		}
		return fileNameList;
	}

    @Override
    public void exitCleanly() {
        shutdown();
    }

	/**
	 * Initializes the app
	 * @param args the command line input arguments in an array of Strings
	 * @return the PostDownlinkProductProcessorOptions object parsed from the input arguments array
	 * @throws Exception
	 */
	public PostDownlinkProductProcessorOptions init(final String[] args) throws Exception {
		// MPCS-8379 10/03/16 - need to now create BaseCommandOptions before parsing the command line
		final BaseCommandOptions cliOptions = createOptions();
		final ICommandLine commandLine = cliOptions.parseCommandLine(args, true);
		final PostDownlinkProductProcessorOptions options = getConfigurationOptions(commandLine);
//		if (null == SessionConfiguration.getGlobalInstance()) {
//			SessionConfiguration.setGlobalInstance(new SessionConfiguration(appContext));
//		}
		return options;
	}

	/**
	 * main loop
	 */
	protected void start() {
		try {
			/*
			 * No input file and no extra input on command line indicates no input files specified
			 */
			if ((null == inputFileNames) || (inputFileNames.size() == 0)) {
				throw new IllegalArgumentException("No input data files specified.");
			}

			log.info("-------------------------------------------------------");
			for (final String filename: inputFileNames) {
				log.info("Processing: \"" + filename + "\"...");
				try {
					getProductProcessor().processProduct(filename, -1, -1);
				}
				catch (final Exception e) {
					log.error(e.getLocalizedMessage());
					if (debug) {
						e.printStackTrace();
					}
				}
				finally {
					log.info("-------------------------------------------------------");
					if (shuttingDown) break;
				}
			}
		}
		catch (final Exception e) {
			log.error(e.getLocalizedMessage());
			if (debug) {
				e.printStackTrace();
			}
		}
		finally {
			if (null != productProcessor) {
				log.info("Processing Complete.");
				log.info(String.format("%4d total product(s) processed:", productProcessor.getTotalProductsProcessed()));
				log.info(String.format("    %4d product(s) processed successfully.", productProcessor.getProductsSuccessfullyProcessed()));
				log.info(String.format("    %4d product(s) aborted due to dictionary mismatch.", productProcessor.getProductsAbortedDueToDictionaryMismatch()));
				log.info(String.format("    %4d product(s) failed for other reasons.", productProcessor.getProductsFailedForOtherReasons()));

				close();
			}
		}
	}

	/**
	 * Return cached product processor if available, otherwise ask sub-class to create one.
	 * 
	 * @return a product product processor
	 * @throws PostDownlinkProductProcessingException
	 */
	protected IPostDownlinkProductProcessor getProductProcessor() throws PostDownlinkProductProcessingException {
		if (null == productProcessor) {
			throw new PostDownlinkProductProcessingException("Must set Poduct Processor by calling setProductProcessor() before using!");
		}
		return productProcessor;
	}

	/**
	 * Set cached product processor if available, otherwise ask sub-class to create one.
	 * 
	 * @param processor
	 * @throws PostDownlinkProductProcessingException
	 */
	public void setProductProcessor(final IPostDownlinkProductProcessor processor) throws PostDownlinkProductProcessingException {
		if (null == productProcessor) {
			synchronized (this) {
				if (null == productProcessor) {
					productProcessor = processor;
				}
			}
		}
		else {
			throw new PostDownlinkProductProcessingException("Cannot set Product Processor more than once!");
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
		log.error(message + " Root cause: " + rootCause.getMessage());
		final IPublishableLogMessage lm = appContext.getBean(IStatusMessageFactory.class).createPublishableLogMessage(TraceSeverity.FATAL, message);
		appContext.getBean(IMessagePublicationBus.class).publish(lm);
		throw new AbortProductAppException(message, rootCause);
	}

	/**
	 * Shut down PDPP processor
	 */
	protected void close() {
		if (null != productProcessor) {
			synchronized(this) {
				if (null != productProcessor) {
					productProcessor.close();
					productProcessor = null;
				}
			}
		}
	}

	/**
	 * ^C Hook
	 */
	protected synchronized void shutdown() {
		shuttingDown = true;
		try {
			wait(2000);
		}
		catch (final Throwable t) {
			// ignore
		}
		close();
	}
}
