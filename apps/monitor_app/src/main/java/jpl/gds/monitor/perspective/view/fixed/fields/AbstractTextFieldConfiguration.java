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

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.FontConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.FormatConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * AbstractTextFieldConfiguration is a FixedFieldConfiguration that contains attributes commonly
 * shared by all text fields on a fixed layout view. These include font, foreground color,
 * background color, formatter, and transparency.
 */
public abstract class AbstractTextFieldConfiguration extends FixedFieldConfiguration 
implements TwoColorConfigSupport, FontConfigSupport, TransparencyConfigSupport, FormatConfigSupport {


	/**
	 * XML foreground attribute name
	 */
	public static final String FOREGROUND_TAG = "foreground";
	
	/**
	 * XML background attribute name
	 */
	public static final String BACKGROUND_TAG = "background";
	
	/**
	 * XML font attribute name
	 */
	public static final String FONT_TAG = "font";
	
	/**
	 * XML transparency flag name
	 */
	public static final String TRANSPARENT_TAG = "transparent";
	
	/**
	 * XML format attribute name
	 */
	public static final String FORMAT_TAG = "format";

	/**
	 * Text field foreground color
	 */
	protected ChillColor foreground;
	
	/**
	 * Text field background color
	 */
	protected ChillColor background;
	
	/**
	 * Text field font
	 */
	protected ChillFont font;
	
	/**
	 * Text field transparency flag
	 */
	protected boolean isTransparentField;
	
	/**
	 * Text field sprintf formatter
	 */
	protected String format;

	/**
	 * Creates a new AbstractTextFieldConfiguration of the given field type.
	 *
	 * @param type the type of text field this is
	 */
	public AbstractTextFieldConfiguration(final FixedFieldType type) {
		super(type);
	}
	
	/**
	 * Creates a new AbstractTextFieldConfiguration of the given field type.
	 * @param appContext the current application context
	 *
	 * @param type the type of text field this is
	 */
	public AbstractTextFieldConfiguration(final ApplicationContext appContext, final FixedFieldType type) {
		super(appContext, type);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport#getBackground()
	 */
	@Override
    public ChillColor getBackground() {
		return background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport#setBackground(jpl.gds.shared.swt.types.ChillColor)
	 */
	@Override
    public void setBackground(final ChillColor background) {
		this.background = background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#getForeground()
	 */
	@Override
    public ChillColor getForeground() {
		return foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#setForeground(jpl.gds.shared.swt.types.ChillColor)
	 */
	@Override
    public void setForeground(final ChillColor foreground) {
		this.foreground = foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#getFont()
	 */
	@Override
    public ChillFont getFont()
	{
		return font;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#setFont(jpl.gds.shared.swt.types.ChillFont)
	 */
	@Override
    public void setFont(final ChillFont font)
	{
		this.font = font;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport#isTransparent()
	 */
	@Override
    public boolean isTransparent() {
		return isTransparentField;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport#setTransparent(boolean)
	 */
	@Override
    public void setTransparent(final boolean isTransparent) {
		this.isTransparentField = isTransparent;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FormatConfigSupport#getFormat()
	 */
	@Override
    public String getFormat()
	{
		return format;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FormatConfigSupport#setFormat(java.lang.String)
	 */
	@Override
    public void setFormat(final String format)
	{
		this.format = format;
	}

	/**
	 * Returns true if the font was set and it is desired to reverse the foreground/background colors
	 * when drawing the text field.
	 * 
	 * @return true to reverse video the text, false to not
	 */
	public Boolean reverseFont() {

		if (font != null) {
			return font.getReverseFlag();
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);
		if (background != null) {
			toAppend.append(BACKGROUND_TAG + "=\""
					+ background.getRgbString() + "\" ");
		}
		if (foreground != null) {
			toAppend.append(FOREGROUND_TAG + "=\""
					+ foreground.getRgbString() + "\" ");
		}
		if (font != null) {
			toAppend.append(FONT_TAG + "=\"" + font.getFontString() + "\" ");
		}
		toAppend.append(TRANSPARENT_TAG + "=\""
				+ isTransparentField + "\" ");
		if(format != null)
		{
			toAppend.append(FORMAT_TAG + "=\""
					+ this.getUnicodeStringForXml(format) + "\" ");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		setStartCoordinate(new ChillPoint(5,5,coordSystem));
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof AbstractTextFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type AbstractTextFieldConfiguration");
		}
		super.copyConfiguration(newConfig);
		final AbstractTextFieldConfiguration textConfig = (AbstractTextFieldConfiguration)newConfig;
		textConfig.format = format;
		textConfig.isTransparentField = isTransparentField;
		if (background != null) {
			textConfig.background = new ChillColor(background);
		}
		if (foreground != null) {
			textConfig.foreground = new ChillColor(foreground);
		}
		if (font != null) {
			textConfig.font = new ChillFont(font);
		}	
	}
}
