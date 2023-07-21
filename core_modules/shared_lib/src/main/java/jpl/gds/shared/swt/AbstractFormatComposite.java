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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.string.ParsedFormatter;

/**
 * The AbstractFormatComposite class is the base class for all formatting
 * GUI composites, in which the user enters a C-style printf formatter for a
 * specific data type.
 * 
 *
 */
public abstract class AbstractFormatComposite {
    private final Composite parent;
    private Composite mainComposite;
    private Text actualText;
    private Text postpendText;
    private Text prependText;
    private Composite actualComposite;
    private Button enterActualButton;
    private Button enterFieldsButton;

    /**
     * Constructor: Creates an AbstractFormatComposite with the given parent.
     * 
     * @param parent
     *            the parent Composite widget
     */
    public AbstractFormatComposite(final Composite parent) {
        this.parent = parent;
    }

    /**
     * Creates the GUI Layout and controls.
     */
    protected void createControls() {
        mainComposite = new Composite(parent, SWT.NONE);
        FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        mainComposite.setLayout(fl);

        final Group buttonGroup = new Group(mainComposite, SWT.BORDER);
        buttonGroup.setLayout(new GridLayout(2, false));
        final FormData buttonFd = new FormData();
        buttonFd.top = new FormAttachment(2);
        buttonFd.left = new FormAttachment(0);
        buttonFd.right = new FormAttachment(100);
        buttonGroup.setLayoutData(buttonFd);

        enterActualButton = new Button(buttonGroup, SWT.RADIO);
        enterActualButton.setText("Enter Actual Formatter");
        enterActualButton.setSelection(false);
        enterFieldsButton = new Button(buttonGroup, SWT.RADIO);
        enterFieldsButton.setText("Enter Using Fields");
        enterFieldsButton.setSelection(true);

        enterActualButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    actualText.setEnabled(enterActualButton.getSelection());
                    enableFields(!enterActualButton.getSelection());
                } catch (final Exception eE) {
                    eE.printStackTrace();
                }
            }
        });
        enterFieldsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    actualText.setEnabled(!enterFieldsButton.getSelection());
                    enableFields(enterFieldsButton.getSelection());
                } catch (final Exception eE) {
                    eE.printStackTrace();
                }
            }
        });

        actualComposite = new Composite(mainComposite, SWT.NONE);
        actualComposite.setLayout(new FormLayout());
        fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 0;
        actualComposite.setLayout(fl);
        final FormData actualFd = new FormData();
        actualFd.top = new FormAttachment(buttonGroup, 5);
        actualFd.left = new FormAttachment(0);
        actualFd.right = new FormAttachment(100);
        actualComposite.setLayoutData(actualFd);

        final Label prependLabel = new Label(actualComposite, SWT.NONE);
        prependLabel.setText("Prefix Text:");
        final FormData prependLabelFd = new FormData();
        prependLabelFd.top = new FormAttachment(2);
        prependLabelFd.left = new FormAttachment(0);
        prependLabel.setLayoutData(prependLabelFd);

        prependText = new Text(actualComposite, SWT.BORDER);
        final FormData prependTextFd = SWTUtilities.getFormData(prependText, 1,
                10);
        prependTextFd.top = new FormAttachment(prependLabel, 0, SWT.CENTER);
        prependTextFd.left = new FormAttachment(prependLabel);
        prependText.setLayoutData(prependTextFd);

        final Label actualLabel = new Label(actualComposite, SWT.NONE);
        actualLabel.setText("Formatter:");
        final FormData actualLabelFd = new FormData();
        actualLabelFd.top = new FormAttachment(2);
        actualLabelFd.left = new FormAttachment(prependText);
        actualLabel.setLayoutData(actualLabelFd);

        actualText = new Text(actualComposite, SWT.BORDER);
        final FormData actualTextFd = SWTUtilities.getFormData(actualText, 1,
                10);
        actualTextFd.top = new FormAttachment(actualLabel, 0, SWT.CENTER);
        actualTextFd.left = new FormAttachment(actualLabel);
        actualText.setLayoutData(actualTextFd);
        actualText.setEnabled(false);

        final Label postpendLabel = new Label(actualComposite, SWT.NONE);
        postpendLabel.setText("Postfix Text:");
        final FormData postpendLabelFd = new FormData();
        postpendLabelFd.top = new FormAttachment(2);
        postpendLabelFd.left = new FormAttachment(actualText);
        postpendLabel.setLayoutData(postpendLabelFd);

        postpendText = new Text(actualComposite, SWT.BORDER);
        final FormData postpendTextFd = SWTUtilities.getFormData(postpendText,
                1, 10);
        postpendTextFd.top = new FormAttachment(actualLabel, 0, SWT.CENTER);
        postpendTextFd.left = new FormAttachment(postpendLabel);
        postpendText.setLayoutData(postpendTextFd);

        final Button actualButton = new Button(actualComposite, SWT.PUSH);
        actualButton.setText("Update");
        final FormData actualButtonFd = new FormData();
        actualButtonFd.top = new FormAttachment(actualLabel, 0, SWT.CENTER);
        actualButtonFd.left = new FormAttachment(postpendText);
        actualButton.setLayoutData(actualButtonFd);

        actualButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                if (enterActualButton.getSelection()) {
                    final String val = AbstractFormatComposite.this
                            .getFormatString();
                    if (val == null || val.trim().equals("")) {
                        // do nothing
                    } else {
                        setActualString(val);
                    }
                    enableFields(false);
                } else {
                    try {
                        final ParsedFormatter parsedFormatter = 
                            getParsedFormatFromFields();
                        if (parsedFormatter != null) {
                            final String pre = 
                                parsedFormatter.getPrefix() == null ? ""
                                    : parsedFormatter.getPrefix();
                            final String post = 
                                parsedFormatter.getSuffix() == null ? ""
                                    : parsedFormatter.getSuffix();
                            final String val = parsedFormatter
                                    .getFormatStringOnly();
                            setActualString(val);
                            setPrefixString(pre);
                            setSuffixString(post);
                        }
                        enableFields(true);

                    } catch (final IllegalArgumentException exception) {
                        final MessageBox mb = new MessageBox((Shell) parent,
                                SWT.ICON_ERROR);
                        mb.setText("Invalid Arguments");
                        mb.setMessage(exception.getMessage());
                        mb.open();
                    }
                }
            }
        });
    }

    /**
     * Displays an error dialog indicating the current format string is invalid
     */
    protected void displayFormatError() {
        SWTUtilities.showWarningDialog(parent.getShell(), "Bad Format String",
                "Current format not understood for the current data type; " +
                "using defaults.");
    }

    /**
     * Enables or disables fields in the composite based upon the data type and
     * the current "use actual" setting. If the user is entering the actual
     * format string, detailed fields are disabled.
     * 
     * @param enable
     *            true to enable individual fields for user interaction, false
     *            to disable them
     */
    protected abstract void enableFields(boolean enable);

    /**
     * Gets the composite object that contains the "actual" fields.
     * 
     * @return Composite object
     */
    protected Composite getActualComposite() {
        return actualComposite;
    }

    /**
     * Gets the main composite object.
     * 
     * @return Composite object
     */
    public Composite getComposite() {
        return mainComposite;
    }

    /**
     * Gets the complete format string specified by the user, including the
     * prefix and postfix text
     * 
     * @return format string with prefix and suffix, or null if no valid
     *         formatter defined
     */
    public String getFormatString() {
        if (enterActualButton.getSelection()) {

            final String val = prependText.getText() + actualText.getText()
                    + postpendText.getText();
            if (val.trim().equals("")) {
                return null;
            } else {
                return val;
            }
        } else {
            final ParsedFormatter parsedFormatter = 
                getParsedFormatFromFields();
            if (parsedFormatter != null) {
                final String val = parsedFormatter.getPrefix()
                        + parsedFormatter.getFormatStringOnly()
                        + parsedFormatter.getSuffix();
                return val;
            } else {
                return null;
            }
        }
    }

    /**
     * Populates a ParsedFormatter object from current field values.
     * 
     * @return ParsedFormatter object
     */
    protected abstract ParsedFormatter getParsedFormatFromFields();

    /**
     * Gets the prefix string: text to be placed before the formatted data
     * value.
     * 
     * @return prefix text, or the empty string if none defined
     */
    public String getPrefixString() {
        return prependText.getText();
    }

    /**
     * Gets the suffix string: text to be placed after the formatted data 
     * value.
     * 
     * @return suffix text, or the empty string if none defined
     */
    public String getSuffixString() {
        return postpendText.getText();
    }

    /**
     * Sets the formatter string operated on by this composite from a string.
     * 
     * @param formatter
     *            C-style printf formatter string
     */
    public void setActualString(final String formatter) {
        if (formatter == null) {
            actualText.setText("");
        } else {
            actualText.setText(formatter);
        }
    }

    /**
     * Sets the prefix string: text to be placed before the formatted data
     * value.
     * 
     * @param prefix
     *            prefix text, or null to clear it
     */
    public void setPrefixString(final String prefix) {
        if (prefix == null) {
            prependText.setText("");
        } else {
            prependText.setText(prefix);
        }
    }

    /**
     * Sets the suffix string: text to be placed after the formatted data 
     * value.
     * 
     * @param suffix
     *            suffix text, or null to clear it
     */
    public void setSuffixString(final String suffix) {
        if (suffix == null) {
            postpendText.setText("");
        } else {
            postpendText.setText(suffix);
        }
    }

    /**
     * Refreshes fields for a "try" operation, which means updating the actual
     * format fields from the individual fields entered by the user.
     */
    public void triggerUpdate() {
        if (enterActualButton.getSelection()) {
            final String val = AbstractFormatComposite.this.getFormatString();
            if (val == null || val.trim().equals("")) {
                // do nothing
            } else {
                setActualString(val);
            }
            enableFields(false);
        } else {
            final ParsedFormatter parsedFormatter = 
                getParsedFormatFromFields();
            if (parsedFormatter != null) {
                final String pre = parsedFormatter.getPrefix() == null ? ""
                        : parsedFormatter.getPrefix();
                final String post = parsedFormatter.getSuffix() == null ? ""
                        : parsedFormatter.getSuffix();
                final String val = parsedFormatter.getFormatStringOnly();
                setActualString(val);
                setPrefixString(pre);
                setSuffixString(post);
            }
            enableFields(true);
        }
    }
}
