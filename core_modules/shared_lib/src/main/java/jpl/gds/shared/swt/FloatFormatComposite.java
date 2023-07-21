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

import static jpl.gds.shared.swt.SWTUtilities.getPaddedFormLayout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.ParsedFormatter;
import jpl.gds.shared.types.AlignmentType;
import jpl.gds.shared.types.FormatType;
import jpl.gds.shared.types.PadCharacterType;

/**
 * The FloatFormatComposite class is a GUI composite specifically for allowing
 * the user to configure a C-style printf format string for a floating point
 * field.
 * 
 */
public class FloatFormatComposite extends AbstractFormatComposite {

    private Text widthText;
    private Text precisionText;
    private Combo alignCombo;
    private Combo padCombo;
    private Button useSignButton;
    private Button useSpaceForSignButton;
    private Button floatButton;
    private Button exponentialButton;

    private Button floatOrExponentialButton;
    private final Tracer trace;

    /**
     * Creates a FloatFormatComposite with the given parent.
     * 
     * @param parent
     *            the parent Composite widget
     * @param trace
     *            Tracer logger
     */
    public FloatFormatComposite(final Composite parent, Tracer trace) {
        super(parent);
        this.trace = trace;
        createControls();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.AbstractFormatComposite#createControls()
     */
    @Override
    protected void createControls() {
        super.createControls();

        final Group formatComposite = new Group(getComposite(), SWT.BORDER);
        formatComposite.setLayout(new GridLayout(3, false));
        final FormData formatFd = new FormData();
        formatFd.top = new FormAttachment(getActualComposite());
        formatFd.left = new FormAttachment(0);
        formatFd.right = new FormAttachment(100);
        formatComposite.setLayoutData(formatFd);

        floatButton = new Button(formatComposite, SWT.RADIO);
        floatButton.setText("Floating Point");
        exponentialButton = new Button(formatComposite, SWT.RADIO);
        exponentialButton.setText("Exponential");
        floatOrExponentialButton = new Button(formatComposite, SWT.RADIO);
        floatOrExponentialButton.setText("Floating Point or Exponential");

        final Composite widthComposite = new Composite(
                getComposite(), SWT.NONE);
        FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 0;
        widthComposite.setLayout(fl);
        final FormData widthFd = new FormData();
        widthFd.top = new FormAttachment(formatComposite, 0, 5);
        widthFd.left = new FormAttachment(0);
        widthFd.right = new FormAttachment(100);
        widthComposite.setLayoutData(widthFd);

        final Label widthLabel = new Label(widthComposite, SWT.NONE);
        widthLabel.setText("Field Width (0 for default):");
        final FormData widthLabelFd = new FormData();
        widthLabelFd.top = new FormAttachment(2);
        widthLabelFd.left = new FormAttachment(0);
        widthLabel.setLayoutData(widthLabelFd);

        widthText = new Text(widthComposite, SWT.BORDER);
        final FormData widthTextFd = SWTUtilities.getFormData(
                widthText, 1, 10);
        widthTextFd.top = new FormAttachment(widthLabel, 0, SWT.CENTER);
        widthTextFd.left = new FormAttachment(30);
        widthText.setLayoutData(widthTextFd);

        final Composite precisionComposite = new Composite(getComposite(),
                SWT.NONE);
        fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 0;
        precisionComposite.setLayout(fl);
        final FormData precisionFd = new FormData();
        precisionFd.top = new FormAttachment(widthComposite, 0, 5);
        precisionFd.left = new FormAttachment(0);
        precisionFd.right = new FormAttachment(100);
        precisionComposite.setLayoutData(precisionFd);

        final Label precisionLabel = new Label(precisionComposite, SWT.NONE);
        precisionLabel.setText("Precision (0 for default):");
        final FormData precisionLabelFd = new FormData();
        precisionLabelFd.top = new FormAttachment(2);
        precisionLabelFd.left = new FormAttachment(0);
        precisionLabel.setLayoutData(precisionLabelFd);

        precisionText = new Text(precisionComposite, SWT.BORDER);
        final FormData precisionTextFd = SWTUtilities.getFormData(
                precisionText, 1, 10);
        precisionTextFd.top = new FormAttachment(
                precisionLabel, 0, SWT.CENTER);
        precisionTextFd.left = new FormAttachment(30);
        precisionText.setLayoutData(precisionTextFd);

        final Composite padComposite = new Composite(getComposite(), SWT.NONE);

        padComposite.setLayout(getPaddedFormLayout());

        final FormData padFd = new FormData();
        padFd.top = new FormAttachment(precisionComposite);
        padFd.left = new FormAttachment(0);
        padFd.right = new FormAttachment(100);
        padComposite.setLayoutData(padFd);

        final Label padLabel = new Label(padComposite, SWT.NONE);
        padLabel.setText("Pad Character:");
        final FormData padLabelFd = new FormData();
        padLabelFd.top = new FormAttachment(0);
        padLabelFd.left = new FormAttachment(0);
        padLabel.setLayoutData(padLabelFd);

        padCombo = new Combo(padComposite, SWT.NONE);
        final FormData padComboFd = SWTUtilities.getFormData(padCombo, 1, 15);
        padComboFd.top = new FormAttachment(padLabel, 0, SWT.CENTER);
        padComboFd.left = new FormAttachment(30);
        padCombo.setLayoutData(padComboFd);
        final PadCharacterType[] padValues = PadCharacterType.values();
        for (int i = 0; i < padValues.length; i++) {
            padCombo.add(padValues[i].toString());
        }
        definePadListener();

        final Composite alignComposite = new Composite(
                getComposite(), SWT.NONE);

        alignComposite.setLayout(getPaddedFormLayout());

        final FormData alignFd = new FormData();
        alignFd.top = new FormAttachment(padComposite);
        alignFd.left = new FormAttachment(0);
        alignFd.right = new FormAttachment(100);
        alignComposite.setLayoutData(alignFd);

        final Label alignmentLabel = new Label(alignComposite, SWT.NONE);
        alignmentLabel.setText("Alignment:");
        final FormData alignLabelFd = new FormData();
        alignLabelFd.top = new FormAttachment(0);
        alignLabelFd.left = new FormAttachment(0);
        alignmentLabel.setLayoutData(alignLabelFd);

        alignCombo = new Combo(alignComposite, SWT.NONE);
        final FormData alignComboFd = SWTUtilities.getFormData(alignCombo, 1,
                15);
        alignComboFd.top = new FormAttachment(alignmentLabel, 0, SWT.CENTER);
        alignComboFd.left = new FormAttachment(30);
        alignCombo.setLayoutData(alignComboFd);
        final AlignmentType[] alignValues = AlignmentType.values();
        for (int i = 0; i < alignValues.length; i++) {
            alignCombo.add(alignValues[i].toString());
        }

        final Group buttonComposite = new Group(getComposite(), SWT.BORDER);
        buttonComposite.setLayout(new GridLayout(2, false));
        final FormData buttonFd = new FormData();
        buttonFd.top = new FormAttachment(alignComposite);
        buttonFd.left = new FormAttachment(0);
        buttonFd.right = new FormAttachment(100);
        buttonComposite.setLayoutData(buttonFd);

        useSignButton = new Button(buttonComposite, SWT.CHECK);
        useSignButton.setText("Always Display Sign");

        useSpaceForSignButton = new Button(buttonComposite, SWT.CHECK);
        useSpaceForSignButton
                .setText("Replace + with Space When Padding with 0");
    }

    private void definePadListener() {
        padCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final PadCharacterType pad = PadCharacterType
                            .valueOf(padCombo.getText());
                    if (pad.equals(PadCharacterType.ZERO)) {
                        useSpaceForSignButton.setEnabled(true);
                    } else {
                        useSpaceForSignButton.setEnabled(false);
                    }
                } catch (final Exception eE) {
                    trace.error(
                                    "padCombo in FloatFormatComposite " +
                                    "caught unhandled and unexpected " +
                                    "exception.");
                    eE.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.AbstractFormatComposite#enableFields(boolean)
     */
    @Override
    protected void enableFields(final boolean enable) {
        if (!enable) {
            widthText.setEnabled(false);
            precisionText.setEnabled(false);
            alignCombo.setEnabled(false);
            padCombo.setEnabled(false);
            useSignButton.setEnabled(false);
            useSpaceForSignButton.setEnabled(false);
            floatButton.setEnabled(false);
            exponentialButton.setEnabled(false);
            floatOrExponentialButton.setEnabled(false);
        } else {
            final ParsedFormatter parsed = ParsedFormatter
                    .parseFloatFormatter(getFormatString());
            if (parsed != null) {
                setFields(parsed);
                super.setActualString(parsed.getFormatStringOnly());
                super.setPrefixString(parsed.getPrefix());
                super.setSuffixString(parsed.getSuffix());
            } else {
                setFieldsToDefaults();
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.AbstractFormatComposite#getParsedFormatFromFields()
     */
    @Override
    protected ParsedFormatter getParsedFormatFromFields() {
        if (getComposite().isDisposed()) {
            return null;
        }
        if (widthText.getText().equals("")) {
            throw new IllegalArgumentException("Width must be specified");
        }
        if (precisionText.getText().equals("")) {
            throw new IllegalArgumentException("Precision must be specified");
        }

        final ParsedFormatter format = new ParsedFormatter();
        format.setWidth(Integer.parseInt(widthText.getText()));
        format.setPrecision(Integer.parseInt(precisionText.getText()));
        format.setAlignment(AlignmentType.valueOf(alignCombo.getText()));
        format.setPadChar(PadCharacterType.valueOf(padCombo.getText()));
        format.setSignIsSpaceWhenPadding(useSpaceForSignButton.getSelection());
        format.setUseUpperCaseHex(false);
        format.setUseSign(useSignButton.getSelection());
        if (floatButton.getSelection()) {
            format.setType(FormatType.FLOAT);
        } else if (exponentialButton.getSelection()) {
            format.setType(FormatType.EXPONENTIAL);
        } else if (floatOrExponentialButton.getSelection()) {
            format.setType(FormatType.FLOAT_OR_EXPONENTIAL);
        }
        format.setPrefix(getPrefixString());
        format.setSuffix(getSuffixString());
        return format;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.AbstractFormatComposite#setActualString(java.lang.String)
     */
    @Override
    public void setActualString(String formatter) {
        if (formatter == null || formatter.equals("")) {
            formatter = "%f";
        }

        final ParsedFormatter parsed = ParsedFormatter
                .parseFloatFormatter(formatter);
        if (parsed != null) {
            setFields(parsed);
            super.setActualString(parsed.getFormatStringOnly());
            super.setPrefixString(parsed.getPrefix());
            super.setSuffixString(parsed.getSuffix());
        } else {
            setFieldsToDefaults();
        }
    }

    private void setFields(final ParsedFormatter formatter) {
        if (getComposite().isDisposed()) {
            return;
        }
        widthText.setText(String.valueOf(formatter.getWidth()));
        precisionText.setText(String.valueOf(formatter.getPrecision()));
        padCombo.setText(formatter.getPadChar().toString());
        alignCombo.setText(formatter.getAlignment().toString());
        widthText.setEnabled(true);
        precisionText.setEnabled(true);
        padCombo.setEnabled(true);
        alignCombo.setEnabled(true);
        useSignButton.setSelection(formatter.isUseSign());
        useSignButton.setEnabled(true);

        if (formatter.getPadChar().equals(PadCharacterType.ZERO)) {
            useSpaceForSignButton.setSelection(formatter
                    .isSignIsSpaceWhenPadding());
            useSpaceForSignButton.setEnabled(true);
        } else {
            useSpaceForSignButton.setSelection(false);
            useSpaceForSignButton.setEnabled(false);
        }

        floatButton.setEnabled(true);
        floatButton.setSelection(formatter.getType() == FormatType.FLOAT);
        exponentialButton.setEnabled(true);
        exponentialButton
                .setSelection(formatter.getType() == FormatType.EXPONENTIAL);
        floatOrExponentialButton.setEnabled(true);
        floatOrExponentialButton
                .setSelection(formatter.getType() == 
                    FormatType.FLOAT_OR_EXPONENTIAL);
    }

    private void setFieldsToDefaults() {
        if (getComposite().isDisposed()) {
            return;
        }
        widthText.setText("0");
        precisionText.setText("0");
        padCombo.setText(PadCharacterType.NONE.toString());
        alignCombo.setText(AlignmentType.RIGHT.toString());
        widthText.setEnabled(true);
        padCombo.setEnabled(true);
        alignCombo.setEnabled(true);
        useSignButton.setSelection(false);
        useSignButton.setEnabled(true);
        useSpaceForSignButton.setSelection(false);
        useSpaceForSignButton.setEnabled(false);
        floatButton.setEnabled(true);
        floatButton.setSelection(true);
        exponentialButton.setEnabled(true);
        exponentialButton.setSelection(false);
        floatOrExponentialButton.setEnabled(true);
        floatOrExponentialButton.setSelection(false);

        displayFormatError();
    }
}
