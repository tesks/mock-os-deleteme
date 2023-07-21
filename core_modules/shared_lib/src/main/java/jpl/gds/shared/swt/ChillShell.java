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
package jpl.gds.shared.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * ChillShell is an interface to be implemented by cooperative SWT shell
 * classes.
 * 
 *
 */
public interface ChillShell {
    /**
     * Gets the active SWT shell object.
     * 
     * @return Shell object
     */
    public Shell getShell();

    /**
     * Gets the name/type of the Shell. This is NOT the name on the title bar,
     * which may be changed, but should be an immutable identifier of the 
     * window type.
     * 
     * @return a name or type String.
     */
    public String getTitle();

    /**
     * Opens (displays) the shell.
     * 
     */
    public void open();

    /**
     * Indicates if the shell was dismissed with a Cancel button.
     * 
     * @return true if cancel was pressed; false otherwise
     */
    public boolean wasCanceled();


    /**
     * Prompt user upon quitting app
     * @param event
     * @return
     */
    default boolean actuallyCloseShell(SelectionEvent event) {
        int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO;
        MessageBox messageBox = new MessageBox(getShell(), style);
        messageBox.setText("Close App");
        messageBox.setMessage("Close this app?");
        return event.doit =messageBox.open() == SWT.YES;
    }

    /**
     * Prompt user upon quitting app.
     * Overloaded to be compatible with {@link org.eclipse.swt.events.ShellListener#shellClosed(ShellEvent)}
     * @param event
     * @return
     */
    default boolean actuallyCloseShell(ShellEvent event) {
        int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO;
        MessageBox messageBox = new MessageBox(getShell(), style);
        messageBox.setText("Close App");
        messageBox.setMessage("Close this app?");
        return event.doit = messageBox.open() == SWT.YES;
    }
}
