package jpl.gds.db.impl.aggregate;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.gds.db.api.sql.fetch.AlarmControl;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.DatabaseTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.DatabaseTimeRange;

/**
 * Config object for controlling the Aggregate query processes
 *
 *
 */
public class AggregateFetchConfig extends GdsHierarchicalProperties implements IAggregateFetchConfig {
    
	private static final String PROPERTY_PREFIX = "aggregate";
	private static final String PROPERTY_FILE = "aggregate_fetch.properties";
	
	private static final String PROCESSOR_BATCH_TEMP_DIRECTORY_ROOT = "aggregate.fetch.batch.directory";
	private static final String MODULE_PATTERN_REGEX = "aggregate.fetch.modulePattern.regex";
	private static final String TEMP_FILE_MAX_THRESHOLD = "aggregate.fetch.tempFile.max.threshold";
	private static final String PARALLEL_WORKER_THREAD_COUNT = "aggregate.fetch.parallel.threads";
	private static final String QUERY_STREAM_BATCH_SIZE = "aggregate.fetch.query.stream.batch.size";
	private static final String PROCESSOR_QUEUE_SIZE = "aggregate.fetch.processor.queue.size";
	private static final String OUTPUT_CONTROLLER_QUEUE_SIZE = "aggregate.fetch.output.controller.queue.size";
	private static final String KEEP_TEMORARY_FILES_FLAG = "aggregate.fetch.keepTempFiles";
	private static final String ORDER_BY_TIME_TYPE = "aggregate.fetch.orderBy";
	private static final String BEGIN_TIME_PAD = "aggregate.fetch.beginTimePad";
	
	private static final int QUERY_STREAM_BATCH_SIZE_DEFAULT = 1000;
	private static final int PROCESSOR_QUEUE_SIZE_DEFAULT = 20;
	private static final int OUTPUT_CONTROLLER_QUEUE_SIZE_DEFAULT = 10;
	
	private static final String PROCESSOR_BATCH_TEMP_DIRECTORY_ROOT_DEFAULT = "/tmp";
	private static final String AGGREGATE_QUERY_TEMP = "aggregate_query_temp_";
	
    private int queryStreamBatchSize;
    private String  processorBatchTempDirectoryRoot;
    private int       pthreads;
    
	private boolean keepTempFiles;
	private String outputFile;
	private Map<String, Object> globalContext;
	private String tableName;
	private String templateName;
	private PrintWriter printWriter;
	private List<String> csvColumns;
	private boolean includePacketInfo;
	private AlarmControl alarms;
	private boolean isUsingAlarmFilter;
	private Set<String> channelLookup;
	private List<IChannelAggregateOrderByType> orderings;
	private DatabaseTimeRange dbTimeRanges;
	private boolean sortEnabled;
	private String modulePattern;
	private boolean changesOnly;
	private boolean showColHeaders;
	private IDbSessionPreFetch sessionPreFetch;
	private List<String> csvHeaders;
	private final String orderByString;
	private int maxTempFileThreshold;
    private int orderByType;
	private int beginTimePadInSeconds;
    
    private final long runTempDirectory;

    private final SseContextFlag               sseFlag;

    /**
     * Test constructor
     */
    public AggregateFetchConfig() {
        this(new SseContextFlag());
    }

	/**
     * Eha aggregation config constructor
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public AggregateFetchConfig(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        queryStreamBatchSize = getIntProperty(QUERY_STREAM_BATCH_SIZE, QUERY_STREAM_BATCH_SIZE_DEFAULT);
        
        processorBatchTempDirectoryRoot = getProperty(PROCESSOR_BATCH_TEMP_DIRECTORY_ROOT, PROCESSOR_BATCH_TEMP_DIRECTORY_ROOT_DEFAULT);
        pthreads = getIntProperty(PARALLEL_WORKER_THREAD_COUNT, 4);
        keepTempFiles = getBooleanProperty(KEEP_TEMORARY_FILES_FLAG, false);
        maxTempFileThreshold = getIntProperty(TEMP_FILE_MAX_THRESHOLD, 100);
        orderByString = getProperty(ORDER_BY_TIME_TYPE, "NONE");
        runTempDirectory = System.nanoTime();
        beginTimePadInSeconds = getIntProperty(BEGIN_TIME_PAD, 30);
        this.sseFlag = sseFlag;
    }

    /**
     * Get the configured chunking size
     * 
     * @return chunkSize
     */
    @Override
    public int getChunkSize() {
        return queryStreamBatchSize;
    }

    /**
     * Set the chunk size
     * 
     * @param size
     *            chunk size
     */
    @Override
    public void setChunkSize(final int size) {
        queryStreamBatchSize = size;
    }


    /**
     * Chunk directory
     * 
     * @return Writable chunk directory
     */
    @Override
    public String getChunkDir() {
        return processorBatchTempDirectoryRoot 
                + File.separator 
                + AGGREGATE_QUERY_TEMP 
                + runTempDirectory;
    }

    /**
     * Set the chunk write directory
     * 
     * @param dir
     *            chunk directory
     */
    @Override
    public void setChunkDir(final String dir) {
        processorBatchTempDirectoryRoot = dir;
    }

    /**
     * Number of parallel threads
     * 
     * @return parallel threads
     */
    @Override
    public int getParallelThreads() {
        return pthreads;
    }

    /**
     * Set the number of parallel chunking threads
     * 
     * @param amount
     *            parallel threads
     */
    @Override
    public void setParallelThreads(final int amount) {
        pthreads = amount;
    }

	@Override
    public void setKeepTempFiles(final boolean keepTempFiles) {
		this.keepTempFiles = keepTempFiles;
	}
	
	@Override
    public boolean isKeepTempFiles() {
		return keepTempFiles;
	}

	@Override
    public void setOutputFile(final String outputFile) {
		this.outputFile = outputFile;
	}
	
	@Override
    public String getOutputFile() {
		return outputFile;
	}

	@Override
    public void setTemplateGlobalContext(final Map<String, Object> globalContext) {
		this.globalContext = globalContext;
	}
	
	@Override
    public Map<String, Object> getTemplateGlobalContext() {
		return globalContext;
	}

	@Override
    public void setTableName(final String tableName) {
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return tableName;
	}

	@Override
    public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

	@Override
    public boolean isTemplateSpecified() {
		if (templateName != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
    public TemplateManager getNewTemplateManager() {
    	DatabaseTemplateManager templateManager = null;
    	try {
            templateManager = MissionConfiguredTemplateManagerFactory.getNewDatabaseTemplateManager(sseFlag);
		} catch (final TemplateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return templateManager;
	}
	
	@Override
    public String getFullTemplateName() {
    	String name = "db" + File.separator + tableName + File.separator + templateName;
    	if(name.endsWith(TemplateManager.EXTENSION) == false) {
    		name += TemplateManager.EXTENSION;
    	}
    	return name;
	}

	@Override
    public void setPrintWriter(final PrintWriter printWriter) {
		this.printWriter = printWriter;
	}
	
	@Override
    public PrintWriter getPrintWriter() {
		return printWriter;
	}

	@Override
    public void setCsvColumns(final List<String> csvColumns) {
		this.csvColumns = csvColumns;
	}
	
	@Override
    public List<String> getCsvColumns() {
		return (csvColumns != null ? csvColumns : Collections.emptyList());
	}

	@Override
    public void setIncludePacketInfo(final boolean includePacketInfo) {
		this.includePacketInfo = includePacketInfo;
	}
	
	@Override
    public boolean isIncludePacketInfo() {
		return includePacketInfo;
	}

	@Override
    public void setAlarms(final AlarmControl alarms) {
		this.alarms = alarms;
		isUsingAlarmFilter = true;
	}
	
	@Override
    public AlarmControl getAlarms() {
		return alarms;
	}
	
	@Override
    public boolean isUsingAlarmFilter() {
		return isUsingAlarmFilter;
	}

	@Override
	public void setChannelLookupTable(final Set<String> channelLookup) {
		this.channelLookup = channelLookup;
	}

	@Override
	public Set<String> getChannelLookupTable() {
		return channelLookup;
	}

	@Override
	public void setOrderings(final List<IChannelAggregateOrderByType> orderings) {
		this.orderings = orderings;
	}

	@Override
	public List<IChannelAggregateOrderByType> getOrderings() {
		return orderings;
	}

	@Override
	public void setDatabaseTimeRanges(final DatabaseTimeRange dbTimeRanges) {
		this.dbTimeRanges = dbTimeRanges;
	}

	@Override
	public DatabaseTimeRange getDatabaseTimeRanges() {
		return dbTimeRanges;
	}
	
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

	@Override
	public boolean isUsingChannelFilter() {
		return (channelLookup != null && !channelLookup.isEmpty());
	}

	@Override
	public boolean isUsingTimeRange() {
		return (dbTimeRanges != null && dbTimeRanges.isRangeSpecified());
	}
	
	@Override
	public int getBatchTempFileThreshold() {
		return maxTempFileThreshold;
	}

	@Override
	public void setSortEnabled() {
		this.sortEnabled = true;
	}

	@Override
	public boolean isSortEnabled() {
		return sortEnabled;
	}

	@Override
	public void setModulePattern(final String modulePattern) {
		this.modulePattern = modulePattern;
	}

	@Override
	public boolean isUsingModuleFilter() {
		return (modulePattern != null);
	}

	@Override
	public String getModulePattern() {
		return modulePattern;
	}

	@Override
	public String getModulePatternRegex() {
		return getProperty(MODULE_PATTERN_REGEX);
	}

	@Override
	public void setChangesOnly() {
		this.changesOnly = true;
	}

	@Override
	public boolean isUsingChangesOnlyFilter() {
		return changesOnly;
	}

	@Override
	public void setShowColumnHeaders(final boolean showColHeaders) {
		this.showColHeaders = showColHeaders;
	}

	@Override
	public boolean isShowColumnHeaders() {
		return showColHeaders;
	}

	@Override
	public void setSessionPreFetch(final IDbSessionPreFetch sessionPreFetch) {
		this.sessionPreFetch = sessionPreFetch;
	}

	@Override
	public IDbSessionPreFetch getSessionPreFetch() {
		return sessionPreFetch;
	}

	@Override
	public void setCsvHeaders(final List<String> csvHeaders) {
		this.csvHeaders = csvHeaders;
	}

	@Override
	public List<String> getCsvHeaders() {
		return csvHeaders;
	}

	@Override
	public String getOrderByString() {
		return orderByString;
	}

    @Override
    public void setOrderByType(final int orderByType) {
        this.orderByType = orderByType;
    }

    @Override
    public int getOrderByType() {
        return orderByType;
    }

	@Override
	public void setBeginTimePad(int padInSeconds) {
		this.beginTimePadInSeconds = padInSeconds;
	}

	@Override
	public int getBeginTimePad() {
		return this.beginTimePadInSeconds;
	}

	@Override
    public void setBatchTempFileThreshold(final int maxTempFileThreshold) {
        this.maxTempFileThreshold = maxTempFileThreshold;
    }

    @Override
    public int getProcessorQueueSize() {
        return getIntProperty(PROCESSOR_QUEUE_SIZE, PROCESSOR_QUEUE_SIZE_DEFAULT);
    }

    @Override
    public int getOutputControllerQueueSize() {
        return getIntProperty(OUTPUT_CONTROLLER_QUEUE_SIZE, OUTPUT_CONTROLLER_QUEUE_SIZE_DEFAULT);
    }
}
