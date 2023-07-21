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
package jpl.gds.dictionary.impl.decom;

import java.nio.ByteOrder;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import jpl.gds.dictionary.api.decom.params.*;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.IDecomStatementFactory;
import jpl.gds.dictionary.api.decom.types.AlgorithmType;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;
import jpl.gds.dictionary.api.decom.types.IGroundVariableDefinition;
import jpl.gds.dictionary.api.decom.types.IMoveStatementDefinition;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition.FloatEncoding;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition.Precision;
import jpl.gds.dictionary.api.decom.types.IMoveStatementDefinition.Direction;
import jpl.gds.dictionary.api.decom.types.IStringDefinition.StringEncoding;
import jpl.gds.dictionary.impl.eu.RawToEngHandler;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.SubHandler;
import jpl.gds.shared.xml.XmlUtility;

/**
 * SAX content handler for decom maps that comply with the latest multimission schema.
 * This class should be used for schemas that are composed of such decom maps.
 * 
 *
 */
public class MultimissionDecomMapParser extends SubHandler {
	
	private static final String BYTE_ORDER_ATTR = "order";
	private static final String BIG_ENDIAN = "big_endian";
	private static final String LITTLE_ENDIAN = "little_endian";
	private static final String ARGS = "args";
	/* Element names */
    private static final String CSTRING = "cstring";
	private static final String STRING = "string";
	private static final String FLOAT = "float";
	private static final String DOUBLE = "double";
	private static final String OPCODE = "opcode";
	private static final String ENUM = "enum";
	private static final String UINT = "uint";
	private static final String INT = "int";
	private static final String BOOLEAN = "bool";
	private static final String DECOM_MAP_REF = "decom_map_ref";
	private static final String ARRAY = "array";
	private static final String TIME_TAG = "time_tag";
	private static final String VALIDATE = "validate";
	private static final String TRANSFORM = "transform";
	private static final String DECOM = "decom";
	private static final String BREAK = "break";
	private static final String REPEAT_BLOCK = "repeat_block";
	private static final String DEFAULT = "default";
	private static final String CASE = "case";
	private static final String SWITCH = "switch";
	private static final String MOVE = "move";
	private static final String DECOM_MAP = "decom_map";
	private static final String DEFINE_BYTE_ORDER = "define_byte_order";
	private static final String RAW_TO_ENG = "raw_to_eng";
	private static final String ENUM_DEFINIITIONS = "enum_definitions";
	private static final String EVENT_RECORD = "event_record";

    /* End element names */

	private final static String MIL_ENCODING = "mil1750a";
	private final static String IEEE_ENCODING = "ieee754";

	private final static int INITIAL_ELEMENT_TEXT_SIZE = 1024;
    private static final Tracer                   trace                     = TraceManager
            .getTracer(Loggers.DICTIONARY);

	private IDecomMapDefinition currentMap;
	
	private final Stack<StatementContainerParams> containerStack = new Stack<>();
    private final Stack<DataStructureParams> structStack = new Stack<>();
    private final Stack<SwitchParams> switchStack = new Stack<>();
	private final IDecomStatementFactory statementFactory;
	
    private final Set<String> declaredVariables;
    
    private Optional<AlgorithmParams> currentAlgo = Optional.empty();
    private boolean inArgs = false;
    private final StringBuilder text = new StringBuilder(INITIAL_ELEMENT_TEXT_SIZE);

    private Map<String, EnumerationDefinition> enumDefs = new HashMap<>();
    
    
    private final XMLReader reader;
    
    private final ContentHandler parent;
    
    /**
     * Each time a RAW_TO_ENG element is encountered, a new handler is created for it;
     * on termination, the instance is cleared
     */
    private Optional<RawToEngHandler> rawToEngHandler; 
    
    private Optional<EnumTableHandler> enumTableHandler;
    
    /**
     * On each number, there may be an enclosed element contain a raw_to_eng definition.
     * Defer creation of corresponding IDecomStatment until the closing tag is encountered.
     */
    private Optional<NumericParams> currentNumericParams;
    
    private final String namespace;
	private Locator locator;
    
    /**
     * Create a decom map SAX parser. The parser will use the XMLReader and ContentHandler
     * passed in to reset the reader's content handler to the parent handler once it encounters the ending
     * element this parser is interested in.
     * @param namespace the namespace to associate with the new map
     * @param mapToParseInto the decom mpa this parser should populate during parsing.
     * @param reader the XML reader driving parsing.
     * @param parent the parent SAX ContentHandler
     */
	public MultimissionDecomMapParser(String namespace, IDecomMapDefinition mapToParseInto, XMLReader reader, ContentHandler parent, boolean callLastEventOnParent) {
		super(DECOM_MAP, reader, parent, callLastEventOnParent);
		this.namespace = namespace;
		this.reader = reader;
		this.parent = parent;
        this.currentMap = mapToParseInto;
        // Initialize declared variables with any previously declared statements
        this.declaredVariables = new HashSet<String>();
        for (IDecomStatement stmt : currentMap.getStatementsToExecute()) {
        	if (stmt instanceof IGroundVariableDefinition) {
        		declaredVariables.add(((IGroundVariableDefinition) stmt).getName());
        	}
        }
        containerStack.push(new StatementContainerParams());
        statementFactory = IDecomStatementFactory.newInstance();

	}
	
	/**
	 * Give the parser a map to store parsed statements into.
	 * @param mapToParseInto decom map to populate
	 */
	public void setMap(final IDecomMapDefinition mapToParseInto) {
		this.currentMap = mapToParseInto;
	}
	
	private static class EnumTableHandler extends DefaultHandler {
		private final Optional<XMLReader> reader;
		private final Optional<DefaultHandler> parent;

		private Locator locator;
		
		Map<String, EnumerationDefinition> enumTable = new HashMap<>();
		
		EnumerationDefinition currentEnumDef;

		public EnumTableHandler(final XMLReader reader, final DefaultHandler parent) {
			this.reader = Optional.ofNullable(reader);
			this.parent = Optional.ofNullable(parent);
		}

		@Override
		public void startElement(final String namespaceUri, final String localName, final String qName, final Attributes attrs) throws SAXException {

			if(qName.equalsIgnoreCase("enum_table")) {
				final String name = attrs.getValue("name");
				if (name == null) {
					throw new SAXParseException("enum_table is missing name attribute", locator);
				}
				currentEnumDef = new EnumerationDefinition(name);
			} else if (qName.equalsIgnoreCase("enum")) {
				final String symbol = attrs.getValue("symbol");
				if (symbol == null) {
					throw new SAXParseException("enum element is missing symbol attribute", locator);
				}
				final String numericStr = attrs.getValue("numeric");
				if (numericStr == null) {
					throw new SAXParseException("enum element is missing numeric attribute", locator);
				}
				int numeric;
				try {
					numeric = Integer.parseInt(numericStr);
				} catch (final NumberFormatException e) {
					throw new SAXParseException("enum element has invalid numeric attribute: [" + numericStr + "]", locator, e);
				}
				currentEnumDef.addValue(numeric, symbol);
			}
		}

		@Override
		public void endElement(final String uri, final String localName,
				final String qName) throws SAXException {
			if (qName.equals("enum_table")) {
				enumTable.put(currentEnumDef.getName(), currentEnumDef);
			} else if (qName.equals(ENUM_DEFINIITIONS)) {
				if (reader.isPresent()) {
					reader.get().setContentHandler(parent.get());
					parent.get().endElement(uri, localName, qName);
				}
			}
		}

		@Override
		public void setDocumentLocator(final Locator l) {
			this.locator = l;
		}
		
		public Map<String, EnumerationDefinition> getEnumTable() {
			return enumTable;
		}
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName,
			final Attributes attributes) throws SAXException {
		try {
			this.text.setLength(0);
			switch (qName) {
				case DECOM_MAP:
					handleDecomMapStart(attributes);
					break;
				case ENUM_DEFINIITIONS:
					this.enumTableHandler = Optional.of(new EnumTableHandler(this.reader, this));
					reader.setContentHandler(this.enumTableHandler.get());
					break;
				case MOVE:
					handleMoveStart(attributes);
					break;
				case SWITCH:
					handleSwitchStart(attributes);
					break;
				case CASE:
					handleCaseStart(attributes);
					break;
				case DEFAULT:
					handleDefaultStart(attributes);
					break;
				case REPEAT_BLOCK:
					handleRepeatBlockStart(attributes);
					break;
				case BREAK:
					addInstruction(this.statementFactory.createBreakStatement());
					break;
				case EVENT_RECORD:
					DecomDataParams params = new DecomDataParams();
					setCommonDataParams(params, attributes);
					addInstruction(this.statementFactory.createEventRecordDefinition(params));
					break;
				case DECOM:
					handleDecomAlgoStart(attributes);
					break;
				case TRANSFORM:
					handleTransformAlgoStart(attributes);
					break;
				case VALIDATE:
					handleValidateAlgoStart(attributes);
					break;
				case TIME_TAG:
					handleTimeStart(attributes);
					break;
				case ARRAY:
					handleArrayStart(attributes);
					break;
				case DECOM_MAP_REF:
					handleMapRefStart(attributes);
					break;
				case DEFINE_BYTE_ORDER:
					handleByteOrder(attributes);
					break;
				case BOOLEAN:
					final BooleanParams params_2 = new BooleanParams();
					setBooleanParams(params_2, attributes);
					addInstruction(this.statementFactory.createBooleanDefinition(params_2));
					break;
				case INT:
					final IntegerParams intParams = new IntegerParams();
					setCommonNumericParams(intParams, attributes);
					intParams.setUnsigned(false);
					this.currentNumericParams = Optional.of(intParams);
					break;
				case UINT:
					final IntegerParams intParams_2 = new IntegerParams();
					setCommonNumericParams(intParams_2, attributes);
					intParams_2.setUnsigned(true);
					this.currentNumericParams = Optional.of(intParams_2);
					break;
				case ENUM:
					final EnumDataParams enumDataParams = new EnumDataParams();
					setEnumParams(enumDataParams, attributes);
					addInstruction(this.statementFactory.createEnumDataDefinition(enumDataParams));
					break;
				case OPCODE:
					final IntegerParams opcodeParams = new IntegerParams();
					setOpcodeParams(opcodeParams, attributes);
					addInstruction(this.statementFactory.createOpcodeDefinition(opcodeParams));
					break;
				case DOUBLE:
					final FloatingPointParams doubleParams = new FloatingPointParams();
					setFloatingPointParams(doubleParams, attributes);
					doubleParams.setPrecision(Precision.DOUBLE);
					this.currentNumericParams = Optional.of(doubleParams);
					break;
				case FLOAT:
					final FloatingPointParams floatParams = new FloatingPointParams();
					setFloatingPointParams(floatParams, attributes);
					floatParams.setPrecision(Precision.SINGLE);
					this.currentNumericParams = Optional.of(floatParams);
					break;
				case STRING:
					final StringParams stringParams = new StringParams();
					setStringParams(stringParams, attributes, "length");
					setCommonStorableParams(stringParams, attributes);
					this.containerStack.peek().addStatement(this.statementFactory.createStringDefinition(stringParams));
					break;
				case CSTRING:
					final StringParams stringParams_2 = new StringParams();
					stringParams_2.setNullTerminated(true);
					setStringParams(stringParams_2, attributes, "max_length");
					setCommonStorableParams(stringParams_2, attributes);
					this.containerStack.peek().addStatement(this.statementFactory.createStringDefinition(stringParams_2));
					break;
				case ARGS:
					this.inArgs = true;
					break;
				case "header":
					// Do nothing
					break;
				case RAW_TO_ENG:
					if(!this.currentAlgo.isPresent()) {
						this.rawToEngHandler = Optional.of(new RawToEngHandler(this.reader, this));
						this.reader.setContentHandler(this.rawToEngHandler.get());
					}
					break;
				default:
					if(!this.currentAlgo.isPresent()) {
						throw new SAXException(String.format("Encountered unknown element: <%s>", qName));
					}
			}
		} catch (final IllegalArgumentException | IllegalStateException | EmptyStackException e) {
			throw new SAXException(
					"Decom map parsing exception (startElement): "
							+ e.getMessage());
		}

	}

	private void handleByteOrder(final Attributes attributes) throws SAXException {
		final String byteOrder = attributes.getValue(BYTE_ORDER_ATTR);
		ByteOrder order;
		if (byteOrder == null) {
			throw new SAXException(DEFINE_BYTE_ORDER + " element is missing " + BYTE_ORDER_ATTR + " element");
		}
		if (LITTLE_ENDIAN.equals(byteOrder)) {
			order = ByteOrder.LITTLE_ENDIAN;
		} else if (BIG_ENDIAN.equals(byteOrder)) {
			order = ByteOrder.BIG_ENDIAN;
		} else {
			throw new SAXException("Unsupport byte order specified: " + byteOrder);
		}
		addInstruction(statementFactory.createByteOrderStatement(order));
	}

	private void handleDecomMapStart(final Attributes attributes) {
		containerStack.clear();
		structStack.clear();
		switchStack.clear();
		final String id = attributes.getValue("id");
		currentMap.setId(new DecomMapId(namespace, id));
		currentMap.setName(id);
		trace.debug("Parsing decom map: id="
				+ (id == null ? "null" : ("\"" + id + "\"")));
		final DecomMapParams params = new DecomMapParams();
		params.setId(id);
		this.containerStack.push(params);
	}
	
	private void handleMoveStart(final Attributes attributes) {
		String offsetAmount = attributes.getValue("forward");
		Direction offsetType = Direction.FORWARD;
		if (offsetAmount == null) {
			offsetAmount = attributes.getValue("backward");
			offsetType = Direction.BACKWARD;
		}
		if (offsetAmount == null) {
			offsetAmount = attributes.getValue("to_offset");
			offsetType = Direction.ABSOLUTE;
		}
		int multiplier = XmlUtility.getIntFromAttr(attributes, "multiplier");
		if (multiplier == 0) {
			multiplier = 1;
		}

		IMoveStatementDefinition stmt;
		try {
			final int offsetAmountValue = Integer.valueOf(offsetAmount);
			stmt = statementFactory.createMoveStatement(offsetAmountValue, offsetType, multiplier);
		} catch (final NumberFormatException e) {
			// The number format exception implies a variable name is given
			// If changing anything concerning the error handling, make sure the difference between a number
			// and a variable name is detected.
			stmt = statementFactory.createMoveStatement(offsetAmount, offsetType, multiplier);
		}
		containerStack.peek().addStatement(stmt);

	}
	
	private void handleSwitchStart(final Attributes attributes) throws SAXException {
		final String varName = attributes.getValue("variable");
		final String modulus = attributes.getValue("modulus");
		final SwitchParams switchParams = new SwitchParams();

		switchParams.setVariableName(varName);
		if (modulus != null) {
			switchParams.setModulus(XmlUtility.getIntFromText(modulus));
		}

		if (!this.declaredVariables.contains(varName)) {
			throw new SAXException("Switch variable " + varName
					+ " not declared earlier in the map");
		}

		switchStack.push(switchParams);

	}
	
	private void handleRepeatBlockStart(final Attributes attributes) {
		final RepeatBlockParams params = new RepeatBlockParams();
		final String maxLength = attributes.getValue("max_length");
		final String absoluteLength = attributes.getValue("abs_length");
		if (absoluteLength != null) {
			params.setAbsoluteLength(XmlUtility.getIntFromText(absoluteLength));
		} else if (maxLength != null) {
			params.setMaxLength(XmlUtility.getIntFromText(maxLength));
		}
		containerStack.push(params);
	}
	
	private void handleCaseStart(final Attributes attributes) throws SAXException {
		final String caseValStr = attributes.getValue("value");

		if (caseValStr == null) {
			throw new SAXException(
					"Case must specify a value attribute");
		}

		final long caseVal = XmlUtility.getLongFromText(caseValStr);

		if (caseVal < 0) {
			throw new SAXException(
					"Case value cannot be negative (got " + caseVal
					+ ")");
		}

		final CaseParams params = new CaseParams();
		this.switchStack.peek().startCase(caseVal, params);
		this.containerStack.push(params);
	}

	private void handleDefaultStart(final Attributes attributes) {
		final CaseParams params = new CaseParams();
		this.containerStack.push(params);
		this.switchStack.peek().startDefaultCase(params);
	}
	
	private void handleDecomAlgoStart(final Attributes attributes) {
		final String algoId = attributes.getValue("decommutator");
		final AlgorithmParams params = new AlgorithmParams();
		params.setType(AlgorithmType.DECOMMUTATOR);
		params.setAlgoId(algoId);
		currentAlgo = Optional.of(params);	
	}
	
	private void handleTransformAlgoStart(final Attributes attributes) {
		final String algoId = attributes.getValue("transformer");
		final AlgorithmParams params = new AlgorithmParams();
		params.setType(AlgorithmType.TRANSFORMER);
		params.setAlgoId(algoId);
		currentAlgo = Optional.of(params);
	}
	
	private void handleValidateAlgoStart(final Attributes attributes) {
		final String algoId = attributes.getValue("validator");
		final AlgorithmParams params = new AlgorithmParams();
		params.setType(AlgorithmType.VALIDATOR);
		params.setAlgoId(algoId);
		currentAlgo = Optional.of(params);

	}
	private void handleMapRefStart(final Attributes attributes) throws SAXException {
		// Delegate map reference
		reader.setContentHandler(new MapRefHandler(attributes.getValue("map_id"), reader));
	}

	private void handleArrayStart(final Attributes attributes) throws SAXException {
		final String numItemsStr = attributes.getValue("num_items");
		DataStructureParams params;
		if (numItemsStr == null) {
			throw new SAXException("Encountered array element without num_items defined.");
		}
		try {
			final int numItems = Integer.valueOf(numItemsStr);
			final StaticArrayParams staticParams = new StaticArrayParams();
			staticParams.setLength(numItems);
			params = staticParams;
		} catch (final NumberFormatException e) {
				final DynamicArrayParams dynaParams = new DynamicArrayParams();
				dynaParams.setLengthVariableName(numItemsStr);
				params = dynaParams;
		}
		structStack.push(params);
	}

	private void handleTimeStart(final Attributes attributes) {
		final String algoId = attributes.getValue("timetag_id");
		final TimeParams params = new TimeParams();
		params.setType(AlgorithmType.TIME);
		params.setAlgoId(algoId);
		params.setIsDelta(XmlUtility.getBooleanFromAttr(attributes, "is_delta"));
		currentAlgo = Optional.of(params);
	}
	
	private void addInstruction(final IDecomDataDefinition def) {
		if (!structStack.isEmpty()) {
			structStack.peek().add(def);
		} else {
			containerStack.peek().addStatement(def);
		}
	}

	private void addInstruction(final IDecomStatement def) {
		if (!structStack.isEmpty()) {
			throw new IllegalArgumentException("Non-data type encountered in data-only element.");
		} 
		containerStack.peek().addStatement(def);
	}
	
	private void setEnumParams(final EnumDataParams enumDataParams, final Attributes attributes) throws SAXException {
		enumDataParams.setEnumFormat(attributes.getValue("enum_format"));
		enumDataParams.setEnumName(attributes.getValue("enum_name"));
		enumDataParams.setEnumDefinition(enumDefs.get(attributes.getValue("enum_name")));
		setCommonNumericParams(enumDataParams, attributes);
	}

	private void setStringParams(final StringParams stringParams, final Attributes attributes, final String lengthAttr) throws SAXException {
		final String encodingStr = attributes.getValue("encoding");
		final int length = XmlUtility.getIntFromAttr(attributes, lengthAttr);
		if (encodingStr != null) {
			StringEncoding encoding;
			try {
			 encoding = StringEncoding.valueOf(encodingStr);
			} catch(final IllegalArgumentException e) {
				throw new SAXException(e.getMessage());
			}
			stringParams.setEncoding(encoding);
		}
		stringParams.setLength(length);
		setChannelParams(stringParams, attributes);
	}

	private void setOpcodeParams(final IntegerParams params, final Attributes attributes) {
		params.setUnsigned(true);
	}

	private void setFloatingPointParams(final FloatingPointParams floatParams, final Attributes attributes) throws SAXException {
		final String encoding = attributes.getValue("encoding");
		FloatEncoding encodingVal;
		if (encoding == null || encoding.equalsIgnoreCase(IEEE_ENCODING)) {
				encodingVal = FloatEncoding.IEEE;
		} else if (encoding.equalsIgnoreCase(MIL_ENCODING)) {
			encodingVal = FloatEncoding.MIL;
		} else {
			throw new SAXException("Decom map parsing error: Invalid float encoding \"" + encoding + "\"");
		}
		floatParams.setEncoding(encodingVal);
		setCommonNumericParams(floatParams, attributes);
	}

	private void setCommonDataParams(final DecomDataParams params, final Attributes attributes) {
		final String name = attributes.getValue("name");
		final String desc = attributes.getValue("sysdesc");
		final String offset = attributes.getValue("offset");
		final String format = attributes.getValue("format");

		if (offset != null) {
			params.setBitOffset(XmlUtility.getIntFromText(offset));
		}
		params.setName(name);
		params.setDescription(desc);
		params.setFormat(format);
		
	}

	private void setChannelParams(final ChannelizableDataParams params, final Attributes attributes) {
		params.setChannelId(attributes.getValue("channel_id"));
		final String channelize = attributes.getValue("channelize");
		// By default, a field should be channelized if an ID is defined. The channelize attribute
		// is intended to allow users to easily toggle a channel off rather than being extra metadata
		// every time they want to channelize a field.
		if (channelize == null) {
			params.setChannelize(true);
		} else {
			params.setChannelize(XmlUtility.getBooleanFromAttr(attributes, "channelize"));
		}
		setCommonDataParams(params, attributes);
	}

	private void setCommonNumericParams(final NumericParams params, final Attributes attributes) throws SAXException {
		/** bit_length is a required attribute for integral data types */
		if (params instanceof IntegerParams) { 
			if (attributes.getValue("bit_length") == null) {
				throw new SAXParseException("Decom map parsing error: Required attribute bit_length not set.", locator);
			}
			final int bitLength = XmlUtility.getIntFromAttr(attributes, "bit_length");
			if (bitLength <= 0) {
				throw new SAXParseException("Decom map parsing error: Invalid bit_length " + bitLength, locator);
			}
			params.setBitLength(bitLength);
		}
		
		final String unitsType = attributes.getValue("units_type");
		final String units = attributes.getValue("units");
		params.setUnits(units);
		params.setUnitsType(unitsType);
		setChannelParams(params, attributes);
		setCommonStorableParams(params, attributes);
	}

	private void setCommonStorableParams(StorableDataParams params, Attributes attributes) {
		boolean isVariable = XmlUtility.getBooleanFromAttr(attributes, "variable");
		params.setShouldStore(isVariable);
		if(isVariable) {
			this.declaredVariables.add(params.getName());
		}
	}

	private void setBooleanParams(final BooleanParams params, final Attributes attributes) throws SAXException {
		final String trueStr = attributes.getValue("true_str");
		params.setTrueString(trueStr);
		final String falseStr = attributes.getValue("false_str");
		params.setFalseString(falseStr);
		setCommonNumericParams(params, attributes);
	}

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {

        final String newText = new String(ch, start, length);

        if (!newText.equals("\n")) {
            this.text.append(newText);
        }

    }

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(final String uri, final String localName, final String qName)
			throws SAXException {
		try {
			final String ntext = XmlUtility.normalizeWhitespace(this.text);

			if (DECOM_MAP.equals(qName)) {
				// This should be the end of the line for this parser.  initialize the
				// map and restore content handling to the parent
				containerStack.pop().getStatements().forEach( (instr) -> currentMap.addStatement(instr) );
				reader.setContentHandler(parent);
				if (callLastEventOnParent) {
					parent.endElement(uri, localName, qName);
				}
			} else if (SWITCH.equals(qName)) {
				final SwitchParams params = this.switchStack.pop();
				containerStack.peek().addStatement(statementFactory.createSwitchStatement(params));
			} else if (DEFAULT.equals(qName)) {
				switchStack.peek().endCase();
				containerStack.pop();
			} else if (CASE.equals(qName)) {
				switchStack.peek().endCase();
				containerStack.pop();
			} else if (currentAlgo.isPresent() && ! ARGS.equals(qName) && inArgs) {
				currentAlgo.get().addArg(qName, ntext);
			} else if (ARGS.equals(qName)) {
				inArgs = false;
			} else if (DECOM.equals(qName) || VALIDATE.equals(qName) || TRANSFORM.equals(qName)) {
				containerStack.peek().addStatement(statementFactory.createAlgoritmInvocation(currentAlgo.get()));
				currentAlgo = Optional.empty();
			} else if (TIME_TAG.equals(qName)) {
				containerStack.peek().addStatement(statementFactory.createTimeDefinition((TimeParams) (currentAlgo.get())));
				currentAlgo = Optional.empty();
			} else if (ARRAY.equals(qName)) {
				final DataStructureParams params = structStack.pop();
				if (params instanceof DynamicArrayParams) {
					addInstruction(statementFactory.createDynamicArrayDefinition((DynamicArrayParams) params));
				} else {
					addInstruction(statementFactory.createStaticArrayDefinition((StaticArrayParams) params));
				}
			} else if (REPEAT_BLOCK.equals(qName)) {
				final IDecomStatement statement = statementFactory.createRepeatBlock((RepeatBlockParams) containerStack.pop());
				containerStack.peek().addStatement(statement);
			} else if (RAW_TO_ENG.equals(qName)) {
				//IDecomStatement statement = containerStack.peek().getStatements().get(containerStack.peek().getStatements().size() - 1);
				//if (statement instanceof INumericDataDefinition) {
				if (currentNumericParams.isPresent()) {
					if (rawToEngHandler.isPresent()) {
						currentNumericParams.get().setDnToEu(rawToEngHandler.get().getEuDef());
						rawToEngHandler = Optional.empty();
					} else {
						throw new IllegalStateException("Encountered " + RAW_TO_ENG + " end tag without first encountering start tag");
					}
				} else {
						throw new IllegalStateException("Encountered " + RAW_TO_ENG + " end tag on a data type that does not support it");
				}
			} else if (INT.equals(qName) || UINT.equals(qName)) {
				if (currentNumericParams.isPresent() && currentNumericParams.get() instanceof IntegerParams) {
					addInstruction(statementFactory.createIntegerDefinition((IntegerParams) currentNumericParams.get()));
				} else {
					throw new IllegalStateException("Encountered closing tag without matching open tag");
				}
			} else if( FLOAT.equals(qName) || DOUBLE.equals(qName)) {
				if (currentNumericParams.isPresent() && currentNumericParams.get() instanceof FloatingPointParams) {
					addInstruction(statementFactory.createFloatingPointDefinition((FloatingPointParams) currentNumericParams.get()));
				} else {
					throw new IllegalStateException("Encountered closing tag without matching open tag");
				}
			} else if (ENUM_DEFINIITIONS.equals(qName)) {
				enumDefs = enumTableHandler.get().getEnumTable();
			}


		} catch (final IllegalArgumentException | IllegalStateException | EmptyStackException e) {
			throw new SAXParseException("Decom map parsing exception (endElement): "
					+ e.getMessage(), locator);
		}
    }
	
	@Override
	public void setDocumentLocator(final Locator l) {
		this.locator = l;
	}


	private class MapRefHandler extends DefaultHandler {
		
		private static final String CHANNEL_MAPPINGS = "channel_mappings";
		private static final String CHANNEL_MAPPING = "channel_mapping";
		private final XMLReader reader;
		private final DecomMapReferenceParams params = new DecomMapReferenceParams();
		

		public MapRefHandler(final String mapId, final XMLReader reader) {
			params.setMapId(mapId);
			this.reader = reader;
		}
		
		@Override
		public void startElement(final String namespaceUri, final String localName, final String qName, final Attributes attrs) throws SAXException {
			if (CHANNEL_MAPPINGS.equals(qName)) {
				// Do Nothing
			} else if (CHANNEL_MAPPING.equals(qName)) {
				final String fieldName = attrs.getValue("from_name");
				final String channelId = attrs.getValue("to_channel_id");
				final String publishChannelStr = attrs.getValue("publish_channel");
				boolean publishChannel = true;
				if (publishChannelStr != null) {
					publishChannel = XmlUtility.getBooleanFromText(publishChannelStr);
				}
				if(publishChannel) {
					params.addMapping(fieldName, channelId);
				}
			} else {
				throw new SAXException("Illegal element encountered");
			}
		}

		@Override
		public void endElement(final String uri, final String localName, final String qName)
				throws SAXException {
			if (DECOM_MAP_REF.equals(qName)) {
				// Store this map reference and reinstate parent content handler
				addInstruction(statementFactory.createDecomMapReference(params));
				reader.setContentHandler(MultimissionDecomMapParser.this);
			}
		}
		
	}

	/**
	 * Add variables that have been created externally to the decom map.
	 * Should be called before decom map parsing for the purpose of allowing
	 * this parser to validate that any variable references encountered refer
	 * to variables that are known to be defined..
	 * @param stmts the list of statements to add variables from
	 */
	public void addVariables(List<IDecomStatement> stmts) {
		for (IDecomStatement stmt : stmts) {
			if (stmt instanceof IGroundVariableDefinition) {
				currentMap.addStatement(stmt);
				declaredVariables.add(((IGroundVariableDefinition) stmt).getName());
			}
		}
	}
}
