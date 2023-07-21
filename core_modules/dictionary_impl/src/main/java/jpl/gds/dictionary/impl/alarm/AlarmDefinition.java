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
package jpl.gds.dictionary.impl.alarm;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.alarm.AlarmDefinitionFactory;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;

/**
 * This class implements the dictionary definition of a single-channel alarm per
 * the IAlarmDefinition interface. It encapsulates all the attributes of a
 * single telemetry channel alarm as read from the channel dictionary. Note that
 * that this class handles the superset of alarm types, providing methods for
 * all types of alarms. There are no specific alarm definition classes for
 * different alarm types. Therefore, the alarm type should always be checked
 * before relying upon any of the accessor methods. 
 * <p>
 * Instances of this class must be created via AlarmDefinitionFactory. All
 * references to instance should be via the IAlarmDefinition interface rather
 * than via this class.
 * 
 *
 * @see AlarmDefinitionFactory
 */
public class AlarmDefinition implements IAlarmDefinition {

	/**
	 * Type of the alarm.
	 */
	private AlarmType type = AlarmType.NO_TYPE;
	
	/**
	 * Channel ID this alarm applies to.
	 */
	protected String channelId;

	/**
	 * DN or EU alarm.
	 */
	protected boolean isDn = true;
	
	/**
	 * Description of the alarm.
	 */
	private String alarmDescription;

	/**
	 * The alarm level.
	 */
	protected AlarmLevel alarmLevel = AlarmLevel.NONE;

	/**
	 * The channel value history count needed to calculate this alarm. Generally
	 * defaults to a value of "1" unless otherwise specified. Used in figuring
	 * out how many values to hold onto for doing hysteresis.
	 */
	private int historyCount;

	/**
	 * The alarm ID, for distinguishing alarms.
	 */
	private String alarmId;

	/**
	 * Specifies how many consecutive "in alarm" values must be received before
	 * this alarm definition specifies that the channel(s) in question actually
	 * are in alarm (when the previous state is not in alarm).
	 */
	private int hysteresisInValue = 1;

	/**
	 * Specifies how many consecutive "out of alarm" values must be received
	 * before this alarm definition specifies that the channel(s) in question
	 * are actually no longer in alarm (when the previous state was in alarm).
	 */
	private int hysteresisOutValue = 1;

	/**
	 * Value mask, used only for digital and mask alarms.
	 */
	private long valueMask;

	/**
	 * Valid mask, used only for digital alarms.
	 */
	private long digitalValidMask;

	/**
	 * Value delta amount, for delta alarms only.
	 */
	private double deltaLimit;

	/**
	 * Value upper bound, Used by range and high alarms.
	 */
	private double upperLimit;

	/**
	 * Value lower bound. Used by range and low alarms.
	 */
	private double lowerLimit; 

	/**
	 * State values to alarm. Used only by state alarms.
	 */
	private List<Long> alarmStates; 
	
	/**
	 *  Category map to hold category name and category value for a channel.
	 *  
	 */
	
	private Categories categories = new Categories();
	
	/**
	 *  Key-value attribute map to hold the keyword name and value of any project-
	 *  specific information.
	 *  
	 */	
	private KeyValueAttributes keyValueAttr = new KeyValueAttributes();

	/**
	 * Constructor that sets the alarm type, alarm ID, channel ID, and alarm
	 * level.
	 * 
	 * @param the
	 *            type of the alarm
	 * @param alarmId
	 *            a unique alarm ID; if null, will be computed
	 * @param cid
	 *            channel ID that this alarm is for
	 * @param level
	 *            level of this alarm
	 *            
	 */
	AlarmDefinition(final AlarmType type, final String alarmId,
			final String cid, final AlarmLevel level) {
		if (type == null) {
			throw new IllegalArgumentException("Alarm type may not be null");
		}
		if (cid == null) {
			throw new IllegalArgumentException("channel ID may not be null");
		}
		if (level != null) {
			this.alarmLevel = level;
		}
		this.channelId = cid;
		this.alarmId = alarmId;
		this.type = type;
	}
	
    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setAlarmDescription(java.lang.String)
     */
    @Override
    public void setAlarmDescription(final String desc) {
        this.alarmDescription = desc;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getAlarmDescription()
     */
    @Override
    public String getAlarmDescription() {
        return alarmDescription;
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#setCategory(java.lang.String, java.lang.String)
	 */
	@Override
	public void setCategory(String catName, String catValue) {
		categories.setCategory(catName, catValue);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#getCategory(java.lang.String)
	 */
	@Override
    public String getCategory(String name) {
		return categories.getCategory(name);
	}
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getCategories()
	 */
	@Override
	public Categories getCategories() {
		return categories;
	}
	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setCategoryMap()
	 */
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#setCategories(jpl.gds.dictionary.impl.impl.api.Categories)
	 */
	@Override
 	public void setCategories(Categories cat) {
 		categories.copyFrom(cat);
 	}
		

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttribute(java.lang.String, java.lang.String)
	 */
	@Override	
	public void setKeyValueAttribute(String key, String value) {
		keyValueAttr.setKeyValue(key, value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#getKeyValueAttribute(java.lang.String)
	 */
	@Override	
	public String getKeyValueAttribute(String key) {
		return keyValueAttr.getValueForKey(key);
	}
	
	/**
	 *  {@inheritDoc}
	 *  @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getKeyValueAttributes()
	 * 
	 */
	@Override	
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttributes(jpl.gds.dictionary.impl.impl.api.KeyValueAttributes)
	 */
	@Override	
	public void setKeyValueAttributes(KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getAlarmId()
	 */
	@Override
	public String getAlarmId() {
		/*  Use generated alarm ID when there isn't one
		 * specified by the dictionary.
		 */
		return alarmId == null ? getUniqueAlarmDefinitionKey() : alarmId;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setAlarmId(java.lang.String)
	 */
	@Override
	public void setAlarmId(final String alarmId) {
		this.alarmId = alarmId;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getHistoryCount()
	 */
	@Override
	public int getHistoryCount() {
		return historyCount;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getChannelId()
	 */
	@Override
	public String getChannelId() {
		return channelId;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setChannelId(java.lang.String)
	 */
	@Override
	public void setChannelId(final String channelId) {
		this.channelId = channelId;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getAlarmType()
	 */
	@Override
	public AlarmType getAlarmType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getAlarmLevel()
	 */
	@Override
	public AlarmLevel getAlarmLevel() {
		return alarmLevel;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setAlarmLevel(jpl.gds.dictionary.impl.impl.api.alarm.AlarmLevel)
	 */
	@Override
	public void setAlarmLevel(final AlarmLevel alarmLevel) {
		this.alarmLevel = alarmLevel;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setCheckOnEu(boolean)
	 */
	@Override
	public void setCheckOnEu(final boolean enable) {
		isDn = !enable;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#isCheckOnDn()
	 */
	@Override
	public boolean isCheckOnDn() {
		return isDn;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#isCheckOnEu()
	 */
	@Override
	public boolean isCheckOnEu() {
		return !isDn;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getDisplayString()
	 */
	@Override
	public String getDisplayString() {
		return getAlarmType().getDisplayState();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getHysteresisInValue()
	 */
	@Override
	public int getHysteresisInValue() {
		return hysteresisInValue;
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setHysteresisInValue(int)
	 */
	@Override
	public void setHysteresisInValue(final int inHysteresisValue) {
		if (inHysteresisValue < 0) {
			throw new IllegalArgumentException(
					"The inHysteresis value for an alarm cannot be negative.");
		}

		hysteresisInValue = inHysteresisValue;
		historyCount = Math.max(historyCount,
				Math.max(hysteresisInValue, hysteresisOutValue));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getHysteresisOutValue()
	 */
	@Override
	public int getHysteresisOutValue() {
		return hysteresisOutValue;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setHysteresisOutValue(int)
	 */
	@Override
	public void setHysteresisOutValue(final int outHysteresisValue) {
		if (outHysteresisValue < 0) {
			throw new IllegalArgumentException(
					"The outHysteresis value for an alarm cannot be negative.");
		}

		hysteresisOutValue = outHysteresisValue;
		historyCount = Math.max(historyCount,
				Math.max(hysteresisInValue, hysteresisOutValue));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#hasHysteresis()
	 */
	@Override
	public boolean hasHysteresis() {
		return (hysteresisInValue > 1 || hysteresisOutValue > 1);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getDeltaLimit()
	 */
	@Override
	public double getDeltaLimit() {
		return this.deltaLimit;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setDeltaLimit(double)
	 */
	@Override
	public void setDeltaLimit(final double deltaLimit) {
		this.deltaLimit = deltaLimit;	
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#addAlarmStates(java.util.List)
	 */
	@Override
	public void addAlarmStates(final List<Long> newAlarmStates) {
		if (this.alarmStates == null) {
			this.alarmStates = new LinkedList<Long>();
		}
		if (newAlarmStates != null) {
			this.alarmStates.addAll(newAlarmStates);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getAlarmStates()
	 */
	@Override
	public List<Long> getAlarmStates() {
		if (this.alarmStates == null) {
			return null;
		}
		return Collections.unmodifiableList(this.alarmStates);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getValueMask()
	 */
	@Override
	public long getValueMask() {
		return this.valueMask;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setValueMask(long)
	 */
	@Override
	public void setValueMask(final long mask) {
		this.valueMask = mask;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getLowerLimit()
	 */
	@Override
	public double getLowerLimit() {
		return this.lowerLimit;
	}

	@Override
	public void setLowerLimit(final double lowerLimit) {
		this.lowerLimit = lowerLimit;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getUpperLimit()
	 */
	@Override
	public double getUpperLimit() {
		return this.upperLimit;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setUpperLimit(double)
	 */
	@Override
	public void setUpperLimit(final double upperLimit) {
		this.upperLimit = upperLimit;	
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getDigitalValidMask()
	 */
	@Override
	public long getDigitalValidMask() {
		return this.digitalValidMask;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#setDigitalValidMask(long)
	 */
	@Override
	public void setDigitalValidMask(final long mask) {
		this.digitalValidMask = mask;	
	}

	/**
	 * Sets the history count: the number of channel values required to
	 * calculate alarm state. This value should be calculated based
	 * upon the alarm type and hysteresis settings.
	 * 
	 * @param count
	 *            the history count to set
	 */
	protected void setHistoryCount(final int count) {
		historyCount = count;
	}

	/**
	 * Creates a unique alarm definition key, given an AlarmDefinition and a
	 * channel Id. Used only when no alarm ID has been supplied.
	 * 
	 * @return a unique text key
	 */
	protected String getUniqueAlarmDefinitionKey() {

		StringBuilder sb = new StringBuilder(128);
		sb.append(getChannelId());
		sb.append("_");
		sb.append(getAlarmLevel().toString());
		sb.append("_");
		sb.append(this.type);

		/* Use underscore rather than parens
		 * in this part of the ID.
		 */
		if (isDn) {
			sb.append("_DN_");
		} else {
			sb.append("_EU_");
		}

		appendAlarmUniqueKey(sb);
		return sb.toString();
	}

	/**
	 * Appends a unique key-string that identifies this alarm definition to the
	 * provided StringBuilder object.
	 * 
	 * @param sb
	 *            the StringBuilder to append to
	 * @return the original StringBuilder object that now includes a unique
	 *         key-string
	 */
	protected StringBuilder appendAlarmUniqueKey(final StringBuilder sb) {

		switch(this.type) {
		case HIGH_VALUE_COMPARE:
			sb.append(Double.toString(upperLimit));
			break;
		case LOW_VALUE_COMPARE:
			sb.append(Double.toString(lowerLimit));
			break;
		case EXCLUSIVE_COMPARE:
		case INCLUSIVE_COMPARE:
			sb.append(Double.toString(upperLimit));
			sb.append("_");
			sb.append(Double.toString(lowerLimit));
			break;
		case MASK_COMPARE:
			sb.append(Long.toHexString(this.valueMask));
			break;
		case DIGITAL_COMPARE:
			sb.append(Long.toHexString(this.valueMask));
			sb.append("_");
			sb.append(Long.toHexString(this.digitalValidMask));
			break;
		case STATE_COMPARE:
			for (Long state : alarmStates) {
				sb.append("_");
				sb.append(Long.toString(state));
			}
			break;
		case VALUE_DELTA:
			sb.append(Double.toString(this.deltaLimit));
			break;
		case VALUE_CHANGE:
			break;
		default:
			throw new IllegalStateException("unrecognized alarm type:" + this.type);
		}

		return sb;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return toString(true);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#toString(boolean)
	 */
	@Override
	public String toString(final boolean includeId) {

		StringBuilder sb = new StringBuilder(128);
		if (includeId && this.alarmId != null) {
			sb.append("ID=" + this.alarmId + ", ");
		}
		sb.append("Channel=" + getChannelId() + ", ");
		sb.append("Level=" + getAlarmLevel().toString() + ", ");
		sb.append("Type=" + this.getAlarmType() + ", ");

		if (isDn) {
			sb.append("on DN");
		} else {
			sb.append("on EU");
		}

		switch (this.type) {
		case HIGH_VALUE_COMPARE:
			sb.append(", Upper=" + Double.toString(upperLimit));
			break;
		case LOW_VALUE_COMPARE:
			sb.append(", Lower=" + Double.toString(lowerLimit));
			break;
		case EXCLUSIVE_COMPARE:
		case INCLUSIVE_COMPARE:
			sb.append(", Lower=" + Double.toString(lowerLimit));
			sb.append(", Upper=" + Double.toString(upperLimit));
			break;
		case MASK_COMPARE:
			sb.append(", Mask=" + Long.toHexString(this.valueMask));
			break;
		case DIGITAL_COMPARE:
			sb.append(", Value Mask=" + Long.toHexString(this.valueMask));
			sb.append(", Valid Mask=" + Long.toHexString(this.digitalValidMask));
			break;
		case STATE_COMPARE:
			sb.append(", States=");
			boolean first = true;
			for (Long state : alarmStates) {
				if (!first) {
					sb.append(";");
				}
				sb.append(Long.toString(state));
				first = false;
			}
			break;
		case VALUE_DELTA:
			sb.append(", Delta=" + Double.toString(this.deltaLimit));
			break;
		case VALUE_CHANGE:
			break;
		default:
			throw new IllegalStateException("unrecognized alarm type:" + this.type);
		}

		return sb.toString();
	}
	/** {@inheritDoc}
	 *
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#clearKeyValue()
	 */
	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();
	}
	
	@Override
    public int hashCode() {
	    return getAlarmId().hashCode();
	}
	
	@Override
    public boolean equals(Object o) {
	    if (!(o instanceof IAlarmDefinition)) {
	        return false;
	    }
	    IAlarmDefinition odef = (IAlarmDefinition)o;
	    return odef.getAlarmId().equals(odef.getAlarmId());
	}

}
