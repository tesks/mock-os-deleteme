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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * BorderedLabel is exactly what it says. A composite that contains a label with
 * a border around it.
 * 
 */
public class BorderedLabel extends Composite {
	private final Label label;

	/**
	 * Constructor.
	 * 
	 * @param parent the parent composite
	 * @param style the SWT style flags for the composite. SWT.BORDER will be added.
	 * @param labelHorizonalAlign horizontal alignment of the label in the composite, e.g., SWT.LEFT
	 * @param labelVerticalAlign vertical alignment of the label in the composite, e.g,, SWT.CENTER
	 */
	public BorderedLabel(Composite parent, int style, int labelHorizonalAlign, int labelVerticalAlign) {
		super(parent, style | SWT.BORDER);
		
		this.setLayout(new GridLayout());
		
		this.label = new Label(this, SWT.RIGHT);

		GridData gd = new GridData(labelHorizonalAlign, labelVerticalAlign, true, true);
		this.label.setLayoutData(gd);
	}
	
	/**
	 * Gets the actual label widget.
	 * 
	 * @return Label
	 */
	public Label getLabelWidget() {
		return label;
	}
}
