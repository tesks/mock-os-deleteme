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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.canvas.support.FontSupport;
import jpl.gds.monitor.canvas.support.OneColorSupport;
import jpl.gds.monitor.canvas.support.TransparencySupport;
import jpl.gds.monitor.canvas.support.TwoColorSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.HeaderFieldConfiguration;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.types.ChillPoint;

/**
 * This HeaderElement is a canvas "super" element that actually contains a 
 * collection of other canvas elements that are configured and positioned as 
 * a group.
 *
 */
public class HeaderElement extends SingleCoordinateCanvasElement 
implements TwoColorSupport, TransparencySupport {

	/**
	 * The default selection priority for this CanvasElement
	 */
	private static final int SELECTION_PRIORITY = 1;

	private Color foreground;
	private Color background;
	private boolean transparent;
	private List<CanvasElement> childElems;

	/**
	 * Creates a HeaderElement with the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 */
	public HeaderElement(final Canvas parent) {
		super(parent, FixedFieldType.HEADER);
		this.setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * Creates a HeaderElement with the given fixed view field configuration 
	 * and parent canvas.
	 * 
	 * @param parent the parent Canvas widget
	 * 
	 * @param headerConfig the HeaderFieldConfiguration object for this 
	 * element from the perspective
	 */
	public HeaderElement(final Canvas parent, final HeaderFieldConfiguration headerConfig) {
		super(parent, headerConfig);
		updateFieldsFromConfig();
		this.setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.OneColorSupport#getForeground()
	 */
	@Override
    public Color getForeground() {
		return foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.OneColorSupport#setForeground(org.eclipse.swt.graphics.Color)
	 */
	@Override
    public void setForeground(final Color foreground) {
		this.foreground = foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TwoColorSupport#getBackground()
	 */
	@Override
    public Color getBackground() {
		return background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TwoColorSupport#setBackground(org.eclipse.swt.graphics.Color)
	 */
	@Override
    public void setBackground(final Color background) {
		this.background = background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TransparencySupport#setTransparent(boolean)
	 */
	@Override
    public void setTransparent(final boolean transparent) {
		this.transparent = transparent;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TransparencySupport#isTransparent()
	 */
	@Override
    public boolean isTransparent() {
		return transparent;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#draw(org.eclipse.swt.graphics.GC)
	 */
	@Override
    public void draw(final GC gc) {

	    if(!displayMe  && this.getFieldConfiguration().getCondition() != null) {
            return;
        }
	    
		if (childElems == null) {
			return;
		}
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		// For each sub-element of the header...
		for (final CanvasElement child: childElems) {

			// Compute child start and end locations, factoring in the header 
		    // position
			final ChillPoint start = child.getStartLocation();
			ChillPoint end = null;
			if (child instanceof DualCoordinateCanvasElement) {
				end = ((DualCoordinateCanvasElement)child).getEndLocation();
			}

			final ChillPoint newStart = new ChillPoint(
			        start.getX() + startPoint.getX(), 
					start.getY() + startPoint.getY(), getCoordinateSystem());
			ChillPoint newEnd = null;
			child.setStartLocation(newStart);

			if (end != null) {
				newEnd = new ChillPoint(end.getX() + 
				        startPoint.getX(), end.getY() + 
				        startPoint.getY(), getCoordinateSystem());
				((DualCoordinateCanvasElement)child).setEndLocation(newEnd);
			}


			Color childForeground = null;
			Color childBackground = null;
			boolean childTransparent = false;
			Font childFont = null;

			// Set child foreground and background colors from header 
			// foreground and background colors
			if (child instanceof OneColorSupport) {
				childForeground = ((OneColorSupport)child).getForeground();
				((OneColorSupport)child).setForeground(foreground);
			}
			if (child instanceof TwoColorSupport) {
				childBackground = ((TwoColorSupport)child).getBackground();
				((TwoColorSupport)child).setBackground(background);
			}

			if (child instanceof TransparencySupport) {
				childTransparent = 
				    ((TransparencySupport)child).isTransparent();
				((TransparencySupport)child).setTransparent(transparent);
			}


			// Set child font from the canvas
			if (child instanceof FontSupport) {
				childFont = ((FontSupport)child).getFont();
				((FontSupport)child).setFont(gc.getFont());
			}

			// Draw the child
			child.draw(gc);

			// Compute child bounds
			final Rectangle childBounds = child.getLastBounds();
			minX = Math.min(childBounds.x, minX);
			maxX = Math.max(childBounds.x, maxX);
			minY = Math.min(childBounds.y, minY);
			maxY = Math.max(childBounds.y, maxY);

			minX = Math.min(childBounds.x + childBounds.width, minX);
			maxX = Math.max(childBounds.x + childBounds.width, maxX);
			minY = Math.min(childBounds.y + childBounds.height, minY);
			maxY = Math.max(childBounds.y + childBounds.height, maxY);

			// Restore old child settings
			child.setStartLocation(start);

			if (end != null) {
				((DualCoordinateCanvasElement)child).setEndLocation(end);
			}

			if (child instanceof OneColorSupport) {
				((OneColorSupport)child).setForeground(childForeground);
			}
			if (child instanceof TwoColorSupport) {
				((TwoColorSupport)child).setBackground(childBackground);
			}
			if (child instanceof TransparencySupport) {
				((TransparencySupport)child).setTransparent(childTransparent);
			}
			if (child instanceof FontSupport) {
				((FontSupport)child).setFont(childFont);
			}
		}
		setLastBounds(minX, minY, maxX, maxY);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#updateFieldsFromConfig()
	 */
	@Override
	protected void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();

		final HeaderFieldConfiguration headerConfig = 
		    (HeaderFieldConfiguration)fieldConfig;

		if(headerConfig.getForeground() != null)
		{
			if (foreground != null && !foreground.isDisposed()) {
				foreground.dispose();
				foreground = null;
			}
			this.setForeground(ChillColorCreator.getColor(
			        headerConfig.getForeground()));
		} 
		if(headerConfig.getBackground() != null)
		{
			if (background != null && !background.isDisposed()) {
				background.dispose();
				background = null;
			}
			this.setBackground(ChillColorCreator.getColor(
			        headerConfig.getBackground()));
		} 
		this.setTransparent(headerConfig.isTransparent());

		createSubElements(headerConfig.getFieldConfigs());
	}

	private void createSubElements(final List<IFixedFieldConfiguration> configs) {
		if (configs == null) {
			return;
		}

		childElems = new ArrayList<CanvasElement>(configs.size());

		for (final IFixedFieldConfiguration config: configs) {
			final CanvasElement element = 
			    CanvasElementFactory.create(config, parent);
			if (element instanceof ChannelElement) {
				((ChannelElement)element).setTextFromChannelDefinition();
			}
			childElems.add(element);
		}
	}

	/**
	 * Gets the current list of child elements inside this header.
	 * 
	 * @return List of CanvasElements, or null if none defined
	 */
	public List<CanvasElement> getChildElements() {
		return childElems;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#enableEditMode(boolean)
	 */
	@Override
	public void enableEditMode(final boolean enable) {
		isEditMode = enable;
		for (final CanvasElement elem: childElems) {
			elem.enableEditMode(enable);
		}
	}
}
