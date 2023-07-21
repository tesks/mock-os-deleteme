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

import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.IDecomHandlerSupport;
import jpl.gds.dictionary.api.eu.EUDefinitionFactory;
import jpl.gds.dictionary.api.eu.IAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IPolynomialEUDefinition;
import jpl.gds.dictionary.api.eu.ITableEUDefinition;
import jpl.gds.product.api.decom.*;
import jpl.gds.product.api.dictionary.IProductDefinition;
import jpl.gds.product.api.dictionary.IProductDefinitionBuilder;
import jpl.gds.product.api.dictionary.IProductObjectDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

/**
 * 
 * ProductDefinitionBuilder builds top level product definitions from XML
 * definition files. It must be extended to supply project-specific methods for
 * the actual parsing. This class provides convenience methods for building
 * product definitions from parsed information.
 * 
 * The end result of the parsing is placed into the currentDefinition field (for
 * product-based dictionaries) or in the currentDpo field (for DPO-based
 * dictionaries).
 * 
 */
public abstract class AbstractProductDefinitionBuilder extends DefaultHandler implements IProductDefinitionBuilder 
{
    /** Shared trace logger instance */
    protected static final Tracer logger = TraceManager.getTracer(Loggers.TLM_PRODUCT);

    private StringBuffer text;
    private boolean isInProduct;
    private boolean isInField;
    private boolean isInArray;
    private boolean isInStates;
    private boolean inBitArray;
    private boolean isInBitField;
    private boolean isInStream;
    private boolean isInEnumDef;
    private boolean isInStructure;
    private boolean isInStructureField;
    private int stateId = 0;
    private IFieldContainer currentBitArray;
    private IProductDecomField currentStreamField;
    private final Stack<IFieldContainer>          containerStack = new Stack<>();
    private final Map<String, IProductDecomField> elementMap     = new HashMap<>();
    private       boolean                         isInDnToEu;
    private final List<Double> dnTable = new ArrayList<>();
    private final List<Double> euTable = new ArrayList<>();
    private int currentPolyIndex = 0;
    private final List<Double> coeffTable = new ArrayList<>();
    private final Map<String, EnumerationDefinition> definedEnums = new HashMap<>();
    private final Map<String, IStructureField> definedStructs = new HashMap<>();
    private String algoClassName;
    private boolean isInDpo;
    
    /**
     * Current product definition being parsed. 
     */
    protected IProductDefinition currentDefinition;
    /**
     * Current primitive field being parsed. 
     */
    protected ISimpleField currentField;
    /**
     * Current array definition being parsed. 
     */
    protected IArrayField currentArray;
    /**
     * Current bit field definition being parsed. 
     */
    protected IPrimitiveField currentBitField;
    /**
     * Current structure definition being parsed. 
     */
    protected IStructureField currentStructure;
    /**
     * Current structure field being parsed. 
     */
    protected IStructureField currentStructureField;
    /**
     * Current enumeration definition being parsed. 
     */
    protected EnumerationDefinition currentEnumDef;
    /**
     * Current data product object definition being parsed. 
     */
    protected IProductObjectDefinition currentDpo;
    
    /** Adding the decom field factory. */
    protected IProductDecomFieldFactory fieldFactory;
    
    /**
     * Constructor.
     * 
     * @param fieldFactory the decom field factory to use
     */
    public AbstractProductDefinitionBuilder(final IProductDecomFieldFactory fieldFactory) {
    	super();
    	this.fieldFactory = fieldFactory;
    }
    
    /**
     * Sets the print format for the field definition currently being parsed.
     * Applies to definition fields and bit fields.
     * 
     * @param format the format string (e.g., "%d")
     */
    protected void setCurrentPrintFormat(final String format) {
        if (format == null || format.length() == 0) {
            return;
        }
        if (inBitField()) {
            currentBitField.setPrintFormat(format);
        } else if (inField()) {
            currentField.setPrintFormat(format);
        }
    }

    /**
     * Sets the handler for the product, dpo, or stream field definition being
     * parsed
     * 
     * @param vw the handler class name or launch string
     * @param internal true if the handler is an internal (Java class) handler;
     *            false if an external application
     * @param wait true if the executing program should wait for the handler to
     *            complete
     */
    protected void setCurrentHandler(final String vw, final boolean internal, final boolean wait) {
    	if (vw == null || vw.length() == 0) {
    		return;
    	}
    	final DecomHandler handler = new DecomHandler();
    	handler.setHandlerName(vw);
    	handler
    	.setType(internal ? DecomHandler.DecomHandlerType.INTERNAL_JAVA_CLASS
    			: DecomHandler.DecomHandlerType.EXTERNAL_PROGRAM);
    	handler.setWait(wait);
    
    	if (inStream() && currentStreamField instanceof IDecomHandlerSupport) {
    		((IDecomHandlerSupport)currentStreamField).setExternalHandler(handler);
    	} else if (inDpo()) {
    		if (internal) {
    			currentDpo.setInternalHandler(handler);
    		} else {
    			currentDpo.setExternalHandler(handler);
    		}
    	} else if (inProduct()) {
    		if (internal) {
    			currentDefinition.setInternalHandler(handler);
    		} else {
    			currentDefinition.setExternalHandler(handler);
    		}
    	}
    }

    /**
     * Starts parsing of an enumerated type definition.
     * 
     * @param name the name of the enumeration
     */
    protected void startEnumDefinition(final String name) {
        isInEnumDef = true;
        currentEnumDef = new EnumerationDefinition(name);
    }

    /**
     * Indicates whether we are in the process of parsing an enumerated type
     * definition.
     * 
     * @return rue if parsing an enumeration
     */
    protected boolean inEnumDefinition() {
        return isInEnumDef;
    }

    /**
     * Ends parsing of an enumerated type definition.
     */
    protected void endEnumDefinition() {
        definedEnums.put(currentEnumDef.getName(), currentEnumDef);
        currentEnumDef = null;
        isInEnumDef = false;
    }

    /**
     * Adds the current enumeration definition as the lookup table on the
     * current field. Must be called before endEnumDefinition().
     */
    protected void setCurrentLookup() {
        if (inBitField()) {
            (currentBitField).setLookupTable(currentEnumDef);
        } else if (inField()) {
            (currentField).setLookupTable(currentEnumDef);
        }
    }

    /**
     * Sets the current index (key) for the current state table mapping or
     * enumeration definition.
     * 
     * @param id the id/key to set
     */
    protected void setCurrentEnumIndex(final int id) {
        stateId = id;
    }

    /**
     * Sets the value to be mapped to the current index (key) for the current
     * state table mapping or enumeration definition.
     * 
     * @param value the value to set
     */
    protected void setCurrentEnumValue(final String value) {
        currentEnumDef.addValue(stateId, value);
    }

    /**
     * Gets a previously parsed enumerated type definition.
     * 
     * @param name the name of the enumeration to get
     * @return the EnumerationDefinition, or null if the name is not found
     */
    protected EnumerationDefinition getEnumDefinition(final String name) {
        return definedEnums.get(name);
    }

    /**
     * Sets a new product definition to be populated by the parser.
     * 
     * @param newDef the new product definition
     */
    protected void startProduct(final IProductDefinition newDef) {
        isInProduct = true;
        currentDefinition = newDef;
        currentDpo = createProductObjectDefinition();
        currentDpo.setName("Main Product Object");
        currentDefinition.addProductObject(currentDpo);
        containerStack.push(currentDpo);
    }
    
    /**
     * Creates a product object definition.
     * 
     * @return definition object
     */
    protected IProductObjectDefinition createProductObjectDefinition() {
        return new ProductObjectDefinition();
    }

    /**
     * Ends parsing of a product definition.
     * 
     */
    protected void endProduct() {
        isInProduct = false;
        containerStack.pop();
    }

    /**
     * Indicates if the parser is in a product definition.
     * 
     * @return true if currently parsing a product definition
     */
    protected boolean inProduct() {
        return isInProduct;
    }

    /**
     * Starts parsing of a structure definition.
     * 
     * @param name of the structure type
     */
    protected void startStructure(final String name) {
        isInStructure = true;
        currentStructure = fieldFactory.createStructureField(name);
        containerStack.push(currentStructure);
    }

    /**
     * Ends parsing of a structure definition.
     */
    protected void endStructure() {
        definedStructs.put(currentStructure.getName(), currentStructure);
        containerStack.pop();
        currentStructure = (IStructureField)findOnStack(IStructureField.class);
        isInStructure = currentStructure != null;
    }

    /**
     * Attaches the given description/comment to the latest container object on
     * the container stack.
     * 
     * @param comment the text to set as the description
     * @throws SAXException if there is no current container
     */
    public void setCommentOnLastContainer(final String comment) throws SAXException {
        final IFieldContainer container = containerStack.peek();
        if (container == null || container instanceof IProductDefinition) {
            throw new SAXException("No container to set comment on");
        }
        (container).setFswDescription(comment);
    }

    /**
     * Gets a previously parsed structure type definition.
     * 
     * @param name the name of the structure to get
     * @return the IDecomFieldContainer for the structure, or null if the name is not found
     */
    protected IStructureField getStructureDefinition(final String name) {
        return definedStructs.get(name);
    }

    /**
     * Starts parsing of a structured array field.
     * 
     * @param name the name of the array
     * @param length the maximum length of the array, or -1 for variable length
     *            arrays
     * @param fswName the FSW name for the array (may be null)
     * @param outputFormat the output format for displaying the contents of the
     *            array (may be null)
     */
    protected void startArray(final String name, final int length,
            final String fswName, final String outputFormat) {
        isInArray = true;
		if (length != -1) {
			currentArray = fieldFactory.createArrayField(name, length);
		}
		else {
			currentArray = fieldFactory.createArrayField(name, -1);
		}
        containerStack.push(currentArray);
        if (fswName != null && fswName.length() != 0) {
            currentArray.setFswName(fswName);
        }
        if (outputFormat != null) {
            currentArray.setPrintFormat(outputFormat);
        }
    }

    /**
     * Starts parsing of a structured array field that has a length variable.
     * 
     * @param name the name of the array
     * @param lengthVar the name of the product field containing the array
     *            length
     * @param fswName the FSW name for the array (may be null)
     * @param outputFormat the output format for displaying the contents of the
     *            array
     * @throws SAXException if the lengthVar does not have proper data type to be
     * a length prefix field
     */
    protected void startArray(final String name, final String lengthVar,
            final String fswName, final String outputFormat) throws SAXException {
        isInArray = true;
        
        final IProductDecomField elem = elementMap.get(lengthVar);
        if (elem != null) {
            if (elem instanceof ISimpleField) {
                final ISimpleField field = (ISimpleField) elem;
                if (!field.getDataType().isValueFieldLengthType()) {
                    throw new SAXException(
                            elem.getFswName()
                                    + " has an illegal field type for dynamic array length field");
                }
            } else {
                throw new SAXException(
                        elem.getFswName()
                                + " has an illegal field type for dynamic array length field");
            }
        }
        if (elem == null) {
            throw new SAXException(
                    lengthVar + " dynamic_array length field not found in preceding element definitions");
        }
        
        currentArray = fieldFactory.createArrayField(name, (ISimpleField) elem);
        containerStack.push(currentArray);
        if (fswName != null && fswName.length() != 0) {
            currentArray.setFswName(fswName);
        }
        if (outputFormat != null) {
            currentArray.setPrintFormat(outputFormat);
        }
    }

    /**
     * Starts parsing of a structured array field that has a length prefix in
     * the data field.
     * 
     * @param name the name of the array
     * @param fswName the FSW name for the array (may be null)
     * @param outputFormat the output format for displaying the contents of the
     *            array
     * @param prefixBitLen the bit length of the length prefix in the data
     */
    protected void startArray(final String name, final String fswName,
            final String outputFormat, final int prefixBitLen) {
        isInArray = true;
        currentArray = fieldFactory.createArrayField(name, -1, prefixBitLen / 8);
        containerStack.push(currentArray);
        if (fswName != null && fswName.length() != 0) {
            currentArray.setFswName(fswName);
        }
        if (outputFormat != null) {
            currentArray.setPrintFormat(outputFormat);
        }
    }

    /**
     * Ends parsing of the current structured array field and attaches it to the
     * current product definition.
     */
    protected void endArray() {
        containerStack.pop();
        final IFieldContainer container = containerStack.peek();
        container.addField(currentArray);
        currentArray = (IArrayField)findOnStack(IArrayField.class);
        isInArray = currentArray != null;
    }


    /**
     * Indicates whether a structured array definition is currently being
     * parsed.
     *
     * @return true if parsing an array definition
     */
    protected boolean inArray() {
        return isInArray;
    }

    /**
     * Starts parsing of a definition field.
     * 
     * @param name the name of the field
     * @param datatype the DataDictionaryType of the field
     * @param fswName the FSW name for the field (may be null)
     * @param isChannel flag indicating whether this is a field in the channel
     *            dictionary
     */
    protected void startField(final String name, final DecomDataType datatype,
            final String fswName, final boolean isChannel) {
        isInField = true;
        currentField = fieldFactory.createSimpleField(name, datatype);
        if (fswName != null && fswName.length() != 0) {
            currentField.setFswName(fswName);
            elementMap.put(fswName, currentField);
        } else {
            elementMap.put(name, currentField);
        }
        (currentField).setIsChannel(isChannel);
    }

    /**
     * Starts parsing of a definition field.
     * 
     * @param name the name of the field
     * @param datatype the DataDictionaryType of the field
     * @param fswName the FSW name for the field (may be null)
     * @param isChannel flag indicating whether this is a field in the channel
     *            dictionary
     * @param prefixLength of the prefix field in bits (for fields preceded by length)
     */
    protected void startField(final String name, final DecomDataType datatype,
            final String fswName, final boolean isChannel, final int prefixLength) {
        isInField = true;
        currentField = fieldFactory.createSimpleField(name, datatype, prefixLength / 8);
        if (fswName != null && fswName.length() != 0) {
            currentField.setFswName(fswName);
            elementMap.put(fswName, currentField);
        } else {
            elementMap.put(name, currentField);
        }
        (currentField).setIsChannel(isChannel);
    }

    /**
     * Ends parsing of the current definition field and attaches it to the
     * current array or product definition.
     * 
     */
    protected void endField() {
        final IFieldContainer container = containerStack.peek();
        container.addField(currentField);
        isInField = false;
        currentField = null;
    }

    /**
     * Indicates whether a definition field is currently being parsed.
     * 
     * @return true if a definition field is being parsed
     */
    protected boolean inField() {
        return isInField;
    }

    /**
     * Starts building a state table mapping (unnamed enumeration).
     */
    protected void startStates() {
        currentEnumDef = new EnumerationDefinition("no-name");
        isInStates = true;
    }

    /**
     * Starts parsing of a structure field (not structure definition).
     * 
     * @param name the GDS name of the field
     * @param sourceType the source structure definition
     * @param fswName the FSW name of the field
     */
    protected void startStructureField(final String name, final IStructureField sourceType,
            final String fswName) {
        isInStructureField = true;
        currentStructureField = copyStructure(sourceType);
        currentStructureField.setName(name);
        currentStructureField.setFswName(fswName);
    }

    
    /**
     * Creates a copy of the current StructureField. Not a deep copy.
     * 
     * @return the new StructureField.
     */
    private IStructureField copyStructure(final IStructureField source) {
        final IStructureField result = fieldFactory.createStructureField(source.getName());
        result.setFswName(source.getFswName());
        result.setFswDescription(source.getFswDescription());
        result.setPrintFormat(source.getPrintFormat());
        result.setSuppressName(source.getSuppressName());
        result.setSysDescription(source.getSysDescription());
        final List<IProductDecomField> elements = source.getFields();
        if (elements != null) {
            for (final IProductDecomField f: elements) {
                result.addField(f);
            }
        }
        return result;
    }
    /**
     * Indicates whether a structure field (not structure definition) is
     * currently being parsed.
     * 
     * @return true if a structure field is being parsed
     */
    protected boolean inStructureField() {
        return isInStructureField;
    }

    /**
     * Ends parsing of a structure field (not a structure definition).
     */
    protected void endStructureField() {
        final IFieldContainer container = containerStack.peek();
        container.addField(currentStructureField);
        isInStructureField = false;
        currentStructureField = null;
    }

    /**
     * Indicates if we are currently building a state table mapping.
     *
     * @return true if building a state table
     */
    protected boolean inStates() {
        return isInStates;
    }

    /**
     * Ends building of a state table mapping (unnamed enumeration).
     */
    protected void endStates() {
        isInStates = false;
        definedEnums.put(currentEnumDef.getName(), currentEnumDef);
        if (inBitField()) {
            (currentBitField).setLookupTable(currentEnumDef);
        } else if (inField()) {
            (currentField).setLookupTable(currentEnumDef);
        }
        currentEnumDef = null;
    }

    /**
     * Starts parsing of a tabular DN to EU interpolation.
     */
    protected void startDnToEu() {
        isInDnToEu = true;
    }

    /**
     * Indicates whether a tabular DN to EU interpolation is being parsed.
     * 
     * @return true if in a tabular DN to EU definition
     */
    protected boolean inDnToEu() {
        return isInDnToEu;
    }

    /**
     * Adds a DN to EU mapping to the current DN to EU interpolation table, if
     * one exists. If not, does nothing.
     * 
     * @param dn the data number to add
     * @param eu the corresponding engineering units to add
     */
    protected void addTableDnToEu(final double dn, final double eu) {
       
        dnTable.add(dn);
        euTable.add(eu);
    }

    /**
     * Ends parsing of the current tabular DN to EU interpolation table.
     */
    protected void endTableDnToEu() {
        
        final ITableEUDefinition dnToEu = EUDefinitionFactory.createTableEU(dnTable, euTable);
        if (inBitField()) {
            (currentBitField).setDnToEu(dnToEu);
        } else if (inField()) {
            (currentField).setDnToEu(dnToEu);
        }
        dnTable.clear();
        euTable.clear();
        isInDnToEu = false;
    }

    /**
     * Sets the current DN to EU polynomial index
     * 
     * @param index the index to set
     */
    protected void setPolyDnToEuIndex(final int index) {
        currentPolyIndex = index;
    }

    /**
     * Sets the coefficient to the current DN to EU polynomial at the current
     * polynomial index, if a polynomial exists. If not, does nothing.
     * 
     * @param coeff the coefficient to add
     */
    protected void setPolyDnToEuCoefficient(final double coeff) {
        while (coeffTable.size() - 1 < currentPolyIndex) {
            coeffTable.add(null);
        }
        coeffTable.set(currentPolyIndex, coeff);
    }

    /**
     * Ends parsing of the current polynomial DN to EU.
     */
    protected void endPolyDnToEu() {
        
    	try {
	        final IPolynomialEUDefinition dnToEu = EUDefinitionFactory.createPolynomialEU(coeffTable); 
	        if (inBitField()) {
	            (currentBitField).setDnToEu(dnToEu);
	        } else if (inField()) {
	            (currentField).setDnToEu(dnToEu);
	        }
    	}
        catch (final NullPointerException npe) {
        	logger.error("A polynomial coefficient is missing from the product dictionary", npe.getCause());

        }
        coeffTable.clear();
        isInDnToEu = false;
    }
    
    /**
     * Sets the class name of the custom EU algorithm definition currently being parsed.
     * @param className the full name of the Java class that computes the EU
     */
    protected void setAlgorithmClassName(final String className) {
        algoClassName = className;
    }
    
    /**
     * Ends parsing of the current algorithmic DN to EU.
     */
    protected void endAlgorithmicDnToEu() {
        final IAlgorithmicEUDefinition dnToEu = EUDefinitionFactory.createAlgorithmicEU(algoClassName);
        if (inBitField()) {
            (currentBitField).setDnToEu(dnToEu);
        } else if (inField()) {
            (currentField).setDnToEu(dnToEu);
        }
        algoClassName = null;
        isInDnToEu = false;
    }

    /**
     * Starts parsing of a bit array definition field.
     * 
     * @param name the name of the bit array
     * @param bitlen the length in bits of the bit array
     * @param fswName the FSW name for the bit array; may be null
     */
    protected void startBitArray(final String name, final int bitlen,
            final String fswName) {
        inBitArray = true;
        currentBitArray = fieldFactory.createBitArrayField(name, bitlen);
        if (fswName != null && fswName.length() != 0) {
            currentBitArray.setFswName(fswName);
        }
    }

    /**
     * Ends parsing of the current bit array definition and attaches it either
     * to the current structured array definition or the current product
     * definition.
     * 
     */
    protected void endBitArray() {
        final IFieldContainer container = containerStack.peek();
        container.addField(currentBitArray);
        currentBitArray = null;
        inBitArray = false;
    }

    /**
     * Indicates whether we are currently parsing a bit array definition.
     * 
     * @return true if parsing bit array definition
     */
    protected boolean inBitArray() {
        return inBitArray;
    }

    /**
     * Starts parsing of a stream field definition.
     * 
     * @param name the name of the stream field
     * @param len the byte length of the stream field
     * @param display the display name of the stream field; may be null
     * @param fswName the FSW name of the stream field; may be null
     */
    protected void startStream(final String name, final int len,
            String display, final String fswName) {
        isInStream = true;
        if (display == null) {
            display = "none";
        }
        currentStreamField = fieldFactory.createStreamField(name, len, display);
        if (fswName != null && fswName.length() != 0) {
            currentStreamField.setFswName(fswName);
        }
    }

    /**
     * Ends parsing of the current stream field and attaches it to the current
     * structured array or product definition.
     * 
     */
    protected void endStream() {
        final IFieldContainer container = containerStack.peek();
        container.addField(currentStreamField);
        isInStream = false;
        currentStreamField = null;
    }

    /**
     * Indicates if we are currently parsing a stream field definition.
     * 
     * @return true if parsing stream field definition
     */
    protected boolean inStream() {
        return isInStream;
    }

    /**
     * Starts parsing of a bit field definition.
     * 
     * @param name the name of the bit field
     * @param bitlen the length in bits of the bit field
     * @param fswName the FSW name for the bit field; may be null
     */
    protected void startBitField(final String name, final int bitlen,
            final String fswName) {
        if (!inBitArray) {
            throw new IllegalStateException(
                    "Cannot include bitfield unless in bit array");
        }
        isInBitField = true;
        currentBitField = fieldFactory.createBitField(name, bitlen);
        if (fswName != null && fswName.length() != 0) {
            currentBitField.setFswName(fswName);
        }
    }

    /**
     * Ends parsing of a bit field and adds it to the current bit array
     * definition.
     * 
     */
    protected void endBitField() {
        isInBitField = false;
        currentBitArray.addField(currentBitField);
        currentBitField = null;
    }

    /**
     * Indicates if we are currently parsing a bit field definition.
     * 
     * @return true if parsing bit field definition
     */
    protected boolean inBitField() {
        return isInBitField;
    }

    /**
     * Gets an integer value from the specified SAX attributes object.
     * 
     * @param attr the Attributes object to get the value from
     * @param name the name of the attribute to get
     * @return the int value of the attribute
     * @throws SAXException if the attribute value is not an int
     */
    protected int getIntAttrValue(final Attributes attr, final String name)
                                                                           throws SAXException {
        int value = 0;
        try {
            final String strValue = attr.getValue(name);
            if (strValue != null) {
                value = Integer.parseInt(strValue);
            }
        } catch (final NumberFormatException e) {
            throw new SAXException("Expected '" + name
                    + "' attribute to be an integer, but found "
                    + attr.getValue(name));
        }
        return value;
    }

    /**
     * Gets a boolean value from the specified SAX attributes object.
     * 
     * @param attr the Attributes object to get the value from
     * @param name the name of the attribute to get
     * @return the boolean value of the attribute
     * @throws SAXException if the attribute value is not boolean
     */
    protected boolean getBooleanAttrValue(final Attributes attr, final String name) throws SAXException {
        boolean value = false;
        try {
            final String strValue = attr.getValue(name);
            if (strValue != null) {
                value = GDR.parse_boolean(strValue);
            }
        } catch (final NumberFormatException e) {
            throw new SAXException("Expected '" + name
                    + "' attribute to be a boolean, but found "
                    + attr.getValue(name));
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] chars, final int start, final int length) throws SAXException {

        if (text == null) {
            text = new StringBuffer();
        }
        final String newText = new String(chars, start, length);
        if (!newText.equals("\n")) {
            text.append(newText);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        throw new SAXException("Parse error in product definition file line "
                + e.getLineNumber() + " col " + e.getColumnNumber() + ": "
                + e.getMessage());
    }


    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        throw new SAXException(
                "Fatal parse error in product definition file line "
                        + e.getLineNumber() + " col " + e.getColumnNumber()
                        + ": " + e.getMessage());
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) {
        logger.warn("Parse warning in product definition file line "
                        + e.getLineNumber() + " col " + e.getColumnNumber()
                        + ": " + e.getMessage());
    }

    /**
     * {@inheritDoc}
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qname, final Attributes attr) throws SAXException {
        text = null;
    }

    /**
     * Gets the value of the current text buffer created by the SAX parser.
     * 
     * @return the String value of the buffer
     */
    protected String getTextBuffer() {
        if (text == null) {
            return "";
        } else {
            return text.toString().trim();
        }
    }

    /**
     * Gets the value of the current text buffer created by the SAX parser.
     * Returns null if the buffer is empty or contains only white space
     * 
     * @return the String value of the buffer; may be null
     */
    protected String getTextBufferNonEmpty() {
        if (text == null) {
            return null;
        } else {
            final String val = text.toString().trim();
            if (val.length() == 0) {
                return null;
            } else {
                return val;
            }
        }
    }

    /**
     * Find the most recent instance of an object with the given class on the
     * container stack.
     *
     * an Iterator on a Stack starts from
     * the bottom of the stack and works its way up to the top. In order to get the
     * "highest on the stack" item, you can't just take the first one you find,
     * you have to take the last one.
     * 
     * @param c the Object class to find
     * @return the most recent (highest on the stack) Object instance, or null
     *         if no object of that class found on the stack
     */
    protected IFieldContainer findOnStack(final Class<?> c) {
        final Iterator<IFieldContainer> it = containerStack
                .iterator();
        IFieldContainer highest = null;
        while (it.hasNext()) {
            final IFieldContainer def = it.next();
            if (c.isAssignableFrom(def.getClass())) {
                highest = def;
            }
        }
        return highest;
    }
    
    /**
     * 
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinitionBuilder#reset()
     */
    @Override
	public void reset() {

        text = null;
        isInProduct = false;
        isInField = false;
        isInArray = false;
        isInStates = false;
        inBitArray = false;
        isInBitField = false;
        isInStream = false;
        isInEnumDef = false;
        isInStructure = false;
        isInStructureField = false;
        stateId = 0;
        currentDefinition = null;
        currentField = null;
        currentArray = null;
        currentBitArray = null;
        currentBitField = null;
        currentStreamField = null;
        currentStructure = null;
        currentStructureField = null;
        containerStack.clear(); 
        elementMap.clear(); 
        isInDnToEu = false;
        dnTable.clear();
        euTable.clear();
        currentPolyIndex = 0;
        coeffTable.clear();
        currentEnumDef = null;
        definedEnums.clear();
        definedStructs.clear();
        currentDpo = null;
        algoClassName = null;
        
    }

    /**
     * Starts parsing of a data product object.
     */
    protected void startDpo() {
    	isInDpo = true;
    	currentDpo = createProductObjectDefinition();
    	containerStack.push(currentDpo);
    }

    /**
     * Indicates if the parser is in a data product object definition.
     * 
     * @return true if currently parsing a data product object definition
     */
    protected boolean inDpo() {
    	return isInDpo;
    }

    /**
     * Ends parsing of a data product object.
     */
    protected void endDpo() {
    	containerStack.pop();
    	final IProductObjectDefinition newDpo = (IProductObjectDefinition) findOnStack(IProductObjectDefinition.class);
    	if (newDpo != null) {
    		currentDpo = newDpo;
    		isInDpo = true;
    	} else {
    		isInDpo = false;
    	}
    }

    /**
     * Allows access to the container stack being used for xml parsing
     * @return the containerStack object
     */
    protected Stack<IFieldContainer> getContainerStack() {
        return containerStack;
    }
}
