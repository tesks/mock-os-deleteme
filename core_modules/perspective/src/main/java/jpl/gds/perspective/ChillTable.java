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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jpl.gds.perspective.config.ViewProperties;

/**
 * ChillTable provides the definition of a table in a perspective view. As such,
 * it provides the ability to save and restore the configuration of any GUI
 * table.
 * 
 *
 */
public class ChillTable {

    /**
     * XML tag name for the table settings element
     */
    public static final String TABLE_SETTINGS_TAG = "TableSettings";
    
    /**
     * XML tag name for the table name element
     */
    public static final String TABLE_NAME_TAG = "name";
    
    /**
     * XML tag name for the table columns element
     */
    public static final String TABLE_COLUMNS_TAG = "tableColumns";
    
    /**
     * XML tag name for the columns enabled flag
     */
    public static final String ENABLED_COLUMNS_TAG = "enabledColumns";
    
    /**
     * XML tag name for the OLD show column header flag
     * @deprecated
     */
    public static final String OLD_SHOW_HEADER_TAG = "showHeader";

    /**
     * XML tag name for the NEW show column header flag
     * 
     */
    public static final String SHOW_COL_HEADER_TAG = "showColumnHeader";

    /**
     * XML tag name for the show row header flag
     * 
     */
    public static final String SHOW_ROW_HEADER_TAG = "showRowHeader";

    /**
     * XML tag name for the sort column flag
     */
    public static final String SORT_COLUMN_TAG = "sortColumn";
    
    /**
     * XML tag name for the sort direction flag
     */
    public static final String SORT_DIRECTION_TAG = "sortAscending";
    
    /**
     * XML tag name for the columns width attribute
     */
    public static final String COLUMN_WIDTHS_TAG = "columnWidths";
    
    /**
     * XML tag name for the lock sort flag
     */
    public static final String SORT_LOCK_TAG = "allowSort";
    
    /**
     * XML tag name for version element
     */
    public static final String VERSION_TAG = "version";

    private static final String DEFAULT_COLUMN_CONFIG = "defaultColumns";
    private static final String DEFAULT_DEPRECATION_CONFIG = "deprecatedColumns";
    private static final String DEFAULT_WIDTH_CONFIG = "defaultColWidths";
    private static final String DEFAULT_SORT_TYPE_CONFIG = "defaultSortTypes";
    private static final String DEFAULT_SORT_LOCK_CONFIG = "allowSort";
    private static final String DEFAULT_HEADER_CONFIG = "showHeader";
    
    /**
     * Represents the table version
     * 
     */
    public static final int CURRENT_VERSION = 2;

    private int version = CURRENT_VERSION;
    private String name;
    private ArrayList<ChillTableColumn> columns = new ArrayList<ChillTableColumn>();
    /* Now have 2 show header flags, one for row and one for column */
    private boolean showColHeader;
    private boolean showRowHeader;
    private boolean sortAscending = true;
    private boolean allowSort = true;
    private HashMap<String, Integer> columnIndices;
    private final HashMap<Integer, Integer> actualIndices = new HashMap<Integer, Integer>();

    /**
     * Creates an instance of ChillTable.
     */
    public ChillTable() {
    }

    /**
     * Creates an instance of ChillTable with the given column names. Should be
     * used for test purposes only.
     * 
     * @param tableName
     *            the name of the table
     * @param columns
     *            the complete array of column names in the table, regardless of
     *            whether they are currently all displayed in the table
     */
    public ChillTable(final String tableName, final String[] columns) {
        this.name = tableName;
        setAvailableColumns(columns);
    }

    /**
     * Retrieves the number of available columns in the table (all columns, not
     * just those that are displayed currently)
     * 
     * @return the total column count
     */
    public int getColumnCount() {
        return this.columns.size();
    }

    /**
     * Retrieves the number of enabled columns in the table (just those columns
     * that are displayed currently)
     * 
     * @return the current column count
     */
    public int getActualColumnCount() {
        int count = 0;
        for (final ChillTableColumn col : this.columns) {
            if (col.isEnabled() && !col.isDeprecated()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retrieves the actual current index of the column with the given absolute
     * index, where absolute index refers to the available column index, and
     * current index refers to the index among columns that are currently
     * enabled.
     * 
     * @param index the index of the column amongst all other columns, 
     *          regardless of whether or not they're enabled
     * @return actual index is the index of the column amongst the enabled 
     *          columns
     */
    public synchronized int getActualIndex(final int index) {
        final Integer actual = this.actualIndices.get(index);
        if (actual != null) {
            return actual;
        }
        int realIndex = 0;
        for(ChillTableColumn col : this.columns) {
            if (col.getConfigurationNumber() == index) {
                this.actualIndices.put(index, realIndex);
                return realIndex;
            }
            if (col.isEnabled() && !col.isDeprecated()) {
                realIndex++;
            }
        }
        return -1;
    }

    /**
     * Retrieves the current display index of the column with the given absolute
     * index, where absolute index refers to the available column index, and
     * current display index refers to the ordering in which the column is to
     * be displayed.
     *
     * @param index the index of the column amongst all other columns,
     *          regardless of whether or not they're enabled
     * @return display index is the index of the display sorting order
     *          of the displayed columns
     */
    public synchronized int getCurrentPosition(final int index) {
        if(index < 0 || index > (this.columns.size() +1)) {
            return -1;
        }
        return this.columns.get(index).getCurrentPosition();
     }

    /**
     * Gets a list of each position for all enabled columns
     * 
     * @return list of positions within the table of enabled columns
     */
    public synchronized int[] getColumnOrder() {
        final int[] result = new int[getActualColumnCount()];
        final Iterator<ChillTableColumn> it = this.columns.iterator();
        int count = 0;
        while (it.hasNext()) {
            final ChillTableColumn col = it.next();
            if (col.isEnabled() && !col.isDeprecated()) {
                result[count++] = col.getCurrentPosition();
            }
        }
        return result;
    }

    /**
     * Sets a new position in the table for each currently enabled column
     * 
     * @param order list of new column positions
     */
    public synchronized void setColumnOrder(final int[] order) {

        this.actualIndices.clear();
        final Iterator<ChillTableColumn> it = this.columns.iterator();
        int i = 0;
        while (it.hasNext()) {
            final ChillTableColumn col = it.next();
            if (col.isEnabled() && !col.isDeprecated()) {
                col.setCurrentPosition(order[getActualIndex(col
                        .getConfigurationNumber())]);
                i++;
            }
        }
    }

    /**
     * Indicates whether the column with the given absolute index is currently
     * enabled for display.
     * 
     * @param index
     *            the absolute column index (among the available columns)
     * @return true if the column is enabled; false if not
     */
    public boolean isColumnEnabled(final int index) {
        return this.columns.get(index).isEnabled();
    }

    /**
     * Indicates whether the column with the given absolute index is deprecated.
     * 
     * @param index
     *            the absolute column index (among the available columns)
     * @return true if the column is deprecated; false if not
     */
    public boolean isColumnDeprecated(final int index) {
        return this.columns.get(index).isDeprecated();
    }

    /**
     * Gets the sort type for the column with the given absolute index.
     * 
     * @param index
     *            the absolute column index (among the available columns)
     * @return SortType
     */
    public ChillTableColumn.SortType getColumnSortType(final int index) {
        return this.columns.get(index).getSortType();
    }

    /**
     * Marks the column at the given absolute index as deprecated.
     * 
     * @param index
     *            the absolute column index in the available column set
     */
    public void deprecateColumn(final int index) {
        this.columns.get(index).setDeprecated(true);
    }

    /**
     * Enables the display of the column at the given absolute index.
     * 
     * @param index
     *            the absolute column index in the available column set
     * @param val
     *            true to enable display; false to disable
     */
    public synchronized void enableColumn(final int index, final boolean val) {
        this.columns.get(index).setEnabled(val);
        this.actualIndices.clear();
    }

    /**
     * Repositions enabled columns at consecutive indices.
     */
    public synchronized void repositionEnabledColumns() {
        int index = 0;
        for (final ChillTableColumn col : this.columns) {
            if (col.isEnabled() && !col.isDeprecated()) {
                col.setCurrentPosition(index++);
            }
        }
        this.actualIndices.clear();
    }

    /**
     * Enables/disables the display of all defined columns.
     * 
     * @param val
     *            true to enable, false to disable
     */
    public synchronized void enableAllColumns(final boolean val) {
        for (final ChillTableColumn col : this.columns) {
            col.setEnabled(val);
        }
        this.actualIndices.clear();
    }

    /**
     * Gets the configuration name of the column name at the given absolute
     * index.
     * 
     * @param index
     *            the absolute column index in the available column set
     * @return the column name
     */
    public String getOfficialColumnName(final int index) {
        return this.columns.get(index).getOfficialName();
    }

    /**
     * Indicates whether the column at the given absolute index is the current
     * sort column.
     * 
     * @param index
     *            the absolute column index in the available column set
     * @return true if it is the sort column; false if not
     */
    public boolean isSortColumn(final int index) {
        return this.columns.get(index).isSortColumn();
    }

    /**
     * Gets the absolute index of the column with the given configuration name
     * 
     * @param name
     *            the column name to look for
     * @return the absolute column index in the available column set
     */
    public int getColumnIndex(final String name) {
        if (this.columnIndices == null) {
            this.columnIndices = new HashMap<String, Integer>();
            final Iterator<ChillTableColumn> it = this.columns.iterator();
            int index = 0;
            while (it.hasNext()) {
                this.columnIndices.put(it.next().getOfficialName(), index++);
            }
        }
        final Integer returnMe = this.columnIndices.get(name);
        if (returnMe == null) {
            throw new RuntimeException("Column " + name
                    + " not found in table definition");
        }
        return returnMe;
    }

    /**
     * Gets the column with the given configuration name
     * 
     * @param name
     *            the column name to look for
     * @return the TableColumn or null if no match
     */
    public ChillTableColumn getColumn(final String name) {
        try {
            final int index = getColumnIndex(name);
            return this.columns.get(index);
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * Sets the display width of the column at the given absolute index.
     * 
     * @param index
     *            the absolute column index in the available column set
     * @param width
     *            the width in pixels
     */
    public void setColumnWidth(final int index, final int width) {
        this.columns.get(index).setCurrentWidth(width);
    }

    /**
     * Gets the display width of the column at the given absolute index.
     * 
     * @param index
     *            the absolute column index in the available column set
     * @return the width in pixels
     */
    public int getColumnWidth(final int index) {
        return this.columns.get(index).getCurrentWidth();
    }

    /**
     * Gets the name of this table
     * 
     * @return Returns the table name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns an XML representation of the table settings.
     * 
     * @return an XML string
     */
    public String toXml() {
        final StringBuffer result = new StringBuffer();
        result.append("<" + TABLE_SETTINGS_TAG + " " + TABLE_NAME_TAG + "=\""
                + getName() + "\" " + VERSION_TAG + "=\"" + getVersion()
                + "\">\n");
        /* Use new XML tags for header flags */
        result.append("   <" + SHOW_COL_HEADER_TAG + ">" + isShowColumnHeader() + "</"
                + SHOW_COL_HEADER_TAG + ">\n");
        result.append("   <" + SHOW_ROW_HEADER_TAG + ">" + isShowRowHeader() + "</"
                + SHOW_ROW_HEADER_TAG + ">\n");
        result.append("   <" + SORT_LOCK_TAG + ">" + isSortAllowed() + "</"
                + SORT_LOCK_TAG + ">\n");
        result.append("   <" + SORT_DIRECTION_TAG + ">" + isSortAscending()
                + "</" + SORT_DIRECTION_TAG + ">\n");
        final Iterator<ChillTableColumn> it = this.columns.iterator();
        while (it.hasNext()) {
            if (it.hasNext()) {
                result.append("   " + it.next().toXml() + "\n");
            }
        }
        result.append("</" + TABLE_SETTINGS_TAG + ">\n");
        return result.toString();
    }

    /**
     * Sets the name
     * 
     * @param name
     *            The name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the official name of the column that is currently set as the 
     * column to be sorted on
     * 
     * @return Returns the sortColumn.
     */
    public String getSortColumn() {
        final Iterator<ChillTableColumn> it = this.columns.iterator();
        while (it.hasNext()) {
            final ChillTableColumn col = it.next();
            if (col.isSortColumn()) {
                return col.getOfficialName();
            }
        }
        return null;
    }

    /**
     * Sets the sortColumn
     * 
     * @param sortColumn
     *            The sortColumn to set.
     */
    public void setSortColumn(final String sortColumn) {
        final Iterator<ChillTableColumn> it = this.columns.iterator();
        while (it.hasNext()) {
            final ChillTableColumn col = it.next();
            if (col.isSortColumn()) {
                col.setSortColumn(false);
            }
            if (col.getOfficialName().equalsIgnoreCase(sortColumn)) {
                col.setSortColumn(true);
            }
        }
    }

    /**
     * Indicates if table column header should be displayed.
     * 
     * @return true if table column header should be displayed
     * 
     */
    public boolean isShowColumnHeader() {
        return this.showColHeader;
    }

    /**
     * Sets the flag indicating if table column header should be shown.
     * 
     * @param showHeader
     *            true to show table column header
     *            
     */
    public void setShowColumnHeader(final boolean showHeader) {
        this.showColHeader = showHeader;
    }


    /**
     * Indicates if table row header should be displayed.
     * 
     * @return true if table row header should be displayed
     * 
     */
    public boolean isShowRowHeader() {
        return this.showRowHeader;
    }

    /**
     * Sets the flag indicating if table row header should be shown.
     * 
     * @param showHeader
     *            true to show table row header
     *            
     */
    public void setShowRowHeader(final boolean showHeader) {
        this.showRowHeader = showHeader;
    }


    /**
     * Indicates if table sorting should be allowed.
     * 
     * @return true if table can be sorted.
     */
    public boolean isSortAllowed() {
        return this.allowSort;
    }

    /**
     * Sets the flag indicating if table can be sorted
     * 
     * @param allow
     *            true to enable sorting
     */
    public void setSortAllowed(final boolean allow) {
        this.allowSort = allow;
    }

    /**
     * Gets the list of all available columns in this table.
     * 
     * @return an array of column names
     */
    public ChillTableColumn[] getAvailableColumns() {
        final ChillTableColumn[] result = new ChillTableColumn[getColumnCount()];
        return this.columns.toArray(result);
    }

    /**
     * Sets the list of available columns names in this table. Automatically
     * sets all columns to be enabled and assigns then a default width.
     * 
     * @param columns
     *            the array of column names to set
     */
    private void setAvailableColumns(final String[] columns) {
        for (int i = 0; i < columns.length; i++) {
            final ChillTableColumn col = new ChillTableColumn();
            col.setConfigurationNumber(i);
            col.setCurrentPosition(i);
            col.setOfficialName(columns[i]);
            col.setDisplayName(columns[i]);
            col.setEnabled(true);
            col.setCurrentWidth(col.getDefaultWidth());
            this.columns.add(col);
        }
    }

    /**
     * Sets new list of available table columns. If there are existing column 
     * names, this list will be merged in with existing.
     * 
     * @param cols list of new columns.
     */
    @SuppressWarnings("unchecked")
    public void setAvailableColumns(final ArrayList<ChillTableColumn> cols) {
        boolean allNew = false;
        if (this.columns.size() == 0) {
            allNew = true;
        }
        if (allNew) {
            // This is a whole new set of columns
            this.columns = (ArrayList<ChillTableColumn>) cols.clone();
        } else {
            // This is a set read from the perspective file, should be merged
            // with existing definition from defaults
            // First disable all the old columns
            for (int i = 0; i < this.columns.size(); i++) {
                this.columns.get(i).setEnabled(false);
            }
            final Iterator<ChillTableColumn> newColIt = cols.iterator();
            while (newColIt.hasNext()) {
                final ChillTableColumn newColumn = newColIt.next();
                for (int i = 0; i < this.columns.size(); i++) {
                    final ChillTableColumn oldColumn = this.columns.get(i);
                    if (oldColumn.getOfficialName().equalsIgnoreCase(
                            newColumn.getOfficialName())) {
                        // Column from perspective file overrides default column
                        // definition.
                        newColumn.setConfigurationNumber(oldColumn
                                .getConfigurationNumber());
                        // Configuration number is still set from the default.
                        this.columns.set(i, newColumn);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Copies this table object.
     * 
     * @return a new ChillTable object
     */
    public ChillTable copy() {
        final ChillTable newbie = new ChillTable();
        newbie.setName(this.getName());
        final Iterator<ChillTableColumn> cols = this.columns.iterator();
        final ArrayList<ChillTableColumn> newCols = new ArrayList<ChillTableColumn>();

        while (cols.hasNext()) {
            final ChillTableColumn newCol = cols.next().copy();
            newCols.add(newCol);
        }
        newbie.setAvailableColumns(newCols);
        /*  Copy both header flags instead of just one. */
        newbie.setShowColumnHeader(this.isShowColumnHeader());
        newbie.setShowRowHeader(this.isShowRowHeader());
        newbie.setSortColumn(this.getSortColumn());
        newbie.setSortAscending(this.isSortAscending());
        newbie.setSortAllowed(this.isSortAllowed());
        return newbie;

    }

    /**
     * Gets the flag that determines the direction of the sort
     * 
     * @return Returns the sort ascending flag. True if ascending, false if 
     * descending.
     */
    public boolean isSortAscending() {
        return this.sortAscending;
    }

    /**
     * Sets the sort ascending flag
     * 
     * @param sortAscending
     *            true to select ascending sort order; false for descending
     */
    public void setSortAscending(final boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

    /**
     * Retrieves the version.
     * 
     * @return the version
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * Sets the version.
     * 
     * @param version
     *            the version to set
     */
    public void setVersion(final int version) {
        this.version = version;
    }

    /**
     * Creates a new chill table with the given columns
     * 
     * @param name title of the table
     * @param propertyBlockName XML property block name
     * @param columns columns that will be added to this table
     * @return new table created with specified properties and columns
     */
    public static ChillTable createTable(final String name,
            final ViewProperties props, final String[] columns) {
         
        final ChillTable table = new ChillTable(); 
        table.setName(name);
        
        final String enableStr = props.getStringDefault(DEFAULT_COLUMN_CONFIG);
        final String[] enabledCols = enableStr.split(",");
        final String deprecateStr = props.getStringDefault(DEFAULT_DEPRECATION_CONFIG);
        String[] deprecatedCols = null;
        if (deprecateStr != null) {
            deprecatedCols = deprecateStr.split(",");
        }
//        /*  Initialize both header flags */
//        final boolean showHeader = config.getBooleanProperty(propertyBlock
//                + DEFAULT_HEADER_CONFIG, true);
//        final boolean showRowHeader = config.getBooleanProperty(propertyBlock
//                + DEFAULT_HEADER_CONFIG, true);
        table.setShowColumnHeader(true);
        table.setShowRowHeader(false);
        final String widthStr = props.getStringDefault(DEFAULT_WIDTH_CONFIG);
        int[] widths = null;
        if (widthStr != null) {
            final String[] widthStrings = widthStr.split(",");
            widths = new int[widthStrings.length];
            for (int i = 0; i < widthStrings.length; i++) {
                /* _ MPCS_7304 - 5/21/15. Added trim() */
                widths[i] = Integer.parseInt(widthStrings[i].trim());
            }
        }

        final boolean sortLock = props.getBooleanDefault(DEFAULT_SORT_LOCK_CONFIG, true);
        table.setSortAllowed(sortLock);

        final String sortTypeStr = props.getStringDefault(DEFAULT_SORT_TYPE_CONFIG);
        ChillTableColumn.SortType sortTypes[] = null;
        if (sortTypeStr != null) {
            final String[] sortStrings = sortTypeStr.split(",");
            sortTypes = new ChillTableColumn.SortType[sortStrings.length];
            for (int i = 0; i < sortStrings.length; i++) {
                sortTypes[i] = Enum.valueOf(ChillTableColumn.SortType.class,
                        sortStrings[i].trim());
            }
        }

        final ArrayList<ChillTableColumn> defColumns = new ArrayList<ChillTableColumn>();
        int usedIndex = 0;
        for (int i = 0; i < columns.length; i++) {
            final ChillTableColumn col = new ChillTableColumn();
            col.setConfigurationNumber(i);
            if (widths != null) {
                col.setCurrentWidth(widths[i]);
                col.setDefaultWidth(widths[i]);
            }
            if (sortTypes != null) {
                col.setSortType(sortTypes[i]);
            }
            col.setDisplayName(columns[i]);
            col.setOfficialName(columns[i]);
            col.setEnabled(arrayContainsString(columns[i], enabledCols));
            col.setDeprecated(arrayContainsString(columns[i], deprecatedCols));
            if (col.isEnabled() && !col.isDeprecated()) {
                col.setCurrentPosition(usedIndex++);
            } else {
                col.setCurrentPosition(ChillTableColumn.DISABLED_INDEX);
            }
            defColumns.add(col);
        }

        table.setAvailableColumns(defColumns);
        return table;
    }

    private static boolean arrayContainsString(final String text,
            final String[] list) {
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.length; i++) {
            if (list[i].equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deprecates columns that are in the comma-separated list of deprecated 
     * columns in the config file
     * 
     * @param propertyBlockName XML property block name for which we want to 
     *          deprecate columns
     * @param columns list of columns to be deprecated
     */
    public void deprecateColumns(final ViewProperties props,
            final String[] columns) {
        final String deprecateStr = props.getStringDefault(DEFAULT_DEPRECATION_CONFIG);
        String[] deprecatedCols = null;
        if (deprecateStr != null) {
            deprecatedCols = deprecateStr.split(",");
        } else {
            return;
        }
        int usedIndex = 0;
        for (int i = 0; i < columns.length; i++) {
            final ChillTableColumn col = this.getColumn(columns[i]);
            col.setDeprecated(arrayContainsString(columns[i], deprecatedCols));
            if (col.isEnabled() && !col.isDeprecated()) {
                col.setCurrentPosition(usedIndex++);
            } else {
                col.setCurrentPosition(ChillTableColumn.DISABLED_INDEX);
            }
        }
    }
}
