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

import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.BoxFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ButtonFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.HeaderFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ImageFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.LineFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration;

/**
 * CanvasElementFactory manufactures the appropriate type of CanvasElement,
 * given a fixed view field configuration object from the perspective.
 *
 */
public class CanvasElementFactory {
	
	/**
	 * Creates a CanvasElement given its configuration object.
	 * 
	 * @param config the FixedFieldConfiguration for the element to create
	 * @param canvas the parent Canvas widget for the CanvasElement
	 * 
	 * @return a new CanvasElement object
	 */
	public static CanvasElement create(
									   final IFixedFieldConfiguration config, final Canvas canvas) {
		switch (config.getType()) {
			case BOX:
				return new BoxElement(canvas, (BoxFieldConfiguration)config);
			case LINE:
				return new LineElement(canvas, (LineFieldConfiguration)config);
			case TEXT:
				return new TextElement(canvas, config);
			case IMAGE:
				return new ImageElement(canvas, (ImageFieldConfiguration)config);
			case CHANNEL:
				return new ChannelElement(config.getApplicationContext(),
										  canvas, (ChannelFieldConfiguration)config);
			case BUTTON:
				return new ButtonElement(canvas, (ButtonFieldConfiguration)config);
			case TIME:
				return new TimeElement(config.getApplicationContext(), canvas, (TimeFieldConfiguration)config);
			case HEADER:
				return new HeaderElement(canvas, (HeaderFieldConfiguration)config);
		}
		return null;
	}
}
