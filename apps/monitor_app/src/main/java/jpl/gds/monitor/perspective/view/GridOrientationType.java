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
/**
 * File: GridOrientationType.java
 */
package jpl.gds.monitor.perspective.view;


/**
 * GridOrientationType is used to represent which dimension in a grid view is dominant.
 * In a ROW_DOMINANT grid, when the row boundary is moved, it moves in the same way for
 * all columns, i.e., all views in the grid row have the same height. In a COLUMN_DOMINANT
 * grid, when the column boundary is moved, it moves in the same way for all rows, i.e.,
 * all views in the grid column have the same width.
 *
 */
public enum GridOrientationType {
     
    /**
     * Row height is maintained when resizing
     */
    ROW_DOMINANT,
     
    /**
     * Column width is maintained when resizing
     */
    COLUMN_DOMINANT;
}
