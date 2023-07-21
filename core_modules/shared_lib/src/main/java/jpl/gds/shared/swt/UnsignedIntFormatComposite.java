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
import jpl.gds.shared.types.IntegerBaseType;
import jpl.gds.shared.types.PadCharacterType;

/**
 * The UnsignedIntFormatComposite class is a GUI composite specifically for
 * allowing the user to configure a C-style printf format string for an 
 * unsigned integer field.
 * 
 */
public class UnsignedIntFormatComposite extends AbstractFormatComposite {

    private Text widthText;
    private Combo baseCombo;
    private Combo alignCombo;
    private Combo padCombo;
    private Button useUpperCaseButton;

    private Button useTypePrefixButton;

    private final Tracer trace;

    /**
     * Creates a new UnsignedIntFormatComposite with the given parent.
     * 
     * @param parent
     *            the parent Composite widget
     */
    public UnsignedIntFormatComposite(final Composite parent, Tracer trace) {
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

        final Composite widthComposite = new Composite(
                getComposite(), SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        fl.marginHeight = 5;
        fl.marginWidth = 0;
        widthComposite.setLayout(fl);
        final FormData widthFd = new FormData();
        widthFd.top = new FormAttachment(getActualComposite(), 0, 5);
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

        final Composite baseComposite = new Composite(
                getComposite(), SWT.NONE);

        baseComposite.setLayout(getPaddedFormLayout());

        final FormData baseFd = new FormData();
        baseFd.top = new FormAttachment(widthComposite);
        baseFd.left = new FormAttachment(0);
        baseFd.right = new FormAttachment(100);
        baseComposite.setLayoutData(baseFd);

        final Label baseLabel = new Label(baseComposite, SWT.NONE);
        baseLabel.setText("Base:");
        final FormData baseLabelFd = new FormData();
        baseLabelFd.top = new FormAttachment(0);
        baseLabelFd.left = new FormAttachment(0);
        baseLabel.setLayoutData(baseLabelFd);

        baseCombo = new Combo(baseComposite, SWT.NONE);
        final FormData baseComboFd = SWTUtilities.getFormData(
                baseCombo, 1, 15);
        baseComboFd.top = new FormAttachment(baseLabel, 0, SWT.CENTER);
        baseComboFd.left = new FormAttachment(30);
        baseCombo.setLayoutData(baseComboFd);
        final IntegerBaseType[] baseValues = IntegerBaseType.values();
        for (int i = 0; i < baseValues.length; i++) {
            baseCombo.add(baseValues[i].toString());
        }
        defineBaseListener();

        final Composite padComposite = new Composite(getComposite(), SWT.NONE);

        padComposite.setLayout(getPaddedFormLayout());

        final FormData padFd = new FormData();
        padFd.top = new FormAttachment(baseComposite);
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

        useUpperCaseButton = new Button(buttonComposite, SWT.CHECK);
        useUpperCaseButton.setText("Use Upper Case Hexadecimal");

        useTypePrefixButton = new Button(buttonComposite, SWT.CHECK);
        useTypePrefixButton.setText("Prepend 0 or 0x for Octal/Hex");
    }

    private void defineBaseListener() {
        baseCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final IntegerBaseType base = IntegerBaseType
                            .valueOf(baseCombo.getText());
                    if (!base.equals(IntegerBaseType.DECIMAL)) {
                        if (base.equals(IntegerBaseType.HEX)) {
                            useUpperCaseButton.setEnabled(true);
                        } else {
                            useUpperCaseButton.setEnabled(false);
                        }
                        if (base.equals(IntegerBaseType.HEX)
                                || base.equals(IntegerBaseType.OCTAL)) {
                            useTypePrefixButton.setEnabled(true);
                        } else {
                            useTypePrefixButton.setEnabled(false);
                        }
                    } else {
                        useUpperCaseButton.setEnabled(false);
                        useUpperCaseButton.setSelection(false);
                        useTypePrefixButton.setEnabled(false);
                        useTypePrefixButton.setSelection(false);
                    }
                } catch (final Exception eE) {
                    trace.error(
                                    "base-COMBO widget caught unhandled and " +
                                    "unexcepted exception in " +
                                    "UnsignedIntFormatComposite.java");
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
            baseCombo.setEnabled(false);
            alignCombo.setEnabled(false);
            padCombo.setEnabled(false);
            useUpperCaseButton.setEnabled(false);
            useTypePrefixButton.setEnabled(false);
        } else {
            final ParsedFormatter parsed = ParsedFormatter
                    .parseUnsignedIntFormatter(getFormatString());
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

        final ParsedFormatter format = new ParsedFormatter();
        format.setWidth(Integer.parseInt(widthText.getText()));
        format.setAlignment(AlignmentType.valueOf(alignCombo.getText()));
        format.setBase(IntegerBaseType.valueOf(baseCombo.getText()));
        format.setPadChar(PadCharacterType.valueOf(padCombo.getText()));
        format.setPrecision(0);
        format.setSignIsSpaceWhenPadding(false);
        format.setUseUpperCaseHex(useUpperCaseButton.getSelection());
        format.setDisplayTypePrefix(useTypePrefixButton.getSelection());
        format.setUseSign(false);
        format.setType(FormatType.UNSIGNED_INT);
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
            formatter = "%u";
        }
        final ParsedFormatter parsed = ParsedFormatter
                .parseUnsignedIntFormatter(formatter);
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
        baseCombo.setText(formatter.getBase().toString());
        padCombo.setText(formatter.getPadChar().toString());
        alignCombo.setText(formatter.getAlignment().toString());
        widthText.setEnabled(true);
        baseCombo.setEnabled(true);
        padCombo.setEnabled(true);
        alignCombo.setEnabled(true);

        if (formatter.getBase().equals(IntegerBaseType.HEX)) {
            useUpperCaseButton.setSelection(formatter.isUseUpperCaseHex());
            useUpperCaseButton.setEnabled(true);
        } else {
            useUpperCaseButton.setSelection(false);
            useUpperCaseButton.setEnabled(false);
        }

        if (formatter.getBase().equals(IntegerBaseType.HEX)
                || formatter.getBase().equals(IntegerBaseType.OCTAL)) {
            useTypePrefixButton.setSelection(formatter.isDisplayTypePrefix());
            useTypePrefixButton.setEnabled(true);
        } else {
            useTypePrefixButton.setSelection(false);
            useTypePrefixButton.setEnabled(false);
        }
    }

    private void setFieldsToDefaults() {
        if (getComposite().isDisposed()) {
            return;
        }
        widthText.setText("0");
        baseCombo.setText(IntegerBaseType.DECIMAL.toString());
        padCombo.setText(PadCharacterType.NONE.toString());
        alignCombo.setText(AlignmentType.RIGHT.toString());
        widthText.setEnabled(true);
        baseCombo.setEnabled(true);
        padCombo.setEnabled(true);
        alignCombo.setEnabled(true);
        useUpperCaseButton.setSelection(false);
        useUpperCaseButton.setEnabled(false);
        useTypePrefixButton.setSelection(false);
        useTypePrefixButton.setEnabled(false);
        displayFormatError();
    }
}
