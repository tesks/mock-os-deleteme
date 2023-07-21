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

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.product.api.AbortProductAppException;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.dictionary.IProductDefinitionDumper;
import jpl.gds.product.api.dictionary.IProductDictionary;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.QuitSignalHandler;
import jpl.gds.shared.sys.Shutdown;

/**
 * DpDefViewApp is the main application class for the product definition viewer. It takes in
 * the product type identification information and produces a dump of the dpo.xml or
 * dp.xml file associated with the product identifier.
 *
 */
public class DpDefViewApp implements IQuitSignalHandler {
    
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_dp_def_view");
    
    /**
     * Short command line option for the APID or DPO VID.
     */
    public static final String ID_OPTION_SHORT = "i";
    /**
     * Long command line option for the APID for DPO VID.
     */
    public static final String ID_OPTION_LONG = "id";
    /**
     * Short command line option for the APID name of DPO name.
     */
    public static final String NAME_OPTION_SHORT = "n";
    /**
     * Long command line option for the APID name of DPO name.
     */
    public static final String NAME_OPTION_LONG = "name";
    /**
     * Short command line option for product definition version.
     */
    public static final String VERSION_OPTION_SHORT = "p";
    /**
     * Long command line option for product definition version.
     */
    public static final String VERSION_OPTION_LONG = "productVersion";
    /**
     * Short command line option for product definition subtype. (MER only)
     */
    public static final String SUBTYPE_OPTION_SHORT = "s";
    /**
     * Long command line option for product definition subtype. (MER only)
     */
    public static final String SUBTYPE_OPTION_LONG = "subtype";
    
    private IProductDictionary dictionary;
    private IApidDefinitionProvider apids;
    private IProductDefinitionDumper dumper;
    private String dictDirOverride;
    private String dictVersionOverride;
    private int productId;
    private String productName;
    private String productVersion = "0";
    private String subClassifier;
    
    private final ApplicationContext appContext;
    
    /**
     * Creates an instance of DpDefViewApp.
     */
    public DpDefViewApp() {
        
        /* Add QuitSignalHandler so logging will be shutdown */
        Runtime.getRuntime().addShutdownHook(new Thread(new QuitSignalHandler(this), Shutdown.THREAD_NAME));
        
        appContext = SpringContextFactory.getSpringContext(true);
        ReservedOptions.setApplicationContext(appContext);
    }
    
    /**
     * Creates the command line options for this application.
     * @return an Options object describing all valid command line options 
     */
    public Options createOptions() {
        final Options options = new Options();
        
        options.addOption(ReservedOptions.getOption(ReservedOptions.HELP_SHORT_VALUE));
        options.addOption(ReservedOptions.getOption(ReservedOptions.VERSION_SHORT_VALUE));
        options.addOption(ReservedOptions.getOption(ReservedOptions.FSW_DICTIONARYDIR_SHORT_VALUE));
        options.addOption(ReservedOptions.getOption(ReservedOptions.FSWVERSION_SHORT_VALUE));
        options.addOption(ReservedOptions.createOption(ID_OPTION_SHORT, 
        	    ID_OPTION_LONG, 
        		"APID or DPO VID", 
        		"The product APID or DPO VID to dump the definition for; name can be supplied with --" + 
        		NAME_OPTION_LONG + " instead"));
        options.addOption(ReservedOptions.createOption(NAME_OPTION_SHORT, 
        		NAME_OPTION_LONG, "APID or DPO Name", 	
        		"The APID or DPO dictionary name to dump the definition for; APID or VID can be supplied with --" + 
        		ID_OPTION_LONG + " instead"));
        options.addOption(ReservedOptions.createOption(VERSION_OPTION_SHORT, 
        		VERSION_OPTION_LONG, "version", 	
        		"The product version to dump the definition for, if applicable"));
        options.addOption(ReservedOptions.createOption(SUBTYPE_OPTION_SHORT, 
        		SUBTYPE_OPTION_LONG, "subtype", 	
        		"The product subtype or class to dump the definition for, if applicable"));
        return options;
    }
    
    /**
     * Parses the command line into a CommandLine object.
     * @param args the command line arguments
     * @return a CommandLine object resulting from the parse
     * @throws ParseException if there is a problem parsing the supplied command line
     */
    @SuppressWarnings("DM_EXIT")
    public CommandLine parseCommandLine(final String[] args) throws ParseException {
        final Options options = createOptions();
        
        CommandLine commandLine = null;
        try {
            final CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(createOptions(), args);
            if(args.length == 0)
            { // no arguments at all...likely need help!
                showHelp(options);
                System.exit(0);
            }
        }
        catch (final MissingOptionException e) {
            
            // if -h,--help is specified, suppress "mission option" errors
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals("-h") || args[i].equals("--help")) {
                    System.exit(0);
                }
            }
            throw new ParseException("Error: Missing option: " + e.getMessage());
         }
        catch (final MissingArgumentException e) {

            throw new ParseException(e.getMessage());
        }
        catch (final ParseException e) {
            throw e;
        }
        
        return commandLine;
    }
    
    /**
     * Display application arguments and options.
     * @param options the Options object defining possible arguments/options
     */
    private void showHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(APP_NAME +  " --id <apid or dpo vid> | --name <apid or dpo name> [--fswDictionaryDir <directory> --fswVersion <version>]\n" +
        		"          [--subtype <sub-classification> --productVersion <version>]", options);
    }
    
    /**
     * Configures the application from command line objects.
     * @param commandLine the parsed CommandLine object
     * @throws ParseException if there is a problem configuring the application from the parsed command line
     */
    @SuppressWarnings("DM_EXIT")
    public void configure(final CommandLine commandLine) throws ParseException {
        
        // help options
        if (commandLine == null) {
            showHelp(createOptions());
            System.exit(1);
        }
        if (commandLine.hasOption(ReservedOptions.HELP_SHORT_VALUE)) {
            showHelp(createOptions());
            System.exit(0);
        }
        
        // version option
        if (commandLine.hasOption(ReservedOptions.VERSION_SHORT_VALUE)) {
            showVersion();
            System.exit(0);
        }
        
        if (commandLine.hasOption(ID_OPTION_SHORT) && commandLine.hasOption(NAME_OPTION_SHORT)) {
        	throw new ParseException("ID or Name should be supplied, but not both");
        }
        
        if (commandLine.hasOption(ID_OPTION_SHORT)) {
            final String idStr = commandLine.getOptionValue(ID_OPTION_SHORT);
            try {
            	productId = GDR.parse_int(idStr);
            } catch (final Exception e) {
            	throw new ParseException("Value for " + ID_OPTION_LONG + " must be an integer");
            }
        }
        
        if (commandLine.hasOption(NAME_OPTION_SHORT)) {	
            productName = commandLine.getOptionValue(NAME_OPTION_SHORT);
        } 
        
        if (commandLine.hasOption(ReservedOptions.FSW_DICTIONARYDIR_SHORT_VALUE)) {
            dictDirOverride = commandLine.getOptionValue(ReservedOptions.FSW_DICTIONARYDIR_SHORT_VALUE);
            final File dirFile = new File(dictDirOverride);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
            	throw new ParseException("Error: Dictionary directory " + dictDirOverride + " does not exist or is not a directory");
            }
            appContext.getBean(DictionaryProperties.class).setFswDictionaryDir(dictDirOverride);
        }
        
        if (commandLine.hasOption(ReservedOptions.FSWVERSION_SHORT_VALUE)) {
            dictVersionOverride = commandLine.getOptionValue(ReservedOptions.FSWVERSION_SHORT_VALUE);
            appContext.getBean(DictionaryProperties.class).setFswVersion(dictVersionOverride);
        } else {
        	 appContext.getBean(DictionaryProperties.class).setFswVersion(
        	         appContext.getBean(DictionaryProperties.class).getDefaultFswVersion());
        }
        
        if (commandLine.hasOption(VERSION_OPTION_SHORT)) {
        	productVersion = commandLine.getOptionValue(VERSION_OPTION_SHORT);
        }
        
        if (commandLine.hasOption(SUBTYPE_OPTION_SHORT)) {
        	subClassifier = commandLine.getOptionValue(SUBTYPE_OPTION_SHORT);
        }
    }
    
    /**
     * Display application version.
     */
    private void showVersion() {
        System.out.println(ReleaseProperties.getProductLine() + " " + APP_NAME + " " + ReleaseProperties.getVersion());
    }

    private void init() throws AbortProductAppException, ProductException {

        try {
            dictionary = appContext.getBean(IProductDictionary.class);
            dumper = appContext.getBean(IProductDefinitionDumper.class);
            apids = appContext.getBean(IApidDefinitionProvider.class);
        } catch (final Exception e) {
            throw new ProductException("Unable to create mission product adapter or dictionary: " + e.toString());
        }       
    }
    
    /**
     * Executes the application. Must be called after configure().
     * @throws IOException if there is a problem reading the product definition file
     * @throws ProductException if there is a problem interpreting the product definition file
     * @throws AbortProductAppException if there is a general problem that causes the application to abort 
     */
    public void execute() throws AbortProductAppException, ProductException, IOException {        
        init();
        
        dumper.dumpDefinition(apids, dictionary, productId, subClassifier, productName, productVersion);
    }
         
	/**
     * The main method for the application.
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        final DpDefViewApp app = new DpDefViewApp();
        
        try {
            final CommandLine commandLine = app.parseCommandLine(args);
            app.configure(commandLine);
        }catch (final ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        try {
          app.execute();
        } catch (final AbortProductAppException e) {
            if (e.getCause() != null) {
            	e.getCause().printStackTrace();
            }
            System.exit(1);
        } catch (final Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
        System.exit(0);
    }    
}
