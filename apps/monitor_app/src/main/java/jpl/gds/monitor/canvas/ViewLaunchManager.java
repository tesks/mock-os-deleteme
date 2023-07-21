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
package jpl.gds.monitor.canvas;

import org.eclipse.swt.widgets.Shell;

/**
 * An interface to be implemented by View Launchers.
 */
public interface ViewLaunchManager {

    /**
     * Launches a view.
     * 
     * @param viewNameOrPath a standalone view name (no XML extension), or
     *        absolute path to the view XML definition
     * @param parent the parent GUI shell for the new view
     */
    public void loadView(String viewNameOrPath, Shell parent);
}
