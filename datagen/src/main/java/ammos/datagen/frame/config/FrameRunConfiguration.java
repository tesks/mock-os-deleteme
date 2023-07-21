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
package ammos.datagen.frame.config;

import org.xml.sax.SAXException;

import ammos.datagen.config.AbstractRunConfiguration;
import ammos.datagen.config.IRunConfiguration;

/**
 * This is the FrameRunConfiguration class for the AMPCS data generators. It
 * parses an XML file that contains configuration information that may change from
 * run to run of the Frame generator. The XML file containing the configuration is
 * verified against its schema before loading it. After the XML file is loaded,
 * the configuration values it contained are available using various accessor
 * methods.
 * 
 *
 */
public class FrameRunConfiguration extends AbstractRunConfiguration implements IRunConfiguration {
    /** Configuration property name for the VCID (integer) */
    public static final String  VCID                 = "Vcid";

    /** Configuration property name for the Spacecraft ID (integer) */
    public static final String  SCID                 = "Scid";

    /** Configuration property name for the Start VCFC (integer) */
    public static final String  START_VCFC           = "StartVcfc";

    /** Configuration property name for the Frame Type (string) */
    public static final String  FRAME_TYPE           = "FrameType";

    /** Configuration property name for the Packet Span Frames (boolean) */
    public static final String PACKET_SPAN_FRAMES = "PacketSpanFrames";

    private static final String SCHEMA_RELATIVE_PATH = "schema/FrameRunConfig.rnc";

    /**
     * Constructor.
     */
    public FrameRunConfiguration() {
        super("Frame Run Configuration", SCHEMA_RELATIVE_PATH);
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
        return super.load(uri, new FrameRunConfigurationParseHandler() {
        });
    }

    /**
     * Establish defaults for properties that require them (have non-zero or
     * non-null default values).
     */
    @Override
    protected void setDefaultProperties() {
        super.setDefaultProperties();

        this.configProperties.put(VCID, 1);
        this.configProperties.put(SCID, 1);
        this.configProperties.put(START_VCFC, 0);
        // no default for FRAME_TYPE
        this.configProperties.put(PACKET_SPAN_FRAMES, false);
    }

    /**
     * This is the SAX parse handler class for the PDU run configuration file.
     * 
     *
     */
    class FrameRunConfigurationParseHandler extends AbstractRunConfigurationParseHandler {
        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(final String namespaceURI, final String localname, final String rawName)
                throws SAXException {

            boolean found = storeIntegerElement(localname, VCID);
            found = found || storeIntegerElement(localname, SCID);
            found = found || storeIntegerElement(localname, START_VCFC);
            found = found || storeStringElement(localname, FRAME_TYPE);
            found = found || storeBooleanElement(localname, PACKET_SPAN_FRAMES);

            // If we got to here and the element is not found, let the
            // super class handle it.

            if (!found) {
                super.endElement(namespaceURI, localname, rawName);
            }
        }
    }

}
