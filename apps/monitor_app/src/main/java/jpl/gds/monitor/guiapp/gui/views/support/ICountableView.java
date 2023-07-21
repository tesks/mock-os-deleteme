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
package jpl.gds.monitor.guiapp.gui.views.support;

/**
 * This interface is to be implemented by table view composites that want to
 * display a CountShell.
 */
public interface ICountableView {

    /**
     * Retrieves the current total row count of the table.
     * @return row count
     */
    public long getRowCount();

    /**
     * Retrieves the count of marked rows in the table.
     * @return marked row count
     */
    public long getMarkedCount();

    /**
     * Retrieves the count of selected rows in the table.
     * @return selected row count
     */
    public long getSelectedCount();
}
