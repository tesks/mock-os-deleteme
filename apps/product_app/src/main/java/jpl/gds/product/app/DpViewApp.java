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
package jpl.gds.product.app;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.cli.legacy.app.CommandLineApp;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.product.api.AbortProductAppException;
import jpl.gds.product.api.DpViewAppConstants;
import jpl.gds.product.api.ProductApiBeans;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.ProductOutputFormat;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.decom.IProductDecom;
import jpl.gds.product.api.decom.IProductDecomUpdater;
import jpl.gds.product.api.decom.IStoredProductInput;
import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.QuitSignalHandler;
import jpl.gds.shared.sys.Shutdown;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * DpViewApp is the main application class for the product viewer. It takes a list
 * of product data or metadata files and displays the product data in human-readable 
 * format. It will also launch product specific custom processors and viewers.
 *
 */
public class DpViewApp implements CommandLineApp {

    private final Tracer                 log;

	private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_dp_view");

	private IProductDecomOutputFormatter outf;
	private ProductOutputFormat outputType = ProductOutputFormat.TEXT_PRODUCT_OUTPUT;
	private List<String> inputList;
	private String directory = null;
	private String file = null;
	private boolean showLaunch = false;
	private boolean launchProductViewer = false;
	private boolean launchDpoViewer = false;
	private final IMessagePublicationBus context;
	private IStoredProductInput input;
	private boolean ignoreChecksum = false;
	private String dictDirOverride;
	private String dictVersionOverride;
	private IProductDecomUpdater dump;
	private boolean xml;
	private boolean csv;
	private final boolean usesDpos;
	private Options options;
	private List<String> dpoList;
	
	private final ApplicationContext appContext;

	/**
	 * Creates an instance of DpViewApp.
	 */
	public DpViewApp() {
	    /* Add QuitSignalHandler so logging will be shutdown */
	    Runtime.getRuntime().addShutdownHook(new Thread(new QuitSignalHandler(this), Shutdown.THREAD_NAME));
	    appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getDefaultTracer(appContext);
	    ReservedOptions.setApplicationContext(appContext);
	    context = appContext.getBean(IMessagePublicationBus.class);
	    usesDpos = appContext.getBean(IProductPropertiesProvider.class).isProcessDpos();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.cli.legacy.app.CommandLineApp#createOptions()
	 */
	@Override
	public Options createOptions() {
		options = new Options();

		options.addOption(ReservedOptions.getOption(ReservedOptions.HELP_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.VERSION_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.FSW_DICTIONARYDIR_SHORT_VALUE));
		options.addOption(ReservedOptions.getOption(ReservedOptions.FSWVERSION_SHORT_VALUE));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.DIRECTORY_OPTION_SHORT, 
		        DpViewAppConstants.DIRECTORY_OPTION_LONG, 
		        DpViewAppConstants.DIRECTORY_OPTION_LONG, 
				"Output to files in the given directory instead of the console; " +
		"result files will be output to that directory and named to match the product files"));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.FILENAME_OPTION_SHORT, 
		        DpViewAppConstants.FILENAME_OPTION_LONG, 
		        DpViewAppConstants.FILENAME_OPTION_LONG, 
		"Output to file instead of the console; only supported for processing one product at a time"));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.XML_OPTION_SHORT, DpViewAppConstants.XML_OPTION_LONG, null, "generate xml output rather than plain text output"));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.CSV_OPTION_SHORT, DpViewAppConstants.CSV_OPTION_LONG, null, "generate csv output rather than plain text output"));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.NOTEXT_OPTION_SHORT, 
		        DpViewAppConstants.NOTEXT_OPTION_LONG, null, "suppress standard product text output to console and file"));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.IGNORE_CHECKSUM_SHORT, 
		        DpViewAppConstants.IGNORE_CHECKSUM_LONG, null, "view products even if checksum is invalid"));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.LAUNCH_PRODUCT_VIEWER_SHORT, 
		        DpViewAppConstants.LAUNCH_PRODUCT_VIEWER_LONG, null, "launch product viewer"));
		options.addOption(ReservedOptions.createOption(DpViewAppConstants.SHOW_LAUNCH_SHORT, 
		        DpViewAppConstants.SHOW_LAUNCH_LONG, null, "show product/DPO viewer launch detail to the console"));
		if (usesDpos) {
			options.addOption(ReservedOptions.createOption(DpViewAppConstants.LAUNCH_DPO_VIEWER_SHORT, 
			        DpViewAppConstants.LAUNCH_DPO_VIEWER_LONG, null, "launch DPO viewers"));
			options.addOption(ReservedOptions.createOption(DpViewAppConstants.DPO_LIST_SHORT, 
			        DpViewAppConstants.DPO_LIST_LONG, "dpo-list", "comma-separated list of DPO VIDS or Names to process; others will be skipped"));
		}
		return options;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.cli.legacy.app.CommandLineApp#showHelp()
	 */
	@Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

		final HelpFormatter formatter = new HelpFormatter();
		final PrintWriter pw = new PrintWriter(System.out);
		System.out.println(APP_NAME + " [options] <input-files>\n" +
		                   APP_NAME + " --directory <destination-dir> [options] <input-files>\n" +
		                   APP_NAME + " --filename <output-file> [options] <input-file>\n\n");
		formatter.printOptions(pw, 80, options, 7, 2);
        pw.flush();
		System.out.println("\nDefault output format is plain text to console. Multiple input file names should");
		System.out.println("be separated by spaces. Input file names may be either " + ProductStorageConstants.METADATA_SUFFIX +
				" or " + ProductStorageConstants.DATA_SUFFIX + " files as");
		System.out.println("created by the downlink process.");
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.cli.legacy.app.CommandLineApp#configure(org.apache.commons.cli.CommandLine)
	 */
	@Override
	@SuppressWarnings("DM_EXIT")
	public void configure(final CommandLine commandLine) throws ParseException {

		if (commandLine == null) {
			showHelp();
			System.exit(1);
		}
		// help options
		ReservedOptions.parseHelp(commandLine,this);
		ReservedOptions.parseVersion(commandLine);

		if (!TraceManager.getDefaultTracer().isDebugEnabled()) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.WARN);

		}
		
		/**
		 * INFO outputs are a regression, so since dictionary parsing is
		 * now published with ExternalTracer, turn this off in the same was as DefaultTracer
		 */
        if (!TraceManager.getTracer(Loggers.DICTIONARY).isDebugEnabled()) {
            TraceManager.getTracer(Loggers.DICTIONARY).setLevel(TraceSeverity.WARN);
		}

		// No extra input on command line indicates no input files specified
		if(commandLine.getArgList().isEmpty()){
			throw new ParseException("Error: No product input files specified on command line");
		}

		inputList = new ArrayList<String>();

		if (commandLine.hasOption(DpViewAppConstants.DIRECTORY_OPTION_SHORT)) {
			directory = commandLine.getOptionValue(DpViewAppConstants.DIRECTORY_OPTION_SHORT);
			if (directory == null) {
				throw new ParseException("You must supply a directory name as value for the " + DpViewAppConstants.DIRECTORY_OPTION_LONG + " option");
			}
		}

		if (commandLine.hasOption(DpViewAppConstants.FILENAME_OPTION_SHORT)) {
			file = commandLine.getOptionValue(DpViewAppConstants.FILENAME_OPTION_SHORT);
			if (file == null) {
				throw new ParseException("You must supply a filename as value for the " + DpViewAppConstants.FILENAME_OPTION_LONG + " option");
			}
		}

		if (file != null && directory != null) {
			throw new ParseException("The " + DpViewAppConstants.DIRECTORY_OPTION_LONG + " and " + DpViewAppConstants.FILENAME_OPTION_LONG + " options are mutually exclusive");
		}
		
		launchProductViewer = commandLine.hasOption(DpViewAppConstants.LAUNCH_PRODUCT_VIEWER_SHORT);
		launchDpoViewer = commandLine.hasOption(DpViewAppConstants.LAUNCH_DPO_VIEWER_SHORT);
		
		ignoreChecksum = commandLine.hasOption(DpViewAppConstants.IGNORE_CHECKSUM_SHORT);
		final boolean noText = commandLine.hasOption(DpViewAppConstants.NOTEXT_OPTION_SHORT);
		if (noText) {
			outputType = ProductOutputFormat.NO_PRODUCT_OUTPUT;
		}
		showLaunch = commandLine.hasOption(DpViewAppConstants.SHOW_LAUNCH_SHORT);

		if (commandLine.hasOption(DpViewAppConstants.XML_OPTION_SHORT)) {
			outputType = ProductOutputFormat.XML_PRODUCT_OUTPUT;
			xml = true;
		}
		
		if (commandLine.hasOption(DpViewAppConstants.CSV_OPTION_SHORT)) {
			outputType = ProductOutputFormat.CSV_PRODUCT_OUTPUT;
			csv = true;
		}

		if (xml && csv) {
			throw new ParseException("Both csv and xml output cannot be requested at the same time");
		}
		
		ReservedOptions.parseFswDictionaryDir(commandLine, false, true);
		if (commandLine.hasOption(ReservedOptions.FSW_DICTIONARY_DIRECTORY.getOpt())) {
			dictDirOverride = appContext.getBean(DictionaryProperties.class).getFswDictionaryDir();
		}
		ReservedOptions.parseFswVersion(commandLine, false, true);
		if (commandLine.hasOption(ReservedOptions.FSW_VERSION.getOpt())) {
			dictVersionOverride = appContext.getBean(DictionaryProperties.class).getFswVersion();
		}
		if (commandLine.hasOption(DpViewAppConstants.DPO_LIST_SHORT)) {
			dpoList = new ArrayList<String>(1);
			final String dpoStr = commandLine.getOptionValue(DpViewAppConstants.DPO_LIST_SHORT);
			
			if (dpoStr == null) {
				throw new ParseException("The --" + DpViewAppConstants.DPO_LIST_LONG + " option must have an argument");
			}
			final String[] dpos = dpoStr.split(",");
			for (final String dpo: dpos) {
				dpoList.add(dpo.trim());
			}
		}
		
		// get remaining command line arguments and treat as input files
		final List<String> argList = commandLine.getArgList();
		inputList = new ArrayList<String>();

		for (int i = 0; i < argList.size(); i++) {
			final File f = new File(argList.get(i));
			if (f.exists() && !f.isDirectory()) {
				inputList.add(argList.get(i));
			} else {
				log.warn("Input file " + argList.get(i) + " does not exist");
			}
		}

		if (inputList.size() == 0) {
			throw new ParseException("There are no input files to process");
		}

		if (file != null && inputList.size() > 1) {
			throw new ParseException("Multiple product files cannot be supplied with a single file as output");
		}
		
		if (outputType == ProductOutputFormat.NO_PRODUCT_OUTPUT && !launchDpoViewer && !launchProductViewer) {
			throw new ParseException("Command line requests no output and no viewers.");
		}
	}

	private void init() throws AbortProductAppException {


		// enabling loading of APID dictionary, load is deferred to product decom
		appContext.getBean(FlightDictionaryLoadingStrategy.class).enableApid();
		input = addStoredProductInput(context);
		dump = addProductDecom(context);
	}

	/**
	 * Executes the application. Must be called after configure().
	 * @return ProductDecom error code
	 * @throws AbortProductAppException if there is a fatal error during processing
	 */
	public int execute() throws AbortProductAppException {

		init();

		File destFile = null;
		if (directory != null) {
			destFile = new File(directory);
			if (!destFile.exists()) {
				final boolean ok = destFile.mkdirs();
				if (!ok) {
					throw new AbortProductAppException("Directory " + destFile.getParent() + " cannot be created");
				}
			}
		} else if (file != null) {
			destFile = new File(file);
			if (destFile.exists() && destFile.isDirectory()) {
				throw new AbortProductAppException("File " + file + " exists and is a directory");
			}
		}
		String filename = null;
		String outputFile = null;
		if (file != null && inputList.size() > 1) {
			throw new AbortProductAppException("Multiple product files cannot be supplied with a single file as output");
		}
		try {
			for (final Iterator<String> i = inputList.iterator(); i.hasNext();) {
				filename = i.next();
				if (destFile != null) {
					if (destFile.isDirectory()) {
						outputFile = destFile.getPath() + File.separator + getNameWithoutExtension(filename) + 
						(xml ? ".xml" : ".txt");
					} else {
						outputFile = file;
					}
				}

				outf = appContext.getBean(IProductDecomOutputFormatter.class, outputType, outputFile);
				dump.setOutputFormatter(outf);
				
				/* Check for null output stream before setting. If no text
				 * output is requested, there is no output stream. Fixes NPE.
				 */
				if (outputFile != null) {
				    dump.setPrintStream(outf.getPrintStream());
				}

				outf.startOutput();
				input.read(filename);
				outf.endOutput();
			}
		}
		catch (final ProductException e) {
			if (e.getMessage() != null) {
				abortApp(e.getMessage(), null);
			} else {
				abortApp("Cannot process product file '" + filename + "'" , e);
			}
		}
		catch (final FileNotFoundException e) {
			abortApp("Product data or metadata file for product '" + filename + "' not found", null);
		}
		catch (final IOException e) {
			abortApp("IO Error reading product data or metadata file for product '" + filename + "'", e);
		}
		
		return dump.getReturnCode();
	}

	private String getNameWithoutExtension(final String name) {
		final File f = new File(name);
		final String nameOnly = f.getName();
		final int dot = nameOnly.lastIndexOf(".");
		if (dot == -1) {
			return nameOnly;
		} else {
			return nameOnly.substring(0, dot);
		}
	}

	/**
	 * Creates the IStoredProductInput object for the current configuration.
	 * IStoredProductInput processes each product as it is read.
	 * @param context the message context to which the IStoredProductInput
	 * should subscribe.
	 * @return the IStoredProductInput object
	 * @throws AbortProductAppException
	 */
	private IStoredProductInput addStoredProductInput(final IMessagePublicationBus context)
	throws AbortProductAppException
	{
		try {
			final IStoredProductInput input = appContext.getBean(IStoredProductInput.class);
			input.setMessageContext(context);
			return input;
		} catch (final Exception e) {
			abortApp("Couldn't create stored product input: " + e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Creates the ProductDumpOutput object for the current configuration. The
	 * ProductDumpOutput sends the formatted product data to output.
	 * @param context
	 * @return
	 * @throws AbortProductAppException
	 */
	private IProductDecomUpdater addProductDecom(final IMessagePublicationBus context) throws AbortProductAppException {
		try {
		    // Use bean name to avoid ambiguity using the interface name alone
			final IProductDecomUpdater output = appContext.getBean( ProductApiBeans.PRODUCT_DECOM, IProductDecomUpdater.class);
			output.setShowProductViewer(launchProductViewer);
			output.setShowDpoViewer(launchDpoViewer);
			output.setIgnoreChecksum(ignoreChecksum);
			output.setShowLaunchInfo(showLaunch);
			output.setDictVersionOverride(dictVersionOverride);
			output.setDictDirOverride(dictDirOverride);
			output.setDpoList(dpoList);
			output.setMessageContext(context);
			output.subscribeToArrivedProducts();
			output.subscribeToAssembledProducts();
			output.subscribeToPartialProducts();
			output.setSupressText(outputType == ProductOutputFormat.NO_PRODUCT_OUTPUT);
			return output;
		} catch (final Exception e) {
			abortApp("Couldn't create product dump output: " + e.getMessage(), e);
		}
		return null;
	}


	/**
	 * Aborts the application.
	 * @param message the error message
	 * @param rootCause the exception that caused the abort
	 * @throws AbortProductAppException
	 */
	private void abortApp(final String message, final Throwable rootCause)
	throws AbortProductAppException
	{
		final IPublishableLogMessage lm = appContext.getBean(IStatusMessageFactory.class).createPublishableLogMessage(TraceSeverity.FATAL,
                message);
		context.publish(lm);
		throw new AbortProductAppException(message, rootCause);
	}

	/**
	 * Retrieves the list of input files that were specific on the command line.
	 * @return a list of String filenames
	 */
	public List<String> getInputList() {
		return inputList;
	}

	/**
	 * Indicates whether product decom of products/DPOS with invalid checksum should be performed.
	 * @return true if viewers suppressed
	 */
	public boolean isIgnoreChecksum() {
		return ignoreChecksum;
	}

	/**
	 * Gets the name of the file or directory to be used for output. Result may be null
	 * if no file output was requested on the command line. 
	 * @return the destination directory or filename; may be null
	 */
	public String getFileOrDirectory() {
		return directory == null ? file : directory;
	}


	/**
	 * The main method for the application.
	 * @param args the command line arguments
	 */
	public static void main(final String[] args) {
		final DpViewApp app = new DpViewApp();

		try {
			final CommandLine commandLine = ReservedOptions.parseCommandLine(args, app);
			app.configure(commandLine);
		}catch (final ParseException e) {
			System.err.println(e.getMessage());
			System.exit(IProductDecom.FAILURE);
		}
		int code = IProductDecom.SUCCESS;
		try {
			code = app.execute();
		} catch (final AbortProductAppException e) {
			if (e.getCause() != null) {
				e.getCause().printStackTrace();
			}
			System.exit(IProductDecom.FAILURE);
		}
		System.exit(code);
	}    
}
