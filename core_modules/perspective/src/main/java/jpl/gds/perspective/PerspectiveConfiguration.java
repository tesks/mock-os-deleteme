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
package jpl.gds.perspective;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.SAXParser;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;


/**
 * Defines the applications in the user's saved perspective and stores the
 * configuration for each application. Note that each application within a
 * perspective configuration must have a uniquely identifying title.
 * 
 */
public class PerspectiveConfiguration implements LockableElement {

    /**
     * Name of the perspective file
     */
    public final String perspectiveFileName;
    
    
    private static final String UPLINK_DISPLAY_FILE = "UplinkDisplay.xml";
    private static final String MONITOR_DISPLAY_FILE = "MonitorDisplay.xml";
    private static final String FSW_DOWN_DISPLAY_FILE = "FswDownDisplay.xml";
    private static final String SSE_DOWN_DISPLAY_FILE = "SseDownDisplay.xml";
    private static final String UPLINK_APP_FILE = "UplinkConfig.xml";
    
    /* Add constants for height and width of
     * various applications in order to decouple from other packages.
     */

    /**
     * Default width of the downlink window.
     */
    public static final int DEFAULT_DOWNLINK_WIDTH = 680;
    /**
     * Default height of the downlink window.
     */
    public static final int DEFAULT_DOWNLINK_HEIGHT = 460;
    /**
     * Default width of the monitor window.
     */
    public static final int DEFAULT_MONITOR_WIDTH = 660;
    /**
     * Default height of the monitor window.
     */
    public static final int DEFAULT_MONITOR_HEIGHT = 400;
    /**
     * Default width of the uplink window.
     */
    public static final int DEFAULT_UPLINK_WIDTH = 875;
    /**
     * Default height of the uplink window.
     */
    public static final int DEFAULT_UPLINK_HEIGHT = 1000;


    /**
     * Name of the monitor configuration XML file
     */
    public static final String MONITOR_APP_FILE = "MonitorConfig.xml";
    private static final String FSW_DOWN_APP_FILE = "FswDownConfig.xml";
    private static final String SSE_DOWN_APP_FILE = "SseDownConfig.xml";
    private static final String BACKUP_DIR_NAME = "backup";

    private static Tracer log;
//    private static PerspectiveConfiguration instance = null;
    private final String defaultPerspective;
    private static boolean useDefaults = true;
//    private static String instanceLock = "Lock";

    private String appId;
    private boolean locked;

    private String perspectiveDirectory;
    private final Vector<String> appNames = new Vector<String>();
    private final Vector<ApplicationConfiguration> configs = new Vector<ApplicationConfiguration>();

    private final PerspectiveProperties perspectiveProps;


	private final ApplicationContext appContext;
    

//    /**
//     * Sets the instance of this perspective configuration to null
//     */
//    public static void resetInstance() {
//        synchronized (instanceLock) {
//            instance = null;
//        }
//    }

    public PerspectiveConfiguration(final ApplicationContext appContext) {
    	this.appContext = appContext;
    	this.perspectiveProps = appContext.getBean(PerspectiveProperties.class); 
        log = TraceManager.getDefaultTracer(appContext);
      	defaultPerspective = perspectiveProps.getDefaultPerspectiveName();
        perspectiveFileName = perspectiveProps.getDefaultPerspectiveFile();
    }
    
    public void create() {
         create(null);
    }

    public void create(final String path) {
    	
        if (path != null) {
            this.perspectiveDirectory = path;
        } else {
            this.perspectiveDirectory = GdsSystemProperties.getUserConfigDir() + File.separator + defaultPerspective;
        }
        
       
        final String perspectiveLocation = this.perspectiveDirectory
                + File.separator + perspectiveFileName;

        // First check if file exists
        final File file = new File(perspectiveLocation);
        if (!file.exists()) {
            try {
                createPerspective(perspectiveLocation);
            } catch (final IOException e) {
                e.printStackTrace();
                log.error("Perspective load/create failed for perspective "
                        + perspectiveLocation);
                throw new RuntimeException(
                        "Perspective load/creation failed for perspective "
                                + perspectiveLocation);
            }
        } else {
            final boolean ok = load(appContext, perspectiveLocation);
            if (!ok) {
                log.error("Perspective load/create failed for perspective "
                        + perspectiveLocation);
                throw new RuntimeException(
                        "Perspective load failed for perspective "
                                + perspectiveLocation);
            }
        }
        
     
    }

//    /**
//     * Create the single instance of the Perspective configuration using the
//     * given override parameters. This method will throw if called more than
//     * once. Use getInstance() without arguments to get the existing instance.
//     * 
//     * @param path
//     *            the path where the new perspective configuration will be
//     *            created
//     * @see getInstance()
//     * @return new instance of a perspective configuration
//     */
//    public static PerspectiveConfiguration create(ApplicationContext appContext, final String path) {
//        // You can only call this method if an instance has never been created.
//        synchronized (instanceLock) {
//            if (instance != null) {
//                throw new IllegalStateException(
//                        "Perspective already created with previous parameters");
//            }
//            instance = new PerspectiveConfiguration(appContext, path);
//        }
//        return instance;
//    }

    /**
     * Create the single instance of the Perspective configuration using the
     * given override parameters. This method will throw if called more than
     * once. Use getInstance() without arguments to get the existing instance.
     * 
     * @param path
     *            the path where the application ID file lives
     * @see getInstance()
     * @return new instance of a perspective configuration
     */
    public void createFromApplicationIdFile(final String path) {

    	try {
    		loadAppId(path);
    	} catch (final IOException e) {

    	}
    	final String perspectiveLocation = perspectiveDirectory
    			+ File.separator + perspectiveFileName;
    	final boolean ok = load(appContext, perspectiveLocation);
    	if (!ok) {
    		log.error("Perspective load/create failed for perspective "
    				+ perspectiveLocation);
    		throw new RuntimeException(
    				"Perspective load failed for perspective "
    						+ perspectiveLocation);
    	}
    }

    /**
     * Checks if the perspective exists at the given path
     * 
     * @param path
     *            the path in which to look for the perspective file
     * @return true if the perspective exists, false otherwise
     */
    public static boolean perspectiveExists(final PerspectiveProperties props, final String path) {
        if (path == null) {
            return false;
        }

        final String perspectiveLocation = path + File.separator
                + props.getDefaultPerspectiveFile();

        // First check if file exists
        final File file = new File(perspectiveLocation);
        return file.exists();
    }

//    /**
//     * Gets (and creates if necessary) the single static instance of this class.
//     * If it is created, it will be created with default parameters.
//     * 
//     * @return the single static PerspectiveConfiguration instance
//     */
//    public static PerspectiveConfiguration getInstance() {
//        synchronized (instanceLock) {
//            if (instance == null) {
//                instance = new PerspectiveConfiguration(null);
//            }
//        }
//        return instance;
//    }

    /**
     * Creates a new set of perspective configuration files.
     * 
     * @param perspectiveLocation
     *            the full path to the user's perspective directory
     */
    private void createPerspective(final String perspectiveLocation)
            throws IOException {
        ApplicationConfiguration[] apps = null;
        if (useDefaults) {
            apps = getDefaults();
        } else {
            apps = getEmptyDefaults();
        }
        for (int index = 0; index < apps.length; index++) {
            apps[index].setConfigPath(getConfigPath());
            this.appNames.add(apps[index].getConfigFilename());
            this.configs.add(apps[index]);
        }
        this.assignNewAppId();
        this.save();
    }

    /**
     * Creates a list of application configurations with one place holder
     * application in it.
     * 
     * @return an array of ApplicationConfiguration objects
     */
    private ApplicationConfiguration[] getEmptyDefaults() {
        final ApplicationConfiguration[] configs = new ApplicationConfiguration[1];
        configs[0] = new ApplicationConfiguration(appContext);
        configs[0].setConfigFile("AppConfig1.xml");
        return configs;
    }

    /**
     * Loads an existing set of perspective configuration files.
     * 
     * @param userPerspective
     *            the full path to the user's perspective file.
     * @return true if the perspective was loaded successfully
     */
    private boolean load(final ApplicationContext appContext, final String userPerspective) {
        SAXParser sp = null;
        try {
            final File perFile = new File(userPerspective);
            if (!perFile.exists()) {
                log.error("User perspective " + userPerspective + " not found");
                return false;
            }
            final PerspectiveConfigSaxHandler tcSax = new PerspectiveConfigSaxHandler(appContext);

            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(userPerspective, tcSax);
            if (this.appId != null) {
                final Enumeration<ApplicationConfiguration> e = this.configs
                        .elements();
                while (e.hasMoreElements()) {
                    e.nextElement().setApplicationId(this.appId);
                }
            }
            return true;
        } catch (final SAXException e) {
            log.error("Unable to parse perspective configuration file "
                    + userPerspective);
            log.error(e.getMessage());
            return false;
        } catch (final Exception e) {
            e.printStackTrace();
            log.error("Unable to parse perspective configuration file "
                    + userPerspective);
            log.error("Unexpected error: " + e.toString());
            return false;
        }
    }

    /**
     * Return all of the application configurations associated with this
     * perspective configuration.
     * 
     * @return an Array of ApplicationConfiguration objects
     */
    public ApplicationConfiguration[] getApplicationConfigurations() {
        ApplicationConfiguration[] result = null;
        if (this.configs.size() != 0) {
            result = new ApplicationConfiguration[this.configs.size()];
            this.configs.copyInto(result);
        } else {
            log.debug("Perspective configuration is getting config defaults");
            result = getDefaults();
        }
        return result;
    }

    /**
     * Gets the application configuration associated with the given application
     * type
     * 
     * @param type
     *            refers to uplink, downlink, monitor
     * @return application configuration object
     */
    public ApplicationConfiguration getApplicationConfiguration(
            final ApplicationType type) {
        final Iterator<ApplicationConfiguration> it = this.configs.iterator();
        while (it.hasNext()) {
            final ApplicationConfiguration appConfig = it.next();
            if (appConfig.getApplicationType().equals(type)) {
                return appConfig;
            }
        }
        return null;
    }

    /**
     * Gets the application configuration associated with the given application
     * type
     * 
     * @param type
     *            refers to uplink, downlink, monitor
     * @return application configuration object
     */
    public ApplicationConfiguration getApplicationConfiguration(
            final ApplicationType type, final boolean fswOnly, final boolean sseOnly) {
    	if (fswOnly && sseOnly) {
    		throw new IllegalArgumentException("Both fswOnly and sseOnly cannot be set at the same time");
    	}
        final Iterator<ApplicationConfiguration> it = this.configs.iterator();
        while (it.hasNext()) {
            final ApplicationConfiguration appConfig = it.next();
            if (appConfig.getApplicationType().equals(type)) {
                if (!sseOnly && fswOnly && appConfig.isFswOnly()) {
                	return appConfig;
                }
                if (!fswOnly && sseOnly && appConfig.isSseOnly()) {
                	return appConfig;
                }
                if (!fswOnly && !sseOnly) {
                	return appConfig;
                }
                
            }
        }
        return null;
    }

    
    /**
     * Creates a set of default application configurations.
     * 
     * @return an array of ApplicationConfiguration objects
     */
    private ApplicationConfiguration[] getDefaults() {
        final List<String> apps = perspectiveProps.getDefaultApplications();

        final ApplicationConfiguration[] configs = new ApplicationConfiguration[apps.size()];
        for (int i = 0; i < configs.length; i++) {
            if (apps.get(i).equalsIgnoreCase("downlink")) {
                configs[i] = getDefaultFswDown();
                configs[i].setConfigFile(FSW_DOWN_APP_FILE);
            } else if (apps.get(i).equalsIgnoreCase("monitor")) {
                configs[i] = getDefaultMonitor();
                configs[i].setConfigFile(MONITOR_APP_FILE);
            } else if (apps.get(i).equalsIgnoreCase("sse_downlink")) {
                configs[i] = getDefaultSseDown();
                configs[i].setConfigFile(SSE_DOWN_APP_FILE);
            } else if (apps.get(i).equalsIgnoreCase("uplink")) {
                configs[i] = getDefaultUplink();
                configs[i].setConfigFile(UPLINK_APP_FILE);
            }
        }
        return configs;
    }

    /**
     * Creates the default application configuration for the Uplink application.
     * 
     * @return a ApplicationConfiguration object
     */
    private ApplicationConfiguration getDefaultUplink() {
        final ApplicationConfiguration config = new ApplicationConfiguration(appContext);
        config.setApplicationType(ApplicationType.UPLINK);
        final DisplayConfiguration displayConfig = new DisplayConfiguration();
        /* Use in-class constants for window height/width */
        displayConfig.setLocation(new ChillLocation(
                DEFAULT_DOWNLINK_WIDTH + 3, DEFAULT_MONITOR_HEIGHT + 13));
        displayConfig.setSize(new ChillSize(DEFAULT_UPLINK_WIDTH,
                DEFAULT_UPLINK_HEIGHT));
        displayConfig.setName("Uplink Window");
        displayConfig.setType(DisplayType.UPLINK);
        displayConfig.setConfigFile(UPLINK_DISPLAY_FILE);
        
        //we don't want a white default for uplink app, so set to an invalid color for uplink app to ignore
        displayConfig.setBackgroundColor(new ChillColor(-1,-1,-1));
        config.addDisplayConfiguration(displayConfig);
        return config;
    }

    /**
     * Creates the default application configuration for the monitor
     * application.
     * 
     * @return a ApplicationConfiguration object
     */
    private ApplicationConfiguration getDefaultMonitor() {
        final ApplicationConfiguration config = new ApplicationConfiguration(appContext);
        config.setApplicationType(ApplicationType.MONITOR);
        final DisplayConfiguration displayConfig = new DisplayConfiguration();
        /*  Use in-class constants for window size */
        displayConfig.setLocation(new ChillLocation(
                DEFAULT_DOWNLINK_WIDTH + 3, 10));
        displayConfig.setSize(new ChillSize(DEFAULT_MONITOR_WIDTH,
                DEFAULT_MONITOR_HEIGHT));
        displayConfig.setName("Monitor Window");
        displayConfig.setType(DisplayType.MESSAGE);
        displayConfig.setConfigFile(MONITOR_DISPLAY_FILE);
        config.addDisplayConfiguration(displayConfig);
        return config;
    }

    /**
     * Creates the default application configuration for the FSW downlink
     * application.
     * 
     * @return a ApplicationConfiguration object
     */
    private ApplicationConfiguration getDefaultFswDown() {
        final ApplicationConfiguration config = new ApplicationConfiguration(appContext);
        config.setFsw(true);
        config.setApplicationType(ApplicationType.DOWNLINK);
        final DisplayConfiguration displayConfig = new DisplayConfiguration();
        displayConfig.setLocation(new ChillLocation(0, 10));
        /* Use in-class constants for window size */
        displayConfig.setSize(new ChillSize(DEFAULT_DOWNLINK_WIDTH,
                DEFAULT_DOWNLINK_HEIGHT));
        displayConfig.setName("FSW Down Window");
        displayConfig.setType(DisplayType.FSW_DOWN);
        displayConfig.setConfigFile(FSW_DOWN_DISPLAY_FILE);
        config.addDisplayConfiguration(displayConfig);
        return config;
    }

    /**
     * Creates the default application configuration for the SSE downlink
     * window.
     * 
     * @return a ApplicationConfiguration object
     */
    private ApplicationConfiguration getDefaultSseDown() {
        final ApplicationConfiguration config = new ApplicationConfiguration(appContext);
        config.setSse(true);
        config.setApplicationType(ApplicationType.DOWNLINK);
        final DisplayConfiguration displayConfig = new DisplayConfiguration();
        /*  Use in-class constants for window size */
        displayConfig.setSize(new ChillSize(DEFAULT_DOWNLINK_WIDTH,
                DEFAULT_DOWNLINK_HEIGHT));
        displayConfig.setLocation(new ChillLocation(0,
                DEFAULT_DOWNLINK_HEIGHT + 13));
        displayConfig.setName("SSE Down Window");
        displayConfig.setType(DisplayType.SSE_DOWN);
        displayConfig.setConfigFile(SSE_DOWN_DISPLAY_FILE);
        config.addDisplayConfiguration(displayConfig);
        return config;
    }

    /**
     * Creates an XML representation of the perspective configuration
     * 
     * @return An XML representation of the perspective configuration file.
     */
    private String toXML() {
        final StringBuffer result = new StringBuffer();
        result.append("<PerspectiveConfiguration locked=\"" + this.locked
                + "\">\n");
        // For each display configuration, get output filename
        // and append it to the XML output
        final Enumeration<ApplicationConfiguration> it = this.configs
                .elements();
        while (it.hasMoreElements()) {
            final ApplicationConfiguration config = it.nextElement();
            final String file = config.getConfigFilename();
            result.append("	<appConfigFile>" + file + "</appConfigFile>\n");
        }
        result.append("</PerspectiveConfiguration>");
        return result.toString();
    }

    /**
     * Saves the current perspective to perspective files, recursively including
     * the application and display configurations. false if just filename
     * 
     * @throws IOException
     *             thrown if perspective directory cannot be created
     */
    public void save() throws IOException {

        // Save application configuration file
        final String saveFile = this.perspectiveDirectory + File.separator
                + perspectiveFileName;
        final File path = new File(saveFile);
        final String filePath = path.getParent();

        final File myPath = new File(filePath);
        if (!myPath.exists()) {
            if (!myPath.mkdirs()) {
                throw new IOException("Unable to create perspective directory "
                        + myPath.getAbsolutePath());
            }
        }
        FileWriter fos = null;
        try {
        	fos = new FileWriter(saveFile);
            fos.write("<?xml version=\"1.0\"?>\n");
            fos.write(this.toXML());
        } finally {
        	fos.close();
        }

        // Then save each application in configuration
        // For each application configuration, set output filename
        final Enumeration<ApplicationConfiguration> it = this.configs
                .elements();
        while (it.hasMoreElements()) {
            final ApplicationConfiguration config = it.nextElement();
            config.setConfigPath(getConfigPath());
            config.save();
        }
        writeAppId();
    }

    /**
     * Gets the application id file path
     * 
     * @return path of the form: /tmp/<user>/<appID>.properties
     */
    public String getAppIdFileName() {
        return "/tmp/" + GdsSystemProperties.getSystemUserName() + "/" + this.appId
                + ".properties";
    }

    /**
     * At least on Linux, the sequence "/./" in a directory path prevents mkdirs
     * from working, so I replace it with the equivalent "/".
     * 
     * @throws IOException
     *             thrown if the application id file cannot be found or created
     * 
     */
    public void writeAppId() throws IOException {
        final String applicationIdFile = getAppIdFileName();
        final File f = new File(applicationIdFile.replace("/./", "/"));
        final File fp = f.getParentFile();

        if (!fp.exists() && !fp.mkdirs()) {
            throw new IOException("Unable to create: " + fp.getPath());
        }

        try {
            SleepUtilities.fullSleep(1000);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        FileWriter fos = null;
        try {
        	fos = new FileWriter(applicationIdFile);
            fos.write(this.appId + "\n");
            fos.write(this.perspectiveDirectory + "\n");
        } finally {
            fos.close();
        }
    }

    private void loadAppId(final String path) throws IOException {
        final String applicationIdFile = path;
        final BufferedReader reader = new BufferedReader(new FileReader(
                new File(applicationIdFile)));
        this.appId = reader.readLine();
        this.perspectiveDirectory = reader.readLine();
        reader.close();
    }

    /**
     * 
     * PerspectiveConfigSaxHandler is the XML parse handler for the perspective
     * files.
     * 
     *
     */
    private class PerspectiveConfigSaxHandler extends DefaultHandler {
        protected StringBuffer text = new StringBuffer();
		private final ApplicationContext appContext;
        
        public PerspectiveConfigSaxHandler(final ApplicationContext appContext) {
        	this.appContext = appContext;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         * java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String qname, final Attributes attr) throws SAXException {
            this.text = new StringBuffer();
            if (qname.equalsIgnoreCase("PerspectiveConfiguration")) {
                final String lockStr = attr.getValue("locked");
                if (lockStr == null) {
                    PerspectiveConfiguration.this.locked = false;
                } else {
                    PerspectiveConfiguration.this.locked = XmlUtility
                            .getBooleanFromText(lockStr);
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        @Override
        public void characters(final char[] chars, final int start,
                final int length) throws SAXException {
            final String newText = new String(chars, start, length);
            if (!newText.equals("\n")) {
                this.text.append(newText);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
         * java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri, final String localName,
                final String qname) throws SAXException {
            if (qname.equals("appConfigFile")) {
                final String appFileName = this.text.toString();
                final ApplicationConfiguration appConfig = new ApplicationConfiguration(appContext);
                appConfig.setConfigPath(getConfigPath());
                final boolean ok = appConfig.load(appFileName);
                if (!ok) {
                    throw new SAXException(
                            "Error parsing application configuration "
                                    + appFileName + " in user perspective");
                }
                addAppConfiguration(appConfig);
                appConfig.setParentPerspective(PerspectiveConfiguration.this);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
         */
        @Override
        public void error(final SAXParseException e) throws SAXException {
            throw new SAXException(
                    "Parse error in perspective configuration file line "
                            + e.getLineNumber() + ", column "
                            + e.getColumnNumber() + ": " + e.getMessage());
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
         */
        @Override
        public void fatalError(final SAXParseException e) throws SAXException {
            throw new SAXException(
                    "Fatal parse error in perspective configuration file line "
                            + e.getLineNumber() + ", column "
                            + e.getColumnNumber() + ": " + e.getMessage());
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
         */
        @Override
        public void warning(final SAXParseException e) {
            log.warn("Parse warning in perspective configuration file line "
                    + e.getLineNumber() + ", column " + e.getColumnNumber()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Gets the path to the perspective files.
     * 
     * @return the directory path
     */
    public String getConfigPath() {
        return this.perspectiveDirectory;
    }

    /**
     * Sets the path to the perspective files.
     * 
     * @param perspectivePath
     *            the directory path
     */
    public void setConfigPath(final String perspectivePath) {
        this.perspectiveDirectory = perspectivePath;
    }

    /**
     * Adds an application configuration to the perspective.
     * 
     * @param config
     *            the ApplicationConfiguration to add
     */
    public void addAppConfiguration(final ApplicationConfiguration config) {
        this.appNames.add(config.getName());
        this.configs.add(config);
    }

    /**
     * Removes all application configurations from this configuration.
     * 
     */
    public void clearAppConfigurations() {
        this.configs.clear();
    }

    /**
     * Assigns a new application ID to all the application configurations in the
     * perspective.
     * 
     */
    public void assignNewAppId() {
        this.appId = createAppId();
        final Enumeration<ApplicationConfiguration> e = this.configs.elements();
        while (e.hasMoreElements()) {
            e.nextElement().setApplicationId(this.appId);
        }
    }

    /**
     * Gets the unique application ID currently assigned to the perspective.
     * 
     * @return the application ID, or null if none currently assigned
     */
    public String getAppId() {
        return this.appId;
    }

    private String createAppId() {
        if (this.perspectiveDirectory == null) {
            throw new IllegalStateException(
                    "Cannot set application ID because the perspective directory is not set.");
        }
        final String appId = this.perspectiveDirectory.replace(' ', '_') + "_"
                + GdsSystemProperties.getSystemUserName() + "_"
                + String.valueOf(new AccurateDateTime().getTime());
        return appId;
    }

    /**
     * Creates a backup directory and places a copy of the perspective there
     * 
     * @throws IOException
     *             thrown if perspective backup directory cannot be created
     */
    public void backupPerspective() throws IOException {

        final String saveFile = this.perspectiveDirectory + File.separator
                + perspectiveFileName;
        final File path = new File(saveFile);
        final String filePath = path.getParent();

        final File myPath = new File(filePath);
        if (!myPath.exists()) {
            return;
        }
        final File backupPath = new File(filePath + File.separator
                + BACKUP_DIR_NAME);
        if (!backupPath.exists() && !backupPath.mkdir()) {
            throw new IOException(
                    "Could not create perspective backup directory "
                            + backupPath.getPath());
        }
        FileUtility.copyDirectory(filePath, backupPath.getPath());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.LockableElement#isLocked()
     */
    @Override
    public boolean isLocked() {
        return this.locked;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.LockableElement#setLocked(boolean)
     */
    @Override
    public void setLocked(final boolean lock) {
        this.locked = lock;
        for (final ApplicationConfiguration appConfig : this.configs) {
            appConfig.setLocked(lock);
        }
    }

    /**
     * Determines if perspective directory can be written. Note: always returns
     * true on Mac
     * 
     * @return true if can write, false otherwise
     */
    public boolean isWriteable() {
        final String saveFile = this.perspectiveDirectory + File.separator
                + perspectiveFileName;
        final File f = new File(saveFile);
        // Note this always returns true on the Mac
        return f.canWrite();
    }
}
