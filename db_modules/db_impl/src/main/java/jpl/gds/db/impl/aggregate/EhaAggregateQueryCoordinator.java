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
package jpl.gds.db.impl.aggregate;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jpl.gds.db.api.sql.fetch.aggregate.*;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.db.api.types.IDbQueryableFactory;
import jpl.gds.db.impl.aggregate.batch.merge.ParallelMergeSort;
import jpl.gds.db.impl.aggregate.batch.output.OutputController;
import jpl.gds.db.impl.aggregate.batch.output.StringRecordOutputWriter;
import jpl.gds.db.impl.aggregate.batch.process.BoundedExecutor;
import jpl.gds.db.impl.aggregate.batch.process.ChangesOnlyFilter;
import jpl.gds.db.impl.aggregate.batch.process.RecordBatchHandler;
import jpl.gds.db.impl.aggregate.batch.query.AggregateQueryStreamProcessor;
import jpl.gds.db.impl.types.DatabaseChannelSample;
import jpl.gds.eha.api.channel.ChannelChangeFilter;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.template.TemplateException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.springframework.context.ApplicationContext;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class coordinates all EHA aggregate query processing components.
 * DB query ResultSet should be specified before starting the thread.
 *
 */
public class EhaAggregateQueryCoordinator implements IEhaAggregateQueryCoordinator {
	/**
	 * Final merge process starts when the temp file count is reduced to less than or equal to this threshold
	 */
	private static final int FINAL_MERGE_TEMP_FILE_COUNT_BOUNDARY = 20;

	private final ApplicationContext appContext;
	private ResultSet resultSet;

	private int numberOfBatchProcessorThreads;
	private final ConcurrentMap<String, ProcessedBatchInfo> recordCacheMap = new ConcurrentHashMap<>();;

	private BlockingQueue<List<String>> outputRecordQueue;
	private LinkedList<String> batchIdList = new LinkedList<>();
	private final Tracer trace;

	private IBoundedExecutor boundedExecutor;
	private ExecutorService batchProcessorThreadPoolService;
	private final IAggregateFetchConfig config;

	private AggregateQueryStreamProcessor queryStreamProcessor;
	private final IBatchProcessorBuilder<IEhaAggregateDbRecord> processorBuilder;

	
	private final IBatchMergeFactory batchMergeFactory;
	private final IBatchReaderFactory batchReaderFactory;
	private final IBatchWriterFactory batchWriterFactory;
	private final IIndexReaderFactory indexReaderFactory;
	private IRecordBatchHandler<IEhaAggregateDbRecord> batchHandler;
	private IOutputController<String> outputController;
	
	private Thread queryStreamProcessorThread;
	private Thread outputControllerThread;
	private Thread parallelMergeSortThread;
	private Thread mergeProcessThread;
	
	private ParallelMergeSort parallelMergeSort;
	
	private boolean readyForIntermediateMerge;
	private boolean parallelMegeEnabled;
	
	private AtomicInteger intermediaryBatchCnt;
	
	private int batchCnt;
	private boolean running;
	private ChannelChangeFilter changeFilter;
	private IOutputConsumer<String> outputConsumer;
	private List<IChannelStreamFilter<String>> outputStreamFilters;
	private File tempDir;
	
	@SuppressWarnings("unchecked")
	public EhaAggregateQueryCoordinator(final ApplicationContext appContext) {
		this.appContext = appContext;
		this.config = appContext.getBean(IAggregateFetchConfig.class);
		this.batchMergeFactory = appContext.getBean(IBatchMergeFactory.class);
		this.batchReaderFactory = appContext.getBean(IBatchReaderFactory.class);
		this.batchWriterFactory = appContext.getBean(IBatchWriterFactory.class);
		this.indexReaderFactory = appContext.getBean(IIndexReaderFactory.class);
        this.trace = TraceManager.getTracer(appContext, Loggers.DB_FETCH);
				
		this.processorBuilder = (IBatchProcessorBuilder<IEhaAggregateDbRecord>) appContext
				.getBean(IDbQueryableFactory.BATCH_PROCESSOR_FACTORY);
	}

	
	/**
	 * Initialize and start up all required components
	 * @throws AggregateFetchException 
	 * @throws InterruptedException 
	 */
	public void init() throws AggregateFetchException {
		batchCnt = 1;
		
		intermediaryBatchCnt = new AtomicInteger(0);
		numberOfBatchProcessorThreads = Runtime.getRuntime().availableProcessors();
	
		trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR, 
		        "System reported availableProcessors = " + numberOfBatchProcessorThreads);
		numberOfBatchProcessorThreads = config.getParallelThreads();
		trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR,
		        "Configured number of parallel threads: " + numberOfBatchProcessorThreads);
		
		outputStreamFilters = new ArrayList<>();
		
		if (config.isUsingChangesOnlyFilter()) {
			changeFilter = new ChannelChangeFilter(appContext.getBean(EhaProperties.class));
			outputStreamFilters.add(new ChangesOnlyFilter(appContext, changeFilter, config.getCsvColumns(), config.isShowColumnHeaders()));
		}
		outputConsumer = new StringRecordOutputWriter(config.getPrintWriter(), trace);
		
		// Create the temporary directory where all the batch files will be stored
		tempDir = new File(config.getChunkDir());
		tempDir.mkdir();

		batchProcessorThreadPoolService = Executors.newFixedThreadPool(numberOfBatchProcessorThreads);
		boundedExecutor = new BoundedExecutor(batchProcessorThreadPoolService, config.getProcessorQueueSize());

		batchHandler = new RecordBatchHandler<IEhaAggregateDbRecord>(processorBuilder, boundedExecutor);
		
		outputRecordQueue = new ArrayBlockingQueue<>(config.getOutputControllerQueueSize());
		
		
		outputController = new OutputController<String>(
				appContext, 
				outputRecordQueue, 
				outputConsumer,
				trace);
		
		outputController.setStreamFilters(outputStreamFilters);
		
		// Start OutputController thread
		// outputProducer = new OutputProducer(appContext, dataRecordQueue, outputFile);
		outputControllerThread = new Thread(outputController, "Output Controller Thread");
		outputControllerThread.start();
		
		// Write the velocity template header if we are using templates for output
		if (config.isTemplateSpecified()) {
			writeTemplateHeader();
		}		
		
		if (!config.isTemplateSpecified() && config.isShowColumnHeaders() ) {			
			final String csvHeader = new DatabaseChannelSample(appContext).getCsvHeader(config.getCsvHeaders());
			
            try {
                outputRecordQueue.put(Arrays.asList(csvHeader));
            } catch (final InterruptedException e) {
                throw new AggregateFetchException("Query Coordinator encountered an exception "
                        + "while putting the CSV header into the Output Record Queue: " + e.getMessage());
            }
		}

		// Start the Merge process thread
		mergeProcessThread = new Thread(batchMergeFactory.getBatchMerge(this, batchReaderFactory, indexReaderFactory), "Batch Merging Thread");
		mergeProcessThread.start();
		
		// Start the QueryStreamProcessor
		queryStreamProcessor = new AggregateQueryStreamProcessor(this, config, trace, resultSet, batchHandler);
		queryStreamProcessorThread = new Thread(queryStreamProcessor, "QueryStreamProcessorThread");
		queryStreamProcessorThread.start();

		// Join the Query Stream Processor Thread
        try {
            queryStreamProcessorThread.join();
        } catch (final InterruptedException e) {
            throw new AggregateFetchException("Query Coordinator encountered an exception "
                    + "while waiting to join the Query Stream Processor Thread: " + e.getMessage());
        }
        trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR, "Query Stream Processor Thread Is Finished");

        // Temporary files are generated only when data sorting is enabled
        // Run parallel merge sort process if temporary file count has passed the
        // threshold.
        if (config.isSortEnabled() && (recordCacheMap.size() > config.getBatchTempFileThreshold())) {
            trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR,
                    "Temporary file count: " + recordCacheMap.size() + " is greater than the configured threshold: "
                    + config.getBatchTempFileThreshold() + ", will run the Parallel Merge Sort Processor");
            parallelMergeSort = new ParallelMergeSort(this, Executors.newFixedThreadPool(numberOfBatchProcessorThreads),
                    batchReaderFactory, indexReaderFactory, batchWriterFactory, config, trace);
            parallelMergeSortThread = new Thread(parallelMergeSort, "Intermadiary Batch Merge Thread");
            parallelMergeSortThread.start();
            parallelMegeEnabled = true;
        }

        // Initiate Orderly Shutdown
        batchProcessorThreadPoolService.shutdown();

        // Wait until all submitted tasks complete
        try {
            batchProcessorThreadPoolService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            throw new AggregateFetchException("Query Coordinator encountered an exception "
                    + "while waiting for Executor Service termination: " + e.getMessage());

        }
        trace.debug("Executer Service Is Finished");

        readyForIntermediateMerge = true;

        try {
            mergeProcessThread.join();
        } catch (final InterruptedException e) {
            throw new AggregateFetchException("Query Coordinator encountered an exception "
                    + "while waiting to join the Merge Process Thread: " + e.getMessage());

        }
        trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR, "Merge Process Is Finished");

        if (config.isTemplateSpecified()) {
            writeTemplateTrailer();
        }

        initiateShutdown();

        try {
            outputControllerThread.join();
        } catch (final InterruptedException e) {
            throw new AggregateFetchException("Query Coordinator encountered an exception "
                    + "while waiting to join the Output Controller Thread: " + e.getMessage());

        }
        trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR,
                "Output Controller Is Finished");

		// Clean up happens in shutdown hook
	}


	@Override
	public boolean threadsAlive() {
		return (outputControllerThread != null && outputControllerThread.isAlive())
				|| (mergeProcessThread != null && mergeProcessThread.isAlive())
				|| (batchProcessorThreadPoolService != null && !batchProcessorThreadPoolService.isTerminated())
				|| (parallelMergeSortThread != null && parallelMergeSortThread.isAlive())
				|| (queryStreamProcessorThread != null && queryStreamProcessorThread.isAlive());
	}


    private void writeTemplateHeader() throws AggregateFetchException {
        final StringWriter writer = new StringWriter();
        final Template template;
        HashMap<String, Object> context = new HashMap<String, Object>();

        context.putAll(config.getTemplateGlobalContext());
        context.put("header", true);
        context.put("hasPacket", config.isIncludePacketInfo());
        context.put("vcidColumn", appContext.getBean(MissionProperties.class).getVcidColumnName());
         
        
        try {
            template = config.getNewTemplateManager().getTemplate(config.getFullTemplateName(), true);
            template.merge(new VelocityContext(context), writer);
            putBatchIntoOutputRecordQueue(null, Arrays.asList(writer.getBuffer().toString()));
            writer.close();
        } catch (TemplateException | IOException e) {
            throw new AggregateFetchException("Encountered an error while attempting to "
                    + "write the Velocity Template Header: " + e.getMessage());
        }

        context.clear();
        context = null;
    }


    private void writeTemplateTrailer() throws AggregateFetchException {
        final StringWriter writer = new StringWriter();
        final Template template;
        HashMap<String, Object> context = new HashMap<String, Object>();

        context.putAll(config.getTemplateGlobalContext());
        context.put("trailer", true);
        
        try {
        	template = config.getNewTemplateManager().getTemplate(config.getFullTemplateName(), true);
        	template.merge(new VelocityContext(context), writer);
        	putBatchIntoOutputRecordQueue(null, Arrays.asList(writer.getBuffer().toString()));
        	writer.close();
        } catch (TemplateException | IOException e) {
            throw new AggregateFetchException("Encountered an error while attempting to write the Velocity Template Trailer: " + e.getMessage());
        }

        context.clear();
        context = null;
    }

	@Override
	public void run() {
		if (resultSet == null) {
			throw new IllegalStateException("ResultSet not set, cannot start the Coordinator !!");
		}
		running = true;
		try {
            init();
        } catch (final AggregateFetchException e) {
            trace.error(AggregateFetchMarkers.QUERY_COORDINATOR, e.getMessage());
        }
		running = false;
	}
	
	@Override
	public void addBatchToCacheMap(final String batchId, final ProcessedBatchInfo indexCacheItem) {
		recordCacheMap.put(batchId, indexCacheItem);
	}

	@Override
	public boolean dataIsBeingProcessed() {
		if (batchProcessorThreadPoolService.isTerminated() && recordCacheMap.isEmpty() && outputRecordQueue.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}


	@Override
	public boolean batchFilesReadyForFinalMerge() {
		if (batchProcessorThreadPoolService.isTerminated() && !recordCacheMap.isEmpty()) {
			if (parallelMegeEnabled && parallelMergeSort.isRunning()) {
				return false;
			} else {
				return true;
			}
		} if (!dataIsBeingProcessed()) {
			return true;
	    } else {
			return false;
		}
	}

	@Override
	public void setResultSet(final ResultSet resultSet) {
		if (running)
			throw new RuntimeException("Cannot set ResultSet after starting the Coordinator !!");
		this.resultSet = resultSet;
	}


	@Override
	public boolean nextBatchReady() {
		synchronized (batchIdList) {
			if (!batchIdList.isEmpty() && recordCacheMap.containsKey(batchIdList.getFirst())) {
				return true;
			} else {
				return false;
			}
		}

	}

	@Override
	public void pushBatchToOutputController() throws AggregateFetchException {
		if (!nextBatchReady())
			throw new AggregateFetchException("Batch with ID: " + batchIdList.getFirst() + " not ready for processing");

		synchronized (batchIdList) {
			final String batchId = batchIdList.removeFirst();
			putBatchIntoOutputRecordQueue(batchId, recordCacheMap.remove(batchId).getRecordList());
		}
	}

	@Override
	public String getNextBatchId() {
		synchronized (batchIdList) {
			if (!batchIdList.isEmpty()) {
				return batchIdList.getFirst();
			} else {
				return "NOT SET";
			}
		}

	}

	@Override
	public void initiateShutdown() throws AggregateFetchException {
	    putBatchIntoOutputRecordQueue(null, Collections.emptyList());
	}


	@Override
	public Iterator<Entry<String, ProcessedBatchInfo>> getCacheMapIterator() {
		return recordCacheMap.entrySet().iterator();
	}


	@Override
	public ProcessedBatchInfo getBatch(final String batchId) {
		return recordCacheMap.get(batchId);
	}


	@Override
	public void removeBatch(final String batchId) {
		recordCacheMap.remove(batchId);
	}


	@Override
	public void pushBatchToOutputController(final List<String> recBatch) throws AggregateFetchException {
	    putBatchIntoOutputRecordQueue(null, recBatch);
	}

	@Override
	public int getOutputQueueSize() {
		return outputRecordQueue.size();
	}


	@Override
	public String generateBatchId() {
		final String batchId = "batch_" + batchCnt + "_" + System.nanoTime();
		synchronized (batchIdList) {
			batchIdList.add(batchId);
		}
		batchCnt++;
		return batchId;
	}


	@Override
	public void addIntermediaryMergedBatchToMap(final BatchSetContainer batchSetContainer) {
		final ProcessedBatchInfo indexCache = batchSetContainer.getBic();
		final String mergedBatchId = "Merged_" + System.nanoTime();
		for (final String key : batchSetContainer.getBatchSet().keySet()) {
			recordCacheMap.remove(key);
		}
		recordCacheMap.put(mergedBatchId, indexCache);
		intermediaryBatchCnt.getAndDecrement();
		trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR, 
		        "Atomic intermediaryBatchCnt after decrement = " + intermediaryBatchCnt);
		/*
		 * MPCS-12432: chill_get_chanvals process hangs when temp file count ends up being a multiple of 4
		 * This condition was missing an '=' sign, instead of '<=' it was just '<'
		 */				
		if (batchProcessorThreadPoolService.isTerminated() && intermediaryBatchCnt.get() == 0 && recordCacheMap.size() <= FINAL_MERGE_TEMP_FILE_COUNT_BOUNDARY) {
			readyForIntermediateMerge = false;
			parallelMergeSort.shutdown();
		}
		
		if (intermediaryBatchCnt.get() == 0 && recordCacheMap.size() > FINAL_MERGE_TEMP_FILE_COUNT_BOUNDARY) {
			readyForIntermediateMerge = true;
		}
	}


	@Override
	public boolean readyForIntermediateMerge() {
		return readyForIntermediateMerge;
	}


	@Override
	public void incrementIntermediaryBatchCount() {
		intermediaryBatchCnt.getAndIncrement();
		trace.debug(AggregateFetchMarkers.QUERY_COORDINATOR,
		        "Atomic intermediaryBatchCnt after increment = " + intermediaryBatchCnt);
	}

	
	@Override
	public void startingMergeProcessors() {
		readyForIntermediateMerge = false;
	}

	@Override
	public void removeBatchIdFromBatchIdList(String batchId) {
		synchronized (batchIdList){
			batchIdList.remove(batchId);
		}
	}

	private void putBatchIntoOutputRecordQueue(final String batchId, final List<String> recBatch) throws AggregateFetchException {
        try {
            outputRecordQueue.put(recBatch);
        } catch (final InterruptedException e) {
            
            final String errorMessage;
            if (batchId != null) {
                errorMessage = "Encountered an exception while attempting "
                        + "to push the next batch with id: " 
                        + batchId + " to the Output Record Queue: " + e.getMessage();
            } else {
                errorMessage = "Encountered an exception while attempting "
                        + "to push a batch to the Output Record Queue: " + e.getMessage();
            }
            Thread.currentThread().interrupt();
            throw new AggregateFetchException(errorMessage);
        }
    }

	@Override
	public void cleanUpTempFiles() {
		if (tempDir != null && !config.isKeepTempFiles()) {
			trace.debug("Attempting to clean up temporary directory '" , tempDir , '"');
			if (tempDir.exists()) {
				for (File file : Objects.requireNonNull(tempDir.listFiles())) {
					trace.debug("Deleting temp file: " , file.getAbsolutePath());
					file.delete();
				}
				FileSystemUtils.deleteRecursively(tempDir);
			}
		}
	}
}
