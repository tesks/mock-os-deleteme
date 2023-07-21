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
import jpl.gds.monitor.perspective.view.fixed.LineConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.LineStyle;
import jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * LineFieldConfiguration is a subclass of FixedFieldConfiguration that
 * represents a drawn line on a fixed layout view. It supports a foreground
 * color, a line thickness, and a line style.
 * 
 */
public class LineFieldConfiguration extends DualPointFixedFieldConfiguration 
implements OneColorConfigSupport, LineConfigSupport {

	// XML element and attribute names
	/**
	 * XML line element name
	 */
	public static final String LINE_TAG = "Line";
	
	/**
	 * XML foreground attribute name
	 */
	public static final String FOREGROUND_TAG = "foreground";
	
	/**
	 * XML line thickness attribute name
	 */
	public static final String LINE_THICKNESS_TAG = "lineThickness";
	
	/**
	 * XML line style attribute name
	 */
	public static final String LINE_STYLE_TAG = "lineStyle";

	private ChillColor foreground;	// Optional
	private int lineThickness = 1;	// Optional
	private LineStyle lineStyle = LineStyle.SOLID; // Optional
	private boolean isDefaultColors;

	/**
	 * Creates a new LineFieldConfiguration.
	 */
	public LineFieldConfiguration() {
		super(FixedFieldType.LINE);
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
	 * @{inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return LINE_TAG;
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
		if (lineThickness != -1) {
			toAppend.append(LINE_THICKNESS_TAG + "=\"" + lineThickness + "\" ");
		}
		if(lineStyle != null)
		{
			toAppend.append(LINE_STYLE_TAG + "=\"" + lineStyle + "\" ");
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		ChillPoint start = null;
		ChillPoint end = null;
		if (coordSystem.equals(CoordinateSystemType.CHARACTER)) {
			start = new ChillPoint(2,5,coordSystem);
			end = new ChillPoint(2,10, coordSystem);
		} else {
			start = new ChillPoint(10,10,coordSystem);
			end = new ChillPoint(10,150, coordSystem);
		}
		this.setStartCoordinate(start);
		this.setEndCoordinate(end);
		this.setLineThickness(1);
		this.setLineStyle(LineStyle.SOLID);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof LineFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type LineFieldConfiguration");
		}
		final LineFieldConfiguration lineConfig = (LineFieldConfiguration)newConfig;
		super.copyConfiguration(newConfig);
		lineConfig.lineStyle = lineStyle;
		lineConfig.lineThickness = lineThickness;
		if (foreground != null) {
			lineConfig.foreground = new ChillColor(foreground);
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors()
	 */
	@Override
    public boolean usesDefaultColors() {
		isDefaultColors = foreground == null ? true : false;
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
