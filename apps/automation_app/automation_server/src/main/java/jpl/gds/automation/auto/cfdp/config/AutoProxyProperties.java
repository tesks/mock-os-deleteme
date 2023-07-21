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
package jpl.gds.automation.auto.cfdp.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.exceptions.PropertyLoadException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Automation Proxy properties object
 * @since R8
 */
public class AutoProxyProperties extends GdsHierarchicalProperties {
    /** AUTO proxy properties file */
    protected static final String                   PROPERTY_FILE       = "auto_proxy.properties";

    private static final String                     PROPERTY_PREFIX     = "autoProxy.";
    private static final String                     CFDP_BLOCK          = PROPERTY_PREFIX + "cfdp.";
    private static final String                     ENTITY_BLOCK         = CFDP_BLOCK + "entity.";
    private static final String                     ENTITY_IDS           = ENTITY_BLOCK + "ids";

    private static final String                     SCID_SUFFIX         = ".scid";
    private static final String                     VCID_SUFFIX         = ".vcid";
    private static final String                     APID_SUFFIX         = ".apid";

    private static final String                     POLICIES            = "policies";
    private static final String                     POLICIES_SUFFIX     = ".policies";
    private static final String                     MAX_PAYLOAD_SUFFIX  = ".maxPayload";
    private static final String                     AGGREGATE_SUFFIX    = ".aggregate";
    private static final String                     FLUSH_SUFFIX        = ".flushTimer";
    private static final String                     DEFAULT_VALUE_SUFFIX = ".defaultValue";
    private static final String                     CONNECTION_BLOCK     = PROPERTY_PREFIX + "connection.";
    private static final String                     HOST_PROPERTY        = "host";
    private static final String                     PORT_PROPERTY        = "port";
    private static final String                     UPLINK_TYPE_PROPERTY = "type";


    private static final int                        DEFAULT_APID        = 1279;
    private static final int                        DEFAULT_VCID         = 0;
    private static final int                        DEFAULT_MAX_PAYLOAD = 1024;
    private static final int                        DEFAULT_FLUSH_TIMER = 60;
    private static final boolean                    DEFAULT_AGGREGATE   = false;
    private static final int                        MINIMUM_PAYLOAD_SIZE = 8;
    private static final int                        MINIMUM_FLUSH_TIMER  = 3;

    private final int                               DEFAULT_SCID;

    private final UplinkConnectionType              connectionType;
    private final int                               uplinkPort;
    private final String                            uplinkHost;

    // FlushTimer is global, not per entity
    private int flushTimer;

    private final Map<Integer, Map<String, String>> entityMap;

    /**
     * CFDP Proxy Configuration
     * 
     * @param connectionProperties
     *            Connection Properties
     * @param missionProperties
     *            Mission Properties
     * @param sseFlag
     *            The SSE context flag
     * 
     * 
     * @throws PropertyLoadException
     *             error loading properties
     */
    public AutoProxyProperties(final ConnectionProperties connectionProperties,
            final MissionProperties missionProperties, final SseContextFlag sseFlag) throws PropertyLoadException {
        super(PROPERTY_FILE, sseFlag);

        entityMap = new HashMap<>();
        String errorMsg = "";

        DEFAULT_SCID = missionProperties.getDefaultScid();
        
        uplinkHost = getProperty(CONNECTION_BLOCK + HOST_PROPERTY, connectionProperties.getDefaultUplinkHost(false));
        uplinkPort = getIntProperty(CONNECTION_BLOCK + PORT_PROPERTY, connectionProperties.getDefaultUplinkPort(false));
        connectionType = UplinkConnectionType.safeValueOf(getProperty(CONNECTION_BLOCK+ UPLINK_TYPE_PROPERTY, 
                connectionProperties.getDefaultUplinkConnectionType(false).name()));

        flushTimer = getIntProperty(CFDP_BLOCK + POLICIES + FLUSH_SUFFIX,
                                        DEFAULT_FLUSH_TIMER);
        if (flushTimer < MINIMUM_FLUSH_TIMER) {
            log.warn("The configured flush timer ", flushTimer, " must be >= ", MINIMUM_FLUSH_TIMER,
                     " seconds. Setting default ", DEFAULT_FLUSH_TIMER);
            flushTimer = DEFAULT_FLUSH_TIMER;
        }

        final List<Integer> entityList = getIntListProperty(ENTITY_IDS, null, ",");
        if (entityList.isEmpty()) {
            errorMsg += "No entity id configuration found. Set the " + ENTITY_IDS + " property\n";
        }


        for (final Integer id : entityList) {
            final Map<String, String> entityProperties = new HashMap<>();

            // check for override on default values, use ours if not specified
            final int defaultVcid = getIntProperty(ENTITY_BLOCK + id + VCID_SUFFIX + DEFAULT_VALUE_SUFFIX,
                                                   DEFAULT_VCID);
            entityProperties.put(ENTITY_BLOCK + id + VCID_SUFFIX + DEFAULT_VALUE_SUFFIX, String.valueOf(defaultVcid));

            final int defaultApid = getIntProperty(ENTITY_BLOCK + id + APID_SUFFIX + DEFAULT_VALUE_SUFFIX,
                                                   DEFAULT_APID);
            entityProperties.put(ENTITY_BLOCK + id + APID_SUFFIX + DEFAULT_VALUE_SUFFIX, String.valueOf(defaultApid));

            
            // If undefined, scid will default to the mission properties scid
            entityProperties.put(ENTITY_BLOCK + id + SCID_SUFFIX,
                                 getProperty(ENTITY_BLOCK + id + SCID_SUFFIX, String.valueOf(DEFAULT_SCID)));

            // If undefined, apid will default to 1279
            entityProperties.put(ENTITY_BLOCK + id + APID_SUFFIX,
                                 String.valueOf(getIntProperty(ENTITY_BLOCK + id + APID_SUFFIX, defaultApid)));

            // If undefined, vcid will default to 0
            entityProperties.put(ENTITY_BLOCK + id + VCID_SUFFIX,
                                 String.valueOf(getIntProperty(ENTITY_BLOCK + id + VCID_SUFFIX, defaultVcid)));
            
            // Load policies
            entityProperties.put(ENTITY_BLOCK + id + POLICIES_SUFFIX + AGGREGATE_SUFFIX,
                                 Boolean.toString(getBooleanProperty(ENTITY_BLOCK + id + POLICIES_SUFFIX + AGGREGATE_SUFFIX, 
                                         DEFAULT_AGGREGATE)));

            int maxPayload = getIntProperty(ENTITY_BLOCK + id + POLICIES_SUFFIX + MAX_PAYLOAD_SUFFIX,
                                                  DEFAULT_MAX_PAYLOAD);
            if (maxPayload < MINIMUM_PAYLOAD_SIZE) {
                log.warn("The configured maximum payload size ", maxPayload, " must be >= ", MINIMUM_PAYLOAD_SIZE,
                         " bytes. Setting default ", DEFAULT_MAX_PAYLOAD);
                maxPayload = DEFAULT_MAX_PAYLOAD;
            }
            entityProperties.put(ENTITY_BLOCK + id + POLICIES_SUFFIX + MAX_PAYLOAD_SUFFIX, String.valueOf(maxPayload));


            entityMap.put(id, entityProperties);
        }

        if (!errorMsg.isEmpty()) {
            throw new PropertyLoadException("Unable to load CFDP proxy properties. Resolve the following errors:\n"
                    + errorMsg);
        }

    }

    /**
     * Check if there is a proxy configuration for a entity id
     * 
     * @param entity
     *            destination entity id
     * @return if a configuration exists for the entity id
     */
    public boolean hasEntityConfig(final int entity) {
        return entityMap.containsKey(entity);
    }

    private String getFromEntityMap(final int entity, final String propertySuffix) {
        String property = entityMap.get(entity).get(ENTITY_BLOCK + String.valueOf(entity) + propertySuffix);
        if (property.isEmpty()) { // undefined. Check for a default property
            property = entityMap.get(entity)
                            .get(ENTITY_BLOCK + String.valueOf(entity) + propertySuffix + DEFAULT_VALUE_SUFFIX);
        }
        return property;
    }

    /**
     * Get the spacecraft id configuration for a given entity id.
     * Defaults to the mission.properties scid
     * 
     * @param entity
     *            destination entity id
     * @return spacecraft id for a destination entity
     */
    public int getScidForEntity(final int entity) {
        if (hasEntityConfig(entity)) {
            return Integer.valueOf(getFromEntityMap(entity, SCID_SUFFIX));
        }
        return DEFAULT_SCID;
    }

    /**
     * Get the VCID configuration for a given entity id.
     * Defaults to 0
     * 
     * @param entity
     *            destination entity id
     * @return VCID for a destination entity
     */
    public int getVcidForEntity(final int entity) {
        if (hasEntityConfig(entity)) {
            return Integer.valueOf(getFromEntityMap(entity, VCID_SUFFIX));
        }
        return DEFAULT_VCID;
    }

    /**
     * Gets the APID configuration for a given entity id
     * Defaults to 1279 (0x4ff)
     * 
     * @param entity
     *            destination entity id
     * @return APID for a destination entity
     */
    public int getApidForEntity(final int entity) {
        if (hasEntityConfig(entity)) {
            return Integer.valueOf(getFromEntityMap(entity, APID_SUFFIX));
        }
        return DEFAULT_APID;
    }

    /**
     * Gets the flush timer configuration
     * 
     * @return flush timer interval
     */
    public int getFlushTimer() {
        return flushTimer;
    }

    /**
     * Gets the maximum allowed pay load size (in bytes) for a given entity id
     * Defaults to 1024 (bytes)
     * 
     * @param entity
     *            destination entity id
     * @return Maximum pay load size for a destination entity
     */
    public int getMaxPayloadForEntity(final int entity) { 
        if (hasEntityConfig(entity)) { 
            return Integer.valueOf(getFromEntityMap(entity, POLICIES_SUFFIX + MAX_PAYLOAD_SUFFIX));
        }
        return DEFAULT_MAX_PAYLOAD;
    }

    /**
     * Gets the PDU aggregation strategy for a given entity id.
     * If enabled, the CFDP server will attempt to aggregate send_pdu requests
     * 
     * Defaults to false
     * 
     * @param entity
     *            destination entity id
     * @return true if aggregating pdu's for a destination entity
     */
    public boolean getAggregateStrategyForEntity(final int entity) {
        if (hasEntityConfig(entity)) {
            return Boolean.valueOf(getFromEntityMap(entity, POLICIES_SUFFIX + AGGREGATE_SUFFIX));
        }
        return DEFAULT_AGGREGATE;
    }

    /**
     * Gets the destination uplink host
     * 
     * @return uplink host destination
     */
    public String getUplinkHost() {
        return uplinkHost;
    }

    /**
     * Gets the destination uplink port
     * 
     * @return uplink port destination
     */
    public int getUplinkPort() {
        return uplinkPort;
    }

    /**
     * Gets the uplink type
     * 
     * @return uplink type
     */
    public UplinkConnectionType getUplinkType() {
        return connectionType;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
