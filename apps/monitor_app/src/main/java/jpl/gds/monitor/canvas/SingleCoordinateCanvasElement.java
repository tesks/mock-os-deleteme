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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.canvas.support.SingleCoordinateSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldConfigurationFactory;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This abstract class is used as the base class for all CanvasElements that
 * are defined by one primary location point (x,y) on the canvas. Also tracks
 * most of the data associated with canvas elements, such as last bounds,
 * character height/width, coordinate system type, etc.
 *
 */
public abstract class SingleCoordinateCanvasElement implements 
CanvasElement, SingleCoordinateSupport {

	/** X,Y coordinate of this canvas element. */
	protected ChillPoint startPoint;
	/** Parent canvas reference */
	protected Canvas parent;
	/** Current pixel width of a character for this element. */
	protected int characterWidth = -1;
	/** Current pixel height of a character for this element. */
	protected int characterHeight = -1;
	/** Coordinate system used to place this element. */
	protected CoordinateSystemType locationType;
	/** Last computed rectangular bounds of this element. */ 
	protected Rectangle lastBounds;
	/** Priority of this element for selection */
	protected int selectionPriority = -1;
	/** Field type of this element */
	protected FixedFieldType fieldType;
	/** Indicates whether this is a static (non-changing) or dynamic element */
	protected boolean isStaticField = true;
	/** Fixed field configuration object for this element */
	protected IFixedFieldConfiguration fieldConfig;
	/** Flag indicating whether the element is being edited. */
	protected boolean isEditMode;
	/** The set of selection ("grab") handles for this element */
	protected Map<SelectionHandleId, ElementHandle> handles = 
	    new HashMap<SelectionHandleId,ElementHandle>(
	            SelectionHandleId.MAX_HANDLES);
	/** Saved graphics context attributes for this element. */
	protected GCSettings gcSettings = new GCSettings();
	/** Flag indicating element is currently selected.*/
	protected boolean selected;
	
	/** Flag to determine whether or not this element should be drawn on the 
	   canvas (based on conditional) */
    protected boolean displayMe;
    /** Flag to determine whether or not this element was previously drawn on the 
	   canvas (based on conditional) */
    protected boolean previousDisplayMe;

	/**
	 * Constructs a SingleCoordinateCanvasElement with the given Canvas as 
	 * parent and the given fixed view field type. This is generally used for 
	 * constructing new CanvasElements in the builder.
	 * 
	 * @param parent the parent Canvas object
	 * @param textConfig the FixedFieldType of this CanvasElement
	 */
	public SingleCoordinateCanvasElement(final Canvas parent, final FixedFieldType type) {
		this.parent = parent;
		fieldType = type;
		locationType = CoordinateSystemType.PIXEL;
		for (final SelectionHandleId id: SelectionHandleId.values()) {
			if (id.equals(SelectionHandleId.HANDLE_NONE)) {
				continue;
			} else {
				handles.put(id, new ElementHandle(0,0));
			}
		}
	}

	/**
	 * Constructs a SingleCoordinateCanvasElement with the given Canvas as 
	 * parent and the given fixed view field configuration. This is generally 
	 * used for constructing CanvasElements that already exist in the user 
	 * perspective.
	 * 
	 * @param parent the parent Canvas object
	 * @param config the FixedFieldConfiguration of this CanvasElement
	 */
	public SingleCoordinateCanvasElement(final Canvas parent, 
	        final IFixedFieldConfiguration config) {
		this(parent, config.getType());
		fieldConfig = config;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getFieldConfiguration()
	 */
	@Override
	public IFixedFieldConfiguration getFieldConfiguration() {
		return fieldConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFieldConfiguration(final IFixedFieldConfiguration fieldConfig) {
		this.fieldConfig = fieldConfig;
		updateFieldsFromConfig();
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getFieldType()
	 */
	@Override
	public FixedFieldType getFieldType() {
		return fieldType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#setFieldType(jpl.gds.monitor.perspective.view.fixed.FixedFieldType)
	 */
	@Override
	public void setFieldType(final FixedFieldType fieldType) {
		this.fieldType = fieldType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getCoordinateSystem()
	 */
	@Override
	public CoordinateSystemType getCoordinateSystem() {
		return locationType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#setCoordinateSystem(jpl.gds.shared.swt.types.CoordinateSystemType)
	 */
	@Override
	public void setCoordinateSystem(
			final CoordinateSystemType locationType) {
		this.locationType = locationType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getLastBounds()
	 */
	@Override
	public Rectangle getLastBounds() {
		return lastBounds;
	}

	/**
	 * Establishes the last drawn bounds of this element in the canvas and
	 * resets the selection handles. These coordinates must be in pixels.
	 * 
	 * @param sx absolute x starting point of the bounding rectangle
	 * @param sy absolute y starting point of the bounding rectangle
	 * @param ex absolute x of the ending point of the bounding rectangle
	 * @param ey absolute y of the ending point of the bounding rectangle
	 */
	protected void setLastBounds(final int sx, final int sy, final int ex, final int ey) {
		final int xLength = Math.max(4,Math.abs(ex - sx));
		final int yLength = Math.max(4, Math.abs(ey - sy));
		final int lowestX = Math.min(sx, ex);
		final int lowestY = Math.min(sy, ey);

		lastBounds = new Rectangle(lowestX, lowestY, xLength, yLength);
		setHandles(sx, sy, ex, ey);
	}
	/**
	 * Resets the selection handles based upon the given x,y starting and 
	 * ending points of the drawing bounds.
	 * 
	 * @param sx absolute x starting point of the bounding rectangle
	 * @param sy absolute y starting point of the bounding rectangle
	 * @param ex absolute x of the ending point of the bounding rectangle
	 * @param ey absolute y of the ending point of the bounding rectangle
	 */
	protected void setHandles(final int sx, final int sy, final int ex, final int ey) {
		handles.get(SelectionHandleId.HANDLE_TOP_LEFT).setCenterX(sx);
		handles.get(SelectionHandleId.HANDLE_TOP_LEFT).setCenterY(sy);
		handles.get(SelectionHandleId.HANDLE_TOP_RIGHT).setCenterX(ex);
		handles.get(SelectionHandleId.HANDLE_TOP_RIGHT).setCenterY(sy);
		handles.get(SelectionHandleId.HANDLE_BOTTOM_LEFT).setCenterX(sx);
		handles.get(SelectionHandleId.HANDLE_BOTTOM_LEFT).setCenterY(ey);		
		handles.get(SelectionHandleId.HANDLE_BOTTOM_RIGHT).setCenterX(ex);
		handles.get(SelectionHandleId.HANDLE_BOTTOM_RIGHT).setCenterY(ey);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getSelectionPriority()
	 */
	@Override
	public int getSelectionPriority() {
		return selectionPriority;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#setSelectionPriority(int)
	 */
	@Override
	public void setSelectionPriority(final int selectionPriority) {
		this.selectionPriority = selectionPriority;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#setStartLocation(jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
	public void setStartLocation(final ChillPoint pt) {
		startPoint = pt;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#isStatic()
	 */
	@Override
	public boolean isStatic() {
		return isStaticField;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#setStatic(boolean)
	 */
	@Override
	public void setStatic(final boolean isStatic) {
		this.isStaticField = isStatic;
	}

	/**
	 * Maps the given X coordinate, which may be in pixels or characters, to 
	 * an absolute pixel location, depending on whether this object is 
	 * configured to supply coordinates in pixels or characters.
	 * 
	 * @param x the x coordinate to map
	 * @param gc the GC (Graphics Context) we are drawing on
	 * @return the absolute x coordinate for drawing, in pixels
	 */
	protected int getXCoordinate(final int x, final GC gc) {
		if (characterWidth == -1 || isEditMode) {
			characterWidth = SWTUtilities.getFontCharacterWidth(gc);
		}
		if (locationType == CoordinateSystemType.CHARACTER) {
			return x * characterWidth;
		} else {
			return x;
		}
	}

	/**
	 * Maps the given Y coordinate, which may be in pixels or characters, to 
	 * an absolute pixel location, depending on whether this object is 
	 * configured to supply coordinates in pixels or characters.
	 * 
	 * @param y the y coordinate to map
	 * @param gc the GC (Graphics Context) we are drawing on
	 * @return the absolute y coordinate for drawing, in pixels
	 */
	protected int getYCoordinate(final int y, final GC gc) {
		if (characterHeight == -1 || isEditMode) {
			characterHeight = SWTUtilities.getFontCharacterHeight(gc);
		}
		if (locationType == CoordinateSystemType.CHARACTER) {
			return y * characterHeight;
		} else {
			return y;
		}
	}

	/**
	 * Updates this CanvasElement from the current FixedFieldConfiguration.
	 */
	protected void updateFieldsFromConfig() {
		this.setStartLocation(fieldConfig.getStartCoordinate());
		this.setCoordinateSystem(fieldConfig.getCoordinateSystem());
		this.setStatic(fieldConfig.isStatic());
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getStartLocation()
	 */
	@Override
	public ChillPoint getStartLocation() {
		return startPoint;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#enableEditMode(boolean)
	 */
	@Override
	public void enableEditMode(final boolean enable) {
		isEditMode = enable;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getEndLocation()
	 */
	@Override
	public ChillPoint getEndLocation() {
		return null;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#setEndLocation(jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
	public void setEndLocation(final ChillPoint pt) {

	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#drawHandles(org.eclipse.swt.graphics.GC)
	 */
	@Override
	public void drawHandles(final GC gc) {
		for (final ElementHandle handle: handles.values()) {
			handle.drawHandle(gc);
		}
	} 

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getHandleIdForPoint(org.eclipse.swt.graphics.Point)
	 */
	@Override
	public SelectionHandleId getHandleIdForPoint(final Point pt) {

		for (final SelectionHandleId handle: handles.keySet()) {
			if (handles.get(handle).containsPoint(pt)) {
				return handle;
			}
		}
		return SelectionHandleId.HANDLE_NONE;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#getHandle(jpl.gds.monitor.canvas.SelectionHandleId)
	 */
	@Override
	public ElementHandle getHandle(final SelectionHandleId id) {
		return handles.get(id);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#isShapeMorphable()
	 */
	@Override
	public boolean isShapeMorphable() {
		return false;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#moveAndResize(jpl.gds.shared.swt.types.ChillPoint, jpl.gds.shared.swt.types.ChillPoint, jpl.gds.shared.swt.types.ChillPoint)
	 */
	@Override
	public void moveAndResize(
	        final ChillPoint start, final ChillPoint end, final ChillPoint maxCoords) {
		if (start == null) {
			return;
		}
		if (start.getX() < 0 || start.getX() > maxCoords.getX()) {
			return;
		}
		if (start.getY() < 0 || start.getY() > maxCoords.getY()) {
			return;
		}
		startPoint.setX(start.getX());
		startPoint.setY(start.getY());
		if (fieldConfig != null) {
			fieldConfig.setStartCoordinate(startPoint);
			fieldConfig.notifyPositionListeners();
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#nudge(int, jpl.gds.shared.swt.types.ChillPoint, int)
	 */
	@Override
	public void nudge(final int direction, final ChillPoint maxCoords, final int amount) {
		int x = 0;
		int y = 0;


		switch (direction) {
		case SWT.ARROW_UP:
			if (startPoint.getY() - amount < 0) {
				return;
			}
			x = startPoint.getX();
			y = Math.max(0, startPoint.getY() - amount);
			break;
		case SWT.ARROW_DOWN:
			if (startPoint.getY() + amount > maxCoords.getY()) {
				return;
			}
			x = startPoint.getX();
			y = Math.min(maxCoords.getY(), startPoint.getY() + amount);
			break;
		case SWT.ARROW_LEFT:
			if (startPoint.getX() - amount < 0) {
				return;
			}
			y = startPoint.getY();
			x = Math.max(0, startPoint.getX() - amount);
			break;
		case SWT.ARROW_RIGHT:
			if (startPoint.getX() + amount > maxCoords.getX()) {
				return;
			}
			y = startPoint.getY();
			x = Math.min(maxCoords.getX(), startPoint.getX() + amount);
			break;
		}

		moveAndResize(new ChillPoint(x,y, getCoordinateSystem()), null, maxCoords);
	}

	/**
	 * Saves the current graphics settings in the given GC.
	 * 
	 * @param gc the GC to get settings from
	 */
	protected void saveGcSettings(final GC gc) {
		gcSettings.font = gc.getFont();
		gcSettings.background = gc.getBackground();
		gcSettings.foreground = gc.getForeground();
		gcSettings.lineThickness = gc.getLineWidth();
		gcSettings.lineStyle = gc.getLineStyle();
		gcSettings.alpha = gc.getAlpha();
	}

	/**
	 * Restores the current graphics settings to the given GC.
	 * 
	 * @param gc the GC to configure
	 */
	protected void restoreGcSettings(final GC gc) {
		gc.setFont(gcSettings.font);
		gc.setBackground(gcSettings.background);
		gc.setForeground(gcSettings.foreground);
		gc.setLineWidth(gcSettings.lineThickness);
		gc.setLineStyle(gcSettings.lineStyle);
		gc.setAlpha(gcSettings.alpha);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#copy(boolean, int, int)
	 */
	@Override
	public CanvasElement copy(
	        final boolean newCoordinates, final int offsetX, final int offsetY) {
		final IFixedFieldConfiguration newConfig = 
		    FixedFieldConfigurationFactory.create(fieldConfig.getApplicationContext(), fieldConfig.getType());
		fieldConfig.copyConfiguration(newConfig);

		final CanvasElement newElem = CanvasElementFactory.create(newConfig, parent);
		newElem.enableEditMode(true);
		if (newCoordinates) {
			final ChillPoint start = newConfig.getStartCoordinate();
			start.setX(start.getX() + offsetX);
			start.setY(start.getY() + offsetY);
			newConfig.setStartCoordinate(start);
		}
		newElem.setFieldConfiguration(newConfig);
		return newElem;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#setSelected(boolean)
	 */
	@Override
	public void setSelected(final boolean set) {
		selected = set;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#isSelected()
	 */
	@Override
	public boolean isSelected() {
		return selected;
	}

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.canvas.CanvasElement#evaluate(org.springframework.context.ApplicationContext, int)
     */
    @Override
	public boolean evaluate(final ApplicationContext appContext, final int stalenessInterval) {
        previousDisplayMe = displayMe;
        displayMe = fieldConfig.getCondition().evaluate(appContext, stalenessInterval);
        return displayMe;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.canvas.CanvasElement#hasConditionChanged()
     */
    @Override
	public boolean hasConditionChanged() {
        return previousDisplayMe != displayMe;
    }

	/**
	 * A simple object to store attributes of a graphics context (GC).
	 */
	public static class GCSettings {
		private Font font;
		private Color background;
		private Color foreground;
		private int lineThickness;
		private int lineStyle;
		private int alpha;
	}
}

