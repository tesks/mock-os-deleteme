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
package jpl.gds.tcapp.icmd.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.cli.legacy.app.AbstractCommandLineApp;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.LoggingConstants;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.exception.ICmdException;
import jpl.gds.tc.api.message.CommandMessageType;


/**
 * The command line application chill_status_publisher.
 *
 * This application runs in the background and runs the UplinkStatusPublisher
 * to monitor the ICMD poller queue and finalize command statuses.
 *
 * This app writes to the database, but only commands, not log or session.
 * It does its own argument processing, since it needs only a few and does
 * not want to inherit most of the ones from the superclass.
 *
 * NOTE: This app assumes it will be connecting to the icmd web server (configured in the gds config)
 * to poll for command status information, and will use the security service if that is configured.
 * Authentication is done only by keytab file. There is no interactive login because this is presumed
 * to be a daemon service, and not a user application.
 *
 *
 * 7/31/13 replaced password file with keytab file
 * 02/07/18 Updated checkForAlreadyRunning and added resetLockFile to
 *          support multiple instances running simultaneously.
 */
public final class CommandStatusPublisherApp extends AbstractCommandLineApp implements IQuitSignalHandler
{
    static {
        // Originally done in the constructor, but Tracer was being
        // instantiated before the constructor set the time zone.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    private static final String LOCK_FILE = "/tmp/UPLINK_STATUS_PUBLISHER.lock";
    private static final String LOG_FILE_LONG    = "logFile";
    private static final String DEFAULT_LOG_FILE = "/tmp/command_status_publisher.log";
    // Security options
    private static final String KEYTAB_FILE_LONG = "keytabFile";
    private static final String USERNAME_LONG = "username";

    /** Uplink status publisher (with poller inside) */
    private UplinkStatusPublisher usp = null;
    /** Bridge to republish messages on session topics to the message service */
    private UplinkStatusMessageServiceBridge handler = null;

    /** Database interface */
    private IDbSqlArchiveController archiveController;

    private static AccessControl accessControl = null;

    private static final CommandUserRole ROLE         = CommandUserRole.VIEWER;

    private static final long SLEEP_SEC = 10L;
    private static final long MILLISECOND = 1000L;

    private static final LoginEnum       LOGIN_METHOD = LoginEnum.KEYTAB_FILE;
    private static String                keytabFile   = null;
    private String                       username     = null;
    

    private String logFile = DEFAULT_LOG_FILE;

    /** Logging interface */
    private Tracer                           log;

    /** Shared reference to the database configuration **/
    private final IDatabaseProperties dbProperties;
    
    private final ApplicationContext appContext;
    private final IMessagePublicationBus bus;
	private final IContextConfiguration sessionConfig;

    /**
     * Create an instance of CommandStatusPublisherApp
     */
    private CommandStatusPublisherApp()
    {
        super();
        appContext = SpringContextFactory.getSpringContext(true);
        sessionConfig = new SessionConfiguration(appContext);
        ReservedOptions.setApplicationContext(appContext);
        bus = appContext.getBean(IMessagePublicationBus.class);
        dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES,
        		                          IDatabaseProperties.class);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final Options options = createOptions();

        final HelpFormatter formatter = new HelpFormatter();

        final PrintWriter pw = new PrintWriter(System.out);

        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() +
                " [options]");
        pw.println("                   ");

        final int width = 80;
        final int leftPad = 7;
        final int descPad = 10;

        formatter.printOptions(pw, width, options, leftPad, descPad);

        pw.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Options createOptions()
    {
        final Options options = new Options();

        options.addOption(ReservedOptions.HELP);
        options.addOption(ReservedOptions.VERSION);
        options.addOption(null, KEYTAB_FILE_LONG, true, "Security keytab file");
        options.addOption(null, USERNAME_LONG, true, "Security username.");
        options.addOption(ReservedOptions.DATABASE_HOST);
        options.addOption(ReservedOptions.DATABASE_PORT);
        options.addOption(ReservedOptions.DATABASE_USERNAME);
        options.addOption(ReservedOptions.DATABASE_PASSWORD);
        options.addOption(ReservedOptions.FSW_UPLINK_HOST);
        options.addOption(ReservedOptions.FSW_UPLINK_PORT);

        options.addOption(null,
                LOG_FILE_LONG,
                true,
                "Log file (default " + DEFAULT_LOG_FILE + ")");

        return options;
    }

    @Override
    public void exitCleanly() {
        TraceManager.getDefaultTracer().debug(ApplicationConfiguration.getApplicationName(),
                                              " received a shutdown request");

        try {
            stopExternalInterfaces();
            resetLockFile();
        } catch (final RuntimeException rte) {
            throw rte;
        } catch (final Exception e) {
            // Nothing to do
        }
    }


    /**
     * Add start of status publisher.
     * 
     * @throws IllegalStateException
     *             If already running
     * @throws IOException
     *             If I/O error
     * @throws InterruptedException
     *             If interrupted
     */
    private void startExternalInterfaces() throws IllegalStateException, IOException, InterruptedException {

        // Do not allow process to start if it appears there
        // is another instance already running.
        // Check is done here to allow use of -h.
        checkForAlreadyRunning();

        if (dbProperties.getUseDatabase()) {
            startTestConfigDatabase(false);
            archiveController.startLogCommandStores(false);
        } else {
            log.info("Database is disabled.");
        }

        // Assume interface to icmd

        if (appContext.getBean(SecurityProperties.class).getEnabled())
        {
            try
            {
                synchronized (CommandStatusPublisherApp.class)
                {
                    if (accessControl == null)
                    {
                    	// use username supplied. If none, use login user
                    	final String user = username == null ? sessionConfig.getContextId().getUser() : username;
                        accessControl =
                            AccessControl.createAccessControl(
                                    appContext.getBean(SecurityProperties.class),
                                    user, ROLE, LOGIN_METHOD, keytabFile, false, null, log);
                        //  check for token indefinitely
                        accessControl.runContinuously(true);
                    }
                }
            }
            catch (final AccessControlException ace)
            {
                throw new IllegalArgumentException("Could not start access "
                        + "control, unable to " + "run", ace);
            }

            try {
                accessControl.requestSsoToken();

                // Now get the real user

                appContext.getBean(AccessControlParameters.class).setUserId(accessControl.getUser());
            } catch (final AccessControlException ace) {
                throw new IllegalArgumentException("Could not get initial "
                        + "token, unable to " + "run", ace);
            }
        } else {
            log.info("Access control is disabled");

            accessControl = null;
        }

        try
        {
            handler = new UplinkStatusMessageServiceBridge(appContext, sessionConfig, log);

            // Only expect the one message type here
            bus.subscribe(CommandMessageType.UplinkStatus,  handler);

            usp = new UplinkStatusPublisher(appContext, log);

            usp.start();

            log.info("Uplink status publisher started");        }
        catch (final RuntimeException re)
        {
            log.error("Uplink status publisher did not start: " + re);

            throw re;
        }
        catch (final Exception e)
        {
            log.error("Uplink status publisher did not start: " + e);
        }
    }

    /**
     * Start the interface to the session database store
     *
     * @param alsoLog
     *            If true start log as well as command
     */
    private void startTestConfigDatabase(final boolean alsoLog) {
        if (archiveController == null) {
        	archiveController = appContext.getBean(IDbSqlArchiveController.class);
        }

        archiveController.addNeededStore(ICommandMessageLDIStore.STORE_IDENTIFIER);
        // need the command update store to finalize.
        archiveController.addNeededStore(ICommandUpdateStore.STORE_IDENTIFIER);

        if (alsoLog) {
            archiveController.addNeededStore(ILogMessageLDIStore.STORE_IDENTIFIER);
        }
    }

    /**
     * Add stop of status publisher.
     */
    private void stopExternalInterfaces()
    {
        if (archiveController != null) {
            archiveController.shutDown();
        }
        archiveController=null;
        try
        {
            if (usp != null)
            {
                usp.stop();

                log.info("Uplink status publisher stopped");

                bus.unsubscribeAll(handler);
                handler.shutdown();
            }
        }
        catch (final RuntimeException re)
        {
            log.error("Uplink status publisher did not stop: " + re);

            throw re;
        }
        catch (final Exception e)
        {
            log.error("Uplink status publisher did not stop: " + e);
        }
        finally
        {
            usp = null;
        }

    }


    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.cli.legacy.app.CommandLineApp#configure(
     *      org.apache.commons.cli.CommandLine)
     */
    @Override
    @SuppressWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public void configure(final CommandLine commandLine)
            throws ParseException
    {
        ReservedOptions.parseHelp(commandLine, this);

        ReservedOptions.parseVersion(commandLine);


        // Set a dummy test session

        sessionConfig.getContextId().setNumber(0L);
        
        if (commandLine.hasOption(LOG_FILE_LONG))
        {
            logFile = StringUtil.safeTrim(
                    commandLine.getOptionValue(LOG_FILE_LONG, null));
        }
        if (logFile == null)
        {
            logFile = DEFAULT_LOG_FILE;
        }
        // MDC.put(LoggingConstants.FILE_ROUTING_KEY,
        // LoggingConstants.FILE_ROUTING_APPLICATION);
        MDC.put(LoggingConstants.FILE_APP_LOG_PATH, logFile);
        log = TraceManager.getTracer(appContext, Loggers.UPLINK);

        appContext.getBean(AccessControlParameters.class).setUserRole(CommandUserRole.VIEWER);

        appContext.getBean(AccessControlParameters.class).setLoginMethod(LoginEnum.KEYTAB_FILE);

        appContext.getBean(IConnectionMap.class).createFswUplinkConnection(
                UplinkConnectionType.COMMAND_SERVICE);

//        ACCESS_CONTROL_CONFIG.setDefaultRole(CommandUserRole.VIEWER);
//
//        ACCESS_CONTROL_CONFIG.setDefaultCliAuthMode(LoginEnum.keytabFile);

        keytabFile = null;
        
        final SecurityProperties securityProps = appContext.getBean(SecurityProperties.class);

        // command line argument has priority
        if (commandLine.hasOption(KEYTAB_FILE_LONG)) {
            keytabFile = StringUtil.safeTrim(
                               commandLine.getOptionValue(
                                   KEYTAB_FILE_LONG,
                                   null));

            if (keytabFile.isEmpty() ||
                ! (new File(keytabFile)).exists())
            {
                throw new ParseException("--" + KEYTAB_FILE_LONG +
                                         " '" + keytabFile +
                                         "' does not exist");
            }

            log.debug("Using user-provided keytab file '" + keytabFile +
                      "'");
        } else {
        	//if no command line argument, check for configured value
            final String  defaultFile    = StringUtil.safeTrim(securityProps.getDefaultDaemonKeytabFile());
            final boolean defaultExists  = (! defaultFile.isEmpty() &&
                                            (new File(defaultFile)).exists());

            if (! defaultExists)
            {
                if (! defaultFile.isEmpty())
                {
                	throw new ParseException("Default keytab file '" +
                                             defaultFile +
                                             "' does not exist");
                }

                throw new ParseException("No default keytab file");
            } else {
                keytabFile = defaultFile;

                log.debug("Using configured keytab file '" + keytabFile +
                          "'");
            }
        }

        // added username option for keytab
        if (keytabFile.isEmpty()) {
            throw new ParseException("No keytab file provided");
        }
        
       // ACCESS_CONTROL_CONFIG.setDefaultKeytabFile(keytabFile);
        
        if (LOGIN_METHOD == LoginEnum.KEYTAB_FILE) {
        	//command line argument has priority
	        if (commandLine.hasOption(USERNAME_LONG))
	        {
	        	username = ReservedOptions.parseUsername(commandLine);
		        if (username.isEmpty()) {
		            throw new ParseException("No username specified");
		        }
	        }
	        //if no command line argument, check for configured value
	        else {
	        	username = securityProps.getDefaultDaemonUsername();
	        	
	        	if (username.isEmpty()) {
	        		throw new ParseException("No username specified in configuration");
	        	}
	        }
        }

        ReservedOptions.parseDatabaseHost(commandLine,false);
        ReservedOptions.parseDatabasePort(commandLine,false);
        ReservedOptions.parseDatabaseUsername(commandLine,false);
        ReservedOptions.parseDatabasePassword(commandLine,false);
        ReservedOptions.parseFswUplinkHost(commandLine,false);
        ReservedOptions.parseFswUplinkPort(commandLine,false);

        final IUplinkConnection hc = sessionConfig.getConnectionConfiguration().getFswUplinkConnection();

        if (hc.getHost() == null)
        {
            // Not specified via command line, get configured

            final String host = StringUtil.emptyAsNull(
                                    appContext.getBean(ConnectionProperties.class).getDefaultUplinkHost(false));
            if (host == null)
            {
                throw new ParseException("No FSW uplink host provided or configured");
            }

            hc.setHost(host);
        }

        if (hc.getPort() == HostPortUtility.UNDEFINED_PORT)
        {
            // Not specified via command line, get configured

            final int port =  appContext.getBean(ConnectionProperties.class).getDefaultUplinkPort(false);

            if (port == HostPortUtility.UNDEFINED_PORT)
            {
                throw new ParseException("No FSW uplink port provided or configured");
            }

            hc.setPort(port);
        }

    }

    /**
     * Uses a FileLock to ensure atomic read/write operations of the file. Opens the lock file as a
     * RandomAccessFile, which will create the file if it does not exist. If the file contains data,
     * each line is checked to see if it is a lock entry from a supporting application - currently
     * just CommandStatusPublisher. Each valid lock entry is checked to see if it indicates a process
     * is using the same host and port of this application. If it is, this function checks to see if
     * the process is still active and if it is, an exception is thrown. If not, the entry is deleted.
     * If the file is empty or no other process is using the same host and port, then this application's
     * lock information is stored in the file.
     * 
     * NOTE: RandomAccessFile instantiation must be made in order to request/obtain the lock.
     * resetLockFile() called in another CommandStatusPublisherApp or an external application
     * can delete the file between the RandomAccessFile and lock request. If this does happen,
     * we will attempt to open the RandomAccessFile a second time. If this fails, an exception is
     * thrown due to a highly unstable environment. 
     * 
     * Updated to assume calling script will pass pid as a property (-Dpid=$$). Note that this will
     * only work in unix-derived platforms (Mac and Linux).
     *
     * @throws IllegalStateException If already running
     * @throws IOException           If I/O error
     *
     * 01/07/19 - No longer sets file to be deleted automatically, uses a
     *          lock to support multiple applications checking file and adding entries.
     *          Entries now include host and port.
     *          Host and port of processes are verified instead of just the presence of anything. 
     */
    private void checkForAlreadyRunning()
        throws IllegalStateException, IOException, InterruptedException
    {
    	FileLock lock = null;
    	final File lockFile = new File(LOCK_FILE);
    	
    	final Pattern p = getPattern();
    	String line = null;
		final StringBuilder allLines = new StringBuilder();
		
		int retry = 2;
        
        final Date now = new Date();
    	final String pid = GdsSystemProperties.getPid();
        /** Use host/port from current uplink connection, not default config */
        final String host = sessionConfig.getConnectionConfiguration().getUplinkConnection().getHost();
        final int port = sessionConfig.getConnectionConfiguration().getUplinkConnection().getPort();
    	
    	final String logString = "process started at "+ now + " pid=" + pid + " host=" + host + " port="
    	                                        + port + System.lineSeparator();
		while (retry > 0) {
			try (RandomAccessFile file = new RandomAccessFile(lockFile, "rw")) {
				lock = getLock(file);
				if (lockFile.exists()) {
					if (lockFile.length() > 0) {
						log.warn("lock file has entries, checking to verify current setup not being used.");
						line = file.readLine();
						while (line != null) {
							// if it has to do with our target host & port we either need to stop this
							// process or remove the old line
							final Matcher m = p.matcher(line);
							if (m.matches() && m.group(2).equalsIgnoreCase(host)
									&& m.group(3).equals(String.valueOf(port))) {
								final String oldpid = m.group(1);
								// See if that pid is still running
								final String[] args = { "ps", "-fp", oldpid };
								final Process ps = Runtime.getRuntime().exec(args);
								final BufferedReader bri = new BufferedReader(new InputStreamReader
										                                    (ps.getInputStream()));
								ps.waitFor();
								bri.readLine(); // header. ignore.
								final String psinfo = bri.readLine();
								log.warn("exiting process info: " + psinfo);
								if (psinfo != null) {
									// process is still up and running
									throw new IllegalStateException(
											"Lock file indicates a process is currently using the "
											+ "requested host and port. pid=" + oldpid);
								}
								// process seems to have vanished
								bri.close();
								log.warn(
										"...but that process is no longer running, so we will remove"
										+ " the old line from the file...");
							}
							// not interfere with this process, keep it
							else {
								allLines.append(line);
								allLines.append(System.lineSeparator());
							}
							line = file.readLine();
						}
					} else {
						log.info("checked for previous instance of process. none found.");
					}
					// update the file
					allLines.append(logString);
					file.setLength(0);
					file.writeBytes(allLines.toString());
					retry = 0;
				}
				/*
				 * lock file was deleted between instantiating the RAF and acquiring the lock.
				 * Another process shouldn't be able to start up and shutdown in the time that
				 * it takes this to loop once more.
				 */
				else {
					log.warn(
							"Lock file was deleted after link to file established, but before lock "
							+ "was acquired. Retrying...");
					retry--;
				}
				// if it does somehow happen that the file isn't there on a second time, we
				// shouldn't be relying on this lock file...
				if (!lockFile.exists() && retry == 0) {
					throw new IOException("Could not add this status publisher to the lock file");
				}
			} finally {
				releaseLock(lock);
			}
		}
	}
    
    /**
     * When this application instance is done, we need to open the lock file and remove the corresponding entry.
     * All other data in the file is retained. If the lock file would be empty after removing this application's
     * entry, then the lock file is deleted.
     * It is assumed the calling script will pass pid as a property (-Dpid=$$). Note that this will
     * only work in unix-derived platforms (Mac and Linux).
     * @throws IOException           If I/O error
     * @throws InterruptedException  If interrupted
     */
    private void resetLockFile() throws IOException, InterruptedException {
    	FileLock lock = null;
    	final File lockFile = new File(LOCK_FILE);
    	
    	final Pattern p = getPattern();
    	String line = null;
		final StringBuilder allLines = new StringBuilder();
    	
		
    	if(lockFile.exists()) {
    		try (RandomAccessFile file = new RandomAccessFile(lockFile, "rw")){
    			
    			lock = getLock(file);
    			//remove the line for this process, but keep the rest
    			line = file.readLine();
    			while(line != null) {
    				final Matcher m = p.matcher(line);
    				if(m.matches()  && m.group(1).equalsIgnoreCase(GdsSystemProperties.getPid())){
    					log.debug("Found this process in the file, removing it...");
    				}
    				else {
    					allLines.append(line);
                        allLines.append(System.lineSeparator());
    				}
    				line = file.readLine();
    			}
    			if(allLines.length() == 0){
    				Files.delete(lockFile.toPath());
    			}
    			else {
    				file.setLength(0);
    				file.writeBytes(allLines.toString());
    			}
    			
    		}
    		finally {
    			releaseLock(lock);
    		}
    	}
    }
    
    private Pattern getPattern() {
    	return Pattern.compile("^.*pid=(\\d+) host=([\\w-\\.]+) port=(\\d+)$");
    }
    
    private FileLock getLock(final RandomAccessFile file) throws InterruptedException, IOException{
    	final FileChannel f = file.getChannel();
		FileLock lock = f.tryLock();
		
		while(lock == null) {
			Thread.sleep(MILLISECOND);
			lock = f.tryLock();
		}
		
		return lock;
    }
    
    private void releaseLock(final FileLock lock) throws IOException {
    	if(lock != null && lock.isValid()) {
			lock.release();
		}
    }


    /**
     * Getter for usp.
     *
     * @return Uplink status publisher
     */
    private UplinkStatusPublisher getUsp()
    {
        return usp;
    }
    
    private void runApp() throws BeansException, ICmdException {
    	
    	/*
         * Ping CPD Client on startup to test connection. This will throw an exception if there
         * is a problem with the SSL Handshake.
         */
		appContext.getBean(ICpdClient.class).pingCommandService();
		while (true) {
			SleepUtilities.checkedSleep(SLEEP_SEC * MILLISECOND);

			final UplinkStatusPublisher usp1 = this.getUsp();

			if ((usp1 == null) || !usp1.isConnected()) {
				break;
			}
		}       
    }


    /**
     * The command line interface.
     *
     * @param args The command line arguments
     */
    @SuppressWarnings("DM_EXIT")
    public static void main(final String[] args) {
    	
        final CommandStatusPublisherApp app = new CommandStatusPublisherApp();
        try {

            final CommandLine commandLine = ReservedOptions.parseCommandLine(args, app);

            app.configure(commandLine);

            app.startExternalInterfaces();
            
            app.runApp();

            TraceManager.getDefaultTracer().error("Not connecting to database, bail out");

            app.stopExternalInterfaces();
            
            app.resetLockFile();

            System.exit(1);
        } catch(final Exception e) {
            TraceManager.getDefaultTracer().error("Exception was encountered while running "
                    + ApplicationConfiguration.getApplicationName() + ": " + ExceptionTools.rollUpMessages(e));
            app.stopExternalInterfaces();
            System.exit(1);
        }
    }
}
