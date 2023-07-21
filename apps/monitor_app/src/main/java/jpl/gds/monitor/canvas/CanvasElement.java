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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This interface must be implemented by all drawable elements on a fixed view
 * canvas.
 *
 */
public interface CanvasElement {
	
	/**
	 * Gets the start location (x,y) for this element on the canvas. Note
	 * that this may be specified in pixels or characters, depending
	 * on whether the configuration has CoordinateSystemType PIXEL or 
	 * CHARACTER.
	 * 
	 * @return a Point object containing the x,y coordinate of this element
	 */
	public ChillPoint getStartLocation();
	
	/**
	 * Gets the end location (x,y) for this element on the canvas. Note
	 * that this may be specified in pixels or characters, depending
	 * on whether the configuration has CoordinateSystemType PIXEL or 
	 * CHARACTER. Note also that not all canvas elements are defined by two 
	 * points.
	 * 
	 * @return a Point object containing the x,y coordinate of this element;
	 * may be -1, -1 if currently undefined (the canvas will figure it out), 
	 * or null if the CanvasElement is not defined using two points.
	 */
	public ChillPoint getEndLocation();
	
	/**
	 * Gets the start location (x,y) for this element on the canvas. Note
	 * that this may be specified in pixels or characters, depending
	 * on whether the current configuration is using CoordinateSystemType 
	 * PIXEL or CHARACTER.
	 * 
	 * @param pt a Point object containing the x,y coordinate of this element
	 */
	public void setStartLocation(ChillPoint pt);
	
	/**
	 * Sets the end location (x,y) for this element on the canvas. Note
	 * that this may be specified in pixels or characters, depending
	 * on whether the current configuration is using CoordinateSystemType 
	 * PIXEL or CHARACTER. Note also that not all canvas elements are defined 
	 * by two points. This method will do nothing in that case.
	 * 
	 * @param pt a Point object containing the x,y coordinate of this element;
	 * coordinate may be undefined to tell the canvas to figure it out. 
	 */
	public void setEndLocation(ChillPoint pt);
	
	/**
	 * Draws the CanvasElement to the given Graphics Context (GC)
	 * 
	 * @param gc the GC to draw to
	 */
	public void draw(GC gc);
	
	/**
	 * Indicates if this CanvasElement will change dynamically based upon 
	 * data content, or whether it remains the same after it is initially
	 * drawn.
	 * 
	 * @return true if the CanvasElement does not change; false if it can
	 */
	public boolean isStatic();
	
	/**
	 * Sets the flag that indicates if this CanvasElement will change 
	 * dynamically based upon data content, or whether it remains the same 
	 * after it is initially drawn.
	 * 
	 * @param isStatic true if the CanvasElement does not change; false if 
	 * it can
	 */	
	public void setStatic(boolean isStatic);
	
	/**
	 * Sets the coordinate system of this element to PIXEL or CHARACTER, 
	 * indicating how its actual canvas coordinates are calculated from its 
	 * x,y location.
	 * 
	 * @param type CoordinateSystemType.PIXEL or CoordinateSystemType.CHARACTER
	 */
	public void setCoordinateSystem(CoordinateSystemType type);
	
	/**
	 * Gets the coordinate system of this element to PIXEL or CHARACTER, 
	 * indicating how its actual canvas coordinates are calculated from its 
	 * x,y location.
	 * 
	 * @return CoordinateSystemType.PIXEL or CoordinateSystemType.CHARACTER
	 */
	public CoordinateSystemType getCoordinateSystem();
	
	/**
	 * Gets the bounds (last drawing coordinates and size) of this 
	 * CanvasElement as it was last drawn on the canvas.
	 * 
	 * @return the Rectangle defining the absolute last bounds of the drawn 
	 * element on the canvas.
	 */
	public Rectangle getLastBounds();
	
	/**
	 * Retrieves the selection priority of this CanvasElement. When there is 
	 * competition for selection or drawing priority, highest priority objects 
	 * are drawn "in the back" and selected last, lowest priority objects 
	 * drawn "in the front" and selected first.
	 * 
	 * @return the priority of this canvas element
	 */
	public int getSelectionPriority();
	
	/**
	 * Sets the selection priority of this CanvasElement. When there is 
	 * competition for selection or drawing priority, highest priority 
	 * objects are drawn "in the back" and selected last, lowest priority 
	 * objects are drawn "in the front" and selected first.
	 * 
	 * @param pri the priority of this canvas element
	 */
	public void setSelectionPriority(int pri);
	
	/**
	 * Sets the fixed view field type of this Canvas Element.
	 * 
	 * @param type the FixedFieldType to set
	 */
	public void setFieldType(FixedFieldType type);
	
	/**
	 * Gets the fixed view field type of this Canvas Element.
	 * 
	 * @return the FixedFieldType
	 */
	public FixedFieldType getFieldType();
	
	/**
	 * Sets the Fixed Field Configuration that defines this CanvasElement in 
	 * the perspective.
	 * 
	 * @param config the IFixedFieldConfiguration to set
	 */
	public void setFieldConfiguration(IFixedFieldConfiguration config);
	
	/**
	 * Gets the Fixed Field Configuration that defines this CanvasElement in
	 * the perspective.
	 * 
	 * @return the IFixedFieldConfiguration to set
	 */
	public IFixedFieldConfiguration getFieldConfiguration();
	
	/**
	 * Draws the selection "handles" for this CanvasElement to the given
	 * Graphics Context (GC) object.
	 * 
	 * @param gc the GC to use for drawing
	 */
	public void drawHandles(GC gc);
	
	/**
	 * Puts the CanvasElement in "edit" mode for the builder, as opposed to
	 * the non-edit, realtime mode.
	 * 
	 * @param enable true to put this element into edit mode; false to 
	 * disable editing
	 */
	public void enableEditMode(boolean enable);
	
	/**
	 * Gets the ID of the selection "handle" under the given x,y location, 
	 * if any.
	 * 
	 * @param pt the absolute x,y coordinate to look for
	 * @return SelectionHandleId
	 */
	public  SelectionHandleId getHandleIdForPoint(Point pt);
	
	/**
	 * Gets the ElementHandle (selection "handle") with the given ID.
	 *  
	 * @param id SelectionHandleId
	 * @return the ElementHandle for the given ID
	 */
	public ElementHandle getHandle(SelectionHandleId id);
	
	/**
	 * Indicates whether this CanvasElement has fixed size, or can be 
	 * "morphed" (stretched) by grabbing the selection "handles".
	 * 
	 * @return true if this CanvasElement can be morphed
	 */
	public boolean isShapeMorphable();
	
	/**
	 * Move the coordinates of the CanvasElement to the given absolute X,Y 
	 * locations, resizing the element if necessary
	 * 
	 * @param start the x,y, coordinate to move the first (starting) point of 
	 * the object to
	 * @param end the x,y, coordinate to move the second (ending) point of 
	 * the object to
	 * @param maxCoords maximum allowed X,Y coordinate
	 */
	public void moveAndResize(
							  ChillPoint start, ChillPoint end, ChillPoint maxCoords);
	
	/**
	 * Moves the canvas element a few pixels either up, down, left, or right.
	 * 
	 * @param keycode SWT.ARROW_UP, SWT.ARROW_DOWN, SWT.ARROW_LEFT, 
	 * SWT.ARROW_RIGHT
	 * @param maxCoords maximum allowed X,Y coordinate
	 * @param amount number of pixels to nudge the object
	 */
	public void nudge(int keycode, ChillPoint maxCoords, int amount);
	
	/**
	 * Makes a deep copy of the CanvasElement. If new coordinates are desired 
	 * (which is the case when drawing the copied objects on the canvas, or 
	 * else the new copy is drawn on top of the old object) then they 
	 * coordinates of the new object will be shifted slightly from the original 
	 * coordinates.
	 * 
	 * @param newCoordinates true to modify coordinates of the new element by 
	 * the indicated offsets; false to copy coordinates exactly
	 * @param offsetX offset to be added to the X coordinate of the new element
	 * @param offsetY offset to be added to the Y coordinate of the new element
	 * @return a copy of an existing canvas element with shifted 
	 * x,y coordinates
	 */
	public CanvasElement copy(
							  boolean newCoordinates, int offsetX, int offsetY);
	
	/**
	 * Sets the selected flag for this CanvasElement, meaning it has been 
	 * selected by the user.
	 * 
	 * @param set true to set as selected, false to deselect
	 */
	public void setSelected(boolean set);
	
	/**
	 * Gets the selected flag for this CanvasElement, indicating whether it 
	 * has been selected by the user.
	 * 
	 * @return true if element is selected by user, false otherwise
	 */
	public boolean isSelected();
	
	/**
     * Determines whether this CanvasElement should be drawn on the screen.
     * This is done by checking the associated condition (if any).
     * 
     * @param stalenessInterval number of seconds that need to pass with no 
     * new data for a channel to be considered stale
     * @return true if element should be drawn on screen based on conditions
     */
    public boolean evaluate(ApplicationContext appContext, int stalenessInterval);
	
    /**
     * Compares the new evaluation value with the old evaluation value to 
     * determine if the canvas should be redrawn
     * @return true if evaluation changed (canvas should be redrawn), 
     *         false otherwise
     */
    public boolean hasConditionChanged();
}
