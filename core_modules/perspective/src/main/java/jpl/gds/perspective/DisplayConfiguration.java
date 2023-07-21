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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.IViewConfigurationContainer;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * DisplayConfiguration contains all of the capability to load and save a
 * particular display from a user's perspective configuration.
 * 
 * This class is final because it does not define a proper clone method -- it
 * does not call super.clone.
 * 
 */
public final class DisplayConfiguration implements ExportableConfiguration,
        IViewConfigurationContainer, Cloneable, LockableElement {

	private static final Tracer log = TraceManager.getDefaultTracer();

	private DisplayType type;
	private String name;
	private ChillLocation location;
	private ChillFont dataFont = new ChillFont();
	private ChillColor backgroundColor = new ChillColor(ChillColor.ColorName.WHITE);
	private String configFile;
	private String configPath;
	private ChillSize size;
	private boolean locked;
	private boolean showHeader;
	private List<IViewConfiguration> viewConfigs;
	private Hashtable<String, String> configItems = new Hashtable<String,String>();

    /**
     * Gets the name
     * 
     * @return Returns the display name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name.
     * 
     * @param name
     *            The name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the display type (monitor, FSW downlink, SSE downlink, uplink,
     * unknown)
     * 
     * @return Returns the display type.
     */
    public DisplayType getType() {
        return type;
    }

    /**
     * Sets the display type.
     * 
     * @param type
     *            The type to set.
     */
    public void setType(final DisplayType type) {
        this.type = type;
    }

    /**
     * Sets the display's screen location.
     * 
     * @param location
     *            the ChillLocation to set
     */
    public void setLocation(final ChillLocation location) {
        this.location = location;
    }

    /**
     * Gets the display's screen location.
     * 
     * @return ChillLocation
     */
    public ChillLocation getLocation() {
        return location;
    }

    /**
     * Gets the default data display font.
     * 
     * @return ChillFont
     */
    public ChillFont getDataFont() {
        return dataFont;
    }

    /**
     * Sets the default data display font.
     * 
     * @param dataFont
     *            the ChillFont to set
     */
    public void setDataFont(final ChillFont dataFont) {
        this.dataFont = dataFont;
    }

    /**
     * Gets the default background color.
     * 
     * @return ChillColor
     */
    public ChillColor getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the default background color.
     * 
     * @param color
     *            the ChillColor to set
     */
    public void setBackgroundColor(final ChillColor color) {
        backgroundColor = color;
    }
    
    /**
     * Gets value of the boolean that determines if a header should be shown
     * on monitor displays
     * @return true if header should be displayed on all monitor windows
     */
    public boolean shouldShowHeader() {
        return showHeader;
    }

    /**
     * Sets the value of the boolean that determines if a header should be 
     * shown
     * @param showHeader true if headers should be on all displays, false 
     * otherwise
     */
    public void setShowHeader(final boolean showHeader) {
        this.showHeader = showHeader;
    }

	/**
	 * Returns an XML string representation of this configuration data.
	 * 
	 * @return XML string representing this object
	 */
	public String toXML() {
		final StringBuffer result = new StringBuffer();
		result.append("<DisplayConfiguration name=\"" + name + "\" showHeader=\"" + showHeader + "\" locked=\"" +
				locked + "\">\n");
		if (location != null) {
			result.append(location.toXml());
		}
		if (type != null) {
			result.append("   <type>" + type.getValueAsString() + "</type>\n");
		}
		if (size != null) {
			result.append(size.toXml());
		}
		if (dataFont != null) {
			result.append("   <defaultDataFont>" + dataFont.toXml() + "</defaultDataFont>\n");
		}
		if (backgroundColor != null) {
			result.append("   <defaultBackgroundColor>" + backgroundColor.toXml() + "</defaultBackgroundColor>\n");
		}

        result.append(getConfigItemXML());

        if (viewConfigs != null) {
            result.append("   <Views>\n");
            final Iterator<IViewConfiguration> it = viewConfigs.iterator();
            while (it.hasNext()) {
                final IViewConfiguration vc = it.next();
                result.append(vc.toXML());
            }
            result.append("   </Views>\n");
        }

        result.append("</DisplayConfiguration>");
        return result.toString();
    }

    /**
     * Get an XML string representing all the generic configuration items in
     * this ViewConfiguration.
     * 
     * @return an XML string
     */
    protected String getConfigItemXML() {
        final StringBuffer result = new StringBuffer("");
        if (configItems.size() > 0) {
            final Enumeration<String> e = configItems.keys();
            while (e.hasMoreElements()) {
                final String key = e.nextElement();
                final Object val = configItems.get(key);
                result.append("   <" + key + ">"
                        + StringEscapeUtils.escapeXml(val.toString()) + "</"
                        + key + ">\n");
            }
        }
        return result.toString();
    }

    /**
     * Loads the DisplayConfiguration from an xml file.
     * 
     * @param path
     *            the path to the user perspective
     * @param filename
     *            the name of the display file to load
     * @return true if the configuration is loaded
     */
    public boolean load(final ApplicationContext appContext, final String path, final String filename) {
        SAXParser sp = null;
        try {
            final String loadName = path + File.separator + filename;

            if (!new File(loadName).exists()) {
                log.error("Display configuration file " + loadName
                        + " does not exist");
                return false;
            }
            setConfigPath(path);
            setConfigFile(filename);
            final DisplayConfigSaxHandler tcSax = new DisplayConfigSaxHandler();
            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(loadName, tcSax);
            if (type.equals(DisplayType.MESSAGE)) {
                appContext.getBean(PerspectiveCounters.class).reset();
            }
            viewConfigs = IViewConfiguration.load(appContext, loadName);
            if (type.equals(DisplayType.MESSAGE)) {
            	appContext.getBean(PerspectiveCounters.class).logCounters();
            }
            if (viewConfigs == null) {
                log
                        .error("Could not load any view configurations for display configuration "
                                + loadName);
                log.warn("Will assume no views are defined");
                viewConfigs = new ArrayList<IViewConfiguration>();
            }
            return true;
        } catch (final SAXException e) {
            log.error("Unable to parse display configuration file " + path
                    + "/" + filename);
            log.error(e.getMessage());
            return false;
        } catch (final Exception e) {
            e.printStackTrace();
            log.error("Unable to parse display configuration file " + path
                    + "/" + filename);
            log.error("Unexpected error: " + e.toString());
            return false;
        }
    }

    /**
     * Sets the name of the display configuration file.
     * 
     * @param filename
     *            the filename (no path)
     */
    public void setConfigFile(final String filename) {
        configFile = filename;
    }

    /**
     * Saves the display configuration to a file.
     * 
     * @param file
     *            the display file name (no path)
     * @throws IOException thrown if given file name is not found or cannot be 
     *            created
     */
    public void save(final String file) throws IOException {
        this.setConfigFile(file);
        final String saveFile = getConfigPath() + File.separator + file;
        final File path = new File(getConfigPath());

        if (!path.exists() && !path.mkdirs()) {
            throw new IOException("Unable to mkdirs: " + path);
        }

        final FileWriter fos = new FileWriter(saveFile);
        fos.write("<?xml version=\"1.0\"?>\n");
        fos.write(this.toXML());
        fos.close();
    }

    /**
     * DisplayConfigSaxHandler is the XML parse handler for display
     * configuration files.
     * 
     *
     */
    private class DisplayConfigSaxHandler extends DefaultHandler {
        protected StringBuffer text = new StringBuffer();
        private int xPos = -1;
        private int yPos = -1;
        private int xW = -1;
        private int yH = -1;
        private int red = -1;
        private int green = -1;
        private int blue = -1;

        private String fontFace;
        private ChillFont.FontSize fontSize;
        private int fontStyle;

        private boolean inViews = false;

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            if ((xPos != -1) && (yPos != -1)) {
                location = new ChillLocation(
                        xPos, yPos);
            }
            if ((xW != -1) && (yH != -1)) {
                size = new ChillSize(xW, yH);
            }
            if (dataFont == null) {
                dataFont = new ChillFont();
                if (fontFace != null) {
                    dataFont.setFont(fontFace,
                            fontSize, fontStyle);
                }
            }
            if (backgroundColor == null) {
                if (red != -1) {
                    backgroundColor = new ChillColor(
                            red, green, blue);
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         * java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName, final String qname,
                final Attributes attr) throws SAXException {
            text = new StringBuffer();
            if (qname.equals("DisplayConfiguration")) {
                name = attr.getValue("name");
                
                final String showHead = attr.getValue("showHeader");
                if (showHead == null) {
                    showHeader = false;
                } else {
                    showHeader = XmlUtility.getBooleanFromText(showHead);
                }
                
                final String lockStr = attr.getValue("locked");
                if (lockStr == null) {
                    locked = false;
                } else {
                    locked = XmlUtility.getBooleanFromText(lockStr);
                }
            } else if (qname.equals("Views")) {
                inViews = true;
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
                text.append(newText);
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
            if (qname.equalsIgnoreCase("DisplayConfiguration")) {
                // nothing
            } else if (qname.equals(ChillLocation.X_LOCATION_TAG)
                    && !inViews) {
                try {
                    final String xStr = text.toString();
                    xPos = Integer.parseInt(xStr);
                } catch (final NumberFormatException e) {
                    throw new SAXException("x position must be an integer");
                }
            } else if (qname.equals(ChillLocation.Y_LOCATION_TAG)
                    && !inViews) {
                try {
                    final String yStr = text.toString();
                    yPos = Integer.parseInt(yStr);
                } catch (final NumberFormatException e) {
                    throw new SAXException("y position must be an integer");
                }
            } else if (qname.equals(ChillSize.X_WIDTH_TAG) && !inViews) {
                try {
                    final String xWidth = text.toString();
                    xW = Integer.parseInt(xWidth);
                } catch (final NumberFormatException e) {
                    throw new SAXException("x position must be an integer");
                }
            } else if (qname.equals(ChillSize.Y_HEIGHT_TAG) && !inViews) {
                try {
                    final String yHeight = text.toString();
                    yH = Integer.parseInt(yHeight);
                } catch (final NumberFormatException e) {
                    throw new SAXException("y position must be an integer");
                }
            } else if (qname.equals(ChillColor.RED_COLOR_TAG) && !inViews) {
                try {
                    final String color = text.toString();
                    red = Integer.parseInt(color);
                } catch (final NumberFormatException e) {
                    throw new SAXException("red color level must be an integer");
                }
            } else if (qname.equals(ChillColor.GREEN_COLOR_TAG)
                    && !inViews) {
                try {
                    final String color = text.toString();
                    green = Integer.parseInt(color);
                } catch (final NumberFormatException e) {
                    throw new SAXException(
                            "green color level must be an integer");
                }
            } else if (qname.equals(ChillColor.BLUE_COLOR_TAG) && !inViews) {
                try {
                    final String color = text.toString();
                    blue = Integer.parseInt(color);
                } catch (final NumberFormatException e) {
                    throw new SAXException(
                            "blue color level must be an integer");
                }
            } else if (qname.equals("type") && !inViews) {
                final String type = text.toString();
                final DisplayType dt = new DisplayType(type);
                setType(dt);

            } else if (qname.equalsIgnoreCase("defaultDataFont")
                    && !inViews) {
                dataFont = new ChillFont();
                dataFont.setFont(text.toString()
                        .trim());

            } else if (qname.equalsIgnoreCase("defaultBackgroundColor")
                    && !inViews) {
                backgroundColor = new ChillColor();
                backgroundColor.setColors(text
                        .toString().trim());

            } else if (qname.equals(ChillFont.FONT_FACE_TAG) && !inViews) {
                fontFace = text.toString();

            } else if (qname.equals(ChillFont.FONT_SIZE_TAG) && !inViews) {
                try {
                    final String sizeStr = text.toString();
                    fontSize = Enum.valueOf(ChillFont.FontSize.class,
                            sizeStr);
                } catch (final NumberFormatException e) {
                    throw new SAXException("font size must be an integer");
                }
            } else if (qname.equals(ChillFont.FONT_STYLE_TAG) && !inViews) {
                try {
                    final String styleStr = text.toString();
                    fontStyle = Integer.parseInt(styleStr);
                } catch (final NumberFormatException e) {
                    throw new SAXException("font style must be an integer");
                }
            } else if (qname.equals("isOpen")) {
                // old version - ignore
            } else if (qname.equalsIgnoreCase("Views")) {
                inViews = false;
            } else if (!inViews) {
                setConfigItem(qname, text.toString());
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
                    "Parse error in display configuration file line "
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
                    "Fatal parse error in display configuration file line "
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
            log.warn("Parse warning in display configuration file line "
                    + e.getLineNumber() + ", column " + e.getColumnNumber()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Gets the name of the configuration's file name
     * @return the configuration file name.
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * Saves the display configuration to the default configuration file.
     * 
     * @throws IOException thrown if configuration file cannot be saved
     */
    public void save() throws IOException {
        save(getConfigFile());
    }

    /**
     * Retrieves path to the display configuration file
     * 
     * @return the path to the display configuration file (no file name).
     */
    public String getConfigPath() {
        return configPath;
    }

    /**
     * Sets the path to the display configuration file (no file name).
     * 
     * @param configPath
     *            The path to set.
     */
    public void setConfigPath(final String configPath) {
        this.configPath = configPath;
    }

    /**
     * Retrieves the display's screen size
     * 
     * @return Returns the display's screen size.
     */
    public ChillSize getSize() {
        return size;
    }

    /**
     * Sets the display's screen size.
     * 
     * @param size
     *            The size to set.
     */
    public void setSize(final ChillSize size) {
        this.size = size;
    }

    /**
     * Adds a view configuration.
     * 
     * @param vc
     *            the ViewConfiguration to add
     */
    @Override
	public void addViewConfiguration(final IViewConfiguration vc) {
        if (viewConfigs == null) {
            viewConfigs = new ArrayList<IViewConfiguration>();
        }
        viewConfigs.add(vc);
    }

    /**
     * Reviews a view configuration.
     * 
     * @param vc
     *            the ViewConfiguration to remove
     */
    @Override
	public void removeViewConfiguration(final IViewConfiguration vc) {
        if (viewConfigs == null) {
            return;
        }
        viewConfigs.remove(vc);
    }

    /**
     * Gets the list of view configurations.
     * 
     * @return an ArrayList of ViewConfiguration objects
     */
    public List<IViewConfiguration> getViewConfigs() {
        if (viewConfigs == null) {
            viewConfigs = new ArrayList<IViewConfiguration>();
        }
        return viewConfigs;
    }

    /**
     * Retrieves the first ViewConfiguration in the application configuration
     * that has the given view type.
     * 
     * @param type
     *            the ViewType to match
     * @return the ViewConfiguration found, or null if no match
     */
    public IViewConfiguration getViewConfig(final ViewType type) {
        final Iterator<IViewConfiguration> it = viewConfigs.iterator();
        while (it.hasNext()) {
            final IViewConfiguration current = it.next();
            if (current.getViewType().equals(type)) {
                return current;
            }
        }
        return null;
    }

    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.ExportableConfiguration#exportToPath(
     * java.lang.String)
     */
    @Override
	public boolean exportToPath(final String path) {
        try {

            final FileWriter fos = new FileWriter(path);
            fos.write("<?xml version=\"1.0\"?>\n");
            fos.write(this.toXML());
            fos.close();
        } catch (final IOException e) {
            log.error("Unable to export display configuration to file " + path);
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.IViewConfigurationContainer#getViews()
     */
    @Override
	public List<IViewConfiguration> getViews() {
        return viewConfigs;
    }

    /**
     * {@inheritDoc}
     * jpl.gds.perspective.view.ViewConfigurationContainer#isImportViews()
     */
    @Override
	public boolean isImportViews() {
        return true;
    }

    /**
     * {@inheritDoc}
     * @see
     * jpl.gds.perspective.view.IViewConfigurationContainer#setViews(java
     * .util.List)
     */
    @Override
	public void setViews(final List<IViewConfiguration> vcList) {
        viewConfigs = vcList;
    }

    /**
     * {@inheritDoc}
     * @see
     * jpl.gds.perspective.view.IViewConfigurationContainer#isRemovable()
     */
    @Override
	public boolean isRemovable() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see
     * jpl.gds.perspective.view.IViewConfigurationContainer#isWindowContainer
     * ()
     */
    @Override
	public boolean isWindowContainer() {
        return true;
    }

    /**
     * This method is final because it is not a proper clone -- it does not call
     * super.clone.
     * 
     * @return Cloned object
     * @throws CloneNotSupportedException indicates that an instance cannot be cloned.
     */
    @Override
    public final Object clone() throws CloneNotSupportedException {
        final DisplayConfiguration newConfig = new DisplayConfiguration();
        newConfig.type = new DisplayType(type.getValueAsInt());
        newConfig.name = name;
        if (location != null) {
            newConfig.location = new ChillLocation(
                    location.getXPosition(), location.getYPosition());
        }
        if (dataFont != null) {
            newConfig.dataFont = new ChillFont(dataFont.getFace(),
                    dataFont.getSize(), dataFont.getStyle());
        }
        if (backgroundColor != null) {
            newConfig.backgroundColor = new ChillColor(backgroundColor
                    .getRed(), backgroundColor.getGreen(),
                    backgroundColor.getBlue());
        }
        newConfig.configFile = configFile;
        newConfig.configPath = configPath;
        if (size != null) {
            newConfig.size = new ChillSize(size.getXWidth(), size
                    .getYHeight());
        }

        if (viewConfigs != null) {
            final Iterator<IViewConfiguration> it = viewConfigs.iterator();
            while (it.hasNext()) {
                newConfig.addViewConfiguration(it.next());
            }
        }
        return newConfig;
    }

    /**
     * Gets the hash table of configuration items for this view
     * @return Returns the hash table of configuration items for this View.
     */
    public Hashtable<String, String> getConfigItems() {
        return configItems;
    }

    /**
     * Removes the given configuration item.
     * 
     * @param name
     *            the name of the configuration item to remove.
     */
    public void removeConfigItem(final String name) {
        configItems.remove(name);
    }

    /**
     * Sets the configuration items for this view.
     * 
     * @param configItems
     *            The hash table of configuration items to set.
     */
    public void setConfigItems(final Hashtable<String, String> configItems) {
        this.configItems = configItems;
    }

    /**
     * Sets a single configuration item.
     * 
     * @param name
     *            the name of the configuration item (should be the same as its
     *            XML tag name)
     * @param value
     *            the value of the configuration item
     */
    public void setConfigItem(final String name, final String value) {
        configItems.put(name, value);
    }

    /**
     * Gets the value of a configuration item.
     * 
     * @param name
     *            the name of the configuration item to get (should be the same
     *            as its XML tag name).
     * @return the value of the configuration item as a String, or null if not
     *         found
     */
    public String getConfigItem(final String name) {
        return configItems.get(name);
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
    }

    /**
     * Removes all view references from the list of view configurations
     */
    public void clearViewReferences() {
        for (final IViewConfiguration view : viewConfigs) {
            view.clearViewReferences();
        }
    }
}
