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
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.decom.DecomMapDefinitionFactory;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapId;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * Multimission parser for multi-decom map files.  These files may define
 * more than one decom map; this parser will parse each decom map and makes
 * the collection available to the owner of the parser.
 *
 */
public class MultimissionMultiDecomMapParser extends DefaultHandler {

    private static final String DECOM_MAP = "decom_map";

	private static final String DECOM_MAPS = "decom_maps";

    private static final Tracer                         trace      = TraceManager
            .getTracer(Loggers.DICTIONARY);

    private final Map<IDecomMapId, IDecomMapDefinition> mapsById = new HashMap<>();
    private IDecomMapDefinition currentMap;
    private final String namespace;
    private MultimissionDecomMapParser parser;
    private GroundConfigurationsParser decomTablesParser;
    private XMLReader reader;

    /**
     * Create a new parser instance, which produces {@link IDecomMapDefinition} instances
     * in the given namespace.
     * @param namespace the namespace to assign new decom map definitions to
     */
	public MultimissionMultiDecomMapParser(final String namespace) {
		this.namespace = namespace;
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName,
			final Attributes attributes) throws SAXException {

		if (DECOM_MAPS.equals(qName)) {
			currentMap = DecomMapDefinitionFactory.createEmptyDecomMap();
			parser = new MultimissionDecomMapParser(namespace, currentMap, reader, this, true);
		} else if (DECOM_MAP.equals(qName)) {
			currentMap = DecomMapDefinitionFactory.createEmptyDecomMap();
			parser.setMap(currentMap);
			reader.setContentHandler(parser);
			parser.startElement(uri, localName, qName, attributes);
			if (decomTablesParser != null) {
				parser.addVariables(decomTablesParser.getStatements());
			}
		} else if (decomTablesParser.getRootElement().equals(qName)) {
			reader.setContentHandler(decomTablesParser);
			decomTablesParser.startElement(uri, localName, qName, attributes);
		}

	}
	
	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		if (DECOM_MAPS.equals(qName)) {
			mapsById.put(new DecomMapId(namespace, currentMap.getName()), currentMap);
		} else if (parser.getRootElement().equals(qName)) {
			mapsById.put(new DecomMapId(namespace, currentMap.getName()), currentMap);
		}
		
	}
	
	/**
	 * Get the map of parsed maps.
	 * @return the map of decom map IDs to decom map definitions
	 */
	public Map<IDecomMapId, IDecomMapDefinition> collectMaps() {
		return mapsById;
	}

	/**
	 * Parse the given file, which should be an XML file that defines a block of decom maps
	 * @param filename the path to the file to parse
	 * @throws DictionaryException if the file is not found or if a parsing error occurs
	 */
	public void parseFile(final String filename) throws DictionaryException {
		final File path = new File(filename);
        trace.debug("Parsing Generic Decom Map XML at "
                + path.getAbsolutePath());
        SAXParser sp = null;

        try {
            sp = SAXParserPool.getInstance().getNonPooledParser();
            this.reader = sp.getXMLReader();
            decomTablesParser = new GroundConfigurationsParser(reader, this, true);
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
	}
	
}
