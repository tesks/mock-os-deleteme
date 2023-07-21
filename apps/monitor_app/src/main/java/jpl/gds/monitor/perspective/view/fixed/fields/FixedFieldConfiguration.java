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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.ICompoundCondition;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.PositionChangeListener;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * The FixedFieldConfiguration class is used as the superclass for all field configurations
 * associated with the Fixed Layout view. These configuration objects then become part
 * of a fixed layout view definition in the perspective.
 */
public abstract class FixedFieldConfiguration implements IFixedFieldConfiguration {
	private int xStart;
	private int yStart;
	private boolean isStaticField = true;
	private FixedFieldType type;
	private CoordinateSystemType locationType = CoordinateSystemType.PIXEL;
	private ChillColor defaultBackground = new ChillColor(ColorName.WHITE);
	private ChillColor defaultForeground = new ChillColor(ColorName.BLACK);
	private ChillFont defaultFont = new ChillFont("Courier,10,NORMAL");
	private boolean defaultReverse;
	/**
	 * Array of listeners for positional changes
	 */
	protected final List<PositionChangeListener> positionListeners = new ArrayList<PositionChangeListener>();
	
	/**
	 * Condition for drawing this field (Optional)
	 */
    protected ICompoundCondition condition;

    /** The current application context */
	protected ApplicationContext appContext;

	/**
	 * Creates a FixedFieldConfiguration with the given field type.
	 * @param type the FixedFieldType of the field configuration to be created
	 */
	public FixedFieldConfiguration(final FixedFieldType type) {
		this.type = type;
	}
	
	/**
     * Creates a FixedFieldConfiguration with the given field type.
	 * @param appContext the current application context
     * @param type the FixedFieldType of the field configuration to be created
     */
	public FixedFieldConfiguration(final ApplicationContext appContext, final FixedFieldType type) {
		this.type = type;
		this.appContext = appContext;
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public ApplicationContext getApplicationContext() {
		return this.appContext;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public CoordinateSystemType getCoordinateSystem() {
		return locationType;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setCoordinateSystem(final CoordinateSystemType locationType) {
		this.locationType = locationType;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public FixedFieldType getType() {
		return type;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setType(final FixedFieldType type) {
		this.type = type;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillPoint getStartCoordinate() {
		final ChillPoint point = new ChillPoint(xStart, yStart, locationType);
		return point;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setStartCoordinate(final ChillPoint start) {
		xStart = start.getX();
		yStart = start.getY();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setXStart(final int start) {
		xStart = start;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setYStart(final int start) {
		yStart = start;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean isStatic() {
		return isStaticField;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setStatic(final boolean isStatic) {
		this.isStaticField = isStatic;
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    public ICompoundCondition getCondition() {
	    return condition;
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    public void setCondition(final ICompoundCondition condition) {
        this.condition = condition;
    }

	/**
	 * Gets the XML string containing the attributes to be attached to this configuration
	 * element and appends it to the given StringBuilder
	 * 
	 * @param toAppend StringBuilder to append to
	 */
	protected void getAttributeXML(final StringBuilder toAppend) {
		if(condition != null) {
		    toAppend.append(CONDITIONS_TAG + "=\"" + condition.toString() + "\" ");
		}
	    toAppend.append(X_START_TAG + "=\"" + xStart + "\" ");
		toAppend.append(Y_START_TAG + "=\"" + yStart + "\" ");
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String toXML() {
		final StringBuilder result = new StringBuilder();
		result.append("   <" + getFieldTag() + " ");
		getAttributeXML(result);
		result.append("/>\n");
		return result.toString();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public abstract String getFieldTag();

	/**
     * {@inheritDoc}
     */
	@Override
    public void setDefaults(final ChillColor background, final ChillColor foreground, final ChillFont font) {
		defaultBackground = background;
		defaultForeground = foreground;
		defaultFont = font;
		defaultReverse = defaultFont.getReverseFlag();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillColor getDefaultBackground() {
		return defaultBackground;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillColor getDefaultForeground() {
		return defaultForeground;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public ChillFont getDefaultFont() {
		return defaultFont;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public boolean getDefaultReverse() {
		return defaultReverse;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public abstract void setBuilderDefaults(CoordinateSystemType coordSystem);

	/**
     * {@inheritDoc}
     */
	@Override
    public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		newConfig.setStatic(isStaticField);
		newConfig.setCoordinateSystem(locationType);
		newConfig.setType(type);
		newConfig.setXStart(xStart);
		newConfig.setYStart(yStart);
		newConfig.setCondition(condition);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void notifyPositionListeners() {

		for (final PositionChangeListener l: positionListeners) {
			l.positionChanged(this, getStartCoordinate());
		}
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void addPositionChangeListener(final PositionChangeListener l) {
		positionListeners.add(l);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void removePositionChangeListener(final PositionChangeListener l) {
		positionListeners.remove(l);
	}

	/**
	 * Translates the given string to substitute escape codes for extended Unicode
	 * characters so the string can be written to an XML file.
	 * @param text String to translate
	 * @return translated string
	 */
	protected String getUnicodeStringForXml(String text) {
		final StringBuilder sb = new StringBuilder();
		text = StringEscapeUtils.escapeXml(text);
		final int len = text.length();
		for (int i= 0; i < len; i++) {
			if (text.charAt(i) > 0x7F) {
				sb.append("&#" + Integer.toString(text.charAt(i)) + ";");
			} else {
				sb.append(text.charAt(i));
			}
		}
		return sb.toString();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void convertCoordinates(final CoordinateSystemType coordSystem, final int charWidth, final int charHeight) {
		if (getCoordinateSystem().equals(coordSystem)) {
			return;
		}
		final ChillPoint p = getStartCoordinate();
		if (coordSystem.equals(CoordinateSystemType.PIXEL)) {
			p.setX(p.getX() * charWidth);
			p.setY(p.getY() * charHeight);
		} else {
			p.setX(p.getX() / charWidth);
			p.setY(p.getY() / charHeight);
		}
		p.setCoordinateSystem(coordSystem);
		setCoordinateSystem(coordSystem);
		setStartCoordinate(p);
	}
}
