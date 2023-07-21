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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import jpl.gds.tc.api.exception.*;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfFactory;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;

/**
 * The command line application chill_send_scmf.
 * 
 * This application is used to transmit an SCMF to the flight system.
 * 
 * An SCMF (a SpaceCraft Message File) is an Deep Space Network (DSN) interface
 * that contains pre-translated uplink ready for transmission to a flight
 * system.
 * 
 */
public class SendScmfApp extends AbstractUplinkApp {
	/**
	 * The user input SCMF file.
	 */
	protected File scmfFile;

	/** Short command option to display checksum checks on the SCMF file. **/
	public static final String DISABLE_CHECKS_PARAM_SHORT = "x";
	/** Long command option to display checksum checks on the SCMF file. **/
	public static final String DISABLE_CHECKS_PARAM_LONG = "disableChecks";

    private final FlagOption   disableCheckOpt            = new FlagOption(DISABLE_CHECKS_PARAM_SHORT,
            DISABLE_CHECKS_PARAM_LONG, "Do not validate that any of the checksums in the SCMF file are correct.  "
                    + "Do not check that the FSW version matches the version inside the SCMF.");

	/**
	 * Create a new Send SCMF Application instance.
	 */
	public SendScmfApp() {
		super();

		scmfFile = null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
    public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);

        appContext.getBean(ScmfProperties.class).setDisableChecksums(disableCheckOpt.parse(commandLine));

		parseScmfFilename(commandLine);
		
	     // Set up uplink security
        loginMethod = securityOptions.LOGIN_METHOD_NON_GUI.parseWithDefault(commandLine, false, true);
        keytabFile = securityOptions.KEYTAB_FILE.parse(commandLine);
        
        checkLoginOptions();

	}

	/**
	 * Parse the command line input SCMF file data
	 * 
	 * @param commandLine The Apache CLI Command Line instance
	 * 
	 * @throws ParseException If there's a problem with the user's input SCMF
	 */
    protected void parseScmfFilename(final ICommandLine commandLine)
			throws ParseException {
		// The SCMF filename should be the only arg on the command line not
		// associated with a
		// - or -- option

        final String[] leftoverArgs = commandLine.getTrailingArguments();
		if (!appContext.getBean(CommandProperties.class).getShowGui()) {
			if (leftoverArgs.length == 0) {
				throw new MissingArgumentException(
						"No input SCMF file found on the command line");
			} else if (leftoverArgs.length > 1) {
				throw new ParseException(
						"Only expected to find one command line value (the SCMF file name), but "
								+ leftoverArgs.length
								+ " values were found.  The found values are: "
								+ Arrays.toString(leftoverArgs));
			}
		}

		if (leftoverArgs.length == 1) {
			// I hear having a file that exists is helpful too
			scmfFile = new File(leftoverArgs[0]);
            if (!scmfFile.exists()) {
				throw new ParseException("The input SCMF file "
						+ leftoverArgs[0] + " does not exist.");
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
        pw.print("Usage: " + ApplicationConfiguration.getApplicationName() + " <options> scmf_file_name\n\n"
                + "\tscmf_file_name - The SCMF file whose contents will be parsed and sent.\n\n");

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

        options.addOption(disableCheckOpt);
        options.addOptions(dictOpts.getFswOptions());
        options.addOptions(securityOptions.getAllNonGuiOptions());

        return options;
	}

	/**
	 * Transmit the stored SCMF
	 * 
	 * @throws ScmfParseException If the user input SCMF can't be parsed
	 * @throws RawOutputException If the output adapter can't be properly loaded
	 * @throws UplinkException If the SCMF cannot be transmitted over the
	 *             network
	 * @throws ScmfWrapUnwrapException If the SCMF has a formatting error
	 * @throws IOException If the SCMF cannot be read properly
	 * @throws InvalidMetadataException 
	 */
	private void sendScmf() throws ScmfParseException, RawOutputException,
			UplinkException, ScmfWrapUnwrapException,
			IOException, InvalidMetadataException {
		sendScmf(appContext, scmfFile, -1);
	}

	/**
	 * Send the given SCMF file
	 * 
	 * @param appContext the ApplicationContext in which this object is
	 *            being used
	 * @param scmfFile The path to the SCMF file to transmit
	 * @param disableChecks True if checksums should be disabled, false
	 *            otherwise
	 * @param id the ID associated wit this uplink
	 * 
	 * @throws ScmfParseException If the user input SCMF can't be parsed
	 * @throws RawOutputException If the output adapter can't be properly loaded
	 * @throws UplinkException If the SCMF cannot be transmitted over the
	 *             network
	 * @throws DictionaryException If the command dictionary cannot be parsed
	 * @throws ScmfWrapUnwrapException If the SCMF has a formatting error
	 * @throws IOException If the SCMF cannot be read properly
	 * @throws InvalidMetadataException 
	 */
	public static void sendScmf(final ApplicationContext appContext, final String scmfFile,
			final Boolean disableChecks, final int id) throws UplinkException,
			RawOutputException, ScmfParseException, DictionaryException,
            ScmfWrapUnwrapException, IOException {
		sendScmf(appContext, new File(scmfFile), disableChecks, id);
	}

	/**
	 * Send the given SCMF file
	 * 
	 * @param appContext the ApplicationContext in which this object is
	 *            being used
	 * @param scmfFile The file pointer to the SCMF to transmit
	 * @param disableChecks True if checksums should be disabled, false
	 *            otherwise
	 * @param id the ID of the associated uplink
	 * 
	 * @throws ScmfParseException If the user input SCMF can't be parsed
	 * @throws RawOutputException If the output adapter can't be properly loaded
	 * @throws UplinkException If the SCMF cannot be transmitted over the
	 *             network
	 * @throws ScmfWrapUnwrapException If the SCMF has a formatting error
	 * @throws IOException If the SCMF cannot be read properly 
	 */
	public static void sendScmf(final ApplicationContext appContext,
			final File scmfFile,
			final Boolean disableChecks, final int id) throws UplinkException,
			RawOutputException, ScmfParseException,
            ScmfWrapUnwrapException, IOException {
		// temporarily use the input checksum setting
		final boolean oldDisableChecks = appContext.getBean(ScmfProperties.class)
				.isDisableChecksums();
		if (disableChecks != null) {
			appContext.getBean(ScmfProperties.class).setDisableChecksums(
					disableChecks.booleanValue());
		}

		try {
			sendScmf(appContext, scmfFile, id);
		} finally {
			// reset the checksum setting
			appContext.getBean(ScmfProperties.class).setDisableChecksums(oldDisableChecks);
		}
	}

	/**
	 * Transmit the given SCMF
	 * 
	 * @param scmfFile The file pointer to the SCMF to transmit
	 * 
	 * @throws ScmfParseException If the user input SCMF can't be parsed
	 * @throws RawOutputException If the output adapter can't be properly loaded
	 * @throws UplinkException If the SCMF cannot be transmitted over the
	 *             network
	 * @throws ScmfWrapUnwrapException If the SCMF has a formatting error
	 * @throws IOException If the SCMF cannot be read properly
	 * @throws InvalidMetadataException 
	 */
	private static void sendScmf(final ApplicationContext appContext, 
			final File scmfFile, final int id)
			throws UplinkException, ScmfParseException, RawOutputException,
            ScmfWrapUnwrapException, IOException {
		// get the necessary output adapter
		final UplinkConnectionType connectType = appContext.getBean(IConnectionMap.class).
				getFswUplinkConnection().getUplinkConnectionType();
		final IRawOutputAdapter output = appContext.getBean(IRawOutputAdapterFactory.class).getUplinkOutput(connectType);

		// parse the input SCMF file
		final IScmf scmf = appContext.getBean(IScmfFactory.class).parse(scmfFile);

		final boolean validateCmdDict = appContext.getBean(ScmfProperties.class).isDictionaryValidation();

		if (validateCmdDict) {
			// if validation is turned on, validate the command dictionary
			// version from the SCMF file (the command dictionary that was used
			// to generate
			// the SCMF) against the current command dictionary version
			final String scmfVersionId = scmf.getCommentField().trim();
			
		      /*
             * Technically only MSL-formatted
             * dictionaries are required to have a build or release version ID.
             * I have modified the CommandDefinitionTable to return the GDS
             * version ID for build and release IDs if they are null. This used
             * to be the other way around, basically. 
             * 
             * In general, I think only MSL-heritage mission use this validation
             * capability anyway.
             */
			
			final String dictVersionId = appContext.getBean(ICommandDefinitionProvider.class)
					.getBuildVersionId();
			final String defaultComment = appContext.getBean(ScmfProperties.class).getComment();
			
			if (!(scmfVersionId.equalsIgnoreCase(dictVersionId) || scmfVersionId.equals(defaultComment))
					&& !appContext.getBean(ScmfProperties.class).isDisableChecksums()) {
				throw new ScmfVersionMismatchException(
						"The FSW version ID \""
								+ scmfVersionId
								+ "\" ("
								+ scmf.getMacroVersion()
								+ ") in the input SCMF does not match the current command dictionary FSW version ID \""
								+ dictVersionId
								+ "\" ("
								+ appContext.getBean(DictionaryProperties.class).getFswVersion()
								+ ").  If you want to transmit this SCMF anyway, you'll need to disable SCMF validity checks.");
			}
		}

		output.sendScmf(scmf, id);
	}

	/**
	 * Gets the file object for the input SCMF file.
	 * 
	 * @return The file pointer to the user input SCMF file.
	 */
	public File getScmfFile() {
		return scmfFile;
	}

	/**
	 * Sets the file object for the input SCMF file.
	 * 
	 * @param scmfFile The file pointer to the user input SCMF file.
	 */
	public void setScmfFile(final File scmfFile) {
		this.scmfFile = scmfFile;
	}


    /**
     * Command line interface
     *
     * @param args The user input command line arguments
     */
    public static void main(final String[] args)
    {

        Shutdown<ShutdownFunctorsEnum> shutdown = null;

        int status = 0;

        try
        {
            final SendScmfApp app = new SendScmfApp();

            shutdown = app.getShutdown();

            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);

            app.startExternalInterfaces();
            
			/**
			 * Enabling and loading the dictionary.
			 */
			app.loadConfiguredCommandDictionary();

            app.sendScmf();
        }
        catch (final ParseException e)
        {
            status = 1;
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
        }
        catch (final Exception e)
        {
        	/*
			 * The exception caught here is
			 * most likely not the original exception thrown, but rather wraps
			 * around it. For example, when UnknownHostException is thrown up in
			 * the chain because of a bad host name (thus a socket error),
			 * printing this exception's getMessage() or toString() will simply
			 * print the bad host's name but what the error actually is. We
			 * solve this by looking for the causing exception, and outputting
			 * what that causing exception is. This results in the actual
			 * exception and the parameter that caused it to be printed.
			 */
            status = 1;
            TraceManager.getDefaultTracer().error("Exception was encountered while running " + ApplicationConfiguration.getApplicationName()
                            + ": " + (e.getCause() != null ? e.getCause() : ExceptionTools.getMessage(e)));

            // Log exception stack trace to file and suppress from console
            TraceManager.getDefaultTracer().error(Markers.SUPPRESS, ExceptionTools.getMessage(e), e);
        }
        finally
        {
            if (shutdown != null) {
                shutdown.shutdown();
            }

            System.exit(status);
        }
    }
}
