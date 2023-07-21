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
package jpl.gds.monitor.guiapp.gui.views.nattable;

import java.util.Map;

import org.eclipse.nebula.widgets.nattable.columnChooser.command.DisplayColumnChooserCommandHandler;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.data.ReflectiveColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow.DefaultGlazedListsStaticFilterStrategy;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.hideshow.ColumnHideShowLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.stack.DefaultBodyLayerStack;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import jpl.gds.monitor.guiapp.common.gui.INatListItem;
import jpl.gds.perspective.ChillTable;

/**
 * This is a factory for assembling GridLayer and the child layers for a NAT
 * table presentation, with support for GlazedList data provider, filtering, and
 * sorting - for a NAT table presentation. So the body data provider is a Glazed
 * Event List, Glazed by a Filter List, Glazed by a Sorted list. A NAT grid
 * consists of a column header region, a row header region, a body region, and a
 * corner region. Each one of these, in turn, must have a data provider, a data
 * layer, and a top layer specific to the region, at minimum. A selection layer
 * is applied to both the body and column layers, to allow for column
 * re-ordering and sorting. The viewport layer is applied on top of the
 * selection layer. The viewport applies to the area of the table that is
 * actually visible to the user.
 *
 * @param <T>
 *            any type that implements INatListItem
 */
public class MonitorGlazedListsGridLayer<T extends INatListItem> extends GridLayer {

    private DataLayer bodyDataLayer;
    private ListDataProvider<T> bodyDataProvider;
    private MonitorGlazedListsColumnHeaderLayerStack<T> columnHeaderLayerStack;
    private DefaultBodyLayerStack bodyLayerStack;
    private SortedList<T> sortedList;
    private FilterList<T> filterList;
    private ViewportLayer viewportLayer;
    private SelectionLayer selectionLayer; 
    private DefaultGlazedListsStaticFilterStrategy<T> filterStrategy;
    private ColumnHideShowLayer columnHideShowLayer;
    private DefaultRowHeaderDataLayer rowHeaderDataLayer; 

    /**
     * Constructor.
     * 
     * @param eventList the Glazed Event List that will be the body data provider.
     * @param propertyNames the property names (keys) for the columns in the table
     * @param propertyToLabelMap a map of column property name to column display label
     * @param configRegistry an IConfigRegistry containing any initial configuration
     * @param tableConfig the ChillTable object from the view configuration
     * @param rowIdAccessor the unique accessor for rows in this table
     */
    public MonitorGlazedListsGridLayer(EventList<T> eventList, String[] propertyNames,
            Map<String, String> propertyToLabelMap,
            IConfigRegistry configRegistry,
            ChillTable tableConfig, IRowIdAccessor<T> rowIdAccessor) {

        /*
         * Column properties will be accessed via reflection. Thus the
         * ReflectiveColumnPropertyAccessor. We use a custom column header data
         * provider.
         */
        this(eventList, new ReflectiveColumnPropertyAccessor<T>(propertyNames),
                new MonitorColumnHeaderDataProvider(propertyNames,
                        propertyToLabelMap, tableConfig), configRegistry, tableConfig, rowIdAccessor);
    }

    /**
     * Constructor.
     * 
     * @param eventList the Glazed Event List that will be the body data provider.
     * @param columnPropertyAccessor the column property accessor, which tells the table how
     *        to access column values in the underlying list items
     * @param columnHeaderDataProvider the column header data provider
     * @param configRegistry an IConfigRegistry containing any initial configuration
     * @param tableConfig the ChillTable object from the view configuration
     * @param rowIdAccessor the unique accessor for rows in this table
     */
    public MonitorGlazedListsGridLayer(EventList<T> eventList,
            IColumnPropertyAccessor<T> columnPropertyAccessor,
            IDataProvider columnHeaderDataProvider,
            IConfigRegistry configRegistry,
            ChillTable tableConfig, IRowIdAccessor<T> rowIdAccessor) {

        /* The false means "do not use default style configuration" */
        super(false);

        /* Wrap the input event list in a filtered list and then a sorted list */
        filterList = new FilterList<T>(eventList);
        sortedList = new SortedList<T>(filterList, null);

        /* The body data provider then gets data from the sorted list */
        this.bodyDataProvider = new ListDataProvider<T>(sortedList,
                columnPropertyAccessor);

        /* Vanilla body data layer. */
        this.bodyDataLayer = new DataLayer(this.bodyDataProvider);

        /* A layer that will propagate events on the underlying glazed lists to the
         * table. 
         */
        GlazedListsEventLayer<T> glazedListsEventLayer = new GlazedListsEventLayer<T>(
                this.bodyDataLayer, filterList);
        
        /* Now create a body layer stack over the glazed list event layer. This
         * will contain a column reorder layer, a column hide/show layer,
         * a selection layer, and a viewport layer. At first it stumped me why
         * the column reorder and hide/show layers are just not in the column header
         * stack, until I realized that the columns in the data must be moved as 
         * well when re-ordered or hidden.
         */
        this.bodyLayerStack = new DefaultBodyLayerStack(glazedListsEventLayer);

        /* Save references to several of the body stack layers for convenience. */

        this.columnHideShowLayer = bodyLayerStack.getColumnHideShowLayer();
        this.selectionLayer = bodyLayerStack.getSelectionLayer();
        this.viewportLayer = bodyLayerStack.getViewportLayer();

        /*
         * Done with body layers. Add a custom column header layer stack that
         * allows for glazed list sorting. This must be supplied the column
         * header data provider and property accessor, as well as the viewport
         * and selection layers already built.
         */
        this.columnHeaderLayerStack = new MonitorGlazedListsColumnHeaderLayerStack<T>(
                columnHeaderDataProvider, sortedList, columnPropertyAccessor,
                configRegistry, viewportLayer, selectionLayer);

        /* The filter strategy controls how rows in the filter list are filtered. 
         */
        filterStrategy = new DefaultGlazedListsStaticFilterStrategy<T>(
                filterList, columnPropertyAccessor, configRegistry);

        /* Done with the column header layers. Now add the row header layers. First
         * create a custom header data provider.
         */
        MonitorRowHeaderDataProvider rowHeaderDataProvider = new MonitorRowHeaderDataProvider(
                this.bodyDataProvider, tableConfig);

        /* Now we just use the default row header data layer and row header layer. */
        rowHeaderDataLayer = new DefaultRowHeaderDataLayer(
                rowHeaderDataProvider);
        RowHeaderLayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer,
                viewportLayer, selectionLayer);

        /* Finally, build a default corner layer stack. The corner is just the
         * area at upper left between row and column headers. Menus can be attached
         * to it. We do nothing special with it.
         */
        DefaultCornerDataProvider cornerDataProvider = new DefaultCornerDataProvider(
                this.columnHeaderLayerStack.getDataProvider(), rowHeaderDataProvider);
        DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
        CornerLayer cornerLayer = new CornerLayer(cornerDataLayer,
                rowHeaderLayer, this.columnHeaderLayerStack);

        /* Tell the superclass all the top layers for each grid area. */
        setBodyLayer(viewportLayer);
        setColumnHeaderLayer(this.columnHeaderLayerStack);
        setRowHeaderLayer(rowHeaderLayer);
        setCornerLayer(cornerLayer);
        
        /* Register the command handler for the column chooser menu item */
        DisplayColumnChooserCommandHandler columnChooserCommandHandler = new DisplayColumnChooserCommandHandler(
                selectionLayer,
                columnHideShowLayer,
                getColumnHeaderLayerStack().getColumnHeaderLayer(),
                getColumnHeaderLayerStack().getDataLayer(),
                null, null);
        registerCommandHandler(columnChooserCommandHandler);
    }

    /**
     * Gets the body data layer from the body layer stack.
     * 
     * @return DataLayer
     */
    public DataLayer getBodyDataLayer() {
        return this.bodyDataLayer;
    }

    /**
     * Gets the body data provider from the body layer stack.
     * 
     * @return ListDataProvider
     */
    public ListDataProvider<T> getBodyDataProvider() {
        return this.bodyDataProvider;
    }

    /**
     * Gets the column header layer stack.
     * 
     * @return MonitorGlazedListsColumnHeaderLayerStack
     */
    public MonitorGlazedListsColumnHeaderLayerStack<T> getColumnHeaderLayerStack() {
        return this.columnHeaderLayerStack;
    }

    /** 
     * Gets the viewport layer.
     * 
     * @return ViewportLayer
     */   
    public ViewportLayer getViewportLayer() {
        return this.viewportLayer;
    }

    /**
     * Gets the selection layer.
     * 
     * @return SelectionLayer
     */
    public SelectionLayer getSelectionLayer() {
        return this.selectionLayer;
    }

    /**
     * Gets the sorted list form the body data provider.
     * 
     * @return SortedList
     */
    public SortedList<T> getSortedList() {
        return this.sortedList;
    }

    /** 
     * Gets the filter strategy.
     * 
     * @return DefaultGlazedListsStaticFilterStrategy
     */
    public DefaultGlazedListsStaticFilterStrategy<T> getFilterStrategy() {
        return this.filterStrategy;
    }

    /**
     * Gets the column hide/show layer from the column header stack.
     * 
     * @return ColumnHideShowLayer
     */
    public ColumnHideShowLayer getColumnHideShowLayer() {
        return this.columnHideShowLayer;
    }

    /**
     * Gets the row header data layer from the row header stack.
     * 
     * @return ColumnHideShowLayer
     */
    public DefaultRowHeaderDataLayer getRowHeaderDataLayer() {
        return this.rowHeaderDataLayer;

    }

    /**
     * Gets the body layer stack.
     * 
     * @return DefaultBodyLayerStack
     */
    public DefaultBodyLayerStack getBodyLayerStack() {
        return this.bodyLayerStack;
    }
    
    /**
     * Custom row header data provider class. We use this so the row header can be 
     * toggled on and off per the current ChillTable definition.
     *
     */
    public static class MonitorRowHeaderDataProvider extends DefaultRowHeaderDataProvider implements IDataProvider {
        /** body data provider */
        protected final IDataProvider bodyDataProvider;
        /** perspective table definition */
        protected final ChillTable tableDef;

        /**
         * Constructor.
         * 
         * @param bodyDataProvider the body data provider
         * @param tableDef the ChillTable definition that defines the table in the perspective
         */
        public MonitorRowHeaderDataProvider(IDataProvider bodyDataProvider, ChillTable tableDef) {
            super(bodyDataProvider);
            this.bodyDataProvider = bodyDataProvider;
            this.tableDef = tableDef;
        }
        /**
         * {@inheritDoc}
         * @see org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider#getColumnCount()
         */
        @Override
        public int getColumnCount() {
            /*
             * Yes, it seems odd at first that a row header provider has a
             * column count. Basically, the row header is a column, if you
             * really look at it, it's just the first column in the table.
             * In short, the NAT sees every region as a table of sorts.
             */
            return tableDef.isShowRowHeader() ? 1 : 0;
        }
    }

    /**
     * Custom column header data provider class. We use this so the column header can be 
     * toggled on and off per the current ChillTable definition.
     *
     */
    public static class MonitorColumnHeaderDataProvider extends DefaultColumnHeaderDataProvider implements IDataProvider {
        /** perspective table definition */
        protected final ChillTable tableDef;

        /**
         * Constructor.
         * 
         * @param propertyNames the property names of the table columns
         * @param propertyToLabelMap the property name to display name mapping for the table columns
         * @param tableDef the ChillTable definition that defines the table in the perspective
         */
        public MonitorColumnHeaderDataProvider(String[] propertyNames,
                Map<String, String> propertyToLabelMap, ChillTable tableDef) {

            super(propertyNames, propertyToLabelMap);
            this.tableDef = tableDef;
        }

        /**
         * {@inheritDoc}
         * @see org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider#getRowCount()
         */
        @Override
        public int getRowCount() {
            /*
             * Yes, it seems odd at first that a column header provider has a
             * row count. Basically, the column header is a row, if you
             * really look at it, it's just the first row in the table.
             * In short, the NAT sees every region as a table of sorts.
             */
            return tableDef.isShowColumnHeader() ? 1 : 0;
        }
    }
   
}
