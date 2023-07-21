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

package jpl.gds.app.tools.xml;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.validation.XmlValidationException;
import jpl.gds.shared.xml.validation.XmlValidator;
import jpl.gds.shared.xml.validation.XmlValidatorFactory;
import jpl.gds.shared.xml.validation.XmlValidatorFactory.SchemaType;

/**
 * This application performs XML validation against a schema.
 */
public class XmlValidatorApp extends AbstractCommandLineApp
{
	private static final int ERROR_CODE = 2;
	private static final int FAILURE_CODE = 1;
	private static final int SUCCESS_CODE = 0;
	
	private static final String SCHEMA_TYPE_OPTION_SHORT = "t";
	private static final String SCHEMA_TYPE_OPTION_LONG = "schemaType";
	
	private File schemaFile;
	private File xmlFile;
	private SchemaType type;
	
	private final EnumOption<SchemaType> schemaOption = new EnumOption<>(SchemaType.class, 
	        SCHEMA_TYPE_OPTION_SHORT, SCHEMA_TYPE_OPTION_LONG,
	        "type", "The type of schema being validated against. Valid options are: XSD, RNC. If not specified" +
	                ", the schema type will be attempted to be determined dynamically from the schema file name.",
	        false);        
	        

    /**
     * Constructor.
     *
     */	
	public XmlValidatorApp()
	{
		this.schemaFile = null;
		this.xmlFile = null;
		this.type = null;

	}

	@Override
	public void configure(final ICommandLine commandLine) throws ParseException
	{
	    super.configure(commandLine);

	    final String[] leftoverArgs = commandLine.getTrailingArguments();

	    if(leftoverArgs.length != 2)
	    {
	        throw new ParseException("This application must be supplied with exactly two command line values.  Use the --help" + 
	                " option to see usage details.");
	    }

	    this.schemaFile = new File(leftoverArgs[0]);
	    if(!this.schemaFile.exists())
	    {
	        throw new ParseException("The input schema file \"" + leftoverArgs[0] + "\" does not exist.");
	    }

	    this.xmlFile = new File(leftoverArgs[1]);
	    if(!this.xmlFile.exists())
	    {
	        throw new ParseException("The input XML file \"" + leftoverArgs[1] + "\" does not exist.");
	    }

	    this.type = schemaOption.parse(commandLine);

	    if (this.type == null)
	    {

	        final String schemaFilename = this.schemaFile.getName();
	        final int dotIndex = schemaFilename.lastIndexOf('.');
	        final String extension = schemaFilename.substring(dotIndex+1);
	        try
	        {
	            this.type = SchemaType.valueOf(extension.trim().toUpperCase());
	        }
	        catch(final IllegalArgumentException iae)
	        {
	            throw new ParseException("The input schema type could not be determined from the schema file name." +
	                    " Please run again using the --" + SCHEMA_TYPE_OPTION_LONG + " option to specify the schema type.  Use the --help" + 
	                    " option to see usage details.");
	        }
	    }
	}

	/**
	 * Creates an Options object containing possible command line arguments/options.
	 * 
	 * @return the Options object
	 */
	@Override
    public BaseCommandOptions createOptions()
	{
	    if (optionsCreated.get()) {
            return options;
        }
        
		super.createOptions();
		schemaOption.getParser().setConvertToUpperCase(true);

		options.addOption(schemaOption);
		
		return options;
	}

	/**
	 * Get the usage for this application
	 * 
	 */
	@Override
    public void showHelp()
	{
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " [options] <schema_file> <xml_file>\n");
        
        createOptions().getOptions().printOptions(pw);
        
        pw.flush();
     
	}

    /**
     * Validate XML against schema.
     *
     * @return Status of validation
     *
     * @throws XmlValidationException Error in validation
     */
	public boolean doValidation() throws XmlValidationException
	{
		final XmlValidator validator = XmlValidatorFactory.createValidator(this.type);
		return(validator.validateXml(this.schemaFile,this.xmlFile));
	}


    /**
     * Main entry to application.
     *
     * @param args command-line arguments
     */	
	public static void main(final String[] args)
	{
		XmlValidatorApp app = null;
		try
		{
			app = new XmlValidatorApp();
			final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
			app.configure(commandLine);
		}
		catch(final ParseException pe)
		{
			TraceManager.getDefaultTracer().error("Command line parameter parsing failed: " + pe.getMessage());
			System.exit(ERROR_CODE);
		}
		
		boolean result = false;
		try
		{
			result = app.doValidation();
		}
		catch(final XmlValidationException e)
		{
		    System.err.println("Validation Failed due to an error: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
			if (e.getCause() != null) {
			    TraceManager.getDefaultTracer().error(e.getCause().toString());
			}
			System.exit(ERROR_CODE);
		}
		
		if(!result)
		{
		    System.err.println("Validation failed.");
			System.exit(FAILURE_CODE);
		}
		
		System.out.println("Validation succeeded.");
		System.exit(SUCCESS_CODE);		
	}
}
