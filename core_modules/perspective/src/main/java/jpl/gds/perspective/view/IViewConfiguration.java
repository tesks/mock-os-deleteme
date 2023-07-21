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
package jpl.gds.perspective.view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.ExportableConfiguration;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;
import jpl.gds.shared.xml.parse.SAXParserPool;

public interface IViewConfiguration extends ExportableConfiguration {

    /**
     * Place holder for the test name that will eventually be replaced.
     */
    public static final String NAME_TOKEN = "[test-name]";
    
    /**
     * Place holder for the test key.
     */
    public static final String ID_TOKEN = "[session-key]";
    
    /**
     * XML view name attribute name
     */
    public static final String VIEW_NAME_TAG = "name";
    
    /**
     * XML view element name
     */
    public static final String VIEW_TAG = "View";
    
    /**
     * XML view class attribute name
     */
    public static final String VIEW_CLASS_TAG = "viewClass";
 
    /**
     * XML view type attribute name
     */
    public static final String VIEW_TYPE_TAG = "type";
    
    /**
     * XML view version attribute name
     */
    public static final String VIEW_VERSION_TAG = "version";
    
    /**
     * XML background color element name
     */
    public static final String OLD_VIEW_BACKGROUND_TAG = "BackgroundColor";
    
    /**
     * XML default background attribute name
     */
    public static final String VIEW_BACKGROUND_TAG = "defaultBackground";
    
    /**
     * XML default foreground attribute name
     */
    public static final String VIEW_FOREGROUND_TAG = "defaultForeground";
    
    /**
     * XML foreground color element name
     */
    public static final String OLD_VIEW_FOREGROUND_TAG = "ForegroundColor";
    
    /**
     * XML view title flag attribute name
     */
    public static final String VIEW_TITLE_ENABLE_TAG = "viewTitleEnabled";
    
    /**
     * XML default font attribute name
     */
    public static final String VIEW_FONT_TAG = "defaultFont";
    
    /**
     * XML view height attribute name
     */
    public static final String VIEW_HEIGHT_TAG = "preferredHeight";
    
    /**
     * XML view width attribute name
     */
    public static final String VIEW_WIDTH_TAG = "preferredWidth";
    
    /**
     * XML flight software version attribute name
     */
    public static final String FSW_DICT_VERSION_TAG = "fswVersion";
    
    /**
     * XML flight software dictionary directory attribute name
     */
    public static final String FSW_DICT_DIR_TAG = "fswDictionaryDir";
    
    /**
     * XML satellite system engineering version attribute name
     */
    public static final String SSE_DICT_VERSION_TAG = "sseVersion";
    
    /**
     * XML satellite system engineering directory attribute name
     */
    public static final String SSE_DICT_DIR_TAG = "sseDictionaryDir";

    
    /**
     * XML name for the default view title (user title).
     */
    public static final String DEFAULT_NAME_PROPERTY = "defaultTitle";
    /**
     * XML name for the configuration title (system title)
     */
    public static final String DEFAULT_CONFIG_NAME_PROPERTY = "configurationTitle";
    
    /**
     * View configuration version
     */
    public static final int WRITE_VERSION = 3;
    
    /**
     * Sets the ViewProperties object for this view.
     * 
     * @param props the properties object to set
     */
    public void setViewProperties(ViewProperties props);

    /**
     * Retrieves the view merged flag, indicating if this view has been merged into the
     * current perspective.
     * @return true if this view has been merged
     */
    public boolean isMerged();

    /**
     * Sets the view merged flag, indicating if this view has been merged into the
     * current perspective.
     * @param merged true to indicate this view has been merged
     */
    public void setMerged(boolean merged);

    /**
     * Gets the screen location of this view
     * 
     * @return Returns the screen location of this view.
     */
    public ChillLocation getLocation();

    /**
     * Sets the screen location of this view.
     *
     * @param location The location to set.
     */
    public void setLocation(ChillLocation location);

    /**
     * Gets the window size of this view
     * 
     * @return Returns the screen size of this view.
     */
    public ChillSize getSize();

    /**
     * Sets the screen size of this view.
     *
     * @param size The size to set.
     */
    public void setSize(ChillSize size);

    /**
     * Gets the data display font.
     * @return ChillFont
     */
    public ChillFont getDataFont();

    /**
     * Sets the data display font.
     * @param dataFont the ChillFont to set
     */
    public void setDataFont(ChillFont dataFont);

    /**
     * Gets the default background color.
     * @return ChillColor
     */
    public ChillColor getBackgroundColor();

    /**
     * Sets the default background color.
     * @param color the ChillColor to set
     */
    public void setBackgroundColor(ChillColor color);

    /**
     * Gets the default foreground color.
     * @return ChillColor
     */
    public ChillColor getForegroundColor();

    /**
     * Sets the default foreground color.
     * @param color the ChillColor to set
     */
    public void setForegroundColor(ChillColor color);

    /**
     * Gets the view type (i.e. channel, alarm, etc)
     * 
     * @return Returns the view type.
     */
    public ViewType getViewType();

    /**
     * Sets the view type.
     *
     * @param viewType The viewType to set.
     */
    public void setViewType(ViewType viewType);

    /**
     * Gets the view class
     * 
     * @return Returns the view Class.
     */
    public Class<?> getViewClass();

    /**
     * Sets the view Class.
     *
     * @param className The Class to set.
     */
    public void setViewClass(String className);

    /**
     * Sets the view Class.
     *
     * @param className The Class to set.
     */
    public void setViewPreferencesClass(String className);

    /**
     * Sets the view Class.
     *
     * @param className The Class to set.
     */
    public void setViewTabClass(String className);

    /**
     * Retrieves the name of and creates the view tab class from the GDS
     * configuration file. This method will only return results for monitor
     * views that go into a tab pane.
     * @return the Class object, or null if the proper class could not 
     * be determined.
     */
    public Class<?> getViewTabClass();

    /**
     * Retrieves the name of and creates the view preferences class from the GDS
     * configuration file. 
     * @return the Class object, or null if the proper class could not 
     * be determined.
     */
    public Class<?> getViewPreferencesClass();

    /**
     * Gets the hash table of configuration items
     * 
     * @return Returns the hash table of configuration items for this View.
     */
    public Hashtable<String, String> getConfigItems();

    /**
     * Removes the given configuration item.
     * @param name the name of the configuration item to remove.
     */
    public void removeConfigItem(String name);

    /**
     * Sets the configuration items for this view.
     *
     * @param configItems The hash table of configuration items to set.
     */
    public void setConfigItems(Hashtable<String, String> configItems);

    /**
     * Gets the view name
     * 
     * @return Returns the view name.
     */
    public String getViewName();

    /**
     * Sets the view name.
     *
     * @param viewName The view name to set.
     */
    public void setViewName(String viewName);

    /**
     * Sets a single configuration item.
     * 
     * @param name the name of the configuration item (should be the same as its XML tag name)
     * @param value the value of the configuration item
     */
    public void setConfigItem(String name, String value);

    /**
     * Sets a single configuration item that consists of a list of Strings. The value
     * will be converted to a comma-separated list.
     * 
     * @param name the name of the configuration item (should be the same as its XML tag name)
     * @param value the value of the configuration item as a String[]
     */
    public void setConfigItem(String name, String[] value);

    /**
     * Gets the value of a configuration item.
     * @param name the name of the configuration item to get (should be 
     * the same as its XML tag name).
     * @return the value of the configuration item as a String, or null if not found
     */
    public String getConfigItem(String name);

    /**
     * Gets the value of comma-separated configuration item as an array.
     * @param name the name of the configuration item to get (should be 
     * the same as its XML tag name).
     * @return the value of the configuration item as a String array, or null if not found
     */
    public String[] getConfigItemList(String name);

    /**
     * Generates an XML representation of this View configuration.
     * @return an XML string
     */
    public String toXML();

    /** 
     * Saves the view configuration to a file.
     * 
     * @param filename the file path to save to.
     * @throws IOException thrown if there are any problems creating, writing 
     *                 to or closing the file writer object
     */
    public void save(String filename) throws IOException;

    /**
     * Adds a table configuration to this view configuration.
     * @param table the ChillTable to add 
     */
    public void addTable(ChillTable table);

    /**
     * Removes a table configuration from this view configuration.
     * @param table the ChillTable to remove
     */
    public void removeTable(ChillTable table);

    /**
     * Gets the table definition with the given name.
     * @param name the table name
     * @return the matching ChillTable, or null if no table found
     */
    public ChillTable getTable(String name);

    /**
     * Replaces a table definition. The table to replace is located based upon the
     * table name. If the table does not already exist, the new one is simply added.
     * @param table the ChillTable to set
     */
    public void setTable(ChillTable table);

    /**
     * Adds a ViewConfigurationListener to this configuration.
     * @param l the ViewConfigurationListener to add
     */
    public void addConfigurationListener(ViewConfigurationListener l);

    /**
     * Removes a ViewConfigurationListener from this configuration.
     * @param l the ViewConfigurationListener to remove
     */
    public void removeConfigurationListener(ViewConfigurationListener l);

    /**
     * Gets the version number
     * 
     * @return Returns the version.
     */
    public int getVersion();

    /**
     * Sets the version.
     *
     * @param version The version to set.
     */
    public void setVersion(int version);

    /**
     * Enables/disables display of view title.
     * @param enable true to turn view title display on; false otherwise
     */
    public void setDisplayViewTitle(boolean enable);

    /**
     * Indicates if display of view title is enabled.
     * @return true if view title is enabled
     */
    public boolean isDisplayViewTitle();

    /**
     * Creates the title for a window-type view from the view name. Takes care of substituting in the
     * current context name and key if the replacement tokens for these are
     * present in the current view name.
     * @param contextName the current context name
     * @param contextId the current context identification string 
     * @return a window title
     */
    public String getWindowTitle(String contextName, String contextId);

    /**
     * Gets the ViewReference object associated with this view, or null if this
     * view is not a reference.
     * @return the ViewReference, or null if none defined
     */
    public ViewReference getViewReference();

    /**
     * Sets the ViewReference for this view.
     * @param reference the ViewReference to set
     */
    public void setViewReference(ViewReference reference);

    /**
     * Indicates if this view object has a defined reference, meaning this view
     * was loaded from a pre-defined view file.
     * @return true if this view has a file reference
     */
    public boolean isReference();

    /**
     * Sets the preferred height of this view in pixels.
     * 
     * @param height pixel height, or 0 to clear height preference
     */
    public void setPreferredHeight(int height);

    /**
     * Gets the preferred height of this view in pixels.
     * 
     * @return pixel height, or 0 if no preference exists
     */
    public int getPreferredHeight();

    /**
     * Sets the preferred width of this view in pixels.
     * 
     * @param width pixel width, or 0 to clear width preference
     */
    public void setPreferredWidth(int width);

    /**
     * Gets the preferred width of this view in pixels.
     * 
     * @return pixel width, or 0 if no preference exists
     */
    public int getPreferredWidth();

    /**
     * Clears the ViewReference for this view and any child views.
     */
    public void clearViewReferences();

    /**
     * Gets a List of all ViewReferences for this view and any child views.
     * 
     * @return List of ViewReference objects, or an empty list if no references
     * found
     */
    public List<ViewReference> getAllViewReferences();

    /**
     * Gets the SSE dictionary version used to build this view.
     * 
     * @return version string
     */
    public String getSseVersion();

    /**
     * Gets the FSW dictionary version used to build this view.
     * 
     * @return version string
     */
    public String getFswVersion();

    /**
     * Gets the SSE dictionary directory used to build this view.
     * 
     * @return directory name
     */
    public String getSseDictionaryDir();

    /**
     * Gets the FSW dictionary directory used to build this view.
     * 
     * @return directory name
     */
    public String getFswDictionaryDir();

    /**
     * Sets the SSE dictionary version used to build this view.
     * 
     * @param version version to set
     */
    public void setSseVersion(String version);

    /**
     * Sets the FSW dictionary version used to build this view.
     * 
     * @param version version to set
     */
    public void setFswVersion(String version);

    /**
     * Sets the FSW dictionary directory used to build this view.
     * 
     * @param dir directory to set
     */
    public void setFswDictionaryDir(String dir);

    /**
     * Sets the SSE dictionary directory used to build this view.
     * 
     * @param dir directory to set
     */
    public void setSseDictionaryDir(String dir);

    /**
     * Set any deprecated field/columns in this view. Called after existing 
     * view is parsed from the configuration.
     */
    public void setDeprecations();

    /**
     * Returns a view-specific parser module. If null, there is
     * no view-specific parsing.
     * 
     * This default implementation always returns null.
     * 
     * @param appContext the current application context
     * 
     * @return  IViewConfigParser for this view; may be null
     */
    public IViewConfigParser getParser(ApplicationContext appContext);
    
    /**
     * Parses a view configuration from an XML configuration file and
     * sets relevant configuration values. 
     * 
     * @param appContext the current application context
     * @param filename the name of the configuration file
     * @return a list of ViewConfiguration objects, or null if an error occurs
     */
    public static List<IViewConfiguration> load(final ApplicationContext appContext, final String filename) {
        final Tracer trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
        SAXParser parser = null;
        try {
            final File f = new File(filename);
            if (!f.exists()) {
                trace.error("View configuration file " + filename + " not found");
                return null;
            }       
            parser = SAXParserPool.getInstance().getNonPooledParser();
            final ViewConfigParseHandler handler = new ViewConfigParseHandler(appContext);
            parser.parse(f, handler);
            return handler.getParsedConfigs();
        } catch (final SAXException e) {
            trace.error("Parse error: Could not load view configurations from file " + filename
                    + ". " + ExceptionTools.getMessage(e));
            return null;
        } catch (final Exception e) {
            trace.error("Unexpected error: Could not load view configurations from file " + filename
                    + ". " + ExceptionTools.getMessage(e), e);
            return null;
        } 
    }
    
    /**
     * Parses a view configuration from an XML configuration file and
     * sets relevant configuration values. 
     * 
     * @param appContext the current application context
     * @param filename the name of the configuration file
     * @return a list of ViewConfiguration objects, or null if an error occurs
     * @throws SAXException if there is an error parsing the View XML
     * @throws ParserConfigurationException if there is an error configuring the XML parser
     * @throws IOException if there is a File I/O error reading the view file
     */
    public static List<IViewConfiguration> loadAndThrow(final ApplicationContext appContext, 
            final String filename) 
            throws ParserConfigurationException, SAXException, IOException 
    {
        SAXParser parser = null;
        final File f = new File(filename);
        if (!f.exists()) {
            TraceManager.getDefaultTracer(appContext).error("View configuration file " + filename + " not found");
            return null;
        }
        parser = SAXParserPool.getInstance().getNonPooledParser();
        final ViewConfigParseHandler handler = new ViewConfigParseHandler(appContext);
        parser.parse(f, handler);
        return handler.getParsedConfigs();
    }

    /**
     * Parses a view configuration from a string of XML and
     * sets relevant configuration values. 
     * 
     * @param appContext the current application context
     * @param xml view configuration XML
     * @return a list of ViewConfiguration objects, or null if an error occurs
     * @throws SAXException if there is an error parsing the View XML
     * @throws ParserConfigurationException if there is an error creating the parser
     * @throws IOException if there is an I/O error reading the view file
     */
    public static List<IViewConfiguration> loadFromString(final ApplicationContext appContext, final String xml) 
            throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = null;
        try {
            final ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());

            parser = SAXParserPool.getInstance().getNonPooledParser();
            final ViewConfigParseHandler handler = new ViewConfigParseHandler(appContext);
            parser.parse(stream, handler);
            return handler.getParsedConfigs();
        } finally {
            if (parser != null) {
                SAXParserPool.getInstance().release(parser);
            }
        }
    }


}