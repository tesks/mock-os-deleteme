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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;

import jpl.gds.automation.common.UplinkRequestManager;
import jpl.gds.automation.config.AutomationAppProperties;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.icmd.exception.ICmdErrorManager;
import jpl.gds.tc.api.icmd.exception.ICmdException;
import jpl.gds.tcapp.app.AbstractUplinkApp;
import jpl.gds.tcapp.app.SendCommandApp;
import jpl.gds.tcapp.app.SendFileApp;
import jpl.gds.tcapp.app.SendRawUplinkDataApp;
import jpl.gds.tcapp.app.SendScmfApp;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

/**
 * The Mtak Uplink server application
 */
public class MtakUplinkServerApp extends AbstractUplinkApp implements Runnable, IQuitSignalHandler {
    /**
     * 0 to this number will define the size of the random number pool that
     * prefixes for IDs to use for "waitForRadiation" will come from. Should be
     * large enough so that identical SCMFs sent within a millisecond of each
     * other will still have unique IDs for the duration that they are in the
     * CPD radiation queue.
     */
    private static final int MAX_RANDOM_ID_PREFIX = 99999;
    
    private final Tracer         log;


	private static final String SUCCESS = "0";
	private static final String FAILURE = "1";
    private static final long    INPUT_POLLING_DELAY_MS = 100;


	private final String delimiter;
	private final String terminator;
	private final String cmdPrefix;
	private final String cmdListPrefix;
	private final String filePrefix;
	private final String scmfPrefix;
	private final String rawFilePrefix;
	private final String logPrefix;
	private final String uplinkRatePrefix;
	private int shutdownDelay;

	private Thread runnerThread;
	private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
	private final BufferedReader br;
	private final Random generator;
	private UplinkRequestManager requestManager;



	/**
	 * Default constructor
	 */
	public MtakUplinkServerApp() {
		super();
		
        final AutomationAppProperties props = appContext.getBean(AutomationAppProperties.class);
		
		delimiter = props.getMtakDelimiter();
		terminator = props.getMtakTerminator();
		cmdPrefix = props.getMtakCommandPrefix();
		cmdListPrefix = props.getMtakCommandListPrefix();
		filePrefix = props.getMtakFilePrefix();
		scmfPrefix = props.getMtakScmfPrefix();
		rawFilePrefix = props.getMtakRawFilePrefix();
		logPrefix = props.getMtakLogPrefix();
		uplinkRatePrefix = props.getMtakUplinkRatePrefix();
		shutdownDelay = props.getShutdownDelay();

		runnerThread = null;
		appContext.getBean(ScmfProperties.class).setWriteScmf(false);
		br = new BufferedReader(new InputStreamReader(System.in));
		generator = new Random();
        log = TraceManager.getTracer(appContext, Loggers.AUTO_UPLINK);
		

	}
    @Override
    public BaseCommandOptions createOptions() {
        if (options != null) {
            return options;
        }
        options = super.createOptions();
        options.addOptions(securityOptions.getAllNonGuiOptions());
        options.addOptions(connectionOpts.getAllSseUplinkOptionsNoConnectionType());


		return (options);
	}
	
    @Override
    public void exitCleanly() {

        shutdownRequested.set(true);

        if (runnerThread != null) {
			SleepUtilities.checkedJoin(runnerThread, shutdownDelay);
		}

        try {
            if (br != null) {
                br.close();
            }
        } catch (final IOException ioe) {
            // don't care
        }

        context.unsubscribeAll();
        stopExternalInterfaces();

    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);
        
        // Set up uplink security
        loginMethod = securityOptions.LOGIN_METHOD_NON_GUI.parseWithDefault(commandLine, false, true);
        keytabFile = securityOptions.KEYTAB_FILE.parse(commandLine);
        
        checkLoginOptions();

    }

	@Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + ApplicationConfiguration.getApplicationName() + " <options>\n\n"
                + "This utility reads a newline separated list of transmissions from STDIN.\n\n");

        options.getOptions().printOptions(pw);

        pw.flush();
	}

	@Override
	protected void createOutputDirectory() {
		// MTAK should not create this directory...it should already exist
	}

	@Override
	protected void writeOutSessionConfig() {
		// MTAK should not write out the session configuration file...it should
		// already exist
	}

	/**
	 * Start up the Mtak uplink server
	 * 
	 */
    public void startup() {
		// parse the command dictionary up front so we don't have to do it again
        TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.ERROR);
		/*
		 * Add try/finally block because if an exception gets thrown,
		 * we want the log to get re-enabled so the Exception can be logged.  No catch needed, just let the exception
		 * be passed up
		 */
		try {
			appContext.getBean(ICommandDefinitionProvider.class);
		} finally {
            // TODO: Setting the level like this prevents debugging mtak. Not
            // important right now since there's no debug messages
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.INFO);
		}

		// UplinkRequestManger requires a session
		// config object, so create it after the app is configured (command-line
		// options parsed).
		this.requestManager = new UplinkRequestManager();
				
		runnerThread = new Thread(this, ApplicationConfiguration.getApplicationName() + " Command Loop");
		runnerThread.start();

		if (SleepUtilities.checkedJoin(runnerThread)) {
			// if we get interrupted, the main thread will go on
			// to exit from main after stopping the external interfaces

			log.warn("Process was interrupted. Cleaning up and terminating.");

			shutdownRequested.set(true);
		}
	}

	/**
     * Reads, parses, and sends the supplied commanding line.
     * 
     * @param line
     *            the line to be sent
     * @throws UplinkException
     *             an error was encountered while transmitting the line
     * @throws UplinkParseException
     */
    protected void readAndSendLine(final String line)
            throws UplinkException, RawOutputException, ParseException, UplinkParseException {
				
		final String[] linePieces = line.split(delimiter);
		if (linePieces.length == 0) {
			throw new IllegalArgumentException(
					"Could not find first delimiter \"" + delimiter + "\".");
		}

		final String prefix = linePieces[0];

		if (logPrefix.equals(prefix)) {
			sendLog(linePieces);
		} else if (cmdPrefix.equals(prefix)) {
			sendCommand(linePieces);
		} else if (cmdListPrefix.equals(prefix)) {
			sendCommandListFile(linePieces);
		} else if (filePrefix.equals(prefix)) {
			sendFileLoad(linePieces);
		} else if (scmfPrefix.equals(prefix)) {
			sendScmf(linePieces);
		} else if (rawFilePrefix.equals(prefix)) {
			sendRawData(linePieces);
		} else if (uplinkRatePrefix.equals(prefix)) {
			configureUplinkRate(linePieces);
		} else {
			throw new IllegalArgumentException("Unrecognized prefix \""
					+ prefix + "\"");
		}

		System.err.println("Success=" + SUCCESS);
	}

	
	/**
	 * Transmit the supplied log line
	 * 
	 * @param linePieces
	 *            the pieces of the parsed line to be sent
	 */
	protected void sendLog(final String[] linePieces) {
		/*
		 * A normal log string from MTAK looks like this: LOG,<level>,<message>
		 * NOTE: The <message> can contain commas of its own
		 */

		if (linePieces.length < 3) {
			throw new IllegalArgumentException("Not enough \"" + delimiter
					+ "\" delimiter characters found in log string.");
		}

		// piece back together <message> from its comma-delimited pieces...
		// this will be necessary for any command that has arguments or any SSE
		// command
		// that contains commas
		// (NOTE: If the last character is a comma, we'll lose it, but I don't
		// think it's worth handling) (brn)
		final StringBuilder messagePieces = new StringBuilder(1024);
		messagePieces.append(linePieces[2]);
		for (int i = 3; i < linePieces.length; i++) {
			messagePieces.append(delimiter);
			messagePieces.append(linePieces[i]);
		}
		final String message = messagePieces.toString();

		TraceSeverity severity = null;
		final String level = linePieces[1].trim();
		if (!level.isEmpty()) {
			severity = TraceManager.mapMtakLevel(level);
		} else {
			severity = TraceSeverity.INFO;
		}

		if (severity == null) {
			throw new IllegalArgumentException(
					"Invalid input MTAK logging level \"" + level + "\".");
		}

		final MtakLogMessage lm = new MtakLogMessage(severity, message);
		context.publish(lm);
        log.log(lm);
	}

	/**
	 * Transmit the supplied command line
	 * 
	 * @param linePieces
	 *            the pieces of the parsed line to be sent
	 * @throws UplinkException
	 *             an error was encountered with sending the line
	 */
	protected void sendCommand(final String[] linePieces)
            throws UplinkException, ICmdException {
		/*
		 * A normal command string from MTAK looks like this:
		 * CMD,<validate_flag>
		 * ,<string_id>,<virtual_channel>,<scid>,<actual_command > NOTE: The
		 * <actual_command> will most likely contain commas of its own
		 */

		if (linePieces.length < 7) {
			throw new IllegalArgumentException("Not enough \"" + delimiter
					+ "\" delimiter characters found in command string.");
		}

		// piece back together <actual_command> from its comma-delimited
		// pieces...
		// this will be necessary for any command that has arguments or any SSE
		// command
		// that contains commas
		final StringBuilder commandPieces = new StringBuilder(1024);
		commandPieces.append(linePieces[6]);
		for (int i = 7; i < linePieces.length; i++) {
			commandPieces.append(delimiter);
			commandPieces.append(linePieces[i]);
		}
		final String commandString = commandPieces.toString();

		// if the command is an SSE command, we don't care about the rest of the
		// parameters,
		// just send it along
		if (ISseCommand.isSseCommand(appContext.getBean(CommandProperties.class), commandString)) {
			try {
				SendCommandApp.sendCommands(appContext, commandString);
			} catch (final Exception e) {
				throw new UplinkException(e);
			}
			return;
		}

		Boolean validate = null;
		String stringId = null;
		Byte virtualChannel = null;
		Short scid = null;
		Integer waitForRadiationTimeout = null;

		// see if command validation is turned on
		final String validateString = linePieces[1].trim();
		if (!validateString.isEmpty()) {
			try {
				validate = Boolean.valueOf(GDR.parse_boolean(validateString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid command validation value \"" + validateString
								+ "\". Valid values are [" + Boolean.TRUE + ","
								+ Boolean.FALSE + "].", nfe);
			}
		}

		// see if a String ID has been specified
		stringId = linePieces[2].trim();
		if (!stringId.isEmpty()) {
			stringId = stringId.toUpperCase();
			try {
				ExecutionStringType.valueOf(stringId);
			} catch (final IllegalArgumentException iae) {
				throw new IllegalArgumentException(
						"Invalid input string ID value \"" + stringId
								+ "\". Valid values are: "
								+ Arrays.toString(ExecutionStringType.values()),
						iae);
			}
		} else {
			stringId = null;
		}

		// see if a Virtual Channel # has been specified
		final String virtualChannelString = linePieces[3].trim();
		if (!virtualChannelString.isEmpty()) {
			try {
				virtualChannel = Byte.valueOf(GDR
						.parse_byte(virtualChannelString));
				if (virtualChannel < 0) {
					throw new NumberFormatException(
							"Virtual channel cannot be negative.");
				}
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid virtual channel number \""
								+ virtualChannelString + "\".", nfe);
			}
		}

		// see if a spacecraft ID has been specified
		final String scidString = linePieces[4].trim();
		if (!scidString.isEmpty()) {
			try {
				scid = Short.valueOf(GDR.parse_short(scidString));
				if (scid < 0) {
					throw new NumberFormatException(
							"Spacecraft ID cannot be negative.");
				}
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalid spacecraft ID \""
						+ scidString + "\".", nfe);
			}
		}

		// see if we should wait for radiation
		final String waitForRadiationString = linePieces[5].trim();
		if (!waitForRadiationString.isEmpty()) {
			try {
				waitForRadiationTimeout = Integer.valueOf(GDR
						.parse_int(waitForRadiationString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid wait for radiation value \""
								+ waitForRadiationString
								+ "\". Must be an integer specifying number of seconds");
			}
		}

		/*
         * Create an unique ID to identify identical commands sent within a
         * millisecond of each other
         */
		final int id = (generator.nextInt(MAX_RANDOM_ID_PREFIX) + commandString + System
				.currentTimeMillis()).hashCode();

		try {
			SendCommandApp.sendCommands(appContext, commandString, validate, stringId,
					virtualChannel, scid, id);
			this.requestManager.waitForRadiation(id, waitForRadiationTimeout);
        }
        catch (final ICmdException e) {
            throw e;
        }
        catch (final UplinkException e) {
			throw e;
		} catch (final Exception e) {
			throw new UplinkException(e);
		}
	}

	/**
	 * Transmit the supplied command list line
	 * @param linePieces the pieces of the parsed line to be sent
	 * @throws UplinkException
	 *             an error was encountered with sending the line
	 */
	protected void sendCommandListFile(final String[] linePieces)
            throws UplinkException, ICmdException {
		/*
		 * A normal command string from MTAK looks like this:
		 * CMDLIST,<validate_flag
		 * >,<string_id>,<virtual_channel>,<scid>,<filename >
		 */

		if (linePieces.length != 7) {
			throw new IllegalArgumentException("Not enough \"" + delimiter
					+ "\" delimiter characters found in command string.");
		}

		Boolean validate = null;
		String stringId = null;
		Byte virtualChannel = null;
		Short scid = null;
		Integer waitForRadiationTimeout = null;

		// see if command validation is turned on
		final String validateString = linePieces[1].trim();
		if (!validateString.isEmpty()) {
			try {
				validate = Boolean.valueOf(GDR.parse_boolean(validateString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid command validation value \"" + validateString
								+ "\". Valid values are [" + Boolean.TRUE + ","
								+ Boolean.FALSE + "].", nfe);
			}
		}

		// see if a String ID has been specified
		stringId = linePieces[2].trim();
		if (!stringId.isEmpty()) {
			stringId = stringId.toUpperCase();
			try {
				ExecutionStringType.valueOf(stringId);
			} catch (final IllegalArgumentException iae) {
				throw new IllegalArgumentException(
						"Invalid input string ID value \"" + stringId
								+ "\". Valid values are: "
								+ Arrays.toString(ExecutionStringType.values()),
						iae);
			}
		} else {
			stringId = null;
		}

		// see if a Virtual Channel # has been specified
		final String virtualChannelString = linePieces[3].trim();
		if (!virtualChannelString.isEmpty()) {
			try {
				virtualChannel = Byte.valueOf(GDR
						.parse_byte(virtualChannelString));
				if (virtualChannel < 0) {
					throw new NumberFormatException(
							"Virtual channel cannot be negative.");
				}
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid virtual channel number \""
								+ virtualChannelString + "\".", nfe);
			}
		}

		// see if a spacecraft ID has been specified
		final String scidString = linePieces[4].trim();
		if (!scidString.isEmpty()) {
			try {
				scid = Short.valueOf(GDR.parse_short(scidString));
				if (scid < 0) {
					throw new NumberFormatException(
							"Spacecraft ID cannot be negative.");
				}
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalid spacecraft ID \""
						+ scidString + "\".", nfe);
			}
		}

		final String filename = linePieces[5];

		// see if we should wait for radiation
		final String waitForRadiationString = linePieces[6].trim();
		if (!waitForRadiationString.isEmpty()) {
			try {
				waitForRadiationTimeout = Integer.valueOf(GDR
						.parse_int(waitForRadiationString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid wait for radiation value \""
								+ waitForRadiationString
								+ "\". Must be an integer specifying number of seconds");
			}
		}

		/*
         * Create an unique ID to identify command lists sent within a
         * millisecond of each other
         */
		final int id = (generator.nextInt(MAX_RANDOM_ID_PREFIX) + filename + System.currentTimeMillis())
				.hashCode();

		try {
			SendCommandApp.sendCommandListFile(appContext, filename, validate, stringId,
					virtualChannel, scid, id);
			this.requestManager.waitForRadiation(id, waitForRadiationTimeout);
        }
        catch (final ICmdException e) {
            throw e;
        }
        catch (final UplinkException e) {
			throw e;
		} catch (final Exception e) {
			throw new UplinkException(e);
		}
	}

	/**
	 * Transmit the supplied file line
	 * 
	 * @param linePieces
	 *            the pieces of the parsed line to be sent
	 * @throws UplinkException
	 *             an error was encountered with sending the line
	 */
	protected void sendFileLoad(final String[] linePieces)
            throws UplinkException, ICmdException {
		/**
		 * A normal file load string from MTAK looks like this:
		 * FILE,<overwrite_flag
		 * >,<string_id>,<virtual_channel>,<scid>,<source_file
		 * >,<target_file>,<file_type>
		 */

		if (linePieces.length < 9) {
			throw new IllegalArgumentException("Not enough \"" + delimiter
					+ "\" delimiter characters found in file load string.");
		}

		String type = null;
		String source = null;
		String target = null;
		Boolean overwrite = null;
		String stringId = null;
		Byte virtualChannel = null;
		Short scid = null;
		Integer waitForRadiationTimeout = null;

		source = linePieces[5].trim();
		target = linePieces[6].trim();
		type = linePieces[7].trim();

		// see if the overwrite flag is set
		final String overwriteString = linePieces[1].trim();
		if (!overwriteString.isEmpty()) {
			try {
				overwrite = Boolean.valueOf(GDR.parse_boolean(overwriteString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid overwrite flag value \"" + overwriteString
								+ "\". Valid values are [" + Boolean.TRUE + ","
								+ Boolean.FALSE + "].", nfe);
			}
		}

		// see if a String ID has been specified
		stringId = linePieces[2].trim();
		if (!stringId.isEmpty()) {
			stringId = stringId.toUpperCase();
			try {
				ExecutionStringType.valueOf(stringId);
			} catch (final IllegalArgumentException iae) {
				throw new IllegalArgumentException(
						"Invalid input string ID value \"" + stringId
								+ "\". Valid values are: "
								+ Arrays.toString(ExecutionStringType.values()),
						iae);
			}
		} else {
			stringId = null;
		}

		// see if a Virtual Channel # has been specified
		final String virtualChannelString = linePieces[3].trim();
		if (!virtualChannelString.isEmpty()) {
			try {
				virtualChannel = Byte.valueOf(GDR
						.parse_byte(virtualChannelString));
				if (virtualChannel < 0) {
					throw new NumberFormatException(
							"Virtual channel cannot be negative.");
				}
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid virtual channel number \""
								+ virtualChannelString + "\".", nfe);
			}
		}

		// see if a spacecraft ID has been specified
		final String scidString = linePieces[4].trim();
		if (!scidString.isEmpty()) {
			try {
				scid = Short.valueOf(GDR.parse_short(scidString));
				if (scid < 0) {
					throw new NumberFormatException(
							"Spacecraft ID cannot be negative.");
				}
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalid spacecraft ID \""
						+ scidString + "\".", nfe);
			}
		}

		// see if we should wait for radiation
		final String waitForRadiationString = linePieces[8].trim();
		if (!waitForRadiationString.isEmpty()) {
			try {
				waitForRadiationTimeout = Integer.valueOf(GDR
						.parse_int(waitForRadiationString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid wait for radiation value \""
								+ waitForRadiationString
								+ "\". Must be an integer specifying number of seconds");
			}
		}

		/**
		 * Create an unique ID to identify identical file loads sent within a
         * millisecond of each other
         */
		final int id = (generator.nextInt(MAX_RANDOM_ID_PREFIX) + source + System.currentTimeMillis())
				.hashCode();

		try {
			SendFileApp.sendFileLoads(appContext, type, source, target, overwrite,
					stringId, virtualChannel, scid, id);
			this.requestManager.waitForRadiation(id, waitForRadiationTimeout);
        } catch (final UplinkException e) {
			throw e;
		} catch (final Exception e) {
			throw new UplinkException(e);
		}
	}

	/**
	 * Transmit the supplied command list line
	 * 
	 * @param linePieces
	 *            the pieces of the parsed line to be sent
	 * @throws UplinkException
	 *             an error was encountered with sending the line
	 */
    protected void sendScmf(final String[] linePieces) throws UplinkException, ICmdException {
		if (linePieces.length < 4) {
			throw new IllegalArgumentException("Not enough \"" + delimiter
					+ "\" delimiter characters found in SCMF string.");
		}

		String scmfFile = null;
		Boolean disableChecksums = null;
		Integer waitForRadiationTimeout = null;

		scmfFile = linePieces[2].trim();

		// see if checksum checking is disabled
		final String disableChecksumsString = linePieces[1].trim();
		if (!disableChecksumsString.isEmpty()) {
			try {
				disableChecksums = Boolean.valueOf(GDR
						.parse_boolean(disableChecksumsString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid disable checksums value \""
								+ disableChecksumsString
								+ "\". Valid values are [" + Boolean.TRUE + ","
								+ Boolean.FALSE + "].", nfe);
			}
		}

		// see if we should wait for radiation
		final String waitForRadiationString = linePieces[3].trim();
		if (!waitForRadiationString.isEmpty()) {
			try {
				waitForRadiationTimeout = Integer.valueOf(GDR
						.parse_int(waitForRadiationString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid wait for radiation value \""
								+ waitForRadiationString
								+ "\". Must be an integer specifying number of seconds");
			}
		}

		/** Create an unique ID to identify identical SCMFs sent within a
         * millisecond of each other
         */
		final int id = (generator.nextInt(MAX_RANDOM_ID_PREFIX) + scmfFile + System.currentTimeMillis())
				.hashCode();

		try {
			SendScmfApp.sendScmf(appContext, scmfFile, disableChecksums, id);
			this.requestManager.waitForRadiation(id, waitForRadiationTimeout);
        }
        catch (final ICmdException e) {
            throw e;
        }
		catch (final UplinkException e) {
			throw e;
		} catch (final Exception e) {
			throw new UplinkException(e);
		}
	}

	/**
	 * Transmit the supplied command list line
	 * 
	 * @param linePieces
	 *            the pieces of the parsed line to be sent
	 * @throws UplinkException
	 *             an error was encountered with sending the line
	 */
	protected void sendRawData(final String[] linePieces)
            throws UplinkException, ICmdException {
		if (linePieces.length < 4) {
			throw new IllegalArgumentException("Not enough \"" + delimiter
					+ "\" delimiter characters found in raw data string.");
		}

		String rawDataFile = null;
		boolean isHex = false;
		Integer waitForRadiationTimeout = null;

		rawDataFile = linePieces[2].trim();

		// see if the hex flag is set
		final String isHexString = linePieces[1].trim();
		if (!isHexString.isEmpty()) {
			try {
				isHex = GDR.parse_boolean(isHexString);
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException("Invalid hex flag value \""
						+ isHexString + "\". Valid values are [" + Boolean.TRUE
						+ "," + Boolean.FALSE + "].", nfe);
			}
		}

		final String waitForRadiationString = linePieces[3].trim();
		if (!waitForRadiationString.isEmpty()) {
			try {
				waitForRadiationTimeout = Integer.valueOf(GDR
						.parse_int(waitForRadiationString));
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Invalid wait for radiation value \""
								+ waitForRadiationString
								+ "\". Must be an integer specifying number of seconds");
			}
		}

		/*
         * Create a unique ID to identify identical raw data files sent within a
         * millisecond of each other
         */
		final int id = (generator.nextInt(MAX_RANDOM_ID_PREFIX) + rawDataFile + System
				.currentTimeMillis()).hashCode();

		try {
			SendRawUplinkDataApp.sendRawUplinkData(appContext, rawDataFile, isHex, id);
			this.requestManager.waitForRadiation(id, waitForRadiationTimeout);
        }
        catch (final ICmdException e) {
            throw e;
        }
        catch (final UplinkException e) {
			throw e;
		} catch (final Exception e) {
			throw new UplinkException(e);
		}
	}

	/**
	 * Report if the UplinkConnectionType is command service
	 * @return TRUE if UplinkConnectionType COMMAND SERVICE is being used, FALSE otherwise
	 */
	protected boolean isCommandServiceUplink() {
		final UplinkConnectionType connType = hostConfig.getFswUplinkConnection().getUplinkConnectionType();

		return connType.equals(UplinkConnectionType.COMMAND_SERVICE);
	}

    /**
     * Set the uplink data rates in the Command Properties
     * 
     * @param linePieces
     *            the data rates to be set in the Command Properties
     * @throws UplinkParseException
     */
    protected void configureUplinkRate(final String[] linePieces)
            throws UplinkParseException {
        if (linePieces.length < 2) {
            throw new IllegalArgumentException(
                    "Not enough \"" + delimiter + "\" delimiter characters found in set uplink rate string.");
        }

        final String[] uplinkRates = Arrays.copyOfRange(linePieces, 1, linePieces.length);
        appContext.getBean(CommandProperties.class).setUplinkRates(uplinkRates);
    }

    @Override
    public void run() {
		/** To make startup more responsive, print
		 * this message that informs the MTAK proxy that bootup is done.  Using System.out
		 * so that log configurations do not accidentally suppress this message.
		 */
    	System.out.println("MTAK uplink server ready for commands.");
		while (!shutdownRequested.get()) {
			Exception ex = null;
			String failureCode = null;
			String line = null;
			try {
				if (br.ready()) {
					line = br.readLine();
					if (line != null) {
						if (line.endsWith(terminator)) {
							line = line.substring(0,
									line.length() - terminator.length());
						}
						readAndSendLine(line);
						continue;
					}
				}

				SleepUtilities.checkedSleep(INPUT_POLLING_DELAY_MS);
			} catch (final UplinkException e) {
				ex = e;

				// determine error code based on failure reason
				final IUplinkResponse resp = e.getUplinkResponse();

				if (resp != null) {
					failureCode = Integer.toString(ICmdErrorManager
							.getErrorCode(resp.getFailureReason()));
				} else {
					failureCode = FAILURE;
				}

            }
            catch (final ICmdException e) {
                ex = e;
                failureCode = Integer.toString(ICmdErrorManager.getErrorCode(e));
            }
            catch (final Exception e) {
				ex = e;
				failureCode = FAILURE;
			} finally {
				if (ex != null) {
					// output error code
					System.err.println("Success=" + failureCode);
					System.err.println("Error sending uplink data \"" + line
							+ "\": " + ex.getMessage());
				}
			}
		}
	}


    @Override
    protected final void startMessagingInterfaces() {
        if (sessionConfig == null) {
            throw new IllegalArgumentException("Cannot start messaging interfaces. Session configuration is null.");
        }

        if (jmsPortal == null) {

            jmsPortal = appContext.getBean(IMessagePortal.class);
            jmsPortal.enableImmediateFlush(true);
            jmsPortal.startService();

        }
    }

	/**
	 * The main function of the mtak uplink server app
	 * @param args the command line arguments supplied as an array of Strings
	 */
	public static void main(final String[] args) {
		try {
			final MtakUplinkServerApp app = new MtakUplinkServerApp();

            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);

			app.startExternalInterfaces();

			app.loadConfiguredCommandDictionary();

			app.startup();
		} catch (final NullPointerException npe) {
            TraceManager.getDefaultTracer()
                        .error("Encountered unexpected NullPointerException while running "
                                + ApplicationConfiguration.getApplicationName(),
                               " ", ExceptionTools.getMessage(npe), ", ", npe.getLocalizedMessage(), npe);
			System.exit(1);
		} catch (final ICmdException e) {
            TraceManager.getDefaultTracer().error("Exception was encountered while running "
					+  ApplicationConfiguration.getApplicationName() + ": "
                    + ExceptionTools.getMessage(e), e);
			final int exitCode = ICmdErrorManager.getErrorCode(e);
			System.exit(exitCode);
		} catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Exception was encountered while running "
					+  ApplicationConfiguration.getApplicationName() + ": "
                    + ExceptionTools.getMessage(e), e);
			System.exit(1);
		}
	}

}
