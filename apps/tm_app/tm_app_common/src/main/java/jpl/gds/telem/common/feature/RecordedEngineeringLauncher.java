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
package jpl.gds.telem.common.feature;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.options.DownlinkStreamTypeOption;
import jpl.gds.common.options.ProductNameListOption;
import jpl.gds.common.options.SubtopicOption;
import jpl.gds.common.options.TestbedNameOption;
import jpl.gds.common.options.VenueTypeOption;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.options.SubscriberTopicsOption;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.message.api.app.MessageAppConstants;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.product.api.config.RecordedProductProperties;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.options.PrintLogOption;
import jpl.gds.shared.cli.options.UniqueIdOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.junit.JunitMarker;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.process.FileLineHandler;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.util.HostPortUtility;


/**
 * RecordedEngineeringLauncher is responsible for creating and initializing the
 * chill_recorded_eng_watcher process for a downlink session.
 *
 * The test may be marked for partial execution through JunitMarker.
 *
 */
public class RecordedEngineeringLauncher extends Object
        implements IService, MessageSubscriber
{
	private static final String COULD_NOT_LAUNCH = "Could not launch ";


	/** Wake up periodically to check */
	private static final long SAFETY = 500L;

	private static final int GOOD_STATUS    =  0;
	private static final int TIMEOUT_STATUS = -1;

    private final Tracer                      log;
	private final String apidTypes;
	private final String configName;
	
	/** Time to wait for process to start and get success text
	 */
	private final long startupTimeout;
	
	/** Time to wait for process to stop */
	private final long shutdownTimeout;

	/*
	 * UNIQUE variable cannot
	 * be static or we get duplicate log files upon session
	 * restart.
	 */
	private String unique;

	/*  Changed name of _success to _successStart. */
	/** Set true if process seems to have started successfully */
	private final AtomicBoolean _successStart = new AtomicBoolean(false);

	/* Added _successShutdown. */
	/** Set true if process seems to have stopped successfully */
	private final AtomicBoolean _successShutdown = new AtomicBoolean(false);

	/** Sync and wait object */
	private final Object _sync = new Object();

	private FileLineHandler _recordedOutputFile = null;

    private volatile long                     messageCount        = 0;

	private boolean _isStarted = false;

	private ProcessLauncher _launcher = null;

	private String _outputDir = null;
	
	private final ApplicationContext appContext;
	private final IMessagePublicationBus bus;
	private final MessageServiceConfiguration msgConfig;
	private Long sessionNumber;
	
    /**
     * @param context
     *            The current application context
     */
	public RecordedEngineeringLauncher(final ApplicationContext context) {
		this.appContext = context;
		
        // Get recorded product properties from the context
		final RecordedProductProperties rpc = appContext.getBean(RecordedProductProperties.class);
        configName = rpc.recordedProductProcessingWatcherScript();

        startupTimeout = rpc.recordedProcessStartupTimeout() * 1000L;
        shutdownTimeout = rpc.getRecordedProcessShutdownTimeout() * 1000L + (5L * 1000L);
		
        this.bus = appContext.getBean(IMessagePublicationBus.class);
		this.msgConfig = appContext.getBean(MessageServiceConfiguration.class);
		
		final IApidDefinitionProvider iad = appContext.getBean(IApidDefinitionProvider.class);
		
		final StringBuilder types = new StringBuilder();
		
		getApidNames(types, iad, rpc.getEhaProductApids());
		getApidNames(types, iad, rpc.getEvrProductApids());
		apidTypes = types.toString();
		
        log = TraceManager.getTracer(context, Loggers.RECORDED_ENG);
	}

	/**
	 * Starts the process. This method will return true unless an actual error
	 * occurs launching the process, but will not start the process if the message service is
	 * disabled in the configuration, or there is no current session
	 * number.
	 *
	 * @return True if the process is started successfully
	 */
    @Override
    public boolean startService()
	{
		if (_isStarted)
		{
			return true;
		}

		if (JunitMarker.getJunitState())
		{
			// Pretend we started
			return true;
		}

		if (! msgConfig.getUseMessaging())
		{
			log.info("The message service is disabled, " + configName + " will not run");

			return false;
		}

		if ((apidTypes == null) || apidTypes.isEmpty())
		{
			log.info("No APIDs configured, " + configName + " will not run");

			return false;
		}

		if (appContext.getBean(IContextIdentification.class).getNumber() == null)
		{
			log.info("No session number, " + configName + " will not run");

			return true;
		}

		unique = System.currentTimeMillis() +
				"."                        +
				HostPortUtility.getLocalHostName();

		if (CheckMessageService.checkMessageServiceRunning(msgConfig, null, 0, log, false))
		{
            bus.subscribe(ProductMessageType.ProductAssembled, this);


		    
			// Message service is running, so try to start now

			return internalStart();
		}


		// If we got here, either we cannot
		// start the process for some reason that is not going to change, or 
		// message service is not running. If it is not running, we should fail to start 
		// the session right up front.  The user can restart the session once
		// the service is back up.
		return false;
	}

    @Override
    public void handleMessage(final IMessage message) {
        if (message.isType(ProductMessageType.ProductAssembled)) {
            messageCount += 1;
        }
    }


	/**
	 * Two log files will be created. We create one to hold the result of
	 * starting and stopping the process; the process will create its own log
	 * file to contain the bulk of the processing.
	 *
	 * @return True if started properly
	 */
	private synchronized boolean internalStart()
	{

		final List<String>         argList = new ArrayList<String>();

		_outputDir = appContext.getBean(IGeneralContextInformation.class).getOutputDir();
        // MDC.put(LoggingConstants.FILE_APP_LOG_PATH, _outputDir +
        // File.separator + "RecordedEngWatcher." + unique + ".log");

		log.info(configName + " is enabled and is being started");

		try
		{
			final String scriptName =
                    GdsSystemProperties.getSystemProperty(
							GdsSystemProperties.DIRECTORY_PROPERTY) +
							File.separator                                        +
							configName;

			// This is a special case.  The script to launch cannot be found.
			// Normally, this would be an error, but it is critical to continue 
			// in this case, because it is impossible to run a debugger in the
			// dev environment if we do not continue.
			if (!new File(scriptName).exists()) {
				log.error("Unable to start " + configName + " because the script cannot be located.");
				return true;
			}

			/*
			 * No longer keep both a start log and
			 * a recorded eng watcher log. There is now only one log file, written
			 * by the chill_down process.
			 */
			/*
			 *  UNIQUE variable cannot
			 * be static or we get duplicate log files upon session
			 * restart.
			 */
            final File launchFile = new File(_outputDir + File.separator + "RecordedEngWatcher." + unique + ".log");

			if (! launchFile.createNewFile())
			{
				log.error(configName                           +
						" could not create log file: " +
						launchFile.getAbsolutePath());

				return false;
			}

			_recordedOutputFile = new RecordedLineHandler(launchFile);

			_launcher = new ProcessLauncher();

			_launcher.setErrorHandler(_recordedOutputFile);
			_launcher.setOutputHandler(_recordedOutputFile);

			argList.add(scriptName);

			populateArgList(argList);

			log.info("Launching " + configName + ": " + argList);

			if (!_launcher.launch(
					argList.toArray(new String[argList.size()]),
                                  GdsSystemProperties.getSystemProperty("user.dir")))
			{
				log.error(COULD_NOT_LAUNCH + configName);

				return false;
			}
		}
		catch (final Exception e)
		{
		    e.printStackTrace();
		    
			log.error(COULD_NOT_LAUNCH + configName + ": " + e);

			return false;
		}

		final Pair<Boolean, Integer> status = waitForStartup();

		if (status.getOne())
		{
			bus.publish(
					appContext.getBean(IStatusMessageFactory.class).createPublishableLogMessage(
                            TraceSeverity.INFO, configName + " has been launched"));
            log.debug(configName + " has been launched");
			_isStarted = true;

			return true;
		}

		final int exitCode = status.getTwo();

		switch (exitCode)
		{
		case GOOD_STATUS:
			// Did not get success text, hence a good status
			// is not good enough
			log.error(COULD_NOT_LAUNCH                   +
					configName                               +
					" as "                             +
					argList                            +
					", no error but no success text. " +
					"See logs in "                     +
					_outputDir);
			break;

		case TIMEOUT_STATUS:
			log.error(COULD_NOT_LAUNCH                         +
					configName                                     +
					" as "                                   +
					argList                                  +
					", timed out waiting for success text. " +
					"See logs in "                           +
					_outputDir);
			break;

		default:
			log.error(COULD_NOT_LAUNCH         +
					configName                     +
					" as "                   +
					argList                  +
					", received error code " +
					exitCode                 +
					". See logs in "         +
					_outputDir);

			break;
		}

		return false;
	}


	/**
	 * Populate argument list for launching.
	 *
	 * @param argList List to populate
	 * @param session Session configuration
	 */
	private void populateArgList(final List<String>         argList)
	{
		final IVenueConfiguration venueConfig = appContext.getBean(IVenueConfiguration.class);
		final VenueType             venue     = venueConfig.getVenueType();
        final IDatabaseProperties dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);

		// Provide unique string for file name disambiguation.
		/*
		 * UNIQUE variable cannot
		 * be static or we get duplicate log files upon session
		 * restart. That means this method cannot be static either.
		 * No idea why it was static in the first place.
		 */
		argList.add("--" + UniqueIdOption.UNIQUE_LONG_OPT);
		argList.add(unique);

		argList.add("--" + MessageServiceCommandOptions.JMS_HOST_LONG);
		argList.add(msgConfig.getMessageServerHost());

		argList.add("--" + MessageServiceCommandOptions.JMS_PORT_LONG);
		argList.add(String.valueOf(msgConfig.getMessageServerPort()));

		argList.add("--" + VenueTypeOption.LONG_OPTION);
		argList.add(venue.toString());

		argList.add("--" + DatabaseCommandOptions.DB_HOST_LONG);
		argList.add(dbProperties.getHost());

		argList.add("--" + DatabaseCommandOptions.DB_PORT_LONG);
		argList.add(String.valueOf(dbProperties.getPort()));
		
		// added topic option. Each TP worker adds a unique value to the main topic name
		argList.add("--" + SubscriberTopicsOption.LONG_OPTION);
		argList.add(appContext.getBean(IGeneralContextInformation.class).getRootPublicationTopic() + ".product");

		argList.add("--" + SessionCommandOptions.SESSION_KEY_LONG);
		argList.add(String.valueOf(appContext.getBean(IContextIdentification.class).getNumber()));

		argList.add("--" + SessionCommandOptions.SESSION_USER_LONG);
		argList.add(appContext.getBean(IContextIdentification.class).getUser());

		argList.add("--" + SessionCommandOptions.SESSION_HOST_LONG);
		argList.add(appContext.getBean(IContextIdentification.class).getHost());

		//        argList.add("--" + MessageResponderApp.LOG_DIRECTORY_LONG_OPT);
		//        argList.add(session.getOutputDir());

		argList.add("--" + jpl.gds.shared.cli.options.ExitWithSessionOption.EXIT_SESSION_LONG);

		argList.add("--" + PrintLogOption.PRINT_LONG_OPT);

		argList.add("--" + ProductNameListOption.PRODUCT_NAMES_LONG);
		argList.add(apidTypes);

		/*
		 * remove adding args to set sessionDssId
		 * chill_recorded_eng_watcher no longer takes it as an argument
		 */

		final Integer vcid =  appContext.getBean(IContextFilterInformation.class).getVcid();

		if (vcid != null)
		{
			argList.add("--" + SessionCommandOptions.SESSION_VCID_LONG);
			argList.add(String.valueOf(vcid));
		}

		/*
		 * Remove references to DownlinkSpacecraftSide
		 * because it is not multimission.
		 */
		if (venue.equals(VenueType.ATLO) ||
				venue.equals(VenueType.TESTBED))
		{
			argList.add("--" + TestbedNameOption.LONG_OPTION);
			argList.add(venueConfig.getTestbedName());

			final DownlinkStreamType stream =
					venueConfig.getDownlinkStreamId();

			// Removed test for NORMAL

            if (!appContext.getBean(SseContextFlag.class).isApplicationSse() && (stream != null))
			{
				argList.add("--" +
						DownlinkStreamTypeOption.LONG_OPTION);
				argList.add(stream.toString());
			}
		}
		else if (venue.isOpsVenue())
		{
			final String subtopic = appContext.getBean(IGeneralContextInformation.class).getSubtopic();

			if (subtopic != null)
			{
				argList.add("--" +
						SubtopicOption.LONG_OPTION);
				argList.add(subtopic);
			}
		}
	}


	/**
	 * Wait for text that marks a successful startup, or time out.
	 *
	 * There are several possibilities. We can get the success text, or
	 * we can time out waiting for it, or the process can exit with an error,
	 * or the process can exit with no error (but no success text).
	 *
	 * Returns a pair of statuses. The first status is true if we received the
	 * success text, and false if not. The second is the exit code.
	 * GOOD_STATUS means no error, TIMEOUT_STATUS is a timeout, other values
	 * mean an error of some kind.
	 *
	 * @return Pair of statuses
	 */
	private Pair<Boolean, Integer> waitForStartup()
	{
		final long timeout = System.currentTimeMillis() + startupTimeout;

		synchronized (_sync)
		{
			while (! _successStart.get())
			{
				if (SleepUtilities.checkedWait(_sync, SAFETY))
				{
					// Interrupt, check condition
					continue;
				}

				/*
				 * he exit code from the launched
				 * process is no longer relevant because it is just a script
				 * that launches another script in the background now. So I have
				 * removed the check on the exit code.
				 */

				if (!_successStart.get() && System.currentTimeMillis() >= timeout)
				{
					// Giving up

					return new Pair<Boolean, Integer>(false, TIMEOUT_STATUS);
				}
			}
		}

		// Got success text in time. Status assumed OK since still running.

		return new Pair<Boolean, Integer>(true, GOOD_STATUS);
	}


	/**
	 * Shuts down the process and closes the log file.
	 */
    @Override
    public synchronized void stopService()
	{
		if (! _isStarted)
		{
			return;
		}

		// Flag process to shut down

		log.info("Started shutdown of " + configName);

		/*
		 * UNIQUE variable cannot
		 * be static or we get duplicate log files upon session
		 * restart.
		 */
		final String flagFileName = String.format(
		        MessageAppConstants.PRODUCT_HANDLER_FLAG_PATTERN, unique);
		final File flagFile = new File(_outputDir                      +
				File.separator                  +
				flagFileName);

		log.debug(configName                  +
				" created flag file " +
				flagFile.getAbsolutePath());

		try
		{
		    /**
             * First, create a temporary file and populate it with the number
             * of expected messages. The subprocess can then read it and attempt to
             * wait for that number of messages.
             */
            final File tmpFile = File.createTempFile(flagFile.getName(), null, flagFile.getParentFile());
            boolean errorWithFile = false;
            try (
                DataOutputStream dos =
                new DataOutputStream(new FileOutputStream(tmpFile))
                    ) {
               dos.writeLong(messageCount);
            } catch (final FileNotFoundException e) {
                log.error(configName                           +
                        " error opening temp flag file " +
                        flagFile.getAbsolutePath());
                errorWithFile = true;
            }

            if (!errorWithFile) {
                try {
                    Files.move(tmpFile.toPath(), flagFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                } catch (final IOException e) {
                    log.error(configName                           +
                            " could not create flag file " +
                            flagFile.getAbsolutePath());

                }
            }
		}
		catch (final IOException ioe)
		{
			log.error(configName                           +
					" could not create flag file " +
					flagFile.getAbsolutePath()     +
					"; "                           +
					ioe);
		}

		// Give it some time to shut down naturally

		final int exitCode = waitForShutdown();

		switch (exitCode)
		{
		case GOOD_STATUS:
			log.info("Successful shutdown of " + configName);
			break;

		case TIMEOUT_STATUS:
			log.error("Shutdown of "              +
					configName                        +
					", timed out. See logs in " +
					_outputDir);
			break;

		default:
			log.error("Shutdown of "           +
					configName                     +
					", received error code " +
					exitCode                 +
					". See logs in "         +
					_outputDir);
			break;
		}

        if (bus != null) {
            bus.unsubscribe(ProductMessageType.ProductAssembled, this);
        }

		if (_launcher != null)
		{
			_launcher.destroy();

			log.debug(configName + " stopped");
		}

		if (_recordedOutputFile != null)
		{
			_recordedOutputFile.shutdown();
		}

		_isStarted = false;
	}


	/**
	 * Wait for a successful shutdown, or time out.
	 *
	 * There are several possibilities. We can time out waiting, or the process
	 * can exit with an error, or the process can exit with no error.
	 *
	 * Returns the exit code. GOOD_STATUS means no error, TIMEOUT_STATUS is a
	 * timeout, other values mean an error of some kind.
	 *
	 * @return Exit status
	 */
	private int waitForShutdown()
	{
        final long timeout = System.currentTimeMillis() + shutdownTimeout;
		int        exitValue = GOOD_STATUS;

		/*
		 *  We now have to wait for exit
		 *  text from the recorded engineering process. That's where
		 *  the successShutdown flag comes from.
		 */
		while (!_successShutdown.get())
		{
			// Sleep for a while, don't worry about interrupts
			// or waking up early

			SleepUtilities.checkedSleep(SAFETY);

			/*
			 * The exit code from the launched
			 * process is no longer relevant because it is just a script
			 * that launches another script in the background now. So I have
			 * removed the check on the exit code.
			 */

			if (!_successShutdown.get() && System.currentTimeMillis() >= timeout)
			{
				// Giving up

				exitValue = TIMEOUT_STATUS;

				break;
			}
		}

		// We've either exited or timed out.

		return exitValue;
	}


	/**
	 * Get APID names from property and construct a comma-separated
	 * string in a string builder.
	 *
	 * @param sb       String builder
	 * @param iad      Apid dictionary
	 * @param property Property name
	 */
	private void getApidNames(final StringBuilder   sb,
			final IApidDefinitionProvider iad, final String[] apids)
	{
		if (apids != null)
		{
			for (final String apid : apids)
			{
				if (sb.length() > 0)
				{
					sb.append(',');
				}

				/* Go through APID definition to get name. */
				final IApidDefinition def = iad.getApidDefinition(Integer.parseInt(apid));
				final String name = def == null? "Unknown" : def.getName();

				sb.append(name);
			}
		}
	}


	/**
	 * RecordedLineHandler is a text handler designed to accept the standard
	 * output and standard error streams from the process.
	 * 
	 * It looks for specific evidence of startup success and notifies the parent
	 * object if that evidence is seen.
	 * 
	 * It also looks for log entries that indicate potential problems ( WARN
	 * severity or higher) and publishes them as ExternalLogMessages so that
	 * chill_down users can see them.
	 * 
	 * Output is then forwarded on to the log file.
	 * 
	 *  Capture log entries
	 *          from the eng_watcher process to the chill_down log
	 */
	private class RecordedLineHandler extends FileLineHandler
	{   

		/**
		 * Creates a RecordedLineHandler for the given log file.
		 *
		 * @param logfile the File object for the log to send output to

		 * @throws IOException if there is a problem opening the log file
		 */
		public RecordedLineHandler(final File logfile) throws IOException
		{
			super(logfile);
		}


		/**
		 * Handle the next line received. Check for the success text that
		 * indicates the process was successfully launched.  Capture WARN and
		 * ERROR logs from the sub-process to the chill_down log.
		 *
		 * @param line Line received
		 *
		 * @throws IOException if there is a problem writing to the log file
		 */
		@Override
        public void handleLine(String line) throws IOException
		{

			/**
			 *
			 * changed to look for message router 'up' message"
			 */
			if (line.contains(MessageAppConstants.MESSAGE_ROUTER_UP_MESSAGE)) {
				_successStart.set(true);

				synchronized (_sync)
				{
					_sync.notifyAll();
				}
			}

			/*  
			 * Added setting of _successShutdown
			 * flag here when message router shutdown text is seen in the output.
			 * This is because we can no longer rely upon process exit
			 * to determine when the process is done. 
			 */ 
            else if (line.indexOf(MessageAppConstants.MESSAGE_ROUTER_DOWN_MESSAGE) >= 0
                    || line.indexOf(MessageAppConstants.LISTENERS_DOWN_MESSAGE) >= 0) {
				_successShutdown.set(true);

				synchronized (_sync)
				{
					_sync.notifyAll();
				}
			}

			/**
			 * Trap all messages from
			 * the recorded eng watcher with severity >= WARNING and log them
			 * like other chill_down log entries. These get onto the chill_down
			 * console and into the database.
			 */

			final int index = line.indexOf(' ');
			if (index > 0) {

				// Chunk of log text up to the first blank is the log
				// level. This level must be mapped from a Log4J level to
				// an AMPCS TraceSeverity.
				final String type = line.substring(0, index);

                TraceSeverity priority;
                try {
                    priority = TraceSeverity.fromStringValue(type);
                } catch (final IllegalArgumentException e) {
                    // Something fishy happened. Default log to INFO level and
                    // keep running
                    priority = TraceSeverity.INFO;
                }

				// If a warning or greater, strip off the existing timestamp
				// from the log entry and re-log it here
				if (priority == TraceSeverity.WARN) { 
                    line = line.substring(line.indexOf("]:") + 3);
                    if (line.contains("{") && line.contains("}")) {
                        log.warn(line.substring(line.indexOf("}") + 1));
                    }
				} else if (priority == TraceSeverity.ERROR || priority == TraceSeverity.FATAL) { 
                    line = line.substring(line.indexOf("]:") + 3);
                    if (line.contains("{") && line.contains("}")) {
                        log.error(line.substring(line.indexOf("}") + 1));
                    }
				}
			}

			super.handleLine(line);
		}
	}

}
