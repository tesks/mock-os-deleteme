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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.DualPointFixedFieldConfiguration;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This abstract class is used as the base class for all CanvasElements that
 * are defined by two location points (x,y) on the canvas.
 */
public abstract class DualCoordinateCanvasElement extends 
SingleCoordinateCanvasElement {
	/**
	 * The second coordinate of the pair required to define
	 * dual coordinate objects
	 */
	protected ChillPoint endPoint = new ChillPoint(
	        ChillPoint.UNDEFINED, 
	        ChillPoint.UNDEFINED, 
	        CoordinateSystemType.PIXEL);

	/**
	 * Constructs a DualCoordinateCanvasElement with the given Canvas as parent
	 * and the given fixed view field type. This is generally used for 
	 * constructing new CanvasElements in the builder.
	 * 
	 * @param parent the parent Canvas object
	 * @param type the FixedFieldType of this CanvasElement
	 */
	public DualCoordinateCanvasElement(final Canvas parent, final FixedFieldType type) {
		super(parent, type);
	}

	/**
	 * Constructs a DualCoordinateCanvasElement with the given Canvas as parent
	 * and the given fixed view field configuration. This is generally used 
	 * for constructing CanvasElements that already exist in the user 
	 * perspective.
	 * 
	 * @param parent the parent Canvas object
	 * @param config the FixedFieldConfiguration of this CanvasElement
	 */
	public DualCoordinateCanvasElement(
	        final Canvas parent, final IFixedFieldConfiguration config) {
		super(parent, config);
		updateFieldsFromConfig();
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#getEndLocation()
	 */
	@Override
	public ChillPoint getEndLocation() {
		if (endPoint.getX() != ChillPoint.UNDEFINED) {
			endPoint.setCoordinateSystem(this.getCoordinateSystem());
			return endPoint;
		} else {
			return null;
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#setEndLocation(jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
	public void setEndLocation(final ChillPoint pt) {
		endPoint = pt;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#moveAndResize(jpl.gds.shared.swt.types.ChillPoint, jpl.gds.shared.swt.types.ChillPoint, jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
	public void moveAndResize(
	        final ChillPoint start, final ChillPoint end, final ChillPoint maxCoords) {
		if (start != null && 
		        (start.getX() < 0 || start.getX() > maxCoords.getX())) {
			return;
		}
		if (start != null && 
		        (start.getY() < 0 || start.getY() > maxCoords.getY())) {
			return;
		}
		if (end != null && (end.getX() < 0 || end.getX() > maxCoords.getX())) {
			return;
		}
		if (end != null && (end.getY() < 0 || end.getY() > maxCoords.getY())) {
			return;
		}
		super.moveAndResize(start, end, maxCoords);
		if (end == null) {
			return;
		}
		endPoint.setX(end.getX());
		endPoint.setY(end.getY());

		if (fieldConfig != null) {
			final ChillPoint endCoord = 
			    ((DualPointFixedFieldConfiguration)fieldConfig).
			    getEndCoordinate();
			endCoord.setX(end.getX());
			endCoord.setY(end.getY());

			((DualPointFixedFieldConfiguration)fieldConfig).
			setEndCoordinate(endCoord);
			fieldConfig.notifyPositionListeners();
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#nudge(int, jpl.gds.shared.swt.types.ChillPoint, int)
	 */
	@Override
	public void nudge(final int direction, final ChillPoint maxCoords, final int amount) {
		int x = 0;
		int y = 0;
		int ex = 0;
		int ey = 0;

		switch (direction) {
		case SWT.ARROW_UP:
			if (startPoint.getY() - amount < 0 || 
			        endPoint.getY() - amount < 0) {
				return;
			}
			x = startPoint.getX();
			y = Math.max(0, startPoint.getY() - amount);
			ex = endPoint.getX();
			ey = Math.max(0, endPoint.getY() - amount);
			break;
		case SWT.ARROW_DOWN:
			if (startPoint.getY() + amount > maxCoords.getY() || 
			        endPoint.getY() + amount > maxCoords.getY()) {
				return;
			}
			x = startPoint.getX();
			y = Math.min(maxCoords.getY(), startPoint.getY() + amount);
			ex = endPoint.getX();
			ey = Math.min(maxCoords.getY(), endPoint.getY() + amount);
			break;
		case SWT.ARROW_LEFT:
			if (startPoint.getX() - amount < 0 || 
			        endPoint.getX() - amount < 0) {
				return;
			}
			y = startPoint.getY();
			x = Math.max(0, startPoint.getX() - amount);
			ey = endPoint.getY();
			ex = Math.max(0, endPoint.getX() - amount);
			break;
		case SWT.ARROW_RIGHT:
			if (startPoint.getX() + amount > maxCoords.getX() || 
			        endPoint.getX() + amount > maxCoords.getX()) {
				return;
			}
			y = startPoint.getY();
			x = Math.min(maxCoords.getX(), startPoint.getX() + amount);
			ey = endPoint.getY();
			ex = Math.min(maxCoords.getX(), endPoint.getX() + amount);
			break;
		}

		moveAndResize(new ChillPoint(x,y, getCoordinateSystem()), 
		        new ChillPoint(ex, ey, getCoordinateSystem()), maxCoords);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#copy(boolean, int, int)
	 */
	@Override
	public CanvasElement copy(
	        final boolean newCoordinates, final int offsetX, final int offsetY) {
		final CanvasElement newElem = super.copy(newCoordinates, offsetX, offsetY);

		if (newCoordinates) {
			final DualPointFixedFieldConfiguration newConfig = 
			    (DualPointFixedFieldConfiguration)newElem.
			    getFieldConfiguration();
			final ChillPoint end = newConfig.getEndCoordinate();
			end.setX(end.getX() + offsetX);
			end.setY(end.getY() + offsetY);
			newConfig.setEndCoordinate(end);
			newElem.setFieldConfiguration(newConfig);
		}
		return newElem;
	}
}
