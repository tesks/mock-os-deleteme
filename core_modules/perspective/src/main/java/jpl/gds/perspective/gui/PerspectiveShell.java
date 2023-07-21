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
package jpl.gds.perspective.gui;

import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.shared.swt.ChillShell;

/**
 * PerspectiveShell is implemented by all Shells that participate in a
 * perspective and have a display configuration.
 * 
 *
 */
public interface PerspectiveShell extends ChillShell {

    /**
     * Sets the display configuration for this PerspectiveShell.
     * 
     * @param config
     *            the DisplayConfiguration to set
     */
    public void setDisplayConfiguration(DisplayConfiguration config);

    /**
     * Instructs a PerspectiveShell to update its display configuration
     * object(s) to reflect current settings. Assumes the display configuration
     * is the same object referenced in the current application perspective, and
     * has not been cloned by the shell.
     */
    public void updateConfiguration();

    /**
     * Sets the perspective listener object for this PerspectiveShell.
     * 
     * @param listener
     *            the PerspectiveListener to set.
     */
    public void setPerspectiveListener(PerspectiveListener listener);

    /**
     * Gets the DisplayConfiguration for this PerspectiveShell.
     * 
     * @return the DisplayConfiguration
     */
    public DisplayConfiguration getDisplayConfiguration();

    /**
     * Exits/closes the PerspectiveShell.
     */
    public void exitShell();

    /**
     * Called when the perspective is saved or locked.
     */
    public void perspectiveChanged();

 // R8 Refactor - Commenting out everything related to session restart
//    /**
//     * Called when the session changes.
//     * 
//     * @param newSession session configuration that replaces old session config
//     */
//    public void sessionChanged(SessionConfiguration newSession);

}
