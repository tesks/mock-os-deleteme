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

import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.dictionary.api.DecomHandler.DecomHandlerType;
import jpl.gds.dictionary.api.apid.ApidContentType;
import jpl.gds.dictionary.api.apid.ApidDefinitionFactory;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.SecondaryHeaderType;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.xml.XmlUtility;

/**
 * APID Definition Dictionary parser for the multimission APID dictionary schema.
 * 
 * @since AMPCS R5
 * 
 *
 */
public class MultimissionApidDictionary extends AbstractApidDictionary {
    
    /*  Get schema version from configuration */
    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.APID);

	private IApidDefinition apidDef;
	private int apidNumber = 0;
	private DecomHandler handler;

	/*  Add members to ensure the XML is to the proper schema. */
	private static String ROOT_ELEMENT_NAME = "apid_dictionary";
	private static String HEADER_ELEMENT_NAME = "header";	
	private String attrKey;
	
	/**
	 * Package protected constructor.
	 * 
	 */
	MultimissionApidDictionary() {
	    super(MM_SCHEMA_VERSION);
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
		setRequiredElements("Multimission", Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
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
		    
		    if (qname.equalsIgnoreCase("apid_definition")) {

				String tmp = getRequiredAttribute("apid", qname, attr);
				try {
					apidNumber = GDR.parse_int(tmp);
					if (apidNumber == 2047) {
						tracer.warn("APID dictionary is redefining reserved APID number 2047");
					}
				} catch (final NumberFormatException e) {
					error(String.format("APID must be a number, not '%s'",
							tmp));
				}

				apidDef = ApidDefinitionFactory.createApid();
				apidDef.setNumber(apidNumber);

				try {
					/*
					 * Use valueOf to get the ApidContentType
					 * enum value. That enum now matches the MM schema.
					 */
					final String formatStr = getRequiredAttribute("format", qname, attr);
					final ApidContentType format = Enum.valueOf(ApidContentType.class, formatStr);
					apidDef.setContentType(format);
				} catch (final IllegalArgumentException e) {
					error(e.getMessage());
				}

				tmp = attr.getValue("recorded");
				if (tmp != null) {
					apidDef.setRecorded(Boolean.valueOf(tmp)); // might be too
					// lenient
				}
			} else if (qname.equalsIgnoreCase("viewer")) {
				this.handler = new DecomHandler();
				final String name = attr.getValue("name");
				if (name == null) {
					error("viewer element is missing 'name' attribute");
				}
				handler.setHandlerName(name);
				handler.setType(DecomHandlerType.EXTERNAL_PROGRAM);
				handler.setWait(false);
				this.apidDef.setProductHandler(handler);
            } else if (qname.equalsIgnoreCase("category")) { /* Parse old category structure */
                final String catName = getRequiredAttribute("name", qname, attr);
                final String catValue = getRequiredAttribute("value", qname, attr);
                if (catName != null && catValue != null) {
                    if (catName.equalsIgnoreCase("ops category")) {
                        apidDef.setCategory(IApidDefinition.OPS_CAT, catValue);                       
                    } else {
                        apidDef.setCategory(catName, catValue);
                    }
                }               
			} else if (qname.equalsIgnoreCase("keyword_value")) {
				attrKey = getRequiredAttribute("key", qname, attr);
            } else if (qname.equalsIgnoreCase("secondary_header")) {
            	final String typeStr = attr.getValue("type");
            	if (typeStr != null) {
            		try {
            			apidDef.setSecondaryHeaderType(Enum.valueOf(SecondaryHeaderType.class, attr.getValue("type")));
            		} catch (final IllegalArgumentException e) {
            			error(e.getMessage());
            		}
            	}
            }

			// Ignore change log
			// Ignore attributes-def
		} catch (final SAXParseException x) {
			error(x);
		}
	}

	/**
	 * @{inheritDoc}
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName,
			final String qname) throws SAXException {
		super.endElement(uri, localName, qname);

		final String ntext = XmlUtility.normalizeWhitespace(text);

		try {
			if (qname.equalsIgnoreCase("apid_definition")) {
				if (apidDef.getName() == null) {
					error("APID definition missing name element");
				}
				if (apidDef.getTitle() == null) {
					error("APID definition missing title element");
				}
				// Check that name, title are not null
				apids.put(apidDef.getNumber(), apidDef);
				apidDef = null;
			} else if (qname.equals("isCommandedProduct")) {
				try {
					apidDef.setCommandedProduct(GDR.parse_boolean(ntext));
				} catch (final NumberFormatException e) {
					error(
							"isCommandedProduct flag does not have boolean value: "
									+ text);
				} 
			} else if (qname.equalsIgnoreCase("name")) {
				apidDef.setName(ntext);
			} else if (qname.equalsIgnoreCase("title")) {
				apidDef.setTitle(ntext);
			} else if (qname.equalsIgnoreCase("description")) {
				apidDef.setDescription(ntext);
            } else if (qname.equalsIgnoreCase("viewer")) {
				if (!ntext.isEmpty()) {
					handler.setHandlerName(handler.getHandlerName() + " " + ntext); 
				}
				handler = null;
			} else if (qname.equalsIgnoreCase("keyword_value")) {
				final String attrValue = text.toString();
				if (attrKey != null && attrValue != null) {
					apidDef.setKeyValueAttribute(attrKey, attrValue);
				}
			} else if (qname.equals("secondary_header")) {
				apidDef.setSecondaryHeaderExtractor(ntext);
			}
		} catch (final SAXParseException e) {
			error(e);
		}
	}

}
