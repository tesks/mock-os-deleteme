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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.SAXParser;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * Defines the configuration of a particular application in the integrated GUI.
 * Manages a set of display configurations.
 * 
 *
 */
public class ApplicationConfiguration implements LockableElement {

	private final Tracer log;

	private final HashMap<ApplicationType, String> appExeNames;

	private String configFile;
	private final HashMap<String, DisplayConfiguration> displayMap = new HashMap<String, DisplayConfiguration>();
	private String configPath;
	private boolean isSseOnlyApp;
	private boolean isFswOnlyApp;
	private ApplicationType applicationType = ApplicationType.UNKNOWN;
	private String applicationId;
	private String uid;
	private boolean locked;
	private PerspectiveConfiguration parentPerspective;
	private final ApplicationContext appContext;
	private final PerspectiveProperties perspectiveProps;

	/**
     * Creates an instance of ApplicationConfiguration.
     * 
     * @param appContext
     *            the current application context
     */
	 public ApplicationConfiguration(final ApplicationContext appContext) {
		 this.appContext = appContext;
        log = TraceManager.getTracer(appContext, Loggers.CONFIG);
		 this.perspectiveProps = appContext.getBean(PerspectiveProperties.class);
		 appExeNames = new HashMap<ApplicationType, String>();
		 final String downlinkApp = perspectiveProps.getDownlinkApplicationName();
		 final String uplinkApp = perspectiveProps.getUplinkApplicationName();
		 final String monitorApp = perspectiveProps.getMonitorApplicationName();
		 appExeNames.put(ApplicationType.DOWNLINK, downlinkApp);
		 appExeNames.put(ApplicationType.UPLINK, uplinkApp);
		 appExeNames.put(ApplicationType.MONITOR, monitorApp);
		 uid = createUid();
	 }

	 /**
	  * Loads this object instance from an XML file.
	  * 
	  * @param filename the pathname of the input file
	  * @return true if the load succeeded; false otherwise.
	  */
	 public boolean load(final String filename) {
		 SAXParser sp = null;
		 try {
			 final String fullPath = getConfigPath() + File.separator + filename;
			 if (!new File(fullPath).exists()) {
				 log.error("Application configuration file " + filename
						 + " does not exist");
				 return false;
			 }
			 final AppConfigSaxHandler tcSax = new AppConfigSaxHandler(appContext);
			 sp = SAXParserPool.getInstance().getNonPooledParser();
			 sp.parse(fullPath, tcSax);
			 setConfigFile(filename);
			 return true;
		 } catch (final SAXException e) {
			 log.error("Unable to parse application configuration file "
					 + filename);
			 log.error(e.getMessage());
			 return false;
		 } catch (final Exception e) {
			 e.printStackTrace();
			 log.error("Unable to parse application configuration file "
					 + filename);
			 log.error("Unexpected error: " + e.toString());
			 return false;
		 }
	 }

	 /**
	  * Sets the name of the configuration file for this application
	  * configuration.
	  * 
	  * @param filename name of the configuration file
	  */
	 public void setConfigFile(final String filename) {
		 configFile = filename;
	 }

	 /**
	  * AppConfigSaxHandler is the XML parse handler for application
	  * configurations.
	  * 
	  */
	 private class AppConfigSaxHandler extends DefaultHandler {
		 
		 private final ApplicationContext appContext;

		public AppConfigSaxHandler(final ApplicationContext appContext) {
			 this.appContext = appContext;
		 }

		 protected StringBuffer text = new StringBuffer();

		 /**
		  * {@inheritDoc}
		  * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
		  *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
		  */
		 @Override
        public void startElement(final String uri, final String localName, final String qname,
				 final Attributes attr) throws SAXException {
			 text = new StringBuffer();
			 if (qname.equalsIgnoreCase("AppConfiguration")) {
				 final String lockStr = attr.getValue("locked");
				 if (lockStr == null) {
					 locked = false;
				 } else {
					 locked = XmlUtility.getBooleanFromText(lockStr);
				 }
			 }
		 }

		 /**
		  * {@inheritDoc}
		  * @see org.xml.sax.ContentHandler#characters(char[], int, int)
		  */
		 @Override
		 public void characters(final char[] chars, final int start, final int length)
		 throws SAXException {
			 final String newText = new String(chars, start, length);
			 if (!newText.equals("\n")) {
				 text.append(newText);
			 }
		 }

		 /**
		  * {@inheritDoc}
		  * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
		  *      java.lang.String, java.lang.String)
		  */
		 @Override
        public void endElement(final String uri, final String localName, final String qname)
		 throws SAXException {
			 if (qname.equals("displayFile")) {
				 final String displayFileName = text.toString();
				 final DisplayConfiguration displayConfig = new DisplayConfiguration();
				 final boolean ok = displayConfig.load(appContext, getConfigPath(),
						 displayFileName);
				 if (!ok) {
					 throw new SAXException(
							 "Error parsing display configuration "
							 + displayFileName
							 + " in application configuration");
				 }
				 displayMap.put(displayFileName, displayConfig);
			 } else if (qname.equals("appType")) {
				 applicationType = new ApplicationType(text.toString());
			 } else if (qname.equals("sseOnly")) {
				 isSseOnlyApp = Boolean.parseBoolean(text.toString());
			 } else if (qname.equals("fswOnly")) {
				 isFswOnlyApp = Boolean.parseBoolean(text.toString());
			 } else if (qname.equals("appUid")) {
				 uid = text.toString();
			 }
		 }

		 /**
		  * {@inheritDoc}
		  * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		  */
		 @Override
        public void error(final SAXParseException e) throws SAXException {
			 throw new SAXException(
					 "Parse error in application configuration file line "
					 + e.getLineNumber() + ", column "
					 + e.getColumnNumber() + ": " + e.getMessage());
		 }

		 /**
		  * {@inheritDoc}
		  * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		  */
		 @Override
        public void fatalError(final SAXParseException e) throws SAXException {
			 throw new SAXException(
					 "Fatal parse error in application configuration file line "
					 + e.getLineNumber() + ", column "
					 + e.getColumnNumber() + ": " + e.getMessage());
		 }

		 /**
		  * {@inheritDoc}
		  * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		  */
		 @Override
        public void warning(final SAXParseException e) {
			 log.warn("Parse warning in application configuration file line "
					 + e.getLineNumber() + ", column " + e.getColumnNumber()
					 + ": " + e.getMessage());
		 }
	 }

	 /**
	  * Return the current config filename.
	  * 
	  * @return filename
	  */
	 public String getConfigFilename() {
		 return configFile;
	 }

	 /**
	  * Save the application configuration to the specified file and instruct
	  * managed displays to save as well.
	  * 
	  * @param file the name of the application configuration file (no directory
	  *            path)
	  * @throws IOException the exception that is thrown when the 
	  *            configuration path does not already exist and it cannot be 
	  *            created
	  */
	 public void save(final String file) throws IOException {
		 // Save app config file
		 final String saveFile = getConfigPath() + File.separator + file;
		 final File path = new File(getConfigPath());
		 if (!path.exists()) {
			 if (!path.mkdirs()) {
				 throw new IOException(
						 "Unable to create configuration directory "
						 + path.getAbsolutePath());
			 }
		 }
		 final FileWriter fos = new FileWriter(saveFile);
		 fos.write("<?xml version=\"1.0\"?>\n");
		 fos.write(this.toXML());
		 fos.close();
		 // Then save each display in config
		 // For each display configuration, get output filename
		 final Collection<DisplayConfiguration> displays = displayMap.values();
		 final Iterator<DisplayConfiguration> it = displays.iterator();
		 while (it.hasNext()) {
			 final DisplayConfiguration config = it.next();
			 config.setConfigPath(configPath);
			 config.save();
		 }
	 }

	 /**
	  * Return full xml description of configuration.
	  * 
	  * @return application configuration xml block
	  */
	 private String toXML() {
		 final StringBuffer result = new StringBuffer();
		 result.append("<AppConfiguration locked=\"" + locked + "\">\n");
		 result.append("   <appType>" + applicationType.getValueAsString()
				 + "</appType>\n");
		 result.append("   <appUid>" + this.getUid() + "</appUid>\n");
		 result.append("   <sseOnly>" + isSseOnlyApp + "</sseOnly>\n");
		 result.append("   <fswOnly>" + isFswOnlyApp + "</fswOnly>\n");
		 // For each display configuration, get output filename
		 final Collection<DisplayConfiguration> displays = displayMap.values();
		 final Iterator<DisplayConfiguration> it = displays.iterator();
		 while (it.hasNext()) {
			 final DisplayConfiguration config = it.next();
			 final String file = config.getConfigFile();
			 result.append("   <displayFile>" + file + "</displayFile>\n");
		 }
		 result.append("</AppConfiguration>");
		 return result.toString();
	 }

	 /**
	  * Save the configuration to the current set filename.
	  * 
	  * @throws IOException exception that is thrown when the configuration 
	  *            file cannot be saved
	  * 
	  */
	 public void save() throws IOException {
		 save(this.getConfigFilename());
	 }

	 /**
	  * Add a display configuration to this application configuration.
	  * 
	  * @param config display configuration that will be added
	  */
	 public void addDisplayConfiguration(final DisplayConfiguration config) {
		 // Check for name uniqueness
		 final String name = config.getConfigFile();
		 // Should not be in there
		 final DisplayConfiguration inDisplay = displayMap.get(name);
		 if (inDisplay != null) {
			 log.warn("Duplicate display name in display map, not adding");
			 return;
		 }
		 displayMap.put(name, config);
	 }

	 /**
	  * Remove a display configuration from this application configuration.
	  * 
	  * @param config display configuration that will be removed
	  */
	 public void removeChillDisplay(final DisplayConfiguration config) {
		 final String name = config.getName();
		 displayMap.remove(name);
	 }

	 /**
	  * Retrieves the display map for this configuration.
	  * 
	  * @return the HashMap of DisplayConfiguration objects
	  */
	 public HashMap<String, DisplayConfiguration> getDisplayMap() {
		 return displayMap;
	 }

	 /**
	  * Retrieves the first DisplayConfiguration in the application configuration
	  * that has the given display type.
	  * 
	  * @param type the DisplayType to match
	  * @return the DisplayConfiguration found, or null if no match
	  */
	 public DisplayConfiguration getDisplayConfig(final DisplayType type) {
		 final Collection<DisplayConfiguration> c = displayMap.values();
		 final Iterator<DisplayConfiguration> it = c.iterator();
		 while (it.hasNext()) {
			 final DisplayConfiguration current = it.next();
			 if (current.getType().equals(type)) {
				 return current;
			 }
		 }
		 return null;
	 }

	 /**
	  * Set the directory path where this file will be saved.
	  * 
	  * @param path the directory path
	  */
	 public void setConfigPath(final String path) {
		 configPath = path;
		 // For each display configuration, set path
		 final Collection<DisplayConfiguration> displays = displayMap.values();
		 final Iterator<DisplayConfiguration> it = displays.iterator();
		 while (it.hasNext()) {
			 final DisplayConfiguration config = it.next();
			 config.setConfigPath(path);
		 }
	 }

	 /**
	  * Retrieves the directory path of this configuration.
	  * 
	  * @return the directory path
	  */
	 public String getConfigPath() {
		 return configPath;
	 }

	 /**
	  * Gets the name of this application configuration.
	  * 
	  * @return the name
	  */
	 public String getName() {
		 return configFile;
	 }

	 /**
	  * Checks if this application configuration is SSE only
	  * 
	  * @return Returns the isSseOnly flag.
	  */
	 public boolean isSseOnly() {
		 return isSseOnlyApp;
	 }

	 /**
	  * Sets the isSseOnly flag.
	  * 
	  * @param isSse The isSseOnly value to set.
	  */
	 public void setSse(final boolean isSse) {
		 isSseOnlyApp = isSse;
	 }

	 /**
	  * Checks if this application configuration is FSW only
	  * 
	  * @return Returns the isFswOnly flag.
	  */
	 public boolean isFswOnly() {
		 return isFswOnlyApp;
	 }

	 /**
	  * Sets the isFswOnly flag.
	  * 
	  * @param isFsw The isFswOnly value to set.
	  */
	 public void setFsw(final boolean isFsw) {
		 isFswOnlyApp = isFsw;
	 }

	 /**
	  * Gets the application's execution name from the hashmap
	  * 
	  * @return Returns the application (binary/script) name.
	  */
	 public String getAppExeName() {
		 return appExeNames.get(applicationType);
	 }

	 /**
	  * Gets the application type (uplink, downlink, monitor, unknown)
	  * 
	  * @return Returns the application type.
	  */
	 public ApplicationType getApplicationType() {
		 return applicationType;
	 }

	 /**
	  * Sets the application type.
	  * 
	  * @param applicationType The applicationType to set.
	  */
	 public void setApplicationType(final ApplicationType applicationType) {
		 this.applicationType = applicationType;
	 }

	 /**
	  * Gets the ID for this appliaction
	  * 
	  * @return Returns the application Id.
	  */
	 public String getApplicationId() {
		 return applicationId;
	 }

	 /**
	  * Sets the application Id.
	  * 
	  * @param applicationId The ID to set.
	  */
	 public void setApplicationId(final String applicationId) {
		 this.applicationId = applicationId;
	 }


	 /**
	  * Retrieves the unique ID for this application configuration
	  * @return the uid
	  */
	 public String getUid() {
		 return uid;
	 }


	 /**
	  * Sets the unique id for this application configuration
	  * @param uid the uid to set
	  */
	 public void setUid(final String uid) {
		 this.uid = uid;
	 }

	 private String createUid() {
		 return String.valueOf(new AccurateDateTime().getTime()) + String.valueOf(Math.round(Math.random()));
	 }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.LockableElement#isLocked()
	 */
	@Override
    public boolean isLocked() {
		 return locked;
	 }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.LockableElement#setLocked(boolean)
	 */
	@Override
    public void setLocked(final boolean lock) {
		 locked = lock;
		 final Collection<DisplayConfiguration> configs = displayMap.values();
		 for (final DisplayConfiguration dispConfig: configs) {
			 dispConfig.setLocked(lock);
		 }
	 }

	/**
	 * Gets the parent perspective configuration
	 * 
	 * @return the parent perspective for this application
	 */
	public PerspectiveConfiguration getParentPerspective() {
		 return parentPerspective;
	 }

	/**
	 * Sets the parent perspective for this application
	 * 
	 * @param parentPerspective the configuration for a parent perspective
	 */
	public void setParentPerspective(final PerspectiveConfiguration parentPerspective) {
		 this.parentPerspective = parentPerspective;
	 }
}
