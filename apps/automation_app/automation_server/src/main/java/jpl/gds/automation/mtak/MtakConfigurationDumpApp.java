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
package jpl.gds.automation.mtak;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.ConfigurationDumpUtility;
import jpl.gds.common.config.ConfigurationDumpUtility.PropertyDumpFormat;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.spring.context.SpringContextFactory;


public class MtakConfigurationDumpApp extends AbstractCommandLineApp implements Runnable
{
    
	private static final String OUTPUT_FILE_SHORT = "f";
	private static final String OUTPUT_FILE_LONG = "outputFilename";
	private static final String DUMP_FORMAT_SHORT = "d";
	private static final String DUMP_FORMAT_LONG = "dump";

	private boolean dumpXml; // true by default will dump in XML format
	
	
	private String filename;
	private final ApplicationContext appContext;
	
	/**
	 * Default constructor
	 */
	public MtakConfigurationDumpApp()
	{
		super();
		filename = null;
		dumpXml = true;
		appContext = SpringContextFactory.getSpringContext(true);
        TraceManager.getTracer(appContext, Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.WARN);
	}
	
	/*
	 * (non-Javadoc)
	 * @see jpl.gds.shared.cli.AbstractCommandLineApp#createOptions()
	 */
	@Override
	public BaseCommandOptions createOptions()
    {
	    
	    if (optionsCreated.get()) {
            return options;
        }
        
	    
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));
        
        options.addOption(new FileOption(OUTPUT_FILE_SHORT,
            	OUTPUT_FILE_LONG,
            	"filename",
            	"Name of the output file for the mtak configuration dump",
            	false, 
            	false));
     
        
        options.addOption(new StringOption(DUMP_FORMAT_SHORT, 
        	DUMP_FORMAT_LONG,
        	"dumpFormat",
        	"The Mtak config dump format [Supports xml and properties (prop) format]\n"
        			+ "-d <xml, prop> ", 
        	false));

        return(options);
    }
	
	/*
	 * (non-Javadoc)
	 * @see jpl.gds.shared.cli.AbstractCommandLineApp#showHelp()
	 */
	@Override
	public void showHelp() { 
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final OptionSet options = createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + "\n"
                + "--f <filename> [MtakConfiguration will output to the specified filename]\n" 
                + "--d <xml, prop> [Configures the MtakConfiguration output. Supports xml and properties format]");
        pw.println("                   ");

        options.printOptions(pw);
        
        pw.println("This application will output the details of the AMPCS mtak configuration for:");
        pw.println("[Gds Configuration, Gds Hierarchy MPCS Configuration, Template Configuration, "
        		+ "Time Configuration, and System Properties]");

        pw.close();
		
	}
    
	/*
	 * (non-Javadoc)
	 * @see jpl.gds.shared.cli.AbstractCommandLineApp#configure(jpl.gds.shared.cli.ICommandLine)
	 */
	@Override
    public void configure(final ICommandLine commandLine) throws ParseException
    {
        final BaseCommandOptions options = this.createOptions();
        super.configure(commandLine);
        
        // If no file option, output will be directed to System.out
        this.filename = (String) options.getOption(OUTPUT_FILE_SHORT).parse(commandLine, false);
        
        // If no dump option, output will be formatted in XML
        final String dumpFormat = (String) options.getOption(DUMP_FORMAT_SHORT).parse(commandLine, false);
        
        
        if (dumpFormat != null) { 
        	if (dumpFormat.equalsIgnoreCase("prop")) { 
        		this.dumpXml = false; // Dump in properties format
        	}
        	// If format is not 'prop' or 'xml', throw exception (unsupported format)
        	else if (! (dumpFormat.equalsIgnoreCase("xml") || dumpFormat.equalsIgnoreCase("prop")) ) { 
        		throw new ParseException("Property dump cannot be displayed in format '" 
        				+ commandLine.getOptionValue(DUMP_FORMAT_SHORT).trim()
            			 + "'\nExpected 'prop' or 'xml' for option -" + DUMP_FORMAT_SHORT);
        	}
        }

    }					
    
    @Override
    public void run()
    {

    	PrintWriter writer = null;
    	String filepath = null;
    	try
		{
    		if(filename != null)
    		{
    			final File tempFile = File.createTempFile("mtak-" + filename, ".tmp");
    			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
    			filepath = tempFile.getAbsolutePath();
    		}
    		else
    		{
    			writer = new PrintWriter(new OutputStreamWriter(System.out,"UTF-8"));
    		}
			
    		if (!this.dumpXml) {
    		    writer.write("[MtakConfigurationDumpApp]\n");
    		}
    		
    		final ConfigurationDumpUtility cdu = new ConfigurationDumpUtility(appContext);
    		
    		final PropertyDumpFormat pdf = this.dumpXml ? PropertyDumpFormat.XML : PropertyDumpFormat.PROPERTIES;
    		
    		cdu.collectAndDumpAllProperties(writer, pdf, true, true, GdsHierarchicalProperties.PropertySet.NO_DESCRIPTIVES);
			
			writer.flush();
			writer.close();
			
			if(filepath != null) { 
                TraceManager.getDefaultTracer()
                        .info("Successfully wrote Mtak config dump to " + filepath);
			}
		}
		catch(final IOException e)
		{
			throw new RuntimeException("Could not initialize properties output file " + filename);
		}
    	
    }
 
    
	/**
	 * Main method of the MtakConfigurationDumpApp. Parses the command line and
	 * returns the properties in the specified format. If supplied, the results
	 * are written to the specified file
	 * 
	 * @param args
	 *            the command line arguments as an array of String values
	 */
    public static void main(final String[] args)
	{
		final MtakConfigurationDumpApp app = new MtakConfigurationDumpApp();

		try
		{
			final BaseCommandOptions opts = app.createOptions();
			final ICommandLine cl = opts.parseCommandLine(args, true);
			
			app.configure(cl);
			app.run();
		}
		catch(final Exception e)
		{
            TraceManager.getDefaultTracer()
                    .error("The " + ApplicationConfiguration.getApplicationName()
                            + " application encountered an exception: " + e.getMessage(), e.getCause());
            e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
}
