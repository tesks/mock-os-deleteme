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
/**
 * 
 */
package jpl.gds.dictionary.impl.decom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.decom.IChannelDecomDictionary;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapId;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This class is responsible for loading all of the XML files associated with
 * generic decom, including the general APID map file and all the associated
 * decom maps it points to. It places these parsed maps into a table for later
 * access by APID. There can be one general map that has no APID. This class
 * follows a singleton design pattern.
 * 
 */
public final class MultimissionChannelDecomDictionary extends AbstractBaseDictionary implements IChannelDecomDictionary {
    
    /* Get schema version from config */
    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.DECOM);


    private static final int INITIAL_TABLE_SIZE = 16;

    /*  Added members to ensure the XML is to the proper schema. */
    private static String ROOT_ELEMENT_NAME = "ApidMapTable";
    private static String HEADER_ELEMENT_NAME = "header";   

    private List<String> decomMapFilesToParse = new LinkedList<String>();
    private final Map<Integer, IDecomMapDefinition> decomMapsByApid;
    private IDecomMapDefinition generalMap; // the "default" map
    private IDecomMapDefinition decomMap; 
    private final  Map<Integer, IDecomMapDefinition> allMaps = new HashMap<Integer,IDecomMapDefinition>();
    private Map<String, IChannelDefinition> channelDefinitionMap = new HashMap<String, IChannelDefinition>();

    /**
     * Constructor. 
     */
    /* package */ MultimissionChannelDecomDictionary() {
        super(DictionaryType.DECOM, MM_SCHEMA_VERSION);

        this.decomMapFilesToParse = new ArrayList<String>(INITIAL_TABLE_SIZE);
        this.decomMapsByApid = new HashMap<Integer, IDecomMapDefinition>(INITIAL_TABLE_SIZE);
        this.generalMap = null;
        this.decomMap = null;
    }
    
    @Override
    public void parse(final String apidMapTableXmlPath, final DictionaryProperties config)
            throws DictionaryException {
        parse(apidMapTableXmlPath, config, tracer);
    }

    @Override
    public void parse(final String apidMapTableXmlPath, final DictionaryProperties config, final Tracer tracer)
            throws DictionaryException {
        super.setDictionaryConfiguration(config);

        /*
         * Use method in super class to parse the top-level file.
         */
        super.parse(apidMapTableXmlPath, config, tracer);

        for (final String mapXmlRelativePath : this.decomMapFilesToParse) {

            /*
             * Allow decom files to be loaded from
             * either the dictionary directory or config directory based upon
             * GDS configuration flag.
             */
            String qualifiedMapXmlPath = null;

            /*
             * Use DictionaryConfiguration to
             * determine search path
             */
            if (getDictionaryConfiguration().getDictionarySearchPath(DictionaryType.DECOM)
                    .equals(DictionaryProperties.DICTIONARY_PATH)) {
                qualifiedMapXmlPath = getDictionaryConfiguration().getDictionaryFile(mapXmlRelativePath);
            } else {
                qualifiedMapXmlPath = GdsSystemProperties.getMostLocalPath(mapXmlRelativePath,
                                                                           config.isDictionarySseEnabled());
            }

            try {
                /*
                 *  Use already-existing method to
                 * parse and add the decom map
                 */
                addDecomMapFromFile(qualifiedMapXmlPath);

            } catch (final DictionaryException de) {
                tracer.error("Could not load decom map file " + mapXmlRelativePath + ". " + de.getMessage());

            }

        }

        tracer.info("Finished parsing " + this.decomMapFilesToParse.size() + " Generic Decom Map files");
    }

    @Override
    public void addMap(final int apid, final IDecomMapDefinition map) {

        if (apid < 0) {
            throw new IllegalArgumentException("Illegal APID: " + apid
                    + " (cannot be negative)");
        }

        if (this.decomMapsByApid.containsKey(apid)) {
            // throw new
            // IllegalArgumentException("APID clash: decom map for APID "
            // + apid + " already exists in the table");

            // As a result of session restart feature, we need to be able to
            // replace existing map entries.
            tracer.debug("Replacing decom map for APID " + apid);
            this.decomMapsByApid.remove(apid);
        }

        if (map == null) {
            throw new IllegalArgumentException("Cannot insert null decom map "
                    + "into the table (APID " + apid);
        }

        this.decomMapsByApid.put(apid, map);
        this.allMaps.put(allMaps.size(), map);
    }

    @Override
    public void setGeneralMap(final IDecomMapDefinition map) {

        /*
         * Removed code to prevent setting
         * the general map again. It was more trouble than it was worth
         * because one could never load the decom map more than once.
         */
        if (map == null) {
            throw new IllegalArgumentException("Cannot insert null general "
                    + "decom map into the table");
        }

        this.generalMap = map;
        this.allMaps.put(allMaps.size(), map);
    }

    @Override
    public IDecomMapDefinition getDecomMapByApid(final int apid) {

        final IDecomMapDefinition map = this.decomMapsByApid.get(apid);

        if (map == null) {
            return this.generalMap;
        } else {
            return map;
        }

    }

    @Override
    public IDecomMapDefinition getGeneralDecomMap() {

        return this.generalMap;
    }

    @Override
    public void setChannelMap(final Map<String, IChannelDefinition> chanMap) {
        if (chanMap == null) {
            throw new IllegalArgumentException("Channel Definition map cannot be null.");
        }
        this.channelDefinitionMap = new HashMap<String, IChannelDefinition>(chanMap);        
    }

    @Override
    public IDecomMapDefinition addDecomMapFromFile(final String filename) throws DictionaryException {

        if (filename == null) {
            throw new DictionaryException("Decom Map file cannot be null.");
        }
        
        final OldDecomMapParser parser = new OldDecomMapParser(this.channelDefinitionMap);
        parser.parseDecomMapXml(filename);
       
        this.decomMap = parser.getMap();
        if (decomMap.getApid() == -1) {
            setGeneralMap(decomMap);
        } else {
            addMap(decomMap.getApid(), decomMap);
        }
        return this.decomMap;
    }

    @Override
    public Map<Integer, IDecomMapDefinition> getAllDecomMaps() {
        return this.allMaps;
    }

    @Override
    public void clear() {
        this.decomMapFilesToParse.clear();
        this.decomMapsByApid.clear();
        this.channelDefinitionMap.clear();
        this.allMaps.clear();
        this.generalMap = null;
        this.decomMap = null;
        super.clear();
    }
    

   @Override
   public void startDocument() throws SAXException {
       super.startDocument();
       /* Set starting required elements */
       setRequiredElements("Multimission", 
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

        super.startElement(uri, localName, qName, attributes);
        
        /* Use common method to parse and report header info */
        parseMultimissionHeader(qName, attributes);


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
        super.endElement(uri, localName, qName);
        final String ntext = XmlUtility.normalizeWhitespace(this.text);
        if ("ApidMap".equals(qName)) {
            decomMapFilesToParse.add(ntext);
        } else if ("GeneralMap".equals(qName)) {
            decomMapFilesToParse.add(ntext);
        }
    }


	@Override
	public IDecomMapDefinition addDecomPacketMap(final int apid) {
		return decomMapsByApid.get(apid);
	}


	@Override
	public IDecomMapDefinition getDecomMapById(final IDecomMapId id) {
		int apid;
		try {
			apid = Integer.parseInt(id.getLocalName());
		} catch (final NumberFormatException e) {
			return null;
		}
		return decomMapsByApid.get(apid);
	}


}