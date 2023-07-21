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
package ammos.datagen.config;

/**
 * This is the GeneralMissionConfiguration class for the AMPCS data generators.
 * It parses an XML file that contains configuration information that is
 * specific to a mission does not generally change from run to run of tools. The
 * XML file containing the configuration is verified against its schema before
 * loading it. After the XML file is loaded, the configuration values it
 * contained are available using various accessor methods.
 * 
 *
 */
public class GeneralMissionConfiguration extends AbstractMissionConfiguration
        implements IMissionConfiguration {

    private static final String SCHEMA_RELATIVE_PATH = "schema/GeneralMissionConfig.rnc";

    /**
     * Constructor.
     */
    public GeneralMissionConfiguration() {

        super("General Mission Configuration", SCHEMA_RELATIVE_PATH);
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

        return super.load(uri, new MissionConfigurationParseHandler());
    }
}
