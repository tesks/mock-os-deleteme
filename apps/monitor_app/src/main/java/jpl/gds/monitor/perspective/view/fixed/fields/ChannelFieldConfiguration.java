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
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.perspective.view.ChannelViewConfiguration;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.shared.swt.types.CoordinateSystemType;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;

/**
 * ChannelFieldConfiguration is a subclass of FixedFieldConfiguration that represents a field
 * on a fixed layout view that is based upon a telemetry channel.  An actual dynamic data fields
 * for the selected channel may be displayed, or some static piece of dictionary information
 * about the channel may be displayed, depending on the ChannelFieldType. Channel fields
 * support all the attributes of text fields, but also include an alarm highlight flag.
 */
public class ChannelFieldConfiguration extends AbstractTextFieldConfiguration implements ChannelViewConfiguration {

	// XML tags and attribute names
    /**
     * XML channel field element name
     */
	public static final String CHANNEL_FIELD_TAG = "Channel";
	
	/**
	 * XML channel ID attribute name
	 */
	public static final String CHANNEL_ID_TAG = "channelId";
    
    /**
     * XML channel source attribute name
     */
	public static final String SOURCE_TAG = "sourceField";
    
    /**
     * XML alarm highlight attribute name
     */
	public static final String HIGHLIGHT_TAG = "alarmHighlight";

	// Choice for which channel field can be displayed.
	/**
	 * Enumeration of all of the possible channel field types for use in a 
	 * fixed page
	 *
	 */
	public enum ChannelFieldType {
		/**
		 * Channel ID
		 */
		ID,
		
		/**
		 * Channel title
		 */
		TITLE,
		
		/**
		 * Data number
		 */
		DN,
		
		/**
		 * Engineering unit
		 */
		EU,
		
		/**
		 * Raw data
		 */
		RAW,
		
		/**
		 * Value (status, EU or raw depending on channel type)
		 */
		VALUE,
		
		/**
		 * Channel status
		 */
		STATUS,
		
		/**
		 * Flight software name
		 */
		FSW_NAME,
		
		/**
		 * Channel module
		 */
		MODULE,
		
		/**
		 * Operational category
		 */
		OPS_CAT,
		
		/**
		 * Subsystem
		 */
		SUBSYSTEM,
		
		/**
		 * Data number unit
		 */
		DN_UNIT,
		
		/**
		 * Engineering unit unit
		 */
		EU_UNIT,
		
		/**
		 * Alarm state
		 */
		ALARM_STATE,
		
		/**
		 * Earth receive time
		 */
		ERT,
		
		/**
		 * Spacecraft clock
		 */
		SCLK,
		
		/**
		 * Record creation time
		 */
		RCT,
		
		/**
		 * Spacecraft event time
		 */
		SCET,
		
		/**
		 * Mean solar time?
		 */
		MST,
		
		/**
		 * Local solar time
		 */
		LST,

		/**
		 * Station ID
		 */
		DSS_ID,
		
		/**
		 * Realtime/recorded flag
		 */
		RECORDED;

		/**
		 * Indicates whether this channel field type is considered static, i.e., will not
		 * change regardless of data received.
		 * 
		 * @return true if this is a static channel field type, false if not
		 */
		public boolean isStaticField() {
			switch(this) {
			case ID:
			case TITLE:
			case FSW_NAME:
			case SUBSYSTEM:
			case MODULE:
			case OPS_CAT:
			case DN_UNIT:
			case EU_UNIT:
				return true;
			default:
				return false;
			}
		}

		/**
		 * Indicates whether this channel field type is highlighted for alarms by default.
		 * 
		 * @return true if the field should be highlighted, false if not
		 */
		public boolean isHighlightByDefault() {
			switch(this) {
			case DN:
			case EU:
			case RAW:
			case VALUE:
			case STATUS:
			case ALARM_STATE:
			case ERT:
			case SCLK:
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
		 * Indicates whether this channel requires a date/time rather than 
		 * C-printf formatter.
		 * 
		 * @return true if the field requires date format; false if not
		 */
		public boolean isTimeFormattedField() {
			switch(this) {
			case ERT:
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
         * Indicates whether this channel requires different date 
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
	};

	private String channelId;
	private ChannelFieldType fieldType = ChannelFieldType.ID;
	private boolean useAlarmHighlight;
	private boolean isDefaultFont = true;
	private boolean isDefaultColors;

	/**
	 * Creates a new ChannelFieldConfiguration.
	 * @param appContext the current application context
	 */
	public ChannelFieldConfiguration(final ApplicationContext appContext) {
		
		super(appContext, FixedFieldType.CHANNEL);
		
		if (fieldType.isHighlightByDefault()) {
			useAlarmHighlight = true;
		}
	}

	/**
	 * Gets the channel ID for this field.
	 * 
	 * @return the channel ID string
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#setFormat(java.lang.String)
	 */
	@Override
    public void setFormat(final String format) throws IllegalArgumentException 
	{
		if (fieldType.isTimeFormattedField()) {
		switch (fieldType) {
		case ERT:
		case SCET:			
		case MST:
            new AccurateDateTime().formatCustom(format);
            break;
		case LST:
			LocalSolarTimeFactory.getNewLst(appContext.getBean(IContextIdentification.class).getSpacecraftId()).formatCustom(format);
            break;
		case RCT:
			// This will validate a standard date/time format string; will throw upon failure
			new SimpleDateFormat(format);
			break;
		}
		}
		super.setFormat(format);
	}
		
	/**
	 * Sets the channel ID for this field.
	 * 
	 * @param channelId the channel ID to set
	 */
	public void setChannelId(final String channelId) {
		this.channelId = channelId;
	}

	/**
	 * Indicates whether this channel field should be highlighted based upon alarm status.
	 * 
	 * @return true if the field should be highlighted, false if not
	 */
	public boolean isUseAlarmHighlight() {
		return useAlarmHighlight;
	}

	/**
	 * Sets the flag indicating whether this channel field should be highlighted based upon 
	 * alarm status.
	 * 
	 * @param useAlarmHighlight true to highlight for alarms, false if not
	 */
	public void setUseAlarmHighlight(final boolean useAlarmHighlight) {
		this.useAlarmHighlight = useAlarmHighlight;
	}

	/**
	 * Gets the type/source channel field to display.
	 * 
	 * @return the ChannelFieldType
	 */
	public ChannelFieldType getFieldType() {
		return fieldType;
	}

	/**
	 * Sets the type/source channel field to display.
	 * 
	 * @param fieldType the ChannelFieldType to set
	 */
	public void setFieldType(final ChannelFieldType fieldType) {
		this.fieldType = fieldType;
		useAlarmHighlight = this.fieldType.isHighlightByDefault();
		setStatic(this.fieldType.isStaticField());
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return CHANNEL_FIELD_TAG;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#getAttributeXML(java.lang.StringBuilder)
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);
		toAppend.append(CHANNEL_ID_TAG + "=\"" + channelId + "\" ");
		toAppend.append(SOURCE_TAG + "=\"" + fieldType.toString() + "\" ");	
		toAppend.append(HIGHLIGHT_TAG + "=\"" + useAlarmHighlight + "\" ");		
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration#setBuilderDefaults(jpl.gds.shared.swt.types.CoordinateSystemType)
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		super.setBuilderDefaults(coordSystem);
		this.setFieldType(ChannelFieldType.ID);
		final SortedSet<String> chans = appContext.getBean(IChannelDefinitionProvider.class).getChanIds();
		if (!chans.isEmpty()) {
			this.setChannelId(chans.first());
		} else {
			this.setChannelId("A-0000");
		}
		this.setUseAlarmHighlight(ChannelFieldType.ID.isHighlightByDefault());
		this.setTransparent(false);
		this.setFormat("%s");
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.ChannelViewConfiguration#getReferencedChannelIds()
	 */
	@Override
    public List<String> getReferencedChannelIds() {
		final List<String> result = new ArrayList<String>(1);
		if (channelId != null) {
			result.add(channelId);
		}
		return result;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.ChannelViewConfiguration#containsNullChannelIds()
	 */
	@Override
    public boolean containsNullChannelIds() {
		return (channelId == null);
	}

	/**
	 * Resets the formatter for this field to its default value.
	 * @return default formatter for this channel field
	 */
	public String setDefaultFormat()
	{
		switch(this.getFieldType())
		{
		case ERT:
		case SCET:
		case RCT:
		case MST:
		    setFormat("yyyy-DDD'T'HH:mm:ss");
			break;
		case LST:
		    setFormat("SOL-xxxx'M'HH:mm:ss");
            break;
		case ID:
		case TITLE:
		case STATUS:
		case FSW_NAME:
		case MODULE:
		case OPS_CAT:
		case SUBSYSTEM:
		case DN_UNIT:
		case EU_UNIT:
		case ALARM_STATE:
		case SCLK:
			setFormat("%s");
			break;
		case DN:
		case RAW:
			final IChannelDefinition def1 = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(this.getChannelId());

			
			//if chan def is null, then channel dictionary was never loaded
			if(def1 == null) {
			    setFormat(null);
			}
			else if (def1.getDnFormat() != null) {
				setFormat(def1.getDnFormat());

			} else {
				final ChannelType type = def1.getChannelType();

				if(type.equals(ChannelType.FLOAT)) {
					setFormat("%f");
				}
				else if(type.equals(ChannelType.SIGNED_INT) || type.equals(ChannelType.STATUS)){
					setFormat("%d");
				}
				else if(type.equals(ChannelType.UNSIGNED_INT) || type.equals(ChannelType.BOOLEAN) || type.equals(ChannelType.DIGITAL)  || type.equals(ChannelType.TIME)) {
				    setFormat("%u");
				} else {
					setFormat("%s");
				}
			}
			break;
		case EU:
		case VALUE:
			final IChannelDefinition def2 = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(this.getChannelId());
			
			//if chan def is null, then channel dictionary was never loaded
			if(def2 == null) {
			    setFormat(null);
            }
			else if (def2.getEuFormat() != null) {
				setFormat(def2.getEuFormat());

			} else {
				final ChannelType type = def2.getChannelType();

				if(def2.hasEu() || type.equals(ChannelType.FLOAT)){
					setFormat("%f");
				}
				else if (type.equals(ChannelType.STATUS) || type.equals(ChannelType.BOOLEAN) || type.equals(ChannelType.ASCII)) {
					setFormat("%s");
				}
				else if(type.equals(ChannelType.SIGNED_INT)){
					if (def2.getDnFormat() == null) {
						setFormat("%d");
					} else {
						setFormat(def2.getDnFormat());
					}
				}
				else if(type.equals(ChannelType.UNSIGNED_INT) || type.equals(ChannelType.DIGITAL) || type.equals(ChannelType.TIME)){
					if (def2.getDnFormat() == null) {
						setFormat("%u");
					} else {
						setFormat(def2.getDnFormat());
					}
				}
			}
			break;

		}
		return format;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof ChannelFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type ChannelFieldConfiguration");
		}
		final ChannelFieldConfiguration chanConfig = (ChannelFieldConfiguration)newConfig;
		super.copyConfiguration(newConfig);
		chanConfig.channelId = channelId;
		chanConfig.fieldType = fieldType;
		chanConfig.useAlarmHighlight = useAlarmHighlight;
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
		isDefaultColors = foreground == null && background == null ? true : false;
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
}
