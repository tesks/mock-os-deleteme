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
package jpl.gds.monitor.perspective.view.fixed.fields;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.TextConfigSupport;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * TextFieldConfiguration is a subclass of FixedFieldConfiguration that
 * represents a simple text field on a fixed layout view.
 */
public class TextFieldConfiguration extends AbstractTextFieldConfiguration
implements TextConfigSupport {

	// XML tags and attribute names
	/**
	 * XML text element name
	 */
	public static final String TEXT_TAG = "Text";
	
	/**
	 * XML text's initial value attribute name
	 */
	public static final String INITIAL_VALUE_TAG = "text";

	private String text;
	private boolean isDefaultFont = true;
	private boolean isDefaultColors;

	/**
	 * Creates a new TextFieldConfiguration.
	 */
	public TextFieldConfiguration() {
		super(FixedFieldType.TEXT);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TextConfigSupport#getText()
	 */
	@Override
    public String getText() {
		return text;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TextConfigSupport#setText(java.lang.String)
	 */
	@Override
    public void setText(final String text) {
		this.text = text;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return TEXT_TAG;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#getAttributeXML(java.lang.StringBuilder)
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);
		toAppend.append(INITIAL_VALUE_TAG + "=\"");
		toAppend.append(this.getUnicodeStringForXml(text));
		toAppend.append("\" ");
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#setBuilderDefaults(jpl.gds.shared.swt.types.CoordinateSystemType)
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		super.setBuilderDefaults(coordSystem);
		this.setText("Text");
		this.setFormat("%s");
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof TextFieldConfiguration)) {
			throw new IllegalArgumentException(
			"Object for copy is not of type TextFieldConfiguration");
		}
		final TextFieldConfiguration textConfig = (TextFieldConfiguration) newConfig;
		super.copyConfiguration(newConfig);
		textConfig.text = text;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#usesDefaultFont()
	 */
	@Override
    public boolean usesDefaultFont() {
		isDefaultFont = font == null ? true : false;
		return isDefaultFont;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#usesDefaultFont(boolean)
	 */
	@Override
    public void usesDefaultFont(final boolean usesDefaultFont) {
		this.isDefaultFont = usesDefaultFont;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors()
	 */
	@Override
    public boolean usesDefaultColors() {
		isDefaultColors = background == null && foreground == null ? true
				: false;
		return isDefaultColors;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors(boolean)
	 */
	@Override
    public void usesDefaultColors(final boolean usesDefaultColors) {
		this.isDefaultColors = usesDefaultColors;
	}
}
