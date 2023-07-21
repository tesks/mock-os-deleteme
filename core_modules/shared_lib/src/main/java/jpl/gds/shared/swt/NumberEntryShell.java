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

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;


/**
 * NumberEntryShell is a GUI window that allows the user to enter an integer.
 * Bounds on the neterd value may be specified,
 * 
 *
 */
public class NumberEntryShell implements ChillShell {

    private static final String ERROR_WINDOW_TITLE = "Invalid Entry";

    private Shell mainShell;
    private final Shell parent;
    private Text numberText;
    private boolean canceled;
    private final boolean allowNegative;
    private final boolean allowZero;
    private final String title;
    private final String prompt;
    private int value;

    /* Added an optional enable button and
     * bounds to limit the allowed values */
    private Button enableButton;
    private String enableButtonText = null;
    private int lowerBound = Integer.MIN_VALUE;
    private int upperBound = Integer.MAX_VALUE;


    /**
     * Creates an instance of NumberEntryShell.
     * 
     * @param parent
     *            the parent Shell
     * @param title
     *            the title for the window
     * @param prompt
     *            the prompt text to display in the window
     * @param allowNegative
     *            flag indicating whether negative values can be entered
     * @param allowZero
     *            flag indicating whether 0 can be entered
     */
    public NumberEntryShell(final Shell parent, final String title,
            final String prompt, final boolean allowNegative,
            final boolean allowZero) {
        this.parent = parent;
        this.allowNegative = allowNegative;
        this.allowZero = allowZero;
        this.prompt = prompt;
        this.title = title;
        createControls();
    }

    /**
     * Creates an instance of NumberEntryShell.
     * 
     * @param parent the parent Shell
     * @param title the title for the window
     * @param prompt the prompt text to display in the window
     * @param allowNegative flag indicating whether negative values can be entered
     * @param allowZero flag indicating whether 0 can be entered
     * @param enableButtonText enable button text that should be shown 
     * 						   in shell
     */
    public NumberEntryShell(final Shell parent, final String title,
            final String prompt, final boolean allowNegative,
            final boolean allowZero, final String enableButtonText) {
        /* Added an argument to the constructor
         * for displaying the enable button */
        this.parent = parent;
        this.allowNegative = allowNegative;
        this.allowZero = allowZero;
        this.enableButtonText = enableButtonText;
        this.prompt = prompt;
        this.title = title;
        createControls();
    }

    /**
     * Creates the GUI controls.
     */
    protected void createControls() {
        mainShell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM);
        mainShell.setText(title);
        // mainShell.setSize(300, 120);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        mainShell.setLayout(shellLayout);

        final Label numberLabel = new Label(mainShell, SWT.NONE);
        numberLabel.setText(prompt + ":");
        final FormData fd1 = new FormData();
        fd1.left = new FormAttachment(0, 5);
        fd1.top = new FormAttachment(0, 5);
        numberLabel.setLayoutData(fd1);

        numberText = new Text(mainShell, SWT.SINGLE | SWT.BORDER);
        numberText.setText("");
        final FormData fd2 = SWTUtilities.getFormData(numberText, 1, 15);
        fd2.left = new FormAttachment(numberLabel);
        fd2.top = new FormAttachment(0);
        numberText.setLayoutData(fd2);

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

        /** Add check box for enabling */
        enableButton = new Button(mainShell, SWT.CHECK);
        if (enableButtonText != null) {
            enableButton.setText(enableButtonText);
        }
        enableButton.setSelection(true);
        final FormData fd0 = new FormData();
        fd0.left = new FormAttachment(0, 5);
        fd0.top = new FormAttachment(numberLabel, 5);
        enableButton.setLayoutData(fd0);

        final Label line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.top = new FormAttachment(enableButton, 0, 2);
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, -3);
        formData6.bottom = new FormAttachment(composite, 5);
        line.setLayoutData(formData6);

        /* Only show enable button if
         * showEnableButton boolean is true */
        if(enableButtonText != null) {
            enableButton.setVisible(true);
        }
        else {
            enableButton.setVisible(false);
            formData6.top = new FormAttachment(numberLabel, 0, 5);
            line.setLayoutData(formData6);
        }

        /*  Add listener to enabled button. */
        enableButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                if(enableButton.getSelection()) {
                    numberText.setEnabled(true);
                } else {
                    /* This getText() call is
                     * necessary so the text remains unchanged.  Without it, 
                     * the number would truncate or revert...must be an SWT 
                     * bug? */
                    numberText.getText();
                    numberText.setEnabled(false);
                }
            }
        });

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final String logStr = numberText.getText().trim();
                    if (logStr.equals("")) {
                        SWTUtilities.showErrorDialog(mainShell,
                                ERROR_WINDOW_TITLE, "You must enter a number.");
                        return;
                    }
                    try {
                        value = Integer.parseInt(logStr);
                    } catch (final NumberFormatException ex) {
                        SWTUtilities.showErrorDialog(mainShell,
                                ERROR_WINDOW_TITLE,
                                "You must enter a valid number.");
                        return;
                    }
                    if (!allowZero && value == 0) {
                        SWTUtilities.showErrorDialog(mainShell,
                                ERROR_WINDOW_TITLE, "The number cannot be 0.");
                        return;
                    }
                    if (!allowNegative && value < 0) {
                        SWTUtilities.showErrorDialog(mainShell,
                                ERROR_WINDOW_TITLE,
                                "The number cannot be negative.");
                        return;
                    }
                    /* added checks for upper
                     *  bound and lower bound */
                    if (value < lowerBound && value != 0) {
                        SWTUtilities.showErrorDialog(mainShell,
                                ERROR_WINDOW_TITLE,
                                "The number must be greater than or equal to " + 
                                        lowerBound + ".");
                        return;
                    }
                    if (value > upperBound) {
                        SWTUtilities.showErrorDialog(mainShell,
                                ERROR_WINDOW_TITLE,
                                "The number must be less than or equal to " + 
                                        upperBound + ".");
                        return;
                    }
                    canceled = false;
                    mainShell.close();
                } catch (final Exception eE) {
                    TraceManager.getTracer(Loggers.UTIL).error(
                                                               "APPLY button caught unhandled and ",
                                                               "unexpected exception in ", "NumberEntryShell.java: ",
                                                               ExceptionTools.getMessage(eE));
                    eE.printStackTrace();
                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                value = 0;
                canceled = true;
                mainShell.close();
            }
        });

        mainShell.pack();
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
     * Gets the numeric value entered by the user.
     * 
     * @return numeric value
     */
    public int getValue() {
        return value;
    }

    /*  Added a getter for the enabled button
     * selection and added setters for lower and upper bound and the enabled 
     * button */
    /**
     * Gets the numeric value entered by the user.
     * 
     * @return numeric value
     */
    public boolean isEnabled() {
        return enableButton.getSelection();
    }

    /**
     * Sets the integer value for lower bound
     * 
     * @param lowerBound the lowest allowed number
     */
    public void setLowerBound(final int lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * Sets the integer value for upper bound
     * 
     * @param upperBound the largest allowed number
     */
    public void setUpperBound(final int upperBound) {
        this.upperBound = upperBound;
    }
    /**
     * Gray out the number text field and uncheck the enable check box button
     */
    public void disable() {
        this.numberText.setEnabled(false);
        this.enableButton.setSelection(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        value = 0;
        canceled = true;
        mainShell.open();
    }

    /**
     * Sets the numeric value displayed in the text field.
     * 
     * @param aValue
     *            numeric value to set
     */
    public void setValue(final int aValue) {
        numberText.setText(Integer.toString(aValue));
    }

    /**
     * Sets the numeric value displayed in the text field.
     * 
     * @param aValue
     *            numeric value to set
     */
    public void setValue(final long aValue) {
        numberText.setText(Long.toString(aValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasCanceled() {
        return canceled;
    }
}
