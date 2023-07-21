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

import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.perspective.view.fixed.LineStyle;
import jpl.gds.monitor.perspective.view.fixed.fields.BoxFieldConfiguration;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;

/**
 * A special CanvasElement that acts as a "selection rectangle" that is created
 * when the user drags the mouse over the fixed canvas. This allows the user
 * to select multiple objects by dragging a box around them.
 *
 */
public class SelectionTracker extends BoxElement {

	/**
	 * Creates a SelectionTracker on the given canvas.
	 * 
	 * @param parent the parent Canvas widget
	 */
	public SelectionTracker(Canvas parent) {
		super(parent);
		BoxFieldConfiguration boxConfig = new BoxFieldConfiguration();
		boxConfig.setLineThickness(2);
		boxConfig.setLineStyle(LineStyle.DASHED);
		boxConfig.setForeground(new ChillColor(ColorName.GREY));
		boxConfig.setTransparent(true);
		this.setFieldConfiguration(boxConfig);
	}
}
