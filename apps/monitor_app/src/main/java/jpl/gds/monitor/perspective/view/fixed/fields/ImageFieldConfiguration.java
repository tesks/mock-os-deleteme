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
package jpl.gds.monitor.perspective.view.fixed.fields;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;


/**
 * ImageFieldConfiguration is a subclass of FixedFieldConfiguration that
 * represents an image field in a fixed layout view. If the end coordinate is
 * undefined, the image is drawn at natural size.
 */
public class ImageFieldConfiguration extends DualPointFixedFieldConfiguration {

	// XML tags and attributes
	/**
	 * XML image element name
	 */
	public static final String IMAGE_TAG = "Image";
	
	/**
	 * XML image file path attribute name
	 */
	public static final String IMAGE_PATH_TAG = "path";

	private String imagePath;

	/**
	 * Creates a new ImageFieldConfiguration.
	 */
	public ImageFieldConfiguration() {
		super(FixedFieldType.IMAGE);
	}

	/**
	 * Gets the path to the image file on the filesystem.
	 * 
	 * @return the file path of the image
	 */
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * Sets the path to the image file on the filesystem.
	 * 
	 * @param imagePath the file path of the image
	 */
	public void setImagePath(final String imagePath) {
		this.imagePath = imagePath;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return IMAGE_TAG;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.DualPointFixedFieldConfiguration#getAttributeXML(java.lang.StringBuilder)
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);
		toAppend.append(IMAGE_PATH_TAG + "=\"" + imagePath + "\" ");
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		final ChillPoint start = new ChillPoint(1,1,coordSystem);
		this.setStartCoordinate(start);
		final ChillPoint end = new ChillPoint(ChillPoint.UNDEFINED, ChillPoint.UNDEFINED, coordSystem);
		this.setEndCoordinate(end);
		this.setImagePath("jpl/gds/monitor/perspective/gui/fixed/DefaultImage.gif");
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof ImageFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type ImageFieldConfiguration");
		}
		final ImageFieldConfiguration imageConfig = (ImageFieldConfiguration)newConfig;
		super.copyConfiguration(newConfig);
		imageConfig.imagePath = imagePath;
	}
}
