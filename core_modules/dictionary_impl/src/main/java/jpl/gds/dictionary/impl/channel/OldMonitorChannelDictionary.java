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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.xml.XmlUtility;

/**
 * MonitorChannelDictionary implements the IChannelDictionary interface for the multi-mission
 * ground generated DSN monitor channels. It parses the information in a monitor channel 
 * definition file (monitor_channel.xml).
 * <br>
 *
 * @deprecated NO NEW MISSIONS SHOULD USE THIS PARSER. Use the multimission channel parser.
 * 
 *
 * @see IChannelDictionary
 *
 */
public class OldMonitorChannelDictionary extends AbstractChannelDictionary {
    
    /* Added supported schema version */
    private static final String IMPLEMENTED_SCHEMA_VERSION = "1.2";

	private String currentArgName = null;

	/*  Add members to ensure the XML is to the proper schema. */
	private static String ROOT_ELEMENT_NAME = "MonitorChannelDictionary";
	private static String HEADER_ELEMENT_NAME = "header";	
	
	/**
	 * Package protected constructor.
	 * 
	 */
	OldMonitorChannelDictionary() {
	    super(DictionaryType.MONITOR, IMPLEMENTED_SCHEMA_VERSION);
	}

	/**
	 * @see jpl.gds.dictionary.impl.impl.impl.channel.AbstractChannelDictionary#clear()
	 */
	@Override
	public void clear() {
		super.clear();
		currentArgName = null;
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		/* Set starting required elements */
		setRequiredElements("Monitor", Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
	}

	/**
	 * @see jpl.gds.dictionary.impl.impl.impl.channel.AbstractChannelDictionary#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName, final String qname,
			final Attributes attr) throws SAXException {
		super.startElement(uri, localName, qname, attr);

		try {
		    
		    /* Use common method to parse and report header info */
		    parseMultimissionHeader(localName, attr);
		    
			if (qname.equalsIgnoreCase("monitor_channel")) {
		        String id = attr.getValue("channel_id");
		        if (id == null) {
		            error("monitor_channel element is missing channel_id attribute");
		        }

				int index = 0;
				try {
					index = XmlUtility.getIntFromAttr(attr, "measurement_id");
				} catch (NumberFormatException e) {
					error("monitor_channel measurement_id attribute must be an integer");
				}

				String opsCat = attr.getValue("ops_cat");
				String subs = attr.getValue("subsystem");
				String name = attr.getValue("channel_name");
				String ctype = attr.getValue("type");

				if (ctype == null) {
					error("monitor_channel type attribute must be supplied");
				}

				String units = attr.getValue("units");
				if (units != null && units.equalsIgnoreCase("UNDEFINED")) {
					units = null;
				}

				String format = attr.getValue("io_format");

				startMonitorChannelDefinition(id, Enum.valueOf(ChannelType.class,ctype));
                /* New call to categories. */
 	            if (opsCat != null) {
	                currentChannel.setCategory(IChannelDefinition.OPS_CAT, opsCat);
	            }
	            if (subs != null) {
	                currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, subs);
	            }
				currentChannel.setIndex(index);
				currentChannel.setTitle(name);
				/* Name is required for multimission conversion. */
				currentChannel.setName(name);
				currentChannel.setDnUnits(units);
				currentChannel.setDnFormat(format);

			} else if (qname.equalsIgnoreCase("states")) {
				startStates();
				/*  Enums must be named for multimission conversion. */
				currentEnumDef.setName("Enum_" + currentChannel.getId());

			} else if (qname.equalsIgnoreCase("enum") && inStates()) {
				int dn = 0;
				try {
					dn = XmlUtility.getIntFromAttr(attr, "dn");
				} catch (NumberFormatException e) {
					error("dn value in state enumeration must be an integer");
				}
				setCurrentEnumIndex(dn);

			} else if (qname.equalsIgnoreCase("monitor_derivation_by_bit_unpacking")) {
				String id = attr.getValue("channel_id");
				if (id == null) {
					error("monitor_derivation_by_java element is missing channel_id attribute");
				}

				int index = 0;
				try {
					index = XmlUtility.getIntFromAttr(attr, "measurement_id");
				} catch (NumberFormatException e) {
					error("monitor_derivation_by_java measurement_id attribute must be an integer");
				}

				String opsCat = attr.getValue("ops_cat");
				String subs = attr.getValue("subsystem");
				String name = attr.getValue("channel_name");
				String ctype = attr.getValue("type");

				if (ctype == null) {
					error("monitor_channel type attribute must be supplied");
				}

				String units = attr.getValue("units");
				if (units != null && units.equalsIgnoreCase("UNDEFINED")) {
					units = null;
				}

				String format = attr.getValue("io_format");

				startMonitorChannelDefinition(id, Enum.valueOf(ChannelType.class,ctype));
	            if (opsCat != null) {
	                currentChannel.setCategory(IChannelDefinition.OPS_CAT, opsCat);
	            }
	            if (subs != null) {
	                currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, subs);
	            }
				currentChannel.setIndex(index);
				currentChannel.setTitle(name);
				/*  Name is required for multimission conversion. */
				currentChannel.setName(name);
				currentChannel.setDnUnits(units);
				currentChannel.setDnFormat(format);

				currentChannel.setDerived(true);
				currentChannel.setDerivationType(DerivationType.BIT_UNPACK);

			} else if (qname.equalsIgnoreCase("monitor_derivation_by_java")) {
				String id = attr.getValue("channel_id");
				if (id == null) {
					error("monitor_derivation_by_java element is missing channel_id attribute");
				}

				int index = 0;
				try {
					index = XmlUtility.getIntFromAttr(attr, "measurement_id");
				} catch (NumberFormatException e) {
					error("monitor_derivation_by_java measurement_id attribute must be an integer");
				}

				String opsCat = attr.getValue("ops_cat");
				String subs = attr.getValue("subsystem");
				String name = attr.getValue("channel_name");
				String ctype = attr.getValue("type");

				if (ctype == null) {
					error("monitor_channel type attribute must be supplied");
				}

				String units = attr.getValue("units");
				if (units != null && units.equalsIgnoreCase("UNDEFINED")) {
					units = null;
				}

				String format = attr.getValue("io_format");

				startMonitorChannelDefinition(id, Enum.valueOf(ChannelType.class,ctype));
                /* New call to categories. */
	            if (opsCat != null) {
	                currentChannel.setCategory(IChannelDefinition.OPS_CAT, opsCat);
	            }
	            if (subs != null) {
	                currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, subs);
	            }
				currentChannel.setIndex(index);
				currentChannel.setTitle(name);
				/* Name is required for multimission conversion. */
				currentChannel.setName(name);
				currentChannel.setDnUnits(units);
				currentChannel.setDnFormat(format);

				currentChannel.setDerived(true);
				currentChannel.setDerivationType(DerivationType.ALGORITHMIC);

			} else if (qname.equalsIgnoreCase("dn_to_eu")) {
				currentChannel.setEuUnits(attr.getValue("eu_units"));
				if (currentChannel.getEuUnits() != null && currentChannel.getEuUnits().equals("")) {
					currentChannel.setEuUnits(null);
				}
				currentChannel.setEuFormat(attr.getValue("eu_io_format"));
			}
			else if (qname.equalsIgnoreCase("dn_eu_table")) {
				startDnToEu();
				/*
				 * Removed setting of superfluous doDnToEu
				 * attribute on channel definitions.
				 */
			}
			else if (qname.equalsIgnoreCase("val") && this.inDnToEu()) {
				try {
					double dn = XmlUtility.getDoubleFromAttr(attr, "dn");
					setTableDn(dn);
				} catch (NumberFormatException e) {
					error("dn value in dn to eu table must be an integer or floating point number");
				}
				try {
					double eu = XmlUtility.getDoubleFromAttr(attr, "eu");
					setTableEu(eu);
				} catch (NumberFormatException e) {
					error("eu value in dn to eu table must be an integer or floating point number");
				}
			}

			else if (qname.equalsIgnoreCase("dn_eu_poly")) {
				startDnToEu();
				/*
				 * Removed setting of superfluous doDnToEu
				 * attribute on channel definitions.
				 */
			} 
			else if (qname.equalsIgnoreCase("dn_eu_java")) {
				startDnToEu();
				/*
				 * Removed setting of superfluous doDnToEu
				 * attribute on channel definitions.
				 */
				String name = attr.getValue("java_class");
				if (name == null) {
					error("dn_eu_java must have a java_class attribute");
				}
				setAlgorithmName(name);
			}

			else if (qname.equalsIgnoreCase("coeff") && inDnToEu()) {
				try {
					int offset = XmlUtility.getIntFromAttr(attr, "index");
					setDnToEuPolyIndex(offset);
				} catch (NumberFormatException e) {
					error("poly index attribute must be an integer");
				}
				try {
					double val = XmlUtility.getDoubleFromAttr(attr, "val");
					setDnToEuPolyCoefficient(val);
				} catch (NumberFormatException e) {
					error("poly val attribute must be a floating point number");
				}
			}
			else if (qname.equalsIgnoreCase("bits")) {
				int start = 0;
				try {
					if (attr.getValue("start_bit") == null) {
						error("bits element must have a start_bit attribute");
					}
					start = XmlUtility.getIntFromAttr(attr, "start_bit");
				} catch (NumberFormatException e) {
					error("start_bits attribute must be an integer");
				}
				int len = 0;
				try {
					if (attr.getValue("num_bits") == null) {
						error("bits element must have a num_bits attribute");
					}
					len = XmlUtility.getIntFromAttr(attr, "num_bits");
				} catch (NumberFormatException e) {
					error("start_bits attribute must be an integer");
				}
				addBitUnpackRange(start, len);
			}

			else if (qname.equalsIgnoreCase("derivation_bit")) {
				String parent = attr.getValue("parent");
				if (parent == null) {
					error("derivation_bit element must have a parent attribute");
				}
				if (!parent.equals("NONE")) {
					startBitUnpackDerivation(parent);
					addDerivationChild(currentChannel.getId().toString());
				}
			}

			else if (qname.equalsIgnoreCase("derivation_java"))
			{
				final String id = attr.getValue("derivation_id");

				if (id == null)
				{
					error("derivation_java element must have a " +
							"derivation_id attribute");
				}

				final String javaClass = attr.getValue("java_class");

				if (javaClass == null)
				{
					error("derivation_java element must have a " +
							"java_class attribute");
				}

				startAlgorithmicDerivation(id);
				setAlgorithmName(javaClass);
				addDerivationChild(currentChannel.getId());
				/* Source derivation ID is required for multimission conversion. */
				currentChannel.setSourceDerivationId(id);
			}
			else if (qname.equalsIgnoreCase("argument") && inAlgorithmDerivation()) {
				final String name = attr.getValue("name");

				if (name == null)
				{
					error("argument element must have a " +
							"name attribute");
				}

				currentArgName = name;
			}
			else if (qname.equalsIgnoreCase("parent") && inAlgorithmDerivation()) {
				String id = attr.getValue("channel_id");
				if (id == null) {
					error("parent element must have channel_id attribute");
				}
				addDerivationParent(id);
			}
		} catch (SAXParseException e) {
			error(e);
		}
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName, final String qName)
			throws SAXException {

		super.endElement(uri, localName, qName);

		String ntext = XmlUtility.normalizeWhitespace(text);

		/**
		 *  Removed dependence on a schema_version element.
		 */

		if (this.inChannelDefinition() &&
				(qName.equalsIgnoreCase("monitor_channel") ||
						qName.equalsIgnoreCase("monitor_derivation_by_java") ||
						qName.equalsIgnoreCase("monitor_derivation_by_bit_unpacking"))) {
			endChannelDefinition();

		} else if (qName.equalsIgnoreCase("enum") && inStates()) {
			setCurrentEnumValue(ntext);
		} else if (qName.equalsIgnoreCase("states")) {
			endStates();

		} else if (qName.equalsIgnoreCase("comment") && inChannelDefinition()) {
			currentChannel.setDescription(ntext);
		}

		else if (qName.equals("dn_eu_table")) {
			this.endDnToEuTable();
		}

		else if (qName.equalsIgnoreCase("dn_eu_poly")) {
			endDnToEuPoly();
		}
		else if (qName.equalsIgnoreCase("dn_eu_java")) {
			endDnToEuAlgorithmic();
		}

		else if (qName.equalsIgnoreCase("derivation_bit")) {
			endBitUnpackDerivation();
		}

		else if (qName.equalsIgnoreCase("derivation_java")) {
			endAlgorithmicDerivation();
		}
		else if("argument".equals(qName) && inAlgorithmDerivation())
		{
			addNamedParameter(currentArgName, text.toString());
			currentArgName = null;
		}
	}
}
