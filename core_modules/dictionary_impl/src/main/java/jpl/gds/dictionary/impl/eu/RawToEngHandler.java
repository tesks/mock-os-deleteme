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
package jpl.gds.dictionary.impl.eu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.dictionary.api.eu.EUDefinitionFactory;
import jpl.gds.dictionary.api.eu.EUType;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.shared.xml.XmlUtility;

/**
 * Sub parser for raw-to-engineering conversion definitions that conform to multimission
 * schema definitions.  Currently supports the definition in CommonDictionary.rnc v3.1.
 * 
 * This class extends {@link DefaultHandler}, so it can be set on an {@link XMLReader}
 * object and will handle elements until it sees an ending tag that closes one of the
 * engineering unit conversion it knows about, e.g. a </table_lookup> tag.
 * 
 * Throws SAXExceptions from its parsing methods - the parent parser will not get back its
 * control in case of an error.
 * 
 * After parsing is passed back to the parent handler, {@link #getEuDef()} can be called
 * to get the object resulting from parsing.
 *
 *
 */
public class RawToEngHandler extends DefaultHandler {

	private static final String LOG = "log";
	private static final String CHANGE_LOG = "change_log";

	private static final String ENG = "eng";
	private static final String RAW = "raw";
	private static final String MISSING_ATTR_TEMPLATE = "Missing required attribute %s in element %s";
	private static final String PARAMETER = "parameter";
	private static final String INDEX = "index";
	private static final String COEFF = "coeff";
	private static final String FACTOR = "factor";
	private static final String POINT = "point";
	private static final String TABLE_LOOKUP = "table_lookup";
	private static final String POLYNOMIAL_EXPANSION = "polynomial_expansion";
	private static final String NAME = "name";
	private static final String PARAMETERIZED_ALGORITHM = "parameterized_algorithm";
	private final Optional<XMLReader> reader;
	private List<Double> dnTable = new ArrayList<Double>();
	private List<Double> euTable = new ArrayList<Double>();
	private Map<String,String> parameterMap = new HashMap<String,String>();
	private List<Double> coeffTable = new ArrayList<Double>();
	private EUType euType = EUType.NONE;
	private String algorithmName;
	private String currentArgName;
	private StringBuilder text = new StringBuilder();

	private IEUDefinition euDef;
	
	private final Optional<DefaultHandler> parent;
	private Locator locator;

	/**
	 * Create an instance that will set the parent {@link DefaultHandler} instance
	 * as the handler on the reader after parsing a EU conversion definition.
	 * @param reader the XMLReader driving parsing
	 * @param parent the handler to pass parsing off to
	 */
	public RawToEngHandler(XMLReader reader, DefaultHandler parent) {
		this.reader = Optional.ofNullable(reader);
		this.parent = Optional.ofNullable(parent);
	}
	
	/* package */ RawToEngHandler() {
		reader = Optional.empty();
		parent = Optional.empty();
	}

	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attrs) throws SAXException {
		if (qName.equalsIgnoreCase(PARAMETERIZED_ALGORITHM)) {
			euType = EUType.PARAMETERIZED_ALGORITHM;
			algorithmName = attrs.getValue(NAME);
			if (algorithmName == null) {
				throw new SAXParseException(String.format(MISSING_ATTR_TEMPLATE, NAME, PARAMETERIZED_ALGORITHM), locator);
			}
		} else if (qName.equalsIgnoreCase(POLYNOMIAL_EXPANSION)) {
			euType = EUType.POLYNOMIAL;
		} else if (qName.equalsIgnoreCase(TABLE_LOOKUP)) {
			euType = EUType.TABLE;
		} else if (qName.equalsIgnoreCase(POINT) && euType == EUType.TABLE) {
			handlePointStart(attrs);
		} else if (qName.equalsIgnoreCase(FACTOR) && euType == EUType.POLYNOMIAL) {
			handleFactorStart(attrs);
		} else if (qName.equalsIgnoreCase(PARAMETER) && euType == EUType.PARAMETERIZED_ALGORITHM) {
			currentArgName = attrs.getValue(NAME);
			if (currentArgName == null) {
				throw new SAXParseException(String.format(MISSING_ATTR_TEMPLATE, NAME, PARAMETER), locator);
			}
		} else if (qName.equals(CHANGE_LOG) && euType == EUType.PARAMETERIZED_ALGORITHM) {
			// Do nothing
		} else if (qName.equals(LOG) && euType == EUType.PARAMETERIZED_ALGORITHM) {
			// Do nothing
		} else {
			if (euType == EUType.NONE) {
				throw new SAXParseException("Invalid element encountered: <" + qName + ">. Expected one of parameterized_algorithm, table_lookup, or polynomial_expansion.", locator);
			} else {
				throw new SAXParseException("Invalid element encountered <" + qName + ">", locator);
			}
		}
	}
;
	private void handlePointStart(Attributes attrs) throws SAXException {
		final String rawStr = attrs.getValue(RAW);
		if (rawStr == null) {
			throw new SAXParseException(String.format(MISSING_ATTR_TEMPLATE, RAW, TABLE_LOOKUP), locator);
		}
		final double raw;
		try {
			raw = Double.valueOf(rawStr);
		} catch (NumberFormatException e) {
			throw new SAXParseException(String.format("Invalid value for attribute %s in element %s", "raw", POINT), locator, e);
		}
		final String engStr = attrs.getValue(ENG);
		if (engStr == null) {
			throw new SAXParseException(String.format(MISSING_ATTR_TEMPLATE, ENG, TABLE_LOOKUP), locator);
		}
		final double eng;
		try {
			eng = Double.valueOf(engStr);
		} catch (NumberFormatException e) {
			throw new SAXParseException(String.format("Invalid value for attribute %s in element %s", "raw", POINT), locator, e);
		}
		dnTable.add(raw);
		euTable.add(eng);
	}

	private void handleFactorStart(Attributes attrs) throws SAXException {
		final String coefStr = attrs.getValue(COEFF);
		if (coefStr == null) {
			throw new SAXParseException(String.format(MISSING_ATTR_TEMPLATE, COEFF, POLYNOMIAL_EXPANSION), locator);
		}
		final String indexStr = attrs.getValue(INDEX);
		if (indexStr == null) {
			throw new SAXParseException(String.format(MISSING_ATTR_TEMPLATE, INDEX, POLYNOMIAL_EXPANSION), locator);
		}
		final double coeff;
		try {
			coeff = Double.valueOf(coefStr);
		} catch (NumberFormatException e) {
			throw new SAXParseException("Invalid value for polynomial expansion coefficient: " + coefStr, locator);
		}
		final int index;
		try {
			index = Integer.valueOf(indexStr);
		} catch (NumberFormatException e){
			throw new SAXParseException("Invalid value for polynomical expansion index: " + indexStr, locator);
		}
		while (coeffTable.size() - 1 < index) {
			coeffTable.add(null);
		}
		coeffTable.set(index, coeff);
	}

	@Override
	public void characters(final char[] chars, final int start, final int length)
			throws SAXException {
		String newText = new String(chars, start, length);
		if (!newText.equals("\n")) {
			text.append(newText);
		}
	}

	@Override
	public void endElement(final String uri, final String localName,
			final String qName) throws SAXException {
		final String ntext = XmlUtility.normalizeWhitespace(text);
		text.setLength(0);
		try {
		if (qName.equals(PARAMETERIZED_ALGORITHM)) {
			euDef = EUDefinitionFactory.createAlgorithmicEU("", algorithmName,
					new HashMap<String,String>(parameterMap));
			if (reader.isPresent()) {
				reader.get().setContentHandler(parent.get());
			}
		} else if (qName.equalsIgnoreCase(TABLE_LOOKUP)) {
			euDef = EUDefinitionFactory.createTableEU(dnTable, euTable);
			if (reader.isPresent()) {
				reader.get().setContentHandler(parent.get());
			}
		} else if (qName.equalsIgnoreCase(POLYNOMIAL_EXPANSION)) {
			euDef = EUDefinitionFactory.createPolynomialEU(coeffTable);
			if (reader.isPresent()) {
				reader.get().setContentHandler(parent.get());
			}
		} else if (qName.equalsIgnoreCase(PARAMETER)) {
			parameterMap.put(currentArgName, ntext);
		}
		} catch (IllegalArgumentException e) {
			throw new SAXParseException(String.format("Invalid definition for element %s", qName), locator, e);
		}
	}
	
	@Override
	public void setDocumentLocator(Locator l) {
		this.locator = l;
	}

	/** 
	 * Get the EU definition that has been built from XML parsing.
	 * @return the {@link IEUDefinition} instance resulting from XML parsing.
	 */
	public IEUDefinition getEuDef() {
		return euDef;
	}
	
	/**
	 * Reset this parser in between definitions. Required if this instance is used to parse
	 * more than one raw-to-eng XML block.
	 * 
	 * The parser's parent and XMLReader references are not reset, so create a new instance
	 * if needed.
	 */
	public void reset() {
		dnTable = new ArrayList<>();
		euTable = new ArrayList<>();
		parameterMap = new HashMap<>();
		coeffTable = new ArrayList<>();
		euDef = null;
		euType = EUType.NONE;
	}
}
