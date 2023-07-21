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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Text;

import jpl.gds.monitor.perspective.view.fixed.fields.TextFieldConfiguration;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This class handles the mouse events on the FixedCanvas, including object
 * selection, drag, and resize.
 * 
 */
public class CanvasMouseHandler {

    private static final int JITTER_SIZE = 8;

	private final FixedCanvas canvasParent;
	private Point dragPoint;
	private List<CanvasElement> currentSelection = 
	    new ArrayList<CanvasElement>(1);
	private SelectionHandleId selectedHandleId = 
	    SelectionHandleId.HANDLE_NONE;
	private final ArrayList<CanvasSelectionListener> selectionListeners = 
	    new ArrayList<CanvasSelectionListener>();
	private SelectionTracker tracker;
	private int leftMargin = 0;
	private int rightMargin = 0;
	private int topMargin = 0;
	private int bottomMargin = 0;
	private boolean buildtime;
	private Point lastMousePosition = new Point(0,0);
	private Text textEntry;
	private TextElement currentTextElement;
	private String origText;

	/**
	 * Creates the CanvasMouseHandler for the given FixedCanvas.
	 * 
	 * @param canvas the FixedCanvas on which to activate mouse handling
	 */
	public CanvasMouseHandler(FixedCanvas canvas) {
		canvasParent = canvas;
	}

	/**
	 * Indicates whether we are in runtime or buildtime mode. This affects the
	 * behavior of the mouse operations.
	 * 
	 * @param en true to enabled build mode, false to disable
	 */
	public void setBuildtime(final boolean en) {
		buildtime = en;
	}

	/**
	 * Gets the currently selected CanvasElements.
	 * 
	 * @return selected list of CanvasElements, or null if none selected.
	 */
	public List<CanvasElement> getSelection() {
		if (currentSelection == null) {
			return null;
		} else {
			return currentSelection;
		}
	}

	/**
	 * Adds a SelectionListener.
	 * 
	 * @param addMe the listener to add
	 */
	public void addSelectionListener(final CanvasSelectionListener addMe) {
		synchronized (selectionListeners) {
			if (!selectionListeners.contains(addMe)) {
				selectionListeners.add(addMe);
			}
		}
	}

	/**
	 * Removes a SelectionListener.
	 * 
	 * @param removeMe the listener to remove
	 */
	public void removeSelectionListener(
	        final CanvasSelectionListener removeMe) {
		synchronized (selectionListeners) {
			if (selectionListeners.contains(removeMe)) {
				selectionListeners.remove(removeMe);
			}
		}
	}

	/**
	 * Notifies selection listeners that there has been a change in the 
	 * selected element.
	 * 
	 * @param selected true to notify there is a new selection, false to notify
	 *            the old selection has been deselected
	 * @param oldElems the list of old elements that were selected
	 * @param newElems the list of new elements to be selected           
	 */
	@SuppressWarnings("unchecked")
	public void notifySelectionListeners(
	        final boolean selected,
			final List<CanvasElement> oldElems, 
			final List<CanvasElement> newElems) {
		ArrayList<CanvasSelectionListener> clonedList = null;

		synchronized (selectionListeners) {
			clonedList = 
			    (ArrayList<CanvasSelectionListener>) selectionListeners.
			    clone();
		}
		for (CanvasSelectionListener listener : clonedList) {
			if (selected) {
				listener.elementsSelected(oldElems, newElems);
			} else {
				listener.elementsDeselected(oldElems, newElems);
			}
		}
	}

	/**
	 * Sets the currently selected canvas element (single element select).
	 * 
	 * @param elem the CanvasElement to select, or null to de-select all
	 */
	public void setSelection(final CanvasElement elem) {
		if (elem != null) {
			elem.setSelected(false);
		}
		List<CanvasElement> newElems = new ArrayList<CanvasElement>(1);
		if (elem != null) {
			newElems.add(elem);
		}
		notifySelectionListeners(false, currentSelection, null);
		currentSelection.clear();
		selectedHandleId = SelectionHandleId.HANDLE_NONE;
		if (elem != null) {
			elem.setSelected(true);
			currentSelection.add(elem);
			notifySelectionListeners(true, null, currentSelection);
		}
	}

	/**
	 * Sets the currently selected canvas elements (multi-element-select).
	 * 
	 * @param elems the CanvasElements to select, or null to deselect all
	 */
	public void setSelection(final List<CanvasElement> elems) {
		for (CanvasElement oldElem: currentSelection) {
			oldElem.setSelected(false);
		}
		notifySelectionListeners(false, currentSelection, null);
		currentSelection.clear();
		selectedHandleId = SelectionHandleId.HANDLE_NONE;
		currentSelection = elems;
		if (elems != null && elems.size() != 0) {
			for (CanvasElement newElem: currentSelection) {
				newElem.setSelected(true);
			}
			notifySelectionListeners(true, null, currentSelection);
		}
	}

	/**
	 * Adds an element to the currently selected canvas elements
	 * (multi-element-select).
	 * 
	 * @param elem the CanvasElement to add to the selection
	 */
	public void addSelection(CanvasElement elem) {
		elem.setSelected(true);
		List<CanvasElement> newElems = 
		    new ArrayList<CanvasElement>(currentSelection);
		newElems.add(elem);
		notifySelectionListeners(true, currentSelection, newElems);
		currentSelection.add(elem);
	}

	/**
	 * Gets the handle ID of the currently "grabbed" selection handle.
	 * 
	 * @return SelectionHandleId, or SelectionHandleId.HANDLE_NONE if no handle
	 *         "grabbed"
	 */
	public SelectionHandleId getSelectedHandle() {
		return selectedHandleId;
	}

	/**
	 * Sets the handle ID of the currently "grabbed" selection handle.
	 * 
	 * @param id the handle to select, or SelectionHandleId.HANDLE_NONE if
	 *            no handle "grabbed"
	 */
	public void setSelectedHandle(SelectionHandleId id) {
		selectedHandleId = id;
	}

	/**
	 * Resets the last mouse position back to 0,0
	 */
	public void resetMousePosition() {
		lastMousePosition = new Point(0,0);
	}

	/**
	 * Gets the selection tracker object, which tracks the size of a rectangle
	 * created by the user's dragging the mouse over the canvas.
	 * 
	 * @return SelectionTracker object, or null if no selection being tracked
	 */
	public SelectionTracker getSelectionTracker() {
		return tracker;
	}

	/**
	 * Gets the absolute x,y coordinate of the last known mouse position
	 * 
	 * @return Point object containing mouse coordinates
	 */
	public Point getLastMousePosition() {
		return lastMousePosition;
	}

	/**
	 * Moves all the selected elements a few pixels in any direction.
	 * 
	 * @param direction SWT.ARROW_UP, SWT.ARROW_DOWN, SWT.ARROW_LEFT,
	 *           SWT.ARROW_RIGHT
	 * @param amountIn number of pixels to nudge the object          
	 */
	public void nudgeSelectedElements(int direction, int amountIn) {
        int amount = amountIn;
		List<CanvasElement> nudgeUs = currentSelection;
		if (nudgeUs == null || nudgeUs.size() == 0) {
			return;
		}
		setMargins();

		for (int i = 0; i < amount; i++) {
			switch (direction) {
			case SWT.ARROW_UP:
				if (getMaxYOffset(-1) != -1) {
					return;
				}
				break;
			case SWT.ARROW_DOWN:
				if (getMaxYOffset(1) != 1) {
					return;
				}
				break;
			case SWT.ARROW_LEFT:
				if (getMaxXOffset(-1) != -1) {
					return;
				}
				break;
			case SWT.ARROW_RIGHT:
				if (getMaxXOffset(1) != 1) {
					return;
				}
				break;
			}
		}
		ChillPoint maxCoords = mapToLayoutCoordinates(new Point(
		        canvasParent.getUnpaddedBounds().width,
				canvasParent.getUnpaddedBounds().height));

		if (canvasParent.isCharacterLayout()) {
			if (direction == SWT.ARROW_DOWN || direction == SWT.ARROW_UP) {
				amount = amount / canvasParent.getCharacterHeight();
			} else {
				amount = amount / canvasParent.getCharacterWidth();
			}
		}
		for (CanvasElement nudgeMe : nudgeUs) {
			nudgeMe.nudge(direction, maxCoords, amount);
		}
	}

	// Computes the margins around the currently select block of objects
	private void setMargins() {
		leftMargin = canvasParent.getUnpaddedBounds().width;
		topMargin = canvasParent.getUnpaddedBounds().height;
		bottomMargin = 0;
		rightMargin = 0;

		for (CanvasElement elem : currentSelection) {
			Rectangle bounds = elem.getLastBounds();
			if (bounds == null) {
				continue;
			}
			if (bounds.x < leftMargin) {
				leftMargin = bounds.x;
			}
			if (bounds.y < topMargin) {
				topMargin = bounds.y;
			}
			if (bounds.x + bounds.width > rightMargin) {
				rightMargin = canvasParent.getUnpaddedBounds().width - bounds.x
				- bounds.width;
			}
			if (bounds.y + bounds.height > bottomMargin) {
				bottomMargin = canvasParent.getUnpaddedBounds().height - bounds.y
				- bounds.height;
			}
		}
	}

	// Computes how far the selection can be moved in the X direction
	private int getMaxXOffset(int xoffIn) {
		int xoff = xoffIn;
		if (xoff == 0) {
			return 0;
		}
		if (selectedHandleId == SelectionHandleId.HANDLE_NONE) {
			if (xoff < 0) {
				if (Math.abs(xoff) > leftMargin) {
					xoff = -leftMargin;
				}
			} else {
				if (xoff > rightMargin) {
					xoff = rightMargin;
				}
			}
		} else if (selectedHandleId == SelectionHandleId.HANDLE_TOP_LEFT ||
				selectedHandleId == SelectionHandleId.HANDLE_BOTTOM_LEFT) {
			if (xoff < 0) {
				if (Math.abs(xoff) > leftMargin) {
					xoff = -leftMargin;
				}
			}
		} else if (selectedHandleId == SelectionHandleId.HANDLE_TOP_RIGHT ||
				selectedHandleId == SelectionHandleId.HANDLE_BOTTOM_RIGHT) {
			if (xoff > 0) {
				if (xoff > rightMargin) {
					xoff = rightMargin;
				}
			}
		}
		return xoff;
	}

	// Computes how far the selection can be moved in the Y direction
	private int getMaxYOffset(int yoffIn) {
		int yoff = yoffIn;
		if (yoff == 0) {
			return 0;
		}
		if (selectedHandleId == SelectionHandleId.HANDLE_NONE) {
			if (yoff < 0) {
				if (Math.abs(yoff) > topMargin) {
					yoff = -topMargin;
				}
			} else {
				if (yoff > bottomMargin) {
					yoff = bottomMargin;
				}
			}
		} else if (selectedHandleId == SelectionHandleId.HANDLE_TOP_LEFT ||
				selectedHandleId == SelectionHandleId.HANDLE_TOP_RIGHT) {
			if (yoff < 0) {
				if (Math.abs(yoff) > topMargin) {
					yoff = -topMargin;
				}
			}
		} else if (selectedHandleId == SelectionHandleId.HANDLE_BOTTOM_LEFT ||
				selectedHandleId == SelectionHandleId.HANDLE_BOTTOM_RIGHT) {
			if (yoff > 0) {
				if (yoff > bottomMargin) {
					yoff = bottomMargin;
				}
			}
		}

		return yoff;
	}

	/**
	 * Enables the drag and selection mouse operations.
	 */
	public void activateMouse() {

		canvasParent.getCanvas().addMouseListener(new MouseAdapter() {
			/**
			 * On mouse down, elements are selected if in edit mode, and 
			 * buttons are marked "pressed" if in runtime mode.
			 * 
			 * @see
			 * org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt
			 * .events.MouseEvent)
			 */
			@Override
			public void mouseDown(MouseEvent e) {
				final MouseEvent event = e;

				Point pt = new Point(event.x, event.y);

				// If not on the canvas, return (why SWT sends mouse events 
				// to the canvas that are not on the canvas, I have no idea).
				if (!canvasParent.containsPoint(pt)) {
					return;
				}
				lastMousePosition = pt;

				// Get the list of canvas elements under the mouse cursor
				List<CanvasElement> matchedElems = canvasParent
				.getElementsForPoint(pt);

				// Nothing matched - cancel the current selection
				if (matchedElems.size() == 0 && buildtime) {
					if (currentSelection != null) {
						setSelection((CanvasElement) null);
						selectedHandleId = SelectionHandleId.HANDLE_NONE;
						canvasParent.redraw();
					}
					dragPoint = null;
					tracker = new SelectionTracker(canvasParent.getCanvas());
					CoordinateSystemType locType = 
					    canvasParent.getViewConfig().getCoordinateSystem();
					tracker.setCoordinateSystem(locType);
					ChillPoint mappedPt = mapToLayoutCoordinates(pt);
					tracker.setStartLocation(mappedPt);
					tracker.setEndLocation(mappedPt);
					canvasParent.redraw();

					// There were elements under the cursor	
				} else {

					// Figure out which of these is the best selection match
					CanvasElement bestMatch = canvasParent
					.selectBestMatch(matchedElems);

					// If in edit mode, add the best match to the current 
					// selection if SHIFT key is down. If no SHIFT, the best
					// match becomes the current selection
					if (buildtime) {
						if (!currentSelection.contains(bestMatch)) {
							if ((event.stateMask & SWT.SHIFT) != 0) {
								addSelection(bestMatch);
							} else {
								setSelection(bestMatch);
							}
							canvasParent.redraw();
						}

						// If only one object is selected, check to see if it 
						// was grabbed by one of its resize handles and save 
						// the handle ID
						if (currentSelection.size() == 1) {
							selectedHandleId = currentSelection.get(0)
							.getHandleIdForPoint(pt);
						} else {
							selectedHandleId = SelectionHandleId.HANDLE_NONE;
						}

						// Set the current mouse position as the drag start 
						// point
						dragPoint = pt;
						tracker = null;

						// Not in edit mode. If the best element match for the 
						// mouse position is a button, "click" the button	
					} else {
						if (bestMatch instanceof ButtonElement) {
							((ButtonElement)bestMatch).setPressed();
							setSelection(bestMatch);
							canvasParent.redraw();
						}
					}
				}
			}

			/**
			 * On mouse double click, the selection is canceled unless the 
			 * click is on a text field, in which case a text entry box is 
			 * draw on the canvas.
			 * 
			 * {@inheritDoc} 
			 * @see org.eclipse.swt.events.MouseAdapter#
			 * mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
			 */
			@Override
			public void mouseDoubleClick(MouseEvent event) {

				Point pt = new Point(event.x, event.y);

				// If not on the canvas, return (why SWT sends mouse events 
				// to the canvas that are not on the canvas, I have no idea).
				if (!canvasParent.containsPoint(pt)) {
					return;
				}
				lastMousePosition = pt;

				// Do nothing for double click if not in edit mode
				if (!buildtime) {
					return;
				}

				// Cancel the current selection
				setSelection((CanvasElement)null);
				selectedHandleId = SelectionHandleId.HANDLE_NONE;
                canvasParent.redraw();
				
				// Find the best element match at the mouse cursor
				List<CanvasElement> matchedElems = canvasParent
				.getElementsForPoint(pt);
				if (matchedElems.size() == 0) {
					return;
				}
				CanvasElement bestMatch = canvasParent.selectBestMatch(
				        matchedElems);

				// If it's not a text element, we are done
				if (!(bestMatch instanceof TextElement)) {
				    return;
				}

				// Otherwise, draw a text entry box on top of the text canvas 
				// element, so the user can enter a new text value
				currentTextElement = (TextElement)bestMatch;
				textEntry = canvasParent.createTextEntryBox(
				        currentTextElement.getStartLocation(), 
						currentTextElement.getFont(), 
						currentTextElement.getText());
				origText = currentTextElement.getText();

				// This key listener handles the rest of the user's text entry
				textEntry.addKeyListener(new KeyListener() {
					/**
					 * {@inheritDoc}
					 * @see org.eclipse.swt.events.KeyListener#
					 * keyPressed(org.eclipse.swt.events.KeyEvent)
					 */
					@Override
					public void keyPressed(KeyEvent keyEvent) {
					}

					/**
					 * {@inheritDoc}
					 * @see org.eclipse.swt.events.KeyListener#
					 * keyReleased(org.eclipse.swt.events.KeyEvent)
					 */
					@Override
					public void keyReleased(KeyEvent keyEvent) {
						try {
							if (textEntry == null || textEntry.isDisposed()) {
								return;
							}
							currentTextElement.setText(textEntry.getText());

							// If the user enters carriage return, he is done 
							// entering text
							if (keyEvent.keyCode == SWT.CR) {
								SWTUtilities.safeAsyncExec(
								        textEntry.getDisplay(), 
								        "Canvas Text Entry Field",  
								        new Runnable () {
									@Override
									public void run () {
										try {
											// Set the new text into the text 
										    // element's configuration object 
											((TextFieldConfiguration)
											        currentTextElement.
											        getFieldConfiguration()).
											        setText(textEntry.getText());

											// Clean up the text box
											textEntry.dispose();
											canvasParent.redraw();
											currentTextElement = null;
											textEntry = null;
											origText = null;
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								});
							} else if (keyEvent.keyCode == SWT.ESC) {
								currentTextElement.setText(origText);
								textEntry.dispose();
								textEntry = null;
								canvasParent.redraw();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				});
			}

			/**
			 * {@inheritDoc}
			 * @see org.eclipse.swt.events.MouseAdapter#
			 * mouseUp(org.eclipse.swt.events.MouseEvent)
			 */
			@Override
			public void mouseUp(MouseEvent event) {

				Point pt = new Point(event.x, event.y);
				
				// If not on the canvas, return (why SWT sends mouse events 
				// to the canvas that are not on the canvas, I have no idea).
				if (!canvasParent.containsPoint(pt)) {
					return;
				}
				lastMousePosition = pt;

				// If in runtime mode and only 1 button object is selected,
				// execute the button action
				if (!buildtime && currentSelection.size() == 1) {
					CanvasElement elem = currentSelection.get(0);
					if (elem instanceof ButtonElement) {
						((ButtonElement)elem).activate();
					}
					((ButtonElement)elem).setReleased();
					setSelection((CanvasElement)null);
					canvasParent.redraw();

				} else if (buildtime) {

					// Clean up any text entry box left from a previous edit
					if (textEntry != null) {
						if (!currentTextElement.getLastBounds().contains(pt)) {
							currentTextElement.setText(origText);
							textEntry.dispose();
							canvasParent.redraw();
							currentTextElement = null;
							textEntry = null;
						}
					}

					// If a drag start point is set, finish the drag
					if (dragPoint != null) {

						setMargins();

						// Compute maximum allowed drag distance
						int xOffset = getMaxXOffset(pt.x - dragPoint.x);
						int yOffset = getMaxYOffset(pt.y - dragPoint.y);
						int xMax = canvasParent.getUnpaddedBounds().width;
						int yMax = canvasParent.getUnpaddedBounds().height;

						ChillPoint maxCoords = mapToLayoutCoordinates(
						        new Point(xMax, yMax));

						// Determine if the object has actually been dragged
						// In character coordinate mode, drags only take 
						// effect when the mouse has been moved at least one 
						// character
						if ((!canvasParent.isCharacterLayout()  && 
						        (xOffset != 0 || yOffset != 0)) ||
								(canvasParent.isCharacterLayout() && 
								        (Math.abs(xOffset) >= 
								            canvasParent.getCharacterWidth() ||
										Math.abs(yOffset) >= 
										    canvasParent.
										    getCharacterHeight()))) {

							// Move/resize the selected canvas elements to 
						    // their new position
							for (CanvasElement dragWidget : currentSelection) {
								ChillPoint dragStart = 
								    dragWidget.getStartLocation();
								ChillPoint dragEnd = 
								    dragWidget.getEndLocation();
								Point oldStart = 
								    mapToPixelCoordinates(dragStart);
								oldStart.x += xOffset;
								oldStart.y += yOffset;
								if (dragEnd != null) {
									Point oldEnd = 
									    mapToPixelCoordinates(dragEnd);
									oldEnd.x += xOffset;
									oldEnd.y += yOffset;
									dragWidget.moveAndResize(
									        mapToLayoutCoordinates(oldStart), 
											mapToLayoutCoordinates(oldEnd),
											maxCoords);
								} else {
									dragWidget.moveAndResize(
									        mapToLayoutCoordinates(oldStart), 
									        null, 
									        maxCoords);
								}
							}
							canvasParent.redraw();
						}
						// end the drag
						dragPoint = null;

					} else {
						// We were not dragging an object. But this may be a 
					    // multiple select. If so, select objects in the 
					    // tracker's rectangle
						if (tracker != null) {
							ChillPoint cpt = mapToLayoutCoordinates(pt);
							tracker.setEndLocation(cpt);
							List<CanvasElement> elemsInTheRect = canvasParent
							.getElementsForRectangle(tracker
									.getLastBounds());
							if (elemsInTheRect.size() > 0) {
								setSelection(elemsInTheRect);
							}

							// End the multiple selection
							tracker = null;
							canvasParent.redraw();
						}
					}
				}
			}
		});

		canvasParent.getCanvas().addMouseMoveListener(new MouseMoveListener() {
			/**
			 * {@inheritDoc}
			 * @see org.eclipse.swt.events.MouseMoveListener#
			 * mouseMove(org.eclipse.swt.events.MouseEvent)
			 */
			@Override
			public void mouseMove(MouseEvent e) {

				Point pt = new Point(e.x, e.y);
				// If the mouse is beyond the bounds of the canvas,
				// do nothing
				if (!canvasParent.containsPoint(pt)) {
					return;
				}
				if (dragPoint != null) {

					// Drag operation in progress
					setMargins();
					boolean changed = false;
					int xOffset = getMaxXOffset(pt.x - dragPoint.x);
					int yOffset = getMaxYOffset(pt.y - dragPoint.y);
					int xMax = canvasParent.getUnpaddedBounds().width;
					int yMax = canvasParent.getUnpaddedBounds().height;
					ChillPoint maxCoords = mapToLayoutCoordinates(
					        new Point(xMax, yMax));

					// Only take action if the mouse cursor has moved on pixel 
					// in pixel mode, or one character in character mode.
					if ((!canvasParent.isCharacterLayout()  && 
					        (xOffset != 0 || yOffset != 0)) ||
							(canvasParent.isCharacterLayout() && 
							        (Math.abs(xOffset) >= 
							            canvasParent.getCharacterWidth() ||
									Math.abs(yOffset) >= 
									    canvasParent.getCharacterHeight()))) {
						changed = true;

						// For each selected element
						for (CanvasElement dragWidget : currentSelection) {

							// Compute new start and end points for the object
							ChillPoint dragStart = dragWidget.getStartLocation();
							ChillPoint dragEnd = dragWidget.getEndLocation();
							Point oldStart = mapToPixelCoordinates(dragStart);
							Point oldEnd = null;
							Point newStart = new Point(oldStart.x + xOffset,
									oldStart.y + yOffset);
							Point newEnd = null;
							if (dragEnd != null) {
								oldEnd = mapToPixelCoordinates(dragEnd);
								newEnd = new Point(oldEnd.x + xOffset, oldEnd.y
										+ yOffset);
							} 
							Point p = null;

							// If a handle is grabbed, and the shape of the 
							// object is modifiable, then this is a stretch 
							// rather than a drag
							if (selectedHandleId != SelectionHandleId.
							        HANDLE_NONE
									&& dragWidget.isShapeMorphable()) {
								switch (selectedHandleId) {
								case HANDLE_TOP_LEFT:
									dragWidget.moveAndResize(
									        mapToLayoutCoordinates(pt), 
									        null,
											maxCoords);
									dragPoint = newStart;
									break;
								case HANDLE_TOP_RIGHT:
								    p = new Point(oldStart.x, pt.y);
								    dragWidget.setStartLocation(
								            mapToLayoutCoordinates(p));
									if (oldEnd != null) {
									    Point ep = new Point(pt.x,
												oldEnd.y);
									    dragWidget.moveAndResize(
									            mapToLayoutCoordinates(p), 
									            mapToLayoutCoordinates(ep), 
									            maxCoords);
									}
									break;
								case HANDLE_BOTTOM_LEFT:
								    p = new Point(pt.x,oldStart.y);
								    dragWidget.setStartLocation(
								            mapToLayoutCoordinates(p));
								    if (oldEnd != null) {
								        Point ep = new Point(oldEnd.x, pt.y);
								        dragWidget.moveAndResize(
								                mapToLayoutCoordinates(p), 
												mapToLayoutCoordinates(ep),
												maxCoords);
								    } else {
									    dragWidget.moveAndResize(
									            mapToLayoutCoordinates(p), 
									            null, 
									            maxCoords);
									}
									break;
								case HANDLE_BOTTOM_RIGHT:
								    dragWidget.moveAndResize(
								            null, 
								            mapToLayoutCoordinates(pt), 
								            maxCoords);
								    break;
								}
							} else {
							    // Otherwise just move the whole object
								if (oldEnd != null) {
								    dragWidget.moveAndResize(
								            mapToLayoutCoordinates(newStart), 
											mapToLayoutCoordinates(newEnd),
											maxCoords);
								} else {
								    dragWidget.moveAndResize(
								            mapToLayoutCoordinates(newStart), 
								            null,
											maxCoords);
								}
							}
						}
						if (changed) {
							dragPoint = pt;
						}
						canvasParent.redraw();
					}
				} else {
				    // Nothing selected, not dragging. Just update the 
				    // selection tracker
					if (tracker != null) {
						tracker.setEndLocation(mapToLayoutCoordinates(pt));
						if (Math.abs(pt.x - canvasParent.getXCoordinate(
						        tracker.getStartLocation().getX())) 
						        > JITTER_SIZE
								|| Math.abs(pt.y - canvasParent.getYCoordinate(
								        tracker.getEndLocation().getY())) 
								        > JITTER_SIZE) {
							canvasParent.redraw();
						}
					}
				}
			}
		});
	}

	/**
	 * Computes the offset from the minimum X,Y coordinate for all selected
	 * objects and the nearest grid line.
	 * 
	 * @param keyCode SWT.ARROW_UP, SWT.ARROW_LEFT, SWT.ARROW_DOWN, 
	 *                SWT.ARROW_UP representing the direction in which to 
	 *                compute the offset
	 *                
	 * @return offset to nearest grid line in the indicated direction in pixels
	 */
	public int getSelectionGapToGrid(int keyCode) {
		int xMin = Integer.MAX_VALUE;
		int yMin = Integer.MAX_VALUE;
		for (CanvasElement elem: currentSelection) {
			xMin = Math.min(elem.getLastBounds().x, xMin);
			yMin = Math.min(elem.getLastBounds().y, yMin);
		}
		boolean useChars = canvasParent.isCharacterLayout();
		int gridSize = canvasParent.getGridSize();
		if (useChars) {
			if (keyCode == SWT.ARROW_DOWN || keyCode == SWT.ARROW_UP) {
				gridSize *= canvasParent.getCharacterHeight();
			} else {
				gridSize *= canvasParent.getCharacterWidth();
			}
		}
		int gapToGrid = gridSize;
		int gridSquares = 0;
		int rem = 0;
		switch (keyCode) {
		case SWT.ARROW_DOWN: 

			gridSquares = (int)Math.floor(((float)yMin / (float)gridSize));
			rem = yMin - (gridSize * gridSquares);
			if (rem != 0) {
				gapToGrid = gapToGrid - rem;
			}
			break;

		case SWT.ARROW_UP: 
			gridSquares = (int)Math.ceil(((float)yMin / (float)gridSize));
			rem = (gridSize * gridSquares) - yMin;
			if (rem != 0) {
				gapToGrid = gapToGrid - rem;
			}
			break;
		case SWT.ARROW_RIGHT: 
			gridSquares = (int)Math.floor(((float)xMin / (float)gridSize));
			rem = xMin - (gridSize * gridSquares);
			if (rem != 0) {
				gapToGrid = gapToGrid - rem;
			}
			break;

		case SWT.ARROW_LEFT: 
			gridSquares = (int)Math.ceil(((float)xMin / (float)gridSize));
			rem = (gridSize * gridSquares) - xMin;
			if (rem != 0) {
				gapToGrid = gapToGrid - rem;
			}
			break;
		}
		return gapToGrid;
	}

	private Point mapToPixelCoordinates(ChillPoint cp) {
		return new Point(canvasParent.getXCoordinate(cp.getX()), 
		        canvasParent.getYCoordinate(cp.getY()));
	}

	private ChillPoint mapToLayoutCoordinates(Point pIn) {
		ChillPoint pOut = new ChillPoint(pIn.x, pIn.y,
		        canvasParent.getViewConfig().getCoordinateSystem());
		if (pOut.getCoordinateSystem().equals(
		        CoordinateSystemType.CHARACTER)) {
			pOut.setX(pIn.x / canvasParent.getCharacterWidth());
			pOut.setY(pIn.y / canvasParent.getCharacterHeight());
		}
		return pOut;
	}
}
