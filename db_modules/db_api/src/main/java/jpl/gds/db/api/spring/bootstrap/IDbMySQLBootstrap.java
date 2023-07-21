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
package jpl.gds.db.api.spring.bootstrap;

/**
 * Interface containing constants for DbMySQLBootstrap
 *
 */
public interface IDbMySQLBootstrap {
    /**
     * Bean name for the MySqlAdaptationProperties Bean
     */
    public static final String MYSQL_ADAPTATION_PROPERTIES = "MYSQL_ADAPTATION_PROPERTIES";

    /**
     * Bean name for the Database Query Config Bean
     */
    public static final String DB_QUERY_CONFIG             = "DB_QUERY_CONFIG";

    /**
     * Bean name for the Database Query Config Bean
     */
    public static final String DB_CONTROLLER               = "DB_CONTROLLER";

    /**
     * 
     */
    public static final String STORE_CONFIG_MAP            = "STORE_CONFIG_MAP";

    /**
     * 
     */
    public static final String STORE_FACTORY               = "STORE_FACTORY";

    /**
     * 
     */
    public static final String FETCH_CONFIG_MAP            = "FETCH_CONFIG_MAP";

    /**
     * 
     */
    public static final String FETCH_FACTORY               = "FETCH_FACTORY";

    /**
     * 
     */
    public static final String FRAME_QUERY_OPTIONS_FACTORY = "FRAME_QUERY_OPTIONS_FACTORY";

    /**
     * 
     */
    public static final String ORDER_BY_TYPE_FACTORY       = "ORDER_BY_TYPE_FACTORY";

    /**
     * DB Log Util bean
     */
    public static final String DB_LOGGING_UTIL = "DB_LOG_UTIL";
}
