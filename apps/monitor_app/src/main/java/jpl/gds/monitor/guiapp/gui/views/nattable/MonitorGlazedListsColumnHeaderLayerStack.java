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


import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultColumnHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;

import ca.odell.glazedlists.SortedList;
import jpl.gds.monitor.guiapp.common.gui.INatListItem;

/**
 * A NAT table column header layer stack that allows for sorting
 * columns by mouse click, with an underlying sorted glazed list
 * and glazed list sort model.
 * 
 * @param <T>
 *            any type that implements INatListItem
 */
public class MonitorGlazedListsColumnHeaderLayerStack<T extends INatListItem> extends
        AbstractLayerTransform {
    private final IDataProvider dataProvider;
    private final DefaultColumnHeaderDataLayer dataLayer;
    private final ColumnHeaderLayer columnHeaderLayer;
    private final SortHeaderLayer<T> sortHeaderLayer;

    /**
     * Constructor.
     * 
     * @param dataProvider the column header data provider
     * @param sortedList the underlying sorted glazed list
     * @param columnPropertyAccessor the column property accessor for the table columns
     * @param configRegistry the configuration registry for the table
     * @param viewportLayer the NAT grid viewport layer
     * @param selectionLayer the NAT grid selection layer
     * @param tableConfig the ChillTable object defining the view table in the perspective
     */
    public MonitorGlazedListsColumnHeaderLayerStack(IDataProvider dataProvider,
            SortedList<T> sortedList,
            IColumnPropertyAccessor<T> columnPropertyAccessor,
            IConfigRegistry configRegistry, ViewportLayer viewportLayer, 
            SelectionLayer selectionLayer) {

        this.dataProvider = dataProvider;
        
        this.dataLayer = new DefaultColumnHeaderDataLayer(dataProvider);
        
        this.columnHeaderLayer = new ColumnHeaderLayer(this.dataLayer, viewportLayer,
                selectionLayer);
        
        this.sortHeaderLayer = new SortHeaderLayer<T>(
                this.columnHeaderLayer, new GlazedListsSortModel<T>(sortedList,
                        columnPropertyAccessor, configRegistry, this.dataLayer),
                        false);

        setUnderlyingLayer(sortHeaderLayer);
    }

    /**
     * Gets the column header data layer from this layer stack.
     * 
     * @return DataLayer
     */
    public DataLayer getDataLayer() {
        return this.dataLayer;
    }

    /**
     * Gets the column header data provider from this layer stack.
     * 
     * @return IDataProvider
     */
    public IDataProvider getDataProvider() {
        return this.dataProvider;
    }

    /**
     * Gets the column sort header layer from this layer stack.
     * @return SortHeaderLayer
     */
    public SortHeaderLayer<T> getSortHeaderLayer() {
        return this.sortHeaderLayer;
    }
    
    /**
     * Gets the column header layer from this layer stack.
     * @return ColumnHeaderLayer
     */
    public ColumnHeaderLayer getColumnHeaderLayer() {
        return this.columnHeaderLayer;
    }
}
