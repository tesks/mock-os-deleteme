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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.ExportableConfiguration;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;

/**
 * ViewConfiguration holds the configuration parameters for a single view
 * in a perspective. It is sub-classed to provide more specific configuration
 * methods.
 * 
 * Note that the static ViewConfiguration parse method knows how to parse
 * for the subclasses. 
 *
 */
public abstract class ViewConfiguration implements ExportableConfiguration, IViewConfiguration {

	/**
	 * Default tracer used for logging helpful information
	 */
	protected final Tracer trace;


	private static final String DEFAULT_SIZE_PROPERTY = "defaultSize";
	private static final String OPEN_TAG = "<";
	private static final String CLOSE_TAG = ">";
	private static final String OPEN_END_TAG = "</";
	private static final String OPEN_INDENT_TAG = "   <";
	private static final String END_TAG_LF = ">\n";
	private static final String EQUALS_WITH_QUOTE = "=\"";
	private static final String QUOTE_SPACE = "\" ";
	
	/**
	 * View name string
	 */
	protected String viewName;
	
	/**
	 * View configuration class
	 */
	private Class<?> viewClass;
	
	private Class<?> viewTabClass;
	
	private Class<?> viewPreferencesClass;
	
	/**
	 * View configuration class name
	 */
	private String viewClassName;
	
	private String viewPreferencesClassName;
	
	private String viewTabClassName;
	
	/**
	 * Screen location of the view configuration
	 */
	private ChillLocation location;
	
	/**
	 * Size of this view configuration window
	 */
	private ChillSize size;
	
	private Hashtable<String, String> configItems = new Hashtable<String,String>();
	private ChillFont dataFont = new ChillFont();
	private ChillColor backgroundColor = new ChillColor(ChillColor.ColorName.WHITE);
	private ChillColor foregroundColor = new ChillColor(ChillColor.ColorName.BLACK);
	
	/**
	 * Enumerated view type
	 */
	protected ViewType viewType;
	
	private final Map<String,ChillTable> tables = new HashMap<String,ChillTable>();
	private final List<ViewConfigurationListener> listeners = new ArrayList<ViewConfigurationListener>();
	
	/**
	 * Version number of this view configuration
	 */
	private int version = 0;
	
	private boolean merged= false;
	
	/**
	 * View reference, if this view is a reference to another view.
	 */
	protected ViewReference reference;
	
	/**
	 * Configuration properties for this view.
	 */
	protected ViewProperties viewProperties;

	/** 
	 * The current application context.
	 */
	protected ApplicationContext appContext;
	
    protected final SseContextFlag                sseFlag;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	protected ViewConfiguration(final ApplicationContext appContext) {
		this.appContext = appContext;
        this.trace = TraceManager.getDefaultTracer(appContext);
        this.sseFlag = appContext.getBean(SseContextFlag.class);
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public void setViewProperties(final ViewProperties props) {
	    this.viewProperties = props;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean isMerged() {
		return merged;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setMerged(final boolean merged) {
		this.merged = merged;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillLocation getLocation() {
		return location;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setLocation(final ChillLocation location) {
		this.location = location;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillSize getSize() {
		return size;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setSize(final ChillSize size) {
		this.size = size;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillFont getDataFont() {
		return dataFont;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setDataFont(final ChillFont dataFont) {
		this.dataFont = dataFont;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillColor getBackgroundColor() {
		return backgroundColor;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setBackgroundColor(final ChillColor color) {
		backgroundColor = color;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillColor getForegroundColor() {
		return foregroundColor;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setForegroundColor(final ChillColor color) {
		foregroundColor = color;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ViewType getViewType() {
		return viewType;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setViewType(final ViewType viewType) {
		this.viewType = viewType;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public Class<?> getViewClass() {
		if (viewClass == null) {
			if (viewClassName == null) {
				throw new IllegalStateException("View class has not been set");
			} else {
				try {
					viewClass = Class.forName(viewClassName);
				} catch (final ClassNotFoundException e) {
					e.printStackTrace();
					TraceManager.getDefaultTracer().error("Unable to locate class " + viewClassName);

				}
			}
		}
		return viewClass;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setViewClass(final String className) {
		viewClassName = className;
		notifyListeners();
	}
	
	
	   /**
     * {@inheritDoc}
     */
    @Override
    public void setViewPreferencesClass(final String className) {
        viewPreferencesClassName = className;
        notifyListeners();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewTabClass(final String className) {
        viewTabClassName = className;
        notifyListeners();
    }
      
	/**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getViewTabClass() {
        
        if (viewTabClass == null) {
            if (viewTabClassName == null) {
                throw new IllegalStateException("View tab class has not been set");
            } else {
                try {
                    viewTabClass = Class.forName(viewTabClassName);
                } catch (final ClassNotFoundException e) {
                    e.printStackTrace();
                    TraceManager.getDefaultTracer().error("Unable to locate class " + viewTabClassName);

                }
            }
        }
        return viewTabClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getViewPreferencesClass() {
        if (viewPreferencesClass == null) {
            if (viewPreferencesClassName == null) {
                throw new IllegalStateException("View preferences class has not been set");
            } else {
                try {
                    viewPreferencesClass = Class.forName(viewPreferencesClassName);
                } catch (final ClassNotFoundException e) {
                    e.printStackTrace();
                    TraceManager.getDefaultTracer().error("Unable to locate class " + viewPreferencesClassName);

                }
            }
        }
        return viewPreferencesClass;
    }


	/**
     * {@inheritDoc}
     */
	@Override
    public Hashtable<String,String> getConfigItems() {
		return configItems;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void removeConfigItem(final String name) {
		configItems.remove(name);
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setConfigItems(final Hashtable<String,String> configItems) {
		this.configItems = configItems;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getViewName() {
		return viewName;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setViewName(final String viewName) {
		this.viewName = viewName;
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setConfigItem(final String name, final String value) {
	    configItems.put(name, value);
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setConfigItem(final String name, final String[] value) {
		final StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < value.length; i++) {
			buffer.append(value[i]);
			if (i < value.length - 1) {
				buffer.append(",");
			}
		}
		configItems.put(name, buffer.toString());
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getConfigItem(final String name) {
		return configItems.get(name);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String[] getConfigItemList(final String name) {
		final String val = configItems.get(name);
		if (val == null) {
			return null;
		}
		return val.split(",");
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String toXML() { 
		final StringBuilder result = new StringBuilder();
		if (this.isReference()) {
			result.append(reference.toXml());
		} else {
			result.append(OPEN_TAG + VIEW_TAG + " " + VIEW_NAME_TAG + EQUALS_WITH_QUOTE + StringEscapeUtils.escapeXml(viewName) + QUOTE_SPACE);
			result.append(VIEW_TYPE_TAG + EQUALS_WITH_QUOTE + viewType.getValueAsString() + QUOTE_SPACE);
			result.append(VIEW_VERSION_TAG + EQUALS_WITH_QUOTE + WRITE_VERSION + "\">\n");
			result.append(getAttributeXML());
			result.append(getTableXML());
			result.append(getConfigItemXML());
			result.append(OPEN_END_TAG + VIEW_TAG + END_TAG_LF);
		}
		return result.toString();
	}

	/**
	 * Get an XML string representing the basic attributes of this 
	 * ViewConfiguration.
	 * @return an XML string
	 */
	protected String getAttributeXML() {
		final StringBuilder result = new StringBuilder();

		if (this.isReference()) {
			result.append(reference.toXml());

		} else {
			if (viewClassName != null) {
				result.append(OPEN_INDENT_TAG + VIEW_CLASS_TAG + CLOSE_TAG + viewClassName + OPEN_END_TAG + VIEW_CLASS_TAG + END_TAG_LF);
			}
			if (location != null) {
				result.append(location.toXml());
			}
			if (size != null) {
				result.append(size.toXml());
			}
			if (dataFont != null) {
				result.append(OPEN_INDENT_TAG + VIEW_FONT_TAG + CLOSE_TAG);
				result.append(dataFont.toXml());
				result.append(OPEN_END_TAG + VIEW_FONT_TAG + END_TAG_LF);
			}
			if (backgroundColor != null) {
				result.append(OPEN_INDENT_TAG + VIEW_BACKGROUND_TAG + CLOSE_TAG);
				result.append(backgroundColor.toXml());
				result.append(OPEN_END_TAG + VIEW_BACKGROUND_TAG + END_TAG_LF);
			}
			if (foregroundColor != null) {
				result.append(OPEN_INDENT_TAG + VIEW_FOREGROUND_TAG + CLOSE_TAG);
				result.append(foregroundColor.toXml());
				result.append(OPEN_END_TAG + VIEW_FOREGROUND_TAG + END_TAG_LF);
			}
		}
		return result.toString();
	}

	/**
	 * Get an XML string representing all the table definitions attached to this
	 * view configuration.
	 * @return an XML String
	 */
	protected String getTableXML() {
		final StringBuilder result = new StringBuilder();
		synchronized(tables) {
			final Collection<ChillTable> items = tables.values(); 
			final Iterator<ChillTable> it = items.iterator();
			while (it.hasNext()) {
				result.append(it.next().toXml());
			}
		}
		return result.toString();
	}

	/**
	 * Get an XML string representing all the generic configuration items in 
	 * this ViewConfiguration.
	 * @return an XML string
	 */
	protected String getConfigItemXML() {
		final StringBuilder result = new StringBuilder("");
		if (configItems.size() > 0) {
			final Enumeration<String> e = configItems.keys();
			while (e.hasMoreElements()) {
				final String key = e.nextElement();
				final Object val = configItems.get(key);
				result.append(OPEN_INDENT_TAG + key + CLOSE_TAG + StringEscapeUtils.escapeXml(val.toString()) + OPEN_END_TAG + key + END_TAG_LF);
			}
		}
		return result.toString();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void save(final String filename) throws IOException {
		FileWriter fw = null;
		try {
			final String output = toXML();
			fw = new FileWriter(filename);
			fw.write(output);
		} finally {
			fw.close();
	    }
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.ExportableConfiguration#exportToPath(java.lang.String)
	 */
	@Override
	public boolean exportToPath(final String path) {
		FileWriter fos = null;

		try {    
			fos = new FileWriter(path);
			fos.write("<?xml version=\"1.0\"?>\n");
			fos.write(this.toXML());
		} catch (final IOException e) {
			trace.error("Unable to export view configuration to file " + path);
			return false;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}



	/**
     * {@inheritDoc}
     */
	@Override
    public void addTable(final ChillTable table) {
		synchronized(tables) {
			tables.put(table.getName(), table);
		}
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void removeTable(final ChillTable table) {
		synchronized(tables) {
			tables.remove(table.getName());
		}
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillTable getTable(final String name) {
		return tables.get(name);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setTable(final ChillTable table) {
		synchronized(tables) {
			tables.put(table.getName(), table);
		}
		notifyListeners();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void addConfigurationListener(final ViewConfigurationListener l) {
		synchronized(listeners) {
			listeners.add(l);
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void removeConfigurationListener(final ViewConfigurationListener l) {
		synchronized(listeners) {
			listeners.remove(l);
		}
	}

	@SuppressWarnings("unchecked")
	private void notifyListeners() {
		List<ViewConfigurationListener> tempListeners = null;
		synchronized(listeners) {
			try {
				tempListeners = (List<ViewConfigurationListener>)((ArrayList<ViewConfigurationListener>)listeners).clone();
			} catch (final Exception e) {
				e.printStackTrace();
				return;
			}
		}
		final Iterator<ViewConfigurationListener> it = tempListeners.iterator();
		while (it.hasNext()) {
			it.next().configurationChanged(this);
		}
	}

	/**
	 * Initializes view configuration defaults from the GDS configuration.
	 *
	 */
	protected abstract void initToDefaults();

	/**
	 * Initializes view defaults from the GDS configuration file. Only initializes attributes generic to
	 * all view types.
	 * @param props the ViewProperties object
	 * @param viewClass the view class name
	 * @param tabClass  the view tab class name
	 * @param prefClass the view preferences class name
	 */
	protected void initToDefaults(final ViewProperties props, final String viewClass, final String tabClass, final String prefClass) {
		setViewType(props.getViewType());
		setViewProperties(props);
		setViewClass(viewClass);
		setViewTabClass(tabClass);
		setViewPreferencesClass(prefClass);
		
		final String name = props.getStringDefault(DEFAULT_NAME_PROPERTY);
		if (name == null) {
			trace.debug("Default name for view " + props.getViewType() + " is not defined in the configuration file");
		}
		setViewName(name);
		final String size = props.getStringDefault(DEFAULT_SIZE_PROPERTY);
		if (size != null) {
			final String[] sizePieces = size.split(",");
			if (sizePieces.length == 2) {
				final int sizeX = Integer.parseInt(sizePieces[0]);
				final int sizeY = Integer.parseInt(sizePieces[1]);
				setSize(new ChillSize(sizeX, sizeY));
			} else {
				trace.warn("Default size for view " + props.getViewType() + " + has the wrong format in the configuration file");
			}
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getVersion() {
		return version;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setVersion(final int version) {
		this.version = version;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setDisplayViewTitle(final boolean enable) {
		this.setConfigItem(VIEW_TITLE_ENABLE_TAG, String.valueOf(enable));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean isDisplayViewTitle() {
		final String bool = this.getConfigItem(VIEW_TITLE_ENABLE_TAG);
		if (bool == null) {
			setDisplayViewTitle(true);
			return true;
		} else {
			return Boolean.parseBoolean(bool);
		}
	}

	/**
	 * Utility method to determine if a string array contains a specific string.
	 * @param text the string to search for
	 * @param list the array of strings to search
	 * @return true if the string is present in the array
	 */
	protected boolean arrayContainsString(final String text, final String[] list) {
		for (int i = 0; i < list.length; i++) {
			if (list[i].equalsIgnoreCase(text)) {
				return true;
			}
		}
		return false;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getWindowTitle(String contextName, String contextId) {
		
		if (contextName == null) {
			contextName = "Unknown Name";
		}
		
		if (contextId == null) {
		    contextId = "Unknown ID";
		}


        final StringBuilder titlePrefix = new StringBuilder(getViewName());

		if (titlePrefix.indexOf(IViewConfiguration.NAME_TOKEN) != -1) {
			final int index = titlePrefix.indexOf(IViewConfiguration.NAME_TOKEN);
			final int len = IViewConfiguration.NAME_TOKEN.length();

            final String left  = titlePrefix.substring(0, index);
            final String right = titlePrefix.substring(index + len);

            titlePrefix.setLength(0);

            titlePrefix.append(left);
            titlePrefix.append(contextName);
            titlePrefix.append(right);
		} 

		if (titlePrefix.indexOf(IViewConfiguration.ID_TOKEN) != -1) {
			final int index = titlePrefix.indexOf(IViewConfiguration.ID_TOKEN);
			final int len = IViewConfiguration.ID_TOKEN.length();

            final String left  = titlePrefix.substring(0, index);
            final String right = titlePrefix.substring(index + len);

            titlePrefix.setLength(0);

            titlePrefix.append(left);

            titlePrefix.append(contextId);
            
            // Not supporting such session-specific capability
            // any more.  Pass the desired display value for
            // contextId.
//
//            if (SessionFragmentHolder.DISPLAY_FRAGMENT)
//            {
//                titlePrefix.append('/');
//                titlePrefix.append(testConfig.getSessionFragment());
//            }

            titlePrefix.append(right);
		}
        else
        {
			titlePrefix.append(" (");
			titlePrefix.append(contextId);

	        // Not supporting such session-specific capability
            // any more.  Pass the desired display value for
            // contextId.
//			
//            if (SessionFragmentHolder.DISPLAY_FRAGMENT)
//            {
//                titlePrefix.append('/');
//                titlePrefix.append(testConfig.getSessionFragment());
//            }

			titlePrefix.append(')');
		}


		return(titlePrefix.toString());
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ViewReference getViewReference() {
		return reference;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setViewReference(final ViewReference reference) {
		this.reference = reference;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean isReference() {
		return reference != null;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setPreferredHeight(final int height) {
		setConfigItem(VIEW_HEIGHT_TAG, String.valueOf(height));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getPreferredHeight() {
		final String str = getConfigItem(VIEW_HEIGHT_TAG);
		if (str == null) {
			return 0;
		}
		return Integer.valueOf(str);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setPreferredWidth(final int width) {
		setConfigItem(VIEW_WIDTH_TAG, String.valueOf(width));
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public int getPreferredWidth() {
		final String str = getConfigItem(VIEW_WIDTH_TAG);
		if (str == null) {
			return 0;
		}
		return Integer.valueOf(str);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void clearViewReferences() {

		setViewReference(null);

		if (this instanceof IViewConfigurationContainer) {
			final IViewConfigurationContainer container = (IViewConfigurationContainer)this;
			if (container.getViews() != null) {
				for (final IViewConfiguration view : container.getViews()) {
					view.clearViewReferences();
				}
			}
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public List<ViewReference> getAllViewReferences() {

		final ArrayList<ViewReference> results = new ArrayList<ViewReference>();

		if (reference != null) {
			results.add(reference);
		}

		if (this instanceof IViewConfigurationContainer) {
			final IViewConfigurationContainer container = (IViewConfigurationContainer)this;
			if (container.getViews() != null) {
				for (final IViewConfiguration view : container.getViews()) {
   				    results.addAll(view.getAllViewReferences());
			    }
			}	
		}
		return results;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getSseVersion() {
		return this.getConfigItem(SSE_DICT_VERSION_TAG);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getFswVersion() {
		return this.getConfigItem(FSW_DICT_VERSION_TAG);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getSseDictionaryDir() {
		return this.getConfigItem(SSE_DICT_DIR_TAG);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getFswDictionaryDir() {
		return this.getConfigItem(FSW_DICT_DIR_TAG);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setSseVersion(final String version) {
		this.setConfigItem(SSE_DICT_VERSION_TAG, version);		
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setFswVersion(final String version) {
		this.setConfigItem(FSW_DICT_VERSION_TAG, version);		
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setFswDictionaryDir(final String dir) {
		this.setConfigItem(FSW_DICT_DIR_TAG, dir);		
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setSseDictionaryDir(final String dir) {
		this.setConfigItem(SSE_DICT_DIR_TAG, dir);		
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setDeprecations() {	
		// Default implementation does nothing. Overridden in subclasses.
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public IViewConfigParser getParser(final ApplicationContext appContext) {
	    return null;
	}
}
