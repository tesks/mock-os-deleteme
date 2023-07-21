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
package jpl.gds.globallad.spring.beans;

public class BeanNames {
    /** GLAD_SPRING_PROPERTY_SOURCES */
    public static final String GLAD_SPRING_PROPERTY_SOURCES = "gladSpringPropertySources";

    /** GLAD_COMMAND_LINE_OVERRIDES */
    public static final String GLAD_COMMAND_LINE_OVERRIDES  = "globalLadCLIOverride";

    /** GLAD_CONFIG_NAME */
    public static final String GLAD_CONFIG_NAME             = "globalLadConfig";

    /** GLAD_EMBEDDED_SERVER */
    public static final String GLAD_EMBEDDED_SERVER         = "gladEmbeddedServer";

    /** GLAD_SOCKET_SERVER */
    public static final String GLAD_SOCKET_SERVER           = "globalLadSocketServer";

    /** GLAD_DATA_STORE */
    public static final String GLAD_DATA_STORE              = "globalLadDataStore";

    /** GLAD_DATA_PRODUCER_NAME */
    public static final String GLAD_DATA_PRODUCER_NAME      = "globalLadDataProducer";

    /** GLAD_DATA_FACTORY */
    public static final String GLAD_DATA_FACTORY            = "globalLadDataFactory";

    /** GLAD_DATA_INSERTER */
    public static final String GLAD_DATA_INSERTER           = "globalLadDataInserter";

    /** Worker Bean: GLAD_PERSISTER */
    public static final String GLAD_PERSISTER               = "globalLadPersister";

    /** Worker Bean: GLAD_REAPER */
    public static final String GLAD_REAPER                  = "globalLadReaper";

    /** Worker Bean: GLAD_WORKER_EXECUTOR */
    public static final String GLAD_WORKER_EXECUTOR         = "globalLadWorkerExecutor";

    /** GLAD Socket Server Executor */
    public static final String GLAD_EXECUTOR                = "gladExecutor";

    /** GLAD JMS Data Source */
    public static final String JMS_DATA_SOURCE              = "jmsDataSource";
}
