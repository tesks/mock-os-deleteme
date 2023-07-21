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
package jpl.gds.cfdp.processor.config;

import cfdp.engine.ChecksumAlgorithm;
import cfdp.engine.ampcs.IDirectoriesConfigurationLookup;
import cfdp.engine.ampcs.IMetadataConfigurationLookup;
import cfdp.engine.ampcs.OrderedProperties;
import jpl.gds.cfdp.common.config.EConfigurationPropertyKey;
import jpl.gds.cfdp.processor.out.EOutboundPduSinkType;
import jpl.gds.cfdp.processor.util.CfdpFileUtil;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfigurationFactory;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.*;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.*;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.UnsignedInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static jpl.gds.shared.metadata.context.ContextConfigurationType.CFDP;

/**
 * Configuration manager for CFDP Processor
 *
 */
@Service
public class ConfigurationManager implements IDirectoriesConfigurationLookup, IMetadataConfigurationLookup {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    private static final String PROPERTY_PREFIX = "cfdp.processor.config.manager.";
    public static final String WRITABLE_CONFIG_FILE_PROPERTY_NAME = PROPERTY_PREFIX + "writable.config.file";
    public static final String USER_OVERRIDES_APPLIED_PROPERTY_NAME = PROPERTY_PREFIX + "user.overrides.applied";

    @Value("${" + WRITABLE_CONFIG_FILE_PROPERTY_NAME + "}")
    private String configFile;

    @Value("${" + USER_OVERRIDES_APPLIED_PROPERTY_NAME + ":false}")
    private boolean userOverridesApplied;

    DateFormat dateFormatter;

    private Object configPropertiesLock;

    /*
     * The actual Properties object that will hold the config properties, for easy
     * serialization
     */
    private final Properties configProperties = new OrderedProperties();

    @Autowired
    private Environment env;

    @Autowired
    private CfdpFileUtil cfdpFileUtil;

    private IDatabaseProperties databaseProperties;

    private IDbSqlArchiveController archiveController;

    private ISimpleContextConfiguration parentContext;

    private String appNameVer;

    /*
    MPCS-10630 4/10/19 @PostConstruct will set the value so that context config database insert will
    properly reflect the port number
     */
    private int port;

    @Autowired
    private IStatusMessageFactory statusMessageFactory;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);

        // MPCS-10631 4/10/19 Find out connection info and log
        final AbstractServletWebServerFactory servletContainer =
                appContext.getBean(AbstractServletWebServerFactory.class);
        final ISslConfiguration sslConfig = appContext.getBean(ISslConfiguration.class);
        final String httpMethod = sslConfig.isSecure() ? "https" : "http";
        port = servletContainer.getPort();
        final String contextPath = servletContainer.getContextPath();

        /*
         Port number above is needed before we store the context config in the database because it's one of the metadata
         */
        startDatabaseStores();

        // MPCS-10094 12/12/2018: Create new context configuration and insert into database
        registerAndInsertContextToDatabase();

        log.info("CFDP Processor configuration file: " + configFile);

        for (final String key : EConfigurationPropertyKey.getAllFullKeyStrings()) {
            configProperties.setProperty(key, env.getProperty(key));
        }

        /*
        If Spring used a different port number than the one configured or provided by user, update the
        configuration file accordingly. This could happen when Spring properties file is used to override the port.
         */
        if (!getProperty(EConfigurationPropertyKey.PORT_PROPERTY.toString()).equals(getCurrentActualPort())) {
            log.warn("Actual port number " + port + " differs from configured or one supplied by user (perhaps Spring" +
                    " properties was used to override it). Saving the actual port number to CFDP Processor configuration file");
            configProperties.setProperty(EConfigurationPropertyKey.PORT_PROPERTY.toString(), getCurrentActualPort());
            userOverridesApplied = true;
        }

        dateFormatter = TimeUtility.getFormatterFromPool();

        configPropertiesLock = new Object();

        if (userOverridesApplied && !configAutosaveDisabled()) {
            // Save to config file so that the user overrides get persisted
            savePropertiesToConfigFile();
        }

        // MPCS-9929  - 8/24/2018 - Removed ability to configure CFDP Processor's log level dynamically
        //appRootLoggerName = CfdpProcessorApp.class.getPackage() != null ? CfdpProcessorApp.class.getPackage().getName()
        //        : null;
        //loggingSystem.setLogLevel(appRootLoggerName != null ? appRootLoggerName : LoggingSystem.ROOT_LOGGER_NAME,
        //        getLogLevel());

        // MPCS-10634 4/9/19 Check that required directories exist, exit if can't create
        checkRequiredDirectoriesAndExitIfError();

        appNameVer =
                ReleaseProperties.getProductLine() + " " + ApplicationConfiguration.getApplicationName() + " " + ReleaseProperties.getVersion();

        // Now find out the local host name for logging
        String canonicalHostName = null;

        try {
            canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (final UnknownHostException e) {
            log.warn("Could not determine local hostname: ", ExceptionTools.getMessage(e));
            canonicalHostName = "<unknown>";
        }

        log.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                appNameVer + " started on " + httpMethod + "://"
                        + canonicalHostName + ":" + port + contextPath + "/",
                LogMessageType.REST));


    }

    @PreDestroy
    public void shutdown() {
        TimeUtility.releaseFormatterToPool(dateFormatter);
        stopDatabaseStores();
        log.info("Shutdown finished");
    }

    private void savePropertiesToConfigFile() {

        synchronized (configPropertiesLock) {

            try (FileOutputStream fos = new FileOutputStream(configFile)) {

                // MPCS-10634 4/9/19 Create directories if missing
                cfdpFileUtil.createParentDirectoriesIfNotExist(configFile);
                configProperties.store(fos, "Autosaved " + dateFormatter.format(new Date()));
            } catch (final IOException ie) {
                log.error("Exception thrown while saving configuration file " + configFile + ": "
                        + ExceptionTools.getMessage(ie));
            }

        }

    }

    private void startDatabaseStores() {
        databaseProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);

        if (databaseProperties.getUseDatabase()) {
            archiveController = appContext.getBean(IDbSqlArchiveController.class);
            archiveController.addNeededStore(ICfdpIndicationLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ICfdpFileGenerationLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ICfdpFileUplinkFinishedLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ICfdpRequestReceivedLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ICfdpRequestResultLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ICfdpPduSentLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ICfdpPduReceivedLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ILogMessageLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(IProductLDIStore.STORE_IDENTIFIER);
            archiveController.addNeededStore(ICommandMessageLDIStore.STORE_IDENTIFIER);

            if (!archiveController.startAllNonSessionStores()) {
                log.error("Unable to start database stores");
            }

            // MPCS-10094 1/5/2019
            archiveController.startSessionStoresWithoutInserting();

        } else {
            log.info("Database is disabled by configuration");
        }

    }

    private void registerAndInsertContextToDatabase() {

        if (databaseProperties.getUseDatabase() && archiveController != null) {
            parentContext = appContext.getBean(IContextConfigurationFactory.class).
                    createContextConfiguration(CFDP, false);
            parentContext.getContextId().setType(CFDP.name());
            parentContext.setRestPort(UnsignedInteger.valueOf(port));

            try {
                archiveController.getContextConfigStore().insertContext(parentContext);
                log.info(Markers.CONTEXT, "Created context ID " + parentContext.getContextId().getNumber(),
                        " at time ", parentContext.getContextId().getStartTimeStr());
            } catch (DatabaseException de) {
                log.error("Error saving context: " + ExceptionTools.getMessage(de));
            }

        } else {
            log.warn("Skipping context registration due to " + (!databaseProperties.getUseDatabase() ?
                    "database being disabled" :
                    "archive controller not set up"));
        }

    }

    private void stopDatabaseStores() {

        if (archiveController != null) {
            archiveController.stopAllStores();
            archiveController.shutDown();
        }

        archiveController = null;
    }

    private void checkRequiredDirectoriesAndExitIfError() {

        final List<String> directoryIdToPathMap = new ArrayList<>(6);
        directoryIdToPathMap.add(getTemporaryFilesDirectory());
        directoryIdToPathMap.add(getSavedStateDirectory());
        directoryIdToPathMap.add(getUplinkFilesTopLevelDirectory());
        directoryIdToPathMap.add(getOutboundPduFilesystemSinkDirectory());
        directoryIdToPathMap.add(getFinishedDownlinkFilesTopLevelDirectory());
        directoryIdToPathMap.add(getActiveDownlinkFilesTopLevelDirectory());

        // MPCS-10634 4/9/19 Create directories if missing
        for (final String s : directoryIdToPathMap) {
            try {
                // Pass in a fake file to create the actual directories we're interested in, i.e. parents
                cfdpFileUtil.createDirectoriesIfNotExist(s);
            } catch (IOException e) {
                log.error("Could not successfully create required directory: ",
                        ExceptionTools.getMessage(e), ". Exiting...");
                ((ConfigurableApplicationContext) appContext).close();
            }

        }

    }

    /**
     * Return the session store
     *
     * @return Session store or {@code null} if either archive controller or its session store is {@code null}
     */
    public ISessionStore getSessionStore() {
        return archiveController != null ? archiveController.getSessionStore() : null;
    }

    /**
     * Return the host store
     *
     * @return Host store or {@code null} if either archive controller or its host store is {@code null}
     */
    public IHostStore getHostStore() {
        return archiveController != null ? archiveController.getHostStore() : null;
    }

    /**
     * Return the context configuration
     *
     * @return Context config
     */
    public ISimpleContextConfiguration getContextConfig() {
        return parentContext;
    }

    /**
     * @return the configProperties
     */
    public Properties getProperties() {
        return configProperties;
    }

    public String getProperty(final String key) {
        return configProperties.getProperty(key);
    }

    public Properties updateProperties(final Properties propertiesToUpdate) {
        final Properties updatedProperties = new Properties();

        for (final String key : propertiesToUpdate.stringPropertyNames()) {

            if (EConfigurationPropertyKey.getAllFullKeyStrings().contains(key)) {

                synchronized (configPropertiesLock) {
                    configProperties.setProperty(key, propertiesToUpdate.getProperty(key));
                }

                updatedProperties.setProperty(key, propertiesToUpdate.getProperty(key));
            } else {
                log.warn("Configuration update skipping invalid key " + key);
            }

        }

        if (updatedProperties.size() > 0 && !configAutosaveDisabled()) {
            // Also save the properties to file
            savePropertiesToConfigFile();
        }

        // For select properties, apply updates now
        // MPCS-9929  - 8/24/2018 - Removed ability to configure CFDP Processor's log level dynamically
        //if (updatedProperties.containsKey(EConfigurationPropertyKey.LOG_LEVEL_PROPERTY.toString())) {
        //    loggingSystem.setLogLevel(appRootLoggerName != null ? appRootLoggerName : LoggingSystem.ROOT_LOGGER_NAME,
        //            getLogLevel());
        //}

        return updatedProperties;
    }

    public boolean deleteProperty(final String key) {
        Object previousValue = null;

        synchronized (configPropertiesLock) {
            previousValue = configProperties.remove(key);
        }

        if (previousValue != null && !configAutosaveDisabled()) {
            // Also save the properties to file
            savePropertiesToConfigFile();
        }

        return previousValue == null ? false : true;
    }

    /**
     * Get the application name and version string.
     *
     * @return Application's name and version
     */
    public String getAppNameVer() {
        return appNameVer;
    }

    public String getCurrentActualPort() {
        return String.valueOf(port);
    }

    /**
     * @return the EConfigurationPropertyKey.LOCAL_CFDP_ENTITY_ID_PROPERTY property
     * value
     */
    public String getLocalCfdpEntityId() {
        return configProperties.getProperty(EConfigurationPropertyKey.LOCAL_CFDP_ENTITY_ID_PROPERTY.toString());
    }

    /**
     * @return the EConfigurationPropertyKey.ACTION_RING_BUFFER_SIZE_PROPERTY
     * property value
     */
    public int getActionRingBufferSize() {
        return Integer.parseInt(
                configProperties.getProperty(EConfigurationPropertyKey.ACTION_RING_BUFFER_SIZE_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.INBOUND_PDU_RING_BUFFER_SIZE_PROPERTY
     * property value
     */
    public int getInboundPduRingBufferSize() {
        return Integer.parseInt(configProperties
                .getProperty(EConfigurationPropertyKey.INBOUND_PDU_RING_BUFFER_SIZE_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.INGEST_ACTION_DISRUPTOR_RING_BUFFER_SIZE_PROPERTY
     * property value
     */
    public int getIngestActionDisruptorRingBufferSize() {
        return Integer.parseInt(configProperties
                .getProperty(EConfigurationPropertyKey.INGEST_ACTION_DISRUPTOR_RING_BUFFER_SIZE_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MESSAGE_DISRUPTOR_RING_BUFFER_SIZE_PROPERTY
     * property value
     */
    public int getMessageDisruptorRingBufferSize() {
        return Integer.parseInt(configProperties
                .getProperty(EConfigurationPropertyKey.MESSAGE_DISRUPTOR_RING_BUFFER_SIZE_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.INGEST_ACTION_DISRUPTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY
     * property value
     */
    public long getIngestActionDisruptorShutdownTimeoutMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.INGEST_ACTION_DISRUPTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MESSAGE_DISRUPTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY
     * property value
     */
    public long getMessageDisruptorShutdownTimeoutMillis() {
        return Long.parseLong(configProperties
                .getProperty(EConfigurationPropertyKey.MESSAGE_DISRUPTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.INBOUND_PDU_FILE_INGESTION_MINIMUM_READ_INTERVAL_MILLIS_PROPERTY
     * property value
     */
    public long getInboundPduFileIngestionMinimumReadIntervalMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.INBOUND_PDU_FILE_INGESTION_MINIMUM_READ_INTERVAL_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.WORKER_TASKS_EXECUTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY
     * property value
     */
    public long getWorkerTasksExecutorShutdownTimeoutMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.WORKER_TASKS_EXECUTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.ENGINE_CYCLER_TASK_EXECUTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY
     * property value
     */
    public long getEngineCyclerTaskExecutorShutdownTimeoutMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.ENGINE_CYCLER_TASK_EXECUTOR_SHUTDOWN_TIMEOUT_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.ENGINE_CYCLE_MINIMUM_INTERVAL_WHEN_IDLE_MILLIS_PROPERTY
     * property value
     */
    public long getEngineCycleMinimumIntervalWhenIdleMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.ENGINE_CYCLE_MINIMUM_INTERVAL_WHEN_IDLE_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.PDU_FILE_READ_BUFFER_SIZE_BYTES_PROPERTY
     * property value
     */
    public int getPduFileReadBufferSize() {
        return Integer.parseInt(configProperties
                .getProperty(EConfigurationPropertyKey.PDU_FILE_READ_BUFFER_SIZE_BYTES_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.DISABLE_CONFIG_AUTOSAVE_PROPERTY
     * property value
     */
    private boolean configAutosaveDisabled() {
        return Boolean.parseBoolean(
                configProperties.getProperty(EConfigurationPropertyKey.DISABLE_CONFIG_AUTOSAVE_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.WRITABLE_MIB_FILE_PROPERTY property
     * value
     */
    public String getWritableMibFile() {
        return configProperties.getProperty(EConfigurationPropertyKey.WRITABLE_MIB_FILE_PROPERTY.toString());
    }

    /**
     * @return the EConfigurationPropertyKey.DISABLE_MIB_AUTOSAVE_PROPERTY property
     * value
     */
    public boolean mibAutosaveDisabled() {
        return Boolean.parseBoolean(
                configProperties.getProperty(EConfigurationPropertyKey.DISABLE_MIB_AUTOSAVE_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.INSTANCE_ID_PROPERTY property value
     */
    public String getInstanceId() {
        return configProperties.getProperty(EConfigurationPropertyKey.INSTANCE_ID_PROPERTY.toString());
    }

    /**
     * @return the EConfigurationPropertyKey.OUTBOUND_PDU_ENABLED_PROPERTY property
     * value
     */
    public boolean isOutboundPduEnabled() {
        return Boolean.parseBoolean(
                configProperties.getProperty(EConfigurationPropertyKey.OUTBOUND_PDU_ENABLED_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.OUTBOUND_PDU_SINK_TYPE_PROPERTY
     * property value
     */
    public EOutboundPduSinkType getOutboundPduSinkType() {
        return EOutboundPduSinkType.valueOf(
                configProperties.getProperty(EConfigurationPropertyKey.OUTBOUND_PDU_SINK_TYPE_PROPERTY.toString())
                        .toUpperCase().toUpperCase());
    }

    /**
     * @return the EConfigurationPropertyKey.OUTBOUND_PDU_URI_PROPERTY property
     * value
     */
    public String getOutboundPduUri() {
        return configProperties.getProperty(EConfigurationPropertyKey.OUTBOUND_PDU_URI_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.OUTBOUND_PDU_URI_SINK_MINIMUM_SEND_INTERVAL_MILLIS_PROPERTY
     * property value
     */
    public long getOutboundPduUriMinimumSendIntervalMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.OUTBOUND_PDU_URI_SINK_MINIMUM_SEND_INTERVAL_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_DIRECTORY_PROPERTY
     * property value
     */
    public String getOutboundPduFilesystemSinkDirectory() {
        return configProperties
                .getProperty(EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_DIRECTORY_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_FILE_PREFIX_PROPERTY
     * property value
     */
    public String getOutboundPduFilesystemSinkFilePrefix() {
        return configProperties
                .getProperty(EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_FILE_PREFIX_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_FILE_EXTENSION_PROPERTY
     * property value
     */
    public String getOutboundPduFilesystemSinkFileExtension() {
        return configProperties
                .getProperty(EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_FILE_EXTENSION_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_FILE_NUMBER_WIDTH_PROPERTY
     * property value
     */
    public int getOutboundPduFilesystemSinkFileNumberWidth() {
        return Integer.parseInt(configProperties.getProperty(
                EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_FILE_NUMBER_WIDTH_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_MINIMUM_WRITE_INTERVAL_MILLIS_PROPERTY
     * property value
     */
    public long getOutboundPduFilesystemMinimumWriteIntervalMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.OUTBOUND_PDU_FILESYSTEM_SINK_MINIMUM_WRITE_INTERVAL_MILLIS_PROPERTY
                        .toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.ACTION_RESULT_TIMEOUT_MILLIS_PROPERTY
     * property value
     */
    public long getActionResultTimeoutMillis() {
        return Long.parseLong(configProperties
                .getProperty(EConfigurationPropertyKey.ACTION_RESULT_TIMEOUT_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.TRANSACTION_SEQUENCE_NUMBER_FILE_PROPERTY
     * property value
     */
    public String getTransactionSequenceNumberFile() {
        return configProperties
                .getProperty(EConfigurationPropertyKey.TRANSACTION_SEQUENCE_NUMBER_FILE_PROPERTY.toString());
    }

    /**
     * @return the EConfigurationPropertyKey.DEFAULT_SERVICE_CLASS_PROPERTY property
     * value
     */
    public byte getDefaultServiceClass() {
        return Byte.parseByte(
                configProperties.getProperty(EConfigurationPropertyKey.DEFAULT_SERVICE_CLASS_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.LOG_LEVEL_PROPERTY property value
     */
    // MPCS-9929 - 8/24/2018 - Removed ability to configure CFDP Processor's log level dynamically
    //public LogLevel getLogLevel() {
    //    return LogLevel.valueOf(
    //            configProperties.getProperty(EConfigurationPropertyKey.LOG_LEVEL_PROPERTY.toString()).toUpperCase());
    //}

    /**
     * @return the
     * EConfigurationPropertyKey.FINISHED_DOWNLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY
     * property value
     */
    @Override
    public String getFinishedDownlinkFilesTopLevelDirectory() {
        return configProperties
                .getProperty(EConfigurationPropertyKey.FINISHED_DOWNLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.ACTIVE_DOWNLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY
     * property value
     */
    @Override
    public String getActiveDownlinkFilesTopLevelDirectory() {
        return configProperties
                .getProperty(EConfigurationPropertyKey.ACTIVE_DOWNLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.UNKNOWN_DESTINATION_FILENAME_DOWNLINK_FILES_SUBDIRECTORY_PROPERTY
     * property value
     */
    @Override
    public String getUnknownDestinationFilenameDownlinkFilesSubdirectory() {
        return configProperties.getProperty(
                EConfigurationPropertyKey.UNKNOWN_DESTINATION_FILENAME_DOWNLINK_FILES_SUBDIRECTORY_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.UPLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY
     * property value
     */
    @Override
    public String getUplinkFilesTopLevelDirectory() {
        return configProperties
                .getProperty(EConfigurationPropertyKey.UPLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.CONSECUTIVE_FILESTORE_FAILURES_BEFORE_DECLARING_ERROR_THRESHOLD_PROPERTY
     * property value
     */
    public int getConsecutiveFilestoreFailuresBeforeDeclaringErrorThreshold() {
        return Integer.parseInt(configProperties.getProperty(
                EConfigurationPropertyKey.CONSECUTIVE_FILESTORE_FAILURES_BEFORE_DECLARING_ERROR_THRESHOLD_PROPERTY
                        .toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.SAVED_STATE_DIRECTORY_PROPERTY property
     * value
     */
    public String getSavedStateDirectory() {
        return configProperties.getProperty(EConfigurationPropertyKey.SAVED_STATE_DIRECTORY_PROPERTY.toString());
    }

    /**
     * @return the EConfigurationPropertyKey.TEMPORARY_FILES_DIRECTORY_PROPERTY
     * property value
     */
    @Override
    public String getTemporaryFilesDirectory() {
        return configProperties.getProperty(EConfigurationPropertyKey.TEMPORARY_FILES_DIRECTORY_PROPERTY.toString());
    }

    /**
     * @return the
     * EConfigurationPropertyKey.FINISHED_TRANSACTIONS_HISTORY_KEEP_TIME_MILLIS_PROPERTY
     * property value
     */
    public long getFinishedTransactionsHistoryKeepTimeMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.FINISHED_TRANSACTIONS_HISTORY_KEEP_TIME_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.FINISHED_TRANSACTIONS_HISTORY_PURGE_PERIOD_MILLIS_PROPERTY
     * property value
     */
    public long getFinishedTransactionsHistoryPurgePeriodMillis() {
        return Long.parseLong(configProperties.getProperty(
                EConfigurationPropertyKey.FINISHED_TRANSACTIONS_HISTORY_PURGE_PERIOD_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MAXIMUM_OPEN_UPLINK_TRANSACTIONS_PER_REMOTE_ENTITY_PROPERTY
     * property value
     */
    public int getMaximumOpenUplinkTransactionsPerRemoteEntity() {
        return Integer.parseInt(configProperties.getProperty(
                EConfigurationPropertyKey.MAXIMUM_OPEN_UPLINK_TRANSACTIONS_PER_REMOTE_ENTITY_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MAXIMUM_DESTINATION_FILENAME_LENGTH_PROPERTY
     * property value
     */
    public int getMaximumDestinationFilenameLength() {
        return Integer.parseInt(configProperties
                .getProperty(EConfigurationPropertyKey.MAXIMUM_DESTINATION_FILENAME_LENGTH_PROPERTY.toString()));
    }
    /**
     * @return the
     * EConfigurationPropertyKey.MAXIMUM_SOURCE_FILENAME_LENGTH_PROPERTY
     * property value
     */
    public int getMaximumSourceFilenameLength() {
        return Integer.parseInt(configProperties
                                        .getProperty(EConfigurationPropertyKey.MAXIMUM_SOURCE_FILENAME_LENGTH_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.MAXIMUM_UPLINK_FILE_SIZE_BYTES_PROPERTY
     * property value
     */
    public long getMaximumUplinkFileSizeBytes() {
        return Long.parseUnsignedLong(configProperties
                .getProperty(EConfigurationPropertyKey.MAXIMUM_UPLINK_FILE_SIZE_BYTES_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MAXIMUM_UPLINK_FILE_SIZES_TOTAL_BYTES_PER_REMOTE_ENTITY_PROPERTY
     * property value
     */
    public BigInteger getMaximumUplinkFileSizesTotalBytesPerRemoteEntity() {
        return new BigInteger(configProperties.getProperty(
                EConfigurationPropertyKey.MAXIMUM_UPLINK_FILE_SIZES_TOTAL_BYTES_PER_REMOTE_ENTITY_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.AUTO_STATE_SAVE_PERIOD_MILLIS_PROPERTY
     * property value
     */
    public int getAutoStateSavePeriodMillis() {
        return Integer.parseInt(configProperties
                .getProperty(EConfigurationPropertyKey.AUTO_STATE_SAVE_PERIOD_MILLIS_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPICS_OVERRIDE_PROPERTY
     * property value
     */
    public String[] getMessageServiceInboundPduRootTopicsOverride() {

        /*-
         * The split pattern string below will split at each comma, which may or may not
         * be preceded and/or followed by spaces, but will not split if the comma is
         * inside double-quotes.
         */
        return configProperties
                .getProperty(EConfigurationPropertyKey.MESSAGE_SERVICE_INBOUND_PDU_ROOT_TOPICS_OVERRIDE_PROPERTY.toString()).trim()
                .split("\\s*,(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)\\s*", -1);
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MESSAGE_SERVICE_INBOUND_PDU_HANDLER_QUEUE_SIZE_PROPERTY
     * property value
     */
    public int getMessageServiceInboundPduHandlerQueueSizeProperty() {
        return Integer.parseInt(configProperties.getProperty(
                EConfigurationPropertyKey.MESSAGE_SERVICE_INBOUND_PDU_HANDLER_QUEUE_SIZE_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MESSAGE_SERVICE_PROGRESS_CFDP_INDICATIONS_PUBLISHING_ENABLED_PROPERTY
     * property value
     */
    public boolean getMessageServiceProgressCfdpIndicationsPublishingEnabledProperty() {
        return Boolean.parseBoolean(configProperties.getProperty(
                EConfigurationPropertyKey.MESSAGE_SERVICE_PROGRESS_CFDP_INDICATION_MESSAGE_PUBLISHING_ENABLED_PROPERTY
                        .toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.MESSAGE_SERVICE_PDU_MESSAGE_PUBLISHING_ENABLED_PROPERTY
     * property value
     */
    public boolean getMessageServicePduMessagePublishingEnabledProperty() {
        return Boolean.parseBoolean(configProperties.getProperty(
                EConfigurationPropertyKey.MESSAGE_SERVICE_PDU_MESSAGE_PUBLISHING_ENABLED_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.DOWNLINK_METADATA_INCLUDE_PDU_LOG_ENABLED_PROPERTY
     * property value
     */
    @Override
    public boolean getDownlinkMetadataIncludePduLogEnabledProperty() {
        return Boolean.parseBoolean(configProperties
                .getProperty(EConfigurationPropertyKey.DOWNLINK_METADATA_INCLUDE_PDU_LOG_ENABLED_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.UPLINK_METADATA_INCLUDE_PDU_LOG_ENABLED_PROPERTY
     * property value
     */
    @Override
    public boolean getUplinkMetadataIncludePduLogEnabledProperty() {
        return Boolean.parseBoolean(configProperties
                .getProperty(EConfigurationPropertyKey.UPLINK_METADATA_INCLUDE_PDU_LOG_ENABLED_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.AMPCS_PRODUCT_PLUGIN_ENABLED_PROPERTY
     * property value
     */
    public boolean isAmpcsProductPluginEnabled() {
        return Boolean.parseBoolean(configProperties
                .getProperty(EConfigurationPropertyKey.AMPCS_PRODUCT_PLUGIN_ENABLED_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.PDPP_ADDER_PROPERTY
     * property value
     */
    public boolean isPdppEnabled() {
        return Boolean.parseBoolean(configProperties.getProperty(EConfigurationPropertyKey.PDPP_ADDER_PROPERTY.toString()));
    }

    /**
     * @return the
     * EConfigurationPropertyKey.INBOUND_PDU_FROM_LINKSIM_PROPERTY
     * property value
     */
    public boolean inboundPduShouldBeFromLinkSim() {
        return Boolean.parseBoolean(configProperties
                .getProperty(EConfigurationPropertyKey.INBOUND_PDU_FROM_LINKSIM_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.MESSAGE_SERVICE_PUBLISHING_ROOT_TOPIC_OVERRIDE_PROPERTY property
     * value
     */
    public String getMessageServicePublishingRootTopicOverride() {
        return configProperties.getProperty(EConfigurationPropertyKey.MESSAGE_SERVICE_PUBLISHING_ROOT_TOPIC_OVERRIDE_PROPERTY.toString());
    }

    /**
     * @return the EConfigurationPropertyKey.MESSAGES_TO_USER_MAP_FILE_PROPERTY property value
     */
    public String getMessagesToUserMapFile() {
        return configProperties.getProperty(EConfigurationPropertyKey.MESSAGES_TO_USER_MAP_FILE_PROPERTY.toString());
    }

    /**
     * @return the EConfigurationPropertyKey.MESSAGES_TO_USER_DIRECT_INPUT_ENABLED_PROPERTY property value
     */
    public boolean isMessagesToUserDirectInputEnabled() {
        return Boolean.parseBoolean(configProperties
                .getProperty(EConfigurationPropertyKey.MESSAGES_TO_USER_DIRECT_INPUT_ENABLED_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.MESSAGES_TO_USER_ALWAYS_REQUIRED_PROPERTY property value
     */
    public boolean isMessagesToUserAlwaysRequired() {
        return Boolean.parseBoolean(configProperties
                .getProperty(EConfigurationPropertyKey.MESSAGES_TO_USER_ALWAYS_REQUIRED_PROPERTY.toString()));
    }


    /**
     * @return the EConfigurationPropertyKey.EOF_PDU_CHECKSUM_VALIDATION_ENABLED_PROPERTY property value
     */
    public boolean isEofPduChecksumValidationEnabled() {
        return Boolean.parseBoolean(configProperties.getProperty(EConfigurationPropertyKey.EOF_PDU_CHECKSUM_VALIDATION_ENABLED_PROPERTY.toString()));
    }

    /**
     * @return the EConfigurationPropertyKey.EOF_PDU_CHECKSUM_ALGORITHM property value
     */
    public ChecksumAlgorithm getEofPduChecksumAlgorithm() {
        return ChecksumAlgorithm.valueOf(configProperties
                .getProperty(EConfigurationPropertyKey.EOF_PDU_CHECKSUM_ALGORITHM_PROPERTY.toString()));
    }

    /**
     * @return whether or not the available file query endpoint is enabled
     */
    public boolean isQueryForAvailableUplinkFilesEnabled() {
        return Boolean.parseBoolean(configProperties.getProperty(EConfigurationPropertyKey.AVAILABLE_UPLINK_FILE_QUERY_PROPERTY.toString()));
    }
}