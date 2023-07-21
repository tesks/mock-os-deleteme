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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.alarm.AlarmDefinitionFactory;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;
import jpl.gds.dictionary.api.alarm.IAlarmOffControl;
import jpl.gds.dictionary.api.alarm.IAlarmReloadListener;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICompoundAlarmDefinition;
import jpl.gds.dictionary.api.alarm.OffScope;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.annotation.Jira;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * 
 * AbstractAlarmDictionary is a base class upon which IAlarmDictionary classes
 * can be built.It is the entry point for XML parsing and provides many methods
 * for constructing alarm definitions. It also provides base implementations of
 * the methods in the IBaseDictionary and IAlarmDictionary interfaces.
 * <p>
 * This is an extremely stateful object. Member variables are set in the course
 * of alarm parsing and ultimately combined into alarm definitions and maps. Be
 * aware of this when using this class. Protected methods should not be utilized
 * outside of the parsing process. The object should be cleared before it is
 * re-used.
 * 
 *
 */
public abstract class AbstractAlarmDictionary extends AbstractBaseDictionary implements
IAlarmDictionary {
    
	/**
	 * Separator used between channel ID and level in the key the off control
	 * map.
	 */
	private static final String OFF_CONTROL_ID_SEP = "/";

	/**
	 * Combination trace logger to share with subclasses.
	 */
    protected static final Tracer                          comboAlarmsLogger             = TraceManager
            .getTracer(Loggers.ALARM);

	/**
	 * Channel Definition Map. Must have a channel definition in this map in
	 * order to parse an alarm for the channel.
	 */
	private Map<String, IChannelDefinition> channelDefinitionMap = new HashMap<String, IChannelDefinition>();

	/**
	 * Map of Channel ID to IAlarmDefinition objects built by the parser. Not
	 * applicable for any combination alarm handling.
	 */
	private final Map<String, List<IAlarmDefinition>> alarmDefinitionsByChannel = new LinkedHashMap<String, List<IAlarmDefinition>>();

	/**
	 * Map of Alarm ID to IAlarmDefinition objects built by the parser. Not
	 * applicable for any combination alarm handling.
	 */
	private final Map<String, IAlarmDefinition> alarmDefinitionsByAlarmId = new HashMap<String, IAlarmDefinition>();

	/**
	 * Map of Alarm ID to Channel ID for IAlarmDefinition objects built by the
	 * parser. Not applicable for any combination alarm handling.
	 */
	private final Map<String, String> alarmIdToChannelIdMap = new TreeMap<String, String>();

	/**
	 * List of Alarm ID to CombinationAlarmDefinition objects built by the
	 * parser.
	 */
	private final Map<String, ICombinationAlarmDefinition> combinationAlarmMap = new HashMap<String, ICombinationAlarmDefinition>();

	/**
	 * Map of off controls built by the parser. The map key is alarm ID or
	 * channel-ID/level, depending upon the scope of the control.
	 */
	private final Map<String, IAlarmOffControl> offControls = new HashMap<String, IAlarmOffControl>();

	/**
	 * Indicates if current compound alarm is on DN or EU.
	 */
	private boolean compoundIsDn = true;

	/**
	 * Indicates if currently parsing a compound alarm.
	 */
	private boolean isInCompoundAlarm;

	/**
	 * The current compound alarm definition.
	 */
	protected ICompoundAlarmDefinition compoundAlarm;

	/**
	 * The following members apply only to the single-channel alarm currently
	 * under construction. They do not relate to combination or compound alarms.
	 */
	private String currentChannelId;
	private String currentAlarmId;
	private IChannelDefinition currentChannelDef;
	private AlarmType alarmType;
	private AlarmLevel alarmLevel;
	private double highCompare;
	private double lowCompare;
	private long valueMask;
	private long validMask;
	private double deltaValue;
	private List<Long> states;
	private boolean isDn = true;
	private int hysteresisIn = 1;
	private int hysteresisOut = 1;
	private String alarmDescription;
	private final Categories categories = new Categories();
	private final KeyValueAttributes kvMap = new KeyValueAttributes();
	
	/**
	 * Indicates whether to skip parsing of the rest of the current alarm.
	 */
	private boolean shouldSkipThisAlarmDefinition = false;

	
	/**
	 * Package protected constructor.
	 * @param maxSchemaVersion the currently implemented max schema version
	 * 
	 */
	AbstractAlarmDictionary(final String maxSchemaVersion) {
	    super(DictionaryType.ALARM, maxSchemaVersion);
	}
    
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDictionary#setChannelMap(java.util.Map)
	 */
	@Override
	public void setChannelMap(
			final Map<String, IChannelDefinition> providedChannelMap) {
	    if (providedChannelMap == null) {

	        throw new IllegalArgumentException(
	                "Null map of channel IDs to channel definition is not supported.");
	    }
		this.channelDefinitionMap = new HashMap<String, IChannelDefinition>(providedChannelMap);
	}


	@Override
	public synchronized Map<String, List<IAlarmDefinition>> getSingleChannelAlarmMapByChannel() {
	    /* Make this a sorted map */
		return new TreeMap<String, List<IAlarmDefinition>>(this.alarmDefinitionsByChannel);
	}



	@Override
	public synchronized Map<String, IAlarmDefinition> getSingleChannelAlarmMapByAlarmId() {
	    /*  Make this a sorted map */
		return new TreeMap<String, IAlarmDefinition>(this.alarmDefinitionsByAlarmId);
	}


	@Override
	public synchronized List<IAlarmDefinition> getSingleChannelAlarmDefinitions(
			final String channelId) {
		if (channelId == null) {
			throw new IllegalArgumentException("Input channel ID was null!");
		}

		final List<IAlarmDefinition> current = this.alarmDefinitionsByChannel
				.get(channelId);
		if ((current == null) || current.isEmpty()) {
			return null;
		}

		return new LinkedList<IAlarmDefinition>(current);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDictionary#getSingleChannelAlarmDefinitions()
	 */
	@Override
	public synchronized List<IAlarmDefinition> getSingleChannelAlarmDefinitions() {
		final List<IAlarmDefinition> aggregateAlarmList = new ArrayList<IAlarmDefinition>(
				alarmDefinitionsByChannel.size());
		for (final List<IAlarmDefinition> al : alarmDefinitionsByChannel.values()) {
			for (final IAlarmDefinition ad : al) {
				aggregateAlarmList.add(ad);
			}
		}

		return aggregateAlarmList;
	}


	@Override
	public synchronized Map<String, ICombinationAlarmDefinition> getCombinationAlarmMap() {
	    /*  Make this a sorted map */
		return new TreeMap<String, ICombinationAlarmDefinition>(this.combinationAlarmMap);
	}


	@Override
	public synchronized List<IAlarmOffControl> getOffControls() {

		return new LinkedList<IAlarmOffControl>(this.offControls.values());
	}
	
    
    @Override
    public void addReloadListener(final IAlarmReloadListener l) {
        throw new UnsupportedOperationException("This alarm definition provider does not support automatic reload");
    }
    
    @Override
    public void removeReloadListener(final IAlarmReloadListener l) {
        throw new UnsupportedOperationException("This alarm definition provider does not support automatic reload");
    }

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.IBaseDictionary#clear()
	 */
	@Override
	public synchronized void clear() {
		alarmDefinitionsByChannel.clear();
		alarmDefinitionsByAlarmId.clear();
		combinationAlarmMap.clear();
		alarmIdToChannelIdMap.clear();
		offControls.clear();
		shouldSkipThisAlarmDefinition = false;
		resetCompoundParameters();
		resetAlarmParameters();
		resetChannelParameters();
		super.clear();
	}

	/**
	 * Sets the given channel to be the current alarm channel. This sets up the
	 * current channel definition and ID for the parser.
	 * 
	 * @param channelId
	 *            the channel ID string
	 * @return true if the channel is found in the channel dictionary; false if
	 *         not
	 */
	protected boolean startChannel(final String channelId) {
		this.currentChannelId = channelId;
		this.currentChannelDef = this.channelDefinitionMap
				.get(currentChannelId);
		if (this.currentChannelDef == null) {
			tracer.warn("No channel definition for alarm channel id "
					+ this.currentChannelId);
			return false;
		}
		return true;
	}

	/**
	 * Ends parsing of alarms for the current channel.
	 */
	protected void endChannel() {
		resetChannelParameters();
	}

	/**
	 * Starts parsing of a single channel alarm definition. Do not use this
	 * method for compound or combination alarms. Note that any argument may be
	 * null, but if so, the affected members must still be set before calling
	 * endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param alarmId
	 *            the unique ID for the new alarm; may be null
	 * @param level
	 *            the level for the new alarm; may be null
	 * @param type
	 *            the type of the new alarm; may be null
	 */
	protected void startSimpleAlarm(final String alarmId,
			final AlarmLevel level, final AlarmType type) {
		this.alarmLevel = level;
		this.currentAlarmId = alarmId;
		this.alarmType = type;
	}

	/**
	 * Ends parsing of the current single-channel alarm definition. Do not use
	 * this method for combination or compound alarms. This method will create
	 * the IAlarmDefinition object using the alarm parameters currently stored
	 * in member variables and add the definition to the appropriate maps.
	 */
	protected void endSimpleAlarm() {
		IAlarmDefinition alarm = null;
		if (alarmType.equals(AlarmType.NO_TYPE)) {
			resetAlarmParameters();
			return;
		}
		alarm = createSimpleAlarm();
		addSimpleOrCompoundAlarm(alarm);
		resetAlarmParameters();
	}

	/**
	 * Starts parsing of a compound alarm.
	 * 
	 * @param alarmId
	 *            the alarm ID; may be null
	 */
	protected void startCompoundAlarm(final String alarmId) {
		isInCompoundAlarm = true;
		compoundAlarm = AlarmDefinitionFactory.createCompoundAlarm(null,
				this.currentChannelId);
		compoundAlarm.setAlarmId(alarmId);
	}

	/**
	 * Indicates whether a compound alarm is currently being parsed.
	 * 
	 * @return true of parsing a compound alarm
	 */
	protected boolean inCompoundAlarm() {
		return isInCompoundAlarm;
	}

	/**
	 * Ends parsing of a compound alarm.
	 */
	protected void endCompoundAlarm() {
		// Must set this first for the add to work right
		isInCompoundAlarm = false;
		compoundAlarm.setChannelId(currentChannelId);
		compoundAlarm.setCheckOnEu(!compoundIsDn);
		addSimpleOrCompoundAlarm(compoundAlarm);
		resetCompoundParameters();
	}

	/**
	 * Retrieves the current single-channel alarm ID. Do not use this method for
	 * combination or compound alarms.
	 * 
	 * @return alarm ID string; may be null
	 */
	protected String getCurrentAlarmId() {
		return this.currentAlarmId;
	}

	/**
	 * Retrieves the current channel definition.
	 * 
	 * @return channel definition object; may be null
	 */
	protected IChannelDefinition getCurrentChannelDefinition() {
		return this.currentChannelDef;
	}

	/**
	 * Gets the type of the current simple alarm.
	 * 
	 * @return the AlarmType
	 */
	protected AlarmType getAlarmType() {
		return this.alarmType;
	}

	/**
	 * Gets the current channel ID.
	 * 
	 * @return the current channel ID, or null if none set
	 */
	protected String getCurrentChannelId() {
		return currentChannelId;
	}

	/**
	 * Returns the integer value of a symbolic value from a named enumeration.
	 * 
	 * @param enumId
	 *            name of the enumeration; if null, the enumeration attached
	 *            to the current channel will be used
	 * @param enumSymbol
	 *            the enumeration symbol (string) to map
	 * @return the integer value of the enumeration symbol, 0 if no such value
	 *         in the named enumeration, or -1 if no such enumeration defined
	 *         for the current channel
	 * 
	 */
	protected long getEnumValue(final String enumId, final String enumSymbol) {
		final EnumerationDefinition currentEnum = currentChannelDef.getLookupTable();
		if (enumId == null || currentEnum.getName().equalsIgnoreCase(enumId)) {
            return currentEnum.getKey(enumSymbol);
		}
		return -1;
	}

	/**
	 * Sets the flag indicating that the current single-channel alarm is on the
	 * channel EU.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 */
	protected void setIsEu() {
		isDn = false;
	}

	/**
	 * Sets the flag indicating that the current compound alarm is on the
	 * channel EU.
	 * <p>
	 * Value will apply to next call of endCompoundAlarm().
	 */
	protected void setCompoundIsEu() {
		compoundIsDn = false;
	}

	/**
	 * Adds state values for the current single-channel alarm.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param inStates
	 *            the values to add
	 */
	protected void addStates(final long[] inStates) {
		if (inStates == null) {
			return;
		}
		if (states == null) {
			states = new ArrayList<Long>();
		}
		for (int i = 0; i < inStates.length; i++) {
			states.add(inStates[i]);
		}

	}

	/**
	 * Sets the delta change limit for the current single-channel alarm.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param limit
	 *            the delta value
	 */
	protected void setDeltaLimit(final double limit) {
		this.deltaValue = limit;
	}

	/**
	 * Resets parameters to the starting state for parsing a new single channel
	 * alarm.
	 */
	protected void resetAlarmParameters() {
		highCompare = -1;
		lowCompare = -1;
		valueMask = -1;
		validMask = -1;
		deltaValue = -1;
		states = null;
		isDn = true;
		hysteresisIn = 1;
		hysteresisOut = 1;
		currentAlarmId = null;
		alarmLevel = AlarmLevel.NONE;
		alarmType = AlarmType.NO_TYPE;
		alarmDescription = null;
		categories.clearMap();
		kvMap.clearKeyValue();
	}

	/**
	 * Resets parameters the starting state for selecting a new channel from the
	 * dictionary.
	 */
	protected void resetChannelParameters() {
		currentChannelDef = null;
		currentChannelId = null;
	}

	/**
	 * Resets parameters to the starting state for parsing new compound alarms.
	 */
	protected void resetCompoundParameters() {
		compoundIsDn = true;
		isInCompoundAlarm = false;
		compoundAlarm = null;
		categories.clearMap();
		kvMap.clearKeyValue();
		alarmDescription = null;
	}

	/**
	 * Creates a single-channel alarm definition based upon the single-channel
	 * parameters already set by the parser. Do not use this method for
	 * combination or compound alarms.
	 * 
	 * @return new IAlarmDefinition
	 */
	protected IAlarmDefinition createSimpleAlarm() {
		IAlarmDefinition alarm = null;

		switch (this.alarmType) {
		case HIGH_VALUE_COMPARE:
			alarm = AlarmDefinitionFactory.createHighAlarm(currentAlarmId,
					currentChannelId, alarmLevel, highCompare);
			break;
		case LOW_VALUE_COMPARE:
			alarm = AlarmDefinitionFactory.createLowAlarm(currentAlarmId,
					currentChannelId, alarmLevel, lowCompare);
			break;
		case STATE_COMPARE:
			alarm = AlarmDefinitionFactory.createStateAlarm(currentAlarmId,
					currentChannelId, alarmLevel, states);
			break;
		case VALUE_CHANGE:
			alarm = AlarmDefinitionFactory.createChangeAlarm(currentAlarmId,
					currentChannelId, alarmLevel);
			break;
		case VALUE_DELTA:
			alarm = AlarmDefinitionFactory.createDeltaAlarm(currentAlarmId,
					currentChannelId, alarmLevel, deltaValue);
			break;
		case EXCLUSIVE_COMPARE:
			alarm = AlarmDefinitionFactory.createExclusiveRangeAlarm(
					currentAlarmId, currentChannelId, alarmLevel, lowCompare,
					highCompare);
			break;
		case INCLUSIVE_COMPARE:
			alarm = AlarmDefinitionFactory.createInclusiveRangeAlarm(
					currentAlarmId, currentChannelId, alarmLevel, lowCompare,
					highCompare);
			break;
		case DIGITAL_COMPARE:
			alarm = AlarmDefinitionFactory.createDigitalAlarm(currentAlarmId,
					currentChannelId, alarmLevel, valueMask, validMask);
			break;
		case MASK_COMPARE:
			alarm = AlarmDefinitionFactory.createMaskAlarm(currentAlarmId,
					currentChannelId, alarmLevel, valueMask);
			break;
		default:
			throw new IllegalArgumentException("AlarmType " + this.alarmType
					+ " is not supported by this method");
		}

		alarm.setCheckOnEu(!isDn);
		alarm.setHysteresisInValue(hysteresisIn);
		alarm.setHysteresisOutValue(hysteresisOut);
		alarm.setAlarmDescription(alarmDescription);
		alarm.setCategories(this.categories);	
		alarm.setKeyValueAttributes(this.kvMap);
		return alarm;
	}

	
	/**
	 * Adds a single-channel or compound alarm definition to the alarm maps as a
	 * top-level alarm, or as a child of the current compound alarm if there is
	 * one. If adding a top-level alarm, removes any off controls that apply to
	 * the new alarm, i.e., addition of this definition to the alarm definitions
	 * for a channel means that alarms on the channel are now enabled again. Do
	 * not use for combination alarms.
	 * 
	 * @param ad
	 *            the IAlarmDefinition to add
	 */
	private void addSimpleOrCompoundAlarm(final IAlarmDefinition ad) {

		/*
		 * Not a top-level alarm definition, but child of current compound alarm
		 * definition. Add it to the current compound alarm definition as a
		 * child. This definition does not go into the maps because it is not
		 * for a standalone alarm.
		 */
		if (isInCompoundAlarm && ad != compoundAlarm) {
			compoundAlarm.addChildAlarmDefinition(ad);
		} else if (currentChannelDef != null) {

			/* First remove any existing definition with the same alarm Id. */
			removeAlarmById(ad.getAlarmId());

			/*
			 * Add it to the list of alarm definitions for
			 * the channel the new alarm applies to. If an alarm with the same
			 * ID already exists, remove it.
			 */
			List<IAlarmDefinition> definedAlarms = alarmDefinitionsByChannel
					.get(ad.getChannelId());
			if (definedAlarms == null) {
				definedAlarms = new LinkedList<IAlarmDefinition>();
				alarmDefinitionsByChannel.put(ad.getChannelId(), definedAlarms);
			}

			definedAlarms.add(ad);

			/* Add the definition to the alarm ID maps. */
			this.alarmDefinitionsByAlarmId.put(ad.getAlarmId(), ad);
			this.alarmIdToChannelIdMap.put(ad.getAlarmId(), ad.getChannelId());

			/*
			 * Remove any off controls that are now invalidated by the addition
			 * of this new definition.
			 */
			removeOffControl(ad.getAlarmId());
			removeOffControl(ad.getChannelId(), ad.getAlarmLevel());
		}

	}

	/**
	 * Adds a combination alarm definition to the map of parsed combination
	 * alarms. If the alarm ID matches a previous ID in the combination alarm
	 * map, the entry is replaced. Removes any off controls that apply to the
	 * new alarm, i.e., adding this alarm definition implies the alarm is valid
	 * again.
	 * 
	 * @param ca
	 *            the combination alarm definition to add
	 * @throws SAXParseException
	 *             if the supplied alarm ID matches an existing single channel
	 *             alarm ID
	 */
	protected void addCombinationAlarm(final ICombinationAlarmDefinition ca)
			throws SAXParseException {

		if (this.alarmDefinitionsByAlarmId.get(ca.getAlarmId()) != null) {
			error("Found the same alarm ID ("
					+ ca.getAlarmId()
					+ ") on combination and single alarm without an off control in between");
		}
		removeAlarmById(ca.getAlarmId());
		this.combinationAlarmMap.put(ca.getAlarmId(), ca);
		removeOffControl(ca.getAlarmId());
	}

	/**
	 * Sets the type of the current single channel alarm.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param type
	 *            the AlarmType to set
	 */
	protected void setAlarmType(final AlarmType type) {
		this.alarmType = type;
	}

	/**
	 * Sets the low limit on the current single channel alarm.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param limit
	 *            the low limit to set
	 */
	protected void setLowCompareLimit(final double limit) {
		this.lowCompare = limit;
	}

	/**
	 * Sets the high limit on the current single channel alarm.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param limit
	 *            the high limit to set
	 */
	protected void setHighCompareLimit(final double limit) {
		this.highCompare = limit;
	}

	/**
	 * Sets mask 2 on the current single channel alarm. For Digital alarms, mask
	 * 2 is the "valid mask". Valid mask does not apply to Mask alarms.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param mask
	 *            the mask to set
	 */
	protected void setValidMask(final long mask) {
		this.validMask = mask;
		if (alarmType != null && alarmType.equals(AlarmType.DIGITAL_COMPARE)) {
			validMask = mask & 0xFFFFFFFFL;

		} else {
			validMask = mask;

		}
	}

	/**
	 * Sets mask 1 on the current single channel alarm. For Digital alarms, mask
	 * 1 is the "value mask". For Mask alarms, mask 1 is the only mask.
	 * 
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param mask
	 *            the mask to set
	 */
	protected void setValueMask(final long mask) {

		if (alarmType != null && alarmType.equals(AlarmType.DIGITAL_COMPARE)) {
			valueMask = mask & 0xFFFFFFFFL;

		} else {
			valueMask = mask;

		}
	}

	/**
	 * Saves a new values for the HysteresisIn setting for the simple alarm.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param hysteresisInValue
	 *            the hysteresis count to set
	 * 
	 */
	protected void setHysteresisInValue(final int hysteresisInValue) {
		hysteresisIn = hysteresisInValue;
	}

	/**
	 * Saves a new values for the HysteresisOut setting for the current simple
	 * alarm.
	 * <p>
	 * Value will apply to next call of endSimpleAlarm() or createSimpleAlarm().
	 * 
	 * @param hysteresisOutValue
	 *            the hysteresis count to set
	 * 
	 */
	protected void setHysteresisOutValue(final int hysteresisOutValue) {
		hysteresisOut = hysteresisOutValue;
	}
	/**
	 * Saves an alarm description for the current alarm.
	 * 
	 * @param desc
	 *            the description of the current alarm
	 * 
	 */
	protected void setAlarmDescription(final String desc) {
		alarmDescription = desc;
	}

	/**
	 * Sets the category name and value in the category map.
	 * 
	 * @param catName To fetch one of the standard categories, use constant Categories.OPS_CAT, 
	 * Categories.SUBSYSTEM or Categories.MODULE.
	 * 
	 * @param catValue The string value of category name.
	 */
	protected void setCategory(final String catName, final String catValue) {
		this.categories.setCategory(catName, catValue);
	}

	/**
	 * Sets the attribute name and value in the key-value map.
	 * 
	 * @param attrName The name of the attribute key.
	 * @param attrValue The string value of the attribute key.
	 */
	protected void setAttribute(final String attrName, final String attrValue) {
		this.kvMap.setKeyValue(attrName, attrValue);
	}

	/**
	 * Sets the flag indicating to skip the current alarm. Used for all alarm
	 * types.
	 */
	protected void skipThisAlarmDefinition() {
		shouldSkipThisAlarmDefinition = true;
	}

	/**
	 * Gets the flag indicating to skip the current alarm. Used for all alarm
	 * types.
	 * 
	 * @return true if the current alarm should be skipped.
	 */
	protected boolean isSkipThisAlarmDefinition() {
		return shouldSkipThisAlarmDefinition;
	}

	/**
	 * Clears the flag indicating to skip the current alarm. Used for all alarm
	 * types.
	 */
	protected void resetSkipThisAlarmDefinition() {
		shouldSkipThisAlarmDefinition = false;
	}

	/**
	 * Removes the alarm with the given ID from all tables.
	 * @param alarmId the alarm ID to remove
	 */
	private void removeAlarmById(final String alarmId) {

		/*
		 * Remove any existing combination alarm definition from the combination
		 * alarm map.
		 */
		if (this.combinationAlarmMap.get(alarmId) != null) {
            tracer.info("Alarm with ID " + alarmId
                    + " appears more than once in the alarm file. The last definition will be the one in effect");
			this.combinationAlarmMap.remove(alarmId);
		}

		/* Remove existing single channel alarm from the alarm ID map. */
		final IAlarmDefinition existingAlarm = this.alarmDefinitionsByAlarmId
				.get(alarmId);
		if (existingAlarm == null) {
			return;
		}

        tracer.info("Alarm with ID " + alarmId
                + " appears more than once in the alarm file. The last definition will be the one in effect");
		this.alarmDefinitionsByAlarmId.remove(alarmId);

		/* Remove existing single channel alarm from the the channel ID map. */
		final String channelId = this.alarmIdToChannelIdMap.get(alarmId);
		this.alarmIdToChannelIdMap.remove(alarmId);

		final List<IAlarmDefinition> alarmsForChannel = this.alarmDefinitionsByChannel
				.get(channelId);

		if (alarmsForChannel == null) {
			return;
		}
		final Iterator<IAlarmDefinition> iter = alarmsForChannel.iterator();
		while (iter.hasNext()) {
			if (iter.next().getAlarmId().equals(alarmId)) {
				iter.remove();
				break;
			}
		}
		if (alarmsForChannel.isEmpty()) {
			this.alarmDefinitionsByChannel.remove(channelId);
		}
	}

	/**
	 * Creates the key for an off control with ALARM scope.
	 * 
	 * @param alarmId the alarmId for the off control
	 * @return unique key
	 */
	private String getOffControlKey(final String alarmId) {
		return OffScope.ALARM + OFF_CONTROL_ID_SEP + alarmId;
	}

	/**
	 * Creates the key for an off control with CHANNEL or CHANNEL_AND_LEVEL scope.
	 * 
	 * @param channelId the channel ID for the off control
	 * @param level the level for the off control; if null or NONE, the off control
	 * is assumed to have scope CHANNEL
	 * @return unique key
	 */
	private String getOffControlKey(final String channelId, final AlarmLevel level) { 
		if (level == null || level == AlarmLevel.NONE) {
			return OffScope.CHANNEL + OFF_CONTROL_ID_SEP + channelId;
		} else {
			return OffScope.CHANNEL_AND_LEVEL + OFF_CONTROL_ID_SEP + channelId + OFF_CONTROL_ID_SEP + level;
		}
	}

	/**
	 * Creates the hash key for the specified off control object.
	 * 
	 * @param off the off control object
	 * @return unique key
	 */
	private String getOffControlKey(final IAlarmOffControl off) {
		switch (off.getScope()) {
		case ALARM:
			return getOffControlKey(off.getAlarmId());
		case CHANNEL:
		case CHANNEL_AND_LEVEL:
			return getOffControlKey(off.getChannelId(), off.getLevel());
		default:
			throw new IllegalArgumentException("Unrecognized off scope " + off.getScope());
		}
	}

	/**
	 * Adds an ALARM scope off control to the off control map. The control will
	 * overwrite any previous off control with the same alarm ID.
	 * Has the side effect of removing any alarms that match the
	 * supplied ID from the current maps, essentially canceling any previous
	 * definition of the alarm.
	 * 
	 * @param alarmId
	 *            alarm ID of the alarm to be turned off
	 */
	protected void addOffControl(final String alarmId) {
		final IAlarmOffControl control = AlarmDefinitionFactory
				.createOffControlForAlarm(alarmId);
		this.offControls.put(getOffControlKey(control), control);
		removeAlarmById(alarmId);
	}

	/**
	 * Adds a CHANNEL scope off control to the off control map. The control will
	 * replace any previous off control object for the specific channel and
	 * level. Has the side effect of removing any alarms that match the supplied
	 * channel ID and level from the current maps, essentially canceling any
	 * previous definition of those alarms.
	 * 
	 * @param channelId channel ID the off control is associated with
	 * @param level alarm level the off control is associated with
	 * 
	 */
	protected void addOffControl(final String channelId, final AlarmLevel level) {

		final AlarmLevel removeLevel = level == null ? AlarmLevel.NONE : level;

		/*
		 * Remove any old matching off control.
		 */
		removeOffControl(channelId, level);

		/* 
		 * Create the new off control and add it to the map.
		 */
		final IAlarmOffControl control = AlarmDefinitionFactory
				.createOffControlForChannel(channelId, removeLevel);
		this.offControls.put(getOffControlKey(control), control);

		/*
		 * Remove any existing definitions that match the given channel ID and
		 * level from both the channel ID map and the Alarm ID map. This type of
		 * off control does not apply to combination alarms.
		 */
		final List<IAlarmDefinition> alarmsForChannel = this.alarmDefinitionsByChannel
				.get(channelId);
		if (alarmsForChannel == null) {
			return;
		}
		final List<IAlarmDefinition> newAlarmsForChannel = new LinkedList<IAlarmDefinition>();
		for (final IAlarmDefinition def : alarmsForChannel) {
			if (def.getAlarmLevel() == removeLevel || removeLevel == AlarmLevel.NONE) {
				this.alarmDefinitionsByAlarmId.remove(def.getAlarmId());
				this.alarmIdToChannelIdMap.remove(def.getAlarmId());
			} else {
				newAlarmsForChannel.add(def);
			}
		}
		if (newAlarmsForChannel.isEmpty()) {
			this.alarmDefinitionsByChannel.remove(channelId);
		} else {
			this.alarmDefinitionsByChannel.put(channelId, newAlarmsForChannel);
		}
	}

	/**
	 * Removes the off control for the alarm with the specified ID.
	 * 
	 * @param alarmId
	 *            the ID of the alarm to remove the control for
	 */
	private void removeOffControl(final String alarmId) {
		this.offControls.remove(getOffControlKey(alarmId));
	}

	/**
	 * Removes the off control for alarms with the specified channel ID and
	 * level. If level is null or NONE, then channel only, RED, and YELLOW off
	 * controls are removed for the channel. If level is RED or YELLOW, and any
	 * channel-only off control exists, the channel only control will be removed
	 * and will be replaced by a RED or YELLOW off control, whichever does NOT
	 * match the input level.
	 * 
	 * @param channelId
	 *            the ID of the channel to remove off controls for
	 * @param level
	 *            the level of the alarms to remove off controls for
	 */
	private void removeOffControl(final String channelId, final AlarmLevel level) {

		if (level == null | level == AlarmLevel.NONE) {

			this.offControls.remove(getOffControlKey(channelId, AlarmLevel.NONE));
			this.offControls.remove(getOffControlKey(channelId, AlarmLevel.RED));
			this.offControls.remove(getOffControlKey(channelId, AlarmLevel.YELLOW));

		} else {
			this.offControls.remove(getOffControlKey(channelId, level));
			final IAlarmOffControl anyLevelControl = this.offControls.get(getOffControlKey(channelId, AlarmLevel.NONE));
			if (anyLevelControl != null) {
				final AlarmLevel otherLevel = (level == AlarmLevel.RED) ? AlarmLevel.YELLOW : AlarmLevel.RED;
				this.offControls.remove(getOffControlKey(channelId, AlarmLevel.NONE));
				addOffControl(channelId, otherLevel);
			}
		}
	}

}
