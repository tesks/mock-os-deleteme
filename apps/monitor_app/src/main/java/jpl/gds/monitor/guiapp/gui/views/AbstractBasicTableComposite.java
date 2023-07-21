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
package jpl.gds.monitor.guiapp.gui.views;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.message.ISuspectChannelsMessage;
import jpl.gds.monitor.perspective.view.RealtimeRecordedSupport;
import jpl.gds.monitor.perspective.view.StationSupport;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.ChillTableColumn;
import jpl.gds.perspective.ChillTableColumn.SortType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.ViewConfigurationListener;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TableItemQuickSort;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.time.TimeUtility;

/**
 * This is the base class for channel tables. It is used by table composites for channel list and alarm views.
 */
public abstract class AbstractBasicTableComposite extends Composite implements ViewConfigurationListener {

	/**
	 * Hash key for channel display item characteristics.
	 */
	protected static final String CHARACTERISTICS = "characteristics";

	/**
	 * Hash key for channel display item channel definition.
	 */
	protected static final String DEFINITION = "definition";

	private static final String UP_IMAGE = "jpl/gds/monitor/gui/up.gif";
	private static final String DOWN_IMAGE = "jpl/gds/monitor/gui/down.gif";

	/**
	 * Red color
	 */
	protected static Color red;

	/**
	 * Yellow color
	 */
	protected static Color yellow;

	/**
	 * White color
	 */
	protected static Color white;

	/**
	 * Black color
	 */
	protected static Color black;

	/**
	 * Default tracer for logging info
	 */
    protected final Tracer           trace;

	/**
	 * Up arrow for ascending sorting symbol
	 */
	protected static Image upImage;

	/**
	 * Down arrow for descending sorting symbol
	 */
	protected static Image downImage;

	/**
	 * Font without formatting
	 */
	protected Font plainFont;

	/**
	 * Font with italics style (used to indicate suspect channels)
	 */
	protected Font italicsFont;

	/**
	 * Data font
	 */
	protected Font dataFont;

	/**
	 * Time formatter
	 */
	protected final DateFormat timeFormat = TimeUtility.getFormatter();

	/**
	 * Table
	 */
	protected Table table;

	/**
	 * Array of table columns
	 */
	protected TableColumn[] tableColumns;

	/**
	 * Table properties
	 */
	protected ChillTable tableDef;

	private MenuItem copyMenuItem;
	private MenuItem showDefMenuItem;

	/**
	 * Foreground color
	 */
	protected Color foreground;

	/**
	 * Background color
	 */
	protected Color background;

	/**
	 * Parent composite
	 */
	protected Composite parent;

	/**
	 * Column that is currently selected as the one to sort on
	 */
	protected ChillTableColumn sortColumn;

	private final List<String> fswSuspectChannels = new ArrayList<String>();
	private final List<String> sseSuspectChannels = new ArrayList<String>();

	/**
	 * View configuration
	 */
	protected IViewConfiguration viewConfig;

	private final ApplicationContext appContext;

	/** Spacecraft aware print formatter object */
    protected SprintfFormat formatUtil;

	private synchronized static void initColorsAndImages() {
		if (red == null) {
			red = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.DARK_RED));
			yellow = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.YELLOW));
			white = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.WHITE));
			black = ChillColorCreator.getColor(new ChillColor(
					ChillColor.ColorName.BLACK));
		}
		if (upImage == null) {
			upImage = SWTUtilities.createImage(Display.getCurrent(), UP_IMAGE);
			downImage = SWTUtilities.createImage(Display.getCurrent(), DOWN_IMAGE);
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 * @param parent parent Composite for this table
	 * @param config ViewConfiguration object for the parent view
	 */
	public AbstractBasicTableComposite(final ApplicationContext appContext, final Composite parent, final IViewConfiguration config) {

		super (parent, SWT.NO_FOCUS);
		this.appContext = appContext;
        trace = TraceManager.getTracer(appContext, Loggers.UTIL);
		formatUtil = appContext.getBean(SprintfFormat.class);
		
		initColorsAndImages();

		this.parent = parent;
		viewConfig = config;
		viewConfig.addConfigurationListener(this);

		plainFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.NONE);
		italicsFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.ITALIC);
	}

	/**
	 * Creates GUI widgets and controls.
	 */
	protected void createControls() {
		final FormLayout fl = new FormLayout();
		this.setLayout(fl);

		createEmptyTable();

		final FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.bottom = new FormAttachment(100);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		table.setLayoutData(data);

		final Menu viewMenu = new Menu(parent);
		table.setMenu(viewMenu);
		copyMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
		copyMenuItem.setText("Copy");
		copyMenuItem.setEnabled(false);
		new MenuItem(viewMenu, SWT.SEPARATOR);

		showDefMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		showDefMenuItem.setText("Show Channel Definition...");
		showDefMenuItem.setEnabled(false);

		copyMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] indices = table.getSelectionIndices();
					if (indices == null || indices.length == 0) {
						return;
					}
					Arrays.sort(indices);
					Clipboard clipboard = new Clipboard(getDisplay());
					final StringBuffer plainText = new StringBuffer();
					for (int i = 0; i < indices.length; i++) {
						final TableItem item = table.getItem(indices[i]);
						for (int j = 0; j < table.getColumnCount(); j++) {
							plainText.append("\"" + item.getText(j) + "\"");
							if (j < table.getColumnCount() - 1) {
								plainText.append(',');
							}
						}
						plainText.append('\n');
					}
					final TextTransfer textTransfer = TextTransfer.getInstance();
					clipboard.setContents(new String[]{plainText.toString()}, new Transfer[]{textTransfer});
					clipboard.dispose();
					clipboard = null;
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in copy menu item handling " + ex.toString());
				}
			}
		});

		showDefMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] indices = table.getSelectionIndices();
					if (indices == null || indices.length != 1) {
						return;
					}
					if (!itemHasDefinition(table.getItem(indices[0]))) {
						return;
					}
					final IChannelDefinition def = (IChannelDefinition)table.getItem(indices[0]).getData(DEFINITION);
					/*
					 * Realtime recorded filter in the perspective and
					 * LAD is now enum rather than boolean, and station ID is required for LAD
					 * access.
					 */
					final MonitorChannelSample sample = appContext.getBean(MonitorChannelLad.class).getMostRecentValue(def.getId(), 
							((RealtimeRecordedSupport)viewConfig).getRealtimeRecordedFilterType(), 
							((StationSupport)viewConfig).getStationId());

					final String text = MonitorChannelSample.getChanDefText(appContext, def.getId(), sample);

                    final TextViewShell tvs = new TextViewShell(table.getShell(), trace);
					tvs.getShell().setSize(400,400);
					tvs.setText(text);
					tvs.open();
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in show definition menu item handling " + ex.toString());
				}
			}
		});

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					enableMenuItems();
				} catch (final Exception ex) {
					ex.printStackTrace();
					trace.error("Error in table selection handler " + ex.toString());
				}
			}
		});

		/*
		 * Removes add of paint listener to the table composite,
		 * which drew a black border around recorded displays. That feature is no longer 
		 * appropriate.
		 */

		// Add dispose listener to get rid of font objects allocated by this object
		this.addDisposeListener(new DisposeListener() {

			/**
			 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			 */
			@Override
			public void widgetDisposed(final DisposeEvent arg0) {
				if(plainFont != null)
				{
					plainFont.dispose();
					plainFont = null;
				}
				if(italicsFont != null)
				{
					italicsFont.dispose();
					italicsFont = null;
				}
				if (dataFont != null) {
					dataFont.dispose();
					dataFont = null;
				}
			}			
		});

		setSortColumn();
	}

	/**
	 * Updates the columns in the table to match the latest table definition.
	 * 
	 * @return Table with updated columns
	 */
	protected Table updateTableColumns() {
		final int numColumns = tableDef.getColumnCount();
		int actualIndex = 0;
		for (int i = 0; i < numColumns; i++) {
			if (!tableDef.isColumnEnabled(i) && tableColumns[i] != null) {
				if (tableColumns[i] == table.getSortColumn()) {
					table.setSortColumn(null);
				}
				tableColumns[i].dispose();
				tableColumns[i] = null;

			}
		}

		final Listener sortListener = new SortListener();
		for (int i = 0; i < numColumns; i++) {
			if (tableDef.isColumnEnabled(i)) {
				if (tableColumns[i] == null) {
					tableColumns[i] = new TableColumn(table,
							SWT.NONE, actualIndex);
					tableColumns[i].setMoveable(true);
					tableColumns[i].setText(tableDef
							.getOfficialColumnName(i));
					tableColumns[i].setWidth(tableDef
							.getColumnWidth(i));
					tableColumns[i].addListener(SWT.Selection,
							sortListener);
				}
				actualIndex++;
			}
		}
		return table;
	}

	/**
	 * Sets the font of the given table item to the suspect channel 
	 * font if the channel is on the current suspect list.
	 * @param item TableItem to set font for
	 * @param id the channel ID for the table item
	 */
	public void setSuspectFont(final TableItem item, final String id)
	{
		synchronized (fswSuspectChannels) {
			final boolean suspect = setSuspectFont(fswSuspectChannels, item, id);
			if (!suspect) {
				setSuspectFont(sseSuspectChannels, item, id);
			}
		}
	}

	/**
	 * Processes a SuspectChannelMessage by saving the list of suspect channels,
	 * @param msg SuspectChannelMessage
	 */
	protected void handleSuspectChannelsMessage(final ISuspectChannelsMessage msg)
	{
		synchronized(fswSuspectChannels) {
			// get latest channel data for given ids
			final List<String> suspectIds = msg.getSuspectTable().getAllSuspectChannelIds();
			List<String> useSuspectList = fswSuspectChannels;

			if (msg.isFromSse()) {
				useSuspectList = sseSuspectChannels;
			}
			useSuspectList.clear();

			//latestSuspectData is null if no channels are marked suspect
			if(suspectIds != null)
			{
				// Add suspect channels to list of suspectAlarms
				for (final String id : suspectIds)
				{
					useSuspectList.add(id);
				}
			}
		}
	}

	/**
	 * Sets the font of a table row to italics if it is considered suspect.  
	 * 
	 * @param useList list of suspect channels
	 * @param item table row
	 * @param cid the channel ID for the table item
	 * @return true if channel is suspect, false otherwise
	 */
	public boolean setSuspectFont(final List<String> useList, final TableItem item, final String cid)
	{
		// Set font to be italics if alarm is suspect,
		boolean isSuspect = false;

		for(int i = 0; i< useList.size(); i++)
		{
			if(useList.get(i).equals(cid))
			{
				isSuspect = true;
				break;
			}	
		}
		if (isSuspect)
		{
			item.setFont(italicsFont);
		}
		else
		{	
			item.setFont(plainFont);
		}
		return isSuspect;
	}


	/**
	 * Indicates whether the given table item has an associated channel definition.
	 * 
	 * @param item TableItem to check for a definition
	 * @return true if the item has a channel definition
	 */
	public boolean itemHasDefinition(final TableItem item) {
		final IChannelDefinition def = (IChannelDefinition)item.getData(DEFINITION);
		return def != null;
	}

	/**
	 * Removes the current sort indicator in the table header.
	 */
	protected void cancelOldSortColumn() {
		final int oldSortColumn = getSortColumnIndex();
		if (oldSortColumn != -1) {
			tableColumns[oldSortColumn].setImage(null);
		}
	}

	/**
	 * Gets the absolute index (in the table definition) of the current sort column.
	 * 
	 * @return index of sort column, or -1 if non defined
	 */
	protected int getSortColumnIndex() {

		int oldSortColumn = -1;
		for (int i = 0; i < tableDef.getColumnCount(); i++) {
			if (tableDef.isColumnEnabled(i) && tableDef.isSortColumn(i)) {
				oldSortColumn = i;
				break;

			}
		}
		return oldSortColumn;		
	}

	/**
	 * Enables/disables menu items based upon current table selection.
	 */
	protected void enableMenuItems() {
		try {
			final int[] i = table.getSelectionIndices();
			if (i != null && i.length != 0) {
				copyMenuItem.setEnabled(true);
			} else {
				copyMenuItem.setEnabled(false);
			}
			if (i != null && i.length == 1) {
				showDefMenuItem.setEnabled(true);
			} else {
				showDefMenuItem.setEnabled(false);
			}

		} catch (final Exception ex) {
			ex.printStackTrace();
			trace.error("Error in table selection handler " + ex.toString());
		}
	}

	/**
	 * Gets the current SWT table object.
	 * 
	 * @return Table object
	 */
	public Table getTable() {
		return table;
	}

	/**
	 * Updates font and color objects to match the current view configuration,
	 */
	protected void updateDataFontAndColors() {
		if (dataFont != null && !dataFont.isDisposed()) {
			dataFont.dispose();
			dataFont = null;
		}
		dataFont = ChillFontCreator.getFont(viewConfig.getDataFont());
		table.setFont(dataFont);

		if (foreground != null && !foreground.isDisposed()) {
			foreground.dispose();
			foreground = null;
		}
		foreground = ChillColorCreator.getColor(viewConfig.getForegroundColor());
		table.setForeground(foreground);

		if (background != null && !background.isDisposed()) {
			background.dispose();
			background = null;
		}
		background = ChillColorCreator.getColor(viewConfig.getBackgroundColor());
		table.setBackground(background);
	}

	/**
	 * Creates the table.
	 */
	private void createEmptyTable() {
		table = new Table(this, SWT.READ_ONLY | SWT.FULL_SELECTION | SWT.MULTI);

		updateDataFontAndColors();

		table.setHeaderVisible(tableDef.isShowColumnHeader());
		table.setLinesVisible(true);
		if (tableDef.isSortAllowed() && tableDef.getSortColumn() != null) {
			table.setSortDirection(tableDef.isSortAscending() ? SWT.UP : SWT.DOWN);
		} else {
			table.setSortDirection(SWT.NONE);
		}

		final Listener sortListener = new SortListener();

		final int numColumns = tableDef.getColumnCount();
		tableColumns = new TableColumn[numColumns];
		for (int i = 0; i < numColumns; i++) {
			if (tableDef.isColumnEnabled(i) && !tableDef.isColumnDeprecated(i)) {
				final int align = SWT.NONE;
				tableColumns[i] = new TableColumn(table, align);
				tableColumns[i].setMoveable(true);
				tableColumns[i].setText(tableDef.getOfficialColumnName(i));
				tableColumns[i].setWidth(tableDef.getColumnWidth(i));
				tableColumns[i].addListener(SWT.Selection, sortListener);
				if (tableDef.isSortColumn(i) && tableDef.isSortAllowed()) {
					table.setSortColumn(tableColumns[i]);
					tableColumns[i].setImage(tableDef.isSortAscending() ? upImage : downImage);
				}
			} else {
				tableColumns[i] = null;
			}
		}
		
		table.setColumnOrder(tableDef.getColumnOrder());

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly.
		 * Therefore, it is critical for performance that these methods be as
		 * efficient as possible.
		 */
		table.addListener(SWT.EraseItem, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				if ((event.detail & SWT.SELECTED) != 0) {
					final GC gc = event.gc;
					final Rectangle area = table.getClientArea();
					/*
					 * If you wish to paint the selection beyond the end of last column,
					 * you must change the clipping region.
					 */
					final int columnCount = table.getColumnCount();
					if (event.index == columnCount - 1 || columnCount == 0) {
						final int width = area.x + area.width - event.x;
						if (width > 0) {
							Region region = new Region();
							gc.getClipping(region);
							region.add(event.x, event.y, width, event.height);
							gc.setClipping(region);
							region.dispose();
							region = null;
						}
					}

					final Rectangle rect = event.getBounds();
					final TableItem item = (TableItem)event.item;
					final AlarmLevel level = (AlarmLevel)item.getData("alarmLevel");
					if (level != null && level.equals(AlarmLevel.RED)) {
						gc.setBackground(red);
					} else if (level != null && level.equals(AlarmLevel.YELLOW)) {
						gc.setBackground(yellow);            	
						gc.setForeground(black);           	
					} else {
						gc.setBackground(table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
					}
					gc.fillRectangle(0, rect.y, 3000, rect.height);
					event.detail &= ~SWT.SELECTED;
				}
			}
		});
	}

	/**
	 * Gets the channel id of the channel currently selected in the table. Throws if
	 * no table item is selected.
	 * 
	 * @return channel ID string.
	 */
	public String getSelectionId() {
		final int selected = table.getSelectionIndex();
		if (selected == -1) {
			throw new IllegalStateException("Table row must be selected");
		}
		final TableItem item = table.getItem(selected);
		final IChannelDefinition def = (IChannelDefinition)item.getData(DEFINITION);
		return def.getId();
	}

	/**
	 * Gets the display characteristics of the channel currently selected in the table.
	 * Throws if no table item is selected.
	 * 
	 * @return ChannelDisplay item object, or null if none defined
	 */
	public ChannelDisplayFormat getSelectionCharacteristics() {
		final int selected = table.getSelectionIndex();
		if (selected == -1) {
			throw new IllegalStateException("Table row must be selected");
		}
		final TableItem item = table.getItem(selected);
		return (ChannelDisplayFormat)item.getData(CHARACTERISTICS);
	}

	/**
	 * Gets the screen location of the currently selected table item. Throws if
	 * no table item is currently selected.
	 * 
	 * @return Point of current selection
	 */
	public Point getSelectionLocation() {
		final int selected = table.getSelectionIndex();
		if (selected == -1) {
			throw new IllegalStateException("Table row must be selected");
		}
		final TableItem item = table.getItem(selected);
		final Rectangle r = item.getBounds();

		return this.toDisplay(r.x, r.y);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.ViewConfigurationListener#configurationChanged(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
	public void configurationChanged(final IViewConfiguration config)
	{
		plainFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.NONE);
		italicsFont = new Font(null, viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.ITALIC);
	}

	/**
	 * Colors the given table item according to its alarm state.
	 * 
	 * @param item TableItem to color
	 * @param level current alarm level for this item
	 * @param inDataIndex index of column to color
	 */
	protected void setAlarmColors(final TableItem item, final AlarmLevel level, final int inDataIndex) {
		int dataIndex = inDataIndex;
		if (!tableDef.isColumnEnabled(dataIndex)) {
			return;
		}
		dataIndex = tableDef.getActualIndex(dataIndex);
		if (level.equals(AlarmLevel.RED)) {
			item.setBackground(dataIndex, red);
			item.setForeground(dataIndex, white);
		} else if (level.equals(AlarmLevel.YELLOW)) {
			item.setBackground(dataIndex, yellow);
			item.setForeground(dataIndex, black);
		} else {
			item.setBackground(dataIndex, null);
			item.setForeground(dataIndex, null);
		}
	}

	/**
	 * Sets the text value of a column in the given table item.
	 * @param item TableItem to set value for
	 * @param index absolute index (in the table definition) of the column to set
	 * @param inVal value to set into the column
	 */
	protected void setColumn(final TableItem item, final int index, final String inVal) {
		String val = inVal;
		if (tableColumns[index] == null) {
			return;
		}
		final int actualIndex = tableDef.getActualIndex(index);
		if (val == null) {
			val = "";
		}
		item.setText(actualIndex, val);
	}

	/**
	 * Sets the current sort column based upon the settings in the view configuration
	 */
	protected void setSortColumn() {
		sortColumn = null;
		if (!tableDef.isSortAllowed() || table.getSortColumn() == null) {
			table.setSortDirection(SWT.NONE);
			return;
		}
		final ChillTableColumn[] cols = tableDef.getAvailableColumns();
		for (final ChillTableColumn col: cols) {
			if (col.isSortColumn()) {
				if (col.isEnabled()) {		
					sortColumn = col;
					break;
				} else {
					sortColumn = null;
					table.setSortDirection(SWT.NONE);
					return;
				}
			}
		}
		table.setSortDirection(tableDef.isSortAscending() ? SWT.UP : SWT.DOWN);
	}

	/**
	 * Gets the list of Table Column objects for the current table.
	 * 
	 * @return array of TableColumn
	 */
	public TableColumn[] getColumns() {
		return tableColumns;
	}

	/**
	 * Sets the current table definition.
	 * 
	 * @param tableDef the table definition to set
	 */
	public void setTableDef(final ChillTable tableDef) {
		this.tableDef = tableDef;
	}

	/**
	 * Fills the given table item cell 0 with the given separator followed by a line of dashes.
	 * @param sep separator text
	 * @param item table item to update
	 */
	protected void fillWithLine(final String sep, final TableItem item) {

		int start = 0;
		if (sep == null || sep.equals("")) {
			start = 0;
		} else {
			start = 1;
			item.setText(0, sep);
		}
		// This is brain dead but it's the only line solution that works.
		for (int i = start; i < table.getColumnCount(); i++) {
			item.setText(i, "------------------------------------------------------------------------------------");
		}
	}

	/**
	 * Builds an alarm state string for the given channel sample.
	 * @param data the channel sample to compute alarm string for
	 * 
	 * @return String to display in alarm state column
	 */
	public String getAlarmState(final MonitorChannelSample data) {
		final AlarmLevel dnLevel = data.getDnAlarmLevel();
		final AlarmLevel euLevel = data.getEuAlarmLevel();
		final StringBuilder fullState = new StringBuilder();

		if (dnLevel != AlarmLevel.NONE) {
			fullState.append("DN=");
			fullState.append(data.getDnAlarmState());
		}
		if (euLevel != AlarmLevel.NONE) {
			if (fullState.length() != 0) {
				fullState.append(',');
			}
			fullState.append("EU=");
			fullState.append(data.getEuAlarmState());

		}
		return fullState.toString();
	}

	/**
	 * Clears all rows in the table.
	 */
	public abstract void clearRows();

	/**
	 * Adds a channel value to the table.
	 * 
	 * @param def ChannelDefinition for the channel value to add,
	 * @param displayStuff display characteristics of the item to add
	 * @param data the ChannelSample object for the channel value to add
	 */
	public abstract void addChannelValue(IChannelDefinition def, ChannelDisplayFormat displayStuff, MonitorChannelSample data);

	/*
	 * Removed viewIsRecorded() method for getting boolean rt/recorded
	 * flag from view config and use common RealtimeRecordedSupport interface instead.
	 */

	/**
	 * Replaces all items in the table with a reordered set.
	 * 
	 * @param items List of current table items, sorted in the new order we want them in
	 */
	protected abstract void replaceTableItems(final TableItem[] items);

	/*
	 * Removed paint listener class that drew a black
	 * border around recorded displays. That feature is no longer appropriate.
	 */

	/**
	 *
	 * SortListener is a class that listens for column sort events and sorts
	 * the table by the selected column.
	 *
	 */
	public class SortListener implements Listener {

		/**
		 * {@inheritDoc}
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		@Override
		public void handleEvent(final Event e) {
			try {
				final TableColumn column = (TableColumn) e.widget;
				int index = 0;
				for (;index < tableColumns.length; index++) {
					if (column == tableColumns[index]) {
						break;
					}
				}
				if (index == tableColumns.length) {
					return;
				}
				final int oldSortColumn = getSortColumnIndex();
				if (oldSortColumn != -1) {
					tableColumns[oldSortColumn].setImage(null);
				}
				table.setSortColumn(column);
				tableDef.setSortColumn(column.getText());
				setSortColumn();
				final int newSortColumn = getSortColumnIndex();
				if (newSortColumn != -1) {
					if (tableDef.isSortAllowed()) {
						tableColumns[newSortColumn].setImage(tableDef.isSortAscending() ? upImage : downImage);
					} else {
						tableColumns[newSortColumn].setImage(null);
					}
				}

				sortTableItems(index);

			} catch (final Exception e1) {
				trace.error("Error handling table sort " + e1.toString());
				e1.printStackTrace();
			}
		}
	};

	/**
	 * Sorts the table on the given column index.
	 * 
	 * @param index the absolute index (in the table definition) of the column to be sorted
	 */
	protected void sortTableItems(final int index) {
		if (!tableDef.isSortAllowed()) {
			return;
		}

		if (index >= tableDef.getColumnCount()) {
			return;
		}
		synchronized(table) {
			final TableItem[] items = table.getItems();
			if ( items.length <= 1 ) {
				return; // Only one item so don't sort
			}

			boolean changed = false;
			final TableItemQuickSort aQuickSort = new TableItemQuickSort();
			final int lo0 = 0;
			int hi0 = table.getItemCount();
			final boolean ascending = SWT.DOWN != table.getSortDirection();
			final int tableIndex = tableDef.getActualIndex(index);
			final ChillTableColumn.SortType sortType = tableDef.getColumnSortType(index);
			TableItemQuickSort.CollatorType collatorType = TableItemQuickSort.CollatorType.CHARACTER;
			if (sortType == SortType.CHARACTER) {
				collatorType = TableItemQuickSort.CollatorType.CHARACTER;
			} else {
				collatorType = TableItemQuickSort.CollatorType.NUMERIC;
			}
			try {
				hi0 = items.length - 1;

				aQuickSort.quickSort ( items, lo0, hi0, ascending, tableIndex, collatorType );
				changed = aQuickSort.wasSwapped();
			}
			catch ( final Exception e )
			{
				TraceManager.getDefaultTracer().error ( "index = " + index + " items.length = " + items.length );

				e.printStackTrace();
			}

			if (changed) {
				replaceTableItems(items);
			}
		}	   
	}

}
