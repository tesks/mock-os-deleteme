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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ammos.datagen.config.AbstractRunConfiguration;
import ammos.datagen.config.IRunConfiguration;
import ammos.datagen.config.TraversalType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is the ChannelRunConfiguration class for the AMPCS data generators. It
 * parses an XML file that contains configuration information may change from
 * run to run of the channel generator. The XML file containing the
 * configuration is verified against its schema before loading it. After the XML
 * file is loaded, the configuration values it contained are available using
 * various accessor methods. <br>
 * It is important to note that the channel generator currently runs in three
 * basic modes: a RANDOM mode, in which channels are placed into packets
 * randomly regardless of APID, a BY_APID mode, which the the channels to be
 * placed into each packet are configured by APID by the channel values are
 * variable, and a CUSTOM mode, in which a specific sequence of custom packets
 * is generated, each having a specified APID and exact list of channel values.
 * In the first two modes, the configuration may also specify a packet rotation
 * count by APID. The mode can be quickly determined using getPacketMode(). <br>
 * In the RANDOM mode, getPacketApidCounts() may return a non-empty list, but
 * both getCustomPackets() and getChannelsPerApid() will return empty lists. <br>
 * In the BY_APID mode, getPacketApidCounts() may return a non-empty list,
 * getCustomPackets() will return an empty list, and getChannelsPerApid() will
 * return a non-empty list. <br>
 * In the CUSTOM mode, getPacketApidCounts() will return an empty list,
 * getCustomPackets() will return a non-empty list, and getChannelsPerApid()
 * will return an empty list.
 * 
 *
 */
public class ChannelRunConfiguration extends AbstractRunConfiguration implements
        IRunConfiguration {

    /**
     * Configuration property name for the channel ID pattern (string).
     */
    public static final String ID_PATTERN = "IdPattern";

    /**
     * Configuration property name for the channel name pattern (string).
     */
    public static final String NAME_PATTERN = "NamePattern";

    /**
     * Configuration property name for the channel module pattern (string).
     */
    public static final String MODULE_PATTERN = "ModulePattern";

    /**
     * Configuration property name for the channel operational category pattern
     * (string).
     */
    public static final String OPSCAT_PATTERN = "OpsCatPattern";

    /**
     * Configuration property name for the channel subsystem pattern (string).
     */
    public static final String SUBSYSTEM_PATTERN = "SubsystemPattern";

    /**
     * Configuration property name for the channel data type list (string).
     * - MPCS-6333 - 7/1/14. Added configuration constant.
     */
    public static final String TYPE_LIST = "DataTypes";

    /**
     * Configuration property name for the flag indicating whether to include
     * invalid channel indices in the channel packets (boolean).
     */
    public static final String INCLUDE_INVALID_INDICES = "IncludeInvalidIndices";

    /**
     * Configuration property name for the approximate percentage of packets
     * with invalid channel indices to generate (float).
     */
    public static final String INVALID_INDEX_PERCENT = "InvalidIndexPercent";
    /**
     * Configuration property name for the flag indicating whether to include
     * invalid EVR binary data (boolean).
     */
    public static final String INCLUDE_INVALID_PACKETS = "IncludeInvalidChannelPackets";
    /**
     * Configuration property name for the property indicating whether to
     * traverse the dictionary randomly or sequentially. (TraversalType).
     */
    public static final String CHANNEL_TRAVERSAL_TYPE = "ChannelTraversalType";
    /**
     * Configuration property name for the maximum channel packet size
     * (integer).
     */
    public static final String MAX_CHANNEL_PACKET_SIZE = "MaxChannelPacketSize";
    /**
     * Configuration property name for the minimum number of channel samples per
     * packet (integer).
     */
    public static final String MIN_PACKET_SAMPLES = "MinPacketSamples";
    /**
     * Configuration property name for the maximum number of channel samples per
     * packet (integer).
     */
    public static final String MAX_PACKET_SAMPLES = "MaxPacketSamples";

    private static final String INVALID_INDEX = "InvalidIndex";
    private static final String INDEX = "index";
    private static final String APID_COUNT = "ApidCount";
    private static final String APID = "apid";
    private static final String COUNT = "count";
    private static final String CHANNELS_PER_APID = "ChannelsPerApid";
    private static final String CHANNEL_ID = "ChannelId";
    private static final String CHANNEL_ID_ATT = "channelId";
    private static final String PACKET = "Packet";
    private static final String VALUE = "Value";
    private static final String DN_VALUE = "dnValue";

    private static final String SCHEMA_RELATIVE_PATH = "schema/ChannelRunConfig.rnc";

    private final SortedSet<Integer> invalidIndices = new TreeSet<Integer>();
    private final List<ApidRotationCount> apidCounts = new LinkedList<ApidRotationCount>();
    private final Map<Integer, List<String>> channelsPerApid = new HashMap<Integer, List<String>>();
    private final List<CustomPacket> customPackets = new LinkedList<CustomPacket>();
    /* MPCS-6333 - 7/1/14. Added list of selected channel data types. */
    private final List<ChannelType> selectedDataTypes = new LinkedList<ChannelType>();

    /**
     * Constructor.
     */
    public ChannelRunConfiguration() {

        super("Channel Run Configuration", SCHEMA_RELATIVE_PATH);
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

        final boolean ok = super.load(uri, new RunConfigurationParseHandler());

        /*
         * Everything parsed ok, so check things the parser cannot easily check.
         */
        if (ok) {
            /*
             * If invalid indices are requested, some invalid indices must be
             * defined.
             */
            if (getBooleanProperty(INCLUDE_INVALID_INDICES, false)
                    && this.invalidIndices.isEmpty()) {
                TraceManager

                        .getDefaultTracer()
                        .error("Invalid index generation specified in the run configuration, but no invalid indices supplied");
                return false;

            }

            /*
             * If minimum and maximum number of samples per packet are defined,
             * the maximum must be >= the minimum.
             */
            if (getIntProperty(MIN_PACKET_SAMPLES, 0) > this.getIntProperty(
                    MAX_PACKET_SAMPLES, 0)) {
                TraceManager.getDefaultTracer().error(

                        "Configured values for " + MAX_PACKET_SAMPLES
                                + " must be greater than or equal to "
                                + MIN_PACKET_SAMPLES);
                return false;
            }
        }
        
        /* MPCS-9375 - 1/3/18 - We cannot support multiple output files in CUSTOM mode.
         * Issue warning and override.  
         */         
        if (getPacketMode() == ChannelPacketMode.CUSTOM && getIntProperty(DESIRED_NUM_FILES, 1) != 1) {
            TraceManager.getDefaultTracer().warn("Configuration requesting multiple output files is ignored in CUSTOM mode");
            this.configProperties.put(DESIRED_NUM_FILES, 1);
        }
        
        return ok;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.config.AbstractXmlConfiguration#clear()
     */
    @Override
    public void clear() {

        super.clear();
        this.invalidIndices.clear();
        this.apidCounts.clear();
        this.channelsPerApid.clear();
        this.customPackets.clear();
        /* MPCS-6333 - 7/1/14. Clear selected data types. */
        this.selectedDataTypes.clear();
    }

    /**
     * Returns the channel packet generation mode.
     * 
     * @return ChannelPacketMode
     */
    public ChannelPacketMode getPacketMode() {

        if (this.channelsPerApid.isEmpty() && this.customPackets.isEmpty()) {
            return ChannelPacketMode.RANDOM;
        } else if (this.customPackets.isEmpty()) {
            return ChannelPacketMode.BY_APID;
        } else {
            return ChannelPacketMode.CUSTOM;
        }
    }

    /**
     * Gets the list of invalid channel indices created from the configuration.
     * 
     * @return List of sorted Integer indices; non-modifiable
     */
    public List<Integer> getInvalidIndices() {

        final List<Integer> l = new LinkedList<Integer>(this.invalidIndices);
        return Collections.unmodifiableList(l);
    }

    /**
     * Gets the list of packet counts by APID, indicating how many packets to
     * generate for each APID before rotating to the next APID. T
     * 
     * @return List of Pairs, where the first element in each pair is APID, and
     *         the second element is a count; non-modifiable.
     */
    public List<ApidRotationCount> getPacketApidCounts() {

        return Collections.unmodifiableList(this.apidCounts);
    }

    /**
     * Gets the list of custom packet definitions, each of which defines a
     * specific packet consisting of APID and a list of channel ID/data number
     * pairs.
     * 
     * @return List of CustomPacket; non-modifiable.
     */
    public List<CustomPacket> getCustomPackets() {

        return Collections.unmodifiableList(this.customPackets);
    }

    /**
     * Gets the list of channels per APID, indicating which channels to place
     * into packets with specific APIDs.
     * 
     * @return Map of Integer (APID) to list of channel IDs. Non-modifiable.
     */
    public Map<Integer, List<String>> getChannelsPerApid() {

        return Collections.unmodifiableMap(this.channelsPerApid);
    }

    /**
     * Gets the list of desired channel data types.
     * 
     * @return List of ChannelType. Will be empty if no channel types specified.
     * 
     * MPCS-6333 - 7/1/14. Added method.
     */
    public List<ChannelType> getChannelTypes() {

        return Collections.unmodifiableList(this.selectedDataTypes);
    }

    /**
     * Establish defaults for properties that require them (have non-zero or
     * non-null default values).
     */
    @Override
    protected void setDefaultProperties() {

        super.setDefaultProperties();

        this.configProperties.put(INCLUDE_INVALID_INDICES, false);
        this.configProperties.put(INCLUDE_INVALID_PACKETS, false);
        this.configProperties.put(CHANNEL_TRAVERSAL_TYPE,
                TraversalType.SEQUENTIAL);
        this.configProperties.put(MAX_CHANNEL_PACKET_SIZE, 65535);
        this.configProperties.put(MIN_PACKET_SAMPLES, 1);
        this.configProperties.put(MAX_PACKET_SAMPLES, 2000);
        this.configProperties.put(MAX_CHANNEL_PACKET_SIZE, 65535);
        this.configProperties.put(STRING_MAX_LEN, 255);
    }

    /**
     * This is the SAX parse handler class for the Channel run configuration
     * file.
     * 
     *
     */
    class RunConfigurationParseHandler extends
            AbstractRunConfigurationParseHandler {

        private int currentPacketApid;
        private CustomPacket customPacket = null;


        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String namespaceURI,
                final String localname, final String rawName,
                final Attributes atts) throws SAXException {

            super.startElement(namespaceURI, localname, rawName, atts);

            if (localname.equalsIgnoreCase(INVALID_INDEX)) {
                /*
                 * Found invalid channel index. Add this invalid index to the
                 * list of invalid indices.
                 */
                final long num = XmlUtility.getUnsignedIntFromAttr(atts, INDEX);
                ChannelRunConfiguration.this.invalidIndices.add(Integer
                        .valueOf((int) num));

            } else if (localname.equalsIgnoreCase(APID_COUNT)) {
                /*
                 * Found packet rotation count for one APID. Add this rotation
                 * count to the list of APID rotation count objects.
                 */
                final long apid = XmlUtility.getUnsignedIntFromAttr(atts, APID);
                final long count = XmlUtility.getUnsignedIntFromAttr(atts,
                        COUNT);
                ChannelRunConfiguration.this.apidCounts
                        .add(new ApidRotationCount((int) apid, count));

            } else if (localname.equalsIgnoreCase(CHANNELS_PER_APID)) {
                /*
                 * Found start of list of channels for one APID. Start a list
                 * for the APID if one does not already exist.
                 */
                this.currentPacketApid = (int) XmlUtility
                        .getUnsignedIntFromAttr(atts, APID);
                ChannelRunConfiguration.this.channelsPerApid.put(
                        this.currentPacketApid, new LinkedList<String>());

            } else if (localname.equalsIgnoreCase(PACKET)) {
                /*
                 * Found a custom packet definitions. Create a custom packet
                 * object and add it to the list of custom packet objects.
                 */
                this.currentPacketApid = (int) XmlUtility
                        .getUnsignedIntFromAttr(atts, APID);
                this.customPacket = new CustomPacket(this.currentPacketApid);
                ChannelRunConfiguration.this.customPackets
                        .add(this.customPacket);

            } else if (localname.equalsIgnoreCase(VALUE)) {
                /*
                 * Found a channel ID/DN value pair for the latest custom
                 * packet. Attach these values to the custom packet object.
                 */
                final String channelId = atts.getValue(CHANNEL_ID_ATT);
                final String dn = atts.getValue(DN_VALUE);
                this.customPacket.addDn(channelId, dn);

            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(final String namespaceURI,
                final String localname, final String rawName)
                throws SAXException {

            if (localname.equalsIgnoreCase(CHANNEL_ID)) {
                /*
                 * Found a channel for inclusion in the channel list for the
                 * current APID. Add it to the channel list for the current
                 * packet APID.
                 */
                final String channelId = this.getBufferText();
                final List<String> channels = ChannelRunConfiguration.this.channelsPerApid
                        .get(this.currentPacketApid);
                channels.add(channelId);
                return;
            } else if (localname.equalsIgnoreCase(PACKET)) {
                /*
                 * Ending custom packet.
                 */
                this.customPacket = null;
                return;
            }

            /*
             * Now parse non-string properties that require no special handling
             * or context and store them in the general map of configuration
             * properties.
             */
            boolean found = storeBooleanElement(localname,
                    INCLUDE_INVALID_INDICES);
            found = found
                    || storeBooleanElement(localname, INCLUDE_INVALID_PACKETS);
            found = found
                    || storeFloatElement(localname, INVALID_INDEX_PERCENT);
            found = found
                    || storeTraversalTypeElement(localname,
                            CHANNEL_TRAVERSAL_TYPE);
            found = found
                    || storeIntegerElement(localname, MAX_CHANNEL_PACKET_SIZE);
            found = found || storeIntegerElement(localname, MIN_PACKET_SAMPLES);
            found = found || storeIntegerElement(localname, MAX_PACKET_SAMPLES);

            /* MPCS-6333 - 7/1/14. Parse selected channel data types. */
            found = found || storeTypeList(localname, TYPE_LIST);

            /*
             * If we got to here and the element is not one we have recognized,
             * let the super class handle it.
             */
            if (!found) {
                super.endElement(namespaceURI, localname, rawName);
            }
        }

        /**
         * Parses a channel data type list.
         * 
         * @param localname
         *            the name of the XML element being parsed.
         * @param property
         *            the name of the XML element we are supposed to be parsing,
         * @return true if the XML element being parsed matches the XML element
         *         we are supposed to parse; false if not
         * 
         * @throws SAXException
         *             if there is an error parsing the channel type list
         * 
         * MPCS-6333 - 7/1/14. Added method.
         */
        private boolean storeTypeList(final String localname,
                final String property) throws SAXException {

            if (!localname.equalsIgnoreCase(property)) {
                return false;
            }
            final String listFromXml = getBufferText();
            if (listFromXml.isEmpty()) {
                throw new SAXException("The value for XML element " + TYPE_LIST
                        + " cannot be empty");
            }

            final String[] pieces = listFromXml.split(",");
            for (final String piece : pieces) {
                try {
                    final ChannelType type = Enum.valueOf(ChannelType.class,
                            piece.trim());
                    ChannelRunConfiguration.this.selectedDataTypes.add(type);
                } catch (final IllegalArgumentException e) {
                    throw new SAXException("The value for XML element "
                            + TYPE_LIST
                            + " contains unrecognized channel data type: "
                            + piece);
                }
            }

            return true;
        }
    }
}
