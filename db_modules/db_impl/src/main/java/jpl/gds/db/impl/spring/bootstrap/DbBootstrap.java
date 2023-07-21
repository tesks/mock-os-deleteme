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
package jpl.gds.db.impl.spring.bootstrap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchMergeFactory;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchProcessorBuilder;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchReaderFactory;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchWriterFactory;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateDbRecord;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.IIndexReaderFactory;
import jpl.gds.db.api.types.IDbChannelMetaDataFactory;
import jpl.gds.db.api.types.IDbChannelSampleFactory;
import jpl.gds.db.api.types.IDbCommandFactory;
import jpl.gds.db.api.types.IDbContextConfigFactory;
import jpl.gds.db.api.types.IDbContextInfoFactory;
import jpl.gds.db.api.types.IDbEndSessionFactory;
import jpl.gds.db.api.types.IDbEvrFactory;
import jpl.gds.db.api.types.IDbFrameFactory;
import jpl.gds.db.api.types.IDbLog1553Factory;
import jpl.gds.db.api.types.IDbLogFactory;
import jpl.gds.db.api.types.IDbPacketFactory;
import jpl.gds.db.api.types.IDbProductMetadataFactory;
import jpl.gds.db.api.types.IDbQueryableFactory;
import jpl.gds.db.api.types.IDbSessionFactory;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpFileGenerationFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpFileUplinkFinishedFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpPduReceivedFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpPduSentFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestReceivedFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestResultFactory;
import jpl.gds.db.impl.aggregate.AggregateFetchConfig;
import jpl.gds.db.impl.aggregate.EhaAggregateQueryCoordinator;
import jpl.gds.db.impl.aggregate.batch.merge.BatchMergeFactory;
import jpl.gds.db.impl.aggregate.batch.process.AggregateBatchProcessorFactory;
import jpl.gds.db.impl.aggregate.batch.read.BatchReaderFactory;
import jpl.gds.db.impl.aggregate.batch.read.IndexReaderFacory;
import jpl.gds.db.impl.aggregate.batch.write.BatchWriterFactory;
import jpl.gds.db.impl.types.DbChannelMetaDataFactory;
import jpl.gds.db.impl.types.DbChannelSampleFactory;
import jpl.gds.db.impl.types.DbCommandFactory;
import jpl.gds.db.impl.types.DbContextConfigFactory;
import jpl.gds.db.impl.types.DbContextInfoFactory;
import jpl.gds.db.impl.types.DbEndSessionFactory;
import jpl.gds.db.impl.types.DbEvrFactory;
import jpl.gds.db.impl.types.DbFrameFactory;
import jpl.gds.db.impl.types.DbLog1553Factory;
import jpl.gds.db.impl.types.DbLogFactory;
import jpl.gds.db.impl.types.DbPacketFactory;
import jpl.gds.db.impl.types.DbProductMetadataFactory;
import jpl.gds.db.impl.types.DbSessionFactory;
import jpl.gds.db.impl.types.DbSessionInfoFactory;
import jpl.gds.db.impl.types.cfdp.DbCfdpFileGenerationFactory;
import jpl.gds.db.impl.types.cfdp.DbCfdpFileUplinkFinishedFactory;
import jpl.gds.db.impl.types.cfdp.DbCfdpIndicationFactory;
import jpl.gds.db.impl.types.cfdp.DbCfdpPduReceivedFactory;
import jpl.gds.db.impl.types.cfdp.DbCfdpPduSentFactory;
import jpl.gds.db.impl.types.cfdp.DbCfdpRequestReceivedFactory;
import jpl.gds.db.impl.types.cfdp.DbCfdpRequestResultFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;


@Configuration
public class DbBootstrap {
	/**
	 * Autowired reference to the Spring Application Context
	 */
	@Autowired
	public ApplicationContext appContext;

	/**
	 * @return the Database Channel Sample Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CHANNEL_SAMPLE_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbChannelSampleFactory createDbChannelSampleFactory() throws BeansException, InvalidMetadataException {
		return new DbChannelSampleFactory(appContext);
	}

	/**
	 * @return the Database Evr Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_EVR_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbEvrFactory createDbEvrFactory() throws BeansException, InvalidMetadataException {
		return new DbEvrFactory(appContext);
	}

	/**
	 * @return the Database Command Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_COMMAND_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCommandFactory createDbCommandFactory() throws BeansException, InvalidMetadataException {
		return new DbCommandFactory(appContext);
	}

	/**
	 * @return the Database Product Metadata Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_PRODUCT_METADATA_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbProductMetadataFactory createDbProductMetadataFactory() throws BeansException, InvalidMetadataException {
		return new DbProductMetadataFactory(appContext);
	}

	/**
	 * @return the Database Log Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_LOG_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbLogFactory createDbLogFactory() throws BeansException, InvalidMetadataException {
		return new DbLogFactory(appContext);
	}

	/**
	 * @return the Database DB 1553 Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_1553_LOG_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbLog1553Factory createDb1553LogFactory() throws BeansException, InvalidMetadataException {
		return new DbLog1553Factory(appContext);
	}

	/**
	 * @return the Database Channel Metadata Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CHANNEL_METADATA_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbChannelMetaDataFactory createDbChannelMetaDataFactory() throws BeansException, InvalidMetadataException {
		return new DbChannelMetaDataFactory(appContext);
	}

	/**
	 * @return the Database End Session Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_END_SESSION_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbEndSessionFactory createDbEndSessionFactory() throws BeansException, InvalidMetadataException {
		return new DbEndSessionFactory(appContext);
	}

	/**
	 * @return the Database Frame Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_FRAME_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbFrameFactory createDbFrameFactory() throws BeansException, InvalidMetadataException {
		return new DbFrameFactory(appContext);
	}

	/**
	 * @return the Database Packet Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_PACKET_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbPacketFactory createDbPacketFactory() throws BeansException, InvalidMetadataException {
		return new DbPacketFactory(appContext);
	}

	/**
	 * @return the Database Session Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_SESSION_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbSessionFactory createDbSessionFactory() throws BeansException, InvalidMetadataException {
		return new DbSessionFactory(appContext);
	}

	/**
	 * @return the Context Config Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CONTEXT_CONFIG_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbContextConfigFactory createDbContextConfigFactory() throws BeansException,
			InvalidMetadataException {
		return new DbContextConfigFactory(appContext);
	}

	/**
	 * @return the Database Session Info Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_SESSION_INFO_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbSessionInfoFactory createDbSessionInfoFactory() throws BeansException, InvalidMetadataException {
		return new DbSessionInfoFactory(appContext);
	}

	/**
	 * @return the Database Context Info Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CONTEXT_INFO_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbContextInfoFactory createDbContextInfoFactory() throws BeansException, InvalidMetadataException {
		return new DbContextInfoFactory(appContext);
	}

	/**
	 * @return the Database CFDP Indication Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CFDP_INDICATION_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCfdpIndicationFactory createDbCfdpIndicationFactory() throws BeansException, InvalidMetadataException {
		return new DbCfdpIndicationFactory(appContext);
	}

	/**
	 * @return the Database CFDP File Generation Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CFDP_FILE_GENERATION_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCfdpFileGenerationFactory createDbCfdpFileGenerationFactory() throws BeansException, InvalidMetadataException {
		return new DbCfdpFileGenerationFactory(appContext);
	}

	/**
	 * @return the Database CFDP File Uplink Finished Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CFDP_FILE_UPLINK_FINISHED_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCfdpFileUplinkFinishedFactory createDbCfdpFileUplinkFinishedFactory() throws BeansException, InvalidMetadataException {
		return new DbCfdpFileUplinkFinishedFactory(appContext);
	}

	/**
	 * @return the Database CFDP Request Received Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CFDP_REQUEST_RECEIVED_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCfdpRequestReceivedFactory createDbCfdpRequestReceivedFactory() throws BeansException, InvalidMetadataException {
		return new DbCfdpRequestReceivedFactory(appContext);
	}
	
	/**
	 * @return the Database CFDP Request Result Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CFDP_REQUEST_RESULT_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCfdpRequestResultFactory createDbCfdpRequestResultFactory() throws BeansException, InvalidMetadataException {
		return new DbCfdpRequestResultFactory(appContext);
	}
	
	/**
	 * @return the Database CFDP PDU Received Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CFDP_PDU_RECEIVED_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCfdpPduReceivedFactory createDbCfdpPduReceivedFactory() throws BeansException, InvalidMetadataException {
		return new DbCfdpPduReceivedFactory(appContext);
	}
	
	/**
	 * @return the Database CFDP PDU Sent Factory Bean
	 * @throws BeansException
	 *             if bean fails to load
	 * @throws InvalidMetadataException
	 *             if Metadata is invalid
	 */
	@Bean(name = IDbQueryableFactory.DB_CFDP_PDU_SENT_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IDbCfdpPduSentFactory createDbCfdpPduSentFactory() throws BeansException, InvalidMetadataException {
		return new DbCfdpPduSentFactory(appContext);
	}
	
    /**
     * @return the Batch Merge Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = IDbQueryableFactory.EHA_AGGREGATE_QUERY_COORDINATOR)
    @Scope("singleton")
    @Lazy(value = true)
    public IEhaAggregateQueryCoordinator getAggregateQueryQoordinator() throws BeansException, InvalidMetadataException {
        return new EhaAggregateQueryCoordinator(appContext);
    }
    
    /**
     * @return the Aggregate Fetch Config bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = IDbQueryableFactory.AGGREGATE_FETCH_CONFIG)
    @Scope("singleton")
    @Lazy(value = true)
    public IAggregateFetchConfig getAggregateFetchConfig(final SseContextFlag sseFlag)
            throws BeansException, InvalidMetadataException {
        return new AggregateFetchConfig(sseFlag);
    }
    
    /**
     * @return the Batch Reader Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = IDbQueryableFactory.BATCH_READER_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IBatchReaderFactory getBatchReaderFactory() {
        return new BatchReaderFactory(
                appContext.getBean(IAggregateFetchConfig.class));
    }
    
    /**
     * @return the Index Reader Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = IDbQueryableFactory.INDEX_READER_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IIndexReaderFactory getIndexReaderFactory() {
        return new IndexReaderFacory();
    }
    
    /**
     * @return the Batch Writer Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = IDbQueryableFactory.BATCH_WRITER_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IBatchWriterFactory getBatchWriterFactory() {
        return new BatchWriterFactory(
                appContext.getBean(IAggregateFetchConfig.class));
    }    
    
    /**
     * @return the Batch Merge Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = IDbQueryableFactory.BATCH_MERGE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IBatchMergeFactory getBatchMergeFactory() {
        return new BatchMergeFactory(
                appContext.getBean(IAggregateFetchConfig.class),
                                     TraceManager.getTracer(appContext, Loggers.DB_FETCH));
    }
    
    /**
     * @return the Batch Merge Factory bean
     * @throws BeansException
     *             if bean fails to load
     * @throws InvalidMetadataException
     *             if Metadata is invalid
     */
    @Bean(name = IDbQueryableFactory.BATCH_PROCESSOR_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IBatchProcessorBuilder<IEhaAggregateDbRecord> getBatchProcessorFactory() {
        return new AggregateBatchProcessorFactory(appContext);
    }
}
