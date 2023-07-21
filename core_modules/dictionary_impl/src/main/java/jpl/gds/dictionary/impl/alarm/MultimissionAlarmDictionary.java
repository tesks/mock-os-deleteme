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

import java.util.Arrays;
import java.util.Stack;

import jpl.gds.dictionary.api.ICategorySupport;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.alarm.AlarmCombinationType;
import jpl.gds.dictionary.api.alarm.AlarmDefinitionFactory;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.AlarmType;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationGroup;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;

/**
 * MultmissionAlarmDictionary implements the IAlarmDictionary interface for
 * multimission. It parses the information in a multimission-format alarm
 * definition file. <br>
 * This class should not be instantiated directly. The AlarmDictionaryFactory
 * class should be used for getting the IAlarmDictionary object tailored to the
 * current mission.
 * 
 * @since AMPCS R5
 *
 * @see IAlarmDictionary
 * @see AlarmDictionaryFactory
 * 
 */
public class MultimissionAlarmDictionary extends AbstractAlarmDictionary {
    
    /* Get schema version from config file */
    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.ALARM);

	private final StringBuilder sb = new StringBuilder(1024);

	private boolean checkIsEu = false;
	private AlarmLevel currentLevel = AlarmLevel.NONE;
	private boolean inCombinationAlarmLevel = false;
	private boolean inCompoundAlarmLevel = false;

	/* Added last source alarm ID member to track
	 * combination source proxy IDs. 
	 */
	private String lastSourceAlarmId = null;
	private String attrKey;

	/*  Added members to ensure the XML is to the proper schema. */
    private static String ROOT_ELEMENT_NAME = "alarm_dictionary";
    private static String HEADER_ELEMENT_NAME = "header";   
	
	/**************************************
	 * BEGIN COMBINATION ALARMS VARIABLES *
	 **************************************/
	/*
	 * currentCombinationAlarm's null or non-null test will serve as the flag
	 * for "in combination alarm definition"; unlike other alarm definitions,
	 * CombinationAlarm is to be instantiated at the start element (not end),
	 * for it provides the context for its source and target components as they
	 * are instantiated.
	 */
	private ICombinationAlarmDefinition currentCombinationAlarm = null;
	private final Stack<ICombinationGroup> combinationAlarmSourcesStack = new Stack<ICombinationGroup>();
	/************************************
	 * END COMBINATION ALARMS VARIABLES *
	 ************************************/

	private enum TestType {
		DN, EU
	};

	/**
	 * Constructor.
	 * 
	 */
	MultimissionAlarmDictionary() {
	    super(MM_SCHEMA_VERSION);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AbstractAlarmDictionary#clear()
	 */
	@Override
	public void clear() {
		checkIsEu = false;
		currentLevel = AlarmLevel.NONE;
		currentCombinationAlarm = null;
		combinationAlarmSourcesStack.clear();
		super.clear();
	}

	/**
	 * Parse test_type attribute value in context of current alarm element
	 * 
	 * @param arg
	 */
	private boolean parseTestTypeIsEU(final String arg) {
		final TestType val = TestType.valueOf(arg);
		return val.equals(TestType.EU);
	}

	/**
	 * Parse level attribute value for the specified alarm element.
	 * 
	 * @param elementName
	 *            the name of the XML element being parsed
	 * @param attr
	 *            the XML attributes of the element
	 * @return AlarmLevel
	 * @throws SAXParseException
	 *             if there is a fatal parsing error
	 */
	private AlarmLevel parseAlarmLevel(final String elementName,
			final Attributes attr) throws SAXParseException {
		final String arg = getRequiredAttribute("level", elementName, attr);
		try {
			return AlarmLevel.valueOf(AlarmLevel.class, arg);
		} catch (final IllegalArgumentException e) {
			error("level attribute on " + elementName
					+ " element must be RED or YELLOW");
		}
		// NOTREACHED, but parser can't tell that
		return AlarmLevel.NONE;
	}
	
	/**
	 * Added to parse the alarm categories.
	 * 
	 * Parse category attribute(s) name and value.
	 * 
	 * @param elementName
	 *            the category name "category"
	 * @param attr
	 *            the name and value attributes of the category
	 */
	private void parseCategoryAttributes(final String elementName, final Attributes attr) 
			throws SAXParseException {
		final String catName = getRequiredAttribute("name", elementName, attr);
		final String catValue = getRequiredAttribute("value", elementName, attr);
		if (catName != null && catValue != null) {
			if (inCombinationAlarmLevel) {
				/* Set category for combination alarm */
				if (catName.equalsIgnoreCase("ops_category")) {
					currentCombinationAlarm.setCategory(ICategorySupport.OPS_CAT, catValue);
				} else {
					currentCombinationAlarm.setCategory(catName, catValue);
				}
			} else if (inCompoundAlarmLevel) {
				if (catName.equalsIgnoreCase("ops_category")) {
					compoundAlarm.setCategory(ICategorySupport.OPS_CAT, catValue);
				} else {
					/* Set category for compound alarm */
					compoundAlarm.setCategory(catName, catValue);
				}
			} else {
				/* Set category for primitive alarm */
				if (catName.equalsIgnoreCase("ops_category")) {
					setCategory(ICategorySupport.OPS_CAT, catValue);
				} else {
					setCategory(catName, catValue);
				}
			}
		}
	}
    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     * 
     */
    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        /* Set starting required elements */
        setRequiredElements("Multimission", Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
    }
	
	 /* {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.impl.alarm.AbstractAlarmDictionary#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName,
			final String qname, final Attributes attr) throws SAXException {
		super.startElement(uri, localName, qname, attr);
		
		/*  Parse header info using common method */
		parseMultimissionHeader(localName, attr);

		if (qname.equalsIgnoreCase("off_control_by_channel")) {
			/*
			 * This is the way of turning alarms for a channel off in the 2.0
			 * schema. Turn off all alarms with the specified level on the
			 * specified channel.
			 */
			final String chanId = getRequiredAttribute("channel_id", qname, attr);
			final String levelStr = attr.getValue("level");
			AlarmLevel level = AlarmLevel.NONE;
			if (levelStr != null) {
				try {
					level = Enum.valueOf(AlarmLevel.class, levelStr);
				} catch (final IllegalArgumentException e) {
					error("Level is invalid in off control element: "
							+ levelStr);
				}
			}
			addOffControl(chanId, level);

		} else if (qname.equalsIgnoreCase("off_control_by_id")) {
			/*
			 * This is the way of turning a specific alarm off in the 2.0
			 * schema.
			 */
			final String alarmId = getRequiredAttribute("id", qname, attr);
			addOffControl(alarmId);

		} else {

			/*
			 * Intermingling pieces of combination alarm definition parsing
			 * logic with that of simple alarm definition's complicates the
			 * code. To keep the code intuitive and simple to troubleshoot,
			 * separate the parsing logic between combination alarms and simple
			 * alarms via two independent branches of execution below.
			 */
			if (qname.equalsIgnoreCase("combination_alarm")
					|| currentCombinationAlarm != null) {
				startElementCombinationAlarmMode(qname, attr);
			} else {
				startElementSimpleAlarmMode(qname, attr);
			}

			/*
			 * Both simple alarms and combination alarms can handle the
			 * extracting of the alarm parameters the same way. Call a common
			 * method for this purpose.
			 */
			startElementSimpleAlarmParameterMode(qname, attr);
		}

	}

	/**
	 * Processes a start element event when the parser knows it is parsing a
	 * simple (non-combination) alarm.
	 * 
	 * @param attr
	 *            the XML attributes of the element
	 * @param elementName
	 *            name the name of the XML element the start event applies to
	 * 
	 * @throws SAXException
	 *             if there is any problem parsing the element
	 */
	private void startElementSimpleAlarmMode(final String elementName,
			final Attributes attr) throws SAXException {
		try {

			if (elementName.equalsIgnoreCase("alarm")) {
				/*
				 * Not an alarm at all, actually, in spite of the element name.
				 * This is starting a set of simple alarm definitions for a new
				 * channel.
				 * 
				 * Both the simple alarms and combination alarms can process the
				 * attached attributes the same way. Call a common method for
				 * handling this element.
				 */
				startAlarmElement(elementName, attr);

			} else if (this.isSkipThisAlarmDefinition()) {
				tracer.debug("Skipping alarm definition");

			} else if (elementName.equalsIgnoreCase("compound_alarm")) {

				/*
				 * Found start of a compound alarm. ID is mandatory in the new
				 * (2.0) schema, but was not in the old schema. For now, use it
				 * if there but do not complain if it is not.
				 */
				final String alarmId = attr.getValue("id");
				this.startCompoundAlarm(alarmId);
				if (checkIsEu) {
					this.setCompoundIsEu();
				}
				inCompoundAlarmLevel = true;

			} else if (elementName.endsWith("_alarm")) {
				inCompoundAlarmLevel = false;

				/*
				 * We found a specific type of simple alarm for the current
				 * channel. Start parsing it. ID is mandatory in the new (2.0)
				 * schema, but was not in the old schema. For now, use it if
				 * there but do not complain if it is not.
				 */
				final String alarmId = attr.getValue("id");
				parseHysteresisValues(attr);
				final AlarmLevel level = parseAlarmLevel(elementName, attr);
				final AlarmType type = mapAlarmType(elementName);
				startSimpleAlarm(alarmId, level, type);
				if (checkIsEu) {
					this.setIsEu();
				}
			} else if (elementName.equalsIgnoreCase("category")) {
				parseCategoryAttributes(elementName, attr);
			} else if (elementName.equalsIgnoreCase("keyword_value")) {
				attrKey = getRequiredAttribute("key", elementName, attr);
			}


		} catch (final SAXParseException x) {
			error(x);
		}

	}

	/**
	 * Processes a start element event when the parser knows it is parsing a
	 * combination alarm.
	 * 
	 * @param attr
	 *            the XML attributes of the element
	 * @param elementName
	 *            name the name of the XML element the start event applies to
	 * 
	 * @throws SAXException
	 *             if there is any problem parsing the element
	 */
	private void startElementCombinationAlarmMode(final String elementName,
			final Attributes attr) throws SAXException {

		try {

			if (elementName.equalsIgnoreCase("combination_alarm")) {
				/*
				 * Found a combination alarm. Start parsing it. The alarm must
				 * have an ID.
				 */
				final String combinationAlarmId = getRequiredAttribute("id",
						elementName, attr).trim();
				tracer.trace("Parsing combination alarm definition: "
						+ combinationAlarmId);

				/*
				 * This level will be applied to the combination target and
				 * source proxies.
				 */
				currentLevel = Enum
						.valueOf(
								AlarmLevel.class,
								getRequiredAttribute("level", elementName, attr)
								.trim());

				/*
				 * Create a new combination alarm.
				 */
				currentCombinationAlarm = AlarmDefinitionFactory
						.createCombinationAlarm(combinationAlarmId,
								currentLevel);
				inCombinationAlarmLevel = true;
			} else if (elementName.equalsIgnoreCase("combination")) {
				inCombinationAlarmLevel = false;

				/*
				 * Starts a source combination group within the current
				 * combination alarm.
				 */
				final AlarmCombinationType comboType = Enum.valueOf(
						AlarmCombinationType.class,
						getRequiredAttribute("combination_type", elementName,
								attr).trim());

				/*
				 * Combinations can have nested combinations. So there is a
				 * stack of combination groups. Create a group ID for this new
				 * group.
				 * 
				 * Group ID will follow this convention: (1) Top-level group
				 * will have the ID of "HEAD" (2) Any group that is not the
				 * top-level element will have the ID of
				 * <parent-group's-ID>_<this-group's-operand-#> (3)
				 * <this-group's-operand-#> is 1-indexed, and the range spans
				 * the operands count of the parent's group only.
				 */
				/*  Use underscore rather than slash
				 * as group ID separator.
				 */
				final String groupId = combinationAlarmSourcesStack.isEmpty() ? "HEAD"
						: combinationAlarmSourcesStack.peek().getGroupId()
						+ "_"
						+ (combinationAlarmSourcesStack.peek()
								.getOperands().size() + 1);

				/*
				 * Now create the combination group and push it onto the stack.
				 * If the stack is not empty, this combination becomes a child
				 * of the combination group currently on the top of the stack.
				 */
				final ICombinationGroup comboGroup = AlarmDefinitionFactory
						.createCombinationAlarmGroup(currentCombinationAlarm,
								comboType, groupId);

				if (!combinationAlarmSourcesStack.isEmpty()) {
					combinationAlarmSourcesStack.peek().addOperand(comboGroup);
				}

				combinationAlarmSourcesStack.push(comboGroup);
				comboAlarmsLogger
				.trace("MultimissionAlarmDictionary: start element -> combination combination_type=\""
						+ comboType
						+ "\"; stack is now "
						+ combinationAlarmSourcesStack.size() + " deep");

			} else if (this.isSkipThisAlarmDefinition()) { // start element
				comboAlarmsLogger
				.trace("MultimissionAlarmDictionary: skip is on; skipping start element -> "
						+ elementName);

			} else if (elementName.equalsIgnoreCase("target_channel")) { // start
				// element
				/*
				 * Found the start of a target channel definition for the
				 * current combination alarm. This sets the current channel, or
				 * will set the "skip alarm" flag if the channel is invalid.
				 */
				startAlarmElement(elementName, attr);

			} else if (elementName.equalsIgnoreCase("combo_source_alarm")) {
				/*
				 * Found the start of a source alarm definition. This sets the
				 * current channel, or will set the "skip alarm" flag if the
				 * channel is invalid.
				 */
				startAlarmElement(elementName, attr);

			} else if (elementName.endsWith("_alarm")) {

				/*
				 * We have found a simple source alarm that is a source to the
				 * current combination alarm for the current channel. Start
				 * parsing of the simple alarm, using the level in the current
				 * combination alarm, which we saved in a member variable. ID is
				 * mandatory in the new (2.0) schema, but was not in the old
				 * schema. For now, use it if there but do not complain if it is
				 * not.
				 */
				/* Added last source alarm ID member to track
				 * combination source proxy IDs. 
				 */
				lastSourceAlarmId = attr.getValue("id");

				parseHysteresisValues(attr);
				final AlarmType type = mapAlarmType(elementName);
				startSimpleAlarm(lastSourceAlarmId, currentLevel, type);
				if (checkIsEu) {
					this.setIsEu();
				}

			}else if (elementName.equalsIgnoreCase("category")) {
				parseCategoryAttributes(elementName, attr);
			} else if (elementName.equalsIgnoreCase("keyword_value")) {
				attrKey = getRequiredAttribute("key", elementName, attr);
			}

		} catch (final SAXParseException x) {
			error(x);
		}

	}

	/**
	 * Parses the common attributes on "alarm", "combo_source_alarm", and
	 * "target_channel" elements. Sets the current channel. If false is
	 * returned, the "skip this alarm" flag will be set.
	 * 
	 * @param elementName
	 *            name the name of the XML element
	 * @param attr
	 *            the XML attributes for the element being parsed
	 * @return true if the attributes are valid, false if not.
	 * @throws SAXException
	 *             if there is a fatal error parsing the attributes
	 */
	private boolean startAlarmElement(final String elementName,
			final Attributes attr) throws SAXException {
		final String cid = getRequiredAttribute("channel_id", elementName, attr);
		if (!this.startChannel(cid)) {
			tracer.warn(String.format(
					"Skipping alarm definition for channel id %s", cid));
			this.skipThisAlarmDefinition();
			return false;
		}
		checkIsEu = parseTestTypeIsEU(getRequiredAttribute("test_type",
				elementName, attr));
		if (checkIsEu) {
			if (!getCurrentChannelDefinition().hasEu()) {
				tracer.warn("Alarm definition on EU but channel "
						+ getCurrentChannelDefinition().getId()
						+ " has no EU. Skipping definition.");
				this.skipThisAlarmDefinition();
				return false;
			}
		}
		return true;

	}

	/**
	 * Parses the parameters of a simple alarm from the alarm start element. For
	 * instance, parses the high limit value for a high alarm and stores it, the
	 * low limit for a low alarm and stores it, etc. The extracted alarm
	 * parameters are saved in the superclass, with the exception of the state
	 * alarm data type, which is stored in a member variable.
	 * 
	 * @param attr
	 *            the XML attributes of that element
	 * @param elementName
	 *            the name of the XML element the start event is for
	 * 
	 * @throws SAXException
	 *             if there is a fatal parsing error
	 */
	private void startElementSimpleAlarmParameterMode(final String elementName,
			final Attributes attr) throws SAXException {

		if (elementName.equalsIgnoreCase("high_alarm")) {

			try {
				final double highLimit = Double.valueOf(getRequiredAttribute(
						"high_limit", elementName, attr));
				this.setHighCompareLimit(highLimit);
			} catch (final NumberFormatException e) {
				error("high_limit attribute must be a double precision number");
			}

		} else if (elementName.equalsIgnoreCase("low_alarm")) {
			try {
				final double lowLimit = Double.valueOf(getRequiredAttribute(
						"low_limit", elementName, attr));
				this.setLowCompareLimit(lowLimit);
			} catch (final NumberFormatException e) {
				error("low_limit attribute must be a double precision number");
			}

		} else if (elementName.equalsIgnoreCase("delta_alarm")) {
			try {
				final double deltaLimit = Double.valueOf(getRequiredAttribute(
						"delta_limit", elementName, attr));
				this.setDeltaLimit(deltaLimit);
			} catch (final NumberFormatException e) {
				error("delta_limit attribute must be a double precision number");
			}

		} else if (elementName.equalsIgnoreCase("inclusive_alarm")
				|| elementName.equalsIgnoreCase("exclusive_alarm")) {
			try {
				final double lowLimit = Double.valueOf(getRequiredAttribute(
						"low_limit", elementName, attr));
				final double highLimit = Double.valueOf(getRequiredAttribute(
						"high_limit", elementName, attr));
				this.setHighCompareLimit(highLimit);
				this.setLowCompareLimit(lowLimit);
			} catch (final NumberFormatException e) {
				error("low_limit and high_limit attributes must be double precision numbers");
			}
		} else if (elementName.equalsIgnoreCase("digital_alarm")) {
			try {
				final int valueMask = GDR.parse_int(getRequiredAttribute(
						"value_mask", elementName, attr));
				final int validMask = GDR.parse_int(getRequiredAttribute(
						"valid_mask", elementName, attr));
				this.setValueMask(valueMask);
				this.setValidMask(validMask);
			} catch (final NumberFormatException e) {
				error("valid_mask and value_mask attributes must be integers");
			}
		} else if (elementName.equalsIgnoreCase("mask_alarm")) {
			try {
				final int mask = GDR.parse_int(getRequiredAttribute("value_mask",
						elementName, attr));
				this.setValueMask(mask);
			} catch (final NumberFormatException e) {
				error("value_mask attribute must be an integer");
			}

		} else if (elementName.equalsIgnoreCase("off_alarm")) {
			/*
			 * This is the only way of turning alarms off in the old (pre 2.0)
			 * schema. Turn off all alarms on the channel for the specified
			 * level.
			 */
			final String chanId = getRequiredAttribute("id", elementName, attr);
			final String levelStr = attr.getValue("level");
			AlarmLevel level = AlarmLevel.NONE;
			if (levelStr != null) {
				try {
					level = Enum.valueOf(AlarmLevel.class, levelStr);
				} catch (final IllegalArgumentException e) {
					error("Level is invalid in off control element: "
							+ levelStr);
				}
			}
			addOffControl(chanId, level);
			skipThisAlarmDefinition();

		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName,
			final String qname) throws SAXException {

		/*
		 * Intermingling pieces of combination alarm definition parsing logic
		 * with that of simple alarm definition's complicates the code. To keep
		 * the code intuitive and simple to troubleshoot, separate the parsing
		 * logic between combination alarms and simple alarms via two
		 * independent branches of execution below.
		 */

		if (qname.equalsIgnoreCase("combination_alarm")
				|| currentCombinationAlarm != null) {
			endElementCombinationAlarmMode(qname);
		} else {
			endElementSimpleAlarmMode(qname);
		}

		super.endElement(uri, localName, qname);
	}

	/**
	 * Processes an end element event when we know we are parsing a simple
	 * alarm.
	 * 
	 * @param elementName
	 *            name of the XML element the event event is for
	 * 
	 * @throws SAXException
	 *             if there is a fatal parsing error
	 */
	private void endElementSimpleAlarmMode(final String elementName)
			throws SAXException {
		if (elementName.equalsIgnoreCase("alarm")) {
			/*
			 * Found the end of an overall simple alarm element.
			 * endSimpleAlarm() has already been invoked.
			 */
			this.resetSkipThisAlarmDefinition();

		} else if (isSkipThisAlarmDefinition()) {
			tracer.debug("Skipping alarm definition");

		} else if (elementName.equalsIgnoreCase("compound_alarm")) {
			inCompoundAlarmLevel = false;
			endCompoundAlarm();

		} else if (elementName.equalsIgnoreCase("state")) {
			/*
			 * Found the end of a state element within a simple state alarm.
			 * Process this individual state, which is actually in the element
			 * text.
			 */
			endStateElement();

		} else if (elementName.equalsIgnoreCase("description")) {

			if (inCompoundAlarmLevel) {
				compoundAlarm.setAlarmDescription(XmlUtility.normalizeWhitespace(text));
			} else {
				setAlarmDescription(XmlUtility.normalizeWhitespace(text));
				
			}
			
		} else if (elementName.endsWith("keyword_value")) {
			final String attrValue = text.toString();
			if (attrKey != null && attrValue != null) {
				if (inCompoundAlarmLevel) {
					/* Set key-value attributes for compound alarm */
					compoundAlarm.setKeyValueAttribute(attrKey, attrValue);
				} else {
					/* Set key-value attributes for primitive alarm */
					setAttribute(attrKey, attrValue);
				}
			}
				
		} else if (elementName.endsWith("_alarm")) {
			/*
			 * Found the end of a specific simple alarm. This call creates the
			 * alarm from parameters already set in the super class and adds the
			 * alarm to the dictionary maps.
			 */
			endSimpleAlarm();
			inCompoundAlarmLevel = true;
		}

	}

	/**
	 * Processes an end element event when we know we are parsing a combination
	 * alarm.
	 * 
	 * @param elementName
	 *            name of the XML element the event event is for
	 * @throws SAXException
	 *             if there is a fatal parsing error
	 */
	private void endElementCombinationAlarmMode(final String elementName)
			throws SAXException {

		if (elementName.equalsIgnoreCase("combination_alarm")) {

			/*
			 * Found the end of a combination alarm definition. Add it to the
			 * combination alarm map.
			 */
			comboAlarmsLogger
			.trace("MultimissionAlarmDictionary: end element -> combination_alarm; skip="
					+ isSkipThisAlarmDefinition());

			if (!isSkipThisAlarmDefinition()) {
				/*
				 * CurrentCombinationAlarm is valid. Add it to combination
				 * alarms table.
				 */
				comboAlarmsLogger
				.trace("SmapAlarmDictionary: building current combination alarm");
				currentCombinationAlarm.build();
				comboAlarmsLogger
				.debug("SmapAlarmDictionary: adding to combination alarm list the combination alarm "
						+ getCurrentAlarmId());
				addCombinationAlarm(currentCombinationAlarm);
			}

			/*
			 * If all goes well, there really should be no combination groups
			 * left on the stack, but sometimes alarm parsing errors may result
			 * in groups left over. Clear them and start fresh for the next
			 * combination alarm.
			 */
			resetSkipThisAlarmDefinition();
			comboAlarmsLogger
			.trace("MultimissionAlarmDictionary: skip set to off");
			combinationAlarmSourcesStack.clear();
			currentCombinationAlarm = null;

		} else if (isSkipThisAlarmDefinition()) {
			comboAlarmsLogger
			.trace("MultimissionAlarmDictionary: skip is on; skipping end element -> "
					+ elementName);

		} else if (elementName.equalsIgnoreCase("combination")) {
			inCombinationAlarmLevel = true;

			/*
			 * Found the end of a combination group. Pop the current group off
			 * the stack. If it is the top level group, set it to be the source
			 * group for the current combination alarm.
			 */
			final ICombinationGroup justPopped = combinationAlarmSourcesStack.pop();

			if (combinationAlarmSourcesStack.isEmpty()) {
				/*
				 * justPopped is the top-most combination. Add to the
				 * combination alarm.
				 */
				currentCombinationAlarm.setSourceGroup(justPopped);

			}
		} else if (elementName.equalsIgnoreCase("state")) {
			/*
			 * Found the end of a state element within a simple state alarm.
			 * Process this individual state, which is actually in the element
			 * text.
			 */
			endStateElement();

		} else if (elementName.equalsIgnoreCase("description")) {
			/*
			 * Found the alarm description element and process it.
			 * 
			 */
			if (inCombinationAlarmLevel) {
				currentCombinationAlarm.setAlarmDescription(XmlUtility.normalizeWhitespace(text));
			} else {
				setAlarmDescription(XmlUtility.normalizeWhitespace(text));
			}

		} else if (elementName.endsWith("keyword_value")) {
			final String attrValue = text.toString();
			if (attrKey != null && attrValue != null) {
				if (inCombinationAlarmLevel) {
					/* Set key-value attributes for combination alarm */
					currentCombinationAlarm.setKeyValueAttribute(attrKey, attrValue);
				} else {
					/* Set key-value attributes for primitive alarm */
					setAttribute(attrKey, attrValue);
				}
			}
			
		} else if (elementName.endsWith("_alarm")) {
			/*
			 * Found the end of a simple alarm that is a source alarm for the
			 * current combination. For combination alarms,
			 * endAlarm(currentLevel) should not be called because that
			 * automatically adds the simple alarm to the alarm dictionary,
			 * which we do not want to do. We need to wrap each simple alarm
			 * with a source proxy. We copy the endAlarm method as
			 * endSourceProxy and modify it to do this wrapping.
			 */
			endSourceProxy(currentLevel);

		} else if (elementName.equalsIgnoreCase("target_channel")) {
			/*
			 * Found the end of a target channel element for the current
			 * combination alarm. Create the target proxy and attach it to the
			 * current combination alarm.
			 */
			currentCombinationAlarm.addTarget(AlarmDefinitionFactory
					.createCombinationTargetAlarm(currentCombinationAlarm,
							getCurrentChannelId(), !checkIsEu));
			checkIsEu = false;
		}

	}

	/**
	 * Parse type of alarm from element name.
	 * 
	 * @param type
	 *            the XML element name.
	 * @return AlarmType enum
	 * @throws SAXParseException
	 *             if the alarm element name is unrecognized
	 */
	private AlarmType mapAlarmType(final String type) throws SAXParseException {
		if (type.equalsIgnoreCase("high_alarm")) {
			return AlarmType.HIGH_VALUE_COMPARE;
		} else if (type.equalsIgnoreCase("low_alarm")) {
			return AlarmType.LOW_VALUE_COMPARE;
		} else if (type.equalsIgnoreCase("change_alarm")) {
			return AlarmType.VALUE_CHANGE;
		} else if (type.equalsIgnoreCase("mask_alarm")) {
			return AlarmType.MASK_COMPARE;
		} else if (type.equalsIgnoreCase("digital_alarm")) {
			return AlarmType.DIGITAL_COMPARE;
		} else if (type.equalsIgnoreCase("exclusive_alarm")) {
			return AlarmType.EXCLUSIVE_COMPARE;
		} else if (type.equalsIgnoreCase("inclusive_alarm")) {
			return AlarmType.INCLUSIVE_COMPARE;
		} else if (type.equalsIgnoreCase("delta_alarm")) {
			return AlarmType.VALUE_DELTA;
		} else if (type.equalsIgnoreCase("state_alarm")) {
			return AlarmType.STATE_COMPARE;
		} else if (type.equalsIgnoreCase("compound_alarm")) {
			return AlarmType.COMPOUND;
		} else {
			error("Unrecognized alarm element: " + type);
		}
		return AlarmType.NO_TYPE;
	}

	/**
	 * Parses the hysteresis values from the given XML attributes object and
	 * sets the hysteresis alarm parameters in the superclass.
	 * 
	 * 
	 * @param attr
	 *            SAX XML Attributes object
	 */
	private void parseHysteresisValues(final Attributes attr) {
		int in = XmlUtility.getIntFromAttr(attr, "hysteresis_in");
		if (in == 0) {
			in = 1;
		}
		setHysteresisInValue(in);

		int out = XmlUtility.getIntFromAttr(attr, "hysteresis_out");
		if (out == 0) {
			out = 1;
		}
		setHysteresisOutValue(out);
	}

	/**
	 * Processes the end event for a "state" element in a simple state alarm.
	 * Verifies the state is valid for the current channel and adds the state to
	 * the current state list in the super class.
	 * 
	 * @throws SAXException
	 *             if the state value is invalid
	 */
	private void endStateElement() throws SAXException {
		final String ntext = XmlUtility.normalizeWhitespace(text);

		long theStateValue = -1;
		try {
			theStateValue = XmlUtility.getLongFromText(ntext);
		} catch (final NumberFormatException nfE) {
			if (getCurrentChannelDefinition().getLookupTable() == null) {
				error("A state value of " + ntext
						+ " was specified for channel " + getCurrentChannelId()
						+ " but the channel has no associated enumeration");
			}
			theStateValue = this.getEnumValue(null, ntext);
			if (theStateValue == -1) {
                TraceManager.getDefaultTracer()
                        .error("Symbol: " + ntext + " not found as enumeration type value -1 returned.");
				error("state value of "
						+ ntext
						+ " for channel "
						+ this.getCurrentChannelId()
						+ " not found in enumeration "
						+ getCurrentChannelDefinition().getLookupTable()
						.getName());
			}
		}

		addStates(new long[] { theStateValue });

	}

	/**
	 * Ends parsing of a combination source alarm.
	 * 
	 * @param level
	 *            the level to apply to the source alarm
	 */
	private void endSourceProxy(final AlarmLevel level) {

		IAlarmDefinition alarm = null;

		if (getAlarmType().equals(AlarmType.NO_TYPE)) {
			resetAlarmParameters();
			return;
		}

		/*
		 * Manufacture a simple alarm using the parameters already set in the
		 * superclass.
		 */
		alarm = createSimpleAlarm();

		/* Only construct alarm ID for the proxy if
		 * one was not supplied in the XML.
		 */
		if (lastSourceAlarmId == null) {

			/*
			 * The source alarm may already have an ID, but even if it does, we want
			 * to disambiguate it, so attach the parent combo and group Ids to the
			 * existing ID.
			 */
			sb.setLength(0);
			sb.append(currentCombinationAlarm.getAlarmId());
			sb.append("_");
			sb.append(combinationAlarmSourcesStack.peek().getGroupId());
			sb.append("_");
			sb.append(alarm.getAlarmId());
			alarm.setAlarmId(sb.toString());
		}

		/*
		 * Instead of adding this new simple alarm to the dictionary, we wrap a
		 * source proxy object around it and add it to the enclosing
		 * combination.
		 */
		combinationAlarmSourcesStack.peek().addOperand(
				AlarmDefinitionFactory.createCombinationSourceAlarm(
						currentCombinationAlarm, alarm));

		lastSourceAlarmId = null;

		resetAlarmParameters();
	}

}
