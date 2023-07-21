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
import jpl.gds.monitor.perspective.view.fixed.PositionChangeListener;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;


/**
 * DualPointFixedFieldConfiguration is a subclass of FixedFieldConfiguration that represents
 * the configuration of an fixed view field that requires two points to define its location
 * on the display.
 *
 */
public abstract class DualPointFixedFieldConfiguration extends FixedFieldConfiguration { 

	// XML attribute names
	/**
	 * XML x end attribute name
	 */
	public static final String X_END_TAG = "xEnd";
	
	/**
	 * XML y end attribute name
	 */
	public static final String Y_END_TAG = "yEnd";

	private int xEnd = ChillPoint.UNDEFINED;
	private int yEnd = ChillPoint.UNDEFINED;

	/**
	 * Creates a DualPointFixedFieldConfiguration with the given field type.
	 * 
	 * @param type the FixedFieldType of the field configuration to be created
	 */
	public DualPointFixedFieldConfiguration(final FixedFieldType type) {
		super(type);
	}

	/**
	 * Gets the secondary X coordinate of this field in the layout. Note that this coordinate must be
	 * interpreted using the proper coordinate system. For objects that are defined by two points,
	 * this is the X for the second point.
	 * 
	 * @see #getCoordinateSystem()
	 * 
	 * @return the X coordinate
	 */
	public ChillPoint getEndCoordinate() {
		final ChillPoint point = new ChillPoint(xEnd, yEnd, getCoordinateSystem());
		return point;
	}

	/**
	 * Sets the secondary X coordinate of this field in the layout. Note that this coordinate must be
	 * interpreted using the proper coordinate system. For objects that are defined by two points,
	 * this is the X for the second point.
	 * 
	 * @see #setCoordinateSystem(CoordinateSystemType loc)
	 * 
	 * @param end the X coordinate
	 */
	public void setEndCoordinate(final ChillPoint end) {
		xEnd = end.getX();
		yEnd = end.getY();
	}

	/**
	 * Sets the secondary X coordinate of this field in the layout. Note that this coordinate must be
	 * interpreted using the proper coordinate system. For objects that are defined by two points,
	 * this is the X for the second point.
	 * 
	 * @see #setCoordinateSystem(CoordinateSystemType loc)
	 * 
	 * @param end the X coordinate
	 */
	public void setXEnd(final int end) {
		xEnd = end;
	}

	/**
	 * Sets the secondary Y coordinate of this field in the layout. Note that this coordinate must be
	 * interpreted using the proper coordinate system. For objects that are defined by two points,
	 * this is the Y for the second point.
	 * 
	 * @see #setCoordinateSystem(CoordinateSystemType loc)
	 * 
	 * @param end the Y coordinate
	 */
	public void setYEnd(final int end) {
		yEnd = end;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);

		toAppend.append(X_END_TAG + "=\"" + xEnd + "\" ");
		toAppend.append(Y_END_TAG + "=\"" + yEnd + "\" ");	
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof DualPointFixedFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type DualPointFixedFieldConfiguration");
		}
		super.copyConfiguration(newConfig);
		((DualPointFixedFieldConfiguration)newConfig).xEnd = xEnd;
		((DualPointFixedFieldConfiguration)newConfig).yEnd = yEnd;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void notifyPositionListeners() {
		for (final PositionChangeListener l: positionListeners) {
			l.positionChanged(this, getStartCoordinate(), getEndCoordinate());
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void convertCoordinates(final CoordinateSystemType coordSystem, final int charWidth, final int charHeight) {
		if (getCoordinateSystem().equals(coordSystem)) {
			return;
		}
		super.convertCoordinates(coordSystem, charWidth, charHeight);

		final ChillPoint p = getEndCoordinate();
		if (coordSystem.equals(CoordinateSystemType.PIXEL)) {
			p.setX(p.getX() * charWidth);
			p.setY(p.getY() * charHeight);
		} else {
			p.setX(p.getX() / charWidth);
			p.setY(p.getY() / charHeight);
		}
		p.setCoordinateSystem(coordSystem);
		setCoordinateSystem(coordSystem);
		setEndCoordinate(p);
	}
}
