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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * Fixed Canvas is the drawing board for both the fixed layout view (runtime)
 * and the fixed layout builder (build time). It contains a set of
 * CanvasElements, and handles drag, drop, and resize of these elements. This
 * class has two "modes": editable mode, for build time support, in which
 * objects can be selected, changed, and moved, and non-editable mode, which
 * presents a non-modifiable canvas.
 * 
 */
public class FixedCanvas {

	/**
	 * Alignment types for snap and align features.
	 */
	public enum AlignmentType {
		/** Align to nearest horizontal grid line.**/
		HORIZONTAL,
		/** Align to nearest vertical grid line **/
		VERTICAL,
		/** Center on nearest grid line. **/
		CENTER;
	}

	/** Default canvas width in pixels. */
	public static final int DEFAULT_PIXEL_WIDTH = 800;
	/** Default canvas height in pixels. */
	public static final int DEFAULT_PIXEL_HEIGHT = 500;
	/** Default canvas width in characters. */
	public static final int DEFAULT_CHARACTER_WIDTH = 120;
	/** Default canvas width in characters. */
	public static final int DEFAULT_CHARACTER_HEIGHT = 40;
	/** Default copy offset in pixels. */
	public static final int PIXEL_COPY_OFFSET = 15;
	/** Default copy offset in characters. */
	public static final int CHARACTER_COPY_OFFSET = 2;
	/** Pad added around the canvas edge, in pixels. */
	public static final int PAD = GdsSystemProperties.isMacOs() ? 10 : 5;

	// GUI components
	private Color gridColor = ChillColorCreator.getColor(new ChillColor(ColorName.LIGHT_GREY));
	private Canvas canvas;
	private final Composite parent;
	private Font defaultFont = ChillFontCreator.getFont(IFixedLayoutViewConfiguration.DEFAULT_FONT);
	private Color defaultBackgroundColor;
	private Color defaultForegroundColor;
	private CanvasHoverHandler hoverHandler;
	private CanvasMouseHandler mouseHandler;

	/** List of drawn elements on this canvas. **/
	protected List<CanvasElement> elements = 
	    new ArrayList<CanvasElement>();

	// Indicates editable/non-editable state of the canvas
	private boolean editable;

	// The FixedLayoutViewConfiguration associated with this canvas
	private IFixedLayoutViewConfiguration viewConfig;

	// Indicates whether to display a drawing grid
	private boolean useDrawingGrid = false;

	// Gap between horizontal grid lines
	private int gridSizeHorizontal = 10;

	// Gap between vertical grid lines
	private int gridSizeVertical = 10;

	// Current character width, based upon default font
	private int characterWidth = -1;

	// Current character height, based upon default font
	private int characterHeight = -1;

	// Indicates the canvas is in character-addressable mode, as opposed to 
	// pixel-addressable
	private boolean isCharacterMode;

	private int lastCopyXOffset = -1;
	private int lastCopyYOffset = -1;

	/**
	 * Creates a new FixedCanvas in non-editable mode.
	 * 
	 * @param parent the Composite parent Widget for the canvas
	 */
	public FixedCanvas(final Composite parent) {
		this.parent = parent;
		init();
	}

	/**
	 * Indicates whether the canvas is in editable mode.
	 * 
	 * @return true if the canvas is in edit mode, false if not
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Sets the canvas to be editable or not editable.
	 * 
	 * @param en true to enabled editing, false to disable
	 */
	public void setEditable(final boolean en) {
		editable = en;
		hoverHandler.setBuildtime(en);
		mouseHandler.setBuildtime(en);
	}

	/** 
	 * Attaches the given popup menu to the canvas.
	 * 
	 * @param popup the Menu to attach
	 */
	public void setMenu(final Menu popup) {
		canvas.setMenu(popup);
	}

	/**
	 * Sets the fixed layout view configuration.
	 * 
	 * @param config the FixedLayoutViewConfiguration to set
	 */
	public void setViewConfiguration(final IFixedLayoutViewConfiguration config) {
		viewConfig = config;
		lastCopyXOffset = -1;
		lastCopyYOffset = -1;
		if (viewConfig != null) {
			isCharacterMode = config.getCoordinateSystem().equals(
			        CoordinateSystemType.CHARACTER);
			redraw();
		}
	}

	/**
	 * Adds a CanvasSelectionListener to the canvas.
	 * 
	 * @param addMe the listener to add
	 */
	public void addSelectionListener(final CanvasSelectionListener addMe) {
		mouseHandler.addSelectionListener(addMe);
	}

	/**
	 * Removes a CanvasSelectionListener from the canvas.
	 * 
	 * @param removeMe the listener to remove
	 */
	public void removeSelectionListener(
	        final CanvasSelectionListener removeMe) {
		mouseHandler.removeSelectionListener(removeMe);
	}

	/**
	 * Sets the current selection to the given canvas element.
	 * 
	 * @param elem
	 *            the CanvasElement to select
	 */
	public void setSelectedElement(final CanvasElement elem) {
		mouseHandler.setSelection(elem);
	}

	/**
	 * Sets the current selection to the given canvas elements.
	 * 
	 * @param elem
	 *            list of CanvasElements to select
	 */
	public void setSelectedElements(final List<CanvasElement> elem) {
		mouseHandler.setSelection(elem);
	}

	/**
	 * Gets the currently selected canvas elements.
	 * 
	 * @return list of CanvasElements that are selected
	 */
	public List<CanvasElement> getSelectedElements() {
		return mouseHandler.getSelection();
	}

	/**
	 * Gets the absolute x,y coordinate of the last known mouse position, in 
	 * pixels
	 * 
	 * @return Point object containing mouse coordinates
	 */
	public Point getLastMousePosition() {
		return mouseHandler.getLastMousePosition();
	}

	/**
	 * Gets the absolute x,y coordinate of the last known mouse position, in 
	 * layout coordinates
	 * 
	 * @return ChillPoint object containing coordinates
	 */
	public ChillPoint getLastMousePositionInLayoutCoordinates() {
		final Point p = mouseHandler.getLastMousePosition();
		ChillPoint result = null;
		if (this.isCharacterLayout()) {
			result = new ChillPoint(p.x / getCharacterWidth(), p.y / 
			        getCharacterHeight(), CoordinateSystemType.CHARACTER);
		} else {
			result = new ChillPoint(p.x, p.y, CoordinateSystemType.PIXEL);			
		}
		return result;
	}

	/**
	 * Gets the CanvasElement under the cursor, selected or not.
	 * 
	 * @return CanvasElement the highest priority element under the cursor, 
	 * or null if there is no element at the cursor position
	 */
	public CanvasElement getElementAtCursor() {
		final Point pt = mouseHandler.getLastMousePosition();
		final List<CanvasElement> possibleMatches = getElementsForPoint(pt);
		if (possibleMatches == null || possibleMatches.isEmpty()) {
			return null;
		}
		return selectBestMatch(possibleMatches);
	}

	/**
	 * Sets the list of CanvasElement objects displayed on the canvas.
	 * 
	 * @param newElems
	 *            the list of CanvasElements to set
	 */
	public void setCanvasElements(final ArrayList<CanvasElement> newElems) {
		elements = newElems;
		for (final CanvasElement elem: elements) {
			elem.enableEditMode(editable);
		}
	}

	/**
	 * Gets the list of CanvasElement objects displayed on the canvas.
	 * 
	 * @return list of CanvasElements
	 */
	public List<CanvasElement> getCanvasElements() {
		return elements;
	}

	/**
	 * Adds a CanvasElement to the Canvas for display.
	 * 
	 * @param addMe
	 *            the CanvasElement to add
	 */
	public void addCanvasElement(final CanvasElement addMe) {
		elements.add(addMe);
		addMe.enableEditMode(editable);
		redraw();
	}

	/**
	 * Removes currently selected CanvasElements from the Canvas.
	 * 
	 */
	public void removeSelectedElements() {
		final List<CanvasElement> selectedElems = mouseHandler.getSelection();
		if (selectedElems == null) {
			return;
		}
		for (final CanvasElement removeMe: selectedElems) {
			for (final CanvasElement elem: elements) {
				if (removeMe == elem && viewConfig != null) {
					viewConfig.removeFieldConfig(elem.getFieldConfiguration());
				}
			}
			elements.remove(removeMe);
		}
		mouseHandler.setSelection((CanvasElement)null);
		redraw();
	}

	/**
	 * Removes all CanvasElements from the Canvas.
	 * 
	 */
	public void removeAllElements() {
		for (final CanvasElement elem: elements) {
			viewConfig.removeFieldConfig(elem.getFieldConfiguration());
		}
		elements.clear();
		redraw();
	}

	/**
	 * Removes a given CanvasElement from the Canvas.
	 * 
	 * @param removeMe
	 *            the CanvasElement to remove
	 */
	public void removeCanvasElement(final CanvasElement removeMe) {
		if (elements.contains(removeMe)) {
			final List<CanvasElement> selectedElems = mouseHandler.getSelection();

			if (selectedElems != null && selectedElems.contains(removeMe)) {
				selectedElems.remove(removeMe);
				mouseHandler.setSelection(selectedElems);
				mouseHandler.setSelectedHandle(SelectionHandleId.HANDLE_NONE);
			}
			elements.remove(removeMe);
			redraw();
		}
	}

	/**
	 * Duplicates the currently selected elements and makes the duplicate set 
	 * the new selection.
	 */
	public void duplicateSelectedElements() {
		final List<CanvasElement> selectedElements = mouseHandler.getSelection();
		if (selectedElements == null || selectedElements.isEmpty()) {
			return;
		}
		int smallestX = Integer.MAX_VALUE;
		int smallestY = Integer.MAX_VALUE;
		int largestX = Integer.MIN_VALUE;
		int largestY = Integer.MIN_VALUE;

		for (final CanvasElement oldElem: selectedElements) {
			final ChillPoint start = oldElem.getStartLocation();
			if (start.getX() < smallestX) {
				smallestX = start.getX();
			}
			if (start.getX() > largestX) {
				largestX = start.getX();
			}
			if (start.getY() < smallestY) {
				smallestY = start.getY();
			}
			if (start.getY() > largestY) {
				largestY = start.getY();
			}
			if (oldElem instanceof DualCoordinateCanvasElement) {
				final ChillPoint end = 
				    ((DualCoordinateCanvasElement)oldElem).getEndLocation();
				if (end.isUndefined()) {
					continue;
				}
				if (end.getX() < smallestX) {
					smallestX = end.getX();
				}
				if (end.getX() > largestX) {
					largestX = end.getX();
				}
				if (end.getY() < smallestY) {
					smallestY = end.getY();
				}
				if (end.getY() > largestY) {
					largestY = end.getY();
				}
			}
		}
		int xOffset;
		int yOffset;

		if (lastCopyXOffset == -1) {
			xOffset = FixedCanvas.PIXEL_COPY_OFFSET;
			yOffset = FixedCanvas.PIXEL_COPY_OFFSET;	
			if (isCharacterMode) {
				xOffset = FixedCanvas.CHARACTER_COPY_OFFSET;
				yOffset = FixedCanvas.CHARACTER_COPY_OFFSET;
			}
		} else {
			xOffset = lastCopyXOffset;
			yOffset = lastCopyYOffset;
		}

		final int xMax = viewConfig.getPreferredWidth();
		final int yMax = viewConfig.getPreferredHeight();

		if (xOffset > 0 && largestX + xOffset > xMax) {
			xOffset = -xOffset;
		} else if (xOffset < 0 && smallestX + xOffset < 0) {
			xOffset = -xOffset;
		}
		if (yOffset > 0 && largestY + yOffset > yMax) {
			yOffset = -yOffset;
		} else if (yOffset < 0 && smallestY + yOffset < 0) {
			yOffset = -yOffset;
		}
		final List<CanvasElement> newElements = 
		    new ArrayList<CanvasElement>(selectedElements.size());
		for (final CanvasElement oldElem: selectedElements) {
			CanvasElement newElem = null;
			newElem = oldElem.copy(true, xOffset, yOffset);
			newElements.add(newElem);
			elements.add(newElem);

		}	    
		mouseHandler.setSelection(newElements);
		redraw();
		lastCopyXOffset = xOffset;
		lastCopyYOffset = yOffset;
	}

	/**
	 * Forces a redraw of all the elements on the canvas.
	 */
	public void redraw() {
		canvas.redraw();
	}

	/**
	 * Resizes the canvas to the given size, in pixels. A pad is added
	 * because SWT won't show the whole canvas otherwise, for some reason.
	 * 
	 * @param width
	 *            the desired width of the canvas, in pixels
	 * @param height
	 *            the desired height of the canvas, in pixels
	 */
	public void resizeInPixels(final int width, final int height) {
		canvas.setSize(width + PAD, height + PAD);
		redraw();
	}
	
	/**
	 * Indicates whether the give point is on the canvas.
	 * 
	 * @param p Point to look for
	 * @return true if the point is on the canvas; false if outside
	 */
	public boolean containsPoint(final Point p) {
		final Point size = canvas.getSize();
		if (p.x > 0 && p.x < size.x - PAD && p.y >0 && p.y < size.y - PAD) {
			return true;
		}
		return false;
	}

	/**
	 * Resizes the canvas to the given size, in characters. A pad is added
	 * because SWT won't show the whole canvas otherwise, for some reason.
	 * 
	 * @param width
	 *            the desired width of the canvas, in characters
	 * @param height
	 *            the desired height of the canvas, in characters
	 */
	public void resizeInCharacters(final int width, final int height) {
		final int realWidth = getXCoordinate(width);
		final int realHeight = getYCoordinate(height); 
		canvas.setSize(realWidth + PAD, realHeight + PAD);
		redraw();
	}

	/**
	 * Gets the "unpadded" bounds for the canvas, in pixels.
	 * 
	 * @return Rectangle representing the canvas bounds
	 */
	public Rectangle getUnpaddedBounds() {
		final Rectangle bounds = canvas.getBounds();
		final Rectangle newBounds = new Rectangle(
		        bounds.x, bounds.y, 
		        bounds.width - PAD, 
				bounds.height - PAD);
		return newBounds;
	}

	/**
	 * Clears all CanvasElements and selections from the canvas, resets the
	 * color and font defaults, and forces a redraw.
	 */
	public void clear() {
		elements.clear();
		mouseHandler.setSelection((CanvasElement)null);
		mouseHandler.setSelectedHandle(SelectionHandleId.HANDLE_NONE);
		mouseHandler.resetMousePosition();
		lastCopyXOffset = -1;
		lastCopyYOffset = -1;
		defaultBackgroundColor = null;
		defaultForegroundColor = null;
		defaultFont = ChillFontCreator.getFont(
		        IFixedLayoutViewConfiguration.DEFAULT_FONT);
		redraw();
	}

	/**
	 * Main GUI entry point.
	 */
	private void init() {

		canvas = new Canvas(parent, SWT.NO_REDRAW_RESIZE | SWT.BORDER);

		if (defaultFont != null) {
			canvas.setFont(defaultFont);
		}
		if (defaultBackgroundColor != null) {
			canvas.setBackground(defaultBackgroundColor);
		}
		if (defaultForegroundColor != null) {
			canvas.setForeground(defaultForegroundColor);
		}

		canvas.addPaintListener(new CanvasPaintListener());

		hoverHandler = new CanvasHoverHandler(parent.getShell(), this);
		hoverHandler.activateHover();

		mouseHandler = new CanvasMouseHandler(this);
		mouseHandler.activateMouse();

		canvas.addKeyListener(new CanvasKeyListener());
	}

	/**
	 * Gets the SWT Canvas widget.
	 * 
	 * @return SWT Canvas
	 */
	public Canvas getCanvas() {
		return canvas;
	}

	/**
	 * Gets the default font used by the canvas.
	 * 
	 * @return SWT Font object, or null if no default font defined.
	 */
	public Font getDefaultFont() {
		return defaultFont;
	}

	/**
	 * Sets the default font used by the canvas.
	 * 
	 * @param defaultFont
	 *            SWT Font object to set
	 */
	public void setDefaultFont(final Font defaultFont) {
		this.defaultFont = defaultFont;
		characterHeight = -1;
		characterWidth = -1;
	}

	/**
	 * Gets the default background color used by the canvas.
	 * 
	 * @return SWT Color object, or null if no background color defined
	 */
	public Color getDefaultBackgroundColor() {
		return defaultBackgroundColor;
	}

	/**
	 * Sets the default background color used by the canvas.
	 * 
	 * @param defaultBackgroundColor
	 *            SWT Color object, or null to clear the setting
	 */
	public void setDefaultBackgroundColor(final Color defaultBackgroundColor) {
		this.defaultBackgroundColor = defaultBackgroundColor;
	}

	/**
	 * Gets the default foreground color used by the canvas.
	 * 
	 * @return SWT Color object, or null if no foreground color defined
	 */
	public Color getDefaultForegroundColor() {
		return defaultForegroundColor;
	}

	/**
	 * Sets the default foreground color used by the canvas.
	 * 
	 * @param defaultForegroundColor
	 *            SWT Color object, or null to clear the setting
	 */
	public void setDefaultForegroundColor(final Color defaultForegroundColor) {
		this.defaultForegroundColor = defaultForegroundColor;
	}

	/**
	 * Indicates whether the drawing grid is enabled.
	 * 
	 * @return true if grid is on, false if it is off
	 */
	public boolean useGrid() {
		return useDrawingGrid;
	}

	/**
	 * Sets the flag indicating whether the drawing grid should be enabled.
	 * 
	 * @param en true to turn grid on, false to turn it off
	 */
	public void setUseGrid(final boolean en) {
		useDrawingGrid = en;
		redraw();
	}

	/**
	 * Gets the drawing grid spacing, in layout coordinates.
	 * 
	 * @return grid size in layout offset units
	 */
	public int getGridSize() {
		if (!this.isCharacterLayout()) {
			return gridSizeHorizontal;
		} else {
			return gridSizeHorizontal / getXCoordinate(1);
		}
	}

	/**
	 * Sets the drawing grid spacing, in layout coordinates.
	 * 
	 * @param size grid size in layout offset units
	 */
	public void setGridSize(final int size) {

		if (viewConfig == null || !isCharacterLayout()) {
			gridSizeHorizontal = size;
			gridSizeVertical = size;
		} else {
			gridSizeHorizontal = size * getXCoordinate(1);
			gridSizeVertical = size * getYCoordinate(1);
		}
		redraw();
	}

	/**
	 * Sets the drawing grid color.
	 * 
	 * @param col the color to set for the grid
	 */
	public void setGridColor(final Color col) {
		gridColor = col;
		redraw();
	}

	/**
	 * Returns the list of CanvasElements on the canvas that "contain" the 
	 * given point on the Canvas within their rectangular bounds.
	 * 
	 * @param pt
	 *            the absolute x,y coordinate to look for, in pixels
	 * @return List of matching CanvasElemenst; the list will be empty if no
	 *         CanvasElements contain the specified coordinate
	 */
	public List<CanvasElement> getElementsForPoint(final Point pt) {

		final ArrayList<CanvasElement> matchedElems = 
		    new ArrayList<CanvasElement>();

		final int pad = this.isEditable() ? ElementHandle.HANDLE_GRAB_SIZE : 0;
		for (final CanvasElement elem : elements) {
			final Rectangle bounds = elem.getLastBounds();
			if (bounds != null) {	
				final Rectangle padBounds = new Rectangle(
				        Math.max(0, bounds.x - pad),
						Math.max(0, bounds.y - pad), 
						bounds.width + pad + 1,
						bounds.height + pad + 3);
				if (padBounds.contains(pt)) {
					matchedElems.add(elem);
				}
			}
		}
		return matchedElems;
	}

	/**
	 * Returns the list of CanvasElements on the canvas that "are contained 
	 * by" the given Rectangle on the Canvas.
	 * 
	 * @param rect
	 *            the rectangle to search for matching elements, in pixel units
	 * @return List of matching CanvasElemenst; the list will be empty if no
	 *         CanvasElements are contained within the rectangle
	 */
	public List<CanvasElement> getElementsForRectangle(final Rectangle rect) {
		final ArrayList<CanvasElement> matchedElems = new ArrayList<CanvasElement>();
		if (rect == null) {
			return matchedElems;
		}
		for (final CanvasElement elem : elements) {
			final ChillPoint start = elem.getStartLocation();
			int x = getXCoordinate(start.getX());
			int y = getYCoordinate(start.getY());
			Point pt = new Point(x, y);
			if (rect.contains(pt)) {
				matchedElems.add(elem);
			} else {
				final ChillPoint end = elem.getEndLocation();
				if (end != null) {
					x = getXCoordinate(end.getX());
					y = getYCoordinate(end.getY());
					pt = new Point(x, y);
					if (pt != null && rect.contains(pt)) {
						matchedElems.add(elem);
					}
				}
			}
		}
		return matchedElems;
	}

	/**
	 * Returns the CanvasElement that, among the list of CanvasElements passed
	 * in, should be considered the "best" (based upon selection priority and 
	 * order) match for user selection.
	 * 
	 * @param elems
	 *            List of candidate CanvasElements for the match
	 * @return "best match" CanvasElement
	 */
	public CanvasElement selectBestMatch(final List<CanvasElement> elems) {
		CanvasElement bestMatch = null;
		for (final CanvasElement elem : elems) {
			if (bestMatch == null) {
				bestMatch = elem;
			} else {
				if (elem.getSelectionPriority() <= bestMatch
						.getSelectionPriority() ||
						elem.isSelected() && !bestMatch.isSelected()) {
					bestMatch = elem;
				}
			}
		}
		return bestMatch;
	}

	/**
	 * Aligns currently selected objects to the nearest grid line, either top 
	 * to bottom or left to right.
	 * 
	 * @param align AlignmentType.HORIZONTAL or AlignmentType.VERTICAL
	 */
	public void snapSelectedElementsToGrid(final AlignmentType align) {

		if (align.equals(AlignmentType.CENTER)) {
			throw new IllegalArgumentException("Snap to grid does not allow " +
					"CENTER alignment");
		}
		final List<CanvasElement> selected = mouseHandler.getSelection();
		if (selected == null || selected.isEmpty()) {
			return;
		}

		// Loop through all selected elements
		for (final CanvasElement elem: selected) {

			// Get starting point of the element and find the closest grid 
		    // point.   
			final ChillPoint start = elem.getStartLocation();
			final Point pixelStart = new Point(getXCoordinate(start.getX()), 
			        getYCoordinate(start.getY()));
			final Point closest = findNearestGridPoint(pixelStart, align);

			// Get ending point of the element is it has one
			Point pixelEnd = null;
			if (elem instanceof DualCoordinateCanvasElement) {
				final ChillPoint end = 
				    ((DualCoordinateCanvasElement)elem).getEndLocation();
				pixelEnd = new Point(getXCoordinate(end.getX()), 
				        getYCoordinate(end.getY()));
			}

			// Compute new starting and ending coordinates to align with the 
			// grid
			switch(align) {
			case VERTICAL:
				if (pixelEnd != null) {
					final int oldYlen = pixelEnd.y - pixelStart.y;
					pixelEnd.y = closest.y + oldYlen;
				}
				pixelStart.y = closest.y;
				break;
			case HORIZONTAL:
				if (pixelEnd != null) {
					final int oldXlen = pixelEnd.x - pixelStart.x;
					pixelEnd.x = closest.x + oldXlen;
				}
				pixelStart.x = closest.x;
				break;
			}
			ChillPoint newStart = null;
			ChillPoint newEnd = null;

			if (pixelEnd != null) {
				newEnd = new ChillPoint(pixelEnd.x, pixelEnd.y, 
				        viewConfig.getCoordinateSystem());
			}

			// Perform character coordinate conversion, if necessary
			if (viewConfig.getCoordinateSystem().equals(
			        CoordinateSystemType.CHARACTER)) {
				newStart = new ChillPoint(pixelStart.x / getCharacterWidth(), 
				        pixelStart.y / getCharacterHeight(), 
						viewConfig.getCoordinateSystem());
				if (pixelEnd != null) {
					newEnd = new ChillPoint(pixelEnd.x / getCharacterWidth(), 
					        pixelEnd.y / getCharacterHeight(), 
							viewConfig.getCoordinateSystem());
				}
			} else {
				newStart = new ChillPoint(pixelStart.x, pixelStart.y, 
				        viewConfig.getCoordinateSystem());
			}

			// Move the element
			elem.moveAndResize(newStart, newEnd, new ChillPoint(
			        Integer.MAX_VALUE, Integer.MAX_VALUE, 
			        viewConfig.getCoordinateSystem()));
		}
		redraw();
	}

	/**
	 * Find the nearest grid point to the given point on the canvas.
	 * 
	 * @param current the x,y on the canvas to look for, in pixel units
	 * @param align AlignmentType indicating whether to look up/down for the
	 * nearest grid line, or left/right. 
	 * 
	 * @return nearest grid Point, in pixels, or null if the grid is not 
	 * enabled.
	 */
	public Point findNearestGridPoint(final Point current, final AlignmentType align) {
		if (!useDrawingGrid) {
			return null;
		}
		if (align.equals(AlignmentType.CENTER)) {
			throw new IllegalArgumentException(
			        "Snap to grid does not allow CENTER alignment");
		}

		// Create a list of all the grid points
		final List<Point> gridPoints = new ArrayList<Point>();

		final int xMax = getUnpaddedBounds().width;
		final int yMax = getUnpaddedBounds().height;

		for (int x = 0; x < xMax; x+= gridSizeHorizontal) {
			for (int y = 0; y < yMax; y+= gridSizeVertical) {
				gridPoints.add(new Point(x, y));
			}
		}

		// Find the one closest to the input point
		Point closest = gridPoints.get(0);

		for (final Point gridPoint: gridPoints) {
			switch(align) {  	
			case VERTICAL:
				final int oldYGap = Math.abs(closest.y - current.y);
				final int newYGap = Math.abs(gridPoint.y - current.y);
				if (newYGap < oldYGap) {
					closest = gridPoint;
				}
				break;
			case HORIZONTAL:
				final int oldXGap = Math.abs(closest.x - current.x);
				final int newXGap = Math.abs(gridPoint.x - current.x);
				if (newXGap < oldXGap) {
					closest = gridPoint;
				}
				break;
			}
		}
		return closest;
	}

	/**
	 * Moves the selected elements to the end/rear of the drawing order. If 
	 * the parameter is false, selected elements will be moved one element; if 
	 * true, elements will all be moved to the very end of the element list.
	 * 
	 * @param allTheWay true to move elements to the very end of the element 
	 * set
	 */
	public void moveSelectedElementsTowardsRear(final boolean allTheWay) {
		final List<CanvasElement> selected = mouseHandler.getSelection();
		if (selected == null || selected.isEmpty()) {
			return;
		}
		for (final CanvasElement elem: selected) {
			boolean moved = true;
			while (moved) {
				moved = moveElementTowardsRear(elem);
				if (!allTheWay) {
					moved = false;
				}
			}
		}
		redraw();
	}

	private boolean moveElementTowardsRear(final CanvasElement moveElem) {
		final int index = elements.indexOf(moveElem);
		if (index == -1) {
			return false;
		}
		if (index < elements.size() - 1) {
			final CanvasElement swap = elements.get(index + 1);
			elements.set(index, swap);
			elements.set(index + 1, moveElem);
			return true;
		}
		return false;
	}


	/**
	 * Moves the selected elements towards index (0) (the front) of the 
	 * drawing order. If the parameter is false, selected elements will be 
	 * moved one position; if true, elements will all be moved to the very 
	 * start of the element list.
	 * 
	 * @param allTheWay true to move elements to the very start of the element 
	 * set
	 */
	public void moveSelectedElementsTowardsZero(final boolean allTheWay) {
		final List<CanvasElement> selected = mouseHandler.getSelection();
		if (selected == null || selected.isEmpty()) {
			return;
		}
		for (final CanvasElement elem: selected) {
			boolean moved = true;
			while (moved) {
				moved = moveElementTowardsZero(elem);
				if (!allTheWay) {
					moved = false;
				}
			}
		}
		redraw();
	}

	private boolean moveElementTowardsZero(final CanvasElement moveElem) {
		final int index = elements.indexOf(moveElem);
		if (index == -1) {
			return false;
		}
		if (index > 0) {
			final CanvasElement swap = elements.get(index - 1);
			elements.set(index, swap);
			elements.set(index - 1, moveElem);
			return true;
		}
		return false;
	}

	private void selectNextElement() {
		if (elements.isEmpty()) {
			return;
		}
		final List<CanvasElement> selected = mouseHandler.getSelection();
		CanvasElement nextSelection = null;
		if (selected == null || selected.isEmpty()) {
			nextSelection = elements.get(0);
		} else {
			final CanvasElement lastSelected = selected.get(selected.size() - 1);
			final int index = elements.indexOf(lastSelected);
			if (index == elements.size() - 1) {
				nextSelection = elements.get(0);
			} else {
				nextSelection = elements.get(index + 1);
			}
		}
		mouseHandler.setSelection(nextSelection);	
	}

	private void selectPreviousElement() {
		if (elements.isEmpty()) {
			return;
		}
		final List<CanvasElement> selected = mouseHandler.getSelection();
		CanvasElement prevSelection = null;
		if (selected == null || selected.isEmpty()) {
			prevSelection = elements.get(elements.size() - 1);
		} else {
			final CanvasElement firstSelected = selected.get(0);
			final int index = elements.indexOf(firstSelected);
			if (index == 0) {
				prevSelection = elements.get(elements.size() - 1);
			} else {
				prevSelection = elements.get(index - 1);
			}
		}
		mouseHandler.setSelection(prevSelection);	
	}
	/**
	 * Saves a snapshot of the current canvas into an image file.
	 * 
	 * @param filepath the file to write to
	 * @param imageType the SWT type constant for the image type
	 * 
	 * @return true if the canvas could be saved, false if not
	 */
	public boolean saveImage(final String filepath, final int imageType) {
		try {
			final Rectangle bounds = getUnpaddedBounds();
			GC gc = new GC (canvas);
			Image image = new Image (canvas.getDisplay(), bounds);
			gc.copyArea(image, 0, 0);
			gc.dispose ();
			gc = null;
			final ImageData imageData = image.getImageData();

			final ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] {imageData};
			imageLoader.save(filepath,imageType);

			image.dispose();
			image = null;
		} catch (final Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Class to handle paint events for the canvas.
	 */
	private class CanvasPaintListener implements PaintListener {

		/**
		 * {@inheritDoc}
		 * @see
		 * org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt
		 * .events.PaintEvent)
		 */
		@Override
		public void paintControl(final PaintEvent e) {

			// Set default colors and fonts to the GC, and fill the background
			if (defaultFont != null) {
				e.gc.setFont(defaultFont);
			}
			if (defaultBackgroundColor != null) {
				e.gc.setBackground(defaultBackgroundColor);
			}
			if (defaultForegroundColor != null) {
				e.gc.setForeground(defaultForegroundColor);
			}
			final Rectangle clientArea = canvas.getClientArea();
			e.gc.fillRectangle(clientArea);

			final List<CanvasElement> selectedElems = mouseHandler.getSelection();

			// Draw the grid first if using one
			if(useDrawingGrid) {
				drawGrid(e.gc);
			}

			// Draw all the elements in order, and draw selection handles on 
			// selected elements
			for (final CanvasElement elem : elements) {
				elem.draw(e.gc);
				if (editable && selectedElems != null && 
				        selectedElems.contains(elem)) {
					elem.drawHandles(e.gc);
				}
			}

			// Draw the selection tracker, if one exists
			if (mouseHandler.getSelectionTracker() != null) {
				mouseHandler.getSelectionTracker().draw(e.gc);
			}
		}

		private void drawGrid(final GC gc) {
			final int xMax = getUnpaddedBounds().width;
			final int yMax = getUnpaddedBounds().height;

			final Color fore = gc.getForeground();
			gc.setForeground(gridColor);

			for (int x = 0; x < xMax; x+= gridSizeHorizontal) {
				gc.drawLine(x, 0, x, yMax);	
			}

			for (int y = 0; y < yMax; y+= gridSizeVertical) {
				gc.drawLine(0, y, xMax, y);	
			}

			gc.setForeground(fore);
		}
	}

	/**
	 * This class handles key events for the canvas.
	 */
	private class CanvasKeyListener implements KeyListener {

		/**
		 * {@inheritDoc}
		 * @see
		 * org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events
		 * .KeyEvent)
		 */
		@Override
		public void keyPressed(final KeyEvent e) {

			int nudgeAmount = 1;
			switch (e.keyCode) {
			// Arrow moves one character or pixel without SHIFT, one grid line 
			// with SHIFT
			case SWT.ARROW_DOWN: 
			case SWT.ARROW_UP:
				if (mouseHandler.getSelection() == null ||
						mouseHandler.getSelection().isEmpty()) {
					return;
				}
				if (isCharacterLayout()) {
					nudgeAmount = getCharacterHeight();
				}
				if ((e.stateMask & SWT.SHIFT) != 0 && useGrid()) {
					nudgeAmount = 
					    mouseHandler.getSelectionGapToGrid(e.keyCode);
				}
				mouseHandler.nudgeSelectedElements(e.keyCode, nudgeAmount);
				break;

				// Arrow moves one character or pixel without SHIFT, one grid 
				// line with SHIFT
			case SWT.ARROW_LEFT:
			case SWT.ARROW_RIGHT:
				if (mouseHandler.getSelection() == null ||
						mouseHandler.getSelection().isEmpty()) {
					return;
				}
				if (isCharacterLayout()) {
					nudgeAmount = getCharacterWidth();
				}
				if ((e.stateMask & SWT.SHIFT) != 0 && useGrid()) {
					nudgeAmount = 
					    mouseHandler.getSelectionGapToGrid(e.keyCode);
				}
				mouseHandler.nudgeSelectedElements(e.keyCode, nudgeAmount);
				break;

				// Delete or backspace delete the selected element	
			case SWT.DEL:
			case SWT.BS:
				if (mouseHandler.getSelection() == null ||
						mouseHandler.getSelection().isEmpty()) {
					return;
				}
				removeSelectedElements();
				break;

				// TAB selects the next element, SHIT-TAB selects the previous
			case SWT.TAB:
				if ((e.stateMask & SWT.SHIFT) != 0) {
					selectPreviousElement();
				} else {
					selectNextElement();
				}
				break;  
			}
			redraw();			
		}

		/** 
		 * {@inheritDoc}
		 * @see
		 * org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events
		 * .KeyEvent)
		 */
		@Override
		public void keyReleased(final KeyEvent e) {
		}
	}

	/**
	 * Creates a Text Entry box on the canvas at the given location.
	 * '
	 * @param startLocation location at which to place the text box
	 * @param font font for the text, or null to use default
	 * @param initialText initial text value, or null for none
	 * 
	 * @return SWT Text object, which must be disposed of by the caller
	 */
	public Text createTextEntryBox(final ChillPoint startLocation, final Font font, 
	        final String initialText) {
		final Text text = new Text(canvas, SWT.BORDER);
		GC gc = new GC(canvas);
		if (font == null) {
			gc.setFont(defaultFont);
		} else {
			gc.setFont(font);
		}
		final int height = SWTUtilities.getFontCharacterHeight(gc);
		int width = SWTUtilities.getFontCharacterWidth(gc);
		if (initialText != null) {
			width = width * Math.max(30, initialText.length() + 2);
		} else {
			width = width * 30;
		}
		final int x = getXCoordinate(startLocation.getX());
		final int y = getYCoordinate(startLocation.getY());
		text.setFont(gc.getFont());
		text.setBounds(Math.max(0, x - 2), Math.max(0, y - 2), width, 
		        (height + 14));
		if (initialText != null) {
			text.setText(initialText);
		}
		text.setFocus();
		gc.dispose();
		gc = null;
		return text;
	}

	/**
	 * Maps the given X coordinate, which may be in pixels or characters, to 
	 * an absolute pixel location, depending on whether this object is 
	 * configured to supply coordinates in pixels or characters.
	 * 
	 * @param x the x coordinate to map
	 * 
	 * @return the absolute x coordinate for drawing, in pixels
	 */
	public int getXCoordinate(final int x) {
		final int width = getCharacterWidth();
		if (this.isCharacterLayout()) {
			return x * width;
		} else {
			return x;
		}
	}

	/**
	 * Gets the width, in pixels, of a single character in the current default 
	 * font.
	 * 
	 * @return width in pixels
	 */
	public int getCharacterWidth() {
		if (characterWidth == -1 || editable) {
			GC gc = new GC(canvas);
			gc.setFont(this.getDefaultFont());
			characterWidth = SWTUtilities.getFontCharacterWidth(gc);
			gc.dispose();
			gc = null;
		}
		return characterWidth;
	}

	/**
	 * Maps the given Y coordinate, which may be in pixels or characters, to 
	 * an absolute pixel location, depending on whether this object is 
	 * configured to supply coordinates in pixels or characters.
	 * 
	 * @param y the y coordinate to map
	 * 
	 * @return the absolute y coordinate for drawing, in pixels
	 */
	public int getYCoordinate(final int y) {
		final int charHeight = getCharacterHeight();
		if (isCharacterMode) {
			return y * charHeight;
		} else {
			return y;
		}
	}

	/**
	 * Gets the height, in pixels, of a single character in the current 
	 * default font.
	 * 
	 * @return height in pixels
	 */
	public int getCharacterHeight() {
		if (characterHeight == -1 || editable) {
			GC gc = new GC(canvas);
			gc.setFont(this.getDefaultFont());
			characterHeight = SWTUtilities.getFontCharacterHeight(gc);
			gc.dispose();
			gc = null;
		}
		return characterHeight;
	}

	/**
	 * Gets the current fixed layout view configuration being used by this 
	 * canvas.
	 * 
	 * @return FixedLayoutViewConfiguration, or null if none is currently set
	 */
	public IFixedLayoutViewConfiguration getViewConfig() {
		return viewConfig;
	}

	/**
	 * Enables or disables character-coordinate addressing in the current 
	 * canvas.
	 * 
	 * @param enable true to turn on character addressing, false for pixel
	 */
	public void setCharacterLayout(final boolean enable) {
		isCharacterMode = enable;
		lastCopyXOffset = -1;
		lastCopyYOffset = -1;
		redraw();
	}

	/**
	 * Indicates whether this canvas is using character coordinates.
	 * 
	 * @return true if using character coordinates, false if not
	 */
	public boolean isCharacterLayout() {
		return isCharacterMode;
	}
}
