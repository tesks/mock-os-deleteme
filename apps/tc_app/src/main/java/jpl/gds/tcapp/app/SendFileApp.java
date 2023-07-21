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

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.IFileLoadInfo;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.FileLoadParseException;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;

/**
 * The command line application chill_send_file.
 * 
 * This application is used to transmit a file load to the flight system.
 * 
 * Transmission of a file load is akin to FTP for spacecraft. The objective is
 * to take a file on the local machine, package it up, transmit it to the
 * spacecraft and have the exact same file we started with end up in a specified
 * local on the flight computer filesystem.
 * 
 *
 */
public class SendFileApp extends AbstractUplinkApp {
	/** Short command line option to set the file overwrite flag **/
	public static final String OVERWRITE_PARAM_SHORT = "x";
	/** Long command line option to set the file overwrite flag **/
	public static final String OVERWRITE_PARAM_LONG = "overwrite";

    private final FlagOption   overwriteOpt          = new FlagOption(OVERWRITE_PARAM_SHORT, OVERWRITE_PARAM_LONG,
            "If this argument is present on the"
                    + " command line, all sent files will have the overwrite flag set to \"true\" in their file load header.  "
                    + "By default, when this argument is not present, the overwrite flag will be set to \"false\"");

	/**
	 * True if this file load should overwrite one of the same name that already
	 * exists on the destination filesystem, false otherwise
	 */
	private boolean overwrite;
	/** An array of input file load (type, source, target) triples from the user */
	private String[] fileLoadStrings;

	/**
	 * Creates an instance of SendFileApp.
	 */
	public SendFileApp() {
		super();

		fileLoadStrings = null;
		overwrite = false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
    public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);

        overwrite = overwriteOpt.parse(commandLine);
        uplinkOptions.parseAllNonBasicOptionsAsOptionalWithDefault(commandLine);
		// the non-argument part of the command line should be
		// all the input files
        fileLoadStrings = commandLine.getTrailingArguments();
		if (fileLoadStrings.length == 0) {
			throw new ParseException("No files specified on the command line");
		}
		
	     // Set up uplink security
        loginMethod = securityOptions.LOGIN_METHOD_NON_GUI.parseWithDefault(commandLine, false, true);
        keytabFile = securityOptions.KEYTAB_FILE.parse(commandLine);
        
        checkLoginOptions();

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
        pw.print("Usage: " + ApplicationConfiguration.getApplicationName()
                + "  <options> {file_type1,}input_filename1,targetfile_name1 ... {file_typeN,}input_filenameN,target_filenameN\n\n"
                + "\tfile_type - This value is\n"
                + "\t            optional and will default to \"0\" if omitted.  Any value that can be represented\n"
                + "\t            by 7 bits will be accepted as input.\n"
                + "\tinput_file_name - The name (including path) of the file to send.\n"
                + "\ttarget_file_name - The target file name on the spacecraft file system where this\n"
                + "\t                   file will be placed.\n\n"
                + "Multiple files may be sent from the same command line.  A file to send should be specified in a \n"
                + "comma-separated triple of the form file_type,input_file_name,target_file_name or a comma-separated\n"
                + "double of the form input_file_name,target_file_name. If a double is specified, the file type will\n"
                + "default to \"0\" (see below). Multiple doubles and/or triples can be specified on the same command\n"
                + "line and should be separated by whitespace.\n");

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

        options.addOption(overwriteOpt);

        options.addOptions(uplinkOptions.getNonBasicUplinkOptions());
        options.addOptions(securityOptions.getAllNonGuiOptions());

		return (options);
	}

	/**
	 * Transmit the user input file loads to the flight system.
	 * 
	 * @throws FileLoadParseException If the input files cannot be parsed and
	 *             packaged properly
	 * @throws RawOutputException If the correct raw output adapter cannot be
	 *             created
	 * @throws UplinkException If there's a network transmission error
	 */
	public void sendFileLoads() throws FileLoadParseException,
			RawOutputException, UplinkException {
		sendFileLoads(appContext, UplinkInputParser.createFileLoadsFromCommandLine(
				appContext, fileLoadStrings, overwrite), -1);
	}

	/**
	 * Transmit the given file load to the flight system
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 * @param type The file type (e.g. SEQUENCE)
	 * @param source The path to the file on the local machine
	 * @param target The target location of the file on the flight computer
	 *            filesystem
	 * @param overwrite True if this file should overwrite an existing file,
	 *            false otherwise
	 * @param stringId The RCE String to target on the spacecraft (generally A,
	 *            B or Both)
	 * @param virtualChannel The virtual channel to send these file loads on
	 *            (usually VC-2)
	 * @param scid The spacecraft ID to target with these file loads
	 * @param id a unique ID to tag this command to allow retrieval of the
	 *            Command Message generated from this uplink
	 * 
	 * @throws FileLoadParseException If the input files cannot be parsed and
	 *             packaged properly
	 * @throws RawOutputException If the correct raw output adapter cannot be
	 *             created
	 * @throws UplinkException If there's a network transmission error
	 */
	public static void sendFileLoads(final ApplicationContext appContext,
			final String type, final String source,
			final String target, final Boolean overwrite,
			final String stringId, final Byte virtualChannel, final Short scid,
			final int id) throws FileLoadParseException, RawOutputException,
			UplinkException {
		final int oldScid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
		final ExecutionStringType oldStringId = ExecutionStringType.valueOf(appContext.getBean(CommandFrameProperties.class).getStringId());

		if (stringId != null) {
			appContext.getBean(CommandFrameProperties.class).setStringId(stringId);
		}

		if (virtualChannel != null) {
			appContext.getBean(CommandFrameProperties.class).setOverrideVirtualChannelNumber(
					virtualChannel);
		}

		if (scid != null) {
			appContext.getBean(IContextIdentification.class).setSpacecraftId(scid.intValue());
		}

		try {
			final IFileLoadInfo fli = appContext.getBean(ICommandObjectFactory.class).createFileLoadInfo();
			fli.setFileType(type);
			fli.setInputFilePath(source);
			fli.setTargetFilePath(target);
			if (overwrite != null) {
				fli.setOverwrite(overwrite.booleanValue());
			}

			sendFileLoads(appContext, UplinkInputParser.createFileLoadsFromInfo(appContext, fli), id);
		} finally {
			appContext.getBean(CommandFrameProperties.class).setStringId(oldStringId.toString());
			appContext.getBean(CommandFrameProperties.class).setOverrideVirtualChannelNumber(
					null);
			appContext.getBean(IContextIdentification.class).setSpacecraftId(oldScid);
		}
	}

	/**
	 * Transmit the given list of file loads to the flight system
	 * 
	 * @param appContext the ApplicationContext in which this object is being used
	 * @param fileLoads The list of file loads to send
	 * @param id An ID associated with this uplink
	 * 
	 * @throws RawOutputException If the correct raw output adapter cannot be
	 *             created
	 * @throws UplinkException If there's a network transmission error
	 */
	public static void sendFileLoads(final ApplicationContext appContext,
			final List<ICommandFileLoad> fileLoads,
			final int id) throws UplinkException, RawOutputException {
		final UplinkConnectionType connectType = appContext.getBean(IConnectionMap.class).
				getFswUplinkConnection().getUplinkConnectionType();
		final IRawOutputAdapter output = appContext.getBean(IRawOutputAdapterFactory.class).getUplinkOutput(connectType);
		output.sendFileLoads(fileLoads, id);
	}

	/**
	 * Gets the value of the file overwrite flag.
	 * 
	 * @return True if new file loads should overwrite existing ones, false
	 *         otherwise
	 */
	public boolean isOverwrite() {
		return overwrite;
	}


    /**
     * Command line interface
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args)
    {
        Shutdown<ShutdownFunctorsEnum> shutdown = null;

        int status = 0;

        try
        {
            final SendFileApp app = new SendFileApp();

            shutdown = app.getShutdown();

            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);

            app.startExternalInterfaces();

            app.sendFileLoads();

        }
        catch (final ParseException pe)
        {
            status = 1;
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(pe));
        }
        catch (final Exception e)
        {
            status = 1;
            TraceManager.getDefaultTracer()
                        .error("Exception was encountered while running ",
                               ApplicationConfiguration.getApplicationName(), ": ", ExceptionTools.getMessage(e));

            // Log exception stack trace to file and suppress from console
            TraceManager.getDefaultTracer().error(Markers.SUPPRESS, "Exception was encountered while running ",
                                                  ApplicationConfiguration.getApplicationName(), ": ",
                                                  ExceptionTools.getMessage(e), e);

            if (RuntimeException.class.isInstance(e))
            {
                throw RuntimeException.class.cast(e);
            }
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
