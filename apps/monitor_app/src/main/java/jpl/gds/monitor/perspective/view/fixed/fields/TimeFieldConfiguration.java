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

import java.text.SimpleDateFormat;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.shared.swt.types.CoordinateSystemType;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;

/**
 * TimeFieldConfiguration is a subclass of FixedFieldConfiguration that
 * represents a "latest time" field in a fixed layout view. The type of time
 * displayed in configurable.
 */
public class TimeFieldConfiguration extends AbstractTextFieldConfiguration {

	// XML tags and attributes
	/**
	 * XML time field element name
	 */
	public static final String TIME_FIELD_TAG = "LatestTime";
	
	/**
	 * XML time's source attribute name
	 */
	public static final String SOURCE_TAG = "sourceTime";

	/**
	 * An enumeration of the possible types of times to display.
	 */
	public enum SourceTimeType {
		/**
		 * Earth Receive Time
		 */
		ERT,
		
		/**
		 * Spacecraft CLocK
		 */
		SCLK,
		
		/**
		 * Coordinated Universal Time
		 */
		UTC,
		
		/**
		 * SpaceCraft Event Time
		 */
		SCET,
		
		/**
		 * Record Creation Time
		 */
		RCT,
		
		/**
		 * Local Solar Time
		 */
		LST,
		
		/**
		 * Monitor Standard Time
		 */
		MST;

		/**
		 * Indicates whether this time requires a date/time rather than
		 * C-printf formatter.
		 * 
		 * @return true if the field requires date format; false if not
		 */
		public boolean isTimeFormattedField() {
			switch (this) {
			case ERT:
			case UTC:
			case RCT:
			case SCET:
			case MST:
			case LST:
				return true;
			default:
				return false;
			}
		}
		
		/**
         * Indicates whether this time requires different date 
         * formatting because it is a Local Solar Time
         * 
         * @return true if the field is an LST; false if not
         */
        public boolean isSolFormattedField() {
            switch (this) {
            case LST:
                return true;
            default:
                return false;
            }
        }
	}

	private SourceTimeType timeType = SourceTimeType.ERT;
	private boolean isDefaultFont = true;
	private boolean isDefaultColors;

	/**
	 * Creates a new TimeFieldConfiguration.
	 * 
	 */
	public TimeFieldConfiguration(final ApplicationContext appContext) {
		super(appContext, FixedFieldType.TIME);
		setStatic(false);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#setFormat(java.lang.String)
	 */
	@Override
	public void setFormat(final String format) throws IllegalArgumentException
	{
		switch (this.timeType) {
		case SCLK:
			break;
		case ERT:
		case SCET:
		case MST:
			new AccurateDateTime().formatCustom(format);
			break;
		case UTC:
		case RCT:
			new SimpleDateFormat(format);
            break;
		case LST:
			LocalSolarTimeFactory.getNewLst(appContext.getBean(IContextIdentification.class).getSpacecraftId()).formatCustom(format);
			break;
		}
		super.setFormat(format);
	}

	
	/**
	 * Gets the time type of this field.
	 * 
	 * @return the SourceTimeType object
	 */
	public SourceTimeType getTimeType() {
		return timeType;
	}

	/**
	 * Sets the time type of this field.
	 * 
	 * @param timeType the SourceTimeType to set
	 */
	public void setTimeType(final SourceTimeType timeType) {
		this.timeType = timeType;
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return TIME_FIELD_TAG;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#getAttributeXML(java.lang.StringBuilder)
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);
		toAppend.append(SOURCE_TAG + "=\"" + timeType.toString() + "\" ");
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#setBuilderDefaults(jpl.gds.shared.swt.types.CoordinateSystemType)
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		super.setBuilderDefaults(coordSystem);
		this.setTimeType(SourceTimeType.ERT);
		this.setFormat("yyyy-DDD'T'HH:mm:ss");
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof TimeFieldConfiguration)) {
			throw new IllegalArgumentException(
			"Object for copy is not of type TimeFieldConfiguration");
		}
		final TimeFieldConfiguration timeConfig = (TimeFieldConfiguration) newConfig;
		super.copyConfiguration(newConfig);
		timeConfig.timeType = timeType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#usesDefaultFont()
	 */
	@Override
	public boolean usesDefaultFont() {
		isDefaultFont = font == null ? true : false;
		return isDefaultFont;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.FontConfigSupport#usesDefaultFont(boolean)
	 */
	@Override
	public void usesDefaultFont(final boolean usesDefaultFont) {
		this.isDefaultFont = usesDefaultFont;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors()
	 */
	@Override
	public boolean usesDefaultColors() {
		isDefaultColors = background == null && foreground == null ? true
				: false;
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
	 * Sets default format for times that use date and default sprintf for
	 * others
	 */
	public void setDefaultFormat() {
		switch (this.getTimeType()) {
		case ERT:
		case UTC:
		case SCET:
		case RCT:
		case MST:
		    setFormat("yyyy-DDD'T'HH:mm:ss");
			break;
			
		case LST:
		    setFormat("SOL-xxxx'M'HH:mm:ss");
            break;
            
		case SCLK:
			setFormat("%s");
			break;
		}
	}
}
