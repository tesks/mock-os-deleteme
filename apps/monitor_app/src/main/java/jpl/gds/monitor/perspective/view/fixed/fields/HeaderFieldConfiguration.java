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

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * HeaderFieldConfiguration is a FixedFieldConfiguration that is actually a pre-defined
 * block of fixed fields containing header information. Headers can be transparent or
 * not, and the foreground and background colors are configurable.
 */
public class HeaderFieldConfiguration extends FixedFieldConfiguration 
implements TwoColorConfigSupport, TransparencyConfigSupport {

	// XML tags and attributes
	/**
	 * XML header element name
	 */
	public static final String HEADER_TAG = "Header";
	
	/**
	 * XML foreground attribute name
	 */
	public static final String FOREGROUND_TAG = "foreground";
	
	/**
	 * XML background attribute name
	 */
	public static final String BACKGROUND_TAG = "background";
	
	/**
	 * XML transparency flag name
	 */
	public static final String TRANSPARENT_TAG = "transparent";
	
	/**
	 * XML header type attribute name
	 */
	public static final String HEADER_TYPE_TAG = "headerType";

	private ChillColor foreground;
	private ChillColor background;
	private boolean isTrans = true;
	private String headerType = "TimesOnlyHeader";
	private final List<IFixedFieldConfiguration> fieldConfigs = new ArrayList<IFixedFieldConfiguration>();
	private boolean isDefaultColors;

	/**
	 * Creates a new HeaderFieldConfiguration.
	 * @param appContext the current application context
	 */
	public HeaderFieldConfiguration(final ApplicationContext appContext) {
		super(appContext, FixedFieldType.HEADER);
		setStatic(false);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport#getBackground()
	 */
	@Override
    public ChillColor getBackground() {
		return background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport#setBackground(jpl.gds.shared.swt.types.ChillColor)
	 */
	@Override
    public void setBackground(final ChillColor background) {
		this.background = background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#getForeground()
	 */
	@Override
    public ChillColor getForeground() {
		return foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#setForeground(jpl.gds.shared.swt.types.ChillColor)
	 */
	@Override
    public void setForeground(final ChillColor foreground) {
		this.foreground = foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport#isTransparent()
	 */
	@Override
    public boolean isTransparent() {
		return isTrans;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TransparencyConfigSupport#setTransparent(boolean)
	 */
	@Override
    public void setTransparent(final boolean isTransparent) {
		this.isTrans = isTransparent;
	}

	/**
	 * Sets the header type name.
	 * @param type the header type to set
	 */
	public void setHeaderType(final String type) {
		headerType = type;
	}

	/**
	 * Gets the header type name.
	 * @return the header type
	 */
	public String getHeaderType() {
		return headerType;
	}

	/**
	 * Loads the header definition. This object cannot be used until it is loaded.
	 * 
	 * @return true if the header was loaded, false if not
	 */
	public boolean load() {
		if (headerType == null) {
			throw new IllegalStateException("header type is not defined in load()");
		}
		final ViewReference ref = new ViewReference();
		ref.setName(headerType);
		final IViewConfiguration vc = ref.parse(appContext);
		if (vc == null) {
			TraceManager.getDefaultTracer().error("There was an error loading header type " + headerType);

			return false;
		}
		if (!vc.getViewType().equals(ViewType.FIXED_LAYOUT)) {
			TraceManager.getDefaultTracer().error("Header view definition for header type " + headerType +

			" does not appear to be valid");
			return false;
		}

		final IFixedLayoutViewConfiguration fvc = (IFixedLayoutViewConfiguration)vc;
		this.setCoordinateSystem(fvc.getCoordinateSystem());

		fieldConfigs.clear();
		fieldConfigs.addAll(fvc.getFieldConfigs());
		for (final IFixedFieldConfiguration field: fieldConfigs) {
			field.setCoordinateSystem(fvc.getCoordinateSystem());
		}
		return true;
	}

	/**
	 * Gets the list of fixed fields configuration objects that make up this header configuration.
	 * 
	 * @return List of FixedFieldConfiguration objects
	 */
	public List<IFixedFieldConfiguration> getFieldConfigs() {
		final ArrayList<IFixedFieldConfiguration> results = new ArrayList<IFixedFieldConfiguration>(fieldConfigs);
		return results;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);
		if (background != null) {
			toAppend.append(BACKGROUND_TAG + "=\""
					+ background.getRgbString() + "\" ");
		}
		if (foreground != null) {
			toAppend.append(FOREGROUND_TAG + "=\""
					+ foreground.getRgbString() + "\" ");
		}

		toAppend.append(TRANSPARENT_TAG + "=\""
				+ isTrans + "\" ");
		if (headerType != null)
		{
			toAppend.append(HEADER_TYPE_TAG + "=\"" + headerType + "\" ");
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		final ChillPoint point = new ChillPoint(5,5,coordSystem);
		this.setStartCoordinate(point);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof HeaderFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type HeaderFieldConfiguration");
		}
		super.copyConfiguration(newConfig);
		final HeaderFieldConfiguration headerConfig = (HeaderFieldConfiguration)newConfig;

		headerConfig.isTrans = isTrans;
		headerConfig.headerType = headerType;

		if (background != null) {
			headerConfig.background = new ChillColor(background);
		}
		if (foreground != null) {
			headerConfig.foreground = new ChillColor(foreground);
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return HEADER_TAG;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors()
	 */
	@Override
    public boolean usesDefaultColors() {
		isDefaultColors = background == null && foreground == null ? true : false;
		return isDefaultColors;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors(boolean)
	 */
	@Override
    public void usesDefaultColors(final boolean usesDefaultColors) {
		this.isDefaultColors = usesDefaultColors;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void convertCoordinates(final CoordinateSystemType coordSystem, final int charWidth, final int charHeight) {
		super.convertCoordinates(coordSystem, charWidth, charHeight);

		for (final IFixedFieldConfiguration config: fieldConfigs) {
			config.convertCoordinates(coordSystem, charWidth, charHeight);
		}
		setCoordinateSystem(coordSystem);
	}
}
