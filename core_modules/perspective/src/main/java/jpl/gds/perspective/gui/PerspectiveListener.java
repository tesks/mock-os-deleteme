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

import org.eclipse.swt.widgets.Shell;

/**
 * PerspectiveListener is the interface by which different applications in a
 * perspective receive notification of perspective-related requests.
 * 
 *
 * 
 */
public interface PerspectiveListener {
    /**
     * Called when the user elects to save the current application configuration
     * to the default location.
     */
    public void saveCalled();

    /**
     * Called when the user elects to save the current application configuration
     * to a new location.
     * 
     * @param parent
     *            the parent Shell (needed for dialog display)
     */
    public void saveAsCalled(Shell parent);

    /**
     * Invoked when the user elects to exit the perspective.
     */
    public void exitCalled();

    /**
     * Sets the PerspectiveShell associated with this listener.
     * 
     * @param shell
     *            the PerspectiveShell to set
     */
    public void setPerspectiveShell(PerspectiveShell shell);

    /**
     * Gets the PerspectiveShell associated with this listener.
     * 
     * @return the perspective shell
     */
    public PerspectiveShell getPerspectiveShell();

    /**
     * Called when the user elects to lock or unlock the perspective.
     * 
     * @param lock
     *            true to lock, false to unlock
     */
    public void setPerspectiveLock(boolean lock);
    
    // R8 Refactor - Commenting out everything related to session restart
//    /**
//     * Called when the user elects to start a new session without shutdown.
//     */
//    public void newSessionStarted();
//

    /**
     * Get exit perspective status.
     *
     * @return True if perspective is exiting
     */
    public boolean getPerspectiveExit();
}
