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
package jpl.gds.shared.config;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import jpl.gds.shared.cli.app.ApplicationConfiguration;


/**
 * This is a static class of methods for getting and setting the current
 * mission, which is stored in a Java system variable whose value is defined by
 * the constant MISSION_PROPERTY. A valid mission setting is needed to operate
 * many AMPCS classes. The base or root mission name is a brief alphanumeric
 * string, and is lower case: msl, mer, nsyt, and etc.
 * <p>
 * The MISSION_PROPERTY should be set to the basic or root mission name.
 * <p>
 * An "SSE/GSE" mission setting is supported in conjunction with the root
 * mission. The SSE/GSE mission name is constructed by concatenating the root
 * mission name and the value of the SSE_MISSION_SUFFIX: mslsse, nsytsse. This
 * is the value that will be returned by getSseNameForSystemMission(), or by
 * getSystemMissionIncludingSse() if the current application is an SSE only
 * application.
 * <p>
 * An application supporting SSE/GSE alone must set the Java system property
 * defined by the constant IS_SSE_PROPERTY to true.
 * <p>
 * Modifications to the properties defined by the MISSION_PROPERTY and
 * IS_SSE_PROPERTY System properties outside of this context will affect its
 * results. In the most common scenario, these properties are defined on the
 * Java command line.
 * 
 *
 * 
 * 
 */
public final class GdsSystemProperties {

    /** System property used to tell which mission is configured. */
    public static final String MISSION_PROPERTY = "GdsMission";

    /** 
     * System property used to tell if we are in an SSE-only application. 
     */
    public static final String IS_SSE_PROPERTY = "GdsApplicationIsSse";

    /**
     * Suffix appended to root mission name to get SSE/GSE mission name.
     */
    public static final String SSE_MISSION_SUFFIX = "sse";

    /**
     * The value used if no mission is defined.
     */
    public static final String DEFAULT_MISSION = "none";
    
    /**
     * System property which represents the process id number of this process.
     */
    public static final String PID_PROPERTY = "GdsPid";
    
    /**
     * Override for location of TPS configuration directory.
     */
    public static final String TPS_CONFIG_DIR = "GdsTpsConfigDir";   
    
    /**
     * Default sub-directory where third party libraries reside, java, python, etc.
     */
    public static final String DEFAULT_THIRD_PARTY_DIR = "tps";

    /**
     * System property, if set the FSW alarm configuration file will be
     * specified by the value of this property.
     */
    public static final String ALARM_FILE_PROPERTY = "GdsUserAlarmFile";
    /**
     * System property, if set the SSE alarm configuration file will be
     * specified by the value of this property.
     */
    public static final String SSE_ALARM_FILE_PROPERTY = "GdsSseUserAlarmFile";

    /** The process id of the Java virtual machine */
    private static int JAVA_VM_PID;

    /** The LOG4J2 system property */
    public static final String LOG4J_CONFIG_PROPERTY   = "log4j.configurationFile";
    
    /**
     * added property for being able to define a custom
     * configuration load path
     */
    /** Override for the directories to be used for loading properties */
    public static final String PROPERTY_LOAD_PATHS = "GdsPropertyDirs";

    /** PDPP Mnemonic */
    public static final String PDPP_MNEMONIC_PROPERTY = "Mnemonic";
    
    /** Property indicating a context/session key is required in the current application */
    public static final String NEEDS_CONTEXT_KEY_PROPERTY = "GdsNeedsContextKey";

    /**
     * System property which specifies if the chill integrated gui is being
     * used.
     */
    public static final String IS_INTEGRATED_GUI_PROPERTY = "GdsIntegrated";

    /**
     * Dynamic property indicating the user has overridden the active session
     * directory.
     */
    public static final String DICT_OVERRIDE_PROPERTY = "SessionDictOverridden";

    /**
     * Returns the value of the MISSION_PROPERTY System property,
     * or the value oF DEFAULT_MISSION if the property is not defined.
     * 
     * @return mission name, or DEFAULT_MISSION
     */

	/** Default directory of mpcs. */
	public static final String DEFAULT_ROOT_DIR = "/msop/mpcs/current";

	// General GDS configuration property names follow
	/** System property used to find where the GDS config is at. */
	public static final String DIRECTORY_PROPERTY = "GdsDirectory";

	private static final String SCHEMA_SUBDIR = "schema";
	
	/**
     * Default name for the directory where user configurations will be stored.
     * Typically under the user's home directory.
     */
    private static final String USER_CONFIG_DIR = "CHILL";
    
	/**
     * System property used to override where to look for user configuration
     * files.
     */
    public static final String USER_DIR_PROPERTY = "GdsUserConfigDir";
    
    /**
     * System property used to override where to look for project configuration
     * files.
     */
    public static final String PROJECT_DIR_PROPERTY = "GdsProjectConfigDir";
    
    /**
     * System property used to override where to look for system (core) configuration
     * files.
     */
    public static final String SYSTEM_DIR_PROPERTY = "GdsSystemConfigDir";
    
    /**
     * System property used for getting the current user name
     */
    public static final String  USER_NAME_PROPERTY         = "user.name";

    /**
     * System property used for get the value of 'java.io.tmpdir'
     */
    public static final String JAVA_IO_TEMPDIR = "java.io.tmpdir";

    private GdsSystemProperties() {}

    private static Properties localSysProperties;
    
    /**
     * Store a copy of all system properties to reduce System calls because that invokes
     * some (very expensive) Security Manager checks
     */
    static {
        localSysProperties = new Properties();
        final Enumeration<?> names = System.getProperties().propertyNames();
        while (names.hasMoreElements()) {
            final String name = (String)names.nextElement();
            localSysProperties.setProperty(name,System.getProperty(name));          
        }
    }

    /**
     * Returns all the cached System Properties
     * 
     * @return all System Properties
     */
    public static Properties getCachedSystemProperties() {
        return localSysProperties;
    }

    /**
     * Sets a system property to a supplied value and caches it locally
     * 
     * @param sysProperty
     *            The system property to set
     * @param value
     *            The value to set the system property to
     */
    public static void setSystemProperty(final String sysProperty, final String value) {
        localSysProperties.setProperty(sysProperty, value);
        System.setProperty(sysProperty, value);
    }

    /**
     * Centralized access point to retrieve cached system properties
     * 
     * @param sysProperty
     *            The system property to look up
     * @return The property value if it is set; otherwise null
     */
    public static String getSystemProperty(final String sysProperty) {
        return localSysProperties.getProperty(sysProperty);
    }

    /**
     * Clears a system property from the cached system properties
     * 
     * @param propertyToClear
     *            The property to clear
     */
    public static void clearProperty(final String propertyToClear) {
        localSysProperties.remove(propertyToClear);
    }

    /**
     * Centralized access point to retrieve cached system properties
     * 
     * @param sysProperty
     *            The system property to look up
     * @param defaultValue
     *            The default value to use if the system property is not set
     * @return value of sysProperty if set; otherwise defaultValue
     */
    public static String getSystemProperty(final String sysProperty, final String defaultValue) {
        return localSysProperties.getProperty(sysProperty, defaultValue);
    }

    /**
     * Gets the current user name from the system property "user.name"
     * 
     * @return The current user
     */
    public static String getSystemUserName() {
        return getSystemProperty(USER_NAME_PROPERTY);
    }

    /**
     * Returns the currently configured mission (value of MISSION_PROPERTY).
     * This method is designed to replace the old GdsConfiguration.getMission()
     * method. Defaults to "none".
     * 
     * @return the mission name, always lower case, never null
     */
    public static String getSystemMission() {
        return localSysProperties.getProperty(MISSION_PROPERTY, DEFAULT_MISSION).toLowerCase();
    }
    

    /**
     * Returns the same thing as getSystemMission() if applicationIsSse()
     * is false. Otherwise, returns the SSE mission name. This method
     * is designed to replace the old GdsConfiguration.getMission() method.
     * 
     * @return mission name, or SSE mission name
     * 
     * @param sseFlag
     *            Whether or not to get the SSE mission name
     */
    public static String getSystemMissionIncludingSse(final boolean sseFlag) {
        if (sseFlag) {
            return getSseNameForSystemMission();
        }
        else {
            return getSystemMission();
        }
    }

    /**
     * Sets the value of the MISSION_PROPERTY System property to the 
     * lower case value of the input string. This action is GLOBAL.
     * 
     * @param mission mission name to set
     */
    public static void setSystemMission(final String mission) {
        setSystemProperty(MISSION_PROPERTY, mission.toLowerCase());
    }


    /**
     * Gets the SSE/GSE mission name equivalent to the current mission. Note
     * that this method does not know whether the mission really supports an
     * SSE/GSE in the AMPCS distribution, or if the mission name is set to
     * DEFAULT_MISSION. It will return an SSE/GSE mission name regardless.
     * 
     * @return SSE mission name, never null
     */
    public static String getSseNameForSystemMission() {
        return getSystemMission() + SSE_MISSION_SUFFIX;
    }

    /**
     * Returns the string version of the process id.
     * 
     * @return process id
     */
    public static String getPid() {
        return localSysProperties.getProperty(PID_PROPERTY, "0");
    }

    /**
     * Returns the integer version of the process id.
     * 
     * @return process id
     */
    public static int getIntegerPid() {
        return Integer.valueOf(localSysProperties.getProperty(PID_PROPERTY, "0"));
    }
    /**
     * Returns the process id of the Java VM. Note that this will likely not
     * be the same as the pid from the property.
     * 
     * @return Process id
     */
    public static final synchronized int getJavaVmPid() {
       /* This is time consuming.  Do not do it in a static
        * block at the top, but rather here, if and when it is needed.
        */
       if (JAVA_VM_PID == 0) {
           // Only need to get this once

           final String   id  = ManagementFactory.getRuntimeMXBean().getName();
           final String[] ids = id.split("@");

           JAVA_VM_PID = Integer.parseInt(ids[0]);
       }

        return JAVA_VM_PID;
    }
    /**
     * Returns the third party configuration directory path.
     * 
     * @return third party configuration directory
     */
    public static String getThirdPartyConfigDir() {
    	final String override = localSysProperties.getProperty("GdsTpsConfigDir");
    	if (override != null) {
    		return override + File.separator;
    	}
        final StringBuilder fullPath = new StringBuilder();
        fullPath.append(GdsSystemProperties.getSystemProperty(DIRECTORY_PROPERTY, DEFAULT_ROOT_DIR));
        fullPath.append(File.separator);
        fullPath.append(DEFAULT_THIRD_PARTY_DIR);
        fullPath.append(File.separator);
        fullPath.append("config");
        fullPath.append(File.separator);
        return fullPath.toString();
    }
    /**
     * Indicates if the current application is running as part of the integrated
     * GUI.
     * 
     * @return true if running integrated
     */
    public static boolean isIntegratedGui() {
        final String str = localSysProperties.getProperty(IS_INTEGRATED_GUI_PROPERTY);
        if (str == null) {
            return false;
        } else {
            return Boolean.valueOf(str);
        }
    }

    /**
     * Set whether the current application is running as part of the integrated GUI.
     * Should be used for test purposes only
     * @param isIntegrated true if this is an integrated GUI application
     */
    public static void setIsIntegratedGui(boolean isIntegrated) {
        setSystemProperty(IS_INTEGRATED_GUI_PROPERTY, Boolean.toString(isIntegrated));
    }

    /**
     * Get the GDS Directory.
     * 
     * @return Return the value of the GdsDirectory system property or the
     *         DEFAULT_ROOT_DIR if it isn't set
     */
    public static String getGdsDirectory() {
        return (localSysProperties.getProperty(DIRECTORY_PROPERTY, DEFAULT_ROOT_DIR));
    }
    /**
     * Returns the system test directory path.
     * 
     * @return system test directory
     */
    public static String getSystemTestDir() {
        final StringBuilder fullPath = new StringBuilder(getGdsDirectory());

        fullPath.append(File.separator);
        fullPath.append("test");
        fullPath.append(File.separator);

        return fullPath.toString();
    }
    /**
     * Determine if the running operating system is Mac.
     * 
     * @return True if Mac OS.
     */
    public static boolean isMacOs() {
        return localSysProperties.getProperty("os.name").contains("Darwin")
                || localSysProperties.getProperty("os.name").contains("Mac");
    }

    /**
     * Determine the PDPP Mnemonic
     *
     * @return null if not set
     */
    public static String getPdppMnemonicProperty() {
        return GdsSystemProperties.getSystemProperty(PDPP_MNEMONIC_PROPERTY, null);
    }
    
    /** 
     * Returns the system level schema directory name.
     * 
     * @return schema directory path
     * 
     */
    public static String getSchemaDirectory() {
        String dir = String.format("%s%s%s", getGdsDirectory(), File.separator, SCHEMA_SUBDIR); 
        
        if (!new File(dir).exists()) {
            dir = String.format("%s%s%s", ApplicationConfiguration.getRootDir(), File.separator, SCHEMA_SUBDIR); 
        }
        
        return dir;
    }



    
    /**
     * Gets the value of the system property indicating if the default or session
     * dictionary location has been overridden by the current application.
     * 
     * @return true if overridden, false if not
     */
    public static boolean isDictionaryOverridden() {
        final String boolStr = localSysProperties.getProperty(DICT_OVERRIDE_PROPERTY, "false");
        try {
            return Boolean.valueOf(boolStr);
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
    
    
    /**
     * Sets the value of the system property indicating if the default or session
     * dictionary location has been overridden by the current application.
     * 
     * @param override true if overridden, false if not
     */
    public static void setDictionaryIsOverridden(final boolean override) {
        localSysProperties.setProperty(DICT_OVERRIDE_PROPERTY, String.valueOf(override));
    }
    
    /**
     * Sets the Log4j2 system configuration property
     * 
     * @param value
     *            property value to set
     *
     */
    public static void setLogConfigProperty(final String value) { 
        /**
         * Log4j2 doesn't look at our cached System Properties,
         * so we still need to call System.setProperty()
         */
        setSystemProperty(LOG4J_CONFIG_PROPERTY, value);
    }
    
    
    /**
     * Returns the system configuration directory path.
     * 
     * @return system config path
     */
    public static String getSystemConfigDir() {
        final String sysOverride = localSysProperties.getProperty(SYSTEM_DIR_PROPERTY);
        if (sysOverride != null) {
            return sysOverride + File.separator;
        }
        final StringBuilder fullPath = new StringBuilder();
        fullPath.append(GdsSystemProperties.getSystemProperty(GdsSystemProperties.DIRECTORY_PROPERTY,
                                                              GdsSystemProperties.DEFAULT_ROOT_DIR));
        fullPath.append(File.separator);
        fullPath.append("config");
        fullPath.append(File.separator);
        return fullPath.toString();
    }
    
    /**
     * Returns the project-specific configuration directory path for the given
     * mission.
     * 
     * @param mission
     *            Mission name to get project config directory for.
     * @return configuration directory
     */
    public static String getProjectConfigDir(final String mission) {
        final String projOverride = localSysProperties.getProperty(PROJECT_DIR_PROPERTY);
        if (projOverride != null) {
            final StringBuilder fullPath = new StringBuilder(projOverride);
            fullPath.append(File.separator);
            fullPath.append(mission);
            fullPath.append(File.separator);
            return fullPath.toString();
        }
        final StringBuilder fullPath = new StringBuilder(getSystemConfigDir());
        fullPath.append(mission);
        fullPath.append(File.separator);
        return fullPath.toString();
    }
    


    /**
     * Returns the project-specific configuration directory path for the current
     * missions.
     * 
     * @param isSse
     *            Whether or not SSE is enabled
     * 
     * @return project configuration directory
     */
    public static String getProjectConfigDir(final boolean isSse) {
        return getProjectConfigDir(GdsSystemProperties.getSystemMissionIncludingSse(isSse));
    }
    
    /**
     * Returns the user-specific config file path.
     * 
     * @return config file directory
     */
    public static String getUserConfigDir() {
        return localSysProperties.getProperty(USER_DIR_PROPERTY,
                localSysProperties.getProperty("user.home") + File.separator
                        + USER_CONFIG_DIR);
    }
    
    
    /**
     * Returns a Unix PATH-style String with each config directory listed in order of most
     * "local" to least "local", terminating with the system config. All paths are absolute paths.
     * Each directory is separated by the the String returned by File.pathSeparator.
     * 
     * @param isSse
     *            whether or not the application is SSE
     * 
     * @return a String containing config directory in order from most local to least local.
     */
    public static String getFullConfigPath(final boolean isSse) {
        if(localSysProperties.getProperty(PROPERTY_LOAD_PATHS) != null){
            return getFlexConfigPath();
        }
        else {
            return getFixedConfigPath(isSse);
        }
    }

    /**
     * Returns a Unix PATH-style String with each config directory listed in
     * order of least "local" to most "local". All paths are absolute paths.
     * Each directory is separated by the the String returned by
     * File.pathSeparator. This function returns the "fixed" configuration path
     * - user, SSE if applicable, project/flight, and system directories only.
     * 
     * @return a String containing config directory in order from most local to
     *         least local.
     */
    private static String getFixedConfigPath(final boolean isSse) {
        final StringBuilder builder = new StringBuilder();

        File temp = new File(getSystemConfigDir());
        builder.append(temp.getAbsolutePath()).append(File.pathSeparator);

        temp = new File(getProjectConfigDir(GdsSystemProperties.getSystemMission()));
        builder.append(temp.getAbsolutePath()).append(File.pathSeparator);

        if (isSse) {
            temp = new File(getProjectConfigDir(isSse));
            builder.append(temp.getAbsolutePath()).append(File.pathSeparator);
        }

        temp = new File(getUserConfigDir());
        builder.append(temp.getAbsolutePath());

        return builder.toString();
    }

    /**
     * Returns the config directories as a List. This function returns the directories
     * with the "least local" (system) properties first.
     * 
     * @param isSse
     *            true if the application is SSE
     * 
     * @return the config directories
     */
    public static List<String> getFullConfigPathList(final boolean isSse) {
        final String configPath = getFullConfigPath(isSse);

        return Arrays.asList(configPath.split(File.pathSeparator));
    }

    
    /**
     * Returns a Unix PATH-style String with each config directory listed in
     * order of least "local" to most "local". Each directory is separated by
     * the the String returned by File.pathSeparator. This function returns the
     * "flexible" configuration path - The directories specified by the system
     * property GdsPropertyDirs and the system directory only. If the
     * GdsPropertyDirs is empty, only the system directory will be returned.
     * 
     * @return a String containing config directory in order from most local to
     *         least local.
     */
    private static String getFlexConfigPath(){
        final StringBuilder builder = new StringBuilder();
        File temp = new File(getSystemConfigDir());
        builder.append(temp.getAbsolutePath());
        
        final String tempString = localSysProperties.getProperty(PROPERTY_LOAD_PATHS);
        
        if(tempString != null){
            final String[] tempStringArr = tempString.split(File.pathSeparator);
            for(final String directory : tempStringArr){
                temp = new File(directory);
                builder.append(File.pathSeparator)
                       .append(temp.getAbsolutePath());
                       
            }
        }
        
        return builder.toString();
    }

    /**
     * Returns the full path of the most local version of the file with the
     * given name. In other words, if the file is found in the user's
     * configuration directory, then the returned path will point to the file in
     * the user's config directory. Next, this method will check to see if the
     * file exists in the project config directory and will return that path if
     * so. Then it will check to see if the file is in the default project
     * config dir. Finally, it will check the system config dir. If the file is
     * not found in any of these locations, this method will return null.
     * 
     * @param filename
     *            the name of the config file to find
     * 
     * @param isSse
     *            Whether or not the application is SSE
     * 
     * @return the full path to the config file, if found; otherwise null
     * 
     */
    public static String getMostLocalPath(final String filename, final boolean isSse) {
        
        final List<String> dirs = getFullConfigPathList(isSse);
        Collections.reverse(dirs);

        for(final String dir : dirs){
            final File f = new File(dir + File.separator + filename);
            if (f.exists()) {
                return f.getPath();
            }
        }
        return null;
    }

    /**
     * Gets the property indicating whether the current application needs a context
     * or session key.
     * 
     * @return true or false
     */
    public static boolean getNeedsContextKey() {
        return Boolean.valueOf(localSysProperties.getProperty(NEEDS_CONTEXT_KEY_PROPERTY, "false"));
    }

    /**
     * Returns true if the current application is running as a SSE/GSE-only
     * application.
     * 
     * @return True if the current application is SSE/GSE, false otherwise.
     * 
     */
    @Deprecated
    public static boolean applicationIsSse() {
        return(localSysProperties.getProperty(IS_SSE_PROPERTY, "false").equalsIgnoreCase("true"));
    }

    /**
     * Sets the flag indicating if the current application is running as a
     * SSE/GSE-only application.
     * 
     * @param isSse
     *            true if the current application is SSE/GSE, false otherwise.
     * 
     */
    @Deprecated
    public static void setApplicationIsSse(final boolean isSse) {
        setSystemProperty(IS_SSE_PROPERTY, String.valueOf(isSse));
    }

    /**
     * Wrapper for
     * jpl.gds.shared.config.GdsSystemProperties.getMostLocalPath
     * (String, String), but using system properties' mission value (i.e.
     * current mission).
     * 
     * @param filename
     *            the name of the config file to find
     * @return the full path to the config file, if found; otherwise null
     */
    @Deprecated
    public static String getMostLocalPath(final String filename) {
        return getMostLocalPath(filename, null);
    }

    /**
     * Returns the full path of the most local version of the file with the
     * given name. In other words, if the file is found in the user's
     * configuration directory, then the returned path will point to the file in
     * the user's config directory. Next, this method will check to see if the
     * file exists in the project config directory and will return that path if
     * so. Then it will check to see if the file is in the default project
     * config dir. Finally, it will check the system config dir. If the file is
     * not found in any of these locations, this method will return null.
     * 
     * @param filename
     *            the name of the config file to find
     * @param mission
     *            override value for the mission string
     * @return the full path to the config file, if found; otherwise null
     * 
     */
    @Deprecated
    public static String getMostLocalPath(final String filename,
                                          final String mission) {
        return getMostLocalPath(filename, false);

    }

    /**
     * Returns the same thing as getSystemMission() if applicationIsSse()
     * is false. Otherwise, returns the SSE mission name. This method
     * is designed to replace the old GdsConfiguration.getMission() method.
     * 
     * @return mission name, or SSE mission name
     * 
     */
    @Deprecated
    public static String getSystemMissionIncludingSse() {
        if (applicationIsSse()) {
            return getSseNameForSystemMission();
        }
        else {
            return getSystemMission();
        }
    }
    

    /**
     * Returns a Unix PATH-style String with each config directory listed in
     * order of least "local" to most "local". All paths are absolute paths.
     * Each directory is separated by the the String returned by
     * File.pathSeparator. This function returns the "fixed" configuration path
     * - user, SSE if applicable, project/flight, and system directories only.
     * 
     * @return a String containing config directory in order from most local to
     *         least local.
     */
    @Deprecated
    private static String getFixedConfigPath() {
        return (getFixedConfigPath(false));
    }


    /**
     * Returns a Unix PATH-style String with each config directory listed in order of most
     * "local" to least "local", terminating with the system config. All paths are absolute paths.
     * Each directory is separated by the the String returned by File.pathSeparator.
     * 
     * @return a String containing config directory in order from most local to least local.
     */
    @Deprecated
    public static String getFullConfigPath() {
    	if(localSysProperties.getProperty(PROPERTY_LOAD_PATHS) != null){
            return getFlexConfigPath();
        }
        else {
            return getFixedConfigPath();
        }
    }

    /**
     * Returns the project-specific configuration directory path for the current
     * missions.
     * 
     * @return project configuration directory
     */
    @Deprecated
    public static String getProjectConfigDir() {
        return getProjectConfigDir(GdsSystemProperties.getSystemMissionIncludingSse());
    }

    /**
     * Resets the GdsSystemProperties to the undefined state.
     * 
     */
    @Deprecated
    public static void reset() {
        setSystemMission(DEFAULT_MISSION);
        setApplicationIsSse(false);
    }

}
