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
package ammos.datagen.evr.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ammos.datagen.config.AbstractMissionConfiguration;
import ammos.datagen.config.IMissionConfiguration;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is the EvrMissionConfiguration class for the AMPCS data generators. It
 * parses an XML file that contains configuration information that is specific
 * to a mission does not generally change from run to run of the EVR generator.
 * The XML file containing the configuration is verified against its schema
 * before loading it. After the XML file is loaded, the configuration values it
 * contained are available using various accessor methods.
 * 
 *
 */
public class EvrMissionConfiguration extends AbstractMissionConfiguration
        implements IMissionConfiguration {

    /**
     * Configuration property name for the EVR dictionary class (string)
     */
    public static final String EVR_DICTIONARY_CLASS = "EvrDictionaryClass";
    /**
     * Configuration property name for the EVR definition class (string)
     */
    public static final String EVR_DEFINITION_CLASS = "EvrDefinitionClass";

    private static final String SCHEMA_RELATIVE_PATH = "schema/EvrMissionConfig.rnc";

    private static final String LEVEL = "Level";
    private static final String APID = "apid";
    private static final String FATAL = "fatal";
    private static final String NAME = "name";

    private final Map<String, EvrLevel> evrLevels = new HashMap<String, EvrLevel>(
            10);

    /**
     * Constructor.
     */
    public EvrMissionConfiguration() {

        super("EVR Mission Configuration", SCHEMA_RELATIVE_PATH);
    }

    /**
     * Loads the configuration file.
     * 
     * @param uri
     *            the path to the configuration file.
     * 
     * @return true if the file was successfully loaded, false if not.
     */
    public boolean load(final String uri) {

        return super.load(uri, new EvrMissionConfigurationParseHandler());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.AbstractXmlConfiguration#clear()
     */
    @Override
    public void clear() {

        super.clear();
        this.evrLevels.clear();
    }

    /**
     * Gets the list of EvrLevel objects created from the configuration.
     * 
     * @return List of EvrLevel
     */
    public List<EvrLevel> getEvrLevels() {

        final List<EvrLevel> l = new LinkedList<EvrLevel>(
                this.evrLevels.values());
        return Collections.unmodifiableList(l);
    }

    /**
     * Gets the configured APID for EVRs of a specified level.
     * 
     * @param level
     *            the EVR level string
     * 
     * @return the EVR apid
     * 
     * @throws IllegalArgumentException
     *             if the specified EVR level is not configured
     */
    public int getEvrLevelApid(final String level)
            throws IllegalArgumentException {

        final EvrLevel l = this.evrLevels.get(level);
        if (l == null) {
            throw new IllegalArgumentException("EVR level " + level
                    + " not recognized");
        }
        return l.getLevelApid();
    }

    /**
     * Gets the EvrLevel object with the specific level.
     * 
     * @param level
     *            the EVR level string
     * 
     * @return matching EVR level object, or null if the specified EVR level is
     *         not configured
     */
    public EvrLevel getEvrLevel(final String level) {

        return this.evrLevels.get(level);
    }

    /**
     * This is the SAX parse handler class for the mission configuration file.
     * 
     *
     */
    private class EvrMissionConfigurationParseHandler extends
            MissionConfigurationParseHandler {


        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String namespaceURI,
                final String localname, final String rawName,
                final Attributes atts) throws SAXException {

            super.startElement(namespaceURI, localname, rawName, atts);

            if (localname.equalsIgnoreCase(LEVEL)) {
                final String levelName = atts.getValue(NAME).trim();
                final int levelApid = XmlUtility.getIntFromAttr(atts, APID);
                final boolean isFatal = XmlUtility.getBooleanFromAttr(atts,
                        FATAL);
                final EvrLevel level = new EvrLevel(levelName, levelApid,
                        isFatal);
                EvrMissionConfiguration.this.evrLevels.put(levelName, level);
            }
        }
    }
}
