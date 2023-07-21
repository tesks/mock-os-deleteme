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
package jpl.gds.monitor.perspective.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.IViewConfigurationContainer;
import jpl.gds.perspective.view.ViewConfiguration;
import jpl.gds.perspective.view.ViewTriple;

/**
 * CustomGridViewConfiguration encapsulates the view configuration for the
 * Custom Grid View in the monitor. Custom Grid View contains a set of child
 * views organized as a table of rows and columns.
 * 
 */
public class CustomGridViewConfiguration extends ViewConfiguration implements
        IViewConfigurationContainer {

    private static final String GRID_ROWS_CONFIG = "gridRows";
    private static final String GRID_COLS_CONFIG = "gridColumns";
    private static final String ROW_WEIGHT_CONFIG = "rowWeight";
    private static final String ROW_COLUMN_WEIGHT_CONFIG = "rowColumnWeights";
    private static final String COL_WEIGHT_CONFIG = "colWeight";
    private static final String COLUMN_ROW_WEIGHT_CONFIG = "columnRowWeights";
    private static final String GRID_ORIENTATION_CONFIG = "gridOrientation";
    private static final int DEFAULT_ROWS = 3;
    private static final int DEFAULT_COLS = 3;

    private List<ViewTriple> viewCoordinates;
    private List<IViewConfiguration> childViews;

    /**
     * Creates an instance of CustomGridViewConfiguration.
     * @param appContext the current application context
     */
    public CustomGridViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }

    /**
     * Sets the display weight for the dominant dimension. The dominant display
     * weight controls how much of the total view's vertical space is alloted to
     * the row in ROW_DOMINANT orientation, or how much of the total view's
     * horizontal space is alloted to the column in the COLUMN_DOMINANT
     * orientation.
     * 
     * @param index
     *            the index of the table row or column to set weight for
     * @param weight
     *            the weight; must be between 0 and 100
     */
    public void setDominantDimensionWeight(final int index, final int weight) {
        if (weight < 0 || weight > 100) {
            throw new IllegalArgumentException(
                    "Weight must be between 0 and 100");
        }
        if (getGridOrientation() == GridOrientationType.ROW_DOMINANT) {
            if (index >= getGridRows()) {
                throw new IllegalArgumentException("Row index is too big");
            }
            this.setConfigItem(ROW_WEIGHT_CONFIG + index, String
                    .valueOf(weight));
        } else {
            if (index >= getGridColumns()) {
                throw new IllegalArgumentException("Column index is too big");
            }
            this.setConfigItem(COL_WEIGHT_CONFIG + index, String
                    .valueOf(weight));
        }
    }

    /**
     * Gets the display weight for the given row (if orientation if
     * ROW_DOMINANT) or column (if orientation is COLUMN_DOMINANT). The display
     * weight controls how much of the total view's vertical space is alloted to
     * the row (if the view is ROW_DOMINANT) or how much of the total view's
     * horizontal space is alloted to the column (if the view is
     * COLUMN_DOMINANT).
     * 
     * @param index
     *            the index of the table row or column to get weight for
     * @return a weight between 0 and 100
     */
    public int getDominantDimensionWeight(final int index) {
        String str = null;
        if (getGridOrientation() == GridOrientationType.ROW_DOMINANT) {
            if (index >= getGridRows()) {
                throw new IllegalArgumentException("Row index is too big");
            }
            str = this.getConfigItem(ROW_WEIGHT_CONFIG + index);
            if (str == null) {
                throw new RuntimeException("No weight for row " + index);
            }
        } else {
            if (index >= getGridColumns()) {
                throw new IllegalArgumentException("Column index is too big");
            }
            str = this.getConfigItem(COL_WEIGHT_CONFIG + index);
            if (str == null) {
                throw new RuntimeException("No weight for column " + index);
            }
        }
        return Integer.parseInt(str);
    }

    /**
     * Gets the display weight for all rows (if orientation is ROW_DOMINANT) or
     * columns (if orientation is COLUMN_DOMINANT). The display weight controls
     * how much of the total view's vertical or horizontal space is alloted to
     * the row or column.
     * 
     * @return an array of weights between 0 and 100
     */
    public int[] getDominantDimensionWeights() {
        int weights[] = null;
        if (getGridOrientation() == GridOrientationType.ROW_DOMINANT) {
            weights = new int[this.getGridRows()];
            for (int i = 0; i < weights.length; i++) {
                final String str = this.getConfigItem(ROW_WEIGHT_CONFIG + i);
                if (str == null) {
                    throw new RuntimeException("No weight for row " + i);
                }
                weights[i] = Integer.parseInt(str);
            }
        } else {
            weights = new int[this.getGridColumns()];
            for (int i = 0; i < weights.length; i++) {
                final String str = this.getConfigItem(COL_WEIGHT_CONFIG + i);
                if (str == null) {
                    throw new RuntimeException("No weight for column " + i);
                }
                weights[i] = Integer.parseInt(str);
            }
        }
        return weights;
    }

    /**
     * Sets the display weights for all the columns within the given row (if
     * orientation is ROW_DOMINANT) or for all the rows within the give column
     * (if orientation is COLUMN_DOMINANT).
     * 
     * @param index
     *            the index of the table row or column to set weights for
     * @param weights
     *            an array of integer weights, one per child view defined in the
     *            row or column
     */
    public synchronized void setVariableWeights(final int index,
            final int[] weights) {
        final StringBuffer weightsBuf = new StringBuffer();
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] < 0 || weights[i] > 100) {
                throw new IllegalArgumentException(
                        "Eeight must be between 0 and 100");
            }
            weightsBuf.append(weights[i]);
            if (i < weights.length - 1) {
                weightsBuf.append(",");
            }
        }
        if (getGridOrientation() == GridOrientationType.ROW_DOMINANT) {
            if (index >= getGridRows()) {
                throw new IllegalArgumentException("Row index is too big");
            }
            if (weights.length > getGridColumns()) {
                throw new IllegalArgumentException("Too many column weights");
            }
            this.setConfigItem(ROW_COLUMN_WEIGHT_CONFIG + index, weightsBuf
                    .toString());
        } else {
            if (index >= getGridColumns()) {
                throw new IllegalArgumentException("Column index is too big");
            }
            if (weights.length > getGridRows()) {
                throw new IllegalArgumentException("Too many row weights");
            }
            this.setConfigItem(COLUMN_ROW_WEIGHT_CONFIG + index, weightsBuf
                    .toString());
        }
    }

    /**
     * Gets the display weights for all the columns within the given row (if
     * orientation is ROW_DOMINANT) or for all the rows within the given column
     * (if orientation is COLUMN_DOMINANT).
     * 
     * @param index
     *            the index of the table row or column to get weights for
     * @return an array of integer weights, one per child view defined in the
     *         row or column
     */
    public synchronized int[] getVariableWeights(final int index) {
        String[] weightPieces = null;
        if (getGridOrientation() == GridOrientationType.ROW_DOMINANT) {
            if (index >= getGridRows()) {
                throw new IllegalArgumentException("Row index is too big");
            }
            final String weights = this.getConfigItem(ROW_COLUMN_WEIGHT_CONFIG
                    + index);
            if (weights == null) {
                throw new RuntimeException("No weights defined for grid row "
                        + index);
            }
            if (weights.equals("")) {
                return new int[0];
            }
            weightPieces = weights.split(",");
            if (weightPieces.length > getGridColumns()) {
                throw new RuntimeException("Too many column weights");
            }
        } else {
            if (index >= getGridColumns()) {
                throw new IllegalArgumentException("Column index is too big");
            }
            final String weights = this.getConfigItem(COLUMN_ROW_WEIGHT_CONFIG
                    + index);
            if (weights == null) {
                throw new RuntimeException(
                        "No weights defined for grid column " + index);
            }
            if (weights.equals("")) {
                return new int[0];
            }
            weightPieces = weights.split(",");
            if (weightPieces.length > getGridRows()) {
                throw new RuntimeException("Too many row weights");
            }
        }

        final int[] result = new int[weightPieces.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Integer.parseInt(weightPieces[i]);
        }
        return result;
    }

    /**
     * Sets the coordinates of child views. A set of ViewTriple objects defines
     * the type, row, column, and number of each child view. ViewTriples must be
     * numbered starting with 0, and should be in the same order as the child
     * view configurations set in this object.
     * 
     * @param viewsToSet
     *            a list of ViewTriple objects, one per child view
     */
    public synchronized void setViewCoordinates(
            final List<ViewTriple> viewsToSet) {
        this.viewCoordinates = getViewCoordinates();
        if (this.viewCoordinates != null) {
            final Iterator<ViewTriple> it = this.viewCoordinates.iterator();
            while (it.hasNext()) {
                final ViewTriple triple = it.next();
                final String tripleName = ViewTriple.VIEW_TRIPLE_CONFIG
                        + triple.getNumber();
                this.removeConfigItem(tripleName);
            }
        }
        this.viewCoordinates = viewsToSet;
        if (this.viewCoordinates != null) {
            final Iterator<ViewTriple> it = this.viewCoordinates.iterator();
            while (it.hasNext()) {
                final ViewTriple triple = it.next();
                final String tripleStr = triple.getViewType()
                        .getValueAsString()
                        + "," + triple.getXLoc() + "," + triple.getYLoc();
                final String tripleName = ViewTriple.VIEW_TRIPLE_CONFIG
                        + triple.getNumber();
                this.setConfigItem(tripleName, tripleStr);
            }
        }
    }

    private void removeViewTriple(final int index) {
        getViewCoordinates();
        final Iterator<ViewTriple> it = this.viewCoordinates.iterator();
        int num = 0;
        while (it.hasNext()) {
            final ViewTriple triple = it.next();
            if (triple.getNumber() == index) {
                final String tripleName = ViewTriple.VIEW_TRIPLE_CONFIG
                        + triple.getNumber();
                this.removeConfigItem(tripleName);
            } else if (triple.getNumber() > index) {
                triple.setNumber(triple.getNumber() - 1);
                final String tripleStr = triple.getViewType()
                        .getValueAsString()
                        + "," + triple.getXLoc() + "," + triple.getYLoc();
                final String tripleName = ViewTriple.VIEW_TRIPLE_CONFIG
                        + triple.getNumber();
                this.setConfigItem(tripleName, tripleStr);
            }
            num++;
        }
        if (num > 0) {
            final String tripleName = ViewTriple.VIEW_TRIPLE_CONFIG
                    + String.valueOf(num - 1);
            this.removeConfigItem(tripleName);
        }
        this.viewCoordinates.remove(index);
        getViewCoordinates();
    }

    /**
     * Gets the coordinates of child views. A set of ViewTriple objects defines
     * the type, row, column, and number of each child view. ViewTriples must be
     * numbered starting with 0, and should be in the same order as the child
     * view configurations set in this object.
     * 
     * @return a List of ViewTriples, one per child view
     */
    public synchronized List<ViewTriple> getViewCoordinates() {
        int tripleNum = 0;
        this.viewCoordinates = new ArrayList<ViewTriple>();
        String tripleStr = this.getConfigItem(ViewTriple.VIEW_TRIPLE_CONFIG
                + tripleNum);
        while (tripleStr != null) {
            final String[] pieces = tripleStr.split(",");
            if (pieces[0].equals("Alarm")) {
            	pieces[0] = ViewType.FAST_ALARM.getValueAsString();
            }
            final ViewType type = new ViewType(pieces[0]);
            final int xLoc = Integer.parseInt(pieces[1]);
            final int yLoc = Integer.parseInt(pieces[2]);
            final ViewTriple triple = new ViewTriple(type, xLoc, yLoc,
                    tripleNum);
            this.viewCoordinates.add(triple);
            tripleNum++;
            tripleStr = this.getConfigItem(ViewTriple.VIEW_TRIPLE_CONFIG
                    + tripleNum);
        }
        return this.viewCoordinates;
    }

    /**
     * Retrieves the ViewConfiguration for the view at the given row/column
     * coordinate.
     * 
     * @param row
     *            the row (y) coordinate of the view
     * @param col
     *            the column (x) coordinate of the view
     * @return the ViewConfiguration for the View at the given coordinate, or
     *         null if none found
     */
    public IViewConfiguration getViewConfigAt(final int row, final int col) {
        getViewCoordinates();
        if (this.childViews == null || this.viewCoordinates == null) {
            return null;
        }
        if (this.childViews.size() != this.viewCoordinates.size()) {
            throw new RuntimeException("Number of defined view coordinates "
                    + this.viewCoordinates.size()
                    + " does not equal number of views "
                    + this.childViews.size());
        }
        final Iterator<ViewTriple> it = this.viewCoordinates.iterator();
        int index = 0;
        while (it.hasNext()) {
            final ViewTriple vt = it.next();
            if (vt.getXLoc() == col && vt.getYLoc() == row) {
                return this.childViews.get(index);
            }
            index++;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.IViewConfigurationContainer#getViews()
     */
    @Override
	public List<IViewConfiguration> getViews() {
        return this.childViews;
    }

    /**
     * {@inheritDoc}
     * @see
     * jpl.gds.perspective.view.IViewConfigurationContainer#setViews(java
     * .util.List)
     */
    @Override
	public void setViews(final List<IViewConfiguration> vcList) {
        this.childViews = vcList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public synchronized void addViewConfiguration(final IViewConfiguration vc) {
        if (this.childViews == null) {
            this.childViews = new ArrayList<IViewConfiguration>();
        }

        this.childViews.add(vc);

    }

    /**
     * {@inheritDoc}
     */
    @Override
	public synchronized void removeViewConfiguration(final IViewConfiguration vc) {
        if (this.childViews == null) {
            return;
        }
        final int index = this.childViews.indexOf(vc);
        if (index != -1) {
            this.childViews.remove(vc);
            final ViewTriple vt = getViewCoordinates().get(index);
            removeViewTriple(index);
            final int row = vt.getYLoc();
            final int col = vt.getXLoc();
            final GridOrientationType orient = getGridOrientation();
            if (orient == GridOrientationType.ROW_DOMINANT) {
                final int[] colWeights = this.getVariableWeights(row);
                final int newColCount = colWeights.length - 1;
                final int[] newColWeights = new int[newColCount];
                if (newColCount == 0) {
                    this.setVariableWeights(row, newColWeights);
                    return;
                }
                final int addColWeight = colWeights[col] / newColCount;
                int j = 0;
                for (int i = 0; i < newColCount; i++) {
                    if (j == col) {
                        continue;
                    }
                    newColWeights[i] = colWeights[j++] + addColWeight;
                }
                this.setVariableWeights(row, newColWeights);
            } else {
                final int[] rowWeights = this.getVariableWeights(col);
                final int newRowCount = rowWeights.length - 1;
                final int[] newRowWeights = new int[newRowCount];
                if (newRowCount == 0) {
                    this.setVariableWeights(col, newRowWeights);
                    return;
                }
                final int addRowWeight = rowWeights[row] / newRowCount;
                int j = 0;
                for (int i = 0; i < newRowCount; i++) {
                    if (j == row) {
                        continue;
                    }
                    newRowWeights[i] = rowWeights[j++] + addRowWeight;
                }
                this.setVariableWeights(col, newRowWeights);
            }
        }
    }

    /**
     * Gets the grid orientation.
     * 
     * @return GridOrientationType
     */
    public GridOrientationType getGridOrientation() {
        final String str = this.getConfigItem(GRID_ORIENTATION_CONFIG);
        if (str == null) {
            this.setGridOrientation(GridOrientationType.ROW_DOMINANT);
            return GridOrientationType.ROW_DOMINANT;
        }
        return Enum.valueOf(GridOrientationType.class, str);
    }

    /**
     * Sets the grid orientation.
     * 
     * @param orient
     *            the GridOrientationType to set
     */
    public void setGridOrientation(final GridOrientationType orient) {
        this.setConfigItem(GRID_ORIENTATION_CONFIG, orient.toString());
    }

    /**
     * Gets the number of rows in the view grid.
     * 
     * @return the number of rows
     */
    public int getGridRows() {
        final String str = this.getConfigItem(GRID_ROWS_CONFIG);
        if (str == null) {
            return DEFAULT_ROWS;
        }
        return Integer.parseInt(str);
    }

    /**
     * Sets the number of rows in the view grid.
     * 
     * @param rows
     *            the number of rows to set
     */
    public void setGridRows(final int rows) {
        this.setConfigItem(GRID_ROWS_CONFIG, String.valueOf(rows));
    }

    /**
     * Sets the number of columns in the view grid.
     * 
     * @param cols
     *            the number of columns to set
     */
    public void setGridColumns(final int cols) {
        this.setConfigItem(GRID_COLS_CONFIG, String.valueOf(cols));
    }

    /**
     * Gets the number of columns in the view grid.
     * 
     * @return the number of columns
     */
    public int getGridColumns() {
        final String str = this.getConfigItem(GRID_COLS_CONFIG);
        if (str == null) {
            return DEFAULT_COLS;
        }
        return Integer.parseInt(str);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public String toXML() {
        final StringBuffer result = new StringBuffer();
        if (this.isReference()) {
            result.append(this.reference.toXml());
        } else {
            result.append("<" + IViewConfigurationContainer.VIEW_CONTAINER_TAG
                    + " " + IViewConfiguration.VIEW_NAME_TAG + "=\""
                    + StringEscapeUtils.escapeXml(this.viewName) + "\" ");
            result.append(IViewConfiguration.VIEW_TYPE_TAG + "=\""
                    + this.viewType.getValueAsString() + "\" ");
            result.append(VIEW_VERSION_TAG + "=\"" + WRITE_VERSION + "\">\n");
            result.append(getAttributeXML());
            result.append(getConfigItemXML());

            if (this.childViews != null) {
                result.append("<" + IViewConfigurationContainer.CHILD_VIEWS_TAG
                        + ">\n");
                final Iterator<IViewConfiguration> it = this.childViews
                        .iterator();
                while (it.hasNext()) {
                    final IViewConfiguration vc = it.next();
                    result.append(vc.toXML());
                }
                result.append("</" + IViewConfigurationContainer.CHILD_VIEWS_TAG
                        + ">\n");
            }

            result.append("</" + IViewConfigurationContainer.VIEW_CONTAINER_TAG
                    + ">\n");
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults() {
        super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.CUSTOM_GRID),
                "jpl.gds.monitor.guiapp.gui.views.CustomGridComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.CustomGridTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.CustomGridPreferencesShell");

        final String orient = viewProperties.getStringDefault(GRID_ORIENTATION_CONFIG, GridOrientationType.ROW_DOMINANT.toString());
        setGridOrientation(Enum.valueOf(GridOrientationType.class, orient));
        final int gRows = viewProperties.getIntegerDefault(GRID_ROWS_CONFIG, DEFAULT_ROWS);
        setGridRows(gRows);
        final int gCols = viewProperties.getIntegerDefault(GRID_COLS_CONFIG, DEFAULT_COLS);
        setGridColumns(gCols);
        final int rowWeight = 100 / gRows;
        final int colWeight = 100 / gCols;
        final int[] colWeights = new int[gCols];
        for (int j = 0; j < gCols; j++) {
            colWeights[j] = colWeight;
        }
        for (int i = 0; i < gRows; i++) {
            setDominantDimensionWeight(i, rowWeight);
            setVariableWeights(i, colWeights);
        }
    }

    /**
     * {@inheritDoc}
     * @see
     * jpl.gds.perspective.view.IViewConfigurationContainer#isImportViews()
     */
    @Override
	public boolean isImportViews() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see
     * jpl.gds.perspective.view.IViewConfigurationContainer#isRemovable()
     */
    @Override
	public boolean isRemovable() {
        return true;
    }

    /**
     * {@inheritDoc}
     * @see
     * jpl.gds.perspective.view.IViewConfigurationContainer#isWindowContainer
     * ()
     */
    @Override
	public boolean isWindowContainer() {
        return false;
    }
}
