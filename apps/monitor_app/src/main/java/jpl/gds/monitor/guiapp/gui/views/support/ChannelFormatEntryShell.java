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
package jpl.gds.monitor.guiapp.gui.views.support;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Point;
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

import jpl.gds.dictionary.api.channel.ChannelDefinitionFactory;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.swt.AbstractFormatComposite;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * The ChannelFormatEntryShell class presents a window containing a format
 * composite, used for entering C-style printf format strings for channel data
 * fields.
 */
public class ChannelFormatEntryShell implements ChillShell {
    private static final String START_TOKEN = "[bot]";
    private static final String END_TOKEN = "[eot]";
    private static final char SPACE_TOKEN = '^';

    private static final String TITLE = "Format Specification";
    private Shell mainShell;
    private final Shell parent;
    private boolean cancelled = true;
    private AbstractFormatComposite formatComp;
    private ChannelType dataType;
    private IChannelDefinition chanDef;
    private String formatter;
    private boolean isRawValue;
    private SprintfFormat formatUtil;

    /**
     * Creates an instance of ChannelFormatEntryShell with the given parent.
     * 
     * @param parent
     *            the parent Shell widget
     */
    public ChannelFormatEntryShell(final Shell parent, SprintfFormat formatter) {
        this.parent = parent;
        this.formatUtil = formatter;
    }

    /**
     * Adds the GUI components and listeners
     */
    private void createControls() {
        mainShell = new Shell(parent, SWT.SHELL_TRIM);
        mainShell.setText(TITLE);
        final Point p = parent.getLocation();
        mainShell.setLocation(p.x + 15, p.y + 15);

        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 10;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        mainShell.setLayout(shellLayout);

        final Label typeLabel = new Label(mainShell, SWT.NONE);
        typeLabel.setText("Data Type: " + dataType.toString());
        final FormData typeFd = new FormData();
        typeFd.left = new FormAttachment(0, 5);
        typeFd.top = new FormAttachment(0, 5);
        typeLabel.setLayoutData(typeFd);

        final Composite tryComposite = new Composite(mainShell, SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 5;
        tryComposite.setLayout(fl);
        final FormData tryFd = new FormData();
        tryFd.left = new FormAttachment(0);
        tryFd.top = new FormAttachment(typeLabel, 0, 10);
        tryFd.right = new FormAttachment(100);
        tryComposite.setLayoutData(tryFd);

        final Label tryLabel = new Label(tryComposite, SWT.NONE);
        tryLabel.setText("Try Value: ");
        final FormData tryLabelFd = new FormData();
        tryLabelFd.left = new FormAttachment(0);
        tryLabelFd.top = new FormAttachment(0);
        tryLabel.setLayoutData(tryLabelFd);

        final Text tryValue = new Text(tryComposite, SWT.BORDER);
        final FormData tryTextFd = SWTUtilities.getFormData(tryValue, 1, 30);
        tryTextFd.left = new FormAttachment(tryLabel);
        tryTextFd.top = new FormAttachment(tryLabel, 0, SWT.CENTER);
        tryValue.setLayoutData(tryTextFd);

        final Button tryButton = new Button(tryComposite, SWT.PUSH);
        tryButton.setText("Try");
        final FormData tryButtonFd = new FormData();
        tryButtonFd.left = new FormAttachment(tryValue);
        tryButtonFd.top = new FormAttachment(tryLabel, 0, SWT.CENTER);
        tryButton.setLayoutData(tryButtonFd);

        final Text formatValue = new Text(tryComposite, SWT.BORDER);
        final FormData tryFormatFd = SWTUtilities.getFormData(formatValue, 1,
                40);
        tryFormatFd.left = new FormAttachment(tryButton);
        tryFormatFd.top = new FormAttachment(tryLabel, 0, SWT.CENTER);
        formatValue.setLayoutData(tryFormatFd);
        formatValue.setText(START_TOKEN + END_TOKEN);
        formatValue.setEditable(false);

        tryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final String val = tryValue.getText().trim();
                    if (val.equals("")) {
                        SWTUtilities.showErrorDialog(mainShell,
                                "Invalid Value", "Try text is empty.");
                        return;
                    }
                    formatComp.triggerUpdate();
                    formatter = formatComp.getFormatString();
                    if (formatter == null) {
                        SWTUtilities.showErrorDialog(mainShell,
                                "Invalid Formatter",
                                "The current format selections are not valid.");
                        return;
                    } else {
                        try {
                            final Object obj = getFormatObject(val);
                            String result = START_TOKEN
                                    + formatUtil.anCsprintf(formatter, obj)
                                    + END_TOKEN;
                            result = result.replace(' ', SPACE_TOKEN);
                            formatValue.setText(result);
                        } catch (final Exception ex) {
                            SWTUtilities.showErrorDialog(mainShell,
                                    "Error Formatting Value",
                                    "Problem formatting try text: "
                                            + ex.toString());
                        }
                    }
                } catch (final Exception eE) {
                    eE.printStackTrace();
                }
            }
        });

        final Label helpLabel = new Label(tryComposite, SWT.NONE);
        final FormData helpFd = new FormData();
        helpFd.left = new FormAttachment(0);
        helpFd.top = new FormAttachment(tryLabel, 0, 5);
        helpLabel.setLayoutData(helpFd);
        helpLabel
                .setText("Legend: <bot>=Beginning of Text, " +
                		"<eot>=End of Text, ^=Space. These will not appear " +
                		"on actual displays.");

        Label line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData lineData = new FormData();
        lineData.left = new FormAttachment(0, 3);
        lineData.right = new FormAttachment(100, 3);
        lineData.top = new FormAttachment(tryComposite, 5);
        line.setLayoutData(lineData);

        formatComp = FormatCompositeFactory.create(mainShell, chanDef, isRawValue);
        if (formatter == null) {
            formatComp.setActualString("");
        } else {
            formatComp.setActualString(formatter);
        }
        final Composite innerComp = formatComp.getComposite();
        final FormData formatFd = new FormData();
        formatFd.left = new FormAttachment(0);
        formatFd.top = new FormAttachment(line);
        formatFd.right = new FormAttachment(100);
        innerComp.setLayoutData(formatFd);

        final Composite composite = new Composite(mainShell, SWT.NONE);
        final GridLayout rl = new GridLayout(2, true);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        formData8.top = new FormAttachment(innerComp);
        composite.setLayoutData(formData8);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Ok");
        mainShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    cancelled = false;
                    try {
                        formatter = formatComp.getFormatString();
                    } catch (final IllegalArgumentException ex) {
                        SWTUtilities.showErrorDialog(mainShell,
                                "Error in Field Entry", ex.getMessage());
                        return;
                    }
                    if (formatter == null) {
                        SWTUtilities.showErrorDialog(mainShell,
                                "Invalid Formatter",
                                "The current format selections are not " +
                                "valid.");
                    } else {
                        cancelled = false;
                        mainShell.close();
                    }
                } catch (final Exception eE) {
                    eE.printStackTrace();
                }
            }
        });

        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");

        line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.bottom = new FormAttachment(composite, 5);
        line.setLayoutData(formData6);

        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                cancelled = true;
                mainShell.close();
            }
        });

        mainShell.pack();
    }

    private Object getFormatObject(final String text) {
        if (dataType.isStringType()) {
            return text;
        } else if (dataType.equals(ChannelType.BOOLEAN)
                || dataType.equals(ChannelType.STATUS)) {
            if (isRawValue) {
                try {
                    final Integer result = GDR.parse_int(text);
                    return result;
                } catch (final NumberFormatException e) {
                    SWTUtilities.showErrorDialog(mainShell, "Invalid Value",
                            "The current try value is not an integer.");
                }
            } else {
                return text;
            }
        } else if (dataType.isIntegralType()) {
            try {
                final Long result = GDR.parse_long(text);
                return result;
            } catch (final NumberFormatException e) {
                SWTUtilities.showErrorDialog(mainShell, "Invalid Value",
                        "The current try value is not an integer.");

            }
        } else if (dataType.isNumberType()) {
            try {
                final Double result = Double.parseDouble(text);
                return result;
            } catch (final NumberFormatException e) {
                SWTUtilities.showErrorDialog(mainShell, "Invalid Value",
                        "The current try value is not an floating point.");
            }
        }
        return null;
    }

    /**
     * Retrieves the C-style format string configured in this window by the
     * user.
     * 
     * @return format string
     */
    public String getFormatString() {
        return formatter;
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
        return TITLE;
    }

    /**
     * Initializes the shell for data entry.
     * 
     * @param def
     *            the channel definition for the channel being formatted
     * @param isRaw
     *            true if formatting the raw/DN value, false if formatting the
     *            value/EU
     * @param initialFormatter
     *            initial C-style format string
     */
    public void init(final IChannelDefinition def, final boolean isRaw,
            final String initialFormatter) {
        dataType = def.getChannelType();
        chanDef = def;
        formatter = initialFormatter;
        this.isRawValue = isRaw;
        createControls();
    }

    /**
     * Initializes the shell for data entry.
     * 
     * @param type
     *            the channel data type for the value being formatted
     * @param isRaw
     *            true if formatting the raw/DN value, false if formatting the
     *            value/EU
     * @param initialFormatter
     *            initial C-style format string
     */
    public void init(final ChannelType type, final boolean isRaw,
            final String initialFormatter) {
        dataType = type;
        chanDef = ChannelDefinitionFactory.createFlightChannel(
                "A-1000", type);
        formatter = initialFormatter;
        this.isRawValue = isRaw;
        createControls();
    }

    /**
     * Indicates if this shell is formatted the raw/DN value.
     * 
     * @return true if formatted raw/DN value, false if value/EU
     */
    public boolean isRaw() {
        return isRawValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {
        mainShell.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wasCanceled() {
        return cancelled;
    }
}
