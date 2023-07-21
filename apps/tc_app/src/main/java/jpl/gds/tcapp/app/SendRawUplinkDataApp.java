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
import java.util.Arrays;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;

/**
 * The command line application chill_send_raw_data.
 * 
 * This application is used to transmit a raw data file to the flight system.
 * 
 * A raw data file is a binary file containing a set of bits to be transmitted
 * as-is to the flight system.
 * 
 *
 */
public class SendRawUplinkDataApp extends AbstractUplinkApp {
	/** A pointer to the user input data file */
	protected File dataFile;

	/**
	 * True if the input file is full of ASCII hex character, false if it's pure
	 * binary
	 */
	protected boolean hexFile;

	/** Short command option for interpreting input file as hex characters **/
	public static final String HEX_FILE_PARAM_SHORT = "x";
	/** Long command option for interpreting input file as hex characters **/
	public static final String HEX_FILE_PARAM_LONG = "hexFile";

    private final FlagOption   hexOpt = new FlagOption(HEX_FILE_PARAM_SHORT, HEX_FILE_PARAM_LONG,
            "Interpret the values in the input file as hex characters rather than raw bytes.");

	/**
	 * Creates an instance of SendRawUplinkDataApp.
	 */
	public SendRawUplinkDataApp() {
		super();

        hexFile = false;
    }


	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
    public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);

        hexFile = hexOpt.parse(commandLine);
        
        parseRawFilename(commandLine);

        uplinkOptions.UPLINK_ONLY_WRITE_SCMF_PARAM.parse(commandLine);
        uplinkOptions.UPLINK_SCMF_PARAM.parse(commandLine);

        // Set up uplink security
        loginMethod = securityOptions.LOGIN_METHOD_NON_GUI.parseWithDefault(commandLine, false, true);
        keytabFile = securityOptions.KEYTAB_FILE.parse(commandLine);
        
        checkLoginOptions();
	}
	
	/**
     * Parse the command line input raw file data
     * 
     * @param commandLine The Apache CLI Command Line instance
     * 
     * @throws ParseException If there's a problem with the user's input file
     */
    protected void parseRawFilename(final ICommandLine commandLine)
            throws ParseException {
        // The raw filename should be the only arg on the command line not associated with a - or -- option

        final String[] leftoverArgs = commandLine.getTrailingArguments();
        if (!appContext.getBean(CommandProperties.class).getShowGui()) {
            if (leftoverArgs.length == 0) {
                throw new MissingArgumentException("No input raw file found on the command line");
            } else if (leftoverArgs.length > 1) {
                throw new ParseException(
                        "Only expected to find one command line value (the raw file name), but "+ leftoverArgs.length
                        + " values were found.  The found values are: " + Arrays.toString(leftoverArgs));
            }
        }

        if (leftoverArgs.length == 1) {
            // having a valid file helps
            dataFile = new File(leftoverArgs[0]);
            if (!dataFile.exists()) {
                throw new ParseException("The input raw file " + leftoverArgs[0] + " does not exist.");
            }
        }
    }

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#showHelp()
	 */
	@Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + ApplicationConfiguration.getApplicationName() + " <options> data_file_name\n\n"
                + "\tdata_file_name - The data file whose contents will be sent as-is over the uplink socket.\n\n");

        options.getOptions().printOptions(pw);

        pw.flush();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
    public BaseCommandOptions createOptions() {
        if (options != null) {
            return options;
        }
        options = super.createOptions();

        options.addOption(hexOpt);

        options.addOption(uplinkOptions.UPLINK_ONLY_WRITE_SCMF_PARAM);
        options.addOption(uplinkOptions.UPLINK_SCMF_PARAM);
        
        options.addOptions(securityOptions.getAllNonGuiOptions());

		return (options);
	}

	/**
	 * Send the user input raw data file
	 * 
	 * @throws RawOutputException If the proper output adapter cannot be
	 *             retrieved
	 * @throws UplinkException If the raw data file cannot be transmitted
	 */
	public void sendRawUplinkData() throws RawOutputException, UplinkException {
		sendRawUplinkData(appContext, dataFile, hexFile, false, -1);
	}

	/**
	 * Send the given raw data file
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 * @param fileToSend The path to the file to be transmitted
	 * @param isHex True if the input file contains ASCII hex characters, false
	 *            if it's pure binary
	 * @param id an ID associated with this uplink
	 * 
	 * @throws RawOutputException If the proper output adapter cannot be
	 *             retrieved
	 * @throws UplinkException If the raw data file cannot be transmitted
	 */
	public static void sendRawUplinkData(final ApplicationContext appContext,
			final String fileToSend,
			final boolean isHex, final int id) throws UplinkException,
			RawOutputException {
		sendRawUplinkData(appContext, new File(fileToSend), isHex, false, id);
	}

	/**
	 * Send the given raw data file
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 * @param fileToSend A pointer to the file to be transmitted
	 * @param isHex True if the input file contains ASCII hex characters, false
	 *            if it's pure binary
	 * @param isFaultInjected if the raw data contains deliberate faults (came
	 *            from fault injector)
	 * @param id an ID associated with this uplink
	 * @throws RawOutputException If the proper output adapter cannot be
	 *             retrieved
	 * @throws UplinkException If the raw data file cannot be transmitted
	 */
	public static void sendRawUplinkData(final ApplicationContext appContext,
			final File fileToSend,
			final boolean isHex, final boolean isFaultInjected, final int id)
			throws UplinkException, RawOutputException {
		final UplinkConnectionType connectType = appContext.getBean(IConnectionMap.class).
				getFswUplinkConnection().getUplinkConnectionType();
		final IRawOutputAdapter output = appContext.getBean(IRawOutputAdapterFactory.class).getUplinkOutput(connectType);
		output.sendRawUplinkData(fileToSend, isHex, isFaultInjected, id);
	}

	/**
	 * Gets the file object for the input data file.
	 * 
	 * @return The pointer to the data file to be transmitted
	 */
	public File getDataFile() {
		return dataFile;
	}

	/**
	 * Gets the flag indicating input file is hex or binary.
	 * 
	 * @return True if the input file contains ASCII hex characters, false if
	 *         it's pure binary
	 */
	public boolean isHexFile() {
		return hexFile;
	}


    /**
     * Command line interface method.
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args)
    {

        Shutdown<ShutdownFunctorsEnum> shutdown = null;

        int status = 0;

        try
        {
            final SendRawUplinkDataApp app = new SendRawUplinkDataApp();

            shutdown = app.getShutdown();

            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);

            app.startExternalInterfaces();

            app.sendRawUplinkData();
        }
        catch (final ParseException pe)
        {
            status = 1;
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(pe));
        }
        // Log Spring startup messages like class not found
        catch (RawOutputException e){
	        status = 1;
	        ExceptionTools.handleSpringBootStartupError(e);
        }
        catch (final Exception e)
        {
            status = 1;
            TraceManager.getDefaultTracer().error("Exception was encountered while running "
                                                  , ApplicationConfiguration.getApplicationName(), ": ",
                                                  ExceptionTools.getMessage(e));

            // Log exception stack trace to file and suppress from console
            TraceManager.getDefaultTracer().error(Markers.SUPPRESS, "Exception was encountered while running ",
                                                  ApplicationConfiguration.getApplicationName(), ": ",
                                                  ExceptionTools.getMessage(e), e);
        }
        finally
        {
            if (shutdown != null) {
                shutdown.shutdown();
            }

            // Give a chance to Db appender to add logs in case of an error
	        try {
		        SleepUtilities.fullSleep(1000);
	        } catch (final ExcessiveInterruptException e) {
	        	//nothing to do
	        }
            System.exit(status);
        }
    }
}
