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
package jpl.gds.watcher.responder.app;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.PrintLogOption;
import jpl.gds.shared.cli.options.UniqueIdOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.LoggingConstants;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.MessageRouter;

/**
 * MessageResponderApp is the main application class for the Message Responder, which
 * listens for messages on the MPCS session topic and routes them to any
 * configured handlers. It is capable of using a ResponderAppHelper object to add additional
 * command line options and parsing to this generic application.
 *
 * SPV is enabled by default
 *      Changed static flag to local and old conditional code to use SPV or not
 *
 */
public class MessageResponderApp extends AbstractCommandLineApp implements Runnable, IQuitSignalHandler
{

    private static final int DEFAULT_QUEUE_SIZE = 65536;
    
    /** Disable for the time being */
    private static final boolean            CHECK_OVERWRITE            = false;

    private static MessageResponderApp      app;
    private static final String             APP_NAME                   = ApplicationConfiguration
            .getApplicationName("chill_message_responder");

    /** Long log directory option */
    public static final String              LOG_DIRECTORY_LONG_OPT     = "logDirectory";

    /** Value returned to indicate we could not connect to the JMS */
    private static final int                NO_JMS_CONNECT_ERROR       = -1;

    private final Tracer                          tracer;
    private boolean                         print;
    private MessageRouter                   router;

    private final AtomicBoolean             done                       = new AtomicBoolean(false);

    private final IContextConfiguration     sessionConfig;

    /*
     * Expand definition of NEED_KEY to allow it to be satisfied by -K or -N command line options
     * NEED_KEY is set by the shell script.
     *      It indicates that we must have session id and session host, OR session config file
     */
    private static final boolean            NEED_KEY                   = GdsSystemProperties.getNeedsContextKey();

    private long                            sessionId                  = 0L;
    private String                          sessionHost                = null;

    /** Disambiguator. May be set from parameter list. */
    private String                          unique                     = System.currentTimeMillis() + "."
            + HostPortUtility.getLocalHostName();

    private ApplicationContext       appContext;

    private final IResponderAppHelper        appHelper;

	/**
	 * Fetch instance factory.
	 */
	protected final IDbSqlFetchFactory fetchFactory;
	
    private UnsignedInteger queueSize;
	
	private SessionCommandOptions sessionOptions;
	private MessageServiceCommandOptions messageOptions;
	private DatabaseCommandOptions databaseOptions;
    // Use shared PrintLogOption
	private final PrintLogOption printLogOption =  new PrintLogOption();
	private final DirectoryOption logDirOption = new DirectoryOption(null, LOG_DIRECTORY_LONG_OPT, "directory",
                  "directory of log file", false, false);
    // Get shared unique ID option
	private final UniqueIdOption uniqueIdOption = new UniqueIdOption(false);
    private ContextCommandOptions contextOptions;
    private final UnsignedIntOption queueSizeOpt = new UnsignedIntOption(null, "queueSize", 
            "size", "Length of the internal message queue as number of messages. MUST BE A POWER OF 2. Defaults to " + DEFAULT_QUEUE_SIZE, false);


    /**
     * Creates an instance of MessageResponderApp.
     * 
     * @throws ClassNotFoundException
     *             If helper name not a class
     * @throws InstantiationException
     *             Helper cannot be instantiated
     * @throws IllegalAccessException
     *             On permission problem
     * @throws ReflectionException
     *             if the helper object could not be created
     */
    public MessageResponderApp()
            throws ClassNotFoundException,
        InstantiationException,
        IllegalAccessException, ReflectionException
    {
        super();
        
        if (GdsSystemProperties.getSystemProperty("GdsResponderTestBootpath") != null) {
            
            appContext = SpringContextFactory.getSpringContext(Arrays.asList(GdsSystemProperties.getSystemProperty("GdsResponderTestBootpath")),
                                                               true); // check to see if this 'true' is necessary.
        } else {
            appContext = SpringContextFactory.getSpringContext(true); 
        }

        sessionConfig = new SessionConfiguration(appContext);
        
        tracer = TraceManager.getTracer(appContext, Loggers.WATCHER);
		/*
		 * Initialize Fetch Factory from application context.
		 */
		this.fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);

        /*
         * Retrieve ResponderAppHelper implementation for local SpringBoot Application Context.
         */
        this.appHelper = appContext.getBean(IResponderAppHelper.class);
    }

    /**
     * Gets the current context configuration. For use by unit tests.
     * 
     * @return context configuration
     */
    public IContextConfiguration getContextConfiguration() {
    	return appContext.getBean(IContextConfiguration.class);
    }
    
    /**
     * Starts the listening thread.
     *
     */
    public void start()
    {
    	new Thread(this).start();
    }


    @Override
	public BaseCommandOptions createOptions() {
        
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        this.sessionOptions = new SessionCommandOptions((SessionConfiguration)sessionConfig);
        this.sessionOptions.setPreventOverrides(CHECK_OVERWRITE);
        options.addOptions(this.sessionOptions.getAllMonitorOptions(true));
        
        this.messageOptions = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
        options.addOptions(this.messageOptions.getAllOptionsWithoutNoJms());
        
        this.databaseOptions = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
        options.addOptions(this.databaseOptions.getAllOptionsWithoutNoDb());
       
        options.addOption(printLogOption);
        
        options.addOption(logDirOption);
        logDirOption.setHidden(true);
        
        options.addOption(uniqueIdOption);
        uniqueIdOption.setDefaultValue(System.currentTimeMillis() + "." + HostPortUtility.getLocalHostName());
        uniqueIdOption.setHidden(true);
        
        contextOptions = new ContextCommandOptions(appContext.getBean(IContextConfiguration.class));
        options.addOption(contextOptions.SUBSCRIBER_TOPICS);

        if (appHelper != null) {
            appHelper.addAppOptions(options);
        }
        
        options.addOption(queueSizeOpt);
        queueSizeOpt.setDefaultValue(UnsignedInteger.valueOf(DEFAULT_QUEUE_SIZE));
        
        return options;
    }

    /**
     * Indicates whether the application is running with verbose output enabled.
     * @return true if verbose output is enabled
     */
    public boolean getVerbose() {
        return print;
    }

    /**
     * Getter for queue size - internal message queue as number of messages
     * Must be a power of 2. Defaults to 65535
     * 
     * @return
     */
    public UnsignedInteger getQueueSize() {
        return queueSize;
    }

    @Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final BaseCommandOptions options = createOptions();
        final PrintWriter pw = new PrintWriter(System.out);
        String addHelp = null;
        
        if (appHelper == null) {
            pw.println("Usage: " + APP_NAME + " [session options] [jms options] [database options] [--printLog]\n");
            pw.println("       " + APP_NAME + " --topics <topic-list> [jms options] [database options] [--printLog]\n");
            options.getOptions().printOptions(pw);
        } else {
            pw.println(appHelper.getUsageText());
            options.getOptions().printOptions(pw);
            addHelp = appHelper.getAdditionalHelpText();
            if (addHelp != null) {
            	pw.println(addHelp);
            }
        }
        pw.close();
    }


    /**
     * Parses the command line arguments, set appropriate flags, and creates the
     * shared data class that subscribes to JMS messages.
     *
     * @param commandLine a CommandLine object initialized using the current command line
     * arguments.
     *
     * @throws ParseException If error in arguments
     */
    @Override
	@SuppressWarnings("DM_EXIT")
    public void configure(final ICommandLine commandLine) throws ParseException
    {
        super.configure(commandLine);
      
        print = printLogOption.parse(commandLine);
        // do unique option before log dir option!
        unique = uniqueIdOption.parseWithDefault(commandLine, false, true);
        final String dir = logDirOption.parse(commandLine);

        if (dir != null) {
            final Path logPath = Paths.get(dir, getLogFileName());
            MDC.put(LoggingConstants.FILE_APP_LOG_PATH, logPath.toAbsolutePath().toString());
        } 

        this.databaseOptions.parseAllOptionsAsOptional(commandLine);
        this.messageOptions.parseAllOptionsAsOptional(commandLine);
       
        if (NEED_KEY && !(commandLine.hasOption(SessionCommandOptions.SESSION_CONFIG_LONG) ||
                (commandLine.hasOption(SessionCommandOptions.SESSION_KEY_LONG) &&
                        commandLine.hasOption(SessionCommandOptions.SESSION_HOST_LONG)))) {

            throw new ParseException("You must either supply --" +
                    SessionCommandOptions.SESSION_KEY_LONG + 
                    " and --" +
                    SessionCommandOptions.SESSION_HOST_LONG + 
                    " or --" +
                    SessionCommandOptions.SESSION_CONFIG_LONG); 
        }
        
        final boolean haveSessionKey = commandLine.hasOption(SessionCommandOptions.SESSION_KEY_LONG);
        final boolean haveSessionConfig = commandLine.hasOption(SessionCommandOptions.SESSION_CONFIG_LONG);
        
        sessionOptions.parseAllMonitorOptionsAsOptional(commandLine, true, false, true, true);

        if (haveSessionKey)
        {
            if (haveSessionConfig && sessionConfig.getContextId().getNumber() != null)
            {
                throw new ParseException("The option --" + SessionCommandOptions.SESSION_KEY_LONG + " may not be specified if the option --" +
                        SessionCommandOptions.SESSION_CONFIG_LONG + " has already been specified and contains a valid session key.");
            }

            final ISessionFetch tsf = fetchFactory.getSessionFetch();
            try
            {
                final IDbSessionInfoUpdater tsi = appContext.getBean(IDbSessionInfoFactory.class).createQueryableUpdater();
                sessionId = sessionConfig.getContextId().getNumber();

                if (NEED_KEY && (sessionId <= 0L))
                {
                    throw new ParseException("Value of --"                        +
                                             SessionCommandOptions.SESSION_KEY_LONG +
                                             " option must be greater than zero");
                }

                tsi.addSessionKey(sessionId);

                @java.lang.SuppressWarnings("unchecked")
                /*
                 * TI/TP: TP Worker "Stop" Execution freezing while
                 * Recorded Eng Watcher is running. Increasing the batch size.
                 * See associated comments below.
                 */
                final List<IDbSessionUpdater> testSessions = (List<IDbSessionUpdater>) tsf.get(tsi, null, 100,
                                                                                               (ISessionOrderByType) null);
                if(testSessions.isEmpty())
                {
                    tsf.close();
                    throw new ParseException("Value of -" + SessionCommandOptions.SESSION_KEY_LONG + " option must be a " +
                            "valid pre-existing session key. No session with the key \"" + sessionId + "\" was found.");
                }

                IDbSessionUpdater dsc = testSessions.get(0);

                // TI/TP: TP Worker "Stop" Execution freezing while
                // Recorded Eng Watcher is running.
                // Output directory was not being set correctly and due to that reason the 'flag' file
                // which is used to coordinate (startup/shutdown) was not being picked up from the right
                // location.
                // Issue here has to do with detecting whether we are running chill_down or TI/TP
                // The only way to figure that out is by the number of session entries in the Session
                // table, chill_down will always have a single entry
                // hence: testSessions.get(0); which has been in place all this time.
                // If there is more than 1 session entry running with TI/TP and need to iterate through all
                // session entries and get the very last one with type 'chill_telem_process'
                //
                // Do NOT want to use the SSE processors session
                // SSE products are not a thing.
                // So, update to use a processor entry where run FSW flag is TRUE

                if (testSessions.size() > 1) {
                    for (IDbSessionUpdater testSession : testSessions) {
                        if (testSession.getType().equalsIgnoreCase("chill_telem_process")
                                && Boolean.TRUE.equals(testSession.getFswDownlinkFlag())) {
                            dsc = testSession;
                        }
                    }
                }
                dsc.setIntoContextConfiguration(sessionConfig);

                // Do not reset type here; do it for relevant responders
            }
            catch (final DatabaseException s)
            {
                tsf.close();
                throw new ParseException("Error connecting to the database while looking up the specified session key: " + s.getLocalizedMessage());
            } finally {
                tsf.close();
            }
        }
        

        // If there is an application helper, it needs to parse command line options as well.
        if (appHelper != null) {
        	appHelper.setContextConfiguration(sessionConfig);
            appHelper.configure(commandLine);
        }
        
        sessionHost = sessionConfig.getContextId().getHost();
        
        contextOptions.SUBSCRIBER_TOPICS.parse(commandLine);
        
        queueSize = queueSizeOpt.parseWithDefault(commandLine, false, true);
        if (this.queueSize != null) {
            final int size = this.queueSize.intValue();
            if ((size & -size) != size) {
                throw new ParseException("The value of the --" + queueSizeOpt.getLongOpt() +
                        " must be a power of 2");
            }
        }
    }

    
    /**
     * Executes the main logic of this application.
     */
    @Override
	@SuppressWarnings("DM_EXIT")
    public void run()
    {
    	try {
    		router = new MessageRouter(appContext, APP_NAME, appHelper, queueSize.intValue());
    		router.setVerbose(print);

    		/*
    		 * If the message service is not up, the process will not start.
    		 * Exit with error if we cannot start here.
    		 */ 
    		if (!router.init((appHelper == null) ? null : appHelper.getOverrideTypes(),
    						NEED_KEY ? sessionId   : 0L,
    								NEED_KEY ? sessionHost : null)) {
    			System.exit ( NO_JMS_CONNECT_ERROR );      	
    		}

    		/*
    		 * This is a general class for all responders/watchers and
    		 * the output comes out for all of them, and is meaningless and useless
    		 * to the user in most cases. The output is needed for processes that come
    		 * up as chill_down daemons only and has been moved to the message handlers
    		 * that are used in these cases.
    		 */       

    		synchronized(router)
    		{
    			while (! done.get())
    			{
    				SleepUtilities.checkedWait(router, 3000L);
    			}
    		}

            if (GdsSystemProperties.getSystemProperty("GdsIsResponderTest") == null) {
    			System.exit(0);
    		}
    	} catch (final Exception e) {
    		tracer.error(ExceptionTools.getMessage(e), e);
    		System.exit(1);
    	}
    }

    /**
     * Gets the message service Port number. For test purposes.
     * @return port number, or -1 if none defined
     */
    public int getMessageServicePort() {
        return appContext.getBean(MessageServiceConfiguration.class).getMessageServerPort();
    }

    /**
     * Gets the message service host name. For test purposes.
     * @return the host name, or null if none defined.
     */
    public String getMessageServiceHost() {
    	return appContext.getBean(MessageServiceConfiguration.class).getMessageServerHost();
    }

    /**
     * Shuts down the application.
     *
     */
    public void shutdown() {
        if (router != null) {
            done.set(true);
            tracer.debug("Router shutting down");
            router.shutdown();
            synchronized (router) {
                router.notifyAll();
            }
        }
    }


    /**
     * Get application helper object.
     *
     * @return App helper
     */
    public IResponderAppHelper getAppHelper() {
        return appHelper;
    }

    /**
     * Get application object.
     *
     * @return Instance
     */
    public static MessageResponderApp getInstance() {
    	return app;
    }


    /**
     * Declare finished and shutting down.
     */    
    public void markDone() {
    	done.set(true);
    }


    /**
     * Disambiguator.
     *
     * @return Disambiguating string
     */
    public String getUnique()
    {
        return unique;
    }


    /**
     * Construct log file name (without path).
     *
     * @return Name for log file
     */
    private String getLogFileName()
    {
        final StringBuilder sb      = new StringBuilder();
        final String logFile = GdsSystemProperties.getSystemProperty("LOGFILE", "unknown");
        final int           dot     = logFile.lastIndexOf('.');

        if (dot >= 0)
        {
            sb.append(logFile.substring(0, dot));
            sb.append('.');
            sb.append(getUnique());
            sb.append(logFile.substring(dot));
        }
        else
        {
            sb.append(logFile);
            sb.append('.');
            sb.append(getUnique());
            sb.append(".log");
        }

        return sb.toString();
    }


    /**
     * Main entry method
     * @param args command line arguments
     */
    public static void main(final String[] args)
    {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        try {
            app = new MessageResponderApp();
            try {
                final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
                app.configure(commandLine);
            } catch (final ParseException e) {
                TraceManager.getTracer(Loggers.WATCHER).error(e.getMessage());
                System.exit(1);
            }
            app.start();
            
            // in some scenarios, like running from Eclipse, we need to pause long enough
            // for the other thread to start running or this thread exits the VM before the
            // other thread actually starts (brn)
            Thread.sleep(2000);
        } catch (final Exception e) {
            TraceManager.getTracer(Loggers.WATCHER).error(e.toString(), e);
            System.exit(1);
        }
    }

    @Override
    public void exitCleanly() {
        tracer.debug(APP_NAME, " received a shutdown request.  Shutting down gracefully...");

        try {
            shutdown();
        } catch (final Exception e) {
            // don't care
        }
    }
}
