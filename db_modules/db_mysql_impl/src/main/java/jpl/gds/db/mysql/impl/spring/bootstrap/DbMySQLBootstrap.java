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
package jpl.gds.db.mysql.impl.spring.bootstrap;

import jpl.gds.db.api.rest.RestfulDbHelper;
import jpl.gds.db.api.rest.IRestfulDbHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.spring.bootstrap.IDbMySQLBootstrap;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.IFetchConfigurationMap;
import jpl.gds.db.api.sql.fetch.IFrameQueryOptionsFactory;
import jpl.gds.db.api.sql.fetch.QueryConfig;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.store.IDbSqlStoreFactory;
import jpl.gds.db.api.sql.store.IStoreConfigurationMap;
import jpl.gds.db.mysql.config.MySqlArchiveAdaptationProperties;
import jpl.gds.db.mysql.impl.sql.DbSqlArchiveController;
import jpl.gds.db.mysql.impl.sql.fetch.FrameQueryOptionsFactory;
import jpl.gds.db.mysql.impl.sql.fetch.MultimissionFetchConfigurationMap;
import jpl.gds.db.mysql.impl.sql.fetch.MultimissionFetchFactory;
import jpl.gds.db.mysql.impl.sql.order.OrderByTypeFactory;
import jpl.gds.db.mysql.impl.sql.store.MultimissionStoreConfigurationMap;
import jpl.gds.db.mysql.impl.sql.store.MultimissionStoreFactory;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;


@Configuration
public class DbMySQLBootstrap implements IDbMySQLBootstrap {
    /**
     * Autowired reference to the Spring Application Context
     */
    @Autowired
    public ApplicationContext         appContext;

    /**
     * @param sseFlag
     *            The SSE context flag
     * @return the Database Query Config bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = MYSQL_ADAPTATION_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public IMySqlAdaptationProperties getMySqlAdaptationProperties(final SseContextFlag sseFlag)
            throws BeansException, InvalidMetadataException {
        final IDatabaseProperties dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES,
                                                                    IDatabaseProperties.class);
        return new MySqlArchiveAdaptationProperties(dbProperties, sseFlag);
    }

    /**
     * @param mysqlProperties The MySqlProperties object (autowired)
     * @return the Database Query Config bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = DB_QUERY_CONFIG)
    @Scope("singleton")
    @Lazy(value = true)
    public QueryConfig getQueryConfig(final IMySqlAdaptationProperties mysqlProperties, final SseContextFlag sseFlag)
            throws BeansException, InvalidMetadataException {
        return new QueryConfig(mysqlProperties, sseFlag);
    }

    /**
     * @return the Database Archive Controller bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = DB_CONTROLLER)
    @Scope("singleton")
    @Lazy(value = true)
    public IDbSqlArchiveController getDatabaseController() throws BeansException, InvalidMetadataException {
        return new DbSqlArchiveController(appContext);
    }

    /**
     * @return the Store Configuration Map bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = STORE_CONFIG_MAP)
    @Scope("singleton")
    @Lazy(value = true)
    public IStoreConfigurationMap getStoreConfigMap() throws BeansException, InvalidMetadataException {
        return new MultimissionStoreConfigurationMap();
    }

    /**
     * @return the Fetch Configuration Map bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = FETCH_CONFIG_MAP)
    @Scope("singleton")
    @Lazy(value = true)
    public IFetchConfigurationMap getFetchConfigMap() throws BeansException, InvalidMetadataException {
        return new MultimissionFetchConfigurationMap();
    }

    /**
     * @return the Store Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = STORE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IDbSqlStoreFactory getStoreFactory() throws BeansException, InvalidMetadataException {
        return new MultimissionStoreFactory(appContext);
    }

    /**
     * @return the Fetch Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = FETCH_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IDbSqlFetchFactory getFetchFactory() throws BeansException, InvalidMetadataException {
        return new MultimissionFetchFactory(appContext);
    }

    /**
     * @return the Fetch Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = FRAME_QUERY_OPTIONS_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IFrameQueryOptionsFactory getFrameQueryOptionsFactoryFactory()
            throws BeansException, InvalidMetadataException {
        return new FrameQueryOptionsFactory(appContext);
    }

    /**
     * @return the Fetch Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = ORDER_BY_TYPE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IOrderByTypeFactory getOrderByTypeFactory() throws BeansException, InvalidMetadataException {
        return new OrderByTypeFactory(appContext);
    }

    /**
     * Gets the DB Log helper
     * @return
     */
    @Bean(name = DB_LOGGING_UTIL)
    @Scope("singleton")
    @Lazy
    public IRestfulDbHelper getRestfulLogHelper() {
        return new RestfulDbHelper(appContext);
    }
}
