/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.ampcs.properties;

import jpl.gds.cfdp.common.config.EConfigurationPropertyKey;
import jpl.gds.cfdp.common.mib.ELocalEntityMibPropertyKey;
import jpl.gds.cfdp.common.mib.ERemoteEntityMibPropertyKey;
import jpl.gds.shared.config.GdsHierarchicalProperties;

public class CfdpProcessorAmpcsProperties extends GdsHierarchicalProperties {

    private static final String PROPERTY_FILE = "cfdp_processor.properties";

    private static final String PROPERTY_PREFIX = "cfdpProcessor.";

    private static final String WRITABLE_CONFIG_FILE_PROPERTY = PROPERTY_PREFIX + "writable.config.file";
    private static final String WRITABLE_CONFIG_FILE_DEFAULT = "/ammos/ampcs/cfdp_processor/config.properties";

    private static final String CONFIG_FILE_INIT_PREFIX = PROPERTY_PREFIX + "config.file.init.";
    private static final String MIB_FILE_INIT_LOCAL_PREFIX = PROPERTY_PREFIX + "mib.file.init.local.";
    private static final String MIB_FILE_INIT_REMOTE_PREFIX = PROPERTY_PREFIX + "mib.file.init.remote.";

    /**
     * Constructor that loads the default property file, which will be found using a
     * standard configuration search.
     */
    public CfdpProcessorAmpcsProperties() {
        super(PROPERTY_FILE, true);
    }

    public String getWritableConfigFile() {
        return getProperty(WRITABLE_CONFIG_FILE_PROPERTY, WRITABLE_CONFIG_FILE_DEFAULT);
    }

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    /**
     * Get config properties file initialization property.
     *
     * @param key property key to look up
     * @return Config properties file initialization property value
     */
    public String getConfigFileInitProperty(final EConfigurationPropertyKey key) {
        return getProperty(CONFIG_FILE_INIT_PREFIX + key.getSubPropertyKeyStr());
    }

    /**
     * Get MIB file initialization property for local entity.
     *
     * @param key property key to look up
     * @return MIB file initialization property value for local entity
     */
    public String getMibFileInitLocalProperty(final ELocalEntityMibPropertyKey key) {
        return getProperty(MIB_FILE_INIT_LOCAL_PREFIX + key.getKeyStr());
    }

    /**
     * Get MIB file initialization property for remote entity.
     *
     * @param key property key to look up
     * @return MIB file initialization property value for remote entity
     */
    public String getMibFileInitRemoteProperty(final ERemoteEntityMibPropertyKey key) {
        return getProperty(MIB_FILE_INIT_REMOTE_PREFIX + key.getKeyStr());
    }

}