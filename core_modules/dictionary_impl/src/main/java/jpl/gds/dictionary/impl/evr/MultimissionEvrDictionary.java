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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.evr.EvrArgumentFactory;
import jpl.gds.dictionary.api.evr.EvrArgumentType;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.xml.XmlUtility;

/**
 * 
 * This is the Multimission EVR dictionary parser. It parses XML EVR
 * dictionaries corresponding to the multimission EVR schema (currently under
 * core/schema/dictionary).
 * 
 * This parser will reject any XML with schema version less than
 * MINIMUM_SCHEMA_VERSION. If a new version of the schema is produced and this
 * parser must be changed to parse it such that the old XML cannot be parsed any
 * more, the MINIMUM_SCHEMA_VERSION should be updated.
 * 
 *
 */
public class MultimissionEvrDictionary extends AbstractEvrDictionary {

    /*  Get schema version from config */
    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.EVR);

    private static final String MODULE_ELEM = "module";
	private static final String OPSCAT_ELEM = "ops_category";
	private static final String SUB_ELEM = "subsystem";

	private static final String NAME = "name";

	private final List<IEvrArgumentDefinition> args;
	private String typedefName;
	private int argNum = 0;
	private String attrKey;

	/* Add members to ensure the XML is to the proper schema. */
	private static String ROOT_ELEMENT_NAME = "evr_dictionary";
	private static String HEADER_ELEMENT_NAME = MM_HEADER_ELEMENT_NAME;

	// flags set to true when parsing these elements
	private boolean inModule;
	private boolean inOpsCat;
	private boolean inSubsystem;

	private final StringBuilder module = new StringBuilder();
	private final StringBuilder opsCat = new StringBuilder();
	private final StringBuilder subsystem = new StringBuilder();

	/**
	 * Basic constructor.
	 * 
	 */
	MultimissionEvrDictionary() {

		super(MM_SCHEMA_VERSION);

		this.args = new ArrayList<IEvrArgumentDefinition>(16);
		this.typedefName = null;
	}


	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		/* Set starting required elements */
		setRequiredElements("Multimission", Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
	}

	@Override
	public void startElement(final String namespaceURI, final String localname,
			final String rawName, final Attributes atts) throws SAXException {

		super.startElement(namespaceURI, localname, rawName, atts);

		debugTracer.trace("EVR dictionary parser: start NS: " + namespaceURI
				+ " ln " + localname + " raw " + rawName);

		try {
		    
		    /* Use common method to parse and report version info */
		    parseMultimissionHeader(localname, atts);
			    
			if (localname.equals("evr")) {

				// Start a new EVR definition

			    String source = atts.getValue("source");
			    if (source == null) {
			        source = "flight"; /*  Handle old dictionary without source attribute */
			    }
			    currentEvrDefinition = new MultimissionEvrDefinition();			        

				/*  Parse EVR ID as UNSIGNED. */
				
				this.currentEvrDefinition.setId(GDR.parse_unsigned(atts.getValue("id")));
				this.currentEvrDefinition.setLevel(atts.getValue("level"));
				final String name = atts.getValue(NAME);
				if (name != null) {
					this.currentEvrDefinition.setName(name);
				}
				this.args.clear();
				this.argNum = 0;
				debugTracer.trace("EVR parser: Entry: " + this.currentEvrDefinition);

			}
			// Support old style category element
			else if (localname.equals("category")) {

				// Establish the module/subsystem/ops category of the current EVR
				// Variable names changed to match those in similar Channel logic -- braun

				final String catname = atts.getValue(NAME);
				final String cat = atts.getValue("value");

				if (catname.equals("module")) {
					this.currentEvrDefinition.setCategory(IEvrDefinition.MODULE, cat);

				} else if (catname.equals("subsystem")) {
					this.currentEvrDefinition.setCategory(IEvrDefinition.SUBSYSTEM, cat);
					//backward compatibility
					if (currentEvrDefinition.getCategory(IEvrDefinition.SUBSYSTEM)==null) {
						this.currentEvrDefinition.setCategory(IEvrDefinition.SUBSYSTEM, cat);
					}

				} else if (catname.equals("ops category")) {
					this.currentEvrDefinition.setCategory(IEvrDefinition.OPS_CAT, cat);
				}

			}
			// New style module / ops_category / subsystem elements
			else if (localname.equals(MODULE_ELEM)) {
				inModule = true;
			}
			else if (localname.equals(SUB_ELEM)) {
				inSubsystem = true;
			}
			else if (localname.equals(OPSCAT_ELEM)) {
				inOpsCat = true;
			}
			else if (localname.equals("keyword_value")) {
				// Start parsing the key-value attributes for the current EVR
				attrKey = atts.getValue("key");

			} else if (localname.equals("format_message")) {

				// Start parsing the message string for the current EVR

				startFormatString();

			} else if (localname.equals("string_arg")) {

				// Add an argument to the current EVR

				try {
					addStringArgument(atts);
				} catch (final IllegalArgumentException e) {
					error(e.getMessage());
				}

			} else if ((localname.equals("numeric_arg")) ||  
			    (localname.equals("integer_arg")) || 
			    (localname.equals("unsigned_arg")) || 
			    (localname.equals("float_arg"))) {

				// Add an argument to the current EVR

				try {
					addNumericArgument(atts, localname);
				} catch (final IllegalArgumentException e) {
					error(e.getMessage());
				}

			} else if (localname.equals("enum_arg")) {

				// Add an argument to the current EVR

				try {
					addEnumArgument(atts);
				} catch (final IllegalArgumentException e) {
					error(e.getMessage());
				}


			} else if (localname.equals("opcode_arg")) {

				// Add an argument to the current EVR

				try {
					addOpcodeArgument(atts);
				} catch (final IllegalArgumentException e) {
					error(e.getMessage());
				}


			} else if (localname.equals("seqid_arg")) {

				// Add an argument to the current EVR

				try {
					addSeqIdArgument(atts);
				} catch (final IllegalArgumentException e) {
					error(e.getMessage());
				}


			} else if (localname.equals("enum_table")) {

				// Start a new enumeration table

				this.typedefName = atts.getValue(NAME);
				EnumerationDefinition values = getEnumTypedef(this.typedefName);
				if (values == null) {
					values = new EnumerationDefinition(this.typedefName);
					addEnumTypedef(values);
				}

			} else if (localname.equals("enum")) {

				// Add a value to the current enumeration table.

				final String symbol = atts.getValue("symbol");
				final long numeric = XmlUtility.getLongFromAttr(atts, "numeric");
				final EnumerationDefinition values = getEnumTypedef(this.typedefName);
				if (values != null) {
					values.addValue(numeric, symbol);
				} else {
					error("Encountered 'enum' tag not inside an 'enum_table' tag");
				}
			}
		} catch (final SAXParseException e) {
			error(e);
		}
	}

	@Override
	public void characters(final char[] ch, final int start, final int end) throws SAXException {
		super.characters(ch, start, end);

		if(inModule){
			module.append(ch, start, end);
		}
		if(inOpsCat){
			opsCat.append(ch, start, end);
		}
		if(inSubsystem){
			subsystem.append(ch, start, end);
		}
	}

	@Override
	public void clear() {
		super.clear();
		inModule = false;
		module.setLength(0);
		inOpsCat = false;
		opsCat.setLength(0);
		inSubsystem = false;
		subsystem.setLength(0);
	}

	@Override
	public void endElement(final String namespaceURI, final String localname,
			final String rawName) throws SAXException {
        super.endElement(namespaceURI, localname, rawName);
		debugTracer.trace("Evr dictionary parser: End NS" + namespaceURI
				+ " ln " + localname + " raw " + rawName);

		if (localname.equals("evr")) {

			// Finish off the current EVR definition.

			debugTracer.trace("Evr dictionary parser: Dict "
					+ this.currentEvrDefinition.getLevel() + " "
					+ this.currentEvrDefinition.getId() + " fmt "
					+ this.currentEvrDefinition.getFormatString());

			// populate arguments
			/*  Changes to account for use of argument list vs array below. */
			final int argumentCount = args.size();
			if (argumentCount > 0) {
				currentEvrDefinition.setArgs(new ArrayList<IEvrArgumentDefinition>(args));
			}

			final int nargs = currentEvrDefinition.getNargs();

			if (argumentCount == 0 && nargs != 0) {
				tracer.error("No argument elements in EVR entry ID: " +
						currentEvrDefinition.getId());
			}
			else if (argumentCount != nargs)
			{
				tracer.error("Mismatch between nargs (" + nargs +
						") and number of argument elements (" + argumentCount +
						") in EVR entry ID: " + currentEvrDefinition.getId());
			}

			// add entry to the list of EVRs we have accumulated
			addEvr(this.currentEvrDefinition);

		} else if (localname.equals("number_of_arguments")) {

			// Set the number of EVR arguments.

			this.currentEvrDefinition.setNargs(XmlUtility
					.getIntFromText(getTrimmedText()));

		} else if (localname.equals("format_message")) {

			// Finish off parsing of the message string for the current EVR.

			endFormatString();

		} else if (localname.equals("enum_table")) {

			// Finish off parsing of the current enumeration table.

			this.typedefName = null;
		} else if (localname.equals("keyword_value")) {
			final String attrValue = this.getTrimmedText();
			if (attrKey != null && attrValue != null) {
				this.currentEvrDefinition.setKeyValueAttribute(attrKey, attrValue);
			}
		}
		else if (localname.equals(MODULE_ELEM)) {
			currentEvrDefinition.setCategory(IEvrDefinition.MODULE, module.toString());
			inModule = false;
			module.setLength(0);

		}
		else if (localname.equals(OPSCAT_ELEM)) {
			currentEvrDefinition.setCategory(IEvrDefinition.OPS_CAT, opsCat.toString());
			inOpsCat = false;
			opsCat.setLength(0);

		}
		else if (localname.equals(SUB_ELEM)) {
			currentEvrDefinition.setCategory(IEvrDefinition.SUBSYSTEM, subsystem.toString());
			inSubsystem = false;
			subsystem.setLength(0);
		}
	}

	/**
	 * Parses a new EVR argument.
	 * 
	 * @param atts
	 *            the XML attributes from the EVR "arg" element in the XML.
	 * 
	 * @throws SAXException
	 *             if there is a problem with the parsing
	 */
	private void addStringArgument(final Attributes atts) {

		// Note: Argument name is not required by the schema.
		String name = atts.getValue(NAME);
		if (null != name) {
			name = name.trim();
		}

		// Now add the argument to the list we are accumulating for the current EVR
		// Use EvrArgumentFactory.
		this.args.add(EvrArgumentFactory.createArgumentDefinition(this.argNum++, name, EvrArgumentType.VAR_STRING, 0, null));
	}

	/**
	 * Parses a new EVR argument.
	 * 
	 * @param atts
	 *            the XML attributes from the EVR "arg" element in the XML.
	 * 
	 * @throws SAXParseException
	 *             if there is a problem with the parsing
	 */
	private void addNumericArgument(final Attributes atts, final String localname) throws SAXParseException {

		// Note: Argument name is not required by the schema.
		String name = atts.getValue(NAME);
		if (null != name) {
			name = name.trim();
		}

		// Argument length
		String lenString = atts.getValue("byte_length");
		if (lenString == null) {
			tracer.warn("EVR dictionary has numeric argument (" + name
					+ ") without byte_length=\"...\" attribute");
			return;
		}
		int len = 0;
		try {
			lenString = lenString.trim();
			len = Integer.parseInt(lenString);
		} catch (final NumberFormatException e) {
			tracer.warn("EVR dictionary has numeric argument (" + name
					+ ") with non-integer byte_length: " + lenString);
			return;
		}

		// Argument data type
		EvrArgumentType argType = null;
		if (localname.equals("numeric_arg")) { /* old style, do not use */

		    final String type = atts.getValue("type");
		    if (null != type) {
		        argType = mapToArgType(type.trim(), len);
		    } else {
		        tracer.warn("EVR dictionary has numeric argument (" + name
		                + ") without type=\"...\" attribute");
		        return;
		    }

		    // Now add the argument to the list we are accumulating for the current EVR. Use EvrArgumentFactory. */
		    this.args.add(EvrArgumentFactory.createArgumentDefinition(this.argNum++, name, argType, len, null));

		} else {
	        final String[] numTypes = localname.split("_");
	        final String numType = numTypes[0].trim();
		    argType = mapToArgType(numType.trim(), len);
            this.args.add(EvrArgumentFactory.createArgumentDefinition(this.argNum++, name, argType, len, null));            		    
		}
	}

	/**
	 * Parses a new EVR argument.
	 * 
	 * @param atts
	 *            the XML attributes from the EVR "arg" element in the XML.
	 * 
	 * @throws SAXParseException
	 *             if there is a problem with the parsing
	 */
	private void addEnumArgument(final Attributes atts) throws SAXParseException {

		// Note: Argument name is not required by the schema.
		String name = atts.getValue(NAME);
		if (null != name) {
			name = name.trim();
		}

		// Argument length
		String lenString = atts.getValue("byte_length");
		if (lenString == null) {
			tracer.warn("EVR dictionary has enum argument (" + name
					+ ") without byte_length=\"...\" attribute");
			return;
		}
		int len = 0;
		try {
			lenString = lenString.trim();
			len = Integer.parseInt(lenString);
		} catch (final NumberFormatException e) {
			tracer.warn("EVR dictionary has argument (" + name
					+ ") with non-integer byte_length: " + lenString);
			return;
		}


		// Argument enumeration table name
		final String enumString = atts.getValue("enum_name");
		EnumerationDefinition enumValues = null;
		if (enumString != null) {
			enumValues = getEnumTypedef(enumString);
			if (enumValues == null) {
				tracer.warn("EVR dictionary has enum argument (" + name
						+ ") with non-defined enum type (" + enumString + ")");
				return;
			}
		} else {
			error("EVR dictionary has enum argument (" + name
					+ ")  without enum_name attribute");
		}

		// Now add the argument to the list we are accumulating for the current
		// EVR. Use EvrArgumentFactory.
		this.args.add(EvrArgumentFactory.createArgumentDefinition(this.argNum++, name, EvrArgumentType.ENUM, len,
				enumValues));
	}

	/**
	 * Parses a new EVR argument.
	 * 
	 * @param atts
	 *            the XML attributes from the EVR "arg" element in the XML.
	 * 
	 */
	private void addOpcodeArgument(final Attributes atts) {

		// Note: Argument name is not required by the schema.
		String name = atts.getValue(NAME);
		if (null != name) {
			name = name.trim();
		}

		// Argument length
		String lenString = atts.getValue("byte_length");
		if (lenString == null) {
			tracer.warn("EVR dictionary has opcode argument (" + name
					+ ") without byte_length=\"...\" attribute");
			return;
		}
		int len = 0;
		try {
			lenString = lenString.trim();
			len = Integer.parseInt(lenString);
		} catch (final NumberFormatException e) {
			tracer.warn("EVR dictionary has opcode argument (" + name
					+ ") with non-integer byte_length: " + lenString);
			return;
		}

		/* Modified argument length check to allow
		 * all lengths of integer.
		 */
		if (len != 1 && len != 2 && len != 4 && len != 8) {
			tracer.warn("EVR dictionary has opcode argument with byte length not equal to 1, 2, 4, or 8");
			return;
		}


		// Now add the argument to the list we are accumulating for the current
		// EVR Use EvrArgumentFactory.
		this.args.add(EvrArgumentFactory.createArgumentDefinition(this.argNum++, name, EvrArgumentType.OPCODE, len,
				null));
	}

	/**
	 * Parses a new EVR argument.
	 * 
	 * @param atts
	 *            the XML attributes from the EVR "arg" element in the XML.
	 * 
	 */
	private void addSeqIdArgument(final Attributes atts) {

		// Note: Argument name is not required by the schema.
		String name = atts.getValue(NAME);
		if (null != name) {
			name = name.trim();
		}

		// Argument length
		String lenString = atts.getValue("byte_length");
		if (lenString == null) {
			tracer.warn("EVR dictionary has seqid argument (" + name
					+ ") without byte_length=\"...\" attribute");
			return;
		}
		int len = 0;
		try {
			lenString = lenString.trim();
			len = Integer.parseInt(lenString);
		} catch (final NumberFormatException e) {
			tracer.warn("EVR dictionary has seqid argument (" + name
					+ ") with non-integer byte_length: " + lenString);
			return;
		}

		if (len != 2 && len != 4) {
			tracer.warn("EVR dictionary has seqid argument with byte length not equal to 2 or 4");
			return;
		}

		// Now add the argument to the list we are accumulating for the current
		// EVR Use EvrArgumentFactory.
		this.args.add(EvrArgumentFactory.createArgumentDefinition(this.argNum++, name, EvrArgumentType.SEQID, len,
				null));
	}

	/**
	 * Map argument data type and length from the XML to the argument type
	 * enumeration value.
	 * 
	 * @param type
	 *            argument data type parsed from the XML
	 * @param len
	 *            argument length parsed from the XML
	 * @return ArgumentType enum value
	 * 
	 * @throws SAXException
	 *             if there is a problem with the understanding of the incoming
	 *             data type
	 */
	private EvrArgumentType mapToArgType(final String type,
			final int len) throws SAXParseException {

		if (type.equals("integer")) {
			switch (len) {
			case 1:
				return EvrArgumentType.I8;
			case 2:
				return EvrArgumentType.I16;
			case 4:
				return EvrArgumentType.I32;
			case 8:
				return EvrArgumentType.I64;
			default:
				error("Invalid argument data type/length combination ("
						+ type + "/" + len + ")");
			}
		} else if (type.equals("unsigned")) {
			switch (len) {
			case 1:
				return EvrArgumentType.U8;
			case 2:
				return EvrArgumentType.U16;
			case 4:
				return EvrArgumentType.U32;
			case 8:
				return EvrArgumentType.U64;
			default:
				error(
						"Invalid argument data type/length combination ("
								+ type + "/" + len + ")");
			}
		} else if (type.equals("float")) {
			switch (len) {
			case 4:
				return EvrArgumentType.F32;
			case 8:
				return EvrArgumentType.F64;
			default:
				error("Invalid argument data type/length combination ("
						+ type + "/" + len + ")");
			}
		} 
		error("Unrecognized numeric EVR argument type " + type);
		return null;
	}
}
