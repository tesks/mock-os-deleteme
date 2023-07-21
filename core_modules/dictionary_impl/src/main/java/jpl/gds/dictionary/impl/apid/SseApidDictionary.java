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
package jpl.gds.dictionary.impl.apid;

import jpl.gds.dictionary.api.apid.ApidContentType;
import jpl.gds.dictionary.api.apid.ApidDefinitionFactory;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.shared.gdr.GDR;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Arrays;

/**
 * SseApidDictionary implements the IApidDictionary interface for the
 * JPL SSE. It parses and stores the information in a generic APID
 * definition file (APID.xml), which contains the definitions for all possible
 * packet types. <br>
 * This class should not be instantiated directly. The ApidDictionaryFactory
 * class should be used for getting the IApidDictionary object tailored to the
 * current mission.
 * 
 *
 * @see IApidDictionary
 * @see ApidDictionaryFactory
 * 
 */
public class SseApidDictionary extends AbstractApidDictionary implements
IApidDictionary {
	/**
	 * The APID definition currently being parsed.
	 */
	private IApidDefinition apidDef;
	/**
	 * The APID number of the APID definition currently being parsed.
	 */
	private int apidNumber = 0;

	/* Add members to ensure the XML is to the proper schema. */
	private static String ROOT_ELEMENT_NAME = "ApidDictionary";
	private static String HEADER_ELEMENT_NAME = "header";	
	
	/**
	 * Package protected constructor.
	 * 
	 */
	SseApidDictionary() {
	    super(UNKNOWN);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.apid.AbstractApidDictionary#clear()
	 */
	@Override
	public void clear() {
		super.clear();
		apidDef = null;
		apidNumber = 0;
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
		setRequiredElements("SSE", Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
	}

	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName,
			final String qname, final Attributes attr) throws SAXException {
		super.startElement(uri, localName, qname, attr);

		try {

			if (qname.equals("version")) {
				setGdsVersionId(attr.getValue("ver_id"));
				
			} else if (qname.equals("apid")) {
				apidDef = ApidDefinitionFactory.createApid();
				try {
					/*  Keep from blowing up when number attribute is null. */
					String numStr = attr.getValue("number");
					if (numStr == null) {
						error("The 'apid' element is lacking required attribute 'number'");
					}
					apidNumber = GDR.parse_int(numStr);
				} catch (NumberFormatException e) {
					error("Apid number must be an integer, not "
							+ attr.getValue("number"));
				}
				apidDef.setNumber(apidNumber);
			}
		} catch (SAXParseException e) {
			error(e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName,
			final String qname) throws SAXException {
		super.endElement(uri, localName, qname);
		String ntext = text.toString().trim();

		try {

		    if (qname.equalsIgnoreCase("version")) {
		        if (!ntext.isEmpty()) {
		            setBuildVersionId(ntext);
		        }
		        reportHeaderInfo();
		        
		    } else if (qname.equalsIgnoreCase("project")) {
		        if (!getTrimmedText().isEmpty()) {
	                setMission(getTrimmedText());
	            }
		        
		    } else if (qname.equals("name") || qname.equals("type")) {
				apidDef.setName(ntext);

			} else if ((qname.equals("autonomously_built")
					|| qname.equals("streaming")) && apidDef.getContentType().equals(ApidContentType.DATA_PRODUCT)) {
				try {
					apidDef.setCommandedProduct(!GDR.parse_boolean(ntext));
				} catch (NumberFormatException e) {
					error(
							"autonomously_built/streaming flag does not have boolean value: "
									+ text);
				}
			} else if (qname.equals("format")) {
				String tmp = new String(text);
				try {
					ApidContentType type = ApidContentType.UNKNOWN;
					if (tmp.equalsIgnoreCase("eha")) {
						type = ApidContentType.PRE_CHANNELIZED;
					} else if (tmp.equalsIgnoreCase("evr")) {
						type = ApidContentType.EVR;
					} else if (tmp.equalsIgnoreCase("product")) {
						type = ApidContentType.DATA_PRODUCT;
					} else if (tmp.equalsIgnoreCase("decom")) {
						type = ApidContentType.DECOM_FROM_MAP;
					} else if (tmp.equalsIgnoreCase("user-defined")) {
						type = ApidContentType.OTHER;
					} else if (tmp.equalsIgnoreCase("time-correlation")) {
						type = ApidContentType.TIME_CORRELATION;
					} else if (tmp.equalsIgnoreCase("CFDP_DATA")) {
	                    type = ApidContentType.CFDP_DATA;
	                } else if (tmp.equalsIgnoreCase("CFDP_PROTOCOL")) {
						type = ApidContentType.CFDP_PROTOCOL;
					}
					apidDef.setContentType(type);

				} catch (IllegalArgumentException e) {
					error("format string not valid: " + tmp);
				}
			} else if (qname.equals("apid")) {
				apids.put(apidNumber, apidDef);
				apidDef = null;

			} else if (qname.equals("desc")) {
				apidDef.setDescription(text.toString());
			} else if (qname.equals("title")) {
				apidDef.setTitle(text.toString());
			}
		} catch (SAXParseException e) {
			error(e);
		}

	}

}
