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
package jpl.gds.app.tools.config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.ConfigurationDumpUtility;
import jpl.gds.common.config.ConfigurationDumpUtility.PropertyDumpFormat;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;

public class ConfigurationDumpApp extends AbstractCommandLineApp 
{
    
	private static final String OUTPUT_FILE_SHORT = "f";
	static final String OUTPUT_FILE_LONG = "outputFilename";

	private static final String DUMP_FORMAT_SHORT = "d";
	static final String DUMP_FORMAT_LONG = "dumpFormat";
	
    private static final String LIST_CATEGORIES_SHORT = "l";
	static final String LIST_CATEGORIES_LONG = "listCategories";
	
    private static final String INCLUDE_DESC_SHORT = "i";
    static final String INCLUDE_DESC_LONG = "includeDescriptives";
	
	private static final String PATTERN_SHORT = "p";
	static final String PATTERN_LONG = "pattern";
	
	private static final String CATEGORY_SHORT = "c";
	static final String CATEGORY_LONG = "category";
	
    private static final String PROPERTY_NAME_SHORT = "n";
    static final String PROPERTY_NAME_LONG = "propertyName";
    
    private static final String WHICH_FILE_SHORT = "w";
    static final String WHICH_FILE_LONG = "whichFile";
    
    private static final String FILENAME_ARG = " <filename>";

	private ConfigurationDumpUtility.PropertyDumpFormat dumpFormat;
	private boolean listCategories;
	private String filename;
	private final ApplicationContext appContext;
	private String regex;
	private GdsHierarchicalProperties.PropertySet includeDescriptives;
	private boolean whichFile;
	private String propertyName;
    private String category;
	
	private FileOption filenameOption;
	private EnumOption<ConfigurationDumpUtility.PropertyDumpFormat> formatOption;
	private FlagOption listCategoriesOption;
    private FlagOption whichFileOption;
	private StringOption regexOption;
	private StringOption propertyOption;
	private StringOption categoryOption;
	private FlagOption descriptivesOption;
	
	/**
	 * Default constructor
	 */
	public ConfigurationDumpApp()
	{
		super();
		filename = null;
		appContext = SpringContextFactory.getSpringContext(true);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BaseCommandOptions createOptions() {
	
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));
        
        filenameOption = new FileOption(OUTPUT_FILE_SHORT,
                OUTPUT_FILE_LONG,
                "filename",
                "Name of the output file for the configuration dump; defaults to stdout",
                false, 
                false);
        options.addOption(filenameOption);
     
        formatOption = 
                new EnumOption<>(ConfigurationDumpUtility.PropertyDumpFormat.class, DUMP_FORMAT_SHORT, 
                DUMP_FORMAT_LONG,
                "format",
                "The configuration dump format",
                false);
        formatOption.setDefaultValue(ConfigurationDumpUtility.PropertyDumpFormat.PROPERTIES);
        options.addOption(formatOption);
        
        listCategoriesOption = new FlagOption(LIST_CATEGORIES_SHORT, LIST_CATEGORIES_LONG, "just list available property categories", false);
        options.addOption(listCategoriesOption); 
        
        whichFileOption = new FlagOption(WHICH_FILE_SHORT, WHICH_FILE_LONG, "guesses which system-level file contains a property", false);
        options.addOption(whichFileOption); 
        
        descriptivesOption = new FlagOption(INCLUDE_DESC_SHORT,
                INCLUDE_DESC_LONG, "include descriptive/documentation properties", false);
        options.addOption(descriptivesOption); 
        
        regexOption = new StringOption(PATTERN_SHORT, PATTERN_LONG, "pattern", "Java regular expression for selecting specific properties", false);
        options.addOption(regexOption);
        
        categoryOption = new StringOption(CATEGORY_SHORT, CATEGORY_LONG, "category", "category name for selecting specific properties", false);
        options.addOption(categoryOption);
        
        propertyOption = new StringOption(PROPERTY_NAME_SHORT, PROPERTY_NAME_LONG, "name", "property name for selecting one specific property", false);
        options.addOption(propertyOption);
        
        return(options);
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showHelp() { 
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final OptionSet options = createOptions().getOptions();
        
        final PrintWriter pw = new PrintWriter(System.out);

        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [--"
                + OUTPUT_FILE_LONG + FILENAME_ARG + "] [--"
                + PROPERTY_NAME_LONG + " <name>" + " | --"
                + CATEGORY_LONG + " <category>" + " | --"
                + PATTERN_LONG + " <pattern>" + "] [--"
                + DUMP_FORMAT_LONG + " <XML|PROPERTIES|CSV|DOC_CSV>]");
        pw.println("       " + ApplicationConfiguration.getApplicationName() + " [--"
                + OUTPUT_FILE_LONG + FILENAME_ARG + "] --"
                + LIST_CATEGORIES_LONG);
        pw.println("       " + ApplicationConfiguration.getApplicationName() + " [--"
                + OUTPUT_FILE_LONG + FILENAME_ARG + "] --"
                + PROPERTY_NAME_LONG + " <name>" + " --"
                + WHICH_FILE_LONG);
        pw.println("                   ");

        options.printOptions(pw);
        
        pw.println("This application will output the details of the current AMPCS configuration.");
        pw.println("By default it will dump all non-descriptive AMPCS configuration properties.");
        pw.println("It can also be supplied with a regular expression, property category,");
        pw.println("or one specific property name to dump. Different dump formats are supported.");
        pw.println("Descriptive properties that describe other properties can also be dumped");
        pw.println("using a flag option. Also, a simple list of property categories can be obtained.");
        
        

        pw.close();
		
	}
    
	/**
	 * {@inheritDoc}
	 */
	@Override
    public void configure(final ICommandLine commandLine) throws ParseException
    {
        super.configure(commandLine);
        
        // If no file option, output will be directed to System.out
        this.filename = filenameOption.parse(commandLine, false);
        
        this.listCategories = listCategoriesOption.parse(commandLine);
        
        if (listCategories && (commandLine.hasOption(formatOption.getLongOpt()) ||
            commandLine.hasOption(regexOption.getLongOpt()) || 
            commandLine.hasOption(categoryOption.getLongOpt()) ||
            commandLine.hasOption(descriptivesOption.getLongOpt()) ||
            commandLine.hasOption(whichFileOption.getLongOpt()) ||
            commandLine.hasOption(propertyOption.getLongOpt()))) {
            throw new ParseException("The --" + listCategoriesOption.getLongOpt() + " option cannot be specified along with other options");
        }
        
        this.whichFile = whichFileOption.parse(commandLine);
              
        if (this.whichFile && commandLine.hasOption(formatOption.getLongOpt())) {
            throw new ParseException("The --" + whichFileOption.getLongOpt() + " option cannot be specified along with the --" +
                formatOption.getLongOpt() + " option");
            
        }
        
        this.dumpFormat = formatOption.parseWithDefault(commandLine, false, true);
        
        this.regex = regexOption.parse(commandLine);
        this.propertyName = propertyOption.parse(commandLine);
        this.category = categoryOption.parse(commandLine);
        
        final boolean regexConflictOk = (regex == null && propertyName == null && category == null) || ((regex != null ^ propertyName != null ^ category != null) ^ 
                (regex != null && propertyName != null && category != null));
        
        if (!regexConflictOk) {
            
            throw new ParseException("Only one of --" + regexOption.getLongOpt() + ", --" +
               categoryOption.getLongOpt() + " or --" + propertyOption.getLongOpt() +
               " can be supplied");
        }
            
        if (category != null) {
            category = category.trim();
            if (category.endsWith(".")) {
                category = category.substring(0, category.length() - 1);
            }
            this.regex = category + "\\..*";
        }
        
        if (this.whichFile && this.propertyName == null) {
            throw new ParseException("If the --" + whichFileOption.getLongOpt() + " option is supplied, the --" +
                propertyOption.getLongOpt() + " option must also be supplied");
        }
        
        final Boolean temp = descriptivesOption.parse(commandLine);
        this.includeDescriptives = GdsHierarchicalProperties.PropertySet.NO_DESCRIPTIVES;
        if (this.dumpFormat == PropertyDumpFormat.DOC_CSV || temp.booleanValue()) {
            this.includeDescriptives = GdsHierarchicalProperties.PropertySet.INCLUDE_DESCRIPTIVES;
        }
    }
	
    /**
     * Execute main application logic
     * @throws ApplicationException if there is a problem writing the output file
     */
    public void exec() throws ApplicationException
    {

    	PrintWriter writer = null;
    	FileOutputStream fos = null;
    	try 
		{
    		if(filename != null)
    		{
    		    fos = new FileOutputStream(filename);
    			writer = new PrintWriter(new OutputStreamWriter(fos));
    		}
    		else
    		{
    			writer = new PrintWriter(new OutputStreamWriter(System.out,"UTF-8"));
    		}
    		
    		final ConfigurationDumpUtility collector = new ConfigurationDumpUtility(appContext);
    		
    		if (this.listCategories) {
    		    final Set<String> cats = collector.collectCategories();
    		    for (final String cat: cats) {
    		        writer.println(cat);
    		    }
    		} else if (this.propertyName != null) {
    		    if (this.whichFile) {
    		        writer.println(collector.getBestMatchToFileText(propertyName));
    		    } else {
    		        collector.dumpOneProperty(writer, this.propertyName, this.dumpFormat);
    		    }
    		} else {
    		    collector.collectAndDumpAllProperties(writer, dumpFormat, regex, false, false, includeDescriptives);
    		}
			writer.flush();
			if (fos != null) {
			    fos.close();
			}
			
			if(filename != null) { 
                TraceManager.getDefaultTracer(appContext).info("Successfully wrote property dump to " + filename);
			}
		}
		catch(final IOException e)
		{
			throw new ApplicationException("Could not write to properties output file " + filename + ": " + e.toString());
		}
    	catch (final Exception e) {
    	    e.printStackTrace();
    	    throw new ApplicationException("Unexpected error writing output file " + filename + ": " + e.toString(), e);
    	}
    	finally {
    	    if (writer != null) {
    	        writer.flush();
    	    }

    	    if (fos != null) {
    	        try {
    	            fos.close();
    	        } catch (final IOException e) {
    	            // do nothing
    	        }
    	    }
    	}
    	
    }
    
	/**
	 * Main method of the ConfigurationDumpApp. Parses the command line and
	 * returns the properties in the specified format. If supplied, the results
	 * are written to the specified file
	 * 
	 * @param args
	 *            the command line arguments as an array of String values
	 */
    public static void main(final String[] args)
	{
		final ConfigurationDumpApp app = new ConfigurationDumpApp();

		try
		{
			final BaseCommandOptions opts = app.createOptions();
			final ICommandLine cl = opts.parseCommandLine(args, true);
			
			app.configure(cl);
			app.exec();
		}
		catch(final ParseException e)
        {
            TraceManager.getDefaultTracer().error(e.getMessage() == null ? e.toString() : e.getMessage());
            System.exit(1);
        }
		catch(final Exception e)
		{
		    e.printStackTrace();
			TraceManager.getDefaultTracer().error("The " + 
		    ApplicationConfiguration.getApplicationName() + " application encountered an exception: " + 
			        e.getMessage(), e);
			System.exit(1);
		}
		System.exit(0);
	}

    // package private getters to use for tests

    String getFilename() {
        return filename;
    }

    String getCategory() {
        return category;
    }

    boolean isListCategories() {
        return listCategories;
    }

    public ConfigurationDumpUtility.PropertyDumpFormat getDumpFormat() {
        return dumpFormat;
    }

    boolean isWhichFile() {
        return whichFile;
    }

    String getPropertyName() {
        return propertyName;
    }

    GdsHierarchicalProperties.PropertySet getIncludeDescriptives() {
        return includeDescriptives;
    }

    String getRegex() {
        return regex;
    }

}
