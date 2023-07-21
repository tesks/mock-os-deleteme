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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.canvas.support.DualCoordinateSupport;
import jpl.gds.monitor.canvas.support.ImageSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.fields.ImageFieldConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This CanvasElement is a scalable image. If no end coordinate is set in the 
 * configuration, the image will be drawn at its natural size. Otherwise, it 
 * will be scaled to the specified size.

 *
 */
public class ImageElement extends DualCoordinateCanvasElement implements 
DualCoordinateSupport, ImageSupport{

	/**
	 * The default selection priority of this CanvasElement
	 */
	private static final int SELECTION_PRIORITY = 5;

	private Image image;


	/**
	 * Creates an ImageElement on the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 */
	public ImageElement(Canvas parent) {
		super(parent, FixedFieldType.IMAGE);
		setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * Creates an ImageElement with the given fixed view field configuration 
	 * on the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 * @param imageConfig the ImageFieldConfiguration object from the 
	 * perspective
	 */
	public ImageElement(Canvas parent, ImageFieldConfiguration imageConfig) {
		super(parent, imageConfig);
		updateFieldsFromConfig();
		setSelectionPriority(SELECTION_PRIORITY);
	}

	/**
	 * Returns the image object drawn by this ImageElement.
	 * 
	 * @return the Image object, of null if none could be loaded
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * Sets the file path to the image to be draw by the ImageElement.
	 * 
	 * @param imagePath the path to the image on the filesystem
	 */
	public void setImage(String imagePath) {
		try {
			image = SWTUtilities.createImage(parent.getDisplay(), imagePath);
		} catch (UnsupportedOperationException e) {
			TraceManager.getDefaultTracer().warn(e.getMessage());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#draw(org.eclipse.swt.graphics.GC)
	 */
	@Override
    public void draw(GC gc) {
		
	    if((!displayMe  && 
	            this.getFieldConfiguration().getCondition() != null) || 
	            image == null) {
            return;
        }
	    
		saveGcSettings(gc);

		int x = getXCoordinate(startPoint.getX(), gc);
		int y = getYCoordinate(startPoint.getY(), gc);

		Rectangle bounds = image.getBounds();

		int tempXEnd = 0;
		int tempYEnd = 0;

		// Compute end point from image size if end point is not defined		
		if (endPoint.isUndefined()) {
			if (this.getCoordinateSystem().equals(
			        CoordinateSystemType.PIXEL)) {
				tempXEnd = startPoint.getX() + bounds.width;
				tempYEnd = startPoint.getY() + bounds.height;
			} else {
				tempXEnd = startPoint.getX() + bounds.width / 
				gc.getFontMetrics().getAverageCharWidth();
				tempYEnd = startPoint.getY() + bounds.height / 
				gc.getFontMetrics().getHeight();
			}
			this.setEndLocation(new ChillPoint(tempXEnd, tempYEnd, 
			        this.getCoordinateSystem()));
		} else {
			tempXEnd = endPoint.getX();
			tempYEnd = endPoint.getY();
		}
		int ex = getXCoordinate(tempXEnd, gc);
		int ey = getYCoordinate(tempYEnd, gc);

		// Draw the image
		gc.drawImage(image, 0, 0, image.getBounds().width, 
		        image.getBounds().height,
				Math.min(x, ex), Math.min(y, ey), 
				Math.abs(ex - x), Math.abs(ey - y));

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
		ImageFieldConfiguration imageConfig = 
		    (ImageFieldConfiguration)fieldConfig;
		this.setImage(imageConfig.getImagePath());
		ChillPoint end = imageConfig.getEndCoordinate();
		if (end == null) {
			end = new ChillPoint(ChillPoint.UNDEFINED, 
			        ChillPoint.UNDEFINED, getCoordinateSystem());
		}
		this.setEndLocation(end);
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
