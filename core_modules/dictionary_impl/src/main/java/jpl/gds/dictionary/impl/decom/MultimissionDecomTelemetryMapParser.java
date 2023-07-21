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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.decom.DecomMapDefinitionFactory;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.impl.StartingRequiredElementTracker;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * Multimission parser for decom packet maps.  These maps specifically define a map associated with a
 * packet APID and contain only one decom map.
 *
 */
public class MultimissionDecomTelemetryMapParser extends DefaultHandler {


    private static final Tracer                  trace               = TraceManager
            .getTracer(Loggers.DICTIONARY);

	private static String ROOT_ELEMENT_NAME = "decom_telemetry_definition";
    private static String HEADER_ELEMENT_NAME = "header";   
    private static String TYPE_ATTR = "type";
    String currentType;

    private final StartingRequiredElementTracker requiredElementTracker; 
	
	private final IDecomMapDefinition currentMap;
	
    private XMLReader reader;
    
    private MultimissionDecomMapParser decomMapParser;
    private GroundConfigurationsParser groundConfigurationsParser;

    /**
     * Create a new parser.
     */
	public MultimissionDecomTelemetryMapParser() {
		currentMap = DecomMapDefinitionFactory.createEmptyDecomMap();
        requiredElementTracker = new StartingRequiredElementTracker("Multimission", DictionaryType.DECOM, 
                Arrays.asList(new String [] {ROOT_ELEMENT_NAME, HEADER_ELEMENT_NAME}));
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

		requiredElementTracker.checkState(localName);

		if (ROOT_ELEMENT_NAME.equals(qName)) {
			final String apid = attributes.getValue("apid");
			if (apid == null) {
				this.currentMap.setGeneral(true);
			} else {
				final int currentApid = XmlUtility.getIntFromText(apid);
				this.currentMap.setApid(currentApid);
			}
			final String currentType = attributes.getValue(TYPE_ATTR);
			if (currentType == null) {
				throw new SAXException(String.format("Decom telemetry definition does not include required element %s", TYPE_ATTR));
			}
			decomMapParser = new MultimissionDecomMapParser(currentType, currentMap, reader, this, false);
		} else if (groundConfigurationsParser.getRootElement().equals(qName)) {
			reader.setContentHandler(groundConfigurationsParser);
			groundConfigurationsParser.startElement(uri, localName, qName, attributes);

		} else if (decomMapParser.getRootElement().equals(qName)) {
			reader.setContentHandler(decomMapParser);
			if (groundConfigurationsParser != null) {
				decomMapParser.addVariables(groundConfigurationsParser.getStatements());
			}
			decomMapParser.startElement(uri, localName, qName, attributes);
		}
	}

	/**
	 * 
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName)
            throws SAXException {
    	if(ROOT_ELEMENT_NAME.equals(qName)) {
    		// This means the decom map parser has returned control.
    		// Do nothing
    	}
    }

	/**
	 * Parse the file at the given path.
	 * @param filename the path to a decom packet map
	 * @return the definition object that results from parsing
	 * @throws DictionaryException if the file does not exist or a parsing error occurs
	 */
	public IDecomMapDefinition parseFile(final String filename) throws DictionaryException {
		final File path = new File(filename);
        trace.debug("Parsing Generic Decom Map XML at "
                + path.getAbsolutePath());
        SAXParser sp = null;

        try {
            sp = SAXParserPool.getInstance().getNonPooledParser();
            this.reader = sp.getXMLReader();
            groundConfigurationsParser = new GroundConfigurationsParser(reader, this, true);
            sp.parse(path, this);

        } catch (final FileNotFoundException e) {
            throw new DictionaryException("Packet Generic Decom Map file "
                    + path.getAbsolutePath() + " not found", e);

        } catch (final IOException e) {
            throw new DictionaryException(
                    "IO Error reading Packet Generic Decom Map XML file "
                            + path.getAbsolutePath(), e);

        } catch (final ParserConfigurationException e) {
            throw new DictionaryException(
                    "Error configuring SAX parser for Packet Generic Decom Map file "
                            + path.getAbsolutePath(), e);
            
        } catch (final SAXException e) {
            trace.error("Error parsing Generic Decom Map file "
                    + path.getAbsolutePath() + ": " + e.getMessage());
            throw new DictionaryException(e.getMessage(), e);

        }
		return currentMap;
	}

}
