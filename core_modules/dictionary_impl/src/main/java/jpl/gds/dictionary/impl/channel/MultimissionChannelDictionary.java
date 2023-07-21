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
package jpl.gds.dictionary.impl.channel;

import java.util.Arrays;

import jpl.gds.dictionary.api.ICategorySupport;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.xml.XmlUtility;

/**
 * Channel dictionary parser for the multimission channel schema.
 * 
 * @since AMPCS R5
 * 
 */
public class MultimissionChannelDictionary extends AbstractChannelDictionary {
    
    /* Get schema version from config */
    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.CHANNEL);

	private static final String NAME = "name";

	private boolean inEuConversion;
	private String currentArgName;
	private String attrKey;
	/* Add fields for handling flight vs sse channel identification.
	 * TODO: This is TEMPORARY pending the V2.0 schema changes going into effect.
	 */
	private boolean fswChannelSeen = false;
	private boolean sseChannelSeen = false;

	/* Add members to ensure the XML is to the proper schema. */
	private static String ROOT_ELEMENT_NAME = "telemetry_dictionary";
	private static String HEADER_ELEMENT_NAME = "header";	
	
	/**
	 * Package protected constructor.
	 * 
	 */
	MultimissionChannelDictionary() {
	    super(MM_SCHEMA_VERSION);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.channel.AbstractChannelDictionary#clear()
	 */
	@Override
	public void clear() {
		super.clear();
		currentArgName = null;
		inEuConversion = false;
		/*  Add fields for handling flight vs sse channel identification.
		 * TODO: This is TEMPORARY pending the V2.0 schema changes going into effect.
		 */
		fswChannelSeen = false;
		sseChannelSeen = false;
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

	/**
	 * Parse an integer value and restrict it to the given range
	 * 
	 * @param val string value to be parsed
	 * @param ename containing element name (for error message only)
	 * @param min
	 * @param max
	 * @return
	 * @throws SAXParseException
	 */
	private int parseInteger(final String vals, final String ename, final int min, final int max)
			throws SAXParseException {
		int value = 0;
		try {
			value = Integer.valueOf(vals);
			if (value < min || value > max) {
				error(String.format(
						"value of attribute %s (%d) not in the range %d - %d",
						ename, value, min, max));
			}
		} catch (final NumberFormatException x) {
			error(String.format("value of attribute %s (%s) is not an integer",
					ename, vals));
		}
		return value;
	}

	/**
	 * Parses a floating point value and throws SAX exception if not a float
	 * @param vals value to parse
	 * @param ename name of attribute being parsed
	 * @return float value
	 * @throws SAXParseException if value is not float
	 */
	private float parseFloat(final String vals, final String ename)
			throws SAXParseException {
		float value = 0f;
		try {
			value = Float.valueOf(vals);
		} catch (final NumberFormatException x) {
			error(String.format("value of attribute %s (%s) is not a float",
					ename, vals));
		}
		return value;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.AbstractBaseDictionary#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName,
			final String qname, final Attributes attr) throws SAXException {

		super.startElement(uri, localName, qname, attr);

		try {

		    /*  Use common method to parse and report header info */
		    parseMultimissionHeader(localName, attr);
		    
            if (qname.equalsIgnoreCase("telemetry")) {

				final String ch_id = getRequiredAttribute("abbreviation", qname, attr);


				final String name = getRequiredAttribute(NAME, qname, attr);
				final ChannelType ct = mapTypeStringToChannelType(getRequiredAttribute(
						"type", qname, attr));
				final int size = parseInteger(
						getRequiredAttribute("byte_length", qname, attr),
						"byte_length", 0, 255);
				final String group = attr.getValue("group_id");
				/*
				 * Use channel source to decide what
				 * type of channel to create. 
				 * 
				 * Schema change has been effected that makes
				 * "source" required and removes the old source options for derived
				 * channels. But the code that accepts the old "derived" and "bit_extract" 
				 * source values will be left for backward compatibility to old XML files.
				 */
				final String source = getRequiredAttribute("source", qname, attr);

				/* Added handling of monitor channels below. */
				if (source.equalsIgnoreCase("flight")) {
					startFlightChannelDefinition(ch_id);
					fswChannelSeen = true;
				} else if (source.equalsIgnoreCase("simulation")) {
					startSseChannelDefinition(ch_id);
					sseChannelSeen = true;
				} else if (source.equalsIgnoreCase("derived")
						|| source.equalsIgnoreCase("bit_extract")) {
					if (fswChannelSeen) {
						startFlightChannelDefinition(ch_id);
					} else if (sseChannelSeen) {
						startSseChannelDefinition(ch_id);
					} else {
						error(
								"Unable to determine whether channel "
										+ ch_id
										+ " is FSW or SSE. First channel in the dictionary must be non-derived");
					}
				} else if (source.equalsIgnoreCase("monitor")) {
					startMonitorChannelDefinition(ch_id, ct);
				} else if (source.equalsIgnoreCase("header")) {
                    startHeaderChannelDefinition(ch_id, ct);
                }else {
					error("Unknown value for channel + " + ch_id + " 'source' attribute: " + source);
				}

				currentChannel.setSize(size * 8); // bits per byte
				currentChannel.setChannelType(ct);
				currentChannel.setTitle(name);
				currentChannel.setName(name);
                currentChannel.setGroupId(group);


				// Note - this is the legacy default format for float channels.
				// This avoids having
				// to specify it on every float channel, and will be overridden
				// by later code if
				// the XML specifically sets it to something else
				if (ct.equals(ChannelType.FLOAT)) {
					currentChannel.setDnFormat("%16.9e");
				}

				// Modified size check to warn if there is a
				// 0 length for anything but string channels. Previous code was allowing it to
				// be 0 on derived channels, which was just not right.
				if (size == 0 && !ct.isStringType()) {
					tracer.warn("byte_length of zero for non-string channel "
							+ ch_id);
				}

				// This was checking size on integer channels
				// only. Should apply to float channels also.
				if (ct.isNumberType() && size > 64) {
					tracer.warn("numeric channel type with byte_length > 64 bits "
							+ ch_id);
				}

			} else if (qname.equalsIgnoreCase("boolean_format")) {
				if (!ChannelType.BOOLEAN
						.equals(currentChannel.getChannelType())) {
					error("boolean_format in non-boolean telemetry definition");
				}
				final String ts = getRequiredAttribute("true_str", qname, attr);
				final String fs = getRequiredAttribute("false_str", qname, attr);
				final EnumerationDefinition enumDef = new EnumerationDefinition(
						"Boolean");

				enumDef.addValue(1, ts);
				enumDef.addValue(0, fs);
				currentChannel.setLookupTable(enumDef);

			} else if (qname.equalsIgnoreCase("enum_format")) {
				if (!ChannelType.STATUS.equals(currentChannel.getChannelType())) {
					error("enum_format in non-enum telemetry definition");
				}
				final String ename = getRequiredAttribute("enum_name", qname, attr);
				final EnumerationDefinition edef = getEnumDefinition(ename);
				if (edef == null) {
					error(String.format(
							"referencing missing enum definition %s", ename));
				}
				currentChannel.setLookupTable(edef);

			} else if (qname.equalsIgnoreCase("simple_algorithm")) {
				// This just declares that an externally defined algorithm
				// exists. We can't
				// really verify it's existence here.
				final String name = getRequiredAttribute(NAME, qname, attr);

				startDnToEu();
				setAlgorithmName(name);

			} else if (qname.equalsIgnoreCase("bit_extract")) {
				currentChannel.setDerived(true);
				currentChannel.setDerivationType(DerivationType.BIT_UNPACK);

			} else if (qname.equalsIgnoreCase("category")) { // Parse old-style categories
				final String catname = attr.getValue(NAME);
				/* Added handling of ops cat and module.
				 * 
				 * Set subsystem instead of module and ops cat here.
				 * Set module to match subsystem only if null. This is to avoid breaking current
				 * capabilities.
				 * 
				 *
				 */
				final String cat = getRequiredAttribute("value", qname, attr);
				
				if ("subsystem".equalsIgnoreCase(catname)) {
					currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, cat);
					if (currentChannel.getCategory(IChannelDefinition.MODULE) == null) {
						currentChannel.setCategory(IChannelDefinition.MODULE, cat);
					}
					
				} else if ("ops_category".equalsIgnoreCase(catname) || catname.equalsIgnoreCase(IChannelDefinition.OPS_CAT)) {
					// schema supports underscore, NOT space, but parse old style for backwards compat
					currentChannel.setCategory(IChannelDefinition.OPS_CAT, cat);
					
				}
				else if ("module".equalsIgnoreCase(catname)) {
					currentChannel.setCategory(IChannelDefinition.MODULE, cat);
				}
				
			} else if (qname.equalsIgnoreCase("keyword_value")) {
				attrKey = attr.getValue("key");
				
			} else if (qname.equals("raw_to_eng") && inChannelDefinition()) {

				// Default EU format - may be overridden by later XML
				currentChannel.setEuFormat("%16.9e");
				this.inEuConversion = true;// needed to determine application of

			} else if (qname.equalsIgnoreCase("enum_table")) {
				final String name = getRequiredAttribute(NAME, qname, attr);
				startEnumDefinition(name);

			} else if (qname.equalsIgnoreCase("enum") && inEnumDefinition()) {
				final String symbol = getRequiredAttribute("symbol", qname, attr);
				final int index = parseInteger(
						getRequiredAttribute("numeric", qname, attr),
						"numeric", Integer.MIN_VALUE, Integer.MAX_VALUE);
				currentEnumDef.addValue(index, symbol);

			} else if (qname.equalsIgnoreCase("derivation")) {
				final String id = getRequiredAttribute("derivation_id", qname, attr);
				startAlgorithmicDerivation(id);

			} else if (qname.equalsIgnoreCase("parameterized_algorithm")
					&& inAlgorithmDerivation()) {
				final String name = getRequiredAttribute(NAME, qname, attr);
				setAlgorithmName(name);

			} else if (qname.equalsIgnoreCase("parameterized_algorithm") && this.inEuConversion) {
				/*  Add parameterized algorithm parsing for EUs */
				final String name = getRequiredAttribute(NAME, qname, attr);
				startDnToEu();
				setAlgorithmName(name);

			} else if (qname.equalsIgnoreCase("polynomial_expansion")
					&& inChannelDefinition()) {
				startDnToEu();

			} else if (qname.equalsIgnoreCase("parameter") && (inAlgorithmDerivation() || inDnToEu())) {
				/*  Now parse parameters for both derivations and EUs. Also,
				 * the "name" attribute never was required, but this code made it so previously. */
				currentArgName = attr.getValue(NAME);

			} else if (qname.equalsIgnoreCase("factor")
					&& inChannelDefinition()) {
				final float coef = parseFloat(
						getRequiredAttribute("coeff", qname, attr), qname);
				final int index = parseInteger(
						getRequiredAttribute("index", qname, attr), "index", 0,
						7);
				setDnToEuPolyIndex(index);
				setDnToEuPolyCoefficient(coef);

			} else if (qname.equalsIgnoreCase("table_lookup")
					&& inChannelDefinition()) {
				startDnToEu();

			} else if (qname.equalsIgnoreCase("point") && inDnToEu()) {
				final float raw = Float.valueOf(getRequiredAttribute("raw", qname,
						attr));
				final float eng = Float.valueOf(getRequiredAttribute("eng", qname,
						attr));
				setTableDn(raw);
				setTableEu(eng);

			} else if (qname.equalsIgnoreCase("bits")
					&& isInBitUnpackDerivation()) {
				int start = parseInteger(
						getRequiredAttribute("offset", qname, attr), qname, 0,
						63);
				final int len = parseInteger(
						getRequiredAttribute("length", qname, attr), qname, 1,
						63);
				if (start + len > 63) {
					error("bit extraction extent exceeds 64 bit word length");
				}
				/**
				 *  Reversed the order of the
				 * following two checks. Must check bit extract length before
				 * adjustment for endianness.
				 */
				if (len > currentChannel.getSize()) {
					tracer.warn("Extraction length exceeds channel length for channel " + this.currentChannel.getId());
				}
				if ("msb_is_zero".equalsIgnoreCase(attr.getValue("endianness"))) {
					// PER MARTI: adjust start
					start = currentChannel.getSize() - start - 1;
				}

				addBitUnpackRange(start, len);
			}

		} catch (final SAXParseException x) {
			error(x);
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

        super.endElement(uri, localName, qname);

		final String ntext = XmlUtility.normalizeWhitespace(text);

		try {
			if (this.inChannelDefinition() && (qname.equalsIgnoreCase("telemetry"))) {
				endChannelDefinition();
			} else if (inChannelDefinition()
					&& qname.equalsIgnoreCase("measurement_id")) {
				currentChannel.setIndex(parseInteger(ntext, "measurement_id", 0,
						65535));
			}

			else if (qname.equalsIgnoreCase("derivation")) {
				checkAlgorithmRequirements();
				// end a bit extraction algorithm definition
				endAlgorithmicDerivation();
			} else if (qname.equals("raw_to_eng")) {
				this.inEuConversion = false;
			} else if (qname.equalsIgnoreCase("raw_units")) {
				if (!ntext.isEmpty()) {
					currentChannel.setDnUnits(ntext);
				} 
			} else if (qname.equalsIgnoreCase("description")) {
				currentChannel.setDescription(ntext);
			} else if (qname.equalsIgnoreCase("module") || 
			        qname.equalsIgnoreCase("subsystem") || 
			        qname.equalsIgnoreCase(ICategorySupport.OPS_CAT))  { // Parse new-style categories, old 'ops category'
			    currentChannel.setCategory(qname, ntext);
			} else if (qname.equalsIgnoreCase("ops_category")) { // parse new 'ops_category'
				currentChannel.setCategory(ICategorySupport.OPS_CAT, ntext);
			} else if (qname.equalsIgnoreCase("keyword_value")) {
			    final String attrValue = text.toString();
			    if (attrKey != null && attrValue != null) {
			        currentChannel.setKeyValueAttribute(attrKey, attrValue);
			    }
			} else if (qname.equalsIgnoreCase("format")) {
				if (this.inEuConversion) {
					currentChannel.setEuFormat(ntext);
				} else {
					currentChannel.setDnFormat(ntext);
				}
			} else if (qname.equalsIgnoreCase("numeric_format")) {
				currentChannel.setDnFormat(ntext); // assuming we are in enum or
				// bool
			} else if (qname.equalsIgnoreCase("string_format")) {
				currentChannel.setEuFormat(ntext);
			} else if (inChannelDefinition() && qname.equalsIgnoreCase("title")) {
				currentChannel.setTitle(ntext);
			} else if (inDnToEu() && qname.equalsIgnoreCase("polynomial_expansion")) {
				endDnToEuPoly();
			} else if (qname.equalsIgnoreCase("table_lookup")) {
				endDnToEuTable();
			} else if (this.inEuConversion && qname.equalsIgnoreCase("eng_units")) {
				currentChannel.setEuUnits(ntext);
			} else if (inAlgorithmDerivation() && qname.equalsIgnoreCase("parent_id")) {
				addDerivationParent(ntext);
			} else if (inAlgorithmDerivation()
					&& qname.equalsIgnoreCase("trigger_id")) {
				this.setAlgorithmTriggerId(ntext);
			}

			// Try to do it Marti's way
			else if (inChannelDefinition()
					&& qname.equalsIgnoreCase("source_derivation_id")) {
				// SHOULD WE VALIDATE THAT source is derived?
				// ntext names a derivation id that this is a child of
				// Need a new base method that can lookup the named derivation id
				// and add the channel-id
				// of the channel we're currently parsing (currentChannel.getId())
				// as a child

				/*
				 * for apps that don't need derivation algorithms,
				 * we don't need to worry about the parent-child mapping 
				 */
				addDerivationChild(ntext, currentChannel.getId());
				currentChannel.setDerived(true);
				currentChannel.setDerivationType(DerivationType.ALGORITHMIC);
				currentChannel.setSourceDerivationId(ntext);
			}
			// We do not parse child_derivation_ids because this is redundant with
			// the parent_id elememnt
			// in the derivation definition

			else if (inChannelDefinition() && qname.equalsIgnoreCase("parent_id")) {
				startBitUnpackDerivation(ntext);
				addDerivationChild(currentChannel.getId());
			} else if (isInBitUnpackDerivation()
					&& qname.equalsIgnoreCase("bit_extract")) {
				endBitUnpackDerivation();
			} else if (qname.equalsIgnoreCase("enum_table")) {
				endEnumDefinition();
			} else if (qname.equalsIgnoreCase("dn_eu_table")) {
				endDnToEuTable();

			} else if (qname.equalsIgnoreCase("simple_algorithm")) {
				endDnToEuAlgorithmic();

			}	else if (qname.equalsIgnoreCase("parameterized_algorithm") && inDnToEu()) {	
				endDnToEuParameterizedAlgorithmic();

			} else if (qname.equalsIgnoreCase("parameter")) {
				/* Now parse parameters for both derivations and EUs */
				addNamedParameter(currentArgName, text.toString());
				currentArgName = null;
			}
		} catch (final SAXParseException e) {
			error(e);
		}
	}

	// channel-type-def = "integer" | "unsigned" | "float" | "enum" | "string" |
	// "time" | "boolean"
	private ChannelType mapTypeStringToChannelType(String type)
			throws SAXParseException {
		type = type.toLowerCase().trim();
		/* Use TIME instead of UNSIGNED_INT for "time" types below */
		if ("integer".equals(type)) {
			return ChannelType.SIGNED_INT;
		} else if ("unsigned".equals(type)) {
			return ChannelType.UNSIGNED_INT;
		} else if ("time".equals(type)) {
			return ChannelType.TIME;
		} else if ("float".equals(type)) {
			return ChannelType.FLOAT;
		} else if ("enum".equals(type)) {
			return ChannelType.STATUS;
		} else if ("string".equals(type)) {
			return ChannelType.ASCII;
		} else if ("boolean".equals(type)) {
			return ChannelType.BOOLEAN;
		}

		error("Unrecognized channel data type: " + type);
		// NOTREACHED -- but parser doesn't recognize that error always throws
		return null;
	}

}
