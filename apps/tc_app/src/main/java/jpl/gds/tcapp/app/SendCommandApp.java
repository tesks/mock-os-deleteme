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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.options.VenueTypeOption;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;

/**
 * The command line application chill_send_cmd
 * 
 * This application is used to transmit a command or list of commands to the FSW
 * or SSE.
 * 
 *
 *         Note: Get rid of System.exit in main. We now do the shutdown in a
 *         shutdown hook, because doing it in showGui doesn't shut down all
 *         topics. But the shutdown hooks are not called on a normal exit, so we
 *         force it by calling System.exit.
 * 
 *         That's not really satisfactory, because it doesn't guarantee that
 *         everything is shut down properly.
 */
public class SendCommandApp extends AbstractUplinkApp {
    /** Short command line option for the command list file **/
    public static final String FILE_PARAM_SHORT        = "f";
    /** Short command line option for the command list file **/
    public static final String FILE_PARAM_LONG         = "file";
    /** Short command option for the do not validate commands option **/
    public static final String NO_VALIDATE_PARAM_SHORT = "x";
    /** Short command option for the do not validate commands option **/
    public static final String NO_VALIDATE_PARAM_LONG  = "noValidateCommands";


    /** The command input by the user on the command line */
    private String             commandString;
    /** The command list file input by the user on the command line */
    private String             commandFileString;

    private final FileOption   cmdFileOpt              = new FileOption(FILE_PARAM_SHORT, FILE_PARAM_LONG, "filename",
            "The input command list file.", false, true);
    private final FlagOption   validateCmdOpt          = new FlagOption(NO_VALIDATE_PARAM_SHORT, NO_VALIDATE_PARAM_LONG,
            "Do not validate the command line arguments against their allowable values in the command dictionary (validation is done by default)");
	

	/**
	 * Create an instance of SendCommandApp
	 */
	public SendCommandApp() {
		super();
		commandString = null;
		commandFileString = null;

	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	@SuppressWarnings("DM_EXIT")
    public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);

		if (autorun) {
			writeOutSessionConfig();
		}

		// check if commands should be validated against the dictionary
        cmdConfig.setValidateCommands(!validateCmdOpt.parse(commandLine));

        // parse all dictionary opts
        dictOpts.parseAllOptionsAsOptionalWithDefaults(commandLine);

        uplinkOptions.parseAllNonBasicOptionsAsOptionalWithDefault(commandLine);

		// see if the user supplied a command list file
        commandFileString = cmdFileOpt.parse(commandLine);

        final String[] leftoverArgs = commandLine.getTrailingArguments();
		// This call retrieves the part of the command line not attached to any
		// arguments. If we're reading a command list file,
		// this should be empty. Otherwise it should be a length of one. The
		// following if/else ladder checks these conditions.
		if (leftoverArgs.length > 0
                && commandFileString != null) {
			throw new ParseException(
					"Invalid arguments specified or extra characters on command line while using "
							+ SendCommandApp.FILE_PARAM_SHORT
							+ "option. "
							+ "Specifying an input file and specifying command line commands"
							+ " are mutually exclusive operations.");
		} else if (leftoverArgs.length == 0
                && commandFileString == null) {
			throw new ParseException(
					"Command information missing on command line and no input file was specified");
		} else if (leftoverArgs.length > 1) {
			final StringBuilder errorString = new StringBuilder(1024);
			errorString
					.append("Invalid arguments specified or extra characters on command line: \n");
			for (int i = 1; i < leftoverArgs.length; i++) {
				errorString.append("Extra Argument #" + (i - 1) + " = "
						+ leftoverArgs[i] + "\n");
			}
			throw new ParseException(errorString.toString());
		}

		// if we're not reading from a file, grab the command info
        if (commandFileString == null) {
			commandString = leftoverArgs[0];
		} else {
			commandString = null;
		}
		
        // Set up uplink security
        loginMethod = securityOptions.LOGIN_METHOD_NON_GUI.parseWithDefault(commandLine, false, true);
        keytabFile = securityOptions.KEYTAB_FILE.parse(commandLine); 
        
        checkLoginOptions();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + ApplicationConfiguration.getApplicationName()
                + " --"
                + VenueTypeOption.LONG_OPTION
                + " venueType --"
                + SessionCommandOptions.SESSION_NAME_LONG
                + " name <options> (--"
                + FILE_PARAM_LONG
                + " <filename> | command)\n\n"
                + "This utility can be used to send hardware or flight software commands.  Commands can either be sent\n"
                + "one at a time from this command line interface or multiple commands may be sent through a file.\n\n"
                + getCommandFormatUsage(cmdConfig));

        options.getOptions().printOptions(pw);

        pw.flush();
	}

	/**
	 * Get the usage instructions for formatting a command. Used in the help
	 * text.
	 * 
	 * @param cmdConfig the CommandProperties used by the calling class
	 * 
	 * @return A string explaining how to format a command as input to this
	 *         tool.
	 */
	public static String getCommandFormatUsage(final CommandProperties cmdConfig) {
		final StringBuilder buf = new StringBuilder(1500);

		buf.append("Individual Command Format: \n"
				+ "\t'stem,arg1_value,...,argN_value' \n\t\tOR \n\t'#opcode,arg1_value,...,argN_value'\n\t(NOTE: The entire command is enclosed in single quotes)\n"
				+ "\nArgument Value Format (within the command format): \n"
				+ "\tFill Arg Value: Fill arguments are not input (skip them)!\n"
				+ "\tNumeric Arg Value: value\n"
				+ "\tLook Arg Value: value\n"
				+ "\tString Arg Value: \"value\"\n\t(NOTE: String values should be enclosed in double quotes)\n"
				+ "\tRepeat Arg Value: #_repeats,arg1_value,...,argN_value\n\n"
				+ "Any argument value may be specified in ASCII format by simply typing its value.\n"
				+ "Any argument value may be specified  in hexadecimal instead of ASCII by \n"
				+ "preceding it with a #.  Similarly, an opcode may be specified in place of the \n"
				+ "stem  value if it is preceded with a #.  If an argument has a default value, \n"
				+ "the ASCII value \""
				+ cmdConfig.getDefaultValueString()
				+ "\" can be specified on the\ncommand line in place of an argument value.\n\n"
				+ "Using a file input, commands should be constructed as described above in the \n"
				+ "\"Individual Command Format\" and should be placed in an ASCII text file \n"
				+ "with each individual command newline delimited.  Any line in this file \n"
				+ "beginning with \""
				+ UplinkInputParser.FILE_COMMENT_PREFIX
				+ "\" will be treated as a comment.\n\n"
				+ "SSE commands: Commands can be sent to the SSE instead of the flight software\n"
				+ "if desired.  Any command that begins with the prefix \""
				+ cmdConfig.getSseCommandPrefix()
				+ "\"\nwill be interpreted as a command to be sent to the SSE and will not be \n"
				+ "parsed. SSE commands are sent straight to the SSE as an ASCII string.\n\n");

		return (buf.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public BaseCommandOptions createOptions() {
        if (options != null) {
            return options;
        }
        options = super.createOptions();

        options.addOption(cmdFileOpt);
        options.addOption(validateCmdOpt);

        options.addOptions(dictOpts.getAllOptions());

        options.addOptions(uplinkOptions.getNonBasicUplinkOptions());
        options.addOptions(securityOptions.getAllNonGuiOptions());
        
        /* Add SSE options */
        options.addOptions(connectionOpts.getAllSseUplinkOptionsNoConnectionType());

		return (options);
	}

	/**
	 * Transmit the user input command or command list
	 * 
	 * @throws CommandFileParseException If the input command list file cannot
	 *             be parsed
	 * @throws CommandParseException If a particular command is malformed
	 * @throws RawOutputException If the proper raw output adapter can't be
	 *             created
	 * @throws UplinkException If the command(s) cannot be sent over the network
	 */
	public void sendCommands() throws CommandFileParseException,
			CommandParseException, RawOutputException, UplinkException {
		if (commandString != null) {
			sendCommands(this.appContext, commandString);
		} else {
			sendCommands(this.appContext, UplinkInputParser.parseCommandListFromFile(appContext, new File(
					commandFileString)), -1);
		}
	}

	/**
	 * Send the input command
	 * 
	 * @param appContext
	 *            the ApplicationContext used by the calling class
	 * 
	 * @param command
	 *            The string of command stem and arguments to transmit (FSW or
	 *            SSE command)
	 * 
	 * @throws CommandParseException
	 *             If a particular command is malformed
	 * @throws RawOutputException
	 *             If the proper raw output adapter can't be created
	 * @throws UplinkException
	 *             If the command(s) cannot be sent over the network
	 */
	public static void sendCommands(final ApplicationContext appContext,
			final String command)
			throws CommandParseException, RawOutputException, UplinkException {
		sendCommands(appContext, command, null, null, null, null, -1);
	}

	/**
	 * Send the input command list file
	 * 
	 * @param appContext
	 *            the ApplicationContext used by the calling class
	 * 
	 * @param filename
	 *            The filename of the command list file whose contents will be
	 *            transmitted
	 * 
	 * @throws CommandFileParseException
	 *             If the input command list file cannot be parsed
	 * @throws RawOutputException
	 *             If the proper raw output adapter can't be created
	 * @throws UplinkException
	 *             If the command(s) cannot be sent over the network
	 */
	public static void sendCommandListFile(final ApplicationContext appContext,
			final String filename)
			throws CommandFileParseException, RawOutputException,
			UplinkException {
		sendCommandListFile(appContext, filename, null, null, null, null, -1);
	}

	/**
	 * Send the given command list file
	 * 
	 * @param appContext
	 *            the ApplicationContext used by the calling class
	 * @param filename
	 *            The filename of the command list file whose contents will be
	 *            transmitted
	 * @param validate
	 *            True if commands should be validated against the command
	 *            dictionary, false otherwise
	 * @param stringId
	 *            The RCE string to target onboard the spacecraft (A, B or both)
	 * @param virtualChannel
	 *            The virtual channel that these commands should be sent on
	 * @param scid
	 *            The spacecraft ID of the spacecraft to target with these
	 *            commands
	 * @param id
	 *            a unique ID to tag this command to allow retrieval of the
	 *            Command Message generated from this uplink
	 * 
	 * @throws CommandFileParseException
	 *             If the input command list file cannot be parsed
	 * @throws RawOutputException
	 *             If the proper raw output adapter can't be created
	 * @throws UplinkException
	 *             If the command(s) cannot be sent over the network
	 */
	public static void sendCommandListFile(final ApplicationContext appContext,
			final String filename,
			final Boolean validate, final String stringId,
			final Byte virtualChannel, final Short scid, final int id)
			throws CommandFileParseException, RawOutputException,
			UplinkException {
		
		final CommandProperties cmdConfig = appContext.getBean(CommandProperties.class);
		final CommandFrameProperties frameConfig = appContext.getBean(CommandFrameProperties.class);
		final IContextIdentification cid = appContext.getBean(IContextIdentification.class);
		
		final boolean oldValidate = cmdConfig.getValidateCommands();
		final ExecutionStringType oldStringId = ExecutionStringType.valueOf(frameConfig.getStringId());
		final int oldScid = cid.getSpacecraftId();

		if (validate != null) {
			cmdConfig.setValidateCommands(validate.booleanValue());
		}

		if (stringId != null) {
			frameConfig.setStringId(stringId);
		}

		if (virtualChannel != null) {
			frameConfig.setOverrideVirtualChannelNumber(virtualChannel);
		}

		if (scid != null) {
			cid.setSpacecraftId(scid.intValue());
		}

		try {
			final List<ICommand> commands = UplinkInputParser
					.parseCommandListFromFile(appContext, new File(filename));
			sendCommands(appContext, commands, id);
		} finally {
			cmdConfig.setValidateCommands(oldValidate);
			frameConfig.setStringId(oldStringId.toString());
			frameConfig.setOverrideVirtualChannelNumber(null);
			cid.setSpacecraftId(oldScid);
		}
	}

	/**
	 * Send the given command
	 * 
	 * @param appContext
	 *            the ApplicationContext used by the calling class
	 * @param command
	 *            The command, with stem and arguments, to transmit (FSW or SSE)
	 * @param validate
	 *            True if commands should be validated against the command
	 *            dictionary, false otherwise
	 * @param stringId
	 *            The RCE string to target onboard the spacecraft (A, B or both)
	 * @param virtualChannel
	 *            The virtual channel that this command should be sent on
	 * @param scid
	 *            The spacecraft ID of the spacecraft to target with this
	 *            command
	 * @param id
	 *            a unique ID to tag this command to allow retrieval of the
	 *            Command Message generated from this uplink
	 * 
	 * @throws CommandParseException
	 *             If the command is malformed
	 * @throws RawOutputException
	 *             If the proper raw output adapter can't be created
	 * @throws UplinkException
	 *             If the command(s) cannot be sent over the network
	 */
	public static void sendCommands(final ApplicationContext appContext,
			final String command,
			final Boolean validate, final String stringId,
			final Byte virtualChannel, final Short scid, final int id)
			throws RawOutputException, UplinkException, CommandParseException {
		
		final CommandProperties cmdConfig = appContext.getBean(CommandProperties.class);
		final CommandFrameProperties frameConfig = appContext.getBean(CommandFrameProperties.class);
		final IContextIdentification cid = appContext.getBean(IContextIdentification.class);
		
		final boolean oldValidate = cmdConfig
				.getValidateCommands();
		final ExecutionStringType oldStringId = ExecutionStringType.valueOf(frameConfig.getStringId());
		final int oldScid = cid.getSpacecraftId();

		if (validate != null) {
            cmdConfig.setValidateCommands(validate.booleanValue());
		}

		if (stringId != null) {
			frameConfig.setStringId(stringId);
		}

		if (virtualChannel != null) {
            frameConfig.setOverrideVirtualChannelNumber(virtualChannel);
		}

		if (scid != null) {
			cid.setSpacecraftId(scid.intValue());
		}

		try {
			if (command == null) {
                throw new CommandParseException("No input commands/arguments specified");
			}

            final List<ICommand> commands = new ArrayList<>(1);
			commands.add(UplinkInputParser.parseCommandString(appContext, command));
			sendCommands(appContext, commands, id);
		} finally {
			cmdConfig.setValidateCommands(oldValidate);
			frameConfig.setStringId(oldStringId.toString());
			frameConfig.setOverrideVirtualChannelNumber(
					null);
			cid.setSpacecraftId(oldScid);
		}
	}

	/**
	 * Send the entire list of commands. Order is preserved. The order of the
	 * list is the order in which the commands will be transmitted.
	 * 
	 * @param appContext
	 *            the ApplicationContext used by the calling class
	 * @param commands The list of commands (both FSW and SSE) to send
	 * @param id an ID associated with this uplink
	 * @throws RawOutputException If the proper raw output adapter can't be
	 *             created
	 * @throws UplinkException If the command(s) cannot be sent over the network
	 */
	public static void sendCommands(final ApplicationContext appContext, final List<ICommand> commands, final int id)
			throws UplinkException, RawOutputException {
        final UplinkConnectionType connectType = appContext.getBean(IConnectionMap.class).getUplinkConnection().getUplinkConnectionType();
		final IRawOutputAdapter output = appContext.getBean(IRawOutputAdapterFactory.class).getUplinkOutput(connectType);
		output.sendCommands(commands, id);

		// Make absolutely sure that no state is being held onto between command
		// transmissions
		// (generally was only a problem with the command builder, but this will
		// solve any problems
		// with accidentally holding onto state for all current and future
		// command interfaces)
		for (final ICommand command : commands) {
			if (command instanceof IFlightCommand) {
				((IFlightCommand) command).clearArgumentValues();
			}
		}
	}


	/**
	 * Gets the command string to be sent.
	 * 
	 * @return The single command to be sent (or null if non-existent)
	 */
	public String getCommandString() {
		return commandString;
	}

	/**
	 * Gets the path to the command file to be sent.
	 * 
	 * @return The command list file to be sent (or null if non-existent)
	 */
	public String getCommandFileString() {
		return commandFileString;
	}

	/**
	 * Add start of status publisher.
	 * 
	 * @throws AuthenticationException if Access Control is enabled and user
	 *             authentication fails
	 */
	@Override
    public void startExternalInterfaces() throws AuthenticationException {
		super.startExternalInterfaces();

		if ((sessionConfig == null)
				|| (hostConfig.getFswUplinkConnection().getUplinkConnectionType() 
						!= UplinkConnectionType.COMMAND_SERVICE)) {
			return;
		}

	}


	/**
	 * The command line interface.
	 * 
	 * @param args The command line arguments
	 */
	public static void main(final String[] args)
    {

        Shutdown<ShutdownFunctorsEnum> shutdown = null;

        int status = 0;

		try
        {
			final SendCommandApp app = new SendCommandApp();

            shutdown = app.getShutdown();
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);

			app.startExternalInterfaces();
			
			/**
			 * Enabling and loading the dictionary.
			 */
			app.loadConfiguredCommandDictionary();

			app.sendCommands();

		}
        catch (final ParseException e) {
            status = 1;
            log.error(ExceptionTools.getMessage(e));
		}
        catch (final Exception e)
        {
            status = 1;
            log.error("Exception was encountered while running "
			   	+  ApplicationConfiguration.getApplicationName() + ": "
                    + ExceptionTools.getMessage(e));

            // Log exception stack trace to file and suppress from console
            log.error(Markers.SUPPRESS, ExceptionTools.getMessage(e), e);
            
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
