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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.decom.DecomMapDefinitionFactory;
import jpl.gds.dictionary.api.decom.IChannelStatementDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomStatementFactory;
import jpl.gds.dictionary.api.decom.IOffsetStatementDefinition;
import jpl.gds.dictionary.api.decom.IStatementContainer;
import jpl.gds.dictionary.api.decom.ISwitchStatementDefinition;
import jpl.gds.dictionary.api.decom.IVariableStatementDefinition;
import jpl.gds.dictionary.api.decom.IWidthStatementDefinition;
import jpl.gds.dictionary.impl.StartingRequiredElementTracker;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * Parses a single decom map file into an IDecomMapDefinition object. Following
 * parsing, the DecomMap object can be accessed using getMap(), or an empty
 * instance to populate can be supplied in the constructor.
 * 
 */
public class OldDecomMapParser extends DefaultHandler {

    private final static int INITIAL_ELEMENT_TEXT_SIZE = 1024;

    /* Change to external tracer so messages get logged in the DB */
    private static final Tracer                   trace                     = TraceManager
            .getTracer(Loggers.DICTIONARY);
    private StringBuilder text;

    private final Stack<Object> currentElementsDepthStack;
    private IOffsetStatementDefinition.OffsetType currentOffsetType;
    private final Set<String> declaredVariables;
    private String filename;

    /* Added members to ensure the XML is to the proper schema. */
    private static String ROOT_ELEMENT_NAME = "DecomMapRoot";
    private static String HEADER_ELEMENT_NAME = "header";   
    private final StartingRequiredElementTracker requiredElementTracker; 
    private final IDecomStatementFactory statementFactory = IDecomStatementFactory.newInstance();

    /*
     * 11/21/13 - currentApid and currentMapIsGeneral members replaced by
     * a currentMap member.
     */
    private final IDecomMapDefinition currentMap;


    private final Map<String, IChannelDefinition> chanMap;


    /**
     * Constructor that takes a map definition to populate and a channel map.
     * The map parser will look for channel definitions in the supplied map.
     * Parsed information will be assigned to the supplied IDecomMapDefinition
     * object.
     * 
     * @param toParseInto
     *            the IDecomMapDefinition to populate
     * @param chanMap
     *            a Map of string channel ID to channel definition
     * 
     */
    OldDecomMapParser(final IDecomMapDefinition toParseInto, final Map<String, IChannelDefinition> chanMap) {

        if (toParseInto == null) {
            throw new IllegalArgumentException("Decom map definition may not be null");
        }
        if (chanMap == null) {
            throw new IllegalArgumentException("Channel map may not be null");
        }
        this.currentMap = toParseInto;
        this.currentElementsDepthStack = new Stack<Object>();
        this.declaredVariables = new HashSet<String>();
        this.chanMap = chanMap;

        /* Add tracker for required elements */
        requiredElementTracker = new StartingRequiredElementTracker("Multimission", DictionaryType.DECOM, 
                Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
    }


    /**
     * Constructor that takes a channel map. The map parser will look for
     * channel definitions in the supplied map, and will also create a new
     * instance of IDecomMapDefinition to populate.
     * 
     * @param chanMap
     *            a Map of string channel ID to channel definition
     * 
     */
    public OldDecomMapParser(final Map<String, IChannelDefinition> chanMap) {
        this(DecomMapDefinitionFactory.createEmptyDecomMap(), chanMap);
    }

    /**
     * Parses the decom map XML file with the given uri and populates the current
     * IDecomMapDefinition, which can be fetched by calling getMap().
     * 
     * @param uri
     *            path to the decom map file
     * @throws DictionaryException
     *             if anything goes wrong
     * 
     * @see #getMap()
     */
    public void parseDecomMapXml(final String uri) throws DictionaryException {

        if (uri == null) {
            throw new DictionaryException(
                    "Generic Decom Map XML path is undefined. Not reading decom map");
        }

        /* Save filename for error messages */
        filename = uri;

        final File path = new File(uri);
        trace.debug("Parsing Generic Decom Map XML at "
                + path.getAbsolutePath());
        SAXParser sp = null;

        try {
            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(uri, this);

        } catch (final FileNotFoundException e) {
            throw new DictionaryException("Generic Decom Map file "
                    + path.getAbsolutePath() + " not found", e);

        } catch (final IOException e) {
            throw new DictionaryException(
                    "IO Error reading Generic Decom Map XML file "
                            + path.getAbsolutePath(), e);

        } catch (final ParserConfigurationException e) {
            throw new DictionaryException(
                    "Error configuring SAX parser for Generic Decom Map file "
                            + path.getAbsolutePath(), e);
            

        } catch (final SAXException e) {
            trace.error("Error parsing Generic Decom Map file "
                    + path.getAbsolutePath());
            throw new DictionaryException(e.getMessage(), e);

        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName,
            final Attributes attributes) throws SAXException {

        this.text = new StringBuilder(INITIAL_ELEMENT_TEXT_SIZE);

        /* Add required element check. */
        requiredElementTracker.checkState(localName);

        try {

            if ("DecomMap".equals(qName)) {
                final String mapName = attributes.getValue("name");
                final String apid = attributes.getValue("apid");
                currentMap.setId(new DecomMapId("", mapName));
                /* missionName no longer required on decom maps. */
                trace.debug("Parsing decom map: name="
                        + (mapName == null ? "null" : ("\"" + mapName + "\""))
                        + " apid="
                        + (apid == null ? "null" : apid));
                /*
                 * Create a current map object as a
                 * member rather than just having the map on the stack.
                 */

                /* Updated to use the decom map definition factory. */
                /* Use map specified in the constructor rather than creating one here.
                 */
                if (apid == null) {
                    this.currentMap.setGeneral(true);
                } else {
                    final int currentApid = XmlUtility.getIntFromText(apid);
                    this.currentMap.setApid(currentApid);
                }
                this.currentMap.setName(mapName);
                this.currentElementsDepthStack.push(this.currentMap);

            } else if ("Offset".equals(qName)) {
                final String offsetDirection = attributes
                        .getValue("relativeDirection");

                if (offsetDirection == null) {
                    this.currentOffsetType = IOffsetStatementDefinition.OffsetType.ABSOLUTE;
                } else if ("+".equals(offsetDirection)) {
                    this.currentOffsetType = IOffsetStatementDefinition.OffsetType.PLUS;
                } else if ("-".equals(offsetDirection)) {
                    this.currentOffsetType = IOffsetStatementDefinition.OffsetType.MINUS;
                }

            } else if ("Skip".equals(qName)) {
                final int skipBits = XmlUtility.getIntFromAttr(attributes, "numBits");
                ((IStatementContainer) this.currentElementsDepthStack.peek())
                .addStatement(statementFactory.createSkipStatement(skipBits));

            } else if ("Channel".equals(qName)) {
                final String cid = attributes.getValue("id");
                final String widthStr = attributes.getValue("width");
                final String offsetStr = attributes.getValue("offset");
                int width = -1;
                int offset = -1;

                if (widthStr != null) {
                    width = XmlUtility.getIntFromText(widthStr);
                }

                if (offsetStr != null) {
                    offset = XmlUtility.getIntFromText(offsetStr);
                }

                /*  Added channel map to constructor call. */
                final IChannelDefinition chanDef = chanMap.get(cid);
                if (chanDef == null) {
                    throw new SAXException("Channel " + cid + 
                            " referenced in decom map file " + filename + " does not appear to be in the channel dictionary");
                }
                final IChannelStatementDefinition cs = statementFactory.
                        createChannelStatement(chanDef, width, offset);
                ((IStatementContainer) this.currentElementsDepthStack.peek())
                .addStatement(cs);

            } else if ("Variable".equals(qName)) {
                final String name = attributes.getValue("name");
                final String widthStr = attributes.getValue("width");
                IVariableStatementDefinition vs;

                if (widthStr == null) {
                    // This is a variable to variable assignment.
                    vs = statementFactory.createVariableStatement(name,
                            attributes.getValue("assignValueFrom"));
                } else {
                    final int width = XmlUtility.getIntFromText(widthStr);
                    final String offsetStr = attributes.getValue("offset");
                    final int offset = offsetStr == null ? -1 : XmlUtility
                            .getIntFromText(offsetStr);
                    vs = statementFactory.createVariableStatement(name, width, offset);
                }

                ((IStatementContainer) this.currentElementsDepthStack.peek())
                .addStatement(vs);
                this.declaredVariables.add(name);

            } else if ("Switch".equals(qName)) {
                final String varName = attributes.getValue("variable");
                final String modulus = attributes.getValue("modulus");
                ISwitchStatementDefinition ss;

                if (modulus == null) {
                    ss = statementFactory.createSwitchStatement(varName);
                } else {
                    ss = statementFactory.createSwitchStatement(varName,
                            XmlUtility.getIntFromText(modulus));
                }

                if (!this.declaredVariables.contains(varName)) {
                    throw new SAXException("Switch variable " + varName
                            + " not declared earlier in the map");
                }

                ((IStatementContainer) this.currentElementsDepthStack.peek())
                .addStatement(ss);
                this.currentElementsDepthStack.push(ss);

            } else if ("Case".equals(qName)) {
                final String caseValStr = attributes.getValue("value");

                if (caseValStr == null) {
                    throw new SAXException(
                            "Case must specify a value attribute");
                }

                final long caseVal = GDR.parse_unsigned(caseValStr);

                if (caseVal < 0) {
                    throw new SAXException(
                            "Case value cannot be negative (got " + caseVal
                            + ")");
                }

                ((ISwitchStatementDefinition) this.currentElementsDepthStack.peek())
                .startPendingCase(caseVal);

            } else if ("Default".equals(qName)) {
                ((ISwitchStatementDefinition) this.currentElementsDepthStack.peek())
                .startDefaultCase();
            }

        } catch (final IllegalArgumentException iae) {
            throw new SAXException(
                    "Decom map parsing exception (startElement): "
                            + iae.getMessage());
        }

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

        final String ntext = XmlUtility.normalizeWhitespace(this.text);

        try {

            if ("DecomMap".equals(qName)) {
                /*
                 * 11/21/13. There used to be code here to add the map to
                 * the DecomMapTable. Now one must call getMap() to get it after
                 * parsing.
                 */
                this.currentElementsDepthStack.pop();

            } else if ("Width".equals(qName)) {
                final IWidthStatementDefinition ws = statementFactory.createWidthStatement(
                        XmlUtility.getIntFromText(ntext));
                ((IStatementContainer) this.currentElementsDepthStack.peek())
                .addStatement(ws);

            } else if ("Offset".equals(qName)) {
                IOffsetStatementDefinition os;

                if (this.currentOffsetType == null) {
                    throw new SAXException(
                            "Could not determine type of Offset statement");
                }

                if ("DATA".equals(ntext)) {

                    if (this.currentOffsetType != IOffsetStatementDefinition.OffsetType.ABSOLUTE) {
                        throw new SAXException(
                                "Offset to DATA but relativeDirection found");
                    }

                    os = statementFactory.createDataOffsetStatement();
                } else {
                    os = statementFactory.createOffsetStatement(XmlUtility.getIntFromText(ntext),
                            this.currentOffsetType);
                }

                ((IStatementContainer) this.currentElementsDepthStack.peek())
                .addStatement(os);
                this.currentOffsetType = null;

            } else if ("Statements".equals(qName)
                    && this.currentElementsDepthStack.peek() instanceof ISwitchStatementDefinition) {
                // This is the end of Statements inside a CaseSet or Case
                // element. Register the statements under those cases
                // (or as default case).
                final ISwitchStatementDefinition ss = (ISwitchStatementDefinition) this.currentElementsDepthStack
                        .peek();

                if (ss.hasCases()) {
                    ((ISwitchStatementDefinition) this.currentElementsDepthStack.peek())
                    .endPendingCases();
                } else {
                    // Must be default.
                    ss.endDefaultCase();
                }

            } else if ("Switch".equals(qName)) {
                this.currentElementsDepthStack.pop();

            }

        } catch (final IllegalArgumentException iae) {
            throw new SAXException("Decom map parsing exception (endElement): "
                    + iae.getMessage());
        }

    }

    /**
     * Gets the parsed IDecomMapDefinition object. Must be called after parseDecomMapXml().
     * 
     * @return IDecomMapDefinition
     * 
     *
     * @see #parseDecomMapXml(String)
     */
    public IDecomMapDefinition getMap() {

        return this.currentMap;
    }

}
