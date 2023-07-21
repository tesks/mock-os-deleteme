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
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.shared.xml.XmlUtility;

/**
 * SseChannelDictionary implements the IChannelDictionary interface for the JPL SSE,
 * using version 1 of the MSAP channel schema. It parses the information in an 
 * SSE channel definition file (channel.xml).
 * <br>
 * This class should not be instantiated directly. The ChannelDictionaryFactory class should
 * be used for getting the IChannelDictionary object tailored to the current mission.
 *
 *
 * @see IChannelDictionary
 * @see ChannelDictionaryFactory
 * 
 */
public class SseChannelDictionary extends AbstractChannelDictionary {

	private static String ROOT_ELEMENT_NAME = "Channel-Dictionary";
	private static String HEADER_ELEMENT_NAME = "header";	
	
	/**
	 * Package protected constructor.
	 * 
	 */
	SseChannelDictionary() {
	    super(UNKNOWN);
	}


	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		/* Set starting required elements */
		setRequiredElements("SSE", Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.channel.AbstractChannelDictionary#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName, final String qname,
			final Attributes attr) throws SAXException {
		super.startElement(uri, localName, qname, attr);

		try {
			if (qname.equalsIgnoreCase("version")) {
				setGdsVersionId(attr.getValue("ver_id"));
				
			} else if (qname.equalsIgnoreCase("channel")) {
				String id = attr.getValue("id");
				if (id == null) {
					error("channel element is missing 'id' attribute");
				}
				int size = 0;
				try {
					size = XmlUtility.getIntFromAttr(attr, "size");
				} catch (NumberFormatException e) {
					error("channel size attribute must be an integer");
				}
				String ctype = attr.getValue("type");
				if (ctype == null) {
					error("channel type attribute must be supplied");
				}

				String subs = attr.getValue("subsystem");
				int idx = 0;

				try {
					idx = XmlUtility.getIntFromAttr(attr, "idx");
				} catch (NumberFormatException e) {
					error("channel index attribute must be an integer");
				}

				/*  Use SSE specific method to create the channel definition. */
				startSseChannelDefinition(id, Enum.valueOf(ChannelType.class,ctype));
               if (subs != null) {
                    currentChannel.setCategory(IChannelDefinition.SUBSYSTEM, subs);
                }
				currentChannel.setSize(size);
				currentChannel.setIndex(idx);
			} else if (qname.equalsIgnoreCase("states")) {
				startStates();
			} else if (qname.equalsIgnoreCase("enum") && inStates()) {
				int dn = 0;
				try {
					dn = XmlUtility.getIntFromAttr(attr, "dn");
				} catch (NumberFormatException e) {
					error("dn value in state enumeration must be an integer");
				}
				setCurrentEnumIndex(dn);
			} else if (qname.equalsIgnoreCase("table")) {
				startDnToEu();
			} else if (qname.equalsIgnoreCase("row") && this.inDnToEu()) {
				try {
					double dn = XmlUtility.getDoubleFromAttr(attr, "dn");
					setTableDn(dn);
				} catch (NumberFormatException e) {
					error("dn value in dn to eu table must be a floating point or integer number");
				}
			} else if (qname.equalsIgnoreCase("poly")) {
				startDnToEu();
			}

			else if (qname.equalsIgnoreCase("coeff") && inDnToEu()) {
				try {
					int offset = XmlUtility.getIntFromAttr(attr, "index");
					setDnToEuPolyIndex(offset);
				} catch (NumberFormatException e) {
					error("poly coeff index must be an integer");
				}
			}
		} catch (SAXParseException e) {
			error(e);
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName, final String qname)
			throws SAXException {
		super.endElement(uri, localName, qname);

		String ntext = XmlUtility.normalizeWhitespace(text);

		try {
			/* Remove check for schema version. The SSE schema does not include
			 * this element.
			 */

		    /* Changes to version parsing. Added project/mission parsing and
		     * header reporting.
		     */
		    if (qname.equalsIgnoreCase("version")) {
		        if (!ntext.isEmpty()) {
                    setBuildVersionId(ntext);
                }
                reportHeaderInfo();
		        
		    } else if (qname.equalsIgnoreCase("project")) {
		        if (!getTrimmedText().isEmpty()) {
	                setMission(getTrimmedText());
	            }
		        
		    } else if (qname.equalsIgnoreCase("channel")) {
				endChannelDefinition();
			} else if (qname.equalsIgnoreCase("enum") && inStates()) {
				setCurrentEnumValue(ntext);
			} else if (qname.equalsIgnoreCase("states")) {
				/** 
				 *  Give the channel enumeration a unique name, or
				 * we will get only one enumeration in the map after the parser finishes. Since
				 * the SSE schema provides no name, we make one up.
				 */
				currentEnumDef.setName(currentChannel.getId() + "_enum");
				endStates();
			} else if (qname.equalsIgnoreCase("table") && this.inDnToEu()) {
				endDnToEuTable();	
			} else if (qname.equals("row") && inDnToEu()) {
				try {
					double eu = XmlUtility.getDoubleFromText(ntext);
					setTableEu(eu);
				} catch (NumberFormatException e) {
					error("dn value in dn to eu table must be a floating point or integer number");
				}
			} else if (qname.equals("coeff") && inDnToEu()) {
				try {
					double val = XmlUtility.getDoubleFromText(ntext);
					setDnToEuPolyCoefficient(val);
				} catch (NumberFormatException e) {
					error("value of coeff element must be an integer");
				}
			} else if (qname.equalsIgnoreCase("poly")) {
				endDnToEuPoly();
			} 

			else if (qname.equalsIgnoreCase("module")) {
			    currentChannel.setCategory(IChannelDefinition.MODULE, ntext);
			} else if (qname.equalsIgnoreCase("gds_title")) {
				currentChannel.setTitle(ntext);
			} else if (qname.equalsIgnoreCase("gds_name")) {
				/*
				* Set as title also if no title.
				* Must also check for empty title as well as null.
				*/
				currentChannel.setName(ntext);
				if (currentChannel.getTitle() == null || currentChannel.getTitle().isEmpty()) {
					currentChannel.setTitle(ntext);
				}
			} else if (qname.equalsIgnoreCase("eng_units")) {
				currentChannel.setEuUnits(ntext);
			} else if (qname.equalsIgnoreCase("dn_units")) {
				currentChannel.setDnUnits(ntext);
			} else if (qname.equals("dn_format")) {
				currentChannel.setDnFormat(ntext);
			} else if (qname.equalsIgnoreCase("eu_format")) {
				currentChannel.setEuFormat(ntext);
			} else if (qname.equalsIgnoreCase("description")
					|| qname.equalsIgnoreCase("channel_desc")
					|| qname.equalsIgnoreCase("fswdesc")) {
				currentChannel.setDescription(ntext);
			} 

		} catch (SAXParseException e) {
			error(e);
		}
	}
}
