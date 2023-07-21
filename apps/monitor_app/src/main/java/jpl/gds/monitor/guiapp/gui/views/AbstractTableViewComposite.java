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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import jpl.gds.monitor.guiapp.gui.views.support.ICountableView;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.ChillTableColumn;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TableItemQuickSort;

/**
 * This class is used as the base class for a variety of view composite classes that 
 * present a scrolling table of messages.
 * 
 */
public abstract class AbstractTableViewComposite implements View, ICountableView {
    private static final String UP_IMAGE = "jpl/gds/monitor/gui/up.gif";
    private static final String DOWN_IMAGE = "jpl/gds/monitor/gui/down.gif";

    /** Metadat keyword for sort key */
    protected static final String SORT_KEY = "sortKey";

    /**
     * Array of table columns
     */
    protected TableColumn[] tableColumns;

    /**
     * Table properties
     */
    protected ChillTable tableDef;

    /**
     * Table
     */
    protected Table table;

    /**
     * Parent composite
     */
    protected Composite parent;

    /**
     * Main composite
     */
    protected Composite mainComposite;

    /**
     * View configuration
     */
    protected IViewConfiguration viewConfig;

    /**
     * Column that is currently selected as the one to sort on
     */
    protected ChillTableColumn sortColumn;

    /**
     * Up arrow for ascending sorting symbol
     */
    protected Image upImage;

    /**
     * Down arrow for descending sorting symbol
     */
    protected Image downImage;

    /**
     * Data font
     */
    protected Font dataFont;

    /**
     * Composite background color
     */
    protected Color background;

    /**
     * Composite foreground color
     */
    protected Color foreground;
    
    private final List<AgedTableItem> agedItems = new ArrayList<AgedTableItem>();

    /**
     * Constructor.
     * 
     * @param config the ViewConfiguration for the parent view
     */
    public AbstractTableViewComposite(final IViewConfiguration config) {
        viewConfig = config;
        upImage = SWTUtilities.createImage(Display.getCurrent(), UP_IMAGE);
        downImage = SWTUtilities.createImage(Display.getCurrent(), DOWN_IMAGE);
    }

    /**
     * Gets the absolute column index of the currently configured sort item.
     * 
     * @return absolute column index
     */
    protected int getSortColumnIndex() {

        int oldSortColumn = -1;
        for (int i = 0; i < tableDef.getColumnCount(); i++) {
            if (tableDef.isColumnEnabled(i)) {
                if (tableDef.isSortColumn(i)) {
                    oldSortColumn = i;
                    break;
                }
            }
        }
        return oldSortColumn;

    }

    /**
     * Cancels the current sort column setting.
     */
    protected void cancelOldSortColumn() {

        final int oldSortColumn = getSortColumnIndex();
        if (oldSortColumn != -1) {
            tableColumns[oldSortColumn].setImage(null);
        }
    }

    /**
     * Updates the table from the current ViewConfiguration.
     * @param tableName name of the table presented by this object in the ViewConfiguration
     * @param columnsUpdated true to indicate column settings have changed
     */
    protected void updateTableFromConfig(final String tableName, final boolean columnsUpdated) {
        this.tableDef = viewConfig.getTable(tableName);
        if (columnsUpdated) {
            updateTableColumns();
        }
        this.table.setHeaderVisible(this.tableDef.isShowColumnHeader());
        updateTableFontAndColors();
        setSortColumn();
        final int newSortColumn = getSortColumnIndex();
        if (newSortColumn != -1) {
            if (!tableDef.isSortAllowed()) {
                tableColumns[newSortColumn].setImage(null);
            } else {
                tableColumns[newSortColumn].setImage(tableDef.isSortAscending() ? upImage : downImage);
            }
        }
        // sort table items if sort direction has changed
        if (this.tableDef.isSortAllowed() && this.table.getSortColumn() != null) {
            final int index = this.tableDef.getColumnIndex(this.tableDef.getSortColumn());
            sortTableItems(index);
        } 
    }

    /**
     * Updates table font and colors from the current ViewConfiguration.
     */
    protected void updateTableFontAndColors() {
        if (this.dataFont != null && !this.dataFont.isDisposed()) {
            this.dataFont.dispose();
            this.dataFont = null;
        }
        dataFont = ChillFontCreator.getFont(viewConfig.getDataFont());
        this.table.setFont(dataFont);

        if (this.foreground != null && !this.foreground.isDisposed()) {
            this.foreground.dispose();
            this.foreground = null;
        }
        this.foreground = ChillColorCreator.getColor(viewConfig.getForegroundColor());
        this.table.setForeground(this.foreground);

        if (this.background != null && !this.background.isDisposed()) {
            this.background.dispose();
            this.background = null;
        }
        this.background = ChillColorCreator.getColor(viewConfig.getBackgroundColor());
        this.table.setBackground(this.background);
    }

    /**
     * Sets a column value.
     * @param item the TableItem representing the row being updated
     * @param index the absolute index in the ViewCOonfiguration of the column to update
     * @param val the text value to set
     */
    protected void setColumn(final TableItem item, final int index, final String val) {
        try {
            if (tableColumns[index] == null) {
                return;
            }
            String tempVal = val;
            if (tempVal == null) {
                tempVal = "";
            }
            final int actualIndex = tableDef.getActualIndex(index);
            item.setText(actualIndex, tempVal);
        } catch (final NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#getMainControl()
     */
    @Override
    public Control getMainControl() {
        return mainComposite;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#getViewConfig()
     */
    @Override
    public IViewConfiguration getViewConfig() {
        return viewConfig;
    }

    /**
     * Sets the current table definition.
     * @param def ChillTable to set
     */
    protected void setTableDefinition(final ChillTable def) {
        tableDef = def;
    }

    /**
     *
     * SortListener is a class that listens for column sort events and sorts
     * the table by the selected column.
     *
     */
    protected class SortListener implements Listener {
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
                    if (tableDef.isSortAllowed() && tableDef.getSortColumn() != null) {
                        tableColumns[newSortColumn].setImage(tableDef.isSortAscending() ? upImage : downImage);
                    } else {
                        tableColumns[newSortColumn].setImage(null);
                    }
                }
                sortTableItems(index);
            } catch (final Exception e1) {
                TraceManager.getDefaultTracer().error("Error handling table sort " + e1.toString());

                e1.printStackTrace();
            }
        }
    };  

    /**
     * Finds the proper index at which to insert the given value, which is assumed to be
     * the value to be inserted in the current sort column.
     * 
     * @param valueToInsert text to insert into the table
     * @return index in the table at which the value should be inserted
     */
    @SuppressWarnings("unchecked")
    protected int findInsertIndex(final String valueToInsert) {

        // No sort column defined.  All new items added at the end of the table
        if (!tableDef.isSortAllowed() || sortColumn == null) {
            return table.getItemCount();
        }
        if (table.getItemCount() == 0) {
            return 0;
        }
        TableItemQuickSort.CollatorType collatorType = null;
        switch(sortColumn.getSortType()) {
        case CHARACTER: collatorType = TableItemQuickSort.CollatorType.CHARACTER;
        break;
        case NUMERIC: collatorType = TableItemQuickSort.CollatorType.NUMERIC;
        break;
        }

        final Comparator<String> collator = (Comparator<String>)TableItemQuickSort.getCollator(collatorType);

        final TableItem[] items = table.getItems(); 
        final boolean ascending = table.getSortDirection() == SWT.UP || table.getSortDirection() == SWT.NONE;

        if (valueToInsert == null) {
            if (ascending) {
                return 0;
            } else {
                return table.getItemCount();
            }
        }

        return sortKeyBinarySearch(valueToInsert, items, collator, ascending);
    }

    /**
     * Sets the current sort column from the ViewConfiguration.
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
     * Gets the current sort column.
     * @return ChillTabelColumn, or null if no sort column defined
     */
    protected ChillTableColumn getSortColumn() {
        return sortColumn;
    }

    /**
     * Sorts table items on the column with the given absolute column index.
     * @param index absolute column index of sort column
     */
    protected void sortTableItems(final int index) {
        if (!tableDef.isSortAllowed()) {
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

            final boolean ascending = table.getSortDirection() != SWT.DOWN && table.getSortDirection() != SWT.NONE;
            final int tableIndex = tableDef.getActualIndex(index);
            final ChillTableColumn.SortType sortType = tableDef.getColumnSortType(index);
            TableItemQuickSort.CollatorType collatorType = TableItemQuickSort.CollatorType.CHARACTER;
            switch(sortType) {
            case CHARACTER: collatorType = TableItemQuickSort.CollatorType.CHARACTER;
            break;
            case NUMERIC: collatorType = TableItemQuickSort.CollatorType.NUMERIC;
            break;
            }
            try {
                final int hi0 = items.length - 1;

                aQuickSort.quickSort ( items, lo0, hi0, ascending, tableIndex, collatorType );
                changed = aQuickSort.wasSwapped();

            }
            catch ( final Exception e )
            {
                TraceManager.getDefaultTracer().error ( "index = " + tableIndex + " items.length = " + items.length );

                e.printStackTrace();
            }

            if (changed) {
                replaceTableItems(items);
            }

            /*
             * Reassign the sort keys attached to all table
             * items.  I hate to do this unconditionally, but I cannot see any way around
             * it.  Even if the row order has not changed, the sort column may have, and we
             * have no way of knowing whether the sort column has actually changed at this
             * point in the code.
             */
            replaceSortKeys();
        }
    }

    /**
     * Reorders the table items in the table to match the order of the given array
     * of TableItems.
     * @param items original TableItems, in new sort order
     */
    protected abstract void replaceTableItems(TableItem[] items);
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#clearView()
     */
    @Override
    public void clearView() {
        synchronized (table) {
            table.removeAll();
            this.agedItems.clear();
        }
    }

    /**
     * Remove the given row from the table.
     * @param item TableItem to remove
     */
    public void removeEntry(final TableItem item) {
        synchronized(table) {
            for (final AgedTableItem agedItem: this.agedItems) {
                if (agedItem.item == item) {
                    this.agedItems.remove(agedItem);
                    break;
                }
            }
            item.dispose();

        }
    }

    /**
     * Updates the column structure in the table to match the current ViewConfiguration.
     */
    protected void updateTableColumns() {
        final int numColumns = tableDef.getColumnCount();
        int actualIndex = 0;
        for (int i = 0; i < numColumns; i++) {
            if (!tableDef.isColumnEnabled(i)) {
                if (tableColumns[i] != null) {
                    if (tableColumns[i] == table.getSortColumn()) {
                        table.setSortColumn(null);
                    }
                    tableColumns[i].dispose();
                    tableColumns[i] = null;
                }
            }
        }

        final Listener sortListener = new SortListener();
        for (int i = 0; i < numColumns; i++) {
            if (tableDef.isColumnEnabled(i)) {
                if (tableColumns[i] == null) {
                    tableColumns[i] = new TableColumn(table, SWT.NONE, actualIndex);
                    tableColumns[i].setText(tableDef.getOfficialColumnName(i));
                    tableColumns[i].setWidth(tableDef.getColumnWidth(i));
                    tableColumns[i].addListener(SWT.Selection, sortListener);
                    tableColumns[i].setMoveable(true);
                }
                actualIndex++;
            }
        }

        //we might have added new columns in the previous loop, need to update column order
        tableDef.setColumnOrder(table.getColumnOrder());

        if (tableDef.isSortAllowed() && table.getSortColumn() != null) {
            final int index = tableDef.getColumnIndex(tableDef.getSortColumn());
            sortTableItems(index);
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#updateViewConfig()
     */
    @Override
    public void updateViewConfig() {
        boolean columnWidthError = false;
        if (tableColumns != null) {
            for (int i = 0; i < tableColumns.length; i++) {
                if (tableColumns[i] != null) {
                    if (tableColumns[i].getWidth() == 0) {
                        columnWidthError = true;
                        break; 
                    }
                    tableDef.setColumnWidth(i, tableColumns[i].getWidth());
                }
            }
        }
        if (tableDef.isSortAllowed() && table.getSortColumn() != null) {
            tableDef.setSortColumn(table.getSortColumn().getText());
        }

        tableDef.setColumnOrder(table.getColumnOrder());

        if (columnWidthError && !WarningUtil.getWidthWarningShown()) {
            SWTUtilities.showWarningDialog(mainComposite.getShell(), 
                    "Save Warning for View " + viewConfig.getViewName(),
                    "Table column widths with 0 values were found during the save of the perspective. " +
                            "This is a known bug with the windowing toolkit. The widths of these table " +
                            "columns will not be saved (will be left as the previous value). If you " +
                            "wish to remove a column, please use the Preferences window rather than " +
                            "the mouse to remove the column. All other changes in your perspective " +
                    "will be saved.");
            WarningUtil.setWidthWarningShown(true);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void init(final Composite parent) {
        this.parent = parent;
        createGui();
    }

    /**
     * Draws the GUI components.
     */
    protected abstract void createGui();

    /**
     * Creates a new table item.
     * @param args column values, in order specified by the view configuration
     * @return new TableItem
     */
    protected TableItem createTableItem(final String[] args) {

        final boolean ascending = table.getSortDirection() == SWT.UP || table.getSortDirection() == SWT.NONE;
        int itemIndex = 0;
        if (ascending) {
            itemIndex = table.getItemCount();
        }
        if (tableDef.isSortAllowed() && sortColumn != null) {
            final int colPos = sortColumn.getConfigurationNumber();

            itemIndex = this.findInsertIndex(args[colPos]);
        }

        TableItem item = null;		 
        synchronized (this.agedItems) {
            item = new TableItem(table, SWT.NONE, itemIndex);
            final AgedTableItem sortItem = new AgedTableItem(item);
            addAgedItem(sortItem);
        }

        final ChillTableColumn[] cols = tableDef.getAvailableColumns();
        if (cols.length != args.length) {
            throw new IllegalArgumentException("Number of arguments does not match number of columns");
        }

        if (sortColumn != null) {
            item.setData(SORT_KEY, args[sortColumn.getConfigurationNumber()]);
        }

        for (final ChillTableColumn col: cols) {
            final int index = col.getConfigurationNumber();
            setColumn(item, index, args[index]);
        }

        return item;
    }

    /**
     * Creates a new empty table item with the given timestamp.
     * @param timestamp column values, in order specified by the view configuration
     * @return new TableItem
     */
    protected TableItem createTableItem(final long timestamp) {
        TableItem item = null;
        synchronized (this.agedItems) {
            item = new TableItem(table, SWT.NONE);
            final AgedTableItem sortItem = new AgedTableItem(item);
            sortItem.setTimestamp(timestamp);
            addAgedItem(sortItem);
        }
        return item;
    }

    /**
     * Removes "limit" oldest entries in the table.
     *  
     * @param limit maximum number of records to remove
     */
    protected void removeOldestEntries(int limit) {
        synchronized (this.agedItems) {
            final TableItem[] items = table.getItems();
            if (limit > items.length) {
                limit = items.length;
            }
            for (int i = 0; i < limit; i++) {
                final AgedTableItem item = this.agedItems.get(0);
                this.agedItems.remove(item);
                item.item.dispose();
            }
        }		
    }

    /**
     * Gets the timestamp associated with a table item.
     * @param item TableItem (row) to get timestamp for
     * @return creation time in milliseconds
     */
    public long getEntryTimestamp(final TableItem item) {
        return (Long)item.getData("timestamp");
    }

    /**
     * Gets the actual table object.
     * @return table object
     */
    public Table getTable() {
        return table;
    }

    /**
     * Performs a binary search of table rows to determine the proper insert
     * index for a new record.
     * 
     * @param value
     *            the text value for the sort column in the new record being
     *            inserted
     * @param items
     *            the current list of table items (rows)
     * @param collator
     *            the sort collator, which is specific to the data type of the
     *            current sort column
     * @param ascending
     *            true if the sort order is ascending, false if descending
     * @return table index at which to insert the new row
     *
     */
    private int sortKeyBinarySearch(final String value, final TableItem[] items,
            final Comparator<String> collator, final boolean ascending) {

        /*
         * This if-block is an optimization.  In 90% of cases, the new record
         * is simply "next" and even minimal binary search is unnecessary.
         */
        if (items.length == 0) {
            /*
             * The table is empty. Insert at index 0.
             */
            return 0;
        } else if (ascending) {
            /*
             * If the sort key of the new record falls after the sort key on the
             * last table row, the new record should be inserted at the end.
             */
            final String currentRowVal = (String) items[items.length - 1]
                    .getData(SORT_KEY);
            final int compare = collator.compare(currentRowVal, value);
            if (compare <= 0) {
                return items.length;
            }

        } else {
            /*
             * If the sort key of the new record falls before the sort key on the
             * first table row, the new record should be inserted at the front.
             */
            final String currentRowVal = (String) items[0].getData(SORT_KEY);
            final int compare = collator.compare(currentRowVal, value);
            if (compare <= 0) {
                return 0;
            }
        }

        /*
         * The optimal case did not work. Perform binary search for the insert index.
         */
        int lowerBound = 0;
        int upperBound = items.length - 1;
        int curIn = 0;
        while (true) {
            curIn = (upperBound + lowerBound) / 2;
            final String currentRowVal = (String) items[curIn].getData(SORT_KEY);
            final int compare = collator.compare(currentRowVal, value);
            if ((ascending && compare <= 0) || (!ascending && compare >= 0)) {
                lowerBound = curIn + 1; // its in the upper
                if (lowerBound > upperBound) {
                    return curIn + 1;
                }
            } else {
                upperBound = curIn - 1; // its in the lower
                if (lowerBound > upperBound) {
                    return curIn;
                }
            }
        }
    }

    /**
     * Replaces the SORT_KEY data member on each table row with the
     * current sort key.
     *
     */
    private void replaceSortKeys() {
        final TableItem[] items = this.table.getItems();

        int tableIndex = -1;

        if (sortColumn != null) {
            final int configIndex = tableDef.getColumnIndex(tableDef.getSortColumn());
            tableIndex = tableDef.getActualIndex(configIndex);
        }
        for (final TableItem item: items) {
            if (sortColumn == null) {
                item.setData(SORT_KEY, null);
            } else {
                final String keyText = item.getText(tableIndex);
                item.setData(SORT_KEY, keyText);
            }
        }
    }

    /**
     * This class is used to store TableItems with their entry timestamp.
     *
     */
    private static class AgedTableItem implements Comparable<AgedTableItem> {
        private final TableItem item;
        private long timestamp;

        /**
         * Constructor
         * @param item TableItem
         */
        public AgedTableItem(final TableItem item) {
            this.item = item;
            this.timestamp = System.currentTimeMillis();
            item.setData("timestamp", timestamp);
        }

        /**
         * Sets the timestamp
         * @param time timestamp for this table item
         */
        public void setTimestamp(final long time) {
            this.timestamp = time;
            item.setData("timestamp", timestamp);
        }

        /**
         * {@inheritDoc}
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(final AgedTableItem o) {
            if (o == null) {
                return -1;
            }
            if (o.timestamp == this.timestamp) {
                return 0;
            }
            if (o.timestamp > this.timestamp) {
                return 1;
            }
            return -1;
        }

        /**
         * {@inheritDoc}
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object o) {
            return (compareTo((AgedTableItem)o) == 0);
        }

        /**
         * {@inheritDoc}
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return (int) this.timestamp;
        }

    }

    /**
     * Inserts a new AgedTableItem into the aged items list in timestamp order.
     * 
     * @param item
     *            aged item to insert
     *
     */
    private void addAgedItem(final AgedTableItem item) {

        /*
         * This is an optimization. In 90% of cases, the new item will have a
         * later timestamp than the last aged item on the list, and just gets
         * added at the end. No search is necessary.
         */
        if (this.agedItems.isEmpty()
                || item.compareTo(this.agedItems.get(this.agedItems.size() - 1)) >= 0) {
            this.agedItems.add(item);
        }

        /*
         * Optimization not possible. Binary search for insert index.
         */
        int lowerBound = 0;
        int upperBound = this.agedItems.size() - 1;
        int curIn = 0;
        while (true) {
            curIn = (upperBound + lowerBound) / 2;
            final AgedTableItem currentRowVal = agedItems.get(curIn);
            final int compare = item.compareTo(currentRowVal);
            if (compare <= 0) {
                lowerBound = curIn + 1; // its in the upper
                if (lowerBound > upperBound) {
                    agedItems.add(curIn + 1, item);
                    return;
                }

            } else {
                upperBound = curIn - 1; // its in the lower
                if (lowerBound > upperBound) {
                    agedItems.add(curIn, item);
                }
            }
        }

    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.support.ICountableView#getRowCount()
     */
    @Override
    public long getRowCount() {
        return this.table.getItemCount();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.support.ICountableView#getMarkedCount()
     */
    @Override
    public long getMarkedCount() {
        return 0;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.support.ICountableView#getSelectedCount()
     */
    @Override
    public long getSelectedCount() {
        return this.table.getSelectionCount();
    }
    

}
