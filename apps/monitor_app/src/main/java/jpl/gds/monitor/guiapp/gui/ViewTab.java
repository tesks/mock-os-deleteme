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
package jpl.gds.monitor.guiapp.gui;

import org.eclipse.swt.custom.CTabItem;

import jpl.gds.perspective.view.View;

/**
 * 
 * ViewTab is implemented by TabItem classes that are to be listed in the
 * view menu of the message monitor.
 */
public interface ViewTab extends View {
        
    /**
     * Gets the TabItem widget.
     * @return the TabItem used in the tab pane.
     */
    public CTabItem getTabItem();
}
