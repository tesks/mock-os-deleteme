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
package jpl.gds.db.impl.aggregate.batch.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.db.api.sql.fetch.AlarmControl;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchProcessor;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchWriterFactory;
import jpl.gds.db.api.sql.fetch.aggregate.RecordBatchContainer;
import jpl.gds.db.api.types.IDbChannelSampleFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.DatabaseTimeRange;

/**
 * The is the base class of the Batch Processor
 *
 * @param <T>
 */
public abstract class BatchProcessor<T> implements IBatchProcessor {

	protected long batchProcessorNumber;
	protected Tracer trace;
	protected RecordBatchContainer<T> batchContainer;
	protected List<String> csvColumns;
	
	protected DatabaseTimeRange databaseTimeRange;
	protected IDbChannelSampleFactory dbChannelSampleFactory;
	protected IAggregateFetchConfig config;
	protected Set<String> channelMap;
	protected AlarmControl alarmControl;
	protected String outputDir;
	protected int orderByType;
	protected String batchId;
	protected Template template;
	protected Map<String, Object> templateGlobalContext;
	protected TemplateManager templateManager;
	protected IBatchWriterFactory batchWriterFactory;
	
	protected boolean isUsingTimeRange;
	protected MissionProperties missionProps;
	
	/**
	 * Constructor.
	 * 
	 * @param appContext the Spring Application Context 
	 * @param batchContainer the record batch container
	 */
	public BatchProcessor(final ApplicationContext appContext, final RecordBatchContainer<T> batchContainer) {
    	this.config = appContext.getBean(IAggregateFetchConfig.class);
    	this.batchWriterFactory = appContext.getBean(IBatchWriterFactory.class);
    	this.dbChannelSampleFactory = appContext.getBean(IDbChannelSampleFactory.class);
    	this.missionProps = appContext.getBean(MissionProperties.class);
        this.trace = TraceManager.getTracer(appContext, Loggers.DB_FETCH);
    	
    	if (config.isUsingAlarmFilter()) {
    		this.alarmControl = config.getAlarms();
    	}
    	this.batchContainer = batchContainer;
    	
    	this.csvColumns = new ArrayList<>();
		for (final String name: config.getCsvColumns()) {
			this.csvColumns.add(name.toUpperCase());
		}
		
		this.orderByType = config.getOrderByType();
		this.channelMap = config.getChannelLookupTable();
		
		if (config.isUsingTimeRange()) {
			isUsingTimeRange = true;
			this.databaseTimeRange = config.getDatabaseTimeRanges();
		}
		
		if (config.isTemplateSpecified()) {
			this.templateManager = config.getNewTemplateManager();
			try {
				this.template = templateManager.getTemplate(config.getFullTemplateName(), true);
			} catch (final TemplateException e) {
				e.printStackTrace();
			}
			this.templateGlobalContext = config.getTemplateGlobalContext();
		}
	
		this.batchId = batchContainer.getBatchId();		
		this.outputDir = config.getChunkDir();
	}
	
	@Override
    public void run() {
		final long batchStart = System.nanoTime();
		batchProcessorNumber = Thread.currentThread().getId();
        processBatch();
        trace.debug(AggregateFetchMarkers.BATCH_PROCESSOR, "BatchProcessor # " + batchProcessorNumber 
        		+ " for: " + batchId 
        		+ " Total Processing Time: " + (System.nanoTime() - batchStart)/1000000.0 + " msecs");
    }
}
