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
package jpl.gds.product.impl.dictionary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.api.decom.BaseDecomDataType;
import jpl.gds.product.api.decom.DecomDataType;
import jpl.gds.product.api.decom.IProductDecomFieldFactory;
import jpl.gds.product.api.dictionary.IProductDefinition;
import jpl.gds.product.api.dictionary.IProductObjectDefinition;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * 
 * ReferenceProductDefinitionBuilder is the generic product definition builder.
 * It parses a product dictionary file and produces a product definition.
 * 
 */
public class ReferenceProductDefinitionBuilder extends AbstractProductDefinitionBuilder {


    /**
     * Constructor.
     * 
     * @param fieldFactory the decom field factory to use
     */
    public ReferenceProductDefinitionBuilder(final IProductDecomFieldFactory fieldFactory) {
		super(fieldFactory);
	}


	/**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinitionBuilder#buildProductDefinition(java.lang.String)
     */
    @Override
	public IProductDefinition buildProductDefinition(final String filename)
                                                                    throws DictionaryException {
        reset();
        if (filename == null) {
            logger.error("Product definition file path is undefined");
            throw new DictionaryException(
                    "Product definition file path is undefined");
        }
        final File path = new File(filename);
        if (logger.isDebugEnabled()) {
            logger.debug("Parsing product definition from ", FileUtility.createFilePathLogMessage(path));
        }
        SAXParser parser = null;
        try {
            parser = SAXParserPool.getInstance().get();
            parser.parse(path, this);
        } catch (final FileNotFoundException e) {
            logger.error("Product definition file " + FileUtility.createFilePathLogMessage(path)
                            + " not found");
            throw new DictionaryException("Product definition file "
                    + path.getAbsolutePath() + " not found", e);
        } catch (final IOException e) {
            logger.error("IO Error reading product definition file "
                            + path.getAbsolutePath());
            throw new DictionaryException(
                    "IO Error reading product definition file "
                            + path.getAbsolutePath(), e);
        } catch (final ParserConfigurationException e) {
            logger.error("Error configuring SAX parser for product definition file "
                            + path.getAbsolutePath());
            throw new DictionaryException(
                    "Error configuring SAX parser for product definition file "
                            + path.getAbsolutePath(), e);
        } catch (final SAXException e) {
            logger.error("Error parsing product definition file "
                            + path.getAbsolutePath());
            logger.error(e.getMessage());
            throw new DictionaryException(e.getMessage(), e);
        } finally {
            if (parser != null) {
                SAXParserPool.getInstance().release(parser);
            }
        }

        return currentDefinition;
    }
    

	/**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.dictionary.AbstractProductDefinitionBuilder#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qname, final Attributes attr) throws SAXException {
        super.startElement(uri, localName, qname, attr);

        if (qname.equalsIgnoreCase("table") && !inProduct()) {
            startProduct(new ReferenceProductDefinition());

        } else if (qname.equalsIgnoreCase("name") && inProduct()) {
            if (attr.getValue("id") == null) {
                throw new SAXException(
                        "table name element is missing 'id' attribute");
            }
            currentDefinition.setApid(getIntAttrValue(attr, "id"));
            currentDefinition.setName(attr.getValue("title"));
            currentDefinition.setDescription(attr.getValue("fsw_id"));

        } else if (qname.equalsIgnoreCase("versioninfo") && inProduct()) {
            if (attr.getValue("ver_num") == null) {
                logger.warn("Product definition did not have ver_num attribute; "
                                + "assuming version 0");
                currentDefinition.setVersion("0");
            } else {
                currentDefinition.setVersion(attr.getValue("ver_num"));
            }

        } else if (qname.equalsIgnoreCase("repeat_block") && inProduct()) {
            startArray("Repeat Block", -1, null, null);

        } else if (qname.equalsIgnoreCase("field") && inProduct()) {
            final String name = attr.getValue("fsw_name");
            final String dataType = attr.getValue("datatype");
            final String bitlen = attr.getValue("bit_len");
            String bytelen = null;
            if (bitlen == null) {
                bytelen = attr.getValue("length");
            }
            if (name == null || dataType == null
                    || (bitlen == null && bytelen == null)) {
                throw new SAXException(
                        "field element must have 'fsw_name', 'datatype', and 'bit_len' or 'length' attributes");
            }
            int length = 0;
            if (bitlen != null) {
                length = getIntAttrValue(attr, "bit_len");
            } else {
                length = getIntAttrValue(attr, "length") * 8;
            }
            startField(name, createDataType(dataType, length), name,
                    getBooleanAttrValue(attr, "isChannel"));
            final String format = attr.getValue("format");
            setCurrentPrintFormat(format);

        } else if (qname.equalsIgnoreCase("dynamic_array") && inProduct()) {
            String name = attr.getValue("fsw_name");
            final String len = attr.getValue("variable");
            final String outputFormat = attr.getValue("output_format");
         
            if (name == null) {
                name = "Anonymous";
            }
            if (len == null) {
                throw new SAXException(
                        "dynamic_array element must have 'variable' attribute");
            }
            startArray(name, len, null, outputFormat);

        } else if (qname.equalsIgnoreCase("array") && inProduct()) {
            String name = attr.getValue("fsw_name");
            final String len = attr.getValue("num_items");
            final String outputFormat = attr.getValue("output_format");
           
            if (name == null) {
                name = "Anonymous";
            }
            if (len == null) {
                throw new SAXException(
                        "array element must have 'num_item' attribute");
            }
            startArray(name, getIntAttrValue(attr, "num_items"), null, outputFormat);
        } else if (qname.equalsIgnoreCase("poly") && inField()) {
            startDnToEu();

        } else if (qname.equalsIgnoreCase("coeff") && inDnToEu()) {
            try {
                final int index = XmlUtility.getIntFromAttr(attr, "index");
                setPolyDnToEuIndex(index);

            } catch (final NumberFormatException e) {
                throw new SAXException(
                        "index attribute of coeff element must be an integer");
            }

        } else if (qname.equalsIgnoreCase("table") && inField()
                && attr.getValue("eu") != null) {

        	// This is the "lookup" element's "table".
        	
            if (!inDnToEu()) {
                startDnToEu();
            }
            try {
                final double dn = XmlUtility.getDoubleFromAttr(attr, "dn");
                final double eu = XmlUtility.getDoubleFromAttr(attr, "eu");
                addTableDnToEu(dn, eu);
            } catch (final NumberFormatException e) {
                throw new SAXException(
                        "Both the dn and eu attributes in a lookup table entry must be numeric");
            }

        } else if (qname.equalsIgnoreCase("states")) {
            if (!inEnumDefinition()) {
                startEnumDefinition("none");
            }
            final String state = attr.getValue("enumerated_value");
            if (state == null) {
                throw new SAXException(
                        "states element must have 'enumerated_value' attribute");
            }
            final String value = attr.getValue("dict_value");
            if (value == null) {
                throw new SAXException(
                        "states element must have 'dict_value' attribute");
            }
            try {
                final int id = XmlUtility.getIntFromAttr(attr, "enumerated_value");
                this.setCurrentEnumIndex(id);
            } catch (final NumberFormatException e) {
                throw new SAXException(
                        "states enumerates_value is not an integer");
            }
            this.setCurrentEnumValue(value);
          
            
        } else if (qname.equalsIgnoreCase("viewer")) {
        	/*
        	 *  Add support for external product viewers.
        	 */
        	final String name = attr.getValue("name");
        	if (name == null) {
        		throw new SAXException("viewer element must have a 'name' attribute");
        	}
        	if (!name.equalsIgnoreCase("default")) {
    			this.setCurrentHandler(name, false, false);
        	}
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qname) throws SAXException {
        if (qname.equalsIgnoreCase("table") && !inField()) {
            endProduct();
        } else if (qname.equalsIgnoreCase("command") && inProduct()) {
            final String command = getTextBufferNonEmpty();
            if (command != null) {
                currentDefinition.setCommand(command);
            }

        } else if (qname.equalsIgnoreCase("units") && inField()) {
            final String unit = getTextBufferNonEmpty();
            if (unit != null) {
                currentField.setUnit(unit);
            }
  
        } else if (qname.equalsIgnoreCase("sysdesc") && inField()) {
            currentField.setSysDescription(getTextBuffer());

        } else if (qname.equalsIgnoreCase("field")) {
            endField();

        } else if (qname.equalsIgnoreCase("array")) {
            endArray();

        } else if (qname.equalsIgnoreCase("dynamic_array")) {
            endArray();

        } else if (qname.equalsIgnoreCase("repeat_block")) {
            endArray();

        } else if (qname.equalsIgnoreCase("lookup") && inEnumDefinition()) {
            this.setCurrentLookup();
            this.endEnumDefinition();

        } else if (qname.equalsIgnoreCase("lookup") && inDnToEu()) {
            endTableDnToEu();

        } else if (qname.equalsIgnoreCase("table") && inField()
                && !this.inDnToEu() && this.inEnumDefinition()) {

        	// This is the "lookup" element's "table".
        	
            this.setCurrentLookup();
            this.endEnumDefinition();

        } else if (qname.equalsIgnoreCase("poly")) {
            endPolyDnToEu();

        } else if (qname.equalsIgnoreCase("coeff") && inDnToEu()) {
            final String coeffStr = getTextBufferNonEmpty();
            if (coeffStr == null) {
                throw new SAXException(
                        "coeff element must have a non-empty numeric value");
            }
            try {
                final double coeff = XmlUtility.getDoubleFromText(coeffStr);
                setPolyDnToEuCoefficient(coeff);
            } catch (final NumberFormatException e) {
                throw new SAXException(
                        "coeff element must have a numeric value");
            }

        } else if (qname.equalsIgnoreCase("viewer")) {
        	/*
        	 * Add support for external product viewers.
        	 */
        	final DecomHandler handler = currentDefinition.getExternalHandler();
        	final String args = getTextBufferNonEmpty();
        	if (args != null) {
        	     handler.setHandlerName(handler.getHandlerName() + " " + args);	
        	}
        }
    }

    // Maps field data types to generic data dictionary type
    private DecomDataType createDataType(final String dataType, final int bitlen)
                                                                                 throws SAXException {
        if (dataType.equalsIgnoreCase("UNSIGNED_INT")) {
            return new DecomDataType(BaseDecomDataType.UNSIGNED_INT, bitlen);
        }
        if (dataType.equalsIgnoreCase("SIGNED_INT")) {
            return new DecomDataType(BaseDecomDataType.SIGNED_INT, bitlen);
        }
        if (dataType.equalsIgnoreCase("FLOAT")) {
            return new DecomDataType(BaseDecomDataType.FLOAT, bitlen);
        }
        if (dataType.equalsIgnoreCase("ASCII")) {
            return new DecomDataType(BaseDecomDataType.STRING, bitlen);
        }
        if (dataType.equalsIgnoreCase("STATUS")) {
            return new DecomDataType(BaseDecomDataType.ENUMERATION, bitlen);
        }
        // NOTE: Not supported by current schema, but left in to support the SMAP mission
        if (dataType.equalsIgnoreCase("DIGITAL")) {
            return new DecomDataType(BaseDecomDataType.DIGITAL, bitlen);
        }
        if (dataType.equalsIgnoreCase("ENUMERATION")) {
            return new DecomDataType(BaseDecomDataType.ENUMERATION, bitlen);
        }
        if (dataType.equalsIgnoreCase("STRING")) {
            return new DecomDataType(BaseDecomDataType.STRING, bitlen);
        }
        if (dataType.equalsIgnoreCase("FILL")) {
            return new DecomDataType(BaseDecomDataType.FILL, bitlen);
        }
        throw new SAXException("Unrecognized data type for field: " + dataType);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinitionBuilder#buildProductObjectDefinition(java.lang.String)
     */
    @Override
	public IProductObjectDefinition buildProductObjectDefinition(final String path) throws DictionaryException {
        throw new UnsupportedOperationException("This product dictionary implementation does not support loading of data product objects");
    }

    @Override
    protected IProductObjectDefinition createProductObjectDefinition() {
        return new ProductObjectDefinition();
    }
    
}
