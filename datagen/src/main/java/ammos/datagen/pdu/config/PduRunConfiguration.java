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
package ammos.datagen.pdu.config;

import org.xml.sax.SAXException;

import ammos.datagen.config.AbstractRunConfiguration;
import ammos.datagen.config.IRunConfiguration;

/**
 * This is the PduRunConfiguration class for the AMPCS data generators. It
 * parses an XML file that contains configuration information that may change from
 * run to run of the PDU generator. The XML file containing the configuration is
 * verified against its schema before loading it. After the XML file is loaded,
 * the configuration values it contained are available using various accessor
 * methods.
 * 
 *
 */
public class PduRunConfiguration extends AbstractRunConfiguration implements IRunConfiguration {

    /** Configuration property name for the PacketApid (integer) */
    public static final String  PACKET_APID          = "PacketApid";

    /** Configuration property name for the StartPacketSeq (integer) */
    public static final String  START_PACKET_SEQ     = "StartPacketSeq";

    /** Configuration property name for the PreferredPduLength (integer) */
    public static final String  PREF_PDU_LENGTH      = "PreferredPduLength";

    /** Configuration property name for the EntityIdLength (integer) */
    public static final String  ENTITY_ID_LENGTH     = "EntityIdLength";

    /** Configuration property name for the TransactionSequenceLength (integer) */
    public static final String  TRANS_SEQ_LENGTH     = "TransactionSequenceLength";

    /** Configuration property name for the source entity ID (integer) */
    public static final String  SOURCE_ENTITY_ID     = "SourceEntityId";

    /** Configuration property name for the destination entity ID (integer) */
    public static final String  DEST_ENTITY_ID       = "DestinationEntityId";

    /** Configuration property name for the TransmissionMode flag (boolean) */
    public static final String  TRANSMISSION_MODE    = "TransmissionMode";

    /** Configuration property name for the GenerateCrc flag (boolean) */
    public static final String  GENERATE_CRC         = "GenerateCrc";

    /** Configuration property name for the SegmentationControl flag (boolean) */
    public static final String  SEG_CONTROL          = "SegmentationControl";

    /** Configuration property name for the DropMeta flag (boolean) */
    public static final String  DROP_META            = "DropMeta";

    /** Configuration property name for the DropData flag (boolean) */
    public static final String  DROP_DATA            = "DropData";

    /** Configuration property name for the DropEof flag (boolean) */
    public static final String  DROP_EOF             = "DropEof";

    private static final String SCHEMA_RELATIVE_PATH = "schema/PduRunConfig.rnc";

    /**
     * Constructor.
     */
    public PduRunConfiguration() {
        super("PDU Run Configuration", SCHEMA_RELATIVE_PATH);
    }
    
    /**
     * Loads the configuration file.
     * 
     * @param uri the path to the configuration file.
     * 
     * @return true if the file was successfully loaded, false if not.
     */
    public boolean load(final String uri) {

        return super.load(uri, new PduRunConfigurationParseHandler() {
        });
    }

    /**
     * Establish defaults for properties that require them (have non-zero or
     * non-null default values).
     */
    @Override
    protected void setDefaultProperties() {

        super.setDefaultProperties();

        this.configProperties.put(TRANSMISSION_MODE, false);
        this.configProperties.put(GENERATE_CRC, false);
        this.configProperties.put(SEG_CONTROL, false);
        this.configProperties.put(DROP_META, false);
        this.configProperties.put(DROP_DATA, false);
        this.configProperties.put(DROP_EOF, false);
        this.configProperties.put(PREF_PDU_LENGTH, 1000);  // bytes
        this.configProperties.put(ENTITY_ID_LENGTH, 1);     // bytes
        this.configProperties.put(TRANS_SEQ_LENGTH, 1);     // bytes
    }

    /**
     * This is the SAX parse handler class for the PDU run configuration file.
     * 
     *
     */
    class PduRunConfigurationParseHandler extends AbstractRunConfigurationParseHandler {
        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(final String namespaceURI, final String localname, final String rawName)
                throws SAXException {

            boolean found = storeBooleanElement(localname, TRANSMISSION_MODE);
            found = found || storeBooleanElement(localname, GENERATE_CRC);
            found = found || storeBooleanElement(localname, SEG_CONTROL);
            found = found || storeBooleanElement(localname, DROP_META);
            found = found || storeBooleanElement(localname, DROP_DATA);
            found = found || storeBooleanElement(localname, DROP_EOF);
            found = found || storeIntegerElement(localname, PACKET_APID);
            found = found || storeIntegerElement(localname, START_PACKET_SEQ);
            found = found || storeIntegerElement(localname, PREF_PDU_LENGTH);
            found = found || storeIntegerElement(localname, ENTITY_ID_LENGTH);
            found = found || storeIntegerElement(localname, TRANS_SEQ_LENGTH);
            found = found || storeIntegerElement(localname, SOURCE_ENTITY_ID);
            found = found || storeIntegerElement(localname, DEST_ENTITY_ID);

            // If we got to here and the element is not found, let the
            // super class handle it.

            if (!found) {
                super.endElement(namespaceURI, localname, rawName);
            }
        }
    }
}
