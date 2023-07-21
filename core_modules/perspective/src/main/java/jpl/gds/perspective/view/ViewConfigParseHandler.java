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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.ChillTableColumn;
import jpl.gds.perspective.PerspectiveCounters;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillLocation;
import jpl.gds.shared.swt.types.ChillSize;

/**
 * ViewConfigParseHandler is the SAX parse handler for the view configurations
 * in a display configuration file. Once parsing is complete, the list of view
 * configurations created can be obtained from this class.
 * 
 *
 */
public class ViewConfigParseHandler extends DefaultHandler {

	private final Tracer tracer; 


	private final Stack<IViewConfigurationContainer> containerStack = new Stack<IViewConfigurationContainer>();
	private IViewConfigurationContainer containerConfig;
	private boolean inChildView;
	private boolean inContainerView;
	private boolean inStandaloneView;
	private boolean inViews;
	private boolean inView;
	private IViewConfigParser customViewParser;
	private boolean inTable;
	private boolean inBackgroundColor;
	private boolean inForegroundColor;
	private IViewConfiguration childView;
	private StringBuilder buffer = new StringBuilder();
	private ChillLocation position;
	private ChillSize size;
	private ChillColor backgroundColor;
	private ChillColor foregroundColor;
	private ChillFont font;
	private ChillTable table;
	private String[] tableColNames;
	private Integer[] tableColWidths;
	private Boolean[] enabledCols;
	private List<ChillTableColumn> tableColumns;
	private String sortColumn;
	private int readVersion;
	private final PerspectiveCounters counters;
	private final List<IViewConfiguration> parsedViews = new ArrayList<IViewConfiguration>();
	private IViewConfiguration viewConfig;

	private final ApplicationContext appContext;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public ViewConfigParseHandler(final ApplicationContext appContext) {
		this.appContext = appContext;
        this.tracer = TraceManager.getDefaultTracer(appContext);
		counters = appContext.getBean(PerspectiveCounters.class);
	}
	
	/**
	 * Retrieves the list of view configurations parsed by the last parser run.
	 * 
	 * @return the list of ViewConfigurations, or an empty list of none parsed.
	 */
	public List<IViewConfiguration> getParsedConfigs() {
		return parsedViews;
	}

	/**
     * {@inheritDoc}
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(final char[] chars, final int start, final int length)
	throws SAXException {

		if (buffer == null) {
			newBuffer();
		}
		final String newText = new String(chars, start, length);
		if (!newText.equals("\n")) {
			buffer.append(newText);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(final String uri, final String localName, final String qname,
			final Attributes attr) throws SAXException {
		newBuffer();

		if (qname.equalsIgnoreCase("Views")) {
			inViews = true;
		}
		else if (qname.equals(IViewConfigurationContainer.VIEW_CONTAINER_TAG)) {
			parseViewContainer(attr);
		}
		else if (qname.equals(IViewConfiguration.VIEW_TAG)) {
			parsePlainView(attr);
		}
		else if (qname.equals(ViewReference.VIEW_REFERENCE_TAG)) {
			parseViewReference(attr);
		}
		else if (qname.equals(ChillTable.TABLE_SETTINGS_TAG) && inViews) {
			parseTable(attr);
		}
		// OLD FORMAT BACKGROUND COLOR:
		else if (qname.equals(IViewConfiguration.OLD_VIEW_BACKGROUND_TAG)
				&& inViews) {
			inBackgroundColor = true;
		}
		// OLD FORMAT FOREGROUND COLOR:
		else if (qname.equals(IViewConfiguration.OLD_VIEW_FOREGROUND_TAG)
				&& inViews) {
			inForegroundColor = true;
		}
		else if (qname.equals(ChillTableColumn.COLUMN_TAG) && inTable
				&& table.getVersion() > 0) {
			parseTableColumn(attr);
		} else if(this.inView && this.customViewParser != null) {
            this.customViewParser.startElement(uri, localName, qname, attr);
        }
	}

	private void parseTableColumn(final Attributes attr) {
		final ChillTableColumn baseCol = table.getColumn(attr
				.getValue(ChillTableColumn.OFFICIAL_NAME_TAG));
		final ChillTableColumn col = new ChillTableColumn();
		col
		.setDisplayName(attr
				.getValue(ChillTableColumn.DISPLAY_NAME_TAG));
		col.setOfficialName(attr
				.getValue(ChillTableColumn.OFFICIAL_NAME_TAG));
		col.setConfigurationNumber(Integer.parseInt(attr
				.getValue(ChillTableColumn.CONFIG_NUMBER_TAG)));
		col.setCurrentPosition(Integer.parseInt(attr
				.getValue(ChillTableColumn.POSITION_TAG)));
		col.setDefaultWidth(Integer.parseInt(attr
				.getValue(ChillTableColumn.DEFAULT_WIDTH_TAG)));
		final String colWidth = attr.getValue(ChillTableColumn.CURRENT_WIDTH_TAG);
		if (colWidth == null) {
			col.setCurrentWidth(col.getDefaultWidth());
		} else {
			final int colw = Integer.parseInt(colWidth);
			if (colw != 0) {
				col.setCurrentWidth(colw);
			} else {
				col.setCurrentWidth(col.getDefaultWidth());
			}
			
		}
		col.setEnabled(Boolean.valueOf(attr
				.getValue(ChillTableColumn.ENABLED_TAG)));
		col.setSortColumn(Boolean.valueOf(attr
				.getValue(ChillTableColumn.SORT_TAG)));
		if (baseCol != null) {
			col.setSortType(baseCol.getSortType());
		}
		tableColumns.add(col);
	}

	private void parseTable(final Attributes attr) throws SAXException {
		if (inContainerView && !(inChildView || inStandaloneView)) {
			throw new SAXException(
			"Parsing of tables in container views is not supported");
		}
		inTable = true;
		int version = 0;
		final String versionStr = attr.getValue(ChillTable.VERSION_TAG);
		if (versionStr != null) {
			version = Integer.parseInt(versionStr);
		}
		final String tableName = attr.getValue(ChillTable.TABLE_NAME_TAG);
		if (tableName == null) {
			throw new SAXException(
			"Table name cannot be null in table settings");
		}
		if (inChildView) {
			table = childView.getTable(tableName);
		} else {
			table = viewConfig.getTable(tableName);
		}
		if (table == null) {
			table = new ChillTable();
			table.setName(tableName);
		}
		table.setVersion(version);
		tableColumns = new ArrayList<ChillTableColumn>();
	}

	private void parseViewReference(final Attributes attr) throws SAXException {
		final String type = attr.getValue(ViewReference.TYPE_TAG);
		final String name = attr.getValue(ViewReference.NAME_TAG);
		final String path = attr.getValue(ViewReference.PATH_TAG);
		final String versionStr = attr
		.getValue(IViewConfiguration.VIEW_VERSION_TAG);
		if (versionStr == null) {
			readVersion = 0;
		} else {
			readVersion = Integer.parseInt(versionStr);
		}
		final ViewReference ref = new ViewReference();
		ref.setPath(path);
		ref.setName(name);
		ref.setType(new ViewType(type));
		final IViewConfiguration vc = ref.parse(appContext);
		
		if (vc == null) {
			throw new SAXException("There are missing view files referenced by this perspective.");
		}

		if (inContainerView) {
			childView = vc;
			inChildView = true;
		} else {
			viewConfig = vc;
			inStandaloneView = true;
		}
		inViews = true;
		size = new ChillSize();
		position = new ChillLocation();
		backgroundColor = new ChillColor(ChillColor.ColorName.WHITE);
		foregroundColor = new ChillColor(ChillColor.ColorName.BLACK);
		font = new ChillFont();
	}

	private void parsePlainView(final Attributes attr) throws SAXException {
	    this.inView = true;
		final String name = attr.getValue(IViewConfiguration.VIEW_NAME_TAG);
		if (name == null) {
			throw new SAXException("View configuration has no name");
		}
		final String type = attr.getValue(IViewConfiguration.VIEW_TYPE_TAG);
		if (type == null) {
			throw new SAXException("View configuration has no type");
		}
		final String versionStr = attr
		.getValue(IViewConfiguration.VIEW_VERSION_TAG);
		if (versionStr == null) {
			readVersion = 0;
		} else {
			readVersion = Integer.parseInt(versionStr);
		}
		
		if (type.equalsIgnoreCase("Alarm")) {
			throw new SAXException("Perpective contains out of date Alarm views and must be updated before it can be used");
		}
		final ViewType vt = new ViewType(type);
		final ViewProperties vp = appContext.getBean(PerspectiveProperties.class).getViewProperties(vt);
		if (vp == null) {
		    throw new SAXException("Unrecognized view type found in configuration: " + vt);
		}
		final Class<?> configClass = vp.getViewConfigurationClass();

		if (inContainerView) {
			try {
				childView = (IViewConfiguration) ReflectionToolkit.createObject(configClass, new Class[] {ApplicationContext.class}, new Object[] {appContext});
				this.customViewParser = childView.getParser(appContext);
				childView.setViewName(name);
				childView.setViewType(vt);
				childView.setVersion(readVersion);
			} catch (final Exception e) {
				throw new SAXException(e.toString());
			}
			inChildView = true;
		} else {
			try {
				viewConfig = (IViewConfiguration) ReflectionToolkit.createObject(configClass, new Class[] {ApplicationContext.class}, new Object[] {appContext});
				this.customViewParser = viewConfig.getParser(appContext);
				viewConfig.setViewName(name);
				viewConfig.setViewType(vt);
				viewConfig.setVersion(readVersion);
			} catch (final Exception e) {
				throw new SAXException(e.toString());
			}
			inStandaloneView = true;
		}
		inViews = true;
		size = new ChillSize();
		position = new ChillLocation();
		backgroundColor = new ChillColor(ChillColor.ColorName.WHITE);
		foregroundColor = new ChillColor(ChillColor.ColorName.BLACK);
		font = new ChillFont();
		counters.incrementViewCount(vt);
		if (this.customViewParser != null) {
		    this.customViewParser.init();
		}
	}

	private void parseViewContainer(final Attributes attr) throws SAXException {
		if (inContainerView) {
			containerStack.push(containerConfig);
		}
		final String name = StringEscapeUtils.unescapeXml(attr
				.getValue(IViewConfiguration.VIEW_NAME_TAG));
		if (name == null) {
			throw new SAXException("View configuration has no name");
		}
		final String type = attr.getValue(IViewConfiguration.VIEW_TYPE_TAG);
		if (type == null) {
			throw new SAXException("View configuration has no type");
		}
		final String versionStr = attr
		.getValue(IViewConfiguration.VIEW_VERSION_TAG);

		if (versionStr == null) {
			readVersion = 0;
		} else {
			readVersion = Integer.parseInt(versionStr);
		}
		final ViewType vt = new ViewType(type);
		final ViewProperties vp = appContext.getBean(PerspectiveProperties.class).getViewProperties(vt);
		if (vp == null) {
		    throw new SAXException("Unrecognized view type found in configuration: " + vt);
		}
		final Class<?> configClass = vp.getViewConfigurationClass();

		try {
			viewConfig = (IViewConfiguration) ReflectionToolkit.createObject(configClass, new Class[] {ApplicationContext.class}, new Object[] {appContext});;
			viewConfig.setViewName(name);
			viewConfig.setViewType(vt);
			viewConfig.setVersion(readVersion);
			containerConfig = (IViewConfigurationContainer) viewConfig;
			viewConfig.setLocation(new ChillLocation());
			viewConfig.setSize(new ChillSize());
			viewConfig.setBackgroundColor(new ChillColor(
					ChillColor.ColorName.WHITE));
			viewConfig.setForegroundColor(new ChillColor(
					ChillColor.ColorName.BLACK));
			viewConfig.setDataFont(new ChillFont());
		} catch (final Exception e) {
			throw new SAXException(e.toString());
		}
		inContainerView = true;
		inViews = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(final String uri, final String localName, final String qname)
	throws SAXException {
		IViewConfiguration containerVC = (IViewConfiguration)containerConfig;
		
		if (qname.equalsIgnoreCase("Views")) {
			inViews = false;
		}

		else if (qname.equalsIgnoreCase(ViewReference.VIEW_REFERENCE_TAG)) {
			endParseViewReference();

		} else if (qname.equalsIgnoreCase(IViewConfiguration.VIEW_CLASS_TAG)) {
			parseViewClass();
		}
		else if (qname.equals(IViewConfiguration.VIEW_FONT_TAG) && inViews) {
			parseDefaultFont();
		}
		// NEW FORMAT BACKGROUND COLOR
		else if (qname.equals(IViewConfiguration.VIEW_BACKGROUND_TAG) && inViews) {
			parseBackgroundColor();
		} else if (qname.equals(IViewConfiguration.OLD_VIEW_BACKGROUND_TAG)
				&& inViews) {
			inBackgroundColor = false;
		}
		// NEW FORMAT FOREGROUND COLOR:
		else if (qname.equals(IViewConfiguration.VIEW_FOREGROUND_TAG) && inViews) {

			final String colors = getBufferText();

			if (inChildView || inStandaloneView) {
				foregroundColor.setColors(colors);

			} else {
				containerVC.getForegroundColor()
				.setColors(colors);
			}
		} else if (qname.equals(IViewConfiguration.OLD_VIEW_FOREGROUND_TAG)
				&& inViews) {
			inForegroundColor = false;
		}

		else if (qname.equals(ChillLocation.X_LOCATION_TAG)) {
			try {
				final String xStr = getBufferText();
				final int xPos = Integer.parseInt(xStr);
				if (inChildView || inStandaloneView) {
					position.setXPosition(xPos);
				} else if (inContainerView) {
					containerVC.getLocation()
					.setXPosition(xPos);
				}
			} catch (final NumberFormatException e) {
				throw new SAXException("x position must be an integer");
			}
		} else if (qname.equals(ChillLocation.Y_LOCATION_TAG)) {
			try {
				final String yStr = getBufferText();
				final int yPos = Integer.parseInt(yStr);
				if (inChildView || inStandaloneView) {
					position.setYPosition(yPos);
				} else if (inContainerView) {
					containerVC.getLocation()
					.setYPosition(yPos);
				}
			} catch (final NumberFormatException e) {
				throw new SAXException("y position must be an integer");
			}
		} else if (qname.equals(ChillSize.X_WIDTH_TAG)) {
			try {
				final String xWidth = getBufferText();
				final int xW = Integer.parseInt(xWidth);
				if (inChildView || inStandaloneView) {
					size.setXWidth(xW);
				} else if (inContainerView) {
					containerVC.getSize().setXWidth(
							xW);
				}
			} catch (final NumberFormatException e) {
				throw new SAXException("x position must be an integer");
			}
		} else if (qname.equals(ChillSize.Y_HEIGHT_TAG)) {
			try {
				final String xHeight = getBufferText();
				final int yH = Integer.parseInt(xHeight);
				if (inChildView || inStandaloneView) {
					size.setYHeight(yH);
				} else if (inContainerView) {
					containerVC.getSize().setYHeight(
							yH);
				}
			} catch (final NumberFormatException e) {
				throw new SAXException("y position must be an integer");
			}
		}
		// OLD FORMAT COLORS: These had red, green, and blue elements as
		// separate tags
		else if (qname.equals(ChillColor.RED_COLOR_TAG) && inViews) {
			if (readVersion < 2) {
				try {
					final String color = getBufferText();
					final int red = Integer.parseInt(color);
					if (inChildView) {
						if (inBackgroundColor || childView.getVersion() == 0) {
							backgroundColor.setRed(red);
						} else if (inForegroundColor) {
							foregroundColor.setRed(red);
						}
					} else if (inStandaloneView) {
						if (inBackgroundColor || viewConfig.getVersion() == 0) {
							backgroundColor.setRed(red);
						} else if (inForegroundColor) {
							foregroundColor.setRed(red);
						}
					} else {
						if (inBackgroundColor
								|| containerVC
								.getVersion() == 0) {
							containerVC
							.getBackgroundColor().setRed(red);
						} else if (inForegroundColor) {
							containerVC
							.getForegroundColor().setRed(red);
						}
					}
				} catch (final NumberFormatException e) {
					throw new SAXException("red color level must be an integer");
				}
			}
		} else if (qname.equals(ChillColor.GREEN_COLOR_TAG) && inViews) {
			if (readVersion < 2) {
				try {
					final String color = getBufferText();
					final int green = Integer.parseInt(color);
					if (inChildView) {
						if (inBackgroundColor || childView.getVersion() == 0) {
							backgroundColor.setGreen(green);
						} else if (inForegroundColor) {
							foregroundColor.setGreen(green);
						}
					} else if (inStandaloneView) {
						if (inBackgroundColor || viewConfig.getVersion() == 0) {
							backgroundColor.setGreen(green);
						} else if (inForegroundColor) {
							foregroundColor.setGreen(green);
						}
					} else {
						if (inBackgroundColor
								|| containerVC
								.getVersion() == 0) {
							containerVC
							.getBackgroundColor().setGreen(green);
						} else if (inForegroundColor) {
							containerVC
							.getForegroundColor().setGreen(green);
						}
					}
				} catch (final NumberFormatException e) {
					throw new SAXException(
					"green color level must be an integer");
				}
			}
		} else if (qname.equals(ChillColor.BLUE_COLOR_TAG) && inViews) {
			if (readVersion < 2) {
				try {
					final String color = getBufferText();
					final int blue = Integer.parseInt(color);
					if (inChildView) {
						if (inBackgroundColor || childView.getVersion() == 0) {
							backgroundColor.setBlue(blue);
						} else if (inForegroundColor) {
							foregroundColor.setBlue(blue);
						}
					} else if (inStandaloneView) {
						if (inBackgroundColor || viewConfig.getVersion() == 0) {
							backgroundColor.setBlue(blue);
						} else if (inForegroundColor) {
							foregroundColor.setBlue(blue);
						}
					} else {
						if (inBackgroundColor
								|| containerVC
								.getVersion() == 0) {
							containerVC
							.getBackgroundColor().setBlue(blue);
						} else if (inForegroundColor) {
							containerVC
							.getForegroundColor().setBlue(blue);
						}
					}
				} catch (final NumberFormatException e) {
					throw new SAXException(
					"blue color level must be an integer");
				}
			}
		}

		// OLD FORMAT FONTS: these had separate face, size, and style tags
		else if (qname.equals(ChillFont.FONT_FACE_TAG) && inViews) {
			if (readVersion < 2) {
				final String fontFace = getBufferText();
				if (inChildView || inStandaloneView) {
					font.setFace(fontFace);
				} else {
					containerVC.getDataFont()
					.setFace(fontFace);
				}
			}
		} else if (qname.equals(ChillFont.FONT_SIZE_TAG) && inViews) {
			if (readVersion < 2) {
				try {
					final String sizeStr = getBufferText();
					final ChillFont.FontSize fontSize = Enum.valueOf(
							ChillFont.FontSize.class, sizeStr);
					if (inChildView || inStandaloneView) {
						font.setSize(fontSize);
					} else {
						containerVC.getDataFont()
						.setSize(fontSize);
					}
				} catch (final NumberFormatException e) {
					throw new SAXException("font size must be an integer");
				}
			}
		} else if (qname.equals(ChillFont.FONT_STYLE_TAG) && inViews) {
			if (readVersion < 2) {
				try {
					final String styleStr = getBufferText();
					final int fontStyle = Integer.parseInt(styleStr);
					if (inChildView || inStandaloneView) {
						font.setStyle(fontStyle);
					} else {
						containerVC.getDataFont()
						.setStyle(fontStyle);
					}
				} catch (final NumberFormatException e) {
					throw new SAXException("font style must be an integer");
				}
			}
		}

		// ENDING A PLAIN VIEW:
		else if (qname.equalsIgnoreCase(IViewConfiguration.VIEW_TAG)) {

			if (inChildView) {
				childView.setLocation(position);
				childView.setSize(size);
				childView.setBackgroundColor(backgroundColor);
				childView.setForegroundColor(foregroundColor);
				childView.setDataFont(font);

				containerConfig.addViewConfiguration(childView);
				inChildView = false;

			} else if (inStandaloneView) {
				viewConfig.setLocation(position);
				viewConfig.setSize(size);
				viewConfig.setBackgroundColor(backgroundColor);
				viewConfig.setForegroundColor(foregroundColor);
				viewConfig.setDataFont(font);
				parsedViews.add(viewConfig);
				inStandaloneView = false;
			}
			this.inView = false;
	        this.customViewParser = null;
		}

		// ENDING VIEW CONTAINER: single window view, tabbed window view, or
		// grid view
		else if (qname
				.equalsIgnoreCase(IViewConfigurationContainer.VIEW_CONTAINER_TAG)) {

			if (containerStack.isEmpty()) {
				parsedViews.add(containerVC);
				inContainerView = false;
			} else {
				final IViewConfigurationContainer prev = containerStack.pop();
				prev.addViewConfiguration(containerVC);
				containerConfig = prev;
				containerVC = (IViewConfiguration)containerConfig;
			}
			/*  Accomodate both old and show column header tags. Add parsing of show row header flag. */
		} else if ((qname.equals(ChillTable.OLD_SHOW_HEADER_TAG) || qname.equals(ChillTable.SHOW_COL_HEADER_TAG)) && inTable) {
		    try {
		        final boolean val = Boolean.parseBoolean(getBufferText());
		        table.setShowColumnHeader(val);
		    } catch (final NumberFormatException e) {
		        throw new SAXException(
		                "Bad boolean value in table settings for "
		                        + table.getName());
		    }
		} else if (qname.equals(ChillTable.SHOW_ROW_HEADER_TAG) && inTable) {
		    try {
		        final boolean val = Boolean.parseBoolean(getBufferText());
		        table.setShowRowHeader(val);
		    } catch (final NumberFormatException e) {
		        throw new SAXException(
		                "Bad boolean value in table settings for "
		                        + table.getName());
		    }
		} else if (qname.equals(ChillTable.SORT_LOCK_TAG) && inTable) {
		    try {
		        final boolean val = Boolean.parseBoolean(getBufferText());
		        table.setSortAllowed(val);
		    } catch (final NumberFormatException e) {
		        throw new SAXException(
		                "Bad boolean value in table settings for "
		                        + table.getName());
			}
		} else if (qname.equals(ChillTable.SORT_DIRECTION_TAG) && inTable) {
			try {
				final boolean val = Boolean.parseBoolean(getBufferText());
				table.setSortAscending(val);
			} catch (final NumberFormatException e) {
				throw new SAXException(
						"Bad boolean value in table settings for "
						+ table.getName());
			}
		} else if (qname.equals(ChillTable.SORT_COLUMN_TAG) && inTable
				&& table.getVersion() == 0) {
			sortColumn = getBufferText();
		} else if (qname.equals(ChillTable.TABLE_COLUMNS_TAG) && inTable
				&& table.getVersion() == 0) {
			tableColNames = getBufferText().split(",");
		} else if (qname.equals(ChillTable.COLUMN_WIDTHS_TAG) && inTable
				&& table.getVersion() == 0) {
			final String[] cols = getBufferText().split(",");
			tableColWidths = new Integer[cols.length];
			for (int i = 0; i < cols.length; i++) {
				tableColWidths[i] = Integer.valueOf(cols[i]);
			}
		} else if (qname.equals(ChillTable.ENABLED_COLUMNS_TAG) && inTable
				&& table.getVersion() == 0) {
			final String[] cols = getBufferText().split(",");
			enabledCols = new Boolean[cols.length];
			for (int i = 0; i < cols.length; i++) {
				enabledCols[i] = Boolean.valueOf(cols[i]);
			}
		} else if (qname.equals(ChillTable.TABLE_SETTINGS_TAG) && inTable) {
			if (table.getVersion() == 0) {
				int enabledIndex = 0;
				for (int i = 0; i < tableColNames.length; i++) {
					final ChillTableColumn column = new ChillTableColumn();
					column.setEnabled(enabledCols[i]);
					column.setConfigurationNumber(i);
					if (enabledCols[i]) {
						column.setCurrentPosition(enabledIndex++);
					} else {
						column
						.setCurrentPosition(ChillTableColumn.DISABLED_INDEX);
					}
					column.setCurrentWidth(tableColWidths[i]);
					column.setDisplayName(tableColNames[i]);
					column.setOfficialName(tableColNames[i]);
					tableColumns.add(column);
					table.setSortColumn(sortColumn);
				}
			}
			table.setAvailableColumns((ArrayList<ChillTableColumn>)tableColumns);
			
			if (inChildView) {
				childView.setTable(table);
				childView.setDeprecations();
			} else {
				viewConfig.setTable(table);
				viewConfig.setDeprecations();
			}
			table.setVersion(ChillTable.CURRENT_VERSION);
			inTable = false;

		} else if (this.inView && this.customViewParser != null &&
                this.customViewParser.endElement(uri, localName, qname, this.buffer)) {

        } else if (inChildView
				&& !qname
				.equalsIgnoreCase(IViewConfigurationContainer.CHILD_VIEWS_TAG)) {
			final String val = StringEscapeUtils.unescapeXml(getBufferText());
			//ChannelCondition is being set as a configItem here
			childView.setConfigItem(qname, val);

		} else if (inContainerView
				&& !qname
				.equalsIgnoreCase(IViewConfigurationContainer.CHILD_VIEWS_TAG)) {
			final String val = StringEscapeUtils.unescapeXml(getBufferText());
			containerVC.setConfigItem(qname, val);
		}
 
		else if (inStandaloneView) {
			final String val = StringEscapeUtils.unescapeXml(getBufferText());
			viewConfig.setConfigItem(qname, val);
		}
	}

	private void parseBackgroundColor() {
		final String colors = getBufferText();

		if (inChildView || inStandaloneView) {
			backgroundColor.setColors(colors);
		} else {
			((IViewConfiguration) containerConfig).getBackgroundColor()
			.setColors(colors);
		}
	}

	private void parseDefaultFont() {
		final String font_string = getBufferText();
		if (inChildView || inStandaloneView) {
			font.setFont(font_string);
		} else {
			((IViewConfiguration) containerConfig).getDataFont().setFont(
					font_string);
		}
	}

	private void parseViewClass() {
		String name = getBufferText();
		name = ViewClassMap.mapClassName(name);
		if (inChildView) {
			childView.setViewClass(name);
		} else if (inStandaloneView) {
			viewConfig.setViewClass(name);
		} else {
			((IViewConfiguration) containerConfig).setViewClass(name);
		}
	}

	private void endParseViewReference() {
		if (inChildView) {
			containerConfig.addViewConfiguration(childView);
			inChildView = false;
		} else if (inStandaloneView) {
			parsedViews.add(viewConfig);
			inStandaloneView = false;
		}
	}

	/**
	 * Starts a new text buffer, which captures text parsed from XML element
	 * values.
	 */
	protected void newBuffer() {
		buffer = new StringBuilder();
	}

	/**
	 * Returns the current content on the text buffer as a String.
	 * 
	 * @return the text String
	 */
	protected String getBufferText() {
		if (buffer == null) {
			return null;
		}
		return buffer.toString().trim();
	}

	/**
     * {@inheritDoc}
	 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
	 */
	@Override
	public void error(final SAXParseException e) throws SAXException {
		throw new SAXException("Parse error in view configuration file line "
				+ e.getLineNumber() + ", column " + e.getColumnNumber() + ": "
				+ e.getMessage());
	}

	/**
     * {@inheritDoc}
	 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	@Override
	public void fatalError(final SAXParseException e) throws SAXException {
		throw new SAXException(
				"Fatal parse error in view configuration file line "
				+ e.getLineNumber() + ", column " + e.getColumnNumber()
				+ ": " + e.getMessage());
	}

	/**
     * {@inheritDoc}
	 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
	 */
	@Override
	public void warning(final SAXParseException e) {
		tracer.warn("Parse warning in view configuration file line "
				+ e.getLineNumber() + ", column " + e.getColumnNumber() + ": "
				+ e.getMessage());
	}

}
