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

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.Tracer;


/**
 * Presents a GUI composite for entering a file path for purposes of saving a
 * file only. The filename can be entered in a text widget or via a file
 * browser. WARNING: IF AN EXISTING FILE IS CHOSEN, IT IS DELETED and the file
 * path is returned.
 * 
 *
 */
public class FileEntryComposite {

    private Composite mainComposite;
    private final Composite parent;
    private Text fileText;
    private final Tracer    trace;

    /**
     * Creates a new FileEntryComposite
     * 
     * @param parent
     *            the parent composite
     * @param label
     *            the text to label the filename field with
     * @param buttonLabel
     *            text that will go on the "browse" button
     * @param trace
     *            The Tracer logger
     */
    public FileEntryComposite(final Composite parent, final String label,
            final String buttonLabel, Tracer trace) {
        this.parent = parent;
        this.trace = trace;
        createControls(label, buttonLabel);
    }

    private void createControls(final String label, final String buttonLabel) {

        mainComposite = new Composite(parent, SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        fl.spacing = 5;
        mainComposite.setLayout(fl);

        final Label titleLabel = new Label(mainComposite, SWT.NONE);
        titleLabel.setText(label);
        titleLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
        final FormData fdLabel2 = new FormData();
        fdLabel2.top = new FormAttachment(0, 10);
        fdLabel2.left = new FormAttachment(0);
        titleLabel.setLayoutData(fdLabel2);
        fileText = new Text(mainComposite, SWT.BORDER);
        final FormData fdTitle2 = SWTUtilities.getFormData(fileText, 1, 50);
        fdTitle2.top = new FormAttachment(titleLabel, 0, SWT.CENTER);
        fdTitle2.left = new FormAttachment(titleLabel);
        fileText.setLayoutData(fdTitle2);

        final Button browseButton = new Button(mainComposite, SWT.PUSH);
        browseButton.setText(buttonLabel);
        final FormData fdb = new FormData();
        fdb.left = new FormAttachment(fileText);
        fdb.top = new FormAttachment(titleLabel, 0, SWT.CENTER);
        browseButton.setLayoutData(fdb);

        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final SWTUtilities swt = new SWTUtilities();
                    final String path = swt.displayStickyFileSaver(
                            mainComposite.getShell(), "FileEntryComposite",
                            null, fileText.getText());
                    if (path != null) {
                        // clear existing file by deleting it
                        final File file = new File(path);
                        
                        if(!file.delete()) {
                            trace.error(
                                    "Existing file was not successfully deleted");
                        }

                        // set the text field to the user specified path
                        fileText.setText(path);
                    }
                } catch (final Exception eE) {
                    trace.error(
                            "BROWSE button caught unhandled and "
                                    + "unexpected exception in "
                                    + "FileEntryComposite.java");
                }
            }
        });

    }

    /**
     * Gets the composite object for layout purposes.
     * 
     * @return the main composite
     */
    public Composite getComposite() {
        return mainComposite;
    }

    /**
     * Gets the text
     * 
     * @return Returns the current file path, null if text field is empty.
     */
    public String getCurrentFile() {
        final String temp = fileText.getText().trim();
        if (temp.equals("")) {
            return null;
        }
        return temp;
    }

    /**
     * Sets the current file path
     * 
     * @param text
     *            the file path to set
     */
    public void setCurrentFile(final String text) {
        fileText.setText(text);
    }
}
