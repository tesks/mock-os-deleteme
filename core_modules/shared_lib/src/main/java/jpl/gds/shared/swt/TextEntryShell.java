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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.Tracer;

/**
 * TextEntryShell is a GUI window that allows the user to enter a text string.
 * 
 *
 */
public class TextEntryShell implements ChillShell {

    private Shell mainShell;
    private final Shell parent;
    private Text text;
    private boolean canceled;
    private boolean allowEmpty = true;
    private final String title;
    private final String prompt;
    private final Tracer trace;

    private String value = "";

    /**
     * Creates an instance of TextEntryShell.
     *
     * @param parent
     *            the parent Shell
     * @param title
     *            the title to display on this shell
     * @param prompt
     *            the prompt text to display
     * @param initialValue
     *            the default value for the entered text
     * @param allowEmpty
     *            flag indicating that the entry can be empty
     * @param trace
     *            Tracer logger
     */
    public TextEntryShell(final Shell parent, final String title,
            final String prompt, final String initialValue,
            final boolean allowEmpty, Tracer trace) {
        this.parent = parent;
        this.allowEmpty = allowEmpty;
        this.prompt = prompt;
        this.title = title;
        if (initialValue != null) {
            this.value = initialValue;
        }
        this.trace = trace;
        createControls();
    }

    /**
     * Creates the GUI controls.
     */
    protected void createControls() {
        mainShell = new Shell(
                this.parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM);
        mainShell.setText(title);
        mainShell.setSize(500, 120);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        mainShell.setLayout(shellLayout);

        final Label numberLabel = new Label(mainShell, SWT.NONE);
        numberLabel.setText(this.prompt + ":");
        final FormData fd1 = new FormData();
        fd1.left = new FormAttachment(0, 5);
        fd1.top = new FormAttachment(0, 5);
        numberLabel.setLayoutData(fd1);
        text = new Text(mainShell, SWT.SINGLE | SWT.BORDER);
        text.setText(this.value);
        final FormData fd2 = SWTUtilities.getFormData(text, 1, 60);
        fd2.left = new FormAttachment(numberLabel);
        fd2.top = new FormAttachment(0);
        fd2.right = new FormAttachment(98);
        text.setLayoutData(fd2);

        final Composite composite = new Composite(mainShell, SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        composite.setLayoutData(formData8);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Ok");
        mainShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);
        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");
        final Label line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.bottom = new FormAttachment(composite, 5);
        line.setLayoutData(formData6);

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    value = text.getText().trim();
                    if (value.equals("") && !allowEmpty) {
                        SWTUtilities.showErrorDialog(mainShell,
                                "Invalid Entry", "You must enter some text.");
                        return;
                    }
                    canceled = false;
                    mainShell.close();
                } catch (final Exception eE) {
                    trace.error(
                                    "APPLY button caught unhandled and " +
                                    "unexpected exception in " +
                                    "TextEntryShell.java");
                    eE.printStackTrace();
                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    value = null;
                    canceled = true;
                    mainShell.close();
                } catch (final Exception eE) {
                    trace.error(
                                    "CANCEL button caught unhandled and " +
                                    "unexpected exception in " +
                                    "TextEntryShell.java");
                    eE.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
	public Shell getShell() {
        return this.mainShell;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
	public String getTitle() {
        return this.title;
    }

    /**
     * Gets the entered text value.
     * 
     * @return string
     */
    public String getValue() {
        return this.value;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
	public void open() {
        this.canceled = true;
        this.mainShell.open();
        text.setText(this.value);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
	public boolean wasCanceled() {
        return this.canceled;
    }
}
