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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.command.VisualRefreshCommand;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.command.ColumnHideCommand;
import org.eclipse.nebula.widgets.nattable.hideshow.event.HideColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.hideshow.event.ShowColumnPositionsEvent;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnOverrideLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionModel;
import org.eclipse.nebula.widgets.nattable.selection.RowSelectionProvider;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionBindings;
import org.eclipse.nebula.widgets.nattable.selection.config.RowOnlySelectionConfiguration;
import org.eclipse.nebula.widgets.nattable.sort.SortConfigAttributes;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.command.SortColumnCommand;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.menu.AbstractHeaderMenuConfiguration;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.springframework.context.ApplicationContext;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.IMonitorConfigChangeListener;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.guiapp.common.gui.BoundedGlazedEventList;
import jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriber;
import jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriberListener;
import jpl.gds.monitor.guiapp.common.gui.INatListItem;
import jpl.gds.monitor.guiapp.gui.ViewPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.nattable.BaseCellLabelAccumulator;
import jpl.gds.monitor.guiapp.gui.views.nattable.MonitorGlazedListsGridLayer;
import jpl.gds.monitor.guiapp.gui.views.nattable.MonitorSelectionStyleConfiguration;
import jpl.gds.monitor.guiapp.gui.views.nattable.MonitorTableStyleConfiguration;
import jpl.gds.monitor.guiapp.gui.views.nattable.NatComparatorUtil;
import jpl.gds.monitor.guiapp.gui.views.nattable.NatTableStatusLineComposite;
import jpl.gds.monitor.guiapp.gui.views.support.ICountableView;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.ChillTableColumn;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * This class is used as the base class for monitor view classes that present a
 * scrolling table based upon the content of a sorted, filtered Glazed Event
 * List, rendered using a NAT Table widget.
 * <p>
 * <br>
 * Basic Design: There is a BoundedGlazedEventList object that contains a
 * central list of data items for display. This is in turn wrapped by some
 * instance of IGlazedListHistorySubscriber, which is a data-type-specific
 * object that is responsible for obtaining the items on the list via some
 * mechanism, such as incoming message service messages (though the source does not have to
 * be limited to the message service) and for overall control of the BoundedGlazedEventList
 * itself. That list MAY NOT be modified by this class. (One exception described
 * below.)
 * <p>
 * <br>
 * For the NAT Table implementation here, the source event list is glazed by a
 * filter list, which filters rows based upon the filtering criteria in this
 * view, and then by a sorted list, which sorts the data according to the column
 * selection in the NatTable in this view. What this table displays, in the end,
 * is the sorted list. Again, neither of these lists should be modified by this
 * view, as modifications affect the underlying event list. Filter and sorting
 * criteria may be modified, as they affect only one view instance.
 * <p>
 * <br>
 * Note: Currently this class modifies the glazed event list content in one way:
 * if a user elects to mark/unmark a row, the mark is attached to the underlying
 * data object stored in the glazed event list. This means that all NAT 
 * views on the same base event list display the same marks.
 * 
 * @param <T>
 *            any type that implements INatListItem
 *
 *
 *
 */
public abstract class AbstractNatTableViewComposite<T extends INatListItem>
        implements View, ICountableView, IGlazedListHistorySubscriberListener,
        IMonitorConfigChangeListener {

    /*
     * Configuration labels. These are applied to data items and columns to tag
     * them for specific types of rendering or sorting.
     */
    private static final String CUSTOM_LONG_COMPARATOR_LABEL = "LONG_COMPARATOR";
    private static final String CUSTOM_DOUBLE_COMPARATOR_LABEL = "DOUBLE_COMPARATOR";
    private static final String CUSTOM_STRING_COMPARATOR_LABEL = "STRING_COMPARATOR";

    /**
     * Default tracer
     */
    protected final Tracer                     tracer;

    private static final int                   DEFAULT_MAX_SIZE               = 1000;


    /**
     * Parent composite.
     */
    protected Composite parent;

    /**
     * Main composite.
     */
    protected Composite mainComposite;

    /**
     * NAT Table object.
     */
    protected NatTable table;

    /**
     * Color used for marking rows in the table
     */
    protected Color markColor;
    
    /**
     * Font used for marking rows in the table
     */
    protected Font markFont;
    
    /**
     * Underlying list subscriber feeding this view.
     */
    protected IGlazedListHistorySubscriber<T>  eventSubscriber;

    /**
     * Flag indicating whether the scrolling view is paused.
     */
    protected boolean isPaused = false;

    /**
     * Unique accessor for table rows. The NAT table needs this in order to
     * maintain row selection and other labels during sorts.
     */
    protected final IRowIdAccessor<T> rowIdAccessor = new IRowIdAccessor<T>() {
        @Override
        public Serializable getRowId(final T rowObject) {
            return rowObject.getUUID();
        }
    };

    /**
     * List of currently selected row indices.
     */
    protected List<T> selectedItems = new LinkedList<T>();

    /**
     * Column that is currently selected as the one to sort on
     */
    protected ChillTableColumn sortColumn;

    /**
     * Table properties from the perspective.
     */
    protected ChillTable tableDef;

    /**
     * View configuration from the perspective.
     */
    protected IViewConfiguration viewConfig;

    /**
     * View preferences shell/window.
     */
    private ViewPreferencesShell prefShell;

    /**
     * Time the view was last cleared.
     */
    private long                               lastClearTime                  = 0;

    /**
     * Column property names. These are the keys used to access the columns in
     * the table. Column keys must be placed on this list with enabled columns
     * first, in the order the user has configured them, followed by all the
     * disabled columns in any order.
     */
    private String[] propertyNames;

    /**
     * Map of column property name to column display name.
     */
    private final Map<String, String> propertyToDisplayLabels = new HashMap<String, String>();

    /**
     * Map of column property name to column official name. The official names
     * are the keys into the ChillTable.
     */
    private final Map<String, String> propertyToOfficialNames = new HashMap<String, String>();

    protected BoundedGlazedEventList<T>        eventList;

    /**
     * Virtual sorted list that glazes (overlays) the underlying event list.
     * This is generally the list that GUI components should interact with.
     */
    private SortedList<T> sortedEventList;

    /**
     * The NAT layer stack for the entire table.
     */
    private MonitorGlazedListsGridLayer<T> glazedListsGridLayer;

    /**
     * Glazed list filter matcher for filtering by last clear time.
     */
    private Matcher<T> clearMatcher;

    /**
     * List of matcher editors for the Glazed list filtering.
     */
    private final List<MatcherEditor<T>> matcherEditors = new LinkedList<MatcherEditor<T>>();

    /**
     * The NAT table menu, used in the body and row header areas.
     */
    private CommonNatTableMenuConfiguration<T> tableMenu;

    /**
     * The Sorted List Listener. Handles auto-scrolling as the list is updated.
     */
    private ListEventListener<T> eventListener;

    /**
     * Optional status line composite, which displays view status at the bottom
     * of the view beneath the table.
     */
    protected NatTableStatusLineComposite statusLine;
    /** The current application context */
	protected final ApplicationContext appContext;

    /**
     * Constructor.
     * @param appContext the current application context
     * 
     * @param config
     *            the ViewConfiguration for the parent view
     */
    public AbstractNatTableViewComposite(final ApplicationContext appContext, final IViewConfiguration config) {
    	this.appContext = appContext;
        tracer = TraceManager.getTracer(appContext, Loggers.UTIL);
        viewConfig = config;
        setMarkAttributes(new ChillColor(ColorName.DARK_GREY));
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void init(final Composite parent) {
        this.parent = parent;
        createGui();
        appContext.getBean(MonitorConfigValues.class).addListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.View#getMainControl()
     */
    @Override
    public Control getMainControl() {
        return mainComposite;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.View#getViewConfig()
     */
    @Override
    public IViewConfiguration getViewConfig() {
        return viewConfig;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.View#updateViewConfig()
     */
    @Override
    public void updateViewConfig() {

        /*
         * In general, most of the view configuration is automatically updated
         * when the user changes preferences or selects certain menu items. What
         * is left is the current column configuration, which needs to be
         * updated in the view configuration to match the current table
         * settings.
         */

        /* Get the NAT layers we need to query for column information. */
        final ColumnHideShowLayer columnHideShow = glazedListsGridLayer
                .getColumnHideShowLayer();
        final DataLayer dataLayer = glazedListsGridLayer.getBodyDataLayer();
        final SortHeaderLayer<T> sortLayer = glazedListsGridLayer
                .getColumnHeaderLayerStack().getSortHeaderLayer();

        /* Loop through the list of property names for all defined columns, hidden or not */
        for (int i = 0; i < propertyNames.length; i++) {

            /*
             * First, locate the ChillTableColumn object that matches the column
             * property name.
             */
            final int actualIndex = columnHideShow.getColumnPositionByIndex(i);
            final String officialName = propertyToOfficialNames.get(propertyNames[i]);
            final ChillTableColumn col = tableDef.getColumn(officialName);

            /*
             * Set the width of the column.
             */
            final int width = dataLayer.getColumnWidthByPosition(i);
            col.setCurrentWidth(width);

            /*
             * Set the enabled and sort status of the column if it is not 
             * hidden. If it is hidden, its column index will be -1.
             */
            if (actualIndex != -1) {
                /*
                 * If this is the sort column, mark it in the table definition.
                 */
                if (sortLayer.getSortModel().isColumnIndexSorted(i)) {
                    tableDef.setSortColumn(officialName);
                    tableDef.setSortAscending(sortLayer.getSortModel()
                            .getSortDirection(i) == SortDirectionEnum.ASC);
                }
                col.setEnabled(true);
                col.setCurrentPosition(actualIndex);
            } else {
                col.setEnabled(false);
            }
        }

    }

    /**
     * Gets a copy of the list of currently selected items.
     * 
     * @return selected item list; may be empty
     */
    public List<T> getSelectedItems() {
        return new LinkedList<T>(this.selectedItems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.View#clearView()
     */
    @Override
    public void clearView() {

        /*
         * Clear is implemented by essentially filtering out all rows before the
         * current receipt time, because the underlying list may be shared and
         * we cannot really clear it. It is important to do this on the base
         * event list rather than the sorted list, since the clear time is
         * grabbed from the last record in the list, and only the base event
         * list keeps records in receipt order.
         */
        
        /* Do nothing if the list is already empty */
        if (eventList.size() == 0) {
            return;
        }
        
        try {
            eventList.getEventList().getReadWriteLock().readLock().lock();
            final int len = eventList.size();
            this.lastClearTime = eventList.getEventList().get(len - 1)
                    .getReceiptTime();
        } finally {
            eventList.getEventList().getReadWriteLock().readLock().unlock();
        }
        resetStaticFilters();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.support.ICountableView#getMarkedCount()
     */
    @Override
    public long getMarkedCount() {
        
        int count = 0;
        /*
         * Count all the marked items in the current sorted list, which are
         * the marked records this particular view sees.
         */
        try {
            this.sortedEventList.getReadWriteLock().readLock().lock();
            for (final T t : this.sortedEventList) {
                if (t.isMarked()) {
                    count++;
                }
            } 
        } finally {
            this.sortedEventList.getReadWriteLock().readLock().unlock();
        }
        return count;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.support.ICountableView#getRowCount()
     */
    @Override
    public long getRowCount() {
        return this.sortedEventList.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.support.ICountableView#getSelectedCount()
     */
    @Override
    public long getSelectedCount() {
        return this.selectedItems.size();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriberListener#pauseReleased()
     */
    @Override
    public void pauseReleased() {
        /*
         * This is notification from the IGlazedListHistorySubscriber that the
         * incoming buffer is full. The view must release any pause it has.
         */
        this.resume();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.config.IMonitorConfigChangeListener#globalConfigurationChange(jpl.gds.monitor.config.GlobalPerspectiveParameter, java.lang.Object)
     */
    @Override
    public void globalConfigurationChange(final GlobalPerspectiveParameter param, final Object newValue) {
        if (param == GlobalPerspectiveParameter.SCLK_FORMAT) {
            glazedListsGridLayer.getViewportLayer().doCommand(new VisualRefreshCommand());
        }
    }

    /**
     * Adds an optional status line composite to the mainComposite. Must be
     * called by the subclass at the appropriate spot in createGui().
     * By default the filter status in the status line is set to false.
     */
    protected void addStatusLine() {
        this.statusLine = new NatTableStatusLineComposite(mainComposite);
        setFiltered(false);
    }

    /**
     * Creates the NAT table and configures it.
     */
    protected void createTable() {

        /*
         * Sets up the column property names/keys and labels. The available
         * column names are in the table definition attached to the view
         * configuration.
         */
        setupColumnProperties();

        /*
         * Get the base event subscriber feeding this table from the subclass,
         * which know what type of data items we are displaying. Add us as a
         * listener to it for control events from the subscriber.
         */
        this.eventSubscriber = getEventSubscriber();
        eventList = new BoundedGlazedEventList<>(DEFAULT_MAX_SIZE);
        this.eventSubscriber.addBoundedEventList(eventList);
        this.eventSubscriber.addListener(this);

        /* The NAT table is primarily configured by stuffing things into its config registry */
        final ConfigRegistry configRegistry = new ConfigRegistry();

        /*
         * This creates all the layers of the table (the layer stack). The grid
         * consists of 4 areas or regions: a row header area, a column header
         * area, and body area, and a corner area (upper left, between row and
         * column header). The body area will be fed by a data provider
         * consisting of the base event list, glazed by a filter list, glazed by
         * a sorted list. And if anyone can figure out how to get rid of the
         * unchecked cast here, I'd love to know.
         */
        glazedListsGridLayer = new MonitorGlazedListsGridLayer<T>(eventList.getEventList(), this.propertyNames,
                this.propertyToDisplayLabels, configRegistry, tableDef,
                rowIdAccessor);

        /*
         * Keep a reference to the sorted list, which is what we mostly interact
         * with.
         */
        this.sortedEventList = glazedListsGridLayer.getSortedList();

        /*
         * This event listener on the sorted list will auto-scroll to the
         * position of the last inserted record unless the view is paused.
         */
        sortedEventList.addListEventListener(createListEventListener());

        /*
         * Set the cell label accumulator, which will tag data rows with
         * specific labels that control their rendering.
         */
        glazedListsGridLayer.getViewportLayer().setConfigLabelAccumulator(
                createCellLabelAccumulator(
                        glazedListsGridLayer.getViewportLayer(),
                        this.sortedEventList));

        /*
         * Add the filters used by the filter glazed list to the filter
         * strategy object attached to the GlazedListsGridLayer.
         */
        addStaticFilters();
        eventList.setFilters(this.matcherEditors);

        /*
         * Now create the table, giving it the whole layer stack. The false
         * argument turns off auto-configuration of the table. If you don't do
         * that, you can't add new configuration or layers after the fact.
         */
        this.table = new NatTable(mainComposite, glazedListsGridLayer, false);

        /*
         * Set the config registry for the table and begin adding more
         * configuration.
         */
        this.table.setConfigRegistry(configRegistry);

        /*
         * This establishes the table style - base fonts and colors, text
         * alignment of cells.
         */
        this.table.addConfiguration(new MonitorTableStyleConfiguration(
                viewConfig));

        /*
         * This adds the ability to sort columns with a mouse click on the
         * header.
         */
        this.table.addConfiguration(new SingleClickSortConfiguration());

        /* This establishes the rendering style for selected rows. */
        this.table.addConfiguration(new MonitorSelectionStyleConfiguration());
        
        /* Create a column override label accumulator, which is used to register
         * labels and attach them to columns. These labels in turn control
         * column behavior and rendering.
         */
        final ColumnOverrideLabelAccumulator labelAccumulator = new ColumnOverrideLabelAccumulator(
                glazedListsGridLayer.getColumnHeaderLayerStack().getDataLayer());
        glazedListsGridLayer.getColumnHeaderLayerStack().getDataLayer().setConfigLabelAccumulator(labelAccumulator);
        

        /* Add column label configuration to the column label accumulator.
         * This establishes the comparators used in sorting by selected column
         */
        this.table
                .addConfiguration(getColumnComparatorConfiguration(labelAccumulator));

        /* Configure row selection behavior */
        configureTableSelection();
        
        /* Configure the NAT table popup menus */
        configureTableMenus();

        /* Add configuration for highlighting user-marked rows. */
        table.addConfiguration(getMarkConfiguration());

        /* Set column widths to the widths configured in the perspective. */
        for (int i = 0; i < propertyNames.length; i++) {
            final ChillTableColumn col = tableDef.getColumn(propertyToOfficialNames
                    .get(propertyNames[i]));
            glazedListsGridLayer.getBodyDataLayer().setColumnWidthByPosition(i,
                    col.getCurrentWidth());
        }


        /* Apply all the established configuration to the NAt table */
        this.table.configure();

        /*
         * Hide columns not enabled in the perspective. Enabled columns are
         * first in the property name list; This disables all columns after the
         * last enabled column.
         */
        final int enabledCols = tableDef.getActualColumnCount();

        for (int i = enabledCols; i < propertyNames.length; i++) {

            /*
             * The last argument is the column position to remove/hide. With each
             * removal, the list gets shorter. Thus, we always use the same
             * index for removal.
             */
            table.doCommand(new ColumnHideCommand(glazedListsGridLayer
                    .getViewportLayer(), enabledCols));
        }

        /*
         * Establish initial sorting state from the view configuration.
         */
        setSortColumn();

        /*
         * Upon dispose, we want to unregister our listener on the sorted list,
         * and remove this view instance as a listener from the event subscriber
         * that is feeding it.
         */
        table.addDisposeListener(new DisposeListener() {

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
             */
            @Override
            public void widgetDisposed(final DisposeEvent arg) {
                sortedEventList.removeListEventListener(eventListener);
                eventSubscriber.removeBoundedEventList(eventList);
                eventSubscriber.removeListener(AbstractNatTableViewComposite.this);
                if (markColor != null && !markColor.isDisposed()) {
                    markColor.dispose();
                }
                appContext.getBean(MonitorConfigValues.class).removeListener(AbstractNatTableViewComposite.this);

            }

        });

    }

    /**
     * Creates the Configuration Label Accumulator for data cells. The
     * configuration labels attached to cells, rows, and columns control how
     * their cells are rendered. There are two ways to do this. I chose to
     * create a base class that defines any common label accumulation, and the
     * subclass must extend it to add more. It could also have been done using
     * and aggregate accumulator, which can combine multiple accumulators, but I
     * didn't know about that at first and this seems to work just fine.
     * 
     * @param bodyLayer
     *            the body layer to be used to get row indices
     * @param sortedEventList
     *            the sorted list containing rows to be rendered
     * @return new IConfigLabelAccumulator
     */
    protected abstract IConfigLabelAccumulator createCellLabelAccumulator(
            final ILayer bodyLayer, final SortedList<T> sortedEventList);
    
   
    /**
     * Updates the status line (if there is one) with the current
     * view filter status.
     * 
     * @param filtered true if the view is filtered, false if not
     */
    protected void setFiltered(final boolean filtered) {
        if (this.statusLine != null) {
            this.statusLine.setFiltered(filtered);
        }
    }
    
    /**
     * Adds a static matcher for the glazed filter list, by creating a matcher
     * editor and placing the editor on a list, and then adds the matcher to 
     * the filter strategy in the grid layer.
     * 
     * @param matcher
     *            the Glazed list Matcher to add
     */
    protected void addMatcherEditor(final Matcher<T> matcher) {
        final MatcherEditor<T> editor = new AbstractMatcherEditor<T>() {
            {
                fireChanged(matcher);
            }
        };
        this.matcherEditors.add(editor);

        glazedListsGridLayer.getFilterStrategy().addStaticFilter(editor);

    }

    /**
     * Adds static filters to the Glazed list filter strategy. Static filters
     * are used by the filtered Glazed list to determine which rows to pass
     * through for rendering.
     */
    protected void addStaticFilters() {

        /*
         * The clear matcher filters out any rows that were received before the
         * last time the user elected to clear the display. Since we cannot
         * delete from the shared event list, I thought this approach was very
         * clever of me.
         */
        clearMatcher = new Matcher<T>() {

            /**
             * {@inheritDoc}
             * @see ca.odell.glazedlists.matchers.Matcher#matches(java.lang.Object)
             */
            @Override
            public boolean matches(final T item) {
                return item.getReceiptTime() > lastClearTime;
            }
        };

        /* Add a matcher editor for this matcher to the overall list of editors. */
        addMatcherEditor(clearMatcher);
    }

    /**
     * Instructs the subclass to add its custom NAT table menu items to the
     * supplied popup menu builder, which builds the menu attached to both the
     * body and raow regions of the grid.
     * 
     * @param menuBuilder
     *            PopupMenuBuilder to add menu items to.
     * @return new PopupMenuBuilder (which is not guaranteed, and in fact is
     *         probably not, the same as the one passed in as argument)
     */
    protected abstract PopupMenuBuilder addCustomBodyMenuItems(
            PopupMenuBuilder menuBuilder);

    /**
     * Instructs the subclass to return the glazed event list subscriber
     * supplying this view with data.
     * 
     * @return IGlazedListHistorySubscriber
     */
    protected abstract IGlazedListHistorySubscriber<T> getEventSubscriber();

    /**
     * Draws the GUI components.
     */
    protected abstract void createGui();
    
    /**
     * Gets the current title for the "toggle column header" menu item, based
     * upon the current state of the header.
     * 
     * @return menu item title
     */
    protected String getToggleColumnHeaderText() {
        return (tableDef.isShowColumnHeader() ? "Hide " : "Show ")
                + CommonNatTableMenuConfiguration.HIDE_SHOW_COL_HEADER_MENU_TITLE;
    }

    /**
     * Gets the current title for the "toggle row header" menu item, based upon
     * the current state of the header.
     * 
     * @return menu item title
     */
    protected String getToggleRowHeaderText() {
        return (tableDef.isShowRowHeader() ? "Hide " : "Show ")
                + CommonNatTableMenuConfiguration.HIDE_SHOW_ROW_HEADER_MENU_TITLE;
    }

    /**
     * Creates a sub-class specific instance of a view preferences shell.
     * 
     * @return ViewPreferencesShell
     */
    protected abstract ViewPreferencesShell createPreferencesShell();

    /**
     * Sets the current mark color and font. Does not trigger any actual drawing update.
     * 
     * @param newColor
     *            the color to set.
     */
    protected void setMarkAttributes(final ChillColor newColor) {
        if (this.markColor != null && !this.markColor.isDisposed()) {
            this.markColor.dispose();
            this.markColor = null;
        }
        this.markColor = ChillColorCreator.getColor(newColor);
        
        if (this.markFont != null && !this.markColor.isDisposed()) {
            this.markFont.dispose();
            this.markFont = null;
        }
        
        final ChillFont cf = new ChillFont(viewConfig.getDataFont().getFace(), viewConfig.getDataFont().getSize(), SWT.ITALIC);
        this.markFont = ChillFontCreator.getFont(cf);
    }

    /**
     * Responds to a new selection event by setting the selected items list. Can
     * be overridden to add additional handling when selection occurs.
     * 
     * @param selected
     *            the list of selected data items
     */
    protected void setSelection(final List<T> selected) {
        selectedItems.clear();
        selectedItems.addAll(selected);
    }

    /**
     * Sets the current table definition, which comes from the view configuration.
     * 
     * @param def
     *            ChillTable to set
     */
    protected void setTableDefinition(final ChillTable def) {
        tableDef = def;
    }

    /**
     * Resumes scrolling of the table if it is paused.
     */
    protected void resume() {
        if (this.isPaused()) {
            this.isPaused = false;
            if (this.statusLine != null) {
                this.statusLine.setPaused(false);
            }
            eventList.releasePause();
        }
    }

    /**
     * Pauses scrolling of the table if it is not already paused.
     */
    protected void pause() {
        if (!this.isPaused()) {
            this.isPaused = true;
            if (this.statusLine != null) {
                this.statusLine.setPaused(true);
            }
            eventList.pause();
        }
    }

    /**
     * Resets all static filters, causing both current and future rows to be
     * re-filtered.
     */
    protected void resetStaticFilters() {

        /*
         * Static filters can be removed and added, but new filtering criteria
         * will only be applied to new rows and not to existing rows. In order to
         * apply the new filters to existing rows, the only thing I could find
         * that works is to reset the MatcherEditors list attached to the filter
         * strategy. If there is a better way, I do not know it.
         */
        EventList<MatcherEditor<T>> configuredEditors = null;
        try {
            configuredEditors = glazedListsGridLayer
                    .getFilterStrategy().getMatcherEditor().getMatcherEditors();
            configuredEditors.getReadWriteLock().writeLock().lock();
            configuredEditors.clear();
            configuredEditors.addAll(this.matcherEditors);
        } finally {
            if (configuredEditors != null) {
                configuredEditors.getReadWriteLock().writeLock().unlock();
            }
        }
        eventList.setFilters(this.matcherEditors);
    }

    /**
     * Indicates if the scrolling of the table is currently paused.
     * 
     * @return true if paused, false if not
     */
    protected boolean isPaused() {
        return this.isPaused;
    }

    /**
     * Toggles the table row header, updating the underlying ChillTable
     * definition in the process.
     */
    protected void toggleRowHeader() {
        final boolean current = tableDef.isShowRowHeader();
        tableDef.setShowRowHeader(!current);
        if (!current) {
            glazedListsGridLayer.getRowHeaderDataLayer().fireLayerEvent(
                    new ShowColumnPositionsEvent(glazedListsGridLayer
                            .getRowHeaderDataLayer(), Arrays
                            .asList(new Integer[] { 0 })));
        } else {
            glazedListsGridLayer.getRowHeaderDataLayer().fireLayerEvent(
                    new HideColumnPositionsEvent(glazedListsGridLayer
                            .getRowHeaderDataLayer(), Arrays
                            .asList(new Integer[] { 0 })));
        }
    }

    /**
     * Toggles the table column header, updating the underlying ChillTable
     * definition in the process.
     */
    protected void toggleColumnHeader() {
        final boolean current = tableDef.isShowColumnHeader();
        tableDef.setShowColumnHeader(!current);
        if (!current) {
            glazedListsGridLayer.getColumnHeaderLayerStack().fireLayerEvent(
                    new ShowColumnPositionsEvent(glazedListsGridLayer
                            .getColumnHeaderLayerStack().getDataLayer(), Arrays
                            .asList(new Integer[] { 0 })));
        } else {
            glazedListsGridLayer.getColumnHeaderLayerStack().fireLayerEvent(
                    new HideColumnPositionsEvent(glazedListsGridLayer
                            .getColumnHeaderLayerStack(), Arrays
                            .asList(new Integer[] { 0 })));
        }
    }

    /**
     * Updates the view based upon a newly modified view configuration.
     */
    protected void updateFromConfigChange() {

        /*
         * Updates mark color and font. Curiously, there is no obvious ability
         * to remove existing configuration. Each new one just seems to override
         * the old one.
         */
        table.addConfiguration(getMarkConfiguration());

        /* Updates base fonts and colors */
        table.addConfiguration(new MonitorTableStyleConfiguration(viewConfig));

        /* Applies the new configuration to the table */
        table.configure();

    }

    /**
     * Displays the view-specific preferences dialog. Updates the view
     * configuration from current table settings before doing so, and updates
     * the view from changes to the view configuration after the preferences
     * window is dismissed.
     */
    protected void displayPreferences() {

        /* If the preferences window is already active, just bring it forward. */
        if (prefShell != null) {
            prefShell.getShell().setActive();
            return;
        }
        
        /* Create the subclass-specific preferences shell. */
        prefShell = createPreferencesShell();

        /*
         * Make sure all current settings are in the view config and set them
         * into the preferences shell.
         */
        updateViewConfig();
        prefShell.setValuesFromViewConfiguration(getViewConfig());

        /*
         * Register what happens when the preferences shell is dismissed.
         */
        prefShell.getShell().addDisposeListener(new DisposeListener() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
             */
            @Override
            public void widgetDisposed(final DisposeEvent event) {
                try {
                    if (!prefShell.wasCanceled()) {
                        /*
                         * Preferences shell was not canceled. Copy the settings
                         * from the preferences shell to the view configuration.
                         * Then update the display from that configuration.
                         */
                        updateViewConfig();
                        prefShell
                                .getValuesIntoViewConfiguration(getViewConfig());
                        updateFromConfigChange();
                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                    tracer.error("Unable to handle exit from Preferences window "
                            + e.toString());
                } finally {
                    prefShell = null;
                }
            }
        });

        /* Open the preferences shell */
        prefShell.open();

    }

    /**
     * Creates a list event listener on the sorted event list. The purpose of
     * this listener is to scroll the viewport when new rows are added.
     * 
     * @return ListEventListener, also assigned to the eventListener member
     *         field
     */
    private ListEventListener<T> createListEventListener() {

        this.eventListener = new ListEventListener<T>() {

            /**
             * {@inheritDoc}
             * 
             * @see ca.odell.glazedlists.event.ListEventListener#listChanged(ca.odell.glazedlists.event.ListEvent)
             */
            @Override
            public void listChanged(final ListEvent<T> listChanges) {
                try {
                    /*
                     * The list event is actually a list of list events (surprise!) You
                     * have to iterate through it to check the individual
                     * events.
                     */
                    while (listChanges.next()) {

                        final int type = listChanges.getType();
                        final int index = listChanges.getIndex();

                        /*
                         * If this is an insert, and the view is not paused,
                         * scroll the new record into the viewport. If the event
                         * is an update, a mark was probably set, but in any case we 
                         * want to reflect any updates, so refresh the viewport.
                         */
                        if (type == ListEvent.INSERT) {

                            if (!isPaused()) {
                                glazedListsGridLayer.getViewportLayer()
                                        .moveRowPositionIntoViewport(index);
                            }
                        } else if (type == ListEvent.UPDATE) {
                            glazedListsGridLayer.getViewportLayer().doCommand(new VisualRefreshCommand());
                        }

                    }
                } catch (final Exception e) {
                    tracer.error("Unable to handle list event in AbstractNatTableViewComposite");
                    e.printStackTrace();
                }

            }
        };

        return this.eventListener;

    }
    
    /**
     * Configures the NAT table popup menus.
     */
    private void configureTableMenus() {

        /*
         * Create the menu configuration for the menu attached to the body and
         * row areas of the grid.
         */
        this.tableMenu = new CommonNatTableMenuConfiguration<T>(this, table);
        table.addConfiguration(this.tableMenu);

        /*
         * The menu detect listener makes sure that the titles of the menu items
         * reflect current state when the menu comes up.
         */
        table.addMenuDetectListener(new MenuDetectListener() {

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.swt.events.MenuDetectListener#menuDetected(org.eclipse.swt.events.MenuDetectEvent)
             */
            @Override
            public void menuDetected(final MenuDetectEvent arg0) {
                tableMenu.setMenuItemNames();
            }

        });

        /*
         * Add the corner, column header, and row header menus to the
         * configuration
         */
        table.addConfiguration(new AbstractHeaderMenuConfiguration(table) {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.AbstractHeaderMenuConfiguration#createColumnHeaderMenu(org.eclipse.nebula.widgets.nattable.NatTable)
             */
            @Override
            protected PopupMenuBuilder createColumnHeaderMenu(final NatTable natTable) {
                return super.createColumnHeaderMenu(natTable)
                        .withHideColumnMenuItem().withShowAllColumnsMenuItem()
                        .withAutoResizeSelectedColumnsMenuItem()
                        .withColumnChooserMenuItem();
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.nebula.widgets.nattable.ui.menu.AbstractHeaderMenuConfiguration#createCornerMenu(org.eclipse.nebula.widgets.nattable.NatTable)
             */
            @Override
            protected PopupMenuBuilder createCornerMenu(final NatTable natTable) {
                /*
                 * Only one menu item on the corner area -- to show all columns.
                 * This prevents the user from being stuck if he hides all
                 * columns and so can't get to the column header menu.
                 */
                return super.createCornerMenu(natTable)
                        .withShowAllColumnsMenuItem();
            }

        });

    }

    /**
     * Sets the current sort column from the ChillTable configuration.
     */
    private void setSortColumn() {
        
        /* Get the sort column name from the table definition. */
        final String sortColName = tableDef.getSortColumn();
        if (sortColName == null) {
            return;
        }
        
        /* Map the column name to its ChillTable index, and then map
         * that to its actual index in the currently displayed table. 
         */
        final int absIndex = tableDef.getColumnIndex(sortColName);
        if (tableDef.isColumnEnabled(absIndex)) {
            final int colIndex = tableDef.getCurrentPosition(tableDef.getColumnIndex(sortColName));
            /* Fire a command to the column header stack to sort by the selected
             * column in the desired order.
             */
            glazedListsGridLayer.getColumnHeaderLayerStack().doCommand(
                    new SortColumnCommand(glazedListsGridLayer
                            .getColumnHeaderLayerStack(), colIndex, tableDef
                            .isSortAscending() ? SortDirectionEnum.ASC
                            : SortDirectionEnum.DESC));
        }

    }

    /**
     * Configures the behavior for table row selection. Right now, we configure
     * the table to support only full row selection, and no column selection.
     */
    private void configureTableSelection() {

        final ListDataProvider<T> bodyDataProvider = glazedListsGridLayer
                .getBodyDataProvider();

        /* Allow selection of complete rows only */
        final SelectionLayer selectionLayer = glazedListsGridLayer
                .getSelectionLayer();
        final RowOnlySelectionConfiguration<T> selectionConfig = new RowOnlySelectionConfiguration<T>();
        selectionLayer.addConfiguration(selectionConfig);
        this.table.addConfiguration(new RowOnlySelectionBindings());

        /*
         * Preserve selection following updates and sorts. NAT table
         * accomplishes this by tracking the unique ID of selected data items.
         * That's the rowIdAccessor. Thank goodness, because this was a truly
         * difficult thing to do with the SWT table we used before.
         */
        final RowSelectionModel<T> rm = new RowSelectionModel<T>(selectionLayer,
                bodyDataProvider, rowIdAccessor);
        selectionLayer.setSelectionModel(rm);

        /* Set up an event provider for row selection. */
        final ISelectionProvider selectionProvider = new RowSelectionProvider<T>(
                selectionLayer, bodyDataProvider, true);

        /* Add event handler for row selection */
        selectionProvider
                .addSelectionChangedListener(new ISelectionChangedListener() {

                    /**
                     * {@inheritDoc}
                     * 
                     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
                     */
                    @SuppressWarnings("unchecked")
                    @Override
                    public void selectionChanged(final SelectionChangedEvent event) {

                        try {
                            final IStructuredSelection selection = (IStructuredSelection) event
                                    .getSelection();
                            if (selection == null) {
                                return;
                            }
                            /* Just save selected items in a member list */
                            setSelection(selection.toList());
                            
                        } catch (final Exception e) {
                            tracer.error("Problem handling table selection event in AbstractNatTableViewComposite");
                            e.printStackTrace();
                        }
                    }

                });

    }

    /**
     * Creates a registry configuration that registers the MARK LABEL
     * configuration label and associates it with a cell style that applies the
     * current mark color and font.
     * 
     * @return AbstractRegistryConfiguration
     */
    private AbstractRegistryConfiguration getMarkConfiguration() {
        return new AbstractRegistryConfiguration() {
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.nebula.widgets.nattable.config.IConfiguration#configureRegistry(org.eclipse.nebula.widgets.nattable.config.IConfigRegistry)
             */
            @Override
            public void configureRegistry(final IConfigRegistry configRegistry) {

                final Style cellStyle = new Style();
                cellStyle.setAttributeValue(
                        CellStyleAttributes.BACKGROUND_COLOR, markColor);
                cellStyle.setAttributeValue(
                        CellStyleAttributes.FONT, markFont);
                configRegistry
                        .registerConfigAttribute(
                                CellConfigAttributes.CELL_STYLE, cellStyle,
                                DisplayMode.NORMAL,
                                BaseCellLabelAccumulator.MARK_LABEL);

            }
        };
    }

    /**
     * Creates the registry configuration that registers the labels for custom
     * sort comparators. The labels for the the custom comparators must
     * specifically go on the columnHeaderDataLayer's accumulator rather than
     * the one for the body layer, since the SortHeaderLayer will resolve cell
     * labels with respect to its underlying layer i.e columnHeaderDataLayer.
     * 
     * @param labelAccumulator
     *            the ColumnOverrideLabelAccumulator for the column header stack
     */
    private IConfiguration getColumnComparatorConfiguration(
            final ColumnOverrideLabelAccumulator labelAccumulator) {

        return new AbstractRegistryConfiguration() {

            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.nebula.widgets.nattable.config.IConfiguration#configureRegistry(org.eclipse.nebula.widgets.nattable.config.IConfigRegistry)
             */
            @Override
            public void configureRegistry(final IConfigRegistry configRegistry) {
                
                /* Loop through all the possible columns in the table. */
                for (int index = 0; index < propertyNames.length; index++) {

                    /*
                     * Get the ChillTableColumn perspective object for this
                     * column.
                     */
                    final String officialName = propertyToOfficialNames
                            .get(propertyNames[index]);
                    final ChillTableColumn col = tableDef.getColumn(officialName);

                    /*
                     * Attach a config label to each column
                     * indicating which comparator should be used based upon its
                     * sort type.
                     */
                    switch (col.getSortType()) {
                    case CHARACTER:
                        labelAccumulator.registerColumnOverrides(index,
                                CUSTOM_STRING_COMPARATOR_LABEL);
                        break;
                    case NUMERIC:
                        labelAccumulator.registerColumnOverrides(index,
                                CUSTOM_LONG_COMPARATOR_LABEL);
                        break;
                    case DOUBLE:
                        labelAccumulator.registerColumnOverrides(index,
                                CUSTOM_DOUBLE_COMPARATOR_LABEL);
                        break;
                    default:
                        labelAccumulator.registerColumnOverrides(index,
                                CUSTOM_STRING_COMPARATOR_LABEL);
                        break;
                    }

                } 

                /*
                 * Now register the custom sort labels to match them up to
                 * custom comparator instances.
                 */
                configRegistry.registerConfigAttribute(
                        SortConfigAttributes.SORT_COMPARATOR,
                        NatComparatorUtil.getCustomStringComparator(),
                        DisplayMode.NORMAL, CUSTOM_STRING_COMPARATOR_LABEL);

                configRegistry.registerConfigAttribute(
                        SortConfigAttributes.SORT_COMPARATOR,
                        NatComparatorUtil.getCustomLongComparator(),
                        DisplayMode.NORMAL, CUSTOM_LONG_COMPARATOR_LABEL);

                configRegistry.registerConfigAttribute(
                        SortConfigAttributes.SORT_COMPARATOR,
                        NatComparatorUtil.getCustomDoubleComparator(),
                        DisplayMode.NORMAL, CUSTOM_DOUBLE_COMPARATOR_LABEL);

            }
        };
    }

    /**
     * Establishes the column property array and label maps from the table
     * configuration in the perspective.
     */
    private void setupColumnProperties() {

        final int[] enabledColPositions = tableDef.getColumnOrder();

        /*
         * We need to assemble two lists: enabled column property names, in
         * order, and disabled column property names. Start with empty lists but
         * pre-allocate the enabled list so we can set the value at any index.
         */
        final List<String> disabledList = new LinkedList<String>();
        final List<String> enabledList = new ArrayList<String>(
                enabledColPositions.length);
        for (int i = 0; i < enabledColPositions.length; i++) {
            enabledList.add(null);
        }

        /*
         * Loop through all the defined columns in the perspective table
         * definition
         */
        for (final ChillTableColumn col : tableDef.getAvailableColumns()) {

            final String officialName = col.getOfficialName();
            final String displayName = col.getDisplayName();

            /*
             * Create the column property name (index name) from the official
             * name by converted blanks to underscores and converting to upper
             * case.
             * 
             * DO NOT CHANGE THIS ALGORITHM UNLESS YOU KNOW WHAT YOU ARE DOING.
             */
            final StringBuilder indexName = new StringBuilder();
            for (int j = 0; j < officialName.length(); j++) {
                if (officialName.charAt(j) != ' ') {
                    indexName.append(Character.toUpperCase(officialName
                            .charAt(j)));
                } else {
                    indexName.append('_');
                }
            }

            /*
             * Add to the maps that supply official name and display name by
             * column property name.
             */
            this.propertyToDisplayLabels.put(indexName.toString(), displayName);
            this.propertyToOfficialNames
                    .put(indexName.toString(), officialName);

            /*
             * If the column is enabled, it must go into the column property
             * array at its configured position. Disabled columns just go onto
             * the disabled list.
             */
            if (col.isEnabled()) {
                enabledList.set(col.getCurrentPosition(), indexName.toString());
            } else {
                disabledList.add(indexName.toString());
            }
        }

        /*
         * The property name array is then created by appending the disabled
         * list to the enabled list.
         */
        final List<String> allList = new LinkedList<String>();
        allList.addAll(enabledList);
        allList.addAll(disabledList);
        this.propertyNames = new String[allList.size()];
        this.propertyNames = allList.toArray(this.propertyNames);

    }
    
    /**
     * Get the text associated with the passed in item. This method uses reflection from
     * configured column names, the same as the underlying NatTable, in order to match
     * the ordering and enabled columns in the view.
     * 
     * @param item
     *            - item to get as text
     * @return string representing the item as a CSV string
     */
    public String getText(final INatListItem item) {
        final ColumnHideShowLayer hideShowLayer = this.glazedListsGridLayer.getColumnHideShowLayer();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hideShowLayer.getColumnCount(); i++) {
            final int index = hideShowLayer.getColumnIndexByPosition(i);
            if (i != 0) {
                builder.append(", ");
            }
            final Class<?> clazz = item.getClass();
            try {
                final Method m = clazz.getMethod("get" + propertyNames[index]);
                builder.append("\"");
                final Object returnVal = m.invoke(item);
                if (returnVal != null) {
                    // Underlying accessor should not be returning null, but taking a chance here would lead to a crash
                    builder.append(returnVal.toString());
                }
                builder.append("\"");

            }
            catch (final ReflectiveOperationException e) {
                tracer.error("Error copying EVR from view", e);
                builder.append("Error copying EVR from view. Check logs for details.");
            }
        }
        return builder.toString();
    }



    /**
     * Triggers an update event on the base event list for the
     * given list item.
     * 
     * @param selectedItem
     *            the item that changed
     */
    protected void updateListItem(final T selectedItem) {
        try {
            /* Trigger the update event on the base event list, not the sorted event
             * list, which is local. We want all composites that use the base list in 
             * any form to see the update event.
             */
            eventList.getEventList().getReadWriteLock().writeLock().lock();
            final int index = sortedEventList.indexOf(selectedItem);
            if (index != -1) {
                sortedEventList.set(index, selectedItem);
            }
        } finally {
            eventList.getEventList().getReadWriteLock().writeLock().unlock();
        }
    }
    
}
