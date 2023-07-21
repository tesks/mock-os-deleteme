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
package jpl.gds.perspective.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * View is an interface to be implemented by all view classes used in a
 * perspective. Views must also implement a constructor that takes a
 * ViewConfiguration object as an argument, and a no-argument constructor. The
 * no-argument constructor should construct a default ViewConfiguration. Once
 * constructed, a View's ViewConfiguration object should not be cloned or
 * re-created, as the perspective will have a reference to it.
 * 
 */
public interface View {
    /**
     * Gets the default name for the view.
     * 
     * @return the name text
     */
    public String getDefaultName();

    /**
     * Instructs the View to update its ViewConfiguration object.
     */
    public void updateViewConfig();

    /**
     * Provides the View with its parent Widget and instructs the View to
     * initialize itself and its graphical components.
     * 
     * @param parent
     *            the parent Composite widget
     */
    public void init(Composite parent);

    /**
     * Retrieves the View's current ViewConfiguration object.
     * 
     * @return view configuration object
     */
    public IViewConfiguration getViewConfig();

    /**
     * Return the view's main control. In ViewTabs this should return the View's
     * main control, not the tab item.
     * 
     * @return the view's main control
     */
    public Control getMainControl();

    /**
     * Clear the view's contents.
     */
    public void clearView();
}
