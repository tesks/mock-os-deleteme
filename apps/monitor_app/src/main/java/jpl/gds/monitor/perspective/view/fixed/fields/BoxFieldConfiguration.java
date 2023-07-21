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
import jpl.gds.monitor.perspective.view.fixed.FontConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.LineConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.LineStyle;
import jpl.gds.monitor.perspective.view.fixed.TextConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * BoxFieldConfiguration is a subclass of FixedFieldConfiguration that
 * represents a drawn box on a fixed layout view. A box is defined by two
 * points, the upper left and lower right corners. It has foreground and
 * background colors, an optional title, and a font for the title. It supports
 * transparency, but transparency and setting of a background color are mutually
 * exclusive. In addition the line thickness and line style of the box border
 * can be configured.
 * 
 */
public class BoxFieldConfiguration extends DualPointFixedFieldConfiguration 
implements TwoColorConfigSupport, FontConfigSupport, TransparencyConfigSupport, 
LineConfigSupport, TextConfigSupport {

	// XML tags and attribute names
	/**
	 * XML box element name
	 */
	public static final String BOX_TAG = "Box";
	
	/**
	 * XML title attribute name
	 */
	public static final String TITLE_TAG = "title";
	
	/**
	 * XML font attribute name
	 */
	public static final String FONT_TAG = "font";
	
	/**
	 * XML line thickness attribute name
	 */
	public static final String LINE_THICKNESS_TAG = "lineThickness";
	
	/**
	 * XML transparency flag name
	 */
	public static final String TRANSPARENT_TAG = "transparent";
	
	/**
	 * XML background color attribute name
	 */
	public static final String BACKGROUND_TAG = "background";
	
	/**
	 * XML foreground color attribute name
	 */
	public static final String FOREGROUND_TAG = "foreground";
	
	/**
	 * XML line style attribute name
	 */
	public static final String LINE_STYLE_TAG = "lineStyle";

	private String title;  // Optional
	private int lineThickness = 1; // Optional
	private LineStyle lineStyle = LineStyle.SOLID;  // Optional
	private boolean transparent;  // Optional
	private ChillColor background;  // Optional
	private ChillColor foreground; // Optional
	private ChillFont font; // Optional
	private boolean isDefaultFont = true;
	private boolean isDefaultColors = true;

	/**
	 * Creates a new BoxFieldConfiguration.
	 */
	public BoxFieldConfiguration() {
		super(FixedFieldType.BOX);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TextConfigSupport#getText()
	 */
	@Override
    public String getText() {
		return title;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TextConfigSupport#setText(java.lang.String)
	 */
	@Override
    public void setText(final String title) {
		this.title = title;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#getFont()
	 */
	@Override
    public ChillFont getFont() {
		return font;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#setFont(jpl.gds.shared.swt.types.ChillFont)
	 */
	@Override
    public void setFont(final ChillFont font) {
		this.font = font;
	}

	/**
	 * Returns true if the font was set and it is desired to reverse the foreground/background colors
	 * when drawing the title field.
	 * 
	 * @return true to reverse video the text, false to not
	 */
	public boolean reverseFont() {

		if (font != null) {
			return font.getReverseFlag();
		}

		return false;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.LineConfigSupport#getLineThickness()
	 */
	@Override
    public int getLineThickness() {
		return lineThickness;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.LineConfigSupport#setLineThickness(int)
	 */
	@Override
    public void setLineThickness(final int lt) {
		lineThickness = lt;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport#isTransparent()
	 */
	@Override
    public boolean isTransparent() {
		return transparent;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport#setTransparent(boolean)
	 */
	@Override
    public void setTransparent(final boolean tr) {
		transparent = tr;
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
    public void setBackground(final ChillColor fc) {
		background = fc;
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
	 * @see jpl.gds.monitor.perspective.view.fixed.LineConfigSupport#getLineStyle()
	 */
	@Override
    public LineStyle getLineStyle() {
		return lineStyle;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.LineConfigSupport#setLineStyle(jpl.gds.monitor.perspective.view.fixed.LineStyle)
	 */
	@Override
    public void setLineStyle(final LineStyle ls) {
		lineStyle = ls;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return BOX_TAG;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.DualPointFixedFieldConfiguration#getAttributeXML(java.lang.StringBuilder)
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);

		if (foreground != null) {
			toAppend.append(FOREGROUND_TAG + "=\"" + foreground.getRgbString() + "\" ");
		}
		if (title != null) {
			toAppend.append(TITLE_TAG + "=\"" + this.getUnicodeStringForXml(title) + "\" ");
		}
		if (font != null) {
			toAppend.append(FONT_TAG + "=\"" + font.getFontString() + "\" ");
		}
		if (lineThickness != -1) {
			toAppend.append(LINE_THICKNESS_TAG + "=\"" + lineThickness + "\" ");
		}

		toAppend.append(TRANSPARENT_TAG + "=\"" + transparent + "\" ");

		if (background != null) {
			toAppend.append(BACKGROUND_TAG + "=\"" + background.getRgbString() + "\" ");
		}
		if(lineStyle != null)
		{
			toAppend.append(LINE_STYLE_TAG + "=\"" + lineStyle + "\" ");
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		ChillPoint start = null;
		ChillPoint end = null;
		if (coordSystem.equals(CoordinateSystemType.CHARACTER)) {
			start = new ChillPoint(1,1,coordSystem);
			end = new ChillPoint(10,10, coordSystem);
		} else {
			start = new ChillPoint(10,10,coordSystem);
			end = new ChillPoint(110,110, coordSystem);
		}
		this.setStartCoordinate(start);
		this.setEndCoordinate(end);
		this.setLineThickness(1);
		this.setLineStyle(LineStyle.SOLID);
		this.setText("Box");
		this.setTransparent(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof BoxFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type BoxFieldConfiguration");
		}
		final BoxFieldConfiguration boxConfig = (BoxFieldConfiguration)newConfig;
		super.copyConfiguration(newConfig);
		boxConfig.title = title;
		boxConfig.lineStyle = lineStyle;
		boxConfig.lineThickness = lineThickness;
		boxConfig.transparent = transparent;
		if (background != null) {
			boxConfig.background = new ChillColor(background);
		}
		if (foreground != null) {
			boxConfig.foreground = new ChillColor(foreground);
		}
		if (font != null) {
			boxConfig.font = new ChillFont(font);
		}
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
		isDefaultColors = foreground == null && background == null ? true : false;
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
