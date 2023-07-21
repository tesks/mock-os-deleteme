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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.canvas.support.DualCoordinateSupport;
import jpl.gds.monitor.canvas.support.LineSupport;
import jpl.gds.monitor.canvas.support.OneColorSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.LineStyle;
import jpl.gds.monitor.perspective.view.fixed.fields.LineFieldConfiguration;
import jpl.gds.shared.swt.ChillColorCreator;

/**
 * This CanvasElement is a drawn line. It support a foreground color, a line 
 * style, and a line width.
 *
 */
public class LineElement extends DualCoordinateCanvasElement implements 
OneColorSupport, LineSupport, DualCoordinateSupport{

	/**
	 * The default selection priority of this CanvasElement
	 */
	private static final int SELECTION_PRIORITY = 2;

	private Color foreground;
	private int thickness = 1;
	private int lineStyle = SWT.LINE_SOLID;

	/**
	 * Creates a LineElement on the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 */
	public LineElement(Canvas parent) {
		super(parent, FixedFieldType.LINE);
		setLineStyle(SWT.LINE_SOLID);
		setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * Creates a LineElement with the given fixed view field configuration 
	 * on the given parent canvas.
	 * 
	 * @param parent the parent Canvas widget
	 * @param lineConfig the LineFieldConfiguration object for this element 
	 * from the perspective
	 */
	public LineElement(Canvas parent, LineFieldConfiguration lineConfig) {
		super(parent, lineConfig);
		updateFieldsFromConfig();
		setSelectionPriority(SELECTION_PRIORITY);
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
    public void setForeground(Color foreground) {
		this.foreground = foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#getLineStyle()
	 */
	@Override
    public int getLineStyle() {
		return lineStyle;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#setLineStyle(int)
	 */
	@Override
    public void setLineStyle(int lineStyle) {
		this.lineStyle = lineStyle;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#getThickness()
	 */
	@Override
    public int getThickness() {
		return thickness;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.LineSupport#setThickness(int)
	 */
	@Override
    public void setThickness(int thickness) {
		this.thickness = thickness;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#draw(org.eclipse.swt.graphics.GC)
	 */
	@Override
    public void draw(GC gc) {
		
	    if(!displayMe && this.getFieldConfiguration().getCondition() != null) {
            return;
        }
	    
	    saveGcSettings(gc);

		gc.setLineWidth(thickness); 
		gc.setLineStyle(lineStyle);

		if (foreground != null) {
			gc.setForeground(foreground);
		}

		int x = getXCoordinate(startPoint.getX(), gc);
		int y = getYCoordinate(startPoint.getY(), gc);
		int ex = getXCoordinate(endPoint.getX(), gc);
		int ey = getYCoordinate(endPoint.getY(), gc);

		gc.drawLine(x, y, ex, ey);

		setLastBounds(x, y, ex, ey);

		restoreGcSettings(gc);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#updateFieldsFromConfig()
	 */
	@Override
	protected void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();

		LineFieldConfiguration lineConfig = (LineFieldConfiguration)fieldConfig;

		this.setEndLocation(lineConfig.getEndCoordinate());

		if(lineConfig.getForeground() != null)
		{
			if (foreground != null && !foreground.isDisposed()) {
				foreground.dispose();
				foreground = null;
			}
			foreground = ChillColorCreator.getColor(lineConfig.getForeground());
		}
		if(lineConfig.getLineThickness() > 0)
		{
			this.setThickness(lineConfig.getLineThickness());
		}
		if(lineConfig.getLineStyle() != null)
		{
			if(lineConfig.getLineStyle().equals(LineStyle.SOLID))
			{
				this.setLineStyle(SWT.LINE_SOLID);
			}
			else if(lineConfig.getLineStyle().equals(LineStyle.DASHED))
			{
				this.setLineStyle(SWT.LINE_DASH);
			}
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#drawHandles(org.eclipse.swt.graphics.GC)
	 */
	@Override
	public void drawHandles(GC gc) {
		handles.get(SelectionHandleId.HANDLE_TOP_LEFT).drawHandle(gc);
		handles.get(SelectionHandleId.HANDLE_BOTTOM_RIGHT).drawHandle(gc);
	} 

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#getHandleIdForPoint(org.eclipse.swt.graphics.Point)
	 */
	@Override
	public SelectionHandleId getHandleIdForPoint(Point pt) {
		SelectionHandleId result = SelectionHandleId.HANDLE_NONE;

		for (SelectionHandleId handle: handles.keySet()) {
			if (handles.get(handle).containsPoint(pt)) {
				result = handle;
			}
		}
		if (result.equals(SelectionHandleId.HANDLE_TOP_RIGHT) || 
		        result.equals(SelectionHandleId.HANDLE_BOTTOM_LEFT)) {
			result = SelectionHandleId.HANDLE_NONE;
		}
		return result;
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
	@Override
	protected void setHandles(int sx, int sy, int ex, int ey) {
		handles.get(SelectionHandleId.HANDLE_TOP_LEFT).setCenterX(sx);
		handles.get(SelectionHandleId.HANDLE_TOP_LEFT).setCenterY(sy);
		handles.get(SelectionHandleId.HANDLE_TOP_RIGHT).setCenterX(-1);
		handles.get(SelectionHandleId.HANDLE_TOP_RIGHT).setCenterY(-1);
		handles.get(SelectionHandleId.HANDLE_BOTTOM_LEFT).setCenterX(-1);
		handles.get(SelectionHandleId.HANDLE_BOTTOM_LEFT).setCenterY(-1);		
		handles.get(SelectionHandleId.HANDLE_BOTTOM_RIGHT).setCenterX(ex);
		handles.get(SelectionHandleId.HANDLE_BOTTOM_RIGHT).setCenterY(ey);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#isShapeMorphable()
	 */
	@Override
	public boolean isShapeMorphable() {
		return true;
	}
}
