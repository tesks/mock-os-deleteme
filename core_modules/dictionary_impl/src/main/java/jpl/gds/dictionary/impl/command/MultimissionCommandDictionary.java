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
package jpl.gds.dictionary.impl.command;

import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.command.CommandArgumentDefinitionFactory;
import jpl.gds.dictionary.api.command.CommandArgumentType;
import jpl.gds.dictionary.api.command.CommandDefinitionFactory;
import jpl.gds.dictionary.api.command.CommandEnumerationDefinition;
import jpl.gds.dictionary.api.command.CommonValidationRange;
import jpl.gds.dictionary.api.command.ICommandArgumentDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.IRepeatCommandArgumentDefinition;
import jpl.gds.dictionary.api.command.SignedEnumeratedValue;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * Multimission command dictionary parser, corresponding to the MM command 
 * schema. 
 *
 */
public class MultimissionCommandDictionary extends AbstractCommandDictionary {
    /* Get schema version from config */
    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.COMMAND);

    private static final String TYPE_ATTRIBUTE_NAME = "type";

	private final MultimissionCommandDictionaryGenerator generator;
	private CommandEnumerationDefinition currentEnum;

	/*  Add members to ensure the XML is to the proper schema. */
	private static String ROOT_ELEMENT_NAME = "command_dictionary";
	private static String HEADER_ELEMENT_NAME = "header";	
	private String key;

	/**
	 * Constructs a MultimissionCommandDictionary object.
	 *
	 * @throws SAXException It throws this exception if it cannot generate 
	 * all element Mappings.
	 * 
	 */
	MultimissionCommandDictionary() throws SAXException {
		super(MM_SCHEMA_VERSION);
		this.generator = new MultimissionCommandDictionaryGenerator();
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
	 * {@inheritDoc}
	 *
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(final String uri, final String localName,
			final String qname, final Attributes attr) throws SAXException {

		super.startElement(uri, localName, qname, attr);

		try {
		    /* Use common method to parse/report header info */
		    parseMultimissionHeader(localName, attr);
		    
			if (qname.equalsIgnoreCase("file_type")) {
			    generator.startFileType(attr);
			} else if (qname.equalsIgnoreCase("fsw_command")) {
				generator.startSoftwareCommand(attr);
			} else if (qname.equalsIgnoreCase("hw_command")) {
				generator.startHardwareCommand(attr);
			} else if (qname.equalsIgnoreCase("enum_table")) {
				generator.startEnumeration(attr);
			} else if (qname.equalsIgnoreCase("enum")) {
				generator.startEnumeratedValue(attr);
			} else if (qname.equalsIgnoreCase("boolean_arg")) {
				generator.startBooleanArg(attr);
			} else if ((qname.equalsIgnoreCase("numeric_arg")) || 
			        (qname.equalsIgnoreCase("integer_arg")) ||
                    (qname.equalsIgnoreCase("unsigned_arg")) ||
			        (qname.equalsIgnoreCase("float_arg"))) {
				generator.startNumericArg(qname, attr);
            } else if (qname.equalsIgnoreCase("repeat_arg")) {
				generator.startRepeatArg(attr);
			} else if (qname.equalsIgnoreCase("var_string_arg")) {
				generator.startVarStringArg(attr);
			} else if (qname.equalsIgnoreCase("fixed_string_arg")) {
				generator.startFixedStringArg(attr);
			} else if (qname.equalsIgnoreCase("time_arg")) {
				generator.startTimeArg(attr);
			} else if (qname.equalsIgnoreCase("enum_arg")) {
				generator.startEnumeratedArg(attr);
			} else if (qname.equalsIgnoreCase("fill_arg")) {
				generator.startFillArg(attr);
			} else if (qname.equalsIgnoreCase("include")) {
				generator.startRange(attr);
			} else if (qname.equalsIgnoreCase("repeat")) {
				generator.startRange(attr);
			}  else if (qname.equalsIgnoreCase("category")) {
				generator.startCategory(attr);
			} else if (qname.equalsIgnoreCase("boolean_format")) {
				generator.startBooleanFormat(attr);
			} else if (qname.equalsIgnoreCase("keyword_value")) {
				generator.startKeyValue(attr);
			}
		} catch (final SAXParseException e) {
			error(e);
		} 
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName,
			final String qname) throws SAXException {
        super.endElement(uri, localName, qname);
		if (qname.equalsIgnoreCase("fsw_command")) {
			generator.endCommand();
		} else if (qname.equalsIgnoreCase("hw_command")) {
			generator.endCommand();
		} else if (qname.equalsIgnoreCase("enum_table")) {
			generator.endEnumeration();
		} else if (qname.equalsIgnoreCase("repeat_arg")) {
			generator.endRepeatArg();
		} else if (qname.equalsIgnoreCase("description")) {
			generator.endDescription();
		} else if (qname.equalsIgnoreCase("keyword_value")) {
			generator.endKeyValue();
		}else if (qname.equalsIgnoreCase("valid_regex")) {
			generator.endRegex();
		} else if (qname.toLowerCase().endsWith("_arg")) {
			generator.endArg();
		}
	}


	/**
	 * This inner class is responsible for handling the callbacks associated
	 * with all the important elements in the command dictionary (those defined
	 * in the element name to function mappings above). 
	 * 
	 */
	private class MultimissionCommandDictionaryGenerator {

		/**
		 * Constructs this generator object.
		 */
		public MultimissionCommandDictionaryGenerator() {
			SystemUtilities.doNothing();
		}

	     /**
         * Starts parsing uplink file types info.
         * 
         * @param attr SAX Attributes object  
         */
        public void startFileType(final Attributes attr) {
            final String name = attr.getValue("name");
            final int id = Integer.parseInt(attr.getValue("id"));
            if (name != null && id >= 0) {
                setUplinkFileType(name, id);
            }
        }

		/**
		 * Starts parsing a flight software command.
		 * 
		 * @param attr SAX Attributes object
		 * 
		 * @throws SAXException if there is a parsing problem
		 */
        public void startSoftwareCommand(final Attributes attr)
                throws SAXException {
            ICommandDefinition command = null;
            String cmdClass = attr.getValue("class");
            if (cmdClass == null) { /* Old version */
                cmdClass = "FSW";
            }
            if (cmdClass.equalsIgnoreCase("FSW")) {
                command = CommandDefinitionFactory
                        .createFlightSoftwareCommand();	        
            } else if (cmdClass.equalsIgnoreCase("SEQ")) {
                command = CommandDefinitionFactory
                        .createSequenceDirective();         
            }

			MultimissionCommandParserUtil.setCommandValuesFromAttributes(attr, command, getDictionaryConfiguration().getOpcodeBitLength(), 
					MultimissionCommandDictionary.this.opcodeUtil);
			MultimissionCommandDictionary.this.currentDepthStack.push(command);
		}

		/**
		 * Starts parsing a hardware command.
		 * 
		 * @param attr SAX Attributes object
		 * 
		 * @throws SAXException if there is a parsing problem         
		 */
		public void startHardwareCommand(final Attributes attr)
				throws SAXException {
			final ICommandDefinition command = CommandDefinitionFactory
					.createHardwareCommand();

			MultimissionCommandParserUtil.setCommandValuesFromAttributes(attr, command, 
					getDictionaryConfiguration().getOpcodeBitLength(), 
					MultimissionCommandDictionary.this.opcodeUtil);
			MultimissionCommandDictionary.this.currentDepthStack.push(command);
		}

		/**
		 * Starts parsing a numeric argument.
		 * @param qname the name of the element being parsed
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem       
		 */
		public void startNumericArg(final String qname, final Attributes attr) throws SAXException {
		    ICommandArgumentDefinition na = null;

		    if (qname.equalsIgnoreCase("numeric_arg")) {
	            final String type = attr.getValue(TYPE_ATTRIBUTE_NAME);

	            if (type == null) {
	                throw new SAXException("Found numeric_arg element without type attribute");
	            }
		        if (type.equalsIgnoreCase("unsigned")) {
		            na = CommandArgumentDefinitionFactory.create(CommandArgumentType.UNSIGNED);
		        } else if (type.equalsIgnoreCase("integer")) {
		            na = CommandArgumentDefinitionFactory.create(CommandArgumentType.INTEGER);
		        } else if (type.equalsIgnoreCase("float")) {
		            na = CommandArgumentDefinitionFactory.create(CommandArgumentType.FLOAT);
		        } else {
		            throw new SAXException("Found unrecognized type value (" + type  + 
		                    ") for numeric_arg element");
		        }

		    } else if (qname.equalsIgnoreCase("unsigned_arg")) {
		        na = CommandArgumentDefinitionFactory.create(CommandArgumentType.UNSIGNED);
		    } else if (qname.equalsIgnoreCase("integer_arg")) {
		        na = CommandArgumentDefinitionFactory.create(CommandArgumentType.INTEGER);
		    } else if (qname.equalsIgnoreCase("float_arg")) {
		        na = CommandArgumentDefinitionFactory.create(CommandArgumentType.FLOAT);
		    } else {
		        throw new SAXException("Found unrecognized type value (" + qname  + 
		                ") for numeric_arg element");
		    }
		    MultimissionCommandParserUtil.setBasicArgValuesFromAttributes(attr, na, true);
		    MultimissionCommandDictionary.this.currentDepthStack.push(na);
		}

		/**
		 * Starts parsing an enumerated argument.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startEnumeratedArg(final Attributes attr)
				throws SAXException {
			final ICommandArgumentDefinition la = CommandArgumentDefinitionFactory.create(CommandArgumentType.SIGNED_ENUMERATION);
			MultimissionCommandParserUtil.setEnumArgValuesFromAttributes(attr, la, getArgumentEnumerations());
			MultimissionCommandDictionary.this.currentDepthStack.push(la);
		}

		/**
		 * Starts parsing a variable length string argument.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing error
		 */
		public void startVarStringArg(final Attributes attr) throws SAXException {
			final ICommandArgumentDefinition sa = CommandArgumentDefinitionFactory.create(CommandArgumentType.VAR_STRING);
			MultimissionCommandParserUtil.setVarStringArgValuesFromAttributes(attr, sa);
			MultimissionCommandDictionary.this.currentDepthStack.push(sa);
		}

		/**
		 * Starts parsing a fixed length string argument.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing error
		 */
		public void startFixedStringArg(final Attributes attr) throws SAXException {
			final ICommandArgumentDefinition sa = CommandArgumentDefinitionFactory.create(CommandArgumentType.FIXED_STRING);
			MultimissionCommandParserUtil.setBasicArgValuesFromAttributes(attr, sa, true);
			MultimissionCommandDictionary.this.currentDepthStack.push(sa);
		}

		/**
		 * Starts parsing a repeat argument.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startRepeatArg(final Attributes attr) throws SAXException {
			MultimissionCommandDictionary.this.insideRepeat = true;
			final IRepeatCommandArgumentDefinition ra = (IRepeatCommandArgumentDefinition)CommandArgumentDefinitionFactory.create(CommandArgumentType.REPEAT);
			MultimissionCommandParserUtil.setRepeatArgValuesFromAttributes(attr, ra);
			MultimissionCommandDictionary.this.currentDepthStack.push(ra);
		}

		/**
		 * Starts parsing a time argument.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startTimeArg(final Attributes attr) throws SAXException {
			final ICommandArgumentDefinition arg = CommandArgumentDefinitionFactory.create(CommandArgumentType.TIME);
			MultimissionCommandParserUtil.setBasicArgValuesFromAttributes(attr, arg, true);
			MultimissionCommandDictionary.this.currentDepthStack.push(arg);
		}

		/**
		 * Starts parsing a filler argument.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startFillArg(final Attributes attr) throws SAXException {
			final ICommandArgumentDefinition arg = CommandArgumentDefinitionFactory.create(CommandArgumentType.FILL);
			MultimissionCommandParserUtil.setFillerArgValuesFromAttributes(attr, arg);
			MultimissionCommandDictionary.this.currentDepthStack.push(arg);
		}

		/**
		 * Starts parsing a boolean argument.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startBooleanArg(final Attributes attr) throws SAXException {
			final ICommandArgumentDefinition arg = CommandArgumentDefinitionFactory.create(CommandArgumentType.BOOLEAN);
			MultimissionCommandParserUtil.setBasicArgValuesFromAttributes(attr, arg, true);
			MultimissionCommandDictionary.this.currentDepthStack.push(arg);
		}

		/**
		 * Starts parsing category info.
		 * 
		 * @param attr SAX Attributes object 
		 */
		public void startCategory(final Attributes attr) {
			final String name = attr.getValue("name");
			final String value = attr.getValue("value");
			if (name.equalsIgnoreCase("module") && 
					MultimissionCommandDictionary.this.currentDepthStack.peek() instanceof ICommandDefinition) {
				final ICommandDefinition def = (ICommandDefinition)MultimissionCommandDictionary.this.currentDepthStack.peek();
				def.setCategory(ICommandDefinition.MODULE, value);
			}
		}
		
		/**
		 * Starts parsing key from keyword attribute.
		 * 
		 * @param attr SAX Attributes object  
		 */
		public void startKeyValue(final Attributes attr) {
			 key = attr.getValue("key");
		}

		/**
		 * Starts parsing an argument or repeat range.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startRange(final Attributes attr) throws SAXException {
			final CommonValidationRange r = new CommonValidationRange();
			MultimissionCommandParserUtil.setRangeValuesFromAttributes(attr, r);
			((ICommandArgumentDefinition) MultimissionCommandDictionary.this.currentDepthStack
					.peek()).addRange(r);
		}

		/**
		 * Starts parsing an enumeration value.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startEnumeratedValue(final Attributes attr)
				throws SAXException {
			final SignedEnumeratedValue v = new SignedEnumeratedValue();
			MultimissionCommandParserUtil.setEnumValuesFromAttributes(attr, v);
			MultimissionCommandDictionary.this.currentEnum.addEnumerationValue(v);
		}

		/**
		 * Starts parsing an enumeration table.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startEnumeration(final Attributes attr)
				throws SAXException {
			final String name = attr.getValue("name");
			if (name == null) {
				throw new SAXException("Found enum_table element without a name attribute");
			}
			final CommandEnumerationDefinition enumDef = new CommandEnumerationDefinition(name);
			MultimissionCommandDictionary.this.currentEnum = enumDef;
			addEnumeration(enumDef);
		}

		/**
		 * Starts parsing a boolean format element.
		 * 
		 * @param attr SAX Attributes object
		 * @throws SAXException if there is a parsing problem   
		 */
		public void startBooleanFormat(final Attributes attr) throws SAXException {
			if (!((ICommandArgumentDefinition)MultimissionCommandDictionary.this.currentDepthStack.peek()).getType().equals(CommandArgumentType.BOOLEAN)) {
				throw new SAXException("Found boolean_format element not associated with boolean_arg element");
			}
			final ICommandArgumentDefinition arg = (ICommandArgumentDefinition)MultimissionCommandDictionary.this.currentDepthStack.peek();
			MultimissionCommandParserUtil.setBooleanFormatValuesFromAttr(attr, arg);
		}


		/**
		 * Ends parsing of any description element.
		 * 
		 */
		public void endDescription() {
			final Object temp = MultimissionCommandDictionary.this.currentDepthStack.peek();
			if (temp instanceof ICommandDefinition) {
				((ICommandDefinition)temp).setDescription(getTrimmedText());
			} else if (temp instanceof ICommandArgumentDefinition) {
				((ICommandArgumentDefinition)temp).setDescription(getTrimmedText());
			}
		}
		/**
		 * Ends parsing of value of the keyword_value element.
		 * 
		 */
		public void endKeyValue() {
			final String value = text.toString();
			final Object temp = MultimissionCommandDictionary.this.currentDepthStack.peek();
			if (key != null && value != null && 
					temp instanceof ICommandDefinition) {
				((ICommandDefinition)temp).setKeyValueAttribute(key, value);
			} else if (key != null && value != null && 
					temp instanceof ICommandArgumentDefinition) {
				((ICommandArgumentDefinition)temp).setKeyValueAttribute(key, value);
			}			
		}
		
		/**
		 * Ends parsing of a valid_regex element.
		 * @throws SAXException if there is a parsing problem   
		 */
		public void endRegex() throws SAXException {
			final Object temp = MultimissionCommandDictionary.this.currentDepthStack.peek();
			if (temp instanceof ICommandArgumentDefinition && ((ICommandArgumentDefinition)temp).getType().isString()) {
				((ICommandArgumentDefinition)temp).setValueRegexp(text.toString());
			} else {
				throw new SAXException("Found valid_regex element not associated with string argument");
			}
		}

		/**
		 * Ends parsing of any command.
		 * 
		 */
		public void endCommand() {
			final ICommandDefinition mc = (ICommandDefinition) MultimissionCommandDictionary.this.currentDepthStack
					.pop();
			addDefinition(mc);
		}

		/**
		 * Ends parsing of a repeat argument.
		 */
		public void endRepeatArg() {
			MultimissionCommandDictionary.this.insideRepeat = false;
			endArg();
		}

		/**
		 * Ends parsing of a general argument.
		 */
		public void endArg() {
			final ICommandArgumentDefinition ca = (ICommandArgumentDefinition) MultimissionCommandDictionary.this.currentDepthStack
					.pop();
			if (!MultimissionCommandDictionary.this.insideRepeat) {
				((ICommandDefinition) MultimissionCommandDictionary.this.currentDepthStack
						.peek()).addArgument(ca);
			} else {
				((IRepeatCommandArgumentDefinition) MultimissionCommandDictionary.this.currentDepthStack
						.peek()).addDictionaryArgument(ca);
			}
		}

		/**
		 * Ends parsing of an enumeration table.
		 */
		public void endEnumeration() {
			MultimissionCommandDictionary.this.currentEnum = null;
		}

	}

}
