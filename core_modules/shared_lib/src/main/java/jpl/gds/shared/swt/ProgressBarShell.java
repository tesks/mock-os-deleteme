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
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * Creates an GUI progress bar component that wraps an SWT progress bar.
 * 
 */
public class ProgressBarShell {
    private final Shell parentShell;
    private Shell progressBarShell;
    private ProgressBar progBar;
    private Label progLabel;

    /**
     * Constructor: creates a progress bar
     * @param parent the parent shell
     */
    public ProgressBarShell(final Shell parent) {
        this.parentShell = parent;
        createComponents();
    }

    private void createComponents() {
        progressBarShell = new Shell(this.parentShell, SWT.TITLE);
        progressBarShell.setText("");
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        progressBarShell.setLayout(fl);

        progLabel = new Label(progressBarShell, SWT.LEFT);
        progLabel.setText("");
        final FormData fd1 = new FormData();
        fd1.left = new FormAttachment(0);
        fd1.right = new FormAttachment(50);
        fd1.top = new FormAttachment(0);
        fd1.bottom = new FormAttachment(0);
        progressBarShell.setLayoutData(fd1);

        progBar = new ProgressBar(progressBarShell, 
                SWT.SMOOTH | SWT.HORIZONTAL);
        progBar.setMinimum(0);
        progBar.setMaximum(1);
        progBar.setSelection(0);
        final FormData fd2 = new FormData();
        fd2.right = new FormAttachment(100);
        fd2.left = new FormAttachment(progLabel);
        fd2.top = new FormAttachment(progLabel, 0, SWT.CENTER);
        progBar.setLayoutData(fd2);

        progressBarShell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(final ShellEvent e) {
                e.doit = false;
            }
        });
    }

    /**
     * Disposes the progress bar
     */
    public void dispose() {
        progressBarShell.dispose();
        progressBarShell = null;
    }

    /**
     * Gets the progress bar widget
     * @return progress bar widget for displaying progress of a process
     */
    public ProgressBar getProgressBar() {
        return (this.progBar);
    }

    /**
     * Get the text on the progress bar
     * 
     * @return label on the progress bar
     */
    public Label getProgressLabel() {
        return (this.progLabel);
    }

    /**
     * Gets the shell that contains the progress bar
     * @return shell in which progress bar is contained
     */
    public Shell getShell() {
        return (this.progressBarShell);
    }

    /**
     * Sets the size of the progress bar shell and opens it
     */
    public void open() {
        progressBarShell.setSize(300, 50);
        progressBarShell.layout(true);
        progressBarShell.open();
    }
}