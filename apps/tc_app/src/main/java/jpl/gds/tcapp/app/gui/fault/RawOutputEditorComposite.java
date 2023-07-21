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
package jpl.gds.tcapp.app.gui.fault;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.exception.RawOutputFormatException;

/**
 * This class is a SWT composite that displays widgets for editing the raw data
 * to send
 * 
 *
 */
public final class RawOutputEditorComposite extends Composite implements
		FaultInjectorGuiComponent {
	private static final String FONT_NAME = "Courier";

    private static final String FAULT_PAGE_TITLE = "Raw Output Editor";
    private static final String FAULT_PAGE_DESC  = "Manually edit the data that will be transmitted.";

	private FaultInjectionState dataState = null;

	private Text rawOutputText = null;

	/**
	 * Constructor
	 * 
	 * @param parent the parent composite
	 */
	public RawOutputEditorComposite(final Composite parent) {
		super(parent, SWT.NONE);

		createControls();

		setTabList(new Control[] { this.rawOutputText });
		layout(true);
	}

	private void createControls() {
		setLayout(new FormLayout());

		// create Label
		final Label rawOutputLabel = new Label(this, SWT.LEFT);
		rawOutputLabel.setText("Data To Transmit (Hex):");
		final FormData rolFormData = new FormData();
		rolFormData.top = new FormAttachment(0, 10);
		rolFormData.left = new FormAttachment(0, 10);
		rolFormData.right = new FormAttachment(100, -10);
		rawOutputLabel.setLayoutData(rolFormData);

		// create Text
		this.rawOutputText = new Text(this, SWT.LEFT | SWT.BORDER | SWT.WRAP
				| SWT.MULTI | SWT.V_SCROLL);
		this.rawOutputText.setFont(getTextFieldFont());
		this.rawOutputText.setText("");

        this.rawOutputText.setEditable(true);

		final FormData rotFormData = new FormData();
		rotFormData.top = new FormAttachment(rawOutputLabel, 10);
		rotFormData.left = rolFormData.left;
		rotFormData.right = rolFormData.right;
		rotFormData.bottom = new FormAttachment(100, -10);
		this.rawOutputText.setLayoutData(rotFormData);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void destroy() {
		dispose();
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getDescription() {
		return (FAULT_PAGE_DESC);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getTitle() {
		return (FAULT_PAGE_TITLE);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public FaultInjectionState getCurrentState() {
		return (this.dataState);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setFromState(final FaultInjectionState state) {
		this.dataState = state;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void updateDisplay() throws FaultInjectorException {
		if (this.dataState.rawOutputHex != null) {
			final String formattedHex = BinOctHexUtility.formatHexString(
					this.dataState.rawOutputHex, 40);
			this.rawOutputText.setText(formattedHex);
			return;
		} else if (this.dataState.cltus != null) {
			final StringBuilder hex = new StringBuilder(1024);
			for (final ICltu cltu : this.dataState.cltus) {
				try {
					hex.append(BinOctHexUtility.toHexFromBytes(cltu.getPlopBytes()));
				} catch (final Exception e) {
					throw new FaultInjectorException(
							"Error transforming CLTUs to bytes: "
									+ e.getMessage(), e);
				}
			}

			final String formattedHex = BinOctHexUtility.formatHexString(
					hex.toString(), 40);
			this.rawOutputText.setText(formattedHex);
			return;
		}

		throw new IllegalStateException(
				getTitle()
						+ " display does not have enough information to construct the editor GUI.");
	}

	private String saveCurrentRawOutput() throws RawOutputFormatException {
		final String outputHex = GDR.removeWhitespaceFromString(this.rawOutputText
				.getText());

		if (!BinOctHexUtility.isValidHex(outputHex) ) {
			throw new RawOutputFormatException(
					"The raw output data is not valid hexadecimal data.");
		}

		return (outputHex);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void updateState() throws FaultInjectorException {
		try {
			this.dataState.rawOutputHex = saveCurrentRawOutput();
		} catch (final RawOutputFormatException e) {
			throw new FaultInjectorException(e.getMessage(), e);
		}
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public Font getTextFieldFont() {
		return (new Font(getDisplay(), new FontData(FONT_NAME, 14, SWT.NONE)));
	}
}
