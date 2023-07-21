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

import org.eclipse.swt.widgets.Shell;

import jpl.gds.perspective.gui.PerspectiveListener;

/**
 * MonitorPerspectiveListener is the interface by which different applications in a
 * perspective receive notification of perspective-related requests, extended for
 * specific use by the monitor.
 * 
 */
public interface MonitorPerspectiveListener extends PerspectiveListener {
  
    /**
     * Called when the user elects to edit the current application
     * configuration.
     * 
     * @param parent
     *            the parent Shell (needed for dialog display)
     * @return true if the perspective was changed and should be restarted
     */
    public boolean editCalled(Shell parent);

    /**
     * Called when the user elects to load a new application configuration.
     * 
     * @param parent
     *            the parent Shell (needed for dialog display)
     * @return true if the perspective was changed and should be restarted
     */
    public boolean loadCalled(Shell parent);

    /**
     * Called when the user elects to load a new application configuration.
     * 
     * @param parent
     *            the parent Shell (needed for dialog display)
     * @param dir
     *            the pre-selected perspective directory
     * @return true if the perspective was changed and should be restarted
     */
    public boolean loadCalled(Shell parent, String dir);

    /**
     * Called when the user elects to merge a new application configuration into
     * the existing one.
     * 
     * @param parent
     *            the parent Shell (needed for dialog display)
     * @return true if the perspective was merged and should be restarted
     */
    public boolean mergeCalled(Shell parent);

    /**
     * Called when the user elects to merge a new application configuration into
     * the existing one.
     * 
     * @param parent
     *            the parent Shell (needed for dialog display)
     * @param dir
     *            the pre-established perspective directory to merge in
     * @return true if the perspective was merged and should be restarted
     */
    public boolean mergeCalled(Shell parent, String dir);


}
