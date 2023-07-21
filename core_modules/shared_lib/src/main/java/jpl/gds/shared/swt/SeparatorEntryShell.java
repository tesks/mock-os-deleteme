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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.Tracer;

/**
 * SeparatorEntryShell is a GUI window that allows the user to select/enter a
 * separator type.
 * 
 *
 */
public class SeparatorEntryShell implements ChillShell {
    /**
     * Contains list of separator types
     *
     *
     */
    public enum SeparatorType {
    	/**
    	 * Text separator.
    	 */
        TEXT, 
        /**
         * Blank line separator.
         */
        BLANK_SPACE, 
        /**
         * Drawn line separator.
         */
        LINE_ONLY, 
        /**
         * Combination test and drawn line separator.
         */
        TEXT_AND_LINE
    }

    private Shell mainShell;
    private final Shell parent;
    private Text text;
    private boolean canceled;
    private boolean allowEmpty = true;
    private final String title;
    private final String prompt;
    private String value = "";
    private SeparatorType type;
    private Tracer        trace;

    /**
     * Creates an instance of SeparatorEntryShell.
     * 
     * @param parent
     *            the parent Shell
     * @param title
     *            the title for this shell
     * @param prompt
     *            the prompt text to display
     * @param initialValue
     *            default value for the text entry
     * @param allowEmpty
     *            flag indicating whether empty text is acceptable for entry
     * @param trace
     *            Tracer logger
     */
    public SeparatorEntryShell(final Shell parent, final String title,
            final String prompt, final String initialValue,
            final boolean allowEmpty, Tracer trace) {
        this.parent = parent;
        this.allowEmpty = allowEmpty;
        this.prompt = prompt;
        this.title = title;
        if (initialValue != null) {
            value = initialValue;
        }
        this.trace = trace;
        createControls();
    }

    /**
     * Creates the GUI controls.
     */
    protected void createControls() {
        mainShell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM);
        mainShell.setText(title);
        mainShell.setSize(500, 160);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 10;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        mainShell.setLayout(shellLayout);

        final Label typeLabel = new Label(mainShell, SWT.NONE);
        typeLabel.setText("Separator Type:");
        final FormData fd0 = new FormData();
        fd0.left = new FormAttachment(0, 5);
        fd0.top = new FormAttachment(0, 5);
        typeLabel.setLayoutData(fd0);
        final Combo typeCombo = new Combo(mainShell, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        for (final SeparatorType s : SeparatorType.values()) {
            typeCombo.add(s.toString());
        }
        typeCombo.setText(SeparatorType.TEXT.toString());
        final FormData fdtc = new FormData();
        fdtc.left = new FormAttachment(typeLabel);
        fdtc.top = new FormAttachment(typeLabel, 0, SWT.CENTER);
        typeCombo.setLayoutData(fdtc);

        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                type = Enum.valueOf(SeparatorType.class, typeCombo.getText());
                text.setEnabled(type == SeparatorType.TEXT
                        || type == SeparatorType.TEXT_AND_LINE);
            }
        });

        final Label numberLabel = new Label(mainShell, SWT.NONE);
        numberLabel.setText(prompt + ":");
        final FormData fd1 = new FormData();
        fd1.left = new FormAttachment(0, 5);
        fd1.top = new FormAttachment(typeCombo);
        numberLabel.setLayoutData(fd1);
        text = new Text(mainShell, SWT.SINGLE | SWT.BORDER);
        text.setText(value);
        final FormData fd2 = SWTUtilities.getFormData(text, 1, 35);
        fd2.left = new FormAttachment(numberLabel);
        fd2.top = new FormAttachment(numberLabel, 0, SWT.CENTER);
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
                    type = Enum.valueOf(SeparatorType.class, typeCombo
                            .getText());
                    value = text.getText().trim();
                    if ((type == SeparatorType.TEXT || 
                            type == SeparatorType.TEXT_AND_LINE)
                            && value.equals("") && !allowEmpty) {
                        SWTUtilities.showErrorDialog(mainShell,
                                "Invalid Entry", "You must enter some text.");
                        return;
                    }
                    if (type == SeparatorType.LINE_ONLY
                            || type == SeparatorType.BLANK_SPACE) {
                        value = "";
                    }
                    canceled = false;
                    mainShell.close();
                } catch (final Exception eE) {
                    trace.error(
                                    "APPLY button caught unhandled and " +
                                    "unexpected exception in " +
                                    "SeparatorEntryShell.java");
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
                                    "SeparatorEntryShell.java");
                    eE.printStackTrace();
                }
            }
        });
    }

    /**
     * Gets the type of separator selected by the user.
     * 
     * @return SeparatorType
     */
    public SeparatorType getSeparatorType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public Shell getShell() {
        return mainShell;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public String getTitle() {
        return title;
    }

    /**
     * Gets the text entered by the user, for separator types that require 
     * text.
     * 
     * @return separator text, or the empty string if the chosen separator type
     *         does not require text
     */
    public String getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void open() {
        canceled = true;
        mainShell.open();
        text.setText(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public boolean wasCanceled() {
        return canceled;
    }
}
