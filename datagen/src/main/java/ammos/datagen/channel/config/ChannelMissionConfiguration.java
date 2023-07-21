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
package ammos.datagen.channel.config;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ammos.datagen.config.AbstractMissionConfiguration;
import ammos.datagen.config.IMissionConfiguration;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is the ChannelMissionConfiguration class for the AMPCS data generators.
 * It parses an XML file that contains configuration information that is
 * specific to a mission does not generally change from run to run of the EVR
 * generator. The XML file containing the configuration is verified against its
 * schema before loading it. After the XML file is loaded, the configuration
 * values it contained are available using various accessor methods.
 * 
 *
 */
public class ChannelMissionConfiguration extends AbstractMissionConfiguration
        implements IMissionConfiguration {

    /**
     * Configuration property name for the channel dictionary class (string)
     */
    public static final String CHANNEL_DICTIONARY_CLASS = "ChannelDictionaryClass";

    private static final String SCHEMA_RELATIVE_PATH = "schema/ChannelMissionConfig.rnc";
    private static final String PACKET_TYPE = "PacketType";
    private static final String APID = "apid";
    private static final String PRE_CHANNELIZED = "prechannelized";

    private final List<ChannelPacketType> packetTypes = new LinkedList<ChannelPacketType>();

    /**
     * Constructor.
     */
    public ChannelMissionConfiguration() {

        super("Channel Mission Configuration", SCHEMA_RELATIVE_PATH);
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

        return super.load(uri, new ChannelMissionConfigurationParseHandler());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.AbstractXmlConfiguration#clear()
     */
    @Override
    public void clear() {

        super.clear();
        this.packetTypes.clear();
    }

    /**
     * Gets the list of ChannelPacketType objects created from the
     * configuration.
     * 
     * @return List of ChannelPacketType
     */
    public List<ChannelPacketType> getChannelPacketTypes() {

        return Collections.unmodifiableList(this.packetTypes);
    }

    /**
     * This is the SAX parse handler class for the mission configuration file.
     * 
     *
     */
    private class ChannelMissionConfigurationParseHandler extends
            MissionConfigurationParseHandler {

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String namespaceURI,
                final String localname, final String rawName,
                final Attributes atts) throws SAXException {

            super.startElement(namespaceURI, localname, rawName, atts);

            /*
             * Found a packet type definition. Build a ChannelPacketType object
             * for it and add it to the list of packet types.
             */
            if (localname.equalsIgnoreCase(PACKET_TYPE)) {
                final int apid = XmlUtility.getIntFromAttr(atts, APID);
                boolean isPrechan = false;
                if (atts.getValue(PRE_CHANNELIZED) != null) {
                    isPrechan = XmlUtility.getBooleanFromAttr(atts,
                            PRE_CHANNELIZED);
                }
                final ChannelPacketType type = new ChannelPacketType(apid,
                        isPrechan);
                ChannelMissionConfiguration.this.packetTypes.add(type);
            }
        }

    }
}
