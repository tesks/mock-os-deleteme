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
package jpl.gds.monitor.canvas;

import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.canvas.support.TextSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TextFieldConfiguration;

/**
 * This CanvasElement is a simple text field.
 *
 */
public class TextElement extends AbstractTextElement implements TextSupport {

	/**
	 * The default selection priority for this CanvasElement.
	 */
	private static final int SELECTION_PRIORITY = 1;

	/**
	 * Creates a TextElement with the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 */
	public TextElement(final Canvas parent) {
		super(parent, FixedFieldType.TEXT);
		this.setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * Creates a TextElement with the given fixed view field configuration and 
	 * parent canvas.
	 * 
	 * @param parent the parent Canvas widget
	 * 
	 * @param textConfig the FixedFieldConfiguration object for this element 
	 * from the perspective
	 */
	public TextElement(final Canvas parent, final IFixedFieldConfiguration textConfig) {
		super(parent, textConfig);
		updateFieldsFromConfig();
		this.setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.AbstractTextElement#updateFieldsFromConfig()
	 */
	@Override
	protected void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();

		final TextFieldConfiguration textConfig = (TextFieldConfiguration)fieldConfig;

		this.setText(textConfig.getText());
	}
}
