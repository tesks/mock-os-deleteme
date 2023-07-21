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
package jpl.gds.automation.inbox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;

/**
 * The DownlinkInboxConfig class encapsulates the configuration information and
 * accessors related to the AMPCS Downlink Inbox Monitor Processing.
 * 
 * @see jpl.gds.automation.inbox.DownlinkInboxMonitor
 */
public class InboxProperties extends GdsHierarchicalProperties
{

    private static final String PROPERTY_FILE = "inbox.properties";
    
    /**
     * Name of the main configuration property block for Inbox File
     * Processing.
     */
    public static final String PROPERTY_PREFIX = "inbox."; // Block

    /**
     * Optional: TraceSeverity (i.e. level of logging information). Acceptable
     * values are: "All", "Trace", "Debug", "Info", "User", "Warning", "Error",
     * "Fatal", or "Off"
     */
    public static final String TRACE_LEVEL = PROPERTY_PREFIX + "traceLevel"; // String

    /**
     * Name of the configuration property used to indicate inbox
     * information is defined.
     */
    public static final String INBOXES_EXIST = PROPERTY_PREFIX + "enableInboxProcessing"; // Boolean

    /**
     * List of 'inbox configurations' to monitor.
     */
    public static final String INBOX_CONFIGURATIONS = PROPERTY_PREFIX + "configurationList"; // CSV
    
    /**
     * Name of the configuration property which specifies the directory that
     * will contain the resulting log files.
     */
    public static final String LOG_FILE_LOCATION = PROPERTY_PREFIX + "logFileLocation"; // String

    /**
     * Name of the configuration property which specified the name of the log
     * file to be created and stored in the LOG_FILE_LOCATION.
     */
    public static final String LOG_FILE_NAME = PROPERTY_PREFIX + "logFileName"; // String

    /**
     * Name of the configuration property which specifies the directory that's
     * to be monitored for incoming Data and Trigger files.
     */
    public static final String DIRECTORY_TO_MONITOR_SUFFIX = ".directoryToMonitor"; // String

    /**
     * Name of the configuration property which specifies the suffix GDS adds to
     * the input file. The existence of which indicates the data file is ready
     * for processing.
     */
    public static final String TRIGGER_FILE_EXTENSION_SUFFIX = ".triggerFileExtension"; // String                                                                             // Item

    /**
     * Name of the configuration property which specifies number of milliseconds
     * for the monitor process to wait (sleep) between checks for data-trigger
     * files.
     */
    public static final String MILLSEC_BETWEEN_DATA_CK_SUFFIX = ".millsecBetweenDataChecks"; // long

    /**
     * Name of the configuration property which specifies number of milliseconds
     * for the monitor process to wait (sleep) between checks for data-trigger
     * files.
     */
    public static final String MILLSEC_BETWEEN_HEARTBEAT_SUFFIX = ".millsecBetweenHeartbeat"; // long

    /**
     * Name of the configuration property which specifies the downlink command
     * parameters used when chill_down is invoked.
     */
    public static final String DOWNLINK_ARGS_SUFFIX = ".downlinkArgs"; // block Item
    
    private final String valMsg = " Value ";
    private final String noCfgMsg = " not configured, Using:";
    
    private static final String DEFAULT_DIRECTORY = "./";
    private static final String DEFAULT_LOG_FILE = "inbox_monitor.log";

    /**
     * default value of 3 seconds
     */
    public static final long THREE_SECONDS = 3000L;

    /**
     * default value of 1 minute
     */
    public static final long ONE_MINUTE = 60000L;

    private Tracer log;

    private boolean inboxesDefined = false;
    private String directoryOfLogFile;
    private String logFileName = null;
    private TraceSeverity traceLevel = null;
    private String gdsDirectory = null;

    private String[] configBlockNames = null;

    private final List<RunConfig> runConfigurations = new ArrayList<RunConfig>();

    private boolean configured = true;

    private final String procName = ApplicationConfiguration.getApplicationName() + ": ";

    /**
     * Constructor that loads the default property file, which will be found
     * using standard configuration search.
     */
    public InboxProperties()
    {
        super(PROPERTY_FILE, true);

    }

    public void init() {
        this.gdsDirectory = GdsSystemProperties.getGdsDirectory();

        setDirectoryOfLogFile(getProperty(LOG_FILE_LOCATION, DEFAULT_DIRECTORY));

        // setup Logging information
        setLogFileName(getProperty(LOG_FILE_NAME, DEFAULT_LOG_FILE));

        final File logDir = new File(getDirectoryOfLogFile());
        if (logDir.exists())
        {
            final String pathLogName = getDirectoryOfLogFile() + File.separator
                    + getLogFileName();
            backUpLog(pathLogName);
            // MDC.put(LoggingConstants.FILE_APP_LOG_PATH, pathLogName);

            log = TraceManager.getTracer(Loggers.CONFIG);

        } else
        {
            log = TraceManager.getTracer(Loggers.CONFIG);
            log.error(procName + "Specified Station LOG directory ("
                    + getDirectoryOfLogFile()
                    + ") doesn't exist.  Process will terminate");
            configured = false;
        }
        log.info(procName + valMsg + LOG_FILE_LOCATION + noCfgMsg + getDirectoryOfLogFile());
        log.info(procName + valMsg + LOG_FILE_NAME + noCfgMsg + getLogFileName());

        String ts;
        if ((ts = getProperty(TRACE_LEVEL)) != null)
        {
            try
            {
                traceLevel = TraceSeverity.fromStringValue(ts);
            } catch (final IllegalArgumentException e)
            {
                log.error(procName
                        + "Specified trace-level (severity of msg output) is invalid: "
                        + ts);
                traceLevel = null;
            }

            if (traceLevel != null)
            {
                TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(traceLevel);
            }
        }

        inboxesDefined = getBooleanProperty(INBOXES_EXIST,false);
        if (!inboxesDefined)
        {
            log.error(procName + " Value: " + INBOXES_EXIST
                    + " is set to false.");
            log.error(procName
                    + "To configure this application, you must enable inbox usage in your project configuration file " +
                    " by setting " + INBOXES_EXIST + " to true and also define inbox names and their associated directories");
            log.error(procName + "Will now terminate");
            configured = false;
            return;
        }

        final List<String> temp = getListProperty(INBOX_CONFIGURATIONS, null, ",");
        if (temp.isEmpty())
        {
            log.error(procName + " Value: " + INBOX_CONFIGURATIONS
                    + " does not exist.  No inboxes are defined. This process will terminate");

            configured = false;
            inboxesDefined = false;
        }
        configBlockNames = temp.toArray(new String[] {});
    }

    /**
     * @return TraceSeverity object or null
     */
    public TraceSeverity getTraceLevel()
    {
        return this.traceLevel;
    }

    /**
     * The directory to place the log file in
     * 
     * @return the directorOfLogFile
     */
    public String getDirectoryOfLogFile()
    {
        return this.directoryOfLogFile;
    }
    
    private void setDirectoryOfLogFile(final String directoryOfLogFile)
    {
        if (directoryOfLogFile == null)
        {
            this.directoryOfLogFile = DEFAULT_DIRECTORY;
        } else
        {
            this.directoryOfLogFile = directoryOfLogFile;
        }
    }

    /**
     * The name of the process log file to create
     * 
     * @return the logFileName
     */
    public String getLogFileName()
    {
        return this.logFileName;
    }
    
    private void setLogFileName(final String logFileName){
        if (logFileName == null)
        {
            this.logFileName = "inboxMon.log";
        } else
        {
            this.logFileName = logFileName;
        }
    }

    /**
     * Create a backup copy of the file if it exists. Note: only 1 cp deep is
     * maintained. Note II: doesn't verify permission
     * 
     * @param logAndPath
     */
    private void backUpLog(final String logAndPath)
    {
        final File res = new File(logAndPath);

        if (res.exists())
        {
            final String newName = new StringBuffer(logAndPath).append(".bkup")
                    .toString();

            // clobber
            if (!(res.renameTo(new File(newName))))
            {
                log.error(procName + "Backup of an existing log file ("
                        + logAndPath + ") was not succesful");
            }
        }

    }

    /**
     * Load the Downlink Inbox Monitor values from the configuration files.
     * @param whichConfigBlock index of the configuration block to be loaded.
     * 
     * @return null or configured RunConfig object
     */
    public RunConfig load(final int whichConfigBlock)
    {
        if (!isConfigured())
        {
            return null;
        }

        final RunConfig rc = new RunConfig(configBlockNames[whichConfigBlock]);

        runConfigurations.add(rc);

        if (!rc.isValidConfiguration())
        {
            return null;
        }

        return rc;
    }

    /**
     * @return true if fully configured otherwise false
     */
    private boolean isConfigured()
    {
        final boolean res = configured;
        if (!res)
        {
            log.error(procName + "Not configured");
        }
        return res;
    }

    /**
     * @param
     * @return values of monitorList
     */
    public String[] getMonitorList()
    {
        return this.configBlockNames;
    }

    /**
     * Flag used to indicate the Downlink Inbox Monitor fields have been defined.
     * 
     * @return the inboxesDefined
     */
    public boolean isInboxesDefined()
    {
        return this.inboxesDefined;
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    /**
     * class to support individual run configurations (i.e. inbox configuration)
     * each occurrence of DownlinkInboxMonitor will have
     * it's own instance of this class w/ run configuration related to that monitoring
     * session
     *
     */
    public class RunConfig
    {
        private String directoryToMonitor = null;
        private String processDataTriggerId = null;
        private long msBetweenDataCk = THREE_SECONDS;
        private long msBetweenHeartbeat = ONE_MINUTE;
        private String chillDownParams = null;

        private boolean configurationValid = true;

        private String activeConfigBlockName = null; // relates to load(#), from
                                                     // inboxConfigurationList
        
        private final String prependProperty = null;
        
        private final String defaultPDTI = ".done";

        /**
         * Load the Downlink Inbox Monitor values from the configuration files.
         * @param configBlockName name of the configuration block to be utilized
         */
        public RunConfig(final String configBlockName)
        {
            activeConfigBlockName = configBlockName;
            
            final String prependProperty = PROPERTY_PREFIX + activeConfigBlockName;

            setChillDownParams(getProperty(prependProperty + DOWNLINK_ARGS_SUFFIX));
            if ( getChillDownParams() == null)
            {
                log.error(procName + valMsg + prependProperty
                        + DOWNLINK_ARGS_SUFFIX
                        + " not configured.  This process will terminate");
                configurationValid = false;

            }

            setDirectoryToMonitor(getProperty(prependProperty  + DIRECTORY_TO_MONITOR_SUFFIX));
            setProcessDataTriggerId(getProperty(prependProperty + TRIGGER_FILE_EXTENSION_SUFFIX));
            setTimeBetweenTriggerChecks(getTimeProperty(prependProperty + MILLSEC_BETWEEN_DATA_CK_SUFFIX));
            setTimeBetweenHeartbeats(getTimeProperty(prependProperty + MILLSEC_BETWEEN_HEARTBEAT_SUFFIX));

        }

        private long getTimeProperty(final String propertyName){
            long tempVal;
            try
            {
                tempVal = getLongProperty(propertyName, -1L);
            } catch(final Exception e)
            {
                tempVal = -1;
            }
            return tempVal;
        }

        /**
         * @return the activeConfigBlockName
         */
        public String getActiveConfigBlockName()
        {
            return this.activeConfigBlockName;
        }

        /**
         * @param blockName the block name to be set as active
         */
        public void setActiveConfigBlockName(final String blockName)
        {
            this.activeConfigBlockName = blockName;
        }

        /**
         * Basic 'Getter'
         * 
         * @return number of milliseconds to wait between data-trigger-file
         *         checks, or -1 if there is an error in the configuration
         */
        public long getTimeBetweenTriggerChecks()
        {
            return this.msBetweenDataCk;
        }

        /**
         * Basic 'Setter'
         * 
         * @param msBetweenDataCk
         *            value
         */
        private void setTimeBetweenTriggerChecks(final long msBetweenDataCk)
        {
            
            if ( msBetweenDataCk < 0)
            {
                this.msBetweenDataCk = THREE_SECONDS;
                log.info(procName + valMsg + prependProperty
                        + MILLSEC_BETWEEN_DATA_CK_SUFFIX + noCfgMsg
                        + getTimeBetweenTriggerChecks());
            } else {
                this.msBetweenDataCk = msBetweenDataCk;
            }
        }

        /**
         * Basic 'Getter'
         * 
         * @return number of milliseconds to wait between heartbeats, or -1 if
         *         there is an error in the configuration
         */
        public long getTimeBetweenHeartbeats()
        {
            return this.msBetweenHeartbeat;
        }

        /**
         * Basic 'Setter'
         * 
         * @param msBwetweenHeartbeat
         *            value
         */
        private void setTimeBetweenHeartbeats(final long msBetweenHeartbeat)
        {
            if ( msBetweenHeartbeat < 0)
            {
                this.msBetweenHeartbeat = ONE_MINUTE;
                log.info(procName + valMsg + prependProperty
                        + MILLSEC_BETWEEN_HEARTBEAT_SUFFIX + noCfgMsg
                        + getTimeBetweenHeartbeats());
            } else
            {
                this.msBetweenHeartbeat = msBetweenHeartbeat;
            }
        }

        /**
         * Determines if the enabling of features was set from the configuration
         * file.
         * 
         * @return configurationValid true if successful construction, false
         *         otherwise.
         */
        public boolean isValidConfiguration()
        {
            return this.configurationValid;
        }

        /**
         * The directory to monitor for the incoming Data and trigger files
         * 
         * @return the directoryToMonitor
         */
        public String getDirectoryToMonitor()
        {
            return this.directoryToMonitor;
        }

        /**
         * Basic 'Setter'
         * 
         * @param directoryToMonitor
         *            the directoryToMonitor to set
         */
        private void setDirectoryToMonitor(final String monitor)
        {
            if(monitor == null){
                this.directoryToMonitor = DEFAULT_DIRECTORY;
                log.info(procName + valMsg + prependProperty
                        + DIRECTORY_TO_MONITOR_SUFFIX + noCfgMsg
                        + getDirectoryToMonitor());
            } else
            {
                this.directoryToMonitor = monitor;
            }
        }

        /**
         * The file extension used to flag when the Data file is in the monitor
         * directory and is complete
         * 
         * @return the processDataTrigerID
         */
        public String getProcessDataTriggerId()
        {
            return this.processDataTriggerId;
        }

        /**
         * Basic 'Setter'
         * 
         * @param processDataTriggerId
         */
        private void setProcessDataTriggerId(final String triggerId)
        {
            if (triggerId == null)
            {
                this.processDataTriggerId = defaultPDTI;
                log.info(procName + valMsg + prependProperty
                        + TRIGGER_FILE_EXTENSION_SUFFIX + noCfgMsg
                        + getProcessDataTriggerId());
            } else
            {
                this.processDataTriggerId = triggerId;
            }
            
        }

        /**
         * @return the gdsDirectory
         */
        public String getGdsDirectory()
        {
            return gdsDirectory;
        }

        /**
         * The parameters which are used to the chill_down process
         * 
         * @return the chillDownParams
         */
        public String getChillDownParams()
        {
            return this.chillDownParams;
        }

        /**
         * Basic 'Setter'
         * 
         * @param chillDownParams
         *            the chillDownParams to set
         */
        private void setChillDownParams(final String chillParams)
        {
            this.chillDownParams = chillParams;
        }

        /**
         * Get access to the process log mechanism
         * @return the logger being used
         */
        public Tracer getLogger()
        {
            return log;
        }

        /**
         * Get the log directory from DownlinkInboxConfig
         * @return the directory containing the log file
         */
        public String getLoggerDir()
        {
            return getDirectoryOfLogFile();
        }

    } // eoc run configuration

} // eoc DownlinkInboxConfig

