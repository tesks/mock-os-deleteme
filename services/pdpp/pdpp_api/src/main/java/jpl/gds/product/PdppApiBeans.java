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
package jpl.gds.product;

/**
 * Defines the beans available in the Spring bootstrap for the pdpp modules.
 *
 */
public final class PdppApiBeans {

    /**
     * restricting instantiation
     */
    private PdppApiBeans() {
    }

    public static final String PDPP_CONTEXT_CACHE = "PDPP_CONTEXT_CACHE";
    public static final String AUTOMATION_PROCESS_CACHE = "AUTOMATION_PROCSS_CACHE";
    public static final String ANCESTOR_MAP = "ANCESTOR_MAP";
    public static final String SESSION_FETCHER = "SESSION_FETCHER";
    public static final String AUTOMATION_FEATURE_MANAGER = "AUTOMATION_FEATURE_MANAGER";
    public static final String PDPP_PRODUCT_FILENAME_BUILDER_FACTORY = "PDPP_PRODUCT_FILENAME_BUILDER_FACTORY";
    public static final String DICTIONARY_MAPPER = "DICTIONARY_MAPPER";
    public static final String PRODUCT_AUTOMATION_PROPERTIES = "PRODUCT_AUTOMATION_PROPERTIES";
    public static final String AUTOMATION_SESSION_FACTORY = "AUTOMATION_SESION_FACTORY";
    public static final String PRODUCT_DAO = "PRODUCT_DAO";
    public static final String ACTION_DAO = "ACTION_DAO";
    public static final String STATUS_DAO = "STATUS_DAO";
    public static final String PROCESS_DAO = "PROCESS_DAO";
    public static final String USER_DAO = "USER_DAO";
    public static final String CLASS_MAP_DAO = "CLASS_MAP_DAO";
    public static final String LOGS_DAO = "LOGS_DAO";
    public static final String AUTOMATION_DOWNLINK_SERVICE = "AUTOMATION_DOWNLINK_SERVICE";
    public static final String PDPP_PRODUCT_METADATA_BUILDER = "PDPP_PRODUCT_METADATA_BUILDER";
    public static final String PDPP_PRODUCT_AUTOMATION_PRODUCT_ADDER = "PDPP_PRODUCT_AUTOMATION_PRODUCT_ADDER";
    public static final String PDPP_CONTEXT_CONTAINER_CREATOR = "PDPP_CONTEXT_CONTAINER_CREATOR";
    public static final String AUTOMATION_LOGGER = "AUTOMATION_LOGGER";
}
