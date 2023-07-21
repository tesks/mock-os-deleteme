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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.canvas.support.DualCoordinateSupport;
import jpl.gds.monitor.canvas.support.FontSupport;
import jpl.gds.monitor.canvas.support.LineSupport;
import jpl.gds.monitor.canvas.support.TextSupport;
import jpl.gds.monitor.canvas.support.TransparencySupport;
import jpl.gds.monitor.canvas.support.TwoColorSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.LineStyle;
import jpl.gds.monitor.perspective.view.fixed.fields.BoxFieldConfiguration;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * This fixed view Canvas Element is a line drawing of a box, with optional
 * title. It also supports foreground and background colors, transparency,
 * line style settings, and a font setting.
 */
public class BoxElement extends DualCoordinateCanvasElement
implements TwoColorSupport, LineSupport, DualCoordinateSupport, TextSupport,
TransparencySupport, FontSupport {
	
    /**
     * Default selection priority for this CanvasElement.
     */
    private static final int SELECTION_PRIORITY = 4;
	
    /**
     * Indentation for the title so that it is not directly on the left
     * corner of the box.
     */
    private static final int TITLE_INDENT = 5;
	
    /**
     * Width of the box border; default is 1pt.
     */
	private int thickness = 1;
	
	/**
	 * Optional title on the upper left corner of the box.
	 */
	private String title;
	
	/**
	 * Color of the border and title.
	 */
	private Color foreground;
	
	/**
	 * Fill color of the box.
	 */
	private Color background;
	
	/**
	 * Font for the title on the box, if any.
	 */
	private Font font;
	
	/**
	 * Style for the box border; may be solid or dashed.
	 */
	private int lineStyle = SWT.LINE_SOLID;
	
	/**
	 * True if canvas background and other elements behind this box
	 * should be shown, false otherwise.
	 */
	private boolean transparent;
	
	/**
	 * True if background and foreground for the box title should be
	 * switched, false if they should remain the same.
	 */
	private boolean reverse;
	
	/**
	 * Creates a BoxElement on the given Canvas.
	 *
	 * @param parent the parent Canvas widget
	 */
	public BoxElement(final Canvas parent) {
		super(parent, FixedFieldType.BOX);
		this.setSelectionPriority(SELECTION_PRIORITY);
	}
	
	/**
	 * Creates a BoxElement with the given field configuration on the given
	 * Canvas.
	 *
	 * @param parent the parent Canvas widget
	 * @param boxConfig the BoxFieldConfiguration object from the
	 * perspective
	 */
	public BoxElement(
					  final Canvas parent, final BoxFieldConfiguration boxConfig) {
		super(parent, boxConfig);
		updateFieldsFromConfig();
		this.setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#getLineStyle()
	 */
	@Override
    public final int getLineStyle() {
		return lineStyle;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#setLineStyle(int)
	 */
	@Override
    public final void setLineStyle(final int style) {
		this.lineStyle = style;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.FontSupport#getFont()
	 */
	@Override
    public final Font getFont() {
		return font;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.FontSupport#setFont(org.eclipse.swt.graphics.Font)
	 */
	@Override
    public final void setFont(final Font letterType) {
		this.font = letterType;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#getThickness()
	 */
	@Override
    public final int getThickness() {
		return thickness;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#setThickness(int)
	 */
	@Override
    public final void setThickness(final int lineThickness) {
		this.thickness = lineThickness;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TextSupport#getText()
	 */
	@Override
    public final String getText() {
		return title;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TextSupport#setText(java.lang.String)
	 */
	@Override
    public final void setText(final String titleText) {
		this.title = titleText;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.OneColorSupport#getForeground()
	 */
	@Override
    public final Color getForeground() {
		return foreground;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.OneColorSupport#setForeground(org.eclipse.swt.graphics.Color)
	 */
	@Override
    public final void setForeground(final Color color) {
		this.foreground = color;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TwoColorSupport#getBackground()
	 */
	@Override
    public final Color getBackground() {
		return background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TwoColorSupport#setBackground(org.eclipse.swt.graphics.Color)
	 */
	@Override
    public final void setBackground(final Color color) {
		background = color;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TransparencySupport#setTransparent(boolean)
	 */
	@Override
    public final void setTransparent(final boolean transparency) {
		transparent = transparency;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TransparencySupport#isTransparent()
	 */
	@Override
    public final boolean isTransparent() {
		return transparent;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#draw(org.eclipse.swt.graphics.GC)
	 */
	@Override
    public final void draw(final GC gc) {
		
	    if (!displayMe
			&& this.getFieldConfiguration().getCondition() != null) {
	        return;
	    }
		
		final Color oldBackground = gc.getBackground();
		
		saveGcSettings(gc);
		
		gc.setLineWidth(thickness);
		gc.setLineStyle(lineStyle);
		
		if (foreground != null) {
			gc.setForeground(foreground);
		}
		if (background != null) {
			gc.setBackground(background);
		}
		
		final int x = getXCoordinate(startPoint.getX(), gc);
		final int y = getYCoordinate(startPoint.getY(), gc);
		final int ex = getXCoordinate(endPoint.getX(), gc);
		final int ey = getYCoordinate(endPoint.getY(), gc);
		
		// Draw the box and fill it
		gc.drawRectangle(x, y, ex - x, ey - y);
		setLastBounds(x, y, ex, ey);
		final int fillLess = thickness % 2 == 0 ? 2 : 1;
		if (!transparent) {
			gc.fillRectangle(
							 x + 1,
							 y + 1,
							 ex - x - fillLess,
							 ey - y - fillLess);
		}
		
		if (font != null) {
			gc.setFont(font);
		}
		
		// Draw the box title, if any
		if (title != null) {
			
			final int titleX = Math.min(x, ex) + TITLE_INDENT;
			final int titleY = Math.min(y, ey) - (
											SWTUtilities.getFontCharacterHeight(gc) / 2);
			
			if (transparent) {
				final Color save = gc.getBackground();
				gc.setBackground(oldBackground);
				final int wx = SWTUtilities.getFontCharacterWidth(gc) *
				title.length();
				final int wy = SWTUtilities.getFontCharacterHeight(gc);
				gc.fillRectangle(titleX, titleY, wx, wy);
				gc.setBackground(save);
			}
			
			if (reverse && !transparent) {
				final Color save = gc.getForeground();
				gc.setForeground(gc.getBackground());
				gc.setBackground(save);
			}
			gc.drawText(title, titleX, Math.max(
												0, titleY), transparent ?
						SWT.DRAW_TRANSPARENT : 0);
		}
		restoreGcSettings(gc);
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#updateFieldsFromConfig()
	 */
	@Override
    protected final void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();
		final BoxFieldConfiguration boxConfig =
		(BoxFieldConfiguration) fieldConfig;
		
		this.setEndLocation(boxConfig.getEndCoordinate());
		this.setText(boxConfig.getText());
		if (boxConfig.getLineThickness() > 0) {
			this.setThickness(boxConfig.getLineThickness());
		}
		if (boxConfig.getForeground() != null) {
			foreground = ChillColorCreator.getColor(
													boxConfig.getForeground());
		}
		if (boxConfig.getFont() != null) {
			this.setFont(ChillFontCreator.getFont(
												  boxConfig.getFont()));
			reverse = boxConfig.getFont().getReverseFlag();
		}
		if (boxConfig.getBackground() != null) {
			background = ChillColorCreator.getColor(
													boxConfig.getBackground());
		}
		if (boxConfig.getLineStyle() != null) {
			if (boxConfig.getLineStyle().equals(LineStyle.SOLID)) {
				this.setLineStyle(SWT.LINE_SOLID);
			}
			else if (boxConfig.getLineStyle().equals(
													 LineStyle.DASHED)) {
				this.setLineStyle(SWT.LINE_DASH);
			}
		}
		setTransparent(boxConfig.isTransparent());
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#isShapeMorphable()
	 */
	@Override
	public final boolean isShapeMorphable() {
		return true;
	}
}
