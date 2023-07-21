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
package jpl.gds.dictionary.impl.evr;

import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is the EVR dictionary parser used for the JPL SSE EVR Dictionaries.
 * 
 *
 * Refactored some of this parser owing to
 *          refactoring of the Reference and Abstract classes it depends upon.
 *          Changes too numerous to mark individually. The most relevant changes
 *          were to the constructor for the ReferenceEvrArgumentEntry class and
 *          to change the ReferenceEnumeratedValue class to
 *          EnumerationDefinition instead. Also, some code was moved into the
 *          base parsing class and most members in the base class were made
 *          private, so accessors must now be used.
 *
 */
public class SseEvrDictionary extends AbstractEvrDictionary
{

	private String currentTableName;
	private EnumerationDefinition currentTable;
	private String currentLookupKey;
	private StringBuffer currentLookupValue;

	/* Add members to ensure the XML is to the proper schema. */
	private static String ROOT_ELEMENT_NAME = "evrs";
	private static String HEADER_ELEMENT_NAME = "header";	

	/**
	 * Basic constructor.
	 * 
	 */
	SseEvrDictionary() {
		super(UNKNOWN);
        addSpacecraftId(UNKNOWN_SCID);

		this.currentTableName = null;
		this.currentTable = null;
		this.currentLookupKey = null;
		this.currentLookupValue = null;
	}


	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		/* Set starting required elements */
		setRequiredElements("SSE", Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.evr.AbstractEvrDictionary#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String namespaceURI, final String localname,
			final String rawName, final Attributes atts)
					throws SAXException
					{
		super.startElement(namespaceURI, localname, rawName, atts);

		try {
			debugTracer.trace("EVR dictionary parser: start NS: " + namespaceURI + " ln " + localname
					+ " raw " + rawName);

			/*  Parse dictionary version */
			if (localname.equals("version")) {
				setGdsVersionId(atts.getValue("ver_id"));

			} else if (localname.equals("evr")) {
			    currentEvrDefinition = new SseEvrDefinition();
				
				/* Parse EVR ID as UNSIGNED. */
				this.currentEvrDefinition.setId(GDR.parse_unsigned(atts.getValue("id").trim()));
				this.currentEvrDefinition.setLevel(atts.getValue("level").trim());
				String name = atts.getValue("name");
				if(name != null)
				{
					name = name.trim();
					this.currentEvrDefinition.setName(name);
				}
				String module = atts.getValue("module");
				if (module != null) {
					module = module.trim();
					this.currentEvrDefinition.setCategory(IEvrDefinition.MODULE, module);
				}
				debugTracer.trace("EVR parser: Entry: " + this.currentEvrDefinition);
			} else if (localname.equals("printfmt")) {
				startFormatString();
			} else if (localname.equals("replace")) {
				/* . Throw if we find a parameter replacement in the dictionary */
				error("The 'replace' element is not supported by this channel dictionary parser"); 
			} else if (localname.equals("table")) {
				this.currentTableName = atts.getValue("name");
				if (this.currentTableName != null) {
					this.currentTableName = this.currentTableName.trim();
					this.currentTable = new EnumerationDefinition(this.currentTableName);
					this.addEnumTypedef(this.currentTable);
				}
			} else if (localname.equals("lookup")) {
				this.currentLookupKey = atts.getValue("id");
				if ((this.currentLookupKey == null) || (this.currentLookupKey.equals(""))) {
					tracer.warn("Lookup table id is empty or missing in EVR dictionary");
				} else {
					this.currentLookupKey = this.currentLookupKey.trim();
					this.currentLookupValue = new StringBuffer();
				}
			}
		} catch (final SAXParseException e) {
			error(e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.evr.AbstractEvrDictionary#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String namespaceURI, final String localname, final String rawName)
			throws SAXException {

		debugTracer.trace("Evr dictionary parser: End NS" + namespaceURI + " ln " + localname + " raw " + rawName);
		
		/*  Changes to version parsing and reporting. Add project/mission parsing */
		if (localname.equalsIgnoreCase("version")) {
		    if (!getTrimmedText().isEmpty()) {
                setBuildVersionId(getTrimmedText());
            }
            reportHeaderInfo();
		} else if (localname.equalsIgnoreCase("project")) {
		    if (!getTrimmedText().isEmpty()) {
		        setMission(getTrimmedText());
		    }
		    
		} else if (localname.equals("evr")) {
			debugTracer.trace("Evr dictionary parser: Dict " + this.currentEvrDefinition.getLevel() + " "
					+ this.currentEvrDefinition.getId() + " fmt " + this.currentEvrDefinition.getFormatString());

			/*  Remove code to populate replacements. */

			// add entry
			addEvr(this.currentEvrDefinition);
		} else if (localname.equals("printfmt")) {
			endFormatString();
		} else if ((this.currentLookupKey != null) && (this.currentTable != null) && localname.equals("lookup")) {
			try {
				final long id = XmlUtility.getLongFromText ( this.currentLookupKey.trim());
				this.currentTable.addValue(Long.valueOf(id), this.currentLookupValue.toString());
			} catch (final NumberFormatException e) {
				tracer.warn("Non-integer lookup key '" + this.currentLookupKey
						+ "' in EVR dictionary");
			}
			this.currentLookupKey = null;
			this.currentLookupValue = null;
		} else if (localname.equals("table")) {
			this.currentTable = null;
		}

	}

}
